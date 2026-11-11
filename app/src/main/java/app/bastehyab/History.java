package app.bastehyab;

import java.util.ArrayList;
import java.util.List;

/** Pure comparison of two observations; safe to use in parser-only tests. */
final class History {
    static final class Change {
        final String key, field;
        final double before, after;
        final long ts;
        Change(String key, String field, double before, double after, long ts) {
            this.key=key; this.field=field; this.before=before; this.after=after; this.ts=ts;
        }
        double percent() { return before > 0 ? (after-before)/before*100.0 : 0; }
    }
    static List<Change> diff(Pkg old, Pkg cur) {
        List<Change> out = new ArrayList<>();
        if (old == null || cur == null || !old.currencyKnown || !cur.currencyKnown
                || old.ambig || cur.ambig || old.key == null || !old.key.equals(cur.key)) return out;
        add(out, cur, "price", old.toman, cur.toman);
        add(out, cur, "gb", old.gb, cur.gb);
        add(out, cur, "days", old.days, cur.days);
        return out;
    }
    private static void add(List<Change> out, Pkg cur, String field, double before, double after) {
        if (before <= 0 || after <= 0 || before == after) return;
        out.add(new Change(cur.key, field, before, after, cur.ts));
    }
}
