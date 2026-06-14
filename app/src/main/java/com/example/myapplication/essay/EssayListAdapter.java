package com.example.myapplication.essay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.example.myapplication.R;
import java.util.ArrayList;
import java.util.List;

/**
 * 申論題列表 Adapter，支援年份 Section Header。
 *
 * Item 類型：
 *   TYPE_HEADER  — 年份標題列（不可點擊）
 *   TYPE_RECORD  — 一般紀錄列
 */
public class EssayListAdapter extends BaseAdapter {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_RECORD = 1;

    /** 統一的列表項目（header 或 record） */
    static class Item {
        final int    type;
        final String headerYear;    // TYPE_HEADER 使用
        final EssayRecord record;   // TYPE_RECORD 使用

        Item(String headerYear) {
            this.type       = TYPE_HEADER;
            this.headerYear = headerYear;
            this.record     = null;
        }
        Item(EssayRecord record) {
            this.type       = TYPE_RECORD;
            this.headerYear = null;
            this.record     = record;
        }
    }

    private final Context context;
    private final List<Item> items = new ArrayList<>();

    public EssayListAdapter(Context context, List<EssayRecord> records) {
        this.context = context;
        setRecords(records);
    }

    /**
     * 依年份重建 items 列表（年份從 timestamp "yyyy/MM/dd HH:mm" 的前 4 字元取得）。
     * 紀錄已是「最新在前」順序，所以同年份的群組自動排好；
     * 無法解析年份的歸入「未知年份」群組。
     */
    public void setRecords(List<EssayRecord> records) {
        items.clear();
        String lastYear = null;
        for (EssayRecord rec : records) {
            String year = extractYear(rec.timestamp);
            if (!year.equals(lastYear)) {
                items.add(new Item(year));
                lastYear = year;
            }
            items.add(new Item(rec));
        }
        notifyDataSetChanged();
    }

    private String extractYear(String timestamp) {
        if (timestamp == null || timestamp.length() < 4) return "未知年份";
        // timestamp 格式：yyyy/MM/dd HH:mm
        String y = timestamp.substring(0, 4);
        try { Integer.parseInt(y); return y + " 年"; }
        catch (NumberFormatException e) { return "未知年份"; }
    }

    // ── BaseAdapter 必要方法 ──────────────────────────────────────

    @Override public int getCount()         { return items.size(); }
    @Override public Object getItem(int i)  { return items.get(i); }
    @Override public long getItemId(int i)  { return i; }
    @Override public int getViewTypeCount() { return 2; }
    @Override public int getItemViewType(int i) { return items.get(i).type; }

    /** Header 不可點擊、不可選 */
    @Override public boolean isEnabled(int position) {
        return items.get(position).type == TYPE_RECORD;
    }

    /** 外部點擊時用此方法取得對應 EssayRecord（會自動跳過 header） */
    public EssayRecord getRecord(int position) {
        Item item = items.get(position);
        return item.type == TYPE_RECORD ? item.record : null;
    }

    // ── View 建立 ─────────────────────────────────────────────────

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Item item = items.get(position);
        if (item.type == TYPE_HEADER) {
            return buildHeaderView(convertView, parent, item.headerYear);
        } else {
            return buildRecordView(convertView, parent, item.record);
        }
    }

    private View buildHeaderView(View convertView, ViewGroup parent, String year) {
        if (convertView == null || convertView.getTag() == null
                || !(convertView.getTag() instanceof String)) {
            TextView tv = new TextView(context);
            tv.setTag("header");
            tv.setTextSize(13f);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.parseColor("#1565C0"));
            tv.setBackgroundColor(Color.parseColor("#E3F2FD"));
            int p = dp(12);
            tv.setPadding(p, dp(8), p, dp(8));
            convertView = tv;
        }
        ((TextView) convertView).setText("📅 " + year);
        return convertView;
    }

    private View buildRecordView(View convertView, ViewGroup parent, EssayRecord rec) {
        if (convertView == null || "header".equals(convertView.getTag())) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_essay_record, parent, false);
        }

        TextView tvSubject = convertView.findViewById(R.id.itemEssaySubject);
        TextView tvTime    = convertView.findViewById(R.id.itemEssayTime);
        TextView tvPreview = convertView.findViewById(R.id.itemEssayPreview);
        TextView tvStatus  = convertView.findViewById(R.id.itemEssayStatus);

        tvSubject.setText(rec.subject.isEmpty() ? "未分類" : rec.subject);
        tvTime.setText(rec.timestamp);

        String preview = rec.question.trim();
        if (preview.length() > 50) preview = preview.substring(0, 50) + "…";
        tvPreview.setText(preview.isEmpty() ? "（無題目）" : preview);

        if (rec.hasAiReply()) {
            tvStatus.setText("✅ 已評分");
            tvStatus.setTextColor(0xFF2E7D32);
            tvStatus.setBackgroundColor(0xFFE8F5E9);
        } else if (rec.hasUserAnswer()) {
            tvStatus.setText("⏳ 待評分");
            tvStatus.setTextColor(0xFFE65100);
            tvStatus.setBackgroundColor(0xFFFFF3E0);
        } else {
            tvStatus.setText("📝 草稿");
            tvStatus.setTextColor(0xFF616161);
            tvStatus.setBackgroundColor(0xFFF5F5F5);
        }

        return convertView;
    }

    private int dp(int val) {
        return (int) (val * context.getResources().getDisplayMetrics().density);
    }
}
