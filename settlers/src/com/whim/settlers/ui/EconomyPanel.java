package com.whim.settlers.ui;

import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.economy.Economy;
import com.whim.settlers.economy.Good;
import com.whim.settlers.economy.Inventory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Right-edge economy panel (toggle with <kbd>E</kbd>): live stockpile, population,
 * the reorderable <b>tool-priority</b> list for the Tool Maker, and the
 * <b>supply-priority</b> controls for goods distribution among contested
 * consumers.
 *
 * <p>Interactive buttons are recorded into {@link #hits} during {@link #render}
 * and dispatched by {@link #handleClick}, so rendering and hit-testing share one
 * source of geometry.
 */
public final class EconomyPanel {

    public static final int WIDTH = 232;
    private static final int BTN = 14;

    private final Font header = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    private final Font body = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    /** Buildings whose distribution priority the player can tune (the contested ones). */
    private static final BuildingType[] SUPPLY = {
        BuildingType.IRON_FOUNDRY, BuildingType.GOLD_FOUNDRY, BuildingType.BLACKSMITH,
        BuildingType.TOOLMAKER, BuildingType.WINDMILL, BuildingType.PIG_FARM,
        BuildingType.COAL_MINE, BuildingType.IRON_MINE,
        BuildingType.GOLD_MINE, BuildingType.STONE_MINE
    };

    private boolean visible;
    private final List<Hit> hits = new ArrayList<Hit>();

    public boolean isVisible() { return visible; }
    public void toggle()       { visible = !visible; }

    public Rectangle bounds(int viewportW, int viewportH) {
        return new Rectangle(viewportW - WIDTH, 0, WIDTH, viewportH);
    }

    public boolean contains(int x, int y, int viewportW, int viewportH) {
        return visible && bounds(viewportW, viewportH).contains(x, y);
    }

    public void render(Graphics2D g, Economy eco, int viewportW, int viewportH) {
        hits.clear();
        if (!visible) return;
        int x0 = viewportW - WIDTH;
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(x0, 0, WIDTH, viewportH);
        g.setColor(new Color(255, 255, 255, 40));
        g.drawLine(x0, 0, x0, viewportH);

        int x = x0 + 10;
        int y = 22;
        g.setFont(header);
        g.setColor(Color.WHITE);
        g.drawString("ECONOMY  (E to close)", x, y);
        y += 18;
        g.setFont(body);
        g.setColor(new Color(210, 210, 210));
        g.drawString("Settlers: " + eco.idle() + " idle / " + eco.totalPopulation() + " total", x, y);
        y += 18;

        y = section(g, x, y, "STOCKPILE");
        y = drawStock(g, x0, x, y, eco.stock());
        y += 6;

        y = section(g, x, y, "TOOL PRIORITY  (Tool Maker)");
        for (Good tool : eco.toolOrder()) {
            y = toolRow(g, eco, x0, x, y, tool);
        }
        y += 6;

        y = section(g, x, y, "SUPPLY PRIORITY  (scarce goods)");
        for (BuildingType t : SUPPLY) {
            y = supplyRow(g, eco, x0, x, y, t);
        }
    }

    private int section(Graphics2D g, int x, int y, String title) {
        g.setFont(header);
        g.setColor(new Color(150, 200, 255));
        g.drawString(title, x, y + 4);
        return y + 18;
    }

    private int drawStock(Graphics2D g, int x0, int x, int y, Inventory stock) {
        g.setFont(body);
        Map<Good, Integer> snap = stock.snapshot();
        int col = 0;
        int rowY = y;
        for (Good gd : Good.values()) {
            Integer n = snap.get(gd);
            if (n == null || n == 0) continue;
            int cx = x + col * 108;
            g.setColor(new Color(220, 220, 220));
            g.drawString(gd.label() + ": " + n, cx, rowY + 10);
            col++;
            if (col == 2) { col = 0; rowY += 14; }
        }
        return (col == 0 ? rowY : rowY + 14) + 2;
    }

    private int toolRow(Graphics2D g, final Economy eco, int x0, int x, int y, final Good tool) {
        g.setFont(body);
        g.setColor(new Color(220, 220, 220));
        g.drawString(tool.label(), x + 34, y + 11);
        int bx = x0 + WIDTH - 2 * BTN - 12;
        button(g, bx, y, "▲", () -> eco.moveTool(tool, -1));       // up
        button(g, bx + BTN + 2, y, "▼", () -> eco.moveTool(tool, 1)); // down
        return y + BTN + 3;
    }

    private int supplyRow(Graphics2D g, final Economy eco, int x0, int x, int y, final BuildingType t) {
        g.setFont(body);
        g.setColor(new Color(220, 220, 220));
        g.drawString(shortName(t), x, y + 11);
        int bx = x0 + WIDTH - 3 * BTN - 16;
        button(g, bx, y, "−", () -> eco.bumpPriority(t, -1)); // minus
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(eco.priorityOf(t)), bx + BTN + 5, y + 11);
        button(g, bx + 2 * BTN + 2, y, "+", () -> eco.bumpPriority(t, 1));
        return y + BTN + 3;
    }

    private void button(Graphics2D g, int x, int y, String label, Runnable action) {
        g.setColor(new Color(70, 80, 95));
        g.fillRect(x, y, BTN, BTN);
        g.setColor(new Color(200, 210, 220));
        g.drawRect(x, y, BTN, BTN);
        g.drawString(label, x + 3, y + 11);
        hits.add(new Hit(new Rectangle(x, y, BTN, BTN), action));
    }

    /** Dispatch a click to a button, if one is under the cursor. */
    public boolean handleClick(int mx, int my) {
        for (Hit h : hits) {
            if (h.rect.contains(mx, my)) { h.action.run(); return true; }
        }
        return false;
    }

    private static String shortName(BuildingType t) {
        String n = t.displayName();
        return n.length() > 20 ? n.substring(0, 20) : n;
    }

    private static final class Hit {
        final Rectangle rect;
        final Runnable action;
        Hit(Rectangle rect, Runnable action) { this.rect = rect; this.action = action; }
    }
}
