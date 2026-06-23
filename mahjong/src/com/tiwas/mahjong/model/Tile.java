package com.tiwas.mahjong.model;

/**
 * An immutable description of a single physical tile.
 *
 * Only the fields relevant to a tile's category are populated; the rest are
 * left null / zero:
 *  - Suited tiles (DOTS/BAMBOO/CHARACTERS): suit + rank (1-9).
 *  - Wind tiles:   suit=WIND + wind.
 *  - Dragon tiles: suit=DRAGON + dragon.
 *  - Flower tiles: suit=FLOWER + flower index (1-4).
 *  - Season tiles: suit=SEASON + season index (1-4).
 *
 * Equality compares the visible face, so the four East winds are all equal and
 * can form a pung, while flower #1 and flower #2 are distinct tiles.
 */
public final class Tile {

    private final TileSuit suit;
    private final int rank;        // 1-9 for suited tiles, else 0
    private final Wind wind;       // for WIND tiles
    private final Dragon dragon;   // for DRAGON tiles
    private final int flower;      // 1-4 for FLOWER tiles, else 0
    private final int season;      // 1-4 for SEASON tiles, else 0

    private Tile(TileSuit suit, int rank, Wind wind, Dragon dragon, int flower, int season) {
        this.suit = suit;
        this.rank = rank;
        this.wind = wind;
        this.dragon = dragon;
        this.flower = flower;
        this.season = season;
    }

    // ---- factory methods ----

    public static Tile suited(TileSuit suit, int rank) {
        if (!suit.isSuited()) {
            throw new IllegalArgumentException("Not a suited tile: " + suit);
        }
        if (rank < 1 || rank > 9) {
            throw new IllegalArgumentException("Rank out of range: " + rank);
        }
        return new Tile(suit, rank, null, null, 0, 0);
    }

    public static Tile wind(Wind wind) {
        return new Tile(TileSuit.WIND, 0, wind, null, 0, 0);
    }

    public static Tile dragon(Dragon dragon) {
        return new Tile(TileSuit.DRAGON, 0, null, dragon, 0, 0);
    }

    public static Tile flower(int index) {
        return new Tile(TileSuit.FLOWER, 0, null, null, index, 0);
    }

    public static Tile season(int index) {
        return new Tile(TileSuit.SEASON, 0, null, null, 0, index);
    }

    // ---- accessors ----

    public TileSuit getSuit() {
        return suit;
    }

    public int getRank() {
        return rank;
    }

    public Wind getWind() {
        return wind;
    }

    public Dragon getDragon() {
        return dragon;
    }

    public int getFlower() {
        return flower;
    }

    public int getSeason() {
        return season;
    }

    // ---- classification helpers ----

    public boolean isSuited() {
        return suit.isSuited();
    }

    public boolean isHonour() {
        return suit.isHonour();
    }

    public boolean isBonus() {
        return suit.isBonus();
    }

    /** Terminal = rank 1 or 9 of a numbered suit. */
    public boolean isTerminal() {
        return isSuited() && (rank == 1 || rank == 9);
    }

    /** Terminal or honour: the tiles that matter for the Thirteen Orphans hand. */
    public boolean isTerminalOrHonour() {
        return isHonour() || isTerminal();
    }

    /**
     * Two suited tiles can chain into a run if they share a suit and the other
     * tile's rank is one higher than this one's.
     */
    public boolean isConsecutiveWith(Tile other) {
        return isSuited() && other.isSuited()
                && suit == other.suit
                && other.rank == rank + 1;
    }

    // ---- value equality on the visible face ----

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tile)) {
            return false;
        }
        Tile t = (Tile) o;
        return rank == t.rank
                && flower == t.flower
                && season == t.season
                && suit == t.suit
                && wind == t.wind
                && dragon == t.dragon;
    }

    @Override
    public int hashCode() {
        int h = suit == null ? 0 : suit.hashCode();
        h = 31 * h + rank;
        h = 31 * h + (wind == null ? 0 : wind.hashCode());
        h = 31 * h + (dragon == null ? 0 : dragon.hashCode());
        h = 31 * h + flower;
        h = 31 * h + season;
        return h;
    }

    /** A short two-character code, e.g. "5D", "Ew", "Rd", "F2". */
    public String code() {
        switch (suit) {
            case DOTS:       return rank + "D";
            case BAMBOO:     return rank + "B";
            case CHARACTERS: return rank + "C";
            case WIND:       return wind.label().charAt(0) + "w";
            case DRAGON:     return dragon.label().charAt(0) + "d";
            case FLOWER:     return "F" + flower;
            case SEASON:     return "S" + season;
            default:         return "??";
        }
    }

    /** A human-readable name, e.g. "5 of Dots", "East Wind", "Red Dragon". */
    public String displayName() {
        switch (suit) {
            case DOTS:       return rank + " of Dots";
            case BAMBOO:     return rank + " of Bamboo";
            case CHARACTERS: return rank + " of Characters";
            case WIND:       return wind.label() + " Wind";
            case DRAGON:     return dragon.label() + " Dragon";
            case FLOWER:     return "Flower " + flower;
            case SEASON:     return "Season " + season;
            default:         return "Unknown";
        }
    }

    @Override
    public String toString() {
        return code();
    }
}
