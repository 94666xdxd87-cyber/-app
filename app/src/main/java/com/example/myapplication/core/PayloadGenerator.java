package com.example.myapplication.core;

import java.util.*;

public class PayloadGenerator {

    public static List<String> generate(List<String> wordPool, int count) {
        List<String> payloads = new ArrayList<>();
        List<String> pool = new ArrayList<>(wordPool);
        Collections.shuffle(pool);
        int idx = 0;
        for (int i = 0; i < count; i++) {
            if (idx + 4 > pool.size()) break;
            List<String> sel = pool.subList(idx, idx + 4);
            payloads.add("正解: " + sel.get(0) + ", 干擾: " + sel.subList(1, 4));
            idx += 4;
        }
        return payloads;
    }

    public static List<String> generateFromWeak(List<WrongWord> weakWords,
                                                 List<String> backupPool,
                                                 int count) {
        if (count == 0 || weakWords.isEmpty()) return new ArrayList<>();
        int actualCount = Math.min(count, weakWords.size());
        List<String> correctAnswers = new ArrayList<>();
        for (int i = 0; i < actualCount; i++) correctAnswers.add(weakWords.get(i).word);
        List<String> distractorPool = new ArrayList<>();
        for (int i = actualCount; i < weakWords.size(); i++) distractorPool.add(weakWords.get(i).word);
        distractorPool.addAll(backupPool);
        distractorPool.removeAll(correctAnswers);
        Collections.shuffle(distractorPool);
        List<String> payloads = new ArrayList<>();
        int dIdx = 0;
        for (String correct : correctAnswers) {
            if (dIdx + 3 > distractorPool.size()) break;
            List<String> chosen = distractorPool.subList(dIdx, dIdx + 3);
            payloads.add("正解: " + correct + ", 干擾: " + chosen);
            dIdx += 3;
        }
        return payloads;
    }
}
