package com.whim.swd6.engine;

import com.whim.swd6.api.Combatant;
import com.whim.swd6.api.DamageResult;
import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.DifficultyTier;
import com.whim.swd6.api.RollResult;
import com.whim.swd6.api.WoundLevel;

import java.util.List;
import java.util.Random;

/**
 * Runs the engine in isolation: rolls several codes, demonstrates an exploding Wild
 * Die and a complication, resolves a sample damage exchange, runs a tiny two-sided
 * encounter to completion, and statistically sanity-checks 3D rolls.
 *
 * Owned by Task 2 (engine).
 */
public final class EngineSelfCheck {

    public static void main(String[] args) {
        Random rng = new Random(20260704L);
        Dice dice = new Dice(rng);
        D6Engine engine = new D6Engine(dice);

        line();
        System.out.println("STAR WARS D6 — ENGINE SELF CHECK");
        line();

        // 1. A handful of plain rolls.
        System.out.println("\n[1] Basic rolls (Wild Die on):");
        String[] codes = {"0D", "1D", "3D+2", "4D", "6D+1"};
        for (String cs : codes) {
            DiceCode code = DiceCode.parse(cs);
            RollResult r = engine.roll(code, true, DifficultyTier.MODERATE);
            System.out.println("  " + pad(cs, 6) + " -> " + describe(r)
                    + "  vs Moderate(" + r.getTarget() + ") = "
                    + (r.isSuccess() ? "SUCCESS" : "fail"));
        }

        // 2. Force an exploding Wild Die and a complication.
        System.out.println("\n[2] Exploding Wild Die (search for a 6-chain):");
        RollResult exploded = rollUntil(engine, DiceCode.parse("3D"), true, false);
        System.out.println("  " + describe(exploded));

        System.out.println("\n[3] Complication (Wild Die shows 1):");
        RollResult complication = rollUntil(engine, DiceCode.parse("3D"), false, true);
        System.out.println("  " + describe(complication)
                + "   (the 1 and the highest normal die were subtracted)");

        // 4. Damage exchange.
        System.out.println("\n[4] Damage exchange: 5D blaster vs 3D+2 resistance:");
        DamageResult dmg = engine.resolveDamage(DiceCode.parse("5D"), DiceCode.parse("3D+2"));
        System.out.println("  damage : " + describe(dmg.getDamageRoll()));
        System.out.println("  resist : " + describe(dmg.getResistRoll()));
        System.out.println("  margin : " + dmg.getMargin() + "  ->  " + dmg.getInflicted().display());

        // 5. Tiny encounter to completion.
        System.out.println("\n[5] Encounter: 1 hero vs 1 thug, to completion:");
        runEncounter(engine);

        // 6. Statistical sanity check.
        System.out.println("\n[6] Statistics over 10000 rolls of 3D:");
        statistics(rng);

        line();
        System.out.println("SELF CHECK COMPLETE");
        line();
    }

    private static void runEncounter(D6Engine engine) {
        Combatant hero = new Combatant();
        hero.setName("Hero");
        hero.setPlayerCharacter(false); // NPC-style quick stats for a self-contained demo
        hero.setAttackCode(DiceCode.parse("5D"));
        hero.setDamageCode(DiceCode.parse("5D"));
        hero.setResistCode(DiceCode.parse("3D"));

        Combatant thug = new Combatant();
        thug.setName("Thug");
        thug.setPlayerCharacter(true); // opposite side so isOver() can resolve PC vs non-PC
        thug.setAttackCode(DiceCode.parse("4D"));
        thug.setDamageCode(DiceCode.parse("4D"));
        thug.setResistCode(DiceCode.parse("3D"));

        Encounter enc = new Encounter(engine);
        enc.add(hero);
        enc.add(thug);
        enc.rollInitiative();

        System.out.println("  Initiative order:");
        for (Combatant c : enc.order()) {
            System.out.println("    " + pad(c.getName(), 6) + " init " + c.getInitiative());
        }

        int safety = 0;
        while (!enc.isOver() && safety++ < 200) {
            Combatant actor = enc.current();
            if (actor == null) {
                enc.next();
                continue;
            }
            Combatant foe = other(enc, actor);
            if (foe == null) {
                break;
            }
            DamageResult hit = engine.resolveDamage(actor.getDamageCode(), foe.getResistCode());
            WoundLevel before = foe.getWoundLevel();
            WoundLevel after = enc.applyHit(foe, hit);
            System.out.println("  R" + enc.round() + " " + pad(actor.getName(), 6)
                    + " hits " + pad(foe.getName(), 6)
                    + " margin " + pad(String.valueOf(hit.getMargin()), 4)
                    + " " + before.display() + " -> " + after.display());
            enc.next();
        }
        Combatant winner = null;
        for (Combatant c : enc.order()) {
            if (!c.getWoundLevel().incapacitatedOrWorse()) {
                winner = c;
            }
        }
        System.out.println("  Encounter over after round " + enc.round()
                + "; winner: " + (winner == null ? "nobody" : winner.getName()));
    }

    private static Combatant other(Encounter enc, Combatant actor) {
        for (Combatant c : enc.order()) {
            if (c != actor && !c.getWoundLevel().incapacitatedOrWorse()) {
                return c;
            }
        }
        return null;
    }

    private static void statistics(Random rng) {
        // Plain 3D (no Wild Die) should average ~10.5.
        D6Engine plain = new D6Engine(new Random(rng.nextLong()));
        DiceCode threeD = DiceCode.parse("3D");
        long sum = 0;
        int n = 10000;
        for (int i = 0; i < n; i++) {
            sum += plain.roll(threeD, false, -1).getTotal();
        }
        double mean = sum / (double) n;
        System.out.println("  plain 3D mean total = " + fmt(mean) + " (expect ~10.5)");

        // With the Wild Die on, count explosions and complications.
        D6Engine wild = new D6Engine(new Random(rng.nextLong()));
        int explosions = 0;
        int complications = 0;
        for (int i = 0; i < n; i++) {
            RollResult r = wild.roll(threeD, true, -1);
            if (r.isWildExploded()) {
                explosions++;
            }
            if (r.isComplication()) {
                complications++;
            }
        }
        System.out.println("  wild 3D explosions  = " + explosions + " / " + n
                + " (expect ~1/6)");
        System.out.println("  wild 3D complications = " + complications + " / " + n
                + " (expect ~1/6)");
    }

    /** Roll until the Wild Die either exploded or produced a complication, as asked. */
    private static RollResult rollUntil(D6Engine engine, DiceCode code,
                                        boolean wantExploded, boolean wantComplication) {
        for (int i = 0; i < 10000; i++) {
            RollResult r = engine.roll(code, true, -1);
            if (wantExploded && r.isWildExploded()) {
                return r;
            }
            if (wantComplication && r.isComplication()) {
                return r;
            }
        }
        // Extremely unlikely; return a final roll so the demo still prints.
        return engine.roll(code, true, -1);
    }

    private static String describe(RollResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("normal=").append(r.getNormalDice());
        sb.append(" wild=").append(r.getWildDieRolls());
        if (r.getPips() > 0) {
            sb.append(" +").append(r.getPips()).append("pip");
        }
        sb.append(" total=").append(r.getTotal());
        if (r.isWildExploded()) {
            sb.append(" [EXPLODED]");
        }
        if (r.isComplication()) {
            sb.append(" [COMPLICATION]");
        }
        return sb.toString();
    }

    private static String fmt(double d) {
        long scaled = Math.round(d * 100.0);
        return (scaled / 100) + "." + pad2(Math.abs(scaled % 100));
    }

    private static String pad2(long v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    private static String pad(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static void line() {
        System.out.println("======================================================");
    }

    private EngineSelfCheck() {
    }
}
