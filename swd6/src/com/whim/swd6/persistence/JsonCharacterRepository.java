package com.whim.swd6.persistence;

import com.whim.swd6.api.Armor;
import com.whim.swd6.api.Attribute;
import com.whim.swd6.api.CharacterRepository;
import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.DifficultyTier;
import com.whim.swd6.api.Equipment;
import com.whim.swd6.api.ForceSkill;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.Skill;
import com.whim.swd6.api.Specialization;
import com.whim.swd6.api.Weapon;
import com.whim.swd6.api.WoundLevel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Saves and loads {@link PlayerCharacter}s as hand-rolled JSON (JDK only). Dice
 * codes are serialized as their canonical "3D+2" strings via {@link DiceCode};
 * enums are stored by name. The default directory is {@code ~/.swd6/characters}.
 */
public final class JsonCharacterRepository implements CharacterRepository {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    // ------------------------------------------------------------------
    // Save
    // ------------------------------------------------------------------

    @Override
    public void save(PlayerCharacter pc, File file) throws IOException {
        if (pc == null) {
            throw new IOException("Cannot save a null character");
        }
        if (file == null) {
            throw new IOException("Cannot save to a null file");
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent);
        }
        String json = Json.write(toMap(pc));
        FileOutputStream fos = null;
        Writer w = null;
        try {
            fos = new FileOutputStream(file);
            w = new OutputStreamWriter(fos, UTF8);
            w.write(json);
            w.flush();
        } finally {
            if (w != null) {
                w.close();
            } else if (fos != null) {
                fos.close();
            }
        }
    }

    private Map<String, Object> toMap(PlayerCharacter pc) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("schema", "swd6-character-1");

        // Identity
        m.put("name", pc.getName());
        m.put("templateName", pc.getTemplateName());
        m.put("species", pc.getSpecies());
        m.put("background", pc.getBackground());
        m.put("motivation", pc.getMotivation());
        m.put("destiny", pc.getDestiny());

        // Attributes -> { "DEXTERITY": "3D+1", ... }
        Map<String, Object> attrs = new LinkedHashMap<String, Object>();
        for (Attribute a : Attribute.values()) {
            attrs.put(a.name(), pc.getAttribute(a).toString());
        }
        m.put("attributes", attrs);

        // Skills
        List<Object> skills = new ArrayList<Object>();
        for (Skill s : pc.getSkills()) {
            skills.add(skillToMap(s));
        }
        m.put("skills", skills);

        // Force
        m.put("forceSensitive", Boolean.valueOf(pc.isForceSensitive()));
        Map<String, Object> force = new LinkedHashMap<String, Object>();
        for (ForceSkill fs : ForceSkill.values()) {
            DiceCode code = pc.getForceSkills().get(fs);
            if (code != null) {
                force.put(fs.name(), code.toString());
            }
        }
        m.put("forceSkills", force);

        // Point economies
        m.put("forcePoints", Integer.valueOf(pc.getForcePoints()));
        m.put("characterPoints", Integer.valueOf(pc.getCharacterPoints()));
        m.put("darkSidePoints", Integer.valueOf(pc.getDarkSidePoints()));

        // Physical
        m.put("move", Integer.valueOf(pc.getMove()));
        m.put("credits", Integer.valueOf(pc.getCredits()));
        m.put("woundLevel", pc.getWoundLevel().name());

        // Inventory
        List<Object> weapons = new ArrayList<Object>();
        for (Weapon w : pc.getWeapons()) {
            weapons.add(weaponToMap(w));
        }
        m.put("weapons", weapons);

        List<Object> armor = new ArrayList<Object>();
        for (Armor a : pc.getArmor()) {
            armor.add(armorToMap(a));
        }
        m.put("armor", armor);

        List<Object> gear = new ArrayList<Object>();
        for (Equipment e : pc.getGear()) {
            gear.add(equipmentToMap(e));
        }
        m.put("gear", gear);

        return m;
    }

    private Map<String, Object> skillToMap(Skill s) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", s.getName());
        m.put("attribute", s.getAttribute().name());
        m.put("added", s.getAdded().toString());
        List<Object> specs = new ArrayList<Object>();
        for (Specialization sp : s.getSpecializations()) {
            Map<String, Object> sm = new LinkedHashMap<String, Object>();
            sm.put("name", sp.getName());
            sm.put("bonus", sp.getBonus().toString());
            specs.add(sm);
        }
        m.put("specializations", specs);
        return m;
    }

    private Map<String, Object> weaponToMap(Weapon w) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", w.getName());
        m.put("skill", w.getSkill());
        m.put("damage", w.getDamage().toString());
        m.put("melee", Boolean.valueOf(w.isMelee()));
        m.put("shortRange", Integer.valueOf(w.getShortRange()));
        m.put("mediumRange", Integer.valueOf(w.getMediumRange()));
        m.put("longRange", Integer.valueOf(w.getLongRange()));
        m.put("shortDifficulty", w.getShortDifficulty().name());
        m.put("mediumDifficulty", w.getMediumDifficulty().name());
        m.put("longDifficulty", w.getLongDifficulty().name());
        m.put("cost", Integer.valueOf(w.getCost()));
        m.put("notes", w.getNotes());
        return m;
    }

    private Map<String, Object> armorToMap(Armor a) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", a.getName());
        m.put("physicalBonus", a.getPhysicalBonus().toString());
        m.put("energyBonus", a.getEnergyBonus().toString());
        m.put("dexPenalty", a.getDexPenalty().toString());
        m.put("cost", Integer.valueOf(a.getCost()));
        m.put("notes", a.getNotes());
        return m;
    }

    private Map<String, Object> equipmentToMap(Equipment e) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", e.getName());
        m.put("quantity", Integer.valueOf(e.getQuantity()));
        m.put("cost", Integer.valueOf(e.getCost()));
        m.put("notes", e.getNotes());
        return m;
    }

    // ------------------------------------------------------------------
    // Load
    // ------------------------------------------------------------------

    @Override
    public PlayerCharacter load(File file) throws IOException {
        if (file == null) {
            throw new IOException("Cannot load from a null file");
        }
        if (!file.exists()) {
            throw new IOException("Character file does not exist: " + file);
        }
        String text = readAll(file);
        Object root;
        try {
            root = Json.parse(text);
        } catch (RuntimeException ex) {
            throw new IOException("Malformed character JSON in " + file + ": " + ex.getMessage(), ex);
        }
        if (!(root instanceof Map)) {
            throw new IOException("Expected a JSON object at the root of " + file);
        }
        try {
            return fromMap(asMap(root));
        } catch (RuntimeException ex) {
            throw new IOException("Could not read character from " + file + ": " + ex.getMessage(), ex);
        }
    }

    private PlayerCharacter fromMap(Map<String, Object> m) {
        PlayerCharacter pc = new PlayerCharacter();

        pc.setName(str(m, "name", ""));
        pc.setTemplateName(str(m, "templateName", ""));
        pc.setSpecies(str(m, "species", "Human"));
        pc.setBackground(str(m, "background", ""));
        pc.setMotivation(str(m, "motivation", ""));
        pc.setDestiny(str(m, "destiny", ""));

        Object attrsObj = m.get("attributes");
        if (attrsObj instanceof Map) {
            Map<String, Object> attrs = asMap(attrsObj);
            for (Attribute a : Attribute.values()) {
                Object code = attrs.get(a.name());
                if (code != null) {
                    pc.setAttribute(a, DiceCode.parse(String.valueOf(code)));
                }
            }
        }

        Object skillsObj = m.get("skills");
        if (skillsObj instanceof List) {
            for (Object o : asList(skillsObj)) {
                Map<String, Object> sm = asMap(o);
                Skill s = new Skill(str(sm, "name", ""),
                        parseAttribute(str(sm, "attribute", "DEXTERITY")),
                        DiceCode.parse(str(sm, "added", "0D")));
                Object specsObj = sm.get("specializations");
                if (specsObj instanceof List) {
                    for (Object so : asList(specsObj)) {
                        Map<String, Object> spm = asMap(so);
                        s.getSpecializations().add(new Specialization(
                                str(spm, "name", ""),
                                DiceCode.parse(str(spm, "bonus", "0D"))));
                    }
                }
                pc.getSkills().add(s);
            }
        }

        pc.setForceSensitive(bool(m, "forceSensitive", false));
        Object forceObj = m.get("forceSkills");
        if (forceObj instanceof Map) {
            Map<String, Object> force = asMap(forceObj);
            for (ForceSkill fs : ForceSkill.values()) {
                Object code = force.get(fs.name());
                if (code != null) {
                    pc.setForceSkill(fs, DiceCode.parse(String.valueOf(code)));
                }
            }
        }

        pc.setForcePoints(intVal(m, "forcePoints", 1));
        pc.setCharacterPoints(intVal(m, "characterPoints", 5));
        pc.setDarkSidePoints(intVal(m, "darkSidePoints", 0));

        pc.setMove(intVal(m, "move", 10));
        pc.setCredits(intVal(m, "credits", 0));
        pc.setWoundLevel(parseWound(str(m, "woundLevel", "HEALTHY")));

        Object weaponsObj = m.get("weapons");
        if (weaponsObj instanceof List) {
            for (Object o : asList(weaponsObj)) {
                pc.getWeapons().add(weaponFromMap(asMap(o)));
            }
        }

        Object armorObj = m.get("armor");
        if (armorObj instanceof List) {
            for (Object o : asList(armorObj)) {
                pc.getArmor().add(armorFromMap(asMap(o)));
            }
        }

        Object gearObj = m.get("gear");
        if (gearObj instanceof List) {
            for (Object o : asList(gearObj)) {
                Map<String, Object> em = asMap(o);
                pc.getGear().add(new Equipment(
                        str(em, "name", ""),
                        intVal(em, "quantity", 1),
                        intVal(em, "cost", 0),
                        str(em, "notes", "")));
            }
        }

        return pc;
    }

    private Weapon weaponFromMap(Map<String, Object> m) {
        Weapon w = new Weapon();
        w.setName(str(m, "name", ""));
        w.setSkill(str(m, "skill", ""));
        w.setDamage(DiceCode.parse(str(m, "damage", "0D")));
        w.setMelee(bool(m, "melee", false));
        w.setShortRange(intVal(m, "shortRange", 0));
        w.setMediumRange(intVal(m, "mediumRange", 0));
        w.setLongRange(intVal(m, "longRange", 0));
        w.setShortDifficulty(parseTier(str(m, "shortDifficulty", "VERY_EASY")));
        w.setMediumDifficulty(parseTier(str(m, "mediumDifficulty", "EASY")));
        w.setLongDifficulty(parseTier(str(m, "longDifficulty", "MODERATE")));
        w.setCost(intVal(m, "cost", 0));
        w.setNotes(str(m, "notes", ""));
        return w;
    }

    private Armor armorFromMap(Map<String, Object> m) {
        Armor a = new Armor();
        a.setName(str(m, "name", ""));
        a.setPhysicalBonus(DiceCode.parse(str(m, "physicalBonus", "0D")));
        a.setEnergyBonus(DiceCode.parse(str(m, "energyBonus", "0D")));
        a.setDexPenalty(DiceCode.parse(str(m, "dexPenalty", "0D")));
        a.setCost(intVal(m, "cost", 0));
        a.setNotes(str(m, "notes", ""));
        return a;
    }

    // ------------------------------------------------------------------
    // Directory / listing
    // ------------------------------------------------------------------

    @Override
    public File defaultDirectory() {
        String home = System.getProperty("user.home", ".");
        File dir = new File(new File(home, ".swd6"), "characters");
        if (!dir.exists()) {
            // Best-effort creation; if it fails the caller's save/list will surface it.
            dir.mkdirs();
        }
        return dir;
    }

    @Override
    public List<File> listSaved() {
        File dir = defaultDirectory();
        File[] files = dir.listFiles(new java.io.FilenameFilter() {
            @Override
            public boolean accept(File d, String name) {
                return name.toLowerCase().endsWith(".json");
            }
        });
        List<File> out = new ArrayList<File>();
        if (files != null) {
            out.addAll(Arrays.asList(files));
            out.sort(new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String readAll(File file) throws IOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = in.read(chunk)) >= 0) {
                buf.write(chunk, 0, n);
            }
            return new String(buf.toByteArray(), UTF8);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("Expected a JSON object but found "
                    + (o == null ? "null" : o.getClass().getSimpleName()));
        }
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        if (!(o instanceof List)) {
            throw new IllegalArgumentException("Expected a JSON array but found "
                    + (o == null ? "null" : o.getClass().getSimpleName()));
        }
        return (List<Object>) o;
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v == null ? def : String.valueOf(v);
    }

    private static boolean bool(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        if (v != null) {
            return Boolean.parseBoolean(String.valueOf(v));
        }
        return def;
    }

    private static int intVal(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v != null) {
            try {
                return Integer.parseInt(String.valueOf(v).trim());
            } catch (NumberFormatException ex) {
                return def;
            }
        }
        return def;
    }

    private static Attribute parseAttribute(String name) {
        try {
            return Attribute.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return Attribute.DEXTERITY;
        }
    }

    private static WoundLevel parseWound(String name) {
        try {
            return WoundLevel.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return WoundLevel.HEALTHY;
        }
    }

    private static DifficultyTier parseTier(String name) {
        try {
            return DifficultyTier.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return DifficultyTier.MODERATE;
        }
    }
}
