package com.whim.powermonger.engine;

import java.util.List;

import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Enums.TerrainType;
import com.whim.powermonger.domain.ArmyBloc;
import com.whim.powermonger.domain.Captain;
import com.whim.powermonger.domain.MapGrid;
import com.whim.powermonger.domain.Tile;
import com.whim.powermonger.domain.WorldState;

/**
 * Real-time skirmish resolution. When two opposing, living blocs are co-located,
 * both sides trade casualties each tick using {@link CombatMath} (strength, posture
 * aggression, terrain elevation advantage, food/morale). A captain reduced to 0
 * strength is eliminated and the balance of power shifts. A pitched battle on a
 * forested tile tramples it (deforestation).
 */
public final class CombatResolver {

    /** Blocs within this many tiles of each other are "in contact". */
    private static final double CONTACT_RANGE = 0.75;

    /**
     * Resolve all co-located opposing pairs for this tick. Appends human-readable
     * notes (eliminations, battles) to {@code events}.
     */
    public void tick(WorldState w, List<String> events) {
        List<Captain> caps = w.captains();
        MapGrid g = w.grid();
        for (int i = 0; i < caps.size(); i++) {
            Captain a = caps.get(i);
            if (!a.alive() || a.strength() <= 0) {
                continue;
            }
            for (int j = i + 1; j < caps.size(); j++) {
                Captain b = caps.get(j);
                if (!b.alive() || b.strength() <= 0) {
                    continue;
                }
                if (!opposing(a, b)) {
                    continue;
                }
                double dist = CommandLag.distance(a.x(), a.y(), b.x(), b.y());
                if (dist > CONTACT_RANGE) {
                    continue;
                }
                skirmish(a, b, g, events);
            }
        }
    }

    private static boolean opposing(Captain a, Captain b) {
        Allegiance aa = a.allegiance();
        Allegiance bb = b.allegiance();
        if (aa == Allegiance.NEUTRAL || bb == Allegiance.NEUTRAL) {
            return false;
        }
        return aa != bb;
    }

    private void skirmish(Captain a, Captain b, MapGrid g, List<String> events) {
        int elevA = elevationAt(g, a.x(), a.y());
        int elevB = elevationAt(g, b.x(), b.y());

        double moraleA = CombatMath.morale(a.food(), a.strength());
        double moraleB = CombatMath.morale(b.food(), b.strength());

        double powerA = CombatMath.effectivePower(a.strength(), a.posture(), elevA, moraleA);
        double powerB = CombatMath.effectivePower(b.strength(), b.posture(), elevB, moraleB);

        int lossA = CombatMath.casualties(powerA, powerB, a.strength());
        int lossB = CombatMath.casualties(powerB, powerA, b.strength());

        ArmyBloc blocA = a.bloc();
        ArmyBloc blocB = b.bloc();
        blocA.addStrength(-lossA);
        blocB.addStrength(-lossB);

        // Fighting consumes rations.
        if ((blocA.food() > 0 || blocB.food() > 0)) {
            blocA.addFood(-1);
            blocB.addFood(-1);
        }

        checkElimination(a, events);
        checkElimination(b, events);

        // A pitched battle tramples woodland underfoot.
        int bx = clampX(g, a.x());
        int by = clampY(g, a.y());
        Tile t = g.tile(bx, by);
        if (t != null && t.terrain() == TerrainType.FOREST && t.hasTrees()) {
            if (g.deforest(bx, by)) {
                // Trampled ground is poorer and slower.
                t.setFoodPotential(Math.max(0, t.foodPotential() - 20));
                events.add("Woodland trampled in battle at (" + bx + "," + by + ")");
            }
        }
    }

    private void checkElimination(Captain c, List<String> events) {
        if (c.alive() && c.strength() <= 0) {
            c.setAlive(false);
            events.add("Captain " + c.name() + " (" + c.allegiance() + ") eliminated");
        }
    }

    private int elevationAt(MapGrid g, double x, double y) {
        Tile t = g.tile(clampX(g, x), clampY(g, y));
        return t == null ? 0 : t.elevation();
    }

    private int clampX(MapGrid g, double x) {
        int xi = (int) Math.round(x);
        if (xi < 0) {
            xi = 0;
        }
        if (xi >= g.width()) {
            xi = g.width() - 1;
        }
        return xi;
    }

    private int clampY(MapGrid g, double y) {
        int yi = (int) Math.round(y);
        if (yi < 0) {
            yi = 0;
        }
        if (yi >= g.height()) {
            yi = g.height() - 1;
        }
        return yi;
    }
}
