package com.whim.samurai.ui;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;

/** Tiny helper to bind a key to a Runnable on a component (WHEN_IN_FOCUSED_WINDOW). */
public final class Keys {
    private Keys() { }

    public static void bind(JComponent c, String keyStroke, final Runnable action) {
        InputMap im = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = c.getActionMap();
        Object key = "act_" + keyStroke;
        im.put(KeyStroke.getKeyStroke(keyStroke), key);
        am.put(key, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }
}
