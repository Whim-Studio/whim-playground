package com.whim.coda.engine;

import com.whim.coda.model.Attribute;
import com.whim.coda.model.AttributeSet;
import com.whim.coda.model.CharacterSheet;
import com.whim.coda.model.Edge;
import com.whim.coda.model.Flaw;
import com.whim.coda.model.Reaction;
import com.whim.coda.model.Skill;
import com.whim.coda.model.SkillRank;
import com.whim.coda.model.Species;
import com.whim.coda.data.DataRepository;

import java.util.List;
import java.util.Random;

/**
 * JUnit-free smoke test for the engine package. Run after the full project is
 * assembled: {@code java -cp /tmp/out com.whim.coda.engine.EngineSelfCheck}.
 * Exits non-zero on the first failed assertion.
 */
public final class EngineSelfCheck {

    private static int checks = 0;

    private EngineSelfCheck() {
    }

    public static void main(String[] args) {
        testRollScores();
        testPointBuy();
        testBaseCaps();
        testSpeciesAndDerived();
        testPackageBuilder();
        System.out.println("EngineSelfCheck: ALL " + checks + " checks passed.");
    }

    private static void testRollScores() {
        Random rng = new Random(42L);
        for (int trial = 0; trial < 1000; trial++) {
            List<Integer> kept = AttributeGenerator.rollScores(rng);
            check(kept.size() == 6, "rollScores returns six values");
            for (int i = 1; i < kept.size(); i++) {
                check(kept.get(i - 1) >= kept.get(i), "rollScores ordered high->low");
            }
            for (Integer v : kept) {
                check(v >= 2 && v <= 12, "each kept score in 2..12 (no exploding)");
            }
        }
    }

    private static void testPointBuy() {
        AttributeSet attrs = new AttributeSet();
        // Standard example array 10,9,7,7,5,4 = 42 <= 46.
        int[] arr = {10, 9, 7, 7, 5, 4};
        Attribute[] all = Attribute.values();
        for (int i = 0; i < all.length; i++) {
            attrs.setBase(all[i], arr[i]);
        }
        check(AttributeGenerator.validatePointBuy(attrs), "standard array within budget");

        attrs.setBase(Attribute.STRENGTH, 13); // over cap
        check(!AttributeGenerator.validatePointBuy(attrs), "base over 12 fails point-buy");

        attrs.setBase(Attribute.STRENGTH, 12);
        attrs.setBase(Attribute.AGILITY, 12);
        attrs.setBase(Attribute.INTELLECT, 12);
        attrs.setBase(Attribute.VITALITY, 12); // total now well over 46
        check(!AttributeGenerator.validatePointBuy(attrs), "over-budget total fails point-buy");
    }

    private static void testBaseCaps() {
        AttributeSet attrs = new AttributeSet();
        for (Attribute a : Attribute.values()) {
            attrs.setBase(a, 12);
        }
        check(RulesEngine.validateBaseCaps(attrs), "all 12 passes base caps");
        attrs.setBase(Attribute.PRESENCE, 13);
        check(!RulesEngine.validateBaseCaps(attrs), "base 13 fails base caps");
    }

    private static void testSpeciesAndDerived() {
        CharacterSheet sheet = new CharacterSheet();
        AttributeSet attrs = sheet.getAttributes();
        attrs.setBase(Attribute.STRENGTH, 8);   // mod +1
        attrs.setBase(Attribute.AGILITY, 10);   // mod +2
        attrs.setBase(Attribute.INTELLECT, 6);  // mod 0
        attrs.setBase(Attribute.VITALITY, 10);  // mod +2
        attrs.setBase(Attribute.PRESENCE, 4);   // mod -1
        attrs.setBase(Attribute.PERCEPTION, 8); // mod +1

        Species vulcan = DataRepository.speciesByName("Vulcan"); // INT+1, STR+2, PRE-3
        sheet.setSpecies(vulcan);
        RulesEngine.applySpecies(sheet);
        check(attrs.getSpeciesMod(Attribute.STRENGTH) == 2, "Vulcan STR mod applied");
        check(attrs.getAdjusted(Attribute.STRENGTH) == 10, "Vulcan adjusted STR = 10");

        RulesEngine.recomputeDerived(sheet);
        // adjusted: STR10(+2) AGI10(+2) INT7(0) VIT10(+2) PRE1(-3) PER8(+1)
        check(sheet.getHealth() == 10 + 2, "Health = adj VIT + STR mod");
        check(sheet.getDefense() == 7 + 2, "Defense = 7 + AGI mod");
        check(sheet.getReaction(Reaction.QUICKNESS) == Math.max(1, 2), "Quickness");
        check(sheet.getReaction(Reaction.SAVVY) == Math.max(-3, 1), "Savvy");
        check(sheet.getReaction(Reaction.STAMINA) == Math.max(2, 2), "Stamina");
        check(sheet.getReaction(Reaction.WILLPOWER) == Math.max(0, 2), "Willpower");
        check(sheet.getCourage() == 3, "Vulcan courage = 3");
        check(sheet.getRenown() == 0, "Renown = 0");

        Species bajoran = DataRepository.speciesByName("Bajoran");
        sheet.setSpecies(bajoran);
        RulesEngine.applySpecies(sheet);
        RulesEngine.recomputeDerived(sheet);
        check(sheet.getCourage() == 4, "Bajoran Pagh courage = 4");
    }

    private static void testPackageBuilder() {
        CharacterSheet sheet = new CharacterSheet();
        Skill marksman = new Skill("Ranged Combat", Attribute.AGILITY);

        PackageBuilder.addSkill(sheet, new SkillRank(marksman, 2, null));
        check(sheet.getSkills().size() == 1, "first skill added");
        check(!PackageBuilder.canAddSkill(sheet, marksman, null), "dup base skill (no specialty) blocked");

        boolean threw = false;
        try {
            PackageBuilder.addSkill(sheet, new SkillRank(marksman, 3, ""));
        } catch (IllegalArgumentException ex) {
            threw = true;
        }
        check(threw, "dup base skill throws");

        check(PackageBuilder.canAddSkill(sheet, marksman, "Phaser"), "distinct specialty allowed");
        PackageBuilder.addSkill(sheet, new SkillRank(marksman, 1, "Phaser"));
        check(sheet.getSkills().size() == 2, "specialty variant added");
        check(!PackageBuilder.canAddSkill(sheet, marksman, "phaser"), "same specialty (case-insensitive) blocked");

        PackageBuilder.addEdge(sheet, new Edge("Bold", "+ courage"));
        PackageBuilder.addFlaw(sheet, new Flaw("Code of Honor", "must keep word"));
        check(sheet.getEdges().size() == 1 && sheet.getFlaws().size() == 1, "edge & flaw added");
    }

    private static void check(boolean cond, String msg) {
        checks++;
        if (!cond) {
            System.err.println("FAILED: " + msg);
            throw new AssertionError(msg);
        }
    }
}
