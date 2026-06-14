package com.example.myapplication.essay;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.util.*;

/** 歷屆考古題的儲存管理器 */
public class PastExamManager {

    private static final String QUESTION_FILE = "past_exam_questions.txt";
    private static final String TAG           = "PastExamManager";

    // ════════════════════════════════
    //  考古題 CRUD
    // ════════════════════════════════

    public static List<PastExamQuestion> loadAllQuestions(Context ctx) {
        List<PastExamQuestion> list = new ArrayList<>();
        File file = new File(ctx.getFilesDir(), QUESTION_FILE);
        if (!file.exists()) return list;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                PastExamQuestion q = PastExamQuestion.deserialize(line);
                if (q != null) list.add(q);
            }
        } catch (Exception e) { Log.e(TAG, "讀取題目失敗: " + e.getMessage()); }
        return list;
    }

    public static List<PastExamQuestion> loadBySubject(Context ctx, String subject) {
        List<PastExamQuestion> result = new ArrayList<>();
        for (PastExamQuestion q : loadAllQuestions(ctx)) {
            if (subject.equals(q.subject)) result.add(q);
        }
        return result;
    }

    public static List<String> loadAllYears(Context ctx) {
        Set<String> set = new LinkedHashSet<>();
        for (PastExamQuestion q : loadAllQuestions(ctx)) set.add(q.year);
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }

    /** 依考試類型取得年份列表（排序後） */
    public static List<String> loadYearsByExamType(Context ctx, String examType) {
        Set<String> set = new LinkedHashSet<>();
        for (PastExamQuestion q : loadAllQuestions(ctx)) {
            if (examType.equals(q.examType)) set.add(q.year);
        }
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }

    /** 依考試類型 + 年份取得考題列表 */
    public static List<PastExamQuestion> loadByExamTypeAndYear(
            Context ctx, String examType, String year) {
        List<PastExamQuestion> result = new ArrayList<>();
        for (PastExamQuestion q : loadAllQuestions(ctx)) {
            if (examType.equals(q.examType) && year.equals(q.year)) result.add(q);
        }
        return result;
    }

    public static List<String> loadAllSubjects(Context ctx) {
        Set<String> set = new LinkedHashSet<>();
        for (PastExamQuestion q : loadAllQuestions(ctx)) set.add(q.subject);
        return new ArrayList<>(set);
    }

    public static void saveQuestion(Context ctx, PastExamQuestion q) {
        try (FileOutputStream fos = ctx.openFileOutput(QUESTION_FILE, Context.MODE_APPEND)) {
            fos.write((q.serialize() + "\n").getBytes("UTF-8"));
        } catch (Exception e) { Log.e(TAG, "儲存題目失敗: " + e.getMessage()); }
    }

    public static boolean deleteQuestion(Context ctx, String questionId) {
        File file = new File(ctx.getFilesDir(), QUESTION_FILE);
        if (!file.exists()) return false;
        try {
            List<String> lines = readLines(file);
            boolean found = false;
            Iterator<String> it = lines.iterator();
            while (it.hasNext()) {
                PastExamQuestion q = PastExamQuestion.deserialize(it.next());
                if (q != null && q.id.equals(questionId)) { it.remove(); found = true; break; }
            }
            if (found) writeLines(ctx, QUESTION_FILE, lines);
            return found;
        } catch (Exception e) { return false; }
    }

    /** 清空全部考古題（重新匯入前使用）*/
    public static void clearAllQuestions(Context ctx) {
        try (FileOutputStream fos = ctx.openFileOutput(QUESTION_FILE, Context.MODE_PRIVATE)) {
            fos.write("".getBytes());
        } catch (Exception e) { Log.e(TAG, "清空失敗: " + e.getMessage()); }
    }

    public static PastExamQuestion findQuestionById(Context ctx, String id) {
        for (PastExamQuestion q : loadAllQuestions(ctx)) {
            if (q.id.equals(id)) return q;
        }
        return null;
    }

    // ════════════════════════════════
    //  與申論紀錄的關聯查詢
    // ════════════════════════════════

    public static List<EssayRecord> loadRecordsForQuestion(Context ctx, String questionId) {
        List<EssayRecord> result = new ArrayList<>();
        for (EssayRecord r : EssayManager.loadAll(ctx)) {
            if (questionId.equals(r.sourceQuestionId)) result.add(r);
        }
        return result;
    }

    // ════════════════════════════════
    //  工具
    // ════════════════════════════════

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    private static List<String> readLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) lines.add(line);
        }
        return lines;
    }

    private static void writeLines(Context ctx, String fileName, List<String> lines) throws Exception {
        try (FileOutputStream fos = ctx.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            for (String line : lines) fos.write((line + "\n").getBytes("UTF-8"));
        }
    }
}
