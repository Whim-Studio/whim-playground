package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Views;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Top resource/status strip: metal / crystal / deuterium (of the selected planet) with
 * production rates, the energy balance, empire dark matter, plus the game tick and a
 * transient status message. Clock controls live in {@link MainFrame}'s toolbar beside it.
 */
public final class ResourceBar extends JPanel implements Refreshable {

    private final ResChip metal = new ResChip(Ids.ResourceType.METAL, "Metal");
    private final ResChip crystal = new ResChip(Ids.ResourceType.CRYSTAL, "Crystal");
    private final ResChip deut = new ResChip(Ids.ResourceType.DEUTERIUM, "Deuterium");
    private final ResChip energy = new ResChip(Ids.ResourceType.ENERGY, "Energy");
    private final ResChip dm = new ResChip(Ids.ResourceType.DARK_MATTER, "Dark Matter");

    public ResourceBar() {
        setOpaque(true);
        setBackground(Palette.BG_PANEL);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(UiUtil.padded(4, 8, 4, 8));
        add(metal);
        add(sep());
        add(crystal);
        add(sep());
        add(deut);
        add(sep());
        add(energy);
        add(sep());
        add(dm);
        add(Box.createHorizontalGlue());
    }

    private JComponentSep sep() {
        return new JComponentSep();
    }

    @Override
    public void refresh(Views.GameStateView state) {
        Views.PlanetView p = state == null ? null : state.selectedPlanet();
        if (p == null) return;
        Views.ResourceView r = p.resources();
        metal.set(r.amount(Ids.ResourceType.METAL), r.productionPerTick(Ids.ResourceType.METAL),
                r.capacity(Ids.ResourceType.METAL));
        crystal.set(r.amount(Ids.ResourceType.CRYSTAL), r.productionPerTick(Ids.ResourceType.CRYSTAL),
                r.capacity(Ids.ResourceType.CRYSTAL));
        deut.set(r.amount(Ids.ResourceType.DEUTERIUM), r.productionPerTick(Ids.ResourceType.DEUTERIUM),
                r.capacity(Ids.ResourceType.DEUTERIUM));
        double bal = r.energyProduced() - r.energyConsumed();
        energy.setEnergy(bal, r.energyRatio());
        dm.setPlain(r.amount(Ids.ResourceType.DARK_MATTER));
    }

    /** Thin vertical rule. */
    private static final class JComponentSep extends JPanel {
        JComponentSep() {
            setOpaque(false);
            setPreferredSize(new Dimension(12, 10));
            setMaximumSize(new Dimension(12, 100));
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Palette.BORDER);
            g.drawLine(getWidth() / 2, 4, getWidth() / 2, getHeight() - 4);
        }
    }

    private static final class ResChip extends JPanel {
        private final Ids.ResourceType type;
        private final GlyphIcon icon = new GlyphIcon(22);
        private final JLabel value = UiUtil.label("0", Palette.TEXT, Palette.FONT_BOLD);
        private final JLabel sub = UiUtil.label("", Palette.TEXT_DIM, Palette.FONT_SMALL);

        ResChip(Ids.ResourceType type, String name) {
            this.type = type;
            setOpaque(false);
            setLayout(new BorderLayout(6, 0));
            setBorder(UiUtil.padded(2, 6, 2, 6));
            icon.resource(type);
            icon.setPreferredSize(new Dimension(22, 22));
            add(icon, BorderLayout.WEST);
            JPanel col = new JPanel();
            col.setOpaque(false);
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            value.setToolTipText(name);
            col.add(value);
            col.add(sub);
            add(col, BorderLayout.CENTER);
            setMaximumSize(new Dimension(200, 48));
        }

        void set(double amount, double prod, double cap) {
            value.setText(UiUtil.compact(amount));
            value.setForeground(amount >= cap ? Palette.WARN : Palette.resourceColor(type));
            sub.setText(UiUtil.signedCompact(prod) + "/h");
            sub.setForeground(prod < 0 ? Palette.BAD : Palette.OK);
            setToolTipText(UiUtil.num(amount) + " / " + UiUtil.num(cap));
        }

        void setEnergy(double balance, double ratio) {
            value.setText(UiUtil.compact(balance));
            value.setForeground(balance < 0 ? Palette.BAD : Palette.ENERGY);
            sub.setText(Math.round(ratio * 100) + "%");
            sub.setForeground(ratio < 1 ? Palette.WARN : Palette.OK);
        }

        void setPlain(double amount) {
            value.setText(UiUtil.compact(amount));
            value.setForeground(Palette.resourceColor(type));
            sub.setText("");
        }
    }
}
