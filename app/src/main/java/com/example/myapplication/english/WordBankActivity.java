package com.example.myapplication.english;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.core.*;
import java.util.*;

public class WordBankActivity extends AppCompatActivity {

    private static final String[] TYPE_LABELS  = {"名詞", "動詞", "副詞", "形容詞", "錯題本"};
    private static final String[] ASSET_FILES  = {"noun.txt", "verb.txt", "adverb.txt", "adjective.txt"};

    private AppState state;
    private int currentTypeIndex = 0;

    private List<String> currentWords = new ArrayList<>();
    private final Set<String> checkedWords = new LinkedHashSet<>();
    private final Set<String> explainedWordsCache = new HashSet<>();
    private String currentQuery = "";

    private Spinner  spinnerWordType;
    private LinearLayout layoutAddWord;
    private EditText etNewWord, etSearch;
    private TextView tvWordCount;
    private Button   btnAiExplain;
    private RecyclerView recyclerWordList;

    private WordBankAdapter  wordBankAdapter;
    private WrongWordAdapter wrongWordAdapter;
    private LinearLayout layoutWrongWordHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_bank);

        state = AppState.getInstance();

        spinnerWordType   = findViewById(R.id.spinnerWordType);
        layoutAddWord     = findViewById(R.id.layoutAddWord);
        recyclerWordList  = findViewById(R.id.recyclerWordList);
        etNewWord         = findViewById(R.id.etNewWord);
        etSearch          = findViewById(R.id.etSearch);
        tvWordCount       = findViewById(R.id.tvWordCount);
        btnAiExplain      = findViewById(R.id.btnAiExplain);

        recyclerWordList.setLayoutManager(new LinearLayoutManager(this));
        recyclerWordList.setHasFixedSize(false);

        wordBankAdapter = new WordBankAdapter(new ArrayList<>(), checkedWords, explainedWordsCache);
        wordBankAdapter.setOnDeleteListener(word -> {
            checkedWords.remove(word);
            currentWords.remove(word);
            renderCurrentTab();
        });
        wordBankAdapter.setOnCheckedListener((word, checked) -> {
            if (checked) checkedWords.add(word); else checkedWords.remove(word);
            updateCountText(wordBankAdapter.getItemCount());
        });

        wrongWordAdapter = new WrongWordAdapter(new ArrayList<>());
        wrongWordAdapter.setOnDeleteListener((ww, type) ->
            new AlertDialog.Builder(this)
                .setTitle("確認刪除")
                .setMessage("確定從錯題本刪除「" + ww.word + "」嗎？")
                .setPositiveButton("刪除", (d, w2) -> {
                    state.errorTracker.removeWord(ww.word, type);
                    state.errorTracker.save();
                    renderCurrentTab();
                })
                .setNegativeButton("取消", null).show()
        );
        wrongWordAdapter.setOnEditListener((ww, type) -> showAdjustStrengthDialog(ww, type));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                currentQuery = s.toString().trim().toLowerCase();
                renderCurrentTab();
            }
        });

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, TYPE_LABELS);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWordType.setAdapter(spinnerAdapter);
        spinnerWordType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (pos != currentTypeIndex || currentWords.isEmpty()) {
                    currentTypeIndex = pos;
                    checkedWords.clear();
                    loadCurrentWords();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        findViewById(R.id.btnAddWord).setOnClickListener(v -> onAddWord());
        findViewById(R.id.btnSaveWordBank).setOnClickListener(v -> onSave());
        btnAiExplain.setOnClickListener(v -> onAiExplain());

        refreshExplainedCache();
        loadCurrentWords();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshExplainedCache();
        renderCurrentTab();
    }

    private void loadCurrentWords() {
        if (currentTypeIndex < 4) {
            switch (currentTypeIndex) {
                case 0: currentWords = new ArrayList<>(state.nounPool);      break;
                case 1: currentWords = new ArrayList<>(state.verbPool);      break;
                case 2: currentWords = new ArrayList<>(state.adverbPool);    break;
                case 3: currentWords = new ArrayList<>(state.adjectivePool); break;
            }
        }
        renderCurrentTab();
    }

    private void renderCurrentTab() {
        boolean isWordBank = (currentTypeIndex < 4);
        layoutAddWord.setVisibility(isWordBank ? View.VISIBLE : View.GONE);
        btnAiExplain.setVisibility(isWordBank ? View.VISIBLE : View.GONE);

        if (isWordBank) {
            hideWrongWordHeader();
            renderWordList(filterWords(currentWords, currentQuery));
        } else {
            renderWrongWords(currentQuery);
        }
    }

    private void renderWordList(List<String> filtered) {
        if (recyclerWordList.getAdapter() != wordBankAdapter) {
            recyclerWordList.setAdapter(wordBankAdapter);
        }
        wordBankAdapter.updateWords(filtered);
        updateCountText(filtered.size());
    }

    private void updateCountText(int filteredSize) {
        String countText = "共 " + currentWords.size() + " 個單字";
        if (!currentQuery.isEmpty()) countText += "　篩選中：" + filteredSize + " 個符合";
        countText += "　（已勾選 " + checkedWords.size() + " 個）";
        tvWordCount.setText(countText);
    }

    private void renderWrongWords(String query) {
        if (recyclerWordList.getAdapter() != wrongWordAdapter) {
            recyclerWordList.setAdapter(wrongWordAdapter);
        }
        ensureWrongWordHeaderVisible();

        ErrorTracker tracker = state.errorTracker;
        int[]    types = {1, 2, 3, 4};
        String[] names = {"📘 名詞", "🎬 動詞", "📗 副詞", "📙 形容詞"};
        int totalMatch = 0, totalAll = 0;

        List<WrongWordAdapter.Item> items = new ArrayList<>();

        for (int t = 0; t < types.length; t++) {
            List<WrongWord> all = tracker.getAllForType(types[t]);
            totalAll += all.size();
            if (all.isEmpty()) continue;

            List<WrongWord> filtered = new ArrayList<>();
            for (WrongWord ww : all) {
                if (query.isEmpty() || ww.word.toLowerCase().startsWith(query)) filtered.add(ww);
            }
            if (filtered.isEmpty()) continue;
            totalMatch += filtered.size();

            String sectionTitle = names[t] + "（" + (query.isEmpty()
                    ? all.size() + " 字"
                    : filtered.size() + "/" + all.size() + " 字符合") + "）";
            items.add(new WrongWordAdapter.Item(sectionTitle));
            for (WrongWord ww : filtered) items.add(new WrongWordAdapter.Item(ww, types[t]));
        }

        wrongWordAdapter.updateItems(items);

        String countText = "錯題本共 " + totalAll + " 筆";
        if (!query.isEmpty()) countText += "　篩選中：" + totalMatch + " 筆符合";
        tvWordCount.setText(countText);
    }

    private void ensureWrongWordHeaderVisible() {
        if (layoutWrongWordHeader == null) {
            layoutWrongWordHeader = buildAddWrongWordSection();
            ViewGroup parent = (ViewGroup) recyclerWordList.getParent();
            int recyclerIndex = parent.indexOfChild(recyclerWordList);
            parent.addView(layoutWrongWordHeader, recyclerIndex);
        }
        layoutWrongWordHeader.setVisibility(View.VISIBLE);
    }

    private void hideWrongWordHeader() {
        if (layoutWrongWordHeader != null) layoutWrongWordHeader.setVisibility(View.GONE);
    }

    private List<String> filterWords(List<String> source, String query) {
        if (query.isEmpty()) return source;
        List<String> result = new ArrayList<>();
        for (String w : source) { if (w.toLowerCase().startsWith(query)) result.add(w); }
        return result;
    }

    private void refreshExplainedCache() {
        explainedWordsCache.clear();
        for (WordExplainRecord r : WordExplainManager.loadAll(this))
            explainedWordsCache.add(r.word.toLowerCase());
    }

    private void onAddWord() {
        String word = etNewWord.getText().toString().trim();
        if (word.isEmpty()) { Toast.makeText(this, "請輸入單字", Toast.LENGTH_SHORT).show(); return; }
        if (currentWords.contains(word)) { Toast.makeText(this, word + " 已存在", Toast.LENGTH_SHORT).show(); return; }
        currentWords.add(word);
        etNewWord.setText("");
        renderCurrentTab();
    }

    private void onSave() {
        if (currentTypeIndex >= 4) return;
        try {
            FileLoader.savePool(this, ASSET_FILES[currentTypeIndex], currentWords);
            state.setPoolByIndex(currentTypeIndex, new ArrayList<>(currentWords));
            Toast.makeText(this, "✅ 已儲存 " + TYPE_LABELS[currentTypeIndex] + " 單字庫（共 "
                    + currentWords.size() + " 字）", Toast.LENGTH_SHORT).show();
            state.poolsLoaded = false;
        } catch (Exception e) {
            Toast.makeText(this, "❌ 儲存失敗：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void onAiExplain() {
        if (checkedWords.isEmpty()) { Toast.makeText(this, "請先勾選要講解的單字！", Toast.LENGTH_SHORT).show(); return; }
        if (!AppState.getInstance().hasApiKey()) { Toast.makeText(this, "請先設定 API Key！", Toast.LENGTH_SHORT).show(); return; }
        Intent intent = new Intent(this, WordExplainActivity.class);
        intent.putStringArrayListExtra("WORDS_TO_EXPLAIN", new ArrayList<>(checkedWords));
        startActivity(intent);
    }

    private LinearLayout buildAddWrongWordSection() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(12, 12, 12, 8);
        section.setBackgroundColor(Color.parseColor("#E8F5E9"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 8);
        section.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("✏️ 手動新增錯題");
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#2E7D32"));
        tvTitle.setPadding(0, 0, 0, 8);
        section.addView(tvTitle);

        String[] typeLabels = {"名詞", "動詞", "副詞", "形容詞"};
        Spinner spinnerType = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, typeLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
        spinnerType.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        section.addView(spinnerType);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(0, 8, 0, 0);
        inputRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText etWord = new EditText(this);
        etWord.setHint("單字");
        etWord.setSingleLine(true);
        etWord.setInputType(InputType.TYPE_CLASS_TEXT);
        etWord.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
        inputRow.addView(etWord);

        EditText etStrength = new EditText(this);
        etStrength.setHint("強度");
        etStrength.setSingleLine(true);
        etStrength.setText("1");
        etStrength.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        sLp.setMarginStart(8);
        etStrength.setLayoutParams(sLp);
        inputRow.addView(etStrength);
        section.addView(inputRow);

        Button btnAdd = new Button(this);
        btnAdd.setText("+ 新增");
        btnAdd.setTextColor(Color.WHITE);
        btnAdd.setBackgroundColor(Color.parseColor("#43A047"));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 8, 0, 0);
        btnAdd.setLayoutParams(btnLp);
        btnAdd.setOnClickListener(v -> {
            String word = etWord.getText().toString().trim();
            if (word.isEmpty()) { Toast.makeText(this, "請輸入單字", Toast.LENGTH_SHORT).show(); return; }
            int strength = 1;
            try { strength = Integer.parseInt(etStrength.getText().toString().trim()); } catch (Exception ignored) {}
            int typeId = spinnerType.getSelectedItemPosition() + 1;
            state.errorTracker.addOrSetError(word, typeId, strength);
            state.errorTracker.save();
            etWord.setText("");
            etStrength.setText("1");
            Toast.makeText(this, "✅ 已新增「" + word + "」（強度: " + strength + "）", Toast.LENGTH_SHORT).show();
            renderCurrentTab();
        });
        section.addView(btnAdd);
        return section;
    }

    private void showAdjustStrengthDialog(WrongWord ww, int type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("調整「" + ww.word + "」的強度");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(p, p / 2, p, 0);

        TextView tvCurrent = new TextView(this);
        tvCurrent.setText("目前強度：" + ww.errorCount);
        tvCurrent.setTextSize(14f);
        tvCurrent.setPadding(0, 0, 0, 8);
        layout.addView(tvCurrent);

        EditText etNew = new EditText(this);
        etNew.setHint("輸入新強度（可為負值）");
        etNew.setText(String.valueOf(ww.errorCount));
        etNew.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        etNew.selectAll();
        layout.addView(etNew);

        TextView tvHint = new TextView(this);
        tvHint.setText("正值＝弱點強度，負值＝學習加成，0＝已掌握");
        tvHint.setTextSize(12f);
        tvHint.setTextColor(Color.parseColor("#757575"));
        tvHint.setPadding(0, 8, 0, 0);
        layout.addView(tvHint);

        builder.setView(layout);
        builder.setPositiveButton("確認調整", (dialog, which) -> {
            String input = etNew.getText().toString().trim();
            if (input.isEmpty()) return;
            try {
                int newStrength = Integer.parseInt(input);
                state.errorTracker.setStrength(ww.word, type, newStrength);
                state.errorTracker.save();
                Toast.makeText(this, "✅ 強度已調整為 " + newStrength, Toast.LENGTH_SHORT).show();
                renderCurrentTab();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "❌ 請輸入有效整數", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
}
