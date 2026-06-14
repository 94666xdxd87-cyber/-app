package com.example.myapplication.essay;

import android.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * 歷屆考古題的資料模型
 * 序列化 v2：id | year | subject | score | question(b64) | examType(b64)
 * （examType 放最後，向前相容：舊資料缺此欄時 examType 預設為「高考」）
 */
public class PastExamQuestion {

    public String id;
    public String year;       // 例如：113年
    public String subject;    // 例如：資通網路
    public int    score;      // 配分
    public String question;   // 題目內容
    public String examType;   // 考試類型：高考、普考、地方特考三等、地方特考四等

    public PastExamQuestion() {}

    public PastExamQuestion(String id, String year, String subject,
                             int score, String question) {
        this(id, year, subject, score, question, "高考");
    }

    public PastExamQuestion(String id, String year, String subject,
                             int score, String question, String examType) {
        this.id       = id       != null ? id       : "";
        this.year     = year     != null ? year     : "";
        this.subject  = subject  != null ? subject  : "";
        this.score    = score;
        this.question = question != null ? question : "";
        this.examType = examType != null ? examType : "高考";
    }

    public String getDisplayTitle() {
        return year + " " + subject + "（" + score + " 分）";
    }

    public String serialize() {
        return id + "|" + year + "|" + b64(subject) + "|" + score
                + "|" + b64(question) + "|" + b64(examType);
    }

    public static PastExamQuestion deserialize(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String[] p = line.split("\\|", -1);
        if (p.length < 5) return null;
        try {
            String examType = (p.length >= 6 && !p[5].isEmpty()) ? dec(p[5]) : "高考";
            return new PastExamQuestion(
                    p[0], p[1], dec(p[2]), parseInt(p[3], 25), dec(p[4]), examType);
        } catch (Exception e) { return null; }
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static String b64(String s) {
        if (s == null) s = "";
        return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    private static String dec(String s) {
        if (s == null || s.isEmpty()) return "";
        try { return new String(Base64.decode(s, Base64.NO_WRAP), StandardCharsets.UTF_8); }
        catch (Exception e) { return ""; }
    }
}
