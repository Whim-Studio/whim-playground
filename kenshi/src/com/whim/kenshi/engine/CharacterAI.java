package com.whim.kenshi.engine;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.AiState;
import com.whim.kenshi.api.Enums.BodyPart;
import com.whim.kenshi.api.Enums.FactionId;
import com.whim.kenshi.api.Enums.MoveState;
import com.whim.kenshi.api.Enums.OrderType;
import com.whim.kenshi.domain.Character;
import com.whim.kenshi.domain.FactionMatrix;
import com.whim.kenshi.domain.WorldNode;
import com.whim.kenshi.domain.WorldState;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Per-character behaviour state machine over {@link AiState}. Decides — but does
 * not execute — movement and combat each step:
 * <ul>
 *   <li>Bandits: WANDER, PURSUE/ATTACK hostiles inside aggro range, FLEE when
 *       badly hurt.</li>
 *   <li>Guards (Holy Nation / Shek / Trade Guild): PATROL their home, PURSUE and
 *       ATTACK hostiles that come near.</li>
 *   <li>Drifters: WANDER; FLEE when injured.</li>
 *   <li>Player units: follow issued orders; when idle, auto-attack a hostile in
 *       aggro range.</li>
 * </ul>
 * Output is written to the character ({@link Character#setAiState}) and its
 * {@link AiMemory} (movement goal + {@code combatTargetId}).
 */
final class CharacterAI {

    static final double AGGRO_RANGE = 240.0;
    static final double PLAYER_AUTO_ATTACK_RANGE = 120.0;
    /** Bandits/drifters give up a chase past this range from their home. */
    static final double LEASH_RANGE = 900.0;
    static final double WANDER_RADIUS = 260.0;
    static final double WANDER_MIN_INTERVAL = 18.0; // world-seconds
    static final double WANDER_MAX_INTERVAL = 42.0;

    private final FactionMatrix factions;
    private final Random rng;

    CharacterAI(FactionMatrix factions, Random rng) {
        this.factions = factions;
        this.rng = rng;
    }

    /** True for members of factions that behave as sedentary town guards. */
    private static boolean isGuard(FactionId f) {
        return f == FactionId.HOLY_NATION || f == FactionId.SHEK || f == FactionId.TRADE_GUILD;
    }

    private static boolean isBandit(FactionId f) {
        return f == FactionId.DUST_BANDITS || f == FactionId.HUNGRY_BANDITS;
    }

    private static double dist2(Character a, Character b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return dx * dx + dy * dy;
    }

    private boolean badlyHurt(Character c) {
        if (c.blood() < 45.0) {
            return true;
        }
        if (c.anatomy().bothLegsDown()) {
            return true;
        }
        int disabled = 0;
        BodyPart[] parts = BodyPart.values();
        for (int i = 0; i < parts.length; i++) {
            if (c.anatomy().disabled(parts[i])) {
                disabled++;
            }
        }
        return disabled >= 3;
    }

    /** Nearest live hostile to {@code c} within {@code range}, or null. */
    private Character nearestHostile(Character c, List<Character> all, double range) {
        double best = range * range;
        Character found = null;
        for (int i = 0; i < all.size(); i++) {
            Character o = all.get(i);
            if (o == c) {
                continue;
            }
            MoveState ms = o.moveState();
            if (ms == MoveState.DEAD || ms == MoveState.DOWNED) {
                continue;
            }
            if (!factions.isHostile(c.faction(), o.faction())) {
                continue;
            }
            double d2 = dist2(c, o);
            if (d2 <= best) {
                best = d2;
                found = o;
            }
        }
        return found;
    }

    private static Character byId(List<Character> all, String id) {
        if (id == null) {
            return null;
        }
        for (int i = 0; i < all.size(); i++) {
            if (id.equals(all.get(i).id())) {
                return all.get(i);
            }
        }
        return null;
    }

    /**
     * Decide the character's behaviour for this step.
     *
     * @param memories per-character scratch state (keyed by id).
     */
    void decide(Character c, List<Character> all, WorldState world,
                Map<String, AiMemory> memories, double dtWorld) {
        MoveState ms = c.moveState();
        if (ms == MoveState.DEAD || ms == MoveState.DOWNED) {
            AiMemory downedMem = memories.get(c.id());
            if (downedMem != null) {
                downedMem.combatTargetId = null;
                downedMem.clearGoal();
            }
            c.setAiState(AiState.IDLE);
            return;
        }

        AiMemory mem = memories.get(c.id());
        if (mem == null) {
            mem = new AiMemory(c.x(), c.y());
            memories.put(c.id(), mem);
        }

        if (c.faction() == FactionId.PLAYER) {
            decidePlayer(c, all, world, mem);
        } else if (isBandit(c.faction())) {
            decideAggressive(c, all, mem, dtWorld, true);
        } else if (isGuard(c.faction())) {
            decideGuard(c, all, mem, dtWorld);
        } else {
            // Drifters and anyone else: skittish wanderers.
            decideAggressive(c, all, mem, dtWorld, false);
        }
    }

    // --- Player: obey orders, auto-attack when idle ------------------------
    private void decidePlayer(Character c, List<Character> all, WorldState world, AiMemory mem) {
        OrderType order = c.orderType();

        if (order == OrderType.ATTACK) {
            Character target = byId(all, c.targetId());
            if (target == null || target.moveState() == MoveState.DEAD
                    || target.moveState() == MoveState.DOWNED) {
                // Target gone: clear the order and fall back to idle behaviour.
                c.clearOrder();
                mem.combatTargetId = null;
                mem.clearGoal();
                c.setAiState(AiState.IDLE);
                return;
            }
            mem.combatTargetId = target.id();
            if (inMelee(c, target)) {
                c.setAiState(AiState.ATTACK);
                mem.clearGoal();
            } else {
                c.setAiState(AiState.PURSUE);
                setGoal(mem, target.x(), target.y());
            }
            return;
        }

        if (order == OrderType.MOVE || order == OrderType.INTERACT) {
            mem.combatTargetId = null;
            double gx = c.targetX();
            double gy = c.targetY();
            double reach = Config.CHAR_RADIUS;
            if (order == OrderType.INTERACT) {
                // orderInteract stores only the node id; resolve its position.
                WorldNode node = world.node(c.nodeId());
                if (node != null) {
                    gx = node.x();
                    gy = node.y();
                    reach = Math.max(Config.CHAR_RADIUS, node.radius());
                }
            }
            if (near(c, gx, gy, reach)) {
                c.clearOrder();
                mem.clearGoal();
                c.setAiState(AiState.IDLE);
            } else {
                // moveState()==MOVING (live MOVE order) plus the Snapshot moving
                // override convey the motion. Tag the AI intent as RETURN so the
                // HUD shows a deliberate march to an ordered point.
                c.setAiState(order == OrderType.INTERACT ? AiState.LOOT : AiState.RETURN);
                setGoal(mem, gx, gy);
            }
            return;
        }

        // No order: hold position, but auto-engage a hostile that wanders close.
        Character threat = nearestHostile(c, all, PLAYER_AUTO_ATTACK_RANGE);
        if (threat != null) {
            mem.combatTargetId = threat.id();
            if (inMelee(c, threat)) {
                c.setAiState(AiState.ATTACK);
                mem.clearGoal();
            } else {
                c.setAiState(AiState.PURSUE);
                setGoal(mem, threat.x(), threat.y());
            }
        } else {
            mem.combatTargetId = null;
            mem.clearGoal();
            c.setAiState(AiState.IDLE);
        }
    }

    // --- Bandits / drifters ------------------------------------------------
    private void decideAggressive(Character c, List<Character> all, AiMemory mem,
                                  double dtWorld, boolean predatory) {
        if (badlyHurt(c)) {
            flee(c, all, mem);
            return;
        }

        Character prey = predatory ? nearestHostile(c, all, AGGRO_RANGE) : null;
        // Non-predators (drifters) only fight what is basically on top of them.
        if (!predatory) {
            prey = nearestHostile(c, all, AGGRO_RANGE * 0.4);
        }

        if (prey != null && withinLeash(c, mem, prey)) {
            mem.combatTargetId = prey.id();
            if (inMelee(c, prey)) {
                c.setAiState(AiState.ATTACK);
                mem.clearGoal();
            } else {
                c.setAiState(AiState.PURSUE);
                setGoal(mem, prey.x(), prey.y());
            }
            return;
        }

        mem.combatTargetId = null;
        wander(c, mem, dtWorld);
    }

    // --- Guards ------------------------------------------------------------
    private void decideGuard(Character c, List<Character> all, AiMemory mem, double dtWorld) {
        if (badlyHurt(c)) {
            flee(c, all, mem);
            return;
        }
        Character intruder = nearestHostile(c, all, AGGRO_RANGE);
        if (intruder != null) {
            mem.combatTargetId = intruder.id();
            if (inMelee(c, intruder)) {
                c.setAiState(AiState.ATTACK);
                mem.clearGoal();
            } else {
                c.setAiState(AiState.PURSUE);
                setGoal(mem, intruder.x(), intruder.y());
            }
            return;
        }
        mem.combatTargetId = null;
        patrol(c, mem);
    }

    // --- Shared behaviours -------------------------------------------------
    private void flee(Character c, List<Character> all, AiMemory mem) {
        c.setAiState(AiState.FLEE);
        mem.combatTargetId = null;
        Character threat = nearestHostile(c, all, AGGRO_RANGE * 1.5);
        if (threat != null) {
            double dx = c.x() - threat.x();
            double dy = c.y() - threat.y();
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 1e-6) {
                dx = 1; dy = 0; len = 1;
            }
            double gx = c.x() + dx / len * 200.0;
            double gy = c.y() + dy / len * 200.0;
            setGoal(mem, clampWorld(gx), clampWorld(gy));
        } else {
            // No visible threat: limp toward home.
            setGoal(mem, mem.homeX, mem.homeY);
        }
    }

    private void wander(Character c, AiMemory mem, double dtWorld) {
        c.setAiState(AiState.WANDER);
        mem.wanderTimer -= dtWorld;
        if (!mem.hasGoal || mem.wanderTimer <= 0.0 || arrived(c, mem.goalX, mem.goalY)) {
            double ang = rng.nextDouble() * Math.PI * 2.0;
            double r = WANDER_RADIUS * (0.3 + 0.7 * rng.nextDouble());
            double gx = clampWorld(mem.homeX + Math.cos(ang) * r);
            double gy = clampWorld(mem.homeY + Math.sin(ang) * r);
            setGoal(mem, gx, gy);
            mem.wanderTimer = WANDER_MIN_INTERVAL
                    + rng.nextDouble() * (WANDER_MAX_INTERVAL - WANDER_MIN_INTERVAL);
        }
    }

    private void patrol(Character c, AiMemory mem) {
        c.setAiState(AiState.PATROL);
        if (mem.patrol == null) {
            mem.patrol = buildPatrol(mem.homeX, mem.homeY);
            mem.patrolIndex = 0;
        }
        double[] wp = mem.patrol[mem.patrolIndex];
        if (arrived(c, wp[0], wp[1])) {
            mem.patrolIndex = (mem.patrolIndex + 1) % mem.patrol.length;
            wp = mem.patrol[mem.patrolIndex];
        }
        setGoal(mem, wp[0], wp[1]);
    }

    private static double[][] buildPatrol(double hx, double hy) {
        double r = 140.0;
        double[][] p = new double[4][2];
        p[0][0] = clampWorld(hx + r); p[0][1] = clampWorld(hy);
        p[1][0] = clampWorld(hx);     p[1][1] = clampWorld(hy + r);
        p[2][0] = clampWorld(hx - r); p[2][1] = clampWorld(hy);
        p[3][0] = clampWorld(hx);     p[3][1] = clampWorld(hy - r);
        return p;
    }

    private boolean withinLeash(Character c, AiMemory mem, Character prey) {
        double dx = prey.x() - mem.homeX;
        double dy = prey.y() - mem.homeY;
        return dx * dx + dy * dy <= LEASH_RANGE * LEASH_RANGE;
    }

    private static void setGoal(AiMemory mem, double gx, double gy) {
        mem.goalX = gx;
        mem.goalY = gy;
        mem.hasGoal = true;
    }

    static boolean inMelee(Character a, Character b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double reach = Config.MELEE_RANGE + Config.CHAR_RADIUS;
        return dx * dx + dy * dy <= reach * reach;
    }

    private static boolean arrived(Character c, double gx, double gy) {
        return near(c, gx, gy, Config.CHAR_RADIUS);
    }

    private static boolean near(Character c, double gx, double gy, double reach) {
        double dx = c.x() - gx;
        double dy = c.y() - gy;
        return dx * dx + dy * dy <= reach * reach;
    }

    private static double clampWorld(double v) {
        double lo = Config.CHAR_RADIUS;
        double hi = Config.WORLD_SIZE - Config.CHAR_RADIUS;
        if (v < lo) { return lo; }
        if (v > hi) { return hi; }
        return v;
    }
}
