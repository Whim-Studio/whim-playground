package com.whim.babylon5.data;

import com.whim.babylon5.domain.Card;
import com.whim.babylon5.domain.CardType;
import com.whim.babylon5.domain.FactionId;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The card catalogue. Loads Premiere + Deluxe card definitions from
 * {@code src/main/resources/cards/*.json} on the classpath, and always falls
 * back to a small embedded set so the prototype runs even with zero JSON files.
 *
 * <p>Loading strategy (first that yields cards wins, results are merged):</p>
 * <ol>
 *   <li>Read {@code cards/manifest.txt} (one JSON filename per line) and parse
 *       each listed file from the classpath — portable, works from a jar.</li>
 *   <li>If the {@code cards} resource resolves to a real directory, list and
 *       parse every {@code *.json} in it — convenient when running exploded.</li>
 *   <li>If nothing loaded, use {@link #embedded()} — guaranteed non-empty.</li>
 * </ol>
 *
 * <p>The JSON schema (documented fully in {@code docs/research-dossier.md}) is a
 * top-level object with a {@code "cards"} array of card objects.</p>
 */
public final class CardDatabase {

    private static final String CARDS_DIR = "cards";
    private static final String MANIFEST = "cards/manifest.txt";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /** id -> definition, insertion-ordered. */
    private static final Map<String, Card> BY_ID = new LinkedHashMap<String, Card>();

    /**
     * User edits, kept separately so the packaged card files stay pristine.
     * Persisted to {@code ~/.babylon5/card-overrides.json} and re-applied on top
     * of the base catalogue every startup.
     */
    private static final Map<String, Card> OVERRIDES = new LinkedHashMap<String, Card>();

    static {
        load();
    }

    private CardDatabase() { }

    public static List<Card> all() {
        return Collections.unmodifiableList(new ArrayList<Card>(BY_ID.values()));
    }

    public static List<Card> forFaction(FactionId f) {
        List<Card> out = new ArrayList<Card>();
        for (Card c : BY_ID.values()) {
            // A faction's deck is its own loyal cards plus neutral (NONALIGNED) cards.
            if (c.getFaction() == f || c.getFaction() == FactionId.NONALIGNED) {
                out.add(c);
            }
        }
        return out;
    }

    public static Card byId(String id) {
        return BY_ID.get(id);
    }

    /** A fresh in-play instance of {@code c} with ready=true and no damage. */
    public static Card copyOf(Card c) {
        if (c == null) return null;
        Card copy = new Card(c.getId(), c.getName(), c.getType(), c.getFaction(),
                c.getCost(), c.getInfluence(), c.getDiplomacy(), c.getIntrigue(),
                c.getPsi(), c.getMilitary(), c.getText(), c.getImageUrl());
        copy.setReady(true);
        copy.clearDamage();
        copy.clearAttachments();
        return copy;
    }

    // ---------------------------------------------------------------------
    // Editing + persistence (card overrides)
    // ---------------------------------------------------------------------

    /**
     * Replace a card definition with an edited one and persist the change.
     * The edit is written to {@code ~/.babylon5/card-overrides.json} and
     * survives restarts; the packaged card files are never modified.
     *
     * <p>Edits change the master catalogue, so they take effect for cards dealt
     * in the <em>next</em> game — cards already in play this game are independent
     * copies made when the game was created.</p>
     *
     * @return {@code true} if the edit was saved to disk, {@code false} if it was
     *         applied in memory but could not be persisted.
     */
    public static synchronized boolean updateCard(Card edited) {
        if (edited == null || edited.getId() == null) {
            return false;
        }
        BY_ID.put(edited.getId(), edited);
        OVERRIDES.put(edited.getId(), edited);
        return saveOverrides();
    }

    /** Absolute path of the overrides file, for surfacing to the user. */
    public static String overridesPath() {
        return overridesFile().getAbsolutePath();
    }

    private static File overridesFile() {
        String home = System.getProperty("user.home");
        File dir = new File(home == null ? "." : home, ".babylon5");
        return new File(dir, "card-overrides.json");
    }

    private static void loadOverrides() {
        File f = overridesFile();
        if (!f.isFile()) {
            return;
        }
        try {
            int n = 0;
            String json = readAll(new java.io.FileInputStream(f));
            Object root = MiniJson.parse(json);
            @SuppressWarnings("unchecked")
            List<Object> arr = extractCards(root);
            for (Object o : arr) {
                if (!(o instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Card c = fromMap((Map<String, Object>) o);
                if (c != null) {
                    OVERRIDES.put(c.getId(), c);
                    BY_ID.put(c.getId(), c); // override existing, or add if brand new
                    n++;
                }
            }
            if (n > 0) {
                System.out.println("[CardDatabase] applied " + n + " card override(s) from " + f);
            }
        } catch (Throwable t) {
            System.err.println("[CardDatabase] override load failed: " + t);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> extractCards(Object root) {
        if (root instanceof Map) {
            Object cards = ((Map<String, Object>) root).get("cards");
            if (cards instanceof List) return (List<Object>) cards;
        } else if (root instanceof List) {
            return (List<Object>) root;
        }
        return new ArrayList<Object>();
    }

    private static boolean saveOverrides() {
        File f = overridesFile();
        try {
            File dir = f.getParentFile();
            if (dir != null && !dir.isDirectory()) {
                dir.mkdirs();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"_comment\": \"Babylon 5 CCG — user card edits. Regenerated by the in-app Card Editor.\",\n");
            sb.append("  \"cards\": [\n");
            boolean first = true;
            for (Card c : OVERRIDES.values()) {
                if (!first) sb.append(",\n");
                first = false;
                appendCardJson(sb, c);
            }
            sb.append("\n  ]\n}\n");
            java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(f), UTF8);
            try {
                w.write(sb.toString());
            } finally {
                w.close();
            }
            return true;
        } catch (Throwable t) {
            System.err.println("[CardDatabase] override save failed: " + t);
            return false;
        }
    }

    private static void appendCardJson(StringBuilder sb, Card c) {
        sb.append("    {");
        sb.append("\"id\": ").append(jsonStr(c.getId())).append(", ");
        sb.append("\"name\": ").append(jsonStr(c.getName())).append(", ");
        sb.append("\"type\": ").append(jsonStr(c.getType().name())).append(", ");
        sb.append("\"faction\": ").append(jsonStr(c.getFaction().name())).append(", ");
        sb.append("\"cost\": ").append(c.getCost()).append(", ");
        sb.append("\"influence\": ").append(c.getInfluence()).append(", ");
        sb.append("\"diplomacy\": ").append(c.getDiplomacy()).append(", ");
        sb.append("\"intrigue\": ").append(c.getIntrigue()).append(", ");
        sb.append("\"psi\": ").append(c.getPsi()).append(", ");
        sb.append("\"military\": ").append(c.getMilitary()).append(", ");
        sb.append("\"text\": ").append(jsonStr(c.getText())).append(", ");
        sb.append("\"imageUrl\": ").append(jsonStr(c.getImageUrl()));
        sb.append("}");
    }

    private static String jsonStr(String s) {
        if (s == null) return "\"\"";
        StringBuilder b = new StringBuilder(s.length() + 2);
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':  b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (ch < 0x20) {
                        b.append(String.format("\\u%04x", (int) ch));
                    } else {
                        b.append(ch);
                    }
            }
        }
        b.append('"');
        return b.toString();
    }

    // ---------------------------------------------------------------------

    private static synchronized void load() {
        BY_ID.clear();
        int loaded = 0;
        try {
            loaded += loadFromManifest();
            if (loaded == 0) {
                loaded += loadFromDirectory();
            }
        } catch (Throwable t) {
            // Never let resource loading break the prototype.
            System.err.println("[CardDatabase] resource load failed: " + t);
        }
        if (BY_ID.isEmpty()) {
            for (Card c : embedded()) {
                BY_ID.put(c.getId(), c);
            }
        }
        loadOverrides();
    }

    private static int loadFromManifest() {
        InputStream in = cl().getResourceAsStream(MANIFEST);
        if (in == null) return 0;
        int count = 0;
        try {
            for (String line : readLines(in)) {
                String name = line.trim();
                if (name.isEmpty() || name.startsWith("#")) continue;
                InputStream js = cl().getResourceAsStream(CARDS_DIR + "/" + name);
                if (js != null) {
                    count += parseInto(readAll(js));
                }
            }
        } catch (Throwable t) {
            System.err.println("[CardDatabase] manifest load failed: " + t);
        }
        return count;
    }

    private static int loadFromDirectory() {
        try {
            URL url = cl().getResource(CARDS_DIR);
            if (url == null || !"file".equals(url.getProtocol())) return 0;
            File dir = new File(url.toURI());
            if (!dir.isDirectory()) return 0;
            File[] files = dir.listFiles();
            if (files == null) return 0;
            int count = 0;
            for (File f : files) {
                if (f.isFile() && f.getName().toLowerCase().endsWith(".json")) {
                    count += parseInto(readAll(new java.io.FileInputStream(f)));
                }
            }
            return count;
        } catch (Throwable t) {
            System.err.println("[CardDatabase] directory load failed: " + t);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static int parseInto(String json) {
        Object root = MiniJson.parse(json);
        List<Object> arr;
        if (root instanceof Map) {
            Object cards = ((Map<String, Object>) root).get("cards");
            arr = (cards instanceof List) ? (List<Object>) cards : new ArrayList<Object>();
        } else if (root instanceof List) {
            arr = (List<Object>) root;
        } else {
            return 0;
        }
        int count = 0;
        for (Object o : arr) {
            if (!(o instanceof Map)) continue;
            Card c = fromMap((Map<String, Object>) o);
            if (c != null) {
                BY_ID.put(c.getId(), c);
                count++;
            }
        }
        return count;
    }

    private static Card fromMap(Map<String, Object> m) {
        try {
            String id = str(m, "id", null);
            String name = str(m, "name", id);
            if (id == null || name == null) return null;
            CardType type = CardType.valueOf(str(m, "type", "CHARACTER").toUpperCase());
            FactionId faction = FactionId.valueOf(str(m, "faction", "NONALIGNED").toUpperCase());
            return new Card(
                    id, name, type, faction,
                    intOf(m, "cost"), intOf(m, "influence"),
                    intOf(m, "diplomacy"), intOf(m, "intrigue"),
                    intOf(m, "psi"), intOf(m, "military"),
                    str(m, "text", ""), str(m, "imageUrl", ""));
        } catch (Throwable t) {
            System.err.println("[CardDatabase] bad card record: " + t);
            return null;
        }
    }

    private static String str(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v == null ? def : String.valueOf(v);
    }

    private static int intOf(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt(((String) v).trim()); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private static ClassLoader cl() {
        ClassLoader c = Thread.currentThread().getContextClassLoader();
        return c != null ? c : CardDatabase.class.getClassLoader();
    }

    private static List<String> readLines(InputStream in) throws Exception {
        List<String> lines = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new InputStreamReader(in, UTF8));
        try {
            String line;
            while ((line = br.readLine()) != null) lines.add(line);
        } finally {
            br.close();
        }
        return lines;
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

    // ---------------------------------------------------------------------
    // Embedded fallback. Minimal text only; no copyrighted card text or art.
    // Public image URLs point at the community wiki (no bytes embedded).
    // ---------------------------------------------------------------------

    private static List<Card> embedded() {
        List<Card> c = new ArrayList<Card>();
        // Ambassadors (one per Premiere player race).
        c.add(new Card("emb-amb-human", "Jeffrey Sinclair", CardType.AMBASSADOR, FactionId.HUMAN,
                0, 0, 3, 2, 0, 0, "Starting Ambassador (Earth Alliance).", ""));
        c.add(new Card("emb-amb-minbari", "Delenn", CardType.AMBASSADOR, FactionId.MINBARI,
                0, 0, 3, 1, 2, 0, "Starting Ambassador (Minbari Federation).", ""));
        c.add(new Card("emb-amb-narn", "G'Kar", CardType.AMBASSADOR, FactionId.NARN,
                0, 0, 2, 3, 0, 0, "Starting Ambassador (Narn Regime).", ""));
        c.add(new Card("emb-amb-centauri", "Londo Mollari", CardType.AMBASSADOR, FactionId.CENTAURI,
                0, 0, 2, 3, 0, 0, "Starting Ambassador (Centauri Republic).", ""));
        // A few supporting characters so decks are playable head-less.
        c.add(new Card("emb-chr-garibaldi", "Michael Garibaldi", CardType.CHARACTER, FactionId.HUMAN,
                2, 0, 1, 3, 0, 0, "Human character.", ""));
        c.add(new Card("emb-chr-lennier", "Lennier", CardType.CHARACTER, FactionId.MINBARI,
                2, 0, 2, 1, 1, 0, "Minbari character.", ""));
        c.add(new Card("emb-chr-natoth", "Na'Toth", CardType.CHARACTER, FactionId.NARN,
                2, 0, 1, 3, 0, 0, "Narn character.", ""));
        c.add(new Card("emb-chr-vir", "Vir Cotto", CardType.CHARACTER, FactionId.CENTAURI,
                1, 0, 2, 1, 0, 0, "Ambassador's Assistant (Centauri).", ""));
        c.add(new Card("emb-chr-talia", "Talia Winters", CardType.CHARACTER, FactionId.NONALIGNED,
                3, 0, 1, 1, 3, 0, "Neutral telepath.", ""));
        // A conflict and an aftermath so the engine has something to resolve.
        c.add(new Card("emb-cnf-diplomacy", "Diplomatic Summit", CardType.CONFLICT, FactionId.NONALIGNED,
                0, 0, 0, 0, 0, 0, "Diplomacy conflict.", ""));
        c.add(new Card("emb-cnf-military", "Border Skirmish", CardType.CONFLICT, FactionId.NONALIGNED,
                0, 0, 0, 0, 0, 0, "Military conflict.", ""));
        c.add(new Card("emb-aft-warhero", "War Hero", CardType.AFTERMATH, FactionId.NONALIGNED,
                0, 0, 0, 0, 0, 0, "Won Military aftermath.", ""));
        return c;
    }
}
