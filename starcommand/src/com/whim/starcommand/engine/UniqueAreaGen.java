package com.whim.starcommand.engine;

import com.whim.starcommand.model.Planet;
import com.whim.starcommand.model.UniqueArea;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Builds a small unique-area complex: a maze of corridors (a randomized spanning
 * tree, so the objective is always reachable, plus a few extra doorways for
 * loops) with the entrance at the near corner, the objective at the far corner,
 * and enemy/loot rooms scattered between. Fog of war reveals along corridors.
 */
public class UniqueAreaGen {

    private final Rng rng;

    public UniqueAreaGen(Rng rng) { this.rng = rng; }

    public UniqueArea generate(Planet planet, boolean boss) {
        int rows = 4, cols = 6;
        UniqueArea area = new UniqueArea(rows, cols);
        area.boss = boss;
        area.title = title(planet, boss);

        carveMaze(area, rows - 1, 0);       // spanning tree rooted at the entrance
        addLoops(area, 3);                  // a few extra doorways so it isn't a pure tree

        // entrance (near corner) and objective (far corner)
        area.pr = rows - 1;
        area.pc = 0;
        UniqueArea.Room entrance = area.at(rows - 1, 0);
        entrance.kind = UniqueArea.Room.Kind.ENTRANCE;
        entrance.visited = true;
        entrance.cleared = true;

        UniqueArea.Room objective = area.at(0, cols - 1);
        objective.kind = UniqueArea.Room.Kind.OBJECTIVE;

        // collect the free interior cells
        List<int[]> free = new ArrayList<int[]>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                UniqueArea.Room room = area.at(r, c);
                if (room.kind == UniqueArea.Room.Kind.EMPTY) free.add(new int[]{r, c});
            }
        shuffle(free);

        int enemies = boss ? 4 : 3;
        int loots = 3;
        int i = 0;
        for (; i < enemies && i < free.size(); i++) {
            area.at(free.get(i)[0], free.get(i)[1]).kind = UniqueArea.Room.Kind.ENEMY;
        }
        for (int k = 0; k < loots && i < free.size(); k++, i++) {
            UniqueArea.Room room = area.at(free.get(i)[0], free.get(i)[1]);
            room.kind = UniqueArea.Room.Kind.LOOT;
            room.loot = 300 + rng.range(1, 8) * 100;
        }

        area.reveal(area.pr, area.pc);
        return area;
    }

    /** Randomized iterative-DFS maze carve, guaranteeing every room is reachable. */
    private void carveMaze(UniqueArea area, int startR, int startC) {
        boolean[][] seen = new boolean[area.rows][area.cols];
        Deque<int[]> stack = new ArrayDeque<int[]>();
        seen[startR][startC] = true;
        stack.push(new int[]{startR, startC});
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!stack.isEmpty()) {
            int[] cur = stack.peek();
            List<int[]> unseen = new ArrayList<int[]>();
            for (int[] d : dirs) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (area.inBounds(nr, nc) && !seen[nr][nc]) unseen.add(new int[]{nr, nc});
            }
            if (unseen.isEmpty()) { stack.pop(); continue; }
            int[] next = unseen.get(rng.nextInt(unseen.size()));
            area.carve(cur[0], cur[1], next[0], next[1]);
            seen[next[0]][next[1]] = true;
            stack.push(next);
        }
    }

    /** Punch a few extra doorways between adjacent rooms to create loops. */
    private void addLoops(UniqueArea area, int count) {
        int[][] dirs = {{1, 0}, {0, 1}};
        int attempts = 0;
        while (count > 0 && attempts++ < 50) {
            int r = rng.nextInt(area.rows);
            int c = rng.nextInt(area.cols);
            int[] d = dirs[rng.nextInt(dirs.length)];
            int nr = r + d[0], nc = c + d[1];
            if (!area.inBounds(nr, nc)) continue;
            if (area.canMove(r, c, nr, nc)) continue; // already open
            area.carve(r, c, nr, nc);
            count--;
        }
    }

    private String title(Planet planet, boolean boss) {
        if (boss) return "PIRATE STRONGHOLD — Blackbeard's Hideout";
        if (planet != null && planet.kind == Planet.Kind.HIVE) return "INSECTOID HIVE — " + planet.name;
        if (planet != null) return "PIRATE BASE — " + planet.name;
        return "UNIQUE AREA";
    }

    private void shuffle(List<int[]> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int[] tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }
}
