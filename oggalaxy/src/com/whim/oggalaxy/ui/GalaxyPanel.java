package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.GameConfig;
import com.whim.oggalaxy.api.GameController;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Result;
import com.whim.oggalaxy.api.Views;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

/**
 * Galaxy tab: a galaxy/system navigator (spinners) rendering the 15 positions of a
 * system with owner / planet / moon / debris, and a live fleet-movements list with
 * recall. Selecting a target row opens the {@link FleetDispatchDialog}.
 */
public final class GalaxyPanel extends JPanel implements Refreshable {

    private final GameController controller;
    private final StatusSink sink;

    private final JSpinner galaxySpinner = new JSpinner(
            new SpinnerNumberModel(1, 1, GameConfig.GALAXIES, 1));
    private final JSpinner systemSpinner = new JSpinner(
            new SpinnerNumberModel(42, 1, GameConfig.SYSTEMS_PER_GALAXY, 1));
    private final JPanel rowsPanel = new JPanel();
    private final JPanel fleetPanel = new JPanel();
    private final CellRow[] cellRows = new CellRow[GameConfig.POSITIONS_PER_SYSTEM];

    private Views.GameStateView state;

    public GalaxyPanel(GameController controller, StatusSink sink) {
        this.controller = controller;
        this.sink = sink;
        setOpaque(true);
        setBackground(Palette.BG_SPACE);
        setLayout(new BorderLayout(8, 0));
        setBorder(UiUtil.padded(8, 8, 8, 8));

        // --- navigator header ---
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.add(UiUtil.label("Galaxy", Palette.ACCENT, Palette.FONT_BOLD));
        header.add(Box.createHorizontalStrut(4));
        galaxySpinner.setMaximumSize(new Dimension(60, 26));
        header.add(galaxySpinner);
        header.add(Box.createHorizontalStrut(12));
        header.add(UiUtil.label("System", Palette.ACCENT, Palette.FONT_BOLD));
        header.add(Box.createHorizontalStrut(4));
        systemSpinner.setMaximumSize(new Dimension(70, 26));
        header.add(systemSpinner);
        header.add(Box.createHorizontalStrut(12));
        JButton prev = UiUtil.button("◀ Prev");
        JButton next = UiUtil.button("Next ▶");
        prev.addActionListener(e -> stepSystem(-1));
        next.addActionListener(e -> stepSystem(1));
        header.add(prev);
        header.add(Box.createHorizontalStrut(4));
        header.add(next);
        header.add(Box.createHorizontalGlue());

        galaxySpinner.addChangeListener(e -> rebuildRows());
        systemSpinner.addChangeListener(e -> rebuildRows());

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(header, BorderLayout.NORTH);

        rowsPanel.setOpaque(false);
        rowsPanel.setLayout(new GridBagLayout());
        buildCellRows();
        JScrollPane sp = new JScrollPane(wrapTop(rowsPanel),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(UiUtil.panelBorder(null));
        sp.getViewport().setBackground(Palette.BG_PANEL);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        center.add(sp, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // --- fleet movements side ---
        fleetPanel.setOpaque(true);
        fleetPanel.setBackground(Palette.BG_PANEL);
        fleetPanel.setLayout(new BoxLayout(fleetPanel, BoxLayout.Y_AXIS));
        JScrollPane fsp = new JScrollPane(wrapTop(fleetPanel),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        fsp.setBorder(UiUtil.panelBorder("Fleet Movements"));
        fsp.getViewport().setBackground(Palette.BG_PANEL);
        fsp.setPreferredSize(new Dimension(280, 100));
        add(fsp, BorderLayout.EAST);
    }

    private JPanel wrapTop(JPanel inner) {
        JPanel w = new JPanel(new BorderLayout());
        w.setOpaque(false);
        w.add(inner, BorderLayout.NORTH);
        return w;
    }

    private void buildCellRows() {
        rowsPanel.removeAll();
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new java.awt.Insets(1, 2, 1, 2);
        for (int i = 0; i < cellRows.length; i++) {
            cellRows[i] = new CellRow(i + 1);
            gc.gridy = i;
            rowsPanel.add(cellRows[i], gc);
        }
    }

    private void rebuildRows() {
        refresh(state);
    }

    private void stepSystem(int d) {
        int s = ((Number) systemSpinner.getValue()).intValue() + d;
        if (s < 1) s = GameConfig.SYSTEMS_PER_GALAXY;
        if (s > GameConfig.SYSTEMS_PER_GALAXY) s = 1;
        systemSpinner.setValue(s);
    }

    @Override
    public void refresh(Views.GameStateView state) {
        this.state = state;
        if (state == null) return;
        int g = ((Number) galaxySpinner.getValue()).intValue();
        int s = ((Number) systemSpinner.getValue()).intValue();
        List<Views.GalaxyCellView> row = state.galaxyRow(g, s);
        for (int i = 0; i < cellRows.length; i++) {
            Views.GalaxyCellView cell = i < row.size() ? row.get(i) : null;
            cellRows[i].update(cell);
        }
        rowsPanel.revalidate();
        rowsPanel.repaint();

        // fleet movements
        fleetPanel.removeAll();
        List<Views.FleetMovementView> fleets = state.fleets();
        if (fleets.isEmpty()) {
            fleetPanel.add(UiUtil.label("  No fleets in flight", Palette.TEXT_FAINT, Palette.FONT_SMALL));
        } else {
            for (Views.FleetMovementView f : fleets) {
                fleetPanel.add(fleetRow(f));
            }
        }
        fleetPanel.revalidate();
        fleetPanel.repaint();
    }

    private JPanel fleetRow(final Views.FleetMovementView f) {
        JPanel row = new JPanel(new BorderLayout(4, 2));
        row.setOpaque(true);
        row.setBackground(f.ownedByPlayer() ? Palette.mix(Palette.BG_PANEL, Palette.ACCENT, 0.10)
                : Palette.mix(Palette.BG_PANEL, Palette.BAD, 0.10));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.BORDER, 1),
                UiUtil.padded(4, 6, 4, 6)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        String head = f.mission() + "  " + UiUtil.coords(f.origin()) + " → " + UiUtil.coords(f.target());
        row.add(UiUtil.label(head, f.ownedByPlayer() ? Palette.ACCENT : Palette.BAD, Palette.FONT_BOLD),
                BorderLayout.NORTH);

        int total = 0;
        if (f.ships() != null) {
            for (Integer n : f.ships().values()) total += n == null ? 0 : n;
        }
        JLabel detail = UiUtil.label(f.ownerName() + " · " + total + " ships · " + f.statusText()
                + " · ETA " + f.arrivalTick() + "h", Palette.TEXT_DIM, Palette.FONT_SMALL);
        row.add(detail, BorderLayout.CENTER);

        if (f.ownedByPlayer() && !f.returning()) {
            JButton recall = UiUtil.button("Recall", Palette.WARN);
            recall.setFont(Palette.FONT_SMALL);
            recall.addActionListener(e -> {
                Result r = controller.recallFleet(f.id());
                sink.status("Recall " + f.id() + ": " + (r.message.isEmpty() ? (r.ok ? "ordered" : "failed") : r.message), r.ok);
                sink.requestRefresh();
            });
            JPanel east = new JPanel(new BorderLayout());
            east.setOpaque(false);
            east.add(recall, BorderLayout.NORTH);
            row.add(east, BorderLayout.EAST);
        }
        return row;
    }

    // ------------------------------------------------------------------
    private final class CellRow extends JPanel {
        private final int position;
        private final JLabel posLabel;
        private final JLabel nameLabel = UiUtil.label("", Palette.TEXT, Palette.FONT_BOLD);
        private final JLabel ownerLabel = UiUtil.label("", Palette.TEXT_DIM, Palette.FONT_SMALL);
        private final JLabel tagLabel = UiUtil.label("", Palette.TEXT_DIM, Palette.FONT_SMALL);
        private final JButton dispatchButton = UiUtil.button("Send", Palette.ACCENT);
        private Views.GalaxyCellView cell;

        CellRow(int position) {
            this.position = position;
            setOpaque(true);
            setBackground(Palette.BG_CELL);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Palette.BORDER, 1),
                    UiUtil.padded(3, 6, 3, 6)));
            setLayout(new BorderLayout(6, 0));

            posLabel = UiUtil.label(String.valueOf(position), Palette.TEXT_FAINT, Palette.FONT_MONO);
            posLabel.setPreferredSize(new Dimension(22, 20));
            add(posLabel, BorderLayout.WEST);

            JPanel mid = new JPanel(new GridBagLayout());
            mid.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0; c.gridy = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            mid.add(nameLabel, c);
            c.gridx = 1; c.weightx = 0;
            mid.add(tagLabel, c);
            c.gridx = 0; c.gridy = 1; c.gridwidth = 2; c.weightx = 1;
            mid.add(ownerLabel, c);
            add(mid, BorderLayout.CENTER);

            dispatchButton.setFont(Palette.FONT_SMALL);
            dispatchButton.addActionListener(e -> openDispatch());
            add(dispatchButton, BorderLayout.EAST);
        }

        private void openDispatch() {
            if (state == null) return;
            int g = ((Number) galaxySpinner.getValue()).intValue();
            int s = ((Number) systemSpinner.getValue()).intValue();
            Views.PlanetView origin = state.selectedPlanet();
            java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(GalaxyPanel.this);
            FleetDispatchDialog dlg = new FleetDispatchDialog(
                    w, controller, sink, origin, g, s, position,
                    cell != null && cell.hasMoon());
            dlg.setVisible(true);
        }

        void update(Views.GalaxyCellView cell) {
            this.cell = cell;
            if (cell == null || cell.empty()) {
                nameLabel.setText("— empty —");
                nameLabel.setForeground(Palette.TEXT_FAINT);
                ownerLabel.setText("");
                tagLabel.setText("");
                setBackground(Palette.BG_CELL);
            } else {
                nameLabel.setText(cell.planetName());
                Color c = cell.ownedByPlayer() ? Palette.ACCENT
                        : (cell.isAI() ? Palette.BAD : Palette.TEXT);
                nameLabel.setForeground(c);
                ownerLabel.setText(cell.ownerName());
                StringBuilder tag = new StringBuilder();
                if (cell.hasMoon()) tag.append("[M] ");
                if (cell.hasDebris()) tag.append("dbz " + UiUtil.compact(cell.debris().structurePoints()));
                tagLabel.setText(tag.toString());
                tagLabel.setForeground(cell.hasDebris() ? Palette.WARN : Palette.TEXT_DIM);
                setBackground(cell.ownedByPlayer()
                        ? Palette.mix(Palette.BG_CELL, Palette.ACCENT, 0.12)
                        : Palette.BG_CELL);
            }
        }
    }
}
