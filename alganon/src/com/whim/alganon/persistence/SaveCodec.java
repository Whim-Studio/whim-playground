package com.whim.alganon.persistence;

import com.whim.alganon.api.Enums.EquipSlot;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.persistence.SaveGame.ListingSave;
import com.whim.alganon.persistence.SaveGame.ObjSave;
import com.whim.alganon.persistence.SaveGame.QuestSave;
import com.whim.alganon.persistence.SaveGame.WarObjSave;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Zero-dependency, human-readable codec for {@link SaveGame}. Format is a flat list of
 * {@code key=value} lines with dotted prefixes for maps and indexed collections, so a
 * save file is trivially diffable and hand-editable. Pipe-delimited compound fields
 * escape {@code | \\ newline}.
 */
public final class SaveCodec {
    private SaveCodec() {}

    public static String encode(SaveGame s) {
        StringBuilder b = new StringBuilder();
        b.append("alganon-save v").append(SaveGame.VERSION).append('\n');
        kv(b, "seed", s.seed);
        kv(b, "race", s.raceId);
        kv(b, "family", s.familyId);
        kv(b, "class", s.classId);
        kv(b, "name", s.name);
        kv(b, "level", s.level);
        kv(b, "xp", s.xp);
        kv(b, "hp", s.hp);
        kv(b, "maxHp", s.maxHp);
        kv(b, "resource", s.resource);
        kv(b, "maxResource", s.maxResource);
        kv(b, "stance", s.stance);
        kv(b, "school", s.school);
        kv(b, "gold", s.gold);
        kv(b, "study", s.studyAssignment == null ? "" : s.studyAssignment);
        kv(b, "bankedStudy", Double.toString(s.bankedStudy));
        kv(b, "posX", s.posX);
        kv(b, "posY", s.posY);
        kv(b, "zone", s.zoneId);
        kv(b, "lastSave", s.lastSaveEpochMillis);
        kv(b, "asharrWar", s.asharrWarScore);
        kv(b, "kujixWar", s.kujixWarScore);

        for (Map.Entry<StatType, Integer> e : s.stats.entrySet()) kv(b, "stat." + e.getKey(), e.getValue());
        for (Map.Entry<SkillType, Integer> e : s.skills.entrySet()) kv(b, "skill." + e.getKey(), e.getValue());
        for (Map.Entry<String, Integer> e : s.inventory.entrySet()) kv(b, "inv." + e.getKey(), e.getValue());
        for (Map.Entry<EquipSlot, String> e : s.equipped.entrySet()) kv(b, "equip." + e.getKey(), e.getValue());

        for (int i = 0; i < s.quests.size(); i++) {
            QuestSave q = s.quests.get(i);
            String p = "quest." + i + ".";
            kv(b, p + "id", q.id);
            kv(b, p + "name", q.name);
            kv(b, p + "desc", q.description);
            kv(b, p + "giver", q.giverNpcId);
            kv(b, p + "turnin", q.turnInNpcId);
            kv(b, p + "levelReq", q.levelReq);
            kv(b, p + "xpReward", q.xpReward);
            kv(b, p + "goldReward", q.goldReward);
            kv(b, p + "procedural", q.procedural);
            kv(b, p + "status", q.status);
            kv(b, p + "rewards", join(q.rewardItemIds));
            for (int j = 0; j < q.objectives.size(); j++) {
                ObjSave o = q.objectives.get(j);
                kv(b, p + "obj." + j, pipe(o.type, o.targetId, String.valueOf(o.count),
                        String.valueOf(o.progress), o.text));
            }
        }
        for (int i = 0; i < s.warObjectives.size(); i++) {
            WarObjSave w = s.warObjectives.get(i);
            kv(b, "war." + i, pipe(w.name, w.control, Double.toString(w.influence), Double.toString(w.nextTick)));
        }
        for (int i = 0; i < s.listings.size(); i++) {
            ListingSave l = s.listings.get(i);
            kv(b, "listing." + i, pipe(l.listingId, l.itemId, String.valueOf(l.quantity), String.valueOf(l.price)));
        }
        return b.toString();
    }

    public static SaveGame decode(String text) {
        SaveGame s = new SaveGame();
        String[] lines = text.split("\n", -1);
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("alganon-save")) continue;
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq);
            String val = line.substring(eq + 1);
            apply(s, key, val);
        }
        return s;
    }

    private static void apply(SaveGame s, String key, String val) {
        if (key.startsWith("stat.")) {
            s.stats.put(StatType.valueOf(key.substring(5)), parseInt(val)); return;
        }
        if (key.startsWith("skill.")) {
            s.skills.put(SkillType.valueOf(key.substring(6)), parseInt(val)); return;
        }
        if (key.startsWith("inv.")) {
            s.inventory.put(key.substring(4), parseInt(val)); return;
        }
        if (key.startsWith("equip.")) {
            s.equipped.put(EquipSlot.valueOf(key.substring(6)), val); return;
        }
        if (key.startsWith("quest.")) { applyQuest(s, key.substring(6), val); return; }
        if (key.startsWith("war.")) {
            String[] p = unpipe(val);
            WarObjSave w = new WarObjSave();
            w.name = p.length > 0 ? p[0] : "";
            w.control = p.length > 1 ? p[1] : "NEUTRAL";
            w.influence = p.length > 2 ? parseDouble(p[2]) : 0;
            w.nextTick = p.length > 3 ? parseDouble(p[3]) : 0;
            s.warObjectives.add(w);
            return;
        }
        if (key.startsWith("listing.")) {
            String[] p = unpipe(val);
            ListingSave l = new ListingSave();
            l.listingId = p.length > 0 ? p[0] : "";
            l.itemId = p.length > 1 ? p[1] : "";
            l.quantity = p.length > 2 ? parseInt(p[2]) : 1;
            l.price = p.length > 3 ? parseLong(p[3]) : 0;
            s.listings.add(l);
            return;
        }
        applyScalar(s, key, val);
    }

    private static void applyScalar(SaveGame s, String key, String val) {
        if (key.equals("seed")) s.seed = parseLong(val);
        else if (key.equals("race")) s.raceId = val;
        else if (key.equals("family")) s.familyId = val;
        else if (key.equals("class")) s.classId = val;
        else if (key.equals("name")) s.name = val;
        else if (key.equals("level")) s.level = parseInt(val);
        else if (key.equals("xp")) s.xp = parseLong(val);
        else if (key.equals("hp")) s.hp = parseInt(val);
        else if (key.equals("maxHp")) s.maxHp = parseInt(val);
        else if (key.equals("resource")) s.resource = parseInt(val);
        else if (key.equals("maxResource")) s.maxResource = parseInt(val);
        else if (key.equals("stance")) s.stance = val;
        else if (key.equals("school")) s.school = val;
        else if (key.equals("gold")) s.gold = parseLong(val);
        else if (key.equals("study")) s.studyAssignment = val.isEmpty() ? null : val;
        else if (key.equals("bankedStudy")) s.bankedStudy = parseDouble(val);
        else if (key.equals("posX")) s.posX = parseInt(val);
        else if (key.equals("posY")) s.posY = parseInt(val);
        else if (key.equals("zone")) s.zoneId = val;
        else if (key.equals("lastSave")) s.lastSaveEpochMillis = parseLong(val);
        else if (key.equals("asharrWar")) s.asharrWarScore = parseInt(val);
        else if (key.equals("kujixWar")) s.kujixWarScore = parseInt(val);
    }

    private static void applyQuest(SaveGame s, String rest, String val) {
        int dot = rest.indexOf('.');
        if (dot < 0) return;
        int idx = parseInt(rest.substring(0, dot));
        String field = rest.substring(dot + 1);
        while (s.quests.size() <= idx) s.quests.add(new QuestSave());
        QuestSave q = s.quests.get(idx);
        if (field.equals("id")) q.id = val;
        else if (field.equals("name")) q.name = val;
        else if (field.equals("desc")) q.description = val;
        else if (field.equals("giver")) q.giverNpcId = val;
        else if (field.equals("turnin")) q.turnInNpcId = val;
        else if (field.equals("levelReq")) q.levelReq = parseInt(val);
        else if (field.equals("xpReward")) q.xpReward = parseInt(val);
        else if (field.equals("goldReward")) q.goldReward = parseInt(val);
        else if (field.equals("procedural")) q.procedural = Boolean.parseBoolean(val);
        else if (field.equals("status")) q.status = val;
        else if (field.equals("rewards")) q.rewardItemIds = split(val);
        else if (field.startsWith("obj.")) {
            int j = parseInt(field.substring(4));
            while (q.objectives.size() <= j) q.objectives.add(new ObjSave());
            ObjSave o = q.objectives.get(j);
            String[] p = unpipe(val);
            o.type = p.length > 0 ? p[0] : "KILL";
            o.targetId = p.length > 1 ? p[1] : "";
            o.count = p.length > 2 ? parseInt(p[2]) : 1;
            o.progress = p.length > 3 ? parseInt(p[3]) : 0;
            o.text = p.length > 4 ? p[4] : "";
        }
    }

    // ---------- low-level helpers ----------

    private static void kv(StringBuilder b, String k, Object v) {
        b.append(k).append('=').append(v == null ? "" : v.toString()).append('\n');
    }

    private static String pipe(String... parts) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) b.append('|');
            b.append(esc(parts[i]));
        }
        return b.toString();
    }

    private static String[] unpipe(String s) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                if (n == 'p') cur.append('|');
                else if (n == 'n') cur.append('\n');
                else if (n == '\\') cur.append('\\');
                else cur.append(n);
            } else if (c == '|') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') b.append("\\\\");
            else if (c == '|') b.append("\\p");
            else if (c == '\n') b.append("\\n");
            else b.append(c);
        }
        return b.toString();
    }

    private static String join(List<String> xs) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < xs.size(); i++) {
            if (i > 0) b.append(',');
            b.append(xs.get(i));
        }
        return b.toString();
    }

    private static List<String> split(String s) {
        List<String> out = new ArrayList<String>();
        if (s == null || s.isEmpty()) return out;
        for (String p : s.split(",")) if (!p.isEmpty()) out.add(p);
        return out;
    }

    private static int parseInt(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private static long parseLong(String s) { try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0L; } }
    private static double parseDouble(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; } }
}
