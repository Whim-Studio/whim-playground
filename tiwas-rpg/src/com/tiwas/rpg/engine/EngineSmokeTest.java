package com.tiwas.rpg.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.tiwas.rpg.domain.AttributeCode;
import com.tiwas.rpg.domain.Character;
import com.tiwas.rpg.domain.Skill;

/**
 * Self-contained smoke check (no JUnit). Run:
 *   java -cp tiwas-rpg/out com.tiwas.rpg.engine.EngineSmokeTest
 */
public final class EngineSmokeTest {
    public static void main(String[] args) {
        seededGeneration();
        knownOverflow();
        marginAndDoubles();
        System.out.println("ALL ENGINE SMOKE TESTS PASSED");
    }

    private static void seededGeneration() {
        Character c = new CharacterGenerator(new Dice(12345L)).generate("Seedling");
        check("name", "Seedling".equals(c.getName()));
        check("24 skills", c.getSkills().size() == 24);
        check("generalXP 0", c.getGeneralXP() == 0);
        for (AttributeCode a : AttributeCode.values()) {
            int v = c.getAttribute(a);
            check("attr range " + a.code(), v >= 1 && v <= 100);
            Skill s = c.getSkill(a.tier1Skill());
            check("skill exists " + a.tier1Skill(), s != null);
            check("skill value " + a.tier1Skill(), s.getValue() == v / 2);
        }
        check("HP at max", c.getCurrentHP() == c.getMaxHP());
        check("PE at max", c.getCurrentPE() == c.getMaxPhysicalEnergy());
        check("MP at max", c.getCurrentMP() == c.getMaxMP());
    }

    // Force a fixed roll so the overflow path is deterministic without knowing the seed.
    private static void knownOverflow() {
        Dice fixed = new Dice(new Random() {
            public int nextInt(int bound) { return 79; } // d100 -> 80
        });
        Character c = new Character("Bruiser");
        c.setAttribute(AttributeCode.BPP, 60);
        c.setCurrentPE(30);
        c.setCurrentHP(100);

        List<String> formula = new ArrayList<String>();
        formula.add("bpp");
        Skill body = new Skill("Might", 1, formula, 50);

        ActionResult r = new ActionResolver(fixed).resolve(c, body);
        check("roll 80", r.getRoll() == 80);
        check("cost 80", r.getCost() == 80);
        check("overflow 50", r.getOverflowDamage() == 50);
        check("used PE not mind", !r.isUsedMind());
        // pool emptied then recovered energyRegen/2; HP took the 50 overflow.
        check("hp dropped by overflow", r.getNewHP() == 100 - 50);
        check("pool after recover", r.getNewPoolValue() == Math.min(0 + c.getEnergyRegen() / 2, c.getMaxPhysicalEnergy()));
    }

    private static void marginAndDoubles() {
        Dice fixed = new Dice(new Random() {
            public int nextInt(int bound) { return 21; } // d100 -> 22 (doubles)
        });
        Character c = new Character("Adept");
        c.setAttribute(AttributeCode.MPP, 90);
        c.setCurrentMP(80);

        List<String> formula = new ArrayList<String>();
        formula.add("mpp");
        Skill mind = new Skill("Cunning", 1, formula, 30);

        ActionResult r = new ActionResolver(fixed).resolve(c, mind);
        check("mind pool used", r.isUsedMind());
        check("failure (22 > 30? no, 22<=30 success)", r.isSuccess());
        check("doubles flagged", r.isDoubles());
        // success so no advanced unlock (unlock only on failure+doubles)
        check("no unlock on success", !r.isUnlockedAdvancedSkill());
        check("margin (30-22)/10=0", r.getMargin() == 0);
    }

    private static void check(String label, boolean cond) {
        if (!cond) {
            throw new AssertionError("FAILED: " + label);
        }
    }
}
