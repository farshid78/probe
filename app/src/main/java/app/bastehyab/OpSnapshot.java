package app.bastehyab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable operator configuration captured at scan-session start. */
final class OpSnapshot {
    final String id, name, cur;
    final int color, policy;
    final boolean enabled, builtin, cmp;
    final List<String> pkgs;

    OpSnapshot(Op source) {
        id = source.id;
        name = source.name;
        cur = source.cur;
        color = source.color;
        policy = source.policy;
        enabled = source.enabled;
        builtin = source.builtin;
        cmp = source.cmp;
        pkgs = Collections.unmodifiableList(new ArrayList<>(source.pkgs));
    }

    boolean ownsPackage(String packageName) { return packageName != null && pkgs.contains(packageName); }

    /** Parser compatibility copy; the session snapshot itself remains immutable. */
    Op toOp() {
        Op copy = new Op(id, name, color, cur, builtin);
        copy.policy = policy;
        copy.enabled = enabled;
        copy.cmp = cmp;
        copy.pkgs.addAll(pkgs);
        return copy;
    }
}
