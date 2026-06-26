package com.whim.starcraft8.engine;

import com.whim.starcraft8.data.MapFactory;
import com.whim.starcraft8.domain.Building;
import com.whim.starcraft8.domain.GameState;
import com.whim.starcraft8.domain.Player;
import com.whim.starcraft8.domain.Race;
import com.whim.starcraft8.domain.Unit;

/**
 * Headless AI-vs-AI sanity run. Pumps the simulation deterministically (no background
 * thread) for a few thousand ticks and prints unit/building counts and the winner.
 * Run after consolidation: {@code java -cp out com.whim.starcraft8.engine.EngineSmokeTest}.
 */
public final class EngineSmokeTest {
    private EngineSmokeTest() {}

    public static void main(String[] args) {
        int maxTicks = args.length > 0 ? Integer.parseInt(args[0]) : 12000;

        GameState gs = MapFactory.newSkirmish(Race.TERRAN, Race.ZERG);
        // humanPlayerId = -1 → every player is AI-driven.
        SimulationImpl sim = (SimulationImpl) Engine.create(gs, -1);

        System.out.println("8-Bit StarCraft — headless AI-vs-AI smoke test");
        System.out.println("players: " + describePlayers(gs));

        long t = 0;
        for (; t < maxTicks; t++) {
            sim.tickOnce();
            if (t % 1200 == 0) report(gs, t);
            if (gs.winnerId() != -1) break;
        }

        System.out.println("------------------------------------------------");
        report(gs, t);
        int w = gs.winnerId();
        if (w == -1) {
            System.out.println("RESULT: no winner after " + t + " ticks (still contesting)");
        } else {
            Player wp = gs.player(w);
            System.out.println("RESULT: winner = player " + w
                    + (wp != null ? " (" + wp.race() + ")" : "") + " after " + t + " ticks");
        }
    }

    private static String describePlayers(GameState gs) {
        StringBuilder sb = new StringBuilder();
        for (Player p : gs.players()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("P").append(p.id()).append('=').append(p.race());
        }
        return sb.toString();
    }

    private static void report(GameState gs, long tick) {
        StringBuilder sb = new StringBuilder();
        sb.append("t=").append(tick);
        for (Player p : gs.players()) {
            int units = 0, blds = 0;
            for (Unit u : gs.units()) if (u.ownerId() == p.id() && u.alive()) units++;
            for (Building b : gs.buildings()) if (b.ownerId() == p.id() && b.alive()) blds++;
            sb.append(" | P").append(p.id()).append('[').append(p.race()).append(']')
              .append(" u=").append(units)
              .append(" b=").append(blds)
              .append(" m=").append(p.minerals())
              .append(" g=").append(p.gas())
              .append(" s=").append(p.supplyUsed()).append('/').append(p.supplyCap());
        }
        System.out.println(sb.toString());
    }
}
