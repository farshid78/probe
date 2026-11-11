package app.bastehyab;

import org.json.JSONArray;
import org.json.JSONObject;

final class Pkg {
    String op, key, title, code = "", why = "";
    double gb, days;
    long toman, ts;
    boolean restricted, ambig, currencyKnown = true;
    String note = "", raw = "";
    // Manual review is a separate, auditable layer; raw extracted fields are never overwritten.
    String reviewStatus = "none", reviewNote = "", reviewSourceFingerprint = "";
    double reviewedGb, reviewedDays;
    long reviewedToman, reviewedAt;
    long[] alts = new long[0];

    double perGb() { return gb > 0 && toman > 0 ? toman / gb : Double.MAX_VALUE; }

    /** volume this package would give per 30 days if renewed back to back */
    double monthlyGb() { return days > 0 ? gb * 30.0 / days : 0; }

    /** price per GB for each month of validity (1 GB kept for 30 days = 1 GB-month) */
    double perGbMonth() { return days > 0 && gb > 0 && toman > 0 ? toman / (gb * days / 30.0) : Double.MAX_VALUE; }

    /**
     * Cost of getting `usage` GB in a 30-day month with this package.
     *  - 30 days or longer: the price is spread over the months it lasts (toman * 30 / days),
     *    and extra packages are added when one package per month is not enough.
     *  - shorter than 30 days: include both the packages required by volume and the minimum
     *    renewals required to keep the offer available throughout the 30-day comparison window.
     */
    double monthlyCost(double usage) {
        if (days <= 0 || gb <= 0 || toman <= 0) return Double.MAX_VALUE;
        if (days >= 30) {
            double mg = monthlyGb();
            double n = Math.max(1, Math.ceil(usage / mg - 1e-9));
            return n * toman * 30.0 / days;
        }
        double byVolume = Math.max(1, Math.ceil(usage / gb - 1e-9));
        double byCoverage = Math.max(1, Math.ceil(30.0 / days - 1e-9));
        return Math.max(byVolume, byCoverage) * toman;
    }

    /** identity of what the buyer really gets: same operator, volume, duration and price = same package */
    String sig() {
        // A night-only / app-only offer is not interchangeable with a general-purpose offer,
        // even when volume, validity and price happen to match.
        String kind = Fmt.norm(why == null ? "" : why).replace(" ", "");
        return op + "|" + Math.round(gb * 1024) + "|" + Math.round(days * 24) + "|" + toman + "|" + kind;
    }

    boolean isIntl() { return why != null && why.startsWith("بین‌الملل"); }

    String sourceFingerprint() {
        return String.valueOf(key) + "|" + String.valueOf(title) + "|" + String.valueOf(code)
                + "|" + gb + "|" + days + "|" + toman + "|" + currencyKnown + "|" + ambig
                + "|" + String.valueOf(why) + "|" + java.util.Arrays.toString(alts);
    }

    boolean hasCurrentManualApproval() {
        return "approved".equals(reviewStatus) && reviewedAt > 0
                && reviewedGb >= 0.05 && reviewedGb <= 5000
                && reviewedDays >= 0 && reviewedDays <= 36500
                && reviewedToman >= 100 && reviewedToman <= 2000000000L
                && sourceFingerprint().equals(reviewSourceFingerprint);
    }

    Pkg comparisonVersion() {
        if (!hasCurrentManualApproval()) return this;
        Pkg p = copy();
        p.gb = reviewedGb; p.days = reviewedDays; p.toman = reviewedToman;
        p.currencyKnown = true; p.ambig = false;
        p.note = reviewNote == null || reviewNote.isEmpty() ? "تأیید دستی" : "تأیید دستی: " + reviewNote;
        return p;
    }

    Pkg copy() {
        Pkg p = new Pkg();
        p.op=op; p.key=key; p.title=title; p.code=code; p.why=why; p.gb=gb; p.days=days;
        p.toman=toman; p.ts=ts; p.restricted=restricted; p.ambig=ambig; p.currencyKnown=currencyKnown;
        p.note=note; p.raw=raw; p.alts=alts == null ? new long[0] : alts.clone();
        p.reviewStatus=reviewStatus; p.reviewNote=reviewNote; p.reviewSourceFingerprint=reviewSourceFingerprint;
        p.reviewedGb=reviewedGb; p.reviewedDays=reviewedDays; p.reviewedToman=reviewedToman; p.reviewedAt=reviewedAt;
        return p;
    }

    String label() {
        String v = gb < 1 ? Fmt.num(Math.round(gb * 1024)) + " مگ" : Fmt.dec(gb) + " گیگ";
        String d = "";
        if (days > 0 && days < 1) d = " • " + Fmt.num(Math.max(1, Math.round(days * 24))) + " ساعت";
        else if (days > 0) d = " • " + Fmt.dec(days) + " روز";
        return v + d;
    }

    JSONObject json() throws Exception {
        JSONObject o = new JSONObject();
        o.put("op", op);
        o.put("key", key);
        o.put("title", title);
        o.put("code", code);
        o.put("why", why);
        o.put("gb", gb);
        o.put("days", days);
        o.put("toman", toman);
        o.put("ts", ts);
        o.put("r", restricted);
        o.put("am", ambig);
        o.put("ck", currencyKnown);
        o.put("note", note);
        o.put("reviewStatus", reviewStatus);
        o.put("reviewNote", reviewNote);
        o.put("reviewSource", reviewSourceFingerprint);
        o.put("reviewedGb", reviewedGb);
        o.put("reviewedDays", reviewedDays);
        o.put("reviewedToman", reviewedToman);
        o.put("reviewedAt", reviewedAt);
        JSONArray a = new JSONArray();
        for (long v : alts) a.put(v);
        o.put("alts", a);
        return o;
    }

    static Pkg from(JSONObject o) throws Exception {
        Pkg p = new Pkg();
        p.op = o.getString("op");
        p.key = o.getString("key");
        p.title = o.optString("title", "");
        p.code = o.optString("code", "");
        p.why = o.optString("why", "");
        p.gb = o.getDouble("gb");
        p.days = o.optDouble("days", 0);
        p.toman = o.getLong("toman");
        p.ts = o.optLong("ts", 0);
        p.restricted = o.optBoolean("r", false);
        p.ambig = o.optBoolean("am", false);
        p.currencyKnown = o.optBoolean("ck", false);
        p.note = o.optString("note", "");
        p.reviewStatus = o.optString("reviewStatus", "none");
        if (!"none".equals(p.reviewStatus) && !"pending".equals(p.reviewStatus)
                && !"approved".equals(p.reviewStatus) && !"rejected".equals(p.reviewStatus)) p.reviewStatus = "none";
        p.reviewNote = o.optString("reviewNote", "");
        p.reviewSourceFingerprint = o.optString("reviewSource", "");
        p.reviewedGb = o.optDouble("reviewedGb", 0);
        p.reviewedDays = o.optDouble("reviewedDays", 0);
        p.reviewedToman = o.optLong("reviewedToman", 0);
        p.reviewedAt = o.optLong("reviewedAt", 0);
        p.raw = o.optString("raw", "");
        JSONArray a = o.optJSONArray("alts");
        if (a != null) {
            p.alts = new long[a.length()];
            for (int i = 0; i < a.length(); i++) p.alts[i] = a.getLong(i);
        }
        return p;
    }
}
