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
 * Per-enemy state lives on the {@link Enemy} fields — {@code aiTimer} is the strike
 * cooldown, {@code attackTimer} is the block-duration countdown. Blocking cadence
 * uses the shared, once-seeded {@link Random} from the engine for determinism.
 */
final class NinjaAI {
    static final double SIGHT_X = 320.0;
    static final int STRIKE_COOLDOWN = 42;
    static final int BLOCK_DURATION = 34;
    static final int BLOCK_CHANCE_DENOM = 90; // ~1-in-N per tick to start a block

    private NinjaAI() {}

    static void update(Enemy e, WorldState w, Random rnd) {
        if (!e.alive()) return;
        Player p = w.playerEntity();
        e.tickAiTimer();
        if (e.attackTimer() > 0) {
            e.tickAttackTimer();
            if (e.attackTimer() == 0) e.setBlocking(false);
        }

        boolean samePlane = p.alive() && p.plane() == e.plane();
        double dx = samePlane ? Math.abs(p.box().centerX() - e.box().centerX()) : Double.MAX_VALUE;
        e.setAggro(samePlane && dx <= SIGHT_X);

        if (!e.aggro()) {
            e.setBlocking(false);
            patrol(e);
            return;
        }

        // Always face the player when engaged.
        e.setFacing((p.box().centerX() >= e.box().centerX()) ? Enums.Facing.RIGHT : Enums.Facing.LEFT);
        double dist = Physics.distance(p.box(), e.box());

        if (dist <= Config.MELEE_RANGE) {
            // In melee range: hold ground, guard, and strike on cadence.
            e.setVx(0);
            maybeBlock(e, rnd);
            if (!e.blocking()) {
                e.setState(Enums.EntityState.IDLE);
                if (e.aiTimer() <= 0) {
                    strike(w, e);
                    e.setAiTimer(STRIKE_COOLDOWN);
                }
            }
        } else {
            // Advance toward the player unless mid-block.
            maybeBlock(e, rnd);
            if (e.blocking()) {
                e.setVx(0);
            } else {
                double dir = (e.facing() == Enums.Facing.RIGHT) ? 1.0 : -1.0;
                e.setVx(dir * (Config.MOVE_SPEED * 0.8));
                e.setState(Enums.EntityState.WALK);
            }
        }
    }

    private static void maybeBlock(Enemy e, Random rnd) {
        if (e.blocking()) {
            e.setState(Enums.EntityState.BLOCK);
            return;
        }
        if (e.aiTimer() <= 0 && rnd.nextInt(BLOCK_CHANCE_DENOM) == 0) {
            e.setBlocking(true);
            e.setAttackTimer(BLOCK_DURATION);
            e.setState(Enums.EntityState.BLOCK);
        }
    }

    private static void patrol(Enemy e) {
        double dir = (e.facing() == Enums.Facing.RIGHT) ? 1.0 : -1.0;
        if (e.box().x() <= e.patrolMinX()) { dir = 1.0; e.setFacing(Enums.Facing.RIGHT); }
        else if (e.box().x() >= e.patrolMaxX()) { dir = -1.0; e.setFacing(Enums.Facing.LEFT); }
        e.setVx(dir * (Config.MOVE_SPEED * 0.5));
        e.setState(Enums.EntityState.WALK);
    }

    private static void strike(WorldState w, Enemy e) {
        e.setState(Enums.EntityState.ATTACK);
        // Melee strike: damage the player if still adjacent and vulnerable.
        Player p = w.playerEntity();
        if (p.alive() && p.plane() == e.plane() && p.invulnTimer() <= 0
                && Physics.distance(p.box(), e.box()) <= Config.MELEE_RANGE + 6.0) {
            CombatSystem.hitPlayer(w);
        }
    }
}
