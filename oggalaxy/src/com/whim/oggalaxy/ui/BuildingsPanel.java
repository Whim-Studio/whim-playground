package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.BuildingDef;
import com.whim.oggalaxy.api.Catalog;
import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.Formulas;
import com.whim.oggalaxy.api.GameController;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Result;
import com.whim.oggalaxy.api.Views;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

/**
 * Buildings tab: a card per {@link Ids.BuildingType} showing current level, the cost
 * and build time of the next level, requirement state, and a build button that is
 * disabled with a reason whenever the upgrade is not currently allowed.
 */
public final class BuildingsPanel extends JPanel implements Refreshable {

    private final GameController controller;
    private final StatusSink sink;
    private final Catalog catalog;
    private final Card[] cards;
    private Views.GameStateView state;

    public BuildingsPanel(GameController controller, StatusSink sink) {
        this.controller = controller;
        this.sink = sink;
        this.catalog = controller.catalog();
        setOpaque(true);
        setBackground(Palette.BG_SPACE);
        setLayout(new BorderLayout());

        JPanel grid = new JPanel(new GridLayout(0, 3, 8, 8));
        grid.setOpaque(false);
        grid.setBorder(UiUtil.padded(10, 10, 10, 10));

        Ids.BuildingType[] types = Ids.BuildingType.values();
        cards = new Card[types.length];
        for (int i = 0; i < types.length; i++) {
            cards[i] = new Card(types[i]);
            grid.add(cards[i]);
        }

        JScrollPane sp = new JScrollPane(grid,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getViewport().setBackground(Palette.BG_SPACE);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        add(sp, BorderLayout.CENTER);
    }

    @Override
    public void refresh(Views.GameStateView state) {
        this.state = state;
        Views.PlanetView p = state == null ? null : state.selectedPlanet();
        Views.EmpireView player = state == null ? null : state.player();
        for (Card c : cards) {
            c.update(p, player);
        }
    }

    private final class Card extends JPanel {
        private final Ids.BuildingType type;
        private final JLabel nameLabel;
        private final JLabel levelLabel = UiUtil.label("", Palette.ACCENT, Palette.FONT_BOLD);
        private final JLabel costLabel = UiUtil.label("");
        private final JLabel infoLabel = UiUtil.label("", Palette.TEXT_DIM, Palette.FONT_SMALL);
        private final JButton buildButton = UiUtil.button("Build", new Color(150, 200, 120));

        Card(Ids.BuildingType type) {
            this.type = type;
            setOpaque(true);
            setBackground(Palette.BG_PANEL);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Palette.BORDER, 1),
                    UiUtil.padded(6, 8, 6, 8)));
            setLayout(new BorderLayout(0, 4));
            setPreferredSize(new Dimension(220, 118));

            BuildingDef def = catalog.building(type);
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            nameLabel = UiUtil.label(def.name, Palette.TEXT, Palette.FONT_BOLD);
            top.add(nameLabel, BorderLayout.WEST);
            top.add(levelLabel, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            JPanel mid = new JPanel(new GridLayout(0, 1, 0, 2));
            mid.setOpaque(false);
            mid.add(costLabel);
            mid.add(infoLabel);
            add(mid, BorderLayout.CENTER);

            add(buildButton, BorderLayout.SOUTH);
            buildButton.addActionListener(e -> doBuild());
        }

        private void doBuild() {
            Views.PlanetView p = state == null ? null : state.selectedPlanet();
            if (p == null) return;
            Result r = controller.enqueueBuilding(p.id(), type);
            sink.status(catalog.building(type).name + ": " + (r.message.isEmpty() ? (r.ok ? "queued" : "failed") : r.message), r.ok);
            sink.requestRefresh();
        }

        void update(Views.PlanetView p, Views.EmpireView player) {
            BuildingDef def = catalog.building(type);
            int level = p == null ? 0 : p.buildingLevel(type);
            levelLabel.setText("Lv " + level);
            Cost next = def.costForNextLevel(level);
            costLabel.setText(UiUtil.costHtml(next, p));

            int robo = p == null ? 0 : p.buildingLevel(Ids.BuildingType.ROBOTICS_FACTORY);
            int nanite = p == null ? 0 : p.buildingLevel(Ids.BuildingType.NANITE_FACTORY);
            int ticks = Formulas.buildTimeTicks(next, robo, nanite);

            String reason = null;
            if (p == null) {
                reason = "No planet";
            } else if (def.moonOnly && !p.isMoon()) {
                reason = "Moon only";
            } else if (!def.moonOnly && p.isMoon()) {
                reason = "Planet only";
            } else if (def.maxLevel > 0 && level >= def.maxLevel) {
                reason = "Max level";
            } else {
                String unmet = UiUtil.unmetRequirement(def.requirements, p, player, catalog);
                if (unmet != null) {
                    reason = "Needs " + unmet;
                } else if (p.currentConstruction() != null) {
                    reason = "Building busy";
                } else if (!UiUtil.canAfford(p, next)) {
                    reason = "Low resources";
                }
            }

            if (reason == null) {
                infoLabel.setText(UiUtil.duration(ticks));
                infoLabel.setForeground(Palette.TEXT_DIM);
                buildButton.setEnabled(true);
                buildButton.setToolTipText(def.description);
            } else {
                infoLabel.setText("\u2716 " + reason);
                infoLabel.setForeground(Palette.WARN);
                buildButton.setEnabled(false);
                buildButton.setToolTipText(reason);
            }
        }
    }
}
