package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.Catalog;
import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.DefenseDef;
import com.whim.oggalaxy.api.GameController;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Result;
import com.whim.oggalaxy.api.ShipDef;
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
import java.util.List;

/**
 * Shipyard tab: build cards (with a count spinner) for every ship and defense type,
 * a live shipyard build queue, and the current on-planet fleet & defense inventory.
 */
public final class ShipyardPanel extends JPanel implements Refreshable {

    private final GameController controller;
    private final StatusSink sink;
    private final Catalog catalog;

    private final JPanel buildColumn = new JPanel();
    private final ShipCard[] shipCards;
    private final DefenseCard[] defenseCards;

    private final JPanel queueList = new JPanel();
    private final JPanel fleetList = new JPanel();
    private Views.GameStateView state;

    public ShipyardPanel(GameController controller, StatusSink sink) {
        this.controller = controller;
        this.sink = sink;
        this.catalog = controller.catalog();
        setOpaque(true);
        setBackground(Palette.BG_SPACE);
        setLayout(new BorderLayout(8, 0));
        setBorder(UiUtil.padded(8, 8, 8, 8));

        buildColumn.setOpaque(false);
        buildColumn.setLayout(new BoxLayout(buildColumn, BoxLayout.Y_AXIS));

        buildColumn.add(sectionLabel("Ships"));
        Ids.ShipType[] ships = Ids.ShipType.values();
        shipCards = new ShipCard[ships.length];
        JPanel shipGrid = flowGrid();
        for (int i = 0; i < ships.length; i++) {
            shipCards[i] = new ShipCard(ships[i]);
            shipGrid.add(shipCards[i]);
        }
        buildColumn.add(shipGrid);
        buildColumn.add(Box.createVerticalStrut(8));
        buildColumn.add(sectionLabel("Defenses"));
        Ids.DefenseType[] defs = Ids.DefenseType.values();
        defenseCards = new DefenseCard[defs.length];
        JPanel defGrid = flowGrid();
        for (int i = 0; i < defs.length; i++) {
            defenseCards[i] = new DefenseCard(defs[i]);
            defGrid.add(defenseCards[i]);
        }
        buildColumn.add(defGrid);

        JScrollPane sp = new JScrollPane(buildColumn,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getViewport().setBackground(Palette.BG_SPACE);
        sp.getVerticalScrollBar().setUnitIncrement(20);
        add(sp, BorderLayout.CENTER);

        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(240, 100));

        queueList.setOpaque(true);
        queueList.setBackground(Palette.BG_PANEL);
        queueList.setLayout(new BoxLayout(queueList, BoxLayout.Y_AXIS));
        JScrollPane qsp = titledScroll(queueList, "Build Queue");
        qsp.setPreferredSize(new Dimension(240, 200));
        qsp.setMaximumSize(new Dimension(240, 220));
        side.add(qsp);
        side.add(Box.createVerticalStrut(8));

        fleetList.setOpaque(true);
        fleetList.setBackground(Palette.BG_PANEL);
        fleetList.setLayout(new BoxLayout(fleetList, BoxLayout.Y_AXIS));
        JScrollPane fsp = titledScroll(fleetList, "On-Planet Fleet & Defense");
        fsp.setPreferredSize(new Dimension(240, 320));
        side.add(fsp);

        add(side, BorderLayout.EAST);
    }

    private static JPanel flowGrid() {
        JPanel p = new JPanel(new java.awt.GridLayout(0, 3, 6, 6));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private static JLabel sectionLabel(String s) {
        JLabel l = UiUtil.label(s, Palette.ACCENT, Palette.FONT_TITLE);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(UiUtil.padded(6, 2, 4, 0));
        return l;
    }

    private JScrollPane titledScroll(JPanel content, String title) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(true);
        wrap.setBackground(Palette.BG_PANEL);
        wrap.add(content, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(wrap,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(UiUtil.panelBorder(title));
        sp.getViewport().setBackground(Palette.BG_PANEL);
        sp.setBackground(Palette.BG_PANEL);
        sp.setAlignmentX(LEFT_ALIGNMENT);
        return sp;
    }

    @Override
    public void refresh(Views.GameStateView state) {
        this.state = state;
        Views.PlanetView p = state == null ? null : state.selectedPlanet();
        Views.EmpireView player = state == null ? null : state.player();
        for (ShipCard c : shipCards) c.update(p, player);
        for (DefenseCard c : defenseCards) c.update(p, player);

        queueList.removeAll();
        if (p != null) {
            List<Views.QueueItemView> q = p.shipyardQueue();
            if (q.isEmpty()) {
                queueList.add(UiUtil.label("  (empty)", Palette.TEXT_FAINT, Palette.FONT_SMALL));
            } else {
                for (Views.QueueItemView item : q) {
                    JPanel row = new JPanel(new BorderLayout());
                    row.setOpaque(false);
                    row.setBorder(UiUtil.padded(2, 4, 2, 4));
                    row.add(UiUtil.label(item.label(), Palette.TEXT, Palette.FONT_SMALL), BorderLayout.NORTH);
                    Gauge g = new Gauge();
                    g.setPreferredSize(new Dimension(210, 14));
                    g.set(item.progressFraction(), UiUtil.duration(item.remainingTicks()), Palette.ACCENT2);
                    row.add(g, BorderLayout.CENTER);
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                    queueList.add(row);
                }
            }
        }
        queueList.revalidate();
        queueList.repaint();

        fleetList.removeAll();
        if (p != null) {
            boolean any = false;
            for (Ids.ShipType t : Ids.ShipType.values()) {
                int n = p.shipCount(t);
                if (n > 0) {
                    fleetList.add(inventoryRow(new GlyphIcon(18).ship(t, Palette.ACCENT),
                            catalog.ship(t).name, n));
                    any = true;
                }
            }
            for (Ids.DefenseType t : Ids.DefenseType.values()) {
                int n = p.defenseCount(t);
                if (n > 0) {
                    fleetList.add(inventoryRow(new GlyphIcon(18).defense(t, Palette.BAD),
                            catalog.defense(t).name, n));
                    any = true;
                }
            }
            if (!any) fleetList.add(UiUtil.label("  (no units)", Palette.TEXT_FAINT, Palette.FONT_SMALL));
        }
        fleetList.revalidate();
        fleetList.repaint();
    }

    private JPanel inventoryRow(GlyphIcon icon, String name, int count) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setBorder(UiUtil.padded(1, 4, 1, 4));
        icon.setPreferredSize(new Dimension(18, 18));
        row.add(icon, BorderLayout.WEST);
        row.add(UiUtil.label(name, Palette.TEXT, Palette.FONT_SMALL), BorderLayout.CENTER);
        row.add(UiUtil.label(UiUtil.num(count), Palette.OK, Palette.FONT_BOLD), BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        return row;
    }

    // ------------------------------------------------------------------
    private abstract class UnitCard extends JPanel {
        final JLabel countLabel = UiUtil.label("×0", Palette.OK, Palette.FONT_BOLD);
        final JLabel costLabel = UiUtil.label("");
        final JLabel infoLabel = UiUtil.label("", Palette.TEXT_DIM, Palette.FONT_SMALL);
        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 0, 1_000_000, 1));
        final JButton buildButton = UiUtil.button("Build", Palette.ACCENT2);

        UnitCard(String name, GlyphIcon icon, String tooltip) {
            setOpaque(true);
            setBackground(Palette.BG_PANEL);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Palette.BORDER, 1),
                    UiUtil.padded(5, 6, 5, 6)));
            setLayout(new BorderLayout(4, 3));
            setPreferredSize(new Dimension(210, 96));
            setToolTipText(tooltip);

            JPanel top = new JPanel(new BorderLayout(4, 0));
            top.setOpaque(false);
            icon.setPreferredSize(new Dimension(22, 22));
            top.add(icon, BorderLayout.WEST);
            top.add(UiUtil.label(name, Palette.TEXT, Palette.FONT_BOLD), BorderLayout.CENTER);
            top.add(countLabel, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            JPanel mid = new JPanel(new java.awt.GridLayout(0, 1, 0, 1));
            mid.setOpaque(false);
            mid.add(costLabel);
            mid.add(infoLabel);
            add(mid, BorderLayout.CENTER);

            JPanel bot = new JPanel(new BorderLayout(4, 0));
            bot.setOpaque(false);
            spinner.setPreferredSize(new Dimension(70, 22));
            bot.add(spinner, BorderLayout.WEST);
            bot.add(buildButton, BorderLayout.CENTER);
            add(bot, BorderLayout.SOUTH);
        }

        int amount() {
            Object v = spinner.getValue();
            return v instanceof Number ? ((Number) v).intValue() : 0;
        }

        void applyReason(String reason, String kindNote) {
            if (reason == null) {
                infoLabel.setText(kindNote);
                infoLabel.setForeground(Palette.TEXT_DIM);
                buildButton.setEnabled(true);
                spinner.setEnabled(true);
            } else {
                infoLabel.setText("\u2716 " + reason);
                infoLabel.setForeground(Palette.WARN);
                buildButton.setEnabled(false);
                spinner.setEnabled(false);
            }
        }
    }

    private final class ShipCard extends UnitCard {
        private final Ids.ShipType type;

        ShipCard(Ids.ShipType type) {
            super(catalog.ship(type).name, new GlyphIcon(22).ship(type, Palette.ACCENT), catalog.ship(type).description);
            this.type = type;
            buildButton.addActionListener(e -> {
                Views.PlanetView p = state == null ? null : state.selectedPlanet();
                if (p == null || amount() <= 0) return;
                Result r = controller.enqueueShip(p.id(), type, amount());
                sink.status(catalog.ship(type).name + " ×" + amount() + ": "
                        + (r.message.isEmpty() ? (r.ok ? "queued" : "failed") : r.message), r.ok);
                sink.requestRefresh();
            });
        }

        void update(Views.PlanetView p, Views.EmpireView player) {
            ShipDef def = catalog.ship(type);
            int have = p == null ? 0 : p.shipCount(type);
            countLabel.setText("×" + UiUtil.num(have));
            costLabel.setText(UiUtil.costHtml(def.cost, p));
            String reason = disableReason(p, player, def.requirements, def.cost);
            applyReason(reason, def.civil ? "civil" : "warship");
        }
    }

    private final class DefenseCard extends UnitCard {
        private final Ids.DefenseType type;

        DefenseCard(Ids.DefenseType type) {
            super(catalog.defense(type).name, new GlyphIcon(22).defense(type, Palette.BAD), catalog.defense(type).description);
            this.type = type;
            if (catalog.defense(type).maxCount == 1) {
                spinner.setValue(1);
            }
            buildButton.addActionListener(e -> {
                Views.PlanetView p = state == null ? null : state.selectedPlanet();
                if (p == null || amount() <= 0) return;
                Result r = controller.enqueueDefense(p.id(), type, amount());
                sink.status(catalog.defense(type).name + " ×" + amount() + ": "
                        + (r.message.isEmpty() ? (r.ok ? "queued" : "failed") : r.message), r.ok);
                sink.requestRefresh();
            });
        }

        void update(Views.PlanetView p, Views.EmpireView player) {
            DefenseDef def = catalog.defense(type);
            int have = p == null ? 0 : p.defenseCount(type);
            countLabel.setText("×" + UiUtil.num(have));
            costLabel.setText(UiUtil.costHtml(def.cost, p));
            String reason = disableReason(p, player, def.requirements, def.cost);
            if (reason == null && def.maxCount == 1 && have >= 1) reason = "Only one allowed";
            applyReason(reason, def.isShieldDome ? "shield dome" : "defense");
        }
    }

    private String disableReason(Views.PlanetView p, Views.EmpireView player,
                                 List<com.whim.oggalaxy.api.Requirement> reqs, Cost cost) {
        if (p == null) return "No planet";
        if (p.isMoon()) return "Not on moon";
        String unmet = UiUtil.unmetRequirement(reqs, p, player, catalog);
        if (unmet != null) return "Needs " + unmet;
        if (!UiUtil.canAfford(p, cost)) return "Low resources";
        return null;
    }
}
