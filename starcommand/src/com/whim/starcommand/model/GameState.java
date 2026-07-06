package com.whim.starcommand.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The full serializable snapshot of a play session: crew, ship, credits,
 * galaxy map, missions and the player's position. Saved/loaded wholesale.
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int GALAXY_W = 8;
    public static final int GALAXY_H = 6;

    public final List<Character> crew = new ArrayList<Character>();
    public Ship ship;
    public int credits = 5000;

    public Sector[][] galaxy = new Sector[GALAXY_W][GALAXY_H];
    public int shipX = 0;
    public int shipY = 0;

    public final List<Mission> missions = new ArrayList<Mission>();
    public int turn = 1;
    public boolean gameWon = false;

    public Sector currentSector() {
        return galaxy[shipX][shipY];
    }

    public int livingCrew() {
        int n = 0;
        for (Character c : crew) if (c.alive) n++;
        return n;
    }
}
