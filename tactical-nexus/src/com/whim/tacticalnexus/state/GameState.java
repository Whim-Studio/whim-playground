package com.whim.tacticalnexus.state;

import com.whim.tacticalnexus.domain.GridMap;
import com.whim.tacticalnexus.domain.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of an entire game: the player plus every floor and the
 * index of the floor the player currently occupies. Mutators return new
 * snapshots that share the unchanged floors.
 */
public final class GameState {
    private final Player player;
    private final List<GridMap> floors;
    private final int floorIndex;

    public GameState(Player player, List<GridMap> floors, int floorIndex) {
        this.player = player;
        this.floors = Collections.unmodifiableList(new ArrayList<GridMap>(floors));
        this.floorIndex = floorIndex;
    }

    public Player player() {
        return player;
    }

    public GridMap currentFloor() {
        return floors.get(floorIndex);
    }

    /** Unmodifiable list of all floors, lowest index first. */
    public List<GridMap> floors() {
        return floors;
    }

    public int floorIndex() {
        return floorIndex;
    }

    public GameState withPlayer(Player p) {
        return new GameState(p, floors, floorIndex);
    }

    public GameState withFloor(int index, GridMap floor) {
        List<GridMap> copy = new ArrayList<GridMap>(floors);
        copy.set(index, floor);
        return new GameState(player, copy, floorIndex);
    }

    public GameState withFloorIndex(int index) {
        return new GameState(player, floors, index);
    }
}
