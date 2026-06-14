package com.example.myapplication.essay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import java.util.List;

/**
 * 考古題詳細頁面。
 * 顯示完整題目內容（可滑動），提供「開始作答」與「查看作答紀錄」按鈕。
 */
public class QuestionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_QUESTION_ID = "question_id";

    private List<EssayRecord> answerRecords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        String questionId = getIntent().getStringExtra(EXTRA_QUESTION_ID);
        if (questionId == null) { finish(); return; }

        PastExamQuestion q = PastExamManager.findQuestionById(this, questionId);
        if (q == null) { finish(); return; }

        // 返回按鈕
        View btnBack = findViewById(R.id.btnQuestionDetailBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // 標題列
        TextView tvTitle = findViewById(R.id.tvQuestionDetailTitle);
        if (tvTitle != null)
            tvTitle.setText(q.year + "  " + q.subject);

        // 資訊標籤列
        TextView tvExamType = findViewById(R.id.tvQuestionMetaExamType);
        if (tvExamType != null) tvExamType.setText(q.examType);

        TextView tvYear = findViewById(R.id.tvQuestionMetaYear);
        if (tvYear != null) tvYear.setText(q.year);

        TextView tvSubject = findViewById(R.id.tvQuestionMetaSubject);
        if (tvSubject != null) tvSubject.setText(q.subject);

        TextView tvScore = findViewById(R.id.tvQuestionMetaScore);
        if (tvScore != null) tvScore.setText(q.score + " 分");

        // 完整題目內容
        TextView tvFullText = findViewById(R.id.tvQuestionFullText);
        if (tvFullText != null) tvFullText.setText(q.question);

        // 作答次數 + 查看紀錄按鈕
        answerRecords = PastExamManager.loadRecordsForQuestion(this, questionId);
        TextView tvAnsweredCount = findViewById(R.id.tvQuestionAnsweredCount);
        Button btnViewRecords = findViewById(R.id.btnViewAnswerRecords);

        if (!answerRecords.isEmpty()) {
            if (tvAnsweredCount != null) {
                tvAnsweredCount.setText("📝 已作答 " + answerRecords.size() + " 次");
                tvAnsweredCount.setVisibility(View.VISIBLE);
            }
            if (btnViewRecords != null) {
                btnViewRecords.setVisibility(View.VISIBLE);
                btnViewRecords.setOnClickListener(v -> openAnswerRecords());
            }
        }

        // 開始作答按鈕
        Button btnStart = findViewById(R.id.btnStartAnswer);
        if (btnStart != null) {
            btnStart.setOnClickListener(v -> {
                Intent intent = new Intent(this, EssayEditActivity.class);
                intent.putExtra(EssayEditActivity.EXTRA_SOURCE_QUESTION, questionId);
                startActivity(intent);
            });
        }
    }

    /** 作答紀錄按鈕點擊：1 筆直接跳轉，多筆先選擇 */
    private void openAnswerRecords() {
        if (answerRecords.size() == 1) {
            // 只有 1 筆，直接開啟
            openDetail(answerRecords.get(0));
        } else {
            // 多筆：顯示對話框選擇
            String[] labels = new String[answerRecords.size()];
            for (int i = 0; i < answerRecords.size(); i++) {
                EssayRecord r = answerRecords.get(i);
                String status = r.hasAiReply() ? "✅ 已評分" :
                                r.hasUserAnswer() ? "⏳ 待評分" : "📝 草稿";
                labels[i] = (i + 1) + ".　" + r.timestamp + "　" + status;
            }
            new AlertDialog.Builder(this)
                    .setTitle("📋 選擇作答紀錄")
                    .setItems(labels, (dialog, which) -> openDetail(answerRecords.get(which)))
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    private void openDetail(EssayRecord record) {
        Intent intent = new Intent(this, EssayDetailActivity.class);
        intent.putExtra(EssayDetailActivity.EXTRA_RECORD_ID, record.id);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 從作答頁/詳情頁返回時，重新載入紀錄並同步 UI + listener
        String questionId = getIntent().getStringExtra(EXTRA_QUESTION_ID);
        if (questionId == null) return;
        answerRecords = PastExamManager.loadRecordsForQuestion(this, questionId);
        TextView tvAnsweredCount = findViewById(R.id.tvQuestionAnsweredCount);
        Button btnViewRecords = findViewById(R.id.btnViewAnswerRecords);
        if (!answerRecords.isEmpty()) {
            if (tvAnsweredCount != null) {
                tvAnsweredCount.setText("📝 已作答 " + answerRecords.size() + " 次");
                tvAnsweredCount.setVisibility(View.VISIBLE);
            }
            if (btnViewRecords != null) {
                btnViewRecords.setVisibility(View.VISIBLE);
                // 每次 onResume 都重新綁定，確保 answerRecords 是最新的
                btnViewRecords.setOnClickListener(v -> openAnswerRecords());
            }
        } else {
            if (tvAnsweredCount != null) tvAnsweredCount.setVisibility(View.GONE);
            if (btnViewRecords != null)  btnViewRecords.setVisibility(View.GONE);
        }
    }
}
