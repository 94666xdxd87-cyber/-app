package com.example.myapplication.essay;

import android.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * 一筆申論題紀錄的資料模型。
 * 序列化格式（pipe 分隔，長文字欄位用 Base64）：
 *   id | timestamp | subject | question(b64) | userAnswer(b64) | aiReply(b64)
 *   | sourceQuestionId | parsedTotalScore | parsedOverallComment(b64) | parsedWeaknesses(b64)
 */
public class EssayRecord {

    public String id;
    public String timestamp;
    public String subject;
    public String question;
    public String userAnswer;
    public String aiReply;

    // ── 考古題關聯 ──
    /** 若從歷屆考古題進入，記錄對應題目的 id；一般新增則為空字串 */
    public String sourceQuestionId;

    // ── 解析後的結構化結果 ──
    public int    parsedTotalScore;          // 解析出的總分
    public String parsedOverallComment;      // 總評
    public String parsedWeaknesses;          // 不足之處（換行分隔）

    public EssayRecord() {}

    public EssayRecord(String id, String timestamp, String subject,
                       String question, String userAnswer, String aiReply) {
        this.id               = id         != null ? id         : "";
        this.timestamp        = timestamp  != null ? timestamp  : "";
        this.subject          = subject    != null ? subject    : "";
        this.question         = question   != null ? question   : "";
        this.userAnswer       = userAnswer != null ? userAnswer : "";
        this.aiReply          = aiReply    != null ? aiReply    : "";
        this.sourceQuestionId = "";
        this.parsedTotalScore = -1;
        this.parsedOverallComment = "";
        this.parsedWeaknesses     = "";
    }

    public boolean hasAiReply()    { return aiReply    != null && !aiReply.trim().isEmpty(); }
    public boolean hasUserAnswer() { return userAnswer  != null && !userAnswer.trim().isEmpty(); }
    public boolean hasParsedResult() { return parsedTotalScore >= 0; }
    public boolean isFromPastExam()  { return sourceQuestionId != null && !sourceQuestionId.isEmpty(); }

    // ════════════════════════════════
    //  序列化（10 欄）
    // ════════════════════════════════

    public String serialize() {
        return id + "|"
                + timestamp + "|"
                + b64(subject) + "|"
                + b64(question) + "|"
                + b64(userAnswer) + "|"
                + b64(aiReply) + "|"
                + (sourceQuestionId != null ? sourceQuestionId : "") + "|"
                + parsedTotalScore + "|"
                + b64(parsedOverallComment) + "|"
                + b64(parsedWeaknesses);
    }

    public static EssayRecord deserialize(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String[] p = line.split("\\|", -1);
        if (p.length < 6) return null;
        try {
            EssayRecord r = new EssayRecord();
            r.id         = p[0];
            r.timestamp  = p[1];
            r.subject    = dec(p[2]);
            r.question   = dec(p[3]);
            r.userAnswer = dec(p[4]);
            r.aiReply    = dec(p[5]);
            r.sourceQuestionId    = p.length >= 7  ? p[6]              : "";
            r.parsedTotalScore    = p.length >= 8  ? parseInt(p[7], -1): -1;
            r.parsedOverallComment= p.length >= 9  ? dec(p[8])         : "";
            r.parsedWeaknesses    = p.length >= 10 ? dec(p[9])         : "";
            return r;
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
