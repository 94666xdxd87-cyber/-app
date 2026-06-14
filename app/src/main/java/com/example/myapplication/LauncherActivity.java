package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.core.AppState;

/**
 * 入口頁面：兩個模式選擇按鈕
 *   ① 英文練習 → MainActivity
 *   ② 申論題   → essay.EssayMainActivity
 *
 * 在此載入 API Key，不論進入哪個模組都能正常呼叫 API。
 */
public class LauncherActivity extends AppCompatActivity {

    private AppState state;
    private Button   btnApiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        state = AppState.getInstance();
        state.initTracker(this);
        state.loadApiKey(this);   // ← 關鍵：在入口就載入，不再依賴進入哪個模組

        btnApiKey = findViewById(R.id.btnLauncherApiKey);
        updateApiKeyButton();
        btnApiKey.setOnClickListener(v -> showApiKeyDialog());

        findViewById(R.id.btnEnglish).setOnClickListener(v ->
                startActivity(new Intent(this, com.example.myapplication.english.MainActivity.class)));

        findViewById(R.id.btnEssay).setOnClickListener(v ->
                startActivity(new Intent(this, com.example.myapplication.essay.EssayMainActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 從其他頁面返回時同步按鈕狀態
        updateApiKeyButton();
    }

    private void updateApiKeyButton() {
        if (state.hasApiKey()) {
            String tail = state.getApiKey();
            tail = tail.substring(Math.max(0, tail.length() - 6));
            btnApiKey.setText("🔑 API Key ✅  ..."+tail);
            btnApiKey.setTextColor(0xFF2E7D32);
        } else {
            btnApiKey.setText("🔑 設定 API Key ⚠️");
            btnApiKey.setTextColor(0xFFE53935);
        }
    }

    private void showApiKeyDialog() {
        int p = (int)(20 * getResources().getDisplayMetrics().density);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(p, p / 2, p, 0);

        TextView tvHint = new TextView(this);
        tvHint.setText("請輸入您的 Google Gemini API Key。\n金鑰儲存在本機，不會上傳。");
        tvHint.setTextSize(13f);
        tvHint.setTextColor(0xFF757575);
        tvHint.setPadding(0, 0, 0, p / 2);
        layout.addView(tvHint);

        final EditText etKey = new EditText(this);
        etKey.setHint("AIzaSy...");
        etKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etKey.setSingleLine(true);
        if (state.hasApiKey()) etKey.setText(state.getApiKey());
        layout.addView(etKey);

        CheckBox cbShow = new CheckBox(this);
        cbShow.setText("顯示 API Key");
        cbShow.setOnCheckedChangeListener((btn, checked) -> {
            etKey.setInputType(checked
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etKey.setSelection(etKey.getText().length());
        });
        layout.addView(cbShow);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(state.hasApiKey()
                ? "✅ 已設定（末 6 碼：..."
                    + state.getApiKey().substring(Math.max(0, state.getApiKey().length() - 6)) + "）"
                : "⚠️ 尚未設定，無法呼叫 AI 功能。");
        tvStatus.setTextSize(12f);
        tvStatus.setTextColor(state.hasApiKey() ? 0xFF2E7D32 : 0xFFE53935);
        tvStatus.setPadding(0, p / 2, 0, 0);
        layout.addView(tvStatus);

        new AlertDialog.Builder(this)
                .setTitle("🔑 設定 Gemini API Key")
                .setView(layout)
                .setPositiveButton("儲存", (d, w) -> {
                    String key = etKey.getText().toString().trim();
                    if (key.isEmpty()) {
                        Toast.makeText(this, "❌ API Key 不能為空！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    state.setApiKey(this, key);
                    Toast.makeText(this, "✅ API Key 已儲存！", Toast.LENGTH_SHORT).show();
                    updateApiKeyButton();
                })
                .setNegativeButton("取消", null)
                .setNeutralButton(state.hasApiKey() ? "清除 Key" : null,
                        state.hasApiKey() ? (d, w) -> {
                            state.setApiKey(this, "");
                            Toast.makeText(this, "🗑️ API Key 已清除。", Toast.LENGTH_SHORT).show();
                            updateApiKeyButton();
                        } : null)
                .show();
    }
}
