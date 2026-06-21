package com.midnight.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.midnight.core.Direction;

/**
 * The 8-way compass control. A 3&times;3 grid of buttons: the eight outer cells
 * are "Look" buttons (N, NE, E, SE, S, SW, W, NW) that rotate the selected
 * lord's facing via {@link Listener#onLook(Direction)}; the centre cell is the
 * "Move" button which advances the lord along its current facing via
 * {@link Listener#onMove()}.
 *
 * <p>The frame disables the whole control when it is NIGHT or the lord is out of
 * hours; whether the forward move is currently legal (passable dest, enough
 * hours) is applied by {@link #setMoveAllowed(boolean)}.
 */
final class CompassPanel extends JPanel {

    /** Callback for the controller (the frame). */
    interface Listener {
        void onLook(Direction d);
        void onMove();
    }

    private final Listener listener;
    private final JButton moveButton;
    private final JButton[] lookButtons = new JButton[Direction.values().length];

    CompassPanel(Listener listener) {
        this.listener = listener;
        setLayout(new GridLayout(3, 3, 4, 4));
        setBorder(BorderFactory.createTitledBorder("Compass — Look & Move"));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Grid order: NW N NE / W [MOVE] E / SW S SE
        add(lookButton(Direction.NORTHWEST, "NW"));
        add(lookButton(Direction.NORTH, "N"));
        add(lookButton(Direction.NORTHEAST, "NE"));
        add(lookButton(Direction.WEST, "W"));

        moveButton = new JButton("Move");
        moveButton.setFont(moveButton.getFont().deriveFont(Font.BOLD, 13f));
        moveButton.setForeground(new Color(0x1A5276));
        moveButton.setToolTipText("Advance the selected lord one tile along its facing.");
        moveButton.addActionListener(e -> listener.onMove());
        add(moveButton);

        add(lookButton(Direction.EAST, "E"));
        add(lookButton(Direction.SOUTHWEST, "SW"));
        add(lookButton(Direction.SOUTH, "S"));
        add(lookButton(Direction.SOUTHEAST, "SE"));
    }

    private JButton lookButton(final Direction d, String text) {
        JButton b = new JButton(text);
        b.setToolTipText("Look " + text + " (turn to face this way).");
        b.addActionListener(e -> listener.onLook(d));
        lookButtons[d.ordinal()] = b;
        return b;
    }

    /** Enable/disable the whole compass (used for NIGHT / out-of-hours). */
    void setControlsEnabled(boolean enabled) {
        moveButton.setEnabled(enabled);
        for (int i = 0; i < lookButtons.length; i++) {
            lookButtons[i].setEnabled(enabled);
        }
    }

    /** Mark the lord's current facing so the player sees where "Move" goes. */
    void setFacing(Direction facing) {
        for (int i = 0; i < lookButtons.length; i++) {
            Direction d = Direction.values()[i];
            boolean on = d == facing;
            lookButtons[i].setFont(lookButtons[i].getFont().deriveFont(on ? Font.BOLD : Font.PLAIN));
        }
        if (facing != null) {
            moveButton.setText("Move →" + shortLabel(facing));
        }
    }

    /** Enable/disable the Move button specifically (e.g. dest impassable). */
    void setMoveAllowed(boolean allowed) {
        moveButton.setEnabled(allowed);
    }

    private static String shortLabel(Direction d) {
        switch (d) {
            case NORTH: return "N";
            case NORTHEAST: return "NE";
            case EAST: return "E";
            case SOUTHEAST: return "SE";
            case SOUTH: return "S";
            case SOUTHWEST: return "SW";
            case WEST: return "W";
            case NORTHWEST: return "NW";
            default: return "?";
        }
    }
}
