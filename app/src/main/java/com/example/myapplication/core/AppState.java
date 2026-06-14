package com.example.myapplication.core;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public class AppState {

    private static AppState instance;
    private static final String PREF_NAME    = "app_settings";
    private static final String PREF_API_KEY = "gemini_api_key";

    public List<String> nounPool      = new ArrayList<>();
    public List<String> verbPool      = new ArrayList<>();
    public List<String> adverbPool    = new ArrayList<>();
    public List<String> adjectivePool = new ArrayList<>();
    public boolean poolsLoaded = false;

    public WordTrie nounTrie      = new WordTrie();
    public WordTrie verbTrie      = new WordTrie();
    public WordTrie adverbTrie    = new WordTrie();
    public WordTrie adjectiveTrie = new WordTrie();

    public List<Quiz> currentQuizList = new ArrayList<>();
    public ErrorTracker errorTracker;

    public int currentMode = 1;
    public String currentModeName = "";

    public String userName = "";

    public int  examDurationSeconds     = 0;
    public long examEndTimestampMs      = 0;
    public String currentExamId         = "";
    public String currentExamSessionKey = "";

    private String apiKey = "";

    public int selectedModelIndex = 0;
    public final String[] modelDisplayNames = {
            "1. (預設) Gemini 3 Flash",
            "2. Gemini 3.1 Flash-Lite",
            "3. Gemini 2.5 Flash",
            "4. Gemini 3.1 Pro (Preview)",
            "5. Gemini 2.5 Flash-Lite"
    };

    public final String[] modelApiIds = {
            "gemini-3-flash-preview",
            "gemini-3.1-flash-lite-preview",
            "gemini-2.5-flash",
            "gemini-3.1-pro-preview",
            "gemini-2.5-flash-lite"
    };

    private AppState() {}

    public static AppState getInstance() {
        if (instance == null) instance = new AppState();
        return instance;
    }

    public void initTracker(Context context) {
        if (errorTracker == null)
            errorTracker = new ErrorTracker(context.getApplicationContext());
    }

    public void rebuildTries() {
        nounTrie      = WordTrie.fromList(nounPool);
        verbTrie      = WordTrie.fromList(verbPool);
        adverbTrie    = WordTrie.fromList(adverbPool);
        adjectiveTrie = WordTrie.fromList(adjectivePool);
    }

    public WordTrie getTrieByIndex(int idx) {
        switch (idx) {
            case 0: return nounTrie;
            case 1: return verbTrie;
            case 2: return adverbTrie;
            case 3: return adjectiveTrie;
            default: return new WordTrie();
        }
    }

    public void setPoolByIndex(int idx, List<String> words) {
        WordTrie newTrie = WordTrie.fromList(words);
        switch (idx) {
            case 0: nounPool      = words; nounTrie      = newTrie; break;
            case 1: verbPool      = words; verbTrie      = newTrie; break;
            case 2: adverbPool    = words; adverbTrie    = newTrie; break;
            case 3: adjectivePool = words; adjectiveTrie = newTrie; break;
        }
    }

    public String getApiKey() { return apiKey; }

    public void setApiKey(Context context, String key) {
        this.apiKey = key != null ? key.trim() : "";
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_API_KEY, this.apiKey).apply();
    }

    public void loadApiKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.apiKey = prefs.getString(PREF_API_KEY, "");
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public static String getModeNameFor(int mode) {
        switch (mode) {
            case 1: return "名詞";   case 2: return "副詞";
            case 3: return "形容詞"; case 4: return "動詞";
            case 5: return "自訂比例"; case 6: return "弱點搶救";
            default: return "未知";
        }
    }
}
