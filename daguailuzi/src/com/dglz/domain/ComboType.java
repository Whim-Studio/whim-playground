package com.dglz.domain;

/** Combination types, ascending strength within a road. */
public enum ComboType {
    // 1/2/3 road:
    SINGLE(Road.SINGLE),
    PAIR(Road.PAIR),
    TRIPLE(Road.TRIPLE),
    // 5 road, ascending:
    STRAIGHT(Road.FIVE),        // 顺子
    FLUSH(Road.FIVE),           // 同花
    FULL_HOUSE(Road.FIVE),      // 葫芦
    FOUR_PLUS_ONE(Road.FIVE),   // 炸弹
    STRAIGHT_FLUSH(Road.FIVE),  // 同花顺
    FIVE_OF_A_KIND(Road.FIVE);  // 5根

    private final Road road;

    ComboType(Road road) {
        this.road = road;
    }

    public Road road() {
        return road;
    }

    /** Intra-road tier ordering. For the FIVE road this is the ordinal among FIVE-road types. */
    public int tier() {
        if (road == Road.FIVE) {
            return ordinal() - STRAIGHT.ordinal();
        }
        return 0;
    }
}
