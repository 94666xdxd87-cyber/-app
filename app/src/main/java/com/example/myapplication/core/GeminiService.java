package com.example.myapplication.core;

import android.os.Handler;
import android.util.Log;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GeminiService {

    private static final String TAG         = "GeminiService";
    private static final int    MAX_RETRIES = 3;

    public interface Callback {
        void onSuccess(String response);
        void onError(String errorMessage);
        void onRetry(int attempt);
    }

    private static String getApiKey() { return AppState.getInstance().getApiKey(); }

    private static String getApiUrl() {
        String modelId = AppState.getInstance().modelApiIds[AppState.getInstance().selectedModelIndex];
        return "https://generativelanguage.googleapis.com/v1beta/models/"
                + modelId + ":generateContent?key=" + getApiKey();
    }

    public static void callAsync(List<String> payloads, Handler mainHandler, Callback callback) {
        new Thread(() -> {
            if (!AppState.getInstance().hasApiKey()) {
                mainHandler.post(() -> callback.onError("尚未設定 API Key，請至主選單設定後再試。"));
                return;
            }
            String result = callGeminiBlocking(buildPrompt(payloads));
            mainHandler.post(() -> {
                if (result.startsWith("ERR:")) callback.onError(result.substring(4));
                else callback.onSuccess(result);
            });
        }).start();
    }

    /**
     * 一般性編寫、直接傳入整段 prompt，不經任何 buildPrompt 處理。
     * 供 Prompt C（申論題自動批改）使用。
     */
    public static void callRawAsync(String prompt, Handler mainHandler, Callback callback) {
        new Thread(() -> {
            if (!AppState.getInstance().hasApiKey()) {
                mainHandler.post(() -> callback.onError("尚未設定 API Key，請至主選單設定後再試。"));
                return;
            }
            String jsonBody = buildJsonBody(prompt);
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                final int a = attempt;
                if (attempt > 1) {
                    mainHandler.post(() -> callback.onRetry(a));
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                }
                try {
                    String body = doPost(jsonBody);
                    if (body == null || body.startsWith("ERR_HTTP:")) continue;
                    String parsed = extractTextFromJson(body);
                    if (parsed == null) continue;
                    final String finalParsed = parsed;
                    mainHandler.post(() -> callback.onSuccess(finalParsed));
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "callRawAsync 異常 (第 " + attempt + " 次): " + e.getMessage());
                }
            }
            mainHandler.post(() -> callback.onError("重試 " + MAX_RETRIES + " 次後仍然失敗，請確認 API Key 與網路狀態。"));
        }).start();
    }

    public static void explainWordsAsync(List<String> words, Handler mainHandler, Callback callback) {
        new Thread(() -> {
            if (!AppState.getInstance().hasApiKey()) {
                mainHandler.post(() -> callback.onError("尚未設定 API Key，請至主選單設定後再試。"));
                return;
            }
            if (words == null || words.isEmpty()) {
                mainHandler.post(() -> callback.onError("沒有選取任何單字。"));
                return;
            }
            String result = callGeminiBlocking(buildExplainPrompt(words));
            mainHandler.post(() -> {
                if (result.startsWith("ERR:")) callback.onError(result.substring(4));
                else callback.onSuccess(result);
            });
        }).start();
    }

    public static List<WordExplainRecord> parseExplainResponse(String response) {
        List<WordExplainRecord> list = new java.util.ArrayList<>();
        if (response == null || response.trim().isEmpty()) return list;
        String[] blocks = response.split("={5,}");
        for (String block : blocks) {
            block = block.trim();
            if (block.isEmpty()) continue;
            String[] parts = block.split("~~", -1);
            if (parts.length < 5) continue;
            try {
                String word    = parts[0].trim();
                String meaning = parts[1].trim();
                String kk      = parts[2].trim();
                String pos     = parts[3].trim();
                String example = parts[4].trim();
                if (!word.isEmpty()) {
                    list.add(new WordExplainRecord(word, meaning, kk, pos, example,
                            WordExplainManager.currentTimestamp()));
                }
            } catch (Exception e) { Log.e(TAG, "解析講解失敗: " + e.getMessage()); }
        }
        return list;
    }

    private static String callGeminiBlocking(String prompt) {
        String jsonBody = buildJsonBody(prompt);
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 1) Thread.sleep(1500);
                String body = doPost(jsonBody);
                if (body == null || body.startsWith("ERR_HTTP:")) continue;
                String parsed = extractTextFromJson(body);
                if (parsed == null) continue;
                return parsed;
            } catch (Exception e) {
                Log.e(TAG, "連線異常 (第 " + attempt + " 次): " + e.getMessage());
            }
        }
        return "ERR:重試 " + MAX_RETRIES + " 次後仍然失敗，請確認 API Key 與網路狀態。";
    }

    private static String doPost(String jsonBody) throws Exception {
        URL url = new URL(getApiUrl());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(150000);
        conn.setReadTimeout(150000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        if (code != 200) {
            InputStream es = conn.getErrorStream();
            if (es != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                return "ERR_HTTP: 代碼 " + code + " | " + sb;
            }
            return "ERR_HTTP: 代碼 " + code;
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private static String buildPrompt(List<String> allPayloads) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一個英文選擇題出題機器。請嚴格按照以下格式輸出，禁止任何開場白、說明或多餘文字。\\n\\n");
        sb.append("每一題的格式必須完全如下（三個部分用 --~~-- 分隔，題尾加 =====）：\\n");
        sb.append("正確答案字母--~~--題目敘述與四個選項--~~--中文解析\\n=====\\n\\n");
        sb.append("範例：\\nB--~~--Which word means a person who studies science?\\n");
        sb.append("(A) artist\\n(B) scientist\\n(C) musician\\n(D) farmer\\n");
        sb.append("--~~--scientist 是科學家的意思，為正確答案。\\n=====\\n\\n");
        sb.append("現在請依照以下單字組出題，每組出一題，正解必須是括號內標示的正解單字：\\n\\n");
        for (int i = 0; i < allPayloads.size(); i++)
            sb.append("[").append(i + 1).append("] ").append(allPayloads.get(i)).append("\\n");
        return sb.toString();
    }

    private static String buildExplainPrompt(List<String> words) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一個英文單字講解老師。請針對以下每個英文單字，嚴格按照格式輸出講解，禁止任何開場白、說明或多餘文字。\\n\\n");
        sb.append("每個單字的格式必須完全如下（五個部分用 ~~ 分隔，單字結尾加 =====）：\\n");
        sb.append("英文單字~~中文意思~~KK音標~~詞性（中文）~~簡單英文例句（含中文翻譯）\\n=====\\n\\n");
        sb.append("範例：\\napple~~蘋果~~[ˋæpl]~~名詞~~I eat an apple every day. 我每天吃一顆蘋果。\\n=====\\n\\n");
        sb.append("現在請講解以下單字：\\n\\n");
        for (String w : words) sb.append(w).append("\\n");
        return sb.toString();
    }

    private static String buildJsonBody(String prompt) {
        String escaped = prompt
                .replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
        return "{\"contents\":[{\"parts\":[{\"text\":\"" + escaped + "\"}]}],"
                + "\"generationConfig\":{\"maxOutputTokens\":8192,\"temperature\":0.3}}";
    }

    private static String extractTextFromJson(String body) {
        String marker = "\"text\": \"";
        int startIdx = body.indexOf(marker);
        if (startIdx == -1) { marker = "\"text\":\""; startIdx = body.indexOf(marker); }
        if (startIdx == -1) return null;
        startIdx += marker.length();
        int endIdx = -1;
        for (int i = startIdx; i < body.length(); i++) {
            if (body.charAt(i) == '"' && body.charAt(i - 1) != '\\') { endIdx = i; break; }
        }
        if (endIdx == -1) return null;
        return body.substring(startIdx, endIdx)
                .replace("\\\\", "TEMP_SLASH").replace("\\n", "\n")
                .replace("\\\"", "\"").replace("TEMP_SLASH", "\\")
                .replace("**", "").trim();
    }
}
