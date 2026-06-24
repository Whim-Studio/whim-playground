package com.whim.coda.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Holds the six BASE attribute scores (pre-species) and the species modifiers,
 * exposing adjusted scores and per-attribute modifiers.
 */
public class AttributeSet {

    private final Map<Attribute, Integer> base = new EnumMap<Attribute, Integer>(Attribute.class);
    private final Map<Attribute, Integer> speciesMod = new EnumMap<Attribute, Integer>(Attribute.class);

    public AttributeSet() {
        for (Attribute a : Attribute.values()) {
            base.put(a, 0);
            speciesMod.put(a, 0);
        }
    }

    public int getBase(Attribute a) {
        Integer v = base.get(a);
        return v == null ? 0 : v.intValue();
    }

    public void setBase(Attribute a, int score) {
        base.put(a, score);
    }

    /** Set by engine when species applied. */
    public int getSpeciesMod(Attribute a) {
        Integer v = speciesMod.get(a);
        return v == null ? 0 : v.intValue();
    }

    public void setSpeciesMod(Attribute a, int mod) {
        speciesMod.put(a, mod);
    }

    /** base + speciesMod */
    public int getAdjusted(Attribute a) {
        return getBase(a) + getSpeciesMod(a);
    }

    /** = modifierFor(getAdjusted(a)) */
    public int getModifier(Attribute a) {
        return modifierFor(getAdjusted(a));
    }

    /**
     * Coda attribute-modifier table (authoritative):
     * 1->-3, 2->-2, 3->-2, 4->-1, 5->-1, 6->0, 7->0, 8->+1, 9->+1,
     * 10->+2, 11->+2, 12->+3, 13->+3. Modifiers are clamped to the range -3..+3.
     */
    public static int modifierFor(int score) {
        int mod = Math.floorDiv(score - 6, 2);
        if (mod < -3) {
            mod = -3;
        } else if (mod > 3) {
            mod = 3;
        }
        return mod;
    }
}
