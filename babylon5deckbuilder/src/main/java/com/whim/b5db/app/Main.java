package com.whim.b5db.app;

import com.whim.b5db.ai.Agent;
import com.whim.b5db.ai.HeuristicAgent;
import com.whim.b5db.ai.MonteCarloAgent;
import com.whim.b5db.ai.RandomAgent;
import com.whim.b5db.engine.GameConfig;
import com.whim.b5db.engine.GameEngine;
import com.whim.b5db.engine.GameResult;
import com.whim.b5db.engine.Seat;
import com.whim.b5db.io.TtsExporter;
import com.whim.b5db.model.Faction;
import com.whim.b5db.sim.AgentFactory;
import com.whim.b5db.sim.BalanceReport;
import com.whim.b5db.sim.Simulator;
import com.whim.b5db.ui.MainFrame;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Application entry point. With no arguments it launches the Swing UI. With
 * {@code --sim} it runs the headless Monte-Carlo balance harness; with
 * {@code --export-tts} it writes the catalogue as a Tabletop Simulator CSV.
 *
 * <pre>
 *   java -jar babylon5-deckbuilder.jar
 *   java -jar babylon5-deckbuilder.jar --sim --games 1000 --players 4 --seed 42 --ai hard
 *   java -jar babylon5-deckbuilder.jar --export-tts assets/tts_cards.csv
 * </pre>
 */
public final class Main {

    public static void main(String[] args) {
        Args a = new Args(args);
        File cardsDir = new File(a.get("cards", "assets/cards"));
        Catalog catalog = Catalog.load(cardsDir);
        System.out.println("Loaded " + catalog.cards().size() + " market cards ("
                + catalog.index().size() + " total in id index).");

        if (a.has("--export-tts")) {
            exportTts(catalog, a);
            return;
        }
        if (a.has("--sim")) {
            runSimulation(catalog, a);
            return;
        }
        launchUi(catalog);
    }

    private static void launchUi(final Catalog catalog) {
        javax.swing.SwingUtilities.invokeLater(() -> new MainFrame(catalog).setVisible(true));
    }

    private static void exportTts(Catalog catalog, Args a) {
        String out = a.get("--export-tts", "assets/tts_cards.csv");
        try {
            new TtsExporter().export(catalog.cards(), Paths.get(out));
            System.out.println("Exported " + catalog.cards().size() + " cards to " + out);
        } catch (Exception e) {
            System.err.println("TTS export failed: " + e.getMessage());
        }
    }

    private static void runSimulation(Catalog catalog, Args a) {
        int games = a.getInt("games", 1000);
        int players = clamp(a.getInt("players", 4), 2, 5);
        long seed = a.getLong("seed", 12345L);
        String ai = a.get("ai", "hard");
        int prestigeTarget = a.getInt("target", 40);

        GameEngine engine = new GameEngine(catalog.cards(), new GameConfig(prestigeTarget));

        List<Seat> seats = new ArrayList<>();
        Faction[] pool = Faction.playable();
        for (int i = 0; i < players; i++) {
            Faction f = pool[i % pool.length];
            seats.add(new Seat(f.display() + "-" + i, f, true));
        }

        final String aiKind = ai.toLowerCase();
        AgentFactory factory = (seatIndex, faction, gameSeed) -> makeAgent(aiKind, gameSeed, seatIndex);

        System.out.println("Running " + games + " games, " + players + " players, ai="
                + aiKind + ", seed=" + seed + " ...");
        long t0 = System.currentTimeMillis();
        List<GameResult> results = new Simulator(engine, seats, factory).run(games, seed);
        long ms = System.currentTimeMillis() - t0;

        BalanceReport report = new BalanceReport(results);
        System.out.println(report.summary());
        System.out.println("Completed in " + ms + " ms.");

        try {
            report.write(Paths.get("reports/balance.csv"), Paths.get("reports/balance.json"));
            System.out.println("Wrote reports/balance.csv and reports/balance.json");
        } catch (Exception e) {
            System.err.println("Could not write reports: " + e.getMessage());
        }
    }

    private static Agent makeAgent(String kind, long gameSeed, int seatIndex) {
        long s = gameSeed * 131L + seatIndex;
        switch (kind) {
            case "random": return new RandomAgent(s);
            case "easy": return new HeuristicAgent(HeuristicAgent.Difficulty.EASY);
            case "normal": return new HeuristicAgent(HeuristicAgent.Difficulty.NORMAL);
            case "mc":
            case "montecarlo": return new MonteCarloAgent(6, s);
            case "hard":
            default: return new HeuristicAgent(HeuristicAgent.Difficulty.HARD);
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Minimal {@code --key value} / {@code --flag} argument parser. */
    private static final class Args {
        private final List<String> raw;

        Args(String[] args) {
            this.raw = new ArrayList<>();
            for (String s : args) {
                raw.add(s);
            }
        }

        boolean has(String flag) {
            return raw.contains(flag);
        }

        String get(String key, String def) {
            String dashed = key.startsWith("--") ? key : "--" + key;
            int i = raw.indexOf(dashed);
            if (i >= 0 && i + 1 < raw.size()) {
                return raw.get(i + 1);
            }
            return def;
        }

        int getInt(String key, int def) {
            try {
                return Integer.parseInt(get(key, String.valueOf(def)));
            } catch (NumberFormatException e) {
                return def;
            }
        }

        long getLong(String key, long def) {
            try {
                return Long.parseLong(get(key, String.valueOf(def)));
            } catch (NumberFormatException e) {
                return def;
            }
        }
    }
}
