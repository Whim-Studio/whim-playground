package com.heroquest.model;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The complete, mutable state of a HeroQuest session. This is the single source of
 * truth read by the UI layer and mutated (only) by the logic layer.
 */
public final class GameState {
    private final DungeonMap map;
    private final List<Hero> heroes = new ArrayList<Hero>();
    private final List<Monster> monsters = new ArrayList<Monster>();
    private final List<String> log = new ArrayList<String>();

    private Deque<Decks.TreasureCard> treasureDeck;

    private Phase phase = Phase.HERO;
    private int activeHeroIndex = 0;

    // Per-hero-turn bookkeeping.
    private int movementRemaining = 0;
    private boolean actionUsed = false;
    private boolean moveRolled = false;

    public GameState(DungeonMap map) {
        this.map = map;
    }

    public DungeonMap getMap() {
        return map;
    }

    public List<Hero> getHeroes() {
        return heroes;
    }

    public List<Monster> getMonsters() {
        return monsters;
    }

    public List<String> getLog() {
        return log;
    }

    public void log(String message) {
        log.add(message);
    }

    public Deque<Decks.TreasureCard> getTreasureDeck() {
        return treasureDeck;
    }

    public void setTreasureDeck(Deque<Decks.TreasureCard> treasureDeck) {
        this.treasureDeck = treasureDeck;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public int getActiveHeroIndex() {
        return activeHeroIndex;
    }

    public void setActiveHeroIndex(int i) {
        this.activeHeroIndex = i;
    }

    /** The Hero currently taking a turn, or null if none are alive. */
    public Hero getActiveHero() {
        List<Hero> living = getLivingHeroes();
        if (living.isEmpty()) {
            return null;
        }
        if (activeHeroIndex < 0 || activeHeroIndex >= heroes.size()) {
            return null;
        }
        Hero h = heroes.get(activeHeroIndex);
        return h.isAlive() ? h : null;
    }

    public int getMovementRemaining() {
        return movementRemaining;
    }

    public void setMovementRemaining(int movementRemaining) {
        this.movementRemaining = movementRemaining;
    }

    public boolean isActionUsed() {
        return actionUsed;
    }

    public void setActionUsed(boolean actionUsed) {
        this.actionUsed = actionUsed;
    }

    public boolean isMoveRolled() {
        return moveRolled;
    }

    public void setMoveRolled(boolean moveRolled) {
        this.moveRolled = moveRolled;
    }

    public List<Hero> getLivingHeroes() {
        List<Hero> out = new ArrayList<Hero>();
        for (Hero h : heroes) {
            if (h.isAlive()) {
                out.add(h);
            }
        }
        return out;
    }

    public List<Monster> getLivingMonsters() {
        List<Monster> out = new ArrayList<Monster>();
        for (Monster m : monsters) {
            if (m.isAlive()) {
                out.add(m);
            }
        }
        return out;
    }

    /** Any living entity standing on the given square, or null. */
    public Entity entityAt(Point p) {
        for (Hero h : heroes) {
            if (h.isAlive() && p.equals(h.getPosition())) {
                return h;
            }
        }
        for (Monster m : monsters) {
            if (m.isAlive() && p.equals(m.getPosition())) {
                return m;
            }
        }
        return null;
    }

    public boolean isOccupied(Point p) {
        return entityAt(p) != null;
    }

    public boolean isVictory() {
        return getLivingMonsters().isEmpty();
    }

    public boolean isDefeat() {
        return getLivingHeroes().isEmpty();
    }
}
