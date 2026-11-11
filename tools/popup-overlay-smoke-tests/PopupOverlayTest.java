package app.bastehyab;

import android.content.Context;
import android.view.FakeWindowManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Regression test for the R5 bug: View.isAttachedToWindow() is false right after addView(), which
 * used to leave orphaned (un-removable, un-movable) scan panels. The fake WindowManager here is as
 * strict as the real one, and isAttachedToWindow() always returns false (the worst case).
 */
public class PopupOverlayTest {
    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }

    static TextView find(View v, String contains) {
        if (v instanceof TextView && String.valueOf(((TextView) v).text).contains(contains)) return (TextView) v;
        if (v instanceof ViewGroup) for (View k : ((ViewGroup) v).kids) { TextView t = find(k, contains); if (t != null) return t; }
        return null;
    }

    static void drag(TextView handle, float fx, float fy, float tx, float ty) {
        check(handle.touchL != null, "drag handle has no touch listener");
        check(handle.touchL.onTouch(handle, new MotionEvent(MotionEvent.ACTION_DOWN, fx, fy)), "DOWN not consumed");
        check(handle.touchL.onTouch(handle, new MotionEvent(MotionEvent.ACTION_MOVE, tx, ty)), "MOVE not consumed");
        handle.touchL.onTouch(handle, new MotionEvent(MotionEvent.ACTION_UP, tx, ty));
    }

    public static void main(String[] args) {
        FakeWindowManager wm = Context.WM;
        Popup popup = new Popup(new Context());
        Runnable noop = () -> {};

        // 1) Repeated refreshes with the same state must reuse the single window.
        for (int i = 0; i < 6; i++) check(popup.showConfirm("ایرانسل", 0, 0, 0, 0, "x", noop, noop, noop), "show failed");
        check(wm.windows.size() == 1 && wm.adds == 1, "same-state refresh created extra windows: " + wm.windows.size());
        check(popup.isConfirmShown(), "panel should be reported as shown");

        // 2) A mode change replaces the panel; never two at once.
        check(popup.showConfirm("ایرانسل", 1, 10, 2, 5, "y", noop, noop, null), "mode change failed");
        check(wm.windows.size() == 1 && wm.adds == 2 && wm.removes == 1, "mode change left orphans: " + wm.windows.size());

        // 3) Dragging moves the window that is actually on screen.
        View panel = wm.windows.get(0);
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) wm.params.get(panel);
        int x0 = lp.x, y0 = lp.y;
        drag(find(panel, "جابه‌جایی"), 500, 500, 560, 640);
        check(lp.x == x0 + 60 && lp.y == y0 + 140, "panel did not follow the finger: " + lp.x + "," + lp.y);
        check(wm.updates > 0, "updateViewLayout never called");
        // Injected swipes temporarily disable hit-testing of the panel, then restore it.
        popup.setConfirmTouchBlocked(true);
        check((lp.flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0,
                "scan panel should not intercept its own injected swipe");
        popup.setConfirmTouchBlocked(false);
        check((lp.flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) == 0,
                "scan panel touchability should be restored after the swipe");
        // the title is a drag area as well
        int ux = lp.x;
        drag(find(panel, "استخراج خودکار"), 100, 100, 90, 100);
        check(lp.x == ux - 10, "title drag failed");

        // 4) A rebuilt panel (pause) keeps the position chosen by the user.
        int movedX = lp.x, movedY = lp.y;
        check(popup.showConfirm("ایرانسل", 2, 10, 2, 5, "z", noop, noop, null), "pause panel failed");
        check(wm.windows.size() == 1, "orphan after pause");
        WindowManager.LayoutParams lp2 = (WindowManager.LayoutParams) wm.params.get(wm.windows.get(0));
        check(lp2.x == movedX && lp2.y == movedY, "position not preserved");

        // 5) A stopped scan can leave a readable final report with a close-only action.
        String report = "اسکرول خودکار در این صفحه پشتیبانی نشد\nآخرین روش: حرکت لمسی";
        check(popup.showConfirm("ایرانسل", 3, 42, 7, 11, report, null,
                () -> popup.hideConfirm(), null), "final report panel failed");
        check(wm.windows.size() == 1, "final report left duplicate windows");
        View reportPanel = wm.windows.get(0);
        check(find(reportPanel, "گزارش نهایی") != null, "final report title is missing");
        TextView reportText = find(reportPanel, "آخرین روش");
        check(reportText != null && String.valueOf(reportText.text).contains("اسکرول خودکار"),
                "final report details were not retained in the visible status text");
        TextView closeReport = find(reportPanel, "بستن گزارش");
        check(closeReport != null && closeReport.clickL != null, "final report close action is missing");
        closeReport.clickL.onClick(closeReport);
        check(wm.windows.isEmpty() && !popup.isConfirmShown(), "close report did not remove the panel");

        // 6) Exit: the panel is really removed, repeatedly and safely.
        popup.hideConfirm();
        check(wm.windows.isEmpty() && !popup.isConfirmShown(), "hideConfirm left a window behind");
        popup.hideConfirm();
        check(wm.windows.isEmpty(), "second hideConfirm misbehaved");

        // 7) Can be shown again afterwards.
        check(popup.showConfirm("ایرانسل", 0, 0, 0, 0, "", noop, noop, noop), "re-show failed");
        check(wm.windows.size() == 1, "re-show count wrong");
        popup.hideConfirm();

        // 8) If Android/OEM detaches the view without our hide path, the next health check
        // detects that WindowManager no longer owns it and a refresh rebuilds it.
        check(popup.showConfirm("ایرانسل", 0, 0, 0, 0, "", noop, noop, noop), "show for detach test failed");
        View detached = wm.windows.get(0);
        wm.windows.remove(detached);
        wm.params.remove(detached);
        try { Thread.sleep(1250L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
        check(!popup.isConfirmShown(), "detached panel was incorrectly reported as visible");
        check(popup.showConfirm("ایرانسل", 0, 0, 0, 0, "", noop, noop, noop), "detached panel was not rebuilt");
        check(wm.windows.size() == 1 && wm.adds >= 6, "rebuild did not attach a fresh panel");
        popup.hideConfirm();

        // 9) Stale drag listener of a removed panel must be inert (no moving of other windows).
        check(popup.showConfirm("ایرانسل", 0, 0, 0, 0, "", noop, noop, noop), "show failed");
        View old = wm.windows.get(0);
        TextView oldHandle = find(old, "جابه‌جایی");
        popup.hideConfirm();
        check(!oldHandle.touchL.onTouch(oldHandle, new MotionEvent(MotionEvent.ACTION_DOWN, 1, 1)), "stale listener still active");
        check(wm.windows.isEmpty(), "window count wrong at end");
        System.out.println("PASS: single overlay window, drag, position memory, final report, exit/removal, stale-listener safety");
    }
}
