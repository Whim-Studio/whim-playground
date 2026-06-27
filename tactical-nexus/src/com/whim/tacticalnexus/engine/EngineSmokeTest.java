package com.whim.tacticalnexus.engine;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

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

/**
 * Dependency-free, JUnit-free assertion harness for the engine layer. Run with:
 *
 * <pre>
 *   javac -d out $(find src -name "*.java")
 *   java -cp out com.whim.tacticalnexus.engine.EngineSmokeTest
 * </pre>
 *
 * Exits non-zero on the first failed assertion. This file touches only the
 * engine + domain/state public API defined by the contract, so it doubles as a
 * compilation check for Task 2 once Task 1 is merged.
 */
public final class EngineSmokeTest {

    private static int checks = 0;

    public static void main(String[] args) {
        testCombatMath();
        testMoveEmptyAndWall();
        testMoveOutOfBounds();
        testDoorAndKey();
        testGem();
        testEnemyMove();
        testStairs();
        System.out.println("OK — all " + checks + " engine assertions passed.");
    }

    // ---- Combat math -------------------------------------------------------

    private static void testCombatMath() {
        // Player ATK 10, DEF 10, HP 1000 vs Slime HP 30 ATK 12 DEF 2.
        Player p = Player.starting(new Position(0, 0));
        Enemy slime = new Enemy("Slime", 30, 12, 2, 5, 3, Color.GREEN);
        CombatResult r = CombatCalculator.resolve(p, slime);
        // playerDamage = 10-2 = 8 ; hitsToKill = ceil(30/8) = 4
        // enemyDamage = max(0,12-10)=2 ; hpLost = 2*(4-1)=6 ; survivable (6<1000)
        check(r.canFight(), "slime canFight");
        check(r.hitsToKill() == 4, "slime hitsToKill==4, got " + r.hitsToKill());
        check(r.hpLost() == 6, "slime hpLost==6, got " + r.hpLost());
        check(r.survivable(), "slime survivable");

        // Unkillable: player ATK 10 <= enemy DEF 10.
        Enemy armor = new Enemy("Armor", 50, 5, 10, 1, 1, Color.GRAY);
        CombatResult r2 = CombatCalculator.resolve(p, armor);
        check(!r2.canFight(), "armor !canFight");
        check(r2.hitsToKill() == 0, "armor hitsToKill==0");

        // Exactly killable in one hit (hpLost == 0, no retaliation).
        Enemy weak = new Enemy("Bat", 5, 100, 0, 1, 1, Color.BLACK);
        CombatResult r3 = CombatCalculator.resolve(p, weak);
        check(r3.canFight() && r3.hitsToKill() == 1 && r3.hpLost() == 0, "bat one-shot, hpLost 0");

        // Lethal: huge enemy ATK, many hits required.
        Player lowHp = Player.starting(new Position(0, 0)).withHp(5).withPosition(new Position(0, 0));
        Enemy brute = new Enemy("Brute", 30, 1000, 2, 1, 1, Color.RED);
        CombatResult r4 = CombatCalculator.resolve(lowHp, brute);
        // hpLost = max(0,1000-10) * (ceil(30/8)-1) = 990*3 = 2970 >= 5 -> not survivable
        check(r4.canFight() && !r4.survivable(), "brute lethal not survivable");
    }

    // ---- Movement ----------------------------------------------------------

    private static GameState singleFloorState(GridMap floor, Player player) {
        List<GridMap> floors = new ArrayList<GridMap>();
        floors.add(floor);
        return new GameState(player, floors, 0);
    }

    private static void testMoveEmptyAndWall() {
        GridMap floor = new GridMap(5, 5);
        Player p = Player.starting(new Position(2, 2));
        GameState st = singleFloorState(floor, p);

        MoveOutcome moved = MoveResolver.resolve(st, 0, 1);
        check(moved.changed(), "empty move changed");
        check(moved.state().player().position().equals(new Position(2, 3)), "empty move position");

        GridMap walled = floor.with(new Position(2, 3), new Wall());
        GameState st2 = singleFloorState(walled, p);
        MoveOutcome wall = MoveResolver.resolve(st2, 0, 1);
        check(!wall.changed(), "wall blocked");
        check(wall.state() == st2, "wall returns same state instance");
        check(wall.state().player().position().equals(new Position(2, 2)), "wall no move");
    }

    private static void testMoveOutOfBounds() {
        GridMap floor = new GridMap(5, 5);
        Player p = Player.starting(new Position(0, 0));
        GameState st = singleFloorState(floor, p);
        MoveOutcome oob = MoveResolver.resolve(st, -1, 0);
        check(!oob.changed(), "oob blocked");
        check(oob.state() == st, "oob same state instance");
    }

    private static void testDoorAndKey() {
        Position keyPos = new Position(2, 3);
        Position doorPos = new Position(2, 3);
        GridMap floor = new GridMap(5, 5).with(keyPos, new KeyItem(KeyColor.YELLOW));
        Player p = Player.starting(new Position(2, 2));
        GameState st = singleFloorState(floor, p);

        // Pick up the key: player advances, key gained, cell cleared.
        MoveOutcome gotKey = MoveResolver.resolve(st, 0, 1);
        check(gotKey.changed(), "key changed");
        check(gotKey.state().player().keyCount(KeyColor.YELLOW) == 1, "key count 1");
        check(gotKey.state().player().position().equals(keyPos), "key advance");
        check(gotKey.state().currentFloor().at(keyPos) == null, "key cell cleared");

        // Door with no key -> blocked.
        GridMap doorFloor = new GridMap(5, 5).with(doorPos, new Door(KeyColor.YELLOW));
        GameState lockedSt = singleFloorState(doorFloor, p);
        MoveOutcome locked = MoveResolver.resolve(lockedSt, 0, 1);
        check(!locked.changed(), "door locked blocked");

        // Door with a key -> spend key, open in place, do NOT advance.
        Player keyed = p.addKey(KeyColor.YELLOW, 1);
        GameState openableSt = singleFloorState(doorFloor, keyed);
        MoveOutcome opened = MoveResolver.resolve(openableSt, 0, 1);
        check(opened.changed(), "door opened changed");
        check(opened.state().player().keyCount(KeyColor.YELLOW) == 0, "door spent key");
        check(opened.state().player().position().equals(new Position(2, 2)), "door no advance");
        check(opened.state().currentFloor().at(doorPos) == null, "door cell cleared");
    }

    private static void testGem() {
        Position gemPos = new Position(2, 3);
        GridMap floor = new GridMap(5, 5).with(gemPos, new StatGem(GemType.ATK, 5));
        Player p = Player.starting(new Position(2, 2));
        GameState st = singleFloorState(floor, p);
        int baseAtk = p.atk();
        MoveOutcome gem = MoveResolver.resolve(st, 0, 1);
        check(gem.changed(), "gem changed");
        check(gem.state().player().atk() == baseAtk + 5, "gem +5 atk");
        check(gem.state().player().position().equals(gemPos), "gem advance");
        check(gem.state().currentFloor().at(gemPos) == null, "gem cell cleared");
    }

    private static void testEnemyMove() {
        Position enemyPos = new Position(2, 3);
        Enemy slime = new Enemy("Slime", 30, 12, 2, 5, 3, Color.GREEN);
        GridMap floor = new GridMap(5, 5).with(enemyPos, slime);
        Player p = Player.starting(new Position(2, 2));
        GameState st = singleFloorState(floor, p);
        int baseHp = p.hp();

        MoveOutcome fight = MoveResolver.resolve(st, 0, 1);
        check(fight.changed(), "enemy fight changed");
        check(fight.state().player().hp() == baseHp - 6, "enemy hp lost 6");
        check(fight.state().player().gold() == 5, "enemy gold +5");
        check(fight.state().player().exp() == 3, "enemy exp +3");
        check(fight.state().player().position().equals(enemyPos), "enemy advance");
        check(fight.state().currentFloor().at(enemyPos) == null, "enemy cleared");

        // Unkillable enemy blocks.
        Enemy armor = new Enemy("Armor", 50, 5, 10, 1, 1, Color.GRAY);
        GridMap af = new GridMap(5, 5).with(enemyPos, armor);
        GameState ast = singleFloorState(af, p);
        MoveOutcome blocked = MoveResolver.resolve(ast, 0, 1);
        check(!blocked.changed(), "unkillable blocked");
        check(blocked.state() == ast, "unkillable same instance");
    }

    private static void testStairs() {
        // Floor 0 has an UP stair; floor 1 has a DOWN stair (landing target).
        Position up0 = new Position(1, 1);
        Position down1 = new Position(3, 3);
        GridMap f0 = new GridMap(5, 5).with(up0, new Staircase(StairDirection.UP));
        GridMap f1 = new GridMap(5, 5).with(down1, new Staircase(StairDirection.DOWN));
        List<GridMap> floors = new ArrayList<GridMap>();
        floors.add(f0);
        floors.add(f1);
        Player p = Player.starting(up0);
        GameState st = new GameState(p, floors, 0);

        // Move onto the up stair (player is on it; stepping in place isn't how it
        // works — instead place player adjacent and step onto it).
        Player adj = Player.starting(new Position(1, 0));
        GameState st2 = new GameState(adj, floors, 0);
        MoveOutcome climb = MoveResolver.resolve(st2, 0, 1); // step onto up0
        check(climb.changed(), "stair changed");
        check(climb.state().floorIndex() == 1, "stair floor 1");
        check(climb.state().player().position().equals(down1), "stair lands on DOWN stair");
    }

    // ---- helpers -----------------------------------------------------------

    private static void check(boolean cond, String label) {
        checks++;
        if (!cond) {
            System.err.println("FAIL: " + label);
            throw new AssertionError(label);
        }
    }
}
