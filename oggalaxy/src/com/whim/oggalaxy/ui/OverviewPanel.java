package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Views;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;

/**
 * Overview tab: procedural art of the selected planet plus its fields, temperature,
 * resource detail and current construction progress.
 */
public final class OverviewPanel extends JPanel implements Refreshable {

    private final PlanetArt art = new PlanetArt();
    private final JLabel nameLabel = UiUtil.label("", Palette.ACCENT, Palette.FONT_BIG);
    private final JLabel coordLabel = UiUtil.label("", Palette.TEXT_DIM, Palette.FONT);
    private final JLabel fieldsLabel = UiUtil.label("");
    private final JLabel tempLabel = UiUtil.label("");
    private final JLabel[] resVals = new JLabel[3];
    private final JLabel[] resProd = new JLabel[3];
    private final JLabel energyLabel = UiUtil.label("");
    private final JLabel dmLabel = UiUtil.label("");
    private final Gauge construction = new Gauge();
    private final JLabel constructionLabel = UiUtil.label("", Palette.TEXT_DIM, Palette.FONT_SMALL);

    private static final Ids.ResourceType[] MCD = {
            Ids.ResourceType.METAL, Ids.ResourceType.CRYSTAL, Ids.ResourceType.DEUTERIUM};

    public OverviewPanel() {
        setOpaque(true);
        setBackground(Palette.BG_SPACE);
        setBorder(UiUtil.padded(10, 10, 10, 10));
        setLayout(new BorderLayout(14, 0));

        art.setPreferredSize(new Dimension(300, 300));
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(art, BorderLayout.CENTER);
        add(left, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        nameLabel.setAlignmentX(LEFT_ALIGNMENT);
        coordLabel.setAlignmentX(LEFT_ALIGNMENT);
        right.add(nameLabel);
        right.add(coordLabel);
        right.add(javax.swing.Box.createVerticalStrut(10));

        JPanel planetInfo = new JPanel(new GridLayout(0, 1, 0, 4));
        planetInfo.setOpaque(true);
        planetInfo.setBackground(Palette.BG_PANEL);
        planetInfo.setBorder(UiUtil.panelBorder("Planet"));
        planetInfo.add(fieldsLabel);
        planetInfo.add(tempLabel);
        planetInfo.setAlignmentX(LEFT_ALIGNMENT);
        planetInfo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        right.add(planetInfo);
        right.add(javax.swing.Box.createVerticalStrut(8));

        JPanel resPanel = new JPanel(new GridLayout(0, 3, 8, 4));
        resPanel.setOpaque(true);
        resPanel.setBackground(Palette.BG_PANEL);
        resPanel.setBorder(UiUtil.panelBorder("Resources"));
        String[] names = {"Metal", "Crystal", "Deuterium"};
        for (int i = 0; i < 3; i++) {
            JPanel col = new JPanel();
            col.setOpaque(false);
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            JLabel head = UiUtil.label(names[i], Palette.resourceColor(MCD[i]), Palette.FONT_BOLD);
            resVals[i] = UiUtil.label("0", Palette.TEXT, Palette.FONT_BOLD);
            resProd[i] = UiUtil.label("+0/h", Palette.OK, Palette.FONT_SMALL);
            col.add(head);
            col.add(resVals[i]);
            col.add(resProd[i]);
            resPanel.add(col);
        }
        resPanel.setAlignmentX(LEFT_ALIGNMENT);
        resPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        right.add(resPanel);
        right.add(javax.swing.Box.createVerticalStrut(8));

        JPanel energyPanel = new JPanel(new GridLayout(0, 2, 8, 4));
        energyPanel.setOpaque(true);
        energyPanel.setBackground(Palette.BG_PANEL);
        energyPanel.setBorder(UiUtil.panelBorder("Energy & Dark Matter"));
        energyPanel.add(energyLabel);
        energyPanel.add(dmLabel);
        energyPanel.setAlignmentX(LEFT_ALIGNMENT);
        energyPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        right.add(energyPanel);
        right.add(javax.swing.Box.createVerticalStrut(8));

        JPanel cPanel = new JPanel(new BorderLayout(0, 4));
        cPanel.setOpaque(true);
        cPanel.setBackground(Palette.BG_PANEL);
        cPanel.setBorder(UiUtil.panelBorder("Construction"));
        cPanel.add(constructionLabel, BorderLayout.NORTH);
        cPanel.add(construction, BorderLayout.CENTER);
        cPanel.setAlignmentX(LEFT_ALIGNMENT);
        cPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        right.add(cPanel);
        right.add(javax.swing.Box.createVerticalGlue());

        add(right, BorderLayout.CENTER);
    }

    @Override
    public void refresh(Views.GameStateView state) {
        Views.PlanetView p = state == null ? null : state.selectedPlanet();
        if (p == null) {
            nameLabel.setText("No planet");
            return;
        }
        art.set(p);
        nameLabel.setText(p.name() + (p.isMoon() ? " (Moon)" : ""));
        coordLabel.setText(UiUtil.coords(p.galaxy(), p.system(), p.position())
                + (p.hasMoon() ? "   [moon]" : ""));
        fieldsLabel.setText("Fields: " + p.fieldsUsed() + " / " + p.fieldsMax()
                + "   (" + (p.fieldsMax() - p.fieldsUsed()) + " free)");
        tempLabel.setText("Temperature: " + p.minTemp() + "°C to " + p.maxTemp() + "°C");

        Views.ResourceView r = p.resources();
        for (int i = 0; i < 3; i++) {
            double amt = r.amount(MCD[i]);
            double cap = r.capacity(MCD[i]);
            resVals[i].setText(UiUtil.num(amt) + " / " + UiUtil.compact(cap));
            resVals[i].setForeground(amt >= cap ? Palette.WARN : Palette.TEXT);
            double prod = r.productionPerTick(MCD[i]);
            resProd[i].setText(UiUtil.signedCompact(prod) + "/h");
            resProd[i].setForeground(prod < 0 ? Palette.BAD : Palette.OK);
        }
        double eProd = r.energyProduced();
        double eCons = r.energyConsumed();
        double bal = eProd - eCons;
        energyLabel.setText("<html><b style='color:#fadc60'>Energy</b> " + UiUtil.num(bal)
                + "<br><span style='color:#8c98b2'>" + UiUtil.num(eProd) + " / " + UiUtil.num(eCons)
                + "  (" + Math.round(r.energyRatio() * 100) + "%)</span></html>");
        energyLabel.setForeground(bal < 0 ? Palette.BAD : Palette.TEXT);
        double dmAmt = r.amount(Ids.ResourceType.DARK_MATTER);
        dmLabel.setText("<html><b style='color:#c484ff'>Dark Matter</b><br>" + UiUtil.num(dmAmt) + "</html>");

        Views.QueueItemView cur = p.currentConstruction();
        if (cur == null) {
            constructionLabel.setText("Idle — nothing under construction");
            construction.set(0, "", Palette.BORDER);
        } else {
            constructionLabel.setText(cur.label());
            construction.set(cur.progressFraction(),
                    UiUtil.duration(cur.remainingTicks()) + " left",
                    new Color(150, 200, 120));
        }
    }

    /** Custom component drawing the selected planet with a starfield backdrop. */
    static final class PlanetArt extends JComponent {
        private Views.PlanetView planet;

        void set(Views.PlanetView p) {
            this.planet = p;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();
            long seed = planet == null ? 1L
                    : SpaceArt.coordSeed(planet.galaxy(), planet.system(), planet.position());
            SpaceArt.starfield(g2, w, h, seed);
            int radius = Math.min(w, h) / 3;
            int cx = w / 2, cy = h / 2;
            if (planet != null) {
                if (planet.isMoon()) {
                    SpaceArt.moon(g2, cx, cy, radius, seed);
                } else {
                    SpaceArt.planet(g2, cx, cy, radius, seed);
                    if (planet.hasMoon()) {
                        SpaceArt.moon(g2, cx + radius + 24, cy - radius + 10, radius / 4, seed ^ 99);
                    }
                }
            }
            g2.dispose();
        }
    }
}
