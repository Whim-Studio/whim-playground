package com.whim.capes.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.whim.capes.content.ExtendedData;
import com.whim.capes.content.NonPersonTemplate;
import com.whim.capes.model.AbilityKind;
import com.whim.capes.model.Character;
import com.whim.capes.model.Conflict;
import com.whim.capes.model.ConflictSide;
import com.whim.capes.model.ConflictType;
import com.whim.capes.model.Die;
import com.whim.capes.model.Drive;
import com.whim.capes.model.DriveType;
import com.whim.capes.model.EventLogEntry;
import com.whim.capes.model.Exemplar;
import com.whim.capes.model.GameState;
import com.whim.capes.model.Page;
import com.whim.capes.model.Player;
import com.whim.capes.model.Scene;

/** Phase 4 tests: Ch.5 non-person participants, Character-Conflict removal, and split-off-new-side. */
public class ExtendedRulesTest {
    private GameState state;
    private GameEngine eng;
    private Player p1;

    @Before public void setUp() {
        state = new GameState();
        p1 = new Player("p1", "Alex");
        state.players().add(p1);
        eng = new GameEngine(state, new Roller.ScriptedRoller(1));
    }

    @Test public void catalogueHasTwentyParticipants() {
        assertEquals(20, ExtendedData.all().size());
    }

    @Test public void nonPersonIsMundaneWithNoDrives() {
        NonPersonTemplate volcano = byName("Volcano");
        Character c = CharacterFactory.fromNonPerson("np1", volcano);
        assertFalse(c.isSuperPowered());
        assertTrue(c.isNonPerson());
        assertTrue(c.drives().isEmpty());
        assertEquals(5, c.abilitiesOfKind(AbilityKind.SKILL).size());   // 5 actions -> Skills
        assertTrue(c.abilitiesOfKind(AbilityKind.ATTITUDE).size() > 0);
    }

    @Test public void resolvingCharacterConflictRemovesParticipantFromScene() {
        Scene scene = eng.declareScene("p1", "Eruption");
        eng.startPage(scene);
        Character volcano = eng.addNonPerson(byName("Volcano"), "p1");

        // Volcano is also allied to an unrelated Conflict.
        Conflict other = eng.addConflict(scene, "p1", "Escape the caldera", ConflictType.GOAL, true);
        other.sides().get(0).ally(volcano.id());
        assertTrue(other.sides().get(0).alliedCharacterIds().contains(volcano.id()));

        // Its Character Conflict enters and is Resolved (side 0 Controls).
        Conflict cc = eng.addCharacterConflict(scene, volcano, ConflictType.EVENT, "Someone falls toward lava", "p1");
        cc.sides().get(0).dice().get(0).set(4);
        cc.sides().get(1).dice().get(0).set(1);
        eng.resolve(scene, cc, 0, "p1");

        // Participant has left the Scene: no longer allied anywhere.
        assertFalse(other.sides().get(0).alliedCharacterIds().contains(volcano.id()));
        assertFalse(scene.conflicts().contains(cc));
    }

    @Test public void foundNewSideSplitsDieAcrossTwoSides() {
        Scene scene = eng.declareScene("p1", "Split");
        eng.startPage(scene);
        Character hero = new Character("h", "Zero-G", true);
        hero.drives().add(new Drive(DriveType.LOVE, 3));
        hero.drive(DriveType.LOVE).addDebt(1);
        state.roster().add(hero);
        p1.controlledCharacterIds().add("h");

        Conflict c = eng.addConflict(scene, "p1", "Rachel falls from balcony", ConflictType.EVENT, true);
        setDice(c.sides().get(0), 4);
        eng.claim(scene, c, 0, "p1", true);

        ConflictSide neu = eng.foundNewSide(scene, c, 0, 0, hero, DriveType.LOVE, "Only I will rescue her", "p1");
        assertEquals(3, c.sides().size());              // original 2 + the founded side
        assertEquals(2, c.sides().get(0).total());      // smaller half stays
        assertEquals(2, neu.total());                   // larger half founds the new side
        assertEquals(1, neu.stakedDebt());              // one point of Debt Staked to found it
    }

    @Test public void turnAdvancesClockwiseAndPhaseAdvances() {
        state.players().add(new Player("p2", "Beth"));
        Scene scene = eng.declareScene("p1", "Turns");
        Page page = eng.startPage(scene);
        assertEquals("Alex", eng.currentActor().name());   // Starter acts first
        assertEquals("Beth", eng.advanceTurn().name());     // clockwise
        assertEquals("Alex", eng.advanceTurn().name());     // wraps
        assertEquals(Page.Phase.CLAIM, page.phase());
        assertEquals(Page.Phase.FREE_NARRATION, eng.advancePhase(page));
        assertEquals(Page.Phase.ACTIONS, eng.advancePhase(page));
    }

    @Test public void exemplarConflictAlliesBothAndSurvivesResolve() {
        Character hero = new Character("h", "Sparky", true);
        Character mentor = new Character("m", "Firestorm", true);
        state.roster().add(hero);
        state.roster().add(mentor);
        hero.exemplars().add(new Exemplar("m", DriveType.DUTY, "Firestorm judges Sparky's worth", ConflictType.EVENT));

        Scene scene = eng.declareScene("p1", "Bank Robbery");
        eng.startPage(scene);
        Conflict c = eng.addExemplarConflict(scene, hero, hero.exemplars().get(0), "p1");
        assertTrue(c.sides().get(0).alliedCharacterIds().contains("h"));
        assertTrue(c.sides().get(1).alliedCharacterIds().contains("m"));
        // Resolving an Exemplar conflict does NOT remove anyone (no characterConflictOwnerId).
        c.sides().get(0).dice().get(0).set(4);
        eng.resolve(scene, c, 0, "p1");
        assertTrue(state.roster().contains(hero));
        assertTrue(state.roster().contains(mentor));
    }

    @Test public void narrationIsLogged() {
        int before = state.eventLog().entries().size();
        eng.logNarration("Captain Liberty soars into the sunset.");
        assertEquals(before + 1, state.eventLog().entries().size());
        EventLogEntry last = state.eventLog().entries().get(state.eventLog().entries().size() - 1);
        assertEquals(EventLogEntry.Category.NARRATION, last.category());
    }

    private NonPersonTemplate byName(String name) {
        for (NonPersonTemplate t : ExtendedData.all()) if (t.name().equals(name)) return t;
        throw new IllegalStateException("missing " + name);
    }

    private static void setDice(ConflictSide side, int... values) {
        side.dice().clear();
        for (int v : values) side.dice().add(new Die(v));
    }
}
