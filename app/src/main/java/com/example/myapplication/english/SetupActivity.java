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
import java.util.*;

public class SetupActivity extends AppCompatActivity {

    private AppState state;
    private int mode;

    private TextView tvTitle, tvWeakStats, tvWeakCategoryLabel, tvError, tvLoadingMsg;
    private Spinner spinnerWeakCategory, spinnerModel;
    private LinearLayout layoutCustom, layoutLoading;
    private EditText etCount, etNounCount, etVerbCount, etAdvCount, etAdjCount;
    private TextView tvCustomWeakHint;
    private Button btnGenerate;

    private int[] weakCounts = new int[]{0, 0, 0, 0};

    private static final int WEAK_NOUN   = 1;
    private static final int WEAK_VERB   = 2;
    private static final int WEAK_ADV    = 3;
    private static final int WEAK_ADJ    = 4;
    private static final int WEAK_ALL    = 5;
    private static final int WEAK_CUSTOM = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        state = AppState.getInstance();
        mode  = state.currentMode;

        bindViews();
        configureForMode();
        setupModelSpinner();
    }

    private void bindViews() {
        tvTitle             = findViewById(R.id.tvSetupTitle);
        tvWeakStats         = findViewById(R.id.tvWeakStats);
        tvWeakCategoryLabel = findViewById(R.id.tvWeakCategoryLabel);
        tvError             = findViewById(R.id.tvError);
        tvLoadingMsg        = findViewById(R.id.tvLoadingMsg);
        spinnerWeakCategory = findViewById(R.id.spinnerWeakCategory);
        spinnerModel        = findViewById(R.id.spinnerModel);
        layoutCustom        = findViewById(R.id.layoutCustom);
        layoutLoading       = findViewById(R.id.layoutLoading);
        etCount             = findViewById(R.id.etCount);
        etNounCount         = findViewById(R.id.etNounCount);
        etVerbCount         = findViewById(R.id.etVerbCount);
        etAdvCount          = findViewById(R.id.etAdvCount);
        etAdjCount          = findViewById(R.id.etAdjCount);
        btnGenerate         = findViewById(R.id.btnGenerate);
        tvCustomWeakHint    = findViewById(R.id.tvCustomWeakHint);
        btnGenerate.setOnClickListener(v -> onGenerateTapped());
    }

    private void setupModelSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, state.modelDisplayNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(adapter);
        spinnerModel.setSelection(state.selectedModelIndex);
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { state.selectedModelIndex = pos; }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void configureForMode() {
        String[] titles = {"", "📘 名詞單獨練習", "📗 副詞單獨練習", "📙 形容詞單獨練習",
                "🎬 動詞單獨練習", "🎯 自訂比例混合模式", "🚨 弱點搶救模式"};
        tvTitle.setText(titles[mode]);

        if (mode == 5) {
            layoutCustom.setVisibility(View.VISIBLE);
            updateCustomHint(false);
        } else if (mode == 6) {
            tvWeakStats.setText(state.errorTracker.getWeakReportString());
            tvWeakStats.setVisibility(View.VISIBLE);
            tvWeakCategoryLabel.setVisibility(View.VISIBLE);
            spinnerWeakCategory.setVisibility(View.VISIBLE);
            String[] categories = {"名詞", "動詞", "副詞", "形容詞", "全部混合", "自訂比例混合"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, categories);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerWeakCategory.setAdapter(adapter);
            spinnerWeakCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    if (pos + 1 == WEAK_CUSTOM) { layoutCustom.setVisibility(View.VISIBLE); updateCustomHint(true); }
                    else layoutCustom.setVisibility(View.GONE);
                }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });
        }
    }

    private void updateCustomHint(boolean isWeakMode) {
        if (tvCustomWeakHint == null) return;
        tvCustomWeakHint.setText(isWeakMode
                ? "各詞性要從弱點字中出幾題（加總需等於總題數）"
                : "各詞性題數（加總需等於總題數）");
    }

    private void onGenerateTapped() {
        tvError.setVisibility(View.GONE);
        int count = parseCount(etCount);
        if (count <= 0) { showError("➤ 請輸入有效的題目數量"); return; }

        List<String> payloads = new ArrayList<>();
        int[] counts  = null;
        int[] typeIds = null;

        try {
            switch (mode) {
                case 1: payloads = safeGenerate(state.nounPool,      count, "名詞"); break;
                case 2: payloads = safeGenerate(state.adverbPool,    count, "副詞"); break;
                case 3: payloads = safeGenerate(state.adjectivePool, count, "形容詞"); break;
                case 4: payloads = safeGenerate(state.verbPool,      count, "動詞"); break;
                case 5: {
                    int nC = parseCount(etNounCount), vC = parseCount(etVerbCount),
                        aC = parseCount(etAdvCount),  adjC = parseCount(etAdjCount);
                    if (nC + vC + aC + adjC != count) { showError("❌ 分配總和不符！ (預計 " + count + " 題)"); return; }
                    List<String> nP   = safeGenerate(state.nounPool,      nC,   "名詞");
                    List<String> vP   = safeGenerate(state.verbPool,       vC,   "動詞");
                    List<String> aP   = safeGenerate(state.adverbPool,     aC,   "副詞");
                    List<String> adjP = safeGenerate(state.adjectivePool,  adjC, "形容詞");
                    payloads.addAll(nP); payloads.addAll(vP); payloads.addAll(aP); payloads.addAll(adjP);
                    counts  = new int[]{nP.size(), vP.size(), aP.size(), adjP.size()};
                    typeIds = new int[]{1, 2, 3, 4};
                    break;
                }
                case 6: {
                    int weakChoice = spinnerWeakCategory.getSelectedItemPosition() + 1;
                    payloads = buildWeakPayloads(weakChoice, count);
                    if (weakChoice <= 4) { counts = new int[]{payloads.size()}; typeIds = new int[]{weakChoice}; }
                    else { counts = weakCounts; typeIds = new int[]{1, 2, 3, 4}; }
                    break;
                }
            }
        } catch (Exception e) { showError("❌ 系統發生錯誤: " + e.getMessage()); return; }

        if (payloads.isEmpty()) { showError("❌ 無法生成題目，請確認字庫庫存或弱點字數量。"); return; }
        startApiCall(payloads, counts, typeIds);
    }

    private List<String> buildWeakPayloads(int weakChoice, int count) {
        switch (weakChoice) {
            case WEAK_NOUN: case WEAK_VERB: case WEAK_ADV: case WEAK_ADJ: {
                List<WrongWord> weakWords = state.errorTracker.getWeakWordsSorted(weakChoice);
                if (weakWords.isEmpty()) { showError("⚠️ 該詞性目前沒有弱點字紀錄！"); return new ArrayList<>(); }
                return PayloadGenerator.generateFromWeak(weakWords, getPoolByTypeId(weakChoice), count);
            }
            case WEAK_ALL: {
                List<ErrorTracker.WeakEntry> allWeak = state.errorTracker.getAllWeakWordsSortedByCount();
                if (allWeak.isEmpty()) { showError("⚠️ 目前沒有任何弱點字紀錄！"); return new ArrayList<>(); }
                return buildMixedWeakPayloads(allWeak, count);
            }
            case WEAK_CUSTOM: {
                int nC = parseCount(etNounCount), vC = parseCount(etVerbCount),
                    aC = parseCount(etAdvCount), adjC = parseCount(etAdjCount);
                if (nC + vC + aC + adjC != count) { showError("❌ 弱點自訂分配總和不符！"); return new ArrayList<>(); }
                List<String> nP   = buildSingleWeakPayloads(1, nC);
                List<String> vP   = buildSingleWeakPayloads(2, vC);
                List<String> aP   = buildSingleWeakPayloads(3, aC);
                List<String> adjP = buildSingleWeakPayloads(4, adjC);
                weakCounts = new int[]{nP.size(), vP.size(), aP.size(), adjP.size()};
                List<String> all = new ArrayList<>();
                all.addAll(nP); all.addAll(vP); all.addAll(aP); all.addAll(adjP);
                return all;
            }
            default: return new ArrayList<>();
        }
    }

    private List<String> buildSingleWeakPayloads(int typeId, int count) {
        if (count <= 0) return new ArrayList<>();
        List<WrongWord> weakWords = state.errorTracker.getWeakWordsSorted(typeId);
        if (weakWords.isEmpty()) return new ArrayList<>();
        return PayloadGenerator.generateFromWeak(weakWords, getPoolByTypeId(typeId), count);
    }

    private List<String> buildMixedWeakPayloads(List<ErrorTracker.WeakEntry> allWeak, int count) {
        List<String> nW = new ArrayList<>(), vW = new ArrayList<>(), aW = new ArrayList<>(), adjW = new ArrayList<>();
        int taken = 0;
        for (ErrorTracker.WeakEntry e : allWeak) {
            if (taken >= count) break;
            switch (e.type) { case 1: nW.add(e.word); break; case 2: vW.add(e.word); break; case 3: aW.add(e.word); break; case 4: adjW.add(e.word); break; }
            taken++;
        }
        List<String> nP   = PayloadGenerator.generateFromWeak(toWWList(nW),   state.nounPool,      nW.size());
        List<String> vP   = PayloadGenerator.generateFromWeak(toWWList(vW),   state.verbPool,      vW.size());
        List<String> aP   = PayloadGenerator.generateFromWeak(toWWList(aW),   state.adverbPool,    aW.size());
        List<String> adjP = PayloadGenerator.generateFromWeak(toWWList(adjW), state.adjectivePool, adjW.size());
        weakCounts = new int[]{nP.size(), vP.size(), aP.size(), adjP.size()};
        List<String> all = new ArrayList<>();
        all.addAll(nP); all.addAll(vP); all.addAll(aP); all.addAll(adjP);
        return all;
    }

    private List<String> getPoolByTypeId(int typeId) {
        switch (typeId) { case 1: return state.nounPool; case 2: return state.verbPool; case 3: return state.adverbPool; case 4: return state.adjectivePool; default: return new ArrayList<>(); }
    }

    private void startApiCall(List<String> payloads, final int[] counts, final int[] typeIds) {
        btnGenerate.setEnabled(false);
        layoutLoading.setVisibility(View.VISIBLE);
        GeminiService.callAsync(payloads, new Handler(Looper.getMainLooper()), new GeminiService.Callback() {
            @Override public void onSuccess(String response) {
                List<Quiz> quizList;
                if (counts != null) quizList = QuizParser.parseWithTypeRanges(response, counts, typeIds);
                else { int type = (mode==1)?1:(mode==2)?3:(mode==3)?4:(mode==4)?2:0; quizList = QuizParser.parse(response, type); }
                if (quizList.isEmpty()) { onError("解析結果為空"); return; }
                for (int i = 0; i < quizList.size(); i++) quizList.get(i).id = i + 1;
                state.currentQuizList = quizList;
                startActivity(new Intent(SetupActivity.this, QuizActivity.class));
                finish();
            }
            @Override public void onError(String errorMessage) {
                layoutLoading.setVisibility(View.GONE);
                btnGenerate.setEnabled(true);
                showError("❌ 無法生成題目：" + errorMessage);
            }
            @Override public void onRetry(int attempt) { tvLoadingMsg.setText("第 " + attempt + " 次重試中..."); }
        });
    }

    private List<WrongWord> toWWList(List<String> words) {
        List<WrongWord> result = new ArrayList<>();
        for (String w : words) { WrongWord ww = new WrongWord(w); ww.errorCount = 1; result.add(ww); }
        return result;
    }

    private List<String> safeGenerate(List<String> pool, int count, String label) {
        if (count <= 0) return new ArrayList<>();
        int max = pool.size() / 4;
        if (count > max) count = max;
        if (count == 0) return new ArrayList<>();
        return PayloadGenerator.generate(pool, count);
    }

    private int parseCount(EditText et) {
        try { return Integer.parseInt(et.getText().toString().trim()); } catch (Exception e) { return 0; }
    }

    private void showError(String msg) { tvError.setText(msg); tvError.setVisibility(View.VISIBLE); }
}
