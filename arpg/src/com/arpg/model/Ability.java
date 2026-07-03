package com.arpg.model;

/**
 * A castable ability. Pure data: the engine reads {@code effectType},
 * {@code magnitude}, {@code targetType}, {@code resourceCost} and
 * {@code cooldown} to resolve what happens — no logic lives here.
 */
public final class Ability implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final String description;
    private final int resourceCost;
    private final int cooldown;          // in combat ticks
    private final EffectType effectType;
    private final int magnitude;
    private final TargetType targetType;
    private final BuffDebuff appliedBuff; // for BUFF/DEBUFF abilities; may be null
    private final String summonPetId;     // for SUMMON abilities; may be null

    public Ability(String id, String name, String description, int resourceCost, int cooldown,
                   EffectType effectType, int magnitude, TargetType targetType,
                   BuffDebuff appliedBuff, String summonPetId) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Ability id must not be blank");
        }
        if (effectType == null || targetType == null) {
            throw new IllegalArgumentException("Ability effectType and targetType are required");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.resourceCost = Math.max(0, resourceCost);
        this.cooldown = Math.max(0, cooldown);
        this.effectType = effectType;
        this.magnitude = Math.max(0, magnitude);
        this.targetType = targetType;
        this.appliedBuff = appliedBuff;
        this.summonPetId = summonPetId;
    }

    /** Convenience for a plain damage/heal ability with no buff or summon. */
    public Ability(String id, String name, String description, int resourceCost, int cooldown,
                   EffectType effectType, int magnitude, TargetType targetType) {
        this(id, name, description, resourceCost, cooldown, effectType, magnitude, targetType, null, null);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getResourceCost() {
        return resourceCost;
    }

    public int getCooldown() {
        return cooldown;
    }

    public EffectType getEffectType() {
        return effectType;
    }

    public int getMagnitude() {
        return magnitude;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public BuffDebuff getAppliedBuff() {
        return appliedBuff;
    }

    public String getSummonPetId() {
        return summonPetId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ability)) {
            return false;
        }
        return id.equals(((Ability) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name + " (" + effectType + " " + magnitude + ", cost " + resourceCost + ")";
    }
}
