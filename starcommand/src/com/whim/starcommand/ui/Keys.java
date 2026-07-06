package com.whim.starcommand.ui;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;

/** Tiny helper to bind a key (by its display letter) to a Runnable on a component. */
public final class Keys {
    private Keys() { }

    public static void bind(JComponent comp, String key, final Runnable action) {
        InputMap im = comp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = comp.getActionMap();
        String id = "act_" + key;
        im.put(KeyStroke.getKeyStroke(key.toUpperCase()), id);
        im.put(KeyStroke.getKeyStroke(key.toLowerCase()), id);
        am.put(id, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }
}
