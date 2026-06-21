package com.whim.startrek.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * One cell of the galaxy grid. Holds terrain, an optional star system, any number
 * of fleets, and an optional wormhole link to another cell.
 */
public class GridCell {

    private final int row;
    private final int col;
    private MapObjectType type;
    private StarSystem system; // null if none
    private final List<Fleet> fleets = new ArrayList<Fleet>();

    private int wormholeLinkRow = -1; // -1 if none
    private int wormholeLinkCol = -1;

    public GridCell(int row, int col, MapObjectType type) {
        this.row = row;
        this.col = col;
        this.type = type == null ? MapObjectType.EMPTY : type;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public MapObjectType getType() {
        return type;
    }

    public void setType(MapObjectType t) {
        this.type = t == null ? MapObjectType.EMPTY : t;
    }

    public StarSystem getSystem() {
        return system;
    }

    public void setSystem(StarSystem s) {
        this.system = s; // null if none
    }

    /** Multiple fleets per cell allowed. */
    public List<Fleet> getFleets() {
        return fleets;
    }

    public int getWormholeLinkRow() {
        return wormholeLinkRow;
    }

    public int getWormholeLinkCol() {
        return wormholeLinkCol;
    }

    public void setWormholeLink(int row, int col) {
        this.wormholeLinkRow = row;
        this.wormholeLinkCol = col;
    }
}
