package app.bastehyab;

/** Regression coverage for conservative three-state end-of-list detection. */
public final class ListEndDetectorTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        ListEndDetector detector = new ListEndDetector();
        check(detector.state() == ListEndDetector.State.UNKNOWN, "initial state must be unknown");

        check(detector.observe(100, 1000, false, false, 1) == ListEndDetector.State.MORE_AVAILABLE,
                "not-at-bottom means more content may exist");
        check(detector.observe(1000, 1000, true, false, 2) == ListEndDetector.State.MORE_AVAILABLE,
                "load-more indicator prevents end confirmation");
        check(detector.observe(1000, 1000, false, true, 3) == ListEndDetector.State.UNKNOWN,
                "a first bottom observation is only a candidate");
        check(detector.observe(1000, 1000, false, false, 3) == ListEndDetector.State.UNKNOWN,
                "duplicate cycle cannot satisfy independent-observation requirement");
        check(detector.observe(1000, 1000, false, false, 4) == ListEndDetector.State.END_CONFIRMED,
                "two independent bottom observations confirm end");

        check(detector.observe(500, 1000, false, false, 6) == ListEndDetector.State.MORE_AVAILABLE,
                "moving away from bottom resets end proof");
        check(detector.observe(0, 0, false, false, 7) == ListEndDetector.State.UNKNOWN,
                "missing scroll range must remain unknown");
        detector.reset();
        check(detector.state() == ListEndDetector.State.UNKNOWN, "reset returns to unknown");

        check(detector.observe(900, 1000, 12, 9, false, false, 10)
                == ListEndDetector.State.MORE_AVAILABLE,
                "a visible item index before the last item means more content remains");
        check(detector.observe(1000, 1000, 12, 11, false, false, 11)
                == ListEndDetector.State.UNKNOWN,
                "first independent bottom observation is not enough");
        check(detector.observe(1000, 1000, 12, 11, false, false, 12)
                == ListEndDetector.State.END_CONFIRMED,
                "scroll offsets and last visible item together confirm the end after two cycles");

        detector.reset();
        check(detector.observeStableViewport("cards-A", false, true, 20) == ListEndDetector.State.UNKNOWN,
                "first stable viewport is a candidate only");
        check(detector.observeStableViewport("cards-A", false, true, 20) == ListEndDetector.State.UNKNOWN,
                "duplicate viewport cycle cannot confirm end");
        check(detector.observeStableViewport("cards-A", false, true, 21) == ListEndDetector.State.END_CONFIRMED,
                "two complete stable viewports after gesture escalation confirm end for providers without offsets");

        detector.reset();
        check(detector.observeStableViewport("cards-A", false, false, 30) == ListEndDetector.State.UNKNOWN,
                "an incomplete accessibility tree cannot prove the end");
        check(detector.observeStableViewport("cards-B", true, true, 31) == ListEndDetector.State.MORE_AVAILABLE,
                "visible pagination control prevents end confirmation");

        // There is deliberately no operator-specific stable-offer shortcut anymore.
        // Every provider must complete through the same bottom/viewport proof above.
        System.out.println("ListEndDetectorTest: PASS");
    }
}
