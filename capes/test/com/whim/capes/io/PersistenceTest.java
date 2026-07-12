package com.whim.capes.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.whim.capes.engine.GameEngine;
import com.whim.capes.engine.Roller;
import com.whim.capes.model.Character;
import com.whim.capes.model.Conflict;
import com.whim.capes.model.ConflictType;
import com.whim.capes.model.Drive;
import com.whim.capes.model.DriveType;
import com.whim.capes.model.GameState;
import com.whim.capes.model.Player;
import com.whim.capes.model.Scene;

/** Phase 5: full GameState round-trips through save/load with all state intact. */
public class PersistenceTest {

    @Test public void roundTripPreservesState() throws Exception {
        GameState state = new GameState();
        Player p1 = new Player("p1", "Alex");
        p1.addStoryTokens(3);
        state.players().add(p1);
        Character hero = new Character("h1", "Captain Liberty", true);
        hero.drives().add(new Drive(DriveType.JUSTICE, 3));
        hero.drive(DriveType.JUSTICE).addDebt(2);
        state.roster().add(hero);
        p1.controlledCharacterIds().add("h1");
        state.comicsCode().add("Super-heroes never die");

        GameEngine eng = new GameEngine(state, new Roller.ScriptedRoller(1));
        Scene scene = eng.declareScene("p1", "Charity Gala Heist");
        Conflict c = eng.addConflict(scene, "p1", "Terrorize Bystanders", ConflictType.GOAL, true);
        c.sides().get(0).dice().get(0).set(4);
        int logBefore = state.eventLog().entries().size();

        File tmp = File.createTempFile("capes-save", ".capes");
        tmp.deleteOnExit();
        Persistence.save(state, tmp);
        GameState loaded = Persistence.load(tmp);

        assertEquals(1, loaded.players().size());
        assertEquals(3, loaded.players().get(0).storyTokens());
        assertEquals(1, loaded.roster().size());
        assertEquals("Captain Liberty", loaded.roster().get(0).name());
        assertEquals(2, loaded.roster().get(0).drive(DriveType.JUSTICE).debt());
        assertEquals(1, loaded.comicsCode().size());
        assertEquals("Charity Gala Heist", loaded.currentScene().title());
        assertEquals(1, loaded.currentScene().conflicts().size());
        assertEquals(4, loaded.currentScene().conflicts().get(0).sides().get(0).total());
        assertEquals(logBefore, loaded.eventLog().entries().size());

        // Event log still works (transient listeners were re-created) after load.
        loaded.eventLog().log(com.whim.capes.model.EventLogEntry.Category.SYSTEM, "post-load");
        assertTrue(loaded.eventLog().entries().size() == logBefore + 1);
    }
}
