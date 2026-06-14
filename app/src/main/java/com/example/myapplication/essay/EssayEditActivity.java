package com.example.myapplication.essay;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.core.AppState;
import com.example.myapplication.core.GeminiService;

/**
 * 申論題編輯頁面 — 4 步驟 AI 評分流程
 *
 * Step 1：複製 Prompt A（帶題目 → 讓 AI 建立評分標準）
 * Step 2：複製答案（讓 AI 批改你的作答）
 * Step 3：複製 Prompt B（讓 AI 輸出結構化結果）
 * Step 4：貼入 Prompt B 的回覆 → 按「解析並儲存」
 */
public class EssayEditActivity extends AppCompatActivity {

    public static final String EXTRA_EDITING_ID       = "editing_id";
    public static final String EXTRA_SOURCE_QUESTION  = "source_question_id";

    private EditText etSubject, etQuestion, etUserAnswer, etAiReply;
    private Button   btnCopyPromptA, btnCopyAnswer, btnCopyPromptB;
    private Button   btnOpenGemini, btnOpenChatGpt;
    private Button   btnParseAndSave;
    private Button   btnAutoGrade;        // API 自動批改
    private ProgressBar pbAutoGrade;      // 謊待中動畫
    private TextView tvStatus, tvParsedPreview, tvAutoGradeStatus;

    private String editingId       = null;
    private String sourceQuestionId = "";

    // ── Prompt B 的固定格式指令 ──────────────────────────────────
    private static final String PROMPT_B =
            "請將剛才的批改結果，嚴格依照以下格式重新輸出，不得增減欄位或改變格式：\n\n" +
            "===RESULT_START===\n" +
            "TOTAL_SCORE: {數字}\n" +
            "---POINTS_START---\n" +
            "POINT_NAME: {要點名稱}\n" +
            "POINT_FULL_SCORE: {該要點滿分}\n" +
            "POINT_SCORE: {該考生得分}\n" +
            "POINT_REASON: {給分理由，說明有無達到基礎/滿分條件}\n" +
            "---\n" +
            "POINT_NAME: {要點名稱}\n" +
            "POINT_FULL_SCORE: {該要點滿分}\n" +
            "POINT_SCORE: {該考生得分}\n" +
            "POINT_REASON: {給分理由，說明有無達到基礎/滿分條件}\n" +
            "---POINTS_END---\n" +
            "OVERALL_COMMENT: {總評，2~3句綜合描述該考生表現}\n" +
            "---WEAKNESS_START---\n" +
            "- {不足之處}\n" +
            "- {不足之處}\n" +
            "---WEAKNESS_END---\n" +
            "===RESULT_END===";

    // ── Prompt C — 一次性花全延倸透過 API 直接回傳結構化結果 ────────
    // 格式：{PROMPT_C_TEMPLATE}題目:{question}\n考生答案:{answer}
    private static final String PROMPT_C_TEMPLATE =
            "你是一名台灣國家考試「資通網路」科目的申論題閱卷委員，具備豐富的命題與評分經驗。\n" +
            "【評分原則】 請依照以下分層標準進行評分，嚴格但公正：\n" +
            " - 基礎給分（該要點約8成分數）：\n" +
            "   概念正確、內容完整、符合題目要求即可獲得\n" +
            " - 滿分條件（該要點完整分數）：\n" +
            "   除基礎內容外，還須提及該領域專業人員才熟悉的\n" +
            "   術語、技術細節或進階概念，方可拿滿\n\n" +
            "【第一階段 — 建立評分標準】\n" +
            "我會提供一道申論題與配分，請你：\n" +
            "1. 條列出本題的核心得分要點（每個要點請簡述應涵蓋的內容）\n" +
            "2. 為每個要點標註建議配分，總分須與題目配分相符\n" +
            "3. 每個要點請分別說明：\n" +
            "   - 基礎給分條件（可獲得約8成分數的內容）\n" +
            "   - 滿分條件（需額外提及的專業術語或進階細節）\n\n" +
            "【第二階段 — 評閱考生答案】\n" +
            "完成評分標準後，我會提供個別考生的解答，\n" +
            "請針對每個得分要點說明：\n" +
            " - 該考生實際得分\n" +
            " - 給分理由（有無達到基礎/滿分條件）\n\n" +
            "請回傳剛剛的評分標準，將剛才的批改結果，嚴格依照以下格式重新輸出，不得增減欄位或改變格式：\n\n" +
            "---RUBRIC_START---\n" +
            "RUBRIC_ITEM: {得分要點名稱}\n" +
            "RUBRIC_FULL_SCORE: {該要點配分}\n" +
            "RUBRIC_BASE: {基礎給分條件}\n" +
            "RUBRIC_PERFECT: {滿分條件}\n" +
            "---\n" +
            "RUBRIC_ITEM: {得分要點名稱}\n" +
            "RUBRIC_FULL_SCORE: {該要點配分}\n" +
            "RUBRIC_BASE: {基礎給分條件}\n" +
            "RUBRIC_PERFECT: {滿分條件}\n" +
            "---RUBRIC_END---\n\n" +
            "===RESULT_START===\n" +
            "TOTAL_SCORE: {數字}\n" +
            "---POINTS_START---\n" +
            "POINT_NAME: {要點名稱}\n" +
            "POINT_FULL_SCORE: {該要點滿分}\n" +
            "POINT_SCORE: {該考生得分}\n" +
            "POINT_REASON: {給分理由，說明有無達到基礎/滿分條件}\n" +
            "---\n" +
            "POINT_NAME: {要點名稱}\n" +
            "POINT_FULL_SCORE: {該要點滿分}\n" +
            "POINT_SCORE: {該考生得分}\n" +
            "POINT_REASON: {給分理由，說明有無達到基礎/滿分條件}\n" +
            "---POINTS_END---\n" +
            "OVERALL_COMMENT: {總評，2~3句綜合描述該考生表現}\n" +
            "---WEAKNESS_START---\n" +
            "- {不足之處}\n" +
            "- {不足之處}\n" +
            "---WEAKNESS_END---\n" +
            "===RESULT_END===\n\n" +
            "題目：\n{QUESTION}\n\n" +
            "考生答案：\n{ANSWER}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_essay_edit);

        bindViews();
        setupBackButton();

        editingId        = getIntent().getStringExtra(EXTRA_EDITING_ID);
        sourceQuestionId = getIntent().getStringExtra(EXTRA_SOURCE_QUESTION);
        if (sourceQuestionId == null) sourceQuestionId = "";

        // 若帶入考古題 id，自動填入題目
        if (!sourceQuestionId.isEmpty() && editingId == null) {
            PastExamQuestion pq = PastExamManager.findQuestionById(this, sourceQuestionId);
            if (pq != null) {
                etSubject.setText(pq.subject);
                etQuestion.setText(pq.question);
            }
        }

        if (editingId != null) loadExistingRecord();

        setupStep1Button();
        setupStep2Button();
        setupStep3Button();
        setupOpenAiButtons();
        setupParseAndSaveButton();
        setupAutoGradeButton();
        setupAiReplyWatcher();
    }

    // ── 綁定 View ────────────────────────────────────────────────

    private void bindViews() {
        etSubject       = findViewById(R.id.etEssaySubject);
        etQuestion      = findViewById(R.id.etEssayQuestion);
        etUserAnswer    = findViewById(R.id.etEssayUserAnswer);
        etAiReply       = findViewById(R.id.etEssayAiReply);
        btnCopyPromptA  = findViewById(R.id.btnCopyPromptA);
        btnCopyAnswer   = findViewById(R.id.btnCopyAnswer);
        btnCopyPromptB  = findViewById(R.id.btnCopyPromptB);
        btnOpenGemini   = findViewById(R.id.btnOpenGemini);
        btnOpenChatGpt  = findViewById(R.id.btnOpenChatGpt);
        btnParseAndSave = findViewById(R.id.btnParseAndSave);
        btnAutoGrade    = findViewById(R.id.btnAutoGrade);
        pbAutoGrade     = findViewById(R.id.pbAutoGrade);
        tvStatus        = findViewById(R.id.tvEssayEditStatus);
        tvParsedPreview = findViewById(R.id.tvParsedPreview);
    }

    private void setupBackButton() {
        View btnBack = findViewById(R.id.btnEssayEditBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> confirmDiscard());
    }

    // ── 載入既有紀錄（編輯模式）─────────────────────────────────

    private void loadExistingRecord() {
        EssayRecord rec = EssayManager.findById(this, editingId);
        if (rec == null) { finish(); return; }
        etSubject.setText(rec.subject);
        etQuestion.setText(rec.question);
        etUserAnswer.setText(rec.userAnswer);
        etAiReply.setText(rec.aiReply);
        if (!rec.sourceQuestionId.isEmpty()) sourceQuestionId = rec.sourceQuestionId;

        // 若已有解析結果，顯示預覽
        if (rec.hasParsedResult()) {
            showParsedPreview(rec.parsedOverallComment, rec.parsedTotalScore, rec.parsedWeaknesses);
        }
        updateStatus(rec);
    }

    // ── Step 1：複製 Prompt A（題目 → AI 建立評分標準）──────────

    private void setupStep1Button() {
        btnCopyPromptA.setOnClickListener(v -> {
            String question = etQuestion.getText().toString().trim();
            if (question.isEmpty()) {
                Toast.makeText(this, "請先填寫題目內容！", Toast.LENGTH_SHORT).show();
                return;
            }
            String subject  = etSubject.getText().toString().trim();
            String promptA =
                    "你是一名台灣國家考試「資通網路」科目的申論題閱卷委員，" +
                    " 具備豐富的命題與評分經驗。\n\n" +
                    "【評分原則】 請依照以下分層標準進行評分，嚴格但公正：\n" +
                    " - 基礎給分（該要點約8成分數）：\n" +
                    "   概念正確、內容完整、符合題目要求即可獲得\n" +
                    " - 滿分條件（該要點完整分數）：\n" +
                    "   除基礎內容外，還須提及該領域專業人員才熟悉的\n" +
                    "   術語、技術細節或進階概念，方可拿滿\n\n" +
                    "【第一階段 — 建立評分標準】\n" +
                    "我會提供一道申論題與配分，請你：\n" +
                    "1. 條列出本題的核心得分要點（每個要點請簡述應涵蓋的內容）\n" +
                    "2. 為每個要點標註建議配分，總分須與題目配分相符\n" +
                    "3. 每個要點請分別說明：\n" +
                    "   - 基礎給分條件（可獲得約8成分數的內容）\n" +
                    "   - 滿分條件（需額外提及的專業術語或進階細節）\n\n" +
                    "【第二階段 — 評閱考生答案】\n" +
                    "完成評分標準後，我會提供個別考生的解答，\n" +
                    "請針對每個得分要點說明：\n" +
                    " - 該考生實際得分\n" +
                    " - 給分理由（有無達到基礎/滿分條件）\n\n" +
                    "---\n\n" +
                    "題目：\n" + question;
            copyToClipboard("Prompt A", promptA);
            setStatus("✅ Prompt A 已複製！請貼給 AI，待 AI 建立標準後繼續 Step 2。", 0xFF00695C);
        });
    }

    // ── Step 2：複製答案 ─────────────────────────────────────────

    private void setupStep2Button() {
        btnCopyAnswer.setOnClickListener(v -> {
            String answer = etUserAnswer.getText().toString().trim();
            if (answer.isEmpty()) {
                Toast.makeText(this, "請先填寫你的回答！", Toast.LENGTH_SHORT).show();
                return;
            }
            String text = "【申論題評分任務 — 第二步：批改作答】\n\n" +
                    "以下是學生的回答，請依據你剛才建立的評分標準進行批改：\n\n" +
                    answer;
            copyToClipboard("答案", text);
            setStatus("✅ 答案已複製！請貼給 AI，待 AI 批改完後繼續 Step 3。", 0xFFE65100);
        });
    }

    // ── Step 3：複製 Prompt B（取得結構化結果）───────────────────

    private void setupStep3Button() {
        btnCopyPromptB.setOnClickListener(v -> {
            copyToClipboard("Prompt B", PROMPT_B);
            setStatus("✅ Prompt B 已複製！請貼給 AI，然後將 AI 的回覆複製到下方 Step 4 欄位。", 0xFF6A1B9A);
        });
    }

    // ── 開啟 AI 按鈕 ─────────────────────────────────────────────

    private void setupOpenAiButtons() {
        btnOpenGemini.setOnClickListener(v -> openUrl("https://gemini.google.com"));
        btnOpenChatGpt.setOnClickListener(v -> openUrl("https://chat.openai.com"));
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "無法開啟瀏覽器", Toast.LENGTH_SHORT).show();
        }
    }

    // ── API 自動批改（Prompt C）────────────────────────────────

    private void setupAutoGradeButton() {
        btnAutoGrade.setOnClickListener(v -> startAutoGrade());
    }

    private void startAutoGrade() {
        String question   = etQuestion.getText().toString().trim();
        String userAnswer = etUserAnswer.getText().toString().trim();

        if (question.isEmpty()) {
            Toast.makeText(this, "❌ 請先填寫題目內容", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userAnswer.isEmpty()) {
            Toast.makeText(this, "❌ 請先填寫你的回答", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!AppState.getInstance().hasApiKey()) {
            Toast.makeText(this, "❌ 尚未設定 API Key，請至主選單設定", Toast.LENGTH_LONG).show();
            return;
        }

        // 組出 Prompt C（帶入題目與答案）
        String prompt = PROMPT_C_TEMPLATE
                .replace("{QUESTION}", question)
                .replace("{ANSWER}", userAnswer);

        // UI 進入等待狀態
        setAutoGradeLoading(true);
        setStatus("⏳ AI 批改中，請稍候…", 0xFF1565C0);

        GeminiService.callRawAsync(prompt, new Handler(Looper.getMainLooper()),
                new GeminiService.Callback() {
                    @Override
                    public void onSuccess(String response) {
                        setAutoGradeLoading(false);
                        handleAutoGradeResponse(response);
                    }
                    @Override
                    public void onError(String errorMessage) {
                        setAutoGradeLoading(false);
                        setStatus("❌ API 呼叫失敗：" + errorMessage, 0xFFC62828);
                        Toast.makeText(EssayEditActivity.this,
                                "批改失敗：" + errorMessage, Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onRetry(int attempt) {
                        setStatus("🔄 重試第 " + attempt + " 次…", 0xFFE65100);
                    }
                });
    }

    private void handleAutoGradeResponse(String response) {
        // 把 API 回覆自動填入 etAiReply 欄位
        etAiReply.setText(response);

        // 嘗試解析
        EssayResultParser.ParsedResult parsed = EssayResultParser.parse(response);
        if (parsed.isValid) {
            setStatus("✅ AI 批改完成！總分：" + parsed.totalScore + "　正在儲存…", 0xFF2E7D32);
            showParsedPreview(parsed.overallComment, parsed.totalScore, parsed.weaknesses);

            // 自動儲存
            String subject    = etSubject.getText().toString().trim();
            String question   = etQuestion.getText().toString().trim();
            String userAnswer = etUserAnswer.getText().toString().trim();
            if (subject.isEmpty()) subject = "未分類";
            doSave(subject, question, userAnswer, response, parsed);
        } else {
            // 解析失敗但 AI 有回傳內容 — 填入欄位讓使用者手動確認
            setStatus("⚠️ API 回覆已填入，但格式解析失敗（" + parsed.errorMsg + "）\n可手動按「解析並儲存」", 0xFFE65100);
            Toast.makeText(this, "格式解析失敗，請手動按「解析並儲存」", Toast.LENGTH_LONG).show();
        }
    }

    private void setAutoGradeLoading(boolean loading) {
        btnAutoGrade.setEnabled(!loading);
        btnAutoGrade.setAlpha(loading ? 0.5f : 1.0f);
        pbAutoGrade.setVisibility(loading ? View.VISIBLE : View.GONE);
        // 同時禁用其他按鈕，防止重複操作
        btnParseAndSave.setEnabled(!loading);
        btnCopyPromptA.setEnabled(!loading);
        btnCopyAnswer.setEnabled(!loading);
        btnCopyPromptB.setEnabled(!loading);
    }

    // ── Step 4：AI 回覆輸入框監聽 ────────────────────────────────

    private void setupAiReplyWatcher() {
        etAiReply.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                boolean hasContent = s.length() > 0;
                btnParseAndSave.setAlpha(hasContent ? 1.0f : 0.5f);
            }
        });
    }

    // ── 解析並儲存 ────────────────────────────────────────────────

    private void setupParseAndSaveButton() {
        btnParseAndSave.setOnClickListener(v -> tryParseAndSave());
    }

    private void tryParseAndSave() {
        String subject    = etSubject.getText().toString().trim();
        String question   = etQuestion.getText().toString().trim();
        String userAnswer = etUserAnswer.getText().toString().trim();
        String aiReply    = etAiReply.getText().toString().trim();

        if (subject.isEmpty()) subject = "未分類";
        if (question.isEmpty()) {
            Toast.makeText(this, "❌ 請填寫題目內容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 嘗試解析 AI 回覆
        EssayResultParser.ParsedResult parsed = null;
        if (!aiReply.isEmpty()) {
            parsed = EssayResultParser.parse(aiReply);
            if (!parsed.isValid) {
                // 解析失敗，詢問是否仍要儲存
                final String finalSubject    = subject;
                final String finalQuestion   = question;
                final String finalUserAnswer = userAnswer;
                new AlertDialog.Builder(this)
                        .setTitle("⚠️ 解析失敗")
                        .setMessage("AI 回覆格式不符合 Prompt B 的結構。\n\n" +
                                "錯誤：" + parsed.errorMsg + "\n\n" +
                                "仍要儲存原始回覆（不解析）？")
                        .setPositiveButton("儲存原始回覆", (d, w) ->
                                doSave(finalSubject, finalQuestion, finalUserAnswer, aiReply, null))
                        .setNegativeButton("重新貼上", null)
                        .show();
                return;
            }
        }

        doSave(subject, question, userAnswer, aiReply, parsed);
    }

    private void doSave(String subject, String question, String userAnswer,
                        String aiReply, EssayResultParser.ParsedResult parsed) {
        if (editingId == null) {
            // 新增
            EssayRecord rec = new EssayRecord(
                    EssayManager.generateId(),
                    EssayManager.currentTimestamp(),
                    subject, question, userAnswer, aiReply);
            rec.sourceQuestionId = sourceQuestionId;
            if (parsed != null && parsed.isValid) applyParsed(rec, parsed);
            EssayManager.save(this, rec);
            Toast.makeText(this, "✅ 紀錄已儲存！", Toast.LENGTH_SHORT).show();
        } else {
            // 更新
            EssayRecord rec = EssayManager.findById(this, editingId);
            if (rec == null) { Toast.makeText(this, "找不到原紀錄", Toast.LENGTH_SHORT).show(); return; }
            rec.subject    = subject;
            rec.question   = question;
            rec.userAnswer = userAnswer;
            rec.aiReply    = aiReply;
            rec.sourceQuestionId = sourceQuestionId;
            if (parsed != null && parsed.isValid) applyParsed(rec, parsed);
            EssayManager.update(this, rec);
            Toast.makeText(this, "✅ 紀錄已更新！", Toast.LENGTH_SHORT).show();
        }

        // 顯示解析預覽（若有）
        if (parsed != null && parsed.isValid) {
            showParsedPreview(parsed.overallComment, parsed.totalScore, parsed.weaknesses);
            // 短暫顯示後離開
            tvParsedPreview.postDelayed(() -> {
                setResult(RESULT_OK);
                finish();
            }, 1800);
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void applyParsed(EssayRecord rec, EssayResultParser.ParsedResult parsed) {
        rec.parsedTotalScore      = parsed.totalScore;
        rec.parsedOverallComment  = parsed.overallComment != null ? parsed.overallComment : "";
        rec.parsedWeaknesses      = parsed.weaknesses     != null ? parsed.weaknesses     : "";
    }

    private void showParsedPreview(String comment, int score, String weaknesses) {
        StringBuilder sb = new StringBuilder();
        sb.append("🏆 解析成功！總分：").append(score).append("\n");
        if (comment != null && !comment.isEmpty())
            sb.append("📝 ").append(comment).append("\n");
        if (weaknesses != null && !weaknesses.isEmpty())
            sb.append("⚠️ 待改進：\n").append(weaknesses);
        tvParsedPreview.setText(sb.toString().trim());
        tvParsedPreview.setVisibility(View.VISIBLE);
    }

    // ── 工具方法 ──────────────────────────────────────────────────

    private void copyToClipboard(String label, String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, text));
    }

    private void setStatus(String msg, int color) {
        tvStatus.setText(msg);
        tvStatus.setTextColor(color);
        tvStatus.setVisibility(View.VISIBLE);
    }

    private void updateStatus(EssayRecord rec) {
        if (rec.hasParsedResult()) {
            setStatus("✅ 此紀錄已解析 — 總分：" + rec.parsedTotalScore, 0xFF2E7D32);
        } else if (rec.hasAiReply()) {
            setStatus("✅ 已有 AI 回覆（尚未解析結構化結果）", 0xFF2E7D32);
        } else if (rec.hasUserAnswer()) {
            setStatus("⏳ 已填答案，尚未貼入 AI 回覆", 0xFFF57F17);
        } else {
            setStatus("📝 編輯中", 0xFF616161);
        }
    }

    // ── 離開確認 ─────────────────────────────────────────────────

    private void confirmDiscard() {
        new AlertDialog.Builder(this)
                .setTitle("放棄變更？")
                .setMessage("尚未儲存的內容將會遺失。")
                .setPositiveButton("放棄", (d, w) -> finish())
                .setNegativeButton("繼續編輯", null)
                .show();
    }

    @Override
    public void onBackPressed() { confirmDiscard(); }
}
