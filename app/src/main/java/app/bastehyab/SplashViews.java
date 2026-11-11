package app.bastehyab;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/**
 * Custom drawing for the opening screen and shared brand assets.
 *
 * The backdrop is a deterministic, static cyber-network scene: no ValueAnimator, no
 * postInvalidateOnAnimation(), no endless frames, no dependency on "reduce motion" settings.
 * This keeps the mysterious/security atmosphere while eliminating battery drain and
 * the motion-accessibility defect found in the audit.
 */
final class SplashViews {
    private SplashViews() { }

    static float dp(View v, float value) {
        return value * v.getResources().getDisplayMetrics().density;
    }

    /** Static cyber/network wallpaper, shared by the splash and the three main tabs. */
    static class AppBackdrop extends View {
        private static final int NODE_COUNT = 30;
        private static final int TRACE_COUNT = 36;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tracePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint packetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Shader backgroundShader, ambientShader;
        private float density;
        private Node[] nodes = new Node[0];
        private Trace[] traces = new Trace[0];

        private static final class Node {
            final float x, y, radius;
            final boolean hub;
            Node(float x, float y, float radius, boolean hub) {
                this.x = x; this.y = y; this.radius = radius; this.hub = hub;
            }
        }

        private static final class Trace {
            final int from, to;
            final float bend;
            Trace(int from, int to, float bend) {
                this.from = from; this.to = to; this.bend = bend;
            }
        }

        AppBackdrop(Context c) {
            super(c);
            density = getResources().getDisplayMetrics().density;
            setWillNotDraw(false);
            gridPaint.setStyle(Paint.Style.STROKE);
            gridPaint.setStrokeCap(Paint.Cap.ROUND);
            tracePaint.setStyle(Paint.Style.STROKE);
            tracePaint.setStrokeCap(Paint.Cap.ROUND);
            tracePaint.setStrokeJoin(Paint.Join.ROUND);
            nodePaint.setStyle(Paint.Style.FILL);
            packetPaint.setStyle(Paint.Style.FILL);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        @Override protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
        }

        /** Compatibility no-ops: MainActivity still calls these, but there is no loop to resume. */
        void resumeLoop() { }
        void pauseLoop() { }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            rebuildShaders(width, height);
            if (nodes.length == 0 || width != oldWidth || height != oldHeight) buildNetwork(width, height);
        }

        void refreshTheme() {
            rebuildShaders(getWidth(), getHeight());
            invalidate();
        }

        private void rebuildShaders(int width, int height) {
            if (width <= 0 || height <= 0) return;
            int[] base = UI.light
                    ? new int[]{0xFFF5F8FD, UI.CARD, 0xFFDDE5F1}
                    : UI.mono
                    ? new int[]{0xFF0B0F15, UI.BG, 0xFF202833}
                    : new int[]{0xFF142033, UI.BG, 0xFF0B1220};
            backgroundShader = new LinearGradient(0, 0, width, height, base, null, Shader.TileMode.CLAMP);
            int glow = UI.light ? 0x180B6E75 : UI.mono ? 0x18C9D4DF : 0x244DE0C8;
            int clear = glow & 0x00FFFFFF;
            ambientShader = new RadialGradient(width * .18f, height * .12f,
                    Math.max(width, height) * .62f,
                    new int[]{glow, clear}, null, Shader.TileMode.CLAMP);
        }

        private void buildNetwork(int width, int height) {
            if (width <= 0 || height <= 0) return;
            java.util.Random random = new java.util.Random(0xBA57E7A5L + width * 31L + height * 17L);
            nodes = new Node[NODE_COUNT];
            traces = new Trace[TRACE_COUNT];
            for (int i = 0; i < NODE_COUNT; i++) {
                float x = .035f + random.nextFloat() * .93f;
                float y = .045f + random.nextFloat() * .91f;
                boolean hub = i == 4 || i == 13 || i == 22;
                float radius = hub ? 2.0f + random.nextFloat() : .8f + random.nextFloat() * .85f;
                nodes[i] = new Node(x, y, radius, hub);
            }
            for (int i = 0; i < TRACE_COUNT; i++) {
                int from = i % NODE_COUNT;
                int to = (from + 1 + random.nextInt(7)) % NODE_COUNT;
                if (to == from) to = (to + 1) % NODE_COUNT;
                traces[i] = new Trace(from, to, .28f + random.nextFloat() * .44f);
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth(), height = getHeight();
            if (width <= 0 || height <= 0 || backgroundShader == null) return;
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(backgroundShader);
            canvas.drawRect(0, 0, width, height, paint);
            paint.setShader(ambientShader);
            canvas.drawRect(0, 0, width, height, paint);
            paint.setShader(null);

            gridPaint.setStrokeWidth(Math.max(1f, density * .55f));
            gridPaint.setColor(UI.light ? 0x120B6E75 : UI.mono ? 0x184A5563 : 0x154DE0C8);
            float step = 38f * density;
            for (float x = 0; x <= width; x += step) canvas.drawLine(x, 0, x, height, gridPaint);
            for (float y = 0; y <= height; y += step) canvas.drawLine(0, y, width, y, gridPaint);

            tracePaint.setStrokeWidth(Math.max(1f, density * 1.05f));
            tracePaint.setColor(UI.light ? 0x300B6E75 : UI.mono ? 0x40BECBD6 : 0x404DE0C8);
            for (Trace trace : traces) {
                Node a = nodes[trace.from], b = nodes[trace.to];
                float ax = a.x * width, ay = a.y * height;
                float bx = b.x * width, by = b.y * height;
                float midX = ax + (bx - ax) * trace.bend;
                canvas.drawLine(ax, ay, midX, ay, tracePaint);
                canvas.drawLine(midX, ay, midX, by, tracePaint);
                canvas.drawLine(midX, by, bx, by, tracePaint);
            }

            paint.setShader(null);
            paint.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
            paint.setTextSize(9f * density);
            paint.setColor(UI.light ? 0x400B6E75 : UI.mono ? 0x50BECBD6 : 0x50A9BCCB);
            String[] marks = {"</>", "0x", "01 10", "{ }", "AES", "TLS", "0101", "#"};
            float[][] anchors = {{.06f, .18f}, {.83f, .12f}, {.08f, .74f}, {.82f, .83f},
                    {.68f, .32f}, {.13f, .48f}, {.86f, .57f}, {.39f, .91f}};
            for (int i = 0; i < marks.length; i++) {
                canvas.drawText(marks[i], anchors[i][0] * width, anchors[i][1] * height, paint);
            }

            int nodeColor = UI.light ? 0xAA0B6E75 : UI.mono ? 0xAAE3F2F1 : 0xAA4DE0C8;
            for (Node node : nodes) {
                float x = node.x * width, y = node.y * height, r = node.radius * density;
                if (node.hub) {
                    nodePaint.setColor(UI.light ? 0x220B6E75 : 0x334DE0C8);
                    canvas.drawCircle(x, y, r * 3.1f, nodePaint);
                }
                nodePaint.setColor(nodeColor);
                canvas.drawCircle(x, y, r, nodePaint);
            }

            // Fixed packet markers (not animated): they suggest data flow without frame invalidation.
            packetPaint.setColor(UI.light ? 0xCC0B6E75 : UI.mono ? 0xCCE3F2F1 : 0xCC9C8CFF);
            for (int packet = 0; packet < 5 && traces.length > 0; packet++) {
                Trace trace = traces[(packet * 7 + 3) % traces.length];
                Node a = nodes[trace.from], b = nodes[trace.to];
                float mx = a.x * width + (b.x * width - a.x * width) * trace.bend;
                float progress = .18f + packet * .16f;
                float x = a.x * width + (mx - a.x * width) * progress;
                float y = a.y * height + (b.y * height - a.y * height) * progress * progress;
                canvas.drawCircle(x, y, 1.7f * density, packetPaint);
            }
            gridPaint.setColor(UI.light ? 0x180B6E75 : 0x184DE0C8);
            canvas.drawLine(width * .08f, height * .37f, width * .92f, height * .37f, gridPaint);
        }
    }

    /** Brand mark shared with the launcher icon; soft raised shadow, no text. */
    static final class BrandMark extends View {
        private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Bitmap mark;
        BrandMark(Context c) {
            super(c);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            shadowPaint.setColor(0xFFFFFFFF);
            shadowPaint.setShadowLayer(dp(this, 10), 0, dp(this, 5), 0xAA050B18);
            Bitmap decoded = BitmapFactory.decodeResource(c.getResources(), R.drawable.brand_mark);
            mark = decoded;
        }
        @Override protected void onDraw(Canvas c) {
            if (mark == null || getWidth() <= 0 || getHeight() <= 0) return;
            float side = Math.min(getWidth(), getHeight()) * .94f;
            RectF dst = new RectF((getWidth() - side) * .5f, (getHeight() - side) * .5f,
                    (getWidth() + side) * .5f, (getHeight() + side) * .5f);
            RectF shadow = new RectF(dst);
            shadow.offset(0, dp(this, 2.5f));
            c.drawBitmap(mark, null, shadow, shadowPaint);
            c.drawBitmap(mark, null, dst, imagePaint);
        }
    }

    /** Soft rounded logo halo with the same dual-shadow language as cards and buttons. */
    static final class LogoHalo extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        LogoHalo(Context c) {
            super(c);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            stroke.setStyle(Paint.Style.STROKE);
        }
        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            float inset = dp(this, 2);
            RectF bounds = new RectF(inset, inset, w - inset, h - inset);
            RectF shadowBounds = new RectF(bounds);
            shadowBounds.offset(0, dp(this, 4));
            fill.setShader(null);
            fill.setColor(UI.mono ? 0x553A3E46 : 0x554CB6D9);
            cv.drawRoundRect(shadowBounds, dp(this, 34), dp(this, 34), fill);
            fill.setShader(new LinearGradient(0, 0, w, h,
                    UI.light ? new int[]{0xAAFFFFFF, 0x556BADC7, 0x33DDE8F0}
                            : UI.mono ? new int[]{0x88666D78, 0x44252A31}
                            : new int[]{0x8870CDE0, 0x553D7897, 0x668D9BFF},
                    null, Shader.TileMode.CLAMP));
            fill.setShadowLayer(dp(this, 12), 0, dp(this, 5), 0xAA55BFEA);
            cv.drawRoundRect(bounds, dp(this, 34), dp(this, 34), fill);
            fill.clearShadowLayer();
            stroke.setStrokeWidth(dp(this, 2.1f));
            stroke.setShader(new LinearGradient(0, 0, w, h,
                    UI.mono ? new int[]{0xFFF5F6F8, 0xFF858D99, 0xFFFFFFFF}
                            : new int[]{0xFFE3FBFF, UI.PRIMARY, UI.ACCENT, 0xFFB7C9FF},
                    null, Shader.TileMode.CLAMP));
            cv.drawRoundRect(bounds, dp(this, 34), dp(this, 34), stroke);
            stroke.setShader(null);
            stroke.setStrokeWidth(dp(this, .8f));
            stroke.setColor(UI.mono ? 0x66868D98 : 0x667FE0F5);
            RectF inner = new RectF(bounds);
            inner.inset(dp(this, 4), dp(this, 4));
            cv.drawRoundRect(inner, dp(this, 30), dp(this, 30), stroke);
        }
    }

    /** Creator portrait cropped into a consistent circular neumorphic frame. */
    static final class PersonAvatar extends View {
        private final Paint fallback = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint photo = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint innerRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint figure = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint status = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix shaderMatrix = new Matrix();
        private final Bitmap portrait;
        private final BitmapShader shader;

        PersonAvatar(Context c) {
            super(c);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            Bitmap loaded;
            try {
                loaded = BitmapFactory.decodeResource(c.getResources(), R.drawable.creator_avatar);
            } catch (RuntimeException ex) {
                loaded = null;
            }
            portrait = loaded;
            shader = portrait == null ? null : new BitmapShader(portrait,
                    Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            photo.setDither(true);
            if (shader != null) photo.setShader(shader);
            fallback.setColor(UI.light ? 0xFF397F9D : UI.mono ? 0xFF34383F : 0xFF18354A);
            glow.setColor(UI.light ? 0x225AAACB : 0x2270CDE0);
            glow.setStyle(Paint.Style.FILL);
            glow.setShadowLayer(dp(this, 4), 0, 0, 0x4470CDE0);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(dp(this, 2.2f));
            ring.setShader(new LinearGradient(0, 0, dp(this, 66), dp(this, 66),
                    UI.mono ? new int[]{0xFFE8EBF0, 0xFF9299A3, 0xFFF4F5F7}
                            : new int[]{UI.PRIMARY, UI.BLUE, UI.ACCENT}, null, Shader.TileMode.CLAMP));
            innerRing.setStyle(Paint.Style.STROKE);
            innerRing.setStrokeWidth(dp(this, .8f));
            innerRing.setColor(0x6679BBD2);
            figure.setColor(0xFFF0F6FF);
        }

        @Override protected void onDraw(Canvas cv) {
            super.onDraw(cv);
            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            float cx = w * .5f, cy = h * .5f;
            float radius = Math.min(w, h) * .5f - dp(this, 4);
            if (radius <= 0) return;
            cv.drawCircle(cx, cy, radius + dp(this, 1), glow);
            if (shader != null && portrait != null) {
                float diameter = (radius - dp(this, 2)) * 2f;
                float scale = diameter / Math.min(portrait.getWidth(), portrait.getHeight());
                float left = cx - portrait.getWidth() * scale * .5f;
                float top = cy - portrait.getHeight() * scale * .5f;
                shaderMatrix.setScale(scale, scale);
                shaderMatrix.postTranslate(left, top);
                shader.setLocalMatrix(shaderMatrix);
                cv.drawCircle(cx, cy, radius - dp(this, 2), photo);
            } else {
                cv.drawCircle(cx, cy, radius - dp(this, 2), fallback);
                cv.drawCircle(cx, cy - radius * .20f, radius * .22f, figure);
                cv.drawOval(cx - radius * .48f, cy + radius * .05f,
                        cx + radius * .48f, cy + radius * .83f, figure);
            }
            cv.drawCircle(cx, cy, radius - dp(this, 1), ring);
            cv.drawCircle(cx, cy, radius - dp(this, 4), innerRing);
            status.setColor(UI.SUCCESS);
            cv.drawCircle(cx + radius * .68f, cy + radius * .68f, dp(this, 5.5f), status);
            status.setColor(UI.mono ? 0xFF101114 : UI.light ? 0xFFFFFFFF : 0xFF0A121C);
            cv.drawCircle(cx + radius * .68f, cy + radius * .68f, dp(this, 2.2f), status);
        }
    }
}
