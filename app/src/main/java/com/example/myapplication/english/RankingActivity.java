package com.example.myapplication.english;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.core.*;
import java.util.ArrayList;
import java.util.List;

public class RankingActivity extends AppCompatActivity {

    private LinearLayout layoutRanking;
    private TextView tvRankingTitle, tvStatus;
    private Button btnRefresh, btnBackFromRank;
    private ProgressBar progressBar;

    private String examId;
    private String sessionKey;
    private String myName;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int POLL_INTERVAL_MS = 5000;
    private boolean isPolling = false;
    private boolean isOfflineMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        examId        = getIntent().getStringExtra("EXAM_ID");
        sessionKey    = getIntent().getStringExtra("SESSION_KEY");
        isOfflineMode = getIntent().getBooleanExtra("OFFLINE_MODE", false);
        myName        = AppState.getInstance().userName;

        tvRankingTitle  = findViewById(R.id.tvRankingTitle);
        tvStatus        = findViewById(R.id.tvStatus);
        layoutRanking   = findViewById(R.id.layoutRanking);
        progressBar     = findViewById(R.id.progressBar);
        btnRefresh      = findViewById(R.id.btnRefresh);
        btnBackFromRank = findViewById(R.id.btnBackFromRank);

        btnRefresh.setOnClickListener(v -> {
            if (isOfflineMode) loadOfflineRanking(); else fetchRanking();
        });
        btnBackFromRank.setOnClickListener(v -> finish());

        if (isOfflineMode) {
            tvRankingTitle.setText("🏆 模擬考排名（歷史）");
            btnRefresh.setVisibility(View.GONE);
            loadOfflineRanking();
        } else {
            isPolling = true;
            fetchRanking();
        }
    }

    private void loadOfflineRanking() {
        String data = HistoryManager.loadRanking(this, sessionKey);
        if (data == null || data.isEmpty()) { tvStatus.setText("❌ 無本地排名記錄"); return; }
        tvStatus.setText("📂 本地儲存的排名記錄");
        renderRanking(data, false);
    }

    private void fetchRanking() {
        tvStatus.setText("📡 從 Server 下載排名中...");
        progressBar.setVisibility(View.VISIBLE);
        btnRefresh.setEnabled(false);

        ExamClient.getInstance().sendCommand("GET_RANKING|" + examId, result ->
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRefresh.setEnabled(true);

                    if (result == null || result.startsWith("ERR")) {
                        tvStatus.setText("❌ 連線異常：" + result);
                        scheduleNextPoll(); return;
                    }
                    if ("RANKING_WAIT".equals(result)) {
                        tvStatus.setText("⏳ 等待所有考生交卷或時間結束...");
                        scheduleNextPoll(); return;
                    }
                    if (result.startsWith("RANKING|")) {
                        isPolling = false;
                        renderRanking(result.substring(8), true);
                    }
                }));
    }

    private void scheduleNextPoll() {
        if (isPolling) handler.postDelayed(this::fetchRanking, POLL_INTERVAL_MS);
    }

    private void renderRanking(String data, boolean saveLocally) {
        tvStatus.setText("✅ 成績已公布");
        layoutRanking.removeAllViews();

        if (saveLocally && sessionKey != null && !sessionKey.isEmpty()) {
            HistoryManager.saveRanking(this, sessionKey, data);
        }

        String[] rows = data.split(";");
        List<String[]> records = new ArrayList<>();
        for (String row : rows) {
            if (row.trim().isEmpty()) continue;
            String[] f = row.trim().split(",");
            if (f.length >= 3) records.add(f);
        }

        if (records.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("尚無成績資料");
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, 40, 0, 0);
            layoutRanking.addView(tv); return;
        }

        for (int i = 0; i < records.size(); i++)
            layoutRanking.addView(buildRankCard(i + 1, records.get(i)));
    }

    private View buildRankCard(int rank, String[] fields) {
        String name  = fields[0];
        String score = fields[1];
        String total = fields.length >= 3 ? fields[2] : "?";
        double rate  = 0;
        if (fields.length >= 4) {
            try { rate = Double.parseDouble(fields[3]); } catch (Exception ignored) {}
        } else {
            try { rate = Integer.parseInt(score) * 100.0 / Integer.parseInt(total); }
            catch (Exception ignored) {}
        }
        boolean isMe = name.equals(myName);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(20, 16, 20, 16);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundColor(isMe ? Color.parseColor("#E3F2FD") : Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 6);
        card.setLayoutParams(lp);

        TextView tvRank = new TextView(this);
        tvRank.setText(rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "#" + rank);
        tvRank.setTextSize(rank <= 3 ? 22f : 16f);
        tvRank.setTypeface(null, Typeface.BOLD);
        tvRank.setLayoutParams(new LinearLayout.LayoutParams(80, LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(tvRank);

        TextView tvName = new TextView(this);
        tvName.setText(isMe ? name + "（我）" : name);
        tvName.setTextSize(15f);
        tvName.setTypeface(null, isMe ? Typeface.BOLD : Typeface.NORMAL);
        tvName.setTextColor(isMe ? Color.parseColor("#1565C0") : Color.parseColor("#212121"));
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(tvName);

        TextView tvScore = new TextView(this);
        tvScore.setText(score + "/" + total + "\n" + String.format("%.1f%%", rate));
        tvScore.setTextSize(14f);
        tvScore.setTextColor(rate >= 80 ? Color.parseColor("#2E7D32")
                : rate >= 60 ? Color.parseColor("#E65100") : Color.parseColor("#C62828"));
        tvScore.setTypeface(null, Typeface.BOLD);
        tvScore.setGravity(Gravity.END);
        card.addView(tvScore);

        return card;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isPolling = false;
        handler.removeCallbacksAndMessages(null);
    }
}
