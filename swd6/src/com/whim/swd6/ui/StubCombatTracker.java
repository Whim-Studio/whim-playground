package com.whim.swd6.ui;

import com.whim.swd6.api.Combatant;
import com.whim.swd6.api.CombatTracker;
import com.whim.swd6.api.DamageResult;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.WoundLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * DEV STUB, replaced by Main (engine's Encounter) at runtime.
 *
 * A straightforward {@link CombatTracker}: rolls initiative, orders highest-first,
 * advances through living combatants, and escalates wounds via {@link WoundLevel}.
 *
 * Owned by Task 3 (ui). Not shipped in the wired app.
 */
public final class StubCombatTracker implements CombatTracker {

    private final List<Combatant> combatants = new ArrayList<Combatant>();
    private final Random rng = new Random();
    private int index = 0;
    private int round = 0;

    @Override
    public void add(Combatant c) {
        combatants.add(c);
    }

    @Override
    public void rollInitiative() {
        for (Combatant c : combatants) {
            // Perception-ish initiative: 3d6 plus a small edge for player characters.
            int roll = rng.nextInt(6) + rng.nextInt(6) + rng.nextInt(6) + 3;
            c.setInitiative(roll + (c.isPlayerCharacter() ? 1 : 0));
        }
        Collections.sort(combatants, new Comparator<Combatant>() {
            @Override
            public int compare(Combatant a, Combatant b) {
                return Integer.compare(b.getInitiative(), a.getInitiative());
            }
        });
        round = 1;
        index = 0;
        advanceToLiving();
    }

    @Override
    public List<Combatant> order() {
        return combatants;
    }

    @Override
    public Combatant current() {
        if (isOver() || combatants.isEmpty()) {
            return null;
        }
        if (index < 0 || index >= combatants.size()) {
            return null;
        }
        return combatants.get(index);
    }

    @Override
    public void next() {
        if (combatants.isEmpty()) {
            return;
        }
        int guard = 0;
        do {
            index++;
            if (index >= combatants.size()) {
                index = 0;
                round++;
            }
            guard++;
            if (guard > combatants.size() * 2 + 2) {
                break; // no living combatant found; avoid infinite loop
            }
        } while (!canAct(combatants.get(index)));
    }

    private void advanceToLiving() {
        int guard = 0;
        while (index < combatants.size() && !canAct(combatants.get(index))) {
            index++;
            if (index >= combatants.size()) {
                index = 0;
            }
            guard++;
            if (guard > combatants.size() + 1) {
                break;
            }
        }
    }

    private boolean canAct(Combatant c) {
        return !c.getWoundLevel().incapacitatedOrWorse();
    }

    @Override
    public WoundLevel applyHit(Combatant target, DamageResult result) {
        WoundLevel next = target.getWoundLevel().escalate(result.getInflicted());
        target.setWoundLevel(next);
        // keep the wrapped PC's wound state in sync so the sheet reflects it
        PlayerCharacter pc = target.getPc();
        if (pc != null) {
            pc.setWoundLevel(next);
        }
        return next;
    }

    @Override
    public boolean isOver() {
        int pcsUp = 0;
        int npcsUp = 0;
        for (Combatant c : combatants) {
            if (canAct(c)) {
                if (c.isPlayerCharacter()) {
                    pcsUp++;
                } else {
                    npcsUp++;
                }
            }
        }
        // Over when one side (or nobody) can still act.
        return pcsUp == 0 || npcsUp == 0;
    }

    @Override
    public int round() {
        return round;
    }
}
