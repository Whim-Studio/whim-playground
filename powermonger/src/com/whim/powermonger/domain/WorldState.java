package com.whim.powermonger.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Enums.CommandType;
import com.whim.powermonger.api.Enums.Job;
import com.whim.powermonger.api.Enums.Posture;
import com.whim.powermonger.api.Enums.Season;
import com.whim.powermonger.api.Enums.TerrainType;
import com.whim.powermonger.api.Enums.Weather;
import com.whim.powermonger.api.Views;

/**
 * The mutable aggregate of all live game state. The engine mutates it on its
 * own thread; {@link #snapshot()} produces an immutable, defensively-copied
 * {@link Views.GameStateView} safe to read on another (EDT) thread.
 */
public final class WorldState {

    public static final int SUPREME_COMMANDER_ID = 0;

    private final MapGrid grid;
    private final List<Town> towns = new ArrayList<Town>();
    private final List<Townsperson> townspeople = new ArrayList<Townsperson>();
    private final List<Captain> captains = new ArrayList<Captain>();
    private final List<Pigeon> pigeons = new ArrayList<Pigeon>();

    private Season season = Season.SPRING;
    private Weather weather = Weather.CLEAR;
    private double movementFactor = 1.0;
    private long tickCount;
    private final BalanceOfPower balance = new BalanceOfPower();
    private String statusMessage = "";
    private boolean gameOver;
    private boolean playerWon;

    public WorldState(MapGrid grid) {
        this.grid = grid;
    }

    // ---- accessors (mutable live state, engine-side) ----
    public MapGrid grid() { return grid; }
    public List<Town> towns() { return towns; }
    public List<Townsperson> townspeople() { return townspeople; }
    public List<Captain> captains() { return captains; }
    public List<Pigeon> pigeons() { return pigeons; }

    public Captain captain(int id) {
        for (int i = 0; i < captains.size(); i++) {
            if (captains.get(i).id() == id) return captains.get(i);
        }
        return null;
    }

    public Captain supremeCommander() {
        for (int i = 0; i < captains.size(); i++) {
            Captain c = captains.get(i);
            if (c.supremeCommander() && c.allegiance() == Allegiance.PLAYER) return c;
        }
        return captain(SUPREME_COMMANDER_ID);
    }

    public BalanceOfPower balance() { return balance; }

    public Season season() { return season; }
    public void setSeason(Season season) { this.season = season; }

    public Weather weather() { return weather; }
    public void setWeather(Weather weather) { this.weather = weather; }

    public double movementFactor() { return movementFactor; }
    public void setMovementFactor(double f) { this.movementFactor = f; }

    public long tickCount() { return tickCount; }
    public void setTickCount(long t) { this.tickCount = t; }
    public void incrementTick() { this.tickCount++; }

    public String statusMessage() { return statusMessage; }
    public void setStatusMessage(String s) { this.statusMessage = s == null ? "" : s; }

    public boolean gameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }

    public boolean playerWon() { return playerWon; }
    public void setPlayerWon(boolean playerWon) { this.playerWon = playerWon; }

    /**
     * Produce a deep, immutable snapshot safe to read from another thread.
     * All elements are copied into immutable view records so a concurrent
     * engine mutation cannot tear a frame the UI is rendering.
     */
    public Views.GameStateView snapshot() {
        int w = grid.width();
        int h = grid.height();
        TileSnap[][] tileSnaps = new TileSnap[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                Tile t = grid.tile(x, y);
                tileSnaps[x][y] = new TileSnap(t.x(), t.y(), t.terrain(), t.elevation(),
                        t.hasTown(), t.townId(), t.hasTrees(), t.foodPotential(), t.snowCovered());
            }
        }

        List<Views.TownView> townViews = new ArrayList<Views.TownView>();
        for (int i = 0; i < towns.size(); i++) {
            Town t = towns.get(i);
            townViews.add(new TownSnap(t.id(), t.tileX(), t.tileY(), t.name(),
                    t.population(), t.allegiance(), t.captured()));
        }

        List<Views.TownspersonView> personViews = new ArrayList<Views.TownspersonView>();
        for (int i = 0; i < townspeople.size(); i++) {
            Townsperson p = townspeople.get(i);
            personViews.add(new PersonSnap(p.id(), p.x(), p.y(), p.job(), p.allegiance()));
        }

        List<Views.CaptainView> capViews = new ArrayList<Views.CaptainView>();
        for (int i = 0; i < captains.size(); i++) {
            Captain c = captains.get(i);
            capViews.add(new CaptainSnap(c.id(), c.name(), c.x(), c.y(), c.allegiance(),
                    c.posture(), c.strength(), c.food(), c.currentOrder(), c.hasDestination(),
                    c.destX(), c.destY(), c.selected(), c.alive(), c.supremeCommander()));
        }

        List<Views.PigeonView> pigeonViews = new ArrayList<Views.PigeonView>();
        for (int i = 0; i < pigeons.size(); i++) {
            Pigeon p = pigeons.get(i);
            pigeonViews.add(new PigeonSnap(p.x(), p.y(), p.targetX(), p.targetY(),
                    p.order(), p.progress()));
        }

        return new StateSnap(w, h, grid.maxElevation(), tileSnaps,
                Collections.unmodifiableList(townViews),
                Collections.unmodifiableList(personViews),
                Collections.unmodifiableList(capViews),
                Collections.unmodifiableList(pigeonViews),
                season, weather, movementFactor, balance.value(),
                tickCount, gameOver, playerWon, statusMessage);
    }

    // ================= immutable snapshot records =================

    private static final class TileSnap implements Views.TileView {
        private final int x, y, elevation, townId, foodPotential;
        private final TerrainType terrain;
        private final boolean town, trees, snow;
        TileSnap(int x, int y, TerrainType terrain, int elevation, boolean town,
                 int townId, boolean trees, int foodPotential, boolean snow) {
            this.x = x; this.y = y; this.terrain = terrain; this.elevation = elevation;
            this.town = town; this.townId = townId; this.trees = trees;
            this.foodPotential = foodPotential; this.snow = snow;
        }
        public int x() { return x; }
        public int y() { return y; }
        public TerrainType terrain() { return terrain; }
        public int elevation() { return elevation; }
        public boolean hasTown() { return town; }
        public int townId() { return townId; }
        public boolean hasTrees() { return trees; }
        public int foodPotential() { return foodPotential; }
        public boolean snowCovered() { return snow; }
    }

    private static final class TownSnap implements Views.TownView {
        private final int id, tileX, tileY, population;
        private final String name;
        private final Allegiance allegiance;
        private final boolean captured;
        TownSnap(int id, int tileX, int tileY, String name, int population,
                 Allegiance allegiance, boolean captured) {
            this.id = id; this.tileX = tileX; this.tileY = tileY; this.name = name;
            this.population = population; this.allegiance = allegiance; this.captured = captured;
        }
        public int id() { return id; }
        public int tileX() { return tileX; }
        public int tileY() { return tileY; }
        public String name() { return name; }
        public int population() { return population; }
        public Allegiance allegiance() { return allegiance; }
        public boolean captured() { return captured; }
    }

    private static final class PersonSnap implements Views.TownspersonView {
        private final int id;
        private final double x, y;
        private final Job job;
        private final Allegiance allegiance;
        PersonSnap(int id, double x, double y, Job job, Allegiance allegiance) {
            this.id = id; this.x = x; this.y = y; this.job = job; this.allegiance = allegiance;
        }
        public int id() { return id; }
        public double x() { return x; }
        public double y() { return y; }
        public Job job() { return job; }
        public Allegiance allegiance() { return allegiance; }
    }

    private static final class CaptainSnap implements Views.CaptainView {
        private final int id, strength, food;
        private final String name;
        private final double x, y, destX, destY;
        private final Allegiance allegiance;
        private final Posture posture;
        private final CommandType currentOrder;
        private final boolean hasDest, selected, alive, supreme;
        CaptainSnap(int id, String name, double x, double y, Allegiance allegiance,
                    Posture posture, int strength, int food, CommandType currentOrder,
                    boolean hasDest, double destX, double destY, boolean selected,
                    boolean alive, boolean supreme) {
            this.id = id; this.name = name; this.x = x; this.y = y; this.allegiance = allegiance;
            this.posture = posture; this.strength = strength; this.food = food;
            this.currentOrder = currentOrder; this.hasDest = hasDest; this.destX = destX;
            this.destY = destY; this.selected = selected; this.alive = alive; this.supreme = supreme;
        }
        public int id() { return id; }
        public String name() { return name; }
        public double x() { return x; }
        public double y() { return y; }
        public Allegiance allegiance() { return allegiance; }
        public Posture posture() { return posture; }
        public int strength() { return strength; }
        public int food() { return food; }
        public CommandType currentOrder() { return currentOrder; }
        public boolean hasDestination() { return hasDest; }
        public double destX() { return destX; }
        public double destY() { return destY; }
        public boolean selected() { return selected; }
        public boolean alive() { return alive; }
        public boolean supremeCommander() { return supreme; }
    }

    private static final class PigeonSnap implements Views.PigeonView {
        private final double x, y, targetX, targetY, progress;
        private final CommandType order;
        PigeonSnap(double x, double y, double targetX, double targetY,
                   CommandType order, double progress) {
            this.x = x; this.y = y; this.targetX = targetX; this.targetY = targetY;
            this.order = order; this.progress = progress;
        }
        public double x() { return x; }
        public double y() { return y; }
        public double targetX() { return targetX; }
        public double targetY() { return targetY; }
        public CommandType order() { return order; }
        public double progress() { return progress; }
    }

    private static final class StateSnap implements Views.GameStateView {
        private final int mapWidth, mapHeight, maxElevation;
        private final TileSnap[][] tiles;
        private final List<Views.TownView> towns;
        private final List<Views.TownspersonView> people;
        private final List<Views.CaptainView> captains;
        private final List<Views.PigeonView> pigeons;
        private final Season season;
        private final Weather weather;
        private final double movementFactor, balance;
        private final long tickCount;
        private final boolean gameOver, playerWon;
        private final String statusMessage;

        StateSnap(int mapWidth, int mapHeight, int maxElevation, TileSnap[][] tiles,
                  List<Views.TownView> towns, List<Views.TownspersonView> people,
                  List<Views.CaptainView> captains, List<Views.PigeonView> pigeons,
                  Season season, Weather weather, double movementFactor, double balance,
                  long tickCount, boolean gameOver, boolean playerWon, String statusMessage) {
            this.mapWidth = mapWidth; this.mapHeight = mapHeight; this.maxElevation = maxElevation;
            this.tiles = tiles; this.towns = towns; this.people = people; this.captains = captains;
            this.pigeons = pigeons; this.season = season; this.weather = weather;
            this.movementFactor = movementFactor; this.balance = balance; this.tickCount = tickCount;
            this.gameOver = gameOver; this.playerWon = playerWon; this.statusMessage = statusMessage;
        }
        public int mapWidth() { return mapWidth; }
        public int mapHeight() { return mapHeight; }
        public int maxElevation() { return maxElevation; }
        public Views.TileView tile(int x, int y) {
            if (x < 0 || y < 0 || x >= mapWidth || y >= mapHeight) return null;
            return tiles[x][y];
        }
        public List<Views.TownView> towns() { return towns; }
        public List<Views.TownspersonView> townspeople() { return people; }
        public List<Views.CaptainView> captains() { return captains; }
        public List<Views.PigeonView> pigeons() { return pigeons; }
        public Season season() { return season; }
        public Weather weather() { return weather; }
        public double movementFactor() { return movementFactor; }
        public double balanceOfPower() { return balance; }
        public long tickCount() { return tickCount; }
        public boolean gameOver() { return gameOver; }
        public boolean playerWon() { return playerWon; }
        public String statusMessage() { return statusMessage; }
    }
}
