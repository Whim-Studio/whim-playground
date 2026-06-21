package com.whim.startrek.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.whim.startrek.domain.BorgState;
import com.whim.startrek.domain.GalaxyMap;
import com.whim.startrek.domain.GameState;
import com.whim.startrek.domain.GridCell;
import com.whim.startrek.domain.StarSystem;

/**
 * The Borg: a persistent, ever-growing plague.
 *
 * <p>While {@link BorgState#isActive() active} and not yet eradicated, every {@link #step(GameState)}:
 * <ul>
 *   <li>raises intensity by one (it grows every active turn and never falls on its own),</li>
 *   <li>expands the controlled region into neighbouring cells,</li>
 *   <li>assimilates independent star systems it now sits on top of, and</li>
 *   <li>builds Cubes, scaling with intensity.</li>
 * </ul>
 *
 * <p>The Borg <em>never auto-resolves</em>. The only way it goes away is external destruction of all its
 * cubes and controlled cells; once that happens {@link #isEradicated(GameState)} is true and a final
 * {@code step} flips it inactive, where it stays unless something re-seeds it.
 */
public class BorgEngine {

    public BorgEngine() {
    }

    public void step(GameState s) {
        if (s == null) {
            return;
        }
        BorgState borg = s.getBorgState();
        if (borg == null) {
            return;
        }
        if (isEradicated(s)) {
            // Fully destroyed by external force — power it down and leave it down. Intensity is left
            // as-is on purpose: it is the "has manifested" marker that keeps eradication sticky.
            borg.setActive(false);
            return;
        }
        if (!borg.isActive()) {
            return; // dormant; an external trigger (EngineServices) decides when the plague begins
        }

        GalaxyMap map = s.getMap();

        // 1) Intensity always grows while active.
        borg.setIntensity(borg.getIntensity() + 1);

        // 2) Seed the first cell if the collective somehow has none yet.
        List<int[]> cells = borg.getControlledCells();
        if (cells.isEmpty()) {
            int r = map.getRows() / 2;
            int c = map.getCols() / 2;
            cells.add(new int[] { r, c });
            markCell(map, r, c);
        }

        // 3) Expand into the orthogonal neighbours of every currently controlled cell.
        Set<Long> occupied = new HashSet<Long>();
        for (int[] cell : cells) {
            occupied.add(key(cell[0], cell[1]));
        }
        List<int[]> frontier = new ArrayList<int[]>();
        int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] cell : cells) {
            for (int[] d : dirs) {
                int nr = cell[0] + d[0];
                int nc = cell[1] + d[1];
                if (!map.inBounds(nr, nc)) {
                    continue;
                }
                long k = key(nr, nc);
                if (occupied.contains(k)) {
                    continue;
                }
                occupied.add(k);
                frontier.add(new int[] { nr, nc });
            }
        }
        // Spread rate scales with intensity so the plague accelerates.
        int spread = Math.min(frontier.size(), 1 + borg.getIntensity() / 2);
        for (int i = 0; i < spread; i++) {
            int[] nc = frontier.get(i);
            cells.add(nc);
            markCell(map, nc[0], nc[1]);
        }

        // 4) Assimilate independent systems under Borg control.
        for (int[] cell : cells) {
            GridCell gc = map.getCell(cell[0], cell[1]);
            if (gc == null) {
                continue;
            }
            StarSystem sys = gc.getSystem();
            if (sys != null && !sys.isBorgControlled() && sys.getOwner() == null) {
                sys.setBorgControlled(true);
                sys.setOwner(null);
            }
        }

        // 5) Build cubes — scales with intensity and territory.
        int targetCubes = 1 + borg.getIntensity() / 3 + cells.size() / 8;
        if (targetCubes > borg.getCubeCount()) {
            borg.setCubeCount(targetCubes);
        }
    }

    /**
     * The Borg is eradicated only once it has actually manifested ({@code intensity > 0}) and then been
     * stripped of every cube and controlled cell. A fresh, never-awakened {@link BorgState} (intensity 0)
     * is NOT eradicated — it is a future threat still waiting to begin.
     */
    public boolean isEradicated(GameState s) {
        if (s == null) {
            return true;
        }
        BorgState borg = s.getBorgState();
        if (borg == null) {
            return true;
        }
        return borg.getIntensity() > 0 && borg.getCubeCount() <= 0 && borg.getControlledCells().isEmpty();
    }

    private void markCell(GalaxyMap map, int row, int col) {
        GridCell gc = map.getCell(row, col);
        if (gc != null && gc.getSystem() != null && gc.getSystem().getOwner() == null) {
            gc.getSystem().setBorgControlled(true);
        }
    }

    private static long key(int row, int col) {
        return (((long) row) << 32) ^ (col & 0xffffffffL);
    }
}
