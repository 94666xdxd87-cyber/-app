package com.example.myapplication.core;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.util.*;

public class ErrorTracker {

    private static final String TAG = "ErrorTracker";

    private final Map<String, WrongWord> nounMap = new LinkedHashMap<>();
    private final Map<String, WrongWord> verbMap = new LinkedHashMap<>();
    private final Map<String, WrongWord> advMap  = new LinkedHashMap<>();
    private final Map<String, WrongWord> adjMap  = new LinkedHashMap<>();

    private final Context context;

    public ErrorTracker(Context context) {
        this.context = context.getApplicationContext();
        loadAll();
    }

    public void addError(String word, int type) {
        if (word == null || word.trim().isEmpty()) return;
        Map<String, WrongWord> map = getMapByType(type);
        if (map != null) {
            if (!map.containsKey(word)) map.put(word, new WrongWord(word));
            map.get(word).incrementCount();
        }
    }

    public void addOrSetError(String word, int type, int strength) {
        if (word == null || word.trim().isEmpty()) return;
        Map<String, WrongWord> map = getMapByType(type);
        if (map == null) return;
        WrongWord ww = map.containsKey(word) ? map.get(word) : new WrongWord(word);
        ww.errorCount = strength;
        map.put(word, ww);
    }

    public void setStrength(String word, int type, int strength) {
        if (word == null || word.trim().isEmpty()) return;
        Map<String, WrongWord> map = getMapByType(type);
        if (map == null) return;
        if (!map.containsKey(word)) map.put(word, new WrongWord(word));
        map.get(word).errorCount = strength;
    }

    public void decreaseError(String word, int type, int amount) {
        if (word == null || word.trim().isEmpty()) return;
        Map<String, WrongWord> map = getMapByType(type);
        if (map != null && map.containsKey(word)) {
            map.get(word).errorCount -= amount;
        }
    }

    public void decreaseErrorAnyType(String word, int amount) {
        if (word == null || word.trim().isEmpty()) return;
        for (int t = 1; t <= 4; t++) {
            Map<String, WrongWord> map = getMapByType(t);
            if (map != null && map.containsKey(word)) {
                map.get(word).errorCount -= amount;
                return;
            }
        }
    }

    public List<WrongWord> getWeakWordsSorted(int type) {
        Map<String, WrongWord> map = getMapByType(type);
        if (map == null) return new ArrayList<>();
        List<WrongWord> result = new ArrayList<>(map.values());
        result.removeIf(ww -> ww.errorCount <= 0);
        result.sort((a, b) -> Integer.compare(b.errorCount, a.errorCount));
        return result;
    }

    public List<WrongWord> getAllForType(int type) {
        Map<String, WrongWord> map = getMapByType(type);
        if (map == null) return new ArrayList<>();
        List<WrongWord> result = new ArrayList<>(map.values());
        result.sort((a, b) -> Integer.compare(b.errorCount, a.errorCount));
        return result;
    }

    public void resetWord(String word, int type) {
        Map<String, WrongWord> map = getMapByType(type);
        if (map != null && map.containsKey(word)) map.get(word).errorCount = 0;
    }

    public void removeWord(String word, int type) {
        Map<String, WrongWord> map = getMapByType(type);
        if (map != null) map.remove(word);
    }

    public List<WeakEntry> getAllWeakWordsSortedByCount() {
        List<WeakEntry> all = new ArrayList<>();
        for (int t = 1; t <= 4; t++) {
            Map<String, WrongWord> map = getMapByType(t);
            if (map == null) continue;
            for (WrongWord ww : map.values()) {
                if (ww.errorCount > 0) all.add(new WeakEntry(ww.word, ww.errorCount, t));
            }
        }
        all.sort((a, b) -> Integer.compare(b.errorCount, a.errorCount));
        return all;
    }

    public String getWeakReportString() {
        StringBuilder sb = new StringBuilder();
        appendWeakSection(sb, "📘 名詞", nounMap);
        appendWeakSection(sb, "🎬 動詞", verbMap);
        appendWeakSection(sb, "📗 副詞", advMap);
        appendWeakSection(sb, "📙 形容詞", adjMap);
        return sb.length() == 0 ? "目前沒有任何錯題紀錄！" : sb.toString();
    }

    private void appendWeakSection(StringBuilder sb, String label, Map<String, WrongWord> map) {
        List<WrongWord> list = new ArrayList<>(map.values());
        list.removeIf(ww -> ww.errorCount <= 0);
        if (list.isEmpty()) return;
        list.sort((a, b) -> Integer.compare(b.errorCount, a.errorCount));
        sb.append(label).append("：");
        for (int i = 0; i < Math.min(list.size(), 8); i++) {
            sb.append(list.get(i).word).append("(").append(list.get(i).errorCount).append(")");
            if (i < Math.min(list.size(), 8) - 1) sb.append("、");
        }
        if (list.size() > 8) sb.append(" 等共 ").append(list.size()).append(" 字");
        sb.append("\n");
    }

    public void save() {
        for (int i = 1; i <= 4; i++) saveSingleMap(getMapByType(i), getFileNameByType(i));
    }

    private void saveSingleMap(Map<String, WrongWord> map, String fileName) {
        StringBuilder sb = new StringBuilder();
        for (WrongWord ww : map.values()) sb.append(ww.word).append(" ").append(ww.errorCount).append(" ");
        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(sb.toString().trim().getBytes("UTF-8"));
        } catch (Exception e) { Log.e(TAG, "寫入失敗: " + fileName + " " + e.getMessage()); }
    }

    private void loadAll() {
        for (int i = 1; i <= 4; i++) loadSingleMap(getMapByType(i), getFileNameByType(i));
    }

    private void loadSingleMap(Map<String, WrongWord> map, String fileName) {
        File file = new File(context.getFilesDir(), fileName);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append(" ");
            String content = sb.toString().trim();
            if (content.isEmpty()) return;
            String[] parts = content.split("\\s+");
            for (int i = 0; i + 1 < parts.length; i += 2) {
                String word  = parts[i];
                int    count = Integer.parseInt(parts[i + 1]);
                WrongWord ww = new WrongWord(word);
                ww.errorCount = count;
                map.put(word, ww);
            }
        } catch (Exception e) { Log.e(TAG, "讀取失敗: " + fileName + " " + e.getMessage()); }
    }

    private Map<String, WrongWord> getMapByType(int type) {
        switch (type) {
            case 1: return nounMap; case 2: return verbMap;
            case 3: return advMap;  case 4: return adjMap;
            default: return null;
        }
    }

    private String getFileNameByType(int type) {
        switch (type) {
            case 1: return "wrong_noun.txt"; case 2: return "wrong_verb.txt";
            case 3: return "wrong_adv.txt";  case 4: return "wrong_adj.txt";
            default: return "wrong_unknown.txt";
        }
    }

    public static class WeakEntry {
        public final String word;
        public final int errorCount;
        public final int type;

        public WeakEntry(String word, int errorCount, int type) {
            this.word = word; this.errorCount = errorCount; this.type = type;
        }

        public String getTypeName() {
            switch (type) {
                case 1: return "名詞"; case 2: return "動詞";
                case 3: return "副詞"; case 4: return "形容詞";
                default: return "未知";
            }
        }

        @Override
        public String toString() {
            return word + "[" + getTypeName() + "](強度:" + errorCount + ")";
        }
    }
}
