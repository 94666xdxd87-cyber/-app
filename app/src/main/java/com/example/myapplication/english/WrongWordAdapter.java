package com.example.myapplication.english;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.core.WrongWord;
import java.util.List;

public class WrongWordAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_SECTION_HEADER = 0;
    public static final int TYPE_WRONG_WORD     = 1;

    public interface OnDeleteListener { void onDelete(WrongWord ww, int type); }
    public interface OnEditListener   { void onEdit(WrongWord ww, int type); }

    public static class Item {
        final int     viewType;
        final String  sectionTitle;
        final WrongWord wrongWord;
        final int     type;

        Item(String sectionTitle) {
            this.viewType     = TYPE_SECTION_HEADER;
            this.sectionTitle = sectionTitle;
            this.wrongWord    = null;
            this.type         = 0;
        }
        Item(WrongWord ww, int type) {
            this.viewType     = TYPE_WRONG_WORD;
            this.sectionTitle = null;
            this.wrongWord    = ww;
            this.type         = type;
        }
    }

    private List<Item> items;
    private OnDeleteListener deleteListener;
    private OnEditListener   editListener;

    public WrongWordAdapter(List<Item> items) { this.items = items; }

    public void setOnDeleteListener(OnDeleteListener l) { deleteListener = l; }
    public void setOnEditListener(OnEditListener l)     { editListener   = l; }

    public void updateItems(List<Item> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) { return items.get(position).viewType; }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SECTION_HEADER) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(14f);
            tv.setTextColor(Color.parseColor("#424242"));
            tv.setPadding(12, 20, 12, 6);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new HeaderVH(tv);
        } else {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(12, 10, 12, 10);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 2);
            row.setLayoutParams(lp);

            TextView tvWord = new TextView(parent.getContext());
            tvWord.setTextSize(15f);
            tvWord.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvWord.setTag("word");
            row.addView(tvWord);

            TextView tvCount = new TextView(parent.getContext());
            tvCount.setTextSize(13f);
            tvCount.setPadding(0, 0, 8, 0);
            tvCount.setTag("count");
            row.addView(tvCount);

            Button btnEdit = new Button(parent.getContext());
            btnEdit.setText("調整");
            btnEdit.setTextSize(11f);
            btnEdit.setTextColor(Color.WHITE);
            btnEdit.setBackgroundColor(Color.parseColor("#1565C0"));
            LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 72);
            editLp.setMarginEnd(6);
            btnEdit.setLayoutParams(editLp);
            btnEdit.setTag("edit");
            row.addView(btnEdit);

            Button btnDel = new Button(parent.getContext());
            btnDel.setText("刪除");
            btnDel.setTextSize(11f);
            btnDel.setTextColor(Color.WHITE);
            btnDel.setBackgroundColor(Color.parseColor("#E53935"));
            btnDel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 72));
            btnDel.setTag("del");
            row.addView(btnDel);

            return new WordVH(row);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = items.get(position);
        if (item.viewType == TYPE_SECTION_HEADER) {
            ((HeaderVH) holder).tvTitle.setText(item.sectionTitle);
        } else {
            WordVH wh = (WordVH) holder;
            WrongWord ww = item.wrongWord;
            wh.itemView.setBackgroundColor(
                    ww.errorCount > 0 ? Color.WHITE : Color.parseColor("#F3E5F5"));
            wh.tvWord.setText(ww.word);
            wh.tvCount.setText("強度: " + ww.errorCount);
            wh.tvCount.setTextColor(
                    ww.errorCount > 3 ? Color.parseColor("#C62828")
                    : ww.errorCount > 0 ? Color.parseColor("#E65100")
                    : Color.parseColor("#7B1FA2"));
            wh.btnEdit.setOnClickListener(v -> { if (editListener != null) editListener.onEdit(ww, item.type); });
            wh.btnDel.setOnClickListener(v ->  { if (deleteListener != null) deleteListener.onDelete(ww, item.type); });
        }
    }

    @Override
    public int getItemCount() { return items == null ? 0 : items.size(); }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvTitle;
        HeaderVH(TextView tv) { super(tv); tvTitle = tv; }
    }

    static class WordVH extends RecyclerView.ViewHolder {
        TextView tvWord, tvCount;
        Button btnEdit, btnDel;
        WordVH(LinearLayout row) {
            super(row);
            tvWord  = (TextView) row.findViewWithTag("word");
            tvCount = (TextView) row.findViewWithTag("count");
            btnEdit = (Button)   row.findViewWithTag("edit");
            btnDel  = (Button)   row.findViewWithTag("del");
        }
    }
}
