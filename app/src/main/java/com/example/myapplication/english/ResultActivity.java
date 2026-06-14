package com.example.myapplication.english;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.core.*;
import java.util.List;
import java.util.regex.*;

public class ResultActivity extends AppCompatActivity {

    private AppState state;
    private boolean isOnlineExam = false;
    private String examId = "";
    private String quizSessionKey = "";
    private String examSessionKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        state        = AppState.getInstance();
        List<Quiz> quizList = state.currentQuizList;
        isOnlineExam = getIntent().getBooleanExtra("IS_ONLINE_EXAM", false);
        examId       = getIntent().getStringExtra("EXAM_ID") != null
                         ? getIntent().getStringExtra("EXAM_ID") : "";

        int score = 0;
        for (Quiz q : quizList) {
            if (q.correctAnswer.equals(q.userAnswer)) score++;
        }
        double rate = score * 100.0 / quizList.size();

        TextView tvScore    = findViewById(R.id.tvScore);
        TextView tvRate     = findViewById(R.id.tvRate);
        TextView tvModeName = findViewById(R.id.tvModeName);

        tvScore.setText("🏁 " + score + " / " + quizList.size());
        tvRate.setText(String.format("得分率 %.1f%%", rate));
        tvModeName.setText(isOnlineExam ? "🏆 全國模擬考" : state.currentModeName + " 模式");

        if (rate >= 80)      tvScore.setTextColor(Color.parseColor("#2E7D32"));
        else if (rate >= 60) tvScore.setTextColor(Color.parseColor("#E65100"));
        else                 tvScore.setTextColor(Color.parseColor("#C62828"));

        LinearLayout layoutResults = findViewById(R.id.layoutResults);
        for (Quiz q : quizList) layoutResults.addView(buildResultCard(q));

        updateErrorTracker(quizList);
        state.errorTracker.save();

        if (isOnlineExam && !state.currentExamSessionKey.isEmpty()) {
            examSessionKey = state.currentExamSessionKey;
            quizSessionKey = examSessionKey;
        } else {
            quizSessionKey = HistoryManager.currentSessionKey(state.currentModeName);
            examSessionKey = "";
        }

        HistoryManager.saveQuizContent(this, quizSessionKey, quizList);

        String category = isOnlineExam ? "EXAM" : "PRACTICE";
        String modeName = isOnlineExam ? "全國模擬考" : state.currentModeName;
        HistoryRecord record = new HistoryRecord(
                HistoryManager.currentTimestamp(), modeName, score, quizList.size(),
                category, examSessionKey, quizSessionKey);
        HistoryManager.save(this, record);

        if (isOnlineExam) {
            setupRankingPushListener();
            uploadScore(score, quizList.size());
        }

        Button btnMain      = findViewById(R.id.btnBackToMain);
        Button btnHistory   = findViewById(R.id.btnHistory);
        Button btnViewQuiz  = findViewById(R.id.btnViewQuiz);

        btnMain.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnViewQuiz.setVisibility(View.VISIBLE);
        btnViewQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizReviewActivity.class);
            intent.putExtra("QUIZ_SESSION_KEY", quizSessionKey);
            startActivity(intent);
        });

        if (isOnlineExam) {
            btnHistory.setText("查看全國排名 📊");
            btnHistory.setOnClickListener(v -> openRankingActivity());
        } else {
            btnHistory.setOnClickListener(v ->
                    startActivity(new Intent(this, HistoryActivity.class)));
        }
    }

    private void setupRankingPushListener() {
        ExamClient.getInstance().setPushListener(rankingData -> {
            HistoryManager.saveRanking(this, examSessionKey, rankingData);
            runOnUiThread(() -> {
                Toast.makeText(this, "🎉 排名已公布！可點擊查看。", Toast.LENGTH_LONG).show();
                Button btn = findViewById(R.id.btnHistory);
                if (btn != null) btn.setText("查看全國排名 📊 (已更新)");
            });
        });
    }

    private void openRankingActivity() {
        boolean hasLocal = HistoryManager.hasRanking(this, examSessionKey);
        Intent intent = new Intent(this, RankingActivity.class);
        intent.putExtra("EXAM_ID", examId);
        intent.putExtra("SESSION_KEY", examSessionKey);
        intent.putExtra("OFFLINE_MODE", hasLocal);
        startActivity(intent);
    }

    private void uploadScore(int score, int total) {
        String userName = state.userName;
        if (userName.isEmpty()) userName = "考生_" + (System.currentTimeMillis() % 10000);
        String cmd = "SUBMIT_SCORE|" + examId + "|" + userName + "|" + score + "|" + total;
        ExamClient.getInstance().sendCommand(cmd, result -> runOnUiThread(() -> {
            if (result != null && result.startsWith("SUBMIT_OK")) {
                Toast.makeText(this, "✅ 成績已上傳！", Toast.LENGTH_SHORT).show();
                pollRankingIfPublished();
            } else {
                Toast.makeText(this, "⚠️ 成績上傳失敗：" + result, Toast.LENGTH_LONG).show();
            }
        }));
    }

    private void pollRankingIfPublished() {
        ExamClient.getInstance().sendCommand("GET_EXAM_STATUS|" + examId, result -> {
            if (result == null || !result.startsWith("EXAM_STATUS")) return;
            String[] p = result.split("\\|", -1);
            if (p.length >= 5 && "1".equals(p[4])) {
                String rankingData = p.length >= 6 ? p[5] : "";
                if (!rankingData.isEmpty()) {
                    HistoryManager.saveRanking(this, examSessionKey, rankingData);
                    runOnUiThread(() ->
                            Toast.makeText(this, "🎉 排名已公布並儲存！", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private View buildResultCard(Quiz q) {
        boolean skipped = q.userAnswer.isEmpty();
        boolean correct = !skipped && q.correctAnswer.equals(q.userAnswer);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(28, 22, 28, 22);
        card.setBackgroundColor(skipped ? Color.parseColor("#FFF9C4")
                : correct ? Color.parseColor("#E8F5E9") : Color.parseColor("#FFEBEE"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 10);
        card.setLayoutParams(lp);

        String icon    = skipped ? "⏭️" : correct ? "✅" : "❌";
        String userAns = skipped ? "未作答" : q.userAnswer;

        TextView tvHeader = new TextView(this);
        tvHeader.setText(icon + " 第 " + q.id + " 題　你的: " + userAns + "　正解: " + q.correctAnswer);
        tvHeader.setTextSize(14f);
        tvHeader.setTypeface(null, Typeface.BOLD);
        tvHeader.setPadding(0, 0, 0, 8);
        card.addView(tvHeader);

        String shortQ = q.questionBody.length() > 120
                ? q.questionBody.substring(0, 120) + "..." : q.questionBody;
        TextView tvQ = new TextView(this);
        tvQ.setText(shortQ);
        tvQ.setTextSize(12f);
        tvQ.setTextColor(Color.parseColor("#424242"));
        tvQ.setPadding(0, 0, 0, 6);
        card.addView(tvQ);

        TextView tvExp = new TextView(this);
        tvExp.setText("💡 " + q.explanation);
        tvExp.setTextSize(13f);
        tvExp.setTextColor(Color.parseColor("#555555"));
        tvExp.setLineSpacing(4f, 1f);
        card.addView(tvExp);

        return card;
    }

    private void updateErrorTracker(List<Quiz> quizList) {
        ErrorTracker tracker = state.errorTracker;
        for (Quiz q : quizList) {
            if (q.userAnswer.isEmpty()) continue;
            boolean correct = q.correctAnswer.equals(q.userAnswer);
            if (correct) {
                String word = extractWord(q.questionBody, q.correctAnswer);
                if (!word.isEmpty()) {
                    if (q.categoryType != 0) tracker.decreaseError(word, q.categoryType, 2);
                    else tracker.decreaseErrorAnyType(word, 2);
                }
            } else {
                for (String opt : new String[]{"A", "B", "C", "D"}) {
                    String word = extractWord(q.questionBody, opt);
                    if (word.isEmpty()) continue;
                    int weight = (opt.equals(q.correctAnswer) || opt.equals(q.userAnswer)) ? 2 : 1;
                    for (int i = 0; i < weight; i++) tracker.addError(word, q.categoryType);
                }
            }
        }
    }

    private String extractWord(String body, String option) {
        try {
            Matcher m = Pattern.compile("\\(" + option + "\\)\\s*([a-zA-Z\\-]+)",
                    Pattern.MULTILINE).matcher(body);
            if (m.find()) return m.group(1).trim();
        } catch (Exception ignored) {}
        return "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ExamClient.getInstance().setPushListener(null);
    }
}
