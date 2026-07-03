package com.whim.populous.domain;

import com.whim.populous.api.Enums.SettlementType;

/**
 * Single source of truth mapping the size of a contiguous flat plateau to a
 * {@link SettlementType} tier and its relative population / mana weights.
 *
 * <p>In Populous (1989) a dwelling's size is dictated by how much <em>flat</em>
 * land surrounds it: a lone flat square only supports a tent, whereas a broad
 * flattened plateau lets your people raise a walled castle that breeds many
 * followers and radiates far more manna. This class encodes the CONTRACT table,
 * refined so tiers align on natural plateau footprints (1, a 2x2-ish cluster, a
 * 3x3 block, a large field, then a fortress-scale expanse).
 *
 * <pre>
 *   flat tiles | tier   | pop | mana
 *   1          | TENT   |  1  |  1
 *   2..3       | HUT    |  2  |  2
 *   4..8       | HOUSE  |  4  |  4
 *   9..15      | TOWER  |  7  |  7
 *   16+        | CASTLE | 12  | 12
 * </pre>
 * Bigger/flatter plateaus therefore breed faster and generate more mana/tick.
 */
public final class SettlementRules {

    private SettlementRules() { }

    // Flat-tile lower bounds for each tier (inclusive).
    public static final int HUT_MIN = 2;
    public static final int HOUSE_MIN = 4;
    public static final int TOWER_MIN = 9;
    public static final int CASTLE_MIN = 16;

    /** Choose the settlement tier for a measured flat-plateau size. */
    public static SettlementType tierFor(int flatTiles) {
        if (flatTiles <= 0) {
            return SettlementType.NONE;
        }
        if (flatTiles < HUT_MIN) {
            return SettlementType.TENT;
        }
        if (flatTiles < HOUSE_MIN) {
            return SettlementType.HUT;
        }
        if (flatTiles < TOWER_MIN) {
            return SettlementType.HOUSE;
        }
        if (flatTiles < CASTLE_MIN) {
            return SettlementType.TOWER;
        }
        return SettlementType.CASTLE;
    }

    /** Relative population weight (people this dwelling supports/breeds). */
    public static int populationWeight(SettlementType type) {
        switch (type) {
            case TENT:   return 1;
            case HUT:    return 2;
            case HOUSE:  return 4;
            case TOWER:  return 7;
            case CASTLE: return 12;
            case NONE:
            default:     return 0;
        }
    }

    /** Relative mana weight generated per tick by this dwelling. */
    public static int manaWeight(SettlementType type) {
        // Mana tracks population 1:1 in classic Populous: more worshippers,
        // more belief, more manna. Kept identical so the two scale together.
        return populationWeight(type);
    }

    /**
     * A small level index (1..) within a tier so the renderer can show growth
     * within a stage (e.g. a HOUSE that is nearly a TOWER). 0 means no dwelling.
     */
    public static int levelWithinTier(int flatTiles) {
        SettlementType t = tierFor(flatTiles);
        switch (t) {
            case TENT:   return 1;
            case HUT:    return flatTiles - HUT_MIN + 1;      // 1..2
            case HOUSE:  return flatTiles - HOUSE_MIN + 1;    // 1..5
            case TOWER:  return flatTiles - TOWER_MIN + 1;    // 1..7
            case CASTLE: return Math.min(9, flatTiles - CASTLE_MIN + 1);
            case NONE:
            default:     return 0;
        }
    }
}
