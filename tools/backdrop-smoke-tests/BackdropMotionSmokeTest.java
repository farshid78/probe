package app.bastehyab;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

/** JVM smoke test: the cyber backdrop must be deterministic and must never run an animation loop. */
public final class BackdropMotionSmokeTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class Probe extends SplashViews.AppBackdrop {
        Probe(Context context) { super(context); }
        void sizeScene() { onSizeChanged(1080, 2340, 0, 0); }
        void drawFrame(Canvas canvas) { onDraw(canvas); }
    }

    private static Object field(Object target, String name) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field f = type.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static float coordinate(Object target, int index, String name) throws Exception {
        Object node = Array.get(target, index);
        Field f = node.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return ((Number) f.get(node)).floatValue();
    }

    public static void main(String[] args) throws Exception {
        View.animationInvalidateRequests = 0;
        Probe view = new Probe(new Context());
        view.sizeScene();
        Object nodes = field(view, "nodes");
        Object traces = field(view, "traces");
        check(Array.getLength(nodes) == 30, "the static cyber scene should contain 30 network nodes");
        check(Array.getLength(traces) == 36, "the static cyber scene should contain 36 circuit traces");
        float firstX = coordinate(nodes, 0, "x");
        float firstY = coordinate(nodes, 0, "y");
        view.drawFrame(new Canvas());
        view.refreshTheme();
        view.drawFrame(new Canvas());
        check(Math.abs(firstX - coordinate(field(view, "nodes"), 0, "x")) < .00001f
                        && Math.abs(firstY - coordinate(field(view, "nodes"), 0, "y")) < .00001f,
                "network geometry must remain stable across resume/theme refresh");
        check(View.animationInvalidateRequests == 0,
                "the background must never schedule frame-by-frame animation invalidations");
        System.out.println("BackdropMotionSmokeTest: PASS (static cyber network, 30 nodes, 36 traces, no animation loop)");
    }
}
