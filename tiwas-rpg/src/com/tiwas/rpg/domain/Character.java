package com.tiwas.rpg.domain;

import com.tiwas.rpg.json.Json;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A player character: 24 attributes, named skills, live resource pools, an
 * inventory, and full JSON save/load. Data + JSON mapping only — no dice,
 * no rules resolution, no UI.
 */
public final class Character {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private String name;
    private final EnumMap<AttributeCode, Integer> attributes;
    private final Map<String, Skill> skills;
    private int currentHP;
    private int currentPE;
    private int currentMP;
    private int generalXP;
    private final List<String> inventory;

    public Character() {
        this("");
    }

    public Character(String name) {
        this.name = name;
        this.attributes = new EnumMap<AttributeCode, Integer>(AttributeCode.class);
        this.skills = new LinkedHashMap<String, Skill>();
        this.inventory = new ArrayList<String>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // ----------------------------------------------------------- attributes

    public int getAttribute(AttributeCode a) {
        Integer v = attributes.get(a);
        return v == null ? 0 : v.intValue();
    }

    public void setAttribute(AttributeCode a, int value) {
        attributes.put(a, Integer.valueOf(value));
    }

    /** Ordered body-then-mind; missing attributes reported as 0. */
    public Map<AttributeCode, Integer> getAttributes() {
        EnumMap<AttributeCode, Integer> out = new EnumMap<AttributeCode, Integer>(AttributeCode.class);
        for (AttributeCode a : AttributeCode.values()) {
            out.put(a, Integer.valueOf(getAttribute(a)));
        }
        return out;
    }

    // --------------------------------------------------------------- skills

    public Map<String, Skill> getSkills() {
        return skills;
    }

    public void putSkill(Skill s) {
        skills.put(s.getName(), s);
    }

    public Skill getSkill(String name) {
        return skills.get(name);
    }

    // ---------------------------------------------------------------- pools

    public int getCurrentHP() {
        return currentHP;
    }

    public void setCurrentHP(int v) {
        this.currentHP = v;
    }

    public int getCurrentPE() {
        return currentPE;
    }

    public void setCurrentPE(int v) {
        this.currentPE = v;
    }

    public int getCurrentMP() {
        return currentMP;
    }

    public void setCurrentMP(int v) {
        this.currentMP = v;
    }

    public int getGeneralXP() {
        return generalXP;
    }

    public void setGeneralXP(int v) {
        this.generalXP = v;
    }

    public List<String> getInventory() {
        return inventory;
    }

    // -------------------------------------------------------- derived stats

    /** Sum of all 12 body attributes. */
    public int getMaxHP() {
        int sum = 0;
        for (AttributeCode a : AttributeCode.bodyAttributes()) {
            sum += getAttribute(a);
        }
        return sum;
    }

    /** Sum of all 12 mind attributes. */
    public int getMaxMP() {
        int sum = 0;
        for (AttributeCode a : AttributeCode.mindAttributes()) {
            sum += getAttribute(a);
        }
        return sum;
    }

    /** bep + bes + bee */
    public int getMaxPhysicalEnergy() {
        return getAttribute(AttributeCode.BEP) + getAttribute(AttributeCode.BES) + getAttribute(AttributeCode.BEE);
    }

    /** bsp + bss + bse */
    public int getSpeed() {
        return getAttribute(AttributeCode.BSP) + getAttribute(AttributeCode.BSS) + getAttribute(AttributeCode.BSE);
    }

    /** bep + bes */
    public int getEnergyRegen() {
        return getAttribute(AttributeCode.BEP) + getAttribute(AttributeCode.BES);
    }

    /** mep + mes */
    public int getMpRegen() {
        return getAttribute(AttributeCode.MEP) + getAttribute(AttributeCode.MES);
    }

    /** (bsp + bss) / 15, round down. */
    public int getMovementSpeed() {
        return (getAttribute(AttributeCode.BSP) + getAttribute(AttributeCode.BSS)) / 15;
    }

    /** Sets current pools to their maxima. */
    public void restoreToFull() {
        this.currentPE = getMaxPhysicalEnergy();
        this.currentMP = getMaxMP();
        this.currentHP = getMaxHP();
    }

    // ----------------------------------------------------------------- JSON

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", name);

        Map<String, Object> attrs = new LinkedHashMap<String, Object>();
        for (AttributeCode a : AttributeCode.values()) {
            attrs.put(a.code(), Integer.valueOf(getAttribute(a)));
        }
        m.put("attributes", attrs);

        List<Object> skillList = new ArrayList<Object>();
        for (Skill s : skills.values()) {
            skillList.add(s.toMap());
        }
        m.put("skills", skillList);

        m.put("currentHP", Integer.valueOf(currentHP));
        m.put("currentPE", Integer.valueOf(currentPE));
        m.put("currentMP", Integer.valueOf(currentMP));
        m.put("generalXP", Integer.valueOf(generalXP));

        List<Object> inv = new ArrayList<Object>();
        for (String item : inventory) {
            inv.add(item);
        }
        m.put("inventory", inv);

        return m;
    }

    public static Character fromMap(Map<String, Object> m) {
        Character c = new Character();
        Object nameObj = m.get("name");
        if (nameObj != null) {
            c.setName(Json.asString(nameObj));
        }

        Object attrsObj = m.get("attributes");
        if (attrsObj != null) {
            Map<String, Object> attrs = Json.asObject(attrsObj);
            for (Map.Entry<String, Object> e : attrs.entrySet()) {
                c.setAttribute(AttributeCode.fromCode(e.getKey()), Json.asInt(e.getValue()));
            }
        }

        Object skillsObj = m.get("skills");
        if (skillsObj != null) {
            for (Object o : Json.asArray(skillsObj)) {
                c.putSkill(Skill.fromMap(Json.asObject(o)));
            }
        }

        if (m.get("currentHP") != null) {
            c.setCurrentHP(Json.asInt(m.get("currentHP")));
        }
        if (m.get("currentPE") != null) {
            c.setCurrentPE(Json.asInt(m.get("currentPE")));
        }
        if (m.get("currentMP") != null) {
            c.setCurrentMP(Json.asInt(m.get("currentMP")));
        }
        if (m.get("generalXP") != null) {
            c.setGeneralXP(Json.asInt(m.get("generalXP")));
        }

        Object invObj = m.get("inventory");
        if (invObj != null) {
            for (Object o : Json.asArray(invObj)) {
                c.getInventory().add(Json.asString(o));
            }
        }

        return c;
    }

    public String toJson() {
        return Json.writePretty(toMap());
    }

    public static Character fromJson(String json) {
        return fromMap(Json.asObject(Json.parse(json)));
    }

    public void save(File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        try {
            Writer w = new OutputStreamWriter(fos, UTF8);
            w.write(toJson());
            w.flush();
        } finally {
            fos.close();
        }
    }

    public static Character load(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        try {
            Reader r = new InputStreamReader(fis, UTF8);
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            return fromJson(sb.toString());
        } finally {
            fis.close();
        }
    }
}
