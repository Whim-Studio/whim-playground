package com.whim.civ.domain;

import java.util.ArrayList;
import java.util.List;

/** The full mutable game state: map, civilizations, cities, units, and turn cursor. */
public final class GameState {
    private final GameMap map;
    private final List<Civilization> civilizations = new ArrayList<Civilization>();
    private final List<City> cities = new ArrayList<City>();
    private final List<Unit> units = new ArrayList<Unit>();
    private int activeCivIndex = 0;
    private int year = -4000;
    private int turnNumber = 1;

    public GameState(GameMap map) {
        this.map = map;
    }

    public GameMap getMap() { return map; }
    public List<Civilization> getCivilizations() { return civilizations; }
    public List<City> getCities() { return cities; }
    public List<Unit> getUnits() { return units; }

    public int getActiveCivIndex() { return activeCivIndex; }
    public void setActiveCivIndex(int i) { this.activeCivIndex = i; }

    public int getYear() { return year; }            // negative == B.C.; starts at -4000
    public void setYear(int y) { this.year = y; }

    public int getTurnNumber() { return turnNumber; } // starts at 1
    public void setTurnNumber(int n) { this.turnNumber = n; }

    // convenience lookups

    public List<Unit> unitsAt(int x, int y) {
        List<Unit> result = new ArrayList<Unit>();
        for (Unit u : units) {
            if (u.getX() == x && u.getY() == y) {
                result.add(u);
            }
        }
        return result;
    }

    public City cityAt(int x, int y) {
        for (City c : cities) {
            if (c.getX() == x && c.getY() == y) {
                return c;
            }
        }
        return null;
    }

    public List<City> citiesOf(int civId) {
        List<City> result = new ArrayList<City>();
        for (City c : cities) {
            if (c.getOwnerCivId() == civId) {
                result.add(c);
            }
        }
        return result;
    }

    public List<Unit> unitsOf(int civId) {
        List<Unit> result = new ArrayList<Unit>();
        for (Unit u : units) {
            if (u.getOwnerCivId() == civId) {
                result.add(u);
            }
        }
        return result;
    }

    public Civilization civById(int id) {
        for (Civilization c : civilizations) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
    }
}
