package com.whim.tacticalnexus.data;

import com.whim.tacticalnexus.domain.Door;
import com.whim.tacticalnexus.domain.Enemy;
import com.whim.tacticalnexus.domain.GemType;
import com.whim.tacticalnexus.domain.GridMap;
import com.whim.tacticalnexus.domain.KeyColor;
import com.whim.tacticalnexus.domain.KeyItem;
import com.whim.tacticalnexus.domain.Player;
import com.whim.tacticalnexus.domain.Position;
import com.whim.tacticalnexus.domain.StairDirection;
import com.whim.tacticalnexus.domain.Staircase;
import com.whim.tacticalnexus.domain.StatGem;
import com.whim.tacticalnexus.domain.Wall;
import com.whim.tacticalnexus.state.GameState;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the initial {@link GameState}: three winnable ~11x11 demo floors that
 * exercise every mechanic — all three key/door colors, every gem type, enemies
 * of increasing difficulty (including one that is unkillable until the player
 * collects an ATK gem), and UP/DOWN stairs linking the floors.
 *
 * <p>The layout is fully deterministic and hand-verified winnable:
 * <ul>
 *   <li>Floor 0: collect ATK+5 / DEF+5 / HP gems and the three keys; a
 *       StoneGolem (DEF 10) is unkillable at the start (player ATK 10) and
 *       becomes killable once the ATK gem lifts ATK to 15. A YELLOW door gates
 *       the UP stair.</li>
 *   <li>Floor 1: BLUE and RED doors gate stronger gems; tougher enemies; an UP
 *       stair to the boss floor and a DOWN stair back.</li>
 *   <li>Floor 2: the Nexus Warden boss and the final gem.</li>
 * </ul>
 */
public final class FloorFactory {

    private static final int SIZE = 11;

    private FloorFactory() {
    }

    public static GameState initialState() {
        List<GridMap> floors = new ArrayList<GridMap>();
        floors.add(buildFloor0());
        floors.add(buildFloor1());
        floors.add(buildFloor2());

        // Player starts on floor 0 at (1,1).
        Player player = Player.starting(new Position(1, 1));
        return new GameState(player, floors, 0);
    }

    // ---- Floor 0 -----------------------------------------------------------

    private static GridMap buildFloor0() {
        GridMap m = bordered();

        // Three keys (every color collectible here).
        m = m.with(new Position(1, 3), new KeyItem(KeyColor.YELLOW));
        m = m.with(new Position(5, 1), new KeyItem(KeyColor.BLUE));
        m = m.with(new Position(5, 3), new KeyItem(KeyColor.RED));

        // One of every gem type.
        m = m.with(new Position(3, 5), new StatGem(GemType.ATK, 5));
        m = m.with(new Position(3, 1), new StatGem(GemType.DEF, 5));
        m = m.with(new Position(3, 3), new StatGem(GemType.HP, 500));

        // Easy enemy: killable from the start (player ATK 10 - DEF 0 = 10).
        m = m.with(new Position(1, 5),
                new Enemy("Slime", 30, 8, 0, 5, 5, new Color(120, 200, 120)));

        // Unkillable-until-ATK-gem enemy: ATK 10 <= DEF 10 blocks the bump;
        // after the ATK gem (ATK 15) it deals 5/ hit and dies.
        m = m.with(new Position(5, 5),
                new Enemy("StoneGolem", 40, 10, 10, 30, 15, new Color(150, 150, 160)));

        // YELLOW-door vault holding the UP stair to floor 1.
        // Room cells (8,8)(8,9)(9,8)(9,9) are sealed except the door at (7,8).
        m = m.with(new Position(7, 9), new Wall());
        m = m.with(new Position(8, 7), new Wall());
        m = m.with(new Position(9, 7), new Wall());
        m = m.with(new Position(7, 8), new Door(KeyColor.YELLOW));
        m = m.with(new Position(9, 9), new Staircase(StairDirection.UP));

        return m;
    }

    // ---- Floor 1 -----------------------------------------------------------

    private static GridMap buildFloor1() {
        GridMap m = bordered();

        // Entry from floor 0 (player lands on this DOWN stair coming up).
        m = m.with(new Position(1, 1), new Staircase(StairDirection.DOWN));

        // Increasing-difficulty enemies.
        m = m.with(new Position(1, 5),
                new Enemy("Bat", 60, 15, 5, 20, 20, new Color(110, 90, 150)));
        m = m.with(new Position(5, 5),
                new Enemy("Wolf", 80, 18, 8, 40, 35, new Color(160, 120, 80)));

        // BLUE-door niche: ATK+10 gem at (1,9), sealed except the door at (2,9).
        m = m.with(new Position(1, 8), new Wall());
        m = m.with(new Position(2, 9), new Door(KeyColor.BLUE));
        m = m.with(new Position(1, 9), new StatGem(GemType.ATK, 10));

        // RED-door niche: HP+1000 gem at (9,1), sealed except the door at (8,1).
        m = m.with(new Position(9, 2), new Wall());
        m = m.with(new Position(8, 1), new Door(KeyColor.RED));
        m = m.with(new Position(9, 1), new StatGem(GemType.HP, 1000));

        // UP stair to the boss floor.
        m = m.with(new Position(9, 9), new Staircase(StairDirection.UP));

        return m;
    }

    // ---- Floor 2 -----------------------------------------------------------

    private static GridMap buildFloor2() {
        GridMap m = bordered();

        // Entry from floor 1 (player lands here coming up).
        m = m.with(new Position(1, 1), new Staircase(StairDirection.DOWN));

        // A DEF gem to soften the boss, then the boss itself.
        m = m.with(new Position(3, 5), new StatGem(GemType.DEF, 5));
        m = m.with(new Position(5, 5),
                new Enemy("Nexus Warden", 200, 40, 20, 500, 100,
                        new Color(200, 70, 70)));

        // Final reward gem beyond the boss.
        m = m.with(new Position(9, 9), new StatGem(GemType.ATK, 20));

        return m;
    }

    // ---- helpers -----------------------------------------------------------

    /** An empty SIZE x SIZE floor walled around its border. */
    private static GridMap bordered() {
        GridMap m = new GridMap(SIZE, SIZE);
        for (int i = 0; i < SIZE; i++) {
            m = m.with(new Position(0, i), new Wall());
            m = m.with(new Position(SIZE - 1, i), new Wall());
            m = m.with(new Position(i, 0), new Wall());
            m = m.with(new Position(i, SIZE - 1), new Wall());
        }
        return m;
    }
}
