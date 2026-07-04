package com.whim.shinobi.engine;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.domain.Enemy;
import com.whim.shinobi.domain.Player;
import com.whim.shinobi.domain.Projectile;
import com.whim.shinobi.domain.WorldState;

/**
 * THUG behaviour: patrol a fixed range; when the player appears on the SAME plane
 * within sight, stop, face the player, and shoot projectiles from range. Loses
 * interest when the player leaves the plane or walks out of sight.
 *
 * Per-enemy state lives on the {@link Enemy} fields (no external maps).
 */
final class ThugAI {
    static final double SIGHT_X = 300.0;
    static final double SHOOT_RANGE = 280.0;
    static final int SHOOT_COOLDOWN = 78;
    static final double SHOT_SPEED = 5.0;

    private ThugAI() {}

    static void update(Enemy e, WorldState w) {
        if (!e.alive) return;
        Player p = w.player;
        if (e.actTimer > 0) e.actTimer--;

        boolean samePlane = p.alive && p.plane == e.plane;
        double dx = samePlane ? Math.abs(p.box.cx() - e.box.cx()) : Double.MAX_VALUE;
        e.aggro = samePlane && dx <= SIGHT_X;

        if (e.aggro) {
            // Face and engage the player from range.
            faceToward(e, p.box.cx());
            e.vx = 0;
            e.state = Enums.EntityState.IDLE;
            if (dx <= SHOOT_RANGE && e.actTimer <= 0) {
                shoot(e, w);
                e.actTimer = SHOOT_COOLDOWN;
            }
        } else {
            patrol(e);
        }
    }

    private static void patrol(Enemy e) {
        double dir = (e.facing == Enums.Facing.RIGHT) ? 1.0 : -1.0;
        if (e.box.x <= e.patrolMinX) { dir = 1.0; e.facing = Enums.Facing.RIGHT; }
        else if (e.box.x >= e.patrolMaxX) { dir = -1.0; e.facing = Enums.Facing.LEFT; }
        e.vx = dir * (Config.MOVE_SPEED * 0.45);
        e.state = Enums.EntityState.WALK;
    }

    private static void faceToward(Enemy e, double targetCx) {
        e.facing = (targetCx >= e.box.cx()) ? Enums.Facing.RIGHT : Enums.Facing.LEFT;
    }

    private static void shoot(Enemy e, WorldState w) {
        Projectile pr = new Projectile();
        pr.fromPlayer = false;
        pr.weapon = Enums.Weapon.SHURIKEN;
        pr.damage = 1;
        pr.plane = e.plane;
        pr.facing = e.facing;
        pr.alive = true;
        pr.state = Enums.EntityState.ATTACK;
        double dir = (e.facing == Enums.Facing.RIGHT) ? 1.0 : -1.0;
        pr.vx = dir * SHOT_SPEED;
        pr.vy = 0;
        pr.box.x = (dir > 0) ? e.box.right() : e.box.x - pr.box.w;
        pr.box.y = e.box.y + e.box.h * 0.35;
        w.projectiles.add(pr);
        e.state = Enums.EntityState.ATTACK;
    }
}
