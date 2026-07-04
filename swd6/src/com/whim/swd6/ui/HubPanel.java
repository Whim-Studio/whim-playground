package com.whim.swd6.ui;

import java.awt.BorderLayout;

/**
 * Base class for the five hub cards. Holds the shared {@link AppContext} and a
 * {@link #onShow()} hook the frame calls each time the card becomes visible so the
 * panel can re-read the active character.
 *
 * Owned by Task 3 (ui).
 */
public abstract class HubPanel extends Ui.SpaceBackground {

    protected final AppContext ctx;

    protected HubPanel(AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout());
    }

    /** Called when this card is shown; default no-op. */
    public void onShow() {
    }
}
