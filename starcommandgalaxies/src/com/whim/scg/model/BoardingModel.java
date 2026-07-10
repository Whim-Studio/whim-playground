package com.whim.scg.model;

import com.whim.scg.api.Enums;
import com.whim.scg.api.GridPos;
import com.whim.scg.api.Views;

import java.util.ArrayList;
import java.util.List;

/** Mutable boarding / away-mission tile grid. */
public final class BoardingModel implements Views.BoardingView {
    public int gridW, gridH;
    public Enums.TileType[][] tiles; // [x][y]
    public final List<CrewModel> friendlies = new ArrayList<CrewModel>();
    public final List<CrewModel> hostiles = new ArrayList<CrewModel>();
    public int selectedCrewId = -1;
    public boolean over;
    public boolean playerWon;
    public String objective = "Eliminate all hostiles";
    public GridPos objectivePos;
    /** enemy ship this party boarded (for salvage / combat cleanup). */
    public ShipModel enemyShip;

    public boolean inBounds(GridPos p) {
        return p != null && p.x >= 0 && p.x < gridW && p.y >= 0 && p.y < gridH;
    }

    public boolean walkable(GridPos p) {
        if (!inBounds(p)) return false;
        Enums.TileType t = tiles[p.x][p.y];
        return t != Enums.TileType.WALL;
    }

    public CrewModel friendlyById(int id) {
        for (CrewModel c : friendlies) if (c.id == id) return c;
        return null;
    }

    public CrewModel occupant(GridPos p) {
        for (CrewModel c : friendlies) if (c.alive() && p.equals(c.boardingPos)) return c;
        for (CrewModel c : hostiles) if (c.alive() && p.equals(c.boardingPos)) return c;
        return null;
    }

    @Override public int gridW() { return gridW; }
    @Override public int gridH() { return gridH; }

    @Override public Enums.TileType tileAt(GridPos p) {
        if (!inBounds(p)) return Enums.TileType.WALL;
        return tiles[p.x][p.y];
    }

    @Override public List<Views.CrewView> friendlies() {
        List<Views.CrewView> out = new ArrayList<Views.CrewView>(friendlies);
        return out;
    }
    @Override public List<Views.CrewView> hostiles() {
        List<Views.CrewView> out = new ArrayList<Views.CrewView>(hostiles);
        return out;
    }

    @Override public int selectedCrewId() { return selectedCrewId; }
    @Override public boolean over() { return over; }
    @Override public boolean playerWon() { return playerWon; }
    @Override public String objective() { return objective; }
}
