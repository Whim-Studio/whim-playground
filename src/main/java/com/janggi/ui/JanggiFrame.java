package com.janggi.ui;

import java.awt.CardLayout;

import javax.swing.JPanel;

import com.janggi.core.SetupChoice;

/**
 * Top-level window. Hosts three screens via {@link CardLayout}: mode selection,
 * pre-game setup, and the running game.
 */
public class JanggiFrame extends javax.swing.JFrame {

    private static final String CARD_MODE = "mode";
    private static final String CARD_SETUP = "setup";
    private static final String CARD_GAME = "game";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final ModePanel modePanel;

    public JanggiFrame() {
        super("Janggi — Korean Chess");
        modePanel = new ModePanel(this);
        root.add(modePanel, CARD_MODE);
        setContentPane(root);
        setMinimumSize(new java.awt.Dimension(640, 760));
        pack();
        showModeSelection();
    }

    /** Return to the opening mode-selection screen. */
    public void showModeSelection() {
        cards.show(root, CARD_MODE);
    }

    /** Advance from mode selection to the per-side arrangement setup screen. */
    public void showSetup(GameConfig config) {
        SetupPanel setup = new SetupPanel(this, config);
        root.add(setup, CARD_SETUP);
        cards.show(root, CARD_SETUP);
    }

    /** Begin the game with the chosen arrangements. */
    public void startGame(GameConfig config, SetupChoice choSetup, SetupChoice hanSetup) {
        GamePanel game = new GamePanel(this, config, choSetup, hanSetup);
        root.add(game, CARD_GAME);
        cards.show(root, CARD_GAME);
    }
}
