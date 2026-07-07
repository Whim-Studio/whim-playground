package com.whim.b5wars.engine;

import com.whim.b5wars.data.CriticalEntry;
import com.whim.b5wars.model.Dice;
import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Section;
import com.whim.b5wars.model.Ship;
import com.whim.b5wars.model.Weapon;
import com.whim.b5wars.model.WeaponTrait;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * To-hit / arc-range / damage resolution.
 *
 * <p>Hit rule: a shot hits when {@code d20 >= modifiedToHit}. A HIGHER modified target number is
 * therefore HARDER to hit, so penalties (range, target speed, sensor/crew damage) add to it and
 * bonuses (large target size, net offensive EW) subtract from it.
 *
 * <p>Damage flow: penetrating damage first erodes the hit facing's defense layer (armor or
 * shield pool), then spills into hull structure of the exposed section (overflowing to other
 * sections). Depleting a section — or a raking weapon — triggers a d20 roll on the data-driven
 * critical table. All to-hit modifier weights and trait effects are APPROXIMATED and marked.
 *
 * <p>Because the model's {@code Ship} carries no field for accumulated system damage (reactor
 * hits, sensor/crew degradation), this engine keeps that per-ship damage in an identity-keyed
 * side table. Weapon disablement and thrust loss ARE expressed through the model's own mutators.
 */
public final class CombatEngine {

    // --- APPROXIMATED to-hit modifier weights (unverified vs rulebook) ---
    // APPROXIMATED, unverified vs rulebook — to-hit penalty added per range bracket beyond the closest.
    static final int RANGE_TOHIT_PENALTY_PER_BRACKET = 2;
    // APPROXIMATED, unverified vs rulebook — target speed divided by this yields the to-hit penalty.
    static final int SPEED_HEXES_PER_TOHIT_PENALTY = 2;
    // APPROXIMATED, unverified vs rulebook — target size proxy (points/structure) divided by this yields the to-hit bonus.
    static final int SIZE_UNITS_PER_TOHIT_BONUS = 100;
    // APPROXIMATED, unverified vs rulebook — weight applied to net (offensive - defensive) EW.
    static final int EW_TOHIT_WEIGHT = 1;
    // APPROXIMATED, unverified vs rulebook — clamp bounds for the final to-hit number.
    static final int TOHIT_MIN = 2;
    static final int TOHIT_MAX = 20;

    // --- APPROXIMATED trait / crit effects (unverified vs rulebook) ---
    // APPROXIMATED, unverified vs rulebook — damage removed per point-defense/interceptor mount vs BALLISTIC/GUIDED.
    static final int INTERCEPTOR_REDUCTION_PER_MOUNT = 2;
    // APPROXIMATED, unverified vs rulebook — thrust lost on a REACTOR critical.
    static final int REACTOR_CRIT_THRUST_LOSS = 2;
    // APPROXIMATED, unverified vs rulebook — thrust lost on an ENGINE critical.
    static final int ENGINE_CRIT_THRUST_LOSS = 2;
    // APPROXIMATED, unverified vs rulebook — reactor criticals needed to destroy the ship.
    static final int REACTOR_HITS_TO_DESTROY = 3;
    // APPROXIMATED, unverified vs rulebook — to-hit penalty added per SENSOR critical (attacker degraded).
    static final int SENSOR_CRIT_TOHIT_PENALTY = 2;
    // APPROXIMATED, unverified vs rulebook — to-hit penalty added per CREW critical (attacker degraded).
    static final int CREW_CRIT_TOHIT_PENALTY = 1;
    // Sentinel "ready turn" that marks a weapon permanently disabled by a critical.
    static final int WEAPON_DISABLED_TURN = 1_000_000;

    private final Dice dice;
    private final List<CriticalEntry> critTable;
    private final Map<Ship, ShipDamage> damage = new IdentityHashMap<Ship, ShipDamage>();

    public CombatEngine(Dice dice, List<CriticalEntry> critTable) {
        this.dice = dice;
        this.critTable = critTable == null ? new ArrayList<CriticalEntry>() : critTable;
    }

    /** Accumulated system damage that the model's Ship cannot itself hold. */
    private static final class ShipDamage {
        int reactorHits = 0;
        int sensorPenalty = 0;
        int crewPenalty = 0;
    }

    private ShipDamage dmg(Ship s) {
        ShipDamage d = damage.get(s);
        if (d == null) {
            d = new ShipDamage();
            damage.put(s, d);
        }
        return d;
    }

    // ------------------------------------------------------------------ arc & range

    /**
     * True when the target lies inside the weapon's firing arc AND within its maximum range
     * bracket. Bearing is computed from the model's own hex geometry and expressed relative to
     * the attacker's facing before consulting the arc.
     */
    public boolean inArcAndRange(Ship attacker, int weaponIndex, Ship target) {
        Weapon w = attacker.getType().getWeapons().get(weaponIndex);
        int dist = attacker.getPos().distance(target.getPos());
        if (dist > maxRange(w)) {
            return false;
        }
        Facing bearing = HexGeometry.bearing(attacker.getPos(), target.getPos());
        if (bearing == null) {
            // Same hex: treat as within every arc.
            return true;
        }
        Facing relative = HexGeometry.relative(bearing, attacker.getFacing());
        return w.getArc().contains(relative);
    }

    private static int maxRange(Weapon w) {
        int[] brackets = w.getRangeBrackets();
        if (brackets == null || brackets.length == 0) {
            return Integer.MAX_VALUE;
        }
        return brackets[brackets.length - 1];
    }

    /** Index of the range bracket a distance falls in (0 = closest); brackets.length if beyond. */
    private static int bracketIndex(Weapon w, int dist) {
        int[] brackets = w.getRangeBrackets();
        if (brackets == null || brackets.length == 0) {
            return 0;
        }
        for (int i = 0; i < brackets.length; i++) {
            if (dist <= brackets[i]) {
                return i;
            }
        }
        return brackets.length;
    }

    // ------------------------------------------------------------------ to-hit

    /**
     * The modified d20 target number for this shot. Hit occurs when {@code d20 >= result}.
     * Applies range, target-speed, target-size, net-EW and attacker sensor/crew degradation.
     */
    public int toHitTarget(Ship attacker, int weaponIndex, Ship target) {
        Weapon w = attacker.getType().getWeapons().get(weaponIndex);
        int dist = attacker.getPos().distance(target.getPos());
        int result = w.getBaseToHit();

        // Range: harder the farther out (per bracket beyond the closest).
        int bracket = bracketIndex(w, dist);
        result += RANGE_TOHIT_PENALTY_PER_BRACKET * bracket;

        // Target speed: faster is harder.
        if (SPEED_HEXES_PER_TOHIT_PENALTY > 0) {
            result += target.getSpeed() / SPEED_HEXES_PER_TOHIT_PENALTY;
        }

        // Target size (points, or total structure as a proxy): bigger is easier.
        if (SIZE_UNITS_PER_TOHIT_BONUS > 0) {
            result -= sizeProxy(target) / SIZE_UNITS_PER_TOHIT_BONUS;
        }

        // Electronic warfare: net offensive EW helps the attacker.
        int netEw = attacker.getEwOffensive() - target.getEwDefensive();
        result -= netEw * EW_TOHIT_WEIGHT;

        // Attacker's own sensor / crew degradation from prior criticals.
        ShipDamage ad = damage.get(attacker);
        if (ad != null) {
            result += ad.sensorPenalty + ad.crewPenalty;
        }

        if (result < TOHIT_MIN) {
            result = TOHIT_MIN;
        }
        if (result > TOHIT_MAX) {
            result = TOHIT_MAX;
        }
        return result;
    }

    private static int sizeProxy(Ship s) {
        int points = s.getType().getPoints();
        return points > 0 ? points : s.totalStructureRemaining();
    }

    // ------------------------------------------------------------------ fire

    /**
     * Resolve one shot from {@code attacker}'s weapon at {@code target} during {@code currentTurn}.
     * Checks arc/range and reload, rolls d20 vs the modified to-hit, and on a hit rolls damage,
     * applies interceptor / armor-piercing modifiers, erodes the hit facing's defense layer then
     * structure, and rolls criticals on section depletion or a raking weapon. Firing (hit or
     * miss) puts the weapon on reload.
     */
    public List<GameEvent> fire(Ship attacker, int weaponIndex, Ship target, int currentTurn) {
        List<GameEvent> events = new ArrayList<GameEvent>();
        Weapon w = attacker.getType().getWeapons().get(weaponIndex);

        if (attacker.isDestroyed()) {
            return events;
        }
        if (attacker.getReloadReadyTurn(weaponIndex) > currentTurn) {
            events.add(new GameEvent("MISS", attacker.getType().getName() + "'s " + w.getName()
                    + " is reloading"));
            return events;
        }
        if (!inArcAndRange(attacker, weaponIndex, target)) {
            events.add(new GameEvent("MISS", target.getType().getName()
                    + " is out of arc/range for " + w.getName()));
            return events;
        }

        // The weapon fires: put it on reload regardless of outcome.
        attacker.setReloadReadyTurn(weaponIndex, currentTurn + 1 + w.getReloadTurns());

        int target20 = toHitTarget(attacker, weaponIndex, target);
        int roll = dice.d20();
        events.add(new GameEvent("FIRE", attacker.getType().getName() + " fires " + w.getName()
                + " at " + target.getType().getName() + " (need " + target20 + ", rolled " + roll + ")"));
        if (roll < target20) {
            events.add(new GameEvent("MISS", w.getName() + " misses"));
            return events;
        }

        events.addAll(resolveDamage(attacker, weaponIndex, target));
        return events;
    }

    /**
     * Apply a confirmed hit: interceptor reduction, armor-piercing-aware defense erosion,
     * structure/section damage, and criticals. Split out from {@link #fire} so the damage flow
     * is testable without depending on the d20 hit roll. Package-private.
     */
    List<GameEvent> resolveDamage(Ship attacker, int weaponIndex, Ship target) {
        List<GameEvent> events = new ArrayList<GameEvent>();
        Weapon w = attacker.getType().getWeapons().get(weaponIndex);

        // Determine hit facing (bearing target->attacker, relative to target facing).
        Facing hitFacing = Facing.F;
        Facing bearing = HexGeometry.bearing(target.getPos(), attacker.getPos());
        if (bearing != null) {
            hitFacing = HexGeometry.relative(bearing, target.getFacing());
        }

        int rawDamage = w.getDamage().roll(dice);

        // INTERCEPTOR: target point-defense reduces incoming BALLISTIC/GUIDED damage before armor.
        if (w.has(WeaponTrait.BALLISTIC) || w.has(WeaponTrait.GUIDED)) {
            int mounts = interceptorMounts(target);
            int reduction = mounts * INTERCEPTOR_REDUCTION_PER_MOUNT;
            if (reduction > 0) {
                int before = rawDamage;
                rawDamage = Math.max(0, rawDamage - reduction);
                events.add(new GameEvent("HIT", "Interceptors reduce damage " + before + " -> "
                        + rawDamage));
            }
        }

        // Defense layer of the hit facing. ARMOR_PIERCING bypasses half the layer's effect.
        Map<Facing, Integer> layer = target.getArmor();
        int pool = layer.containsKey(hitFacing) ? layer.get(hitFacing).intValue() : 0;
        int effective = w.has(WeaponTrait.ARMOR_PIERCING) ? pool / 2 : pool;
        int absorbed = Math.min(rawDamage, effective);
        int penetrating = rawDamage - absorbed;
        layer.put(hitFacing, pool - absorbed);

        events.add(new GameEvent("HIT", w.getName() + " hits " + hitFacing + " for " + rawDamage
                + " (" + absorbed + " stopped by defense, " + penetrating + " penetrates)"));

        if (penetrating > 0) {
            Section section = HexGeometry.sectionForFacing(hitFacing);
            boolean depleted = applyToStructure(target, section, penetrating);
            if (depleted || w.has(WeaponTrait.RAKING)) {
                events.addAll(rollCritical(target));
            }
        }

        updateStatus(target, events);
        return events;
    }

    private static int interceptorMounts(Ship s) {
        int count = 0;
        for (Weapon w : s.getType().getWeapons()) {
            if (w.has(WeaponTrait.INTERCEPTOR)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Apply penetrating damage to the exposed section, overflowing to PRIMARY then the rest.
     * Returns true if any section was reduced to 0.
     */
    private boolean applyToStructure(Ship target, Section primary, int amount) {
        boolean depleted = false;
        Map<Section, Integer> structure = target.getStructure();
        Section[] order = new Section[] {
                primary, Section.PRIMARY, Section.FORE, Section.AFT, Section.PORT, Section.STARBOARD
        };
        for (Section sec : order) {
            if (amount <= 0) {
                break;
            }
            Integer curBoxed = structure.get(sec);
            if (curBoxed == null) {
                continue;
            }
            int cur = curBoxed.intValue();
            if (cur <= 0) {
                continue;
            }
            int hit = Math.min(cur, amount);
            structure.put(sec, cur - hit);
            amount -= hit;
            if (cur - hit == 0) {
                depleted = true;
            }
        }
        return depleted;
    }

    private List<GameEvent> rollCritical(Ship target) {
        List<GameEvent> events = new ArrayList<GameEvent>();
        int roll = dice.d20();
        String effect = lookupCrit(roll);
        events.add(new GameEvent("CRIT", target.getType().getName() + " critical (d20=" + roll
                + "): " + effect));
        applyCrit(target, effect, events);
        return events;
    }

    private String lookupCrit(int roll) {
        for (CriticalEntry e : critTable) {
            if (roll >= e.getRollMin() && roll <= e.getRollMax()) {
                return e.getEffect();
            }
        }
        return "NONE";
    }

    private void applyCrit(Ship target, String effect, List<GameEvent> events) {
        if (effect == null) {
            effect = "NONE";
        }
        if ("REACTOR".equals(effect)) {
            dmg(target).reactorHits++;
            target.setThrustAvailable(Math.max(0,
                    target.getThrustAvailable() - REACTOR_CRIT_THRUST_LOSS));
        } else if ("ENGINE".equals(effect)) {
            target.setThrustAvailable(Math.max(0,
                    target.getThrustAvailable() - ENGINE_CRIT_THRUST_LOSS));
        } else if ("WEAPON".equals(effect)) {
            disableFirstWeapon(target, events);
        } else if ("SENSOR".equals(effect)) {
            dmg(target).sensorPenalty += SENSOR_CRIT_TOHIT_PENALTY;
        } else if ("CREW".equals(effect)) {
            dmg(target).crewPenalty += CREW_CRIT_TOHIT_PENALTY;
        }
        // "NONE" (or unknown): no state change.
    }

    private void disableFirstWeapon(Ship target, List<GameEvent> events) {
        int n = target.getType().getWeapons().size();
        for (int i = 0; i < n; i++) {
            if (target.getReloadReadyTurn(i) < WEAPON_DISABLED_TURN) {
                target.setReloadReadyTurn(i, WEAPON_DISABLED_TURN);
                events.add(new GameEvent("CRIT", target.getType().getName() + "'s "
                        + target.getType().getWeapons().get(i).getName() + " is disabled"));
                return;
            }
        }
    }

    /** Recompute destroyed / crippled after damage. */
    private void updateStatus(Ship target, List<GameEvent> events) {
        boolean structureGone = target.totalStructureRemaining() <= 0;
        boolean reactorGone = dmg(target).reactorHits >= REACTOR_HITS_TO_DESTROY;
        if (structureGone || reactorGone) {
            if (!target.isDestroyed()) {
                target.setDestroyed(true);
                events.add(new GameEvent("VICTORY", target.getType().getName() + " is destroyed"));
            }
            return;
        }
        if (!target.isCrippled() && allWeaponsDisabled(target) && dmg(target).reactorHits > 0) {
            target.setCrippled(true);
            events.add(new GameEvent("CRIT", target.getType().getName() + " is crippled"));
        }
    }

    private boolean allWeaponsDisabled(Ship target) {
        int n = target.getType().getWeapons().size();
        if (n == 0) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (target.getReloadReadyTurn(i) < WEAPON_DISABLED_TURN) {
                return false;
            }
        }
        return true;
    }
}
