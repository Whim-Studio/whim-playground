package com.whim.shinobi.engine;

import java.util.List;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.domain.Aabb;
import com.whim.shinobi.domain.Entity;
import com.whim.shinobi.domain.Platform;

/**
 * AABB terrain physics, fully decoupled from Swing. Operates on {@link Entity}
 * via its accessor API (position via {@code box()}, velocity {@code vx()/vy()},
 * {@code grounded()}).
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
        e.setVx(dir * Config.MOVE_SPEED);
    }

    /** Begin a jump if grounded. */
    static void jump(Entity e) {
        if (e.grounded()) {
            e.setVy(Config.JUMP_VELOCITY);
            e.setGrounded(false);
        }
    }

    /**
     * Advance one entity by a single tick: integrate horizontal motion (clamped to
     * level bounds), apply gravity, then resolve vertical collisions against the
     * ground line and any solid {@link Platform} on the entity's current plane.
     */
    static void step(Entity e, List<Platform> platforms, int levelWidth) {
        Aabb box = e.box();

        // --- Horizontal ---
        box.setX(box.x() + e.vx());
        if (box.x() < 0) box.setX(0);
        double maxX = levelWidth - box.w();
        if (box.x() > maxX) box.setX(maxX);

        // --- Vertical: gravity + integrate ---
        double groundLine = groundY(e.plane());
        e.setVy(e.vy() + Config.GRAVITY);
        box.setY(box.y() + e.vy());

        // Feet cannot pass the plane's ground line.
        double feetLimitTop = groundLine - box.h();
        boolean landed = false;
        if (box.y() >= feetLimitTop) {
            box.setY(feetLimitTop);
            landed = true;
        }

        // Solid platforms on this plane act as one-way-ish ground when falling.
        if (e.vy() >= 0) {
            for (int i = 0; i < platforms.size(); i++) {
                Platform p = platforms.get(i);
                if (p.plane() != e.plane()) continue;
                if (landsOnTop(e, p)) {
                    box.setY(p.box().y() - box.h());
                    landed = true;
                }
            }
        }

        if (landed) {
            if (e.vy() > 0) e.setVy(0);
            e.setGrounded(true);
        } else {
            e.setGrounded(false);
        }
    }

    /** True if a falling entity's feet should rest on the top edge of a platform. */
    private static boolean landsOnTop(Entity e, Platform p) {
        Aabb box = e.box();
        Aabb pb = p.box();
        double exL = box.x();
        double exR = box.x() + box.w();
        double pxL = pb.x();
        double pxR = pb.x() + pb.w();
        if (exR <= pxL || exL >= pxR) return false;
        double feet = box.y() + box.h();
        // Feet within a small band of the platform top while descending.
        return feet >= pb.y() && feet <= pb.y() + Math.max(e.vy(), 0) + 6.0;
    }

    /**
     * Plane-shift: move an entity to the other plane and snap its feet to that
     * plane's ground line (classic Shinobi plane jump). Horizontal x is preserved.
     */
    static void shiftPlane(Entity e) {
        Enums.Plane target = (e.plane() == Enums.Plane.LOWER) ? Enums.Plane.UPPER : Enums.Plane.LOWER;
        e.setPlane(target);
        e.box().setY(groundY(target) - e.box().h());
        e.setVy(0);
        e.setGrounded(true);
    }

    /** Center-to-center pixel distance between two boxes. */
    static double distance(Aabb a, Aabb b) {
        double dx = a.centerX() - b.centerX();
        double dy = a.centerY() - b.centerY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** Horizontal-only pixel distance between two box centers. */
    static double horizontalDistance(Aabb a, Aabb b) {
        return Math.abs(a.centerX() - b.centerX());
    }
}
