package com.whim.startrek.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.whim.startrek.domain.Empire;
import com.whim.startrek.domain.FacilityType;
import com.whim.startrek.domain.GameState;
import com.whim.startrek.domain.ResourceType;
import com.whim.startrek.domain.StarSystem;
import com.whim.startrek.engine.EconomyEngine;

/**
 * Modal popup for a clicked {@link StarSystem}: a build menu over {@link FacilityType}
 * and a trade board wired to {@link EconomyEngine}. Per the contract, OFFICERS are
 * non-tradable and therefore omitted from the trade board entirely.
 */
class SystemDialog {

    private static final long TRADE_LOT = 1000L;

    private final Component owner;
    private final GameState state;
    private final EconomyEngine economy;
    private final StarSystem system;

    SystemDialog(Component owner, GameState state, EconomyEngine economy, StarSystem system) {
        this.owner = owner;
        this.state = state;
        this.economy = economy;
        this.system = system;
    }

    void showDialog() {
        Window w = SwingUtilities.getWindowAncestor(owner);
        final JDialog dialog = new JDialog(w, system.getName(), JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());

        JLabel header = new JLabel(systemHeader(), SwingConstants.CENTER);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        dialog.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Build", buildPanel());
        tabs.addTab("Trade", tradePanel(dialog, header));
        dialog.add(tabs, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel();
        south.add(close);
        dialog.add(south, BorderLayout.SOUTH);

        dialog.setPreferredSize(new Dimension(420, 380));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private String systemHeader() {
        Empire player = state.getPlayerEmpire();
        StringBuilder sb = new StringBuilder("<html><b>").append(esc(system.getName()))
                .append("</b> &nbsp; owner: ")
                .append(system.getOwner() == null ? "independent" : system.getOwner().name())
                .append(" &nbsp; pop: ").append(system.getPopulation());
        if (player != null) {
            sb.append("<br>Treasury — ");
            boolean first = true;
            for (ResourceType r : ResourceType.values()) {
                if (!r.isTradable()) {
                    continue;
                }
                if (!first) {
                    sb.append(", ");
                }
                sb.append(r.name()).append(": ").append(player.getTreasury(r));
                first = false;
            }
        }
        return sb.append("</html>").toString();
    }

    // ---- build menu -----------------------------------------------------

    private JPanel buildPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 3, 6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (final FacilityType ft : FacilityType.values()) {
            final JLabel count = new JLabel(label(ft), SwingConstants.LEFT);
            final JButton build = new JButton("Build +1");
            build.addActionListener(e -> {
                system.setFacility(ft, system.getFacility(ft) + 1);
                count.setText(label(ft));
                owner.repaint();
            });
            panel.add(count);
            panel.add(new JLabel());
            panel.add(build);
        }
        return panel;
    }

    private String label(FacilityType ft) {
        return ft.name() + " ×" + system.getFacility(ft);
    }

    // ---- trade board ----------------------------------------------------

    private JPanel tradePanel(final JDialog dialog, final JLabel header) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        final Empire player = state.getPlayerEmpire();
        if (player == null) {
            panel.add(new JLabel("No player empire."));
            return panel;
        }

        for (final ResourceType r : ResourceType.values()) {
            // OFFICERS (and any other non-tradable) are intentionally excluded.
            if (!r.isTradable()) {
                continue;
            }
            final double price = economy.basePricePer1000(r, state);
            JPanel row = new JPanel(new BorderLayout(6, 0));
            final JLabel info = new JLabel();
            row.add(info, BorderLayout.CENTER);

            JPanel btns = new JPanel();
            JButton buy = new JButton("Buy 1k");
            JButton sell = new JButton("Sell 1k");
            buy.addActionListener(e -> {
                if (!economy.buy(player, r, TRADE_LOT, state)) {
                    warn(dialog, "Cannot buy " + r.name() + " (insufficient credits).");
                }
                refreshRow(info, player, r, price);
                header.setText(systemHeader());
            });
            sell.addActionListener(e -> {
                if (!economy.sell(player, r, TRADE_LOT, state)) {
                    warn(dialog, "Cannot sell " + r.name() + " (insufficient stock).");
                }
                refreshRow(info, player, r, price);
                header.setText(systemHeader());
            });
            btns.add(buy);
            btns.add(sell);
            row.add(btns, BorderLayout.EAST);

            refreshRow(info, player, r, price);
            panel.add(row);
            panel.add(Box.createVerticalStrut(4));
        }

        JLabel note = new JLabel("OFFICERS are non-tradable and hidden from this board.");
        note.setForeground(UiTheme.TEXT_DIM);
        panel.add(Box.createVerticalStrut(6));
        panel.add(note);
        return panel;
    }

    private void refreshRow(JLabel info, Empire player, ResourceType r, double price) {
        info.setText(String.format("%-10s  held %,d   @ %.2f /1k",
                r.name(), player.getTreasury(r), price));
    }

    private void warn(JDialog dialog, String msg) {
        JOptionPane.showMessageDialog(dialog, msg, "Trade", JOptionPane.WARNING_MESSAGE);
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
