package com.whim.oggalaxy.engine;

import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.FleetOrder;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.NewGameSetup;
import com.whim.oggalaxy.api.Result;
import com.whim.oggalaxy.api.Views;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Headless smoke test for the whole simulation. Creates a game, runs ~50 ticks, dispatches
 * a player attack fleet to force a deterministic battle, saves and reloads the game, and
 * prints a summary. Lets the engine be verified with no UI:
 *
 *   javac --release 8 -d out $(find src -name '*.java')
 *   java  -cp out com.whim.oggalaxy.engine.EngineSelfCheck
 */
public final class EngineSelfCheck {

    public static void main(String[] args) throws Exception {
        System.out.println("=== OG Galaxy — Engine self-check ===");

        GameEngine engine = new GameEngine();

        List<NewGameSetup.AIConfig> opponents = new ArrayList<NewGameSetup.AIConfig>();
        opponents.add(new NewGameSetup.AIConfig("Zarkon Hegemony", Ids.Difficulty.HARD));
        opponents.add(new NewGameSetup.AIConfig("Vega Collective", Ids.Difficulty.EASY));
        opponents.add(new NewGameSetup.AIConfig(null, Ids.Difficulty.RANDOM));
        NewGameSetup setup = new NewGameSetup("Test Commander", Ids.PlayerClass.GENERAL, opponents, 1234567L);

        engine.newGame(setup);
        Views.GameStateView s = engine.state();
        System.out.println("New game: " + s.empires().size() + " empires, player = " + s.player().name());
        System.out.println("Resolved AI difficulties:");
        for (Views.EmpireView e : s.empires()) {
            if (e.isAI()) System.out.println("  - " + e.name() + ": " + e.difficulty() + " / " + e.playerClass());
        }

        // ---- run ~50 ticks of simulation ----
        engine.advance(50);
        s = engine.state();
        System.out.println("\nAfter 50 ticks (tick=" + s.currentTick() + ", " + s.formattedTime() + "):");
        printEmpires(s);
        Views.PlanetView home = s.selectedPlanet();
        System.out.println("Player home resources: " + resLine(home));
        System.out.println("Player home production/h: m=" + (long) home.resources().productionPerTick(Ids.ResourceType.METAL)
                + " c=" + (long) home.resources().productionPerTick(Ids.ResourceType.CRYSTAL)
                + " d=" + (long) home.resources().productionPerTick(Ids.ResourceType.DEUTERIUM)
                + " energyRatio=" + String.format("%.2f", home.resources().energyRatio()));

        // ---- dispatch a fleet to force a battle ----
        Views.EmpireView targetEmpire = null;
        for (Views.EmpireView e : s.empires()) {
            if (e.isAI() && e.alive() && !e.planets().isEmpty()) { targetEmpire = e; break; }
        }
        if (targetEmpire != null) {
            Views.PlanetView target = targetEmpire.planets().get(0);
            Map<Ids.ShipType, Integer> ships = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
            addIf(ships, home, Ids.ShipType.LIGHT_FIGHTER);
            addIf(ships, home, Ids.ShipType.CRUISER);
            addIf(ships, home, Ids.ShipType.HEAVY_FIGHTER);
            addIf(ships, home, Ids.ShipType.BATTLESHIP);
            if (ships.isEmpty()) addIf(ships, home, Ids.ShipType.SMALL_CARGO);
            FleetOrder order = new FleetOrder(home.id(), target.galaxy(), target.system(), target.position(),
                    false, Ids.MissionType.ATTACK, ships, Cost.ZERO, 100, 0);
            Result r = engine.dispatchFleet(order);
            System.out.println("\nDispatch attack on " + targetEmpire.name() + " @ "
                    + target.galaxy() + ":" + target.system() + ":" + target.position() + " -> " + r.message);
        }

        // ---- advance until the battle resolves ----
        for (int i = 0; i < 12 && engine.state().combatReports().isEmpty(); i++) engine.advance(1);
        s = engine.state();

        List<Views.CombatReportView> combats = s.combatReports();
        System.out.println("\nCombat reports: " + combats.size());
        if (!combats.isEmpty()) {
            Views.CombatReportView cr = combats.get(combats.size() - 1);
            System.out.println("  latest: " + cr.attackerName() + " vs " + cr.defenderName()
                    + " @ " + cr.location()[0] + ":" + cr.location()[1] + ":" + cr.location()[2]);
            System.out.println("  outcome: " + cr.outcome());
            System.out.println("  rounds : " + cr.roundSummaries().size()
                    + ", debris=" + cr.debris() + ", plunder=" + cr.plunder() + ", moon=" + cr.moonCreated());
            for (String line : cr.roundSummaries()) System.out.println("    " + line);
        } else {
            System.out.println("  (no battle occurred in the window — dispatch may have been blocked)");
        }

        // ---- dispatch an expedition to exercise that path too ----
        Views.PlanetView home2 = engine.state().selectedPlanet();
        if (home2.shipCount(Ids.ShipType.LIGHT_FIGHTER) > 0 || home2.shipCount(Ids.ShipType.SMALL_CARGO) > 0) {
            Map<Ids.ShipType, Integer> ex = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
            addIf(ex, home2, Ids.ShipType.SMALL_CARGO);
            addIf(ex, home2, Ids.ShipType.LIGHT_FIGHTER);
            if (!ex.isEmpty()) {
                FleetOrder exp = new FleetOrder(home2.id(), 1, home2.system(), 15, false,
                        Ids.MissionType.EXPEDITION, ex, Cost.ZERO, 100, 1);
                Result er = engine.dispatchFleet(exp);
                System.out.println("\nDispatch expedition -> " + er.message);
                for (int i = 0; i < 12 && engine.state().expeditionReports().isEmpty(); i++) engine.advance(1);
            }
        }
        List<Views.ExpeditionReportView> exps = engine.state().expeditionReports();
        System.out.println("Expedition reports: " + exps.size()
                + (exps.isEmpty() ? "" : " (latest: " + exps.get(exps.size() - 1).outcome() + ")"));

        // ---- save + load ----
        File tmp = File.createTempFile("oggalaxy-selfcheck", ".sav");
        Result saveR = engine.save(tmp);
        System.out.println("\nSave: " + saveR.message + " (" + tmp.length() + " bytes)");
        int tickBefore = engine.state().currentTick();
        long scoreBefore = engine.state().player().score();
        Result loadR = engine.load(tmp);
        System.out.println("Load: " + loadR.message);
        Views.GameStateView after = engine.state();
        boolean ok = after.currentTick() == tickBefore && after.player().score() == scoreBefore;
        System.out.println("Round-trip check: tick " + tickBefore + "->" + after.currentTick()
                + ", score " + scoreBefore + "->" + after.player().score() + " : " + (ok ? "OK" : "MISMATCH"));

        // ---- continue a few ticks post-load to prove the reloaded graph still ticks ----
        engine.advance(5);
        System.out.println("Post-load advance OK, tick now " + engine.state().currentTick());
        tmp.delete();

        // ---- final summary ----
        System.out.println("\n=== Summary ===");
        s = engine.state();
        System.out.println("phase=" + s.phase() + ", tick=" + s.currentTick()
                + ", fleets=" + s.fleets().size() + ", logs=" + s.log().size()
                + ", combats=" + s.combatReports().size() + ", expeditions=" + s.expeditionReports().size());
        printEmpires(s);
        System.out.println("\nSelf-check completed successfully.");
    }

    private static void addIf(Map<Ids.ShipType, Integer> m, Views.PlanetView p, Ids.ShipType t) {
        int c = p.shipCount(t);
        if (c > 0) m.put(t, c);
    }

    private static void printEmpires(Views.GameStateView s) {
        for (Views.EmpireView e : s.empires()) {
            System.out.println("  " + pad(e.name(), 22) + " " + (e.isPlayer() ? "[you] " : "[AI]  ")
                    + "planets=" + e.planets().size() + " score=" + e.score()
                    + " alive=" + e.alive());
        }
    }

    private static String resLine(Views.PlanetView p) {
        return "m=" + (long) p.resources().amount(Ids.ResourceType.METAL)
                + " c=" + (long) p.resources().amount(Ids.ResourceType.CRYSTAL)
                + " d=" + (long) p.resources().amount(Ids.ResourceType.DEUTERIUM)
                + " dm=" + (long) p.resources().amount(Ids.ResourceType.DARK_MATTER);
    }

    private static String pad(String s, int n) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }
}
