package app.bastehyab;

import org.junit.Test;
import static org.junit.Assert.*;

/** Verifies the operator-neutral scroll order now shared by Irancell and MCI. */
public final class ScanStrategyPolicyTest {
    @Test public void accessibilityActionIsAttemptedAtStartForEveryOperator() {
        assertTrue(ScanStrategyPolicy.shouldUseAccessibilityActionFirst(0));
        assertFalse(ScanStrategyPolicy.shouldUseAccessibilityActionFirst(1));
        assertFalse(ScanStrategyPolicy.shouldUseAccessibilityActionFirst(2));
    }

    @Test public void appViewportSwipeIsUsedOnlyAfterRepeatedNoMovement() {
        assertFalse(ScanStrategyPolicy.shouldPreferAppViewportSwipe(0));
        assertFalse(ScanStrategyPolicy.shouldPreferAppViewportSwipe(1));
        assertTrue(ScanStrategyPolicy.shouldPreferAppViewportSwipe(2));
    }
}
