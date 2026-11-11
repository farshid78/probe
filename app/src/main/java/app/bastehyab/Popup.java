package app.bastehyab;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Floating comparison card drawn as an accessibility overlay (no extra permission needed). */
final class Popup {
    private final Context c;
    private final WindowManager wm;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable autoHide = this::hide;
    private LinearLayout root;
    private WindowManager.LayoutParams lp;
    private boolean attached, compact;
    private Runnable onClose;
    private Compare.Result last;
    private Set<String> screen;
    private String lastSig = "";
    private static volatile long overlayAt;

    /** True right after one of our own overlays was added/removed (it may raise a window event). */
    static boolean recentOverlayChange() {
        return android.os.SystemClock.elapsedRealtime() - overlayAt < 1500L;
    }

    private LinearLayout confirmRoot;
    private WindowManager.LayoutParams confirmParams;
    private boolean confirmAttached;
    private boolean confirmTouchBlocked;
    /** Last successful WindowManager health check; throttles checks during noisy accessibility events. */
    private long confirmLastVerifiedAt;
    private int confirmMode = -1; // 0: waiting, 1: scanning, 2: paused
    private String confirmOperator = "";
    private String confirmError = "";
    private ProgressBar confirmProgress;
    private TextView confirmProgressLabel;
    private TextView confirmStatus;
    /** Last position chosen by the user; reused when the panel is rebuilt for a new mode. */
    private int savedX = -1, savedY = -1;

    /**
     * View.isAttachedToWindow() stays false until the first traversal AFTER WindowManager.addView(),
     * so it can never be used right after adding. The add/remove flags below are authoritative and
     * these listeners only clear them if the system detaches an overlay by itself.
     */
    private final View.OnAttachStateChangeListener rootWatch = new View.OnAttachStateChangeListener() {
        @Override public void onViewAttachedToWindow(View v) {}
        @Override public void onViewDetachedFromWindow(View v) { if (v == root) attached = false; }
    };
    private final View.OnAttachStateChangeListener confirmWatch = new View.OnAttachStateChangeListener() {
        @Override public void onViewAttachedToWindow(View v) {}
        @Override public void onViewDetachedFromWindow(View v) { if (v == confirmRoot) confirmAttached = false; }
    };

    /**
     * The service context must be retained (not replaced with the application context),
     * because TYPE_ACCESSIBILITY_OVERLAY is owned by the connected accessibility service.
     */
    Popup(Context context) {
        if (context == null) throw new IllegalArgumentException("context == null");
        c = context;
        UI.applyTheme(context);
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) throw new IllegalStateException("WindowManager is unavailable");
    }

    void onClose(Runnable callback) {
        onClose = callback;
    }

    /**
     * The comparison card and scan controls are mutually exclusive overlays.
     * Both use TYPE_ACCESSIBILITY_OVERLAY and both receive touches inside their bounds;
     * allowing them to coexist can cover the scan buttons or consume operator-page gestures.
     */
    private boolean isConfirmVisible() {
        if (!confirmAttached || confirmRoot == null || confirmParams == null) return false;
        long now = android.os.SystemClock.elapsedRealtime();
        // The internal flag can outlive a window removed by Android/OEM window transitions.
        // Verify with WindowManager occasionally, not on every accessibility event.
        if (now - confirmLastVerifiedAt >= 1200L) {
            try {
                wm.updateViewLayout(confirmRoot, confirmParams);
                confirmLastVerifiedAt = now;
            } catch (RuntimeException staleWindow) {
                confirmLastVerifiedAt = now;
                confirmError = staleWindow.getClass().getSimpleName();
                android.util.Log.w("BastehyabPopup", "Scan controls are no longer attached: "
                        + confirmError);
                return false;
            }
        }
        return true;
    }

    /** True while the scan-control panel is on screen (cheap; safe to call on every event). */
    boolean isConfirmShown() {
        return isConfirmVisible();
    }

    /** Show/update the comparison card only when no full extraction/control panel is active. */
    void show(Compare.Result result, Set<String> onScreen) {
        if (result == null) return;
        if (ScanService.fullMode || isConfirmVisible()) {
            // Defensive central guard: callers such as the settings preview must not
            // place a touchable comparison window above the operator during extraction.
            hide();
            return;
        }
        last = result;
        screen = onScreen == null ? new HashSet<String>() : new HashSet<>(onScreen);

        // `attached` is maintained by our own add/remove calls (see rootWatch); never query
        // isAttachedToWindow() here because it is false until the first frame is drawn.
        String signature;
        try {
            signature = sig(result);
            boolean changed = !attached || !signature.equals(lastSig);
            if (root == null) {
                root = new LinearLayout(c);
                root.addOnAttachStateChangeListener(rootWatch);
                attached = false;
            }

            if (changed) {
                render();
                lastSig = signature;
            }

            if (!attached) {
                makeParams();
                overlayAt = android.os.SystemClock.elapsedRealtime();
                UI.applyTypefaceTree(root);
                wm.addView(root, lp);
                attached = true;
            } else if (changed) {
                try {
                    wm.updateViewLayout(root, lp);
                } catch (RuntimeException updateError) {
                    // A stale attached flag must never prevent a clean re-attach.
                    attached = false;
                    makeParams();
                    overlayAt = android.os.SystemClock.elapsedRealtime();
                    UI.applyTypefaceTree(root);
                    wm.addView(root, lp);
                    attached = true;
                }
            }
        } catch (Exception error) {
            android.util.Log.w("BastehyabPopup", "Could not show comparison popup: "
                    + error.getClass().getSimpleName());
            // Force the next refresh to rebuild/retry even when the window survived the error.
            lastSig = "";
            if (!attached) lp = null;
        }

        h.removeCallbacks(autoHide);
        int seconds;
        try {
            seconds = Cfg.hideSec(c);
        } catch (RuntimeException configError) {
            seconds = 0;
            android.util.Log.w("BastehyabPopup", "Could not read popup timeout: "
                    + configError.getClass().getSimpleName());
        }
        if (attached && seconds > 0) h.postDelayed(autoHide, seconds * 1000L);
    }

    /** Hide only the comparison popup; floating scan controls remain visible. */
    void hide() {
        h.removeCallbacks(autoHide);
        final LinearLayout view = root;
        if (view != null && attached) {
            overlayAt = android.os.SystemClock.elapsedRealtime();
            try {
                wm.removeView(view);
            } catch (RuntimeException error) {
                // It may already have been detached by Android during an app transition.
                android.util.Log.d("BastehyabPopup", "Comparison popup already detached: "
                        + error.getClass().getSimpleName());
            }
        }
        attached = false;
        root = null;      // a fresh view is built next time; no stale window can ever be orphaned
        lp = null;
        lastSig = "";
    }

    /**
     * Shows a movable accessibility overlay. The same panel becomes scan controls after
     * confirmation and remains available for pause/resume until the user exits.
     * Exactly one such window can exist: it is tracked by explicit add/remove state, because
     * isAttachedToWindow() is still false right after addView() and trusting it used to leave
     * orphaned, un-removable and un-movable windows behind (one per refresh).
     * @param mode 0 = waiting for confirmation, 1 = scanning, 2 = paused, 3 = final report
     */
    boolean showConfirm(String operatorName, int mode, int percent, int steps,
                        int packageCount, String status, final Runnable onPrimary,
                        final Runnable onExit, final Runnable onLater) {
        if (mode < 0 || mode > 3) return false;
        // Enforce overlay exclusivity in the opposite direction as well: the scan panel
        // must never be added on top of a still-visible comparison card.
        hide();
        String safeName = operatorName == null || operatorName.trim().isEmpty()
                ? "اپراتور" : operatorName.trim();
        if (confirmAttached && confirmRoot != null && confirmParams != null
                && confirmMode == mode && safeName.equals(confirmOperator)) {
            try {
                // Revalidate the window instead of trusting a stale local attached flag.
                wm.updateViewLayout(confirmRoot, confirmParams);
                confirmLastVerifiedAt = android.os.SystemClock.elapsedRealtime();
                confirmError = "";
                updateConfirmProgress(percent, steps, packageCount, status);
                return true;
            } catch (RuntimeException staleWindow) {
                confirmError = staleWindow.getClass().getSimpleName();
                android.util.Log.w("BastehyabPopup", "Rebuilding stale scan controls: " + confirmError);
            }
        }

        // Remove the previous panel (if any) before building the next one.
        hideConfirm();
        confirmError = "";
        confirmError = "";
        final LinearLayout panel = new LinearLayout(c);
        boolean added = false;
        try {
            int pad = UI.dp(c, 9);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            panel.setPadding(pad, pad, pad, pad);
            panel.setBackground(UI.rr(c, UI.CARD, 15, UI.STROKE));
            panel.setElevation(UI.dp(c, 9));

            DisplayMetrics dm = c.getResources().getDisplayMetrics();
            int width = Math.min(dm.widthPixels - UI.dp(c, 24), UI.dp(c, 340));
            width = Math.max(UI.dp(c, 210), width);
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    width, WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = savedX >= 0 ? Math.max(0, Math.min(savedX, Math.max(0, dm.widthPixels - width)))
                    : Math.max(0, (dm.widthPixels - width) / 2);
            params.y = savedY >= 0 ? Math.max(0, Math.min(savedY, Math.max(0, dm.heightPixels - UI.dp(c, 60))))
                    : UI.dp(c, 76);
            params.setTitle("Bastehyab scan controls");

            LinearLayout header = UI.hbox(c);
            header.setGravity(Gravity.CENTER_VERTICAL);
            String title = mode == 0 ? "آماده‌ی شروع اسکن " + safeName
                    : mode == 1 ? "استخراج خودکار " + safeName
                    : mode == 2 ? "اسکن موقتاً متوقف است"
                    : "گزارش نهایی اسکن " + safeName;
            TextView titleView = UI.tv(c, title, 12, UI.PRIMARY, true);
            header.addView(titleView, UI.lpw(1f));
            TextView drag = UI.tv(c, "⠿ جابه‌جایی", 10, UI.MUTED, true);
            drag.setGravity(Gravity.CENTER);
            drag.setPadding(UI.dp(c, 12), UI.dp(c, 10), UI.dp(c, 12), UI.dp(c, 10));
            drag.setMinWidth(UI.dp(c, 92));
            drag.setMinHeight(UI.dp(c, 42));
            drag.setBackground(UI.rr(c, UI.CARD2, 9));
            header.addView(drag);
            // The listener is bound to THIS panel and THIS params object (not to mutable fields),
            // so it always moves the window the finger is actually on.
            View.OnTouchListener mover = new View.OnTouchListener() {
                float downX, downY;
                int startX, startY;
                @Override public boolean onTouch(View v, MotionEvent e) {
                    if (panel != confirmRoot || !confirmAttached) return false;
                    switch (e.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            downX = e.getRawX();
                            downY = e.getRawY();
                            startX = params.x;
                            startY = params.y;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            DisplayMetrics metrics = c.getResources().getDisplayMetrics();
                            int w = panel.getWidth() > 0 ? panel.getWidth() : params.width;
                            int hh = Math.max(panel.getHeight(), UI.dp(c, 60));
                            int maxX = Math.max(0, metrics.widthPixels - w);
                            int maxY = Math.max(0, metrics.heightPixels - hh);
                            params.x = Math.max(0, Math.min(maxX,
                                    startX + Math.round(e.getRawX() - downX)));
                            params.y = Math.max(0, Math.min(maxY,
                                    startY + Math.round(e.getRawY() - downY)));
                            savedX = params.x;
                            savedY = params.y;
                            try {
                                wm.updateViewLayout(panel, params);
                            } catch (RuntimeException ex) {
                                confirmError = ex.getClass().getSimpleName();
                                android.util.Log.w("BastehyabPopup", "Could not move scan controls: "
                                        + confirmError);
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            return true;
                        default:
                            return true;
                    }
                }
            };
            // Both the handle and the title can drag the panel; buttons below keep normal clicks.
            drag.setClickable(true);
            drag.setLongClickable(false);
            drag.setOnTouchListener(mover);
            titleView.setOnTouchListener(mover);
            panel.addView(header);

            String message = mode == 0
                    ? "پس از بازشدن صفحه‌ی بسته‌ها، شروع را بزنید."
                    : mode == 1
                    ? "اسکن فعال است؛ برای مکث، توقف را بزنید."
                    : mode == 2
                    ? "اطلاعات حفظ شده؛ ادامه از همین صفحه انجام می‌شود."
                    : "این گزارش نتیجه‌ی آخرین تلاش اسکن را نشان می‌دهد.";
            panel.addView(UI.tv(c, message, 10, UI.TEXT, false),
                    UI.lp(c, -1, -2, 0, 4, 0, 5));

            confirmProgressLabel = UI.tv(c, mode == 3 ? "نتیجه‌ی پیمایش" : "پیشرفت تقریبی پیمایش • ۰٪",
                    10, UI.PRIMARY, true);
            panel.addView(confirmProgressLabel, UI.lp(c, -1, -2, 0, 1, 0, 2));
            confirmProgress = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
            confirmProgress.setMax(100);
            confirmProgress.setProgressTintList(ColorStateList.valueOf(UI.PRIMARY));
            confirmProgress.setProgressBackgroundTintList(ColorStateList.valueOf(0x5548445F));
            confirmProgress.setProgress(0);
            panel.addView(confirmProgress, UI.lp(c, -1, UI.dp(c, 4), 0, 0, 0, 2));
            confirmStatus = UI.tv(c, "در انتظار شروع", 9, UI.MUTED, false);
            confirmStatus.setSingleLine(false);
            confirmStatus.setMaxLines(4);
            confirmStatus.setEllipsize(TextUtils.TruncateAt.END);
            panel.addView(confirmStatus, UI.lp(c, -1, -2, 0, 0, 0, 7));

            LinearLayout row = UI.hbox(c);
            if (mode == 3) {
                row.addView(UI.btn(c, "اسکن جدید", UI.PRIMARY, 0xFF0B1220,
                        v -> { if (onPrimary != null) onPrimary.run(); }), UI.lpw(1f));
                row.addView(UI.btn(c, "بستن گزارش", UI.CARD2, UI.TEXT,
                        v -> { if (onExit != null) onExit.run(); }), UI.lp(c, -2, -2, 7, 0, 0, 0));
            } else {
                if (mode == 0) {
                    row.addView(UI.btn(c, "تایید و شروع", UI.PRIMARY, 0xFF0B1220,
                            v -> { if (onPrimary != null) onPrimary.run(); }), UI.lpw(1f));
                    row.addView(UI.btn(c, "بعداً", UI.CARD2, UI.TEXT,
                            v -> { if (onLater != null) onLater.run(); }), UI.lp(c, -2, -2, 7, 0, 0, 0));
                } else if (mode == 1) {
                    row.addView(UI.btn(c, "توقف اسکن", UI.CARD2, UI.PRIMARY,
                            v -> { if (onPrimary != null) onPrimary.run(); }), UI.lpw(1f));
                } else {
                    row.addView(UI.btn(c, "ادامه اسکن", UI.PRIMARY, 0xFF0B1220,
                            v -> { if (onPrimary != null) onPrimary.run(); }), UI.lpw(1f));
                }
                row.addView(UI.btn(c, "خروج", UI.CARD2, UI.DANGER,
                        v -> { if (onExit != null) onExit.run(); }), UI.lp(c, -2, -2, 7, 0, 0, 0));
            }
            panel.addView(row);

            panel.addOnAttachStateChangeListener(confirmWatch);
            confirmRoot = panel;
            confirmParams = params;
            confirmMode = mode;
            confirmOperator = safeName;
            confirmTouchBlocked = false;
            overlayAt = android.os.SystemClock.elapsedRealtime();
            UI.applyTypefaceTree(panel);
            wm.addView(panel, params);
            added = true;
            confirmAttached = true;   // authoritative: the window now exists in WindowManager
            confirmLastVerifiedAt = android.os.SystemClock.elapsedRealtime();
            updateConfirmProgress(percent, steps, packageCount, status);
            return true;
        } catch (Exception e) {
            confirmError = e.getClass().getSimpleName();
            if (added) {
                try { wm.removeView(panel); } catch (RuntimeException ignored) {}
            }
            confirmAttached = false;
            confirmRoot = null;
            confirmParams = null;
            confirmMode = -1;
            confirmOperator = "";
            confirmTouchBlocked = false;
            confirmProgress = null;
            confirmProgressLabel = null;
            confirmStatus = null;
            android.util.Log.w("BastehyabPopup", "Could not show scan controls: " + confirmError);
            return false;
        }
    }

    String confirmError() { return confirmError; }

    /**
     * Temporarily lets injected scroll gestures pass through this control panel. The scan worker
     * requests this only for the 420ms swipe; all buttons and dragging are restored on completion.
     * Must be called on the service main thread.
     */
    void setConfirmTouchBlocked(boolean blocked) {
        if (!confirmAttached || confirmRoot == null || confirmParams == null
                || confirmTouchBlocked == blocked) return;
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        if (blocked) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        confirmParams.flags = flags;
        try {
            wm.updateViewLayout(confirmRoot, confirmParams);
            confirmTouchBlocked = blocked;
        } catch (RuntimeException error) {
            confirmError = error.getClass().getSimpleName();
            android.util.Log.w("BastehyabPopup", "Could not update scan-control touch mode: "
                    + confirmError);
        }
    }

    /** Removes the scan-control window for sure; safe to call repeatedly. */
    void hideConfirm() {
        final LinearLayout panel = confirmRoot;
        if (panel != null && confirmAttached) {
            overlayAt = android.os.SystemClock.elapsedRealtime();
            try {
                wm.removeView(panel);
            } catch (RuntimeException e) {
                // Detachment during an app/window transition is already an effective hide.
                confirmError = e.getClass().getSimpleName();
                android.util.Log.d("BastehyabPopup", "Scan controls already detached: "
                        + confirmError);
            }
        }
        confirmAttached = false;
        confirmTouchBlocked = false;
        confirmLastVerifiedAt = 0L;
        confirmRoot = null;
        confirmParams = null;
        confirmMode = -1;
        confirmOperator = "";
        confirmProgress = null;
        confirmProgressLabel = null;
        confirmStatus = null;
    }

    private void updateConfirmProgress(int percent, int steps, int packageCount, String status) {
        int safePercent = Math.max(0, Math.min(100, percent));
        if (confirmProgress != null) confirmProgress.setProgress(safePercent);
        if (confirmProgressLabel != null) {
            String prefix = confirmMode == 3 ? "نتیجه‌ی پیمایش" : "پیشرفت تقریبی پیمایش";
            confirmProgressLabel.setText(prefix + " • " + Fmt.fa(String.valueOf(safePercent)) + "٪");
        }
        if (confirmStatus != null) {
            String line = (status == null || status.trim().isEmpty() ? "در انتظار شروع" : status.trim());
            line += "\n" + Fmt.fa(String.valueOf(steps)) + " مرحله"
                    + "  |  " + Fmt.fa(String.valueOf(packageCount)) + " بسته‌ی یکتا";
            confirmStatus.setText(line);
        }
    }

    private String sig(Compare.Result r) {
        StringBuilder b = new StringBuilder();
        b.append(Cfg.pmode(c)).append('|').append(Cfg.rows(c)).append('|').append(compact).append('|')
                .append(r.metric).append('|').append(r.bucket).append('|').append(Cfg.displayCurrency(c)).append('|').append(r.ranked.size());
        int n = Math.min(12, r.ranked.size());
        for (int i = 0; i < n; i++) b.append(r.ranked.get(i).key).append(Cfg.finalPrice(c, r.ranked.get(i)));
        for (Pkg p : r.screen) b.append('#').append(p.key).append(Cfg.finalPrice(c, p));
        for (Op o : Cfg.ops(c)) b.append(o.cmp ? '1' : '0');
        return b.toString();
    }

    private void makeParams() {
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        boolean bottom = Cfg.bottom(c);
        lp = new WindowManager.LayoutParams(
                dm.widthPixels - UI.dp(c, 20), WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = (bottom ? Gravity.BOTTOM : Gravity.TOP) | Gravity.CENTER_HORIZONTAL;
        lp.y = UI.dp(c, bottom ? 90 : 48);
    }

    private TextView icon(String s) {
        TextView t = UI.tv(c, s, 16, UI.MUTED, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(UI.dp(c, 10), UI.dp(c, 4), UI.dp(c, 10), UI.dp(c, 4));
        return t;
    }

    private View ticks() {
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout r = UI.hbox(c);
        List<Op> ops = Cfg.ops(c);
        for (final Op o : ops) {
            TextView t = UI.tv(c, (o.cmp ? "✓ " : "") + o.name, 11, o.cmp ? UI.contrastText(o.color) : UI.TEXT, true);
            if (o.cmp) t.setTextColor(UI.contrastText(o.color));
            t.setPadding(UI.dp(c, 10), UI.dp(c, 4), UI.dp(c, 10), UI.dp(c, 4));
            t.setBackground(UI.rr(c, o.cmp ? o.color : UI.CARD2, 12));
            t.setOnClickListener(v -> {
                o.cmp = !o.cmp;
                Cfg.saveOps(c, ops);
                last = Compare.run(c, screen);
                if (attached) render();
            });
            r.addView(t, UI.lp(c, -2, -2, 0, 0, 6, 0));
        }
        hs.addView(r);
        return hs;
    }

    private void render() {
        final Compare.Result r = last;
        int pad = UI.dp(c, 12);
        root.removeAllViews();
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackground(UI.rr(c, UI.CARD, 20, UI.STROKE));
        root.setElevation(UI.dp(c, 8));

        LinearLayout hd = UI.hbox(c);
        hd.addView(UI.tv(c, "⚡ مقایسه‌ی آنی", 15, UI.PRIMARY, true), UI.lpw(1f));
        TextView min = icon(compact ? "▴" : "▾");
        min.setOnClickListener(v -> {
            compact = !compact;
            lastSig = "";
            render();
        });
        TextView x = icon("✕");
        x.setOnClickListener(v -> {
            hide();
            if (onClose != null) onClose.run();
        });
        hd.addView(min);
        hd.addView(x);
        hd.setOnTouchListener(new View.OnTouchListener() {
            float sy;
            int oy;

            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (lp == null || !attached) return false;
                        sy = e.getRawY();
                        oy = lp.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (lp == null || root == null || !attached) return false;
                        float d = e.getRawY() - sy;
                        boolean bottom = (lp.gravity & Gravity.BOTTOM) == Gravity.BOTTOM;
                        lp.y = oy + (int) (bottom ? -d : d);
                        try {
                            wm.updateViewLayout(root, lp);
                        } catch (Exception ignored) {}
                        return true;
                    default:
                        return false;
                }
            }
        });
        root.addView(hd);

        String sub = Fmt.fa(String.valueOf(r.ranked.size())) + " بسته از " + Fmt.fa(String.valueOf(r.ops))
                + " اپراتور • " + r.label + (r.bucket != 0 ? " • " + Compare.BUCKETS[r.bucket] : "");
        root.addView(UI.tv(c, sub, 11, UI.MUTED, false), UI.lp(c, -1, -2, 0, 0, 0, 6));
        if (!compact) root.addView(ticks(), UI.lp(c, -1, -2, 0, 0, 0, 6));

        int mode = Cfg.pmode(c);
        boolean showScreen = mode != 1, showAll = mode != 2;

        if (compact) {
            if (showAll && !r.ranked.isEmpty()) root.addView(mini("برترین کل", r, r.ranked.get(0)), UI.lp(c, -1, -2, 0, 3, 0, 3));
            if (showScreen && !r.screen.isEmpty()) root.addView(mini("✓ این صفحه", r, r.screen.get(0)), UI.lp(c, -1, -2, 0, 3, 0, 3));
            return;
        }

        Set<String> shown = new HashSet<>();
        if (showScreen && !r.screen.isEmpty()) {
            root.addView(UI.tv(c, "✓ بهترین‌های همین صفحه (از " + Fmt.fa(String.valueOf(r.screen.size())) + " بسته)", 11, UI.PRIMARY, true), UI.lp(c, -1, -2, 0, 4, 0, 2));
            int k = Math.min(3, r.screen.size());
            for (int i = 0; i < k; i++) {
                Pkg p = r.screen.get(i);
                shown.add(p.key);
                root.addView(row(r, p, r.rank.containsKey(p.key) ? r.rank.get(p.key) : i + 1, true), UI.lp(c, -1, -2, 0, 3, 0, 3));
            }
        }
        if (showAll && !r.ranked.isEmpty()) {
            root.addView(UI.tv(c, "برترین‌ها در همه‌ی اپراتورهای تیک‌خورده", 11, UI.ACCENT, true), UI.lp(c, -1, -2, 0, 6, 0, 2));
            int n = 0;
            for (Pkg p : r.ranked) {
                if (n >= Cfg.rows(c)) break;
                if (shown.contains(p.key)) continue;
                n++;
                root.addView(row(r, p, r.rank.get(p.key), false), UI.lp(c, -1, -2, 0, 3, 0, 3));
            }
        }

        String foot;
        if (r.ranked.isEmpty()) {
            foot = "هیچ اپراتوری تیک نخورده یا با فیلترهای شما بسته‌ای نمانده است.";
        } else if (r.screen.isEmpty() && screen != null && !screen.isEmpty()) {
            foot = "بسته‌های همین صفحه با فیلترها (حجم، مدت، ویژه‌ها) جور نیستند؛ بالا فقط برترین‌های اپراتورهای دیگر است.";
        } else if (r.ops < 2) {
            foot = "برای مقایسه‌ی بین اپراتورها، برنامه‌ی اپراتور دیگری را هم باز کنید.";
        } else if (r.bestScreen != null && r.metric == Cfg.DURATION) {
            double bestDays = r.ranked.get(0).days, mineDays = r.bestScreen.days;
            long difference = Math.max(0L, Math.round(bestDays - mineDays));
            foot = r.rankScreen == 1 ? "بسته‌ی همین صفحه بیشترین اعتبار را دارد."
                    : "بسته‌ی همین صفحه رتبه‌ی " + Fmt.fa(String.valueOf(r.rankScreen)) + " را دارد و "
                    + Fmt.fa(String.valueOf(difference)) + " روز اعتبار کمتری از برترین دارد.";
        } else if (r.bestScreen != null) {
            double best = r.val.get(r.ranked.get(0).key), mine = r.val.get(r.bestScreen.key);
            long pct = best > 0 ? Math.round((mine - best) / best * 100) : 0;
            foot = r.rankScreen == 1 ? "رتبه ۱ صفحه، برترین کل است."
                    : "بهترین بسته‌ی همین صفحه، رتبه‌ی " + Fmt.fa(String.valueOf(r.rankScreen)) + " کل است و "
                    + Fmt.fa(String.valueOf(pct)) + "٪ گران‌تر از برترین.";
        } else {
            foot = "برای دقت بیشتر، لیست بسته‌ها را تا آخر اسکرول کنید.";
        }
        root.addView(UI.tv(c, foot, 11, UI.MUTED, false), UI.lp(c, -1, -2, 0, 6, 0, 0));
    }

    private View mini(String label, Compare.Result r, Pkg p) {
        Op op = Cfg.opById(c, p.op);
        int col = op == null ? UI.MUTED : op.color;
        LinearLayout row = UI.hbox(c);
        int pd = UI.dp(c, 8);
        row.setPadding(pd, pd, pd, pd);
        row.setBackground(UI.rr(c, UI.CARD, 12, UI.STROKE));
        row.setElevation(UI.dp(c, 4));
        int numericRank = r.rank.containsKey(p.key) ? r.rank.get(p.key) : 0;
        TextView badge = UI.tv(c, Fmt.fa(String.valueOf(numericRank)), 12, UI.TEXT, true);
        badge.setGravity(Gravity.CENTER);
        badge.setIncludeFontPadding(false);
        badge.setBackground(UI.rr(c, numericRank == 1 ? UI.ACCENT : UI.CARD2, 9));
        row.addView(badge, UI.lp(c, 28, 28, 0, 0, 6, 0));
        row.addView(UI.tv(c, label, 10, UI.MUTED, true));
        row.addView(UI.chip(c, op == null ? p.op : op.name, UI.tint(col), col), UI.lp(c, -2, -2, 6, 0, 0, 0));
        row.addView(UI.tv(c, p.label(), 13, UI.TEXT, true), UI.lp2(c, 8));
        String value = r.metric == Cfg.DURATION ? Fmt.dec(p.days) :
                Fmt.num(Math.round(Cfg.displayMetric(c, r.val.get(p.key))));
        String unit = r.metric == Cfg.DURATION ? " روز" : " " + Cfg.metricUnit(c, r.unit);
        row.addView(UI.tv(c, value + unit, 12, UI.PRIMARY, true));
        return row;
    }

    private View row(Compare.Result r, Pkg p, int badgeNo, boolean here) {
        Op op = Cfg.opById(c, p.op);
        int g = r.rank.containsKey(p.key) ? r.rank.get(p.key) : badgeNo;
        int col = op == null ? UI.MUTED : op.color;
        LinearLayout card = UI.vbox(c);
        int pd = UI.dp(c, 9);
        card.setPadding(pd, pd, pd, pd);
        card.setBackground(UI.rr(c, g == 1 ? 0xFF16283A : UI.CARD, 12,
                here ? UI.PRIMARY : UI.STROKE));
        card.setElevation(UI.dp(c, g == 1 ? 6 : 4));

        LinearLayout head = UI.hbox(c);
        TextView badge = UI.tv(c, Fmt.fa(String.valueOf(g)), 12, UI.TEXT, true);
        badge.setGravity(Gravity.CENTER);
        badge.setIncludeFontPadding(false);
        badge.setBackground(UI.rr(c, g == 1 ? UI.ACCENT : UI.CARD2, 10));
        head.addView(badge, UI.lp(c, 30, 30, 0, 0, 7, 0));
        head.addView(UI.chip(c, op == null ? p.op : op.name, UI.tint(col), col));
        if (p.restricted) head.addView(UI.chip(c, "ویژه", 0x33F5B84B, UI.ACCENT),
                UI.lp(c, -2, -2, 5, 0, 0, 0));
        if (r.cheap.contains(p.key)) head.addView(UI.chip(c, "بررسی قیمت", 0x33F5B84B, UI.ACCENT),
                UI.lp(c, -2, -2, 5, 0, 0, 0));
        card.addView(head);

        TextView label = UI.tv(c, p.label(), 13, UI.TEXT, true);
        label.setMaxLines(1);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(label, UI.lp(c, -1, -2, 0, 5, 0, 5));

        LinearLayout foot = UI.hbox(c);
        LinearLayout total = UI.vbox(c);
        total.addView(UI.tv(c, "قیمت نهایی", 9, UI.MUTED, false));
        total.addView(UI.tv(c, Cfg.priceLabel(c, Cfg.finalPrice(c, p)), 11, UI.TEXT, true));
        foot.addView(total, UI.lpw(1f));
        LinearLayout metric = UI.vbox(c);
        metric.setGravity(Gravity.CENTER_HORIZONTAL);
        metric.addView(UI.tv(c, "مقایسه", 9, UI.MUTED, false));
        String metricValue = r.metric == Cfg.DURATION ? Fmt.dec(p.days) :
                Fmt.num(Math.round(Cfg.displayMetric(c, r.val.get(p.key))));
        String metricUnit = r.metric == Cfg.DURATION ? "روز" : Cfg.metricUnit(c, r.unit);
        metric.addView(UI.tv(c, metricValue + " " + metricUnit,
                11, g == 1 ? UI.PRIMARY : UI.TEXT, true));
        foot.addView(metric, UI.lpw(1f));
        card.addView(foot);
        return card;
    }
}
