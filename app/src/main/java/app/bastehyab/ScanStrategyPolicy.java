package app.bastehyab;

/** Operator-neutral scan/scroll policy. Every operator follows the same MCI-validated order. */
final class ScanStrategyPolicy {
    private ScanStrategyPolicy() {}

    /** Use ACTION_SCROLL_FORWARD first; after no movement, the service escalates to gestures. */
    static boolean shouldUseAccessibilityActionFirst(int strategy) {
        return strategy <= 0;
    }

    /** After the first failed movement, prefer a gesture in the full app content viewport. */
    static boolean shouldPreferAppViewportSwipe(int strategy) {
        return strategy >= 2;
    }
}
