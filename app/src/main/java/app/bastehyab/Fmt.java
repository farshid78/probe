package app.bastehyab;

import java.util.Locale;

final class Fmt {
    static String fa(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) b.append(c >= '0' && c <= '9' ? (char) ('\u06f0' + (c - '0')) : c);
        return b.toString();
    }

    static String num(long v) {
        return fa(String.format(Locale.US, "%,d", v)).replace(',', '\u066c');
    }

    static String dec(double v) {
        if (Math.abs(v - Math.rint(v)) < 0.05) return fa(String.valueOf((long) Math.rint(v)));
        return fa(String.format(Locale.US, "%.1f", v)).replace('.', '\u066b');
    }

    static String ago(long ts) {
        long now = System.currentTimeMillis();
        if (ts > now + 5 * 60 * 1000L) return "زمان ثبت جلوتر از ساعت دستگاه است";
        long s = Math.max(0L, (now - ts) / 1000L);
        if (s < 60) return "همین حالا";
        if (s < 3600) return fa(String.valueOf(s / 60)) + " دقیقه پیش";
        if (s < 86400) return fa(String.valueOf(s / 3600)) + " ساعت پیش";
        return fa(String.valueOf(s / 86400)) + " روز پیش";
    }

    // Persian/Arabic digits -> ASCII, unify letters, ZWNJ -> space
    static String norm(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c >= '\u06f0' && c <= '\u06f9') b.append((char) ('0' + c - '\u06f0'));
            else if (c >= '\u0660' && c <= '\u0669') b.append((char) ('0' + c - '\u0660'));
            else if (c == '\u066c' || c == '\u060c') b.append(',');
            else if (c == '\u066b') b.append('.');
            else if (c == '\u064a' || c == '\u0649') b.append('\u06cc');
            else if (c == '\u0643') b.append('\u06a9');
            else if (c == '\u0640') { /* remove Arabic tatweel */ }
            else if (c == '\u200c' || c == '\u200e' || c == '\u200f'
                    || (c >= '\u202a' && c <= '\u202e') || (c >= '\u2066' && c <= '\u2069')
                    || c == '\u200b' || c == '\u200d' || c == '\ufeff'
                    || c == '\u00a0' || c == '\u202f' || c == '\u2007') b.append(' ');
            else b.append(c);
        }
        return b.toString().replaceAll("(\\d)/(\\d)", "$1.$2")
                .replaceAll("\\s+", " ").trim();
    }

    static double parse(String s, double def) {
        try {
            String t = norm(s).trim();
            if (t.isEmpty()) return def;
            double value = Double.parseDouble(t);
            return Double.isNaN(value) || Double.isInfinite(value) ? def : value;
        } catch (Exception e) {
            return def;
        }
    }

    /** Final price in whole toman with VAT percent (0..100). Rounds half up; rejects an out-of-range percent. */
    static long withVat(long toman, int percent) {
        if (percent < 0 || percent > 100) throw new IllegalArgumentException("VAT percent must be 0..100");
        return Math.round(toman * (100 + percent) / 100.0);
    }
}
