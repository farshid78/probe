package app.bastehyab;

import java.util.HashSet;
import java.util.Set;

/** Conservative list-end detector fed by accessibility scroll events and stable-viewport evidence. */
final class ListEndDetector {
    private static final int REQUIRED_INDEPENDENT_BOTTOM_CYCLES = 2;
    private static final int REQUIRED_STABLE_VIEWPORT_CYCLES = 2;
    enum State { MORE_AVAILABLE, END_CONFIRMED, UNKNOWN }

    private State state = State.UNKNOWN;
    private final Set<Long> proofCycles = new HashSet<>();
    private final Set<Long> stableCycles = new HashSet<>();
    private int lastMax = -1;
    private int lastY = -1;
    private String lastStableFingerprint = "";

    synchronized State observe(int scrollY, int maxScrollY, boolean moreIndicator,
                               boolean forwardActionAccepted, long scanCycle) {
        return observe(scrollY, maxScrollY, -1, -1, moreIndicator, forwardActionAccepted, scanCycle);
    }

    /** Uses scroll offsets where exposed, or RecyclerView-style visible-item indexes as fallback. */
    synchronized State observe(int scrollY, int maxScrollY, int itemCount, int toIndex,
                               boolean moreIndicator, boolean forwardActionAccepted, long scanCycle) {
        if (moreIndicator) {
            resetProofs();
            state = State.MORE_AVAILABLE;
            return state;
        }

        boolean hasOffset = maxScrollY > 0 && scrollY >= 0;
        boolean hasItems = itemCount > 0 && toIndex >= 0;
        if (!hasOffset && !hasItems) {
            state = State.UNKNOWN;
            proofCycles.clear();
            lastMax = lastY = -1;
            return state;
        }

        int range = hasOffset ? maxScrollY : itemCount;
        int position = hasOffset ? scrollY : toIndex;
        int tolerance = hasOffset ? Math.max(2, maxScrollY / 100) : 0;
        boolean atBottom = hasOffset
                ? scrollY >= maxScrollY - tolerance
                : toIndex >= itemCount - 1;
        if (!atBottom) {
            resetProofs();
            state = State.MORE_AVAILABLE;
            lastMax = range;
            lastY = position;
            return state;
        }

        // The same range must be observed at the bottom in separate completed scan cycles.
        // Android may report a forward action as accepted even when it did not move the list.
        if (lastMax != range || lastY < 0 || Math.abs(lastY - position) > tolerance) {
            proofCycles.clear();
        }
        lastMax = range;
        lastY = position;
        stableCycles.clear();
        lastStableFingerprint = "";
        proofCycles.add(scanCycle);
        state = proofCycles.size() >= REQUIRED_INDEPENDENT_BOTTOM_CYCLES
                ? State.END_CONFIRMED : State.UNKNOWN;
        return state;
    }

    /**
     * Fallback for Compose/WebView providers that expose no scroll range or item indexes.
     * Called only after the scanner has escalated to real app-viewport gestures. A complete
     * readable tree, no visible pagination control, and two independent unchanged viewports
     * are required; incomplete trees are never accepted as end-of-list proof.
     */
    synchronized State observeStableViewport(String fingerprint, boolean moreIndicator,
                                             boolean treeComplete, long scanCycle) {
        if (moreIndicator) {
            resetProofs();
            state = State.MORE_AVAILABLE;
            return state;
        }
        if (!treeComplete || fingerprint == null || fingerprint.isEmpty()) {
            stableCycles.clear();
            lastStableFingerprint = "";
            if (state != State.MORE_AVAILABLE) state = State.UNKNOWN;
            return state;
        }
        if (!fingerprint.equals(lastStableFingerprint)) {
            stableCycles.clear();
            lastStableFingerprint = fingerprint;
        }
        proofCycles.clear();
        stableCycles.add(scanCycle);
        state = stableCycles.size() >= REQUIRED_STABLE_VIEWPORT_CYCLES
                ? State.END_CONFIRMED : State.UNKNOWN;
        return state;
    }

    private void resetProofs() {
        proofCycles.clear();
        stableCycles.clear();
        lastStableFingerprint = "";
    }

    synchronized State state() { return state; }

    synchronized void reset() {
        state = State.UNKNOWN;
        resetProofs();
        lastMax = lastY = -1;
    }
}
