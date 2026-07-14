package com.whim.babylon5.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A faction in play (the user's "Faction"). Owns one {@link Zone} per
 * {@link ZoneType} and tracks influence + power.
 *
 * <p>Rulebook ("Influence" / "Victory"): a faction's Influence Rating starts at
 * 4 and its base Power equals its current Influence Rating. The spendable
 * influence "pool" models influence applied during a turn and restored each
 * Ready round.</p>
 */
public final class PlayerState {

    private final String name;
    private final FactionId faction;
    private final boolean human;

    private int influenceRating = 4;   // rulebook: starts at 4
    private int influencePool = 4;     // spendable; restored to rating each Ready round
    private int power = 4;             // base Power == Influence Rating; engine recomputes

    /**
     * Rulebook ("Conflict Cards"): "Each faction may normally initiate only one conflict
     * per turn." Set when this faction declares a conflict, cleared at the start of its
     * next Conflict round.
     */
    private boolean initiatedConflictThisTurn = false;

    private final Map<ZoneType, Zone> zones = new EnumMap<ZoneType, Zone>(ZoneType.class);

    public PlayerState(String name, FactionId faction, boolean human) {
        this.name = name;
        this.faction = faction;
        this.human = human;
        for (ZoneType t : ZoneType.values()) {
            zones.put(t, new Zone(t));
        }
    }

    public String getName() { return name; }
    public FactionId getFaction() { return faction; }
    public boolean isHuman() { return human; }

    public int getInfluenceRating() { return influenceRating; }
    public void setInfluenceRating(int r) { this.influenceRating = r; }

    public int getInfluencePool() { return influencePool; }
    public void setInfluencePool(int p) { this.influencePool = p; }

    public void adjustInfluencePool(int delta) { this.influencePool += delta; }

    public int getPower() { return power; }
    public void setPower(int power) { this.power = power; }

    public boolean hasInitiatedConflictThisTurn() { return initiatedConflictThisTurn; }
    public void setInitiatedConflictThisTurn(boolean v) { this.initiatedConflictThisTurn = v; }

    public Zone zone(ZoneType t) { return zones.get(t); }

    /**
     * The Ambassador in the Inner Circle, or {@code null} if none. Returns the
     * first {@link CardType#AMBASSADOR} card found in the INNER_CIRCLE zone.
     */
    public Card getAmbassador() {
        List<Card> inner = zones.get(ZoneType.INNER_CIRCLE).getCards();
        for (Card c : inner) {
            if (c.getType() == CardType.AMBASSADOR) return c;
        }
        return null;
    }
}
