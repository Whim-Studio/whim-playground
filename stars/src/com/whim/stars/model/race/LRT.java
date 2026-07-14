package com.whim.stars.model.race;

/**
 * Lesser Racial Traits — the optional point-cost trade-offs a race may stack on
 * top of its {@link PRT}. A race can take any combination; some are advantages
 * (positive point cost) and some are disadvantages (they refund points).
 *
 * <p>This is the common thirteen-trait set. Numeric effects are applied by the
 * engine where modeled; the enum captures identity only.
 */
public enum LRT {
    IFE("Improved Fuel Efficiency"),
    TT("Total Terraforming"),
    ARM("Advanced Remote Mining"),
    ISB("Improved Starbases"),
    GR("Generalized Research"),
    UR("Ultimate Recycling"),
    NRSE("No Ram Scoop Engines"),
    CE("Cheap Engines"),
    OBRM("Only Basic Remote Mining"),
    NAS("No Advanced Scanners"),
    BET("Bleeding Edge Technology"),
    MA("Mineral Alchemy"),
    RS("Regenerating Shields");

    private final String label;

    LRT(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
