package com.example.myapplication.core;

import androidx.annotation.NonNull;

public class HistoryRecord {
    public String timestamp;
    public String modeName;
    public int    score;
    public int    total;
    public String category;
    public String examSessionKey;
    public String quizSessionKey;

    public HistoryRecord(String timestamp, String modeName, int score, int total,
                         String category, String examSessionKey, String quizSessionKey) {
        this.timestamp      = (timestamp != null) ? timestamp : "";
        this.modeName       = (modeName != null) ? modeName : "未知模式";
        this.score          = score;
        this.total          = total;
        this.category       = (category != null && !category.isEmpty()) ? category : "PRACTICE";
        this.examSessionKey = (examSessionKey != null) ? examSessionKey : "";
        this.quizSessionKey = (quizSessionKey != null) ? quizSessionKey : "";
    }

    public HistoryRecord(String timestamp, String modeName, int score, int total,
                         String category, String quizSessionKey) {
        this(timestamp, modeName, score, total, category, "", quizSessionKey);
    }

    public double getRate() { return total == 0 ? 0 : score * 100.0 / total; }

    @NonNull
    public String serialize() {
        return String.format("%s|%s|%d|%d|%s|%s|%s",
                timestamp, modeName, score, total, category, examSessionKey, quizSessionKey);
    }

    public static HistoryRecord deserialize(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String[] p = line.split("\\|", -1);
        if (p.length < 4) return null;
        try {
            String time = p[0];
            String mode = p[1];
            int s = Integer.parseInt(p[2]);
            int t = Integer.parseInt(p[3]);
            String cat     = (p.length >= 5 && !p[4].isEmpty()) ? p[4] : "PRACTICE";
            String examKey = (p.length >= 6) ? p[5] : "";
            String quizKey = (p.length >= 7) ? p[6] : "";
            return new HistoryRecord(time, mode, s, t, cat, examKey, quizKey);
        } catch (Exception e) { return null; }
    }
}
