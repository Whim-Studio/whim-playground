package com.whim.b5db.engine;

import com.whim.b5db.model.Card;
import com.whim.b5db.model.ContestType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable per-player state: the four private zones, the running PRESTIGE track,
 * the per-turn INFLUENCE pool, and the four per-turn attribute pools.
 */
public final class PlayerState {

    private final String name;
    private final com.whim.b5db.model.Faction faction;
    private final boolean ai;

    private final List<Card> drawDeck = new ArrayList<>();
    private final List<Card> hand = new ArrayList<>();
    private final List<Card> playArea = new ArrayList<>();
    private final List<Card> commandRow = new ArrayList<>();
    private final List<Card> discard = new ArrayList<>();
    private final List<Card> outOfGame = new ArrayList<>();

    private int influence;
    private int prestige;
    private final Map<ContestType, Integer> pools = new EnumMap<>(ContestType.class);

    public PlayerState(String name, com.whim.b5db.model.Faction faction, boolean ai) {
        this.name = name;
        this.faction = faction;
        this.ai = ai;
        resetPools();
    }

    /** Deep copy for Monte-Carlo rollouts. Cards are shared (immutable flyweights). */
    public PlayerState(PlayerState other) {
        this.name = other.name;
        this.faction = other.faction;
        this.ai = other.ai;
        this.drawDeck.addAll(other.drawDeck);
        this.hand.addAll(other.hand);
        this.playArea.addAll(other.playArea);
        this.commandRow.addAll(other.commandRow);
        this.discard.addAll(other.discard);
        this.outOfGame.addAll(other.outOfGame);
        this.influence = other.influence;
        this.prestige = other.prestige;
        this.pools.putAll(other.pools);
    }

    public String name() { return name; }
    public com.whim.b5db.model.Faction faction() { return faction; }
    public boolean ai() { return ai; }

    public List<Card> drawDeck() { return drawDeck; }
    public List<Card> hand() { return hand; }
    public List<Card> playArea() { return playArea; }
    public List<Card> commandRow() { return commandRow; }
    public List<Card> discard() { return discard; }
    public List<Card> outOfGame() { return outOfGame; }

    public int influence() { return influence; }
    public void addInfluence(int n) { influence += n; }
    public boolean spendInfluence(int n) {
        if (influence < n) return false;
        influence -= n;
        return true;
    }

    public int prestige() { return prestige; }
    public void addPrestige(int n) { prestige += n; }

    public int pool(ContestType t) {
        Integer v = pools.get(t);
        return v == null ? 0 : v;
    }

    public void addPool(ContestType t, int n) {
        pools.put(t, pool(t) + n);
    }

    public boolean spendPool(ContestType t, int n) {
        if (pool(t) < n) return false;
        pools.put(t, pool(t) - n);
        return true;
    }

    /** Reset INFLUENCE and all attribute pools to zero (START_PHASE / CLEANUP). */
    public void resetPools() {
        influence = 0;
        for (ContestType t : ContestType.values()) {
            pools.put(t, 0);
        }
    }

    /** Draw up to {@code n} cards, reshuffling the discard when the deck empties. */
    public int draw(int n, Rng rng) {
        int drawn = 0;
        for (int k = 0; k < n; k++) {
            if (drawDeck.isEmpty()) {
                if (discard.isEmpty()) {
                    break; // genuinely out of cards
                }
                drawDeck.addAll(discard);
                discard.clear();
                rng.shuffle(drawDeck);
            }
            hand.add(drawDeck.remove(drawDeck.size() - 1));
            drawn++;
        }
        return drawn;
    }

    /** Total cards the player owns across every zone (for stats/scoring). */
    public int totalCards() {
        return drawDeck.size() + hand.size() + playArea.size()
                + commandRow.size() + discard.size();
    }
}
