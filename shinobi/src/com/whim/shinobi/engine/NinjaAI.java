package com.whim.shinobi.engine;

import java.util.Random;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.domain.Enemy;
import com.whim.shinobi.domain.Player;
import com.whim.shinobi.domain.WorldState;

/**
 * NINJA behaviour: patrol until the player appears on the SAME plane, then advance
 * to melee range and periodically raise a block (can deflect the player's shuriken,
 * see {@link CombatSystem}). Strikes the player on a cadence when adjacent.
 *
 * Per-enemy state lives on the {@link Enemy} fields; blocking cadence uses the
 * shared, once-seeded {@link Random} from the engine for determinism.
 */
final class NinjaAI {
    static final double SIGHT_X = 320.0;
    static final int STRIKE_COOLDOWN = 42;
    static final int BLOCK_DURATION = 34;
    static final int BLOCK_CHANCE_DENOM = 90; // ~1-in-N per tick to start a block

    private NinjaAI() {}

    static void update(Enemy e, WorldState w, Random rnd) {
        if (!e.alive) return;
        Player p = w.player;
        if (e.actTimer > 0) e.actTimer--;
        if (e.blockTimer > 0) {
            e.blockTimer--;
            if (e.blockTimer == 0) e.blocking = false;
        }

        boolean samePlane = p.alive && p.plane == e.plane;
        double dx = samePlane ? Math.abs(p.box.cx() - e.box.cx()) : Double.MAX_VALUE;
        e.aggro = samePlane && dx <= SIGHT_X;

        if (!e.aggro) {
            e.blocking = false;
            patrol(e);
            return;
        }

        // Always face the player when engaged.
        e.facing = (p.box.cx() >= e.box.cx()) ? Enums.Facing.RIGHT : Enums.Facing.LEFT;
        double dist = Physics.distance(p.box, e.box);

        if (dist <= Config.MELEE_RANGE) {
            // In melee range: hold ground, guard, and strike on cadence.
            e.vx = 0;
            maybeBlock(e, rnd);
            if (!e.blocking) {
                e.state = Enums.EntityState.IDLE;
                if (e.actTimer <= 0) {
                    strike(w, e);
                    e.actTimer = STRIKE_COOLDOWN;
                }
            }
        } else {
            // Advance toward the player unless mid-block.
            maybeBlock(e, rnd);
            if (e.blocking) {
                e.vx = 0;
            } else {
                double dir = (e.facing == Enums.Facing.RIGHT) ? 1.0 : -1.0;
                e.vx = dir * (Config.MOVE_SPEED * 0.8);
                e.state = Enums.EntityState.WALK;
            }
        }
    }

    private static void maybeBlock(Enemy e, Random rnd) {
        if (e.blocking) {
            e.state = Enums.EntityState.BLOCK;
            return;
        }
        if (e.actTimer <= 0 && rnd.nextInt(BLOCK_CHANCE_DENOM) == 0) {
            e.blocking = true;
            e.blockTimer = BLOCK_DURATION;
            e.state = Enums.EntityState.BLOCK;
        }
    }

    private static void patrol(Enemy e) {
        double dir = (e.facing == Enums.Facing.RIGHT) ? 1.0 : -1.0;
        if (e.box.x <= e.patrolMinX) { dir = 1.0; e.facing = Enums.Facing.RIGHT; }
        else if (e.box.x >= e.patrolMaxX) { dir = -1.0; e.facing = Enums.Facing.LEFT; }
        e.vx = dir * (Config.MOVE_SPEED * 0.5);
        e.state = Enums.EntityState.WALK;
    }

    private static void strike(WorldState w, Enemy e) {
        e.state = Enums.EntityState.ATTACK;
        // Melee strike: damage the player if still adjacent and vulnerable.
        Player p = w.player;
        if (p.alive && p.plane == e.plane && p.invuln <= 0
                && Physics.distance(p.box, e.box) <= Config.MELEE_RANGE + 6.0) {
            CombatSystem.hitPlayer(w);
        }
    }
}
