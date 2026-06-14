package com.example.myapplication.core;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class HistoryManager {

    private static final String FILE_NAME = "quiz_history.txt";
    private static final String TAG = "HistoryManager";

    public static void save(Context ctx, HistoryRecord record) {
        try (FileOutputStream fos = ctx.openFileOutput(FILE_NAME, Context.MODE_APPEND)) {
            fos.write((record.serialize() + "\n").getBytes("UTF-8"));
        } catch (Exception e) { Log.e(TAG, "儲存失敗: " + e.getMessage()); }
    }

    public static List<HistoryRecord> loadAll(Context ctx) { return loadByCategory(ctx, null); }

    public static List<HistoryRecord> loadByCategory(Context ctx, String category) {
        List<HistoryRecord> list = new ArrayList<>();
        File file = new File(ctx.getFilesDir(), FILE_NAME);
        if (!file.exists()) return list;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                HistoryRecord rec = HistoryRecord.deserialize(line);
                if (rec == null) continue;
                if (category == null || category.equals(rec.category)) list.add(rec);
            }
        } catch (Exception e) { Log.e(TAG, "讀取失敗: " + e.getMessage()); }
        return list;
    }

    public static void clear(Context ctx) {
        try (FileOutputStream fos = ctx.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write("".getBytes());
        } catch (Exception e) { Log.e(TAG, "清除失敗: " + e.getMessage()); }
    }

    public static void saveRanking(Context ctx, String sessionKey, String rawRankingData) {
        if (sessionKey == null || sessionKey.isEmpty()) return;
        writeFile(ctx, "ranking_" + sanitizeKey(sessionKey) + ".txt", rawRankingData);
    }

    public static String loadRanking(Context ctx, String sessionKey) {
        if (sessionKey == null || sessionKey.isEmpty()) return null;
        return readFile(ctx, "ranking_" + sanitizeKey(sessionKey) + ".txt");
    }

    public static boolean hasRanking(Context ctx, String sessionKey) {
        if (sessionKey == null || sessionKey.isEmpty()) return false;
        return new File(ctx.getFilesDir(), "ranking_" + sanitizeKey(sessionKey) + ".txt").exists();
    }

    public static void saveQuizContent(Context ctx, String sessionKey, List<Quiz> quizList) {
        if (sessionKey == null || sessionKey.isEmpty() || quizList == null) return;
        StringBuilder sb = new StringBuilder();
        for (Quiz q : quizList) {
            String qBody = Base64.encodeToString(q.questionBody.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            String qExp  = Base64.encodeToString(q.explanation.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            sb.append(q.id).append("|").append(q.correctAnswer).append("|")
              .append(q.userAnswer).append("|").append(q.categoryType).append("|")
              .append(qBody).append("|").append(qExp).append("\n");
        }
        writeFile(ctx, "quiz_" + sanitizeKey(sessionKey) + ".txt", sb.toString());
    }

    public static List<Quiz> loadQuizContent(Context ctx, String sessionKey) {
        if (sessionKey == null || sessionKey.isEmpty()) return new ArrayList<>();
        String content = readFile(ctx, "quiz_" + sanitizeKey(sessionKey) + ".txt");
        if (content == null) return new ArrayList<>();
        List<Quiz> list = new ArrayList<>();
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] p = line.split("\\|", -1);
            if (p.length < 6) continue;
            try {
                int    id      = Integer.parseInt(p[0]);
                String correct = p[1];
                String userAns = p[2];
                int    catType = Integer.parseInt(p[3]);
                String body    = new String(Base64.decode(p[4], Base64.NO_WRAP), StandardCharsets.UTF_8);
                String exp     = new String(Base64.decode(p[5], Base64.NO_WRAP), StandardCharsets.UTF_8);
                Quiz q = new Quiz(id, correct, body, exp, catType);
                q.userAnswer = userAns;
                list.add(q);
            } catch (Exception e) { Log.e(TAG, "題目解析失敗: " + e.getMessage()); }
        }
        return list;
    }

    public static boolean hasQuizContent(Context ctx, String sessionKey) {
        if (sessionKey == null || sessionKey.isEmpty()) return false;
        return new File(ctx.getFilesDir(), "quiz_" + sanitizeKey(sessionKey) + ".txt").exists();
    }

    private static void writeFile(Context ctx, String fileName, String content) {
        try (FileOutputStream fos = ctx.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(content.getBytes("UTF-8"));
        } catch (Exception e) { Log.e(TAG, "寫入失敗 " + fileName + ": " + e.getMessage()); }
    }

    private static String readFile(Context ctx, String fileName) {
        File file = new File(ctx.getFilesDir(), fileName);
        if (!file.exists()) return null;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    private static String sanitizeKey(String key) {
        return key.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    public static String currentTimestamp() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date());
    }

    public static String currentSessionKey(String prefix) {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        return sanitizeKey(prefix) + "_" + ts;
    }
}
