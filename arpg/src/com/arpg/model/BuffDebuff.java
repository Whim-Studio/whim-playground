package com.arpg.model;

import java.util.HashMap;
import java.util.Map;

/**
 * A timed effect attached to a {@link CombatParticipant}. Carries flat stat
 * modifiers (applied while active) and/or a periodic health delta applied each
 * tick. The engine ticks down {@code remainingTicks} and applies the effects —
 * this class only stores the data and offers a {@link #copy()} so a shared
 * template can be instanced onto a target.
 */
public final class BuffDebuff implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final String description;
    private final boolean beneficial;
    private final int durationTicks;
    private final Map<StatType, Integer> statModifiers;
    private final int periodicHealthDelta; // negative = damage-over-time, positive = heal-over-time
    private int remainingTicks;

    public BuffDebuff(String id, String name, String description, boolean beneficial,
                      int durationTicks, Map<StatType, Integer> statModifiers, int periodicHealthDelta) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("BuffDebuff id must not be blank");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.beneficial = beneficial;
        this.durationTicks = Math.max(0, durationTicks);
        this.statModifiers = new HashMap<StatType, Integer>();
        if (statModifiers != null) {
            this.statModifiers.putAll(statModifiers);
        }
        this.periodicHealthDelta = periodicHealthDelta;
        this.remainingTicks = this.durationTicks;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isBeneficial() {
        return beneficial;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    /** Unmodifiable-safe copy of the flat stat modifiers this effect grants. */
    public Map<StatType, Integer> getStatModifiers() {
        return new HashMap<StatType, Integer>(statModifiers);
    }

    public int getStatModifier(StatType type) {
        Integer v = statModifiers.get(type);
        return v == null ? 0 : v.intValue();
    }

    public int getPeriodicHealthDelta() {
        return periodicHealthDelta;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public void setRemainingTicks(int remainingTicks) {
        this.remainingTicks = Math.max(0, remainingTicks);
    }

    /** Decrement remaining ticks by one, floored at zero. Returns the new value. */
    public int decrementTick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
        return remainingTicks;
    }

    public boolean isExpired() {
        return remainingTicks <= 0;
    }

    /** Fresh instance of this template with duration reset — used when applying to a target. */
    public BuffDebuff copy() {
        return new BuffDebuff(id, name, description, beneficial, durationTicks, statModifiers, periodicHealthDelta);
    }

    @Override
    public String toString() {
        return name + " (" + remainingTicks + "/" + durationTicks + " ticks)";
    }
}
