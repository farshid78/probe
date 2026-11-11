package app.bastehyab;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Compare {
    static final String[] BUCKETS = {"همه", "هفتگی", "ماهانه", "۳ ماهه", "بلندمدت"};

    static boolean inBucket(double days, int b) {
        if (b == 0) return true;
        if (days <= 0) return false;
        switch (b) {
            case 1: return days <= 7;
            case 2: return days > 7 && days <= 35;
            case 3: return days > 35 && days <= 100;
            default: return days > 100;
        }
    }

    /** duration class used to compare like with like: 0 unknown, 1 hours/day, 2 week, 3 month, 4 quarter, 5 longer */
    /** Intl/roaming has its own switch and must not be hidden by the domestic-special switch. */
    static boolean passesSpecialFilters(Pkg p, boolean includeSpecial, boolean includeIntl) {
        if (p == null) return false;
        if (p.isIntl()) return includeIntl;
        return !p.restricted || includeSpecial;
    }

    static int group(double days) {
        if (days <= 0) return 0;
        if (days <= 1) return 1;
        if (days <= 7) return 2;
        if (days <= 35) return 3;
        if (days <= 100) return 4;
        return 5;
    }

    static final class Result {
        final List<Pkg> ranked = new ArrayList<>();
        final Map<String, Double> val = new HashMap<>();
        final List<Pkg> ambig = new ArrayList<>(); // kept out of the ranking: price could not be trusted
        final Map<String, Integer> rank = new HashMap<>();
        final java.util.Set<String> cheap = new HashSet<>(); // far below the median: double-check
        final List<Pkg> screen = new ArrayList<>(); // ranked packages that are on the current screen
        int bucket;
        double median;
        int ops, metric;
        String label = "", unit = "";
        Pkg bestScreen;
        int rankScreen;
    }

    /** Keep the displayed ordinal in exact agreement with the numeric comparison value. */
    static void sortRanked(List<Pkg> packages, final Map<String, Double> values) {
        Collections.sort(packages, (a, b) -> {
            Double av = values.get(a.key), bv = values.get(b.key);
            int cmp = Double.compare(av == null ? Double.MAX_VALUE : av,
                    bv == null ? Double.MAX_VALUE : bv);
            if (cmp == 0) cmp = Boolean.compare(a.restricted, b.restricted);
            if (cmp == 0) cmp = a.op.compareTo(b.op);
            return cmp != 0 ? cmp : a.key.compareTo(b.key);
        });
    }

    static double value(Pkg p, int metric, double usage, long price) {
        if (p.gb <= 0 || price <= 0) return Double.MAX_VALUE;
        switch (metric) {
            case Cfg.DURATION:
                // Negative duration sorts longest-validity bundles first with the shared ascending sorter.
                return p.days > 0 ? -p.days : Double.MAX_VALUE;
            case Cfg.PER_GB_MONTH:
                return p.days > 0 ? price / (p.gb * p.days / 30.0) : Double.MAX_VALUE;
            case Cfg.MY_USAGE:
                if (p.days <= 0) return Double.MAX_VALUE;
                if (p.days >= 30) {
                    double monthlyGb = p.gb * 30.0 / p.days;
                    return Math.max(1, Math.ceil(usage / monthlyGb - 1e-9)) * price * 30.0 / p.days;
                }
                double byVolume = Math.max(1, Math.ceil(usage / p.gb - 1e-9));
                double byCoverage = Math.max(1, Math.ceil(30.0 / p.days - 1e-9));
                return Math.max(byVolume, byCoverage) * price;
            default: return price / p.gb;
        }
    }

    static Result run(Context c, Set<String> onScreen) {
        final Result r = new Result();
        int metric = Cfg.metric(c);
        double usage = Cfg.usage(c);
        r.metric = metric;
        r.bucket = Cfg.bucket(c);
        long minTs = System.currentTimeMillis() - Cfg.fresh(c) * 86400000L;
        Set<String> enabled = new HashSet<>();
        for (Op o : Cfg.ops(c)) if (o.cmp) enabled.add(o.id);
        Set<String> seen = new HashSet<>();
        float minGb = Cfg.minGb(c);
        int maxDays = Cfg.maxDays(c);
        boolean restricted = Cfg.restricted(c);
        boolean intl = Cfg.intl(c);
        // The signature includes classification so a limited offer cannot shadow a general-purpose twin.
        Map<String, Pkg> uniq = new HashMap<>();
        for (Pkg raw : Store.all(c)) {
            // Manual approval is applied as a separate comparison view; raw extraction remains intact.
            if ("rejected".equals(raw.reviewStatus)) continue;
            Pkg p = raw.comparisonVersion();
            String sg = p.sig();
            Pkg o = uniq.get(sg);
            if (o == null || (o.restricted && !p.restricted) || (o.restricted == p.restricted && p.ts > o.ts)) uniq.put(sg, p);
        }
        // a package that is on screen may have been merged into a twin with another title: keep it "on screen"
        Set<String> screenKeys = new HashSet<>();
        if (onScreen != null) {
            for (Pkg raw : Store.all(c)) {
                if ("rejected".equals(raw.reviewStatus)) continue;
                Pkg p = raw.comparisonVersion();
                Pkg merged = uniq.get(p.sig());
                if (onScreen.contains(raw.key) && merged != null) screenKeys.add(merged.key);
            }
        }
        for (Pkg p : uniq.values()) {
            if (!enabled.contains(p.op) || p.ts < minTs || p.gb < minGb) continue;
            if (maxDays > 0 && p.days > maxDays) continue;
            if (!inBucket(p.days, r.bucket)) continue;
            if (!passesSpecialFilters(p, restricted, intl)) continue;
            // Ambiguous offers may be inspected manually, but never enter the definitive ranking.
            if (!p.currencyKnown || p.ambig) {
                r.ambig.add(p);
                continue;
            }
            double v = value(p, metric, usage, Fmt.withVat(p.toman, Cfg.vat(c)));
            if (v >= Double.MAX_VALUE / 2) continue;
            r.val.put(p.key, v);
            r.ranked.add(p);
            seen.add(p.op);
        }
        sortRanked(r.ranked, r.val);
        r.ops = seen.size();
        if (!r.ranked.isEmpty()) {
            int n = r.ranked.size();
            double hi = r.val.get(r.ranked.get(n / 2).key);
            r.median = n % 2 == 1 ? hi : (hi + r.val.get(r.ranked.get(n / 2 - 1).key)) / 2;
        }
        // "too cheap" is judged against packages of the same duration class, so a long package is not
        // flagged just because it is naturally cheaper per GB-month than a weekly one
        Map<Integer, List<Double>> grp = new HashMap<>();
        for (Pkg p : r.ranked) {
            int g = group(p.days);
            List<Double> l = grp.get(g);
            if (l == null) grp.put(g, l = new ArrayList<>());
            l.add(r.val.get(p.key));
        }
        Map<Integer, Double> gmed = new HashMap<>();
        for (Map.Entry<Integer, List<Double>> e : grp.entrySet()) {
            List<Double> l = e.getValue();
            Collections.sort(l);
            int n = l.size();
            if (n >= 5) gmed.put(e.getKey(), n % 2 == 1 ? l.get(n / 2) : (l.get(n / 2) + l.get(n / 2 - 1)) / 2);
        }
        for (int i = 0; i < r.ranked.size(); i++) {
            Pkg p = r.ranked.get(i);
            r.rank.put(p.key, i + 1);
            if (screenKeys.contains(p.key)) r.screen.add(p);
            Double gm = gmed.get(group(p.days));
            if (metric != Cfg.DURATION && gm != null && r.val.get(p.key) < 0.6 * gm) r.cheap.add(p.key);
        }
        if (!r.screen.isEmpty()) {
            r.bestScreen = r.screen.get(0);
            r.rankScreen = r.rank.get(r.bestScreen.key);
        }
        if (metric == Cfg.DURATION) {
            r.label = "بیشترین اعتبار بسته";
            r.unit = "روز";
        } else if (metric == Cfg.PER_GB_MONTH) {
            r.label = "قیمت هر گیگ در ماه";
            r.unit = "تومان/گیگ‌ماه";
        } else if (metric == Cfg.MY_USAGE) {
            r.label = "هزینه‌ی ماهانه برای " + Fmt.dec(usage) + " گیگ";
            r.unit = "تومان/ماه";
        } else {
            r.label = "قیمت هر گیگ";
            r.unit = "تومان/گیگ";
        }
        return r;
    }
}
