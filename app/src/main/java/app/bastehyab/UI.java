package app.bastehyab;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/**
 * "Neumorphic Security" design system.
 *
 * One token set drives every screen and the accessibility overlay. Soft dual shadows,
 * raised rounded surfaces and a cyan/violet-on-graphite palette follow the two supplied
 * neumorphic references while keeping the mysterious security-console atmosphere.
 * All text tokens satisfy WCAG AA (>= 4.5:1) in dark, light and monochrome themes.
 */
final class UI {
    private UI() { }

    static boolean light, mono;
    static int BG, CARD, CARD2, STROKE, PRIMARY, ACCENT, TEXT, MUTED, DANGER, BLUE, SUCCESS;
    static int SHADOW, HIGHLIGHT;
    static Typeface BODY, MEDIUM, BOLD, SYMBOL;
    private static final String SYMBOL_CHARS = "⚡✓✕▴▾⠿●○↗−©—▪• ";

    static void applyTheme(Context c) {
        String appearance = Cfg.sp(c).getString("appearance", "auto");
        boolean night = true;
        if ("auto".equals(appearance)) {
            Configuration configuration = c.getResources().getConfiguration();
            night = (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
        } else if ("light".equals(appearance)) {
            night = false;
        }
        light = "light".equals(appearance) || ("auto".equals(appearance) && !night);
        mono = "mono".equals(appearance);
        if (light) {
            BG = 0xFFE9EEF7; CARD = 0xFFE9EEF7; CARD2 = 0xFFDDE5F1; STROKE = 0xFFC7D2E4;
            PRIMARY = 0xFF0B6E75; ACCENT = 0xFF4734A8; TEXT = 0xFF16233A;
            MUTED = 0xFF4A5B70; DANGER = 0xFF9A2740; BLUE = 0xFF1D5FA8;
            SUCCESS = 0xFF0B6B4D; SHADOW = 0xFFB7C4D8; HIGHLIGHT = 0xFFFFFFFF;
        } else if (mono) {
            BG = 0xFF121821; CARD = 0xFF1B2330; CARD2 = 0xFF27303E; STROKE = 0xFF3C4958;
            PRIMARY = 0xFFE3F2F1; ACCENT = 0xFFC9D4DF; TEXT = 0xFFF5F8FA;
            MUTED = 0xFFBECBD6; DANGER = 0xFFF0B9C2; BLUE = 0xFFD3E2F0;
            SUCCESS = 0xFFB8E6D2; SHADOW = 0xFF080B10; HIGHLIGHT = 0xFF3B4757;
        } else {
            BG = 0xFF101825; CARD = 0xFF182231; CARD2 = 0xFF1F2C3E; STROKE = 0xFF33465C;
            PRIMARY = 0xFF4DE0C8; ACCENT = 0xFF9C8CFF; TEXT = 0xFFF2F7FA;
            MUTED = 0xFFA9BCCB; DANGER = 0xFFFF8CA0; BLUE = 0xFF6BB7FF;
            SUCCESS = 0xFF79E6AE; SHADOW = 0xFF070C14; HIGHLIGHT = 0xFF26354A;
        }
        if (BODY == null || MEDIUM == null || BOLD == null || SYMBOL == null) {
            try {
                BODY = Typeface.createFromAsset(c.getAssets(), "fonts/NotoSansArabic-Regular.ttf");
                MEDIUM = Typeface.createFromAsset(c.getAssets(), "fonts/NotoSansArabic-Medium.ttf");
                BOLD = Typeface.createFromAsset(c.getAssets(), "fonts/NotoSansArabic-Bold.ttf");
                SYMBOL = Typeface.createFromAsset(c.getAssets(), "fonts/DejaVuSans-Symbols.ttf");
            } catch (RuntimeException fontError) {
                BODY = Typeface.create("sans-serif", Typeface.NORMAL);
                MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
                BOLD = Typeface.create("sans-serif", Typeface.BOLD);
                SYMBOL = Typeface.create("monospace", Typeface.NORMAL);
            }
        }
    }

    /** Semantic replacement for the old ~40-entry legacy hex switch. */
    static int themeColor(int color) {
        int alpha = color >>> 24;
        int rgb = color & 0x00FFFFFF;
        if (alpha == 0 || isToken(rgb)) return color;
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        double luminance = luminance(r, g, b);
        if (mono) {
            if (luminance < .035) return (alpha << 24) | (BG & 0x00FFFFFF);
            if (luminance < .10) return (alpha << 24) | (CARD & 0x00FFFFFF);
            if (luminance < .22) return (alpha << 24) | (CARD2 & 0x00FFFFFF);
            if (luminance < .55) return (alpha << 24) | (MUTED & 0x00FFFFFF);
            return (alpha << 24) | (TEXT & 0x00FFFFFF);
        }
        if (luminance < .06) return (alpha << 24) | (light ? CARD : BG & 0x00FFFFFF);
        if (luminance < .18) return (alpha << 24) | (CARD & 0x00FFFFFF);
        if (luminance < .36) return (alpha << 24) | (STROKE & 0x00FFFFFF);
        if (r > g + 30 && r > b + 30) return (alpha << 24) | (DANGER & 0x00FFFFFF);
        if (g > r + 20 && g > b + 10) return (alpha << 24) | (SUCCESS & 0x00FFFFFF);
        if (b > r + 20 && b > g + 10) return (alpha << 24) | (ACCENT & 0x00FFFFFF);
        if (g > r + 12 || b > r + 12) return (alpha << 24) | (PRIMARY & 0x00FFFFFF);
        if (luminance > .78) return (alpha << 24) | (TEXT & 0x00FFFFFF);
        return (alpha << 24) | (MUTED & 0x00FFFFFF);
    }

    private static boolean isToken(int rgb) {
        return rgb == (BG & 0x00FFFFFF) || rgb == (CARD & 0x00FFFFFF)
                || rgb == (CARD2 & 0x00FFFFFF) || rgb == (STROKE & 0x00FFFFFF)
                || rgb == (PRIMARY & 0x00FFFFFF) || rgb == (ACCENT & 0x00FFFFFF)
                || rgb == (TEXT & 0x00FFFFFF) || rgb == (MUTED & 0x00FFFFFF)
                || rgb == (DANGER & 0x00FFFFFF) || rgb == (BLUE & 0x00FFFFFF)
                || rgb == (SUCCESS & 0x00FFFFFF) || rgb == (SHADOW & 0x00FFFFFF)
                || rgb == (HIGHLIGHT & 0x00FFFFFF);
    }

    private static double luminance(int r, int g, int b) {
        double[] v = {channel(r), channel(g), channel(b)};
        return .2126 * v[0] + .7152 * v[1] + .0722 * v[2];
    }

    private static double channel(int value) {
        double v = value / 255.0;
        return v <= .04045 ? v / 12.92 : Math.pow((v + .055) / 1.055, 2.4);
    }

    static void applyTypefaceTree(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            CharSequence value = text.getText();
            if (isSymbolOnly(value)) text.setTypeface(SYMBOL);
            else if (text instanceof Button || text instanceof Switch) text.setTypeface(MEDIUM);
            else if (text.getTypeface() != null && text.getTypeface().isBold()) text.setTypeface(BOLD);
            else text.setTypeface(BODY);
            // Persian diacritics need their font padding; removing it clipped glyphs on API 24.
            text.setIncludeFontPadding(true);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyTypefaceTree(group.getChildAt(i));
        }
    }

    private static boolean isSymbolOnly(CharSequence value) {
        if (value == null || value.length() == 0) return false;
        for (int i = 0; i < value.length(); i++) {
            if (SYMBOL_CHARS.indexOf(value.charAt(i)) < 0) return false;
        }
        return true;
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static int dimen(Context c, int id) {
        return c.getResources().getDimensionPixelSize(id);
    }

    static int sz(Context c, int v) { return v > 0 ? dp(c, v) : v; }

    static int surfaceColor(int color) {
        if ((color >>> 24) == 0) return color;
        if ((color & 0x00FFFFFF) == 0x00FFFFFF) return light ? CARD : TEXT;
        return themeColor(color);
    }

    static GradientDrawable rr(Context c, int fill, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(surfaceColor(fill));
        g.setCornerRadius(dp(c, r));
        return g;
    }

    static GradientDrawable rr(Context c, int fill, float r, int stroke) {
        GradientDrawable g = rr(c, fill, r);
        g.setStroke(Math.max(1, dp(c, 1)), surfaceColor(stroke));
        return g;
    }

    static GradientDrawable grad(Context c, int a, int b, float r) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{surfaceColor(a), surfaceColor(b)});
        g.setCornerRadius(dp(c, r));
        return g;
    }

    /** Dual soft shadows: dark bottom-right, light top-left, raised surface on top. */
    static Drawable neumorphic(Context c, int fill, float radiusDp, boolean strong) {
        int r = dp(c, radiusDp);
        int offset = dp(c, strong ? 5 : 3);
        GradientDrawable shadow = new GradientDrawable();
        shadow.setColor(SHADOW);
        shadow.setCornerRadius(r);
        GradientDrawable highlight = new GradientDrawable();
        highlight.setColor(HIGHLIGHT);
        highlight.setCornerRadius(r);
        GradientDrawable surface = rr(c, fill, radiusDp, STROKE);
        LayerDrawable layers = new LayerDrawable(new Drawable[]{shadow, highlight, surface});
        layers.setLayerInset(0, offset, offset, 0, 0);
        layers.setLayerInset(1, 0, 0, offset, offset);
        layers.setLayerInset(2, dp(c, 1), dp(c, 1), offset / 2, offset / 2);
        return layers;
    }

    static Drawable appBackground(Context c) {
        if (light) return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFF5F8FD, 0xFFE9EEF7, 0xFFDDE5F1});
        if (mono) return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0B0F15, BG, 0xFF202833});
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF142033, BG, 0xFF0B1220});
    }

    static int tint(int color) { return (themeColor(color) & 0x00FFFFFF) | 0x22000000; }

    static int contrastText(int background) {
        double bg = luminance((background >> 16) & 255, (background >> 8) & 255, background & 255);
        double white = 1.0, dark = luminance(0x16, 0x23, 0x3A);
        double withWhite = (white + .05) / (bg + .05);
        double withDark = (bg + .05) / (dark + .05);
        return withDark >= withWhite ? 0xFF16233A : 0xFFFFFFFF;
    }

    static TextView tv(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(Math.max(12f, sp));
        t.setTextColor(themeColor(color));
        t.setTypeface(bold ? BOLD : BODY);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setGravity(Gravity.TOP | Gravity.START);
        t.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        t.setIncludeFontPadding(true);
        t.setLineSpacing(dp(c, 2f), 1.22f);
        t.setContentDescription(s);
        return t;
    }

    static ImageView iconView(Context c, int resId, String description) {
        ImageView image = new ImageView(c);
        image.setImageResource(resId);
        image.setColorFilter(PRIMARY);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int target = dimen(c, R.dimen.touch_target);
        image.setMinimumWidth(target);
        image.setMinimumHeight(target);
        image.setPadding(dp(c, 12), dp(c, 12), dp(c, 12), dp(c, 12));
        image.setBackground(neumorphic(c, CARD2, 16, false));
        image.setContentDescription(description);
        image.setClickable(true);
        return image;
    }

    static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return l;
    }

    static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout.LayoutParams lp(Context c, int w, int h, float s, float t, float e, float b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(sz(c, w), sz(c, h));
        p.setMargins(0, dp(c, t), 0, dp(c, b));
        p.setMarginStart(dp(c, s));
        p.setMarginEnd(dp(c, e));
        return p;
    }

    static LinearLayout.LayoutParams lp2(Context c, float start) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.setMarginStart(dp(c, start));
        return p;
    }

    static LinearLayout.LayoutParams lpw(float weight) {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
    }

    static LinearLayout card(Context c) {
        LinearLayout l = vbox(c);
        int p = dimen(c, R.dimen.gutter);
        l.setPadding(p, p, p, p);
        l.setBackground(neumorphic(c, CARD, 22, true));
        l.setClipToOutline(true);
        l.setElevation(dp(c, 4));
        return l;
    }

    static TextView chip(Context c, String s, int bg, int fg) {
        TextView t = tv(c, s, 12, fg, true);
        int target = dimen(c, R.dimen.touch_target);
        t.setMinHeight(target);
        t.setMinimumHeight(target);
        t.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        boolean selected = (themeColor(bg) & 0x00FFFFFF) == (PRIMARY & 0x00FFFFFF);
        if (selected) {
            t.setTextColor(contrastText(PRIMARY));
            t.setBackground(neumorphic(c, PRIMARY, 16, true));
        } else {
            t.setTextColor(contrastText(surfaceColor(bg)));
            t.setBackground(neumorphic(c, bg, 16, false));
        }
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setContentDescription(s);
        return t;
    }

    static Button btn(Context c, String s, int bg, int fg, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTypeface(MEDIUM);
        boolean primary = (themeColor(bg) & 0x00FFFFFF) == (PRIMARY & 0x00FFFFFF);
        GradientDrawable shape = primary ? grad(c, PRIMARY, BLUE, 16) : rr(c, bg, 16, STROKE);
        b.setTextColor(primary ? contrastText(PRIMARY) : themeColor(fg));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), shape,
                rr(c, 0xFFFFFFFF, 18)));
        b.setStateListAnimator(null);
        b.setElevation(dp(c, 3));
        int target = dimen(c, R.dimen.touch_target);
        b.setMinHeight(target);
        b.setMinimumHeight(target);
        b.setGravity(Gravity.CENTER);
        b.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        b.setTextDirection(View.TEXT_DIRECTION_RTL);
        b.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        b.setContentDescription(s);
        b.setOnClickListener(l);
        return b;
    }
}
