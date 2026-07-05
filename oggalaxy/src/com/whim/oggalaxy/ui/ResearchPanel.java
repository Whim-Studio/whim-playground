package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.Catalog;
import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.Formulas;
import com.whim.oggalaxy.api.GameController;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Result;
import com.whim.oggalaxy.api.TechDef;
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
 * Research tab: an empire-wide in-progress research bar plus a card per
 * {@link Ids.TechType} with its level, next-level cost, requirement state and a
 * research button (disabled + reason when not allowed). Research is performed at the
 * Research Lab on the currently selected planet.
 */
public final class ResearchPanel extends JPanel implements Refreshable {

    private final GameController controller;
    private final StatusSink sink;
    private final Catalog catalog;
    private final Card[] cards;
    private final JLabel researchLabel = UiUtil.label("", Palette.TEXT_DIM, Palette.FONT_SMALL);
    private final Gauge researchGauge = new Gauge();
    private Views.GameStateView state;

    public ResearchPanel(GameController controller, StatusSink sink) {
        this.controller = controller;
        this.sink = sink;
        this.catalog = controller.catalog();
        setOpaque(true);
        setBackground(Palette.BG_SPACE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(true);
        top.setBackground(Palette.BG_PANEL);
        top.setBorder(UiUtil.panelBorder("Current Research"));
        top.add(researchLabel, BorderLayout.NORTH);
        top.add(researchGauge, BorderLayout.CENTER);
        JPanel topWrap = new JPanel(new BorderLayout());
        topWrap.setOpaque(false);
        topWrap.setBorder(UiUtil.padded(8, 10, 4, 10));
        topWrap.add(top, BorderLayout.CENTER);
        add(topWrap, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 3, 8, 8));
        grid.setOpaque(false);
        grid.setBorder(UiUtil.padded(4, 10, 10, 10));

        Ids.TechType[] types = Ids.TechType.values();
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
        Views.QueueItemView research = player == null ? null : player.currentResearch();
        if (research == null) {
            researchLabel.setText("No research in progress");
            researchGauge.set(0, "", Palette.BORDER);
        } else {
            researchLabel.setText(research.label());
            researchGauge.set(research.progressFraction(),
                    UiUtil.duration(research.remainingTicks()) + " left", Palette.CRYSTAL);
        }
        for (Card c : cards) {
            c.update(p, player);
        }
    }

    private final class Card extends JPanel {
        private final Ids.TechType type;
        private final JLabel levelLabel = UiUtil.label("", Palette.ACCENT, Palette.FONT_BOLD);
        private final JLabel costLabel = UiUtil.label("");
        private final JLabel infoLabel = UiUtil.label("", Palette.TEXT_DIM, Palette.FONT_SMALL);
        private final JButton researchButton = UiUtil.button("Research", Palette.CRYSTAL);

        Card(Ids.TechType type) {
            this.type = type;
            setOpaque(true);
            setBackground(Palette.BG_PANEL);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Palette.BORDER, 1),
                    UiUtil.padded(6, 8, 6, 8)));
            setLayout(new BorderLayout(0, 4));
            setPreferredSize(new Dimension(220, 118));

            TechDef def = catalog.tech(type);
            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            head.add(UiUtil.label(def.name, Palette.TEXT, Palette.FONT_BOLD), BorderLayout.WEST);
            head.add(levelLabel, BorderLayout.EAST);
            add(head, BorderLayout.NORTH);

            JPanel mid = new JPanel(new GridLayout(0, 1, 0, 2));
            mid.setOpaque(false);
            mid.add(costLabel);
            mid.add(infoLabel);
            add(mid, BorderLayout.CENTER);

            add(researchButton, BorderLayout.SOUTH);
            researchButton.addActionListener(e -> doResearch());
        }

        private void doResearch() {
            Views.PlanetView p = state == null ? null : state.selectedPlanet();
            if (p == null) return;
            Result r = controller.enqueueResearch(type, p.id());
            sink.status(catalog.tech(type).name + ": " + (r.message.isEmpty() ? (r.ok ? "started" : "failed") : r.message), r.ok);
            sink.requestRefresh();
        }

        void update(Views.PlanetView p, Views.EmpireView player) {
            TechDef def = catalog.tech(type);
            int level = player == null ? 0 : player.techLevel(type);
            levelLabel.setText("Lv " + level);
            Cost next = def.costForNextLevel(level);
            costLabel.setText(UiUtil.costHtml(next, p));

            int lab = p == null ? 0 : p.buildingLevel(Ids.BuildingType.RESEARCH_LAB);
            int ticks = Formulas.researchTimeTicks(next, lab);

            String reason = null;
            if (p == null) {
                reason = "No planet";
            } else if (def.maxLevel > 0 && level >= def.maxLevel) {
                reason = "Max level";
            } else {
                String unmet = UiUtil.unmetRequirement(def.requirements, p, player, catalog);
                if (unmet != null) {
                    reason = "Needs " + unmet;
                } else if (player != null && player.currentResearch() != null) {
                    reason = "Lab busy";
                } else if (!UiUtil.canAfford(p, next)) {
                    reason = "Low resources";
                }
            }

            if (reason == null) {
                infoLabel.setText(UiUtil.duration(ticks));
                infoLabel.setForeground(Palette.TEXT_DIM);
                researchButton.setEnabled(true);
                researchButton.setToolTipText(def.description);
            } else {
                infoLabel.setText("\u2716 " + reason);
                infoLabel.setForeground(Palette.WARN);
                researchButton.setEnabled(false);
                researchButton.setToolTipText(reason);
            }
        }
    }
}
