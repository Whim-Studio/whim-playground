package com.whim.b5wars.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A playable engagement: map size, ship placements, and victory terms. */
public final class Scenario {
    private final String name;
    private final int mapWidth;
    private final int mapHeight;
    private final List<Placement> placements;
    private final VictoryCondition victory;
    private final int turnLimit;

    public Scenario(String name, int mapWidth, int mapHeight,
                    List<Placement> placements,
                    VictoryCondition victory, int turnLimit) {
        this.name = name;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.placements = Collections.unmodifiableList(
                new ArrayList<Placement>(placements == null ? new ArrayList<Placement>() : placements));
        this.victory = victory;
        this.turnLimit = turnLimit;
    }

    public String getName() {
        return name;
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public List<Placement> getPlacements() {
        return placements;
    }

    public VictoryCondition getVictory() {
        return victory;
    }

    public int getTurnLimit() {
        return turnLimit;
    }
}
