package com.whim.b5wars.ui;

import com.whim.b5wars.engine.GameEvent;
import com.whim.b5wars.model.Ship;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * Allocate thrust and offensive/defensive EW for the selected ship during POWER / EW phases.
 * The engine auto-allocates sensible defaults when those phases begin; this panel lets the player
 * override them before IMPULSE. Spinners are enabled only in the matching phase.
 */
public final class PowerEwPanel extends JPanel implements GameListener {

    private final GameController controller;

    private final JSpinner thrustSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));
    private final JSpinner ewOffSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));
    private final JSpinner ewDefSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));
    private final JLabel hint = new JLabel();

    private boolean updating;

    public PowerEwPanel(GameController controller) {
        this.controller = controller;
        setBackground(UiTheme.PANEL_BG);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(380, 130));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Power / EW Allocation");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.FONT_HEADER);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        add(title, c);
        c.gridwidth = 1;

        addRow(c, 1, "Thrust available", thrustSpinner);
        addRow(c, 2, "EW — offensive", ewOffSpinner);
        addRow(c, 3, "EW — defensive", ewDefSpinner);

        hint.setForeground(UiTheme.TEXT_DIM);
        hint.setFont(UiTheme.FONT_SMALL);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        add(hint, c);

        thrustSpinner.addChangeListener(e -> applyThrust());
        ewOffSpinner.addChangeListener(e -> applyEw());
        ewDefSpinner.addChangeListener(e -> applyEw());

        controller.addListener(this);
        gameChanged();
    }

    private void addRow(GridBagConstraints c, int row, String label, JSpinner spinner) {
        JLabel l = new JLabel(label);
        l.setForeground(UiTheme.TEXT);
        l.setFont(UiTheme.FONT_SMALL);
        c.gridx = 0;
        c.gridy = row;
        add(l, c);
        spinner.setPreferredSize(new Dimension(70, 24));
        c.gridx = 1;
        add(spinner, c);
    }

    private void applyThrust() {
        if (updating) {
            return;
        }
        Ship s = controller.selectedShip();
        if (s != null) {
            s.setThrustAvailable(((Number) thrustSpinner.getValue()).intValue());
        }
    }

    private void applyEw() {
        if (updating) {
            return;
        }
        Ship s = controller.selectedShip();
        if (s != null) {
            s.setEwOffensive(((Number) ewOffSpinner.getValue()).intValue());
            s.setEwDefensive(((Number) ewDefSpinner.getValue()).intValue());
        }
    }

    @Override
    public void gameChanged() {
        updating = true;
        Ship s = controller.selectedShip();
        boolean power = controller.isPowerPhase();
        boolean ew = controller.isEwPhase();
        if (s != null) {
            thrustSpinner.setValue(Integer.valueOf(s.getThrustAvailable()));
            ewOffSpinner.setValue(Integer.valueOf(s.getEwOffensive()));
            ewDefSpinner.setValue(Integer.valueOf(s.getEwDefensive()));
            hint.setText("<html><font color='#96a0b4'>" + s.getType().getName()
                    + " — max thrust " + s.getType().getThrust()
                    + ", EW rating " + s.getType().getEwRating() + "</font></html>");
        } else {
            hint.setText("Select a ship.");
        }
        thrustSpinner.setEnabled(power && s != null);
        ewOffSpinner.setEnabled(ew && s != null);
        ewDefSpinner.setEnabled(ew && s != null);
        updating = false;
    }

    @Override
    public void logEvents(List<GameEvent> events) {
        // n/a
    }
}
