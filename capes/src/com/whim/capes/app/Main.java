package com.whim.capes.app;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.whim.capes.engine.GameEngine;
import com.whim.capes.engine.Roller;
import com.whim.capes.model.Ability;
import com.whim.capes.model.AbilityKind;
import com.whim.capes.model.Character;
import com.whim.capes.model.Conflict;
import com.whim.capes.model.ConflictSide;
import com.whim.capes.model.ConflictType;
import com.whim.capes.model.Drive;
import com.whim.capes.model.DriveType;
import com.whim.capes.model.GameState;
import com.whim.capes.model.Player;
import com.whim.capes.model.Scene;
import com.whim.capes.ui.MainFrame;

/**
 * Application entry point. Builds a small demo {@link GameState} and a
 * {@link GameEngine} so the interactive Table View has a live Scene, two
 * playable characters and one Conflict to act on, then launches the Swing UI on
 * the EDT. Headless environments print a readiness line and exit.
 */
public final class Main {
    public static void main(String[] args) {
        GameState state = demoState();
        GameEngine engine = new GameEngine(state, new Roller.SeededRoller());

        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("CapesTabletop: headless environment detected — UI not shown.");
            System.out.println("Model OK: " + state.players().size() + " players, "
                    + state.roster().size() + " characters, "
                    + state.currentScene().conflicts().size() + " conflict(s) on the table.");
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
                catch (Exception ignored) { /* default L&F */ }
                MainFrame frame = new MainFrame(state, engine);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    private static GameState demoState() {
        GameState s = new GameState();
        Player alex = new Player("p1", "Alex");
        Player beth = new Player("p2", "Beth");
        alex.addStoryTokens(2);
        beth.addStoryTokens(2);
        s.players().add(alex);
        s.players().add(beth);

        Character liberty = hero("h1", "Captain Liberty", DriveType.JUSTICE, "Godling");
        Character grim = hero("v1", "Professor Grim", DriveType.PRIDE, "Mastermind");
        liberty.drive(DriveType.JUSTICE).addDebt(3); // some Debt on hand so Staking works immediately
        grim.drive(DriveType.PRIDE).addDebt(3);
        s.roster().add(liberty);
        s.roster().add(grim);
        alex.controlledCharacterIds().add("h1");
        beth.controlledCharacterIds().add("v1");

        s.comicsCode().add("Super-heroes never die");
        s.comicsCode().add("Innocents are never harmed by the heroes' failure");

        Scene scene = new Scene(1, alex.id(), "Charity Gala Heist");
        s.scenes().add(scene);
        s.recordSceneDeclared(alex.id());

        Conflict c = new Conflict("cf1-1", "Terrorize Bystanders", ConflictType.GOAL, beth.id());
        ConflictSide heroSide = c.sides().get(0);
        heroSide.setResolutionStatement("Captain Liberty keeps the crowd calm");
        heroSide.dice().get(0).set(3);
        heroSide.ally("h1");
        heroSide.claim(alex.id());
        ConflictSide villainSide = c.sides().get(1);
        villainSide.setResolutionStatement("Grim's goons panic the guests");
        villainSide.dice().get(0).set(2);
        villainSide.ally("v1");
        villainSide.claim(beth.id());
        scene.conflicts().add(c);

        s.eventLog().log(com.whim.capes.model.EventLogEntry.Category.SCENE,
                "Scene 1: \"Charity Gala Heist\". Try Start Page, then act on the Conflict.");
        return s;
    }

    private static Character hero(String id, String name, DriveType main, String concept) {
        Character c = new Character(id, name, true);
        c.setConcept(concept);
        String[] powers = { "Signature Power", "Strength", "Speed", "Resilience", "Ranged Blast" };
        String[] atts = { "Confident", "Determined", "Wry", "Fierce", "Watchful" };
        String[] styles = { "Grandstand", "Overpower", "Improvise", "Intimidate", "Protect" };
        for (int i = 0; i < 5; i++) c.abilities().add(new Ability(powers[i], AbilityKind.POWER, i + 1, true));
        for (int i = 0; i < 5; i++) c.abilities().add(new Ability(atts[i], AbilityKind.ATTITUDE, i + 1, false));
        for (int i = 0; i < 5; i++) c.abilities().add(new Ability(styles[i], AbilityKind.STYLE, i + 1, false));
        c.drives().add(new Drive(main, 3));
        c.drives().add(new Drive(DriveType.HOPE, 2));
        c.drives().add(new Drive(DriveType.DUTY, 2));
        c.drives().add(new Drive(DriveType.LOVE, 1));
        c.drives().add(new Drive(DriveType.TRUTH, 1));
        return c;
    }
}
