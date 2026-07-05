package com.whim.kenshi.domain;

import com.whim.kenshi.api.Enums.FactionId;
import com.whim.kenshi.api.Enums.Relation;

import java.util.EnumMap;
import java.util.Map;

/**
 * Pairwise faction dispositions plus a player-reputation score per faction.
 * Seeded with sensible Kenshi-flavoured defaults (Holy Nation vs Shek hostile,
 * bandits hostile to everyone, Trade Guild neutral, Drifters neutral).
 *
 * <p>Relations are symmetric: setting A→B also sets B→A.
 */
public final class FactionMatrix {

    private final Map<FactionId, Map<FactionId, Relation>> rel =
            new EnumMap<FactionId, Map<FactionId, Relation>>(FactionId.class);
    private final Map<FactionId, Integer> reputation =
            new EnumMap<FactionId, Integer>(FactionId.class);

    public FactionMatrix() {
        // default everything to NEUTRAL, self to ALLY
        for (FactionId a : FactionId.values()) {
            Map<FactionId, Relation> row = new EnumMap<FactionId, Relation>(FactionId.class);
            for (FactionId b : FactionId.values()) {
                row.put(b, a == b ? Relation.ALLY : Relation.NEUTRAL);
            }
            rel.put(a, row);
            reputation.put(a, 0);
        }
        seedDefaults();
    }

    private void seedDefaults() {
        // The great faction feud.
        set(FactionId.HOLY_NATION, FactionId.SHEK, Relation.HOSTILE);

        // Bandits prey on everyone (including each other).
        FactionId[] bandits = { FactionId.DUST_BANDITS, FactionId.HUNGRY_BANDITS };
        for (FactionId bandit : bandits) {
            for (FactionId other : FactionId.values()) {
                if (other == bandit) continue;
                set(bandit, other, Relation.HOSTILE);
            }
        }
        set(FactionId.DUST_BANDITS, FactionId.HUNGRY_BANDITS, Relation.HOSTILE);

        // Trade Guild keeps the peace with the settled factions; allied to none.
        set(FactionId.TRADE_GUILD, FactionId.HOLY_NATION, Relation.NEUTRAL);
        set(FactionId.TRADE_GUILD, FactionId.SHEK, Relation.NEUTRAL);

        // Drifters are harmless wanderers — neutral to all non-bandits.
        // (bandits already set hostile above)

        // Player starts neutral with the settled world, hostile with bandits.
        set(FactionId.PLAYER, FactionId.DUST_BANDITS, Relation.HOSTILE);
        set(FactionId.PLAYER, FactionId.HUNGRY_BANDITS, Relation.HOSTILE);

        // Starting reputations from the player's perspective.
        reputation.put(FactionId.HOLY_NATION, 5);
        reputation.put(FactionId.SHEK, 0);
        reputation.put(FactionId.TRADE_GUILD, 10);
        reputation.put(FactionId.DRIFTERS, 0);
        reputation.put(FactionId.DUST_BANDITS, -40);
        reputation.put(FactionId.HUNGRY_BANDITS, -30);
        reputation.put(FactionId.PLAYER, 100);
    }

    /** Symmetric set of the relation between two factions. */
    public void set(FactionId a, FactionId b, Relation r) {
        rel.get(a).put(b, r);
        rel.get(b).put(a, r);
    }

    /** Relation of {@code a} toward {@code b}. */
    public Relation relation(FactionId a, FactionId b) {
        return rel.get(a).get(b);
    }

    /** True when two factions are mutually hostile. */
    public boolean isHostile(FactionId a, FactionId b) {
        return relation(a, b) == Relation.HOSTILE;
    }

    /** True when two factions are allied. */
    public boolean isAllied(FactionId a, FactionId b) {
        return relation(a, b) == Relation.ALLY;
    }

    /** Player-facing reputation with a faction, clamped to [-100, 100]. */
    public int reputationWithPlayer(FactionId faction) {
        return reputation.get(faction);
    }

    public void setReputation(FactionId faction, int value) {
        if (value < -100) value = -100;
        if (value > 100) value = 100;
        reputation.put(faction, value);
    }

    public void addReputation(FactionId faction, int delta) {
        setReputation(faction, reputation.get(faction) + delta);
    }
}
