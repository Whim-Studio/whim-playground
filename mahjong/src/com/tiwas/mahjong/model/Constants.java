package com.tiwas.mahjong.model;

/**
 * All scoring and game constants in one place, as required by the rulebook
 * (§6 Scoring). Point values follow the brief exactly.
 */
public final class Constants {

    private Constants() {
    }

    // ---- table / game shape ----
    public static final int NUM_PLAYERS = 4;
    public static final int STARTING_HAND_SIZE = 13;
    public static final int TILES_IN_SET = 144;
    public static final int HANDS_PER_ROUND = 4;     // four seats deal in turn
    public static final int ROUNDS_PER_GAME = 4;     // four prevailing winds
    public static final int TOTAL_HANDS = HANDS_PER_ROUND * ROUNDS_PER_GAME; // 16
    public static final int CLAIM_TIMEOUT_SECONDS = 6;

    // ---- points limit ----
    public static final int DEFAULT_LIMIT = 1000;

    // ---- base points (§6) ----
    public static final int PUNG_SIMPLE_EXPOSED = 2;
    public static final int PUNG_SIMPLE_CONCEALED = 4;
    public static final int PUNG_HONOUR_EXPOSED = 4;
    public static final int PUNG_HONOUR_CONCEALED = 8;

    public static final int KONG_SIMPLE_EXPOSED = 4;
    public static final int KONG_SIMPLE_CONCEALED = 8;
    public static final int KONG_HONOUR_EXPOSED = 8;
    public static final int KONG_HONOUR_CONCEALED = 16;

    public static final int CHOW_POINTS = 0;
    public static final int FLOWER_OR_SEASON_POINTS = 4;

    // ---- doubles (each value is a number of doublings, applied multiplicatively) ----
    public static final int DBL_ALL_CONCEALED = 2;        // Fully Concealed Hand
    public static final int DBL_NO_CHOWS = 1;
    public static final int DBL_ALL_CHOWS = 1;
    public static final int DBL_ALL_KONGS = 4;
    public static final int DBL_ALL_CONCEALED_KONGS = 4;  // stacks with All Kongs
    public static final int DBL_DOUBLE_PUNG = 2;          // same number, two suits
    public static final int DBL_MIXED_DOUBLE_CHOW = 1;    // same chow, two suits
    public static final int DBL_SHORT_STRAIGHT = 1;       // 123+456 or 456+789 same suit
    public static final int DBL_TWO_TERMINAL_CHOWS = 1;   // 123 & 789 same suit
    public static final int DBL_ALL_ONE_SUIT_WITH_HONOURS = 7;
    public static final int DBL_ALL_HONOURS = 10;
    public static final int DBL_HEAVENLY_HAND = 13;       // dealer wins on starting hand
    public static final int DBL_EARTHLY_HAND = 13;        // non-dealer wins on first discard
    public static final int DBL_HUMAN_HAND = 13;          // win before discarding any tile

    // ---- mahjong-circumstance doubles ----
    public static final int DBL_LAST_TILE_WIN = 2;        // win on last drawn tile
    public static final int DBL_FINAL_DISCARD_WIN = 1;    // win on the final discard

    // ---- special / fixed awards ----
    public static final int FIRST_TILE_MAHJONG_POINTS = 1000; // mahjong on the very first tile
    public static final int FALSE_MAHJONG_PENALTY = 1000;     // paid to EACH other player

    /** Mahjong bonus = 1% of the limit, rounded down. */
    public static int mahjongBonus(int limit) {
        return limit / 100;
    }
}
