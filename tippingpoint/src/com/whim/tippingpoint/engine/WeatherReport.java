package com.whim.tippingpoint.engine;

import com.whim.tippingpoint.domain.WeatherCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable record of what a resolved Weather Phase did, for the UI log.
 * Holds defensive copies so callers cannot mutate the engine's revealed pile.
 */
public final class WeatherReport {
    private final List<WeatherCard> revealed;
    private final int globalCo2;
    private final List<String> lines;

    public WeatherReport(List<WeatherCard> revealed, int globalCo2, List<String> lines) {
        this.revealed = Collections.unmodifiableList(
                new ArrayList<WeatherCard>(revealed == null ? new ArrayList<WeatherCard>() : revealed));
        this.globalCo2 = globalCo2;
        this.lines = Collections.unmodifiableList(
                new ArrayList<String>(lines == null ? new ArrayList<String>() : lines));
    }

    public List<WeatherCard> getRevealed() {
        return revealed;
    }

    public int getGlobalCo2() {
        return globalCo2;
    }

    /** Human-readable per-player effect lines. */
    public List<String> getLines() {
        return lines;
    }
}
