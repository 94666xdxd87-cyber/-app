package com.example.myapplication.english;

import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.core.*;
import java.util.*;

public class WordExplainActivity extends AppCompatActivity {

    private LinearLayout layoutExplainList, layoutSearchBar, layoutDownloadBar;
    private TextView tvStatus, tvDownloadStatus;
    private ProgressBar progressBar;
    private Button btnBack, btnDownloadAll;
    private EditText etSearch;

    private List<WordExplainRecord> allRecords = new ArrayList<>();
    private boolean isHistoryMode = false;
    private String currentQuery = "";

    private MediaPlayer mediaPlayer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_explain);

        layoutExplainList = findViewById(R.id.layoutExplainList);
        layoutSearchBar   = findViewById(R.id.layoutSearchBar);
        layoutDownloadBar = findViewById(R.id.layoutDownloadBar);
        tvStatus          = findViewById(R.id.tvExplainStatus);
        tvDownloadStatus  = findViewById(R.id.tvDownloadStatus);
        progressBar       = findViewById(R.id.pbExplain);
        btnBack           = findViewById(R.id.btnBackFromExplain);
        btnDownloadAll    = findViewById(R.id.btnDownloadAll);
        etSearch          = findViewById(R.id.etSearch);

        btnBack.setOnClickListener(v -> finish());
        btnDownloadAll.setOnClickListener(v -> onDownloadAll());

        ArrayList<String> wordsToExplain = getIntent().getStringArrayListExtra("WORDS_TO_EXPLAIN");
        isHistoryMode = (wordsToExplain == null || wordsToExplain.isEmpty());

        if (isHistoryMode) {
            layoutSearchBar.setVisibility(View.VISIBLE);
            setupSearch();
            loadHistory();
        } else {
            layoutSearchBar.setVisibility(View.GONE);
            layoutDownloadBar.setVisibility(View.GONE);
            tvStatus.setText("正在請 AI 講解 " + wordsToExplain.size() + " 個單字...");
            progressBar.setVisibility(View.VISIBLE);
            callAiExplain(wordsToExplain);
        }
    }

    private void onDownloadAll() {
        List<WordExplainRecord> noAudio = WordExplainManager.loadWithoutAudio(this);
        if (noAudio.isEmpty()) { Toast.makeText(this, "✅ 所有單字已有音檔！", Toast.LENGTH_SHORT).show(); return; }

        btnDownloadAll.setEnabled(false);
        tvDownloadStatus.setText("📥 下載中 (0/" + noAudio.size() + ")...");

        AudioService.downloadBatchAsync(this, noAudio, new AudioService.BatchCallback() {
            @Override
            public void onProgress(int done, int total, String currentWord) {
                mainHandler.post(() ->
                    tvDownloadStatus.setText("📥 下載中 (" + done + "/" + total + ")：" + currentWord));
            }

            @Override
            public void onComplete(int success, int noAudioCount, int error) {
                mainHandler.post(() -> {
                    btnDownloadAll.setEnabled(true);
                    String msg = "✅ 完成：下載 " + success + " 個";
                    if (noAudioCount > 0) msg += "，" + noAudioCount + " 個無音檔資料";
                    if (error > 0) msg += "，" + error + " 個失敗";
                    tvDownloadStatus.setText(msg);
                    allRecords = WordExplainManager.loadAll(WordExplainActivity.this);
                    Collections.reverse(allRecords);
                    renderFiltered();
                    refreshDownloadBar();
                });
            }
        });
    }

    private void refreshDownloadBar() {
        List<WordExplainRecord> noAudio = WordExplainManager.loadWithoutAudio(this);
        if (noAudio.isEmpty()) {
            tvDownloadStatus.setText("✅ 所有單字皆已有發音音檔");
            btnDownloadAll.setEnabled(false);
            btnDownloadAll.setText("已完成");
        } else {
            tvDownloadStatus.setText("📥 尚有 " + noAudio.size() + " 個單字未下載音檔");
            btnDownloadAll.setEnabled(true);
            btnDownloadAll.setText("🔊 下載音檔");
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                currentQuery = s.toString().trim().toLowerCase();
                renderFiltered();
            }
        });
    }

    private void renderFiltered() {
        if (currentQuery.isEmpty()) {
            tvStatus.setText("📚 AI 單字講解歷史（共 " + allRecords.size() + " 筆）");
            renderRecords(allRecords, true);
        } else {
            List<WordExplainRecord> filtered = new ArrayList<>();
            for (WordExplainRecord r : allRecords) {
                if (r.word.toLowerCase().startsWith(currentQuery)) filtered.add(r);
            }
            tvStatus.setText("📚 篩選中：" + filtered.size() + " / " + allRecords.size() + " 筆");
            if (filtered.isEmpty()) {
                layoutExplainList.removeAllViews();
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("沒有以「" + currentQuery + "」開頭的單字");
                tvEmpty.setTextSize(14f);
                tvEmpty.setTextColor(Color.parseColor("#9E9E9E"));
                tvEmpty.setGravity(android.view.Gravity.CENTER);
                tvEmpty.setPadding(0, 40, 0, 0);
                layoutExplainList.addView(tvEmpty);
            } else {
                renderRecords(filtered, true);
            }
        }
    }

    private void loadHistory() {
        allRecords = WordExplainManager.loadAll(this);
        Collections.reverse(allRecords);

        if (allRecords.isEmpty()) {
            tvStatus.setText("📚 AI 單字講解歷史（共 0 筆）");
            layoutDownloadBar.setVisibility(View.GONE);
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("尚無講解記錄。\n請至單字庫管理勾選單字後使用 AI 講解。");
            tvEmpty.setTextSize(14f);
            tvEmpty.setTextColor(Color.parseColor("#9E9E9E"));
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            tvEmpty.setPadding(0, 40, 0, 0);
            layoutExplainList.addView(tvEmpty);
            return;
        }

        layoutDownloadBar.setVisibility(View.VISIBLE);
        refreshDownloadBar();
        renderFiltered();
    }

    private void callAiExplain(List<String> words) {
        GeminiService.explainWordsAsync(words, new Handler(Looper.getMainLooper()),
                new GeminiService.Callback() {
                    @Override
                    public void onSuccess(String response) {
                        List<WordExplainRecord> records = GeminiService.parseExplainResponse(response);
                        if (records.isEmpty()) { onError("AI 回傳內容無法解析，請再試一次。"); return; }
                        WordExplainManager.saveAll(WordExplainActivity.this, records);
                        progressBar.setVisibility(View.GONE);
                        tvStatus.setText("✅ 已完成 " + records.size() + " 個單字的講解（已自動儲存）");
                        renderRecords(records, false);
                    }
                    @Override
                    public void onError(String errorMessage) {
                        progressBar.setVisibility(View.GONE);
                        tvStatus.setText("❌ 講解失敗：" + errorMessage);
                    }
                    @Override
                    public void onRetry(int attempt) {
                        tvStatus.setText("第 " + attempt + " 次重試中...");
                    }
                });
    }

    private void renderRecords(List<WordExplainRecord> records, boolean showDeleteBtn) {
        layoutExplainList.removeAllViews();
        for (WordExplainRecord r : records) layoutExplainList.addView(buildExplainCard(r, showDeleteBtn));
    }

    private View buildExplainCard(WordExplainRecord r, boolean showDeleteBtn) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 18, 20, 18);
        card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 10);
        card.setLayoutParams(lp);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvWord = new TextView(this);
        tvWord.setText(r.word);
        tvWord.setTextSize(22f);
        tvWord.setTypeface(null, Typeface.BOLD);
        tvWord.setTextColor(Color.parseColor("#1565C0"));
        tvWord.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(tvWord);

        TextView tvKK = new TextView(this);
        tvKK.setText(r.kkPhonetic);
        tvKK.setTextSize(14f);
        tvKK.setTextColor(Color.parseColor("#757575"));
        tvKK.setPadding(0, 0, 10, 0);
        headerRow.addView(tvKK);

        Button btnPlay = new Button(this);
        boolean localExists = AudioService.hasLocalAudio(this, r.word);
        if (localExists) {
            btnPlay.setText("🔊");
            btnPlay.setBackgroundColor(Color.parseColor("#1565C0"));
        } else {
            btnPlay.setText("🔇");
            btnPlay.setBackgroundColor(Color.parseColor("#BDBDBD"));
        }
        btnPlay.setTextColor(Color.WHITE);
        btnPlay.setTextSize(14f);
        btnPlay.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
        btnPlay.setOnClickListener(v -> onPlayTapped(r, btnPlay));
        headerRow.addView(btnPlay);

        card.addView(headerRow);

        TextView tvPos = new TextView(this);
        tvPos.setText("【" + r.partOfSpeech + "】");
        tvPos.setTextSize(13f);
        tvPos.setTextColor(Color.parseColor("#E65100"));
        tvPos.setPadding(0, 4, 0, 6);
        card.addView(tvPos);

        TextView tvMeaning = new TextView(this);
        tvMeaning.setText("📖 " + r.chineseMeaning);
        tvMeaning.setTextSize(16f);
        tvMeaning.setTextColor(Color.parseColor("#212121"));
        tvMeaning.setTypeface(null, Typeface.BOLD);
        tvMeaning.setPadding(0, 0, 0, 8);
        card.addView(tvMeaning);

        TextView tvExample = new TextView(this);
        tvExample.setText("✏️ " + r.exampleSentence);
        tvExample.setTextSize(13f);
        tvExample.setTextColor(Color.parseColor("#424242"));
        tvExample.setLineSpacing(4f, 1f);
        card.addView(tvExample);

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        footerLp.setMargins(0, 10, 0, 0);
        footer.setLayoutParams(footerLp);

        TextView tvTime = new TextView(this);
        tvTime.setText(r.timestamp);
        tvTime.setTextSize(11f);
        tvTime.setTextColor(Color.parseColor("#BDBDBD"));
        tvTime.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        footer.addView(tvTime);

        if (showDeleteBtn) {
            Button btnDel = new Button(this);
            btnDel.setText("刪除");
            btnDel.setTextSize(11f);
            btnDel.setTextColor(Color.WHITE);
            btnDel.setBackgroundColor(Color.parseColor("#E53935"));
            btnDel.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 64));
            btnDel.setOnClickListener(v -> {
                WordExplainManager.delete(this, r.word);
                allRecords.removeIf(rec -> rec.word.equalsIgnoreCase(r.word));
                renderFiltered();
                refreshDownloadBar();
            });
            footer.addView(btnDel);
        }

        card.addView(footer);
        return card;
    }

    private void onPlayTapped(WordExplainRecord r, Button btnPlay) {
        String localPath = AudioService.getLocalAudioPath(this, r.word);

        if (localPath != null) {
            playAudio(localPath, r.word);
        } else {
            btnPlay.setText("⏳");
            btnPlay.setEnabled(false);
            AudioService.downloadAsync(this, r.word, new AudioService.DownloadCallback() {
                @Override
                public void onSuccess(String word, String path) {
                    r.audioPath = path;
                    WordExplainManager.updateAudioPath(WordExplainActivity.this, word, path);
                    mainHandler.post(() -> {
                        btnPlay.setText("🔊");
                        btnPlay.setEnabled(true);
                        btnPlay.setBackgroundColor(Color.parseColor("#1565C0"));
                        playAudio(path, word);
                        refreshDownloadBar();
                    });
                }
                @Override
                public void onNoAudio(String word) {
                    mainHandler.post(() -> {
                        btnPlay.setText("🔇");
                        btnPlay.setEnabled(true);
                        Toast.makeText(WordExplainActivity.this, "「" + word + "」無音檔資料", Toast.LENGTH_SHORT).show();
                    });
                }
                @Override
                public void onError(String word, String reason) {
                    mainHandler.post(() -> {
                        btnPlay.setText("🔇");
                        btnPlay.setEnabled(true);
                        Toast.makeText(WordExplainActivity.this, "下載失敗：" + reason, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void playAudio(String path, String word) {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "🔊 " + word, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "播放失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
