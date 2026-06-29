package com.whim.ruinlander.domain;

/** A wearable armor item. */
public class Armor extends Item {
    private final double damageReduction; // 0..1 fraction of incoming damage absorbed
    private final double coverage;        // 0..1 chance the reduction applies on a hit

    public Armor(String id, String name, double weight,
                 double damageReduction, double coverage) {
        super(id, name, ItemCategory.ARMOR, weight, false);
        this.damageReduction = clamp01(damageReduction);
        this.coverage = clamp01(coverage);
    }

    public double getDamageReduction() { return damageReduction; }
    public double getCoverage() { return coverage; }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
