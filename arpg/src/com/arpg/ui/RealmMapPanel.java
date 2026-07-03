package com.arpg.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.arpg.model.Realm;

/**
 * Zone-selection screen (a.k.a. RealmMapPanel). Draws a Graphics2D placeholder
 * map: one node per realm laid out on a path, sized/colored by difficulty tier,
 * with the current realm highlighted. Clicking a node fires MOVE_TO_REALM.
 * A control strip offers Save / Load and Advance / Tick so the player can drive
 * encounters.
 */
public class RealmMapPanel extends JPanel {

    private final ActionSink sink;
    private final GameControls controls;
    private final MapCanvas canvas = new MapCanvas();

    private List<Realm> realms = new ArrayList<Realm>();
    private String currentRealmId;

    public RealmMapPanel(ActionSink sink, GameControls controls) {
        super(new BorderLayout(6, 6));
        this.sink = sink;
        this.controls = controls;
        setBackground(UiTheme.BG_PANEL);
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("Realms");
        title.setFont(UiTheme.TITLE);
        title.setForeground(UiTheme.FG_TEXT);
        add(title, BorderLayout.NORTH);

        add(canvas, BorderLayout.CENTER);
        add(buildControls(), BorderLayout.SOUTH);
    }

    private JPanel buildControls() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bar.setOpaque(false);

        JButton advance = new JButton("Advance Encounter");
        advance.setFocusable(false);
        advance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sink.submit(UiActions.advanceEncounter());
            }
        });

        JButton tick = new JButton("Tick");
        tick.setFocusable(false);
        tick.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controls.tick();
            }
        });

        JButton save = new JButton("Save");
        save.setFocusable(false);
        save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controls.saveGame();
            }
        });

        JButton load = new JButton("Load");
        load.setFocusable(false);
        load.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controls.loadGame();
            }
        });

        bar.add(advance);
        bar.add(tick);
        bar.add(save);
        bar.add(load);
        return bar;
    }

    /** Provide the full realm list (from GameEngine.getAvailableRealms()). */
    public void setRealms(List<Realm> realms) {
        this.realms = realms == null ? new ArrayList<Realm>() : realms;
        canvas.repaint();
    }

    /** Mark which realm the player currently occupies. */
    public void setCurrentRealm(Realm realm) {
        this.currentRealmId = realm == null ? null : realm.getId();
        canvas.repaint();
    }

    /** The clickable node canvas. */
    private final class MapCanvas extends JPanel {
        private final List<java.awt.Rectangle> hitBoxes = new ArrayList<java.awt.Rectangle>();

        MapCanvas() {
            setBackground(UiTheme.BG_DARK);
            setPreferredSize(new Dimension(520, 300));
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    handleClick(e.getPoint());
                }
            });
        }

        private void handleClick(Point p) {
            for (int i = 0; i < hitBoxes.size() && i < realms.size(); i++) {
                if (hitBoxes.get(i).contains(p)) {
                    sink.submit(UiActions.moveToRealm(realms.get(i).getId()));
                    return;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            drawStars(g2, w, h);

            hitBoxes.clear();
            int n = realms.size();
            if (n == 0) {
                g2.setColor(UiTheme.FG_MUTED);
                g2.setFont(UiTheme.BODY);
                g2.drawString("No realms available.", 16, 24);
                g2.dispose();
                return;
            }

            int margin = 60;
            int usableW = Math.max(1, w - margin * 2);
            int node = 46;
            Point prev = null;
            for (int i = 0; i < n; i++) {
                Realm realm = realms.get(i);
                double t = n == 1 ? 0.5 : (double) i / (double) (n - 1);
                int cx = margin + (int) Math.round(usableW * t);
                // Gentle sine weave so the path isn't a straight line.
                int cy = h / 2 + (int) Math.round(Math.sin(t * Math.PI * 2) * (h / 6.0));

                if (prev != null) {
                    g2.setColor(new Color(0x50, 0x50, 0x60));
                    g2.drawLine(prev.x, prev.y, cx, cy);
                }
                prev = new Point(cx, cy);

                drawNode(g2, realm, cx, cy, node);
                hitBoxes.add(new java.awt.Rectangle(cx - node / 2, cy - node / 2, node, node));
            }
            g2.dispose();
        }

        private void drawNode(Graphics2D g2, Realm realm, int cx, int cy, int node) {
            int tier = realm.getDifficultyTier();
            Color base = tierColor(tier);
            boolean isCurrent = realm.getId() != null && realm.getId().equals(currentRealmId);

            g2.setColor(base.darker());
            g2.fillOval(cx - node / 2, cy - node / 2, node, node);
            g2.setColor(base);
            g2.fillOval(cx - node / 2 + 4, cy - node / 2 + 4, node - 8, node - 8);

            if (isCurrent) {
                g2.setColor(UiTheme.LOOT);
                g2.setStroke(new java.awt.BasicStroke(3f));
                g2.drawOval(cx - node / 2 - 3, cy - node / 2 - 3, node + 6, node + 6);
                g2.setStroke(new java.awt.BasicStroke(1f));
            }

            g2.setColor(Color.WHITE);
            g2.setFont(UiTheme.BODY_BOLD);
            String tierLabel = "T" + tier;
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(tierLabel, cx - fm.stringWidth(tierLabel) / 2, cy + fm.getAscent() / 2 - 2);

            g2.setColor(UiTheme.FG_TEXT);
            g2.setFont(UiTheme.BODY);
            String name = realm.getName();
            java.awt.FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(name, cx - fm2.stringWidth(name) / 2, cy + node / 2 + 14);
        }

        private void drawStars(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(0x26, 0x26, 0x30));
            // Deterministic pseudo-scatter (no Random, keeps paint stable).
            for (int i = 0; i < 60; i++) {
                int x = (i * 73 + 17) % Math.max(1, w);
                int y = (i * 129 + 41) % Math.max(1, h);
                g2.fillOval(x, y, 2, 2);
            }
        }

        private Color tierColor(int tier) {
            switch (Math.max(1, Math.min(5, tier))) {
                case 1: return new Color(0x4FBF6A);
                case 2: return new Color(0x2E86DE);
                case 3: return new Color(0xA335EE);
                case 4: return new Color(0xF2792E);
                default: return new Color(0xC0392B);
            }
        }
    }
}
