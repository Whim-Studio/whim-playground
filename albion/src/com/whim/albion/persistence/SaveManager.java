package com.whim.albion.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Save/load to a saves directory using a hand-rolled, human-readable text format
 * (no external libraries, {@code java.io} only). The engine owns model access and
 * hands this class a {@link Snapshot}; this class owns the on-disk format and slot
 * management.
 *
 * <p>Because the {@code api} exposes no model setters, a save records the seed plus
 * the deltas that <em>can</em> be re-applied on load (map/position, gold, flags,
 * quests, party LP/SP). The engine rebuilds a fresh model from the seed and replays
 * those deltas. See {@code docs/task2-notes.md}.</p>
 *
 * <p>File format ({@code slotN.sav}):</p>
 * <pre>
 * ALBION_SAVE v1
 * seed=&lt;long&gt;
 * map=&lt;mapId&gt;
 * pos=&lt;x&gt;,&lt;y&gt;,&lt;FACING&gt;
 * gold=&lt;int&gt;
 * vital=&lt;lp&gt;,&lt;sp&gt;         (one per party member, in order)
 * flag=&lt;key&gt;=&lt;0|1&gt;
 * quest=&lt;id&gt;|&lt;title&gt;|&lt;STATUS&gt;|&lt;obj&gt;;;&lt;obj&gt;
 * </pre>
 */
public final class SaveManager {

    public static final int MAX_SLOTS = 5;
    private static final String MAGIC = "ALBION_SAVE v1";

    private final File dir;

    public SaveManager() {
        this(new File(System.getProperty("user.home", "."), ".albion" + File.separator + "saves"));
    }

    public SaveManager(File dir) {
        this.dir = dir;
        if (!dir.exists()) dir.mkdirs();
    }

    private File slotFile(int slot) { return new File(dir, "slot" + slot + ".sav"); }

    /** Human-readable labels for each slot (empty or a short summary). */
    public List<String> slotLabels() {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < MAX_SLOTS; i++) {
            File f = slotFile(i);
            if (!f.exists()) { out.add("Slot " + i + ": <empty>"); continue; }
            Snapshot s = read(i);
            if (s == null) out.add("Slot " + i + ": <corrupt>");
            else out.add("Slot " + i + ": " + (s.mapId == null ? "?" : s.mapId) + "  gold " + s.gold);
        }
        return out;
    }

    // ------------------------------------------------------------ write

    public boolean write(int slot, Snapshot s) {
        if (slot < 0 || slot >= MAX_SLOTS || s == null) return false;
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(slotFile(slot)), "UTF-8"));
            w.write(MAGIC); w.newLine();
            w.write("seed=" + s.seed); w.newLine();
            w.write("map=" + esc(s.mapId)); w.newLine();
            w.write("pos=" + s.px + "," + s.py + "," + esc(s.facing)); w.newLine();
            w.write("gold=" + s.gold); w.newLine();
            for (int i = 0; i < s.vitals.size(); i++)
                { w.write("vital=" + s.vitals.get(i)[0] + "," + s.vitals.get(i)[1]); w.newLine(); }
            for (Map.Entry<String, Boolean> e : s.flags.entrySet())
                { w.write("flag=" + esc(e.getKey()) + "=" + (e.getValue() ? 1 : 0)); w.newLine(); }
            for (int i = 0; i < s.quests.size(); i++) {
                QuestRec q = s.quests.get(i);
                StringBuilder objs = new StringBuilder();
                for (int j = 0; j < q.objectives.size(); j++) {
                    if (j > 0) objs.append(";;");
                    objs.append(esc(q.objectives.get(j)));
                }
                w.write("quest=" + esc(q.id) + "|" + esc(q.title) + "|" + esc(q.status) + "|" + objs);
                w.newLine();
            }
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            if (w != null) try { w.close(); } catch (IOException ignore) { }
        }
    }

    // ------------------------------------------------------------ read

    public Snapshot read(int slot) {
        if (slot < 0 || slot >= MAX_SLOTS) return null;
        File f = slotFile(slot);
        if (!f.exists()) return null;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            String header = r.readLine();
            if (header == null || !header.startsWith("ALBION_SAVE")) return null;
            Snapshot s = new Snapshot();
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq);
                String val = line.substring(eq + 1);
                if (key.equals("seed")) s.seed = parseLong(val);
                else if (key.equals("map")) s.mapId = unesc(val);
                else if (key.equals("pos")) {
                    String[] p = val.split(",", 3);
                    if (p.length == 3) { s.px = parseInt(p[0]); s.py = parseInt(p[1]); s.facing = unesc(p[2]); }
                } else if (key.equals("gold")) s.gold = parseInt(val);
                else if (key.equals("vital")) {
                    String[] p = val.split(",", 2);
                    if (p.length == 2) s.vitals.add(new int[]{ parseInt(p[0]), parseInt(p[1]) });
                } else if (key.equals("flag")) {
                    int e2 = val.lastIndexOf('=');
                    if (e2 > 0) s.flags.put(unesc(val.substring(0, e2)), val.substring(e2 + 1).trim().equals("1"));
                } else if (key.equals("quest")) {
                    String[] p = val.split("\\|", -1);
                    QuestRec q = new QuestRec();
                    if (p.length > 0) q.id = unesc(p[0]);
                    if (p.length > 1) q.title = unesc(p[1]);
                    if (p.length > 2) q.status = unesc(p[2]);
                    if (p.length > 3 && !p[3].isEmpty()) {
                        String[] objs = p[3].split(";;", -1);
                        for (int j = 0; j < objs.length; j++) q.objectives.add(unesc(objs[j]));
                    }
                    s.quests.add(q);
                }
            }
            return s;
        } catch (IOException ex) {
            return null;
        } finally {
            if (r != null) try { r.close(); } catch (IOException ignore) { }
        }
    }

    // ------------------------------------------------------------ escaping

    /** Encode characters that would break the line format. */
    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '|':  b.append("\\p"); break;
                case '=':  b.append("\\e"); break;
                case ';':  b.append("\\s"); break;
                case ',':  b.append("\\c"); break;
                default:   b.append(c);
            }
        }
        return b.toString();
    }

    private static String unesc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case '\\': b.append('\\'); break;
                    case 'n':  b.append('\n'); break;
                    case 'r':  b.append('\r'); break;
                    case 'p':  b.append('|'); break;
                    case 'e':  b.append('='); break;
                    case 's':  b.append(';'); break;
                    case 'c':  b.append(','); break;
                    default:   b.append(n);
                }
            } else b.append(c);
        }
        return b.toString();
    }

    private static long parseLong(String s) { try { return Long.parseLong(s.trim()); } catch (RuntimeException e) { return 0L; } }
    private static int parseInt(String s) { try { return Integer.parseInt(s.trim()); } catch (RuntimeException e) { return 0; } }

    // ------------------------------------------------------------ DTOs

    /** Everything a save records; produced and consumed by the engine. */
    public static final class Snapshot {
        public long seed;
        public String mapId;
        public int px, py;
        public String facing = "SOUTH";
        public int gold;
        public final List<int[]> vitals = new ArrayList<int[]>();   // {lp, sp} per member
        public final Map<String, Boolean> flags = new LinkedHashMap<String, Boolean>();
        public final List<QuestRec> quests = new ArrayList<QuestRec>();
    }

    /** One quest's savable state. */
    public static final class QuestRec {
        public String id;
        public String title;
        public String status = "ACTIVE";
        public final List<String> objectives = new ArrayList<String>();
    }
}
