package app.bastehyab;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Bounded local history. Failure codes never include page text, account data or package contents. */
final class HistoryLog {
    private static final String KEY = "history";
    private static final String ERROR_COUNT = "history_error_count";
    private static final String LAST_ERROR = "history_error_last";

    static final class Entry {
        final String title, field, note;
        final double before, after;
        final long ts;
        Entry(String title, String field, double before, double after, long ts, String note) {
            this.title=title; this.field=field; this.before=before; this.after=after; this.ts=ts; this.note=note;
        }
    }

    private static void markError(Context c, String code) {
        try {
            SharedPreferences sp = Cfg.sp(c);
            int count = sp.getInt(ERROR_COUNT, 0);
            sp.edit().putInt(ERROR_COUNT, Math.min(1000000, count + 1))
                    .putString(LAST_ERROR, code).apply();
            android.util.Log.w("BastehyabHistory", "History operation failed: " + code);
        } catch (RuntimeException ignored) {
            android.util.Log.w("BastehyabHistory", "History error could not be persisted");
        }
    }

    static synchronized boolean hasError(Context c) {
        try { return Cfg.sp(c).getInt(ERROR_COUNT, 0) > 0; }
        catch (RuntimeException e) { return true; }
    }

    static synchronized String errorSummary(Context c) {
        try {
            SharedPreferences sp = Cfg.sp(c);
            int count = sp.getInt(ERROR_COUNT, 0);
            if (count <= 0) return "";
            String code = sp.getString(LAST_ERROR, "UNKNOWN");
            return "یک مشکل در تاریخچه ثبت شده است (" + count + " مورد؛ کد " + code + ").";
        } catch (RuntimeException e) {
            return "وضعیت تاریخچه قابل خواندن نیست.";
        }
    }

    static synchronized void clearError(Context c) {
        try { Cfg.sp(c).edit().remove(ERROR_COUNT).remove(LAST_ERROR).apply(); }
        catch (RuntimeException e) {
            // The warning cannot be cleared if preferences are unavailable; keep a local log code.
            android.util.Log.w("BastehyabHistory", "History error indicator could not be cleared");
        }
    }

    static synchronized void record(Context c, Pkg old, Pkg cur) {
        List<History.Change> changes = History.diff(old, cur);
        if (changes.isEmpty()) return;
        SharedPreferences sp;
        try { sp = Cfg.sp(c); }
        catch (RuntimeException e) { markError(c, "PREF_UNAVAILABLE"); return; }
        JSONArray a;
        try { a = new JSONArray(sp.getString(KEY, "[]")); }
        catch (Exception e) {
            markError(c, "JSON_CORRUPT");
            a = new JSONArray();
        }
        try {
            for (History.Change ch : changes) {
                JSONObject x = new JSONObject();
                x.put("title", cur.title); x.put("field", ch.field);
                x.put("before", ch.before); x.put("after", ch.after); x.put("ts", ch.ts);
                a.put(x);
            }
            persist(sp, a);
        } catch (Exception e) { markError(c, "JSON_WRITE_FAILED"); }
    }

    static synchronized void recordReview(Context c, Pkg p, boolean approved) {
        try {
            SharedPreferences sp = Cfg.sp(c);
            JSONArray a;
            try { a = new JSONArray(sp.getString(KEY, "[]")); }
            catch (Exception corrupt) { markError(c, "JSON_CORRUPT"); a = new JSONArray(); }
            JSONObject event = new JSONObject();
            event.put("title", p.title == null ? "بسته" : p.title);
            event.put("field", approved ? "review_approved" : "review_rejected");
            event.put("before", p.toman);
            event.put("after", approved ? p.reviewedToman : 0);
            event.put("ts", p.reviewedAt);
            event.put("note", p.reviewNote == null ? "" : p.reviewNote);
            a.put(event);
            persist(sp, a);
        } catch (Exception e) { markError(c, "REVIEW_HISTORY_FAILED"); }
    }

    private static void persist(SharedPreferences sp, JSONArray a) throws Exception {
        JSONArray bounded = new JSONArray();
        for (int i=Math.max(0,a.length()-300); i<a.length(); i++) bounded.put(a.getJSONObject(i));
        sp.edit().putString(KEY, bounded.toString()).apply();
    }

    static synchronized List<Entry> recent(Context c, int limit) {
        List<Entry> out = new ArrayList<>();
        if (limit <= 0) return out;
        try {
            JSONArray a = new JSONArray(Cfg.sp(c).getString(KEY, "[]"));
            for (int i=a.length()-1; i>=0 && out.size()<limit; i--) {
                JSONObject x=a.getJSONObject(i);
                out.add(new Entry(x.optString("title", ""), x.optString("field", ""),
                        x.optDouble("before", 0), x.optDouble("after", 0), x.optLong("ts", 0),
                        x.optString("note", "")));
            }
        } catch (RuntimeException e) {
            markError(c, "PREF_READ_FAILED");
        } catch (Exception e) {
            markError(c, "JSON_READ_FAILED");
        }
        return out;
    }

    static synchronized void clear(Context c) {
        try { Cfg.sp(c).edit().remove(KEY).apply(); }
        catch (RuntimeException e) { markError(c, "CLEAR_FAILED"); }
    }
}
