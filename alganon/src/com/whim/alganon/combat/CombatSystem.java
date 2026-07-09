package com.whim.alganon.combat;

import com.whim.alganon.api.ActionResult;
import com.whim.alganon.api.CharacterModel;
import com.whim.alganon.api.Combatant;
import com.whim.alganon.api.Content;
import com.whim.alganon.api.Defs.AbilityDef;
import com.whim.alganon.api.Defs.LootDrop;
import com.whim.alganon.api.Defs.MobDef;
import com.whim.alganon.api.Enums.AbilityKind;
import com.whim.alganon.api.Enums.ChatChannel;
import com.whim.alganon.api.Enums.DamageType;
import com.whim.alganon.api.Enums.ResourceType;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.Stance;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.api.Enums.TargetType;
import com.whim.alganon.api.GameContext;
import com.whim.alganon.api.GameModel;
import com.whim.alganon.api.GridPos;
import com.whim.alganon.api.WorldModel;
import com.whim.alganon.api.WorldModel.MobEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Real-time-with-cooldowns combat. Drives targeting, per-class resource spend, ability
 * cooldowns, cast times, DOT/HOT ticks, stance/school modifiers, simple mob AI, and
 * death → loot/XP → level-up. All state here is transient (not persisted): it is reset
 * on new-game/load. Operates purely through {@code api} interfaces.
 *
 * <p>Design decisions ([Gap — my design] unless anchored): aggro radius, mob attack
 * cadence, flee-at-low-hp for passive mobs, fixed damage scaling, and 1s DOT/HOT ticks.</p>
 */
public final class CombatSystem {

    /** Engine callbacks so quest/study/state react to combat outcomes. */
    public interface Listener {
        void onMobKilled(MobDef def, MobEntity mob);
        void onPlayerDeath();
        void onAbilityUsed(AbilityDef ability);
        void onLoot(String itemId, int qty);
    }

    // ---- tuning ([Gap — my design]) ----
    private static final int AGGRO_RADIUS = 4;
    private static final int ENGAGE_RANGE = 6;        // how far a targeted ability reaches
    private static final double MOB_ATTACK_CD = 1.8;  // seconds between mob swings
    private static final double MOB_MOVE_CD = 0.5;    // seconds between mob steps
    private static final int LOG_MAX = 40;

    private final Content content;
    private final GameContext ctx;
    private final Listener listener;

    private final List<MobEntity> engaged = new ArrayList<MobEntity>();
    private final Map<String, Double> cooldowns = new HashMap<String, Double>();
    private final List<ActiveEffect> effects = new ArrayList<ActiveEffect>();
    private final Map<MobEntity, Double> mobAttackCd = new HashMap<MobEntity, Double>();
    private final Map<MobEntity, Double> mobMoveCd = new HashMap<MobEntity, Double>();
    private final Deque<String> combatLog = new ArrayDeque<String>();
    private CastState cast;

    public CombatSystem(Content content, GameContext ctx, Listener listener) {
        this.content = content;
        this.ctx = ctx;
        this.listener = listener;
    }

    // ---------- queries for the view builder ----------

    public boolean isActive() { return !engaged.isEmpty(); }

    public List<MobEntity> engaged() { return engaged; }

    public double cooldownRemaining(String abilityId) {
        Double d = cooldowns.get(abilityId);
        return d == null ? 0.0 : Math.max(0.0, d);
    }

    public boolean isCasting() { return cast != null; }

    public String combatLogText() {
        StringBuilder sb = new StringBuilder();
        for (String s : combatLog) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(s);
        }
        return sb.toString();
    }

    public void reset() {
        for (MobEntity m : engaged) m.setInCombat(false);
        engaged.clear();
        cooldowns.clear();
        effects.clear();
        mobAttackCd.clear();
        mobMoveCd.clear();
        combatLog.clear();
        cast = null;
    }

    // ---------- player ability use ----------

    public ActionResult useAbility(GameModel model, String abilityId, int targetIndex) {
        CharacterModel p = model.player();
        if (p == null || !p.alive()) return ActionResult.fail("You are in no shape to fight.");
        if (cast != null) return ActionResult.fail("You are already casting " + cast.def.name + ".");
        if (!p.knownAbilityIds().contains(abilityId)) return ActionResult.fail("You have not learned that ability.");

        AbilityDef ab = content.ability(abilityId);
        if (ab == null) return ActionResult.fail("Unknown ability.");
        if (p.level() < ab.levelReq) return ActionResult.fail(ab.name + " requires level " + ab.levelReq + ".");
        if (cooldownRemaining(abilityId) > 0) return ActionResult.fail(ab.name + " is not ready.");
        if (p.resource() < ab.resourceCost) return ActionResult.fail("Not enough " + p.resourceType() + ".");

        Combatant target = resolveTarget(model, ab, targetIndex);
        if (target == null) return ActionResult.fail("No valid target in range.");

        // Commit cost + cooldown up front (cast or instant).
        p.setResource(p.resource() - ab.resourceCost);
        cooldowns.put(abilityId, ab.cooldownSec);

        if (ab.castSec > 0) {
            cast = new CastState(ab, target, ab.castSec);
            log(ChatChannel.COMBAT, p.getName() + " begins to cast " + ab.name + ".");
            return ActionResult.ok("Casting " + ab.name + "…");
        }
        resolve(model, ab, target);
        return ActionResult.ok(ab.name + "!");
    }

    private Combatant resolveTarget(GameModel model, AbilityDef ab, int targetIndex) {
        CharacterModel p = model.player();
        TargetType tt = ab.target;
        if (tt == TargetType.SELF || tt == TargetType.NONE || tt == TargetType.GROUND
                || ab.kind == AbilityKind.STANCE || ab.kind == AbilityKind.PET_SUMMON) {
            return p;
        }
        if (tt == TargetType.ALLY) {
            return p; // no allied combatants in v1 [Gap — my design]
        }
        // ENEMY: explicit index into combatants (0 = player, 1.. = engaged), else auto-acquire.
        if (targetIndex >= 1 && targetIndex - 1 < engaged.size()) {
            MobEntity m = engaged.get(targetIndex - 1);
            if (m.alive()) return m;
        }
        MobEntity nearest = nearestMob(model, p.pos(), ENGAGE_RANGE);
        if (nearest != null) {
            engage(nearest);
            return nearest;
        }
        for (MobEntity m : engaged) if (m.alive()) return m;
        return null;
    }

    private void resolve(GameModel model, AbilityDef ab, Combatant target) {
        CharacterModel p = model.player();
        switch (ab.kind) {
            case DAMAGE: {
                int dmg = computeOutgoing(p, ab);
                int dealt = target.takeDamage(dmg, ab.damageType);
                log(ChatChannel.COMBAT, p.getName() + " strikes " + target.name() + " with " + ab.name + " for " + dealt + ".");
                gainFury(p);
                if (target instanceof MobEntity) engage((MobEntity) target);
                break;
            }
            case DOT: {
                effects.add(new ActiveEffect(target, AbilityKind.DOT, ab.damageType, dotTick(p, ab), ab.durationSec, ab.name));
                log(ChatChannel.COMBAT, target.name() + " is afflicted by " + ab.name + ".");
                gainFury(p);
                if (target instanceof MobEntity) engage((MobEntity) target);
                break;
            }
            case HEAL: {
                int amount = ab.power + spiritScale(p);
                target.heal(amount);
                log(ChatChannel.COMBAT, p.getName() + " restores " + amount + " health with " + ab.name + ".");
                break;
            }
            case HOT: {
                effects.add(new ActiveEffect(p, AbilityKind.HOT, ab.damageType, Math.max(1, ab.power / 2 + spiritScale(p) / 2), ab.durationSec, ab.name));
                log(ChatChannel.COMBAT, p.getName() + " is soothed by " + ab.name + ".");
                break;
            }
            case DEBUFF: {
                effects.add(new ActiveEffect(target, AbilityKind.DEBUFF, ab.damageType, ab.power, ab.durationSec, ab.name));
                log(ChatChannel.COMBAT, target.name() + " is weakened by " + ab.name + ".");
                if (target instanceof MobEntity) engage((MobEntity) target);
                break;
            }
            case BUFF: {
                effects.add(new ActiveEffect(p, AbilityKind.BUFF, ab.damageType, ab.power, ab.durationSec, ab.name));
                log(ChatChannel.COMBAT, p.getName() + " is bolstered by " + ab.name + ".");
                break;
            }
            case TRAP: {
                // A ground trap that immediately bites the nearest enemy. [Gap — my design]
                MobEntity m = nearestMob(model, p.pos(), ENGAGE_RANGE);
                if (m != null) {
                    int dealt = m.takeDamage(computeOutgoing(p, ab), ab.damageType);
                    log(ChatChannel.COMBAT, ab.name + " snaps shut on " + m.name() + " for " + dealt + ".");
                    engage(m);
                } else {
                    log(ChatChannel.COMBAT, p.getName() + " sets " + ab.name + ".");
                }
                break;
            }
            case PET_SUMMON:
                log(ChatChannel.COMBAT, p.getName() + " summons a companion (" + ab.name + ").");
                break;
            case STANCE:
            case UTILITY:
            default:
                log(ChatChannel.COMBAT, p.getName() + " uses " + ab.name + ".");
                break;
        }
        if (listener != null) listener.onAbilityUsed(ab);
        checkDeaths(model);
    }

    // ---------- damage math ----------

    private int computeOutgoing(CharacterModel p, AbilityDef ab) {
        double base = ab.power + p.attackPower() * 0.4;
        base *= stanceDamageMult(p.stance());
        if (ab.school != School.NONE && ab.school == p.school()) base *= 1.20; // school-match bonus [Gap]
        // deterministic-ish ±10% variance from the shared rng
        double variance = 0.90 + ctx.rng().nextDouble() * 0.20;
        return (int) Math.max(1, Math.round(base * variance));
    }

    private int dotTick(CharacterModel p, AbilityDef ab) {
        double perTick = ab.power * 0.6 + p.attackPower() * 0.15;
        perTick *= stanceDamageMult(p.stance());
        return (int) Math.max(1, Math.round(perTick));
    }

    private static double stanceDamageMult(Stance s) {
        if (s == Stance.POWER) return 1.25;
        if (s == Stance.DEFENSE) return 0.80;
        return 1.0;
    }

    private int spiritScale(CharacterModel p) {
        Integer spirit = p.stats().get(StatType.SPIRIT);
        return (spirit == null ? 0 : spirit) / 2;
    }

    private void gainFury(CharacterModel p) {
        if (p.resourceType() == ResourceType.FURY) {
            p.setResource(Math.min(p.maxResource(), p.resource() + 8));
        }
    }

    // ---------- per-tick advancement ----------

    public void tick(GameModel model, double dt) {
        CharacterModel p = model.player();
        if (p == null) return;

        // cooldowns
        Iterator<Map.Entry<String, Double>> it = cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Double> e = it.next();
            double v = e.getValue() - dt;
            if (v <= 0) it.remove(); else e.setValue(v);
        }

        // cast completion
        if (cast != null) {
            cast.remaining -= dt;
            if (cast.remaining <= 0) {
                AbilityDef ab = cast.def;
                Combatant t = cast.target;
                cast = null;
                if (t == p || (t != null && t.alive())) {
                    resolve(model, ab, t);
                } else {
                    log(ChatChannel.COMBAT, "The cast fizzles — target is gone.");
                }
            }
        }

        tickEffects(dt);
        regenResource(p, dt);
        mobAi(model, dt);
        checkDeaths(model);

        if (!p.alive()) {
            log(ChatChannel.SYSTEM, p.getName() + " has fallen.");
            reset();
            if (listener != null) listener.onPlayerDeath();
        } else {
            pruneEngaged(model);
        }
    }

    private void tickEffects(double dt) {
        Iterator<ActiveEffect> it = effects.iterator();
        while (it.hasNext()) {
            ActiveEffect ef = it.next();
            int ticks = ef.advance(dt);
            for (int i = 0; i < ticks; i++) {
                if (!ef.target.alive() && ef.kind != AbilityKind.HOT) break;
                if (ef.kind == AbilityKind.DOT) {
                    int dealt = ef.target.takeDamage(ef.power, ef.type);
                    log(ChatChannel.COMBAT, ef.target.name() + " suffers " + dealt + " from " + ef.name + ".");
                } else if (ef.kind == AbilityKind.HOT) {
                    ef.target.heal(ef.power);
                    log(ChatChannel.COMBAT, ef.target.name() + " recovers " + ef.power + " from " + ef.name + ".");
                }
                // BUFF/DEBUFF have no per-tick payload in v1.
            }
            if (ef.expired()) it.remove();
        }
    }

    private void regenResource(CharacterModel p, double dt) {
        ResourceType rt = p.resourceType();
        if (rt == ResourceType.FURY) {
            // Fury decays out of combat, holds in combat. [Gap — my design]
            if (!isActive() && p.resource() > 0) {
                p.setResource(Math.max(0, p.resource() - (int) Math.round(3 * dt)));
            }
        } else {
            // Mana/Focus regen toward max.
            double regen = (p.maxResource() * 0.04 + 2) * dt;
            int add = (int) Math.round(regen);
            if (add < 1 && ctx.rng().nextDouble() < regen) add = 1;
            if (p.resource() < p.maxResource() && add > 0) {
                p.setResource(Math.min(p.maxResource(), p.resource() + add));
            }
        }
    }

    // ---------- mob AI ----------

    private void mobAi(GameModel model, double dt) {
        CharacterModel p = model.player();
        WorldModel world = model.world();
        if (world == null) return;

        // Acquire aggro for aggressive mobs near the player.
        for (MobEntity m : new ArrayList<MobEntity>(world.mobs())) {
            if (!m.alive()) continue;
            if (engaged.contains(m)) continue;
            MobDef def = content.mob(m.defId());
            boolean aggressive = def != null && def.behavior == com.whim.alganon.api.Enums.MobBehavior.AGGRESSIVE;
            if (aggressive && m.pos().manhattan(p.pos()) <= AGGRO_RADIUS) {
                engage(m);
                log(ChatChannel.COMBAT, m.name() + " notices you and closes in!");
            }
        }

        for (MobEntity m : new ArrayList<MobEntity>(engaged)) {
            if (!m.alive()) continue;
            MobDef def = content.mob(m.defId());
            boolean passive = def != null && def.behavior == com.whim.alganon.api.Enums.MobBehavior.PASSIVE;

            double move = mobMoveCd.containsKey(m) ? mobMoveCd.get(m) - dt : 0;
            double atk = mobAttackCd.containsKey(m) ? mobAttackCd.get(m) - dt : 0;

            boolean fleeing = passive || (m.hp() * 4 < m.maxHp()); // flee when very low or passive
            int dist = m.pos().manhattan(p.pos());

            if (dist <= 1 && !fleeing) {
                if (atk <= 0) {
                    DamageType dt2 = def != null ? def.damageType : DamageType.PHYSICAL;
                    int dealt = p.takeDamage(m.attackPower(), dt2);
                    log(ChatChannel.COMBAT, m.name() + " hits you for " + dealt + ".");
                    atk = MOB_ATTACK_CD;
                }
            } else if (move <= 0) {
                stepMob(model, m, !fleeing);
                move = MOB_MOVE_CD;
            }
            mobMoveCd.put(m, Math.max(0, move));
            mobAttackCd.put(m, Math.max(0, atk));
        }
    }

    private void stepMob(GameModel model, MobEntity m, boolean toward) {
        CharacterModel p = model.player();
        WorldModel world = model.world();
        GridPos mp = m.pos();
        GridPos pp = p.pos();
        int dx = Integer.compare(pp.x, mp.x);
        int dy = Integer.compare(pp.y, mp.y);
        if (!toward) { dx = -dx; dy = -dy; }
        // prefer the dominant axis
        int adx = Math.abs(pp.x - mp.x);
        int ady = Math.abs(pp.y - mp.y);
        int[] first = adx >= ady ? new int[]{dx, 0} : new int[]{0, dy};
        int[] second = adx >= ady ? new int[]{0, dy} : new int[]{dx, 0};
        if (tryStep(world, m, mp, first)) return;
        tryStep(world, m, mp, second);
    }

    private boolean tryStep(WorldModel world, MobEntity m, GridPos from, int[] d) {
        if (d[0] == 0 && d[1] == 0) return false;
        int nx = from.x + d[0], ny = from.y + d[1];
        if (world.walkable(nx, ny)) {
            m.setPos(new GridPos(nx, ny));
            return true;
        }
        return false;
    }

    // ---------- death handling ----------

    private void checkDeaths(GameModel model) {
        for (MobEntity m : new ArrayList<MobEntity>(engaged)) {
            if (!m.alive()) killMob(model, m);
        }
        // also handle mobs killed by DOT that were engaged; and any world mob at <=0
        for (MobEntity m : new ArrayList<MobEntity>(model.world() == null ? new ArrayList<MobEntity>() : model.world().mobs())) {
            if (!m.alive() && engaged.contains(m)) killMob(model, m);
        }
    }

    private void killMob(GameModel model, MobEntity m) {
        engaged.remove(m);
        mobAttackCd.remove(m);
        mobMoveCd.remove(m);
        removeEffectsFor(m);
        m.setInCombat(false);

        CharacterModel p = model.player();
        MobDef def = content.mob(m.defId());
        log(ChatChannel.COMBAT, m.name() + " is slain.");
        if (model.world() != null) model.world().mobs().remove(m);

        if (def != null) {
            Progression.grantXp(p, def.xpReward, ctx, content);
            rollLoot(p, def);
        }
        if (listener != null) listener.onMobKilled(def, m);
    }

    private void rollLoot(CharacterModel p, MobDef def) {
        for (LootDrop drop : def.loot) {
            if (ctx.rng().nextDouble() <= drop.chance) {
                int qty = drop.min + (drop.max > drop.min ? ctx.rng().nextInt(drop.max - drop.min + 1) : 0);
                if (qty <= 0) qty = Math.max(1, drop.min);
                p.addItem(drop.itemId, qty);
                log(ChatChannel.LOOT, "You loot " + qty + "x " + lootName(drop.itemId) + ".");
                if (listener != null) listener.onLoot(drop.itemId, qty);
            }
        }
    }

    private String lootName(String itemId) {
        com.whim.alganon.api.Defs.ItemDef d = content.item(itemId);
        return d != null ? d.name : itemId;
    }

    private void removeEffectsFor(Combatant c) {
        Iterator<ActiveEffect> it = effects.iterator();
        while (it.hasNext()) if (it.next().target == c) it.remove();
    }

    private void pruneEngaged(GameModel model) {
        CharacterModel p = model.player();
        Iterator<MobEntity> it = engaged.iterator();
        while (it.hasNext()) {
            MobEntity m = it.next();
            if (!m.alive()) { it.remove(); continue; }
            // drop aggro if the player has run far away and mob isn't adjacent
            if (m.pos().manhattan(p.pos()) > AGGRO_RADIUS + ENGAGE_RANGE) {
                m.setInCombat(false);
                mobAttackCd.remove(m);
                mobMoveCd.remove(m);
                it.remove();
            }
        }
    }

    // ---------- helpers ----------

    private void engage(MobEntity m) {
        if (!engaged.contains(m)) {
            engaged.add(m);
            m.setInCombat(true);
        }
    }

    private MobEntity nearestMob(GameModel model, GridPos from, int maxRange) {
        WorldModel world = model.world();
        if (world == null) return null;
        MobEntity best = null;
        int bestD = Integer.MAX_VALUE;
        for (MobEntity m : world.mobs()) {
            if (!m.alive()) continue;
            int d = m.pos().manhattan(from);
            if (d <= maxRange && d < bestD) { best = m; bestD = d; }
        }
        return best;
    }

    private void log(ChatChannel ch, String text) {
        combatLog.addLast(text);
        while (combatLog.size() > LOG_MAX) combatLog.removeFirst();
        if (ctx != null) ctx.log(ch, text);
    }

    /** A pending cast in progress. */
    private static final class CastState {
        final AbilityDef def;
        final Combatant target;
        double remaining;
        CastState(AbilityDef def, Combatant target, double remaining) {
            this.def = def; this.target = target; this.remaining = remaining;
        }
    }
}
