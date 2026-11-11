package app.bastehyab;

import android.content.Context;
import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public final class ReviewQueueSmokeTest {
    private static void ok(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("bastehyab-review-test").toFile();
        Context context = new Context() {
            @Override public File getFilesDir() { return dir; }
        };

        List<Op> editCopy = Cfg.ops(context);
        ok(!editCopy.isEmpty(), "default operators are loaded");
        editCopy.get(0).enabled = false;
        ok(Cfg.ops(context).get(0).enabled, "mutating a read copy cannot alter cached operator settings");
        OpSnapshot snapshot = Cfg.snapshotForPackage(context, "com.myirancell");
        ok(snapshot != null && snapshot.enabled, "session snapshot captures current operator config");
        boolean immutable = false;
        try { snapshot.pkgs.add("com.fake.operator"); }
        catch (UnsupportedOperationException expected) { immutable = true; }
        ok(immutable, "operator package list is immutable inside a scan snapshot");

        Pkg ambiguous = new Pkg();
        ambiguous.op = "irancell";
        ambiguous.key = "review-fixture-1";
        ambiguous.title = "بسته آزمایشی ۱ گیگ";
        ambiguous.gb = 1;
        ambiguous.days = 30;
        ambiguous.toman = 150000;
        ambiguous.currencyKnown = false;
        ambiguous.ambig = true;
        ambiguous.ts = System.currentTimeMillis();
        Store.upsert(context, Collections.singletonList(ambiguous));
        Store.flush(context);

        List<Pkg> queue = Store.reviewQueue(context);
        ok(queue.size() == 1, "ambiguous packages enter the review queue");
        String sourceFingerprint = queue.get(0).sourceFingerprint();
        ok(Store.saveReview(context, ambiguous.key, true, 2.5, 45, 210000, "مبلغ از صفحه رسمی تطبیق داده شد"),
                "valid manual approval is stored");

        Pkg raw = Store.all(context).get(0);
        ok(raw.ambig && !raw.currencyKnown && raw.gb == 1 && raw.toman == 150000,
                "raw parser values remain unchanged after approval");
        ok(raw.sourceFingerprint().equals(sourceFingerprint), "source fingerprint is stable across review");
        Pkg effective = raw.comparisonVersion();
        ok(effective != raw && !effective.ambig && effective.currencyKnown
                        && effective.gb == 2.5 && effective.days == 45 && effective.toman == 210000,
                "approved values are exposed only through the comparison view");
        ok(Store.reviewQueue(context).isEmpty(), "approved package leaves pending review queue");
        Pkg staleApproval = Store.all(context).get(0);
        String originalTitle = staleApproval.title;
        staleApproval.title = originalTitle + " تغییر منبع";
        ok(!staleApproval.hasCurrentManualApproval(), "changed source fingerprint invalidates approval before a rescan");
        ok(Store.reviewQueue(context).size() == 1, "stale approval is returned to review queue instead of being hidden");
        staleApproval.title = originalTitle;
        List<HistoryLog.Entry> history = HistoryLog.recent(context, 10);
        ok(!history.isEmpty() && "review_approved".equals(history.get(0).field),
                "manual review decision is recorded in history");

        Pkg changed = new Pkg();
        changed.op = ambiguous.op; changed.key = ambiguous.key; changed.title = ambiguous.title;
        changed.gb = 1; changed.days = 30; changed.toman = 175000;
        changed.currencyKnown = false; changed.ambig = true;
        Store.upsert(context, Collections.singletonList(changed));
        Pkg refreshed = Store.all(context).get(0);
        ok(!refreshed.hasCurrentManualApproval() && refreshed.ambig,
                "a changed source observation invalidates the previous manual correction");
        ok(Store.reviewQueue(context).size() == 1, "changed ambiguous observation returns to review queue");
        ok(Store.saveReview(context, ambiguous.key, false, 0, 0, 0, "این پیشنهاد مناسب نیست"),
                "manual rejection is stored");
        Pkg rejected = new Pkg();
        rejected.op = ambiguous.op; rejected.key = ambiguous.key; rejected.title = ambiguous.title;
        rejected.gb = 1; rejected.days = 30; rejected.toman = 175000;
        rejected.currencyKnown = false; rejected.ambig = true;
        Store.upsert(context, Collections.singletonList(rejected));
        ok("rejected".equals(Store.all(context).get(0).reviewStatus),
                "same source observation keeps a user's rejection decision");
        ok(Store.reviewQueue(context).isEmpty(), "rejected proposal does not reappear after an unchanged scan");

        Cfg.sp(context).edit().putString("history", "not-json").apply();
        HistoryLog.recent(context, 5);
        ok(HistoryLog.hasError(context), "corrupt history is visible as a diagnostic error");
        ok(!HistoryLog.errorSummary(context).isEmpty(), "history error has a sanitized status summary");

        deleteTree(dir);
        System.out.println("PASS: review queue provenance, immutable operator snapshots, raw/effective separation, stale-review invalidation and history diagnostics.");
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        file.delete();
    }
}
