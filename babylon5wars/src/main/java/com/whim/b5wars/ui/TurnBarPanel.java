package com.whim.b5wars.ui;

import com.whim.b5wars.engine.GameEvent;
import com.whim.b5wars.engine.TurnPhase;
import com.whim.b5wars.model.Ship;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Top bar: turn #, phase, impulse #, initiative winner, and the controls to drive the game.
 * "Advance Phase" walks the FSM; during IMPULSE the maneuver buttons drive {@code MovementEngine}
 * and "Fire…" opens the {@link WeaponFireDialog} for the selected ship.
 */
public final class TurnBarPanel extends JPanel implements GameListener {

    private final GameController controller;

    private final JLabel status = new JLabel();
    private final JButton advanceBtn = new JButton("Advance Phase");
    private final JButton forwardBtn = new JButton("Forward");
    private final JButton turnLeftBtn = new JButton("Turn ⟲");
    private final JButton turnRightBtn = new JButton("Turn ⟳");
    private final JButton slipLeftBtn = new JButton("Slip ◀");
    private final JButton slipRightBtn = new JButton("Slip ▶");
    private final JButton accelBtn = new JButton("Accel +");
    private final JButton decelBtn = new JButton("Decel −");
    private final JButton fireBtn = new JButton("Fire…");

    public TurnBarPanel(GameController controller) {
        this.controller = controller;
        setLayout(new GridLayout(2, 1));
        setBackground(UiTheme.PANEL_BG_ALT);
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        infoRow.setOpaque(false);
        status.setFont(UiTheme.FONT_HEADER);
        status.setForeground(UiTheme.TEXT);
        infoRow.add(status);
        add(infoRow);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        btnRow.setOpaque(false);
        style(advanceBtn);
        btnRow.add(advanceBtn);
        btnRow.add(sep());
        for (JButton b : new JButton[] {forwardBtn, turnLeftBtn, turnRightBtn, slipLeftBtn,
                slipRightBtn, accelBtn, decelBtn}) {
            style(b);
            btnRow.add(b);
        }
        btnRow.add(sep());
        style(fireBtn);
        btnRow.add(fireBtn);
        add(btnRow);

        advanceBtn.addActionListener(e -> controller.advancePhase());
        forwardBtn.addActionListener(e -> controller.moveForward());
        turnLeftBtn.addActionListener(e -> controller.turnShip(-1));
        turnRightBtn.addActionListener(e -> controller.turnShip(1));
        slipLeftBtn.addActionListener(e -> controller.sideslip(-1));
        slipRightBtn.addActionListener(e -> controller.sideslip(1));
        accelBtn.addActionListener(e -> controller.accelerate(1));
        decelBtn.addActionListener(e -> controller.accelerate(-1));
        fireBtn.addActionListener(e -> openFire());

        controller.addListener(this);
        gameChanged();
    }

    private Component sep() {
        return Box.createHorizontalStrut(10);
    }

    private void style(JButton b) {
        b.setFocusable(false);
        b.setFont(UiTheme.FONT_SMALL);
        b.setBackground(UiTheme.PANEL_BG);
        b.setForeground(UiTheme.TEXT);
    }

    private void openFire() {
        Ship attacker = controller.selectedShip();
        Window owner = SwingUtilities.getWindowAncestor(this);
        WeaponFireDialog dialog = new WeaponFireDialog(owner, controller, attacker);
        dialog.setVisible(true);
    }

    @Override
    public void gameChanged() {
        boolean over = controller.state().isOver();
        TurnPhase phase = controller.state().getPhase();
        boolean impulse = phase == TurnPhase.IMPULSE && !over;

        String phaseName = over ? "GAME OVER" : phase.name();
        String winner = "";
        if (over) {
            winner = controller.state().getWinner() == null
                    ? "  (draw)" : "  Winner: Side " + controller.state().getWinner();
        }
        Ship sel = controller.selectedShip();
        String selName = sel == null ? "none" : sel.getType().getName() + " (Side " + sel.getSide() + ")";
        status.setText("Turn " + controller.state().getTurn()
                + "   Phase: " + phaseName
                + "   Impulse: " + controller.state().getImpulse()
                + "   Initiative: " + controller.initiativeText()
                + "   Selected: " + selName + winner);
        status.setForeground(over ? UiTheme.SELECT : UiTheme.TEXT);

        advanceBtn.setEnabled(!over);
        advanceBtn.setText(impulse ? "End Turn ▶" : (over ? "Advance Phase" : "Advance Phase ▶"));

        boolean canAct = impulse && sel != null && !sel.isDestroyed();
        for (JButton b : new JButton[] {forwardBtn, turnLeftBtn, turnRightBtn, slipLeftBtn,
                slipRightBtn, accelBtn, decelBtn, fireBtn}) {
            b.setEnabled(canAct);
        }
    }

    @Override
    public void logEvents(List<GameEvent> events) {
        // n/a
    }
}
