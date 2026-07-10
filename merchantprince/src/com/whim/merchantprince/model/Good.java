package com.whim.merchantprince.model;

/**
 * The tradeable commodities confirmed for Merchant Prince (1994): Venetian glass,
 * French wine ("grog"), Holy Land relics, African gold & ivory, Chinese silk and
 * Indonesian spices (the original notably excludes salt and slaves — GAME_DESIGN_REFERENCE §3).
 *
 * <p>Each good carries a nominal "world" base value used only as the seed for a
 * city's local price; the live price is produced by the pricing engine from local
 * supply. Goods are indexed by {@link #ordinal()} so cities can hold flat int/double
 * arrays keyed by good — keep this enum's order stable, it is the array layout.
 */
public enum Good {
    GLASS ("Venetian Glass",  40),
    GROG  ("French Wine",     20),
    RELICS("Holy Relics",    120),
    GOLD  ("African Gold",   150),
    IVORY ("Ivory",           90),
    SILK  ("Chinese Silk",   110),
    SPICES("Spices",          80);

    public static final Good[] ALL = values();
    public static final int COUNT = ALL.length;

    public final String label;
    /** Nominal world value in florins per unit — seed for local pricing only. */
    public final int nominalValue;

    Good(String label, int nominalValue) {
        this.label = label;
        this.nominalValue = nominalValue;
    }
}
