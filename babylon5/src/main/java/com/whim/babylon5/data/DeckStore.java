package com.whim.babylon5.data;

import com.whim.babylon5.domain.FactionId;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-faction custom decks, chosen in the in-app Deck Builder and persisted to
 * {@code ~/.babylon5/decks.json}. A deck is an ordered map of card id -&gt; count.
 *
 * <p>When a faction has a saved deck, {@link com.whim.babylon5.domain.GameFactory}
 * builds that player's draw deck from it (expanding counts) instead of using every
 * eligible card. Factions with no saved deck keep the default "all eligible cards"
 * behaviour, so the game always runs even with an empty {@code decks.json}.</p>
 */
public final class DeckStore {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /** faction name -> (card id -> count), insertion-ordered. */
    private static final Map<String, LinkedHashMap<String, Integer>> DECKS =
            new LinkedHashMap<String, LinkedHashMap<String, Integer>>();

    static {
        load();
    }

    private DeckStore() { }

    /** True if this faction has a non-empty custom deck saved. */
    public static synchronized boolean hasDeck(FactionId f) {
        LinkedHashMap<String, Integer> d = DECKS.get(f.name());
        return d != null && !d.isEmpty();
    }

    /** A copy of this faction's saved deck (card id -&gt; count); empty if none. */
    public static synchronized LinkedHashMap<String, Integer> deckFor(FactionId f) {
        LinkedHashMap<String, Integer> d = DECKS.get(f.name());
        return d == null ? new LinkedHashMap<String, Integer>()
                         : new LinkedHashMap<String, Integer>(d);
    }

    /**
     * Save (or clear, if {@code counts} is empty/null) this faction's deck and persist.
     * @return true if written to disk.
     */
    public static synchronized boolean saveDeck(FactionId f, Map<String, Integer> counts) {
        LinkedHashMap<String, Integer> clean = new LinkedHashMap<String, Integer>();
        if (counts != null) {
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                int c = e.getValue() == null ? 0 : e.getValue();
                if (c > 0) {
                    clean.put(e.getKey(), c);
                }
            }
        }
        if (clean.isEmpty()) {
            DECKS.remove(f.name());
        } else {
            DECKS.put(f.name(), clean);
        }
        return save();
    }

    public static String decksPath() {
        return decksFile().getAbsolutePath();
    }

    // ---------------------------------------------------------------------

    private static File decksFile() {
        String home = System.getProperty("user.home");
        return new File(new File(home == null ? "." : home, ".babylon5"), "decks.json");
    }

    @SuppressWarnings("unchecked")
    private static void load() {
        File f = decksFile();
        if (!f.isFile()) {
            return;
        }
        try {
            Object root = MiniJson.parse(readAll(new FileInputStream(f)));
            if (!(root instanceof Map)) {
                return;
            }
            Object decks = ((Map<String, Object>) root).get("decks");
            if (!(decks instanceof Map)) {
                return;
            }
            for (Map.Entry<String, Object> e : ((Map<String, Object>) decks).entrySet()) {
                if (!(e.getValue() instanceof List)) continue;
                LinkedHashMap<String, Integer> deck = new LinkedHashMap<String, Integer>();
                for (Object o : (List<Object>) e.getValue()) {
                    if (!(o instanceof Map)) continue;
                    Map<String, Object> m = (Map<String, Object>) o;
                    Object id = m.get("id");
                    if (id == null) continue;
                    int count = 1;
                    Object cnt = m.get("count");
                    if (cnt instanceof Number) count = ((Number) cnt).intValue();
                    else if (cnt instanceof String) {
                        try { count = Integer.parseInt(((String) cnt).trim()); }
                        catch (NumberFormatException ignore) { count = 1; }
                    }
                    if (count > 0) deck.put(String.valueOf(id), count);
                }
                if (!deck.isEmpty()) {
                    DECKS.put(e.getKey(), deck);
                }
            }
        } catch (Throwable t) {
            System.err.println("[DeckStore] load failed: " + t);
        }
    }

    private static boolean save() {
        File f = decksFile();
        try {
            File dir = f.getParentFile();
            if (dir != null && !dir.isDirectory()) {
                dir.mkdirs();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"_comment\": \"Babylon 5 CCG — custom decks. Edited by the in-app Deck Builder.\",\n");
            sb.append("  \"decks\": {\n");
            boolean firstFaction = true;
            for (Map.Entry<String, LinkedHashMap<String, Integer>> e : DECKS.entrySet()) {
                if (!firstFaction) sb.append(",\n");
                firstFaction = false;
                sb.append("    ").append(jsonStr(e.getKey())).append(": [\n");
                boolean firstCard = true;
                for (Map.Entry<String, Integer> c : e.getValue().entrySet()) {
                    if (!firstCard) sb.append(",\n");
                    firstCard = false;
                    sb.append("      {\"id\": ").append(jsonStr(c.getKey()))
                      .append(", \"count\": ").append(c.getValue()).append("}");
                }
                sb.append("\n    ]");
            }
            sb.append("\n  }\n}\n");
            Writer w = new OutputStreamWriter(new FileOutputStream(f), UTF8);
            try {
                w.write(sb.toString());
            } finally {
                w.close();
            }
            return true;
        } catch (Throwable t) {
            System.err.println("[DeckStore] save failed: " + t);
            return false;
        }
    }

    private static String jsonStr(String s) {
        if (s == null) return "\"\"";
        StringBuilder b = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':  b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (ch < 0x20) b.append(String.format("\\u%04x", (int) ch));
                    else b.append(ch);
            }
        }
        return b.append('"').toString();
    }

    private static String readAll(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(in, UTF8));
        try {
            char[] buf = new char[4096];
            int n;
            while ((n = br.read(buf)) != -1) sb.append(buf, 0, n);
        } finally {
            br.close();
        }
        return sb.toString();
    }
}
