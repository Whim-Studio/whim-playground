package com.arpg.engine;

import com.arpg.model.Ability;
import com.arpg.model.BuffDebuff;
import com.arpg.model.CombatParticipant;
import com.arpg.model.EffectType;
import com.arpg.model.StatType;
import com.arpg.model.TargetType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Headless combat resolver: ability resolution, attack-vs-defense damage/heal math, buff/debuff
 * application, per-tick periodic effects + expiry, cooldown tracking, death handling and targeting.
 * Holds no game-wide state beyond per-participant cooldown timers (reset each encounter).
 */
final class CombatEngine {
    static final int BUFF_DURATION_TICKS = 3;
    static final int DEBUFF_DURATION_TICKS = 3;
    private static final double VARIANCE = 0.15;

    private final Random rng;
    private final EventBus bus;

    /** participant -> (abilityId -> remaining cooldown ticks). Identity-keyed. */
    private final Map<CombatParticipant, Map<String, Integer>> cooldowns =
            new IdentityHashMap<CombatParticipant, Map<String, Integer>>();

    CombatEngine(Random rng, EventBus bus) {
        this.rng = rng;
        this.bus = bus;
    }

    // ---- effective stats (fold in active buff/debuff modifiers) -------------------------------

    static int effectiveAttack(CombatParticipant p) {
        int atk = p.getAttackPower();
        List<BuffDebuff> buffs = p.getActiveBuffs();
        for (int i = 0; i < buffs.size(); i++) atk += buffs.get(i).getStatModifier(StatType.ATTACK_POWER);
        return Math.max(0, atk);
    }

    static int effectiveDefense(CombatParticipant p) {
        int def = p.getDefense();
        List<BuffDebuff> buffs = p.getActiveBuffs();
        for (int i = 0; i < buffs.size(); i++) def += buffs.get(i).getStatModifier(StatType.ARMOR);
        return Math.max(0, def);
    }

    // ---- cooldowns ----------------------------------------------------------------------------

    void resetCooldowns() { cooldowns.clear(); }

    boolean isOnCooldown(CombatParticipant p, String abilityId) {
        Map<String, Integer> m = cooldowns.get(p);
        return m != null && m.containsKey(abilityId) && m.get(abilityId) > 0;
    }

    private void setCooldown(CombatParticipant p, Ability a) {
        if (a.getCooldown() <= 0) return;
        Map<String, Integer> m = cooldowns.get(p);
        if (m == null) { m = new HashMap<String, Integer>(); cooldowns.put(p, m); }
        m.put(a.getId(), a.getCooldown());
    }

    /** Decrement every participant's cooldown timers by one tick. */
    void tickCooldowns() {
        for (Map<String, Integer> m : cooldowns.values()) {
            for (Map.Entry<String, Integer> e : m.entrySet()) {
                if (e.getValue() > 0) e.setValue(e.getValue() - 1);
            }
        }
    }

    boolean canUse(CombatParticipant p, Ability a) {
        return p.isAlive()
                && p.getCurrentResource() >= a.getResourceCost()
                && !isOnCooldown(p, a.getId());
    }

    // ---- targeting ----------------------------------------------------------------------------

    static List<CombatParticipant> living(List<CombatParticipant> list) {
        List<CombatParticipant> out = new ArrayList<CombatParticipant>();
        for (int i = 0; i < list.size(); i++) if (list.get(i).isAlive()) out.add(list.get(i));
        return out;
    }

    private static CombatParticipant pick(List<CombatParticipant> opposing, int targetIndex) {
        List<CombatParticipant> alive = living(opposing);
        if (alive.isEmpty()) return null;
        if (targetIndex >= 0 && targetIndex < opposing.size() && opposing.get(targetIndex).isAlive()) {
            return opposing.get(targetIndex);
        }
        return alive.get(0);
    }

    private static CombatParticipant lowestHealth(List<CombatParticipant> allies) {
        CombatParticipant best = null;
        double bestPct = 2.0;
        for (int i = 0; i < allies.size(); i++) {
            CombatParticipant a = allies.get(i);
            if (!a.isAlive()) continue;
            double pct = a.getMaxHealth() == 0 ? 1.0 : (double) a.getCurrentHealth() / a.getMaxHealth();
            if (pct < bestPct) { bestPct = pct; best = a; }
        }
        return best;
    }

    // ---- resolution ---------------------------------------------------------------------------

    /**
     * Resolve an ability cast by {@code source} against the {@code opposing} side, with {@code allies}
     * on the caster's side. Spends resource, applies the effect, fires events and starts the cooldown.
     * Returns true when the ability requested a SUMMON (the facade performs the actual summon).
     */
    boolean resolveAbility(CombatParticipant source, Ability ability,
                           List<CombatParticipant> opposing, List<CombatParticipant> allies,
                           int targetIndex) {
        if (!canUse(source, ability)) {
            bus.log(source.getName() + " cannot use " + ability.getName() + " (not enough resource or on cooldown).");
            return false;
        }
        source.spendResource(ability.getResourceCost());
        setCooldown(source, ability);

        boolean summon = false;
        switch (ability.getEffectType()) {
            case DAMAGE:
                if (ability.getTargetType() == TargetType.AOE_ENEMIES) {
                    List<CombatParticipant> alive = living(opposing);
                    for (int i = 0; i < alive.size(); i++) dealDamage(source, alive.get(i), ability);
                } else {
                    CombatParticipant t = pick(opposing, targetIndex);
                    if (t != null) dealDamage(source, t, ability);
                }
                break;
            case HEAL: {
                CombatParticipant t = ability.getTargetType() == TargetType.SELF
                        ? source : lowestHealth(allies);
                if (t == null) t = source;
                heal(source, t, ability.getMagnitude());
                break;
            }
            case BUFF: {
                CombatParticipant t = ability.getTargetType() == TargetType.ALLY
                        ? lowestHealth(allies) : source;
                if (t == null) t = source;
                applyBuff(t, buffTemplate(ability));
                break;
            }
            case DEBUFF:
                if (ability.getTargetType() == TargetType.AOE_ENEMIES) {
                    List<CombatParticipant> alive = living(opposing);
                    for (int i = 0; i < alive.size(); i++) applyBuff(alive.get(i), debuffTemplate(ability));
                } else {
                    CombatParticipant t = pick(opposing, targetIndex);
                    if (t != null) applyBuff(t, debuffTemplate(ability));
                }
                break;
            case SUMMON:
                summon = true;
                bus.log(source.getName() + " calls a companion to its side!");
                break;
            default:
                break;
        }
        return summon;
    }

    /** Basic weapon attack: attackPower vs defense, no ability magnitude. */
    void basicAttack(CombatParticipant source, List<CombatParticipant> opposing, int targetIndex) {
        CombatParticipant t = pick(opposing, targetIndex);
        if (t == null) return;
        int raw = effectiveAttack(source);
        int dmg = applyVariance(Math.max(1, raw - effectiveDefense(t)));
        deal(source, t, dmg, null, source.getName() + " strikes " + t.getName() + " for " + dmg + ".");
    }

    private void dealDamage(CombatParticipant source, CombatParticipant target, Ability ability) {
        int raw = effectiveAttack(source) + ability.getMagnitude();
        int dmg = applyVariance(Math.max(1, raw - effectiveDefense(target)));
        deal(source, target, dmg, ability,
                source.getName() + " hits " + target.getName() + " with " + ability.getName() + " for " + dmg + ".");
    }

    private void deal(CombatParticipant source, CombatParticipant target, int dmg, Ability ability, String line) {
        boolean wasAlive = target.isAlive();
        target.applyDamage(dmg);
        bus.damageDealt(source, target, dmg, ability);
        bus.log(line);
        if (wasAlive && !target.isAlive()) {
            bus.log(target.getName() + " has been slain.");
            bus.death(target);
        }
    }

    private void heal(CombatParticipant source, CombatParticipant target, int magnitude) {
        int amount = magnitude + effectiveAttack(source) / 4;
        int before = target.getCurrentHealth();
        target.applyHealing(amount);
        int actual = target.getCurrentHealth() - before;
        bus.healed(source, target, actual);
        bus.log(source.getName() + " heals " + target.getName() + " for " + actual + ".");
    }

    private void applyBuff(CombatParticipant target, BuffDebuff template) {
        BuffDebuff fresh = template.copy();
        target.addBuff(fresh);
        bus.buffApplied(target, fresh);
        bus.log(target.getName() + (fresh.isBeneficial() ? " gains " : " is afflicted by ") + fresh.getName() + ".");
    }

    /** A short offensive self/ally buff synthesised from a BUFF ability (or the ability's own buff). */
    private BuffDebuff buffTemplate(Ability a) {
        if (a.getAppliedBuff() != null) return a.getAppliedBuff();
        int mag = a.getMagnitude();
        int atkMod = Math.max(1, mag / 2);
        Map<StatType, Integer> mods = new HashMap<StatType, Integer>();
        mods.put(StatType.ATTACK_POWER, atkMod);
        mods.put(StatType.ARMOR, Math.max(1, mag / 3));
        return new BuffDebuff("buff." + a.getId(), a.getName(), a.getName(), true,
                BUFF_DURATION_TICKS, mods, 0);
    }

    /** A short debuff (attack/armor down + damage-over-time) synthesised from a DEBUFF ability. */
    private BuffDebuff debuffTemplate(Ability a) {
        if (a.getAppliedBuff() != null) return a.getAppliedBuff();
        int mag = a.getMagnitude();
        int drop = Math.max(1, mag / 2);
        Map<StatType, Integer> mods = new HashMap<StatType, Integer>();
        mods.put(StatType.ATTACK_POWER, -drop);
        mods.put(StatType.ARMOR, -drop);
        return new BuffDebuff("debuff." + a.getId(), a.getName(), a.getName(), false,
                DEBUFF_DURATION_TICKS, mods, -drop);
    }

    /**
     * Apply one tick of periodic buff/debuff effects to a participant, then age and expire buffs.
     * Fires healed/damage + buffExpired events. Safe to call on dead participants (no-op).
     */
    void tickBuffs(CombatParticipant p) {
        if (!p.isAlive()) return;
        List<BuffDebuff> snapshot = new ArrayList<BuffDebuff>(p.getActiveBuffs());
        for (int i = 0; i < snapshot.size(); i++) {
            BuffDebuff b = snapshot.get(i);
            int periodic = b.getPeriodicHealthDelta();
            if (periodic < 0) {
                int dmg = -periodic;
                boolean wasAlive = p.isAlive();
                p.applyDamage(dmg);
                bus.damageDealt(null, p, dmg, null);
                bus.log(p.getName() + " suffers " + dmg + " from " + b.getName() + ".");
                if (wasAlive && !p.isAlive()) { bus.log(p.getName() + " succumbs."); bus.death(p); }
            } else if (periodic > 0 && p.isAlive()) {
                int before = p.getCurrentHealth();
                p.applyHealing(periodic);
                bus.healed(null, p, p.getCurrentHealth() - before);
            }
            b.decrementTick();
            if (b.isExpired()) {
                p.removeBuff(b);
                bus.buffExpired(p, b);
                bus.log(p.getName() + "'s " + b.getName() + " fades.");
            }
        }
    }

    private int applyVariance(int base) {
        int delta = (int) Math.round(base * VARIANCE);
        if (delta <= 0) return Math.max(1, base);
        int roll = rng.nextInt(2 * delta + 1) - delta;
        return Math.max(1, base + roll);
    }

    /**
     * Enemy/pet AI: self-heal when badly hurt, occasionally weave a buff/debuff, otherwise use the
     * strongest affordable damage ability. Returns null to fall back to a basic attack.
     */
    Ability chooseAbility(CombatParticipant actor) {
        List<Ability> abilities = actor.getAbilities();
        double hpPct = actor.getMaxHealth() == 0 ? 1.0 : (double) actor.getCurrentHealth() / actor.getMaxHealth();
        if (hpPct < 0.4) {
            Ability heal = strongestAffordable(actor, abilities, EffectType.HEAL);
            if (heal != null) return heal;
        }
        if (rng.nextInt(100) < 35) {
            Ability util = strongestAffordable(actor, abilities, EffectType.BUFF);
            if (util == null) util = strongestAffordable(actor, abilities, EffectType.DEBUFF);
            if (util != null) return util;
        }
        return strongestAffordable(actor, abilities, EffectType.DAMAGE);
    }

    private Ability strongestAffordable(CombatParticipant actor, List<Ability> abilities, EffectType type) {
        Ability best = null;
        for (int i = 0; i < abilities.size(); i++) {
            Ability a = abilities.get(i);
            if (a.getEffectType() != type || !canUse(actor, a)) continue;
            if (best == null || a.getMagnitude() > best.getMagnitude()) best = a;
        }
        return best;
    }
}
