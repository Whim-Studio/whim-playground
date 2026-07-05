package com.whim.populous.api;

import java.util.List;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.GodPower;
import com.whim.populous.api.Enums.SettlementType;
import com.whim.populous.api.Enums.TerrainType;

/**
 * Read-only projection interfaces. The UI renders EXCLUSIVELY through these and
 * never casts a *View down to a concrete domain class. Task 1's domain classes
 * implement these interfaces; the engine mutates the domain, the UI re-reads.
 *
 * Coordinate convention across the whole project:
 *   col = x axis (0..cols-1), row = y axis (0..rows-1). Follower positions are
 *   floating point in TILE units so movement can be sub-tile-smooth.
 */
public final class Views {

    private Views() { }

    /** One terrain cell. */
    public interface TileView {
        int col();
        int row();
        /** Signed integer height. May be negative (below sea level). */
        int elevation();
        TerrainType terrain();
        /** Owner of the settlement on this tile (NEUTRAL if none). */
        Allegiance owner();
        SettlementType settlement();
        /** 0 if no settlement, else a small level index within the tier. */
        int settlementLevel();
    }

    /** The whole landscape. */
    public interface MapView {
        int cols();
        int rows();
        TileView tileAt(int col, int row);
        /** Sea level reference used by the renderer for water shading. */
        int seaLevel();
    }

    /** A single walker. */
    public interface FollowerView {
        double x();          // tile-space x (col units, fractional)
        double y();          // tile-space y (row units, fractional)
        Allegiance allegiance();
        int health();        // 0..100
        int stamina();       // 0..100
        boolean alive();
    }

    /** The papal magnet / rally point for one side. */
    public interface PapalMagnetView {
        boolean active();
        int col();
        int row();
        Allegiance side();
    }

    /** Everything the UI needs for one frame. Snapshot-consistent per read. */
    public interface GameStateView {
        MapView map();
        List<FollowerView> followers();

        int goodMana();
        int evilMana();
        int maxMana();            // full-bar reference for the mana meter

        int goodPopulation();
        int evilPopulation();
        int populationCap();      // per-side soft cap

        GodPower selectedPower(); // currently armed player power
        boolean powerAffordable(GodPower p);

        PapalMagnetView goodMagnet();
        PapalMagnetView evilMagnet();

        boolean gameOver();
        Allegiance winner();      // NEUTRAL while in progress
        long tick();              // engine tick counter
        String statusLine();      // short human-readable status for the HUD
    }
}
