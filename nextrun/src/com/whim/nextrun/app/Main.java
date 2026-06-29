package com.whim.nextrun.app;

import com.whim.nextrun.domain.HeroClass;
import com.whim.nextrun.engine.GameState;
import com.whim.nextrun.ui.GameFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point. Prompts for a hero class, builds a fresh {@link GameState} with a
 * procedurally generated world, and shows the {@link GameFrame}.
 */
public final class Main {

    private static final int MAP_W = 22;
    private static final int MAP_H = 16;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to default L&F
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { newRun(); }
        });
    }

    /** Show the class picker and launch a new run; also used by "play again". */
    public static void newRun() {
        HeroClass chosen = pickClass();
        if (chosen == null) { System.exit(0); return; }
        long seed = System.nanoTime();
        GameState game = new GameState(chosen, MAP_W, MAP_H, seed);
        new GameFrame(game).setVisible(true);
    }

    private static HeroClass pickClass() {
        HeroClass[] classes = HeroClass.values();
        String[] options = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            options[i] = classes[i].label() + " — " + classes[i].passive();
        }
        Object sel = JOptionPane.showInputDialog(
            null,
            "Choose your hero. Time is your enemy — every action burns turns,\n"
                + "and waves of fiends grow stronger. Win by gold, forge, settlement,\n"
                + "or by reaching the portal alive.",
            "Next Run — Choose a Class",
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]);
        if (sel == null) return null;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(sel)) return classes[i];
        }
        return classes[0];
    }
}
