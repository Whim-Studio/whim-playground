package com.midnight.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A plain-English summary of one night's events — the battles fought and the
 * army movements — for the UI message area. Owned by Task 2
 * ({@code com.midnight.ai}); declared here only so the core's {@code endDay}
 * signature can compile and be tested in isolation.
 */
public final class NightReport {

    private final List<BattleResult> battles;
    private final List<String> movements;

    public NightReport(List<BattleResult> battles, List<String> movements) {
        this.battles = battles == null
                ? new ArrayList<BattleResult>()
                : new ArrayList<BattleResult>(battles);
        this.movements = movements == null
                ? new ArrayList<String>()
                : new ArrayList<String>(movements);
    }

    public List<BattleResult> battles() {
        return Collections.unmodifiableList(battles);
    }

    public List<String> movements() {
        return Collections.unmodifiableList(movements);
    }

    /** A joined, plain-English summary suitable for the UI message area. */
    public String narrative() {
        StringBuilder sb = new StringBuilder();
        for (String m : movements) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(m);
        }
        for (BattleResult b : battles) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(b.text());
        }
        return sb.toString();
    }
}
