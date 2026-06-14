package com.example.myapplication.essay;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 申論題紀錄的本地儲存管理器。
 * 每筆紀錄一行，序列化格式見 EssayRecord。
 */
public class EssayManager {

    private static final String FILE_NAME = "essay_records.txt";
    private static final String TAG       = "EssayManager";

    // ── 新增一筆紀錄 ──────────────────────────────────────────────

    public static void save(Context ctx, EssayRecord record) {
        try (FileOutputStream fos = ctx.openFileOutput(FILE_NAME, Context.MODE_APPEND)) {
            fos.write((record.serialize() + "\n").getBytes("UTF-8"));
        } catch (Exception e) {
            Log.e(TAG, "儲存失敗: " + e.getMessage());
        }
    }

    // ── 讀取全部（最新在前）──────────────────────────────────────

    public static List<EssayRecord> loadAll(Context ctx) {
        List<EssayRecord> list = new ArrayList<>();
        File file = new File(ctx.getFilesDir(), FILE_NAME);
        if (!file.exists()) return list;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                EssayRecord rec = EssayRecord.deserialize(line);
                if (rec != null) list.add(rec);
            }
        } catch (Exception e) {
            Log.e(TAG, "讀取失敗: " + e.getMessage());
        }
        // 最新在前
        Collections.reverse(list);
        return list;
    }

    // ── 更新一筆（依 id 找到後整行替換）─────────────────────────

    public static boolean update(Context ctx, EssayRecord updated) {
        File file = new File(ctx.getFilesDir(), FILE_NAME);
        if (!file.exists()) return false;
        try {
            // 讀全部行
            List<String> lines = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = r.readLine()) != null) lines.add(line);
            }
            // 找到同 id 的行並替換
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                EssayRecord rec = EssayRecord.deserialize(lines.get(i));
                if (rec != null && rec.id.equals(updated.id)) {
                    lines.set(i, updated.serialize());
                    found = true;
                    break;
                }
            }
            if (!found) return false;
            // 寫回
            try (FileOutputStream fos = ctx.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
                for (String line : lines) {
                    fos.write((line + "\n").getBytes("UTF-8"));
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "更新失敗: " + e.getMessage());
            return false;
        }
    }

    // ── 刪除一筆（依 id）─────────────────────────────────────────

    public static boolean delete(Context ctx, String id) {
        File file = new File(ctx.getFilesDir(), FILE_NAME);
        if (!file.exists()) return false;
        try {
            List<String> lines = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = r.readLine()) != null) lines.add(line);
            }
            boolean found = false;
            Iterator<String> it = lines.iterator();
            while (it.hasNext()) {
                EssayRecord rec = EssayRecord.deserialize(it.next());
                if (rec != null && rec.id.equals(id)) {
                    it.remove();
                    found = true;
                    break;
                }
            }
            if (!found) return false;
            try (FileOutputStream fos = ctx.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
                for (String line : lines) {
                    fos.write((line + "\n").getBytes("UTF-8"));
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "刪除失敗: " + e.getMessage());
            return false;
        }
    }

    // ── 依 id 讀取單筆 ────────────────────────────────────────────

    public static EssayRecord findById(Context ctx, String id) {
        for (EssayRecord r : loadAll(ctx)) {
            if (r.id.equals(id)) return r;
        }
        return null;
    }

    // ── 工具方法 ──────────────────────────────────────────────────

    /** 產生唯一 id（yyyyMMdd_HHmmss_ms） */
    public static String generateId() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date())
                + "_" + (System.currentTimeMillis() % 1000);
    }

    /** 產生顯示用時間戳 */
    public static String currentTimestamp() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date());
    }
}
