package com.whim.merchantprince.ui;

import com.whim.merchantprince.app.Game;
import com.whim.merchantprince.app.Screen;
import com.whim.merchantprince.engine.TravelEngine;
import com.whim.merchantprince.model.City;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Good;
import com.whim.merchantprince.model.TransportUnit;
import com.whim.merchantprince.render.Palette;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The fleet manager (GAME_DESIGN_REFERENCE §4). Lists the player's ships and caravans
 * with their type, name, location or destination + turns remaining, and cargo. Lets
 * the player dispatch a docked unit to a destination city (via {@link TravelEngine})
 * and build or toggle an automated recurring route. Respects each unit's land/sea
 * capability when offering destinations.
 */
public class FleetScreen extends Screen {

    private final DefaultListModel<TransportUnit> model = new DefaultListModel<TransportUnit>();
    private final JList<TransportUnit> unitList = new JList<TransportUnit>(model);
    private final JPanel detail = new JPanel();

    public FleetScreen(Game game) {
        super(game);
        setLayout(new BorderLayout());

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        nav.setBackground(Palette.PARCHMENT_DK);
        JButton back = UiKit.button("Back to Map");
        back.addActionListener(e -> game.screens.show(Game.MAP));
        nav.add(back);
        nav.add(UiKit.label("Fleet Manager", UiKit.HEAD, Palette.INK));
        add(nav, BorderLayout.NORTH);

        unitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        unitList.setFixedCellHeight(28);
        unitList.setBackground(Palette.PANEL);
        unitList.setForeground(Palette.INK);
        unitList.setFont(UiKit.BODY);
        unitList.setCellRenderer(new UnitRenderer());
        unitList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) rebuildDetail();
        });
        JScrollPane scroll = new JScrollPane(unitList);
        scroll.setPreferredSize(new Dimension(260, 0));
        scroll.setBorder(BorderFactory.createLineBorder(Palette.PANEL_LINE, 2));
        add(scroll, BorderLayout.WEST);

        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.setBackground(Palette.PARCHMENT);
        detail.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        add(detail, BorderLayout.CENTER);
    }

    @Override public String name() { return Game.FLEET; }

    @Override public void onShow() { refreshList(); }

    private void refreshList() {
        GameState s = game.state;
        model.clear();
        if (s == null) { rebuildDetail(); return; }
        int want = game.focusUnitId;
        int selectIdx = -1;
        List<TransportUnit> units = s.unitsOf(s.playerId);
        for (int i = 0; i < units.size(); i++) {
            model.addElement(units.get(i));
            if (units.get(i).id == want) selectIdx = i;
        }
        if (selectIdx < 0 && !units.isEmpty()) selectIdx = 0;
        if (selectIdx >= 0) unitList.setSelectedIndex(selectIdx);
        rebuildDetail();
    }

    private TransportUnit selected() {
        return unitList.getSelectedValue();
    }

    // ------------------------------------------------------------------
    // Detail / control panel for the selected unit.
    // ------------------------------------------------------------------
    private void rebuildDetail() {
        detail.removeAll();
        GameState s = game.state;
        TransportUnit u = selected();
        if (s == null || u == null) {
            detail.add(UiKit.label("No unit selected.", UiKit.HEAD, Palette.INK));
            detail.revalidate();
            detail.repaint();
            return;
        }

        detail.add(UiKit.label(u.displayName(), UiKit.TITLE, Palette.INK));
        detail.add(UiKit.label(u.type.label + "   ·   capacity " + u.type.capacity
                + "   ·   " + (u.type.sea ? "sea" : "land"), UiKit.BODY, Palette.INK));
        detail.add(Box.createVerticalStrut(8));

        // Status: docked or in transit.
        String statusText;
        if (u.inTransit()) {
            City dest = s.city(u.destinationCityId);
            statusText = "In transit → " + (dest == null ? "?" : dest.name)
                    + "   (" + u.turnsRemaining + " turn" + (u.turnsRemaining == 1 ? "" : "s") + " left)";
        } else {
            City at = s.city(u.locationCityId);
            statusText = "Docked at " + (at == null ? "?" : at.name);
        }
        detail.add(UiKit.label(statusText, UiKit.HEAD, u.inTransit() ? Palette.CRIMSON : Palette.GREEN));
        detail.add(Box.createVerticalStrut(10));

        // Cargo breakdown.
        detail.add(UiKit.label("Cargo (" + u.cargoUsed() + " / " + u.type.capacity + ")", UiKit.HEAD, Palette.INK));
        boolean any = false;
        for (Good gd : Good.ALL) {
            int q = u.cargo[gd.ordinal()];
            if (q > 0) {
                detail.add(UiKit.label("   " + gd.label + ":  " + q, UiKit.BODY, Palette.INK));
                any = true;
            }
        }
        if (!any) detail.add(UiKit.label("   (empty)", UiKit.BODY, Palette.PANEL_LINE));
        detail.add(Box.createVerticalStrut(12));

        // Dispatch controls (only when docked).
        addDispatchControls(s, u);
        detail.add(Box.createVerticalStrut(12));

        // Automated route controls.
        addRouteControls(s, u);

        detail.revalidate();
        detail.repaint();
    }

    private void addDispatchControls(GameState s, TransportUnit u) {
        JPanel row = row();
        row.add(UiKit.label("Dispatch to:", UiKit.HEAD, Palette.INK));
        JComboBox<CityItem> dest = new JComboBox<CityItem>();
        for (City c : eligibleDestinations(s, u)) dest.addItem(new CityItem(c));
        JButton go = UiKit.button("Dispatch");
        boolean canGo = !u.inTransit() && dest.getItemCount() > 0;
        go.setEnabled(canGo);
        dest.setEnabled(canGo);
        go.addActionListener(e -> {
            CityItem item = (CityItem) dest.getSelectedItem();
            if (item == null) return;
            TravelEngine.dispatch(s, u, item.city);
            rebuildDetail();
        });
        row.add(dest);
        row.add(go);
        detail.add(row);
        if (u.inTransit()) {
            detail.add(UiKit.label("   (already sailing — cannot redirect mid-voyage)",
                    UiKit.SMALL, Palette.PANEL_LINE));
        }
    }

    private void addRouteControls(GameState s, TransportUnit u) {
        detail.add(UiKit.label("Automated route", UiKit.HEAD, Palette.INK));
        detail.add(UiKit.label("   " + routeText(s, u), UiKit.BODY, Palette.INK));

        JPanel row = row();
        JComboBox<CityItem> stop = new JComboBox<CityItem>();
        for (City c : eligibleDestinations(s, u)) stop.addItem(new CityItem(c));
        JButton add = UiKit.button("Add Stop");
        add.setEnabled(stop.getItemCount() > 0);
        add.addActionListener(e -> {
            CityItem item = (CityItem) stop.getSelectedItem();
            if (item == null) return;
            u.route.add(item.city.id);
            rebuildDetail();
        });
        JButton clear = UiKit.button("Clear");
        clear.addActionListener(e -> {
            u.route.clear();
            u.routeIndex = 0;
            u.autoRoute = false;
            rebuildDetail();
        });
        row.add(stop);
        row.add(add);
        row.add(clear);
        detail.add(row);

        JCheckBox auto = new JCheckBox("Run this route automatically", u.autoRoute);
        auto.setFont(UiKit.BODY);
        auto.setBackground(Palette.PARCHMENT);
        auto.setForeground(Palette.INK);
        auto.setEnabled(!u.route.isEmpty());
        auto.addActionListener(e -> {
            u.autoRoute = auto.isSelected();
            rebuildDetail();
        });
        detail.add(auto);
    }

    private String routeText(GameState s, TransportUnit u) {
        if (u.route.isEmpty()) return "(none) — add stops to build a trade loop";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < u.route.size(); i++) {
            City c = s.city(u.route.get(i));
            if (i > 0) sb.append(" → ");
            sb.append(c == null ? "?" : c.name);
        }
        sb.append(u.autoRoute ? "   [ACTIVE]" : "   [paused]");
        return sb.toString();
    }

    /** Cities this unit could travel to, honouring sea/land capability. */
    private List<City> eligibleDestinations(GameState s, TransportUnit u) {
        List<City> out = new ArrayList<City>();
        for (City c : s.cities) {
            if (c.id == u.locationCityId) continue;
            // Sea units require a coastal city; land units may reach any city overland.
            if (u.type.sea && !u.type.land && !c.coastal) continue;
            out.add(c);
        }
        return out;
    }

    private JPanel row() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setBackground(Palette.PARCHMENT);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Palette.PARCHMENT);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override public Dimension getPreferredSize() { return new Dimension(900, 680); }

    // ------------------------------------------------------------------
    // Small helper types.
    // ------------------------------------------------------------------
    private static final class CityItem {
        final City city;
        CityItem(City city) { this.city = city; }
        @Override public String toString() {
            return city.name + (city.open ? "" : " (closed)");
        }
    }

    private final class UnitRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof TransportUnit && game.state != null) {
                TransportUnit u = (TransportUnit) value;
                String where;
                if (u.inTransit()) {
                    City d = game.state.city(u.destinationCityId);
                    where = "→ " + (d == null ? "?" : d.name) + " (" + u.turnsRemaining + "t)";
                } else {
                    City at = game.state.city(u.locationCityId);
                    where = "@ " + (at == null ? "?" : at.name);
                }
                setText(u.displayName() + "  " + where);
            }
            setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            if (!isSelected) {
                setBackground(Palette.PANEL);
                setForeground(Palette.INK);
            } else {
                setBackground(new Color(0xE3D2A8));
                setForeground(Palette.INK);
            }
            return this;
        }
    }
}
