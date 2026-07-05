package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.Catalog;
import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Requirement;
import com.whim.oggalaxy.api.Views;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Small formatting / requirement / styling helpers shared across the UI. Pure
 * functions plus a couple of Swing component factories. No game state is held here.
 */
public final class UiUtil {

    private UiUtil() {
    }

    private static final NumberFormat INT_FMT = NumberFormat.getIntegerInstance(Locale.US);

    /** Full grouped integer, e.g. 1,234,567. */
    public static String num(double v) {
        return INT_FMT.format((long) Math.floor(v));
    }

    /** Compact human figure: 12.3K / 4.5M / 1.2B. Good for tight resource bars. */
    public static String compact(double v) {
        double a = Math.abs(v);
        String sign = v < 0 ? "-" : "";
        if (a < 1000) return sign + (long) a;
        if (a < 1_000_000) return sign + trim(a / 1000.0) + "K";
        if (a < 1_000_000_000) return sign + trim(a / 1_000_000.0) + "M";
        return sign + trim(a / 1_000_000_000.0) + "B";
    }

    private static String trim(double v) {
        if (v >= 100) return String.valueOf((long) v);
        return String.format(Locale.US, "%.1f", v);
    }

    /** Signed compact production, e.g. "+1.8K" or "-320". */
    public static String signedCompact(double v) {
        String s = compact(Math.abs(v));
        return (v < 0 ? "-" : "+") + s;
    }

    /** A tick is one in-game hour. Render a tick span as e.g. "2d 5h" / "7h" / "now". */
    public static String duration(int ticks) {
        if (ticks <= 0) return "done";
        int h = ticks;
        int d = h / 24;
        h = h % 24;
        if (d > 0) return d + "d " + h + "h";
        return h + "h";
    }

    // ------------------------------------------------------------------
    // requirement / affordability checks (UI-side mirror of engine rules)
    // ------------------------------------------------------------------

    /**
     * Returns null if every requirement is met on the given planet + player empire,
     * otherwise a short human-readable reason naming the first unmet requirement.
     */
    public static String unmetRequirement(List<Requirement> reqs, Views.PlanetView planet,
                                           Views.EmpireView player, Catalog catalog) {
        if (reqs == null) return null;
        for (Requirement r : reqs) {
            if (r.isBuilding()) {
                int have = planet == null ? 0 : planet.buildingLevel(r.building);
                if (have < r.level) {
                    return catalog.building(r.building).name + " " + r.level;
                }
            } else {
                int have = player == null ? 0 : player.techLevel(r.tech);
                if (have < r.level) {
                    return catalog.tech(r.tech).name + " " + r.level;
                }
            }
        }
        return null;
    }

    /** True if the planet currently stores enough metal/crystal/deut to pay {@code cost}. */
    public static boolean canAfford(Views.PlanetView planet, Cost cost) {
        if (planet == null || cost == null) return false;
        Views.ResourceView r = planet.resources();
        return r.amount(Ids.ResourceType.METAL) >= cost.metal
                && r.amount(Ids.ResourceType.CRYSTAL) >= cost.crystal
                && r.amount(Ids.ResourceType.DEUTERIUM) >= cost.deuterium;
    }

    // ------------------------------------------------------------------
    // Swing styling helpers
    // ------------------------------------------------------------------

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Palette.TEXT);
        l.setFont(Palette.FONT);
        return l;
    }

    public static JLabel label(String text, Color fg, Font font) {
        JLabel l = new JLabel(text);
        l.setForeground(fg);
        l.setFont(font);
        return l;
    }

    public static JLabel title(String text) {
        return label(text, Palette.ACCENT, Palette.FONT_TITLE);
    }

    public static Border padded(int t, int l, int b, int r) {
        return BorderFactory.createEmptyBorder(t, l, b, r);
    }

    public static Border panelBorder(String titleText) {
        Border line = BorderFactory.createLineBorder(Palette.BORDER, 1);
        if (titleText == null) {
            return BorderFactory.createCompoundBorder(line, padded(6, 8, 6, 8));
        }
        javax.swing.border.TitledBorder tb = BorderFactory.createTitledBorder(line, titleText);
        tb.setTitleColor(Palette.ACCENT);
        tb.setTitleFont(Palette.FONT_BOLD);
        return BorderFactory.createCompoundBorder(tb, padded(4, 6, 4, 6));
    }

    /** A themed button with a coloured accent; used across the app. */
    public static JButton button(String text) {
        return button(text, Palette.ACCENT);
    }

    public static JButton button(String text, final Color accent) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(Palette.FONT_BOLD);
        b.setForeground(Palette.TEXT);
        b.setBackground(Palette.mix(Palette.BG_PANEL_HI, accent, 0.20));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent.darker(), 1),
                padded(4, 12, 4, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Style an arbitrary component with the dark theme background/foreground. */
    public static <T extends JComponent> T themeDark(T c) {
        c.setBackground(Palette.BG_PANEL);
        c.setForeground(Palette.TEXT);
        c.setFont(Palette.FONT);
        return c;
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    /**
     * HTML snippet showing a metal/crystal/deut(/energy) cost, each coloured by its
     * resource, and rendered dim-red when the planet cannot currently afford that part.
     */
    public static String costHtml(Cost cost, Views.PlanetView planet) {
        StringBuilder sb = new StringBuilder("<html>");
        appendCostPart(sb, cost.metal, planet, Ids.ResourceType.METAL, Palette.METAL);
        appendCostPart(sb, cost.crystal, planet, Ids.ResourceType.CRYSTAL, Palette.CRYSTAL);
        appendCostPart(sb, cost.deuterium, planet, Ids.ResourceType.DEUTERIUM, Palette.DEUTERIUM);
        if (cost.energy != 0) {
            sb.append("<span style='color:").append(hex(Palette.ENERGY)).append("'>")
              .append(compact(cost.energy)).append("e</span> ");
        }
        sb.append("</html>");
        return sb.toString();
    }

    private static void appendCostPart(StringBuilder sb, double amount, Views.PlanetView planet,
                                       Ids.ResourceType type, Color c) {
        if (amount <= 0) return;
        boolean afford = planet != null && planet.resources().amount(type) >= amount;
        Color show = afford ? c : Palette.BAD;
        sb.append("<span style='color:").append(hex(show)).append("'>")
          .append(compact(amount)).append("</span>&nbsp; ");
    }

    public static String coords(int g, int s, int p) {
        return "[" + g + ":" + s + ":" + p + "]";
    }

    public static String coords(int[] c) {
        if (c == null || c.length < 3) return "[?]";
        return coords(c[0], c[1], c[2]);
    }
}
