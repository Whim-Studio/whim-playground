package com.whim.necromunda.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * A fighter's profile: a base value per {@link Stat} plus a separate,
 * non-destructive modifier layer.
 *
 * <p>Flesh wounds ({@code -1}) and campaign advances ({@code +1}) are applied
 * to the modifier layer so the base profile is never mutated. This means an
 * advance can cleanly cancel a flesh wound, and the original profile is always
 * recoverable. {@code effective(stat) = base(stat) + modifier(stat)}.
 */
public final class StatLine {

    private final Map<Stat, Integer> base = new EnumMap<Stat, Integer>(Stat.class);
    private final Map<Stat, Integer> modifiers = new EnumMap<Stat, Integer>(Stat.class);

    public StatLine() {
        for (Stat s : Stat.values()) {
            base.put(s, 0);
            modifiers.put(s, 0);
        }
    }

    /** Convenience builder for the canonical stat order M,WS,BS,S,T,W,I,A,Ld. */
    public static StatLine of(int m, int ws, int bs, int s, int t, int w, int i, int a, int ld) {
        StatLine sl = new StatLine();
        sl.setBase(Stat.M, m);
        sl.setBase(Stat.WS, ws);
        sl.setBase(Stat.BS, bs);
        sl.setBase(Stat.S, s);
        sl.setBase(Stat.T, t);
        sl.setBase(Stat.W, w);
        sl.setBase(Stat.I, i);
        sl.setBase(Stat.A, a);
        sl.setBase(Stat.LD, ld);
        return sl;
    }

    public void setBase(Stat stat, int value) {
        base.put(stat, value);
    }

    public int base(Stat stat) {
        return base.get(stat);
    }

    /** Adjust the modifier layer (e.g. {@code modify(WS, -1)} for a flesh wound). */
    public void modify(Stat stat, int delta) {
        modifiers.put(stat, modifiers.get(stat) + delta);
    }

    public int modifier(Stat stat) {
        return modifiers.get(stat);
    }

    /** The value used everywhere in the rules: base + accumulated modifiers. */
    public int effective(Stat stat) {
        return base.get(stat) + modifiers.get(stat);
    }

    /** A deep, independent copy — mutating the copy never touches this instance. */
    public StatLine copy() {
        StatLine c = new StatLine();
        for (Stat s : Stat.values()) {
            c.base.put(s, base.get(s));
            c.modifiers.put(s, modifiers.get(s));
        }
        return c;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Stat s : Stat.values()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(s.name()).append(effective(s));
        }
        return sb.toString();
    }
}
