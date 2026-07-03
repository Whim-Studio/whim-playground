package com.whim.powermonger.domain;

import java.util.List;

import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Views;

/**
 * Standalone runnable summary of a generated world, so the domain package can be
 * exercised without the engine or UI. Prints map size, town count, captain
 * positions and the balance of power.
 */
public final class DomainSelfCheck {

    private DomainSelfCheck() {}

    public static void main(String[] args) {
        long seed = args.length > 0 ? parseSeed(args[0]) : 42L;
        WorldState world = WorldGenerator.generate(seed);
        Views.GameStateView s = world.snapshot();

        System.out.println("=== Powermonger Domain Self-Check ===");
        System.out.println("Seed          : " + seed);
        System.out.println("Map size      : " + s.mapWidth() + " x " + s.mapHeight()
                + " (maxElevation " + s.maxElevation() + ")");
        System.out.println("Season/Weather: " + s.season() + " / " + s.weather()
                + "  movementFactor=" + s.movementFactor());

        int[] terrainCounts = new int[com.whim.powermonger.api.Enums.TerrainType.values().length];
        int trees = 0;
        for (int x = 0; x < s.mapWidth(); x++) {
            for (int y = 0; y < s.mapHeight(); y++) {
                Views.TileView t = s.tile(x, y);
                terrainCounts[t.terrain().ordinal()]++;
                if (t.hasTrees()) trees++;
            }
        }
        System.out.println("Tree tiles    : " + trees);
        System.out.print("Terrain       : ");
        com.whim.powermonger.api.Enums.TerrainType[] tt =
                com.whim.powermonger.api.Enums.TerrainType.values();
        for (int i = 0; i < tt.length; i++) {
            System.out.print(tt[i] + "=" + terrainCounts[i] + (i < tt.length - 1 ? ", " : ""));
        }
        System.out.println();

        List<Views.TownView> towns = s.towns();
        System.out.println("Towns         : " + towns.size());
        for (int i = 0; i < towns.size(); i++) {
            Views.TownView t = towns.get(i);
            System.out.println("   [" + t.id() + "] " + t.name() + " @(" + t.tileX() + ","
                    + t.tileY() + ") pop=" + t.population() + " " + t.allegiance());
        }

        System.out.println("Townspeople   : " + s.townspeople().size());

        List<Views.CaptainView> caps = s.captains();
        int players = 0, enemies = 0;
        for (int i = 0; i < caps.size(); i++) {
            if (caps.get(i).allegiance() == Allegiance.PLAYER) players++;
            else if (caps.get(i).allegiance() == Allegiance.ENEMY) enemies++;
        }
        System.out.println("Captains      : " + caps.size()
                + " (player=" + players + ", enemy=" + enemies + ")");
        for (int i = 0; i < caps.size(); i++) {
            Views.CaptainView c = caps.get(i);
            System.out.printf("   [%d] %-9s %-7s @(%.1f,%.1f) str=%d food=%d %s%n",
                    c.id(), c.name(), c.allegiance(), c.x(), c.y(),
                    c.strength(), c.food(),
                    c.supremeCommander() ? "<SUPREME>" : "");
        }

        System.out.println("Balance       : " + s.balanceOfPower());
        System.out.println("Status        : " + s.statusMessage());

        // Exercise a couple of helpers so the summary reflects real behaviour.
        MapGrid grid = world.grid();
        int deforested = 0;
        for (int x = 0; x < grid.width() && deforested < 3; x++) {
            for (int y = 0; y < grid.height() && deforested < 3; y++) {
                if (grid.deforest(x, y)) deforested++;
            }
        }
        world.balance().onTownCaptured(true);
        System.out.println("Deforested    : " + deforested + " tile(s) (helper check)");
        System.out.println("Balance after capture nudge: " + world.balance().value());
        System.out.println("=== OK ===");
    }

    private static long parseSeed(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return s.hashCode(); }
    }
}
