package com.example.myapplication.english;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.core.WordExplainRecord;
import java.util.List;
import java.util.Set;

public class WordBankAdapter extends RecyclerView.Adapter<WordBankAdapter.VH> {

    public interface OnDeleteListener  { void onDelete(String word); }
    public interface OnCheckedListener { void onChecked(String word, boolean checked); }

    private List<String> words;
    private final Set<String> checkedWords;
    private final Set<String> explainedWordsCache;
    private OnDeleteListener  deleteListener;
    private OnCheckedListener checkedListener;

    public WordBankAdapter(List<String> words, Set<String> checkedWords, Set<String> explainedWordsCache) {
        this.words               = words;
        this.checkedWords        = checkedWords;
        this.explainedWordsCache = explainedWordsCache;
    }

    public void setOnDeleteListener(OnDeleteListener l)   { deleteListener  = l; }
    public void setOnCheckedListener(OnCheckedListener l) { checkedListener = l; }

    public void updateWords(List<String> newWords) {
        this.words = newWords;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout row = new LinearLayout(parent.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(12, 6, 12, 6);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        CheckBox cb = new CheckBox(parent.getContext());
        cb.setTag("cb");
        row.addView(cb);

        TextView tvDot = new TextView(parent.getContext());
        tvDot.setText("●");
        tvDot.setTextColor(Color.parseColor("#FF6F00"));
        tvDot.setTextSize(10f);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dotLp.setMarginEnd(4);
        tvDot.setLayoutParams(dotLp);
        tvDot.setTag("dot");
        row.addView(tvDot);

        TextView tvWord = new TextView(parent.getContext());
        tvWord.setTextSize(15f);
        tvWord.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvWord.setPadding(4, 0, 0, 0);
        tvWord.setTag("word");
        row.addView(tvWord);

        Button btnDel = new Button(parent.getContext());
        btnDel.setText("刪除");
        btnDel.setTextSize(12f);
        btnDel.setTextColor(Color.WHITE);
        btnDel.setBackgroundColor(Color.parseColor("#E53935"));
        btnDel.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 72));
        btnDel.setTag("del");
        row.addView(btnDel);

        return new VH(row);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final String word = words.get(position);

        holder.itemView.setBackgroundColor(
                position % 2 == 0 ? Color.WHITE : Color.parseColor("#F5F5F5"));

        holder.cb.setOnCheckedChangeListener(null);
        holder.cb.setChecked(checkedWords.contains(word));
        holder.cb.setOnCheckedChangeListener((btn, checked) -> {
            if (checkedListener != null) checkedListener.onChecked(word, checked);
        });

        boolean hasExplain = explainedWordsCache.contains(word.toLowerCase());
        holder.tvDot.setVisibility(hasExplain ? View.VISIBLE : View.GONE);
        holder.tvWord.setText(word);

        holder.btnDel.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(word);
        });
    }

    @Override
    public int getItemCount() { return words == null ? 0 : words.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cb;
        TextView tvDot, tvWord;
        Button btnDel;

        VH(LinearLayout row) {
            super(row);
            cb     = (CheckBox) row.findViewWithTag("cb");
            tvDot  = (TextView) row.findViewWithTag("dot");
            tvWord = (TextView) row.findViewWithTag("word");
            btnDel = (Button)   row.findViewWithTag("del");
        }
    }
}
