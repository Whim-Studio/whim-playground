package com.whim.firetop;

import com.whim.firetop.engine.Combat;
import com.whim.firetop.engine.Dice;
import com.whim.firetop.engine.GameEngine;
import com.whim.firetop.model.Board;
import com.whim.firetop.model.Character;
import com.whim.firetop.model.GameState;
import com.whim.firetop.model.Monster;
import com.whim.firetop.model.Room;
import com.whim.firetop.persistence.SaveGame;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Headless end-to-end smoke run driving the engine like the UI would, but with no
 * Swing. It plays a full game (new game -> dice movement toward the lair ->
 * combats -> defeat Zagor -> win) and then verifies a save/load round-trip.
 * Not shipped in the runnable jar; it lives in the test source tree.
 */
public final class SmokeTest {

    public static void main(String[] args) throws Exception {
        boolean won = false;
        long winningSeed = -1;
        String summary = "";

        for (long seed = 1; seed <= 400 && !won; seed++) {
            GameEngine engine = GameEngine.newGame(new ArrayList<String>(Arrays.asList("Hero")), seed);
            PlayResult pr = playToEnd(engine);
            if (pr.victory) {
                won = true;
                winningSeed = seed;
                summary = pr.summary;
            }
        }

        if (!won) {
            throw new IllegalStateException("SMOKE FAILED: no winning playthrough found in 400 seeds");
        }
        System.out.println("SMOKE: full playthrough won on seed " + winningSeed);
        System.out.println("SMOKE: " + summary);

        // Save/load round-trip on a fresh mid-game state.
        GameEngine e2 = GameEngine.newGame(new ArrayList<String>(Arrays.asList("A", "B")), 12345L);
        e2.rollMovement();
        Set<Integer> reach = e2.reachableRooms();
        if (!reach.isEmpty()) {
            e2.moveTo(reach.iterator().next());
        }
        Character before = e2.getState().getPlayers().get(0);
        int beforeStam = before.getStaminaCurrent();
        int beforeRoom = before.getRoomId();
        int beforeIdx = e2.getState().getCurrentPlayerIndex();

        File tmp = File.createTempFile("firetop-smoke", ".sav");
        tmp.deleteOnExit();
        e2.syncForSave();
        SaveGame.save(e2.getState(), tmp);
        GameState loaded = SaveGame.load(tmp);

        require(loaded.getPlayers().size() == 2, "player count preserved");
        require(loaded.getPlayers().get(0).getStaminaCurrent() == beforeStam, "stamina preserved");
        require(loaded.getPlayers().get(0).getRoomId() == beforeRoom, "position preserved");
        require(loaded.getCurrentPlayerIndex() == beforeIdx, "turn index preserved");
        require(loaded.getBoard().getLairId() == e2.getState().getBoard().getLairId(), "board preserved");
        System.out.println("SMOKE: save/load round-trip verified (2 players, stamina/pos/turn/board intact)");

        System.out.println("SMOKE: PASS");
    }

    private static final class PlayResult {
        boolean victory;
        String summary;
    }

    /** Plays a single game to a terminal state, steering toward the lair. */
    private static PlayResult playToEnd(GameEngine engine) {
        Board board = engine.getBoard();
        int lair = board.getLairId();
        Map<Integer, Integer> dist = distancesTo(board, lair);
        int guard = 0;

        while (!engine.getState().isGameOver() && guard++ < 3000) {
            Character c = engine.currentCharacter();
            if (!c.isAlive()) {
                if (!engine.endTurn()) {
                    break;
                }
                continue;
            }
            // Eat before pushing on if hurt.
            if (c.getStaminaCurrent() < 10 && c.getProvisions() > 0) {
                engine.eatProvision();
            }
            engine.rollMovement();
            Set<Integer> reach = engine.reachableRooms();
            if (reach.isEmpty()) {
                engine.endTurn();
                continue;
            }
            // Move to the reachable room closest to the lair.
            int best = -1;
            int bestD = Integer.MAX_VALUE;
            for (Integer rid : reach) {
                Integer d = dist.get(rid);
                if (d != null && d < bestD) {
                    bestD = d;
                    best = rid;
                }
            }
            if (best < 0) {
                best = reach.iterator().next();
            }
            GameEngine.RoomResolution res = engine.moveTo(best);
            handle(engine, res, true);
            if (engine.getState().isGameOver()) {
                break;
            }
            engine.endTurn();
        }

        PlayResult pr = new PlayResult();
        pr.victory = engine.getState().isVictory();
        Character hero = engine.getState().getPlayers().get(0);
        pr.summary = "hero " + hero.getName() + " STAMINA " + hero.getStaminaCurrent()
                + "/" + hero.getStaminaInitial() + ", gold " + hero.getGold()
                + ", room " + hero.getRoomId() + ", turns " + guard;
        return pr;
    }

    private static void handle(GameEngine engine, GameEngine.RoomResolution res, boolean topLevel) {
        switch (res.getKind()) {
            case COMBAT:
                Room room = topLevel
                        ? engine.getBoard().getRoom(engine.currentCharacter().getRoomId()) : null;
                autoFight(engine, res.getMonster(), room);
                break;
            case CARD:
                GameEngine.RoomResolution follow = engine.resolveCard(
                        res.getCard(), res.getSource(), engine.currentCharacter());
                if (follow.getKind() == GameEngine.RoomResolution.Kind.COMBAT) {
                    autoFight(engine, follow.getMonster(), null);
                }
                break;
            default:
                break;
        }
    }

    private static void autoFight(GameEngine engine, Monster monster, Room room) {
        Character c = engine.currentCharacter();
        Dice dice = engine.getDice();
        int guard = 0;
        while (!monster.isDefeated() && c.isAlive() && guard++ < 500) {
            if (c.getStaminaCurrent() <= 4 && c.getProvisions() > 0) {
                c.eatProvision(GameEngine.PROVISION_HEAL);
            }
            Combat.RoundResult r = Combat.resolveRound(c, monster, dice);
            if (r.getOutcome() == Combat.Outcome.PLAYER_WINS && c.getLuckCurrent() > 6) {
                Combat.applyLuckToAttack(c, monster, dice);
            } else if (r.getOutcome() == Combat.Outcome.MONSTER_WINS
                    && c.getStaminaCurrent() <= 6 && c.getLuckCurrent() > 6) {
                Combat.applyLuckToDefense(c, dice);
            }
        }
        if (monster.isDefeated()) {
            engine.onMonsterDefeated(room, monster);
        } else if (!c.isAlive()) {
            engine.onCharacterDefeated(c);
        }
    }

    private static Map<Integer, Integer> distancesTo(Board board, int target) {
        Map<Integer, Integer> dist = new HashMap<Integer, Integer>();
        Deque<Integer> q = new ArrayDeque<Integer>();
        Set<Integer> seen = new HashSet<Integer>();
        q.add(target);
        seen.add(target);
        dist.put(target, 0);
        while (!q.isEmpty()) {
            int cur = q.poll();
            for (Room n : board.neighbors(cur)) {
                if (!seen.contains(n.getId())) {
                    seen.add(n.getId());
                    dist.put(n.getId(), dist.get(cur) + 1);
                    q.add(n.getId());
                }
            }
        }
        return dist;
    }

    private static void require(boolean cond, String what) {
        if (!cond) {
            throw new IllegalStateException("SMOKE FAILED: " + what);
        }
    }

    private SmokeTest() { }
}
