package com.example.myapplication.essay;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import java.util.List;

public class EssayDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECORD_ID = "record_id";
    private static final int REQUEST_EDIT = 1001;

    private TextView tvSubject, tvTimestamp, tvQuestion, tvUserAnswer, tvAiReply;
    private View     cardUserAnswer, cardAiReply;
    private Button   btnEdit, btnDelete, btnViewScore;

    private String recordId;
    private EssayRecord currentRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_essay_detail);

        recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);
        if (recordId == null) { finish(); return; }

        bindViews();
        setupBackButton();
        loadRecord();
    }

    private void bindViews() {
        tvSubject      = findViewById(R.id.tvDetailSubject);
        tvTimestamp    = findViewById(R.id.tvDetailTimestamp);
        tvQuestion     = findViewById(R.id.tvDetailQuestion);
        tvUserAnswer   = findViewById(R.id.tvDetailUserAnswer);
        tvAiReply      = findViewById(R.id.tvDetailAiReply);
        cardUserAnswer = findViewById(R.id.cardDetailUserAnswer);
        cardAiReply    = findViewById(R.id.cardDetailAiReply);
        btnEdit        = findViewById(R.id.btnDetailEdit);
        btnDelete      = findViewById(R.id.btnDetailDelete);
        btnViewScore   = findViewById(R.id.btnViewScore);
    }

    private void setupBackButton() {
        View btnBack = findViewById(R.id.btnDetailBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void loadRecord() {
        currentRecord = EssayManager.findById(this, recordId);
        if (currentRecord == null) { finish(); return; }
        renderRecord(currentRecord);
    }

    private void renderRecord(EssayRecord rec) {
        tvSubject.setText(rec.subject.isEmpty() ? "未分類" : rec.subject);
        tvTimestamp.setText(rec.timestamp);
        tvQuestion.setText(rec.question.isEmpty() ? "（無題目內容）" : rec.question);

        if (rec.hasUserAnswer()) {
            tvUserAnswer.setText(rec.userAnswer);
            cardUserAnswer.setVisibility(View.VISIBLE);
        } else {
            cardUserAnswer.setVisibility(View.GONE);
        }

        if (rec.hasAiReply()) {
            tvAiReply.setText(rec.aiReply);
            cardAiReply.setVisibility(View.VISIBLE);
            // 有 AI 回覆才顯示一覽成績按鈕
            btnViewScore.setVisibility(View.VISIBLE);
        } else {
            tvAiReply.setText("尚無 AI 回覆。\n點擊「編輯」以填入 AI 評分內容。");
            tvAiReply.setTextColor(0xFF9E9E9E);
            cardAiReply.setVisibility(View.VISIBLE);
            btnViewScore.setVisibility(View.GONE);
        }

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, EssayEditActivity.class);
            intent.putExtra(EssayEditActivity.EXTRA_EDITING_ID, rec.id);
            startActivityForResult(intent, REQUEST_EDIT);
        });

        btnDelete.setOnClickListener(v -> confirmDelete(rec));

        btnViewScore.setOnClickListener(v -> showScoreOverview(rec));
    }

    // ════════════════════════════════
    //  一覽成績對話框
    // ════════════════════════════════

    private void showScoreOverview(EssayRecord rec) {
        // 即時解析 aiReply（不依賴快取，確保資料最新）
        EssayResultParser.ParsedResult result = EssayResultParser.parse(rec.aiReply);

        // 最外層 ScrollView
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(p, p, p, p);
        scrollView.addView(root);

        if (!result.isValid) {
            // 解析失敗：給出提示
            TextView tvErr = new TextView(this);
            tvErr.setText("❌ 解析失敗：" + result.errorMsg
                    + "\n\n請確認 AI 回覆中包含完整的\n===RESULT_START=== ... ===RESULT_END=== 區塊。");
            tvErr.setTextSize(14f);
            tvErr.setTextColor(Color.parseColor("#C62828"));
            tvErr.setLineSpacing(4f, 1f);
            root.addView(tvErr);
        } else {
            // ── 計算滿分總分（各要點滿分加總）──
            int totalFullScore = 0;
            for (EssayResultParser.PointResult pt : result.points) {
                totalFullScore += pt.fullScore;
            }

            // ── 總分區塊 ──
            root.addView(buildSectionHeader("🏆 總分"));
            root.addView(buildTotalScoreCard(result.totalScore, totalFullScore));

            // ── 各要點 ──
            if (!result.points.isEmpty()) {
                root.addView(buildSectionHeader("📋 各要點得分"));
                for (int i = 0; i < result.points.size(); i++) {
                    root.addView(buildPointCard(i + 1, result.points.get(i)));
                }
            }

            // ── 整體評價 ──
            if (result.overallComment != null && !result.overallComment.isEmpty()) {
                root.addView(buildSectionHeader("📝 整體評價"));
                root.addView(buildTextCard(result.overallComment, "#1A237E", "#EEF2FF"));
            }

            // ── 弱點 ──
            if (result.weaknesses != null && !result.weaknesses.isEmpty()) {
                root.addView(buildSectionHeader("⚠️ 待加強之處"));
                root.addView(buildTextCard(result.weaknesses, "#7B1F1F", "#FFF8F8"));
            }

            // ── 儲存解析結果到 record（省去下次重新解析）──
            if (!rec.hasParsedResult()) {
                rec.parsedTotalScore     = result.totalScore;
                rec.parsedOverallComment = result.overallComment != null ? result.overallComment : "";
                rec.parsedWeaknesses     = result.weaknesses != null ? result.weaknesses : "";
                EssayManager.update(this, rec);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("📊 成績一覽")
                .setView(scrollView)
                .setPositiveButton("關閉", null)
                .show();
    }

    // ════════════════════════════════
    //  卡片建構輔助方法
    // ════════════════════════════════

    /** 區塊標題（灰色小字）*/
    private View buildSectionHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#616161"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 16, 0, 6);
        tv.setLayoutParams(lp);
        return tv;
    }

    /** 總分大卡片 */
    private View buildTotalScoreCard(int score, int fullScore) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(20, 20, 20, 20);
        card.setBackgroundColor(Color.parseColor("#E8F5E9"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 4);
        card.setLayoutParams(lp);

        // 大分數顯示
        TextView tvScore = new TextView(this);
        tvScore.setText(score + "");
        tvScore.setTextSize(48f);
        tvScore.setTypeface(null, Typeface.BOLD);

        // 根據得分率決定顏色
        double rate = fullScore > 0 ? (double) score / fullScore : 0;
        tvScore.setTextColor(rate >= 0.8 ? Color.parseColor("#1B5E20")
                : rate >= 0.6 ? Color.parseColor("#E65100")
                : Color.parseColor("#C62828"));
        tvScore.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(tvScore);

        // 滿分與百分比
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rcLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        rcLp.setMarginStart(12);
        rightCol.setLayoutParams(rcLp);

        TextView tvFull = new TextView(this);
        tvFull.setText("/ " + fullScore + " 分");
        tvFull.setTextSize(20f);
        tvFull.setTextColor(Color.parseColor("#424242"));
        rightCol.addView(tvFull);

        if (fullScore > 0) {
            TextView tvRate = new TextView(this);
            tvRate.setText(String.format("得分率 %.1f%%", rate * 100));
            tvRate.setTextSize(14f);
            tvRate.setTextColor(Color.parseColor("#757575"));
            rightCol.addView(tvRate);
        }

        card.addView(rightCol);
        return card;
    }

    /** 單一要點卡片 */
    private View buildPointCard(int no, EssayResultParser.PointResult pt) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 14, 16, 14);
        card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 6);
        card.setLayoutParams(lp);

        // 要點名稱 + 得分（同一列）
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvName = new TextView(this);
        tvName.setText("要點 " + no + "：" + pt.name);
        tvName.setTextSize(14f);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.parseColor("#212121"));
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(tvName);

        // 得分（右對齊，帶顏色）
        double rate = pt.fullScore > 0 ? (double) pt.score / pt.fullScore : 0;
        int scoreColor = rate >= 1.0 ? Color.parseColor("#2E7D32")
                : rate >= 0.8 ? Color.parseColor("#388E3C")
                : rate >= 0.6 ? Color.parseColor("#E65100")
                : Color.parseColor("#C62828");

        TextView tvScore = new TextView(this);
        tvScore.setText(pt.score + " / " + pt.fullScore);
        tvScore.setTextSize(16f);
        tvScore.setTypeface(null, Typeface.BOLD);
        tvScore.setTextColor(scoreColor);
        tvScore.setGravity(Gravity.END);
        headerRow.addView(tvScore);

        card.addView(headerRow);

        // 得分理由（小字）
        if (pt.reason != null && !pt.reason.isEmpty()) {
            // 分隔線
            View divider = new View(this);
            divider.setBackgroundColor(Color.parseColor("#E0E0E0"));
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divLp.setMargins(0, 10, 0, 8);
            divider.setLayoutParams(divLp);
            card.addView(divider);

            TextView tvReason = new TextView(this);
            tvReason.setText(pt.reason);
            tvReason.setTextSize(13f);
            tvReason.setTextColor(Color.parseColor("#555555"));
            tvReason.setLineSpacing(3f, 1f);
            card.addView(tvReason);
        }

        return card;
    }

    /** 通用文字卡片（整體評價 / 弱點）*/
    private View buildTextCard(String text, String textColorHex, String bgColorHex) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14f);
        tv.setTextColor(Color.parseColor(textColorHex));
        tv.setLineSpacing(4f, 1f);
        tv.setPadding(16, 14, 16, 14);
        tv.setBackgroundColor(Color.parseColor(bgColorHex));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    // ════════════════════════════════
    //  其他
    // ════════════════════════════════

    private void confirmDelete(EssayRecord rec) {
        new AlertDialog.Builder(this)
                .setTitle("刪除紀錄")
                .setMessage("確定要刪除「" + rec.subject + "」的這筆紀錄嗎？\n此操作無法還原。")
                .setPositiveButton("刪除", (d, w) -> {
                    EssayManager.delete(this, rec.id);
                    Toast.makeText(this, "已刪除", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT && resultCode == RESULT_OK) {
            loadRecord();
            setResult(RESULT_OK);
        }
    }
}
