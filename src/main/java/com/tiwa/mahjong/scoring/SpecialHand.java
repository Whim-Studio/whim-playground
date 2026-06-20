package com.tiwa.mahjong.scoring;

/**
 * Special limit hands from Sections 5-6 of Tiwa's Mah Jong Rulebook that score as a flat
 * Limit Hand instead of going through the normal base + bonus + doubles pipeline.
 *
 * <ul>
 *   <li>{@link #NONE} - an ordinary hand; score it through the full pipeline.</li>
 *   <li>{@link #THIRTEEN_ORPHANS} - the 13 Orphans limit hand (must be fully concealed).</li>
 *   <li>{@link #ALL_FLOWERS_AND_SEASONS} - the All Flowers &amp; Seasons limit hand.</li>
 *   <li>{@link #MAHJONG_ON_FIRST_TILE} - Mahjong on the very first tile: a flat Points Limit win.</li>
 * </ul>
 *
 * <p>For every value other than {@link #NONE} the scorer short-circuits the doubles step and the
 * hand scores exactly the Points Limit (the Mahjong bonus is folded in but the result is capped at
 * the limit, so the reported value equals the Limit Hand value).</p>
 */
public enum SpecialHand {
    NONE,
    THIRTEEN_ORPHANS,
    ALL_FLOWERS_AND_SEASONS,
    MAHJONG_ON_FIRST_TILE
}
