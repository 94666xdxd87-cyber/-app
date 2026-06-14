package com.example.myapplication.english;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.core.*;
import java.util.ArrayList;
import java.util.List;

public class QuizReviewActivity extends AppCompatActivity {

    private LinearLayout layoutReviewList;
    private TextView tvReviewTitle;
    private List<Quiz> reviewQuizList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_review);

        layoutReviewList = findViewById(R.id.layoutReviewList);
        tvReviewTitle    = findViewById(R.id.tvReviewTitle);

        Button btnAiExplain = findViewById(R.id.btnAiExplainQuick);
        if (btnAiExplain != null) {
            btnAiExplain.setOnClickListener(v -> showAiExplainDialog());
        }

        String sessionKey = getIntent().getStringExtra("QUIZ_SESSION_KEY");
        if (sessionKey != null && !sessionKey.isEmpty()) {
            List<Quiz> savedList = HistoryManager.loadQuizContent(this, sessionKey);
            if (savedList != null) reviewQuizList = savedList;
        }
        if (reviewQuizList.isEmpty()) {
            List<Quiz> stateList = AppState.getInstance().currentQuizList;
            if (stateList != null) reviewQuizList = stateList;
        }

        renderReview();
    }

    // ════════════════════════════════
    //  即時 AI 講解對話框
    // ════════════════════════════════

    private void showAiExplainDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🤖 即時 AI 單字講解");

        int p = (int) (16 * getResources().getDisplayMetrics().density);

        // ── 最外層 ScrollView：讓整個對話框內容可垂直捲動 ──
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(p, p / 2, p, p / 2);
        scrollView.addView(layout);

        // 說明文字
        TextView tvHint = new TextView(this);
        tvHint.setText("輸入想查詢的單字，多個單字請用空格或逗號分隔。\n例如：apple, run, quickly");
        tvHint.setTextSize(13f);
        tvHint.setTextColor(Color.parseColor("#757575"));
        tvHint.setPadding(0, 0, 0, p / 2);
        layout.addView(tvHint);

        // 輸入框
        EditText etWords = new EditText(this);
        etWords.setHint("輸入單字...");
        etWords.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etWords.setMinLines(2);
        etWords.setMaxLines(4);
        etWords.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        layout.addView(etWords);

        // 快速選取區塊
        if (!reviewQuizList.isEmpty()) {
            List<String> quizWords = extractAllWords();

            if (!quizWords.isEmpty()) {
                // 全選 / 清除按鈕列
                LinearLayout actionRow = new LinearLayout(this);
                actionRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams arLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                arLp.setMargins(0, p, 0, 4);
                actionRow.setLayoutParams(arLp);

                TextView tvQuickLabel = new TextView(this);
                tvQuickLabel.setText("此次考題單字（" + quizWords.size() + " 個，點選填入）");
                tvQuickLabel.setTextSize(12f);
                tvQuickLabel.setTextColor(Color.parseColor("#1565C0"));
                tvQuickLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tvQuickLabel.setGravity(android.view.Gravity.CENTER_VERTICAL);
                actionRow.addView(tvQuickLabel);

                // 「全選」按鈕
                Button btnSelectAll = new Button(this);
                btnSelectAll.setText("全選");
                btnSelectAll.setTextSize(11f);
                btnSelectAll.setTextColor(Color.WHITE);
                btnSelectAll.setBackgroundColor(Color.parseColor("#1565C0"));
                LinearLayout.LayoutParams saLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, 64);
                saLp.setMarginEnd(6);
                btnSelectAll.setLayoutParams(saLp);
                btnSelectAll.setOnClickListener(v -> {
                    StringBuilder sb = new StringBuilder();
                    for (String w : quizWords) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(w);
                    }
                    etWords.setText(sb.toString());
                    etWords.setSelection(etWords.getText().length());
                });
                actionRow.addView(btnSelectAll);

                // 「清除」按鈕
                Button btnClear = new Button(this);
                btnClear.setText("清除");
                btnClear.setTextSize(11f);
                btnClear.setTextColor(Color.WHITE);
                btnClear.setBackgroundColor(Color.parseColor("#757575"));
                btnClear.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, 64));
                btnClear.setOnClickListener(v -> etWords.setText(""));
                actionRow.addView(btnClear);

                layout.addView(actionRow);

                // ── chip 區域：移除數量上限，全部顯示，每列 4 個 ──
                LinearLayout verticalChips = new LinearLayout(this);
                verticalChips.setOrientation(LinearLayout.VERTICAL);
                verticalChips.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

                LinearLayout rowLayout = null;
                for (int i = 0; i < quizWords.size(); i++) {
                    if (i % 4 == 0) {
                        rowLayout = new LinearLayout(this);
                        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        rowLp.setMargins(0, 4, 0, 0);
                        rowLayout.setLayoutParams(rowLp);
                        verticalChips.addView(rowLayout);
                    }

                    final String word = quizWords.get(i);
                    Button chip = new Button(this);
                    chip.setText(word);
                    chip.setTextSize(11f);
                    chip.setTextColor(Color.WHITE);
                    chip.setBackgroundColor(Color.parseColor("#42A5F5"));
                    chip.setAllCaps(false);
                    LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(0, 68, 1f);
                    chipLp.setMargins(0, 0, 4, 0);
                    chip.setLayoutParams(chipLp);
                    chip.setOnClickListener(v -> {
                        String current = etWords.getText().toString().trim();
                        if (current.isEmpty()) {
                            etWords.setText(word);
                        } else if (!current.contains(word)) {
                            etWords.setText(current + ", " + word);
                        }
                        etWords.setSelection(etWords.getText().length());
                    });
                    if (rowLayout != null) rowLayout.addView(chip);
                }
                layout.addView(verticalChips);
            }
        }

        builder.setView(scrollView);

        builder.setPositiveButton("開始講解", (dialog, which) -> {
            String input = etWords.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "請輸入至少一個單字", Toast.LENGTH_SHORT).show(); return;
            }
            if (!AppState.getInstance().hasApiKey()) {
                Toast.makeText(this, "請先設定 API Key！", Toast.LENGTH_SHORT).show(); return;
            }
            String[] parts = input.split("[,\\s]+");
            ArrayList<String> words = new ArrayList<>();
            for (String w : parts) { w = w.trim(); if (!w.isEmpty()) words.add(w); }
            if (words.isEmpty()) {
                Toast.makeText(this, "請輸入有效的單字", Toast.LENGTH_SHORT).show(); return;
            }
            Intent intent = new Intent(this, WordExplainActivity.class);
            intent.putStringArrayListExtra("WORDS_TO_EXPLAIN", words);
            startActivity(intent);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // ════════════════════════════════
    //  從題目選項中抽取所有英文單字
    // ════════════════════════════════

    private List<String> extractAllWords() {
        java.util.Set<String> wordSet = new java.util.LinkedHashSet<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\\([A-D]\\)\\s*([a-zA-Z\\-]+)", java.util.regex.Pattern.MULTILINE);
        for (Quiz q : reviewQuizList) {
            java.util.regex.Matcher m = pattern.matcher(q.questionBody);
            while (m.find()) wordSet.add(m.group(1).toLowerCase().trim());
        }
        return new ArrayList<>(wordSet);
    }

    // ════════════════════════════════
    //  渲染題目列表
    // ════════════════════════════════

    private void renderReview() {
        if (layoutReviewList == null) return;
        layoutReviewList.removeAllViews();

        if (reviewQuizList == null || reviewQuizList.isEmpty()) {
            if (tvReviewTitle != null) tvReviewTitle.setText("無題目紀錄");
            return;
        }

        if (tvReviewTitle != null) {
            tvReviewTitle.setText("題目解析（共 " + reviewQuizList.size() + " 題）");
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < reviewQuizList.size(); i++) {
            Quiz q = reviewQuizList.get(i);
            View card = inflater.inflate(R.layout.item_quiz_review, layoutReviewList, false);

            ((TextView) card.findViewById(R.id.tvReviewNo)).setText("第 " + (i + 1) + " 題");
            ((TextView) card.findViewById(R.id.tvReviewBody)).setText(q.questionBody);

            String uAns = (q.userAnswer == null || q.userAnswer.isEmpty()) ? "未作答" : q.userAnswer;
            TextView tvUserAns = card.findViewById(R.id.tvUserAnswer);
            tvUserAns.setText("你的答案: " + uAns);

            ((TextView) card.findViewById(R.id.tvCorrectAnswer)).setText("正確答案: " + q.correctAnswer);
            ((TextView) card.findViewById(R.id.tvExplanation)).setText("解析:\n" + q.explanation);

            if (q.correctAnswer.equals(q.userAnswer)) tvUserAns.setTextColor(Color.parseColor("#2E7D32"));
            else tvUserAns.setTextColor(Color.RED);

            layoutReviewList.addView(card);
        }
    }

    public void onBackClick(View v) { finish(); }
}
