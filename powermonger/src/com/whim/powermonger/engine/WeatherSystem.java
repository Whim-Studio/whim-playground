package com.whim.powermonger.engine;

import java.util.Random;

import com.whim.powermonger.api.Enums.Season;
import com.whim.powermonger.api.Enums.TerrainType;
import com.whim.powermonger.api.Enums.Weather;
import com.whim.powermonger.domain.MapGrid;
import com.whim.powermonger.domain.Tile;
import com.whim.powermonger.domain.WorldState;

/**
 * Seasons + weather. Advances SPRING&rarr;SUMMER&rarr;AUTUMN&rarr;WINTER on a tick
 * schedule, re-rolls the active {@link Weather} (winter/late-autumn raise snow
 * probability), sets the global movement factor (rain/snow &lt; 1.0) and paints snow
 * cover onto land tiles. Mutates domain state only; call under the engine lock.
 */
public final class WeatherSystem {

    private final int ticksPerSeason;
    private final int weatherRerollTicks;

    public WeatherSystem() {
        this(240, 80);
    }

    public WeatherSystem(int ticksPerSeason, int weatherRerollTicks) {
        this.ticksPerSeason = Math.max(1, ticksPerSeason);
        this.weatherRerollTicks = Math.max(1, weatherRerollTicks);
    }

    /** Advance seasons/weather for the current tick. Returns a season-change note or null. */
    public String tick(WorldState w, Random rng) {
        long t = w.tickCount();
        String note = null;
        if (t > 0 && t % ticksPerSeason == 0) {
            Season next = nextSeason(w.season());
            w.setSeason(next);
            note = "Season turns to " + next;
        }
        if (t % weatherRerollTicks == 0) {
            rollWeather(w, rng);
        }
        applyMovementFactor(w);
        applySnowCover(w);
        return note;
    }

    private static Season nextSeason(Season s) {
        switch (s) {
            case SPRING: return Season.SUMMER;
            case SUMMER: return Season.AUTUMN;
            case AUTUMN: return Season.WINTER;
            case WINTER: return Season.SPRING;
            default: return Season.SPRING;
        }
    }

    private void rollWeather(WorldState w, Random rng) {
        double snowProb;
        double rainProb;
        switch (w.season()) {
            case WINTER: snowProb = 0.60; rainProb = 0.15; break;
            case AUTUMN: snowProb = 0.25; rainProb = 0.40; break;
            case SPRING: snowProb = 0.05; rainProb = 0.45; break;
            case SUMMER: default: snowProb = 0.00; rainProb = 0.20; break;
        }
        double r = rng.nextDouble();
        if (r < snowProb) {
            w.setWeather(Weather.SNOW);
        } else if (r < snowProb + rainProb) {
            w.setWeather(Weather.RAIN);
        } else {
            w.setWeather(Weather.CLEAR);
        }
    }

    private void applyMovementFactor(WorldState w) {
        double f;
        switch (w.weather()) {
            case SNOW: f = 0.50; break;
            case RAIN: f = 0.70; break;
            case CLEAR: default: f = 1.00; break;
        }
        // Winter is sluggish even between storms.
        if (w.season() == Season.WINTER) {
            f *= 0.85;
        }
        w.setMovementFactor(f);
    }

    private void applySnowCover(WorldState w) {
        boolean snowing = w.weather() == Weather.SNOW;
        boolean cold = w.season() == Season.WINTER;
        MapGrid g = w.grid();
        for (int y = 0; y < g.height(); y++) {
            for (int x = 0; x < g.width(); x++) {
                Tile tile = g.tile(x, y);
                if (tile == null) {
                    continue;
                }
                boolean land = tile.terrain() != TerrainType.DEEP_WATER
                        && tile.terrain() != TerrainType.SHALLOW_WATER;
                // Snow settles when it is snowing, or on high winter peaks.
                boolean cover = land && (snowing || (cold && tile.elevation() >= g.maxElevation() - 1));
                tile.setSnow(cover);
            }
        }
    }
}
