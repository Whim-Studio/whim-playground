package com.whim.stars.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The root of the entire game state: the universe dimensions, every planet,
 * player and fleet, and the current game year. A single {@code Galaxy} object
 * graph is what gets serialized to a save file.
 */
public final class Galaxy implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Square universe sizes (width in light-years) with typical planet counts. */
    public enum UniverseSize {
        TINY(400, 24),
        SMALL(800, 60),
        MEDIUM(1200, 128),
        LARGE(1600, 240),
        HUGE(2000, 400);

        private final int width;
        private final int typicalPlanets;
        UniverseSize(int width, int typicalPlanets) {
            this.width = width;
            this.typicalPlanets = typicalPlanets;
        }
        public int width() { return width; }
        public int typicalPlanets() { return typicalPlanets; }
    }

    /** First game year in Stars!. */
    public static final int START_YEAR = 2400;

    private final UniverseSize size;
    private int year = START_YEAR;

    private final Map<Integer, Planet> planets = new LinkedHashMap<Integer, Planet>();
    private final List<Player> players = new ArrayList<Player>();
    private final Map<Integer, Fleet> fleets = new LinkedHashMap<Integer, Fleet>();

    private int nextFleetId = 1;

    public Galaxy(UniverseSize size) {
        this.size = size;
    }

    public UniverseSize size() { return size; }
    public int width() { return size.width(); }

    public int year() { return year; }
    public void setYear(int year) { this.year = year; }
    public void advanceYear() { this.year++; }

    // --- Planets ---
    public void addPlanet(Planet planet) {
        planets.put(planet.id(), planet);
    }
    public Planet planet(int id) {
        return planets.get(id);
    }
    public List<Planet> planets() {
        return Collections.unmodifiableList(new ArrayList<Planet>(planets.values()));
    }
    public List<Planet> planetsOf(Player player) {
        List<Planet> owned = new ArrayList<Planet>();
        for (Planet p : planets.values()) {
            if (p.ownerId() == player.id()) {
                owned.add(p);
            }
        }
        return owned;
    }

    // --- Players ---
    public void addPlayer(Player player) {
        players.add(player);
    }
    public List<Player> players() {
        return Collections.unmodifiableList(players);
    }
    public Player player(int id) {
        for (Player p : players) {
            if (p.id() == id) return p;
        }
        return null;
    }

    // --- Fleets ---
    /** Create, register and return a new fleet with an allocated id. */
    public Fleet newFleet(int ownerId, String name, double x, double y) {
        Fleet f = new Fleet(nextFleetId++, ownerId, name, x, y);
        fleets.put(f.id(), f);
        return f;
    }
    public void addFleet(Fleet fleet) {
        fleets.put(fleet.id(), fleet);
        if (fleet.id() >= nextFleetId) {
            nextFleetId = fleet.id() + 1;
        }
    }
    public void removeFleet(Fleet fleet) {
        fleets.remove(fleet.id());
    }
    public Fleet fleet(int id) {
        return fleets.get(id);
    }
    public List<Fleet> fleets() {
        return Collections.unmodifiableList(new ArrayList<Fleet>(fleets.values()));
    }
    public List<Fleet> fleetsOf(Player player) {
        List<Fleet> owned = new ArrayList<Fleet>();
        for (Fleet f : fleets.values()) {
            if (f.ownerId() == player.id()) {
                owned.add(f);
            }
        }
        return owned;
    }
}
