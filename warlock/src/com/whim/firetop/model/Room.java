package com.whim.firetop.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * One tile/room in the dungeon graph. Has a type, a grid position used only for
 * drawing, exits to adjacent rooms, and optional contents (a monster, gold).
 */
public final class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final RoomType type;
    private final int gridX;
    private final int gridY;
    private final List<Integer> exits = new ArrayList<Integer>();
    private final String description;

    private Monster monster; // nullable
    private int gold;
    private boolean visited;
    private boolean resolved; // contents consumed (monster beaten / treasure taken)

    public Room(int id, String name, RoomType type, int gridX, int gridY, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.gridX = gridX;
        this.gridY = gridY;
        this.description = description;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public RoomType getType() { return type; }
    public int getGridX() { return gridX; }
    public int getGridY() { return gridY; }
    public List<Integer> getExits() { return exits; }
    public String getDescription() { return description; }

    public Monster getMonster() { return monster; }
    public void setMonster(Monster monster) { this.monster = monster; }

    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = gold; }

    public boolean isVisited() { return visited; }
    public void setVisited(boolean visited) { this.visited = visited; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    /** Adds a two-way link only from this side (Board wires both directions). */
    public void addExit(int otherId) {
        if (!exits.contains(otherId)) {
            exits.add(otherId);
        }
    }
}
