package com.example.myapplication.english;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.core.*;

public class MainActivity extends AppCompatActivity {

    private TextView tvPoolStats;
    private AppState state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        state = AppState.getInstance();
        state.initTracker(this);
        state.loadApiKey(this);

        tvPoolStats = findViewById(R.id.tvPoolStats);

        setupModeButtons();
        setupToolButtons();
        setupOnlineExamButton();
        loadWordPools();

        if (!state.hasApiKey()) showApiKeyDialog(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!state.poolsLoaded) {
            loadWordPools();
        } else {
            tvPoolStats.setText(
                    "📘 名詞 " + state.nounPool.size() + " 字  "
                    + "🎬 動詞 " + state.verbPool.size() + " 字  "
                    + "📗 副詞 " + state.adverbPool.size() + " 字  "
                    + "📙 形容詞 " + state.adjectivePool.size() + " 字");
        }
    }

    private void setupModeButtons() {
        int[] ids   = {R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn6, R.id.btn7};
        int[] modes = {1, 2, 3, 4, 5, 6};
        for (int i = 0; i < ids.length; i++) {
            final int mode = modes[i];
            Button btn = findViewById(ids[i]);
            if (btn != null) btn.setOnClickListener(v -> launchSetup(mode));
        }
    }

    private void setupToolButtons() {
        android.view.View v;
        v = findViewById(R.id.btnWordBank);
        if (v != null) v.setOnClickListener(x -> startActivity(new Intent(this, WordBankActivity.class)));
        v = findViewById(R.id.btnHistory);
        if (v != null) v.setOnClickListener(x -> startActivity(new Intent(this, HistoryActivity.class)));
        v = findViewById(R.id.btnApiKey);
        if (v != null) v.setOnClickListener(x -> showApiKeyDialog(false));
        v = findViewById(R.id.btnWordExplain);
        if (v != null) v.setOnClickListener(x -> startActivity(new Intent(this, WordExplainActivity.class)));
    }

    private void setupOnlineExamButton() {
        Button btn = findViewById(R.id.btnOnlineExam);
        if (btn != null) btn.setOnClickListener(v -> showIpInputDialog());
    }

    private void showApiKeyDialog(boolean isFirstLaunch) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔑 設定 Gemini API Key");
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(p, p / 2, p, 0);
        TextView tvHint = new TextView(this);
        tvHint.setText("請輸入您的 Google Gemini API Key。\n金鑰儲存在本機，不會上傳。");
        tvHint.setTextSize(13f); tvHint.setTextColor(0xFF757575);
        tvHint.setPadding(0, 0, 0, p / 2); layout.addView(tvHint);
        final EditText etApiKey = new EditText(this);
        etApiKey.setHint("AIzaSy...");
        etApiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etApiKey.setSingleLine(true);
        if (state.hasApiKey()) etApiKey.setText(state.getApiKey());
        layout.addView(etApiKey);
        CheckBox cbShow = new CheckBox(this);
        cbShow.setText("顯示 API Key");
        cbShow.setOnCheckedChangeListener((btn, checked) -> {
            etApiKey.setInputType(checked
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etApiKey.setSelection(etApiKey.getText().length());
        });
        layout.addView(cbShow);
        TextView tvStatus = new TextView(this);
        tvStatus.setText(state.hasApiKey()
                ? "✅ 已設定（末 6 碼：..." + state.getApiKey().substring(Math.max(0, state.getApiKey().length() - 6)) + "）"
                : "⚠️ 尚未設定 API Key，無法出題。");
        tvStatus.setTextSize(12f);
        tvStatus.setTextColor(state.hasApiKey() ? 0xFF2E7D32 : 0xFFE53935);
        tvStatus.setPadding(0, p / 2, 0, 0); layout.addView(tvStatus);
        builder.setView(layout);
        builder.setPositiveButton("儲存", (d, w) -> {
            String key = etApiKey.getText().toString().trim();
            if (key.isEmpty()) { Toast.makeText(this, "❌ API Key 不能為空！", Toast.LENGTH_SHORT).show(); return; }
            state.setApiKey(this, key);
            Toast.makeText(this, "✅ API Key 已儲存！", Toast.LENGTH_SHORT).show();
            updateApiKeyButtonAppearance();
        });
        if (isFirstLaunch) {
            builder.setNegativeButton("稍後再說", (d, w) ->
                    Toast.makeText(this, "⚠️ 未設定 API Key，出題功能無法使用。", Toast.LENGTH_LONG).show());
        } else {
            builder.setNegativeButton("取消", null);
            if (state.hasApiKey())
                builder.setNeutralButton("清除 Key", (d, w) -> {
                    state.setApiKey(this, "");
                    Toast.makeText(this, "🗑️ API Key 已清除。", Toast.LENGTH_SHORT).show();
                    updateApiKeyButtonAppearance();
                });
        }
        builder.setCancelable(!isFirstLaunch);
        builder.show();
    }

    private void updateApiKeyButtonAppearance() {
        android.view.View v = findViewById(R.id.btnApiKey);
        if (v instanceof Button) {
            Button btn = (Button) v;
            btn.setText(state.hasApiKey() ? "🔑 API Key ✅" : "🔑 設定 API Key ⚠️");
            btn.setTextColor(state.hasApiKey() ? 0xFF2E7D32 : 0xFFE53935);
        }
    }

    private void showIpInputDialog() {
        final EditText etIp = new EditText(this);
        etIp.setHint("輸入 Server IP（例如 192.168.1.10）");
        etIp.setText("192.168.1.1");
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        etIp.setPadding(p, p, p, p);
        new AlertDialog.Builder(this)
                .setTitle("連接模擬考伺服器").setView(etIp)
                .setPositiveButton("連線", (d, w) -> {
                    String ip = etIp.getText().toString().trim();
                    if (ip.isEmpty()) { Toast.makeText(this, "請輸入 IP", Toast.LENGTH_SHORT).show(); return; }
                    connectToServer(ip);
                })
                .setNegativeButton("取消", null).show();
    }

    private void connectToServer(String ip) {
        Toast.makeText(this, "正在連線至 " + ip + "...", Toast.LENGTH_SHORT).show();
        ExamClient.getInstance().connect(ip, 6000, new ExamClient.ConnectCallback() {
            @Override public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "連線成功！", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, OnlineExamActivity.class));
                });
            }
            @Override public void onFailure(String error) {
                runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                        .setTitle("連線失敗")
                        .setMessage("無法連線，請確認 IP 或網路。\n" + error)
                        .setPositiveButton("確定", null).show());
            }
        });
    }

    private void launchSetup(int mode) {
        if (!state.poolsLoaded) { Toast.makeText(this, "⏳ 單字庫尚未載入，請稍候...", Toast.LENGTH_SHORT).show(); return; }
        if (!state.hasApiKey()) { Toast.makeText(this, "請先設定 API Key！", Toast.LENGTH_SHORT).show(); showApiKeyDialog(false); return; }
        state.currentMode = mode;
        state.currentModeName = AppState.getModeNameFor(mode);
        startActivity(new Intent(this, SetupActivity.class));
    }

    private void loadWordPools() {
        tvPoolStats.setText("⏳ 載入單字庫中...");
        new Thread(() -> {
            try {
                state.nounPool      = FileLoader.loadNouns(this);
                state.verbPool      = FileLoader.loadVerbs(this);
                state.adverbPool    = FileLoader.loadAdverbs(this);
                state.adjectivePool = FileLoader.loadAdjectives(this);
                state.poolsLoaded   = true;
                state.rebuildTries();
                String stats = "📘 名詞 " + state.nounPool.size() + " 字  "
                        + "🎬 動詞 " + state.verbPool.size() + " 字  "
                        + "📗 副詞 " + state.adverbPool.size() + " 字  "
                        + "📙 形容詞 " + state.adjectivePool.size() + " 字";
                new Handler(Looper.getMainLooper()).post(() -> {
                    tvPoolStats.setText(stats);
                    updateApiKeyButtonAppearance();
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        tvPoolStats.setText("❌ 載入失敗：" + e.getMessage()));
            }
        }).start();
    }
}
