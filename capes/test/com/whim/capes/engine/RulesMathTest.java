package com.whim.capes.engine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.whim.capes.model.Character;
import com.whim.capes.model.Ability;
import com.whim.capes.model.AbilityKind;
import com.whim.capes.model.Drive;
import com.whim.capes.model.DriveType;
import com.whim.capes.model.Conflict;
import com.whim.capes.model.ConflictType;
import com.whim.capes.model.Die;

/**
 * Phase 1 smoke tests for the decoupled rules core and key model invariants.
 * These exercise the arithmetic that later phases build on, verifying the
 * design is sound before gameplay logic is layered on.
 */
public class RulesMathTest {

    @Test public void overdrawn() {
        assertTrue(RulesMath.isOverdrawn(4, 2));
        assertFalse(RulesMath.isOverdrawn(2, 2));
        assertFalse(RulesMath.isOverdrawn(1, 2));
    }

    @Test public void evenSplitSpreadsRemainderToLargestParts() {
        assertArrayEquals(new int[]{3, 2}, RulesMath.evenSplit(5, 2));   // p.37 example
        assertArrayEquals(new int[]{3, 2, 2}, RulesMath.evenSplit(7, 3));
        assertArrayEquals(new int[]{2, 2}, RulesMath.evenSplit(4, 2));
    }

    @Test(expected = IllegalMoveException.class)
    public void cannotSplitBelowOnePerDie() {
        RulesMath.evenSplit(2, 3);
    }

    @Test public void inspirationPairingDifferencesAndExcess() {
        // Metamorph 6,4,2 vs Doctor Phantom 4,2 (p.31): resolver 2,2 and excess 2; opposing none.
        RulesMath.InspirationSplit s = RulesMath.pairInspirations(
                Arrays.asList(6, 4, 2), Arrays.asList(4, 2));
        assertEquals(Arrays.asList(2, 2, 2), s.resolverInspirations);
        assertTrue(s.opposingInspirations.isEmpty());
    }

    @Test public void inspirationPairingYieldsOpposingWhenWinnerDieSmaller() {
        // Winning side totals more but a low winning die matched vs a high losing die -> opposing Inspiration.
        RulesMath.InspirationSplit s = RulesMath.pairInspirations(
                Arrays.asList(6, 1), Arrays.asList(3, 3));
        // pairs: 6-3=+3 (resolver), 1-3=-2 (opposing)
        assertEquals(Arrays.asList(3), s.resolverInspirations);
        assertEquals(Arrays.asList(2), s.opposingInspirations);
    }

    @Test public void deadlockOnlyWhenTiedMaxedAndNoDebt() {
        assertTrue(RulesMath.isDeadlocked(true, false, true));
        assertFalse(RulesMath.isDeadlocked(true, true, true));   // debt available
        assertFalse(RulesMath.isDeadlocked(true, false, false)); // dice not maxed
        assertFalse(RulesMath.isDeadlocked(false, false, true)); // not tied
    }

    @Test public void dieRollAndRevert() {
        Die d = new Die(2);
        d.placeRoll(1);
        assertEquals(1, d.value());
        d.revert();
        assertEquals(2, d.value());        // "turn the die back" (p.38)
    }

    @Test public void controlGoesToHighestTotalElseTie() {
        Conflict c = new Conflict("c", "Clobbering", ConflictType.GOAL, "p1");
        c.sides().get(0).dice().get(0).set(4);
        c.sides().get(1).dice().get(0).set(2);
        assertEquals(c.sides().get(0), c.controllingSide());
        c.sides().get(1).dice().get(0).set(4);
        assertNull(c.controllingSide());   // tie -> nobody Controls
        assertTrue(c.isTied());
    }

    @Test public void detailedDrivesMustTotalNine() {
        Character hero = new Character("h", "Metamorph", true);
        addTriad(hero);
        hero.drives().add(new Drive(DriveType.LOVE, 3));
        hero.drives().add(new Drive(DriveType.HOPE, 2));
        hero.drives().add(new Drive(DriveType.DUTY, 2));
        hero.drives().add(new Drive(DriveType.JUSTICE, 1));
        hero.drives().add(new Drive(DriveType.TRUTH, 1));
        assertNull(hero.validateDrives());       // totals 9
        hero.drive(DriveType.LOVE).setStrength(5);
        assertTrue(hero.validateDrives().contains("total")); // now 11
    }

    @Test public void abilityShapeRequiresThreeToFivePerColumnNumberedFromOne() {
        Character hero = new Character("h", "Brick", true);
        addTriad(hero);
        assertNull(hero.validateAbilityShape());
    }

    /** Minimal valid 3/3/3 ability triad for a super character. */
    private void addTriad(Character hero) {
        for (int i = 1; i <= 3; i++) hero.abilities().add(new Ability("Pow" + i, AbilityKind.POWER, i, true));
        for (int i = 1; i <= 3; i++) hero.abilities().add(new Ability("Att" + i, AbilityKind.ATTITUDE, i, false));
        for (int i = 1; i <= 3; i++) hero.abilities().add(new Ability("Sty" + i, AbilityKind.STYLE, i, false));
    }
}
