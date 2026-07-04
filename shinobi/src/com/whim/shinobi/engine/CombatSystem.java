package com.whim.shinobi.engine;

import java.util.Iterator;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.domain.Aabb;
import com.whim.shinobi.domain.Enemy;
import com.whim.shinobi.domain.Player;
import com.whim.shinobi.domain.Projectile;
import com.whim.shinobi.domain.WorldState;

/**
 * The context-sensitive attack rule and all hit resolution. No Swing, no timing —
 * pure per-tick state mutation driven by {@link GameEngine}.
 *
 * Proximity rule (contract): on attack, measure pixel distance to the CLOSEST
 * same-plane living enemy. {@code <= Config.MELEE_RANGE} → melee swing (arc in
 * front of the player); otherwise throw the current {@link Enums.Weapon} as a
 * {@link Projectile}.
 */
final class CombatSystem {

    private static final int MELEE_RECOVERY_TICKS = 12;
    private static final int KILL_POINTS_THUG = 100;
    private static final int KILL_POINTS_NINJA = 200;

    // Projectile geometry / lifespan (backstop; off-screen culling also applies).
    private static final int PW = 14;
    private static final int PH = 8;
    private static final int SHOT_LIFE = 180;

    private CombatSystem() {}

    /** Resolve a player attack request. Returns true if an attack was performed. */
    static boolean playerAttack(WorldState w) {
        Player p = w.playerEntity();
        if (!p.alive() || p.attackCooldown() > 0) {
            return false;
        }

        Enemy closest = closestSamePlaneEnemy(w, p);
        double dist = (closest == null) ? Double.MAX_VALUE : Physics.distance(p.box(), closest.box());

        if (closest != null && dist <= Config.MELEE_RANGE) {
            meleeSwing(w, p);
            p.setLastAttack(Enums.AttackMode.MELEE);
            p.setState(Enums.EntityState.ATTACK);
            p.setAttackCooldown(MELEE_RECOVERY_TICKS);
        } else {
            throwWeapon(w, p);
            p.setLastAttack(Enums.AttackMode.PROJECTILE);
            p.setState(Enums.EntityState.ATTACK);
            p.setAttackCooldown(p.weapon().cooldownTicks());
        }
        return true;
    }

    /** Closest living enemy on the player's current plane, or null. */
    private static Enemy closestSamePlaneEnemy(WorldState w, Player p) {
        Enemy best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < w.enemyList().size(); i++) {
            Enemy e = w.enemyList().get(i);
            if (!e.alive() || e.plane() != p.plane()) continue;
            double d = Physics.distance(p.box(), e.box());
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }

    /** Instantaneous damage to living same-plane enemies in a short arc in front. */
    private static void meleeSwing(WorldState w, Player p) {
        boolean right = p.facing() == Enums.Facing.RIGHT;
        double reach = Config.MELEE_RANGE;
        double arcL = right ? p.box().centerX() : p.box().centerX() - reach;
        double arcR = right ? p.box().centerX() + reach : p.box().centerX();
        for (int i = 0; i < w.enemyList().size(); i++) {
            Enemy e = w.enemyList().get(i);
            if (!e.alive() || e.plane() != p.plane()) continue;
            // Vertical overlap + horizontal within the arc.
            boolean vOverlap = e.box().y() < p.box().bottom() && e.box().bottom() > p.box().y();
            boolean hInArc = e.box().right() >= arcL && e.box().x() <= arcR;
            if (vOverlap && hInArc) {
                // Melee ignores ninja block (a blade beats a raised guard up close).
                damageEnemy(w, e, 2);
            }
        }
    }

    /** Spawn a projectile of the current weapon travelling in the facing direction. */
    private static void throwWeapon(WorldState w, Player p) {
        double dir = (p.facing() == Enums.Facing.RIGHT) ? 1.0 : -1.0;
        double px = (dir > 0) ? p.box().right() : p.box().x() - PW;
        double py = p.box().y() + p.box().h() * 0.35;
        Projectile pr = new Projectile(new Aabb(px, py, PW, PH), p.plane(),
                true, p.weapon(), SHOT_LIFE);
        pr.setFacing(p.facing());
        pr.setState(Enums.EntityState.ATTACK);
        pr.setVx(dir * p.weapon().projectileSpeed());
        pr.setVy(0);
        w.projectileList().add(pr);
    }

    /**
     * Advance every projectile one tick and resolve collisions:
     * player projectile ↔ enemy (with ninja block), enemy projectile ↔ player.
     */
    static void updateProjectiles(WorldState w) {
        Iterator<Projectile> it = w.projectileList().iterator();
        while (it.hasNext()) {
            Projectile pr = it.next();
            pr.box().setX(pr.box().x() + pr.vx());
            pr.box().setY(pr.box().y() + pr.vy());
            boolean expired = pr.tickLifespan();

            boolean dead = !pr.alive() || expired
                    || pr.box().right() < 0 || pr.box().x() > w.levelWidth();

            if (!dead) {
                if (pr.fromPlayer()) {
                    dead = resolvePlayerProjectile(w, pr);
                } else {
                    dead = resolveEnemyProjectile(w, pr);
                }
            }
            if (dead) it.remove();
        }
    }

    private static boolean resolvePlayerProjectile(WorldState w, Projectile pr) {
        for (int i = 0; i < w.enemyList().size(); i++) {
            Enemy e = w.enemyList().get(i);
            if (!e.alive() || e.plane() != pr.plane()) continue;
            if (!pr.box().intersects(e.box())) continue;

            // Ninja can block a SHURIKEN when guarding and facing the incoming shot.
            if (e.type() == Enums.EnemyType.NINJA && e.blocking()
                    && pr.weapon() == Enums.Weapon.SHURIKEN && facingIncoming(e, pr)) {
                return true; // deflected: projectile consumed, no damage
            }
            damageEnemy(w, e, pr.weapon().damage());
            return true;
        }
        return false;
    }

    private static boolean resolveEnemyProjectile(WorldState w, Projectile pr) {
        Player p = w.playerEntity();
        if (!p.alive() || p.plane() != pr.plane()) return false;
        if (p.invulnTimer() > 0) return false;
        if (pr.box().intersects(p.box())) {
            hitPlayer(w);
            return true;
        }
        return false;
    }

    /** True if the enemy faces the direction the projectile is coming from. */
    private static boolean facingIncoming(Enemy e, Projectile pr) {
        boolean projFromLeft = pr.vx() > 0; // travelling right => came from the left
        return projFromLeft ? (e.facing() == Enums.Facing.LEFT) : (e.facing() == Enums.Facing.RIGHT);
    }

    static void damageEnemy(WorldState w, Enemy e, int dmg) {
        if (e.damage(dmg)) {
            e.kill();
            e.setBlocking(false);
            w.playerEntity().addScore((e.type() == Enums.EnemyType.NINJA) ? KILL_POINTS_NINJA : KILL_POINTS_THUG);
        }
    }

    /** Player takes a hit: lose a life, brief invulnerability, respawn feet on ground. */
    static void hitPlayer(WorldState w) {
        Player p = w.playerEntity();
        if (p.invulnTimer() > 0) return;
        p.loseLife();
        p.setInvulnTimer(90);
        if (p.lives() <= 0) {
            p.kill();
        } else {
            p.box().setY(Physics.groundY(p.plane()) - p.box().h());
            p.setVy(0);
            p.setGrounded(true);
        }
    }
}
