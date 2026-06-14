package com.example.myapplication.essay;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 歷屆考古題匯入器。
 *
 * assets/historyexam/
 *   高考三級-100/
 *     100高考三級-資通網路與安全.txt
 *     ...
 *
 * 每個 txt 代表一個科目，「一、」「二、」... 分割成各道題。
 * 使用 SharedPreferences 記錄已匯入版本，避免重複匯入。
 *
 * v3：新增 examType 欄位，從目錄名稱前綴解析考試類型。
 */
public class PastExamImporter {

    private static final String TAG          = "PastExamImporter";
    private static final String PREF_NAME    = "past_exam_import";
    private static final String PREF_VERSION = "imported_version";
    // ★ 升版至 3：新增 examType，舊資料需重新匯入
    private static final int    CURRENT_VERSION = 3;

    // 跳過「國文」和「法學知識與英文」，只匯入專業科目
    private static final String[] SKIP_KEYWORDS = {"國文", "法學知識與英文"};

    // 題號前綴：「一、」「二、」... 等
    private static final Pattern QUESTION_SPLIT_PATTERN =
            Pattern.compile("(?m)^([一二三四五六七八九十]+)、");

    public interface SuccessCallback { void onComplete(int totalImported, int skipped); }
    public interface ErrorCallback   { void onError(String msg); }

    // ════════════════════════════════
    //  公開 API
    // ════════════════════════════════

    public static void importIfNeeded(Context ctx,
                                      SuccessCallback onSuccess,
                                      ErrorCallback   onError) {
        new Thread(() -> doImport(ctx, false, onSuccess, onError)).start();
    }

    public static void forceReimport(Context ctx,
                                     SuccessCallback onSuccess,
                                     ErrorCallback   onError) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().remove(PREF_VERSION).apply();
        new Thread(() -> doImport(ctx, true, onSuccess, onError)).start();
    }

    // ════════════════════════════════
    //  內部執行
    // ════════════════════════════════

    private static void doImport(Context ctx, boolean force,
                                  SuccessCallback onSuccess,
                                  ErrorCallback   onError) {
        try {
            SharedPreferences prefs =
                    ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            int importedVersion = prefs.getInt(PREF_VERSION, 0);

            if (!force && importedVersion >= CURRENT_VERSION) {
                onSuccess.onComplete(0, 0);
                return;
            }

            PastExamManager.clearAllQuestions(ctx);

            int totalImported = 0;
            int skipped       = 0;

            String[] yearDirs = ctx.getAssets().list("historyexam");
            if (yearDirs == null || yearDirs.length == 0) {
                onError.onError("找不到 assets/historyexam 目錄");
                return;
            }

            for (String yearDir : yearDirs) {
                String year     = extractYear(yearDir);
                String examType = extractExamType(yearDir);

                String[] files = ctx.getAssets().list("historyexam/" + yearDir);
                if (files == null) continue;

                for (String fileName : files) {
                    if (fileName.contains("答案")) { skipped++; continue; }
                    if (shouldSkip(fileName))       { skipped++; continue; }

                    String subject = extractSubject(fileName);
                    String path    = "historyexam/" + yearDir + "/" + fileName;

                    try {
                        String content = readAsset(ctx, path);
                        List<PastExamQuestion> questions =
                                parseQuestions(content, year, subject, examType);
                        for (PastExamQuestion q : questions) {
                            PastExamManager.saveQuestion(ctx, q);
                            totalImported++;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "解析失敗：" + path + " → " + e.getMessage());
                        skipped++;
                    }
                }
            }

            prefs.edit().putInt(PREF_VERSION, CURRENT_VERSION).apply();
            onSuccess.onComplete(totalImported, skipped);

        } catch (Exception e) {
            Log.e(TAG, "匯入失敗：" + e.getMessage());
            onError.onError(e.getMessage());
        }
    }

    // ════════════════════════════════
    //  解析：一個科目 txt → 多道題
    // ════════════════════════════════

    static List<PastExamQuestion> parseQuestions(String content,
                                                  String year,
                                                  String subject,
                                                  String examType) {
        List<PastExamQuestion> list = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) return list;

        content = stripHeader(content);

        Matcher m = QUESTION_SPLIT_PATTERN.matcher(content);
        List<Integer> starts = new ArrayList<>();
        while (m.find()) starts.add(m.start());

        if (starts.isEmpty()) {
            String body = content.trim();
            if (!body.isEmpty())
                list.add(makeQuestion(year, subject, body, 25, examType));
            return list;
        }

        for (int i = 0; i < starts.size(); i++) {
            int end  = (i + 1 < starts.size()) ? starts.get(i + 1) : content.length();
            String body = content.substring(starts.get(i), end).trim();
            if (body.isEmpty()) continue;
            list.add(makeQuestion(year, subject, body, extractScore(body), examType));
        }

        return list;
    }

    /** 保留舊版呼叫相容（預設高考） */
    static List<PastExamQuestion> parseQuestions(String content, String year, String subject) {
        return parseQuestions(content, year, subject, "高考");
    }

    private static PastExamQuestion makeQuestion(String year, String subject,
                                                   String body, int score, String examType) {
        return new PastExamQuestion(
                PastExamManager.generateId(), year, subject, score, body, examType);
    }

    private static String stripHeader(String content) {
        Matcher m = QUESTION_SPLIT_PATTERN.matcher(content);
        if (m.find()) return content.substring(m.start());
        return content;
    }

    private static int extractScore(String body) {
        Matcher m = Pattern.compile("[（(](\\d{1,3})\\s*分[）)]").matcher(body);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        Matcher m2 = Pattern.compile("(\\d{1,3})\\s*分").matcher(body);
        if (m2.find()) {
            try {
                int s = Integer.parseInt(m2.group(1));
                if (s >= 5 && s <= 50) return s;
            } catch (Exception ignored) {}
        }
        return 25;
    }

    // ════════════════════════════════
    //  工具方法
    // ════════════════════════════════

    /** 從目錄名稱取得年份，例如「高考三級-100」→「100年」 */
    private static String extractYear(String dirName) {
        Matcher m = Pattern.compile("(\\d{3})$").matcher(dirName);
        return m.find() ? m.group(1) + "年" : dirName;
    }

    /**
     * 從目錄名稱解析考試類型。
     * 目錄格式範例：高考三級-100、普考-100、地方特考三等-100、地方特考四等-100
     */
    private static String extractExamType(String dirName) {
        if (dirName.startsWith("高考"))       return "高考";
        if (dirName.startsWith("普考"))       return "普考";
        if (dirName.startsWith("地方特考三")) return "地方特考三等";
        if (dirName.startsWith("地方特考四")) return "地方特考四等";
        return "高考";  // 預設
    }

    private static String extractSubject(String fileName) {
        String name = fileName.replaceAll("\\.txt$", "");
        int dash = name.indexOf('-');
        return (dash >= 0 && dash < name.length() - 1)
                ? name.substring(dash + 1).trim() : name;
    }

    private static boolean shouldSkip(String fileName) {
        for (String kw : SKIP_KEYWORDS) if (fileName.contains(kw)) return true;
        return false;
    }

    private static String readAsset(Context ctx, String path) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(ctx.getAssets().open(path), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        reader.close();
        return sb.toString();
    }
}
