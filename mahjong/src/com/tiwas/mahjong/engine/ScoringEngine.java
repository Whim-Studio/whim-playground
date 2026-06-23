package com.tiwas.mahjong.engine;

import java.util.ArrayList;
import java.util.List;

import com.tiwas.mahjong.model.Constants;
import com.tiwas.mahjong.model.Hand;
import com.tiwas.mahjong.model.Meld;
import com.tiwas.mahjong.model.ScoreSheet;
import com.tiwas.mahjong.model.Scorable;
import com.tiwas.mahjong.model.Tile;
import com.tiwas.mahjong.model.TileSuit;

/**
 * Implements the rulebook §6 scoring, in order: sum base points (+flowers),
 * add the mahjong bonus, apply doubles multiplicatively, cap at the limit, and
 * round down. Special limit hands (Thirteen Orphans, All Flowers & Seasons) are
 * scored as the limit and skip extra doubles.
 */
public final class ScoringEngine implements Scorable {

    /**
     * Score a winning hand, choosing the highest-scoring legal decomposition.
     */
    public ScoreSheet scoreWin(Hand hand, WinContext ctx, int limit) {
        // --- special limit hands first ---
        List<Tile> concealed = new ArrayList<Tile>(hand.getTiles());

        if (HandAnalyzer.isThirteenOrphans(concealed, hand.getMelds())) {
            return specialLimit(hand, "Thirteen Orphans (Limit Hand)", limit);
        }
        if (isAllFlowersAndSeasons(hand)) {
            return specialLimit(hand, "All Flowers & Seasons (Limit Hand)", limit);
        }

        int setsNeeded = 4 - hand.getMelds().size();
        List<HandAnalyzer.Decomposition> parses =
                HandAnalyzer.decomposeConcealed(concealed, setsNeeded);

        ScoreSheet best = null;
        for (int i = 0; i < parses.size(); i++) {
            HandAnalyzer.Decomposition d = parses.get(i);
            List<Meld> allMelds = new ArrayList<Meld>(hand.getMelds());
            allMelds.addAll(d.melds);
            ScoreSheet sheet = scoreDecomposition(allMelds, d.pair, hand.getBonus(), ctx, limit);
            if (best == null || sheet.getFinalScore() > best.getFinalScore()) {
                best = sheet;
            }
        }
        if (best == null) {
            // No standard parse (should not happen for a validated win) — minimal sheet.
            best = new ScoreSheet();
            best.setTitle("Winning Hand");
            best.setFinalScore(Constants.mahjongBonus(limit));
        }
        return best;
    }

    private ScoreSheet specialLimit(Hand hand, String title, int limit) {
        ScoreSheet sheet = new ScoreSheet();
        sheet.setTitle(title);
        sheet.setLimitHand(true);
        sheet.setLimited(true);
        int bonus = Constants.mahjongBonus(limit);
        sheet.addBase("Limit hand", "scored as the points limit = " + limit);
        sheet.addBase("Mahjong bonus", "+" + bonus);
        // flowers/seasons points still apply
        int flowerPts = hand.getBonus().size() * Constants.FLOWER_OR_SEASON_POINTS;
        if (flowerPts > 0) {
            sheet.addBase("Flowers/Seasons", hand.getBonus().size() + " x "
                    + Constants.FLOWER_OR_SEASON_POINTS + " = " + flowerPts);
        }
        sheet.setBasePoints(limit + bonus + flowerPts);
        sheet.setTotalDoubles(0);
        sheet.setRawScore(limit + bonus + flowerPts);
        sheet.setFinalScore(limit + bonus + flowerPts);
        return sheet;
    }

    private ScoreSheet scoreDecomposition(List<Meld> melds, List<Tile> pair,
                                          List<Tile> bonus, WinContext ctx, int limit) {
        ScoreSheet sheet = new ScoreSheet();
        sheet.setTitle("Winning Hand");

        int base = calculateBaseFromMelds(melds, bonus, sheet);
        sheet.setBasePoints(base);

        // (2) add mahjong bonus
        int bonusPts = Constants.mahjongBonus(limit);
        sheet.addBase("Mahjong bonus", "+" + bonusPts);
        int baseWithBonus = base + bonusPts;

        // (3) doubles
        int doubles = computeDoubles(melds, pair, ctx, sheet);
        sheet.setTotalDoubles(doubles);

        long raw = (long) baseWithBonus;
        for (int i = 0; i < doubles; i++) {
            raw *= 2L;
        }
        if (raw > Integer.MAX_VALUE) {
            raw = Integer.MAX_VALUE;
        }
        sheet.setRawScore((int) raw);

        // (4) cap at limit, round down (already integer)
        int finalScore;
        if (raw > limit) {
            finalScore = limit;
            sheet.setLimited(true);
        } else {
            finalScore = (int) raw;
        }
        sheet.setFinalScore(finalScore);
        return sheet;
    }

    private int calculateBaseFromMelds(List<Meld> melds, List<Tile> bonus, ScoreSheet sheet) {
        int total = 0;
        for (int i = 0; i < melds.size(); i++) {
            Meld m = melds.get(i);
            int pts = meldPoints(m);
            if (pts > 0) {
                sheet.addBase(meldLabel(m), "+" + pts);
            }
            total += pts;
        }
        int flowerPts = bonus.size() * Constants.FLOWER_OR_SEASON_POINTS;
        if (flowerPts > 0) {
            sheet.addBase("Flowers/Seasons", bonus.size() + " x "
                    + Constants.FLOWER_OR_SEASON_POINTS + " = " + flowerPts);
            total += flowerPts;
        }
        return total;
    }

    private int meldPoints(Meld m) {
        boolean honour = m.isHonourMeld();
        boolean concealed = m.isConcealed();
        if (m.isChow()) {
            return Constants.CHOW_POINTS;
        }
        if (m.isPung()) {
            if (honour) {
                return concealed ? Constants.PUNG_HONOUR_CONCEALED : Constants.PUNG_HONOUR_EXPOSED;
            }
            return concealed ? Constants.PUNG_SIMPLE_CONCEALED : Constants.PUNG_SIMPLE_EXPOSED;
        }
        // kong
        if (honour) {
            return concealed ? Constants.KONG_HONOUR_CONCEALED : Constants.KONG_HONOUR_EXPOSED;
        }
        return concealed ? Constants.KONG_SIMPLE_CONCEALED : Constants.KONG_SIMPLE_EXPOSED;
    }

    private String meldLabel(Meld m) {
        String kind = m.isChow() ? "Chow" : m.isKong() ? "Kong" : "Pung";
        String vis = m.isConcealed() ? "concealed" : "exposed";
        return (m.isChow() ? "Chow" : vis + " " + kind) + " " + m.representative().code();
    }

    private int computeDoubles(List<Meld> melds, List<Tile> pair, WinContext ctx, ScoreSheet sheet) {
        int doubles = 0;

        int chows = 0, pungs = 0, kongs = 0, concealedKongs = 0;
        for (int i = 0; i < melds.size(); i++) {
            Meld m = melds.get(i);
            if (m.isChow()) {
                chows++;
            } else if (m.isKong()) {
                kongs++;
                if (m.isConcealed()) {
                    concealedKongs++;
                }
            } else {
                pungs++;
            }
        }

        // Fully concealed hand
        if (ctx.fullyConcealed) {
            doubles += Constants.DBL_ALL_CONCEALED;
            sheet.addDouble("All Concealed Hand", "x2^" + Constants.DBL_ALL_CONCEALED);
        }
        // No chows
        if (chows == 0) {
            doubles += Constants.DBL_NO_CHOWS;
            sheet.addDouble("No Chows", "x2^" + Constants.DBL_NO_CHOWS);
        }
        // All chows (4 chow sets)
        if (chows == 4) {
            doubles += Constants.DBL_ALL_CHOWS;
            sheet.addDouble("All Chows", "x2^" + Constants.DBL_ALL_CHOWS);
        }
        // All kongs
        if (kongs == 4) {
            doubles += Constants.DBL_ALL_KONGS;
            sheet.addDouble("All Kongs", "x2^" + Constants.DBL_ALL_KONGS);
        }
        // All concealed kongs (stacks with all kongs)
        if (concealedKongs == 4) {
            doubles += Constants.DBL_ALL_CONCEALED_KONGS;
            sheet.addDouble("All Concealed Kongs", "x2^" + Constants.DBL_ALL_CONCEALED_KONGS);
        }

        // Double pung: same number, two suits (pung/kong of suited tiles)
        int[] suitRankPung = new int[3 * 10]; // suit*10+rank flags count
        for (int i = 0; i < melds.size(); i++) {
            Meld m = melds.get(i);
            if ((m.isPung() || m.isKong()) && m.representative().isSuited()) {
                int s = m.representative().getSuit().ordinal();
                int r = m.representative().getRank();
                suitRankPung[s * 10 + r]++;
            }
        }
        for (int r = 1; r <= 9; r++) {
            int suitsWith = 0;
            for (int s = 0; s < 3; s++) {
                if (suitRankPung[s * 10 + r] > 0) {
                    suitsWith++;
                }
            }
            if (suitsWith >= 2) {
                doubles += Constants.DBL_DOUBLE_PUNG;
                sheet.addDouble("Double Pung (" + r + ")", "x2^" + Constants.DBL_DOUBLE_PUNG);
            }
        }

        // Chow-based doubles: gather chow starts per suit
        List<int[]> chowList = new ArrayList<int[]>(); // {suitOrdinal, startRank}
        for (int i = 0; i < melds.size(); i++) {
            Meld m = melds.get(i);
            if (m.isChow()) {
                Tile rep = lowestOfChow(m);
                chowList.add(new int[] { rep.getSuit().ordinal(), rep.getRank() });
            }
        }
        // Mixed double chow: same start rank, two suits
        for (int r = 1; r <= 7; r++) {
            int suitsWith = 0;
            for (int s = 0; s < 3; s++) {
                if (hasChow(chowList, s, r)) {
                    suitsWith++;
                }
            }
            if (suitsWith >= 2) {
                doubles += Constants.DBL_MIXED_DOUBLE_CHOW;
                sheet.addDouble("Mixed Double Chow (" + r + ")",
                        "x2^" + Constants.DBL_MIXED_DOUBLE_CHOW);
            }
        }
        // Per-suit chow patterns
        for (int s = 0; s < 3; s++) {
            boolean c1 = hasChow(chowList, s, 1);
            boolean c4 = hasChow(chowList, s, 4);
            boolean c7 = hasChow(chowList, s, 7);
            if ((c1 && c4) || (c4 && c7)) {
                doubles += Constants.DBL_SHORT_STRAIGHT;
                sheet.addDouble("Short Straight", "x2^" + Constants.DBL_SHORT_STRAIGHT);
            }
            if (c1 && c7) {
                doubles += Constants.DBL_TWO_TERMINAL_CHOWS;
                sheet.addDouble("Two Terminal Chows", "x2^" + Constants.DBL_TWO_TERMINAL_CHOWS);
            }
        }

        // Suit composition doubles
        boolean anyHonour = false;
        boolean anySuited = false;
        TileSuit theSuit = null;
        boolean multiSuit = false;
        List<Tile> all = allTiles(melds, pair);
        for (int i = 0; i < all.size(); i++) {
            Tile t = all.get(i);
            if (t.isHonour()) {
                anyHonour = true;
            } else if (t.isSuited()) {
                anySuited = true;
                if (theSuit == null) {
                    theSuit = t.getSuit();
                } else if (theSuit != t.getSuit()) {
                    multiSuit = true;
                }
            }
        }
        if (!anySuited && anyHonour) {
            doubles += Constants.DBL_ALL_HONOURS;
            sheet.addDouble("All Honours", "x2^" + Constants.DBL_ALL_HONOURS);
        } else if (anySuited && !multiSuit && anyHonour) {
            doubles += Constants.DBL_ALL_ONE_SUIT_WITH_HONOURS;
            sheet.addDouble("All One Suit with Honours",
                    "x2^" + Constants.DBL_ALL_ONE_SUIT_WITH_HONOURS);
        }

        // Situational doubles
        if (ctx.firstTile && ctx.dealer) {
            doubles += Constants.DBL_HEAVENLY_HAND;
            sheet.addDouble("Heavenly Hand", "x2^" + Constants.DBL_HEAVENLY_HAND);
        }
        if (ctx.firstTile && !ctx.dealer && !ctx.selfDraw) {
            doubles += Constants.DBL_EARTHLY_HAND;
            sheet.addDouble("Earthly Hand", "x2^" + Constants.DBL_EARTHLY_HAND);
        }
        if (ctx.firstTile && !ctx.dealer && ctx.selfDraw) {
            // Human hand: win before discarding any tile (on opening self-draw)
            doubles += Constants.DBL_HUMAN_HAND;
            sheet.addDouble("Human Hand", "x2^" + Constants.DBL_HUMAN_HAND);
        }
        if (ctx.lastTile) {
            doubles += Constants.DBL_LAST_TILE_WIN;
            sheet.addDouble("Win on Last Tile", "x2^" + Constants.DBL_LAST_TILE_WIN);
        }
        if (ctx.finalDiscard) {
            doubles += Constants.DBL_FINAL_DISCARD_WIN;
            sheet.addDouble("Win on Final Discard", "x2^" + Constants.DBL_FINAL_DISCARD_WIN);
        }

        return doubles;
    }

    private Tile lowestOfChow(Meld m) {
        List<Tile> ts = m.getTiles();
        Tile low = ts.get(0);
        for (int i = 1; i < ts.size(); i++) {
            if (ts.get(i).getRank() < low.getRank()) {
                low = ts.get(i);
            }
        }
        return low;
    }

    private boolean hasChow(List<int[]> chows, int suitOrdinal, int startRank) {
        for (int i = 0; i < chows.size(); i++) {
            if (chows.get(i)[0] == suitOrdinal && chows.get(i)[1] == startRank) {
                return true;
            }
        }
        return false;
    }

    private List<Tile> allTiles(List<Meld> melds, List<Tile> pair) {
        List<Tile> all = new ArrayList<Tile>();
        for (int i = 0; i < melds.size(); i++) {
            all.addAll(melds.get(i).getTiles());
        }
        all.addAll(pair);
        return all;
    }

    /** True if the player holds all eight bonus tiles (flowers 1-4 + seasons 1-4). */
    public static boolean isAllFlowersAndSeasons(Hand hand) {
        if (hand.getBonus().size() < 8) {
            return false;
        }
        boolean[] flowers = new boolean[5];
        boolean[] seasons = new boolean[5];
        for (int i = 0; i < hand.getBonus().size(); i++) {
            Tile t = hand.getBonus().get(i);
            if (t.getSuit() == TileSuit.FLOWER && t.getFlower() >= 1 && t.getFlower() <= 4) {
                flowers[t.getFlower()] = true;
            } else if (t.getSuit() == TileSuit.SEASON && t.getSeason() >= 1 && t.getSeason() <= 4) {
                seasons[t.getSeason()] = true;
            }
        }
        for (int i = 1; i <= 4; i++) {
            if (!flowers[i] || !seasons[i]) {
                return false;
            }
        }
        return true;
    }

    // ---- Scorable interface (lower-level hooks; the UI uses scoreWin) ----

    public int calculateBasePoints(Hand hand, ScoreSheet sheet) {
        return calculateBaseFromMelds(hand.getMelds(), hand.getBonus(), sheet);
    }

    public int applyDoubles(int basePoints, int doubles, ScoreSheet sheet) {
        long raw = basePoints;
        for (int i = 0; i < doubles; i++) {
            raw *= 2L;
        }
        if (raw > Integer.MAX_VALUE) {
            raw = Integer.MAX_VALUE;
        }
        sheet.setRawScore((int) raw);
        return (int) raw;
    }

    public int getFinalScore(ScoreSheet sheet, int limit) {
        int raw = sheet.getRawScore();
        return raw > limit ? limit : raw;
    }
}
