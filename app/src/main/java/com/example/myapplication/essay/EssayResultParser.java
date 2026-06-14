package com.example.myapplication.essay;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

/**
 * 解析 PromptB / PromptC 格式的 AI 回覆
 *
 * PromptB 格式（手動流程）：
 * ===RESULT_START===
 * TOTAL_SCORE: {數字}
 * ---POINTS_START---
 * POINT_NAME: ...
 * POINT_FULL_SCORE: ...
 * POINT_SCORE: ...
 * POINT_REASON: ...
 * ---
 * ...
 * ---POINTS_END---
 * OVERALL_COMMENT: ...
 * ---WEAKNESS_START---
 * - ...
 * ---WEAKNESS_END---
 * ===RESULT_END===
 *
 * PromptC 格式（API 自動流程）：在 RESULT 區塊前額外含 ---RUBRIC_START--- 評分標準 ---RUBRIC_END---
 */
public class EssayResultParser {

    public static class PointResult {
        public String name;
        public int    fullScore;
        public int    score;
        public String reason;

        public PointResult(String name, int fullScore, int score, String reason) {
            this.name      = name;
            this.fullScore = fullScore;
            this.score     = score;
            this.reason    = reason;
        }
    }

    // PromptC 評分標準條目
    public static class RubricItem {
        public String name;
        public int    fullScore;
        public String baseCondition;   // 基礎給分條件
        public String perfectCondition; // 滿分條件

        public RubricItem(String name, int fullScore, String baseCondition, String perfectCondition) {
            this.name             = name;
            this.fullScore        = fullScore;
            this.baseCondition    = baseCondition;
            this.perfectCondition = perfectCondition;
        }
    }

    public static class ParsedResult {
        public boolean isValid = false;
        public int     totalScore;
        public List<PointResult> points = new ArrayList<>();
        public List<RubricItem>  rubric = new ArrayList<>();  // PromptC 評分標準
        public String  overallComment;
        public String  weaknesses;   // 換行分隔
        public String  errorMsg;

        /** 組出可顯示的摘要字串 */
        public String toDisplayString() {
            if (!isValid) return "❌ 解析失敗：" + errorMsg;
            StringBuilder sb = new StringBuilder();
            sb.append("🏆 總分：").append(totalScore).append("\n\n");
            for (PointResult p : points) {
                sb.append("▸ ").append(p.name)
                  .append("  ").append(p.score).append("/").append(p.fullScore).append("\n");
                sb.append("  ").append(p.reason).append("\n\n");
            }
            if (overallComment != null && !overallComment.isEmpty()) {
                sb.append("📝 總評：\n").append(overallComment).append("\n\n");
            }
            if (weaknesses != null && !weaknesses.isEmpty()) {
                sb.append("⚠️ 待改進：\n").append(weaknesses);
            }
            return sb.toString().trim();
        }
    }

    // ════════════════════════════════
    //  主解析入口
    // ════════════════════════════════

    public static ParsedResult parse(String raw) {
        ParsedResult result = new ParsedResult();
        if (raw == null || raw.trim().isEmpty()) {
            result.errorMsg = "輸入為空"; return result;
        }

        // RUBRIC（PromptC 格式，可選）
        int rbStart = raw.indexOf("---RUBRIC_START---");
        int rbEnd   = raw.indexOf("---RUBRIC_END---");
        if (rbStart != -1 && rbEnd != -1 && rbEnd > rbStart) {
            String rubricBlock = raw.substring(rbStart + 18, rbEnd);
            String[] rbItems = rubricBlock.split("---(?!RUBRIC)");
            for (String ri : rbItems) {
                ri = ri.trim();
                if (ri.isEmpty()) continue;
                RubricItem item = parseRubricItem(ri);
                if (item != null) result.rubric.add(item);
            }
        }

        // 取出 ===RESULT_START=== ... ===RESULT_END=== 之間的內容
        int start = raw.indexOf("===RESULT_START===");
        int end   = raw.indexOf("===RESULT_END===");
        if (start == -1 || end == -1 || end < start) {
            result.errorMsg = "找不到 ===RESULT_START=== / ===RESULT_END===，請確認貼入的是 Prompt B 的回覆";
            return result;
        }
        String body = raw.substring(start + 18, end).trim();

        // TOTAL_SCORE
        Matcher mTotal = Pattern.compile("TOTAL_SCORE:\\s*(\\d+)").matcher(body);
        if (!mTotal.find()) { result.errorMsg = "找不到 TOTAL_SCORE"; return result; }
        result.totalScore = Integer.parseInt(mTotal.group(1));

        // POINTS
        int psStart = body.indexOf("---POINTS_START---");
        int psEnd   = body.indexOf("---POINTS_END---");
        if (psStart != -1 && psEnd != -1 && psEnd > psStart) {
            String pointsBlock = body.substring(psStart + 18, psEnd);
            String[] pointBlocks = pointsBlock.split("---(?!POINTS)");
            for (String pb : pointBlocks) {
                pb = pb.trim();
                if (pb.isEmpty()) continue;
                PointResult pr = parsePoint(pb);
                if (pr != null) result.points.add(pr);
            }
        }

        // OVERALL_COMMENT
        Matcher mComment = Pattern.compile("OVERALL_COMMENT:\\s*(.+?)(?=---WEAKNESS_START---|===RESULT_END===|$)",
                Pattern.DOTALL).matcher(body);
        if (mComment.find()) result.overallComment = mComment.group(1).trim();

        // WEAKNESSES
        int wsStart = body.indexOf("---WEAKNESS_START---");
        int wsEnd   = body.indexOf("---WEAKNESS_END---");
        if (wsStart != -1 && wsEnd != -1 && wsEnd > wsStart) {
            String wBlock = body.substring(wsStart + 20, wsEnd).trim();
            // 移除開頭的 "- " 但保留換行
            result.weaknesses = wBlock.replaceAll("(?m)^-\\s*", "• ").trim();
        }

        result.isValid = true;
        return result;
    }

    private static PointResult parsePoint(String block) {
        try {
            Matcher mName  = Pattern.compile("POINT_NAME:\\s*(.+)").matcher(block);
            Matcher mFull  = Pattern.compile("POINT_FULL_SCORE:\\s*(\\d+)").matcher(block);
            Matcher mScore = Pattern.compile("POINT_SCORE:\\s*(\\d+)").matcher(block);
            Matcher mReason= Pattern.compile("POINT_REASON:\\s*(.+)", Pattern.DOTALL).matcher(block);
            if (!mName.find() || !mFull.find() || !mScore.find() || !mReason.find()) return null;
            return new PointResult(
                    mName.group(1).trim(),
                    Integer.parseInt(mFull.group(1).trim()),
                    Integer.parseInt(mScore.group(1).trim()),
                    mReason.group(1).trim()
            );
        } catch (Exception e) { return null; }
    }

    private static RubricItem parseRubricItem(String block) {
        try {
            Matcher mName    = Pattern.compile("RUBRIC_ITEM:\\s*(.+)").matcher(block);
            Matcher mFull    = Pattern.compile("RUBRIC_FULL_SCORE:\\s*(\\d+)").matcher(block);
            Matcher mBase    = Pattern.compile("RUBRIC_BASE:\\s*(.+?)(?=RUBRIC_PERFECT:|$)",
                    Pattern.DOTALL).matcher(block);
            Matcher mPerfect = Pattern.compile("RUBRIC_PERFECT:\\s*(.+)", Pattern.DOTALL).matcher(block);
            if (!mName.find() || !mFull.find()) return null;
            String base    = mBase.find()    ? mBase.group(1).trim()    : "";
            String perfect = mPerfect.find() ? mPerfect.group(1).trim() : "";
            return new RubricItem(
                    mName.group(1).trim(),
                    Integer.parseInt(mFull.group(1).trim()),
                    base, perfect
            );
        } catch (Exception e) { return null; }
    }
}
