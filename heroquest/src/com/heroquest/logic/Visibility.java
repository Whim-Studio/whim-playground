package com.heroquest.logic;

import com.heroquest.model.DungeonMap;
import com.heroquest.model.GameState;
import com.heroquest.model.Point;
import com.heroquest.model.Tile;

/**
 * Fog of War / line-of-sight reveal. Rooms are revealed as a whole once seen
 * (as on the physical board), while corridors are revealed square-by-square via
 * straight rays that stop at walls and closed doors.
 */
public final class Visibility {

    private static final int[][] DIRS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private Visibility() {
    }

    /** Reveal everything a Hero standing on {@code from} can currently see. */
    public static void revealFrom(GameState state, Point from) {
        DungeonMap map = state.getMap();
        if (!map.inBounds(from)) {
            return;
        }
        Tile origin = map.tileAt(from);
        origin.setRevealed(true);
        if (origin.isRoom()) {
            revealRoom(state, origin.getRoomId());
        }

        // Always reveal the immediately adjacent squares (including enclosing walls).
        for (int[] d : DIRS) {
            Point p = from.translate(d[0], d[1]);
            if (map.inBounds(p)) {
                map.tileAt(p).setRevealed(true);
            }
        }

        // Cast straight rays outward through transparent squares.
        for (int[] d : DIRS) {
            Point p = from;
            while (true) {
                p = p.translate(d[0], d[1]);
                if (!map.inBounds(p)) {
                    break;
                }
                Tile t = map.tileAt(p);
                t.setRevealed(true);
                if (t.isRoom()) {
                    revealRoom(state, t.getRoomId());
                }
                if (!map.isTransparent(p)) {
                    break; // wall or closed door: revealed, but the ray stops here
                }
            }
        }
    }

    /** Reveals an entire room plus the walls/doors enclosing it. */
    public static void revealRoom(GameState state, int roomId) {
        if (roomId < 0) {
            return;
        }
        DungeonMap map = state.getMap();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile t = map.tileAt(x, y);
                if (t.getRoomId() == roomId) {
                    t.setRevealed(true);
                    for (int[] d : DIRS) {
                        Point n = new Point(x + d[0], y + d[1]);
                        if (map.inBounds(n)) {
                            map.tileAt(n).setRevealed(true);
                        }
                    }
                }
            }
        }
    }
}
