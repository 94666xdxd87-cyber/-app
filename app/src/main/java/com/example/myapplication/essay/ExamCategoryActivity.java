package com.example.myapplication.essay;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;

/**
 * 考試類別選擇頁面。
 * 提供 4 個考試類型按鈕：高考、普考、地方特考三等、地方特考四等。
 * 進入時自動觸發考古題匯入（importIfNeeded），匯入完成前按鈕不可點選。
 */
public class ExamCategoryActivity extends AppCompatActivity {

    public static final String EXTRA_EXAM_TYPE = "exam_type";

    public static final String TYPE_SENIOR = "高考";
    public static final String TYPE_JUNIOR = "普考";
    public static final String TYPE_LOCAL3 = "地方特考三等";
    public static final String TYPE_LOCAL4 = "地方特考四等";

    private LinearLayout layoutButtons;
    private TextView     tvLoadingHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_category);

        View btnBack = findViewById(R.id.btnExamCategoryBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        layoutButtons = findViewById(R.id.layoutCategoryButtons);
        tvLoadingHint = findViewById(R.id.tvCategoryLoadingHint);

        // 先鎖住按鈕、顯示 loading，等匯入完成再解鎖
        setButtonsEnabled(false);
        if (tvLoadingHint != null) {
            tvLoadingHint.setVisibility(View.VISIBLE);
            tvLoadingHint.setText("⏳ 資料載入中…");
        }

        PastExamImporter.importIfNeeded(this,
            (total, skipped) -> new Handler(Looper.getMainLooper()).post(() -> {
                setButtonsEnabled(true);
                if (tvLoadingHint != null) tvLoadingHint.setVisibility(View.GONE);
                if (total > 0)
                    Toast.makeText(this, "✅ 匯入完成，共 " + total + " 道題", Toast.LENGTH_SHORT).show();
            }),
            msg -> new Handler(Looper.getMainLooper()).post(() -> {
                setButtonsEnabled(true);
                if (tvLoadingHint != null) {
                    tvLoadingHint.setText("⚠️ 資料載入失敗，請重試");
                }
                Toast.makeText(this, "❌ 匯入失敗：" + msg, Toast.LENGTH_LONG).show();
            })
        );

        setupCategoryButton(R.id.btnCategorySenior, TYPE_SENIOR);
        setupCategoryButton(R.id.btnCategoryJunior, TYPE_JUNIOR);
        setupCategoryButton(R.id.btnCategoryLocal3, TYPE_LOCAL3);
        setupCategoryButton(R.id.btnCategoryLocal4, TYPE_LOCAL4);
    }

    private void setupCategoryButton(int buttonId, String examType) {
        Button btn = findViewById(buttonId);
        if (btn != null) {
            btn.setOnClickListener(v -> openYearSelection(examType));
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        if (layoutButtons == null) return;
        for (int i = 0; i < layoutButtons.getChildCount(); i++) {
            layoutButtons.getChildAt(i).setEnabled(enabled);
            layoutButtons.getChildAt(i).setAlpha(enabled ? 1f : 0.5f);
        }
    }

    private void openYearSelection(String examType) {
        Intent intent = new Intent(this, PastExamYearActivity.class);
        intent.putExtra(EXTRA_EXAM_TYPE, examType);
        startActivity(intent);
    }
}
