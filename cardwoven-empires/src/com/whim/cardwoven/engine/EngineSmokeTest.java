package com.whim.cardwoven.engine;

import java.util.ArrayList;
import java.util.List;

import com.whim.cardwoven.api.ActionResult;
import com.whim.cardwoven.api.Enums.CardType;
import com.whim.cardwoven.api.Enums.Faction;
import com.whim.cardwoven.api.Enums.ResourceType;
import com.whim.cardwoven.api.Enums.VictoryType;
import com.whim.cardwoven.api.GameController;
import com.whim.cardwoven.api.Views.CardView;
import com.whim.cardwoven.api.Views.GameStateView;
import com.whim.cardwoven.api.Views.MapView;
import com.whim.cardwoven.api.Views.PlayerView;
import com.whim.cardwoven.api.Views.TileView;
import com.whim.cardwoven.domain.GameState;

/**
 * Headless end-to-end smoke test: builds a game, then plays many full turns via
 * the {@link GameController} seam ONLY (exactly as the UI would), driving the
 * human with a greedy heuristic and letting the engine run AI opponents, yields,
 * combat, Sin accrual, and victory checks. Prints the event log so you can see
 * every subsystem fire. No JUnit dependency so it runs with a bare `java`.
 */
public final class EngineSmokeTest {

    private EngineSmokeTest() {}

    public static void main(String[] args) {
        // Lead The Unfaithful so Sin accrual is exercised alongside everything else.
        GameState state = GameState.create(Faction.THE_UNFAITHFUL, 20260703L);
        GameController engine = new GameEngine(state);

        final int maxTurns = 120;
        int turn = 0;
        while (turn < maxTurns && !engine.state().isGameOver()) {
            playHumanHand(engine);
            ActionResult end = engine.endTurn();
            turn += 1;
            if (turn % 10 == 0 || engine.state().isGameOver()) {
                printSnapshot(engine.state(), end);
            }
        }

        System.out.println();
        System.out.println("================ EVENT LOG (tail) ================");
        List<String> log = engine.state().recentLog();
        int from = Math.max(0, log.size() - 45);
        for (int i = from; i < log.size(); i++) {
            System.out.println("  " + log.get(i));
        }

        System.out.println();
        System.out.println("================ FINAL STATE ================");
        printSnapshot(engine.state(), null);
        GameStateView s = engine.state();
        if (s.isGameOver()) {
            System.out.println("RESULT: " + s.players().get(s.winnerPlayerIndex()).name()
                    + " won by " + s.winningVictory().display()
                    + " on turn " + s.turnNumber());
        } else {
            System.out.println("RESULT: no winner within " + maxTurns + " turns "
                    + "(victory monitor still ran every turn — see progress above)");
        }
        System.out.println("SMOKE TEST COMPLETE.");
    }

    /** Greedily play every card the human can, via the controller only. */
    private static void playHumanHand(GameController engine) {
        // Re-snapshot each pass because the hand mutates as cards resolve.
        boolean acted = true;
        int safety = 0;
        while (acted && safety < 50) {
            acted = false;
            safety += 1;
            GameStateView s = engine.state();
            if (s.isGameOver() || !s.currentPlayer().isHuman()) {
                return;
            }
            List<CardView> hand = new ArrayList<CardView>(s.currentPlayer().hand());
            for (int i = 0; i < hand.size(); i++) {
                CardView card = hand.get(i);
                if (tryPlay(engine, s, card)) {
                    acted = true;
                    break; // hand changed; re-snapshot
                }
            }
        }
    }

    private static boolean tryPlay(GameController engine, GameStateView s, CardView card) {
        CardType t = card.type();
        if (t == CardType.BUILDING) {
            int[] tile = firstBuildableTile(s.map());
            if (tile != null) {
                return engine.playBuilding(card.id(), tile[0], tile[1]).isSuccess();
            }
        } else if (t == CardType.ATTACHMENT) {
            int bId = firstOwnedBuilding(s);
            if (bId >= 0) {
                return engine.attachCard(card.id(), bId).isSuccess();
            }
        } else if (t == CardType.ECONOMY) {
            return engine.playCard(card.id(), 0, 0).isSuccess();
        } else if (t == CardType.EXPLORE) {
            int[] tile = firstUnexplored(s.map());
            if (tile != null) {
                return engine.playCard(card.id(), tile[0], tile[1]).isSuccess();
            }
        } else if (t == CardType.MILITARY) {
            int[] tile = firstRaider(s.map());
            if (tile != null) {
                return engine.resolveCombat(card.id(), tile[0], tile[1]).isSuccess();
            }
        }
        return false;
    }

    private static int[] firstBuildableTile(MapView map) {
        for (int r = 0; r < map.rows(); r++) {
            for (int c = 0; c < map.cols(); c++) {
                TileView tile = map.tile(r, c);
                if (tile.building() == null) {
                    return new int[] { r, c };
                }
            }
        }
        return null;
    }

    private static int firstOwnedBuilding(GameStateView s) {
        int me = s.currentPlayerIndex();
        List<com.whim.cardwoven.api.Views.BuildingView> mine = s.map().buildingsOf(me);
        return mine.isEmpty() ? -1 : mine.get(0).id();
    }

    private static int[] firstUnexplored(MapView map) {
        for (int r = 0; r < map.rows(); r++) {
            for (int c = 0; c < map.cols(); c++) {
                if (!map.tile(r, c).explored()) {
                    return new int[] { r, c };
                }
            }
        }
        return null;
    }

    private static int[] firstRaider(MapView map) {
        for (int r = 0; r < map.rows(); r++) {
            for (int c = 0; c < map.cols(); c++) {
                if (map.tile(r, c).raiderStrength() > 0) {
                    return new int[] { r, c };
                }
            }
        }
        return null;
    }

    private static void printSnapshot(GameStateView s, ActionResult lastEnd) {
        System.out.println();
        System.out.println("--- Turn " + s.turnNumber() + " (phase " + s.phase() + ") "
                + (lastEnd == null ? "" : "[" + lastEnd.message() + "]"));
        List<PlayerView> players = s.players();
        for (int i = 0; i < players.size(); i++) {
            PlayerView p = players.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append("  ").append(p.name());
            sb.append(" | gold=").append(p.resource(ResourceType.GOLD));
            sb.append(" cmd=").append(p.resource(ResourceType.COMMAND_POINTS));
            sb.append(" bld=").append(s.map().buildingsOf(p.index()).size());
            sb.append(" deck=").append(p.deckCount());
            sb.append(" disc=").append(p.discardCount());
            sb.append(" | progress");
            VictoryType[] vs = VictoryType.values();
            for (int v = 0; v < vs.length; v++) {
                sb.append(' ').append(shortName(vs[v])).append('=')
                  .append(pct(p.victoryProgress(vs[v])));
            }
            System.out.println(sb.toString());
        }
    }

    private static String shortName(VictoryType v) {
        return v.name().substring(0, 3);
    }

    private static String pct(double d) {
        long p = Math.round(d * 100.0);
        return p + "%";
    }
}
