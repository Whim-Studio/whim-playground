package com.whim.civ.domain;

/**
 * Governments affect corruption, trade, and military unrest. Values are data-driven.
 *
 * <p>COMMUNISM resolution: the contract notes that COMMUNISM may reference a real
 * {@code TechType.COMMUNISM} constant OR, if no such constant exists in the tech subset,
 * use a {@code null} prereq. This domain keeps COMMUNISM OUT of the {@link TechType} subset
 * (to avoid inventing a fake/placeholder constant), so the COMMUNISM government is given a
 * {@code null} prereq here. It is therefore treated as always-eligible by prereq checks.
 */
public enum Government {
    // maxTradePerTile, corruptionPct, martialLawUnits, tradeBonusEnabled, prereqTech
    ANARCHY  (1, 75, 0, false, null),
    DESPOTISM(2, 50, 3, false, null),   // tiles producing 3+ of a yield lose 1 (despotism penalty)
    MONARCHY (3, 30, 3, false, TechType.MONARCHY),
    COMMUNISM(3, 0,  3, false, null),   // null prereq: COMMUNISM tech kept out of the subset
    REPUBLIC (5, 20, 0, true,  TechType.THE_REPUBLIC),  // +1 trade on tiles already making trade
    DEMOCRACY(5, 0,  0, true,  TechType.DEMOCRACY);

    private final int maxTradePerTile;
    private final int corruptionPct;
    private final int martialLawUnits;
    private final boolean tradeBonusEnabled;
    private final TechType prereq;

    Government(int maxTradePerTile, int corruptionPct, int martialLawUnits,
               boolean tradeBonusEnabled, TechType prereq) {
        this.maxTradePerTile = maxTradePerTile;
        this.corruptionPct = corruptionPct;
        this.martialLawUnits = martialLawUnits;
        this.tradeBonusEnabled = tradeBonusEnabled;
        this.prereq = prereq;
    }

    public int getMaxTradePerTile() { return maxTradePerTile; }
    public int getCorruptionPct() { return corruptionPct; }
    public int getMartialLawUnits() { return martialLawUnits; } // # of military units that quell unhappiness
    public boolean hasTradeBonus() { return tradeBonusEnabled; } // Republic/Democracy +1 trade on trade tiles
    public boolean appliesDespotismPenalty() { return this == DESPOTISM || this == ANARCHY; }
    public TechType getPrereq() { return prereq; }
}
