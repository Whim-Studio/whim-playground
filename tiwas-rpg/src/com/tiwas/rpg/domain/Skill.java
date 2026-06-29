package com.tiwas.rpg.domain;

import com.tiwas.rpg.json.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A named skill: a value plus the attribute-code formula it derives its cap
 * from. Pure data — contains no dice or rules resolution.
 */
public final class Skill {

    private final String name;
    private final int tier;
    private final List<String> attributeCodes;
    private int value;
    private String weaponClass; // nullable
    private boolean advanced;   // true for player-forged Advanced Skills (Epiphany)

    public Skill(String name, int tier, List<String> attributeCodes, int value) {
        this.name = name;
        this.tier = tier;
        this.attributeCodes = new ArrayList<String>();
        if (attributeCodes != null) {
            for (String c : attributeCodes) {
                this.attributeCodes.add(c == null ? null : c.toLowerCase());
            }
        }
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getTier() {
        return tier;
    }

    /** Lowercase codes, e.g. ["bpp"] or ["mpp","mss"]. */
    public List<String> getAttributeCodes() {
        return attributeCodes;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getWeaponClass() {
        return weaponClass;
    }

    public void setWeaponClass(String wc) {
        this.weaponClass = wc;
    }

    /** True if this skill was forged as an Advanced Skill via an Epiphany. */
    public boolean isAdvanced() {
        return advanced;
    }

    public void setAdvanced(boolean advanced) {
        this.advanced = advanced;
    }

    /** True if the first attribute code starts with 'm'. */
    public boolean isMind() {
        if (attributeCodes.isEmpty()) {
            return false;
        }
        String first = attributeCodes.get(0);
        return first != null && first.length() > 0 && (first.charAt(0) == 'm' || first.charAt(0) == 'M');
    }

    /** sum(attribute values in formula) / tier, rounded down. Tier 0 guards to /1. */
    public int maxCap(Character c) {
        int sum = 0;
        for (String code : attributeCodes) {
            sum += c.getAttribute(AttributeCode.fromCode(code));
        }
        int t = tier <= 0 ? 1 : tier;
        return sum / t;
    }

    // ---------------------------------------------------------------- JSON

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", name);
        m.put("tier", Integer.valueOf(tier));
        List<Object> codes = new ArrayList<Object>();
        for (String c : attributeCodes) {
            codes.add(c);
        }
        m.put("attributeCodes", codes);
        m.put("value", Integer.valueOf(value));
        m.put("weaponClass", weaponClass);
        if (advanced) {
            m.put("advanced", Boolean.TRUE);
        }
        return m;
    }

    public static Skill fromMap(Map<String, Object> m) {
        String name = Json.asString(m.get("name"));
        int tier = Json.asInt(m.get("tier"));
        List<String> codes = new ArrayList<String>();
        Object rawCodes = m.get("attributeCodes");
        if (rawCodes != null) {
            for (Object o : Json.asArray(rawCodes)) {
                codes.add(Json.asString(o));
            }
        }
        int value = Json.asInt(m.get("value"));
        Skill s = new Skill(name, tier, codes, value);
        Object wc = m.get("weaponClass");
        if (wc != null) {
            s.setWeaponClass(Json.asString(wc));
        }
        Object adv = m.get("advanced");
        if (adv != null) {
            s.setAdvanced(Json.asBoolean(adv));
        }
        return s;
    }
}
