package com.whim.scg.model;

import com.whim.scg.api.Enums;
import com.whim.scg.api.Views;

import java.util.ArrayList;
import java.util.List;

/** Root mutable game state; the engine's single source of truth. */
public final class GameState implements Views.GameView {
    public Enums.Mode mode = Enums.Mode.MENU;
    public int credits;
    public int day = 1;
    public String captainName = "Captain";
    public ShipModel playerShip;
    public GalaxyModel galaxy;
    public CombatModel combat;   // null unless SPACE_COMBAT
    public BoardingModel boarding; // null unless BOARDING
    public final List<TechModel> techTree = new ArrayList<TechModel>();
    public final List<String> log = new ArrayList<String>();
    public boolean paused;
    public String flash = "";
    public double flashTime;

    /** monotonically increasing id source for crew/rooms/etc. */
    public int nextId = 1;
    /** deterministic RNG seed persisted across saves. */
    public long seed = 1;
    public boolean bossDefeated;

    public int newId() { return nextId++; }

    public void logLine(String s) {
        log.add(s);
        while (log.size() > 60) log.remove(0);
    }

    public void setFlash(String s) { flash = s; flashTime = 3.0; }

    @Override public Enums.Mode mode() { return mode; }
    @Override public int credits() { return credits; }
    @Override public int day() { return day; }
    @Override public ShipModel playerShip() { return playerShip; }
    @Override public GalaxyModel galaxy() { return galaxy; }
    @Override public CombatModel combat() { return combat; }
    @Override public BoardingModel boarding() { return boarding; }

    @Override public List<Views.TechView> techTree() {
        List<Views.TechView> out = new ArrayList<Views.TechView>(techTree);
        return out;
    }

    @Override public List<String> log() {
        return new ArrayList<String>(log);
    }

    @Override public boolean paused() { return paused; }
    @Override public String flash() { return flash == null ? "" : flash; }
}
