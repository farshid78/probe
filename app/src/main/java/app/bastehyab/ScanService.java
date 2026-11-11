package app.bastehyab;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScanService extends AccessibilityService {
    static volatile ScanService inst;
    static volatile String lastScanStatus = "هنوز اسکن انجام نشده است";
    static volatile int lastScannedPackages, lastValidPackages, lastSuspiciousPackages, lastDuplicatePackages;
    static volatile long lastCompletedScanAt;
    static volatile long lastScanDurationMs;
    static volatile String lastOpId = "";
    static volatile String lastEmptyReason = "";
    static volatile String lastPackageStatus = "";
    static volatile String lastScanError = "";
    static volatile int lastNodeCount;

    /** Single readable view of the session state (derived from the flags below; for UI/diagnostics). */
    static String phase() {
        if (!fullMode) return fullFinished ? "FINISHED" : "IDLE";
        if (!fullConfirmed) return "WAITING_CONFIRM";
        if (fullPaused) return "PAUSED";
        return fullStartedScanning ? "SCANNING" : "WAITING_PAGE";
    }

    // ---- user-started full extraction: the app scrolls the operator list ONLY after an explicit user request
    static volatile boolean fullMode;
    static volatile boolean fullStartedScanning;
    static volatile boolean fullTargetObservedOnce;
    static volatile String fullTargetOpId = "";
    static volatile String fullTargetPackage = "";
    static volatile long fullStartedAt;
    static volatile long fullLastStepAt;
    /** Earliest time the next scan may run; scroll events must not shorten this delay. */
    static volatile long fullNextScanAt;
    static volatile int fullSteps, fullNoNew;
    /** Post-confirmation page probes, retained for diagnostics and transient-root recovery. */
    static volatile int fullPageProbeCount;
    static final int FULL_PAGE_PROBE_LIMIT = 3;
    static volatile int fullNoMove, fullScrollStrategy;
    /** Best observed scroll offset from the operator's Accessibility scroll events. */
    static volatile int fullObservedScrollPercent = -1;
    static volatile int fullLastScrollY = -1, fullLastMaxScrollY = -1;
    static volatile int fullLastScrollItemCount = -1, fullLastScrollToIndex = -1;
    /** Timestamp of the latest target-app scroll metrics; stale events never prove the bottom. */
    static volatile long fullLastScrollEventAt = 0L;
    static volatile String lastScrollMethod = "هنوز اسکرولی انجام نشده";
    static volatile String lastGestureResult = "not-run";
    static volatile String lastPopupError = "";
    static volatile boolean fullFinished;
    static volatile ListEndDetector.State fullEndState = ListEndDetector.State.UNKNOWN;
    private static final ListEndDetector fullEndDetector = new ListEndDetector();
    private static volatile java.util.Map<String, OpSnapshot> fullOperatorSnapshot = java.util.Collections.emptyMap();
    /** The user must confirm (floating dialog) that the operator's internet-package page is open. */
    static volatile boolean fullConfirmed;
    /** Temporary pause preserves all collected packages and the current operator page. */
    static volatile boolean fullPaused;
    static volatile long fullPausedAt;
    /** The user chose "later": the dialog stays hidden until the next screen change in the operator app. */
    static volatile boolean promptDismissed;
    /** Short settle window between explicit confirmation and the first accessibility read. */
    static final long FULL_CONFIRM_DELAY_MS = 160L;
    /** Pause between the operator app appearing and the confirmation dialog showing. */
    static final long FULL_PROMPT_DELAY_MS = 900L;
    static final long FULL_PROMPT_RETRY_MS = 900L;
    // Keep retrying while the target operator is foregrounded; transitions can take longer
    // than a fixed seven-second retry window on slower devices.
    static final int FULL_PROMPT_MAX_ATTEMPTS = 240;
    static final long FULL_SCROLL_SETTLE_MS = 380L;
    /** If the page still looks unchanged after the settle delay, wait this long once more before counting "no move". */
    static final long FULL_SETTLE_RETRY_MS = 180L;
    private static volatile boolean settleRetried;
    static final long FULL_RESUME_DELAY_MS = 160L;
    static final java.util.Set<String> fullSeen = new HashSet<>();
    static final int FULL_MAX_STEPS = 120;
    static final long FULL_WAIT_MS = 240000L;
    static final long FULL_MAX_MS = 300000L;

    /** Start only for the exact package opened by the operator button; wait for a real offer page. */
    static synchronized void startFull(String operatorId, String targetPackage) {
        fullSeen.clear();
        fullSteps = 0;
        fullNoNew = 0;
        fullPageProbeCount = 0;
        fullNoMove = 0;
        fullEndDetector.reset();
        fullEndState = ListEndDetector.State.UNKNOWN;
        fullOperatorSnapshot = java.util.Collections.emptyMap();
        fullScrollStrategy = 0;
        fullObservedScrollPercent = -1;
        fullLastScrollY = -1;
        fullLastMaxScrollY = -1;
        fullLastScrollItemCount = -1;
        fullLastScrollToIndex = -1;
        fullLastScrollEventAt = 0L;
        lastScrollMethod = "هنوز اسکرولی انجام نشده";
        lastGestureResult = "not-run";
        lastPopupError = "";
        fullLastFingerprint = "";
        settleRetried = false;
        fullFinished = false;
        fullConfirmed = false;
        fullPaused = false;
        fullPausedAt = 0L;
        promptDismissed = false;
        fullStartedScanning = false;
        fullTargetObservedOnce = false;
        fullTargetOpId = operatorId == null ? "" : operatorId.trim();
        fullTargetPackage = targetPackage == null ? "" : targetPackage.trim();
        ScanService sessionService = inst;
        if (sessionService != null) {
            java.util.Map<String, OpSnapshot> snapshot = new java.util.LinkedHashMap<>();
            for (OpSnapshot op : Cfg.snapshotOps(sessionService)) snapshot.put(op.id, op);
            fullOperatorSnapshot = java.util.Collections.unmodifiableMap(snapshot);
        }
        // Navigation time is not part of the five-minute extraction window.
        fullStartedAt = 0L;
        fullLastStepAt = 0L;
        fullNextScanAt = 0L;
        lastPackageStatus = "";
        if (fullTargetOpId.isEmpty() || fullTargetPackage.isEmpty()) {
            fullMode = false;
            lastScanStatus = "استخراج خودکار شروع نشد؛ شناسه‌ی اپراتور یا برنامه معتبر نیست";
            return;
        }
        fullMode = true;
        lastScanStatus = "استخراج خودکار آماده است؛ وارد بخش بسته‌های اینترنت شوید و در پنجره‌ی شناور تایید کنید";
        if (inst != null) {
            inst.h.removeCallbacks(inst.promptRunnable);
            inst.h.removeCallbacks(inst.controlsRetry);
            inst.promptPending = false;
            inst.promptAttempts = 0;
            if (inst.popup != null) inst.popup.hideConfirm();
            // The floating comparison window is touchable. Keeping it visible over an app
            // can swallow the very swipe used to scroll that app, especially in Irancell/MCI.
            if (inst.popup != null) inst.popup.hide();
            inst.h.removeCallbacks(inst.fullTimeout);
            inst.h.postDelayed(inst.fullTimeout, FULL_WAIT_MS);
            // Do not rely solely on TYPE_WINDOW_STATE_CHANGED from the operator app. Some
            // OEMs/Compose transitions omit or delay that event; launch already succeeded
            // in MainActivity, so make one independent attempt while the app is opening.
            inst.promptPending = true;
            inst.h.postDelayed(inst.promptRunnable, FULL_PROMPT_DELAY_MS + 450L);
        }
    }

    static boolean isFullTarget(String packageName) {
        return !fullTargetPackage.isEmpty() && fullTargetPackage.equals(packageName);
    }

    private static OpSnapshot fullSnapshotForPackage(String packageName) {
        for (OpSnapshot op : fullOperatorSnapshot.values()) {
            if (op.ownsPackage(packageName)) return op;
        }
        return null;
    }

    private Op operatorForScan(String packageName, boolean fullAtStart) {
        if (fullAtStart) {
            OpSnapshot snapshot = fullSnapshotForPackage(packageName);
            return snapshot == null ? null : snapshot.toOp();
        }
        return Cfg.opFor(this, packageName);
    }

    /** Ignore window changes until the target operator app was actually reached. */
    static boolean shouldStopFullForWindowChange(String packageName) {
        return fullMode && !fullPaused && fullTargetObservedOnce && !ignorable(packageName == null ? "" : packageName)
                && !isFullTarget(packageName == null ? "" : packageName);
    }

    static synchronized void stopFull(String reason) {
        stopFull(reason, false);
    }

    /** Stop a scan and optionally leave a readable final report over the operator app. */
    static synchronized void stopFull(String reason, boolean showFinalReport) {
        if (!fullMode) return;
        OpSnapshot targetSnapshot = fullOperatorSnapshot.get(fullTargetOpId);
        String targetName = targetSnapshot == null ? "اپراتور" : targetSnapshot.name;
        fullMode = false;
        // A stationary viewport does not prove that the end of the list was reached.
        // Completion is derived from typed evidence, never from a localized reason string.
        fullFinished = fullEndState == ListEndDetector.State.END_CONFIRMED;
        fullConfirmed = false;
        fullPaused = false;
        fullPausedAt = 0L;
        fullNextScanAt = 0L;
        promptDismissed = false;
        if (inst != null) {
            inst.h.removeCallbacks(inst.fullTimeout);
            inst.h.removeCallbacks(inst.promptRunnable);
            inst.h.removeCallbacks(inst.controlsRetry);
            inst.cancelScheduledScan();
            inst.promptPending = false;
            inst.scanAgain = false;
            if (!showFinalReport && inst.popup != null) inst.popup.hideConfirm();
        }
        lastScanStatus = (reason == null ? "استخراج خودکار متوقف شد" : reason)
                + " • " + fullSeen.size() + " بسته‌ی یکتا";
        if (inst != null) {
            Store.flush(inst);
            if (showFinalReport) inst.showFinalScanStatus(targetName);
        }
        fullOperatorSnapshot = java.util.Collections.emptyMap();
    }

    private void showFinalScanStatus(String operatorName) {
        if (popup == null) return;
        String detail = lastScanStatus;
        if (lastScrollMethod != null && !lastScrollMethod.trim().isEmpty()
                && !"هنوز اسکرولی انجام نشده".equals(lastScrollMethod)) {
            detail += "\nآخرین روش: " + lastScrollMethod;
        }
        popup.showConfirm(operatorName, 3, fullProgressPercent(), fullSteps, fullSeen.size(),
                detail, ScanService::restartFullFromFinal, () -> popup.hideConfirm(), null);
    }

    /** Starts a fresh full-scan session for the same selected operator from the final report. */
    static synchronized void restartFullFromFinal() {
        if (fullMode) return;
        String operatorId = fullTargetOpId;
        String targetPackage = fullTargetPackage;
        if (operatorId == null || operatorId.trim().isEmpty()
                || targetPackage == null || targetPackage.trim().isEmpty()) return;
        startFull(operatorId, targetPackage);
        // The explicit "new scan" button itself is the user's authorization.
        confirmFull();
    }

    /** Called from the floating panel after the user confirms the internet-package page. */
    static synchronized void confirmFull() {
        if (!fullMode || fullConfirmed || fullPaused) return;
        fullConfirmed = true;
        // Drop scroll measurements collected while the user was navigating through the
        // operator app. End-of-list proof must describe the confirmed package page only.
        fullLastScrollY = -1;
        fullLastMaxScrollY = -1;
        fullLastScrollItemCount = -1;
        fullLastScrollToIndex = -1;
        fullLastScrollEventAt = 0L;
        fullObservedScrollPercent = -1;
        fullEndDetector.reset();
        fullEndState = ListEndDetector.State.UNKNOWN;
        ScanService s = inst;
        if (s != null) {
            // Confirmation authorizes scanning, but it is not proof that the target window
            // is already readable. Keep the session in WAITING_PAGE until a target root is
            // actually observed by the shared operator scan pipeline. This is the same
            // parser/worker path used for MCI and the other operators.
            s.ensureFloatingControls();
            lastScanStatus = "تأیید شد؛ در حال اتصال به صفحه و خواندن اطلاعات قابل‌دسترسی";
            fullNextScanAt = System.currentTimeMillis() + FULL_CONFIRM_DELAY_MS;
            s.scheduleScanReplacing(FULL_CONFIRM_DELAY_MS);
            s.h.removeCallbacks(s.fullTimeout);
            s.h.postDelayed(s.fullTimeout, FULL_WAIT_MS);
        }
    }

    /** Pause without clearing the seen set, counters, target app, or current viewport. */
    static synchronized void pauseFull() {
        if (!fullMode || !fullConfirmed || fullPaused) return;
        fullPaused = true;
        fullPausedAt = System.currentTimeMillis();
        lastScanStatus = "اسکن موقتاً متوقف شد؛ صفحه و بسته‌های شناسایی‌شده حفظ شدند";
        ScanService s = inst;
        if (s != null) {
            s.cancelScheduledScan();
            s.h.removeCallbacks(s.fullTimeout);
            s.scanAgain = false;
            s.ensureFloatingControls();
            Store.flush(s);
        }
    }

    /** Resume from the visible page; paused time does not consume the scan time limit. */
    static synchronized void resumeFull() {
        if (!fullMode || !fullConfirmed || !fullPaused) return;
        ScanService s = inst;
        if (s != null && !isFullTarget(s.fg)) {
            lastScanStatus = "برای ادامه، ابتدا به برنامه‌ی اپراتور برگردید؛ اطلاعات اسکن حفظ شده است";
            s.ensureFloatingControls();
            return;
        }
        long now = System.currentTimeMillis();
        long pausedFor = fullPausedAt > 0L ? Math.max(0L, now - fullPausedAt) : 0L;
        if (fullStartedAt > 0L) fullStartedAt += pausedFor;
        fullPaused = false;
        fullPausedAt = 0L;
        lastScanStatus = "ادامه‌ی اسکن از صفحه‌ی فعلی";
        if (s != null) {
            s.ensureFloatingControls();
            fullNextScanAt = now + FULL_RESUME_DELAY_MS;
            s.h.removeCallbacks(s.fullTimeout);
            long remaining = fullStartedAt > 0L
                    ? Math.max(1000L, FULL_MAX_MS - Math.max(0L, now - fullStartedAt))
                    : FULL_WAIT_MS;
            s.h.postDelayed(s.fullTimeout, remaining);
            s.scheduleScanReplacing(FULL_RESUME_DELAY_MS);
        }
    }

    static void togglePauseFull() {
        if (fullPaused) resumeFull(); else pauseFull();
    }

    /** "Later" hides the pre-start panel without cancelling; it returns on a screen change. */
    static void postponePrompt() {
        if (!fullMode || fullConfirmed) return;
        promptDismissed = true;
        lastScanStatus = "پنجره‌ی تایید پنهان شد؛ با تغییر صفحه‌ی اپراتور دوباره نمایش داده می‌شود";
        ScanService s = inst;
        if (s != null) {
            s.h.removeCallbacks(s.controlsRetry);
            if (s.popup != null) s.popup.hideConfirm();
        }
    }

    static boolean shouldPrompt(boolean dismissed, boolean windowStateChanged) {
        return !dismissed || windowStateChanged;
    }

    /**
     * The user explicitly confirms that the package page is open. Keep the text-based page
     * detector as the preferred path, but do not let a changed operator UI block scrolling
     * forever when its accessibility labels are different or absent.
     */
    static boolean shouldUseConfirmedPageFallback(boolean confirmed, boolean pageRecognized, int probes) {
        return confirmed && !pageRecognized && probes >= FULL_PAGE_PROBE_LIMIT;
    }

    /** All operators use the same MCI reference order: accessibility action first, then gestures. */
    static boolean shouldUseAccessibilityScroll(int strategy) {
        return ScanStrategyPolicy.shouldUseAccessibilityActionFirst(strategy);
    }

    /** After repeated no-movement observations, prefer a swipe across the app content viewport. */
    static boolean shouldPreferAppViewportSwipe(int strategy) {
        return ScanStrategyPolicy.shouldPreferAppViewportSwipe(strategy);
    }

    /** Computes event debounce without allowing view-scroll noise to bypass the next-step lock. */
    static long eventScanDelay(long now, long lastScan, boolean full, boolean scrollEvent,
                               long nextFullScanAt) {
        long floor = full ? 250L : (scrollEvent ? 220L : 450L);
        long minimum = full ? 100L : (scrollEvent ? 120L : 180L);
        long eligibleAt = lastScan + floor;
        if (full) eligibleAt = Math.max(eligibleAt, nextFullScanAt);
        return Math.max(minimum, eligibleAt - now);
    }

    /** Keep the earliest queued scan deadline; event bursts must never postpone a pending scan. */
    static long coalescedScanDeadline(long existingDueAt, long requestedDueAt) {
        if (existingDueAt <= 0L) return requestedDueAt;
        if (requestedDueAt <= 0L) return existingDueAt;
        return Math.min(existingDueAt, requestedDueAt);
    }

    static void cancelFull() {
        ScanService service = inst;
        // 1) Bring Bastehyab to the front while our overlay still counts as a visible window;
        //    Android blocks activity starts from the background once it has been removed.
        if (service != null) service.returnToMainActivity();
        // 2) Stop the session (no-op when already stopped) ...
        stopFull("استخراج خودکار با انتخاب شما خارج شد");
        // 3) ... and always remove the floating windows, even if the session had already ended.
        if (service != null) {
            service.h.removeCallbacks(service.controlsRetry);
            service.h.removeCallbacks(service.promptRunnable);
            service.promptPending = false;
            if (service.popup != null) {
                service.popup.hideConfirm();
                service.popup.hide();
            }
        }
    }

    /** The Exit control must return to Bastehyab, not merely hide the floating panel. */
    private void returnToMainActivity() {
        if (!launchMainActivity()) {
            // One more attempt after the click has been fully processed.
            h.postDelayed(() -> { launchMainActivity(); }, 250L);
        }
    }

    private boolean launchMainActivity() {
        try {
            Intent intent = new Intent();
            intent.setClassName(getPackageName(), getPackageName() + ".MainActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        } catch (RuntimeException error) {
            android.util.Log.w("BastehyabScan", "Could not return to main activity: "
                    + error.getClass().getSimpleName());
            return false;
        }
    }

    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean promptPending;
    private int promptAttempts;
    // Keep fields used by field-initialized callbacks declared before those callbacks.
    private final Map<String, List<String>> dumps = new HashMap<>();
    private Popup popup;
    private long lastScanAt;
    private static volatile String fullLastFingerprint = "";
    private final Runnable writeDiag = this::writeDiagnosticFile;
    private int emptyStreak;
    private boolean scanning, scanAgain;
    /** One coalesced scan callback; accessibility event storms cannot keep moving its deadline. */
    private boolean jobScheduled;
    private long jobDueAt;
    private String fg = "", dismissed = "";
    /** Heavy accessibility-tree parsing runs away from the service/UI thread. */
    private volatile ExecutorService scanExecutor;

    private static final class DiagnosticSnapshot {
        final String packageName, dump;
        DiagnosticSnapshot(String packageName, String dump) {
            this.packageName = packageName;
            this.dump = dump;
        }
    }

    private static final class ScanOutcome {
        boolean fullModeAtStart;
        String targetPackage = "";
        int scanned, valid, suspicious, duplicates, emptyStreak;
        boolean incomplete, treeComplete, treeEvidenceSeen, fullTargetObserved, packagePageFound, hideComparison, hasLoadMoreIndicator;
        int nodes;
        String fullFingerprint = "", lastEmptyReason = "", operatorLabel = "", packageStatus = "";
        String error = "";
        final List<String> fullSignatures = new ArrayList<>();
        final java.util.SortedSet<String> fullRootFingerprints = new java.util.TreeSet<>();
        final List<DiagnosticSnapshot> diagnostics = new ArrayList<>();
        Compare.Result comparison;
        Set<String> comparisonKeys;
        long durationMs;
    }

    private final Runnable promptRunnable = () -> {
        promptPending = false;
        if (!fullMode || fullConfirmed || fullPaused || promptDismissed) return;
        if (refreshFloatingControls()) {
            promptAttempts = 0;
            lastPopupError = "";
            scheduleControlsWatchdog();
            return;
        }
        promptAttempts++;
        String error = popup == null ? "سرویس پنجره شناور آماده نیست" : popup.confirmError();
        lastPopupError = error == null ? "خطای نامشخص" : error;
        lastScanStatus = "نمایش پنجره‌ی کنترل ناموفق بود؛ تلاش مجدد "
                + Fmt.fa(String.valueOf(promptAttempts)) + " از "
                + Fmt.fa(String.valueOf(FULL_PROMPT_MAX_ATTEMPTS));
        if (promptAttempts < FULL_PROMPT_MAX_ATTEMPTS) {
            schedulePromptRetry();
        } else {
            lastScanStatus = "پنجره‌ی شناور نمایش داده نشد؛ سرویس دسترسی بسته‌یاب را بررسی کنید ("
                    + lastPopupError + ")";
        }
    };
    private final Runnable fullTimeout = () -> {
        if (fullMode && !fullPaused) stopFull(fullStartedScanning
                ? "توقف: مهلت پنج‌دقیقه‌ای استخراج به پایان رسید"
                : "توقف: مهلت ورود به صفحه‌ی بسته‌های اینترنت به پایان رسید", true);
    };
    private final Runnable job = () -> {
        jobScheduled = false;
        jobDueAt = 0L;
        scan();
    };

    /** Queue a scan once. A later accessibility event may pull it forward, never push it back. */
    private void scheduleScan(long delayMs) {
        long now = System.currentTimeMillis();
        long requestedDue = now + Math.max(0L, delayMs);
        if (scanning) {
            scanAgain = true;
            return;
        }
        long due = jobScheduled ? coalescedScanDeadline(jobDueAt, requestedDue) : requestedDue;
        if (jobScheduled && due == jobDueAt) return;
        if (jobScheduled) h.removeCallbacks(job);
        jobDueAt = due;
        jobScheduled = true;
        h.postDelayed(job, Math.max(0L, due - now));
    }

    /** Explicit state transitions (confirm/resume/settle) may replace a queued deadline. */
    private void scheduleScanReplacing(long delayMs) {
        if (scanning) {
            scanAgain = true;
            return;
        }
        if (jobScheduled) cancelScheduledScan();
        jobDueAt = System.currentTimeMillis() + Math.max(0L, delayMs);
        jobScheduled = true;
        h.postDelayed(job, Math.max(0L, delayMs));
    }

    private void cancelScheduledScan() {
        h.removeCallbacks(job);
        jobScheduled = false;
        jobDueAt = 0L;
    }

    private void schedulePromptRetry() {
        promptPending = true;
        h.postDelayed(promptRunnable, FULL_PROMPT_RETRY_MS);
    }

    /** Rebuilds the one floating control panel for the current extraction state. */
    private boolean refreshFloatingControls() {
        if (!fullMode || popup == null) return false;
        OpSnapshot sessionTarget = fullOperatorSnapshot.get(fullTargetOpId);
        Op target = sessionTarget == null ? Cfg.opById(this, fullTargetOpId) : sessionTarget.toOp();
        int mode = !fullConfirmed ? 0 : (fullPaused ? 2 : 1);
        int percent = fullProgressPercent();
        boolean shown = popup.showConfirm(target == null ? "اپراتور" : target.name, mode,
                percent, fullSteps, fullSeen.size(), lastScanStatus,
                mode == 0 ? ScanService::confirmFull : ScanService::togglePauseFull,
                ScanService::cancelFull,
                mode == 0 ? ScanService::postponePrompt : null);
        lastPopupError = shown ? "" : popup.confirmError();
        return shown;
    }


    /** Best-effort progress: real scroll offsets when available, otherwise completed scan steps. */
    static int fullProgressPercent() {
        if (fullFinished) return 100;
        int stepPercent = fullSteps <= 0 ? 0
                : Math.min(95, Math.max(1, fullSteps * 100 / FULL_MAX_STEPS));
        // Reaching a 100% viewport offset is not by itself proof that a virtualized list is done.
        // Only stopFull() after END_CONFIRMED may expose 100% completion.
        int observedPercent = Math.max(0, Math.min(99, fullObservedScrollPercent));
        return Math.max(stepPercent, observedPercent);
    }

    private final Runnable controlsRetry = this::retryFloatingControls;

    private void scheduleControlsWatchdog() {
        if (!fullMode || (promptDismissed && !fullConfirmed)) return;
        h.removeCallbacks(controlsRetry);
        h.postDelayed(controlsRetry, 1800L);
    }

    private void ensureFloatingControls() {
        if (!fullMode || (promptDismissed && !fullConfirmed)) return;
        h.removeCallbacks(controlsRetry);
        if (refreshFloatingControls()) scheduleControlsWatchdog();
        else h.postDelayed(controlsRetry, FULL_PROMPT_RETRY_MS);
    }

    private void retryFloatingControls() {
        if (!fullMode || (promptDismissed && !fullConfirmed)) return;
        if (refreshFloatingControls()) scheduleControlsWatchdog();
        else h.postDelayed(controlsRetry, FULL_PROMPT_RETRY_MS);
    }

    @Override public void onInterrupt() {
        cancelScheduledScan();
        pendingReset();
        stopFull("سرویس دسترسی‌پذیری متوقف شد");
        Store.flush(this);
        if (popup != null) { popup.hide(); popup.hideConfirm(); }
    }

    private void pendingReset() {
        scanAgain = false;
        scanning = false;
    }

    @Override protected void onServiceConnected() {
        inst = this;
        // Remove a possible raw diagnostic file left by versions before the privacy fix.
        File legacyDiagnostic = new File(getFilesDir(), "diag.txt");
        if (legacyDiagnostic.exists()) legacyDiagnostic.delete();
        dumps.clear();
        ExecutorService previous = scanExecutor;
        if (previous != null) previous.shutdownNow();
        scanExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Bastehyab-ScanWorker");
            t.setDaemon(true);
            return t;
        });
        popup = new Popup(this);
        popup.onClose(() -> dismissed = fg);
    }

    @Override public void onDestroy() {
        // Idempotent safety net: never leave windows, callbacks or the worker thread behind.
        h.removeCallbacksAndMessages(null);
        pendingReset();
        stopFull("سرویس دسترسی‌پذیری بسته شد");
        ExecutorService worker = scanExecutor;
        scanExecutor = null;
        if (worker != null) worker.shutdownNow();
        if (popup != null) { popup.hide(); popup.hideConfirm(); }
        Store.flush(this);
        if (inst == this) inst = null;
        super.onDestroy();
    }

    @Override public boolean onUnbind(Intent i) {
        h.removeCallbacksAndMessages(null);
        pendingReset();
        stopFull("سرویس دسترسی‌پذیری غیرفعال شد");
        ExecutorService worker = scanExecutor;
        scanExecutor = null;
        if (worker != null) worker.shutdownNow();
        Store.flush(this);
        inst = null;
        dumps.clear();
        if (popup != null) { popup.hide(); popup.hideConfirm(); }
        return super.onUnbind(i);
    }

    private static boolean ignorable(String p) {
        String s = p.toLowerCase();
        return s.equals("com.android.systemui") || s.contains("inputmethod") || s.contains("keyboard") || s.contains("honeyboard");
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        CharSequence cs = e.getPackageName();
        if (cs == null) return;
        String p = cs.toString();
        if (fullMode && isFullTarget(p)) {
            fullTargetObservedOnce = true;
            if (e.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                int maxY = e.getMaxScrollY();
                int scrollY = e.getScrollY();
                int itemCount = e.getItemCount();
                int toIndex = e.getToIndex();
                // Replace, rather than retain, the latest event snapshot. Some providers emit
                // range-less scroll events after switching between native and Compose lists;
                // stale offsets from a previous screen must not prove this screen is at bottom.
                fullLastMaxScrollY = maxY;
                fullLastScrollY = scrollY;
                fullLastScrollItemCount = itemCount;
                fullLastScrollToIndex = toIndex;
                fullLastScrollEventAt = System.currentTimeMillis();
                if (maxY > 0) {
                    int observed = Math.max(0, Math.min(100, (int) ((long) scrollY * 100L / maxY)));
                    fullObservedScrollPercent = Math.max(fullObservedScrollPercent, observed);
                }
            }
        }
        boolean screenChange = e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        // Leaving the operator normally ends extraction, but a deliberate pause preserves it.
        if (screenChange && shouldStopFullForWindowChange(p)
                && !(p.equals(getPackageName()) && Popup.recentOverlayChange())) {
            stopFull("استخراج خودکار متوقف شد؛ از برنامه‌ی اپراتور خارج شدید");
        }
        if (screenChange && !ignorable(p)
                && !(p.equals(getPackageName()) && Popup.recentOverlayChange())) {
            fg = p;
            if (!p.equals(dismissed)) dismissed = "";
            if (!p.equals(getPackageName()) && popup != null) popup.hide();
        }
        // Only re-create the panel when it is missing; per-event refreshes flooded WindowManager.
        if (fullMode && fullConfirmed && isFullTarget(p)
                && (popup == null || !popup.isConfirmShown())) ensureFloatingControls();
        if (fullMode && !fullConfirmed && isFullTarget(p) && !promptPending
                && shouldPrompt(promptDismissed, screenChange)) {
            if (screenChange && promptAttempts >= FULL_PROMPT_MAX_ATTEMPTS) promptAttempts = 0;
            promptDismissed = false;
            promptPending = true;
            h.postDelayed(promptRunnable, FULL_PROMPT_DELAY_MS);
        }
        if (p.equals(getPackageName())) return;
        if (fullMode && fullPaused) return;
        Op op = fullMode ? operatorForScan(p, true) : Cfg.opFor(this, p);
        if (op == null || !op.enabled) return;
        long now = System.currentTimeMillis();
        // Accessibility scroll events can arrive repeatedly while an operator page is still
        // rendering. They may reschedule a scan, but never before fullNextScanAt.
        boolean scrollEvent = e.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED;
        long delay = eventScanDelay(now, lastScanAt, fullMode, scrollEvent, fullNextScanAt);
        // Preserve the first pending deadline instead of restarting it on every accessibility event.
        scheduleScan(delay);
    }

    /**
     * Debounced entry point on the service thread. Only window handles are traversed by the
     * worker; parsing, fingerprinting, persistence and comparison no longer block Android UI.
     */
    private void scan() {
        if (fullMode && fullPaused) return;
        if (fullMode && !fullConfirmed) {
            lastScanStatus = "در انتظار تایید شما در پنجره‌ی شناور؛ وارد بخش بسته‌های اینترنت شوید";
            return;
        }
        if (scanning) {
            scanAgain = true;
            return;
        }
        ExecutorService worker = scanExecutor;
        if (worker == null || worker.isShutdown()) {
            lastScanStatus = "پردازشگر اسکن آماده نیست؛ تلاش مجدد پس از بازیابی سرویس";
            if (fullMode && fullConfirmed && !fullPaused) scheduleScan(250L);
            return;
        }

        scanning = true;
        lastScanAt = System.currentTimeMillis();
        if (fullMode && popup != null) popup.hide();
        if (fullMode && fullConfirmed) {
            lastScanStatus = fullStartedScanning
                    ? "در حال استخراج بسته‌های صفحه‌ی فعلی..."
                    : "در حال خواندن ساختار صفحه برای شروع استخراج...";
        }
        final boolean fullAtStart = fullMode;
        final String targetAtStart = fullTargetPackage;
        final String dismissedAtStart = dismissed;
        final int emptyStreakAtStart = emptyStreak;
        final long startedAt = android.os.SystemClock.elapsedRealtime();
        try {
            worker.execute(() -> {
                ScanOutcome outcome = performScanWork(fullAtStart, targetAtStart,
                        dismissedAtStart, emptyStreakAtStart, startedAt);
                h.post(() -> finishScan(outcome));
            });
        } catch (RejectedExecutionException rejected) {
            scanning = false;
            lastScanStatus = "پردازش اسکن پذیرفته نشد؛ دوباره تلاش می‌شود";
            if (fullMode && fullConfirmed && !fullPaused) scheduleScan(250L);
        }
    }

    /** CPU- and I/O-heavy part; deliberately contains no Popup/WindowManager operations. */
    private ScanOutcome performScanWork(boolean fullAtStart, String targetAtStart,
                                        String dismissedAtStart, int initialEmptyStreak,
                                        long startedAt) {
        ScanOutcome out = new ScanOutcome();
        out.fullModeAtStart = fullAtStart;
        out.targetPackage = targetAtStart == null ? "" : targetAtStart;
        out.emptyStreak = initialEmptyStreak;
        Set<String> scanKeys = new HashSet<>();
        try {
            List<AccessibilityNodeInfo> roots = new ArrayList<>();
            List<AccessibilityWindowInfo> windows = null;
            try { windows = getWindows(); }
            catch (RuntimeException windowError) {
                android.util.Log.w("BastehyabScan", "Window enumeration failed; trying active root: "
                        + windowError.getClass().getSimpleName());
            }
            if (windows != null) {
                for (AccessibilityWindowInfo window : windows) {
                    if (window == null) continue;
                    try {
                        if (window.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                            AccessibilityNodeInfo root = window.getRoot();
                            if (root != null) roots.add(root);
                        }
                    } catch (RuntimeException staleWindow) {
                        android.util.Log.w("BastehyabScan", "Skipping stale accessibility window: "
                                + staleWindow.getClass().getSimpleName());
                    } finally {
                        try { window.recycle(); } catch (RuntimeException ignored) {}
                    }
                }
            }
            if (roots.isEmpty()) {
                try {
                    AccessibilityNodeInfo root = getRootInActiveWindow();
                    if (root != null) roots.add(root);
                } catch (RuntimeException activeRootError) {
                    android.util.Log.w("BastehyabScan", "Active root fallback failed: "
                            + activeRootError.getClass().getSimpleName());
                }
            }

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            for (AccessibilityNodeInfo root : roots) {
                try {
                    String packageName = String.valueOf(root.getPackageName());
                    if (fullAtStart && !isFullTarget(packageName)) continue;
                    Op op = operatorForScan(packageName, fullAtStart);
                    if (op == null || !op.enabled) continue;
                    if (fullAtStart) {
                        out.fullTargetObserved = true;
                    }

                    Extractor.Result result = Extractor.extract(root, op, metrics.heightPixels, fullAtStart);
                    out.operatorLabel = op.name;
                    out.nodes += result.nodes;
                    out.diagnostics.add(new DiagnosticSnapshot(packageName, result.dump));
                    out.incomplete |= result.incomplete;
                    if (fullAtStart) {
                        if (!out.treeEvidenceSeen) {
                            out.treeComplete = result.treeComplete;
                            out.treeEvidenceSeen = true;
                        } else {
                            out.treeComplete = out.treeComplete && result.treeComplete;
                        }
                    }
                    out.hasLoadMoreIndicator |= result.hasLoadMoreIndicator;
                    out.duplicates += result.duplicates;

                    for (Pkg item : result.items) {
                        if (!scanKeys.add(item.key)) {
                            out.duplicates++;
                            continue;
                        }
                        out.scanned++;
                        if (item.currencyKnown && !item.ambig) out.valid++;
                        else out.suspicious++;
                    }

                    // Offer signatures alone are insufficient: some apps expose an entire list
                    // while the viewport is moving. Combine them with stable visible text + position.
                    if (fullAtStart) {
                        String offersFp = itemsFingerprint(result.items);
                        String viewportFp = pageFingerprint(root);
                        String rootFp = offersFp.isEmpty() ? viewportFp
                                : offersFp + "|" + viewportFp;
                        if (!rootFp.isEmpty()) out.fullRootFingerprints.add(rootFp);
                    }
                    boolean offerPage = fullAtStart && result.items.isEmpty() && looksLikeOfferPage(root);
                    if (offerPage) out.packagePageFound = true;
                    if (result.items.isEmpty()) {
                        out.lastEmptyReason = "گره‌ها: " + result.nodes + "، متن‌های قابل‌مشاهده: " + result.leaves
                                + (result.fullTraversal ? "، پیمایش کامل" : "");
                        out.emptyStreak++;
                        if (out.emptyStreak >= 3) out.hideComparison = true;
                        continue;
                    }

                    out.packagePageFound = true;
                    out.emptyStreak = 0;
                    Store.upsert(this, result.items);

                    boolean hasDefinitivePrice = false;
                    for (int i = result.items.size() - 1; i >= 0; i--) {
                        Pkg latest = result.items.get(i);
                        if (latest.currencyKnown && !latest.ambig && latest.toman >= 100) {
                            out.packageStatus = latest.title + " • "
                                    + Cfg.priceLabel(this, latest.toman) + " ثبت شد";
                            hasDefinitivePrice = true;
                            break;
                        }
                    }
                    if (!hasDefinitivePrice) {
                        Pkg uncertain = result.items.get(result.items.size() - 1);
                        out.packageStatus = uncertain.title + " • استخراج شد و برای بررسی دستی نگهداری شد";
                    }
                    if (fullAtStart) {
                        // Merge into the cross-page set on the main thread after this batch completes.
                        for (Pkg item : result.items) out.fullSignatures.add(item.sig());
                    }

                    // Full extraction only collects and persists offers. Ranking the same page
                    // after every scroll is expensive and its comparison popup is intentionally
                    // hidden until the full scan ends; defer it to the normal one-page path.
                    if (!fullAtStart) {
                        Compare.Result comparison = Compare.run(this, result.keys);
                        if (Cfg.popup(this) && !packageName.equals(dismissedAtStart)
                                && !comparison.ranked.isEmpty()
                                && comparison.ops >= Cfg.minOps(this)) {
                            out.comparison = comparison;
                            out.comparisonKeys = new HashSet<>(result.keys);
                        }
                    }
                } finally {
                    root.recycle();
                }
            }
        } catch (RuntimeException error) {
            out.error = error.getClass().getSimpleName();
        } finally {
            out.durationMs = Math.max(0L, android.os.SystemClock.elapsedRealtime() - startedAt);
        }
        if (!out.fullRootFingerprints.isEmpty()) {
            StringBuilder combined = new StringBuilder("roots:");
            for (String fp : out.fullRootFingerprints) {
                if (combined.length() > 6) combined.append("||");
                combined.append(fp);
            }
            out.fullFingerprint = combined.toString();
        }
        return out;
    }

    private void beginFullScanning(String status) {
        if (fullStartedScanning) return;
        fullStartedScanning = true;
        fullStartedAt = System.currentTimeMillis();
        fullNoNew = 0;
        h.removeCallbacks(fullTimeout);
        h.postDelayed(fullTimeout, FULL_MAX_MS);
        lastScanStatus = status;
    }

    /** Applies the worker result and performs all visible-window and gesture operations. */
    private void finishScan(ScanOutcome out) {
        if (out == null) {
            scanning = false;
            if (fullMode && fullConfirmed && !fullPaused) scheduleScan(250L);
            return;
        }
        lastScanDurationMs = out.durationMs;
        lastScanError = out.error;
        lastNodeCount = out.nodes;
        if (inst != this) {
            scanning = false;
            return;
        }

        lastScannedPackages = out.scanned;
        lastValidPackages = out.valid;
        lastSuspiciousPackages = out.suspicious;
        lastDuplicatePackages = out.duplicates;
        lastCompletedScanAt = System.currentTimeMillis();
        emptyStreak = out.emptyStreak;
        if (!out.lastEmptyReason.isEmpty()) lastEmptyReason = out.lastEmptyReason;
        if (!out.packageStatus.isEmpty()) lastPackageStatus = out.packageStatus;
        for (DiagnosticSnapshot snapshot : out.diagnostics) diag(snapshot.packageName, snapshot.dump);

        boolean sameFullSession = out.fullModeAtStart && fullMode
                && out.targetPackage.equals(fullTargetPackage);
        boolean staleSession = (out.fullModeAtStart && !sameFullSession)
                || (!out.fullModeAtStart && fullMode);
        int fullNew = 0;
        if (sameFullSession && out.fullTargetObserved) {
            fullTargetObservedOnce = true;
            for (String signature : out.fullSignatures) if (fullSeen.add(signature)) fullNew++;
        }

        if (!staleSession && !(sameFullSession && fullPaused)) {
            if (!out.error.isEmpty()) {
                lastScanStatus = "خطا در اسکن: " + out.error;
                android.util.Log.w("BastehyabScan", "Accessibility scan failed: " + out.error);
                if (sameFullSession && !fullStartedScanning && fullConfirmed
                        && (out.fullTargetObserved || fullTargetObservedOnce)) {
                    fullPageProbeCount++;
                    if (shouldUseConfirmedPageFallback(true, false, fullPageProbeCount)) {
                        beginFullScanning("استخراج صفحه با خطای موقت روبه‌رو شد؛ پس از تأیید شما پیمایش تطبیقی آغاز شد");
                    }
                }
            } else if (sameFullSession) {
                boolean targetWindowSeen = out.fullTargetObserved || fullTargetObservedOnce;
                if (!fullStartedScanning && targetWindowSeen) {
                    fullPageProbeCount = 0;
                    beginFullScanning(out.packagePageFound
                            ? "صفحه‌ی بسته‌ها شناسایی شد؛ استخراج خودکار آغاز شد"
                            : "صفحه با تأیید شما انتخاب شد؛ استخراج و پیمایش خودکار آغاز شد");
                } else {
                    lastScanStatus = (out.incomplete ? "استخراج ناقص؛ بخشی از درخت صفحه در دسترس نبود"
                            : out.scanned == 0 ? "در صفحه‌ی فعلی بسته‌ای شناسایی نشد (" + lastEmptyReason + ")"
                            : "اسکن کامل شد")
                            + (out.operatorLabel.isEmpty() ? "" : " • " + out.operatorLabel);
                }
            } else {
                lastScanStatus = (out.incomplete ? "استخراج ناقص؛ بخشی از درخت صفحه در دسترس نبود"
                        : out.scanned == 0 ? "در صفحه‌ی فعلی بسته‌ای شناسایی نشد (" + lastEmptyReason + ")"
                        : "اسکن کامل شد")
                        + (out.operatorLabel.isEmpty() ? "" : " • " + out.operatorLabel);
            }
        }

        if (out.hideComparison && !fullMode && popup != null) popup.hide();
        if (!fullMode && !out.fullModeAtStart && out.comparison != null
                && out.comparisonKeys != null && popup != null) {
            popup.show(out.comparison, out.comparisonKeys);
        }
        if (fullMode) ensureFloatingControls();
        if (sameFullSession && out.error.isEmpty() && fullStartedScanning && fullConfirmed && !fullPaused
                && (out.fullTargetObserved || fullTargetObservedOnce)) {
            // Once the user confirms the exact operator app, keep the scroll loop alive even if
            // a new Compose/WebView screen exposes no package text to the current parser.
            advanceFull(fullNew, out.fullFingerprint, out.hasLoadMoreIndicator, out.treeComplete);
        }

        scanning = false;
        if (scanAgain) {
            scanAgain = false;
            if (!fullPaused) {
                long rerunDelay = fullMode
                        ? Math.max(220L, fullNextScanAt - System.currentTimeMillis()) : 450L;
                scheduleScan(rerunDelay);
            }
        } else if (fullMode && fullConfirmed && !fullPaused && !fullStartedScanning) {
            // Poll independently of accessibility events until the target window is readable.
            // A single missed getWindows()/root snapshot must not terminate the confirmed flow.
            scheduleScan(250L);
        }
    }

    /**
     * Recognize an operator offer page even when an individual card is temporarily unreadable.
     * This lets full extraction continue across sparse/partially-rendered pages instead of
     * waiting forever for the parser to produce at least one package.
     */
    private boolean looksLikeOfferPage(AccessibilityNodeInfo root) {
        if (root == null) return false;
        boolean hasInternet = false, hasBundle = false, hasOfferData = false;
        java.util.ArrayDeque<AccessibilityNodeInfo> q = new java.util.ArrayDeque<>();
        java.util.List<AccessibilityNodeInfo> owned = new java.util.ArrayList<>();
        q.add(root);
        int visited = 0;
        try {
            while (!q.isEmpty() && visited++ < 700) {
                AccessibilityNodeInfo n = q.removeFirst();
                CharSequence t = n.getText();
                CharSequence d = n.getContentDescription();
                String v = ((t == null ? "" : t.toString()) + " " + (d == null ? "" : d.toString())).toLowerCase(java.util.Locale.ROOT);
                if (v.contains("اینترنت") || v.contains("internet")) hasInternet = true;
                if (v.contains("بسته") || v.contains("package")) hasBundle = true;
                if (v.contains("گیگ") || v.contains("مگ") || v.contains("قیمت") || v.contains("تومان")
                        || v.contains("ریال") || v.contains("خرید")) hasOfferData = true;
                if (hasInternet && hasBundle && hasOfferData) return true;
                for (int i = 0; i < n.getChildCount(); i++) {
                    AccessibilityNodeInfo child = n.getChild(i);
                    if (child != null) { owned.add(child); q.addLast(child); }
                }
            }
        } catch (RuntimeException ignored) {
            return false;
        } finally {
            for (AccessibilityNodeInfo n : owned) try { n.recycle(); } catch (RuntimeException ignored) {}
        }
        return hasBundle && hasOfferData;
    }

    /** True only when recent scroll metrics explicitly place the current viewport at the bottom. */
    private boolean hasFreshBottomMetrics(long now) {
        if (fullLastScrollEventAt <= 0L || now - fullLastScrollEventAt > 2200L) return false;
        if (fullLastMaxScrollY > 0 && fullLastScrollY >= 0) {
            int tolerance = Math.max(2, fullLastMaxScrollY / 100);
            if (fullLastScrollY >= fullLastMaxScrollY - tolerance) return true;
        }
        return fullLastScrollItemCount > 0 && fullLastScrollToIndex >= fullLastScrollItemCount - 1;
    }

    /** One step of the user-started extraction. The scroll method adapts when the app ignores it. */
    private void advanceFull(int newThisScan, String fingerprint,
                             boolean hasMoreIndicator, boolean treeComplete) {
        if (!fullMode || fullPaused) return;
        long now = System.currentTimeMillis();
        if (now - fullLastStepAt < 280L) return;
        String currentFp = fingerprint == null ? "" : fingerprint;
        boolean sameViewport = fullSteps > 0 && !currentFp.isEmpty()
                && currentFp.equals(fullLastFingerprint);
        boolean recentBottom = hasFreshBottomMetrics(now);
        if (sameViewport && !recentBottom && !settleRetried) {
            // Give a moving list one short settle retry, but never delay when recent metrics
            // already prove that the viewport has reached the physical end of its range.
            settleRetried = true;
            fullNextScanAt = now + FULL_SETTLE_RETRY_MS;
            scheduleScanReplacing(FULL_SETTLE_RETRY_MS);
            return;
        }
        if (!sameViewport) settleRetried = false;
        fullLastStepAt = now;
        fullSteps++;
        fullNoNew = newThisScan == 0 ? fullNoNew + 1 : 0;

        String fp = fingerprint == null ? "" : fingerprint;
        if (fp.isEmpty()) {
            // No stable viewport evidence means we cannot declare completion. Still bound retries
            // so an unreadable/unsupported list cannot run until the five-minute hard timeout.
            fullNoMove++;
            if (fullNoMove == 1) fullScrollStrategy = Math.max(fullScrollStrategy, 1);
            else if (fullNoMove >= 2) fullScrollStrategy = 2;
        } else if (fp.equals(fullLastFingerprint)) {
            fullNoMove++;
            // ACTION_SCROLL_FORWARD is best when supported. If the visible page did not change,
            // try a real vertical gesture inside the list, then a broader app-window swipe.
            if (fullNoMove == 1) fullScrollStrategy = Math.max(fullScrollStrategy, 1);
            else if (fullNoMove >= 2) fullScrollStrategy = 2;
        } else {
            fullLastFingerprint = fp;
            fullNoMove = 0;
            // A changed viewport proves that the previous fallback worked; start the next
            // cycle with the accessibility action again, then escalate only if it stalls.
            fullScrollStrategy = 0;
        }

        boolean hasScrollMetrics = fullLastMaxScrollY > 0
                || (fullLastScrollItemCount > 0 && fullLastScrollToIndex >= 0);
        if (!treeComplete) {
            // An incomplete tree cannot prove that no pagination control is present.
            fullEndDetector.reset();
            fullEndState = hasMoreIndicator ? ListEndDetector.State.MORE_AVAILABLE
                    : ListEndDetector.State.UNKNOWN;
        } else if (hasScrollMetrics) {
            fullEndState = fullEndDetector.observe(fullLastScrollY, fullLastMaxScrollY,
                    fullLastScrollItemCount, fullLastScrollToIndex, hasMoreIndicator,
                    false, fullSteps);
        } else if (fullScrollStrategy >= 2) {
            fullEndState = fullEndDetector.observeStableViewport(fp, hasMoreIndicator,
                    true, fullSteps);
        } else if (hasMoreIndicator) {
            fullEndState = fullEndDetector.observe(-1, 0, true, false, fullSteps);
        }

        // All operators now use the same MCI-validated scan path: native scroll action,
        // viewport verification, list-region gesture, app-viewport gesture, then the same
        // conservative end-of-list detector. Only package parsing/normalization remains
        // operator-specific; no operator ID can change the scan or stop conditions.

        if (fullEndState == ListEndDetector.State.END_CONFIRMED) {
            stopFull("اسکن با موفقیت به پایان رسید؛ انتهای فهرست بسته‌ها تأیید شد", true);
            return;
        }

        lastScanStatus = "استخراج کامل: مرحله " + fullSteps + "، " + fullSeen.size()
                + " بسته‌ی یکتا؛ " + newThisScan + " بسته‌ی تازه";
        if (fullSteps >= FULL_MAX_STEPS || now - fullStartedAt > FULL_MAX_MS) {
            stopFull("توقف ایمن: به سقف زمان یا تعداد حرکت رسید", true);
            return;
        }
        if (fullNoMove >= 4) {
            if (fullEndState == ListEndDetector.State.END_CONFIRMED) {
                stopFull("اسکن با موفقیت به پایان رسید؛ انتهای فهرست بسته‌ها تأیید شد", true);
            } else {
                stopFull("اسکرول پس از چند تلاش متوقف شد؛ پایان فهرست با شواهد کافی تأیید نشد", true);
            }
            return;
        }

        final String targetAtStart = fullTargetPackage;
        ExecutorService worker = scanExecutor;
        if (worker == null || worker.isShutdown()) {
            stopFull("پردازشگر اسکرول آماده نیست؛ اسکن به‌صورت ایمن متوقف شد", true);
            return;
        }
        try {
            worker.execute(() -> {
                // Finding the scrollable node can require many remote accessibility queries.
                // Keep that work off the main thread too, then update the overlay on main.
                boolean requested = fullMode && !fullPaused
                        && targetAtStart.equals(fullTargetPackage) && performAdaptiveScroll();
                String method = lastScrollMethod;
                h.post(() -> finishFullScroll(targetAtStart, requested, method));
            });
        } catch (RejectedExecutionException rejected) {
            stopFull("پردازشگر اسکرول در دسترس نبود؛ اسکن به‌صورت ایمن متوقف شد", true);
        }
    }

    /** Completes the scroll cycle on the service/UI thread after worker-side node traversal. */
    private void finishFullScroll(String target, boolean requested, String method) {
        if (!fullMode || fullPaused || !target.equals(fullTargetPackage)) return;
        if (fullEndState == ListEndDetector.State.END_CONFIRMED) {
            stopFull("اسکن با موفقیت به پایان رسید؛ انتهای فهرست بسته‌ها تأیید شد", true);
            return;
        }
        if (!requested) {
            stopFull("اسکرول خودکار در این صفحه پشتیبانی نشد؛ وضعیت فهرست نامشخص است", true);
            return;
        }
        lastScanStatus += " • " + method;
        ensureFloatingControls();
        fullNextScanAt = System.currentTimeMillis() + FULL_SCROLL_SETTLE_MS;
        scheduleScanReplacing(FULL_SCROLL_SETTLE_MS);
    }

    /**
     * Scroll strategy: accessible list action first, then a vertical swipe inside the detected
     * list, finally a swipe in the app content viewport. This is only called after the user has
     * explicitly started a scan and the selected operator's offers page has been recognized.
     */
    private AccessibilityNodeInfo getFullTargetRoot() {
        AccessibilityNodeInfo best = null;
        long bestScore = -1L;
        AccessibilityNodeInfo active = null;
        try {
            active = getRootInActiveWindow();
            if (active != null && isFullTarget(String.valueOf(active.getPackageName()))) {
                Rect bounds = new Rect();
                active.getBoundsInScreen(bounds);
                best = active;
                bestScore = (long) Math.max(0, bounds.width()) * Math.max(0, bounds.height());
                active = null; // ownership transferred to best
            }
        } catch (RuntimeException error) {
            android.util.Log.w("BastehyabScan", "Active accessibility root unavailable: "
                    + error.getClass().getSimpleName());
        } finally {
            if (active != null) try { active.recycle(); } catch (RuntimeException ignored) {}
        }

        // Search all app windows even when the active root belongs to the target app: some
        // operator dialogs expose only a small focused root while the full list remains behind.
        List<AccessibilityWindowInfo> windows = null;
        try { windows = getWindows(); }
        catch (RuntimeException error) {
            android.util.Log.w("BastehyabScan", "Accessibility windows unavailable: "
                    + error.getClass().getSimpleName());
        }
        if (windows != null) {
            for (AccessibilityWindowInfo window : windows) {
                if (window == null) continue;
                AccessibilityNodeInfo candidate = null;
                try {
                    if (window.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        candidate = window.getRoot();
                        if (candidate != null && isFullTarget(String.valueOf(candidate.getPackageName()))) {
                            Rect bounds = new Rect();
                            candidate.getBoundsInScreen(bounds);
                            long score = (long) Math.max(0, bounds.width()) * Math.max(0, bounds.height());
                            if (score > bestScore) {
                                if (best != null) try { best.recycle(); } catch (RuntimeException ignored) {}
                                best = candidate;
                                candidate = null;
                                bestScore = score;
                            }
                        }
                    }
                } catch (RuntimeException ignored) {
                    // Continue looking for another window belonging to the exact operator app.
                } finally {
                    if (candidate != null) try { candidate.recycle(); } catch (RuntimeException ignored) {}
                    try { window.recycle(); } catch (RuntimeException ignored) {}
                }
            }
        }
        return best; // Caller owns and recycles this root.
    }

    /** Scroll strategy: accessible list action, swipe in the list, then swipe in app viewport. */
    private boolean performAdaptiveScroll() {
        if (!fullMode || fullPaused) return false;
        AccessibilityNodeInfo root = null;
        AccessibilityNodeInfo list = null;
        Rect appBounds = new Rect();
        boolean appBoundsReady = false;
        boolean actionAccepted = false;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        try {
            root = getFullTargetRoot();
            if (root != null) {
                try {
                    root.getBoundsInScreen(appBounds);
                    appBoundsReady = appBounds.width() >= 180 && appBounds.height() >= 260;
                } catch (RuntimeException ignored) {}
            }
            if (!appBoundsReady) {
                int topInset = Math.round(48f * metrics.density);
                int bottomInset = Math.round(72f * metrics.density);
                appBounds.left = 0;
                appBounds.top = topInset;
                appBounds.right = metrics.widthPixels;
                appBounds.bottom = Math.max(Math.round(300f * metrics.density),
                        metrics.heightPixels - bottomInset);
                appBoundsReady = appBounds.width() >= 180 && appBounds.height() >= 260;
            }
            if (!appBoundsReady) return false;

            Rect listBounds = new Rect(appBounds);
            if (root != null) {
                try {
                    list = findScrollable(root);
                } catch (RuntimeException error) {
                    android.util.Log.w("BastehyabScan", "Scrollable candidate lookup failed: "
                            + error.getClass().getSimpleName());
                }
                if (list != null) {
                    try {
                        list.getBoundsInScreen(listBounds);
                        if (listBounds.width() < 180 || listBounds.height() < 260) {
                            listBounds.left = appBounds.left; listBounds.top = appBounds.top;
                            listBounds.right = appBounds.right; listBounds.bottom = appBounds.bottom;
                        }
                        // AccessibilityNodeInfo on compileSdk 34 does not expose the View
                        // scroll offsets/range. Do not call getScrollY()/getMaxScrollY(): those
                        // methods are not part of this API surface. An accepted action proves
                        // only that Android accepted the request, NOT that the list moved or
                        // reached its end. The next viewport comparison verifies actual movement;
                        // if it remains unchanged, the shared strategy escalates to a real swipe.
                        // Keep end state UNKNOWN until independent evidence exists.
                        if (shouldUseAccessibilityScroll(fullScrollStrategy)) {
                            try { actionAccepted = list.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD); }
                            catch (RuntimeException ignored) { actionAccepted = false; }
                        }
                    } catch (RuntimeException error) {
                        android.util.Log.w("BastehyabScan", "Scrollable node became stale: "
                                + error.getClass().getSimpleName());
                        listBounds.left = appBounds.left; listBounds.top = appBounds.top;
                        listBounds.right = appBounds.right; listBounds.bottom = appBounds.bottom;
                    }
                }
            }
            if (actionAccepted) {
                lastScrollMethod = "فرمان پیمایش فهرست پذیرفته شد؛ حرکت در مرحله‌ی بعد بررسی می‌شود";
                return true;
            }

            // With a confirmed target app, a root may temporarily be unavailable (transition,
            // Compose subtree refresh, or a modal). The display viewport is the guarded fallback.
            int variant = Math.abs(fullNoMove) % 3;
            if (root == null) variant = (variant + 1) % 3;
            // If the list action was accepted but the next scan still saw the same viewport,
            // switch to a real touch gesture. After a second unchanged viewport, try the whole
            // app content area first: some operator apps expose a nested node that is not the
            // actual touch owner of their vertically scrolling feed.
            boolean preferAppViewport = shouldPreferAppViewportSwipe(fullScrollStrategy);
            if (preferAppViewport && dispatchVerticalSwipe(appBounds, variant)) {
                lastScrollMethod = "حرکت لمسی در ناحیه‌ی اصلی برنامه (جایگزین فهرست بدون حرکت)";
                return true;
            }
            if (dispatchVerticalSwipe(listBounds, variant)) {
                lastScrollMethod = root == null
                        ? "حرکت لمسی در محدوده‌ی صفحه (درخت موقتاً ناموجود)"
                        : "حرکت لمسی داخل فهرست (مسیر " + (variant + 1) + ")";
                return true;
            }
            if (dispatchVerticalSwipe(appBounds, (variant + 1) % 3)) {
                lastScrollMethod = "حرکت لمسی در ناحیه‌ی اصلی برنامه";
                return true;
            }
            lastScrollMethod = "هیچ فرمان پیمایشی پذیرفته نشد";
            return false;
        } catch (RuntimeException error) {
            lastScrollMethod = "خطای دسترس‌پذیری هنگام پیمایش: " + error.getClass().getSimpleName();
            android.util.Log.w("BastehyabScan", "Adaptive scroll failed: "
                    + error.getClass().getSimpleName());
            // One final screen gesture is safer than silently abandoning a confirmed page.
            try {
                if (appBoundsReady && dispatchVerticalSwipe(appBounds, Math.floorMod(fullNoMove + 1, 3))) {
                    lastScrollMethod = "حرکت لمسی جایگزین پس از خطای درخت";
                    return true;
                }
            } catch (RuntimeException ignored) {}
            return false;
        } finally {
            if (list != null && list != root) try { list.recycle(); } catch (RuntimeException ignored) {}
            if (root != null) try { root.recycle(); } catch (RuntimeException ignored) {}
        }
    }

    /**
     * Injects the swipe from the service thread. The floating control overlay is made temporarily
     * non-touchable during the gesture so a panel dragged over the swipe lane cannot eat it.
     * Completion/cancellation is reported by Android and restored on the service main thread.
     */
    private boolean dispatchVerticalSwipe(Rect bounds, int variant) {
        if (!fullMode || fullPaused || bounds == null) return false;
        int width = bounds.width(), height = bounds.height();
        if (width < 180 || height < 260) return false;
        // Start at side lanes first; the floating panel is centered by default.
        float[] lanes = {0.72f, 0.28f, 0.50f};
        int x = bounds.left + Math.round(width * lanes[Math.floorMod(variant, lanes.length)]);
        int y1 = bounds.top + Math.round(height * 0.84f);
        int y2 = bounds.top + Math.round(height * 0.27f);
        if (y1 <= y2) return false;
        Path path = new Path();
        path.moveTo(x, y1);
        path.lineTo(x, y2);
        final GestureDescription gesture;
        try {
            gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0L, 300L))
                    .build();
        } catch (RuntimeException error) {
            lastGestureResult = "build-failed:" + error.getClass().getSimpleName();
            return false;
        }

        final boolean[] accepted = {false};
        final CountDownLatch dispatched = new CountDownLatch(1);
        boolean posted = h.post(() -> {
            if (!fullMode || fullPaused) {
                lastGestureResult = "cancelled-before-dispatch";
                dispatched.countDown();
                return;
            }
            Popup controls = popup;
            if (controls != null) controls.setConfirmTouchBlocked(true);
            AtomicBoolean restored = new AtomicBoolean(false);
            Runnable restoreTouch = () -> {
                if (!restored.compareAndSet(false, true)) return;
                Popup current = popup;
                if (current != null) current.setConfirmTouchBlocked(false);
            };
            try {
                accepted[0] = dispatchGesture(gesture, new GestureResultCallback() {
                    private void restore(String result) {
                        lastGestureResult = result;
                        restoreTouch.run();
                    }
                    @Override public void onCompleted(GestureDescription completedGesture) {
                        restore("completed");
                    }
                    @Override public void onCancelled(GestureDescription cancelledGesture) {
                        restore("cancelled");
                    }
                }, h);
                if (!accepted[0]) {
                    lastGestureResult = "rejected";
                    restoreTouch.run();
                } else {
                    if (!restored.get()) lastGestureResult = "dispatched";
                    // A missing framework callback must never leave the user's floating controls
                    // non-touchable forever; normal completion/cancellation wins this race.
                    h.postDelayed(() -> {
                        if (!restored.get()) {
                            lastGestureResult = "callback-timeout";
                            restoreTouch.run();
                        }
                    }, 1800L);
                }
            } catch (RuntimeException error) {
                lastGestureResult = "dispatch-failed:" + error.getClass().getSimpleName();
                restoreTouch.run();
            } finally {
                dispatched.countDown();
            }
        });
        if (!posted) {
            lastGestureResult = "main-thread-unavailable";
            return false;
        }
        try {
            if (!dispatched.await(2L, TimeUnit.SECONDS)) {
                lastGestureResult = "dispatch-timeout";
                return false;
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            lastGestureResult = "dispatch-interrupted";
            return false;
        }
        return accepted[0];
    }

    /**
     * Fingerprint built from the already-parsed offers (no extra accessibility queries): the
     * page "moved" exactly when the set of visible offers changed.
     */
    private static String itemsFingerprint(List<Pkg> items) {
        java.util.TreeSet<String> signatures = new java.util.TreeSet<>();
        if (items != null) for (Pkg item : items) {
            if (item != null) signatures.add(item.sig());
        }
        int hash = 1;
        for (String signature : signatures) hash = 31 * hash + signature.hashCode();
        return signatures.isEmpty() ? "" : "offers:" + signatures.size() + ":" + Integer.toHexString(hash);
    }

    /**
     * A short-lived hash of the complete visible app root, matching the shared MCI scan path.
     * Raw page text is never written to disk.
     */
    private String pageFingerprint(AccessibilityNodeInfo root) {
        return root == null ? "" : fingerprintTree(root);
    }

    /**
     * Fingerprint visible, stable offer-related labels together with their coarse screen positions.
     * Position is essential: the same offer text can remain exposed while a long list scrolls.
     * Clock/countdown/loading text is excluded so it cannot make a stationary viewport flicker.
     */
    private String fingerprintTree(AccessibilityNodeInfo root) {
        if (root == null) return "";
        java.util.ArrayDeque<AccessibilityNodeInfo> queue = new java.util.ArrayDeque<>();
        java.util.List<AccessibilityNodeInfo> owned = new ArrayList<>();
        java.util.TreeSet<String> stableLabels = new java.util.TreeSet<>();
        queue.add(root);
        int visited = 0;
        boolean traversalFailed = false;
        try {
            while (!queue.isEmpty() && visited++ < 1800) {
                AccessibilityNodeInfo n = queue.removeFirst();
                try {
                    if (n.isVisibleToUser() && !n.isEditable() && !n.isPassword()) {
                        CharSequence text = n.getText();
                        if (text == null || text.length() == 0) text = n.getContentDescription();
                        if (text != null && text.length() > 0) {
                            String normalized = Fmt.norm(text.toString()).replaceAll("\\s+", " ").trim();
                            if (isStablePageLabel(normalized)) {
                                Rect bounds = new Rect();
                                n.getBoundsInScreen(bounds);
                                int topBand = Math.round(bounds.top / 12f);
                                int bottomBand = Math.round(bounds.bottom / 12f);
                                stableLabels.add(normalized + "@" + topBand + ":" + bottomBand);
                            }
                        }
                    }
                } catch (RuntimeException ignored) {
                    // A stale node invalidates this snapshot; it must not help confirm end-of-list.
                    traversalFailed = true;
                }
                int children;
                try {
                    children = Math.max(0, Math.min(500, n.getChildCount()));
                } catch (RuntimeException ignored) {
                    traversalFailed = true;
                    children = 0;
                }
                for (int i = 0; i < children; i++) {
                    try {
                        AccessibilityNodeInfo child = n.getChild(i);
                        if (child != null) { owned.add(child); queue.addLast(child); }
                    } catch (RuntimeException ignored) { traversalFailed = true; }
                }
            }
        } catch (RuntimeException ignored) {
            traversalFailed = true;
        } finally {
            for (AccessibilityNodeInfo n : owned) try { n.recycle(); } catch (RuntimeException ignored) {}
        }
        if (visited >= 1800 || traversalFailed || stableLabels.isEmpty()) return "";
        int hash = 1;
        for (String label : stableLabels) hash = 31 * hash + label.hashCode();
        return "viewport:" + stableLabels.size() + ":" + Integer.toHexString(hash);
    }

    private static boolean isStablePageLabel(String value) {
        if (value == null || value.isEmpty() || value.length() > 180) return false;
        String s = value.toLowerCase(java.util.Locale.ROOT);
        if (s.matches(".*(?:\\b\\d{1,2}:\\d{2}\\b|\\b\\d{1,2}/\\d{1,2}/\\d{2,4}\\b).*")) return false;
        if (s.contains("ثانیه پیش") || s.contains("دقیقه پیش") || s.contains("seconds ago")
                || s.contains("minutes ago") || s.contains("در حال بارگذاری") || s.contains("loading")
                || s.contains("به‌روزرسانی شد") || s.contains("آخرین بروزرسانی")) return false;
        // Keep package/content labels; ignore generic sticky chrome such as Home, Back, Settings.
        return s.contains("بسته") || s.contains("اینترنت") || s.contains("گیگ")
                || s.contains("مگ") || s.contains("تومان") || s.contains("ریال")
                || s.contains("نامحدود") || s.contains("روز") || s.contains("هفته")
                || s.contains("ماه") || s.contains("شبانه") || s.contains("حجم")
                || s.contains("قیمت") || s.contains("خرید") || s.contains("package")
                || s.contains("internet") || s.contains("gb") || s.contains("mb");
    }

    private static int safeChildCount(AccessibilityNodeInfo node) {
        try { return Math.max(0, Math.min(500, node.getChildCount())); }
        catch (RuntimeException ignored) { return 0; }
    }

    /** Select the largest visible vertical scroll container using the shared MCI strategy. */
    static AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo root) {
        if (root == null) return null;
        java.util.ArrayDeque<AccessibilityNodeInfo> queue = new java.util.ArrayDeque<>();
        java.util.List<AccessibilityNodeInfo> owned = new ArrayList<>();
        queue.add(root);
        AccessibilityNodeInfo best = null;
        long bestScore = -1L;
        int visited = 0;
        try {
            while (!queue.isEmpty() && visited++ < 1200) {
                AccessibilityNodeInfo n = queue.poll();
                try {
                    if (n.isScrollable() && n.isVisibleToUser()) {
                        Rect bounds = new Rect();
                        n.getBoundsInScreen(bounds);
                        long width = Math.max(0L, (long) bounds.width());
                        long height = Math.max(0L, (long) bounds.height());
                        // Reject horizontal carousels and tiny nested controls.
                        boolean verticalCandidate = height >= width * 0.55 && height >= 240L;
                        if (verticalCandidate) {
                            long score = width * height + 3L * height * height;
                            if (score > bestScore) {
                                bestScore = score;
                                best = n;
                            }
                        }
                    }
                } catch (RuntimeException staleNode) {
                    // A disappearing Compose/WebView child must not abort sibling traversal.
                }
                int childCount = 0;
                try { childCount = Math.max(0, Math.min(500, n.getChildCount())); }
                catch (RuntimeException ignored) {}
                for (int i = 0; i < childCount; i++) {
                    try {
                        AccessibilityNodeInfo child = n.getChild(i);
                        if (child != null) { owned.add(child); queue.add(child); }
                    } catch (RuntimeException ignored) {
                        // Continue with siblings when one remote child cannot be fetched.
                    }
                }
            }
        } catch (RuntimeException treeError) {
            android.util.Log.w("BastehyabScan", "Scrollable tree traversal partial: "
                    + treeError.getClass().getSimpleName());
        } finally {
            for (AccessibilityNodeInfo child : owned) {
                if (child == best) continue;
                try { child.recycle(); } catch (RuntimeException ignored) {}
            }
        }
        return best;
    }

    void showTest() {
        // The comparison overlay is touchable and spans most of the screen. Never
        // allow the settings preview to appear while automatic extraction is active.
        if (fullMode) {
            hideComparisonPopup();
            Toast.makeText(this, "هنگام استخراج خودکار، پنجره‌ی مقایسه غیرفعال است.", Toast.LENGTH_SHORT).show();
            return;
        }
        Compare.Result cr = Compare.run(this, new HashSet<String>());
        if (cr.ranked.isEmpty()) {
            Toast.makeText(this, "هنوز داده‌ای برای نمایش نیست", Toast.LENGTH_SHORT).show();
            return;
        }
        popup.show(cr, new HashSet<String>());
    }

    void hideComparisonPopup() {
        if (popup != null) popup.hide();
    }

    private void diag(String p, String dump) {
        List<String> history = dumps.get(p);
        if (history == null) {
            history = new ArrayList<>();
            dumps.put(p, history);
        }
        // Avoid logging the same viewport repeatedly; retain up to the latest 30 distinct
        // snapshots so the exported report shows what happened before and after each scroll.
        if (history.isEmpty() || !history.get(history.size() - 1).equals(dump)) {
            history.add("time=" + new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                    .format(new java.util.Date()) + " step=" + fullSteps + " unique=" + fullSeen.size()
                    + " scroll=" + lastScrollMethod + "\n" + dump);
            while (history.size() > 12) history.remove(0);
        }
        // Debounce file I/O but always write the newest snapshot.
        h.removeCallbacks(writeDiag);
        h.postDelayed(writeDiag, 2500L);
    }

    /** Force a final report write before sharing the diagnostic file. */
    static void flushDiagnostics() {
        ScanService service = inst;
        if (service == null) return;
        service.h.removeCallbacks(service.writeDiag);
        service.writeDiagnosticFile();
    }

    private void writeDiagnosticFile() {
        StringBuilder sb = new StringBuilder();
        sb.append("Bastehyab diagnostic • version 1.6.8\n");
        sb.append("capturedAt=").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                .format(new java.util.Date())).append('\n');
        sb.append("status=").append(lastScanStatus).append('\n');
        sb.append("phase=").append(phase()).append(" lastScanError=").append(lastScanError)
                .append(" lastNodes=").append(lastNodeCount).append('\n');
        sb.append("fullMode=").append(fullMode).append(" paused=").append(fullPaused)
                .append(" promptError=").append(lastPopupError).append(" nextScanAt=").append(fullNextScanAt)
                .append(" target=").append(fullTargetPackage)
                .append(" steps=").append(fullSteps).append(" unique=").append(fullSeen.size())
                .append(" observedScrollPercent=").append(fullObservedScrollPercent)
                .append(" noMove=").append(fullNoMove).append(" scrollStrategy=").append(fullScrollStrategy)
                .append(" scrollMethod=").append(lastScrollMethod)
                .append(" gestureResult=").append(lastGestureResult).append('\n');
        sb.append("lastScanDurationMs=").append(lastScanDurationMs)
                .append(" scanned=").append(lastScannedPackages).append(" valid=").append(lastValidPackages)
                .append(" suspicious=").append(lastSuspiciousPackages).append(" duplicates=")
                .append(lastDuplicatePackages).append('\n');
        List<String> keys = new ArrayList<>(dumps.keySet());
        java.util.Collections.sort(keys);
        for (String key : keys) {
            sb.append("=== package=").append(key).append(" snapshots=")
                    .append(dumps.get(key).size()).append('\n');
            int page = 0;
            for (String snapshot : dumps.get(key)) {
                sb.append("--- snapshot ").append(++page).append(" ---\n")
                        .append(snapshot).append('\n');
            }
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(
                new File(getFilesDir(), "diag.txt")), StandardCharsets.UTF_8)) {
            w.write(sb.toString());
        } catch (Exception ignored) {}
    }

}
