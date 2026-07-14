package com.whim.b5db.sim;

import com.whim.b5db.engine.GameResult;
import com.whim.b5db.io.Json;
import com.whim.b5db.model.Faction;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates {@link GameResult}s into faction win-rates and game-length stats,
 * and serialises the summary to CSV and JSON. The CSV also flags whether each
 * faction lands inside the GDD balance target band (20%–35%).
 */
public final class BalanceReport {

    private static final double TARGET_LOW = 0.20;
    private static final double TARGET_HIGH = 0.35;

    private final int games;
    private final Map<Faction, Integer> wins = new EnumMap<>(Faction.class);
    private final Map<Faction, Integer> appearances = new EnumMap<>(Faction.class);
    private final double avgTurns;
    private final int minTurns;
    private final int maxTurns;

    public BalanceReport(List<GameResult> results) {
        this.games = results.size();
        long turnSum = 0;
        int mn = Integer.MAX_VALUE;
        int mx = Integer.MIN_VALUE;
        for (GameResult r : results) {
            wins.merge(r.winnerFaction(), 1, Integer::sum);
            for (Faction f : r.factions()) {
                appearances.merge(f, 1, Integer::sum);
            }
            turnSum += r.turns();
            mn = Math.min(mn, r.turns());
            mx = Math.max(mx, r.turns());
        }
        this.avgTurns = games == 0 ? 0 : (double) turnSum / games;
        this.minTurns = games == 0 ? 0 : mn;
        this.maxTurns = games == 0 ? 0 : mx;
    }

    public int games() {
        return games;
    }

    public double winRate(Faction f) {
        int w = wins.getOrDefault(f, 0);
        return games == 0 ? 0 : (double) w / games;
    }

    public double avgTurns() {
        return avgTurns;
    }

    /** True if every faction that appeared lands within the GDD target band. */
    public boolean withinTargets() {
        for (Faction f : Faction.playable()) {
            if (appearances.getOrDefault(f, 0) == 0) {
                continue;
            }
            double wr = winRate(f);
            if (wr < TARGET_LOW || wr > TARGET_HIGH) {
                return false;
            }
        }
        return true;
    }

    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("faction,wins,games,win_rate,within_target_band\n");
        for (Faction f : Faction.values()) {
            int w = wins.getOrDefault(f, 0);
            if (w == 0 && appearances.getOrDefault(f, 0) == 0) {
                continue;
            }
            double wr = winRate(f);
            boolean band = wr >= TARGET_LOW && wr <= TARGET_HIGH;
            sb.append(f.name()).append(',').append(w).append(',').append(games)
                    .append(',').append(String.format("%.4f", wr))
                    .append(',').append(band).append('\n');
        }
        sb.append("# avg_turns,").append(String.format("%.2f", avgTurns)).append('\n');
        sb.append("# min_turns,").append(minTurns).append('\n');
        sb.append("# max_turns,").append(maxTurns).append('\n');
        return sb.toString();
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"games\": ").append(games).append(",\n");
        sb.append("  \"avgTurns\": ").append(String.format("%.2f", avgTurns)).append(",\n");
        sb.append("  \"minTurns\": ").append(minTurns).append(",\n");
        sb.append("  \"maxTurns\": ").append(maxTurns).append(",\n");
        sb.append("  \"withinTargetBand\": ").append(withinTargets()).append(",\n");
        sb.append("  \"factions\": [\n");
        List<String> rows = new ArrayList<>();
        for (Faction f : Faction.values()) {
            if (wins.getOrDefault(f, 0) == 0 && appearances.getOrDefault(f, 0) == 0) {
                continue;
            }
            rows.add("    {\"faction\": " + Json.quote(f.name())
                    + ", \"wins\": " + wins.getOrDefault(f, 0)
                    + ", \"winRate\": " + String.format("%.4f", winRate(f)) + "}");
        }
        sb.append(String.join(",\n", rows)).append("\n  ]\n}\n");
        return sb.toString();
    }

    public void write(Path csvPath, Path jsonPath) throws IOException {
        Files.createDirectories(csvPath.toAbsolutePath().getParent());
        try (Writer w = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
            w.write(toCsv());
        }
        try (Writer w = Files.newBufferedWriter(jsonPath, StandardCharsets.UTF_8)) {
            w.write(toJson());
        }
    }

    /** Short human-readable summary for the console log. */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Games: ").append(games)
                .append(" | avg turns: ").append(String.format("%.1f", avgTurns))
                .append(" | within 20-35% band: ").append(withinTargets()).append('\n');
        for (Faction f : Faction.playable()) {
            sb.append(String.format("  %-20s %5.1f%%%n", f.display(), winRate(f) * 100));
        }
        return sb.toString();
    }
}
