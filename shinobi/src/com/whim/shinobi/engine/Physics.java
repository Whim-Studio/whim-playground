package com.whim.shinobi.engine;

import java.util.List;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.domain.Aabb;
import com.whim.shinobi.domain.Entity;
import com.whim.shinobi.domain.Platform;

/**
 * AABB terrain physics, fully decoupled from Swing. Operates on {@link Entity}
 * public fields (position via {@code box}, velocity {@code vx/vy}, {@code grounded}).
 *
 * Coordinate convention (from {@link Config}): (x,y) is the top-left of the box;
 * a plane's {@code GROUND_Y_*} is the feet line, so a grounded entity has
 * {@code box.y = groundY - box.h}.
 */
final class Physics {
    private Physics() {}

    /** World Y of the ground (feet line) for a plane. */
    static double groundY(Enums.Plane plane) {
        return (plane == Enums.Plane.UPPER) ? Config.GROUND_Y_UPPER : Config.GROUND_Y_LOWER;
    }

    /** Apply an intended horizontal walk velocity (pixels/tick), clamped later. */
    static void setWalk(Entity e, double dir) {
        e.vx = dir * Config.MOVE_SPEED;
    }

    /** Begin a jump if grounded. */
    static void jump(Entity e) {
        if (e.grounded) {
            e.vy = Config.JUMP_VELOCITY;
            e.grounded = false;
        }
    }

    /**
     * Advance one entity by a single tick: integrate horizontal motion (clamped to
     * level bounds), apply gravity, then resolve vertical collisions against the
     * ground line and any solid {@link Platform} on the entity's current plane.
     */
    static void step(Entity e, List<Platform> platforms, int levelWidth) {
        // --- Horizontal ---
        e.box.x += e.vx;
        if (e.box.x < 0) e.box.x = 0;
        double maxX = levelWidth - e.box.w;
        if (e.box.x > maxX) e.box.x = maxX;

        // --- Vertical: gravity + integrate ---
        double groundLine = groundY(e.plane);
        e.vy += Config.GRAVITY;
        e.box.y += e.vy;

        // Feet cannot pass the plane's ground line.
        double feetLimitTop = groundLine - e.box.h;
        boolean landed = false;
        if (e.box.y >= feetLimitTop) {
            e.box.y = feetLimitTop;
            landed = true;
        }

        // Solid platforms on this plane act as one-way-ish ground when falling.
        if (e.vy >= 0) {
            for (int i = 0; i < platforms.size(); i++) {
                Platform p = platforms.get(i);
                if (p.plane != e.plane) continue;
                if (landsOnTop(e, p)) {
                    e.box.y = p.box.y - e.box.h;
                    landed = true;
                }
            }
        }

        if (landed) {
            if (e.vy > 0) e.vy = 0;
            e.grounded = true;
        } else {
            e.grounded = false;
        }
    }

    /** True if a falling entity's feet should rest on the top edge of a platform. */
    private static boolean landsOnTop(Entity e, Platform p) {
        double exL = e.box.x;
        double exR = e.box.x + e.box.w;
        double pxL = p.box.x;
        double pxR = p.box.x + p.box.w;
        if (exR <= pxL || exL >= pxR) return false;
        double feet = e.box.y + e.box.h;
        // Feet within a small band of the platform top while descending.
        return feet >= p.box.y && feet <= p.box.y + Math.max(e.vy, 0) + 6.0;
    }

    /**
     * Plane-shift: move an entity to the other plane and snap its feet to that
     * plane's ground line (classic Shinobi plane jump). Horizontal x is preserved.
     */
    static void shiftPlane(Entity e) {
        e.plane = (e.plane == Enums.Plane.LOWER) ? Enums.Plane.UPPER : Enums.Plane.LOWER;
        e.box.y = groundY(e.plane) - e.box.h;
        e.vy = 0;
        e.grounded = true;
    }

    /** Center-to-center pixel distance between two boxes. */
    static double distance(Aabb a, Aabb b) {
        double dx = a.cx() - b.cx();
        double dy = a.cy() - b.cy();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** Horizontal-only pixel distance between two box centers. */
    static double horizontalDistance(Aabb a, Aabb b) {
        return Math.abs(a.cx() - b.cx());
    }
}
