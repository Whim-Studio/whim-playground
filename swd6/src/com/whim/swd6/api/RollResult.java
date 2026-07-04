package com.whim.swd6.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable outcome of a dice-code roll. Produced by the engine, read by the UI.
 *
 * Captures every physical die so the UI can show them: the "normal" dice, the
 * Wild Die's roll chain (multiple entries when it exploded on 6), the pip modifier,
 * the final total, and the two rules flags:
 *   - {@link #isComplication()} true when the Wild Die came up 1
 *   - {@link #isWildExploded()} true when the Wild Die rolled at least one 6
 *
 * When a target number was supplied, {@link #getTarget()} is &gt;= 0 and
 * {@link #isSuccess()} reflects total &gt;= target; otherwise target is -1.
 *
 * Owned by the orchestrator (api).
 */
public final class RollResult {

    private final List<Integer> normalDice;
    private final List<Integer> wildDieRolls; // chain; empty when no wild die used
    private final int pips;
    private final int total;
    private final boolean complication;
    private final boolean wildExploded;
    private final int target;         // -1 when none supplied
    private final DiceCode code;      // the code that was rolled

    public RollResult(DiceCode code, List<Integer> normalDice, List<Integer> wildDieRolls,
                      int pips, int total, boolean complication, boolean wildExploded, int target) {
        this.code = code;
        this.normalDice = Collections.unmodifiableList(new ArrayList<Integer>(normalDice));
        this.wildDieRolls = Collections.unmodifiableList(new ArrayList<Integer>(wildDieRolls));
        this.pips = pips;
        this.total = total;
        this.complication = complication;
        this.wildExploded = wildExploded;
        this.target = target;
    }

    public DiceCode getCode() { return code; }
    public List<Integer> getNormalDice() { return normalDice; }
    public List<Integer> getWildDieRolls() { return wildDieRolls; }
    public int getPips() { return pips; }
    public int getTotal() { return total; }
    public boolean isComplication() { return complication; }
    public boolean isWildExploded() { return wildExploded; }
    public int getTarget() { return target; }

    public boolean hasTarget() { return target >= 0; }
    public boolean isSuccess() { return hasTarget() && total >= target; }
}
