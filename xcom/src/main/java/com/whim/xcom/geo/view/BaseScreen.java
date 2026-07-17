package com.whim.xcom.geo.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

import com.whim.xcom.app.GameContext;
import com.whim.xcom.geo.GeoFactory;
import com.whim.xcom.geo.GeoGame;
import com.whim.xcom.meta.Campaign;
import com.whim.xcom.meta.ManufactureJob;
import com.whim.xcom.meta.ResearchProject;
import com.whim.xcom.meta.SaveGame;
import com.whim.xcom.meta.Soldier;
import com.whim.xcom.rules.def.ManufactureNode;
import com.whim.xcom.rules.def.ResearchNode;

/**
 * The base management screen: science &amp; engineering effort, the research and
 * manufacturing queues, the soldier roster, and save/load. Opened from the
 * Geoscape as a modal panel; all actions mutate the live {@link Campaign} through
 * the {@link GeoGame} so funds and unlocks stay consistent.
 */
public final class BaseScreen extends JPanel {

    private static final File SAVE_FILE = new File("xcom-savegame.json");

    private final transient GameContext ctx;
    private transient GeoGame game;

    private final JLabel header = new JLabel();
    private final JTextArea researchArea = new JTextArea(6, 30);
    private final JTextArea manufactureArea = new JTextArea(6, 30);
    private final JTextArea rosterArea = new JTextArea(10, 60);
    private final JComboBox<String> researchCombo = new JComboBox<String>();
    private final JComboBox<String> manufactureCombo = new JComboBox<String>();
    private final JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));

    public BaseScreen(GameContext ctx, GeoGame game) {
        this.ctx = ctx;
        this.game = game;
        setLayout(new BorderLayout(8, 8));
        setBackground(new Color(12, 18, 24));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(760, 640));

        header.setFont(new Font("Monospaced", Font.BOLD, 15));
        header.setForeground(new Color(160, 220, 200));
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(buildResearchPanel());
        center.add(buildManufacturePanel());
        center.add(buildRosterPanel());
        add(center, BorderLayout.CENTER);
        add(buildSaveBar(), BorderLayout.SOUTH);

        refresh();
    }

    private JPanel section(String title) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 90, 80)), title));
        return p;
    }

    private JTextArea styleArea(JTextArea a) {
        a.setEditable(false);
        a.setBackground(new Color(8, 12, 16));
        a.setForeground(new Color(170, 210, 190));
        a.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return a;
    }

    private JButton button(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Monospaced", Font.BOLD, 12));
        b.setForeground(new Color(220, 235, 225));
        b.setBackground(new Color(22, 40, 34));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 110), 1));
        return b;
    }

    private JPanel buildResearchPanel() {
        JPanel p = section("Research (scientists work per day)");
        p.add(new JScrollPane(styleArea(researchArea)), BorderLayout.CENTER);
        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.add(new JLabel("Start:"));
        controls.add(researchCombo);
        JButton start = button("Research");
        start.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { startResearch(); }
        });
        controls.add(start);
        p.add(controls, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildManufacturePanel() {
        JPanel p = section("Manufacturing (engineers work per hour)");
        p.add(new JScrollPane(styleArea(manufactureArea)), BorderLayout.CENTER);
        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.add(new JLabel("Build:"));
        controls.add(manufactureCombo);
        controls.add(new JLabel("Qty:"));
        controls.add(qtySpinner);
        JButton order = button("Manufacture");
        order.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { orderManufacture(); }
        });
        controls.add(order);
        return p;
    }

    private JPanel buildRosterPanel() {
        JPanel p = section("Soldier Roster");
        p.add(new JScrollPane(styleArea(rosterArea)), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSaveBar() {
        JPanel bar = new JPanel(new GridLayout(1, 3, 8, 0));
        bar.setOpaque(false);
        JButton save = button("Save Game");
        save.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { save(); }
        });
        JButton load = button("Load Game");
        load.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { load(); }
        });
        bar.add(save);
        bar.add(load);
        return bar;
    }

    // ---- actions ------------------------------------------------------------

    private Campaign campaign() {
        return game.campaign();
    }

    private void startResearch() {
        Campaign c = campaign();
        if (c == null || researchCombo.getSelectedItem() == null) {
            return;
        }
        for (ResearchNode n : c.availableResearch(ctx.ruleset())) {
            if (n.name().equals(researchCombo.getSelectedItem())) {
                c.startResearch(n, Math.max(1, c.scientists()));
                break;
            }
        }
        refresh();
    }

    private void orderManufacture() {
        Campaign c = campaign();
        if (c == null || manufactureCombo.getSelectedItem() == null) {
            return;
        }
        for (ManufactureNode n : ctx.ruleset().manufactureProjects()) {
            if (n.name().equals(manufactureCombo.getSelectedItem())) {
                int qty = (Integer) qtySpinner.getValue();
                if (!game.orderManufacture(n, qty, Math.max(1, c.engineers()))) {
                    JOptionPane.showMessageDialog(this,
                            "Cannot build " + n.name() + " (funds or research).",
                            "Manufacture", JOptionPane.WARNING_MESSAGE);
                }
                break;
            }
        }
        refresh();
    }

    private void save() {
        try {
            SaveGame.Snapshot snap = SaveGame.capture(campaign(), game.funds(),
                    game.totalScore(), game.clock().seconds());
            SaveGame.write(snap, SAVE_FILE);
            JOptionPane.showMessageDialog(this, "Saved to " + SAVE_FILE.getAbsolutePath(),
                    "Save Game", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(),
                    "Save Game", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void load() {
        try {
            if (!SAVE_FILE.exists()) {
                JOptionPane.showMessageDialog(this, "No save file found.",
                        "Load Game", JOptionPane.WARNING_MESSAGE);
                return;
            }
            SaveGame.Snapshot snap = SaveGame.read(SAVE_FILE);
            game.setCampaign(SaveGame.restoreCampaign(snap, ctx.ruleset()));
            game.restoreState(snap.funds, snap.score, snap.clockSeconds);
            refresh();
            JOptionPane.showMessageDialog(this, "Campaign loaded.",
                    "Load Game", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage(),
                    "Load Game", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---- refresh ------------------------------------------------------------

    private void refresh() {
        Campaign c = campaign();
        header.setText(String.format("BASE  —  Funds $%,d   Score %d   Scientists %d   Engineers %d",
                game.funds(), game.totalScore(),
                c == null ? 0 : c.scientists(), c == null ? 0 : c.engineers()));

        StringBuilder r = new StringBuilder();
        if (c != null) {
            for (ResearchProject p : c.activeResearch()) {
                if (!p.complete()) {
                    r.append(String.format("%-22s %s  %d%%%n", p.name(),
                            bar(p.percent()), p.percent()));
                }
            }
            if (!c.completedResearch().isEmpty()) {
                r.append("\nCompleted: ").append(String.join(", ", c.completedResearch()));
            }
        }
        researchArea.setText(r.length() == 0 ? "(labs idle — start a project below)" : r.toString());

        StringBuilder m = new StringBuilder();
        if (c != null) {
            for (ManufactureJob j : c.manufacturing()) {
                m.append(String.format("%-22s x%d  %s %d%%%n", j.name(), j.quantityRemaining(),
                        bar(j.percentOfCurrentUnit()), j.percentOfCurrentUnit()));
            }
            if (!c.stores().isEmpty()) {
                m.append("\nStores: ").append(c.stores());
            }
        }
        manufactureArea.setText(m.length() == 0 ? "(workshops idle)" : m.toString());

        StringBuilder s = new StringBuilder();
        s.append(String.format("%-16s %-10s %3s %3s %3s %3s  %3s  %s%n",
                "NAME", "RANK", "TU", "HP", "ACC", "REA", "MIS", "STATUS"));
        if (c != null) {
            for (Soldier sol : c.roster().soldiers()) {
                s.append(String.format("%-16s %-10s %3d %3d %3d %3d  %3d  %s%n",
                        sol.name(), sol.rankName(), sol.timeUnits(), sol.health(),
                        sol.firingAccuracy(), sol.reactions(), sol.missions(),
                        sol.deployable() ? "ready" : ("wounded " + sol.woundedDays() + "d")));
            }
        }
        rosterArea.setText(s.toString());

        refreshCombos();
    }

    private void refreshCombos() {
        Campaign c = campaign();
        researchCombo.removeAllItems();
        manufactureCombo.removeAllItems();
        if (c == null) {
            return;
        }
        for (ResearchNode n : c.availableResearch(ctx.ruleset())) {
            researchCombo.addItem(n.name());
        }
        for (ManufactureNode n : ctx.ruleset().manufactureProjects()) {
            if (c.researchUnlocksManufacture(n)) {
                manufactureCombo.addItem(n.name());
            }
        }
    }

    private static String bar(int pct) {
        int filled = Math.round(pct / 10f);
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            b.append(i < filled ? '#' : '.');
        }
        return b.append(']').toString();
    }
}
