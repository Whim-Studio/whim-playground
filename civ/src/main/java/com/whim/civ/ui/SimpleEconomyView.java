package com.whim.civ.ui;

import com.whim.civ.domain.City;
import com.whim.civ.domain.Civilization;
import com.whim.civ.domain.EconomyView;
import com.whim.civ.domain.GameMap;
import com.whim.civ.domain.GameState;
import com.whim.civ.domain.Government;
import com.whim.civ.domain.Tile;

import java.util.List;

/**
 * UI-side fallback implementation of the domain {@link EconomyView}. The orchestrator's
 * {@code Main} normally wires an {@code EconomyEngine}-backed adapter into the screens, but
 * {@link MainFrame}'s contract constructor takes only {@code (GameState, EngineServices)} —
 * so the UI ships this lightweight, engine-free view to keep {@link CityScreen} populated
 * with sensible numbers when run standalone.
 *
 * <p>It sums the city's worked tiles (center + up to population tiles on the fat cross),
 * applies the despotism 3+ penalty and the Republic/Democracy trade bonus, then splits
 * trade by the civ's tax/science/luxury rates. It deliberately omits corruption decay and
 * specialist handling — the real engine owns those. This is display-only and never mutates
 * state.
 */
final class SimpleEconomyView implements EconomyView {

    @Override
    public int food(GameState s, City c) {
        return sum(s, c, 0);
    }

    @Override
    public int shields(GameState s, City c) {
        return sum(s, c, 1);
    }

    @Override
    public int trade(GameState s, City c) {
        return sum(s, c, 2);
    }

    /** which: 0 food, 1 shields, 2 trade. */
    private int sum(GameState s, City c, int which) {
        GameMap map = s.getMap();
        Civilization civ = s.civById(c.getOwnerCivId());
        Government gov = civ != null ? civ.getGovernment() : Government.DESPOTISM;
        List<int[]> work = map.cityWorkTiles(c.getX(), c.getY());

        // Center tile is always worked; remaining citizens each work one tile. Take the
        // best-yielding tiles for the requested resource so the preview is encouraging.
        int citizens = Math.max(1, c.getPopulation());
        int worked = 0;
        int total = 0;
        // Center first.
        total += tileYield(map.getTile(c.getX(), c.getY()), which, gov);
        worked++;
        // Greedy pick of remaining tiles by this resource's yield.
        int[] best = new int[work.size()];
        int n = 0;
        for (int i = 0; i < work.size(); i++) {
            int[] xy = work.get(i);
            if (xy[0] == c.getX() && xy[1] == c.getY()) {
                continue;
            }
            best[n++] = tileYield(map.getTile(xy[0], xy[1]), which, gov);
        }
        // simple insertion sort descending (small arrays)
        for (int i = 1; i < n; i++) {
            int v = best[i];
            int j = i - 1;
            while (j >= 0 && best[j] < v) {
                best[j + 1] = best[j];
                j--;
            }
            best[j + 1] = v;
        }
        for (int i = 0; i < n && worked < citizens; i++) {
            total += best[i];
            worked++;
        }
        return total;
    }

    private int tileYield(Tile t, int which, Government gov) {
        int v;
        if (which == 0) {
            v = t.yieldFood();
        } else if (which == 1) {
            v = t.yieldShields();
        } else {
            v = t.yieldTrade();
            if (gov.hasTradeBonus() && v > 0) {
                v += 1;
            }
        }
        if (gov.appliesDespotismPenalty() && v >= 3) {
            v -= 1;
        }
        return v;
    }

    @Override
    public int[] tradeSplit(Civilization civ, int totalTrade) {
        int tax = totalTrade * civ.getTaxRate() / 100;
        int sci = totalTrade * civ.getScienceRate() / 100;
        int lux = totalTrade - tax - sci;
        return new int[] { tax, sci, lux };
    }
}
