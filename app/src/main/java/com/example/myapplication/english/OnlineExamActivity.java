package com.example.myapplication.english;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.core.*;
import java.util.ArrayList;
import java.util.List;

public class OnlineExamActivity extends AppCompatActivity {

    private static final String ROOM_ID = "room01";

    private LinearLayout layoutNameInput, layoutEntryClosed, layoutExamRoom;
    private EditText etUserName;
    private Button btnConnect, btnStartExam;
    private TextView tvExamTitle, tvStatus, tvCountdownTime, tvCountdownLabel, tvEntryDeadlineLabel;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isExamReady   = false;
    private boolean isDownloading = false;

    private long examEndTimestampMs  = 0;
    private long localExamStartMs    = 0;
    private long localEntryCloseMs   = 0;
    private int  examDurationSeconds = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_exam);

        layoutNameInput   = findViewById(R.id.layoutNameInput);
        etUserName        = findViewById(R.id.etUserName);
        btnConnect        = findViewById(R.id.btnConnect);
        layoutEntryClosed = findViewById(R.id.layoutEntryClosed);
        layoutExamRoom    = findViewById(R.id.layoutExamRoom);
        tvExamTitle       = findViewById(R.id.tvExamTitle);
        tvStatus          = findViewById(R.id.tvStatus);
        tvCountdownTime   = findViewById(R.id.tvCountdownTime);
        tvCountdownLabel  = findViewById(R.id.tvCountdownLabel);
        tvEntryDeadlineLabel = findViewById(R.id.tvEntryDeadlineLabel);
        btnStartExam      = findViewById(R.id.btnStartExam);

        AppState.getInstance().currentQuizList = new ArrayList<>();
        btnConnect.setOnClickListener(v -> onConnectTapped());
        btnStartExam.setOnClickListener(v -> startQuiz());
    }

    private void onConnectTapped() {
        String name = etUserName.getText().toString().trim();
        if (name.isEmpty()) { Toast.makeText(this, "請先輸入姓名！", Toast.LENGTH_SHORT).show(); return; }
        if (!ExamClient.getInstance().isConnected()) {
            Toast.makeText(this, "尚未連線至 Server，請先在主選單設定連線", Toast.LENGTH_LONG).show(); return;
        }
        AppState.getInstance().userName = name;
        layoutNameInput.setVisibility(View.GONE);
        layoutExamRoom.setVisibility(View.VISIBLE);
        tvStatus.setText("正在連線至考場...");
        handler.post(syncRunnable);
    }

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            String userName = AppState.getInstance().userName;
            ExamClient.getInstance().sendCommand(
                    "JOIN_EXAM|" + ROOM_ID + "|" + userName,
                    result -> runOnUiThread(() -> handleSyncResult(result)));
            if (!isExamReady) handler.postDelayed(this, 2000);
        }
    };

    private void handleSyncResult(String result) {
        if (result == null || result.startsWith("ERR")) { tvStatus.setText("❌ 連線異常：" + result); return; }
        if ("ENTRY_CLOSED".equals(result)) {
            layoutExamRoom.setVisibility(View.GONE);
            layoutEntryClosed.setVisibility(View.VISIBLE);
            handler.removeCallbacks(syncRunnable); return;
        }
        if (!result.startsWith("JOIN_OK")) return;

        String[] parts = result.split("\\|");
        if (parts.length < 6) { tvStatus.setText("Server 格式異常"); return; }

        tvExamTitle.setText(parts[1]);
        long secUntilStart      = Long.parseLong(parts[2]);
        long secUntilEntryClose = Long.parseLong(parts[3]);
        examDurationSeconds     = Integer.parseInt(parts[4]);
        long serverExamEndMs    = Long.parseLong(parts[5]);

        AppState state = AppState.getInstance();
        state.examDurationSeconds  = examDurationSeconds;
        state.examEndTimestampMs   = serverExamEndMs;
        state.currentExamId        = ROOM_ID;

        long nowMs = System.currentTimeMillis();
        if (localExamStartMs == 0) {
            localExamStartMs  = nowMs + secUntilStart * 1000L;
            localEntryCloseMs = nowMs + secUntilEntryClose * 1000L;
        }
        examEndTimestampMs = serverExamEndMs;

        updateUI();
    }

    private void updateUI() {
        long nowMs             = System.currentTimeMillis();
        long msUntilStart      = localExamStartMs  - nowMs;
        long msUntilEntryClose = localEntryCloseMs - nowMs;

        if (msUntilEntryClose <= 0 && localEntryCloseMs != 0) {
            layoutExamRoom.setVisibility(View.GONE);
            layoutEntryClosed.setVisibility(View.VISIBLE);
            handler.removeCallbacks(syncRunnable); return;
        }

        if (msUntilStart > 0) {
            tvCountdownLabel.setText("距離開考還有");
            tvCountdownTime.setText(formatTime(msUntilStart / 1000));
            tvCountdownTime.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            if (msUntilEntryClose > 0)
                tvEntryDeadlineLabel.setText("⚠️ 入場截止倒數：" + formatTime(msUntilEntryClose / 1000));
            return;
        }

        if (!isExamReady) {
            isExamReady = true;
            tvCountdownTime.setText("00:00");
            tvCountdownTime.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            tvCountdownLabel.setText("考試進行中");
            tvStatus.setText("考試開始！獲取題目中...");
            btnStartExam.setEnabled(true);
            if (msUntilEntryClose > 0)
                tvEntryDeadlineLabel.setText("⚠️ 入場截止倒數：" + formatTime(msUntilEntryClose / 1000));
            if (!isDownloading && AppState.getInstance().currentQuizList.isEmpty())
                fetchQuestionsFromServer();
        }
    }

    private void fetchQuestionsFromServer() {
        isDownloading = true;
        tvStatus.setText("下載題目中...");
        ExamClient.getInstance().sendCommand("FETCH_QUESTIONS|" + ROOM_ID, result -> runOnUiThread(() -> {
            if (result != null && result.startsWith("QUESTIONS|")) {
                List<Quiz> quizzes = QuizParser.parse(result.substring(10), 0);
                if (quizzes != null && !quizzes.isEmpty()) {
                    AppState.getInstance().currentQuizList = quizzes;
                    isDownloading = false;
                    Toast.makeText(this, "✅ 題目下載完成！", Toast.LENGTH_SHORT).show();
                    startQuiz();
                } else { isDownloading = false; tvStatus.setText("題目解析失敗，請重試"); }
            } else if (result != null && result.startsWith("WAIT")) {
                isDownloading = false; tvStatus.setText("Server 正在準備題目，請稍候...");
            } else { isDownloading = false; tvStatus.setText("下載失敗：" + result); }
        }));
    }

    private void startQuiz() {
        if (AppState.getInstance().currentQuizList == null
                || AppState.getInstance().currentQuizList.isEmpty()) {
            if (!isDownloading) fetchQuestionsFromServer(); return;
        }
        String sessionKey = HistoryManager.currentSessionKey(ROOM_ID);
        AppState.getInstance().currentExamSessionKey = sessionKey;

        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtra("IS_ONLINE_EXAM", true);
        intent.putExtra("EXAM_ID", ROOM_ID);
        startActivity(intent);
        finish();
    }

    private String formatTime(long sec) {
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
