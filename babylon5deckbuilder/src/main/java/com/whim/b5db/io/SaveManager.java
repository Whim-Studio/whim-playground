package com.whim.b5db.io;

import com.whim.b5db.engine.GameState;
import com.whim.b5db.engine.Market;
import com.whim.b5db.engine.PlayerState;
import com.whim.b5db.engine.Rng;
import com.whim.b5db.model.Card;
import com.whim.b5db.model.Faction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSON save/load of a game snapshot. Cards are stored by id and resolved back
 * through a supplied id→card index on load, so save files stay compact and are
 * robust to catalogue changes (unknown ids are skipped with a warning).
 */
public final class SaveManager {

    /** Serialise a game to a JSON string. */
    public String toJson(GameState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"prestigeTarget\": ").append(state.prestigeTarget()).append(",\n");
        sb.append("  \"turn\": ").append(state.turn()).append(",\n");
        sb.append("  \"currentPlayer\": ").append(state.currentPlayerIndex()).append(",\n");
        sb.append("  \"seed\": ").append(state.rng().seed()).append(",\n");
        sb.append("  \"rim\": ").append(ids(state.market().rim())).append(",\n");
        sb.append("  \"players\": [\n");
        List<String> players = new ArrayList<>();
        for (PlayerState p : state.players()) {
            players.add(playerJson(p));
        }
        sb.append(String.join(",\n", players)).append("\n  ]\n}\n");
        return sb.toString();
    }

    private String playerJson(PlayerState p) {
        StringBuilder sb = new StringBuilder();
        sb.append("    {\n");
        sb.append("      \"name\": ").append(Json.quote(p.name())).append(",\n");
        sb.append("      \"faction\": ").append(Json.quote(p.faction().name())).append(",\n");
        sb.append("      \"ai\": ").append(p.ai()).append(",\n");
        sb.append("      \"prestige\": ").append(p.prestige()).append(",\n");
        sb.append("      \"drawDeck\": ").append(ids(p.drawDeck())).append(",\n");
        sb.append("      \"hand\": ").append(ids(p.hand())).append(",\n");
        sb.append("      \"commandRow\": ").append(ids(p.commandRow())).append(",\n");
        sb.append("      \"discard\": ").append(ids(p.discard())).append(",\n");
        sb.append("      \"outOfGame\": ").append(ids(p.outOfGame())).append("\n");
        sb.append("    }");
        return sb.toString();
    }

    private String ids(List<Card> cards) {
        List<String> out = new ArrayList<>();
        for (Card c : cards) {
            out.add(Json.quote(c.id()));
        }
        return "[" + String.join(", ", out) + "]";
    }

    public void save(GameState state, Path path) throws IOException {
        if (path.toAbsolutePath().getParent() != null) {
            Files.createDirectories(path.toAbsolutePath().getParent());
        }
        Files.write(path, toJson(state).getBytes(StandardCharsets.UTF_8));
    }

    /** Reconstruct a game from a JSON snapshot and an id→card index. */
    @SuppressWarnings("unchecked")
    public GameState fromJson(String json, Map<String, Card> index) {
        Map<String, Object> root = Json.asObject(Json.parse(json));
        int prestigeTarget = Json.intv(root, "prestigeTarget", 40);
        long seed = (long) Math.round(((Number) root.getOrDefault("seed", 0.0)).doubleValue());

        List<PlayerState> players = new ArrayList<>();
        for (Object po : Json.asArray(root.get("players"))) {
            Map<String, Object> pm = Json.asObject(po);
            Faction faction = Faction.valueOf(Json.str(pm, "faction", "NON_ALIGNED"));
            PlayerState p = new PlayerState(Json.str(pm, "name", "Player"), faction,
                    Boolean.TRUE.equals(pm.get("ai")));
            p.addPrestige(Json.intv(pm, "prestige", 0));
            fill(p.drawDeck(), pm.get("drawDeck"), index);
            fill(p.hand(), pm.get("hand"), index);
            fill(p.commandRow(), pm.get("commandRow"), index);
            fill(p.discard(), pm.get("discard"), index);
            fill(p.outOfGame(), pm.get("outOfGame"), index);
            players.add(p);
        }

        List<Card> rim = new ArrayList<>();
        fill(rim, root.get("rim"), index);
        Rng rng = new Rng(seed);
        Market market = new Market(rim, com.whim.b5db.engine.BasicCards.corridorPiles(), rng);
        return new GameState(players, market, rng, prestigeTarget);
    }

    private void fill(List<Card> target, Object arr, Map<String, Card> index) {
        if (!(arr instanceof List)) {
            return;
        }
        for (Object o : (List<Object>) arr) {
            Card c = index.get(o.toString());
            if (c != null) {
                target.add(c);
            } else {
                System.err.println("Save references unknown card id: " + o);
            }
        }
    }

    public GameState load(Path path, Map<String, Card> index) throws IOException {
        return fromJson(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), index);
    }
}
