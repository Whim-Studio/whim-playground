package com.tiwas.rpg.domain;

import com.tiwas.rpg.json.Json;

import java.util.LinkedHashMap;
import java.util.Map;

/** Streamlined NPC stat block (per contract §9). Data + JSON mapping only. */
public final class Npc {

    private String name;
    private String tier; // Minion/Standard/Elite/Boss
    private int hp;
    private int mp;
    private int pe;
    private int speed;
    private int movement;
    private int armorRating;
    private int damageModifier;
    private int defaultSkill;
    private String special;
    private final Map<String, Integer> skills;

    public Npc() {
        this.skills = new LinkedHashMap<String, Integer>();
    }

    public String getName() {
        return name;
    }

    public void setName(String s) {
        this.name = s;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String s) {
        this.tier = s;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int v) {
        this.hp = v;
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int v) {
        this.mp = v;
    }

    public int getPe() {
        return pe;
    }

    public void setPe(int v) {
        this.pe = v;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int v) {
        this.speed = v;
    }

    public int getMovement() {
        return movement;
    }

    public void setMovement(int v) {
        this.movement = v;
    }

    public int getArmorRating() {
        return armorRating;
    }

    public void setArmorRating(int v) {
        this.armorRating = v;
    }

    public int getDamageModifier() {
        return damageModifier;
    }

    public void setDamageModifier(int v) {
        this.damageModifier = v;
    }

    public int getDefaultSkill() {
        return defaultSkill;
    }

    public void setDefaultSkill(int v) {
        this.defaultSkill = v;
    }

    public String getSpecial() {
        return special;
    }

    public void setSpecial(String s) {
        this.special = s;
    }

    public Map<String, Integer> getSkills() {
        return skills;
    }

    // ----------------------------------------------------------------- JSON

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", name);
        m.put("tier", tier);
        m.put("hp", Integer.valueOf(hp));
        m.put("mp", Integer.valueOf(mp));
        m.put("pe", Integer.valueOf(pe));
        m.put("speed", Integer.valueOf(speed));
        m.put("movement", Integer.valueOf(movement));
        m.put("armorRating", Integer.valueOf(armorRating));
        m.put("damageModifier", Integer.valueOf(damageModifier));
        m.put("defaultSkill", Integer.valueOf(defaultSkill));
        m.put("special", special);
        Map<String, Object> sk = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Integer> e : skills.entrySet()) {
            sk.put(e.getKey(), e.getValue());
        }
        m.put("skills", sk);
        return m;
    }

    public static Npc fromMap(Map<String, Object> m) {
        Npc n = new Npc();
        if (m.get("name") != null) {
            n.setName(Json.asString(m.get("name")));
        }
        if (m.get("tier") != null) {
            n.setTier(Json.asString(m.get("tier")));
        }
        if (m.get("hp") != null) {
            n.setHp(Json.asInt(m.get("hp")));
        }
        if (m.get("mp") != null) {
            n.setMp(Json.asInt(m.get("mp")));
        }
        if (m.get("pe") != null) {
            n.setPe(Json.asInt(m.get("pe")));
        }
        if (m.get("speed") != null) {
            n.setSpeed(Json.asInt(m.get("speed")));
        }
        if (m.get("movement") != null) {
            n.setMovement(Json.asInt(m.get("movement")));
        }
        if (m.get("armorRating") != null) {
            n.setArmorRating(Json.asInt(m.get("armorRating")));
        }
        if (m.get("damageModifier") != null) {
            n.setDamageModifier(Json.asInt(m.get("damageModifier")));
        }
        if (m.get("defaultSkill") != null) {
            n.setDefaultSkill(Json.asInt(m.get("defaultSkill")));
        }
        if (m.get("special") != null) {
            n.setSpecial(Json.asString(m.get("special")));
        }
        Object skObj = m.get("skills");
        if (skObj != null) {
            for (Map.Entry<String, Object> e : Json.asObject(skObj).entrySet()) {
                n.getSkills().put(e.getKey(), Integer.valueOf(Json.asInt(e.getValue())));
            }
        }
        return n;
    }
}
