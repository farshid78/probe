package app.bastehyab;

import org.junit.Test;
import static org.junit.Assert.*;

/** JVM regression tests for immutable scan-session operator configuration. */
public final class OpSnapshotTest {
    @Test public void snapshotDoesNotChangeWhenOriginalOperatorIsMutated() {
        Op source = new Op("irancell", "ایرانسل", 0xff00aa99, "IRT", true, "com.myirancell");
        source.enabled = true;
        OpSnapshot snapshot = new OpSnapshot(source);
        source.name = "Changed";
        source.pkgs.clear();
        source.enabled = false;
        assertEquals("ایرانسل", snapshot.name);
        assertTrue(snapshot.enabled);
        assertTrue(snapshot.ownsPackage("com.myirancell"));
        assertFalse(snapshot.ownsPackage("com.changed"));
    }

    @Test public void snapshotPackageListCannotBeMutated() {
        Op source = new Op("mci", "همراه اول", 1, "IRT", true, "com.mci");
        OpSnapshot snapshot = new OpSnapshot(source);
        try { snapshot.pkgs.add("com.injected"); fail("snapshot package list must be immutable"); }
        catch (UnsupportedOperationException expected) { /* expected */ }
        assertFalse(snapshot.ownsPackage("com.injected"));
    }

    @Test public void parserGetsDefensiveMutableCopy() {
        Op source = new Op("rightel", "رایتل", 1, "IRT", true, "ir.rightel");
        OpSnapshot snapshot = new OpSnapshot(source);
        Op copy = snapshot.toOp();
        copy.pkgs.clear();
        copy.name = "Temporary parser copy";
        assertEquals("رایتل", snapshot.name);
        assertTrue(snapshot.ownsPackage("ir.rightel"));
    }
}
