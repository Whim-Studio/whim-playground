package com.whim.civ.engine;

import com.whim.civ.domain.Building;
import com.whim.civ.domain.City;
import com.whim.civ.domain.Civilization;
import com.whim.civ.domain.GameMap;
import com.whim.civ.domain.GameState;
import com.whim.civ.domain.TechType;
import com.whim.civ.domain.Terrain;
import com.whim.civ.domain.Tile;
import com.whim.civ.domain.Unit;
import com.whim.civ.domain.UnitType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rival (non-human) AI. One {@link #takeTurn} runs a civ's full turn purely through domain
 * mutators: it keeps research going, expands with settlers, moves and attacks with military
 * units, and sets city production.
 *
 * <p>Diplomacy note: the domain carries no explicit treaty state, so "war/peace" is implicit
 * — the AI engages any adjacent enemy (a state of war) and otherwise leaves units that have
 * no enemy in reach to explore (de-facto peace). {@link #peaceful} centralises that decision
 * so it can be tuned without touching the movement code.
 */
public final class AIController {

    private static final int MIN_CITY_SPACING = 3;

    private final Random rng;
    private final CombatResolver combat;
    private final ResearchEngine research;

    public AIController(Random rng) {
        this.rng = rng;
        this.combat = new CombatResolver(rng);
        this.research = new ResearchEngine();
    }

    public void takeTurn(GameState s, Civilization civ) {
        if (!civ.isAlive()) {
            return;
        }
        manageResearch(civ);

        // Iterate over a snapshot: founding/combat mutates the live unit list.
        for (Unit u : new ArrayList<Unit>(s.unitsOf(civ.getId()))) {
            if (u.isAlive() && u.getOwnerCivId() == civ.getId()) {
                handleUnit(s, civ, u);
            }
        }

        for (City c : s.citiesOf(civ.getId())) {
            ensureProduction(s, civ, c);
        }
    }

    private void manageResearch(Civilization civ) {
        if (civ.getResearching() == null) {
            List<TechType> options = research.researchable(civ);
            if (!options.isEmpty()) {
                civ.setResearching(options.get(rng.nextInt(options.size())));
            }
        }
    }

    private void handleUnit(GameState s, Civilization civ, Unit u) {
        if (u.getType().canFound()) {
            if (shouldFound(s, u)) {
                foundCity(s, civ, u);
            } else {
                moveRandom(s, u);
            }
            return;
        }

        if (u.getType().getAttack() > 0) {
            Unit target = adjacentEnemy(s, civ, u);
            if (target != null && !peaceful(s, civ, target)) {
                attack(s, civ, u, target);
                return;
            }
        }
        moveRandom(s, u);
    }

    /** War/peace decision hook. Currently the AI always fights a reachable enemy. */
    private boolean peaceful(GameState s, Civilization civ, Unit enemy) {
        return false;
    }

    private boolean shouldFound(GameState s, Unit u) {
        Tile t = s.getMap().getTile(u.getX(), u.getY());
        if (t.getTerrain() == Terrain.OCEAN || t.getTerrain() == Terrain.ARCTIC) {
            return false;
        }
        if (s.cityAt(u.getX(), u.getY()) != null) {
            return false;
        }
        for (City c : s.getCities()) {
            if (chebyshev(c.getX(), c.getY(), u.getX(), u.getY()) < MIN_CITY_SPACING) {
                return false;
            }
        }
        return true;
    }

    private void foundCity(GameState s, Civilization civ, Unit settler) {
        City city = new City(civ.getId(), "City " + (s.getCities().size() + 1),
                settler.getX(), settler.getY());
        city.setPopulation(1);
        // Young cities crank out a defender first.
        city.setProducingUnit(UnitType.MILITIA);
        s.getCities().add(city);
        s.getMap().getTile(settler.getX(), settler.getY()).setOwnerCivId(civ.getId());
        s.getUnits().remove(settler);
    }

    private Unit adjacentEnemy(GameState s, Civilization civ, Unit u) {
        for (int[] xy : s.getMap().neighbors(u.getX(), u.getY())) {
            for (Unit other : s.unitsAt(xy[0], xy[1])) {
                if (other.isAlive() && other.getOwnerCivId() != civ.getId()) {
                    return other;
                }
            }
        }
        return null;
    }

    private void attack(GameState s, Civilization civ, Unit attacker, Unit defender) {
        GameMap map = s.getMap();
        Terrain terrain = map.getTile(defender.getX(), defender.getY()).getTerrain();
        City defCity = s.cityAt(defender.getX(), defender.getY());
        boolean walls = defCity != null && defCity.getBuildings().contains(Building.CITY_WALLS);

        boolean won = combat.resolveCombat(attacker, defender, terrain,
                defender.isFortified(), walls);

        if (won) {
            if (!defender.isAlive()) {
                s.getUnits().remove(defender);
            }
            // Advance into the tile only if it has been cleared of enemies.
            boolean clear = true;
            for (Unit remaining : s.unitsAt(defender.getX(), defender.getY())) {
                if (remaining.getOwnerCivId() != civ.getId()) {
                    clear = false;
                    break;
                }
            }
            if (clear && attacker.getMovesLeft() > 0) {
                attacker.setPosition(defender.getX(), defender.getY());
                attacker.setMovesLeft(attacker.getMovesLeft() - 1);
            }
        } else if (!attacker.isAlive()) {
            s.getUnits().remove(attacker);
        }
    }

    private void moveRandom(GameState s, Unit u) {
        if (u.getMovesLeft() <= 0) {
            return;
        }
        GameMap map = s.getMap();
        List<int[]> options = new ArrayList<int[]>();
        for (int[] xy : map.neighbors(u.getX(), u.getY())) {
            Terrain terrain = map.getTile(xy[0], xy[1]).getTerrain();
            // Land units avoid stepping into ocean.
            if (terrain != Terrain.OCEAN) {
                options.add(xy);
            }
        }
        if (options.isEmpty()) {
            return;
        }
        int[] dest = options.get(rng.nextInt(options.size()));
        u.setPosition(dest[0], dest[1]);
        u.setMovesLeft(u.getMovesLeft() - 1);
    }

    /** Choose a production order for a city that has none. */
    private void ensureProduction(GameState s, Civilization civ, City c) {
        if (c.getProducingUnit() != null || c.getProducingBuilding() != null) {
            return;
        }
        int cities = s.citiesOf(civ.getId()).size();
        int settlers = 0;
        for (Unit u : s.unitsOf(civ.getId())) {
            if (u.getType().isSettler()) {
                settlers++;
            }
        }

        // Keep expanding while the empire is small and the city can support a settler.
        if (cities < 6 && settlers < cities && c.getPopulation() >= 2) {
            c.setProducingUnit(UnitType.SETTLERS);
            return;
        }

        // Otherwise prefer the best defender we can build, else fall back to a Temple/Militia.
        UnitType defender = bestDefender(civ);
        if (defender != null) {
            c.setProducingUnit(defender);
        } else if (canBuild(civ, Building.TEMPLE)) {
            c.setProducingBuilding(Building.TEMPLE);
        } else {
            c.setProducingUnit(UnitType.MILITIA);
        }
    }

    private UnitType bestDefender(Civilization civ) {
        UnitType best = UnitType.MILITIA;
        UnitType[] ladder = new UnitType[]{UnitType.PHALANX, UnitType.MUSKETEER};
        for (UnitType t : ladder) {
            if (t.getPrereq() == null || civ.knows(t.getPrereq())) {
                if (t.getDefense() >= best.getDefense()) {
                    best = t;
                }
            }
        }
        return best;
    }

    private boolean canBuild(Civilization civ, Building b) {
        return b.getPrereq() == null || civ.knows(b.getPrereq());
    }

    private int chebyshev(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }
}
