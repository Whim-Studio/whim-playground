package com.whim.tippingpoint.domain;

/** Fixed rule constants for Tipping Point. */
public final class Rules {
    private Rules() {}
    public static final int   MIN_PLAYERS = 2;
    public static final int   MAX_PLAYERS = 4;
    public static final int   START_CASH_FLOW = 3;      // $ granted each Development Phase
    public static final int   START_CASH = 5;           // starting bank per player
    public static final int   START_FOOD = 2;           // starting foodProduction
    public static final int   CITIZEN_COST = 2;         // cash to recruit any citizen
    public static final int   FARMER_FOOD_YIELD = 2;    // foodProduction added per farmer recruited
    public static final int   RISK_X2_AT = 4;           // co2 >= 4 -> at least X2
    public static final int   RISK_X3_AT = 8;           // co2 >= 8 -> X3
    public static final int   CO2_PER_EXTRA_CARD = 5;   // weather cards = 1 + globalCo2/5
    public static final int   MAX_WEATHER_CARDS = 5;
    public static final int   TIPPING_POINT_CO2 = 30;   // global; >= is collective loss
    public static final int   TARGET_POPULATION = 20;   // citizens to win
    public static final int   START_YEAR = 2020;
    public static final int   END_YEAR = 2100;
    public static final int   YEARS_PER_ROUND = 10;
    public static final int   MARKET_ROWS = 3;
    public static final int   MARKET_COLS = 4;          // 3x4 = 12 face-up developments
}
