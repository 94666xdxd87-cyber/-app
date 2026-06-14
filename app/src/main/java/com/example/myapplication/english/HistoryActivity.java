package com.example.myapplication.english;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.core.*;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private LinearLayout layoutHistory;
    private TextView tvHistorySummary;
    private Button btnTabAll, btnTabPractice, btnTabExam;
    private String currentCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        layoutHistory    = findViewById(R.id.layoutHistory);
        tvHistorySummary = findViewById(R.id.tvHistorySummary);
        btnTabAll        = findViewById(R.id.btnTabAll);
        btnTabPractice   = findViewById(R.id.btnTabPractice);
        btnTabExam       = findViewById(R.id.btnTabExam);

        btnTabAll.setOnClickListener(v      -> switchTab(null));
        btnTabPractice.setOnClickListener(v -> switchTab("PRACTICE"));
        btnTabExam.setOnClickListener(v     -> switchTab("EXAM"));

        findViewById(R.id.btnClearHistory).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("清除歷史")
                        .setMessage("確定要清除所有歷史成績嗎？")
                        .setPositiveButton("清除", (d, w) -> { HistoryManager.clear(this); renderHistory(); })
                        .setNegativeButton("取消", null).show());

        findViewById(R.id.btnBackFromHistory).setOnClickListener(v -> finish());

        selectTab(btnTabAll);
        renderHistory();
    }

    private void switchTab(String category) {
        currentCategory = category;
        selectTab(category == null ? btnTabAll
                : "PRACTICE".equals(category) ? btnTabPractice : btnTabExam);
        renderHistory();
    }

    private void selectTab(Button active) {
        for (Button btn : new Button[]{btnTabAll, btnTabPractice, btnTabExam}) {
            btn.setBackgroundColor(btn == active ? Color.parseColor("#E3F2FD") : Color.WHITE);
            btn.setTextColor(btn == active ? Color.parseColor("#1565C0") : Color.parseColor("#757575"));
            btn.setTypeface(null, btn == active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void renderHistory() {
        layoutHistory.removeAllViews();
        List<HistoryRecord> records = HistoryManager.loadByCategory(this, currentCategory);

        if (records.isEmpty()) {
            String label = currentCategory == null ? "歷史紀錄"
                    : "EXAM".equals(currentCategory) ? "模擬考紀錄" : "練習紀錄";
            tvHistorySummary.setText("尚無" + label);
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("完成測驗後即會顯示記錄");
            tvEmpty.setTextSize(14f);
            tvEmpty.setTextColor(Color.parseColor("#9E9E9E"));
            tvEmpty.setPadding(12, 40, 12, 12);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            layoutHistory.addView(tvEmpty);
            return;
        }

        int total = records.size();
        double sumRate = 0, bestRate = 0;
        for (HistoryRecord r : records) {
            sumRate += r.getRate();
            if (r.getRate() > bestRate) bestRate = r.getRate();
        }
        tvHistorySummary.setText(String.format(
                "共 %d 次　平均 %.1f%%　最高 %.1f%%", total, sumRate / total, bestRate));

        for (int i = records.size() - 1; i >= 0; i--) {
            layoutHistory.addView(buildHistoryCard(records.get(i)));
        }
    }

    private View buildHistoryCard(HistoryRecord r) {
        boolean isExam = "EXAM".equals(r.category);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(40, 30, 40, 30);
        card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, 20);
        card.setLayoutParams(cardLp);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizReviewActivity.class);
            intent.putExtra("QUIZ_SESSION_KEY", r.quizSessionKey);
            startActivity(intent);
        });

        TextView tvHeader = new TextView(this);
        tvHeader.setText(r.timestamp + " · " + r.modeName);
        tvHeader.setTextSize(12f);
        tvHeader.setTextColor(Color.GRAY);
        card.addView(tvHeader);

        TextView tvScore = new TextView(this);
        String scoreText = r.score + " / " + r.total + " (" + String.format("%.1f", r.getRate()) + "%)";
        tvScore.setText(scoreText);
        tvScore.setTextSize(20f);
        tvScore.setPadding(0, 10, 0, 10);
        tvScore.setTypeface(null, android.graphics.Typeface.BOLD);
        tvScore.setTextColor(r.getRate() >= 60 ? Color.parseColor("#2E7D32") : Color.RED);
        card.addView(tvScore);

        TextView tvHint = new TextView(this);
        tvHint.setText("🔍 點擊整列查看題目詳解");
        tvHint.setTextSize(11f);
        tvHint.setTextColor(Color.LTGRAY);
        card.addView(tvHint);

        if (isExam && r.examSessionKey != null && !r.examSessionKey.isEmpty()) {
            Button btnViewRank = new Button(this);
            btnViewRank.setText("🏆 查看本次排名");
            btnViewRank.setTextSize(13f);
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            btnLp.setMargins(0, 15, 0, 0);
            btnViewRank.setLayoutParams(btnLp);
            btnViewRank.setOnClickListener(v -> {
                Intent intent = new Intent(this, RankingActivity.class);
                intent.putExtra("SESSION_KEY", r.examSessionKey);
                intent.putExtra("OFFLINE_MODE", true);
                startActivity(intent);
            });
            card.addView(btnViewRank);
        }
        return card;
    }
}
