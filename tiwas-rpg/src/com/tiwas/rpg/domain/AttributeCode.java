package com.tiwas.rpg.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The 24 Tiwas RPG attributes. Body group (12) is declared first, then the
 * Mind group (12), each in the exact order fixed by the interface contract.
 *
 * Each constant carries its 3-letter lowercase code, an animal, a full name,
 * its Tier-1 skill name, and whether it belongs to the Body group.
 */
public enum AttributeCode {
    // --- Body (12) ---
    BPP("bpp", "Bear", "Body Power Power", "Might", true),
    BPS("bps", "Tiger", "Body Power Speed", "Impact", true),
    BPE("bpe", "Elephant", "Body Power Endurance", "Brawn", true),
    BPX("bpx", "Dragon", "Body Power Social", "Presence", true),
    BSP("bsp", "Hawk", "Body Speed Power", "Agility", true),
    BSS("bss", "Rat", "Body Speed Speed", "Reflexes", true),
    BSE("bse", "Horse", "Body Speed Endurance", "Quickness", true),
    BSX("bsx", "Monkey", "Body Speed Social", "Grace", true),
    BEP("bep", "Badger", "Body Endurance Power", "Toughness", true),
    BES("bes", "Wolf", "Body Endurance Speed", "Stamina", true),
    BEE("bee", "Ox", "Body Endurance Endurance", "Vitality", true),
    BEX("bex", "Dog", "Body Endurance Social", "Poise", true),

    // --- Mind (12) ---
    MPP("mpp", "Stag", "Mind Power Power", "Cunning", false),
    MPS("mps", "Snake", "Mind Power Speed", "Wits", false),
    MPE("mpe", "Rooster", "Mind Power Endurance", "Willpower", false),
    MPX("mpx", "Fox", "Mind Power Social", "Glamour", false),
    MSP("msp", "Bat", "Mind Speed Power", "Acuity", false),
    MSS("mss", "Cat", "Mind Speed Speed", "Perception", false),
    MSE("mse", "Rabbit", "Mind Speed Endurance", "Alacrity", false),
    MSX("msx", "Otter", "Mind Speed Social", "Charm", false),
    MEP("mep", "Crane", "Mind Endurance Power", "Focus", false),
    MES("mes", "Goat", "Mind Endurance Speed", "Discipline", false),
    MEE("mee", "Owl", "Mind Endurance Endurance", "Resolve", false),
    MEX("mex", "Pig", "Mind Endurance Social", "Composure", false);

    private final String code;
    private final String animal;
    private final String fullName;
    private final String tier1Skill;
    private final boolean body;

    AttributeCode(String code, String animal, String fullName, String tier1Skill, boolean body) {
        this.code = code;
        this.animal = animal;
        this.fullName = fullName;
        this.tier1Skill = tier1Skill;
        this.body = body;
    }

    public String code() {
        return code;
    }

    public String animal() {
        return animal;
    }

    public String fullName() {
        return fullName;
    }

    public String tier1Skill() {
        return tier1Skill;
    }

    public boolean isBody() {
        return body;
    }

    /** Case-insensitive lookup by 3-letter code. Throws if unknown. */
    public static AttributeCode fromCode(String code) {
        if (code != null) {
            String norm = code.trim().toLowerCase();
            for (AttributeCode a : values()) {
                if (a.code.equals(norm)) {
                    return a;
                }
            }
        }
        throw new IllegalArgumentException("Unknown attribute code: " + code);
    }

    /** The 12 body attributes, in declaration order. */
    public static List<AttributeCode> bodyAttributes() {
        List<AttributeCode> out = new ArrayList<AttributeCode>();
        for (AttributeCode a : values()) {
            if (a.body) {
                out.add(a);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** The 12 mind attributes, in declaration order. */
    public static List<AttributeCode> mindAttributes() {
        List<AttributeCode> out = new ArrayList<AttributeCode>();
        for (AttributeCode a : values()) {
            if (!a.body) {
                out.add(a);
            }
        }
        return Collections.unmodifiableList(out);
    }
}
