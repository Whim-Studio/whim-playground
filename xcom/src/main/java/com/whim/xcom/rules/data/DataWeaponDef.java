package com.whim.xcom.rules.data;

import com.whim.xcom.model.DamageType;
import com.whim.xcom.model.FireMode;
import com.whim.xcom.rules.def.WeaponDef;

/**
 * Immutable {@link WeaponDef} populated from a data pack (Gson sets the private
 * fields directly; the interface methods read them). Per-mode stats are stored as
 * flat fields to keep the JSON schema simple and self-describing.
 */
public final class DataWeaponDef implements WeaponDef {

    private String id;
    private String name;
    private int power;
    private DamageType damageType = DamageType.ARMOR_PIERCING;
    private boolean twoHanded;
    private int weight;
    private int clipSize;

    // Per-mode accuracy % / TU % / shots; -1 (default) means the mode is unsupported.
    private int snapAccuracy = -1;
    private int snapTu = -1;
    private int aimedAccuracy = -1;
    private int aimedTu = -1;
    private int autoAccuracy = -1;
    private int autoTu = -1;
    private int autoShots = 1;

    /** Full constructor for programmatic (non-data-pack) construction and tests. */
    public DataWeaponDef(String id, String name, int power, DamageType damageType,
                         boolean twoHanded, int weight, int clipSize,
                         int snapAccuracy, int snapTu,
                         int aimedAccuracy, int aimedTu,
                         int autoAccuracy, int autoTu, int autoShots) {
        this.id = id;
        this.name = name;
        this.power = power;
        this.damageType = damageType;
        this.twoHanded = twoHanded;
        this.weight = weight;
        this.clipSize = clipSize;
        this.snapAccuracy = snapAccuracy;
        this.snapTu = snapTu;
        this.aimedAccuracy = aimedAccuracy;
        this.aimedTu = aimedTu;
        this.autoAccuracy = autoAccuracy;
        this.autoTu = autoTu;
        this.autoShots = autoShots;
    }

    /** No-arg constructor for Gson. */
    DataWeaponDef() {
    }

    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public int power() { return power; }
    @Override public DamageType damageType() { return damageType; }
    @Override public boolean twoHanded() { return twoHanded; }
    @Override public int weight() { return weight; }
    @Override public int clipSize() { return clipSize; }

    @Override
    public boolean supports(FireMode mode) {
        return accuracyPercent(mode) >= 0 && tuPercent(mode) >= 0;
    }

    @Override
    public int accuracyPercent(FireMode mode) {
        switch (mode) {
            case SNAP:  return snapAccuracy;
            case AIMED: return aimedAccuracy;
            case AUTO:  return autoAccuracy;
            default:    return -1;
        }
    }

    @Override
    public int tuPercent(FireMode mode) {
        switch (mode) {
            case SNAP:  return snapTu;
            case AIMED: return aimedTu;
            case AUTO:  return autoTu;
            default:    return -1;
        }
    }

    @Override
    public int shots(FireMode mode) {
        return mode == FireMode.AUTO ? Math.max(1, autoShots) : 1;
    }
}
