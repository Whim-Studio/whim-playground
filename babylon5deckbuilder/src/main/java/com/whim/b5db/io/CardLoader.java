package com.whim.b5db.io;

import com.whim.b5db.model.Card;
import com.whim.b5db.model.CardType;
import com.whim.b5db.model.ContestType;
import com.whim.b5db.model.Effect;
import com.whim.b5db.model.Faction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@link Card} definitions from the JSON card DSL.
 *
 * <p>Cards can be supplied two ways, both honoured at startup so that
 * <em>dropping a new JSON file in and restarting makes the card available</em>
 * (an acceptance criterion):</p>
 * <ul>
 *   <li>Bundled resources under {@code classpath:/cards/*.json} (listed in
 *       {@code /cards/index.txt}, one filename per line).</li>
 *   <li>An optional external directory (e.g. {@code assets/cards}) whose
 *       {@code *.json} files are scanned at runtime.</li>
 * </ul>
 *
 * <p>Each JSON file is either a single card object or an array of card objects.</p>
 */
public final class CardLoader {

    /** Parse every {@code *.json} file in an external directory. */
    public List<Card> loadDirectory(File dir) {
        List<Card> cards = new ArrayList<>();
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
        if (files == null) {
            return cards;
        }
        for (File f : files) {
            try {
                String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                cards.addAll(parseDocument(text));
            } catch (IOException | RuntimeException e) {
                System.err.println("Skipping card file " + f.getName() + ": " + e.getMessage());
            }
        }
        return cards;
    }

    /** Load bundled card resources listed in {@code /cards/index.txt}. */
    public List<Card> loadClasspathIndex() {
        List<Card> cards = new ArrayList<>();
        List<String> names = readIndex();
        for (String name : names) {
            String path = "/cards/" + name.trim();
            try (InputStream in = CardLoader.class.getResourceAsStream(path)) {
                if (in == null) {
                    System.err.println("Bundled card resource not found: " + path);
                    continue;
                }
                cards.addAll(parseDocument(readAll(in)));
            } catch (IOException | RuntimeException e) {
                System.err.println("Skipping bundled card " + name + ": " + e.getMessage());
            }
        }
        return cards;
    }

    private List<String> readIndex() {
        List<String> names = new ArrayList<>();
        try (InputStream in = CardLoader.class.getResourceAsStream("/cards/index.txt")) {
            if (in == null) {
                return names;
            }
            for (String line : readAll(in).split("\\r?\\n")) {
                String t = line.trim();
                if (!t.isEmpty() && !t.startsWith("#")) {
                    names.add(t);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read /cards/index.txt: " + e.getMessage());
        }
        return names;
    }

    /** Parse a JSON document that is either a card object or an array of cards. */
    public List<Card> parseDocument(String text) {
        List<Card> cards = new ArrayList<>();
        Object root = Json.parse(text);
        if (root instanceof List) {
            for (Object o : Json.asArray(root)) {
                cards.add(fromMap(Json.asObject(o)));
            }
        } else {
            cards.add(fromMap(Json.asObject(root)));
        }
        return cards;
    }

    /** Translate one parsed JSON object into a {@link Card}. */
    @SuppressWarnings("unchecked")
    public Card fromMap(Map<String, Object> m) {
        String id = req(m, "id");
        String name = Json.str(m, "name", id);
        Faction faction = Faction.valueOf(Json.str(m, "faction", "NON_ALIGNED"));
        CardType type = CardType.valueOf(Json.str(m, "type", "CHARACTER"));
        int cost = Json.intv(m, "cost", 0);
        int prestige = Json.intv(m, "prestige", 0);

        Map<ContestType, Integer> attrs = new EnumMap<>(ContestType.class);
        Object a = m.get("attributes");
        if (a instanceof Map) {
            Map<String, Object> am = (Map<String, Object>) a;
            for (Map.Entry<String, Object> e : am.entrySet()) {
                attrs.put(ContestType.valueOf(e.getKey()),
                        (int) Math.round(((Number) e.getValue()).doubleValue()));
            }
        }

        ContestType contest = null;
        if (m.get("contest") != null) {
            contest = ContestType.valueOf(m.get("contest").toString());
        }
        int difficulty = Json.intv(m, "difficulty", 0);

        List<Effect> effects = new ArrayList<>();
        Object fx = m.get("effects");
        if (fx instanceof List) {
            for (Object o : (List<Object>) fx) {
                effects.add(effectFromMap((Map<String, Object>) o));
            }
        }

        String flavor = Json.str(m, "text", "");
        return new Card(id, name, faction, type, cost, prestige, attrs, contest, difficulty, effects, flavor);
    }

    private Effect effectFromMap(Map<String, Object> m) {
        Effect.Type type = Effect.Type.valueOf(req(m, "type"));
        int amount = Json.intv(m, "amount", 0);
        ContestType attr = null;
        if (m.get("attribute") != null) {
            attr = ContestType.valueOf(m.get("attribute").toString());
        }
        return new Effect(type, amount, attr);
    }

    private static String req(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) {
            throw new IllegalArgumentException("Missing required field '" + key + "'");
        }
        return v.toString();
    }

    private static String readAll(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) >= 0) {
                sb.append(buf, 0, n);
            }
        }
        return sb.toString();
    }

    /** Index cards by id, keeping the last definition for duplicate ids. */
    public static Map<String, Card> index(List<Card> cards) {
        Map<String, Card> byId = new LinkedHashMap<>();
        for (Card c : cards) {
            byId.put(c.id(), c);
        }
        return byId;
    }
}
