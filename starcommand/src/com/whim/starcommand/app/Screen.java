package com.whim.starcommand.app;

import javax.swing.JPanel;

/** Base class for a full-window screen managed by {@link ScreenManager}. */
public abstract class Screen extends JPanel {

    protected final Game game;

    public Screen(Game game) {
        this.game = game;
        setFocusable(true);
    }

    /** Unique card name used with the CardLayout. */
    public abstract String name();

    /** Called every time this screen becomes visible; refresh state here. */
    public void onShow() { }
}
