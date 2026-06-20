package com.tiwa.mahjong.scoring;

import com.tiwa.mahjong.api.Meld;
import com.tiwa.mahjong.api.MeldType;
import com.tiwa.mahjong.api.Suit;
import com.tiwa.mahjong.api.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the hand point value of a winning {@link WinContext}, implementing the math of
 * Sections 5-6 of Tiwa's Mah Jong Rulebook.
 *
 * <p>Order of operations (strict):</p>
 * <ol>
 *   <li>Sum base points from melds + 4 per Flower/Season.</li>
 *   <li>Add the Mahjong bonus (1% of the Points Limit, rounded down; 0 in unlimited games).</li>
 *   <li>Apply every double multiplicatively (2 doubles = x4, 3 = x8, ...).</li>
 *   <li>If the result exceeds the Points Limit, cap it at the limit (Limit Hand).</li>
 * </ol>
 *
 * <p>Special limit hands ({@link SpecialHand}) short-circuit steps 1 and 3: they receive the
 * Mahjong bonus but apply no doubles, and the capped result equals the Points Limit.</p>
 */
public final class ScoreCalculator {

    private static final int BONUS_TILE_POINTS = 4;

    /** Computes the final, capped hand points for the given win. */
    public long computeHandPoints(WinContext ctx) {
        // Special limit hands score as a flat Limit Hand: bonus folded in, no extra doubles, capped.
        if (ctx.getSpecialHand() != SpecialHand.NONE) {
            long withBonus = (long) basePoints(ctx) + mahjongBonus(ctx);
            return cap(withBonus < ctx.getPointsLimit() ? ctx.getPointsLimit() : withBonus, ctx);
        }

        long total = basePoints(ctx);
        total += mahjongBonus(ctx);

        int doubles = countDoubles(ctx);
        for (int i = 0; i < doubles; i++) {
            total *= 2L;
        }

        return cap(total, ctx);
    }

    private long cap(long total, WinContext ctx) {
        if (ctx.isUnlimitedPoints()) {
            return total;
        }
        return total > ctx.getPointsLimit() ? ctx.getPointsLimit() : total;
    }

    // ----- Step 1: base points --------------------------------------------------------------

    /** Sum of per-meld base points plus 4 per Flower/Season. The pair contributes 0. */
    public int basePoints(WinContext ctx) {
        int sum = 0;
        for (Meld meld : ctx.getMelds()) {
            sum += meldPoints(meld);
        }
        sum += ctx.getBonusTiles().size() * BONUS_TILE_POINTS;
        return sum;
    }

    private int meldPoints(Meld meld) {
        MeldType type = meld.getType();
        if (type == MeldType.CHOW || type == MeldType.PAIR) {
            return 0;
        }
        boolean honor = meld.representative().isHonor();
        boolean concealed = meld.isConcealed();
        if (type == MeldType.PUNG) {
            if (honor) {
                return concealed ? 8 : 4;
            }
            return concealed ? 4 : 2;
        }
        // KONG
        if (honor) {
            return concealed ? 16 : 8;
        }
        return concealed ? 8 : 4;
    }

    // ----- Step 2: Mahjong bonus ------------------------------------------------------------

    /** 1% of the Points Limit rounded down; 0 in unlimited-points games. */
    public int mahjongBonus(WinContext ctx) {
        if (ctx.isUnlimitedPoints()) {
            return 0;
        }
        return ctx.getPointsLimit() / 100;
    }

    // ----- Step 3: doubles ------------------------------------------------------------------

    /** Total number of doubles that apply to this hand (each one multiplies the total by 2). */
    public int countDoubles(WinContext ctx) {
        int doubles = 0;

        List<Meld> sets = ctx.getMelds();
        List<Meld> chows = byType(sets, MeldType.CHOW);
        List<Meld> kongs = byType(sets, MeldType.KONG);

        boolean allConcealed = isAllConcealed(ctx);

        if (allConcealed) {
            doubles += 2; // All concealed hand
        }
        if (chows.isEmpty()) {
            doubles += 1; // No Chows
        }
        if (!chows.isEmpty() && chows.size() == sets.size() && allConcealed) {
            doubles += 1; // All Chows (all concealed)
        }

        // Four Kongs + pair. The four-concealed-kongs bonus stacks on top.
        if (kongs.size() == 4 && sets.size() == 4) {
            doubles += 4; // All Kongs
            if (allKongsConcealed(kongs)) {
                doubles += 4; // All Concealed Kongs (separate, stacks)
            }
        }

        doubles += doublePungDoubles(sets);
        doubles += mixedDoubleChowDoubles(chows);
        doubles += shortStraightDoubles(chows);
        doubles += twoTerminalChowsDoubles(chows);

        if (isAllHonours(ctx)) {
            doubles += 10; // All Honours
        } else if (isAllOneSuitWithHonours(ctx)) {
            doubles += 7; // All One Suit with Honours
        }

        // Win-condition doubles (flags).
        if (ctx.isHeavenlyHand()) {
            doubles += 13;
        }
        if (ctx.isEarthlyHand()) {
            doubles += 13;
        }
        if (ctx.isHumanHand()) {
            doubles += 13;
        }
        if (ctx.isMahjongOnLastDrawnTile()) {
            doubles += 2;
        }
        if (ctx.isMahjongOnFinalDiscard()) {
            doubles += 1;
        }

        return doubles;
    }

    private boolean isAllConcealed(WinContext ctx) {
        for (Meld meld : ctx.getMelds()) {
            if (!meld.isConcealed()) {
                return false;
            }
        }
        return ctx.getPair() == null || ctx.getPair().isConcealed();
    }

    private boolean allKongsConcealed(List<Meld> kongs) {
        for (Meld kong : kongs) {
            if (!kong.isConcealed()) {
                return false;
            }
        }
        return true;
    }

    /** Two pungs/kongs of the same numeric rank in two different suited suits = 2 doubles each. */
    private int doublePungDoubles(List<Meld> sets) {
        List<Meld> pungLike = new ArrayList<Meld>();
        for (Meld meld : sets) {
            if (meld.getType() == MeldType.PUNG || meld.getType() == MeldType.KONG) {
                if (meld.representative().isSuited()) {
                    pungLike.add(meld);
                }
            }
        }
        int doubles = 0;
        boolean[] used = new boolean[pungLike.size()];
        for (int i = 0; i < pungLike.size(); i++) {
            if (used[i]) {
                continue;
            }
            Tile a = pungLike.get(i).representative();
            for (int j = i + 1; j < pungLike.size(); j++) {
                if (used[j]) {
                    continue;
                }
                Tile b = pungLike.get(j).representative();
                if (a.getRank() == b.getRank() && a.getSuit() != b.getSuit()) {
                    used[i] = true;
                    used[j] = true;
                    doubles += 2;
                    break;
                }
            }
        }
        return doubles;
    }

    /** The same chow (same starting rank) in two different suits = 1 double each pairing. */
    private int mixedDoubleChowDoubles(List<Meld> chows) {
        int doubles = 0;
        boolean[] used = new boolean[chows.size()];
        for (int i = 0; i < chows.size(); i++) {
            if (used[i]) {
                continue;
            }
            Tile a = chows.get(i).representative();
            for (int j = i + 1; j < chows.size(); j++) {
                if (used[j]) {
                    continue;
                }
                Tile b = chows.get(j).representative();
                if (a.getRank() == b.getRank() && a.getSuit() != b.getSuit()) {
                    used[i] = true;
                    used[j] = true;
                    doubles += 1;
                    break;
                }
            }
        }
        return doubles;
    }

    /** Two consecutive chows in the same suit (1-2-3 + 4-5-6, or 4-5-6 + 7-8-9) = 1 double each. */
    private int shortStraightDoubles(List<Meld> chows) {
        int doubles = 0;
        boolean[] used = new boolean[chows.size()];
        for (int i = 0; i < chows.size(); i++) {
            if (used[i]) {
                continue;
            }
            Tile a = chows.get(i).representative();
            for (int j = i + 1; j < chows.size(); j++) {
                if (used[j]) {
                    continue;
                }
                Tile b = chows.get(j).representative();
                if (a.getSuit() != b.getSuit()) {
                    continue;
                }
                int lo = Math.min(a.getRank(), b.getRank());
                int hi = Math.max(a.getRank(), b.getRank());
                if ((lo == 1 && hi == 4) || (lo == 4 && hi == 7)) {
                    used[i] = true;
                    used[j] = true;
                    doubles += 1;
                    break;
                }
            }
        }
        return doubles;
    }

    /** A 1-2-3 chow paired with a 7-8-9 chow (same or different suit) = 1 double each pairing. */
    private int twoTerminalChowsDoubles(List<Meld> chows) {
        int doubles = 0;
        boolean[] used = new boolean[chows.size()];
        for (int i = 0; i < chows.size(); i++) {
            if (used[i]) {
                continue;
            }
            int ri = chows.get(i).representative().getRank();
            for (int j = i + 1; j < chows.size(); j++) {
                if (used[j]) {
                    continue;
                }
                int rj = chows.get(j).representative().getRank();
                if ((ri == 1 && rj == 7) || (ri == 7 && rj == 1)) {
                    used[i] = true;
                    used[j] = true;
                    doubles += 1;
                    break;
                }
            }
        }
        return doubles;
    }

    private boolean isAllHonours(WinContext ctx) {
        boolean any = false;
        for (Tile tile : allHandTiles(ctx)) {
            any = true;
            if (!tile.isHonor()) {
                return false;
            }
        }
        return any;
    }

    /** All suited tiles share one suit and at least one honor is present. */
    private boolean isAllOneSuitWithHonours(WinContext ctx) {
        Suit suitedSuit = null;
        boolean hasHonor = false;
        boolean hasSuited = false;
        for (Tile tile : allHandTiles(ctx)) {
            if (tile.isHonor()) {
                hasHonor = true;
            } else if (tile.isSuited()) {
                hasSuited = true;
                if (suitedSuit == null) {
                    suitedSuit = tile.getSuit();
                } else if (suitedSuit != tile.getSuit()) {
                    return false;
                }
            } else {
                return false; // bonus tile leaked into the hand body
            }
        }
        return hasHonor && hasSuited;
    }

    /** Every tile making up the four sets and the pair (excludes bonus tiles). */
    private List<Tile> allHandTiles(WinContext ctx) {
        List<Tile> tiles = new ArrayList<Tile>();
        for (Meld meld : ctx.getMelds()) {
            tiles.addAll(meld.getTiles());
        }
        if (ctx.getPair() != null) {
            tiles.addAll(ctx.getPair().getTiles());
        }
        return tiles;
    }

    private List<Meld> byType(List<Meld> melds, MeldType type) {
        List<Meld> out = new ArrayList<Meld>();
        for (Meld meld : melds) {
            if (meld.getType() == type) {
                out.add(meld);
            }
        }
        return out;
    }
}
