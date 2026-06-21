package com.midnight.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable summary of a single NIGHT phase: the army movements Doomdark made
 * and the battles that were fought. {@link #narrative()} joins them into a
 * plain-English block for the UI's message area.
 */
public final class NightReport {

    private final List<BattleResult> battles;
    private final List<String> movements;

    public NightReport(List<BattleResult> battles, List<String> movements) {
        this.battles = Collections.unmodifiableList(
                new ArrayList<BattleResult>(battles == null ? Collections.<BattleResult>emptyList() : battles));
        this.movements = Collections.unmodifiableList(
                new ArrayList<String>(movements == null ? Collections.<String>emptyList() : movements));
    }

    /** The battles resolved this night (possibly empty), never {@code null}. */
    public List<BattleResult> battles() {
        return battles;
    }

    /** The army-movement descriptions this night (possibly empty), never {@code null}. */
    public List<String> movements() {
        return movements;
    }

    /**
     * A joined, plain-English summary of the night's movements and battles,
     * suitable for the UI message area. Never {@code null}; returns a quiet-night
     * line when nothing happened.
     */
    public String narrative() {
        StringBuilder sb = new StringBuilder();
        sb.append("Night falls across Midnight.");
        for (String m : movements) {
            sb.append('\n').append(m);
        }
        for (BattleResult b : battles) {
            sb.append('\n').append(b.text());
        }
        if (movements.isEmpty() && battles.isEmpty()) {
            sb.append("\nDoomdark's armies hold their ground; the night passes quietly.");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return narrative();
    }
}
