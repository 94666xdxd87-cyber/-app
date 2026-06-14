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

public class QuizActivity extends AppCompatActivity {

    private AppState state;
    private List<Quiz> quizList;
    private int currentIndex = 0;

    private TextView tvProgress, tvAnsweredCount, tvCategory, tvQuestion, tvExamTimer;
    private Button btnA, btnB, btnC, btnD, btnPrev, btnNext;

    private boolean isOnlineExam = false;
    private String  examId       = "";

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private boolean timerRunning = false;

    private static final int COLOR_SELECTED = Color.parseColor("#1565C0");
    private static final int COLOR_DEFAULT  = Color.parseColor("#2196F3");
    private static final String[] TYPE_NAMES = {"", "名詞", "動詞", "副詞", "形容詞"};
    private static final long SYNC_INTERVAL_MS = 60_000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        state        = AppState.getInstance();
        quizList     = state.currentQuizList;
        isOnlineExam = getIntent().getBooleanExtra("IS_ONLINE_EXAM", false);
        examId       = getIntent().getStringExtra("EXAM_ID") != null
                         ? getIntent().getStringExtra("EXAM_ID") : "";

        tvProgress      = findViewById(R.id.tvProgress);
        tvAnsweredCount = findViewById(R.id.tvAnsweredCount);
        tvCategory      = findViewById(R.id.tvCategory);
        tvQuestion      = findViewById(R.id.tvQuestion);
        tvExamTimer     = findViewById(R.id.tvExamTimer);
        btnA = findViewById(R.id.btnA); btnB = findViewById(R.id.btnB);
        btnC = findViewById(R.id.btnC); btnD = findViewById(R.id.btnD);
        btnPrev = findViewById(R.id.btnPrev); btnNext = findViewById(R.id.btnNext);

        btnA.setOnClickListener(v -> onSelectOption("A"));
        btnB.setOnClickListener(v -> onSelectOption("B"));
        btnC.setOnClickListener(v -> onSelectOption("C"));
        btnD.setOnClickListener(v -> onSelectOption("D"));
        btnPrev.setOnClickListener(v -> navigate(-1));
        btnNext.setOnClickListener(v -> navigate(1));

        if (isOnlineExam && state.examEndTimestampMs > 0) {
            tvExamTimer.setVisibility(View.VISIBLE);
            startServerSyncedTimer();
        }

        renderQuestion();
    }

    private void startServerSyncedTimer() {
        timerRunning = true;
        timerHandler.post(timerTick);
        timerHandler.postDelayed(serverSyncTask, SYNC_INTERVAL_MS);
    }

    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            if (!timerRunning) return;
            long remaining = state.examEndTimestampMs - System.currentTimeMillis();
            if (remaining <= 0) {
                tvExamTimer.setText("⏱ 00:00");
                Toast.makeText(QuizActivity.this, "⏰ 考試時間到，自動交卷！", Toast.LENGTH_LONG).show();
                submitAndGoResult();
                return;
            }
            long sec = remaining / 1000;
            tvExamTimer.setText(String.format("⏱ %02d:%02d", sec / 60, sec % 60));
            tvExamTimer.setTextColor(sec <= 300
                    ? Color.parseColor("#D32F2F") : Color.parseColor("#1B5E20"));
            timerHandler.postDelayed(this, 1000);
        }
    };

    private final Runnable serverSyncTask = new Runnable() {
        @Override
        public void run() {
            if (!timerRunning) return;
            ExamClient.getInstance().sendCommand(
                    "GET_EXAM_STATUS|" + examId, result -> {
                        if (result == null || !result.startsWith("EXAM_STATUS")) return;
                        String[] p = result.split("\\|");
                        if (p.length < 5) return;
                        long remainSec  = Long.parseLong(p[1]);
                        int isPublished = Integer.parseInt(p[4]);
                        state.examEndTimestampMs = System.currentTimeMillis() + remainSec * 1000L;
                        if (isPublished == 1 && timerRunning) {
                            runOnUiThread(() -> {
                                Toast.makeText(QuizActivity.this,
                                        "🎉 所有考生已交卷，系統自動收卷！", Toast.LENGTH_LONG).show();
                                submitAndGoResult();
                            });
                        }
                    });
            timerHandler.postDelayed(this, SYNC_INTERVAL_MS);
        }
    };

    private void renderQuestion() {
        Quiz q = quizList.get(currentIndex);
        tvProgress.setText("第 " + (currentIndex + 1) + " 題 / " + quizList.size() + " 題");
        tvAnsweredCount.setText("已作答：" + countAnswered() + "/" + quizList.size());
        int cat = q.categoryType;
        tvCategory.setText(cat > 0 && cat <= 4 ? TYPE_NAMES[cat] : "");
        tvQuestion.setText(q.questionBody);
        resetOptionButtons();
        if (!q.userAnswer.isEmpty()) highlightSelected(getButton(q.userAnswer));
        btnPrev.setEnabled(currentIndex > 0);
        btnNext.setText(currentIndex == quizList.size() - 1 ? "交卷 ✅" : "下一題 →");
    }

    private void onSelectOption(String opt) {
        quizList.get(currentIndex).userAnswer = opt;
        resetOptionButtons();
        highlightSelected(getButton(opt));
        tvAnsweredCount.setText("已作答：" + countAnswered() + "/" + quizList.size());
    }

    private void navigate(int dir) {
        if (dir > 0 && currentIndex == quizList.size() - 1) { confirmSubmit(); return; }
        int next = currentIndex + dir;
        if (next < 0 || next >= quizList.size()) return;
        currentIndex = next;
        renderQuestion();
    }

    private void confirmSubmit() {
        int unanswered = quizList.size() - countAnswered();
        new AlertDialog.Builder(this)
                .setTitle("交卷確認")
                .setMessage(unanswered > 0
                        ? "尚有 " + unanswered + " 題未作答，確定交卷？"
                        : "確定交卷？")
                .setPositiveButton("確定交卷", (d, w) -> submitAndGoResult())
                .setNegativeButton("繼續作答", null).show();
    }

    private void submitAndGoResult() {
        timerRunning = false;
        timerHandler.removeCallbacksAndMessages(null);
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("IS_ONLINE_EXAM", isOnlineExam);
        intent.putExtra("EXAM_ID", examId);
        startActivity(intent);
        finish();
    }

    private void resetOptionButtons() {
        for (Button b : new Button[]{btnA, btnB, btnC, btnD}) {
            b.setBackgroundColor(Color.TRANSPARENT);
            b.setTextColor(COLOR_DEFAULT);
        }
    }

    private void highlightSelected(Button btn) {
        btn.setBackgroundColor(COLOR_SELECTED);
        btn.setTextColor(Color.WHITE);
    }

    private Button getButton(String opt) {
        switch (opt) {
            case "A": return btnA; case "B": return btnB; case "C": return btnC; default: return btnD;
        }
    }

    private int countAnswered() {
        int c = 0;
        for (Quiz q : quizList) if (!q.userAnswer.isEmpty()) c++;
        return c;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerRunning = false;
        timerHandler.removeCallbacksAndMessages(null);
    }
}
