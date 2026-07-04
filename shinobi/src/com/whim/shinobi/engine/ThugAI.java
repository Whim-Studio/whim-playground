package com.whim.shinobi.engine;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.domain.Aabb;
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

    // Projectile geometry / lifespan (backstop; off-screen culling also applies).
    static final int PW = 14;
    static final int PH = 8;
    static final int SHOT_LIFE = 180;

    private ThugAI() {}

    static void update(Enemy e, WorldState w) {
        if (!e.alive()) return;
        Player p = w.playerEntity();
        e.tickAiTimer();

        boolean samePlane = p.alive() && p.plane() == e.plane();
        double dx = samePlane ? Math.abs(p.box().centerX() - e.box().centerX()) : Double.MAX_VALUE;
        e.setAggro(samePlane && dx <= SIGHT_X);

        if (e.aggro()) {
            // Face and engage the player from range.
            faceToward(e, p.box().centerX());
            e.setVx(0);
            e.setState(Enums.EntityState.IDLE);
            if (dx <= SHOOT_RANGE && e.aiTimer() <= 0) {
                shoot(e, w);
                e.setAiTimer(SHOOT_COOLDOWN);
            }
        } else {
            patrol(e);
        }
    }

    private static void patrol(Enemy e) {
        double dir = (e.facing() == Enums.Facing.RIGHT) ? 1.0 : -1.0;
        if (e.box().x() <= e.patrolMinX()) { dir = 1.0; e.setFacing(Enums.Facing.RIGHT); }
        else if (e.box().x() >= e.patrolMaxX()) { dir = -1.0; e.setFacing(Enums.Facing.LEFT); }
        e.setVx(dir * (Config.MOVE_SPEED * 0.45));
        e.setState(Enums.EntityState.WALK);
    }

    private static void faceToward(Enemy e, double targetCx) {
        e.setFacing((targetCx >= e.box().centerX()) ? Enums.Facing.RIGHT : Enums.Facing.LEFT);
    }

    private static void shoot(Enemy e, WorldState w) {
        double dir = (e.facing() == Enums.Facing.RIGHT) ? 1.0 : -1.0;
        double px = (dir > 0) ? e.box().right() : e.box().x() - PW;
        double py = e.box().y() + e.box().h() * 0.35;
        Projectile pr = new Projectile(new Aabb(px, py, PW, PH), e.plane(),
                false, Enums.Weapon.SHURIKEN, SHOT_LIFE);
        pr.setFacing(e.facing());
        pr.setState(Enums.EntityState.ATTACK);
        pr.setVx(dir * SHOT_SPEED);
        pr.setVy(0);
        w.projectileList().add(pr);
        e.setState(Enums.EntityState.ATTACK);
    }
}
