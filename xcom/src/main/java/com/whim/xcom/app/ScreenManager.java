package com.whim.xcom.app;

import java.awt.CardLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A minimal CardLayout-based screen switcher. Screens (main menu, Battlescape,
 * and later the Geoscape) register a component under a name; {@link #show(String)}
 * brings it to the front. This is the single wiring point the orchestrator uses
 * to slot the Phase 2 Geoscape in without the gameplay screens knowing about
 * each other.
 */
public final class ScreenManager extends JPanel {

    /** Well-known screen names. */
    public static final String MAIN_MENU = "main-menu";
    public static final String BATTLE = "battle";
    public static final String GEOSCAPE = "geoscape";

    private final CardLayout cards = new CardLayout();

    public ScreenManager() {
        super();
        setLayout(cards);
    }

    /** Register (or replace) a screen under a name. */
    public void setScreen(String name, JComponent screen) {
        // Remove any existing card with this name so replacement works.
        for (java.awt.Component c : getComponents()) {
            if (name.equals(c.getName())) {
                remove(c);
                break;
            }
        }
        screen.setName(name);
        add(screen, name);
        revalidate();
        repaint();
    }

    public void show(String name) {
        cards.show(this, name);
    }
}
