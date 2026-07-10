package com.whim.merchantprince.app;

import javax.swing.JPanel;
import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;

/**
 * Mirrors the original's screen flow with a {@link CardLayout}: registered screens
 * are swapped by name (Main Menu -> Map -> Market -> Fleet -> Venice ...).
 */
public class ScreenManager {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final Map<String, Screen> screens = new HashMap<String, Screen>();

    public JPanel root() { return root; }

    public void register(Screen screen) {
        screens.put(screen.name(), screen);
        root.add(screen, screen.name());
    }

    public Screen get(String name) { return screens.get(name); }

    public void show(String name) {
        Screen s = screens.get(name);
        if (s != null) s.onShow();
        cards.show(root, name);
        if (s != null) s.requestFocusInWindow();
    }
}
