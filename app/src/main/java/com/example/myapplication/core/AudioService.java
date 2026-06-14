package com.example.myapplication.core;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class AudioService {

    private static final String TAG      = "AudioService";
    private static final String BASE_URL =
            "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=en&q=";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public interface DownloadCallback {
        void onSuccess(String word, String localPath);
        void onNoAudio(String word);
        void onError(String word, String reason);
    }

    public interface BatchCallback {
        void onProgress(int done, int total, String currentWord);
        void onComplete(int successCount, int noAudioCount, int errorCount);
    }

    public static void downloadAsync(Context ctx, String word, DownloadCallback callback) {
        new Thread(() -> {
            try {
                String localPath = downloadMp3(ctx, word);
                if (localPath != null) callback.onSuccess(word, localPath);
                else callback.onError(word, "下載失敗");
            } catch (Exception e) { callback.onError(word, e.getMessage()); }
        }).start();
    }

    public static void downloadBatchAsync(Context ctx, List<WordExplainRecord> records,
                                          BatchCallback callback) {
        new Thread(() -> {
            int success = 0, noAudio = 0, error = 0;
            int total = records.size();
            for (int i = 0; i < total; i++) {
                WordExplainRecord r = records.get(i);
                callback.onProgress(i + 1, total, r.word);
                try {
                    String localPath = downloadMp3(ctx, r.word);
                    if (localPath != null) {
                        r.audioPath = localPath;
                        WordExplainManager.updateAudioPath(ctx, r.word, localPath);
                        success++;
                    } else { error++; }
                } catch (Exception e) { error++; }
            }
            callback.onComplete(success, noAudio, error);
        }).start();
    }

    private static String downloadMp3(Context ctx, String word) throws Exception {
        String fileName = "audio_" + sanitize(word) + ".mp3";
        File outFile    = new File(ctx.getFilesDir(), fileName);
        if (outFile.exists() && outFile.length() > 0) return outFile.getAbsolutePath();

        String urlStr = BASE_URL + java.net.URLEncoder.encode(word.trim(), "UTF-8");
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code != 200) return null;

        String contentType = conn.getContentType();
        if (contentType != null && contentType.contains("text")) return null;

        try (InputStream is = conn.getInputStream();
             FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buf = new byte[4096]; int len;
            while ((len = is.read(buf)) != -1) fos.write(buf, 0, len);
        }
        if (outFile.length() < 1024) { outFile.delete(); return null; }
        return outFile.getAbsolutePath();
    }

    public static String getLocalAudioPath(Context ctx, String word) {
        File f = new File(ctx.getFilesDir(), "audio_" + sanitize(word) + ".mp3");
        return (f.exists() && f.length() > 0) ? f.getAbsolutePath() : null;
    }

    public static boolean hasLocalAudio(Context ctx, String word) {
        return getLocalAudioPath(ctx, word) != null;
    }

    public static void deleteLocalAudio(Context ctx, String word) {
        File f = new File(ctx.getFilesDir(), "audio_" + sanitize(word) + ".mp3");
        if (f.exists()) f.delete();
    }

    private static String sanitize(String word) {
        return word.toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
    }
}
