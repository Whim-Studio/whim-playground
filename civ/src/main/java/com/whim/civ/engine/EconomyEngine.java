package com.whim.civ.engine;

import com.whim.civ.domain.Building;
import com.whim.civ.domain.City;
import com.whim.civ.domain.Civilization;
import com.whim.civ.domain.GameMap;
import com.whim.civ.domain.GameState;
import com.whim.civ.domain.Government;
import com.whim.civ.domain.Tile;
import com.whim.civ.domain.Unit;
import com.whim.civ.domain.UnitType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * City economy: food / shields / trade over the 21-tile fat cross, growth, production,
 * trade split and civil disorder.
 *
 * <p>Per-tile yields are read from {@link Tile} (which already reflects improvements), then
 * government tile effects are applied:
 * <ul>
 *   <li><b>Despotism penalty</b> ({@link Government#appliesDespotismPenalty()}): any yield
 *       component producing 3+ loses 1 of that component.</li>
 *   <li><b>Republic/Democracy trade bonus</b> ({@link Government#hasTradeBonus()}): tiles
 *       already producing trade gain +1 trade.</li>
 * </ul>
 * Corruption ({@link Government#getCorruptionPct()}) is applied to the summed city trade.
 *
 * <p>The city works the center tile plus up to {@code getPopulation()} of the best remaining
 * tiles in its work radius (chosen once by combined effective yield, so the three yield
 * methods agree on which tiles are worked).
 */
public final class EconomyEngine {

    public EconomyEngine() {
    }

    private Government governmentOf(GameState s, City c) {
        Civilization civ = s.civById(c.getOwnerCivId());
        return civ != null ? civ.getGovernment() : Government.DESPOTISM;
    }

    /** Per-tile yields after improvements and government tile effects: {food, shields, trade}. */
    private int[] tileYield(Tile t, Government gov) {
        int food = t.yieldFood();
        int shields = t.yieldShields();
        int trade = t.yieldTrade();
        if (gov.appliesDespotismPenalty()) {
            if (food >= 3) {
                food -= 1;
            }
            if (shields >= 3) {
                shields -= 1;
            }
            if (trade >= 3) {
                trade -= 1;
            }
        }
        if (gov.hasTradeBonus() && trade > 0) {
            trade += 1;
        }
        return new int[]{food, shields, trade};
    }

    /** Center tile plus the best up-to-population worked tiles, chosen by combined yield. */
    private List<int[]> workedTiles(GameState s, City c) {
        GameMap map = s.getMap();
        final Government gov = governmentOf(s, c);
        List<int[]> all = map.cityWorkTiles(c.getX(), c.getY());

        int[] center = null;
        List<int[]> others = new ArrayList<int[]>();
        for (int[] xy : all) {
            if (xy[0] == c.getX() && xy[1] == c.getY()) {
                center = xy;
            } else {
                others.add(xy);
            }
        }

        others.sort(new Comparator<int[]>() {
            public int compare(int[] a, int[] b) {
                int[] ya = tileYield(map.getTile(a[0], a[1]), gov);
                int[] yb = tileYield(map.getTile(b[0], b[1]), gov);
                return (yb[0] + yb[1] + yb[2]) - (ya[0] + ya[1] + ya[2]);
            }
        });

        List<int[]> worked = new ArrayList<int[]>();
        if (center != null) {
            worked.add(center);
        }
        int n = Math.min(c.getPopulation(), others.size());
        for (int i = 0; i < n; i++) {
            worked.add(others.get(i));
        }
        return worked;
    }

    public int computeFood(GameState s, City c) {
        Government gov = governmentOf(s, c);
        GameMap map = s.getMap();
        int sum = 0;
        for (int[] xy : workedTiles(s, c)) {
            sum += tileYield(map.getTile(xy[0], xy[1]), gov)[0];
        }
        return sum;
    }

    public int computeShields(GameState s, City c) {
        Government gov = governmentOf(s, c);
        GameMap map = s.getMap();
        int sum = 0;
        for (int[] xy : workedTiles(s, c)) {
            sum += tileYield(map.getTile(xy[0], xy[1]), gov)[1];
        }
        return sum;
    }

    /** Post-corruption total trade for the city. */
    public int computeTrade(GameState s, City c) {
        Government gov = governmentOf(s, c);
        GameMap map = s.getMap();
        int raw = 0;
        for (int[] xy : workedTiles(s, c)) {
            raw += tileYield(map.getTile(xy[0], xy[1]), gov)[2];
        }
        int corruption = raw * gov.getCorruptionPct() / 100;
        return raw - corruption;
    }

    /**
     * Split city trade into {tax, science, luxury} by the civ's percentage rates. Tax and
     * luxury take their floored share; science receives the remainder so the parts always
     * sum back to {@code totalTrade}.
     */
    public int[] splitTrade(Civilization civ, int totalTrade) {
        int tax = totalTrade * civ.getTaxRate() / 100;
        int luxury = totalTrade * civ.getLuxuryRate() / 100;
        int science = totalTrade - tax - luxury;
        return new int[]{tax, science, luxury};
    }

    /**
     * One turn of growth: add {@code (food - 2*population)} to the food store. On reaching the
     * food box {@code (pop+1)*10} the city grows; a negative store starves a citizen.
     */
    public void grow(GameState s, City c) {
        int food = computeFood(s, c);
        int net = food - 2 * c.getPopulation();
        int store = c.getFoodStore() + net;
        int box = c.getFoodBoxSize();

        if (store < 0) {
            // Famine: lose one citizen, empty the food store (never below size 1).
            c.setPopulation(Math.max(1, c.getPopulation() - 1));
            c.setFoodStore(0);
        } else if (store >= box) {
            c.setPopulation(c.getPopulation() + 1);
            c.setFoodStore(store - box);
        } else {
            c.setFoodStore(store);
        }
    }

    /** Add net shields to the store and complete the current production order if affordable. */
    public void produce(GameState s, City c) {
        int store = c.getShieldStore() + computeShields(s, c);
        UnitType producingUnit = c.getProducingUnit();
        Building producingBuilding = c.getProducingBuilding();

        int cost = -1;
        if (producingUnit != null) {
            cost = producingUnit.getCost();
        } else if (producingBuilding != null) {
            cost = producingBuilding.getCost();
        }

        if (cost >= 0 && store >= cost) {
            store -= cost;
            if (producingUnit != null) {
                s.getUnits().add(new Unit(producingUnit, c.getOwnerCivId(), c.getX(), c.getY()));
                // A city keeps cranking out the same unit type until ordered otherwise.
            } else {
                if (!c.getBuildings().contains(producingBuilding)) {
                    c.getBuildings().add(producingBuilding);
                }
                // A building is built once; clear the order so shields don't burn rebuilding it.
                c.setProducingBuilding(null);
            }
        }
        c.setShieldStore(store);
    }

    /**
     * Civil disorder happiness check. Citizens beyond a content base become unhappy; luxury
     * spending, a Temple, and martial-law military units quell unhappiness. The city falls
     * into disorder when unhappy citizens are at least as many as the remaining content ones.
     */
    public boolean computeDisorder(GameState s, City c) {
        int pop = c.getPopulation();
        final int contentBase = 4; // Civ1 default: first few citizens are content.
        int unhappy = Math.max(0, pop - contentBase);
        if (unhappy == 0) {
            return false;
        }

        Civilization civ = s.civById(c.getOwnerCivId());

        // Luxuries: every 2 luxury makes one unhappy citizen content.
        int trade = computeTrade(s, c);
        int luxury = splitTrade(civ, trade)[2];
        unhappy = Math.max(0, unhappy - luxury / 2);

        // Temple settles unhappiness.
        if (c.getBuildings().contains(Building.TEMPLE)) {
            unhappy = Math.max(0, unhappy - 2);
        }

        // Martial law: up to government's allotment of military units quell one unhappy each.
        int martialCap = civ.getGovernment().getMartialLawUnits();
        int military = 0;
        for (Unit u : s.unitsAt(c.getX(), c.getY())) {
            if (u.getOwnerCivId() == c.getOwnerCivId() && u.getType().getAttack() > 0) {
                military++;
            }
        }
        unhappy = Math.max(0, unhappy - Math.min(martialCap, military));

        int content = pop - unhappy;
        return unhappy >= content;
    }
}
