package com.whim.alganon.combat;

import com.whim.alganon.api.Combatant;
import com.whim.alganon.api.Enums.AbilityKind;
import com.whim.alganon.api.Enums.DamageType;

/**
 * A live over-time effect (DOT/HOT) or timed buff/debuff attached to a combatant.
 * The engine ticks these once per second; damage/heal magnitude is {@link #power}
 * per tick. Buffs/debuffs carry no per-tick numeric payload in v1 — they just live
 * for {@link #remaining} seconds and read as flavor/state.
 *
 * <p>[Gap — my design] one-second tick granularity for DOT/HOT.</p>
 */
public final class ActiveEffect {
    public final Combatant target;
    public final AbilityKind kind;
    public final DamageType type;
    public final int power;
    public final String name;
    public double remaining;
    private double tickAcc;

    public ActiveEffect(Combatant target, AbilityKind kind, DamageType type, int power, double duration, String name) {
        this.target = target;
        this.kind = kind;
        this.type = type;
        this.power = power;
        this.remaining = duration;
        this.name = name;
    }

    /** Advance by dt; returns the number of whole one-second ticks that fired this step. */
    public int advance(double dt) {
        remaining -= dt;
        tickAcc += dt;
        int ticks = 0;
        while (tickAcc >= 1.0) {
            tickAcc -= 1.0;
            ticks++;
        }
        return ticks;
    }

    public boolean expired() {
        return remaining <= 0.0;
    }
}
