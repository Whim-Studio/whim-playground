package com.whim.monopoly.domain;

import com.whim.monopoly.data.BoardData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The canonical US-edition 40-space Monopoly board.
 */
public class StandardBoard implements Board {
    private final List<Space> spaces;
    private final List<RailroadSpace> railroads;
    private final List<UtilitySpace> utilities;

    public StandardBoard() {
        this.spaces = BoardData.buildSpaces();
        if (this.spaces.size() != SIZE) {
            throw new IllegalStateException("Board must have " + SIZE + " spaces, got " + this.spaces.size());
        }
        List<RailroadSpace> rr = new ArrayList<RailroadSpace>();
        List<UtilitySpace> ut = new ArrayList<UtilitySpace>();
        for (Space sp : spaces) {
            if (sp instanceof RailroadSpace) {
                rr.add((RailroadSpace) sp);
            } else if (sp instanceof UtilitySpace) {
                ut.add((UtilitySpace) sp);
            }
        }
        this.railroads = Collections.unmodifiableList(rr);
        this.utilities = Collections.unmodifiableList(ut);
    }

    public Space spaceAt(int index) {
        return spaces.get(index);
    }

    public List<Space> spaces() {
        return spaces;
    }

    public List<RailroadSpace> railroads() {
        return railroads;
    }

    public List<UtilitySpace> utilities() {
        return utilities;
    }

    public List<StreetSpace> streetsInGroup(ColorGroup group) {
        List<StreetSpace> out = new ArrayList<StreetSpace>();
        for (Space sp : spaces) {
            if (sp instanceof StreetSpace && ((StreetSpace) sp).getColorGroup() == group) {
                out.add((StreetSpace) sp);
            }
        }
        return out;
    }

    public int nextRailroadFrom(int index) {
        return nextOfType(index, SpaceType.RAILROAD);
    }

    public int nextUtilityFrom(int index) {
        return nextOfType(index, SpaceType.UTILITY);
    }

    /** Returns the index of the next space of the given type strictly after {@code index}, wrapping around. */
    private int nextOfType(int index, SpaceType type) {
        for (int step = 1; step <= SIZE; step++) {
            int i = (index + step) % SIZE;
            if (spaces.get(i).getType() == type) {
                return i;
            }
        }
        return -1; // unreachable on the standard board
    }
}
