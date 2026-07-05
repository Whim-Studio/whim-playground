package com.whim.oggalaxy.combat;

import com.whim.oggalaxy.api.Catalog;
import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.DefenseDef;
import com.whim.oggalaxy.api.GameConfig;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.ShipDef;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * DETERMINISTIC combat, implemented exactly as the OG Galaxy build contract specifies.
 * No RNG affects the battle outcome — the only randomness is the (separately seeded)
 * moon roll, which is derived from the resulting debris and never changes ship losses.
 *
 * Model summary (see contract "DETERMINISTIC combat"):
 *   effective attack = base.weapon * (1 + 0.1*weaponsTech) * ownerCombatBonus
 *   effective shield = base.shield * (1 + 0.1*shieldTech)
 *   effective hull   = base.hull   * (1 + 0.1*armourTech)
 * Up to 6 rounds; each round both sides fire simultaneously from a start-of-round
 * snapshot. Rapid fire is modelled as a deterministic expected value:
 *   rapidMult(f, enemy) = 1 + Σ_t (enemyCount_t / enemyTotal) * max(0, rf(f→t) - 1)
 * Damage is distributed across enemy stacks in proportion to each stack's share of total
 * enemy hull; shields regenerate to full every round.
 *
 * This class imports only {@code api} — the engine feeds it plain maps and applies the
 * returned survivor maps / debris / plunder to its model.
 */
public final class CombatEngine {

    private CombatEngine() {
    }

    /** Combat-relevant profile of one side's owner empire. */
    public static final class Profile {
        public final double combatBonus;
        public final int weaponsTech;
        public final int shieldTech;
        public final int armourTech;

        public Profile(double combatBonus, int weaponsTech, int shieldTech, int armourTech) {
            this.combatBonus = combatBonus;
            this.weaponsTech = weaponsTech;
            this.shieldTech = shieldTech;
            this.armourTech = armourTech;
        }
    }

    /** Everything the engine needs to apply after a battle. */
    public static final class Outcome {
        public final List<String> roundSummaries = new ArrayList<String>();
        public Map<Ids.ShipType, Integer> attackerSurvivors = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        public Map<Ids.ShipType, Integer> defenderShipSurvivors = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        public Map<Ids.DefenseType, Integer> defenderDefenseSurvivors = new EnumMap<Ids.DefenseType, Integer>(Ids.DefenseType.class);
        public Map<Ids.ShipType, Integer> attackerLosses = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        public Map<Ids.ShipType, Integer> defenderShipLosses = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        public Map<Ids.DefenseType, Integer> defenderDefenseLosses = new EnumMap<Ids.DefenseType, Integer>(Ids.DefenseType.class);
        public Cost debris = Cost.ZERO;
        public Cost plunder = Cost.ZERO;
        public boolean moonCreated;
        public boolean attackerWon;
        public boolean defenderWon;
        public String outcomeText = "Draw";
    }

    /** A homogeneous stack of one unit type on one side. */
    private static final class Stack {
        final boolean isDefense;
        final Ids.ShipType ship;
        final Ids.DefenseType def;
        final ShipDef sdef;         // null for defenses (no rapid fire)
        int count;
        final double attack, shield, hull;
        final Cost unitCost;
        final double cargo;

        Stack(Ids.ShipType ship, ShipDef sdef, int count, double attack, double shield, double hull) {
            this.isDefense = false;
            this.ship = ship;
            this.def = null;
            this.sdef = sdef;
            this.count = count;
            this.attack = attack;
            this.shield = shield;
            this.hull = hull;
            this.unitCost = sdef.cost;
            this.cargo = sdef.cargo;
        }

        Stack(Ids.DefenseType def, DefenseDef ddef, int count, double attack, double shield, double hull) {
            this.isDefense = true;
            this.ship = null;
            this.def = def;
            this.sdef = null;
            this.count = count;
            this.attack = attack;
            this.shield = shield;
            this.hull = hull;
            this.unitCost = ddef.cost;
            this.cargo = 0;
        }
    }

    /**
     * Resolve a battle. Attacker fields ships only; defender fields ships and defenses.
     *
     * @param defenderIsPlanet true when the defender is a planet (enables plunder/moon)
     * @param defenderStored   the planet's stored m/c/d (for plunder); may be {@link Cost#ZERO}
     * @param moonRng          seeded RNG used ONLY for the moon roll
     */
    public static Outcome resolve(int[] loc,
                                  Map<Ids.ShipType, Integer> attackerShips, Profile attacker,
                                  Map<Ids.ShipType, Integer> defenderShips,
                                  Map<Ids.DefenseType, Integer> defenderDefenses, Profile defender,
                                  Catalog catalog, boolean defenderIsPlanet, Cost defenderStored,
                                  Random moonRng) {

        Outcome out = new Outcome();

        Map<Ids.ShipType, Integer> attStart = normalizeShips(attackerShips);
        Map<Ids.ShipType, Integer> defShipStart = normalizeShips(defenderShips);
        Map<Ids.DefenseType, Integer> defDefStart = normalizeDefenses(defenderDefenses);

        List<Stack> att = buildShipStacks(attStart, attacker, catalog);
        List<Stack> def = buildDefenderStacks(defShipStart, defDefStart, defender, catalog);

        int maxRounds = GameConfig.COMBAT_MAX_ROUNDS;
        for (int round = 1; round <= maxRounds; round++) {
            if (totalUnits(att) == 0 || totalUnits(def) == 0) break;

            double attackerDamage = sideDamage(att, def);
            double defenderDamage = sideDamage(def, att);

            distribute(def, attackerDamage);   // attacker hits defender
            distribute(att, defenderDamage);    // defender hits attacker

            out.roundSummaries.add("Round " + round + ": attacker deals "
                    + fmt(attackerDamage) + ", defender deals " + fmt(defenderDamage)
                    + "  →  attacker " + totalUnits(att) + " units / defender " + totalUnits(def) + " units");

            if (totalUnits(att) == 0 || totalUnits(def) == 0) break;
        }

        // ---- survivors ----
        Map<Ids.ShipType, Integer> attSurv = collectShips(att);
        Map<Ids.ShipType, Integer> defShipSurv = collectShips(def);
        Map<Ids.DefenseType, Integer> defDefSurv = collectDefenses(def);

        // ---- losses ----
        out.attackerLosses = diffShips(attStart, attSurv);
        out.defenderShipLosses = diffShips(defShipStart, defShipSurv);

        // ---- debris = 0.30 * (metal+crystal of destroyed SHIPS from both sides) ----
        double debrisM = 0, debrisC = 0;
        debrisM += lostMetal(catalog, out.attackerLosses);
        debrisC += lostCrystal(catalog, out.attackerLosses);
        debrisM += lostMetal(catalog, out.defenderShipLosses);
        debrisC += lostCrystal(catalog, out.defenderShipLosses);
        out.debris = new Cost(Math.floor(debrisM * GameConfig.DEBRIS_FIELD_RATIO),
                Math.floor(debrisC * GameConfig.DEBRIS_FIELD_RATIO), 0);

        // ---- outcome ----
        boolean attAlive = totalUnits(att) > 0;
        boolean defAlive = totalUnits(def) > 0;
        out.attackerWon = attAlive && !defAlive;
        out.defenderWon = defAlive && !attAlive;
        if (out.attackerWon) out.outcomeText = "Attacker victory";
        else if (out.defenderWon) out.outcomeText = "Defender holds — attacker destroyed";
        else out.outcomeText = "Draw — attacker retreats after " + maxRounds + " rounds";

        // ---- defense rebuild (deterministic) ----
        Map<Ids.DefenseType, Integer> defDefFinal = new EnumMap<Ids.DefenseType, Integer>(Ids.DefenseType.class);
        for (Map.Entry<Ids.DefenseType, Integer> e : defDefStart.entrySet()) {
            Ids.DefenseType t = e.getKey();
            int start = e.getValue();
            int survived = defDefSurv.containsKey(t) ? defDefSurv.get(t) : 0;
            int destroyed = start - survived;
            int rebuilt = (int) Math.round(GameConfig.DEFENSE_REBUILD_CHANCE * destroyed);
            int finalCount = Math.min(start, survived + rebuilt);
            if (finalCount > 0) defDefFinal.put(t, finalCount);
        }
        out.defenderDefenseSurvivors = defDefFinal;
        out.defenderShipSurvivors = defShipSurv;
        out.attackerSurvivors = attSurv;
        out.defenderDefenseLosses = diffDefenses(defDefStart, defDefFinal);

        // ---- plunder (attacker win vs a planet only) ----
        if (out.attackerWon && defenderIsPlanet && defenderStored != null) {
            double freeCargo = 0;
            for (Stack s : att) freeCargo += s.count * s.cargo;
            double storedTotal = defenderStored.structurePoints();
            if (freeCargo > 0 && storedTotal > 0) {
                double lootable = Math.min(freeCargo, GameConfig.MAX_PLUNDER_FRACTION * storedTotal);
                double share = lootable / storedTotal;
                double pm = Math.floor(defenderStored.metal * Math.min(GameConfig.MAX_PLUNDER_FRACTION, share));
                double pc = Math.floor(defenderStored.crystal * Math.min(GameConfig.MAX_PLUNDER_FRACTION, share));
                double pd = Math.floor(defenderStored.deuterium * Math.min(GameConfig.MAX_PLUNDER_FRACTION, share));
                out.plunder = new Cost(pm, pc, pd);
            }
        }

        // ---- moon roll (seeded; does NOT affect the battle) ----
        if (defenderIsPlanet && out.debris.structurePoints() > 0 && moonRng != null) {
            double prob = Math.min(GameConfig.MOON_CHANCE_CAP,
                    out.debris.structurePoints() * GameConfig.MOON_CHANCE_PER_DEBRIS);
            out.moonCreated = moonRng.nextDouble() < prob;
        }

        return out;
    }

    // ------------------------------------------------------------------ helpers

    private static Map<Ids.ShipType, Integer> normalizeShips(Map<Ids.ShipType, Integer> in) {
        Map<Ids.ShipType, Integer> m = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        if (in != null) {
            for (Map.Entry<Ids.ShipType, Integer> e : in.entrySet()) {
                if (e.getValue() != null && e.getValue() > 0) m.put(e.getKey(), e.getValue());
            }
        }
        return m;
    }

    private static Map<Ids.DefenseType, Integer> normalizeDefenses(Map<Ids.DefenseType, Integer> in) {
        Map<Ids.DefenseType, Integer> m = new EnumMap<Ids.DefenseType, Integer>(Ids.DefenseType.class);
        if (in != null) {
            for (Map.Entry<Ids.DefenseType, Integer> e : in.entrySet()) {
                if (e.getValue() != null && e.getValue() > 0) m.put(e.getKey(), e.getValue());
            }
        }
        return m;
    }

    private static List<Stack> buildShipStacks(Map<Ids.ShipType, Integer> ships, Profile p, Catalog cat) {
        List<Stack> list = new ArrayList<Stack>();
        for (Map.Entry<Ids.ShipType, Integer> e : ships.entrySet()) {
            ShipDef d = cat.ship(e.getKey());
            double atk = d.weapon * (1.0 + 0.1 * p.weaponsTech) * p.combatBonus;
            double shd = d.shield * (1.0 + 0.1 * p.shieldTech);
            double hul = d.hull * (1.0 + 0.1 * p.armourTech);
            list.add(new Stack(e.getKey(), d, e.getValue(), atk, shd, hul));
        }
        return list;
    }

    private static List<Stack> buildDefenderStacks(Map<Ids.ShipType, Integer> ships,
                                                   Map<Ids.DefenseType, Integer> defenses,
                                                   Profile p, Catalog cat) {
        List<Stack> list = buildShipStacks(ships, p, cat);
        for (Map.Entry<Ids.DefenseType, Integer> e : defenses.entrySet()) {
            DefenseDef d = cat.defense(e.getKey());
            double atk = d.weapon * (1.0 + 0.1 * p.weaponsTech) * p.combatBonus;
            double shd = d.shield * (1.0 + 0.1 * p.shieldTech);
            double hul = d.hull * (1.0 + 0.1 * p.armourTech);
            list.add(new Stack(e.getKey(), d, e.getValue(), atk, shd, hul));
        }
        return list;
    }

    private static int totalUnits(List<Stack> side) {
        int n = 0;
        for (Stack s : side) n += s.count;
        return n;
    }

    /** Total damage a firing side deals this round, including expected-value rapid fire. */
    private static double sideDamage(List<Stack> firing, List<Stack> enemy) {
        int enemyTotal = totalUnits(enemy);
        if (enemyTotal == 0) return 0;
        double dmg = 0;
        for (Stack f : firing) {
            if (f.count == 0) continue;
            double mult = rapidMult(f, enemy, enemyTotal);
            dmg += f.count * f.attack * mult;
        }
        return dmg;
    }

    private static double rapidMult(Stack f, List<Stack> enemy, int enemyTotal) {
        if (f.sdef == null) return 1.0; // defenses have no rapid fire
        double sum = 0;
        for (Stack e : enemy) {
            if (e.count == 0) continue;
            int rf;
            if (e.isDefense) {
                Integer v = f.sdef.rapidFireVsDefense.get(e.def);
                rf = v == null ? 1 : v;
            } else {
                Integer v = f.sdef.rapidFireVsShips.get(e.ship);
                rf = v == null ? 1 : v;
            }
            if (rf > 1) {
                sum += ((double) e.count / enemyTotal) * (rf - 1);
            }
        }
        return 1.0 + sum;
    }

    /** Distribute total damage across enemy stacks proportional to each stack's hull share. */
    private static void distribute(List<Stack> enemy, double damage) {
        if (damage <= 0) return;
        double totalHull = 0;
        for (Stack s : enemy) totalHull += s.count * s.hull;
        if (totalHull <= 0) return;
        for (Stack s : enemy) {
            if (s.count == 0) continue;
            double stackHull = s.count * s.hull;
            double dmgToStack = damage * (stackHull / totalHull);
            double shieldAbsorbed = s.count * s.shield;   // shields reset each round
            double hullDamage = Math.max(0, dmgToStack - shieldAbsorbed);
            int destroyed = (int) Math.floor(hullDamage / s.hull);
            if (destroyed > s.count) destroyed = s.count;
            s.count -= destroyed;
        }
    }

    private static Map<Ids.ShipType, Integer> collectShips(List<Stack> side) {
        Map<Ids.ShipType, Integer> m = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        for (Stack s : side) {
            if (!s.isDefense && s.count > 0) m.put(s.ship, s.count);
        }
        return m;
    }

    private static Map<Ids.DefenseType, Integer> collectDefenses(List<Stack> side) {
        Map<Ids.DefenseType, Integer> m = new EnumMap<Ids.DefenseType, Integer>(Ids.DefenseType.class);
        for (Stack s : side) {
            if (s.isDefense && s.count > 0) m.put(s.def, s.count);
        }
        return m;
    }

    private static Map<Ids.ShipType, Integer> diffShips(Map<Ids.ShipType, Integer> start,
                                                        Map<Ids.ShipType, Integer> end) {
        Map<Ids.ShipType, Integer> m = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        for (Map.Entry<Ids.ShipType, Integer> e : start.entrySet()) {
            int lost = e.getValue() - (end.containsKey(e.getKey()) ? end.get(e.getKey()) : 0);
            if (lost > 0) m.put(e.getKey(), lost);
        }
        return m;
    }

    private static Map<Ids.DefenseType, Integer> diffDefenses(Map<Ids.DefenseType, Integer> start,
                                                              Map<Ids.DefenseType, Integer> end) {
        Map<Ids.DefenseType, Integer> m = new EnumMap<Ids.DefenseType, Integer>(Ids.DefenseType.class);
        for (Map.Entry<Ids.DefenseType, Integer> e : start.entrySet()) {
            int lost = e.getValue() - (end.containsKey(e.getKey()) ? end.get(e.getKey()) : 0);
            if (lost > 0) m.put(e.getKey(), lost);
        }
        return m;
    }

    private static double lostMetal(Catalog cat, Map<Ids.ShipType, Integer> losses) {
        double m = 0;
        for (Map.Entry<Ids.ShipType, Integer> e : losses.entrySet()) {
            m += cat.ship(e.getKey()).cost.metal * e.getValue();
        }
        return m;
    }

    private static double lostCrystal(Catalog cat, Map<Ids.ShipType, Integer> losses) {
        double c = 0;
        for (Map.Entry<Ids.ShipType, Integer> e : losses.entrySet()) {
            c += cat.ship(e.getKey()).cost.crystal * e.getValue();
        }
        return c;
    }

    private static String fmt(double v) {
        return String.format("%,d dmg", (long) v);
    }
}
