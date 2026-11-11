package app.bastehyab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import android.widget.LinearLayout;
import android.widget.ProgressBar;

import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    interface IntCb { void on(int v); }
    interface StrCb { void on(String s); }
    interface W { void w(OutputStream o) throws Exception; }

    private static final int[] ICONS = {R.drawable.ic_home, R.drawable.ic_compare, R.drawable.ic_tune};
    private static final String[] NAMES = {"خانه", "مقایسه", "تنظیمات"};

    private FrameLayout body;
    private FrameLayout outer;
    private SplashViews.AppBackdrop appBackdrop;
    private View splashView;
    private boolean splashShowing;
    private final Handler splashHandler = new Handler(Looper.getMainLooper());
    private final Handler liveHandler = new Handler(Looper.getMainLooper());
    private final Runnable liveRefresh = new Runnable() {
        @Override public void run() {
            if (isFinishing()) return;
            if (tab == 0 && !splashShowing) render();
            if (ScanService.fullMode) liveHandler.postDelayed(this, 1500L);
        }
    };
    private LinearLayout nav;
    private ScrollView sv;
    private int tab;
    private final int[] scroll = new int[3];
    private String newName = "", newPkg = "";
    private int newCur;

    private int dp(float v) { return UI.dp(this, v); }

    private void applySystemBars() {
        getWindow().setStatusBarColor(UI.BG);
        getWindow().setNavigationBarColor(UI.CARD);
        int flags = UI.light ? (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) : 0;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        UI.applyTheme(this);
        applySystemBars();
        LinearLayout root = UI.vbox(this);
        root.setBackgroundColor(0x00000000);
        body = new FrameLayout(this);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
        nav = UI.hbox(this);
        nav.setBackground(UI.rr(this, UI.CARD, 24, UI.STROKE));
        nav.setElevation(dp(12));
        nav.setPadding(dp(7), dp(7), dp(7), dp(7));
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(-1, -2);
        navParams.setMargins(dp(14), dp(5), dp(14), dp(12));
        root.addView(nav, navParams);
        outer = new FrameLayout(this);
        appBackdrop = new SplashViews.AppBackdrop(this);
        outer.addView(appBackdrop, new FrameLayout.LayoutParams(-1, -1));
        outer.addView(root, new FrameLayout.LayoutParams(-1, -1));
        setContentView(outer);
        if (b == null) showCreatorSplash();
        else {
            tab = Cfg.clamp(b.getInt("state.tab", 0), 0, 2);
            for (int i = 0; i < scroll.length; i++) scroll[i] = Math.max(0, b.getInt("state.scroll." + i, 0));
            render(); // Rebuild the visible screen and restore the selected tab/scroll position.
        }
    }

    private void showCreatorSplash() {
        splashShowing = true;
        FrameLayout shell = new FrameLayout(this);
        // Reuse the activity's static cyber wallpaper: one compositor, no stacked animation layers.
        shell.setBackgroundColor(0x00000000);
        View veil = new View(this);
        veil.setBackground(UI.grad(this, UI.light ? 0x22FFFFFF : UI.tint(UI.PRIMARY),
                UI.light ? 0x11EAF0FA : UI.tint(UI.ACCENT), 0));
        shell.addView(veil, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        shell.addView(scrollView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout col = UI.vbox(this);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(dp(24), dp(30), dp(24), dp(24));
        scrollView.addView(col, new ScrollView.LayoutParams(-1, -2));

        FrameLayout logo = new FrameLayout(this);
        logo.setElevation(dp(12));
        logo.addView(new SplashViews.LogoHalo(this),
                new FrameLayout.LayoutParams(dp(128), dp(128), Gravity.CENTER));
        SplashViews.BrandMark brandMark = new SplashViews.BrandMark(this);
        brandMark.setElevation(dp(7));
        logo.addView(brandMark,
                new FrameLayout.LayoutParams(dp(120), dp(120), Gravity.CENTER));
        col.addView(logo, UI.lp(this, 140, 140, 0, 0, 0, 12));

        TextView title = UI.tv(this, "بسته‌یاب", 34, UI.TEXT, true);
        title.setGravity(Gravity.CENTER);
        col.addView(title, UI.lp(this, -1, -2, 0, 0, 0, 2));
        TextView latin = UI.tv(this, "BASTEHYAB", 11, UI.PRIMARY, true);
        latin.setLetterSpacing(0.18f);
        latin.setTextDirection(View.TEXT_DIRECTION_LTR);
        latin.setGravity(Gravity.CENTER);
        col.addView(latin, UI.lp(this, -1, -2, 0, 0, 0, 5));
        TextView subtitle = UI.tv(this, "مقایسه‌ی هوشمند بسته‌های اینترنت", 14, UI.ACCENT, true);
        subtitle.setGravity(Gravity.CENTER);
        col.addView(subtitle, UI.lp(this, -1, -2, 0, 0, 0, 20));

        LinearLayout features = UI.hbox(this);
        features.setGravity(Gravity.CENTER);
        String[] featureNames = {"اسکن محلی", "مقایسه قیمت", "بدون ارسال داده"};
        for (int i=0;i<featureNames.length;i++) {
            TextView f = UI.chip(this, featureNames[i], UI.tint(UI.ACCENT), UI.ACCENT);
            features.addView(f, UI.lp(this, -2, -2, i==0?0:5, 0, 0, 0));
        }
        col.addView(features, UI.lp(this, -1, -2, 0, 0, 0, 18));

        LinearLayout how = UI.card(this);
        how.setPadding(dp(16), dp(15), dp(16), dp(15));
        how.addView(UI.tv(this, "سه گام تا مقایسه", 14, UI.TEXT, true));
        LinearLayout steps = UI.hbox(this);
        String[] stepsText = {"بازکردن اپراتور", "ورود به بسته‌ها", "مقایسه خودکار"};
        for (int i=0;i<stepsText.length;i++) {
            LinearLayout step = UI.vbox(this);
            step.setGravity(Gravity.CENTER);
            TextView n = UI.tv(this, Fmt.fa(String.valueOf(i+1)), 12, UI.TEXT, true);
            n.setGravity(Gravity.CENTER);
            n.setBackground(UI.rr(this, UI.tint(UI.ACCENT), 18, UI.STROKE));
            step.addView(n, UI.lp(this, 30, 30, 0, 0, 0, 5));
            TextView label = UI.tv(this, stepsText[i], 10, UI.MUTED, true);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            step.addView(label);
            steps.addView(step, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        how.addView(steps, UI.lp(this, -1, -2, 0, 12, 0, 0));
        col.addView(how, UI.lp(this, -1, -2, 0, 0, 0, 14));

        TextView enter = UI.btn(this, "ورود به برنامه", UI.PRIMARY, 0xFF061326, v -> enterApp());
        col.addView(enter, UI.lp(this, -1, -2, 0, 0, 0, 14));

        LinearLayout creator = UI.hbox(this);
        creator.setGravity(Gravity.CENTER_VERTICAL);
        creator.setPadding(dp(14), dp(13), dp(14), dp(13));
        creator.setBackground(UI.rr(this, UI.CARD2, 18, UI.STROKE));
        creator.setElevation(dp(6));
        creator.addView(new SplashViews.PersonAvatar(this), UI.lp(this, 66, 66, 0, 0, 12, 0));
        LinearLayout creatorText = UI.vbox(this);
        creatorText.addView(UI.tv(this, "طراحی و توسعه", 10, UI.MUTED, false));
        creatorText.addView(UI.tv(this, "فرشید نبی‌پور", 16, UI.TEXT, true),
                UI.lp(this, -1, -2, 0, 3, 0, 0));
        creator.addView(creatorText, UI.lpw(1f));
        TextView contact = UI.tv(this, "ارتباط با سازنده  ↗", 10, UI.PRIMARY, true);
        contact.setGravity(Gravity.CENTER);
        contact.setPadding(dp(10), dp(10), dp(10), dp(10));
        contact.setBackground(UI.rr(this, UI.tint(UI.ACCENT), 12, UI.STROKE));
        contact.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Farshid_Np"))); }
            catch (Exception e) { toast("امکان باز کردن پیوند ارتباطی وجود ندارد"); }
        });
        creator.addView(contact, UI.lp(this, -2, -2, 8, 0, 0, 0));
        col.addView(creator, UI.lp(this, -1, -2, 0, 0, 0, 8));

        TextView privacy = UI.tv(this, "پردازش روی همین گوشی انجام می‌شود؛ اطلاعات صفحه به سرور ارسال نمی‌شود.\nنسخه ۱.۷.۹  •  © ۲۰۲۶", 10, UI.MUTED, false);
        privacy.setGravity(Gravity.CENTER);
        privacy.setMaxLines(3);
        col.addView(privacy, UI.lp(this, -1, -2, 0, 0, 0, 0));

        UI.applyTypefaceTree(shell);
        splashView = shell;
        outer.addView(splashView, new FrameLayout.LayoutParams(-1, -1));
    }

    private void enterApp() {
        if (!splashShowing) return;
        View fading = splashView;
        if (fading == null) {
            splashShowing = false;
            render();
            return;
        }
        fading.animate().alpha(0f).setDuration(180L).withEndAction(() -> {
            if (splashView == fading) {
                outer.removeView(fading);
                splashView = null;
                splashShowing = false;
                render();
            }
        }).start();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("state.tab", tab);
        for (int i = 0; i < scroll.length; i++) out.putInt("state.scroll." + i, scroll[i]);
        super.onSaveInstanceState(out);
    }

    @Override protected void onResume() {
        super.onResume();
        if (appBackdrop != null) appBackdrop.resumeLoop();
        if (!splashShowing) render();
        if (ScanService.fullMode) liveHandler.post(liveRefresh);
    }

    @Override protected void onPause() {
        liveHandler.removeCallbacks(liveRefresh);
        if (appBackdrop != null) appBackdrop.pauseLoop();
        Store.flush(this);
        super.onPause();
    }

    @Override protected void onDestroy() {
        splashHandler.removeCallbacksAndMessages(null);
        liveHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // ---------------------------------------------------------------- shell

    private void render() { render(true); }

    private void render(boolean captureCurrentScroll) {
        if (captureCurrentScroll && sv != null) scroll[tab] = sv.getScrollY();
        body.removeAllViews();
        final ScrollView screenScroll = new ScrollView(this);
        screenScroll.setFillViewport(true);
        screenScroll.setVerticalScrollBarEnabled(false);
        screenScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        screenScroll.setClipToPadding(false);
        screenScroll.setClipChildren(false);
        sv = screenScroll;
        LinearLayout col = UI.vbox(this);
        col.setClipChildren(false);
        col.setPadding(dp(18), dp(18), dp(18), dp(28));
        screenScroll.addView(col, new FrameLayout.LayoutParams(-1, -2));
        body.addView(screenScroll, new FrameLayout.LayoutParams(-1, -1));
        if (tab == 0) home(col);
        else if (tab == 1) compare(col);
        else settings(col);
        UI.applyTypefaceTree(col);
        final int y = scroll[tab];
        screenScroll.post(() -> screenScroll.scrollTo(0, y));
        buildNav();
    }

    private void buildNav() {
        nav.removeAllViews();
        nav.setPadding(dp(7), dp(7), dp(7), dp(7));
        GradientDrawable navBg = UI.rr(this, UI.CARD, 24, UI.STROKE);
        navBg.setStroke(dp(1), UI.STROKE);
        nav.setBackground(navBg);
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            boolean on = i == tab;
            LinearLayout it = UI.vbox(this);
            it.setGravity(Gravity.CENTER);
            it.setPadding(dp(7), dp(7), dp(7), dp(7));
            it.setBackground(on
                    ? UI.rr(this, UI.CARD2, 17, UI.PRIMARY)
                    : UI.rr(this, 0x00000000, 17));
            ImageView im = new ImageView(this);
            im.setImageResource(ICONS[i]);
            im.setColorFilter(on ? UI.PRIMARY : UI.MUTED);
            it.addView(im, UI.lp(this, 22, 22, 0, 1, 0, 0));
            TextView label = UI.tv(this, NAMES[i], 10, on ? UI.PRIMARY : UI.MUTED, on);
            label.setGravity(Gravity.CENTER);
            it.addView(label, UI.lp(this, -2, -2, 0, 3, 0, 0));
            it.setContentDescription(NAMES[i] + (on ? "، صفحه فعلی" : "، رفتن به صفحه"));
            it.setSelected(on);
            it.setOnClickListener(v -> {
                // Re-tapping the active destination is intentionally a no-op.
                // Rebuilding the same screen used to reset focus/layout and cause a visible jump.
                if (idx == tab) return;
                if (sv != null) scroll[tab] = sv.getScrollY();
                tab = idx;
                // The target tab already owns its own saved scroll position; do not
                // overwrite it with the outgoing tab's scroll offset during render.
                render(false);
            });
            nav.addView(it, new LinearLayout.LayoutParams(0, -2, 1f));
        }
    }

    private boolean serviceOn() {
        String en = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return en != null && en.contains(getPackageName());
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    // ---------------------------------------------------------------- home

    private void home(LinearLayout col) {
        boolean permission = serviceOn();
        List<Op> ops = Cfg.ops(this);
        List<Pkg> all = Store.all(this);
        Compare.Result cr = Compare.run(this, new HashSet<String>());
        int active = 0, withData = 0;
        long latest = 0;
        for (Op o : ops) if (o.enabled) active++;
        for (Pkg p : all) if (p.ts > latest) latest = p.ts;
        for (Op o : ops) {
            for (Pkg p : all) if (p.op.equals(o.id)) { withData++; break; }
        }

        LinearLayout hero = UI.hbox(this);
        hero.setPadding(dp(17), dp(17), dp(17), dp(17));
        GradientDrawable heroBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                UI.mono ? new int[]{0xFF202833, UI.CARD}
                        : UI.light ? new int[]{0xFFF7FAFC, UI.CARD}
                        : new int[]{UI.CARD2, UI.CARD});
        heroBg.setCornerRadius(dp(22));
        heroBg.setStroke(dp(1), UI.STROKE);
        hero.setElevation(dp(2));
        hero.setBackground(heroBg);
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.mipmap.ic_launcher);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        hero.addView(mark, UI.lp(this, 70, 70, 0, 0, 14, 0));
        LinearLayout brand = UI.vbox(this);
        brand.addView(UI.tv(this, "بسته‌یاب", 24, UI.TEXT, true));
        brand.addView(UI.tv(this, "مقایسه‌ی هوشمند بسته‌های اینترنت", 11, UI.MUTED, false), UI.lp(this, -1, -2, 0, 3, 0, 6));
        brand.addView(UI.chip(this, permission ? "● دسترسی فعال" : "● دسترسی غیرفعال",
                permission ? UI.tint(UI.SUCCESS) : UI.tint(UI.DANGER), permission ? UI.SUCCESS : UI.DANGER));
        hero.addView(brand, UI.lpw(1f));
        col.addView(hero);

        if (!permission) {
            LinearLayout access = UI.card(this);
            access.addView(UI.tv(this, "برای شروع، دسترسی اسکن را فعال کنید", 14, UI.TEXT, true));
            access.addView(UI.tv(this, "بسته‌یاب فقط محتوای قابل‌مشاهده‌ی صفحه‌ی بسته‌های اپراتوری را که خودتان باز می‌کنید می‌خواند؛ خریدی انجام نمی‌دهد و اطلاعاتی ارسال نمی‌کند.", 11, UI.MUTED, false), UI.lp(this, -1, -2, 0, 5, 0, 9));
            access.addView(UI.btn(this, "فعال‌سازی دسترسی اندروید", UI.PRIMARY, 0xFF061326, v ->
                    new AlertDialog.Builder(this)
                            .setTitle("تأیید دسترسی")
                            .setMessage("این سرویس فقط برای خواندن اطلاعات قابل‌مشاهده‌ی بسته‌ها در اپراتور انتخابی استفاده می‌شود. متن خام صفحه ذخیره یا ارسال نمی‌شود و هیچ خریدی انجام نمی‌شود. برای ادامه، تنظیمات دسترسی‌پذیری باز شود؟")
                            .setNegativeButton("فعلاً نه", null)
                            .setPositiveButton("باز کردن تنظیمات", (dialog, which) -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                            .show()), UI.lp(this, -1, -2, 0, 0, 0, 0));
            col.addView(access, UI.lp(this, -1, -2, 0, 12, 0, 0));
        }

        col.addView(sectionTitle("نمای کلی", "آمار ذخیره‌شده روی این گوشی"), UI.lp(this, -1, -2, 0, 16, 0, 8));
        LinearLayout stats1 = UI.hbox(this);
        stats1.addView(stat(Fmt.fa(String.valueOf(cr.ranked.size())), "بسته‌ی معتبر"), UI.lpw(1f));
        stats1.addView(stat(Fmt.fa(String.valueOf(withData)), "اپراتور دارای داده"), UI.lp2(this, 8));
        LinearLayout stats2 = UI.hbox(this);
        stats2.addView(stat(Fmt.fa(String.valueOf(active)), "اپراتور فعال"), UI.lpw(1f));
        stats2.addView(stat(latest == 0 ? "—" : Fmt.ago(latest), "آخرین دریافت"), UI.lp2(this, 8));
        col.addView(stats1, UI.lp(this, -1, -2, 0, 0, 0, 8));
        col.addView(stats2, UI.lp(this, -1, -2, 0, 0, 0, 0));

        LinearLayout scan = UI.card(this);
        LinearLayout scanHead = UI.hbox(this);
        scanHead.addView(UI.tv(this, "وضعیت آخرین اسکن", 14, UI.TEXT, true), UI.lpw(1f));
        scanHead.addView(UI.chip(this, ScanService.fullMode ? (ScanService.fullPaused ? "متوقف موقت" : "در حال اجرا") : "گزارش اسکن",
                ScanService.fullMode ? UI.tint(UI.PRIMARY) : UI.tint(UI.ACCENT), ScanService.fullMode ? UI.PRIMARY : UI.MUTED));
        scan.addView(scanHead);
        String scanAge = ScanService.lastCompletedScanAt > 0 ? " • " + Fmt.ago(ScanService.lastCompletedScanAt) : "";
        scan.addView(UI.tv(this, ScanService.lastScanStatus + scanAge, 12, UI.MUTED, false), UI.lp(this, -1, -2, 0, 7, 0, 0));
        if (ScanService.lastCompletedScanAt > 0) {
            scan.addView(UI.tv(this, "مدت پردازش: " + Fmt.fa(String.valueOf(ScanService.lastScanDurationMs)) + " میلی‌ثانیه",
                    9, UI.MUTED, false), UI.lp(this, -1, -2, 0, 4, 0, 0));
        }
        scan.addView(UI.tv(this, "شناسایی " + Fmt.fa(String.valueOf(ScanService.lastScannedPackages))
                + "  |  معتبر " + Fmt.fa(String.valueOf(ScanService.lastValidPackages))
                + "  |  مشکوک " + Fmt.fa(String.valueOf(ScanService.lastSuspiciousPackages))
                + "  |  تکراری " + Fmt.fa(String.valueOf(ScanService.lastDuplicatePackages)), 10, UI.MUTED, false),
                UI.lp(this, -1, -2, 0, 5, 0, 0));
        if (ScanService.fullMode || ScanService.fullSteps > 0) {
            int pct = ScanService.fullFinished ? 100 : Math.min(95, Math.max(1, ScanService.fullSteps * 100 / ScanService.FULL_MAX_STEPS));
            Op target = Cfg.opById(this, ScanService.fullTargetOpId);
            String phase = ScanService.fullMode ? (ScanService.fullPaused ? "اسکن موقتاً متوقف" : (ScanService.fullStartedScanning ? "استخراج خودکار" : (ScanService.fullConfirmed ? "در انتظار صفحه‌ی بسته‌ها" : "در انتظار تایید شما")))
                    : (ScanService.fullFinished ? "استخراج پایان یافت" : "استخراج متوقف شد؛ نیازمند بررسی");
            scan.addView(UI.tv(this, phase + (target == null ? "" : " • " + target.name) + "  •  " + Fmt.fa(String.valueOf(pct)) + "٪",
                    10, UI.PRIMARY, true), UI.lp(this, -1, -2, 0, 7, 0, 3));
            ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setProgressTintList(ColorStateList.valueOf(UI.PRIMARY));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(UI.STROKE));
            progress.setMax(100); progress.setProgress(pct);
            scan.addView(progress, UI.lp(this, -1, 5, 0, 0, 0, 0));
        }
        col.addView(scan, UI.lp(this, -1, -2, 0, 12, 0, 0));

        LinearLayout best = UI.card(this);
        LinearLayout bh = UI.hbox(this);
        bh.addView(UI.tv(this, "بهترین پیشنهاد فعلی", 14, UI.TEXT, true), UI.lpw(1f));
        bh.addView(UI.chip(this, "پیشنهاد هوشمند", UI.tint(UI.ACCENT), UI.ACCENT));
        best.addView(bh);
        if (cr.ranked.isEmpty()) {
            best.addView(UI.tv(this, "هنوز بسته‌ی معتبر برای مقایسه ثبت نشده است. یکی از اپراتورها را باز کنید و وارد صفحه‌ی بسته‌ها شوید.",
                    11, UI.MUTED, false), UI.lp(this, -1, -2, 0, 8, 0, 0));
        } else {
            Pkg p = cr.ranked.get(0);
            Op op = Cfg.opById(this, p.op);
            LinearLayout offer = UI.hbox(this);
            LinearLayout details = UI.vbox(this);
            details.addView(UI.chip(this, op == null ? p.op : op.name, UI.tint(op == null ? UI.MUTED : op.color), op == null ? UI.MUTED : op.color));
            details.addView(UI.tv(this, p.label(), 17, UI.TEXT, true), UI.lp(this, -1, -2, 0, 7, 0, 3));
            details.addView(UI.tv(this, "اعتبار " + Fmt.dec(p.days) + " روز", 10, UI.MUTED, false));
            details.addView(UI.tv(this, "قیمت نهایی: " + Cfg.priceLabel(this, Cfg.finalPrice(this, p)), 11, UI.MUTED, false), UI.lp(this, -1, -2, 0, 5, 0, 0));
            offer.addView(details, UI.lpw(1f));
            LinearLayout metric = UI.vbox(this);
            metric.setGravity(Gravity.CENTER);
            String metricValue = cr.metric == Cfg.DURATION
                    ? Fmt.dec(p.days) : Fmt.num(Math.round(Cfg.displayMetric(this, cr.val.get(p.key))));
            metric.addView(UI.tv(this, metricValue, 22, UI.PRIMARY, true));
            metric.addView(UI.tv(this, Cfg.metricUnit(this, cr.unit), 9, UI.MUTED, false));
            offer.addView(metric);
            best.addView(offer, UI.lp(this, -1, -2, 0, 10, 0, 0));
            best.addView(UI.btn(this, "مشاهده‌ی رتبه‌بندی بسته‌ها", UI.PRIMARY, 0xFF061326, v -> { tab = 1; render(); }),
                    UI.lp(this, -1, -2, 0, 12, 0, 0));
        }
        col.addView(best, UI.lp(this, -1, -2, 0, 12, 0, 0));

        col.addView(sectionTitle("اپراتورها", "وضعیت داده و شروع اسکن هر اپراتور"), UI.lp(this, -1, -2, 0, 16, 0, 8));
        for (final Op o : ops) {
            int count = 0; long ts = 0;
            for (Pkg p : all) if (p.op.equals(o.id)) { count++; if (p.ts > ts) ts = p.ts; }
            LinearLayout row = UI.hbox(this);
            row.setPadding(dp(13), dp(11), dp(13), dp(11));
            row.setMinimumHeight(dp(68));
            row.setBackground(UI.rr(this, UI.CARD, 18, UI.STROKE));
            row.addView(new OperatorLogo(this, o.id), UI.lp(this, 42, 34, 0, 0, 10, 0));
            LinearLayout info = UI.vbox(this);
            info.addView(UI.tv(this, o.name, 13, UI.TEXT, true));
            String status = !o.enabled ? "اسکن غیرفعال" : count > 0 ? Fmt.fa(String.valueOf(count)) + " بسته • " + Fmt.ago(ts) : "آماده‌ی اسکن • هنوز داده‌ای نیست";
            info.addView(UI.tv(this, status, 10, count > 0 ? UI.PRIMARY : UI.MUTED, false), UI.lp(this, -1, -2, 0, 2, 0, 0));
            row.addView(info, UI.lpw(1f));
            row.addView(UI.btn(this, o.enabled ? "اسکن" : "بازکردن", UI.CARD2, UI.PRIMARY, v -> launch(o)));
            col.addView(row, UI.lp(this, -1, -2, 0, 0, 0, 7));
        }
        LinearLayout privacy = UI.hbox(this);
        privacy.setPadding(dp(12), dp(10), dp(12), dp(10));
        privacy.setBackground(UI.rr(this, UI.tint(UI.ACCENT), 13, UI.STROKE));
        privacy.addView(UI.tv(this, "پردازش آفلاین روی همین گوشی", 10, UI.PRIMARY, true), UI.lpw(1f));
        privacy.addView(UI.tv(this, "بدون ارسال اطلاعات", 9, UI.MUTED, false));
        col.addView(privacy, UI.lp(this, -1, -2, 0, 8, 0, 0));
    }

    /** Uses the installed operator application's real launcher identity (offline, no remote logo fetch). */
    private static final class OperatorLogo extends View {
        private final String id;
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.drawable.Drawable operatorIcon;
        private final int brandColor;

        OperatorLogo(Context c, String id) {
            super(c);
            this.id = id == null ? "" : id;
            Op op = Cfg.opById(c, this.id);
            brandColor = op == null ? 0xFF9C72E8 : op.color;
            android.graphics.drawable.Drawable found = null;
            if (op != null && op.pkgs != null) {
                for (String pkg : op.pkgs) {
                    try {
                        found = c.getPackageManager().getApplicationIcon(pkg);
                        break;
                    } catch (android.content.pm.PackageManager.NameNotFoundException ignored) { }
                }
            }
            operatorIcon = found == null ? null : found.getConstantState() == null
                    ? found : found.getConstantState().newDrawable(c.getResources()).mutate();
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setContentDescription(op == null ? "نشان اپراتور" : "نشان برنامه‌ی " + op.name);
        }

        @Override protected void onDraw(android.graphics.Canvas c) {
            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            float side = Math.min(w, h) * .90f;
            float left = (w - side) * .5f, top = (h - side) * .5f;
            paint.setStyle(android.graphics.Paint.Style.FILL);
            paint.setColor(0x22FFFFFF);
            c.drawRoundRect(left - UI.dp(getContext(), 2), top - UI.dp(getContext(), 2),
                    left + side + UI.dp(getContext(), 2), top + side + UI.dp(getContext(), 2),
                    UI.dp(getContext(), 11), UI.dp(getContext(), 11), paint);
            if (operatorIcon != null) {
                operatorIcon.setBounds(Math.round(left), Math.round(top), Math.round(left + side), Math.round(top + side));
                operatorIcon.draw(c);
            } else {
                paint.setColor((brandColor & 0x00FFFFFF) | 0xFF000000);
                c.drawRoundRect(left, top, left + side, top + side, UI.dp(getContext(), 12), UI.dp(getContext(), 12), paint);
                paint.setColor(0xFFFFFFFF);
                paint.setTextAlign(android.graphics.Paint.Align.CENTER);
                paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
                paint.setTextSize(side * .40f);
                String mark = id.equals("irancell") ? "I" : id.equals("mci") ? "M"
                        : id.equals("rightel") ? "R" : id.equals("samantel") ? "S" : "SM";
                c.drawText(mark, w * .5f, h * .5f + side * .14f, paint);
            }
        }
    }

    private View bestMini(String heading, Pkg p) {
        LinearLayout c = UI.vbox(this);
        c.setPadding(dp(9), dp(10), dp(9), dp(10));
        c.setMinimumHeight(dp(88));
        c.setBackground(UI.rr(this, UI.CARD, 14, UI.STROKE));
        c.setElevation(dp(4));
        c.addView(UI.tv(this, heading, 10, UI.ACCENT, true));
        c.addView(UI.tv(this, p.label(), 11, UI.TEXT, true), UI.lp(this, -1, -2, 0, 5, 0, 0));
        c.addView(UI.tv(this, Cfg.priceLabel(this, Cfg.finalPrice(this, p)), 10, UI.MUTED, false));
        return c;
    }

    private View stat(String big, String small) {
        LinearLayout c = UI.vbox(this);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(8), dp(12), dp(8), dp(12));
        c.setMinimumHeight(dp(96));
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(UI.rr(this, UI.CARD, 18, UI.STROKE));
        c.setElevation(dp(5));
        TextView number = UI.tv(this, big, 23, UI.PRIMARY, true);
        number.setGravity(Gravity.CENTER);
        number.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        c.addView(number);
        TextView t = UI.tv(this, small, 10, UI.MUTED, false);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        c.addView(t);
        return c;
    }

    private void launch(Op o) {
        String targetPackage = null;
        Intent launchIntent = null;
        for (String p : o.pkgs) {
            Intent candidate = getPackageManager().getLaunchIntentForPackage(p);
            if (candidate != null) {
                targetPackage = p;
                launchIntent = candidate;
                break;
            }
        }
        if (launchIntent == null || targetPackage == null) {
            toast("برنامه‌ی " + o.name + " پیدا نشد؛ نصب بودن برنامه و نام پکیج را بررسی کنید.");
            return;
        }

        boolean auto = ScanService.inst != null && o.enabled;
        if (auto) ScanService.startFull(o.id, targetPackage);
        try {
            startActivity(launchIntent);
            if (auto) {
                liveHandler.removeCallbacks(liveRefresh);
                liveHandler.post(liveRefresh);
                toast("برنامه‌ی " + o.name + " باز شد؛ پس از ورود به بخش بسته‌های اینترنت، در پنجره‌ی شناور «تایید و شروع» را بزنید. برای مکث از «توقف اسکن» استفاده کنید.");
            } else if (!o.enabled) {
                toast("برنامه باز شد؛ این اپراتور در تنظیمات غیرفعال است و استخراج انجام نمی‌شود.");
            } else {
                toast("برنامه باز شد؛ برای استخراج خودکار، ابتدا سرویس دسترسی بسته‌یاب را فعال کنید.");
            }
        } catch (RuntimeException e) {
            if (auto) ScanService.stopFull("باز کردن برنامه‌ی اپراتور ناموفق بود");
            toast("باز کردن برنامه‌ی " + o.name + " ناموفق بود: " + e.getClass().getSimpleName());
        }
    }

    // ---------------------------------------------------------------- compare

    private void compare(LinearLayout col) {
        LinearLayout header = UI.hbox(this);
        LinearLayout titles = UI.vbox(this);
        titles.addView(UI.tv(this, "مقایسه‌ی بسته‌ها", 22, UI.TEXT, true));
        titles.addView(UI.tv(this, "رتبه‌بندی قیمت، حجم و اعتبار", 11, UI.MUTED, false), UI.lp(this, -1, -2, 0, 3, 0, 0));
        header.addView(titles, UI.lpw(1f));
        header.addView(UI.chip(this, Fmt.fa(String.valueOf(Store.all(this).size())) + " ثبت‌شده", UI.tint(UI.ACCENT), UI.PRIMARY));
        col.addView(header);

        LinearLayout filter = UI.card(this);
        filter.addView(UI.tv(this, "معیار رتبه‌بندی", 13, UI.TEXT, true));
        chips(filter, new String[]{"ارزان‌ترین هر گیگ", "بیشترین اعتبار", "هزینه مصرف من"}, Cfg.metric(this), i -> {
            Cfg.setInt(this, "metric", i); scroll[1] = 0; render();
        });
        filter.addView(UI.tv(this, "مقایسه‌ی بسته‌های هم‌اعتبار", 11, UI.MUTED, false), UI.lp(this, -1, -2, 0, 5, 0, 0));
        chips(filter, Compare.BUCKETS, Cfg.bucket(this), i -> {
            Cfg.setInt(this, "bucket", i); scroll[1] = 0; render();
        });
        if (Cfg.metric(this) == Cfg.MY_USAGE) {
            stepRow(filter, "مصرف ماهانه‌ی شما", "برآورد برای محاسبه‌ی هزینه‌ی مصرف", (int) Cfg.usage(this), 5, 500, 5, " گیگ", v -> {
                Cfg.setFloat(this, "usage", v); render();
            });
        }
        col.addView(filter, UI.lp(this, -1, -2, 0, 12, 0, 0));

        col.addView(tickCard(), UI.lp(this, -1, -2, 0, 10, 0, 6));
        Compare.Result cr = Compare.run(this, new HashSet<String>());
        if (cr.ranked.isEmpty()) {
            LinearLayout empty = UI.card(this);
            empty.setGravity(Gravity.CENTER);
            empty.addView(UI.tv(this, anyCmp() ? "هنوز بسته‌ی قابل مقایسه‌ای نیست" : "اپراتوری انتخاب نشده است", 15, UI.TEXT, true));
            empty.addView(UI.tv(this, "اپراتور را انتخاب کنید و از صفحه‌ی خانه وارد برنامه‌ی آن شوید تا فهرست بسته‌ها خوانده شود.", 11, UI.MUTED, false), UI.lp(this, -1, -2, 0, 7, 0, 0));
            col.addView(empty);
            return;
        }
        LinearLayout summary = UI.hbox(this);
        summary.setPadding(dp(12), dp(10), dp(12), dp(10));
        summary.setBackground(UI.rr(this, UI.tint(UI.ACCENT), 13, UI.STROKE));
        summary.addView(UI.tv(this, Fmt.fa(String.valueOf(cr.ranked.size())) + " بسته‌ی قطعی", 11, UI.PRIMARY, true), UI.lpw(1f));
        summary.addView(UI.tv(this, cr.label, 10, UI.MUTED, false));
        col.addView(summary, UI.lp(this, -1, -2, 0, 10, 0, 8));

        int n = Math.min(80, cr.ranked.size());
        for (int i = 0; i < n; i++) col.addView(pkgRow(cr.ranked.get(i), i + 1, cr), UI.lp(this, -1, -2, 0, 0, 0, 8));
        if (cr.ranked.size() > n) {
            col.addView(UI.tv(this, "برای سبک ماندن صفحه، ۸۰ نتیجه‌ی نخست نمایش داده می‌شود.", 10, UI.MUTED, false), UI.lp(this, -1, -2, 0, 4, 0, 0));
        }
    }

    private boolean anyCmp() {
        for (Op o : Cfg.ops(this)) if (o.cmp) return true;
        return false;
    }

    private void setAllCmp(boolean on) {
        List<Op> ops = Cfg.ops(this);
        for (Op o : ops) o.cmp = on;
        Cfg.saveOps(this, ops);
        render();
    }

    private View tickCard() {
        LinearLayout c = UI.card(this);
        List<Op> ops = Cfg.ops(this);
        LinearLayout hd = UI.hbox(this);
        hd.addView(UI.tv(this, "انتخاب اپراتورها", 13, UI.TEXT, true), UI.lpw(1f));
        hd.addView(UI.btn(this, "همه", UI.CARD2, UI.PRIMARY, v -> setAllCmp(true)), UI.lp(this, -2, -2, 0, 0, 5, 0));
        hd.addView(UI.btn(this, "هیچ‌کدام", UI.CARD2, UI.MUTED, v -> setAllCmp(false)));
        c.addView(hd);
        LinearLayout pills = UI.vbox(this);
        for (int i = 0; i < ops.size(); i += 2) {
            LinearLayout row = UI.hbox(this);
            for (int j = i; j < Math.min(i + 2, ops.size()); j++) {
                final Op o = ops.get(j);
                TextView pill = UI.tv(this, (o.cmp ? "✓  " : "○  ") + o.name, 11, o.cmp ? UI.contrastText(o.color) : UI.MUTED, true);
                if (o.cmp) pill.setTextColor(UI.contrastText(UI.PRIMARY));
                pill.setGravity(Gravity.CENTER);
                pill.setPadding(dp(8), dp(9), dp(8), dp(9));
                pill.setBackground(o.cmp
                        ? UI.grad(this, UI.PRIMARY, UI.BLUE, 13)
                        : UI.rr(this, UI.CARD2, 13, UI.STROKE));
                pill.setOnClickListener(v -> {
                    o.cmp = !o.cmp;
                    Cfg.saveOps(this, ops);
                    render();
                });
                LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, -2, 1f);
                pp.setMargins(dp(3), dp(8), dp(3), 0);
                row.addView(pill, pp);
            }
            if (ops.size() % 2 == 1 && i + 1 == ops.size() - 1) {
                row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
            }
            pills.addView(row);
        }
        c.addView(pills);
        int selected = 0;
        for (Op o : ops) if (o.cmp) selected++;
        c.addView(UI.tv(this, selected == 0 ? "برای نمایش نتیجه، یک اپراتور را انتخاب کنید." :
                Fmt.fa(String.valueOf(selected)) + " اپراتور در مقایسه شرکت می‌کنند.", 10, UI.MUTED, false), UI.lp(this, -1, -2, 0, 7, 0, 0));
        return c;
    }

    private View pkgRow(Pkg p, int rank, Compare.Result cr) {
        Op op = Cfg.opById(this, p.op);
        int actualRank = cr.rank.containsKey(p.key) ? cr.rank.get(p.key) : rank;
        int opColor = op == null ? UI.MUTED : op.color;
        double metric = cr.val.containsKey(p.key) ? cr.val.get(p.key) : 0d;
        double med = cr.median;
        long discount = cr.metric != Cfg.DURATION && med > 0 && metric < med
                ? Math.round((med - metric) * 100d / med) : 0;

        LinearLayout card = UI.vbox(this);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        int fill = actualRank == 1 ? UI.CARD2 : UI.CARD;
        int border = actualRank == 1 ? UI.PRIMARY : UI.STROKE;
        card.setBackground(UI.rr(this, fill, 20, border));
        card.setMinimumHeight(dp(156));
        card.setElevation(actualRank == 1 ? dp(7) : dp(5));

        LinearLayout top = UI.hbox(this);
        TextView badge = UI.tv(this, Fmt.fa(String.valueOf(actualRank)), 13, actualRank == 1 ? 0xFFFFFFFF : UI.TEXT, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(UI.rr(this, actualRank == 1 ? UI.PRIMARY : UI.CARD2, 12));
        top.addView(badge, UI.lp(this, 34, 34, 0, 0, 8, 0));
        top.addView(new OperatorLogo(this, op == null ? p.op : op.id), UI.lp(this, 34, 30, 0, 0, 7, 0));
        top.addView(UI.chip(this, op == null ? p.op : op.name, UI.tint(opColor), opColor));
        if (p.restricted) top.addView(UI.chip(this, "بسته‌ی ویژه", UI.tint(UI.DANGER), UI.DANGER), UI.lp(this, -2, -2, 5, 0, 0, 0));
        if (discount > 0) top.addView(UI.chip(this, Fmt.fa(String.valueOf(discount)) + "٪ زیر میانه", UI.tint(UI.SUCCESS), UI.SUCCESS), UI.lp(this, -2, -2, 5, 0, 0, 0));
        card.addView(top);

        TextView label = UI.tv(this, p.label(), 16, UI.TEXT, true);
        label.setMaxLines(2);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        label.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        card.addView(label, UI.lp(this, -1, -2, 0, 9, 0, 3));
        card.addView(UI.tv(this, "اعتبار بسته: " + Fmt.dec(p.days) + " روز", 10, UI.MUTED, false), UI.lp(this, -1, -2, 0, 0, 0, 10));

        LinearLayout metrics = UI.hbox(this);
        LinearLayout total = UI.vbox(this);
        total.addView(UI.tv(this, "قیمت نهایی", 9, UI.MUTED, false));
        total.addView(UI.tv(this, Cfg.priceLabel(this, Cfg.finalPrice(this, p)), 13, UI.TEXT, true), UI.lp(this, -1, -2, 0, 3, 0, 0));
        metrics.addView(total, UI.lpw(1f));

        LinearLayout per = UI.vbox(this);
        per.setGravity(Gravity.CENTER);
        per.addView(UI.tv(this, "هزینه‌ی هر گیگ", 9, UI.MUTED, false));
        double perGbToman = p.gb > 0 ? Cfg.finalPrice(this, p) / p.gb : 0;
        TextView perValue = UI.tv(this, Cfg.priceLabel(this, Math.round(perGbToman)).replace(" تومان", "")
                .replace(" ریال", "") + " / گیگ", 12, actualRank == 1 ? UI.PRIMARY : UI.TEXT, true);
        perValue.setGravity(Gravity.CENTER);
        perValue.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        per.addView(perValue, UI.lp(this, -1, -2, 0, 3, 0, 0));
        metrics.addView(per, UI.lpw(1f));
        card.addView(metrics);
        card.setOnClickListener(v -> showDetail(p));
        return card;
    }

    private void showDetail(final Pkg p) {
        final Op op = Cfg.opById(this, p.op);
        String operator = op == null ? p.op : op.name;
        StringBuilder sb = new StringBuilder();
        sb.append(operator).append("\n");
        sb.append(p.label()).append("\n");
        if (p.currencyKnown && !p.ambig) {
            sb.append("قیمت پایه: ").append(Cfg.priceLabel(this, p.toman)) .append("\n");
            sb.append("با ارزش افزوده: ").append(Cfg.priceLabel(this, Cfg.finalPrice(this, p))).append("\n");
        } else {
            // Never display a candidate number as the package price when the parser could not
            // verify which amount belongs to this offer.
            sb.append("قیمت این بسته به‌صورت قطعی شناسایی نشد.\n");
            sb.append("تا بررسی دوباره، در رتبه‌بندی و مقایسه استفاده نمی‌شود.");
        }
        if (p.restricted && !p.ambig) {
            sb.append("\nمحدودیت: ").append(p.why);
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("جزئیات بسته")
                .setMessage(sb.toString())
                .setPositiveButton("بستن", null)
                .create();
        dialog.show();
    }

    // ---------------------------------------------------------------- settings

    private void settings(LinearLayout col) {
        LinearLayout heading = UI.hbox(this);
        LinearLayout title = UI.vbox(this);
        title.addView(UI.tv(this, "تنظیمات", 22, UI.TEXT, true));
        title.addView(UI.tv(this, "کنترل اسکن، مقایسه و خروجی اطلاعات", 11, UI.MUTED, false), UI.lp(this, -1, -2, 0, 3, 0, 0));
        heading.addView(title, UI.lpw(1f));
        heading.addView(UI.chip(this, "ذخیره‌ی خودکار", UI.tint(UI.ACCENT), UI.PRIMARY));
        col.addView(heading);

        LinearLayout display = section(col, "ظاهر و نمایش");
        display.addView(UI.tv(this, "حالت رنگی برنامه", 12, UI.TEXT, true));
        int activeTheme = UI.mono ? 1 : UI.light ? 2 : 0;
        chips(display, new String[]{"سایبری", "تک‌رنگ", "روشن"},
                activeTheme, i -> {
                    String selected = i == 1 ? "mono" : i == 2 ? "light" : "dark";
                    Cfg.sp(this).edit().putString("appearance", selected).apply();
                    UI.applyTheme(this);
                    applySystemBars();
                    if (appBackdrop != null) appBackdrop.refreshTheme();
                    render();
                });
        display.addView(UI.tv(this,
                "سایبری: سرمه‌ای عمیق با شبکه‌ی مدارهای زنده با پالس‌های آرام و تأکید فیروزه‌ای؛ تک‌رنگ: مینیمال؛ روشن: زمینه‌ی روشن و کم‌کنتراست.",
                10, UI.MUTED, false), UI.lp(this, -1, -2, 0, 0, 0, 12));
        display.addView(UI.tv(this, "واحد نمایش قیمت‌ها", 12, UI.TEXT, true));
        chips(display, new String[]{"تومان", "ریال"}, "IRR".equals(Cfg.displayCurrency(this)) ? 1 : 0, i -> {
            Cfg.setDisplayCurrency(this, i == 1 ? "IRR" : "IRT");
            if (ScanService.inst != null) ScanService.inst.hideComparisonPopup();
            render();
        });
        display.addView(UI.tv(this, "قیمت‌ها در حافظه بر اساس تومان نگهداری می‌شوند؛ تغییر این گزینه فقط واحد نمایش را عوض می‌کند.", 10, UI.MUTED, false), UI.lp(this, -1, -2, 0, 2, 0, 0));

        LinearLayout c = section(col, "اسکن اپراتورها");
        c.addView(UI.tv(this, "اسکن فقط برای اپراتورهای روشن انجام می‌شود.", 10, UI.MUTED, false));
        List<Op> ops = Cfg.ops(this);
        for (final Op o : ops) {
            LinearLayout r = UI.hbox(this);
            LinearLayout t = UI.vbox(this);
            r.addView(new OperatorLogo(this, o.id), UI.lp(this, 42, 34, 0, 0, 10, 0));
            t.addView(UI.tv(this, o.name, 13, UI.TEXT, true));
            TextView pk = UI.tv(this, TextUtils.join("، ", o.pkgs) + " • " + (o.cur.equals("IRR") ? "ریال" : o.cur.equals("IRT") ? "تومان" : "واحد خودکار"), 9, UI.MUTED, false);
            // متن ترکیبی فارسی و نام پکیج باید پاراگراف RTL بماند تا ترتیب واژه‌ها به‌هم نریزد.
            pk.setTextDirection(View.TEXT_DIRECTION_RTL);
            t.addView(pk, UI.lp(this, -1, -2, 0, 2, 0, 0));
            r.addView(t, UI.lpw(1f));
            r.addView(UI.btn(this, "پیکربندی", UI.CARD2, UI.PRIMARY, v -> editOp(o)), UI.lp(this, -2, -2, 0, 0, 6, 0));
            Switch sw = new Switch(this);
            styleSwitch(sw);
            sw.setChecked(o.enabled);
            sw.setOnCheckedChangeListener((bv, enabled) -> { o.enabled = enabled; Cfg.saveOps(this, ops); });
            r.addView(sw);
            c.addView(r, UI.lp(this, -1, -2, 0, 8, 0, 8));
            if (!o.builtin) c.addView(UI.btn(this, "حذف اپراتور سفارشی: " + o.name, UI.tint(UI.DANGER), UI.DANGER, v -> {
                ops.remove(o); Cfg.saveOps(this, ops); Store.clearOp(this, o.id); render();
            }), UI.lp(this, -1, -2, 0, 0, 0, 5));
        }
        int enabledCount = 0;
        for (Op o : ops) if (o.cmp) enabledCount++;
        stepRow(c, "حداقل تعداد اپراتور برای مقایسه", "برای نمایش کادر مقایسه‌ی چنداپراتوری", Cfg.minOps(this), 1, Math.max(1, enabledCount), 1, "", v -> {
            Cfg.setInt(this, "minOps", v); render();
        });

        LinearLayout custom = section(col, "افزودن اپراتور سفارشی");
        custom.addView(input("نام اپراتور", newName, s -> newName = s), UI.lp(this, -1, -2, 0, 3, 0, 6));
        EditText pkgInput = input("نام بسته‌ی برنامه؛ مانند ir.example.app", newPkg, s -> newPkg = s);
        pkgInput.setTextDirection(View.TEXT_DIRECTION_LTR);
        custom.addView(pkgInput, UI.lp(this, -1, -2, 0, 0, 0, 6));
        chips(custom, new String[]{"واحد خودکار", "ریال", "تومان"}, newCur, i -> { newCur = i; render(); });
        custom.addView(UI.btn(this, "افزودن اپراتور", UI.PRIMARY, 0xFF061326, v -> addOp()), UI.lp(this, -1, -2, 0, 8, 0, 0));

        LinearLayout comparison = section(col, "فیلترهای مقایسه");
        comparison.addView(UI.tv(this, "معیار رتبه‌بندی", 12, UI.TEXT, true));
        chips(comparison, new String[]{"ارزان‌ترین هر گیگ", "بیشترین اعتبار", "هزینه مصرف من"}, Cfg.metric(this), i -> { Cfg.setInt(this, "metric", i); render(); });
        numberRow(comparison, "حداقل حجم بسته (مگابایت؛ صفر = همه)", Fmt.num(Math.round(Cfg.minGb(this) * 1024)),
                s -> Cfg.setFloat(this, "minGb", Math.max(0f, Math.min(5120000f, (float) (Fmt.parse(s, 0) / 1024.0)))));
        numberRow(comparison, "حداکثر اعتبار (روز؛ صفر = بدون محدودیت)", Fmt.dec(Cfg.maxDays(this)),
                s -> Cfg.setInt(this, "maxDays", Cfg.clamp((int) Fmt.parse(s, 0), 0, 36500)));
        numberRow(comparison, "مصرف ماهانه‌ی من (گیگ)", Fmt.dec(Cfg.usage(this)),
                s -> Cfg.setFloat(this, "usage", Cfg.finiteClamp((float) Fmt.parse(s, 30), 0.1f, 5000f, 30f)));
        numberRow(comparison, "ارزش افزوده (درصد)", Fmt.num(Cfg.vat(this)), s -> Cfg.setInt(this, "vat", Math.max(0, Math.min(100, (int) Fmt.parse(s, Cfg.vat(this))))));
        swRow(comparison, "نمایش بسته‌های ویژه", "شبانه، هدیه، اپلیکیشن و محدود", Cfg.restricted(this), (bv, on) -> Cfg.setBool(this, "restricted", on));
        swRow(comparison, "نمایش بسته‌های بین‌الملل و رومینگ", "جدا از مقایسه‌ی بسته‌های داخلی", Cfg.intl(this), (bv, on) -> Cfg.setBool(this, "intl", on));

        LinearLayout popup = section(col, "پنجره‌ی شناور و اعلان‌ها");
        swRow(popup, "نمایش خودکار پنجره‌ی مقایسه", "پس از شناسایی بسته‌ها نمایش داده شود", Cfg.popup(this), (bv, on) -> Cfg.setBool(this, "popup", on));
        popup.addView(UI.tv(this, "محتوای پنجره", 12, UI.TEXT, true), UI.lp(this, -1, -2, 0, 8, 0, 0));
        chips(popup, new String[]{"هر دو", "برترین کل", "همین صفحه"}, Cfg.pmode(this), i -> { Cfg.setInt(this, "pmode", i); render(); });
        stepRow(popup, "تعداد ردیف‌های پنجره", null, Cfg.rows(this), 3, 8, 1, "", v -> { Cfg.setInt(this, "rows", v); render(); });
        int[] hv = {0, 8, 15, 30}; int hsel = 0;
        for (int i=0;i<hv.length;i++) if (hv[i] == Cfg.hideSec(this)) hsel = i;
        chips(popup, new String[]{"بدون بستن خودکار", "۸ ثانیه", "۱۵ ثانیه", "۳۰ ثانیه"}, hsel, i -> { Cfg.setInt(this, "hide", hv[i]); render(); });
        chips(popup, new String[]{"بالای صفحه", "پایین صفحه"}, Cfg.bottom(this) ? 1 : 0, i -> { Cfg.setBool(this, "bottom", i == 1); render(); });
        popup.addView(UI.btn(this, "آزمایش پنجره‌ی شناور", UI.CARD2, UI.PRIMARY, v -> {
            ScanService service = ScanService.inst;
            if (service == null) toast("ابتدا دسترسی اسکن را فعال کنید"); else service.showTest();
        }), UI.lp(this, -1, -2, 0, 8, 0, 0));

        LinearLayout data = section(col, "اعتبار داده و خروجی");
        data.addView(UI.tv(this, "داده‌های قدیمی‌تر از این مدت از مقایسه کنار گذاشته می‌شوند.", 10, UI.MUTED, false));
        int[] fresh = {3, 7, 14, 30}; int sel = 0;
        for (int i=0;i<fresh.length;i++) if (fresh[i] == Cfg.fresh(this)) sel = i;
        chips(data, new String[]{"۳ روز", "۷ روز", "۱۴ روز", "۳۰ روز"}, sel, i -> { Cfg.setInt(this, "fresh", fresh[i]); render(); });
        List<Pkg> pendingReview = Store.reviewQueue(this);
        data.addView(UI.tv(this, "بسته‌های مبهم تا زمان تأیید دستی وارد رتبه‌بندی قطعی نمی‌شوند.", 10, UI.MUTED, false), UI.lp(this, -1, -2, 0, 8, 0, 0));
        data.addView(UI.btn(this, "بازبینی بسته‌های نیازمند تأیید  •  " + Fmt.fa(String.valueOf(pendingReview.size())),
                UI.CARD2, UI.PRIMARY, v -> showReviewQueue()), UI.lp(this, -1, -2, 0, 7, 0, 0));
        if (HistoryLog.hasError(this)) {
            LinearLayout historyWarning = UI.card(this);
            historyWarning.addView(UI.tv(this, HistoryLog.errorSummary(this), 10, UI.DANGER, false));
            historyWarning.addView(UI.btn(this, "پاک‌کردن هشدار پس از بررسی", UI.CARD2, UI.TEXT, v -> {
                HistoryLog.clearError(this); render();
            }), UI.lp(this, -1, -2, 0, 6, 0, 0));
            data.addView(historyWarning, UI.lp(this, -1, -2, 0, 8, 0, 0));
        }
        data.addView(UI.btn(this, "خروجی بسته‌ها (CSV / Excel)", UI.CARD2, UI.PRIMARY, v -> exportCsv()), UI.lp(this, -1, -2, 0, 10, 0, 0));
        data.addView(UI.btn(this, "خروجی گزارش خطا (TXT)", UI.CARD2, UI.TEXT, v -> exportDiag()), UI.lp(this, -1, -2, 0, 7, 0, 0));
        LinearLayout history = UI.card(this);
        history.addView(UI.tv(this, "تاریخچه‌ی تغییرات بسته‌ها", 13, UI.TEXT, true));
        List<HistoryLog.Entry> changes = HistoryLog.recent(this, 6);
        if (changes.isEmpty()) history.addView(UI.tv(this, "هنوز تغییری بین دو اسکن ثبت نشده است.", 10, UI.MUTED, false), UI.lp(this, -1, -2, 0, 6, 0, 0));
        for (HistoryLog.Entry ch : changes) {
            boolean reviewApproved = "review_approved".equals(ch.field);
            boolean reviewRejected = "review_rejected".equals(ch.field);
            if (reviewApproved || reviewRejected) {
                String label = reviewApproved ? "تأیید دستی" : "رد دستی";
                String detail = ch.note == null || ch.note.isEmpty() ? "" : "\n" + ch.note;
                history.addView(UI.tv(this, label + " • " + ch.title + detail + "  ·  " + Fmt.ago(ch.ts),
                        10, reviewApproved ? UI.PRIMARY : UI.DANGER, false), UI.lp(this, -1, -2, 0, 7, 0, 0));
                continue;
            }
            boolean price = "price".equals(ch.field);
            String field = price ? "قیمت" : ("gb".equals(ch.field) ? "حجم" : "مدت");
            String before = price ? Cfg.priceLabel(this, (long)ch.before) : Fmt.dec(ch.before) + ("gb".equals(ch.field) ? " گیگ" : " روز");
            String after = price ? Cfg.priceLabel(this, (long)ch.after) : Fmt.dec(ch.after) + ("gb".equals(ch.field) ? " گیگ" : " روز");
            history.addView(UI.tv(this, field + " • " + ch.title + "\n" + before + " ← " + after + "  ·  " + Fmt.ago(ch.ts), 10, price ? UI.ACCENT : UI.MUTED, false),
                    UI.lp(this, -1, -2, 0, 7, 0, 0));
        }
        data.addView(history, UI.lp(this, -1, -2, 0, 10, 0, 0));
        data.addView(UI.btn(this, "پاک‌کردن بسته‌های ذخیره‌شده", UI.tint(UI.DANGER), UI.DANGER, v -> new AlertDialog.Builder(this)
                .setTitle("پاک‌کردن اطلاعات")
                .setMessage("همه‌ی بسته‌های ذخیره‌شده و تاریخچه‌ی تغییرات حذف شوند؟")
                .setNegativeButton("انصراف", null)
                .setPositiveButton("پاک شود", (d, w) -> { Store.clearAll(this); HistoryLog.clear(this); HistoryLog.clearError(this); toast("اطلاعات پاک شد"); render(); })
                .show()), UI.lp(this, -1, -2, 0, 8, 0, 0));

        TextView privacy = UI.tv(this, "حریم خصوصی: اسکن و پردازش به‌صورت محلی انجام می‌شود؛ اطلاعات صفحه به سرور ارسال نمی‌شود.", 10, UI.MUTED, false);
        privacy.setGravity(Gravity.CENTER);
        col.addView(privacy, UI.lp(this, -1, -2, 0, 16, 0, 0));
    }

    private void showReviewQueue() {
        List<Pkg> queue = Store.reviewQueue(this);
        if (queue.isEmpty()) {
            toast("بسته‌ای برای بازبینی باقی نمانده است");
            return;
        }
        String[] labels = new String[queue.size()];
        for (int i = 0; i < queue.size(); i++) {
            Pkg p = queue.get(i);
            Op op = Cfg.opById(this, p.op);
            String owner = op == null ? p.op : op.name;
            labels[i] = owner + " • " + (p.title == null || p.title.isEmpty() ? "بسته‌ی بدون عنوان" : p.title)
                    + "\n" + p.label() + " • " + (p.currencyKnown ? Cfg.priceLabel(this, p.toman) : "واحد/قیمت نیازمند بررسی");
        }
        new AlertDialog.Builder(this).setTitle("صف بازبینی بسته‌های مبهم")
                .setItems(labels, (dialog, which) -> editReview(queue.get(which)))
                .setNegativeButton("بستن", null).show();
    }

    private void editReview(Pkg source) {
        final EditText volume = input("حجم صحیح بر حسب گیگ", Fmt.dec(source.gb), value -> {});
        final EditText days = input("مدت صحیح بر حسب روز؛ صفر اگر نامشخص است", Fmt.dec(source.days), value -> {});
        final EditText price = input("قیمت صحیح بر حسب تومان", String.valueOf(source.toman), value -> {});
        volume.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        days.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        price.setInputType(InputType.TYPE_CLASS_NUMBER);
        volume.setTextDirection(View.TEXT_DIRECTION_LTR);
        days.setTextDirection(View.TEXT_DIRECTION_LTR);
        price.setTextDirection(View.TEXT_DIRECTION_LTR);
        volume.setGravity(Gravity.CENTER); days.setGravity(Gravity.CENTER); price.setGravity(Gravity.CENTER);
        volume.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        days.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        price.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        final EditText note = input("یادداشت بازبین (اختیاری)", "", value -> {});
        LinearLayout fields = UI.vbox(this);
        fields.setPadding(dp(20), dp(8), dp(20), dp(4));
        String reviewReason = source.note == null || source.note.trim().isEmpty()
                ? (source.why == null || source.why.trim().isEmpty() ? "شواهد کافی برای تأیید خودکار وجود ندارد." : source.why)
                : source.note;
        fields.addView(UI.tv(this, "علت نیاز به بازبینی: " + reviewReason, 10, UI.DANGER, false),
                UI.lp(this, -1, -2, 0, 0, 0, 8));
        fields.addView(UI.tv(this, "مقادیر استخراج‌شده تغییر نمی‌کنند؛ مقادیر تأییدشده جدا ذخیره می‌شوند و فقط پس از تأیید وارد مقایسه خواهند شد.",
                10, UI.MUTED, false), UI.lp(this, -1, -2, 0, 0, 0, 8));
        fields.addView(volume, UI.lp(this, -1, -2, 0, 0, 0, 6));
        fields.addView(days, UI.lp(this, -1, -2, 0, 0, 0, 6));
        fields.addView(price, UI.lp(this, -1, -2, 0, 0, 0, 6));
        fields.addView(note, UI.lp(this, -1, -2, 0, 0, 0, 0));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("بازبینی: " + (source.title == null ? "بسته" : source.title))
                .setView(fields)
                .setPositiveButton("تأیید و ورود به مقایسه", (d, which) -> {})
                .setNeutralButton("رد این بسته", (d, which) -> {})
                .setNegativeButton("لغو", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                double gb = Fmt.parse(volume.getText().toString(), -1);
                double validity = Fmt.parse(days.getText().toString(), -1);
                double priceValue = Fmt.parse(price.getText().toString(), -1);
                long toman = priceValue < 0 || priceValue > 2000000000L ? -1 : Math.round(priceValue);
                boolean saved = Store.saveReview(this, source.key, true, gb, validity, toman, note.getText().toString());
                if (!saved) { toast("مقادیر معتبر وارد کنید: حجم حداقل ۰٫۰۵ گیگ و قیمت به تومان"); return; }
                dialog.dismiss(); toast("بازبینی ذخیره شد؛ مقادیر تأییدشده وارد مقایسه شدند"); render();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                boolean saved = Store.saveReview(this, source.key, false, 0, 0, 0, note.getText().toString());
                if (!saved) { toast("ثبت رد بسته انجام نشد؛ دوباره تلاش کنید"); return; }
                dialog.dismiss(); toast("بسته از صف بازبینی و رتبه‌بندی حذف شد"); render();
            });
        });
        dialog.show();
    }

    private View sectionTitle(String title, String subtitle) {
        LinearLayout box = UI.hbox(this);
        View accent = new View(this);
        accent.setBackground(UI.rr(this, UI.PRIMARY, 3));
        box.addView(accent, UI.lp(this, 4, 27, 0, 0, 10, 0));
        LinearLayout labels = UI.vbox(this);
        labels.addView(UI.tv(this, title, 15, UI.TEXT, true));
        if (subtitle != null && !subtitle.isEmpty()) {
            labels.addView(UI.tv(this, subtitle, 10, UI.MUTED, false), UI.lp(this, -1, -2, 0, 2, 0, 0));
        }
        box.addView(labels, UI.lpw(1f));
        return box;
    }

    private LinearLayout section(LinearLayout col, String title) {
        LinearLayout heading = UI.hbox(this);
        View accent = new View(this);
        accent.setBackground(UI.rr(this, UI.PRIMARY, 3));
        heading.addView(accent, UI.lp(this, 4, 20, 0, 0, 9, 0));
        TextView label = UI.tv(this, title, 13, UI.TEXT, true);
        heading.addView(label, UI.lpw(1f));
        col.addView(heading, UI.lp(this, -1, -2, 0, 18, 0, 8));
        LinearLayout c = UI.card(this);
        col.addView(c);
        return c;
    }

    private void styleSwitch(Switch sw) {
        int[][] states = new int[][]{{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(states, new int[]{UI.PRIMARY, UI.MUTED}));
        sw.setTrackTintList(new ColorStateList(states, new int[]{
                (UI.PRIMARY & 0x00FFFFFF) | 0x88000000, (UI.STROKE & 0x00FFFFFF) | 0x66000000}));
    }

    private void swRow(LinearLayout c, String title, String sub, boolean on, CompoundButton.OnCheckedChangeListener l) {
        LinearLayout r = UI.hbox(this);
        LinearLayout t = UI.vbox(this);
        t.addView(UI.tv(this, title, 14, UI.TEXT, false));
        if (sub != null) t.addView(UI.tv(this, sub, 11, UI.MUTED, false));
        r.addView(t, UI.lpw(1f));
        Switch s = new Switch(this);
        styleSwitch(s);
        s.setChecked(on);
        s.setOnCheckedChangeListener(l);
        r.addView(s);
        c.addView(r, UI.lp(this, -1, -2, 0, 8, 0, 4));
    }

    private void chips(LinearLayout c, String[] labels, int sel, final IntCb cb) {
        LinearLayout r = UI.hbox(this);
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            boolean on = i == sel;
            TextView t = UI.tv(this, labels[i], 12, on ? UI.contrastText(UI.PRIMARY) : UI.TEXT, on);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(6), dp(9), dp(6), dp(9));
            t.setBackground(on
                    ? UI.grad(this, UI.PRIMARY, UI.BLUE, 13)
                    : UI.rr(this, UI.CARD2, 13, UI.STROKE));
            t.setOnClickListener(v -> cb.on(idx));
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
            if (i < labels.length - 1) r.addView(new View(this), new LinearLayout.LayoutParams(dp(6), 1));
        }
        c.addView(r, UI.lp(this, -1, -2, 0, 6, 0, 6));
    }

    private void stepRow(LinearLayout c, String title, String sub, final int val, final int min, final int max, final int step, String suffix, final IntCb cb) {
        LinearLayout r = UI.hbox(this);
        LinearLayout t = UI.vbox(this);
        t.addView(UI.tv(this, title, 13, UI.TEXT, false));
        if (sub != null) t.addView(UI.tv(this, sub, 10, UI.MUTED, false));
        r.addView(t, UI.lpw(1f));
        r.addView(UI.btn(this, "−", UI.CARD2, UI.TEXT, v -> cb.on(Math.max(min, val - step))));
        TextView n = UI.tv(this, Fmt.fa(String.valueOf(val)) + suffix, 14, UI.PRIMARY, true);
        n.setGravity(Gravity.CENTER);
        n.setMinWidth(dp(56));
        r.addView(n);
        r.addView(UI.btn(this, "+", UI.CARD2, UI.TEXT, v -> cb.on(Math.min(max, val + step))));
        c.addView(r, UI.lp(this, -1, -2, 0, 6, 0, 6));
    }

    private EditText input(String hint, String text, final StrCb cb) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(UI.MUTED);
        e.setTextColor(UI.TEXT);
        e.setTextSize(13);
        e.setSingleLine();
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setText(text);
        e.setBackground(UI.rr(this, UI.CARD2, 12, UI.STROKE));
        e.setPadding(dp(14), dp(11), dp(14), dp(11));
        e.setMinHeight(dp(46));
        e.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        e.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { cb.on(s.toString()); }
        });
        return e;
    }

    private void numberRow(LinearLayout c, String label, String cur, StrCb cb) {
        c.addView(UI.tv(this, label, 13, UI.TEXT, false), UI.lp(this, -1, -2, 0, 10, 0, 4));
        EditText value = input("", cur, cb);
        // Numeric settings use centered LTR presentation so digits/decimal separators stay predictable.
        value.setTextDirection(View.TEXT_DIRECTION_LTR);
        value.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        value.setGravity(Gravity.CENTER);
        value.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        c.addView(value);
    }

    private void addOp() {
        String name = newName == null ? "" : newName.trim();
        String pkg = newPkg == null ? "" : newPkg.trim();
        if (name.isEmpty() || pkg.isEmpty() || name.length() > 40) {
            toast("نام اپراتور و نام پکیج معتبر (حداکثر ۴۰ نویسه برای نام) وارد کنید");
            return;
        }
        if (!Cfg.validPackageName(pkg)) {
            toast("نام پکیج معتبر نیست؛ نمونه: ir.example.app");
            return;
        }
        List<Op> l = Cfg.ops(this);
        for (Op existing : l) if (existing.pkgs.contains(pkg)) {
            toast("این پکیج قبلاً به «" + existing.name + "» اختصاص داده شده است");
            return;
        }
        int[] pal = {0xFF8B5CF6, 0xFFEC4899, 0xFF22C55E, 0xFFF97316, 0xFF06B6D4};
        String[] cur = {"AUTO", "IRR", "IRT"};
        l.add(new Op("c" + System.currentTimeMillis(), name, pal[l.size() % pal.length], cur[Math.max(0, Math.min(2, newCur))], false, pkg));
        Cfg.saveOps(this, l);
        newName = "";
        newPkg = "";
        render();
    }

    private void editOp(final Op o) {
        final EditText e = input("com.example.app, com.example.beta", TextUtils.join(", ", o.pkgs), s -> {});
        e.setTextDirection(View.TEXT_DIRECTION_LTR);
        FrameLayout f = new FrameLayout(this);
        f.setPadding(dp(20), dp(8), dp(20), 0);
        f.addView(e);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("پکیج‌های " + o.name)
                .setMessage("نام پکیج برنامه‌ها را با ویرگول جدا کنید.")
                .setView(f)
                .setPositiveButton("ذخیره", (dlg, w) -> {})
                .setNegativeButton("لغو", null)
                .create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            List<String> proposed = new ArrayList<>();
            for (String raw : e.getText().toString().split("[,\\s]+")) {
                String value = raw.trim();
                if (value.isEmpty()) continue;
                if (!Cfg.validPackageName(value)) {
                    toast("نام پکیج نامعتبر است: " + value);
                    return;
                }
                if (proposed.contains(value)) {
                    toast("پکیج تکراری وارد شده است: " + value);
                    return;
                }
                for (Op other : Cfg.ops(this)) {
                    if (!other.id.equals(o.id) && other.pkgs.contains(value)) {
                        toast("این پکیج به «" + other.name + "» اختصاص دارد: " + value);
                        return;
                    }
                }
                proposed.add(value);
            }
            if (proposed.isEmpty()) {
                toast("حداقل یک نام پکیج معتبر وارد کنید");
                return;
            }
            List<Op> updated = Cfg.ops(this);
            boolean found = false;
            for (Op item : updated) {
                if (item.id.equals(o.id)) {
                    item.pkgs.clear(); item.pkgs.addAll(proposed); found = true; break;
                }
            }
            if (!found) { toast("اپراتور تغییر کرده است؛ صفحه را تازه کنید"); return; }
            Cfg.saveOps(this, updated);
            dialog.dismiss();
            render();
        }));
        dialog.show();
    }

    // ---------------------------------------------------------------- export

    private static String csv(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private void exportCsv() {
        final List<Pkg> all = Store.all(this);
        if (all.isEmpty()) {
            toast("داده‌ای برای خروجی نیست");
            return;
        }
        String ts = new SimpleDateFormat("MMdd_HHmmss", Locale.US).format(new Date());
        try {
            String where = save("bastehyab_" + ts + ".csv", "text/csv", o -> {
                StringBuilder sb = new StringBuilder("\uFEFFoperator,title,gb,days,price_toman,price_with_vat_toman,vat_percent,currency_known,ambiguous,per_gb_toman,restricted,reason,review_note,captured\n");
                // one row per real package (operator + volume + duration + price), newest observation kept
                Map<String, Pkg> uniqueRows = new LinkedHashMap<>();
                for (Pkg p : all) {
                    if (p.op == null || p.op.isEmpty()) continue;
                    Pkg prev = uniqueRows.get(p.sig());
                    if (prev == null || p.ts > prev.ts) uniqueRows.put(p.sig(), p);
                }
                for (Pkg p : uniqueRows.values()) {
                    Op op = Cfg.opById(this, p.op);
                    sb.append(csv(op == null ? p.op : op.name)).append(',').append(csv(p.title)).append(',')
                            .append(p.gb).append(',').append(p.days).append(',')
                             .append(p.currencyKnown ? String.valueOf(p.toman) : "").append(',')
                            .append(p.currencyKnown ? String.valueOf(Cfg.finalPrice(this, p)) : "").append(',')
                            .append(Cfg.vat(this)).append(',')
                            .append(p.currencyKnown).append(',')
                            .append(p.ambig).append(',')
                            .append(p.currencyKnown && !p.ambig ? String.valueOf(Math.round(p.perGb())) : "").append(',')
                            .append(p.restricted).append(',').append(csv(p.why)).append(',')
                            .append(csv(p.note)).append(',')
                            .append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(p.ts))).append('\n');
                }
                o.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            });
            toast("ذخیره شد: " + where);
        } catch (Exception e) {
            toast(e.toString());
        }
    }

    private void exportDiag() {
        // Flush the in-memory snapshot first; otherwise a fast tap just after a scan may
        // incorrectly report that no diagnostic exists because the debounce has not fired.
        ScanService.flushDiagnostics();
        final File f = new File(getFilesDir(), "diag.txt");
        if (!f.exists()) {
            toast("هنوز گزارشی ساخته نشده؛ برنامه‌ی اپراتور را باز کنید و صفحه‌ی بسته‌ها را ببینید.");
            return;
        }
        String ts = new SimpleDateFormat("MMdd_HHmmss", Locale.US).format(new Date());
        try {
            String where = save("diag_" + ts + ".txt", "text/plain", o -> {
                try (InputStream i = new FileInputStream(f)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = i.read(buf)) > 0) o.write(buf, 0, n);
                }
            });
            toast("ذخیره شد: " + where);
        } catch (Exception e) {
            toast(e.toString());
        }
    }

    /** Returns a human-readable location. API 29+ uses MediaStore Downloads; older Android uses the app's own Downloads folder. */
    private String save(String name, String mime, W w) throws Exception {
        if (Build.VERSION.SDK_INT < 29) {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) throw new java.io.IOException("حافظه‌ی خارجی در دسترس نیست");
            if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("ساخت پوشه‌ی خروجی ناموفق بود");
            File out = new File(dir, name);
            try (OutputStream o = new FileOutputStream(out)) {
                w.w(o);
            } catch (Exception e) {
                out.delete();
                throw e;
            }
            return out.getAbsolutePath();
        }
        ContentValues v = new ContentValues();
        v.put(MediaStore.Downloads.DISPLAY_NAME, name);
        v.put(MediaStore.Downloads.MIME_TYPE, mime);
        Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        if (u == null) throw new java.io.IOException("ساخت فایل در Downloads ناموفق بود");
        try (OutputStream o = getContentResolver().openOutputStream(u)) {
            if (o == null) throw new java.io.IOException("دسترسی نوشتن به فایل خروجی برقرار نشد");
            w.w(o);
        } catch (Exception e) {
            getContentResolver().delete(u, null, null);
            throw e;
        }
        return "Downloads";
    }
}
