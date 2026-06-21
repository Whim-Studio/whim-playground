package com.whim.startrek.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * A group of ships that moves as one stack on the TBS grid. Many fleets may occupy
 * the same {@link GridCell}. A fleet counts as cloaked only when every cloak-capable
 * ship aboard is currently cloaked.
 */
public class Fleet {

    private final int id;
    private final Race owner;
    private final List<Ship> ships = new ArrayList<Ship>();

    private int row = -1;
    private int col = -1;
    private int destRow = -1;
    private int destCol = -1;

    public Fleet(int id, Race owner) {
        this.id = id;
        this.owner = owner;
    }

    public int getId() {
        return id;
    }

    public Race getOwner() {
        return owner;
    }

    public List<Ship> getShips() {
        return ships;
    }

    public void addShip(Ship s) {
        if (s != null) {
            ships.add(s);
        }
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setCell(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getDestRow() {
        return destRow;
    }

    public int getDestCol() {
        return destCol;
    }

    public void setDestination(int row, int col) {
        this.destRow = row;
        this.destCol = col;
    }

    /**
     * True if every cloak-capable ship is cloaked. A fleet with no cloak-capable
     * ships is not considered cloaked.
     */
    public boolean isCloaked() {
        boolean anyCloakable = false;
        for (Ship s : ships) {
            if (s.isCloakCapable()) {
                anyCloakable = true;
                if (!s.isCloaked()) {
                    return false;
                }
            }
        }
        return anyCloakable;
    }

    /** Sum of officers required to crew every ship in the fleet. */
    public int totalOfficersRequired() {
        int total = 0;
        for (Ship s : ships) {
            total += s.getOfficersRequired();
        }
        return total;
    }

    public boolean isEmpty() {
        return ships.isEmpty();
    }
}
