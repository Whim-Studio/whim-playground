package com.tiwas.mahjong;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.tiwas.mahjong.engine.AIPlayerLogic;
import com.tiwas.mahjong.engine.GameEngine;
import com.tiwas.mahjong.engine.HandAnalyzer;
import com.tiwas.mahjong.engine.HandResult;
import com.tiwas.mahjong.engine.ScoringEngine;
import com.tiwas.mahjong.engine.TurnStatus;
import com.tiwas.mahjong.engine.WinContext;
import com.tiwas.mahjong.model.Constants;
import com.tiwas.mahjong.model.Dragon;
import com.tiwas.mahjong.model.GameState;
import com.tiwas.mahjong.model.Hand;
import com.tiwas.mahjong.model.Meld;
import com.tiwas.mahjong.model.MeldType;
import com.tiwas.mahjong.model.ScoreSheet;
import com.tiwas.mahjong.model.Tile;
import com.tiwas.mahjong.model.TileSuit;
import com.tiwas.mahjong.model.Wind;

/**
 * Self-contained test runner (no JUnit). Exercises hand analysis, scoring, and a
 * full headless game where the human seat is auto-played. Exits non-zero on the
 * first failure.
 */
public final class EngineSmokeTest {

    private static int checks = 0;

    public static void main(String[] args) {
        testStandardWinDetection();
        testThirteenOrphans();
        testConcealedPungScoring();
        testLimitCap();
        testFullGamesHeadless();
        System.out.println("ALL TESTS PASSED (" + checks + " checks)");
    }

    private static void check(boolean cond, String msg) {
        checks++;
        if (!cond) {
            System.out.println("FAIL: " + msg);
            System.exit(1);
        }
    }

    // ----- analysis -----

    private static Tile d(int r) { return Tile.suited(TileSuit.DOTS, r); }
    private static Tile b(int r) { return Tile.suited(TileSuit.BAMBOO, r); }
    private static Tile c(int r) { return Tile.suited(TileSuit.CHARACTERS, r); }

    private static void testStandardWinDetection() {
        // 123 456 789 of dots, 111 bamboo, EE pair = 4 sets + pair
        List<Tile> tiles = new ArrayList<Tile>();
        for (int r = 1; r <= 9; r++) {
            tiles.add(d(r));
        }
        tiles.add(b(1)); tiles.add(b(1)); tiles.add(b(1));
        tiles.add(Tile.wind(Wind.EAST)); tiles.add(Tile.wind(Wind.EAST));
        check(HandAnalyzer.isStandardWin(new ArrayList<Meld>(), tiles), "standard win 14 tiles");

        // remove one tile -> not a win
        List<Tile> broken = new ArrayList<Tile>(tiles);
        broken.remove(0);
        check(!HandAnalyzer.isStandardWin(new ArrayList<Meld>(), broken), "13 tiles not a win");
    }

    private static void testThirteenOrphans() {
        List<Tile> tiles = new ArrayList<Tile>(HandAnalyzer.thirteenOrphanFaces());
        tiles.add(Tile.dragon(Dragon.RED)); // duplicate for the pair
        check(HandAnalyzer.isThirteenOrphans(tiles, new ArrayList<Meld>()), "thirteen orphans");
        check(tiles.size() == 14, "13 orphans has 14 tiles");
    }

    private static void testConcealedPungScoring() {
        // Four concealed pungs of simples + a pair, fully concealed self-draw.
        Hand hand = new Hand();
        addPung(hand, d(2));
        addPung(hand, b(5));
        addPung(hand, c(8));
        addPung(hand, d(6));
        hand.addTile(Tile.dragon(Dragon.GREEN));
        hand.addTile(Tile.dragon(Dragon.GREEN));
        // melds added concealed via decomposition: put all as concealed tiles
        Hand concealedHand = new Hand();
        addTiles(concealedHand, d(2), d(2), d(2), b(5), b(5), b(5), c(8), c(8), c(8),
                d(6), d(6), d(6), Tile.dragon(Dragon.GREEN), Tile.dragon(Dragon.GREEN));

        WinContext ctx = new WinContext();
        ctx.selfDraw = true;
        ctx.fullyConcealed = true;
        ScoringEngine eng = new ScoringEngine();
        ScoreSheet sheet = eng.scoreWin(concealedHand, ctx, Constants.DEFAULT_LIMIT);
        check(sheet.getFinalScore() > 0, "concealed pung hand scores > 0");
        check(sheet.getTotalDoubles() >= Constants.DBL_ALL_CONCEALED + Constants.DBL_NO_CHOWS,
                "concealed + no-chows doubles present");
    }

    private static void testLimitCap() {
        // All Honours-ish hand to drive a big double count and verify the cap.
        Hand hand = new Hand();
        addTiles(hand,
                Tile.wind(Wind.EAST), Tile.wind(Wind.EAST), Tile.wind(Wind.EAST),
                Tile.wind(Wind.SOUTH), Tile.wind(Wind.SOUTH), Tile.wind(Wind.SOUTH),
                Tile.dragon(Dragon.RED), Tile.dragon(Dragon.RED), Tile.dragon(Dragon.RED),
                Tile.dragon(Dragon.GREEN), Tile.dragon(Dragon.GREEN), Tile.dragon(Dragon.GREEN),
                Tile.dragon(Dragon.WHITE), Tile.dragon(Dragon.WHITE));
        WinContext ctx = new WinContext();
        ctx.selfDraw = true;
        ctx.fullyConcealed = true;
        ScoringEngine eng = new ScoringEngine();
        ScoreSheet sheet = eng.scoreWin(hand, ctx, Constants.DEFAULT_LIMIT);
        check(sheet.getFinalScore() == Constants.DEFAULT_LIMIT, "all-honours capped at limit");
        check(sheet.isLimited(), "limited flag set");
    }

    private static void addPung(Hand hand, Tile t) {
        List<Tile> ts = new ArrayList<Tile>();
        ts.add(t); ts.add(t); ts.add(t);
        hand.addMeld(new Meld(MeldType.PUNG, ts, true));
    }

    private static void addTiles(Hand hand, Tile... ts) {
        for (int i = 0; i < ts.length; i++) {
            hand.addTile(ts[i]);
        }
    }

    // ----- full headless games -----

    private static void testFullGamesHeadless() {
        for (int g = 0; g < 5; g++) {
            playOneGame(new Random(100 + g));
        }
    }

    private static void playOneGame(Random rng) {
        GameEngine engine = new GameEngine(rng);
        AIPlayerLogic autoHuman = new AIPlayerLogic(rng);
        engine.startGame();
        engine.drainLog();

        int guard = 0;
        while (!engine.isGameOver()) {
            guard++;
            check(guard < 200000, "game terminates within step budget");
            TurnStatus st = engine.advance();
            engine.drainLog();
            GameState state = engine.getState();
            switch (st.kind) {
                case AWAIT_HUMAN_DRAW:
                    engine.humanDraw();
                    break;
                case AWAIT_HUMAN_DISCARD:
                    if (engine.canHumanSelfMahjong()) {
                        engine.humanSelfMahjong();
                    } else {
                        Hand h = state.getPlayer(0).getHand();
                        Tile pick = autoHuman.chooseDiscard(h,
                                state.getPlayer(0).getSeatWind(), state.getRoundWind());
                        check(pick != null, "human has a tile to discard");
                        engine.humanDiscard(pick);
                    }
                    break;
                case AWAIT_HUMAN_CLAIM:
                    engine.humanPass();
                    break;
                case HAND_OVER:
                    HandResult r = engine.getLastResult();
                    int sum = 0;
                    for (int i = 0; i < r.deltas.length; i++) {
                        sum += r.deltas[i];
                    }
                    check(sum == 0, "per-hand score deltas sum to zero");
                    engine.nextHand();
                    break;
                default:
                    break;
            }
        }
        // Whole-game scores must net to zero.
        GameState state = engine.getState();
        int total = 0;
        for (int i = 0; i < state.getPlayers().size(); i++) {
            total += state.getPlayer(i).getScore();
        }
        check(total == 0, "whole-game scores net to zero");
    }
}
