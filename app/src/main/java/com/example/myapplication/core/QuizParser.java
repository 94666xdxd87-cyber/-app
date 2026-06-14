package com.example.myapplication.core;

import java.util.*;
import java.util.regex.*;

public class QuizParser {

    public static List<Quiz> parse(String fullResponse, int type) {
        return parseWithTypeRanges(fullResponse, new int[]{Integer.MAX_VALUE}, new int[]{type});
    }

    public static List<Quiz> parseWithTypeRanges(String fullResponse, int[] counts, int[] typeIds) {
        List<Quiz> quizList = new ArrayList<>();
        if (fullResponse == null || fullResponse.startsWith("ERR:")) return quizList;

        int[] boundaries = new int[counts.length + 1];
        boundaries[0] = 0;
        for (int i = 0; i < counts.length; i++) boundaries[i + 1] = boundaries[i] + counts[i];

        String[] blocks = fullResponse.split("={5,}");
        int idCounter = 1, blockCounter = 0;

        for (String block : blocks) {
            String clean = block.trim();
            if (clean.isEmpty()) continue;

            int currentType = 0;
            for (int i = 0; i < counts.length; i++) {
                if (blockCounter >= boundaries[i] && blockCounter < boundaries[i + 1]) {
                    currentType = typeIds[i]; break;
                }
            }
            blockCounter++;

            String correctAnswer = findCorrectAnswer(clean);
            String questionBody = "", explanation = "（無解析）";
            if (!clean.contains("--~~--")) continue;
            String[] parts = clean.split("--~~--");
            if (parts.length >= 2) questionBody = parts[1].trim();
            if (parts.length >= 3) explanation = parts[2].trim();
            if (correctAnswer.isEmpty()) continue;
            quizList.add(new Quiz(idCounter++, correctAnswer, questionBody, explanation, currentType));
        }
        return quizList;
    }

    private static String findCorrectAnswer(String text) {
        int splitIdx = text.indexOf("--~~--");
        if (splitIdx != -1) {
            String pre = text.substring(0, splitIdx).trim().toUpperCase();
            Matcher m = Pattern.compile("^([A-D])$", Pattern.MULTILINE).matcher(pre);
            if (m.find()) return m.group(1);
            Matcher m2 = Pattern.compile("[A-D]").matcher(pre);
            String last = "";
            while (m2.find()) last = m2.group();
            if (!last.isEmpty()) return last;
        }
        Matcher m3 = Pattern.compile("^\\s*([A-D])\\s*$", Pattern.MULTILINE).matcher(text);
        if (m3.find()) return m3.group(1).toUpperCase();
        return "";
    }
}
