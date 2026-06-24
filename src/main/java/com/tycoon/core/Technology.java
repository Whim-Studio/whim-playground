package com.tycoon.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Chronological tech tree of game engines, modelled after Mad Games Tycoon's
 * progression from primitive text/sprite engines through to modern real-time
 * rendering. A technology becomes available once enough in-game weeks have
 * elapsed ({@link #unlockWeek()}); using a more advanced engine raises the
 * production-quality ceiling ({@link #qualityBonus()}).
 *
 * <p>The list is ordered oldest-first, matching the order players unlock them.</p>
 */
public enum Technology {
    TEXT_PARSER("Text Parser Engine", 0, 1.00),
    SPRITE_2D("2D Sprite Engine", 8, 1.05),
    ISOMETRIC("Isometric Engine", 26, 1.10),
    POLYGON_3D("Early 3D Polygon Engine", 52, 1.16),
    SHADER("Programmable Shader Engine", 104, 1.22),
    PHYSICS("Physics & Ragdoll Engine", 156, 1.28),
    OPEN_WORLD("Streaming Open-World Engine", 234, 1.34),
    RAY_TRACING("Real-Time Ray-Tracing Engine", 338, 1.40);

    private final String display;
    private final int unlockWeek;
    private final double qualityBonus;

    Technology(String display, int unlockWeek, double qualityBonus) {
        this.display = display;
        this.unlockWeek = unlockWeek;
        this.qualityBonus = qualityBonus;
    }

    public String display() {
        return display;
    }

    /** In-game week (day/7) at which this engine becomes researchable/usable. */
    public int unlockWeek() {
        return unlockWeek;
    }

    /** Multiplier applied to the development-points component of a review. */
    public double qualityBonus() {
        return qualityBonus;
    }

    public boolean isUnlockedAtWeek(int week) {
        return week >= unlockWeek;
    }

    /** Engines available at the given in-game week, oldest first. */
    public static List<Technology> unlockedAtWeek(int week) {
        List<Technology> out = new ArrayList<Technology>();
        for (Technology t : values()) {
            if (t.isUnlockedAtWeek(week)) {
                out.add(t);
            }
        }
        return out;
    }

    /** The most advanced engine available at the given in-game week. */
    public static Technology bestAtWeek(int week) {
        Technology best = TEXT_PARSER;
        for (Technology t : values()) {
            if (t.isUnlockedAtWeek(week)) {
                best = t;
            }
        }
        return best;
    }

    @Override
    public String toString() {
        return display;
    }
}
