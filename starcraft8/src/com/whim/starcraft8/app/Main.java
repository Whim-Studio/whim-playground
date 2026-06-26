package com.whim.starcraft8.app;

import com.whim.starcraft8.data.MapFactory;
import com.whim.starcraft8.domain.GameState;
import com.whim.starcraft8.domain.Race;
import com.whim.starcraft8.engine.Engine;
import com.whim.starcraft8.engine.Simulation;
import com.whim.starcraft8.ui.GameFrame;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * 8-Bit StarCraft — application entry point.
 *
 * Wires the three layers built against STARCRAFT8_CONTRACT.md:
 *   data.MapFactory (Task 1)  ->  engine.Engine/Simulation (Task 2)  ->  ui.GameFrame (Task 3)
 *
 * A tiny race-select dialog picks the human race (player 0) and the AI race
 * (player 1). The simulation runs on its own 60-tps background thread; the
 * Swing UI reads state only through Simulation.readState under the engine lock.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                launch();
            }
        });
    }

    private static void launch() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Cosmetic only; the dialog still works with the default L&F.
        }

        Race human = pickRace("Choose YOUR race", Race.TERRAN);
        if (human == null) {
            return; // user cancelled
        }
        Race ai = pickRace("Choose the ENEMY (AI) race", Race.ZERG);
        if (ai == null) {
            return;
        }

        GameState state = MapFactory.newSkirmish(human, ai);
        Simulation sim = Engine.create(state, 0); // human is player id 0
        GameFrame frame = new GameFrame(sim);
        frame.setVisible(true);
        sim.start();
    }

    private static Race pickRace(String prompt, Race fallback) {
        JComboBox<Race> box = new JComboBox<Race>(Race.values());
        box.setSelectedItem(fallback);
        int result = JOptionPane.showConfirmDialog(
                null, box, prompt,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        Race chosen = (Race) box.getSelectedItem();
        return chosen != null ? chosen : fallback;
    }
}
