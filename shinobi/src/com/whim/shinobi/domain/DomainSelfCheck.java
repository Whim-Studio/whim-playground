package com.whim.shinobi.domain;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/**
 * Standalone smoke test for the domain layer — no engine/UI needed. Builds the
 * first level, prints counts, and asserts a few invariants so we can verify the
 * data is playable before Task 2/3 land. Run: {@code java ... DomainSelfCheck}.
 */
public final class DomainSelfCheck {
    private DomainSelfCheck() {}

    public static void main(String[] args) {
        WorldState world = LevelBuilder.firstLevel();
        Views.GameStateView view = world;

        int platforms = world.platformList().size();
        int hostages = world.hostageList().size();
        int enemies = world.enemyList().size();

        System.out.println("=== Shinobi domain self-check ===");
        System.out.println("level width      : " + view.levelWidth());
        System.out.println("platforms        : " + platforms);
        System.out.println("hostages         : " + hostages);
        System.out.println("enemies          : " + enemies);
        System.out.println("phase            : " + view.phase());
        System.out.println("seconds remaining: " + view.secondsRemaining());
        System.out.println("player weapon    : " + view.player().weapon());
        System.out.println("player lives     : " + view.player().lives());
        System.out.println("player ninjutsu  : " + view.player().ninjutsu());

        int thugs = 0, ninjas = 0;
        for (int i = 0; i < world.enemyList().size(); i++) {
            if (world.enemyList().get(i).type() == Enums.EnemyType.NINJA) ninjas++; else thugs++;
        }
        System.out.println("enemy mix        : " + thugs + " THUG / " + ninjas + " NINJA");

        // --- Invariants ---
        require(view.levelWidth() == Config.LEVEL_W, "level width == Config.LEVEL_W");
        require(platforms >= 2, "at least one lower ground + one upper ledge");
        require(hostages >= 3 && hostages <= 5, "3-5 hostages");
        require(enemies >= 2, "at least a couple of enemies");
        require(thugs > 0 && ninjas > 0, "mix of THUG and NINJA");
        require(view.hostagesTotal() == hostages, "hostagesTotal matches list");

        // Player grounded on its plane.
        Views.PlayerView p = view.player();
        double feet = p.y() + p.h();
        double ground = world.map().groundYFor(Enums.Plane.LOWER, p.x() + p.w() * 0.5);
        require(Math.abs(feet - ground) < 0.001, "player feet rest on LOWER ground");

        // Every enemy grounded on its plane.
        for (int i = 0; i < world.enemyList().size(); i++) {
            Enemy e = world.enemyList().get(i);
            double ef = e.y() + e.h();
            double eg = world.map().groundYFor(e.plane(), e.x() + e.w() * 0.5);
            require(Math.abs(ef - eg) < 0.001, "enemy " + i + " grounded on " + e.plane());
        }

        // Continuous lower ground spans the whole level at both ends.
        require(world.map().groundYFor(Enums.Plane.LOWER, 0) == Config.GROUND_Y_LOWER,
                "lower ground at x=0");
        require(world.map().groundYFor(Enums.Plane.LOWER, Config.LEVEL_W - 1) == Config.GROUND_Y_LOWER,
                "lower ground at far edge");

        // Aabb geometry sanity.
        Aabb a = new Aabb(0, 0, 10, 10);
        Aabb b = new Aabb(5, 5, 10, 10);
        require(a.intersects(b), "overlapping boxes intersect");
        require(a.contains(5, 5), "box contains interior point");
        require(Math.abs(a.centerX() - 5) < 0.001, "centerX");

        System.out.println("\nALL INVARIANTS PASSED");
    }

    private static void require(boolean cond, String what) {
        if (!cond) throw new AssertionError("FAILED invariant: " + what);
        System.out.println("  ok: " + what);
    }
}
