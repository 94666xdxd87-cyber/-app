package com.example.myapplication.core;

import android.content.Context;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileLoader {

    public static List<String> loadNouns(Context ctx) throws Exception      { return loadPool(ctx, "noun.txt"); }
    public static List<String> loadVerbs(Context ctx) throws Exception      { return loadPool(ctx, "verb.txt"); }
    public static List<String> loadAdverbs(Context ctx) throws Exception    { return loadPool(ctx, "adverb.txt"); }
    public static List<String> loadAdjectives(Context ctx) throws Exception { return loadPool(ctx, "adjective.txt"); }

    public static void savePool(Context ctx, String fileName, List<String> words) throws Exception {
        String internalName = "words_" + fileName;
        StringBuilder sb = new StringBuilder();
        for (String w : words) sb.append(w.trim()).append(" ");
        try (FileOutputStream fos = ctx.openFileOutput(internalName, Context.MODE_PRIVATE)) {
            fos.write(sb.toString().trim().getBytes("UTF-8"));
        }
    }

    public static String internalFileName(String assetFileName) {
        return "words_" + assetFileName;
    }

    private static List<String> loadPool(Context ctx, String assetFileName) throws Exception {
        String internalName = "words_" + assetFileName;
        File internalFile = new File(ctx.getFilesDir(), internalName);
        String content;
        if (internalFile.exists()) {
            content = readInternalFile(internalFile);
        } else {
            content = readAsset(ctx, assetFileName);
            try (FileOutputStream fos = ctx.openFileOutput(internalName, Context.MODE_PRIVATE)) {
                fos.write(content.getBytes("UTF-8"));
            }
        }
        return parseWords(content);
    }

    private static String readAsset(Context ctx, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(ctx.getAssets().open(fileName), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append(" ");
        reader.close();
        return sb.toString();
    }

    private static String readInternalFile(File file) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append(" ");
        reader.close();
        return sb.toString();
    }

    private static List<String> parseWords(String content) {
        List<String> pool = new ArrayList<>();
        for (String w : content.split("[\\s,]+")) {
            String trimmed = w.trim();
            if (!trimmed.isEmpty()) pool.add(trimmed);
        }
        return pool;
    }
}
