package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Views;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;

/**
 * Reports tab: two lists — combat reports and expedition reports — each with a
 * selectable master list and a formatted detail view (rounds, losses, debris,
 * plunder, moon for combat; outcome, gains and dark matter for expeditions).
 */
public final class ReportsPanel extends JPanel implements Refreshable {

    private final DefaultListModel<Views.CombatReportView> combatModel = new DefaultListModel<Views.CombatReportView>();
    private final JList<Views.CombatReportView> combatList = new JList<Views.CombatReportView>(combatModel);
    private final JTextArea combatDetail = detailArea();

    private final DefaultListModel<Views.ExpeditionReportView> expModel = new DefaultListModel<Views.ExpeditionReportView>();
    private final JList<Views.ExpeditionReportView> expList = new JList<Views.ExpeditionReportView>(expModel);
    private final JTextArea expDetail = detailArea();

    private int lastCombatCount = -1;
    private int lastExpCount = -1;

    public ReportsPanel() {
        setOpaque(true);
        setBackground(Palette.BG_SPACE);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        UiUtil.themeDark(tabs);
        tabs.addTab("Combat", buildCombat());
        tabs.addTab("Expeditions", buildExpeditions());
        add(tabs, BorderLayout.CENTER);
    }

    private JSplitPane buildCombat() {
        combatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        combatList.setBackground(Palette.BG_PANEL);
        combatList.setForeground(Palette.TEXT);
        combatList.setCellRenderer(new CombatRenderer());
        combatList.addListSelectionListener(e -> showCombat());
        JScrollPane left = new JScrollPane(combatList);
        left.setBorder(UiUtil.panelBorder("Battles"));
        left.getViewport().setBackground(Palette.BG_PANEL);
        left.setPreferredSize(new Dimension(240, 100));

        JScrollPane right = new JScrollPane(combatDetail);
        right.setBorder(UiUtil.panelBorder("Detail"));
        right.getViewport().setBackground(Palette.BG_DEEP);

        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        sp.setDividerLocation(250);
        sp.setBorder(null);
        return sp;
    }

    private JSplitPane buildExpeditions() {
        expList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        expList.setBackground(Palette.BG_PANEL);
        expList.setForeground(Palette.TEXT);
        expList.setCellRenderer(new ExpRenderer());
        expList.addListSelectionListener(e -> showExp());
        JScrollPane left = new JScrollPane(expList);
        left.setBorder(UiUtil.panelBorder("Expeditions"));
        left.getViewport().setBackground(Palette.BG_PANEL);
        left.setPreferredSize(new Dimension(240, 100));

        JScrollPane right = new JScrollPane(expDetail);
        right.setBorder(UiUtil.panelBorder("Detail"));
        right.getViewport().setBackground(Palette.BG_DEEP);

        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        sp.setDividerLocation(250);
        sp.setBorder(null);
        return sp;
    }

    private static JTextArea detailArea() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBackground(Palette.BG_DEEP);
        a.setForeground(Palette.TEXT);
        a.setFont(Palette.FONT_MONO);
        a.setBorder(UiUtil.padded(8, 10, 8, 10));
        return a;
    }

    @Override
    public void refresh(Views.GameStateView state) {
        if (state == null) return;
        List<Views.CombatReportView> combats = state.combatReports();
        if (combats.size() != lastCombatCount) {
            lastCombatCount = combats.size();
            int sel = combatList.getSelectedIndex();
            combatModel.clear();
            for (Views.CombatReportView c : combats) combatModel.addElement(c);
            if (sel >= 0 && sel < combatModel.size()) combatList.setSelectedIndex(sel);
            else if (!combatModel.isEmpty()) combatList.setSelectedIndex(combatModel.size() - 1);
        }
        List<Views.ExpeditionReportView> exps = state.expeditionReports();
        if (exps.size() != lastExpCount) {
            lastExpCount = exps.size();
            int sel = expList.getSelectedIndex();
            expModel.clear();
            for (Views.ExpeditionReportView e : exps) expModel.addElement(e);
            if (sel >= 0 && sel < expModel.size()) expList.setSelectedIndex(sel);
            else if (!expModel.isEmpty()) expList.setSelectedIndex(expModel.size() - 1);
        }
    }

    private void showCombat() {
        Views.CombatReportView c = combatList.getSelectedValue();
        if (c == null) {
            combatDetail.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(c.attackerName()).append("  vs  ").append(c.defenderName()).append('\n');
        sb.append("Location ").append(UiUtil.coords(c.location()))
          .append("    T+").append(c.tick()).append("h\n");
        sb.append("Outcome: ").append(c.outcome()).append("\n");
        sb.append("--------------------------------------\n");
        for (String r : c.roundSummaries()) sb.append(r).append('\n');
        sb.append("--------------------------------------\n");
        appendShipLosses(sb, "Attacker losses", c.attackerLosses());
        appendShipLosses(sb, "Defender ship losses", c.defenderShipLosses());
        appendDefenseLosses(sb, "Defender defense losses", c.defenderDefenseLosses());
        sb.append('\n');
        sb.append("Debris field:  ").append(costLine(c.debris())).append('\n');
        sb.append("Plunder:       ").append(costLine(c.plunder())).append('\n');
        sb.append("Moon created:  ").append(c.moonCreated() ? "YES (moon)" : "no").append('\n');
        combatDetail.setText(sb.toString());
        combatDetail.setCaretPosition(0);
    }

    private void appendShipLosses(StringBuilder sb, String head, Map<Ids.ShipType, Integer> losses) {
        if (losses == null || losses.isEmpty()) return;
        sb.append(head).append(":\n");
        for (Map.Entry<Ids.ShipType, Integer> e : losses.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                sb.append("   ").append(e.getValue()).append("× ").append(e.getKey().name()).append('\n');
            }
        }
    }

    private void appendDefenseLosses(StringBuilder sb, String head, Map<Ids.DefenseType, Integer> losses) {
        if (losses == null || losses.isEmpty()) return;
        sb.append(head).append(":\n");
        for (Map.Entry<Ids.DefenseType, Integer> e : losses.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                sb.append("   ").append(e.getValue()).append("× ").append(e.getKey().name()).append('\n');
            }
        }
    }

    private void showExp() {
        Views.ExpeditionReportView x = expList.getSelectedValue();
        if (x == null) {
            expDetail.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("T+").append(x.tick()).append("h\n");
        sb.append("Outcome: ").append(x.outcome()).append("\n\n");
        sb.append(x.detail() == null ? "" : x.detail()).append("\n\n");
        sb.append("Gains:        ").append(costLine(x.gains())).append('\n');
        sb.append("Dark Matter:  ").append(UiUtil.num(x.darkMatter())).append('\n');
        expDetail.setText(sb.toString());
        expDetail.setCaretPosition(0);
    }

    private static String costLine(Cost c) {
        if (c == null || c.structurePoints() == 0 && c.energy == 0) return "—";
        return UiUtil.num(c.metal) + "m  " + UiUtil.num(c.crystal) + "c  " + UiUtil.num(c.deuterium) + "d";
    }

    // ------------------------------------------------------------------
    private final class CombatRenderer extends javax.swing.JLabel
            implements ListCellRenderer<Views.CombatReportView> {
        CombatRenderer() {
            setOpaque(true);
            setBorder(UiUtil.padded(4, 8, 4, 8));
            setFont(Palette.FONT);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Views.CombatReportView> list,
                Views.CombatReportView value, int index, boolean selected, boolean focus) {
            setText("<html><b>" + value.attackerName() + "</b> → " + value.defenderName()
                    + "<br><span style='color:#8c98b2'>" + UiUtil.coords(value.location())
                    + "  T+" + value.tick() + "h</span></html>");
            setForeground(Palette.TEXT);
            setBackground(selected ? Palette.mix(Palette.BG_PANEL, Palette.BAD, 0.28) : Palette.BG_PANEL);
            return this;
        }
    }

    private final class ExpRenderer extends javax.swing.JLabel
            implements ListCellRenderer<Views.ExpeditionReportView> {
        ExpRenderer() {
            setOpaque(true);
            setBorder(UiUtil.padded(4, 8, 4, 8));
            setFont(Palette.FONT);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Views.ExpeditionReportView> list,
                Views.ExpeditionReportView value, int index, boolean selected, boolean focus) {
            setText("<html><b>" + value.outcome() + "</b>"
                    + "<br><span style='color:#8c98b2'>T+" + value.tick() + "h</span></html>");
            setForeground(Palette.TEXT);
            setBackground(selected ? Palette.mix(Palette.BG_PANEL, Palette.ACCENT2, 0.28) : Palette.BG_PANEL);
            return this;
        }
    }
}
