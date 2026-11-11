package app.bastehyab;

import android.content.Context;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Store {
    private static final Map<String, Pkg> m = new LinkedHashMap<>();
    private static boolean loaded;
    private static long lastSave;
    /** Changes not yet written to disk; writes are batched so a scan step never blocks on file I/O. */
    private static boolean dirty;
    private static final long SAVE_INTERVAL_MS = 4000L;


    private static void migrateCurrencyCertainty(Pkg p, Context c) {
        // Older records had no currency-certainty field. Recover only from explicit unit evidence
        // in the retained card text or a fixed operator profile; otherwise fail closed.
        Op op = Cfg.opById(c, p.op);
        String evidence = Fmt.norm((p.title == null ? "" : p.title) + " " + (p.raw == null ? "" : p.raw));
        boolean explicit = evidence.contains("ریال") || evidence.contains("تومان") || evidence.contains("تومن");
        boolean fixed = op != null && ("IRR".equals(op.cur) || "IRT".equals(op.cur));
        if (!p.currencyKnown && (explicit || fixed)) p.currencyKnown = true;
        if (!p.currencyKnown) {
            p.ambig = true;
            if (p.note == null || p.note.isEmpty()) p.note = "واحد پول از داده ذخیره‌شده قابل تأیید نیست";
        }
    }

    private static File f(Context c) {
        return new File(c.getApplicationContext().getFilesDir(), "data.json");
    }

    private static void load(Context c) {
        if (loaded) return;
        loaded = true;
        if (!f(c).exists()) return; // Fresh install: an empty database is a normal state.
        boolean sanitizedRawRecords = false;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f(c)), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String ln;
            while ((ln = r.readLine()) != null) sb.append(ln);
            JSONArray a = new JSONArray(sb.toString());
            for (int i = 0; i < a.length(); i++) {
                Pkg p = Pkg.from(a.getJSONObject(i));
                migrateCurrencyCertainty(p, c);
                // Retain the previously classified restriction when raw page text is no longer
                // persisted; only the current scan uses its in-memory text for classification.
                String evidence = p.title + " " + p.raw + " " + p.why;
                String parsedWhy = Extractor.whyOf(evidence, p.days, Extractor.volCount(p.title));
                String w = Extractor.mergeWhy(parsedWhy, p.why);
                if (Extractor.INTL.matcher(Fmt.norm(evidence)).find()) {
                    w = Extractor.mergeWhy(Extractor.INTL_WHY, w);
                }
                p.why = w;
                p.restricted = !w.isEmpty();
                if (p.raw != null && !p.raw.isEmpty()) {
                    p.raw = "";
                    sanitizedRawRecords = true;
                }
                p.key = Extractor.keyOf(p.op, p.title, p.code, p.why);
                Pkg old = m.get(p.key);
                if (old == null || p.ts >= old.ts) m.put(p.key, p);
            }
            if (sanitizedRawRecords) save(c);
        } catch (Exception e) {
            android.util.Log.e("BastehyabStore", "Could not load package database", e);
            File backup = new File(c.getFilesDir(), "data.json.bak");
            // Do not expose records from a partially parsed corrupt primary file. If recovery
            // is impossible, start empty rather than silently using an unpredictable subset.
            m.clear();
            if (backup.exists()) {
                try {
                    File target = f(c);
                    if (target.exists()) target.delete();
                    if (backup.renameTo(target)) {
                        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(target), StandardCharsets.UTF_8))) {
                            StringBuilder sb = new StringBuilder(); String ln;
                            while ((ln = r.readLine()) != null) sb.append(ln);
                            JSONArray a = new JSONArray(sb.toString());
                            for (int i = 0; i < a.length(); i++) {
                                Pkg p = Pkg.from(a.getJSONObject(i));
                                migrateCurrencyCertainty(p, c);
                                String evidence = p.title + " " + p.raw + " " + p.why;
                                String parsedWhy = Extractor.whyOf(evidence, p.days, Extractor.volCount(p.title));
                                String w = Extractor.mergeWhy(parsedWhy, p.why);
                                if (Extractor.INTL.matcher(Fmt.norm(evidence)).find()) {
                                    w = Extractor.mergeWhy(Extractor.INTL_WHY, w);
                                }
                                p.why = w;
                                p.restricted = !w.isEmpty();
                                p.raw = "";
                                p.key = Extractor.keyOf(p.op, p.title, p.code, p.why);
                                m.put(p.key, p);
                            }
                            save(c);
                        }
                    }
                } catch (Exception recovery) {
                    android.util.Log.e("BastehyabStore", "Database recovery failed", recovery);
                    m.clear();
                }
            }
        }
    }

    private static void save(Context c) {
        // Keep the pending-write flag until the replacement has actually reached disk.
        // Clearing it before I/O made one transient filesystem error lose all retry attempts.
        JSONArray a = new JSONArray();
        for (Pkg p : m.values()) {
            try {
                a.put(p.json());
            } catch (Exception ignored) {}
        }
        File target = f(c);
        File temp = new File(c.getFilesDir(), "data.json.tmp");
        File backup = new File(c.getFilesDir(), "data.json.bak");
        try {
            try (Writer w = new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8)) {
                w.write(a.toString());
                w.flush();
            }
            if (backup.exists()) backup.delete();
            if (target.exists() && !target.renameTo(backup)) throw new java.io.IOException("backup rename failed");
            if (!temp.renameTo(target)) {
                if (backup.exists()) backup.renameTo(target);
                throw new java.io.IOException("atomic replace failed");
            }
            if (backup.exists()) backup.delete();
            dirty = false;
            lastSave = System.currentTimeMillis();
        } catch (Exception e) {
            dirty = true;
            android.util.Log.e("BastehyabStore", "Could not persist package database; write remains pending", e);
        }
    }

    private static boolean sameStoredFields(Pkg a, Pkg b) {
        return a != null && b != null
                && java.util.Objects.equals(a.op, b.op)
                && java.util.Objects.equals(a.key, b.key)
                && java.util.Objects.equals(a.title, b.title)
                && java.util.Objects.equals(a.code, b.code)
                && java.util.Objects.equals(a.why, b.why)
                && java.util.Objects.equals(a.note, b.note)
                && Double.compare(a.gb, b.gb) == 0
                && Double.compare(a.days, b.days) == 0
                && a.toman == b.toman
                && a.restricted == b.restricted
                && a.ambig == b.ambig
                && a.currencyKnown == b.currencyKnown
                && java.util.Objects.equals(a.reviewStatus, b.reviewStatus)
                && java.util.Objects.equals(a.reviewNote, b.reviewNote)
                && java.util.Objects.equals(a.reviewSourceFingerprint, b.reviewSourceFingerprint)
                && Double.compare(a.reviewedGb, b.reviewedGb) == 0
                && Double.compare(a.reviewedDays, b.reviewedDays) == 0
                && a.reviewedToman == b.reviewedToman && a.reviewedAt == b.reviewedAt
                && java.util.Arrays.equals(a.alts, b.alts);
    }

    // returns number of new/changed packages
    static synchronized int upsert(Context c, List<Pkg> items) {
        load(c);
        int ch = 0;
        long now = System.currentTimeMillis();
        for (Pkg p : items) {
            // Last line of defence: malformed or zero-price offers must never enter storage.
            if (p == null || p.op == null || p.key == null || p.key.trim().isEmpty()
                    // Keep uncertain offers for manual review. Compare.run() excludes them from
                    // definitive rankings; dropping them here made Samantel appear to have 0 offers.
                    || p.gb < 0.05 || p.gb > 5000 || p.toman < 100 || p.toman > 2000000000L
                    || Double.isNaN(p.gb) || Double.isInfinite(p.gb)
                    || Double.isNaN(p.days) || Double.isInfinite(p.days) || p.days < 0) continue;
            Pkg o = m.get(p.key);
            p.ts = now;
            if (o != null) {
                // A manual correction stays valid only while the extracted source data is identical.
                if (("approved".equals(o.reviewStatus) || "rejected".equals(o.reviewStatus))
                        && o.sourceFingerprint().equals(p.sourceFingerprint())) {
                    p.reviewStatus = o.reviewStatus; p.reviewNote = o.reviewNote;
                    p.reviewSourceFingerprint = o.reviewSourceFingerprint;
                    p.reviewedGb = o.reviewedGb; p.reviewedDays = o.reviewedDays;
                    p.reviewedToman = o.reviewedToman; p.reviewedAt = o.reviewedAt;
                } else if (p.ambig || !p.currencyKnown) {
                    p.reviewStatus = "pending";
                }
                HistoryLog.record(c, o, p);
            } else if (p.ambig || !p.currencyKnown) {
                p.reviewStatus = "pending";
            }
            if (o == null || o.toman != p.toman || Math.abs(o.gb - p.gb) > 1e-9
                    || o.days != p.days || o.restricted != p.restricted
                    || o.ambig != p.ambig || o.currencyKnown != p.currencyKnown) ch++;
            // Persist title/classification/note changes as well as the observation timestamp.
            // Compare.run() applies a freshness window; without persisting ts refreshes an
            // continuously visible unchanged offer would eventually expire after app restart.
            if (o == null || o.ts != now || !sameStoredFields(o, p)) dirty = true;
            // Raw accessibility text is transient parser input, not a stored user datum.
            p.raw = "";
            m.put(p.key, p);
        }
        // Batch disk writes (the whole database is re-serialized on every save).
        if (dirty && now - lastSave >= SAVE_INTERVAL_MS) save(c);
        return ch;
    }

    /** Writes pending changes now; call when a scan ends, pauses, or the app is going away. */
    static synchronized void flush(Context c) {
        if (c != null && dirty) save(c);
    }

    static synchronized List<Pkg> all(Context c) {
        load(c);
        return new ArrayList<>(m.values());
    }

    /** Returns only records awaiting human confirmation; rejected records stay out of rankings. */
    static synchronized List<Pkg> reviewQueue(Context c) {
        load(c);
        List<Pkg> out = new ArrayList<>();
        for (Pkg p : m.values()) {
            boolean approvalCurrent = p.hasCurrentManualApproval();
            if ((p.ambig || !p.currencyKnown) && !approvalCurrent
                    && !"rejected".equals(p.reviewStatus)) out.add(p.copy());
        }
        out.sort((a, b) -> Long.compare(b.ts, a.ts));
        return out;
    }

    /** Save a human decision without mutating the original parser output. */
    static synchronized boolean saveReview(Context c, String key, boolean approve,
                                            double gb, double days, long toman, String note) {
        load(c);
        Pkg p = m.get(key);
        if (p == null) return false;
        if (approve && (Double.isNaN(gb) || Double.isInfinite(gb) || gb < 0.05 || gb > 5000
                || Double.isNaN(days) || Double.isInfinite(days) || days < 0 || days > 36500
                || toman < 100 || toman > 2000000000L)) return false;
        p.reviewStatus = approve ? "approved" : "rejected";
        String safeNote = note == null ? "" : note.trim();
        p.reviewNote = safeNote.length() > 240 ? safeNote.substring(0, 240) : safeNote;
        p.reviewedAt = System.currentTimeMillis();
        p.reviewSourceFingerprint = p.sourceFingerprint();
        if (approve) {
            p.reviewedGb = gb; p.reviewedDays = days; p.reviewedToman = toman;
        } else {
            p.reviewedGb = 0; p.reviewedDays = 0; p.reviewedToman = 0;
        }
        HistoryLog.recordReview(c, p, approve);
        save(c);
        return true;
    }

    static synchronized void clearOp(Context c, String opId) {
        load(c);
        Iterator<Pkg> it = m.values().iterator();
        while (it.hasNext()) if (it.next().op.equals(opId)) it.remove();
        save(c);
    }

    static synchronized void reprice(Context c, String opId, int policy) {
        load(c);
        for (Pkg p : m.values()) {
            if (!p.op.equals(opId) || p.alts.length < 2 || policy == 0) continue;
            long best = p.alts[0];
            for (long a : p.alts) best = policy == 1 ? Math.max(best, a) : Math.min(best, a);
            p.toman = best;
            // A user-selected price policy cannot clear ambiguity caused by incomplete extraction
            // or unknown currency evidence. A fresh complete scan must resolve it.
        }
        save(c);
    }

    static synchronized void clearAll(Context c) {
        load(c);
        m.clear();
        save(c);
    }
}
