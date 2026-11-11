package app.bastehyab;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

final class Cfg {
    static final int PER_GB = 0, DURATION = 1, MY_USAGE = 2, PER_GB_MONTH = 3;
    private static List<Op> cache;

    static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("cfg", 0);
    }

    private static List<Op> defaults() {
        List<Op> d = new ArrayList<>();
        d.add(new Op("irancell", "ایرانسل", 0xFFFFCB05, "AUTO", true, "com.myirancell"));
        d.add(new Op("mci", "همراه اول", 0xFF1EA7E1, "AUTO", true, "ir.mci.ecareapp"));
        d.add(new Op("rightel", "رایتل", 0xFFD81B60, "AUTO", true, "ir.rightel.myrightel"));
        d.add(new Op("samantel", "سامانتل", 0xFFFF7043, "IRR", true, "com.mysamantel"));
        d.add(new Op("shatel", "شاتل", 0xFF26A69A, "AUTO", true, "com.shmapp"));
        return d;
    }

    static synchronized List<Op> ops(Context c) {
        if (cache != null) return copyOps(cache);
        // The previous build recorded package names of every foreground app to suggest
        // possible operators. This is unnecessary usage metadata; clear it during migration.
        sp(c).edit().remove("recent").remove("ambig").apply();
        List<Op> l = new ArrayList<>();
        String s = sp(c).getString("ops", null);
        if (s != null) {
            try {
                JSONArray a = new JSONArray(s);
                for (int i = 0; i < a.length(); i++) l.add(Op.from(a.getJSONObject(i)));
            } catch (Exception e) {
                l.clear();
            }
        }
        if (sp(c).getInt("defv", 0) < 6 || l.isEmpty()) {
            for (Op d : defaults()) {
                Op ex = null;
                for (Op o : l) if (o.id.equals(d.id)) ex = o;
                if (ex == null) {
                    l.add(d);
                } else if (ex.builtin) {
                    ex.pkgs.clear();
                    ex.pkgs.addAll(d.pkgs);
                    // Irancell now uses the same automatic currency inference as MCI.
                    // Samantel explicitly exposes Rial amounts and retains its operator currency.
                    if ("irancell".equals(ex.id)) ex.cur = "AUTO";
                    if ("samantel".equals(ex.id)) ex.cur = "IRR";
                    // All built-in operators use the same nearby-label price selection strategy.
                    ex.policy = 0;
                }
            }
            sp(c).edit().putInt("defv", 6).apply();
            saveOps(c, l);
        }
        cache = copyOps(l);
        return copyOps(cache);
    }

    private static List<Op> copyOps(List<Op> source) {
        List<Op> copy = new ArrayList<>();
        if (source == null) return copy;
        for (Op o : source) {
            if (o == null) continue;
            Op c = new Op(o.id, o.name, o.color, o.cur, o.builtin);
            c.enabled = o.enabled; c.cmp = o.cmp; c.policy = o.policy;
            c.pkgs.clear(); c.pkgs.addAll(o.pkgs);
            copy.add(c);
        }
        return copy;
    }

    static synchronized void saveOps(Context c, List<Op> l) {
        cache = copyOps(l);
        JSONArray a = new JSONArray();
        for (Op o : cache) {
            try {
                a.put(o.json());
            } catch (Exception ignored) {}
        }
        sp(c).edit().putString("ops", a.toString()).apply();
    }

    static Op opFor(Context c, String pkg) {
        for (Op o : ops(c)) if (o.pkgs.contains(pkg)) return o;
        return null;
    }

    static Op opById(Context c, String id) {
        for (Op o : ops(c)) if (o.id.equals(id)) return o;
        return null;
    }

    static synchronized List<OpSnapshot> snapshotOps(Context c) {
        List<OpSnapshot> result = new ArrayList<>();
        for (Op op : ops(c)) result.add(new OpSnapshot(op));
        return java.util.Collections.unmodifiableList(result);
    }

    static synchronized OpSnapshot snapshotById(Context c, String id) {
        for (OpSnapshot op : snapshotOps(c)) if (op.id.equals(id)) return op;
        return null;
    }

    static synchronized OpSnapshot snapshotForPackage(Context c, String packageName) {
        for (OpSnapshot op : snapshotOps(c)) if (op.ownsPackage(packageName)) return op;
        return null;
    }

    static int metric(Context c) {
        // One-time migration: the old index 1 meant price/GB-month; the redesigned tab 1 means duration.
        if (sp(c).getInt("metricModeVersion", 0) < 1) {
            sp(c).edit().putInt("metric", PER_GB).putInt("metricModeVersion", 1).apply();
        }
        return clamp(sp(c).getInt("metric", PER_GB), PER_GB, MY_USAGE);
    }
    static float minGb(Context c) { return finiteClamp(sp(c).getFloat("minGb", 0f), 0f, 5000f, 0f); }
    static int maxDays(Context c) { return clamp(sp(c).getInt("maxDays", 0), 0, 36500); }
    static boolean restricted(Context c) { return sp(c).getBoolean("restricted", true); }
    static boolean intl(Context c) { return sp(c).getBoolean("intl", false); }
    static float usage(Context c) { return finiteClamp(sp(c).getFloat("usage", 30f), 0.1f, 5000f, 30f); }
    static boolean popup(Context c) { return sp(c).getBoolean("popup", true); }
    static int rows(Context c) { return clamp(sp(c).getInt("rows", 3), 3, 8); }
    static int hideSec(Context c) {
        int v = sp(c).getInt("hide", 0);
        return v == 8 || v == 15 || v == 30 ? v : 0;
    }
    static boolean bottom(Context c) { return sp(c).getBoolean("bottom", false); }
    static int fresh(Context c) {
        int v = sp(c).getInt("fresh", 3);
        if (v == 7 || v == 14 || v == 30) return v;
        return 3;
    }
    static int minOps(Context c) {
        int enabled = 0;
        for (Op o : ops(c)) if (o.cmp) enabled++;
        return clamp(sp(c).getInt("minOps", 1), 1, Math.max(1, enabled));
    }
    static int pmode(Context c) { return clamp(sp(c).getInt("pmode", 0), 0, 2); } // 0 screen + overall, 1 overall, 2 screen
    static int bucket(Context c) { return clamp(sp(c).getInt("bucket", 0), 0, 4); }
    static int vat(Context c) { return Math.max(0, Math.min(100, sp(c).getInt("vat", 10))); }
    static String displayCurrency(Context c) {
        String v = sp(c).getString("displayCurrency", "IRT");
        return "IRR".equals(v) ? "IRR" : "IRT";
    }
    static void setDisplayCurrency(Context c, String v) {
        sp(c).edit().putString("displayCurrency", "IRR".equals(v) ? "IRR" : "IRT").apply();
    }
    static String priceLabel(Context c, long toman) {
        boolean rial = "IRR".equals(displayCurrency(c));
        long amount = rial ? Math.round(toman * 10.0d) : toman;
        return Fmt.num(amount) + (rial ? " ریال" : " تومان");
    }
    static String metricUnit(Context c, String unit) {
        return "IRR".equals(displayCurrency(c)) ? unit.replace("تومان", "ریال") : unit;
    }
    static double displayMetric(Context c, double tomanValue) {
        return "IRR".equals(displayCurrency(c)) ? tomanValue * 10.0d : tomanValue;
    }
    static long finalPrice(Context c, Pkg p) { return Fmt.withVat(p.toman, vat(c)); }

    static void setInt(Context c, String k, int v) { sp(c).edit().putInt(k, v).apply(); }
    static void setFloat(Context c, String k, float v) { sp(c).edit().putFloat(k, v).apply(); }
    static void setBool(Context c, String k, boolean v) { sp(c).edit().putBoolean(k, v).apply(); }

    static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    static float finiteClamp(float value, float min, float max, float fallback) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }

    /** Android application IDs are dot-separated Java-style identifiers. */
    static boolean validPackageName(String value) {
        if (value == null) return false;
        String s = value.trim();
        if (s.length() < 3 || s.length() > 255) return false;
        String[] parts = s.split("\\.", -1);
        if (parts.length < 2) return false;
        for (String part : parts) {
            if (part.isEmpty() || !part.matches("[A-Za-z_][A-Za-z0-9_]*")) return false;
        }
        return true;
    }

}
