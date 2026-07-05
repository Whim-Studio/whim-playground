package com.whim.populous.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.GodPower;
import com.whim.populous.api.Views.FollowerView;
import com.whim.populous.api.Views.GameStateView;
import com.whim.populous.api.Views.MapView;
import com.whim.populous.api.Views.PapalMagnetView;

/**
 * The full mutable game state, implementing the read-only {@link GameStateView}
 * the UI renders through. Holds the map, the follower list, per-side mana and
 * population, the armed power, both papal magnets, and end-of-game status.
 *
 * <p>All the read accessors are plain field reads so a snapshot is cheap; the
 * engine performs its mutations on its own thread and then re-reads. The one
 * collection accessor, {@link #followers()}, returns an unmodifiable copy so a
 * render pass cannot tear against a concurrent add/remove.
 */
public final class GameState implements GameStateView {

    private final MapGrid map;
    private final List<Follower> followers = new ArrayList<Follower>();

    private final PapalMagnet goodMagnet = new PapalMagnet(Allegiance.GOOD);
    private final PapalMagnet evilMagnet = new PapalMagnet(Allegiance.EVIL);

    private int goodMana;
    private int evilMana;
    private int maxMana;
    private int populationCap;

    private int goodPopulation;
    private int evilPopulation;

    private GodPower selectedPower = GodPower.RAISE_LAND;

    private boolean gameOver;
    private Allegiance winner = Allegiance.NEUTRAL;
    private long tick;
    private String statusLine = "";

    public GameState(MapGrid map, int maxMana, int populationCap) {
        this.map = map;
        this.maxMana = maxMana;
        this.populationCap = populationCap;
    }

    // ---- GameStateView ------------------------------------------------------

    @Override public MapView map() { return map; }

    @Override
    public List<FollowerView> followers() {
        List<FollowerView> copy = new ArrayList<FollowerView>(followers.size());
        for (int i = 0; i < followers.size(); i++) {
            copy.add(followers.get(i));
        }
        return Collections.unmodifiableList(copy);
    }

    @Override public int goodMana() { return goodMana; }
    @Override public int evilMana() { return evilMana; }
    @Override public int maxMana() { return maxMana; }

    @Override public int goodPopulation() { return goodPopulation; }
    @Override public int evilPopulation() { return evilPopulation; }
    @Override public int populationCap() { return populationCap; }

    @Override public GodPower selectedPower() { return selectedPower; }

    @Override
    public boolean powerAffordable(GodPower p) {
        int mana = goodMana; // the player is GOOD
        return mana >= p.manaCost();
    }

    @Override public PapalMagnetView goodMagnet() { return goodMagnet; }
    @Override public PapalMagnetView evilMagnet() { return evilMagnet; }

    @Override public boolean gameOver() { return gameOver; }
    @Override public Allegiance winner() { return winner; }
    @Override public long tick() { return tick; }
    @Override public String statusLine() { return statusLine; }

    // ---- concrete accessors for the engine ---------------------------------

    public MapGrid grid() { return map; }

    /** Live, mutable follower list (engine owns concurrency around it). */
    public List<Follower> followerList() { return followers; }

    public PapalMagnet goodMagnetRef() { return goodMagnet; }
    public PapalMagnet evilMagnetRef() { return evilMagnet; }

    public PapalMagnet magnetFor(Allegiance side) {
        return side == Allegiance.EVIL ? evilMagnet : goodMagnet;
    }

    // ---- mutation -----------------------------------------------------------

    public void setGoodMana(int m) { this.goodMana = clampMana(m); }
    public void setEvilMana(int m) { this.evilMana = clampMana(m); }
    public void addGoodMana(int d) { this.goodMana = clampMana(this.goodMana + d); }
    public void addEvilMana(int d) { this.evilMana = clampMana(this.evilMana + d); }

    public void addMana(Allegiance side, int d) {
        if (side == Allegiance.EVIL) {
            addEvilMana(d);
        } else if (side == Allegiance.GOOD) {
            addGoodMana(d);
        }
    }

    public int manaFor(Allegiance side) {
        return side == Allegiance.EVIL ? evilMana : goodMana;
    }

    public void setMaxMana(int m) { this.maxMana = m; }
    public void setPopulationCap(int cap) { this.populationCap = cap; }

    public void setGoodPopulation(int p) { this.goodPopulation = p; }
    public void setEvilPopulation(int p) { this.evilPopulation = p; }

    public void setSelectedPower(GodPower p) { this.selectedPower = p; }

    public void setGameOver(boolean over) { this.gameOver = over; }
    public void setWinner(Allegiance w) { this.winner = w; }
    public void setTick(long t) { this.tick = t; }
    public void incrementTick() { this.tick++; }
    public void setStatusLine(String s) { this.statusLine = s; }

    private int clampMana(int m) {
        if (m < 0) {
            return 0;
        }
        return m > maxMana ? maxMana : m;
    }
}
