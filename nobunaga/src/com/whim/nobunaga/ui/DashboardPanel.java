package com.whim.nobunaga.ui;

import com.whim.nobunaga.domain.Daimyo;
import com.whim.nobunaga.domain.GameState;
import com.whim.nobunaga.domain.Province;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * EAST component: a dense monospaced retro readout of the currently selected
 * province — owner, treasury, provisions, loyalty, tax, flood control,
 * cultivation (wealth), soldiers and the adjacency list.
 *
 * <p>Pure view: {@link #refresh()} rebuilds the text straight from the live
 * {@link GameState}; it never mutates anything.
 */
public final class DashboardPanel extends JPanel {

    private static final Color BG = new Color(22, 22, 28);
    private static final Color FG = new Color(210, 226, 210);

    private final GameController controller;
    private final JTextArea area;

    public DashboardPanel(GameController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        setBackground(BG);
        setPreferredSize(new Dimension(280, 760));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBackground(BG);
        area.setForeground(FG);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(60, 70, 60)));
        add(scroll, BorderLayout.CENTER);

        refresh();
    }

    /** Rebuilds the readout from the selected province. */
    public void refresh() {
        Province p = controller.selectedProvince();
        if (p == null) {
            area.setText("\n  Select a province on the map\n  to inspect its fief.\n");
            return;
        }
        GameState s = controller.state();
        StringBuilder sb = new StringBuilder();
        sb.append("=========================\n");
        sb.append(pad(p.getName(), 16)).append(" #").append(p.getId()).append("\n");
        sb.append("=========================\n");
        sb.append(line("Owner", ownerName(s, p)));
        sb.append(line("Gold", p.getGold() + " kan"));
        sb.append(line("Rice", p.getRice() + " koku"));
        sb.append("\n");
        sb.append(line("Loyalty", bar(p.getLoyalty()) + " " + p.getLoyalty()));
        sb.append(line("Tax %", bar(p.getTaxRate()) + " " + p.getTaxRate()));
        sb.append(line("Flood", bar(p.getFloodControl()) + " " + p.getFloodControl()));
        sb.append(line("Wealth", bar(p.getCultivation()) + " " + p.getCultivation()));
        sb.append("  (cultivation)\n");
        sb.append("\n");
        sb.append(line("Soldiers", String.valueOf(p.getSoldiers())));
        sb.append("\n-------------------------\n");
        sb.append("Adjacent provinces:\n");
        List<Integer> adj = p.getAdjacent();
        if (adj.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (Integer id : adj) {
                Province q = s.province(id.intValue());
                sb.append("  ").append(q.isNeutral() ? "·" : tag(s, q))
                        .append(" ").append(q.getName())
                        .append(" (").append(q.getSoldiers()).append(")\n");
            }
        }
        area.setText(sb.toString());
        area.setCaretPosition(0);
    }

    private static String ownerName(GameState s, Province p) {
        if (p.isNeutral()) {
            return "Neutral";
        }
        Daimyo d = s.daimyo(p.getOwnerId());
        return d.getName() + " (" + d.getAbbrev() + ")";
    }

    private static String tag(GameState s, Province q) {
        return s.daimyo(q.getOwnerId()).getAbbrev();
    }

    private static String line(String key, String value) {
        return pad(key, 9) + ": " + value + "\n";
    }

    private static String pad(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /** A tiny 10-cell ASCII gauge for a 0..100 stat. */
    private static String bar(int value) {
        int filled = Math.max(0, Math.min(10, Math.round(value / 10f)));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? '#' : '.');
        }
        return sb.append("]").toString();
    }
}
