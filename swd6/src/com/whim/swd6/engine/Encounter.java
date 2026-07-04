package com.whim.swd6.engine;

import com.whim.swd6.api.Attribute;
import com.whim.swd6.api.Combatant;
import com.whim.swd6.api.CombatTracker;
import com.whim.swd6.api.DamageResult;
import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.RollResult;
import com.whim.swd6.api.RpgEngine;
import com.whim.swd6.api.WoundLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A single combat encounter: holds the combatants, rolls initiative, drives the
 * turn order and round counter, and applies resolved hits to wound state.
 *
 * <p>Initiative is a Wild-Die Perception-style check per combatant: a PC uses its
 * Perception attribute code, an NPC a default 3D. The order is sorted highest-first
 * and the rolled number is stored back on each {@link Combatant} via
 * {@code setInitiative}. Turn advancement skips combatants who are Incapacitated or
 * worse, and the encounter is over once one side (PCs vs non-PCs) has nobody able to
 * act.</p>
 *
 * Owned by Task 2 (engine).
 */
public final class Encounter implements CombatTracker {

    /** Initiative code used for NPC combatants (no PlayerCharacter attached). */
    private static final DiceCode NPC_PERCEPTION = DiceCode.parse("3D");

    private final RpgEngine engine;
    private final List<Combatant> combatants = new ArrayList<Combatant>();
    private final List<Combatant> turnOrder = new ArrayList<Combatant>();

    private int currentIndex = 0;
    private int round = 1;

    public Encounter(RpgEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("engine is required");
        }
        this.engine = engine;
    }

    @Override
    public void add(Combatant c) {
        if (c != null) {
            combatants.add(c);
        }
    }

    @Override
    public void rollInitiative() {
        for (Combatant c : combatants) {
            DiceCode code = initiativeCode(c);
            RollResult r = engine.roll(code, true, -1);
            c.setInitiative(r.getTotal());
        }
        turnOrder.clear();
        turnOrder.addAll(combatants);
        // Highest initiative first; stable for ties.
        Collections.sort(turnOrder, new Comparator<Combatant>() {
            @Override
            public int compare(Combatant a, Combatant b) {
                return Integer.compare(b.getInitiative(), a.getInitiative());
            }
        });
        round = 1;
        currentIndex = firstActableFrom(0);
    }

    private DiceCode initiativeCode(Combatant c) {
        if (c.isPlayerCharacter() && c.getPc() != null) {
            return c.getPc().getAttribute(Attribute.PERCEPTION);
        }
        return NPC_PERCEPTION;
    }

    @Override
    public List<Combatant> order() {
        return Collections.unmodifiableList(turnOrder);
    }

    @Override
    public Combatant current() {
        if (turnOrder.isEmpty() || isOver()) {
            return null;
        }
        if (currentIndex < 0 || currentIndex >= turnOrder.size()) {
            return null;
        }
        Combatant c = turnOrder.get(currentIndex);
        return c.getWoundLevel().incapacitatedOrWorse() ? null : c;
    }

    @Override
    public void next() {
        if (turnOrder.isEmpty()) {
            return;
        }
        // Scan forward for the next combatant able to act, wrapping into a new round.
        for (int i = 0; i < turnOrder.size(); i++) {
            currentIndex++;
            if (currentIndex >= turnOrder.size()) {
                currentIndex = 0;
                round++;
            }
            if (!turnOrder.get(currentIndex).getWoundLevel().incapacitatedOrWorse()) {
                return;
            }
        }
        // Nobody left able to act; leave index as-is (isOver() will report true).
    }

    @Override
    public int round() {
        return round;
    }

    @Override
    public WoundLevel applyHit(Combatant target, DamageResult result) {
        if (target == null || result == null) {
            return target == null ? WoundLevel.HEALTHY : target.getWoundLevel();
        }
        WoundLevel updated = target.getWoundLevel().escalate(result.getInflicted());
        target.setWoundLevel(updated);
        return updated;
    }

    @Override
    public boolean isOver() {
        boolean pcCanAct = false;
        boolean npcCanAct = false;
        for (Combatant c : combatants) {
            if (!c.getWoundLevel().incapacitatedOrWorse()) {
                if (c.isPlayerCharacter()) {
                    pcCanAct = true;
                } else {
                    npcCanAct = true;
                }
            }
        }
        return !(pcCanAct && npcCanAct);
    }

    /** Index of the first combatant able to act at or after {@code from}. */
    private int firstActableFrom(int from) {
        for (int i = from; i < turnOrder.size(); i++) {
            if (!turnOrder.get(i).getWoundLevel().incapacitatedOrWorse()) {
                return i;
            }
        }
        return from;
    }
}
