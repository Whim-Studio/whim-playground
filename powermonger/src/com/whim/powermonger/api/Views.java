package com.whim.powermonger.api;

import java.util.List;

import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Enums.CommandType;
import com.whim.powermonger.api.Enums.Job;
import com.whim.powermonger.api.Enums.Posture;
import com.whim.powermonger.api.Enums.Season;
import com.whim.powermonger.api.Enums.TerrainType;
import com.whim.powermonger.api.Enums.Weather;

/**
 * Read-only view interfaces — the ONLY way the UI (Task 3) reads game state.
 * Task 1 domain classes implement these; the UI never casts a *View back to a
 * concrete domain type. All coordinates are in TILE units; blocs/people use
 * fractional tile coordinates for smooth movement.
 */
public final class Views {
    private Views() {}

    public interface TileView {
        int x();
        int y();
        TerrainType terrain();
        /** Elevation band, 0 (sea level) .. maxElevation(). Drives the 2.5D lift. */
        int elevation();
        boolean hasTown();
        /** Town id if hasTown(), else -1. */
        int townId();
        /** True where trees stand; deforesting clears this. */
        boolean hasTrees();
        /** Harvestable food potential of this tile (farm/fish/graze), 0..100. */
        int foodPotential();
        /** True when the current weather/season has painted this tile with snow. */
        boolean snowCovered();
    }

    public interface TownView {
        int id();
        int tileX();
        int tileY();
        String name();
        int population();
        Allegiance allegiance();
        boolean captured();
    }

    public interface TownspersonView {
        int id();
        double x();
        double y();
        Job job();
        Allegiance allegiance();
    }

    /** An army bloc led by a Captain. Also the selectable/orderable unit. */
    public interface CaptainView {
        int id();
        String name();
        double x();
        double y();
        Allegiance allegiance();
        Posture posture();
        /** Fighting men in the bloc. */
        int strength();
        /** Food carried by the bloc. */
        int food();
        CommandType currentOrder();
        boolean hasDestination();
        double destX();
        double destY();
        boolean selected();
        boolean alive();
        /** True for the player's supreme commander (the one that issues to others). */
        boolean supremeCommander();
    }

    /** A carrier pigeon animating an in-flight order (command lag). */
    public interface PigeonView {
        double x();
        double y();
        double targetX();
        double targetY();
        CommandType order();
        /** 0.0 at launch .. 1.0 on arrival. */
        double progress();
    }

    /** Immutable-ish snapshot the UI renders each frame. */
    public interface GameStateView {
        int mapWidth();
        int mapHeight();
        int maxElevation();
        TileView tile(int x, int y);

        List<TownView> towns();
        List<TownspersonView> townspeople();
        List<CaptainView> captains();
        List<PigeonView> pigeons();

        Season season();
        Weather weather();
        /** Movement multiplier currently imposed by weather/season (<= 1.0). */
        double movementFactor();

        /** Balance of Power in [-1, +1]: +1 = total player victory, -1 = defeat. */
        double balanceOfPower();

        long tickCount();
        boolean gameOver();
        boolean playerWon();
        String statusMessage();
    }
}
