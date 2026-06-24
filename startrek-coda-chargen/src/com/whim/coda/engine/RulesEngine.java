package com.whim.coda.engine;

import com.whim.coda.model.Attribute;
import com.whim.coda.model.AttributeSet;
import com.whim.coda.model.CharacterSheet;
import com.whim.coda.model.Reaction;
import com.whim.coda.model.Species;

/**
 * Core Coda rules: base-attribute caps, species application, and derived-value
 * recomputation. All derived values are cached back onto the {@link CharacterSheet}.
 */
public final class RulesEngine {

    private RulesEngine() {
    }

    /** Maximum starting BASE attribute, enforced BEFORE species adjustments. */
    public static final int MAX_START_ATTRIBUTE = 12;
    /** Defense baseline before the Agility modifier. */
    public static final int BASE_DEFENSE = 7;
    /** Courage starts here (before species bonus). */
    public static final int STARTING_COURAGE = 3;
    /** Renown always starts at zero for a new character. */
    public static final int STARTING_RENOWN = 0;

    /**
     * Enforce the pre-adjustment cap: returns {@code false} (and the UI should
     * block) if any BASE attribute exceeds {@link #MAX_START_ATTRIBUTE}.
     */
    public static boolean validateBaseCaps(AttributeSet attrs) {
        if (attrs == null) {
            return false;
        }
        for (Attribute a : Attribute.values()) {
            if (attrs.getBase(a) > MAX_START_ATTRIBUTE) {
                return false;
            }
        }
        return true;
    }

    /** Copy the selected species' attribute mods into {@code attrs.speciesMod}. */
    public static void applySpecies(CharacterSheet sheet) {
        if (sheet == null) {
            throw new IllegalArgumentException("sheet must not be null");
        }
        AttributeSet attrs = sheet.getAttributes();
        Species species = sheet.getSpecies();
        for (Attribute a : Attribute.values()) {
            int mod = (species == null) ? 0 : species.getMod(a);
            attrs.setSpeciesMod(a, mod);
        }
    }

    /**
     * Recompute ALL derived values and cache them on the sheet:
     * <ul>
     *   <li>Health  = adjusted Vitality + Strength modifier</li>
     *   <li>Defense = 7 + Agility modifier</li>
     *   <li>Quickness = max(mod Perception, mod Agility)</li>
     *   <li>Savvy     = max(mod Presence, mod Perception)</li>
     *   <li>Stamina   = max(mod Strength, mod Vitality)</li>
     *   <li>Willpower = max(mod Intellect, mod Vitality)</li>
     *   <li>Courage = 3 + species.bonusCourage; Renown = 0</li>
     * </ul>
     */
    public static void recomputeDerived(CharacterSheet sheet) {
        if (sheet == null) {
            throw new IllegalArgumentException("sheet must not be null");
        }
        AttributeSet attrs = sheet.getAttributes();

        int strMod = attrs.getModifier(Attribute.STRENGTH);
        int agiMod = attrs.getModifier(Attribute.AGILITY);
        int intMod = attrs.getModifier(Attribute.INTELLECT);
        int vitMod = attrs.getModifier(Attribute.VITALITY);
        int preMod = attrs.getModifier(Attribute.PRESENCE);
        int perMod = attrs.getModifier(Attribute.PERCEPTION);

        int health = attrs.getAdjusted(Attribute.VITALITY) + strMod;
        sheet.setHealth(health);

        sheet.setDefense(BASE_DEFENSE + agiMod);

        sheet.setReaction(Reaction.QUICKNESS, Math.max(perMod, agiMod));
        sheet.setReaction(Reaction.SAVVY, Math.max(preMod, perMod));
        sheet.setReaction(Reaction.STAMINA, Math.max(strMod, vitMod));
        sheet.setReaction(Reaction.WILLPOWER, Math.max(intMod, vitMod));

        Species species = sheet.getSpecies();
        int bonusCourage = (species == null) ? 0 : species.getBonusCourage();
        sheet.setCourage(STARTING_COURAGE + bonusCourage);

        sheet.setRenown(STARTING_RENOWN);
    }
}
