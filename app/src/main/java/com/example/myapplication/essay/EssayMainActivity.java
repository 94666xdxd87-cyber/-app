package com.example.myapplication.essay;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import java.util.*;

/**
 * 申論題主頁面。
 * 顯示所有紀錄列表（最新在前），可新增、查看詳情、進入考古題庫、查看重點整理。
 */
public class EssayMainActivity extends AppCompatActivity {

    private static final int REQUEST_EDIT      = 1001;
    private static final int REQUEST_DETAIL    = 1002;
    private static final int REQUEST_PAST_EXAM = 1003;

    private ListView listView;
    private TextView tvEmpty;
    private List<EssayRecord> records = new ArrayList<>();
    private EssayListAdapter  adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_essay_main);

        listView = findViewById(R.id.essayListView);
        tvEmpty  = findViewById(R.id.tvEssayEmpty);

        View btnBack = findViewById(R.id.btnEssayMainBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnAdd = findViewById(R.id.btnEssayAdd);
        if (btnAdd != null) btnAdd.setOnClickListener(v -> openEdit(null));

        View btnPastExam = findViewById(R.id.btnPastExam);
        if (btnPastExam != null) btnPastExam.setOnClickListener(v ->
                startActivityForResult(
                        new Intent(this, ExamCategoryActivity.class), REQUEST_PAST_EXAM));

        // ── 重點整理按鈕 ──────────────────────────────────────────
        View btnKeyPoints = findViewById(R.id.btnKeyPoints);
        if (btnKeyPoints != null) btnKeyPoints.setOnClickListener(v -> showKeyPointsDialog());

        adapter = new EssayListAdapter(this, records);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            EssayRecord rec = adapter.getRecord(position);
            if (rec != null) openDetail(rec);
        });

        loadRecords();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecords();
    }

    // ════════════════════════════════
    //  重點整理 Dialog
    // ════════════════════════════════

    private void showKeyPointsDialog() {
        List<EssayRecord> allRecords = EssayManager.loadAll(this);

        Map<String, LinkedHashSet<String>> subjectPointMap = new LinkedHashMap<>();

        for (EssayRecord rec : allRecords) {
            if (!rec.hasAiReply()) continue;

            EssayResultParser.ParsedResult parsed = EssayResultParser.parse(rec.aiReply);

            String subject = rec.subject.trim().isEmpty() ? "未分類" : rec.subject.trim();
            LinkedHashSet<String> pointSet = subjectPointMap.computeIfAbsent(
                    subject, k -> new LinkedHashSet<>());

            if (parsed.isValid) {
                for (EssayResultParser.PointResult pt : parsed.points) {
                    if (pt.name != null && !pt.name.trim().isEmpty())
                        pointSet.add(pt.name.trim());
                }
                for (EssayResultParser.RubricItem ri : parsed.rubric) {
                    if (ri.name != null && !ri.name.trim().isEmpty())
                        pointSet.add(ri.name.trim());
                }
            } else {
                collectPointsFromRaw(rec.aiReply, pointSet);
            }
        }

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        root.setPadding(p, p / 2, p, p);
        scrollView.addView(root);

        if (subjectPointMap.isEmpty()) {
            TextView tvHint = new TextView(this);
            tvHint.setText("尚無任何含有 AI 評分的紀錄。\n\n完成一次作答並貼入 AI 回覆後即可看到考點整理。");
            tvHint.setTextSize(14f);
            tvHint.setTextColor(0xFF757575);
            tvHint.setPadding(0, dp(12), 0, 0);
            root.addView(tvHint);
        } else {
            int totalPoints = 0;
            for (LinkedHashSet<String> s : subjectPointMap.values()) totalPoints += s.size();

            TextView tvSummary = new TextView(this);
            tvSummary.setText("共 " + subjectPointMap.size() + " 個科目　" + totalPoints + " 個考點");
            tvSummary.setTextSize(12f);
            tvSummary.setTextColor(0xFF9E9E9E);
            tvSummary.setPadding(0, 0, 0, dp(8));
            root.addView(tvSummary);

            root.addView(makeDivider());

            for (Map.Entry<String, LinkedHashSet<String>> entry : subjectPointMap.entrySet()) {
                String subject = entry.getKey();
                LinkedHashSet<String> points = entry.getValue();
                if (points.isEmpty()) continue;

                // 科目標題
                TextView tvSubject = new TextView(this);
                tvSubject.setText("▌ " + subject + "（" + points.size() + " 個考點）");
                tvSubject.setTextSize(15f);
                tvSubject.setTypeface(null, Typeface.BOLD);
                tvSubject.setTextColor(Color.parseColor("#1565C0"));
                LinearLayout.LayoutParams subjectLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                subjectLp.setMargins(0, dp(14), 0, dp(6));
                tvSubject.setLayoutParams(subjectLp);
                root.addView(tvSubject);

                // 各考點（帶序號）
                int index = 1;
                for (String point : points) {
                    TextView tvPoint = new TextView(this);
                    tvPoint.setText(index + ".  " + point);
                    tvPoint.setTextSize(14f);
                    tvPoint.setTextColor(Color.parseColor("#212121"));
                    // ★ 修正：改用兩參數版本 setLineSpacing(extra, multiplier)
                    tvPoint.setLineSpacing(0f, 1.4f);

                    LinearLayout.LayoutParams ptLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    ptLp.setMargins(dp(8), 0, 0, dp(4));
                    tvPoint.setLayoutParams(ptLp);

                    tvPoint.setPadding(dp(10), dp(7), dp(10), dp(7));
                    tvPoint.setBackgroundColor(
                            index % 2 == 0
                                    ? Color.parseColor("#F5F5F5")
                                    : Color.WHITE);
                    root.addView(tvPoint);
                    index++;
                }

                root.addView(makeDivider());
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("📌 考點重點整理")
                .setView(scrollView)
                .setPositiveButton("關閉", null)
                .show();
    }

    private void collectPointsFromRaw(String raw, Set<String> pointSet) {
        if (raw == null || raw.isEmpty()) return;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:POINT_NAME|RUBRIC_ITEM):\\s*(.+)")
                .matcher(raw);
        while (m.find()) {
            String name = m.group(1).trim();
            if (!name.isEmpty()) pointSet.add(name);
        }
    }

    private View makeDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#E0E0E0"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, dp(6), 0, 0);
        divider.setLayoutParams(lp);
        return divider;
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    private void loadRecords() {
        records.clear();
        records.addAll(EssayManager.loadAll(this));
        adapter.setRecords(records);
        tvEmpty.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openEdit(String editingId) {
        Intent intent = new Intent(this, EssayEditActivity.class);
        if (editingId != null) intent.putExtra(EssayEditActivity.EXTRA_EDITING_ID, editingId);
        startActivityForResult(intent, REQUEST_EDIT);
    }

    private void openDetail(EssayRecord rec) {
        Intent intent = new Intent(this, EssayDetailActivity.class);
        intent.putExtra(EssayDetailActivity.EXTRA_RECORD_ID, rec.id);
        startActivityForResult(intent, REQUEST_DETAIL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) loadRecords();
    }
}
