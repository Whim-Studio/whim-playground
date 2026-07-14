package com.whim.b5db.model;

/**
 * A single effect in the JSON card-effect DSL.
 *
 * <p>DSL shape (one element of a card's {@code "effects"} array):</p>
 * <pre>
 *   { "type": "GAIN_INFLUENCE", "amount": 2 }
 *   { "type": "GAIN_ATTRIBUTE", "attribute": "MILITARY", "amount": 3 }
 *   { "type": "DRAW", "amount": 1 }
 *   { "type": "GAIN_PRESTIGE", "amount": 2 }
 *   { "type": "DISCARD_OPPONENT", "amount": 1 }
 *   { "type": "TRASH", "amount": 1 }
 * </pre>
 *
 * Effects are intentionally small and declarative so new card behaviour can be
 * added as data rather than code. The {@link com.whim.b5db.engine.EffectResolver}
 * interprets them against a {@link com.whim.b5db.engine.GameState}.
 */
public final class Effect {

    /** The supported effect verbs. */
    public enum Type {
        GAIN_INFLUENCE,
        GAIN_PRESTIGE,
        GAIN_ATTRIBUTE,
        DRAW,
        TRASH,
        DISCARD_OPPONENT,
        ADD_AGENDA_PROGRESS,
        CONVERT_DIPLOMACY
    }

    private final Type type;
    private final int amount;
    private final ContestType attribute; // nullable; only meaningful for GAIN_ATTRIBUTE

    public Effect(Type type, int amount, ContestType attribute) {
        this.type = type;
        this.amount = amount;
        this.attribute = attribute;
    }

    public Type type() {
        return type;
    }

    public int amount() {
        return amount;
    }

    /** @return the target attribute for GAIN_ATTRIBUTE, otherwise null. */
    public ContestType attribute() {
        return attribute;
    }

    @Override
    public String toString() {
        if (type == Type.GAIN_ATTRIBUTE && attribute != null) {
            return type + "(" + attribute + "+" + amount + ")";
        }
        return type + "(" + amount + ")";
    }
}
