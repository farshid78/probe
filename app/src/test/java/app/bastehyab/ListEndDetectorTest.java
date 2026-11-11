package app.bastehyab;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** JVM regression tests for conservative list-end detection. */
public final class ListEndDetectorTest {
    @Test public void requiresTwoIndependentBottomObservations() {
        ListEndDetector detector = new ListEndDetector();
        assertEquals(ListEndDetector.State.UNKNOWN, detector.state());
        assertEquals(ListEndDetector.State.MORE_AVAILABLE, detector.observe(10, 100, false, false, 1));
        assertEquals(ListEndDetector.State.MORE_AVAILABLE, detector.observe(100, 100, true, false, 2));
        assertEquals(ListEndDetector.State.UNKNOWN, detector.observe(100, 100, false, true, 3));
        assertEquals(ListEndDetector.State.UNKNOWN, detector.observe(100, 100, false, false, 3));
        assertEquals(ListEndDetector.State.END_CONFIRMED, detector.observe(100, 100, false, false, 4));
    }

    @Test public void movementAndLoadMoreResetEndEvidence() {
        ListEndDetector detector = new ListEndDetector();
        detector.observe(100, 100, false, false, 1);
        assertEquals(ListEndDetector.State.END_CONFIRMED, detector.observe(100, 100, false, false, 2));
        assertEquals(ListEndDetector.State.MORE_AVAILABLE, detector.observe(20, 100, false, false, 3));
        assertEquals(ListEndDetector.State.MORE_AVAILABLE, detector.observe(100, 100, true, false, 4));
        assertEquals(ListEndDetector.State.UNKNOWN, detector.observe(100, 100, false, false, 5));

        // Providers without scroll offsets use the same complete-viewport proof.
        // Two independent, complete, unchanged viewport observations are required.
        assertEquals(ListEndDetector.State.UNKNOWN,
                detector.observeStableViewport("offers-A", false, true, 6));
        assertEquals(ListEndDetector.State.UNKNOWN,
                detector.observeStableViewport("offers-A", false, true, 6));
        assertEquals(ListEndDetector.State.END_CONFIRMED,
                detector.observeStableViewport("offers-A", false, true, 7));

        // A changed viewport or incomplete accessibility tree resets the proof.
        assertEquals(ListEndDetector.State.UNKNOWN,
                detector.observeStableViewport("offers-B", false, true, 8));
        assertEquals(ListEndDetector.State.UNKNOWN,
                detector.observeStableViewport("offers-B", false, false, 9));
        assertEquals(ListEndDetector.State.UNKNOWN,
                detector.observeStableViewport("offers-B", false, true, 10));
        assertEquals(ListEndDetector.State.END_CONFIRMED,
                detector.observeStableViewport("offers-B", false, true, 11));
    }

    @Test public void missingRangeRemainsUnknownAndResetClearsEvidence() {
        ListEndDetector detector = new ListEndDetector();
        assertEquals(ListEndDetector.State.UNKNOWN, detector.observe(0, 0, false, false, 1));
        detector.observe(100, 100, false, false, 2);
        detector.reset();
        assertEquals(ListEndDetector.State.UNKNOWN, detector.state());
    }
}
