package com.whim.capes.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.whim.capes.model.Ability;
import com.whim.capes.model.AbilityKind;
import com.whim.capes.model.Character;
import com.whim.capes.model.Conflict;
import com.whim.capes.model.ConflictSide;
import com.whim.capes.model.ConflictType;
import com.whim.capes.model.Die;
import com.whim.capes.model.Drive;
import com.whim.capes.model.DriveType;
import com.whim.capes.model.GameState;
import com.whim.capes.model.Page;
import com.whim.capes.model.Player;
import com.whim.capes.model.Scene;

/** Phase 3 tests: the stateful loop, Stake limits, Ability costs, Overdraw, Gloat, and the Resolve token math. */
public class GameEngineTest {
    private GameState state;
    private Player p1, p2;
    private Character metamorph, phantom;

    @Before public void setUp() {
        state = new GameState();
        p1 = new Player("p1", "Mimi");
        p2 = new Player("p2", "Phil");
        state.players().add(p1);
        state.players().add(p2);

        metamorph = superChar("m", "Metamorph", DriveType.LOVE);
        phantom = superChar("d", "Doctor Phantom", DriveType.DESPAIR);
        state.roster().add(metamorph);
        state.roster().add(phantom);
        p1.controlledCharacterIds().add("m");
        p2.controlledCharacterIds().add("d");
    }

    private Character superChar(String id, String name, DriveType main) {
        Character c = new Character(id, name, true);
        for (int i = 1; i <= 3; i++) c.abilities().add(new Ability(name + "-Pow" + i, AbilityKind.POWER, i, true));
        for (int i = 1; i <= 3; i++) c.abilities().add(new Ability(name + "-Att" + i, AbilityKind.ATTITUDE, i, false));
        for (int i = 1; i <= 3; i++) c.abilities().add(new Ability(name + "-Sty" + i, AbilityKind.STYLE, i, false));
        c.drives().add(new Drive(main, 5));
        c.drives().add(new Drive(DriveType.HOPE, 1));
        c.drives().add(new Drive(DriveType.DUTY, 1));
        c.drives().add(new Drive(DriveType.JUSTICE, 1));
        c.drives().add(new Drive(DriveType.TRUTH, 1));
        return c;
    }

    /** Reproduces the p.31 worked example end-to-end and checks every token movement. */
    @Test public void resolveMatchesRulebookExample() {
        GameEngine eng = new GameEngine(state, new Roller.ScriptedRoller(1));
        Scene scene = eng.declareScene("p1", "Peggy Sue and the Volcano");
        eng.startPage(scene);
        Conflict c = eng.addConflict(scene, "p1", "Peggy Sue falls toward the active volcano", ConflictType.EVENT, true);
        eng.claim(scene, c, 0, "p1", true);
        eng.claim(scene, c, 1, "p2", true);

        // Give each their Debt then Stake (Metamorph 3 Love, Doctor Phantom 2 Despair).
        metamorph.drive(DriveType.LOVE).addDebt(3);
        phantom.drive(DriveType.DESPAIR).addDebt(2);
        eng.stake(c, 0, metamorph, DriveType.LOVE, 3);
        eng.stake(c, 1, phantom, DriveType.DESPAIR, 2);

        // End-of-page dice: Metamorph 6,4,2 vs Doctor Phantom 4,2 (set directly for a deterministic check).
        setDice(c.sides().get(0), 6, 4, 2);
        setDice(c.sides().get(1), 4, 2);

        ResolveResult r = eng.resolve(scene, c, 0, "p1");

        assertEquals(ResolveResult.Kind.NORMAL, r.kind);
        // Doctor Phantom takes back 4 (doubled) Debt to Despair.
        assertEquals(4, phantom.drive(DriveType.DESPAIR).debt());
        assertEquals(4, r.debtReturned);
        // Metamorph gives his 3 Staked Debt to Phil as Story Tokens.
        assertEquals(3, p2.storyTokens());
        assertEquals(3, r.storyTokensAwarded);
        assertEquals(0, r.storyTokensDiscarded);
        // Winner keeps none.
        assertEquals(0, p1.storyTokens());
        // Inspirations: 6-4=2, 4-2=2, excess 2 -> three 2-point Inspirations for the Resolver, none opposing.
        assertEquals(3, p1.inspirations().size());
        assertEquals(0, p2.inspirations().size());
        int sum = 0;
        for (com.whim.capes.model.Inspiration ins : p1.inspirations()) sum += ins.value();
        assertEquals(6, sum);
    }

    @Test public void stakeRejectsSecondDriveAndOverStrength() {
        GameEngine eng = new GameEngine(state, new Roller.ScriptedRoller(1));
        Scene scene = eng.declareScene("p1", "S");
        eng.startPage(scene);
        Conflict c = eng.addConflict(scene, "p1", "Goal", ConflictType.GOAL, true);
        eng.claim(scene, c, 0, "p1", true);
        metamorph.drive(DriveType.LOVE).addDebt(5);
        metamorph.drive(DriveType.HOPE).addDebt(1);
        eng.stake(c, 0, metamorph, DriveType.LOVE, 2);
        // second Drive on same Conflict -> illegal
        try { eng.stake(c, 0, metamorph, DriveType.HOPE, 1); fail("expected"); }
        catch (IllegalMoveException ok) { assertTrue(ok.getMessage().contains("one Drive")); }
        // exceeding Strength (LOVE strength 5, already 2, +4) -> illegal
        try { eng.stake(c, 0, metamorph, DriveType.LOVE, 4); fail("expected"); }
        catch (IllegalMoveException ok) { assertTrue(ok.getMessage().contains("Strength")); }
    }

    @Test public void superAbilityEarnsDebtAndBlocksSecondUseThisPage() {
        GameEngine eng = new GameEngine(state, new Roller.ScriptedRoller(5, 4));
        Scene scene = eng.declareScene("p1", "S");
        Page page = eng.startPage(scene);
        Conflict c = eng.addConflict(scene, "p1", "Clobber", ConflictType.GOAL, true);
        eng.claim(scene, c, 0, "p1", true);
        Ability pow3 = metamorph.abilitiesOfKind(AbilityKind.POWER).get(2); // score 3
        int rolled = eng.useAbilityRoll(scene, page, metamorph, pow3, c, 0, 0, DriveType.LOVE);
        assertEquals(5, rolled);
        assertEquals(5, c.sides().get(0).dice().get(0).value());
        assertEquals(1, metamorph.drive(DriveType.LOVE).debt()); // earned 1 Debt
        // second use of same super Ability this Page -> illegal
        try { eng.useAbilityRoll(scene, page, metamorph, pow3, c, 0, 0, DriveType.LOVE); fail("expected"); }
        catch (IllegalMoveException ok) { assertTrue(ok.getMessage().contains("this Page")); }
    }

    @Test public void abilityScoreMustReachDieValue() {
        GameEngine eng = new GameEngine(state, new Roller.ScriptedRoller(6));
        Scene scene = eng.declareScene("p1", "S");
        Page page = eng.startPage(scene);
        Conflict c = eng.addConflict(scene, "p1", "Clobber", ConflictType.GOAL, true);
        setDice(c.sides().get(0), 5);
        Ability pow2 = metamorph.abilitiesOfKind(AbilityKind.POWER).get(1); // score 2 < 5
        try { eng.useAbilityRoll(scene, page, metamorph, pow2, c, 0, 0, DriveType.LOVE); fail("expected"); }
        catch (IllegalMoveException ok) { assertTrue(ok.getMessage().contains("cannot affect")); }
    }

    @Test public void splitRespectsStakeCount() {
        GameEngine eng = new GameEngine(state, new Roller.ScriptedRoller(1));
        Scene scene = eng.declareScene("p1", "S");
        eng.startPage(scene);
        Conflict c = eng.addConflict(scene, "p1", "E", ConflictType.EVENT, true);
        eng.claim(scene, c, 0, "p1", true);
        metamorph.drive(DriveType.LOVE).addDebt(2);
        eng.stake(c, 0, metamorph, DriveType.LOVE, 1); // 1 Stake -> cannot split into 2
        setDice(c.sides().get(0), 4);
        try { eng.split(c, 0, 0, 2); fail("expected"); }
        catch (IllegalMoveException ok) { assertTrue(ok.getMessage().contains("as many dice as Stakes")); }
        eng.stake(c, 0, metamorph, DriveType.LOVE, 1); // now 2 Stakes
        eng.split(c, 0, 0, 2);
        assertEquals(2, c.sides().get(0).dice().size());
        assertEquals(4, c.sides().get(0).total()); // 2+2
    }

    @Test public void overdrawnPenaltyLowersHighestDie() {
        // Metamorph LOVE strength 5 but 6 Debt -> Overdrawn; owns a die of 5; scripted roll 3 lowers it.
        metamorph.drive(DriveType.LOVE).addDebt(6);
        GameEngine eng = new GameEngine(state, new Roller.ScriptedRoller(3));
        Scene scene = eng.declareScene("p1", "S");
        // Seed a live conflict with a die owned by Metamorph before the page starts its checks.
        Conflict c = new Conflict("pre", "Pre", ConflictType.GOAL, "p1");
        setDice(c.sides().get(0), 5);
        c.sides().get(0).ally("m");
        scene.conflicts().add(c);
        eng.startPage(scene);
        assertEquals(3, c.sides().get(0).dice().get(0).value());
    }

    @Test public void gloatTurnsDiceAndAwardsStoryTokens() {
        GameEngine eng = new GameEngine(state, new Roller.ScriptedRoller(1));
        Scene scene = eng.declareScene("p1", "S");
        eng.startPage(scene);
        Conflict c = eng.addConflict(scene, "p2", "Battle-fleet Fires", ConflictType.EVENT, true);
        setDice(c.sides().get(0), 6, 4, 1);
        ResolveResult r = eng.gloat(c, 0, "p1", 2);
        assertEquals(ResolveResult.Kind.GLOAT, r.kind);
        assertEquals(2, r.storyTokensAwarded);
        assertEquals(2, p1.storyTokens());
        assertEquals(3, c.sides().get(0).total()); // 1,1,1
    }

    private static void setDice(ConflictSide side, int... values) {
        side.dice().clear();
        for (int v : values) side.dice().add(new Die(v));
    }
}
