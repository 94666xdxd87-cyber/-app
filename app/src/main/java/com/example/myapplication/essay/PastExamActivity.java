package com.example.myapplication.essay;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import java.util.*;

/**
 * 歷屆考古題列表頁面。
 *
 * 可接收 Intent extras：
 *   EXTRA_EXAM_TYPE  — 考試類型（高考、普考…），不傳則顯示全部
 *   EXTRA_YEAR       — 年份篩選（113年），不傳則顯示全部
 *
 * 若同時傳入 examType + year，則隱藏 Spinner，直接顯示該年考題。
 */
public class PastExamActivity extends AppCompatActivity {

    public static final String EXTRA_EXAM_TYPE = "exam_type";
    public static final String EXTRA_YEAR      = "year";

    private Spinner      spinnerYear, spinnerSubject;
    private EditText     etSearch;
    private ListView     listView;
    private TextView     tvEmpty, tvImportStatus, tvCurrentFilter;
    private LinearLayout layoutImporting, layoutFilterBar;
    private Button       btnReimport;

    private final List<PastExamQuestion> allQuestions  = new ArrayList<>();
    private final List<PastExamQuestion> showQuestions = new ArrayList<>();
    private PastExamListAdapter adapter;

    // 從 Intent 取得的固定篩選（不可在 UI 改變）
    private String intentExamType = null;
    private String intentYear     = null;
    private boolean isFixedFilter = false;  // 若為 true，隱藏 Spinner

    private String filterYear    = null;
    private String filterSubject = null;
    private String filterSearch  = "";

    private static final String ALL_YEARS    = "全部年份";
    private static final String ALL_SUBJECTS = "全部科目";

    // ════════════════════════════════
    //  生命週期
    // ════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_exam);

        // 讀取 Intent 篩選參數
        intentExamType = getIntent().getStringExtra(EXTRA_EXAM_TYPE);
        intentYear     = getIntent().getStringExtra(EXTRA_YEAR);
        isFixedFilter  = (intentExamType != null && intentYear != null);

        if (isFixedFilter) {
            filterYear = intentYear;
        }

        bindViews();
        setupTitle();
        setupButtons();
        setupSearchFilter();
        setupList();
        triggerImport(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadAndFilter();
    }

    // ════════════════════════════════
    //  View 綁定
    // ════════════════════════════════

    private void bindViews() {
        spinnerYear      = findViewById(R.id.spinnerPastExamYear);
        spinnerSubject   = findViewById(R.id.spinnerPastExamSubject);
        etSearch         = findViewById(R.id.etPastExamSearch);
        listView         = findViewById(R.id.pastExamListView);
        tvEmpty          = findViewById(R.id.tvPastExamEmpty);
        tvImportStatus   = findViewById(R.id.tvImportStatus);
        layoutImporting  = findViewById(R.id.layoutImporting);
        btnReimport      = findViewById(R.id.btnReimport);
        tvCurrentFilter  = findViewById(R.id.tvCurrentFilter);
        layoutFilterBar  = findViewById(R.id.layoutFilterBar);
    }

    // ════════════════════════════════
    //  標題與篩選列顯示
    // ════════════════════════════════

    private void setupTitle() {
        if (isFixedFilter) {
            // 固定篩選模式：顯示「高考 — 113年」標題，隱藏 Spinner
            TextView tvPageTitle = findViewById(R.id.tvPastExamPageTitle);
            if (tvPageTitle != null)
                tvPageTitle.setText("📄 " + intentExamType + " — " + intentYear);

            if (layoutFilterBar != null)
                layoutFilterBar.setVisibility(View.GONE);

            if (tvCurrentFilter != null) {
                tvCurrentFilter.setText(intentExamType + "  " + intentYear);
                tvCurrentFilter.setVisibility(View.VISIBLE);
            }
        }
    }

    // ════════════════════════════════
    //  按鈕
    // ════════════════════════════════

    private void setupButtons() {
        View btnBack = findViewById(R.id.btnPastExamBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnAdd = findViewById(R.id.btnPastExamAdd);
        if (btnAdd != null) btnAdd.setOnClickListener(v -> showAddDialog());

        if (btnReimport != null) {
            btnReimport.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                            .setTitle("重新匯入考古題")
                            .setMessage("這將清除所有現有考古題並重新從檔案匯入。確定嗎？")
                            .setPositiveButton("確定匯入", (d, w) -> triggerImport(true))
                            .setNegativeButton("取消", null)
                            .show());
        }
    }

    // ════════════════════════════════
    //  篩選器
    // ════════════════════════════════

    private void setupSearchFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterSearch = s.toString().trim();
                applyFilter();
            }
        });
    }

    private void rebuildSpinners() {
        if (isFixedFilter) return;  // 固定篩選模式不需要 Spinner

        // ── 年份 ──
        List<String> years = new ArrayList<>();
        years.add(ALL_YEARS);
        years.addAll(PastExamManager.loadAllYears(this));
        ArrayAdapter<String> yearAdp = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, years);
        yearAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdp);
        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filterYear = (pos == 0) ? null : years.get(pos);
                applyFilter();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // ── 科目 ──
        List<String> subjects = new ArrayList<>();
        subjects.add(ALL_SUBJECTS);
        subjects.addAll(PastExamManager.loadAllSubjects(this));
        ArrayAdapter<String> subAdp = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, subjects);
        subAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subAdp);
        spinnerSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filterSubject = (pos == 0) ? null : subjects.get(pos);
                applyFilter();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    // ════════════════════════════════
    //  列表
    // ════════════════════════════════

    private void setupList() {
        adapter = new PastExamListAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, pos, id) ->
                openQuestionDetail(showQuestions.get(pos)));

        listView.setOnItemLongClickListener((parent, view, pos, id) -> {
            confirmDelete(showQuestions.get(pos));
            return true;
        });
    }

    // ════════════════════════════════
    //  匯入
    // ════════════════════════════════

    private void triggerImport(boolean force) {
        layoutImporting.setVisibility(View.VISIBLE);
        tvImportStatus.setText("⏳ 正在匯入歷屆考古題…");

        PastExamImporter.SuccessCallback onSuccess = (total, skipped) ->
                new Handler(Looper.getMainLooper()).post(() -> {
                    layoutImporting.setVisibility(View.GONE);
                    if (total > 0) {
                        Toast.makeText(this,
                                "✅ 匯入完成！共 " + total + " 道題",
                                Toast.LENGTH_LONG).show();
                    }
                    reloadAndFilter();
                    rebuildSpinners();
                });

        PastExamImporter.ErrorCallback onError = msg ->
                new Handler(Looper.getMainLooper()).post(() -> {
                    layoutImporting.setVisibility(View.GONE);
                    Toast.makeText(this, "❌ 匯入失敗：" + msg, Toast.LENGTH_LONG).show();
                    reloadAndFilter();
                });

        if (force) {
            PastExamImporter.forceReimport(this, onSuccess, onError);
        } else {
            PastExamImporter.importIfNeeded(this, onSuccess, onError);
        }
    }

    // ════════════════════════════════
    //  資料讀取與篩選
    // ════════════════════════════════

    private void reloadAndFilter() {
        allQuestions.clear();
        if (isFixedFilter) {
            // 只載入指定考試類型 + 年份的題目
            allQuestions.addAll(
                    PastExamManager.loadByExamTypeAndYear(this, intentExamType, intentYear));
        } else {
            allQuestions.addAll(PastExamManager.loadAllQuestions(this));
        }
        applyFilter();
    }

    private void applyFilter() {
        showQuestions.clear();
        for (PastExamQuestion q : allQuestions) {
            if (!isFixedFilter) {
                if (filterYear    != null && !filterYear.equals(q.year))       continue;
                if (filterSubject != null && !filterSubject.equals(q.subject)) continue;
            } else {
                // 固定模式仍可用科目 Spinner（若存在）
                if (filterSubject != null && !filterSubject.equals(q.subject)) continue;
            }
            if (!filterSearch.isEmpty()) {
                String lq = filterSearch.toLowerCase();
                if (!q.year.toLowerCase().contains(lq)
                        && !q.subject.toLowerCase().contains(lq)
                        && !q.question.toLowerCase().contains(lq)) continue;
            }
            showQuestions.add(q);
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setText("尚無符合條件的考古題\n（共 " + allQuestions.size() + " 道）");
        tvEmpty.setVisibility(showQuestions.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ════════════════════════════════
    //  進入題目詳細頁
    // ════════════════════════════════

    private void openQuestionDetail(PastExamQuestion q) {
        Intent intent = new Intent(this, QuestionDetailActivity.class);
        intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION_ID, q.id);
        startActivity(intent);
    }

    // ════════════════════════════════
    //  進入作答（保留供其他地方呼叫）
    // ════════════════════════════════

    private void openEditFromQuestion(PastExamQuestion q) {
        Intent intent = new Intent(this, EssayEditActivity.class);
        intent.putExtra(EssayEditActivity.EXTRA_SOURCE_QUESTION, q.id);
        startActivity(intent);
    }

    // ════════════════════════════════
    //  手動新增 Dialog
    // ════════════════════════════════

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📄 手動新增考古題");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        layout.setPadding(p, p / 2, p, 0);

        EditText etYear     = addLabeledField(layout, "年份",    "113年",    InputType.TYPE_CLASS_TEXT);
        space(layout, 10);
        EditText etSubject  = addLabeledField(layout, "科目",    "資通網路",  InputType.TYPE_CLASS_TEXT);
        space(layout, 10);
        EditText etScore    = addLabeledField(layout, "配分",    "25",       InputType.TYPE_CLASS_NUMBER);
        etScore.setText("25");
        space(layout, 10);

        addLabel(layout, "題目內容");
        EditText etQuestion = new EditText(this);
        etQuestion.setHint("貼上或輸入題目文字…");
        etQuestion.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etQuestion.setMinLines(3);
        etQuestion.setMaxLines(10);
        etQuestion.setGravity(Gravity.TOP);
        layout.addView(etQuestion);

        builder.setView(layout);
        builder.setPositiveButton("新增", (d, w) -> {
            String year     = etYear.getText().toString().trim();
            String subject  = etSubject.getText().toString().trim();
            String question = etQuestion.getText().toString().trim();
            int score = 25;
            try { score = Integer.parseInt(etScore.getText().toString().trim()); }
            catch (Exception ignored) {}

            if (question.isEmpty()) {
                Toast.makeText(this, "❌ 題目內容不能為空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (year.isEmpty())    year    = "未知年份";
            if (subject.isEmpty()) subject = "未分類";

            String examType = (intentExamType != null) ? intentExamType : "高考";
            PastExamQuestion q = new PastExamQuestion(
                    PastExamManager.generateId(), year, subject, score, question, examType);
            PastExamManager.saveQuestion(this, q);
            reloadAndFilter();
            rebuildSpinners();
            Toast.makeText(this, "✅ 已新增：" + q.getDisplayTitle(), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // ════════════════════════════════
    //  刪除確認
    // ════════════════════════════════

    private void confirmDelete(PastExamQuestion q) {
        int related = PastExamManager.loadRecordsForQuestion(this, q.id).size();
        String msg = "確定刪除「" + q.getDisplayTitle() + "」？\n此操作不可還原。";
        if (related > 0)
            msg += "\n\n注意：已有 " + related + " 筆作答紀錄，紀錄本身不會被刪除。";

        new AlertDialog.Builder(this)
                .setTitle("刪除考古題").setMessage(msg)
                .setPositiveButton("刪除", (d, w) -> {
                    PastExamManager.deleteQuestion(this, q.id);
                    reloadAndFilter();
                    Toast.makeText(this, "已刪除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null).show();
    }

    // ════════════════════════════════
    //  Dialog UI 輔助
    // ════════════════════════════════

    private EditText addLabeledField(LinearLayout parent, String label,
                                     String hint, int inputType) {
        addLabel(parent, label);
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setSingleLine(true);
        parent.addView(et);
        return et;
    }

    private void addLabel(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTextColor(0xFF616161);
        parent.addView(tv);
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    private void space(LinearLayout parent, int dpVal) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(dpVal)));
        parent.addView(v);
    }

    // ════════════════════════════════
    //  Adapter
    // ════════════════════════════════

    private class PastExamListAdapter extends BaseAdapter {

        @Override public int    getCount()              { return showQuestions.size(); }
        @Override public Object getItem(int pos)        { return showQuestions.get(pos); }
        @Override public long   getItemId(int pos)      { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh;
            if (convertView == null) {
                convertView = LayoutInflater.from(PastExamActivity.this)
                        .inflate(R.layout.item_past_exam_question, parent, false);
                vh = new ViewHolder(convertView);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }

            PastExamQuestion q = showQuestions.get(position);

            vh.tvTitle.setText(q.year + "  " + q.subject + "  （" + q.score + " 分）");

            String preview = q.question.trim().replaceAll("\\n+", " ");
            if (preview.length() > 60) preview = preview.substring(0, 60) + "…";
            vh.tvPreview.setText(preview);

            int count = PastExamManager.loadRecordsForQuestion(
                    PastExamActivity.this, q.id).size();
            if (count > 0) {
                vh.tvAnswered.setText("已作答 " + count + " 次");
                vh.tvAnswered.setVisibility(View.VISIBLE);
            } else {
                vh.tvAnswered.setVisibility(View.GONE);
            }

            convertView.setBackgroundColor(
                    position % 2 == 0 ? Color.WHITE : Color.parseColor("#F5F5F5"));

            return convertView;
        }

        class ViewHolder {
            TextView tvTitle, tvPreview, tvAnswered;
            ViewHolder(View v) {
                tvTitle    = v.findViewById(R.id.itemPastExamTitle);
                tvPreview  = v.findViewById(R.id.itemPastExamPreview);
                tvAnswered = v.findViewById(R.id.itemPastExamAnswered);
            }
        }
    }
}
