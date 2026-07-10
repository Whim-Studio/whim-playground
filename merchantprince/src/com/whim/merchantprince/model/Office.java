package com.whim.merchantprince.model;

/**
 * Venetian state and Church positions a family can acquire through bribery and
 * influence (GAME_DESIGN_REFERENCE §6). State offices are won by bribing the
 * Council of Ten; DOGE is the pinnacle of the Republic; CARDINAL/POPE are the
 * ecclesiastical track that lets a family sway the papacy.
 *
 * <p>{@code bribeCost} seeds the politics engine's cost tables; {@code netWorth}
 * is the florin value the office contributes at scoring time (a bribed senator or
 * cardinal counts toward final net worth — the confirmed win rule, §7).
 */
public enum Office {
    MINISTER    ("Minister",      2000,  1500),
    ADMIRAL     ("Admiral",       3000,  2500),
    GENERAL     ("General",       3000,  2500),
    COUNCIL_HEAD("Head of the Council of Ten", 6000, 5000),
    DOGE        ("Doge of Venice", 15000, 15000),
    CARDINAL    ("Cardinal",       5000,  4000),
    POPE        ("Pope",          20000, 20000);

    public static final Office[] ALL = values();

    public final String label;
    public final int bribeCost;
    public final int netWorth;

    Office(String label, int bribeCost, int netWorth) {
        this.label = label;
        this.bribeCost = bribeCost;
        this.netWorth = netWorth;
    }
}
