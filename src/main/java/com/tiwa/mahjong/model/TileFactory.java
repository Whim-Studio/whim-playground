package com.tiwa.mahjong.model;

import com.tiwa.mahjong.api.Suit;
import com.tiwa.mahjong.api.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the full 144-tile set of Tiwa's Mah Jong Rulebook (Section 1):
 * <ul>
 *   <li>Dots, Bamboo, Characters: ranks 1-9, x4 each = 108.</li>
 *   <li>Winds E/S/W/N: x4 = 16.</li>
 *   <li>Dragons Red/Green/White: x4 = 12.</li>
 *   <li>Flowers 1-4: x1 each = 4.</li>
 *   <li>Seasons 1-4: x1 each = 4.</li>
 * </ul>
 * Total = 108 + 16 + 12 + 4 + 4 = 144.
 */
public final class TileFactory {

    public static final int TOTAL_TILES = 144;

    private TileFactory() {
    }

    /** Builds a fresh list of exactly 144 tiles with correct multiplicities. */
    public static List<Tile> buildFullSet() {
        List<Tile> tiles = new ArrayList<Tile>(TOTAL_TILES);

        // Suited: 1-9, four copies each.
        addCopies(tiles, Suit.DOTS, 1, 9, 4);
        addCopies(tiles, Suit.BAMBOO, 1, 9, 4);
        addCopies(tiles, Suit.CHARACTERS, 1, 9, 4);

        // Winds: 1-4 (E/S/W/N), four copies each.
        addCopies(tiles, Suit.WIND, 1, 4, 4);

        // Dragons: 1-3 (Red/Green/White), four copies each.
        addCopies(tiles, Suit.DRAGON, 1, 3, 4);

        // Bonus: four distinct flowers and four distinct seasons, one copy each.
        addCopies(tiles, Suit.FLOWER, 1, 4, 1);
        addCopies(tiles, Suit.SEASON, 1, 4, 1);

        return tiles;
    }

    private static void addCopies(List<Tile> tiles, Suit suit, int minRank, int maxRank, int copies) {
        for (int rank = minRank; rank <= maxRank; rank++) {
            for (int c = 0; c < copies; c++) {
                tiles.add(new StandardTile(suit, rank));
            }
        }
    }
}
