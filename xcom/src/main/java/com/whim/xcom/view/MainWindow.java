package com.whim.xcom.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.whim.xcom.app.AudioManager;
import com.whim.xcom.rules.Ruleset;

/**
 * Top-level application window. Hosts the main menu for Phase 0; later phases
 * will swap the content pane for the Geoscape/Battlescape screens.
 */
public final class MainWindow extends JFrame {

    private final transient Ruleset ruleset;
    private final transient AudioManager audio;

    public MainWindow(Ruleset ruleset, AudioManager audio) {
        super("UFO: Enemy Unknown — clean-room recreation");
        this.ruleset = ruleset;
        this.audio = audio;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new MainMenuPanel(ruleset, newGame(), options(), quit()));
        pack();
        setLocationRelativeTo(null);
    }

    private ActionListener newGame() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                audio.playSfx("menu_select");
                JOptionPane.showMessageDialog(MainWindow.this,
                        "New Game is a Phase 1+ feature.\n\nPhase 0 delivers the ruleset engine:\n"
                                + ruleset.displayName() + "\n"
                                + ruleset.weapons().size() + " weapons, "
                                + ruleset.aliens().size() + " aliens, "
                                + ruleset.facilities().size() + " facilities loaded.",
                        "New Game", JOptionPane.INFORMATION_MESSAGE);
            }
        };
    }

    private ActionListener options() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                audio.playSfx("menu_select");
                JOptionPane.showMessageDialog(MainWindow.this,
                        "Options placeholder (audio, difficulty, display) — Phase 1+.",
                        "Options", JOptionPane.INFORMATION_MESSAGE);
            }
        };
    }

    private ActionListener quit() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                System.exit(0);
            }
        };
    }
}
