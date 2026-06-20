package com.tiwa.mahjong.api;

/**
 * The kinds of groups a hand is built from. A legal Mah Jong hand is 4 sets + 1 pair (Section 5).
 *
 * <ul>
 *   <li>{@link #PUNG} - three identical tiles.</li>
 *   <li>{@link #KONG} - four identical tiles (counts as one set; draws a replacement tile).</li>
 *   <li>{@link #CHOW} - three consecutive suited tiles. Chows MUST be concealed, may NOT be claimed
 *       from discards, and are worth 0 points.</li>
 *   <li>{@link #PAIR} - two identical tiles (the eyes).</li>
 * </ul>
 */
public enum MeldType {
    PUNG, KONG, CHOW, PAIR
}
