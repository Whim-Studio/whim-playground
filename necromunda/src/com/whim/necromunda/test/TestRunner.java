package com.whim.necromunda.test;

import com.whim.necromunda.engine.Dice;
import com.whim.necromunda.engine.rules.ArmourSave;
import com.whim.necromunda.engine.rules.InjuryResolver;
import com.whim.necromunda.engine.rules.InjuryResult;
import com.whim.necromunda.engine.rules.MeleeContest;
import com.whim.necromunda.engine.rules.RangedToHit;
import com.whim.necromunda.engine.rules.WoundTable;
import com.whim.necromunda.model.Fighter;
import com.whim.necromunda.model.FighterStatus;
import com.whim.necromunda.model.FighterType;
import com.whim.necromunda.model.Stat;
import com.whim.necromunda.model.StatLine;

/**
 * Milestone 1 stat-math tests — the to-hit / to-wound / armour-save / injury /
 * melee tables plus the StatLine modifier layer and dice determinism. Runs under
 * plain {@code java} with no test framework.
 */
public final class TestRunner {

    public static void main(String[] args) {
        Assert a = new Assert();

        a.section("Ranged to-hit (7 - BS)");
        a.equalsInt("BS3 base target", 4, RangedToHit.baseTarget(3));
        a.equalsInt("BS4 base target", 3, RangedToHit.baseTarget(4));
        a.equalsInt("BS5 base target", 2, RangedToHit.baseTarget(5));
        a.equalsInt("BS6 clamps to 2", 2, RangedToHit.baseTarget(6));
        a.equalsInt("BS1 base target", 6, RangedToHit.baseTarget(1));
        a.equalsInt("BS4 +1 short range easier", 2, RangedToHit.modifiedTarget(4, 1));
        a.equalsInt("BS4 -1 long range harder", 4, RangedToHit.modifiedTarget(4, -1));
        a.equalsInt("BS4 -2 hard cover harder", 5, RangedToHit.modifiedTarget(4, -2));
        a.that("roll 4 hits target 3", RangedToHit.hits(4, 3));
        a.that("roll 2 misses target 3", !RangedToHit.hits(2, 3));
        a.that("natural 6 always hits", RangedToHit.hits(6, 7));

        a.section("To-wound (S vs T table)");
        a.equalsInt("S8 vs T4 (>=2T) -> 2+", 2, WoundTable.target(8, 4));
        a.equalsInt("S8 vs T3 (>=2T) -> 2+", 2, WoundTable.target(8, 3));
        a.equalsInt("S4 vs T3 (S>T) -> 3+", 3, WoundTable.target(4, 3));
        a.equalsInt("S3 vs T3 (S=T) -> 4+", 4, WoundTable.target(3, 3));
        a.equalsInt("S3 vs T4 (S=T-1) -> 5+", 5, WoundTable.target(3, 4));
        a.equalsInt("S2 vs T4 (S<=T-2) -> 6+", 6, WoundTable.target(2, 4));
        a.equalsInt("S1 vs T6 (far below) -> 6+", 6, WoundTable.target(1, 6));
        a.that("natural 6 always wounds", WoundTable.wounds(6, 6));
        a.that("roll 5 fails 6+ wound", !WoundTable.wounds(5, 6));

        a.section("Armour saves");
        a.equalsInt("Mesh (5+) no AP -> 5", 5, ArmourSave.target(5, 0));
        a.equalsInt("Mesh (5+) AP1 -> 6", 6, ArmourSave.target(5, 1));
        a.equalsInt("Mesh (5+) AP2 -> 7", 7, ArmourSave.target(5, 2));
        a.equalsInt("Carapace (4+) AP1 -> 5", 5, ArmourSave.target(4, 1));
        a.equalsInt("Unarmoured -> 7", 7, ArmourSave.target(7, 0));
        a.that("target 5 can save", ArmourSave.saves(5, 5));
        a.that("target 7 cannot save", !ArmourSave.saves(6, 7));
        a.that("roll 5 saves target 5", ArmourSave.saves(5, 5));
        a.that("roll 4 fails target 5", !ArmourSave.saves(4, 5));
        a.that("roll 6 cannot save when target 7", !ArmourSave.saves(6, 7));

        a.section("Injury classification (1-2 / 3-4 / 5-6)");
        a.equals("roll 1 -> Flesh Wound", InjuryResult.FLESH_WOUND, InjuryResolver.classify(1));
        a.equals("roll 2 -> Flesh Wound", InjuryResult.FLESH_WOUND, InjuryResolver.classify(2));
        a.equals("roll 3 -> Down", InjuryResult.DOWN, InjuryResolver.classify(3));
        a.equals("roll 4 -> Down", InjuryResult.DOWN, InjuryResolver.classify(4));
        a.equals("roll 5 -> Out of Action", InjuryResult.OUT_OF_ACTION, InjuryResolver.classify(5));
        a.equals("roll 6 -> Out of Action", InjuryResult.OUT_OF_ACTION, InjuryResolver.classify(6));

        a.section("Injury application");
        Fighter twoWound = new Fighter("t1", "Tank", FighterType.CHAMPION,
                StatLine.of(4, 3, 3, 4, 4, 2, 3, 1, 7));
        InjuryResult first = InjuryResolver.applyWound(twoWound, 5);
        a.that("2W fighter absorbs 1st wound (no injury)", first == null);
        a.equalsInt("...wounds now 1", 1, twoWound.woundsRemaining());
        a.that("...still ACTIVE", twoWound.status() == FighterStatus.ACTIVE);

        Fighter fw = new Fighter("t2", "Graze", FighterType.GANGER,
                StatLine.of(4, 3, 3, 3, 3, 1, 3, 1, 7));
        InjuryResolver.applyResult(fw, InjuryResult.FLESH_WOUND);
        a.that("Flesh Wound keeps fighter up", fw.status() == FighterStatus.ACTIVE);
        a.equalsInt("Flesh Wound applies -1 WS", 2, fw.stat(Stat.WS));

        Fighter dn = new Fighter("t3", "Drop", FighterType.GANGER,
                StatLine.of(4, 3, 3, 3, 3, 1, 3, 1, 7));
        InjuryResolver.applyResult(dn, InjuryResult.DOWN);
        a.that("Down sets DOWN status", dn.status() == FighterStatus.DOWN);

        Fighter oa = new Fighter("t4", "Goner", FighterType.GANGER,
                StatLine.of(4, 3, 3, 3, 3, 1, 3, 1, 7));
        InjuryResolver.applyResult(oa, InjuryResult.OUT_OF_ACTION);
        a.that("Out of Action removes fighter", oa.status() == FighterStatus.OUT_OF_ACTION);
        a.that("...no longer in play", !oa.status().inPlay());

        a.section("Melee contest");
        a.equalsInt("score = highest die + WS + bonus", 9,
                MeleeContest.score(new int[]{2, 5, 3}, 4, 0));
        int attacker = MeleeContest.score(new int[]{4}, 3, 1); // charger: 4+3+1 = 8
        int defender = MeleeContest.score(new int[]{4}, 3, 0); // 4+3+0 = 7
        a.that("charger's +1 wins the tie of dice",
                MeleeContest.resolve(attacker, defender) == MeleeContest.Outcome.ATTACKER_WINS);
        a.that("higher total defender wins",
                MeleeContest.resolve(6, 9) == MeleeContest.Outcome.DEFENDER_WINS);
        a.that("equal totals draw (parry)",
                MeleeContest.resolve(8, 8) == MeleeContest.Outcome.DRAW);

        a.section("StatLine modifiers");
        StatLine sl = StatLine.of(4, 3, 3, 3, 3, 1, 3, 1, 7);
        a.equalsInt("base WS", 3, sl.effective(Stat.WS));
        sl.modify(Stat.WS, -1);
        a.equalsInt("after -1 flesh wound", 2, sl.effective(Stat.WS));
        sl.modify(Stat.WS, 1);
        a.equalsInt("after +1 advance stacks back", 3, sl.effective(Stat.WS));
        a.equalsInt("base value untouched by modifiers", 3, sl.base(Stat.WS));
        StatLine copy = sl.copy();
        copy.modify(Stat.WS, -2);
        a.equalsInt("copy is independent", 3, sl.effective(Stat.WS));

        a.section("Dice determinism");
        Dice d1 = new Dice(42L);
        Dice d2 = new Dice(42L);
        boolean sameSeq = true;
        for (int i = 0; i < 20; i++) {
            if (d1.d6() != d2.d6()) {
                sameSeq = false;
            }
        }
        a.that("same seed -> same sequence", sameSeq);
        Dice d3 = new Dice(7L);
        boolean inRange = true;
        for (int i = 0; i < 50; i++) {
            int r = d3.d6();
            if (r < 1 || r > 6) {
                inRange = false;
            }
        }
        a.that("d6 always in 1..6", inRange);

        a.finish();
    }
}
