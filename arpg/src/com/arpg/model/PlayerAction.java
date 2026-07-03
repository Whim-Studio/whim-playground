package com.arpg.model;

/**
 * An intent produced by the UI and consumed by the engine. Immutable data
 * holder — build one with the nested {@link Builder}. Kept simple and
 * Serializable so it can be logged or replayed.
 */
public final class PlayerAction implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        USE_ABILITY,
        BASIC_ATTACK,
        MOVE_TO_REALM,
        EQUIP_ITEM,
        UNEQUIP_ITEM,
        USE_PET_ABILITY,
        REFORGE_ITEM,
        ADVANCE_ENCOUNTER,
        ALLOCATE_ATTRIBUTE
    }

    private final Type type;
    private final String abilityId;
    private final String itemId;
    private final String realmId;
    private final int targetIndex;
    private final String attribute;

    public PlayerAction(Type type, String abilityId, String itemId, String realmId,
                        int targetIndex, String attribute) {
        if (type == null) {
            throw new IllegalArgumentException("PlayerAction type must not be null");
        }
        this.type = type;
        this.abilityId = abilityId;
        this.itemId = itemId;
        this.realmId = realmId;
        this.targetIndex = targetIndex;
        this.attribute = attribute;
    }

    public Type getType() {
        return type;
    }

    public String getAbilityId() {
        return abilityId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getRealmId() {
        return realmId;
    }

    public int getTargetIndex() {
        return targetIndex;
    }

    public String getAttribute() {
        return attribute;
    }

    // ---- Convenience factory methods for the common cases ----

    public static PlayerAction useAbility(String abilityId, int targetIndex) {
        return new PlayerAction(Type.USE_ABILITY, abilityId, null, null, targetIndex, null);
    }

    public static PlayerAction basicAttack(int targetIndex) {
        return new PlayerAction(Type.BASIC_ATTACK, null, null, null, targetIndex, null);
    }

    public static PlayerAction moveToRealm(String realmId) {
        return new PlayerAction(Type.MOVE_TO_REALM, null, null, realmId, -1, null);
    }

    public static PlayerAction equipItem(String itemId) {
        return new PlayerAction(Type.EQUIP_ITEM, null, itemId, null, -1, null);
    }

    public static PlayerAction unequipItem(String itemId) {
        return new PlayerAction(Type.UNEQUIP_ITEM, null, itemId, null, -1, null);
    }

    public static PlayerAction usePetAbility(String abilityId, int targetIndex) {
        return new PlayerAction(Type.USE_PET_ABILITY, abilityId, null, null, targetIndex, null);
    }

    public static PlayerAction reforgeItem(String itemId) {
        return new PlayerAction(Type.REFORGE_ITEM, null, itemId, null, -1, null);
    }

    public static PlayerAction advanceEncounter() {
        return new PlayerAction(Type.ADVANCE_ENCOUNTER, null, null, null, -1, null);
    }

    public static PlayerAction allocateAttribute(String attribute) {
        return new PlayerAction(Type.ALLOCATE_ATTRIBUTE, null, null, null, -1, attribute);
    }

    /** Fluent builder for callers that prefer it over the factory methods. */
    public static final class Builder {
        private Type type;
        private String abilityId;
        private String itemId;
        private String realmId;
        private int targetIndex = -1;
        private String attribute;

        public Builder type(Type t) {
            this.type = t;
            return this;
        }

        public Builder abilityId(String v) {
            this.abilityId = v;
            return this;
        }

        public Builder itemId(String v) {
            this.itemId = v;
            return this;
        }

        public Builder realmId(String v) {
            this.realmId = v;
            return this;
        }

        public Builder targetIndex(int v) {
            this.targetIndex = v;
            return this;
        }

        public Builder attribute(String v) {
            this.attribute = v;
            return this;
        }

        public PlayerAction build() {
            return new PlayerAction(type, abilityId, itemId, realmId, targetIndex, attribute);
        }
    }

    @Override
    public String toString() {
        return "PlayerAction{" + type
                + (abilityId != null ? ", ability=" + abilityId : "")
                + (itemId != null ? ", item=" + itemId : "")
                + (realmId != null ? ", realm=" + realmId : "")
                + (targetIndex >= 0 ? ", target=" + targetIndex : "")
                + (attribute != null ? ", attr=" + attribute : "")
                + "}";
    }
}
