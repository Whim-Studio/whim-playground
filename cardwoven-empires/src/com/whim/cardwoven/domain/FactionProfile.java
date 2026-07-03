package com.whim.cardwoven.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.whim.cardwoven.api.Enums.Faction;
import com.whim.cardwoven.api.Enums.VictoryType;

/**
 * Static per-faction tuning: base hand size, starting resources, building
 * cost/yield modifiers, and the victory paths the faction pursues.
 *
 * <ul>
 *   <li><b>Lands of the King</b> — balanced, high draw (hand 6), all-round
 *       victory paths.</li>
 *   <li><b>Babylon</b> — building-focused: cheaper buildings and a per-building
 *       yield bonus; leans expansion / economic.</li>
 *   <li><b>The Unfaithful</b> — powerful cards, extra starting resources, but
 *       Sin cards clog the deck; leans military / dominance.</li>
 * </ul>
 */
public final class FactionProfile {

    private final Faction faction;
    private final int baseHandSize;
    private final int startingGold;
    private final int startingCommand;
    private final int buildingCostModifier; // added to building card cost (may be negative)
    private final int buildingYieldBonus;   // added to each building's per-turn gold
    private final List<VictoryType> pursuedVictories;

    private FactionProfile(Faction faction, int baseHandSize, int startingGold,
                           int startingCommand, int buildingCostModifier,
                           int buildingYieldBonus, List<VictoryType> pursued) {
        this.faction = faction;
        this.baseHandSize = baseHandSize;
        this.startingGold = startingGold;
        this.startingCommand = startingCommand;
        this.buildingCostModifier = buildingCostModifier;
        this.buildingYieldBonus = buildingYieldBonus;
        this.pursuedVictories =
                Collections.unmodifiableList(new ArrayList<VictoryType>(pursued));
    }

    /** The tuning profile for a faction. */
    public static FactionProfile of(Faction faction) {
        if (faction == Faction.BABYLON) {
            List<VictoryType> v = new ArrayList<VictoryType>();
            v.add(VictoryType.EXPANSION);
            v.add(VictoryType.ECONOMIC);
            v.add(VictoryType.FAITH);
            return new FactionProfile(faction, faction.baseHandSize(),
                    6, 2, -1, 1, v);
        }
        if (faction == Faction.THE_UNFAITHFUL) {
            List<VictoryType> v = new ArrayList<VictoryType>();
            v.add(VictoryType.MILITARY);
            v.add(VictoryType.DOMINANCE);
            v.add(VictoryType.ECONOMIC);
            return new FactionProfile(faction, faction.baseHandSize(),
                    8, 3, 0, 0, v);
        }
        // LANDS_OF_THE_KING (balanced, high draw)
        List<VictoryType> v = new ArrayList<VictoryType>();
        v.add(VictoryType.ECONOMIC);
        v.add(VictoryType.MILITARY);
        v.add(VictoryType.EXPANSION);
        v.add(VictoryType.FAITH);
        v.add(VictoryType.DOMINANCE);
        return new FactionProfile(faction, faction.baseHandSize(),
                5, 2, 0, 0, v);
    }

    public Faction faction() { return faction; }
    public int baseHandSize() { return baseHandSize; }
    public int startingGold() { return startingGold; }
    public int startingCommand() { return startingCommand; }
    public int buildingCostModifier() { return buildingCostModifier; }
    public int buildingYieldBonus() { return buildingYieldBonus; }

    public List<VictoryType> pursuedVictories() { return pursuedVictories; }

    @Override
    public String toString() {
        return "FactionProfile(" + faction + ",hand=" + baseHandSize
                + ",gold=" + startingGold + ",cmd=" + startingCommand + ")";
    }
}
