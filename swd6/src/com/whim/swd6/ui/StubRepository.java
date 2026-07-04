package com.whim.swd6.ui;

import com.whim.swd6.api.Attribute;
import com.whim.swd6.api.CharacterRepository;
import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.ForceSkill;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.Skill;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DEV STUB, replaced by Main (JsonCharacterRepository) at runtime.
 *
 * A minimal file-backed {@link CharacterRepository} using a trivial line-oriented
 * "key=value" format (not the real JSON format Task 1 ships) — just enough for the
 * UI's Save/Load buttons to round-trip a character while running standalone.
 *
 * Owned by Task 3 (ui). Not shipped in the wired app.
 */
public final class StubRepository implements CharacterRepository {

    private final File dir;

    public StubRepository() {
        File base = new File(System.getProperty("java.io.tmpdir"), "swd6-stub-saves");
        base.mkdirs();
        this.dir = base;
    }

    @Override
    public File defaultDirectory() {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    @Override
    public void save(PlayerCharacter pc, File file) throws IOException {
        PrintWriter w = null;
        try {
            w = new PrintWriter(new FileWriter(file));
            w.println("name=" + esc(pc.getName()));
            w.println("template=" + esc(pc.getTemplateName()));
            w.println("species=" + esc(pc.getSpecies()));
            w.println("background=" + esc(pc.getBackground()));
            w.println("motivation=" + esc(pc.getMotivation()));
            w.println("destiny=" + esc(pc.getDestiny()));
            w.println("force=" + pc.isForceSensitive());
            w.println("fp=" + pc.getForcePoints());
            w.println("cp=" + pc.getCharacterPoints());
            w.println("dsp=" + pc.getDarkSidePoints());
            w.println("move=" + pc.getMove());
            w.println("credits=" + pc.getCredits());
            w.println("wound=" + pc.getWoundLevel().name());
            for (Attribute a : Attribute.values()) {
                w.println("attr." + a.name() + "=" + pc.getAttribute(a).toString());
            }
            for (Skill s : pc.getSkills()) {
                w.println("skill=" + esc(s.getName()) + "|" + s.getAttribute().name()
                        + "|" + s.getAdded().toString());
            }
            if (pc.isForceSensitive()) {
                for (ForceSkill fs : ForceSkill.values()) {
                    w.println("force." + fs.name() + "=" + pc.getForceSkill(fs).toString());
                }
            }
        } finally {
            if (w != null) {
                w.close();
            }
        }
    }

    @Override
    public PlayerCharacter load(File file) throws IOException {
        PlayerCharacter pc = new PlayerCharacter();
        pc.getSkills().clear();
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(file));
            String line;
            while ((line = r.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                String key = line.substring(0, eq);
                String val = line.substring(eq + 1);
                apply(pc, key, val);
            }
        } finally {
            if (r != null) {
                r.close();
            }
        }
        return pc;
    }

    private void apply(PlayerCharacter pc, String key, String val) {
        if (key.equals("name")) pc.setName(unesc(val));
        else if (key.equals("template")) pc.setTemplateName(unesc(val));
        else if (key.equals("species")) pc.setSpecies(unesc(val));
        else if (key.equals("background")) pc.setBackground(unesc(val));
        else if (key.equals("motivation")) pc.setMotivation(unesc(val));
        else if (key.equals("destiny")) pc.setDestiny(unesc(val));
        else if (key.equals("force")) pc.setForceSensitive(Boolean.parseBoolean(val));
        else if (key.equals("fp")) pc.setForcePoints(parseInt(val, 1));
        else if (key.equals("cp")) pc.setCharacterPoints(parseInt(val, 5));
        else if (key.equals("dsp")) pc.setDarkSidePoints(parseInt(val, 0));
        else if (key.equals("move")) pc.setMove(parseInt(val, 10));
        else if (key.equals("credits")) pc.setCredits(parseInt(val, 0));
        else if (key.equals("wound")) pc.setWoundLevel(parseWound(val));
        else if (key.startsWith("attr.")) {
            Attribute a = attrOf(key.substring("attr.".length()));
            if (a != null) pc.setAttribute(a, safeCode(val));
        } else if (key.startsWith("force.")) {
            ForceSkill fs = forceOf(key.substring("force.".length()));
            if (fs != null) pc.setForceSkill(fs, safeCode(val));
        } else if (key.equals("skill")) {
            String[] parts = val.split("\\|", -1);
            if (parts.length >= 3) {
                Attribute a = attrOf(parts[1]);
                pc.getSkills().add(new Skill(unesc(parts[0]),
                        a == null ? Attribute.DEXTERITY : a, safeCode(parts[2])));
            }
        }
    }

    @Override
    public List<File> listSaved() {
        List<File> out = new ArrayList<File>();
        File[] files = defaultDirectory().listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".chr")) {
                    out.add(f);
                }
            }
        }
        return out;
    }

    // ----- helpers -----
    private static final Map<String, com.whim.swd6.api.WoundLevel> WOUNDS = wounds();

    private static Map<String, com.whim.swd6.api.WoundLevel> wounds() {
        Map<String, com.whim.swd6.api.WoundLevel> m = new LinkedHashMap<String, com.whim.swd6.api.WoundLevel>();
        for (com.whim.swd6.api.WoundLevel w : com.whim.swd6.api.WoundLevel.values()) {
            m.put(w.name(), w);
        }
        return m;
    }

    private com.whim.swd6.api.WoundLevel parseWound(String v) {
        com.whim.swd6.api.WoundLevel w = WOUNDS.get(v);
        return w == null ? com.whim.swd6.api.WoundLevel.HEALTHY : w;
    }

    private DiceCode safeCode(String v) {
        try {
            return DiceCode.parse(v);
        } catch (RuntimeException ex) {
            return DiceCode.ZERO;
        }
    }

    private int parseInt(String v, int def) {
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private Attribute attrOf(String n) {
        for (Attribute a : Attribute.values()) {
            if (a.name().equals(n)) return a;
        }
        return null;
    }

    private ForceSkill forceOf(String n) {
        for (ForceSkill f : ForceSkill.values()) {
            if (f.name().equals(n)) return f;
        }
        return null;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\n", "\\n");
    }

    private String unesc(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n").replace("\\\\", "\\");
    }
}
