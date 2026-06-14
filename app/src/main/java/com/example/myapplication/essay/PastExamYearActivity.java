package com.example.myapplication.essay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import java.util.List;

/**
 * 年份選擇頁面。
 * 根據傳入的考試類型，列出所有可用年份按鈕，點選後進入該年份的考題列表。
 */
public class PastExamYearActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_year);

        String examType = getIntent().getStringExtra(ExamCategoryActivity.EXTRA_EXAM_TYPE);
        if (examType == null) examType = "高考";

        // 設定標題
        TextView tvTitle = findViewById(R.id.tvExamYearTitle);
        if (tvTitle != null) tvTitle.setText("📅 " + examType + " — 選擇年份");

        // 返回按鈕
        View btnBack = findViewById(R.id.btnExamYearBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // 動態建立年份按鈕
        LinearLayout yearContainer = findViewById(R.id.yearButtonContainer);
        if (yearContainer != null) {
            buildYearButtons(yearContainer, examType);
        }
    }

    private void buildYearButtons(LinearLayout container, String examType) {
        List<String> years = PastExamManager.loadYearsByExamType(this, examType);

        if (years.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("目前尚未匯入「" + examType + "」的考題資料");
            tvEmpty.setTextSize(15f);
            tvEmpty.setTextColor(0xFF9E9E9E);
            tvEmpty.setPadding(dp(24), dp(48), dp(24), dp(0));
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            container.addView(tvEmpty);
            return;
        }

        int margin = dp(10);
        for (String year : years) {
            Button btn = new Button(this);
            btn.setText(year);
            btn.setTextSize(16f);
            btn.setTextColor(0xFFFFFFFF);
            btn.setBackgroundResource(R.drawable.year_button_bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52));
            lp.setMargins(dp(16), margin, dp(16), 0);
            btn.setLayoutParams(lp);

            final String finalExamType = examType;
            btn.setOnClickListener(v -> openPastExam(finalExamType, year));
            container.addView(btn);
        }
    }

    private void openPastExam(String examType, String year) {
        Intent intent = new Intent(this, PastExamActivity.class);
        intent.putExtra(PastExamActivity.EXTRA_EXAM_TYPE, examType);
        intent.putExtra(PastExamActivity.EXTRA_YEAR, year);
        startActivity(intent);
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }
}
