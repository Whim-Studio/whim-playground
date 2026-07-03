package com.arpg.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The four original playable archetypes. Each carries its starting attribute
 * spread, base pools, the display name of its resource, and the ids of the
 * abilities it can learn. The concrete {@link Ability} objects for these ids are
 * built in {@link GameContent}.
 */
public enum CharacterClass {

    IRONCLAD_VANGUARD(
            "Ironclad Vanguard",
            "A frontline bulwark who trades blows and shrugs off punishment.",
            "Fury",
            /* str */ 9, /* agi */ 4, /* int */ 2, /* vit */ 9,
            /* health */ 140, /* resource */ 60,
            "abil.van.cleave", "abil.van.shieldbreak", "abil.van.rallying_roar", "abil.van.ironskin"),

    GALE_WARDEN(
            "Gale Warden",
            "A precise ranged skirmisher who strikes from the wind's edge.",
            "Focus",
            5, 10, 3, 5,
            105, 70,
            "abil.war.piercing_shot", "abil.war.volley", "abil.war.hunters_mark", "abil.war.evasive_roll"),

    EMBERWEAVER(
            "Emberweaver",
            "An arcane caster who bends flame and frost to raw destruction.",
            "Mana",
            2, 4, 11, 4,
            90, 120,
            "abil.emb.ember_bolt", "abil.emb.frost_nova", "abil.emb.arcane_ward", "abil.emb.meteor"),

    THORNSHEPHERD(
            "Thornshepherd",
            "A summoner-support who mends allies and calls beasts to the fray.",
            "Spirit",
            3, 5, 8, 7,
            110, 95,
            "abil.shp.mend", "abil.shp.thorn_lash", "abil.shp.summon_grovling", "abil.shp.wild_blessing");

    private final String displayName;
    private final String description;
    private final String resourceName;
    private final int baseStrength;
    private final int baseAgility;
    private final int baseIntellect;
    private final int baseVitality;
    private final int baseHealth;
    private final int baseResource;
    private final List<String> abilityIds;

    CharacterClass(String displayName, String description, String resourceName,
                   int baseStrength, int baseAgility, int baseIntellect, int baseVitality,
                   int baseHealth, int baseResource, String... abilityIds) {
        this.displayName = displayName;
        this.description = description;
        this.resourceName = resourceName;
        this.baseStrength = baseStrength;
        this.baseAgility = baseAgility;
        this.baseIntellect = baseIntellect;
        this.baseVitality = baseVitality;
        this.baseHealth = baseHealth;
        this.baseResource = baseResource;
        this.abilityIds = Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(abilityIds)));
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getResourceName() {
        return resourceName;
    }

    public int getBaseStrength() {
        return baseStrength;
    }

    public int getBaseAgility() {
        return baseAgility;
    }

    public int getBaseIntellect() {
        return baseIntellect;
    }

    public int getBaseVitality() {
        return baseVitality;
    }

    public int getBaseHealth() {
        return baseHealth;
    }

    public int getBaseResource() {
        return baseResource;
    }

    public List<String> getAbilityIds() {
        return abilityIds;
    }

    /** Which attribute this class scales its offence from — a UI/engine hint. */
    public StatType getPrimaryStat() {
        switch (this) {
            case IRONCLAD_VANGUARD:
                return StatType.STRENGTH;
            case GALE_WARDEN:
                return StatType.AGILITY;
            case EMBERWEAVER:
                return StatType.INTELLECT;
            case THORNSHEPHERD:
            default:
                return StatType.INTELLECT;
        }
    }
}
