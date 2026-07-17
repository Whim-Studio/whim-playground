package com.whim.xcom.geo.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.whim.xcom.app.GameContext;
import com.whim.xcom.meta.Campaign;
import com.whim.xcom.meta.Ufopaedia;

/**
 * The in-game UFOpaedia: a browsable list of the encyclopedia entries the player
 * has unlocked (via research and captures), with a generated description and a
 * small procedurally-drawn glyph per entry. Reads the pure {@link Ufopaedia}
 * catalog so its gating and text stay testable and data-driven.
 */
public final class UfopaediaScreen extends JPanel {

    private final transient List<Ufopaedia.Entry> entries;

    private final JList<String> list = new JList<String>();
    private final JLabel title = new JLabel(" ");
    private final JTextArea body = new JTextArea();
    private final GlyphCanvas glyph = new GlyphCanvas();

    public UfopaediaScreen(GameContext ctx, Campaign campaign) {
        this.entries = Ufopaedia.unlockedEntries(ctx.ruleset(), campaign);
        setLayout(new BorderLayout(8, 8));
        setBackground(new Color(10, 14, 20));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(680, 460));

        JLabel head = new JLabel("UFOPAEDIA  —  " + entries.size() + " entries unlocked");
        head.setFont(new Font("Monospaced", Font.BOLD, 15));
        head.setForeground(new Color(160, 220, 240));
        add(head, BorderLayout.NORTH);

        DefaultListModel<String> model = new DefaultListModel<String>();
        for (Ufopaedia.Entry e : entries) {
            model.addElement("[" + shortCat(e.category()) + "] " + e.title());
        }
        list.setModel(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(new Color(8, 12, 16));
        list.setForeground(new Color(180, 210, 200));
        list.setFont(new Font("Monospaced", Font.PLAIN, 12));
        list.addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    showSelected();
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setPreferredSize(new Dimension(230, 100));
        listScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(50, 80, 90)), "Entries"));
        add(listScroll, BorderLayout.WEST);

        add(buildDetail(), BorderLayout.CENTER);

        if (!entries.isEmpty()) {
            list.setSelectedIndex(0);
        } else {
            title.setText("No entries yet");
            body.setText("Research alien technology and capture live aliens to fill the UFOpaedia.");
        }
    }

    private JComponent buildDetail() {
        JPanel detail = new JPanel(new BorderLayout(6, 6));
        detail.setOpaque(false);

        title.setFont(new Font("Monospaced", Font.BOLD, 14));
        title.setForeground(new Color(230, 210, 140));
        title.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.add(glyph, BorderLayout.WEST);
        top.add(title, BorderLayout.CENTER);
        detail.add(top, BorderLayout.NORTH);

        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setBackground(new Color(6, 10, 14));
        body.setForeground(new Color(180, 210, 200));
        body.setFont(new Font("Monospaced", Font.PLAIN, 13));
        body.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        detail.add(new JScrollPane(body), BorderLayout.CENTER);
        return detail;
    }

    private void showSelected() {
        int i = list.getSelectedIndex();
        if (i < 0 || i >= entries.size()) {
            return;
        }
        Ufopaedia.Entry e = entries.get(i);
        title.setText(e.title());
        body.setText(e.body());
        body.setCaretPosition(0);
        glyph.setCategory(e.category());
    }

    private static String shortCat(Ufopaedia.Category c) {
        switch (c) {
            case WEAPONS: return "WPN";
            case EQUIPMENT: return "EQP";
            case ALIENS: return "ALN";
            case UFOS: return "UFO";
            case FACILITIES: return "FAC";
            default: return "?";
        }
    }

    /** A small Java2D glyph that changes with the entry category — original art. */
    private static final class GlyphCanvas extends JComponent {
        private Ufopaedia.Category category = Ufopaedia.Category.WEAPONS;

        GlyphCanvas() {
            setPreferredSize(new Dimension(72, 72));
        }

        void setCategory(Ufopaedia.Category c) {
            this.category = c;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(16, 24, 32));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(60, 100, 120));
            g2.drawRect(0, 0, w - 1, h - 1);
            int cx = w / 2;
            int cy = h / 2;
            switch (category) {
                case WEAPONS:
                    g2.setColor(new Color(210, 210, 120));
                    g2.fillRect(cx - 22, cy - 4, 36, 8);
                    g2.fillRect(cx - 10, cy + 4, 8, 12);
                    break;
                case EQUIPMENT:
                    g2.setColor(new Color(120, 180, 220));
                    g2.fillRoundRect(cx - 14, cy - 18, 28, 36, 10, 10);
                    break;
                case ALIENS:
                    g2.setColor(new Color(150, 220, 150));
                    g2.fillOval(cx - 16, cy - 18, 32, 30);
                    g2.setColor(new Color(20, 20, 20));
                    g2.fillOval(cx - 9, cy - 6, 6, 9);
                    g2.fillOval(cx + 3, cy - 6, 6, 9);
                    break;
                case UFOS:
                    g2.setColor(new Color(180, 180, 220));
                    g2.fillOval(cx - 22, cy - 4, 44, 12);
                    g2.setColor(new Color(150, 210, 240));
                    g2.fillOval(cx - 10, cy - 14, 20, 16);
                    break;
                case FACILITIES:
                default:
                    g2.setColor(new Color(150, 190, 160));
                    g2.fillRect(cx - 16, cy - 6, 32, 22);
                    g2.setColor(new Color(90, 130, 110));
                    g2.fillRect(cx - 6, cy - 18, 12, 12);
                    break;
            }
        }
    }
}
