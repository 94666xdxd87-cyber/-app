package com.example.myapplication.core;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class WordExplainManager {

    private static final String FILE_NAME = "word_explain_history.txt";
    private static final String TAG = "WordExplainManager";

    public static void save(Context ctx, WordExplainRecord record) {
        List<WordExplainRecord> all = loadAll(ctx);
        all.removeIf(r -> r.word.equalsIgnoreCase(record.word));
        all.add(record);
        writeAll(ctx, all);
    }

    public static void saveAll(Context ctx, List<WordExplainRecord> records) {
        List<WordExplainRecord> existing = loadAll(ctx);
        for (WordExplainRecord r : records) {
            existing.removeIf(e -> e.word.equalsIgnoreCase(r.word));
            existing.add(r);
        }
        writeAll(ctx, existing);
    }

    public static void updateAudioPath(Context ctx, String word, String audioPath) {
        List<WordExplainRecord> all = loadAll(ctx);
        boolean updated = false;
        for (WordExplainRecord r : all) {
            if (r.word.equalsIgnoreCase(word)) { r.audioPath = audioPath; updated = true; break; }
        }
        if (updated) writeAll(ctx, all);
    }

    public static List<WordExplainRecord> loadAll(Context ctx) {
        List<WordExplainRecord> list = new ArrayList<>();
        File file = new File(ctx.getFilesDir(), FILE_NAME);
        if (!file.exists()) return list;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                WordExplainRecord rec = WordExplainRecord.deserialize(line);
                if (rec != null) list.add(rec);
            }
        } catch (Exception e) { Log.e(TAG, "讀取失敗: " + e.getMessage()); }
        return list;
    }

    public static List<WordExplainRecord> loadWithoutAudio(Context ctx) {
        List<WordExplainRecord> result = new ArrayList<>();
        for (WordExplainRecord r : loadAll(ctx)) {
            if (!r.hasAudio() && !AudioService.hasLocalAudio(ctx, r.word)) result.add(r);
        }
        return result;
    }

    public static boolean hasRecord(Context ctx, String word) {
        for (WordExplainRecord r : loadAll(ctx)) {
            if (r.word.equalsIgnoreCase(word)) return true;
        }
        return false;
    }

    public static void delete(Context ctx, String word) {
        List<WordExplainRecord> all = loadAll(ctx);
        all.removeIf(r -> r.word.equalsIgnoreCase(word));
        writeAll(ctx, all);
        AudioService.deleteLocalAudio(ctx, word);
    }

    public static void clear(Context ctx) {
        try (FileOutputStream fos = ctx.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write("".getBytes());
        } catch (Exception e) { Log.e(TAG, "清除失敗: " + e.getMessage()); }
    }

    private static void writeAll(Context ctx, List<WordExplainRecord> list) {
        try (FileOutputStream fos = ctx.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            StringBuilder sb = new StringBuilder();
            for (WordExplainRecord r : list) sb.append(r.serialize()).append("\n");
            fos.write(sb.toString().getBytes("UTF-8"));
        } catch (Exception e) { Log.e(TAG, "寫入失敗: " + e.getMessage()); }
    }

    public static String currentTimestamp() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date());
    }
}
