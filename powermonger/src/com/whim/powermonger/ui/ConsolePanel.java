package com.whim.powermonger.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.whim.powermonger.api.ActionResult;
import com.whim.powermonger.api.Enums.CommandType;
import com.whim.powermonger.api.Enums.Posture;
import com.whim.powermonger.api.GameController;
import com.whim.powermonger.api.Views.CaptainView;
import com.whim.powermonger.api.Views.GameStateView;

/**
 * Command console: eight {@link CommandType} icon buttons, a three-sword posture
 * toggle, and a read-out of the selected captain's strength / food / current
 * order. All actions route through the {@link GameController}.
 */
public final class ConsolePanel extends JPanel {

    private static final CommandType[] COMMANDS = {
        CommandType.SCOUT, CommandType.FIGHT, CommandType.GATHER_FOOD,
        CommandType.SUPPLY_FOOD, CommandType.RECRUIT, CommandType.DISBAND,
        CommandType.INVENT, CommandType.TRADE
    };

    private final GameController controller;
    private String lastMessage = "Select a captain, then issue orders.";
    private final ReadOut readOut = new ReadOut();
    private final SwordToggle swordToggle = new SwordToggle();

    public ConsolePanel(GameController controller) {
        this.controller = controller;
        setBackground(UiPalette.PANEL_BG);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(210, 100));
        setLayout(new java.awt.BorderLayout(6, 6));

        JPanel grid = new JPanel(new GridLayout(2, 4, 6, 6));
        grid.setOpaque(false);
        for (int i = 0; i < COMMANDS.length; i++) {
            grid.add(new IconButton(COMMANDS[i]));
        }

        JPanel south = new JPanel(new java.awt.BorderLayout(6, 4));
        south.setOpaque(false);
        south.add(swordToggle, java.awt.BorderLayout.NORTH);
        south.add(readOut, java.awt.BorderLayout.CENTER);

        add(grid, java.awt.BorderLayout.CENTER);
        add(south, java.awt.BorderLayout.SOUTH);
    }

    private CaptainView selected() {
        GameStateView st = controller.state();
        int id = controller.selectedCaptainId();
        if (id < 0) return null;
        for (CaptainView c : st.captains()) {
            if (c.id() == id) return c;
        }
        return null;
    }

    private void issue(CommandType type) {
        CaptainView c = selected();
        if (c == null) {
            lastMessage = "No captain selected.";
            repaint();
            return;
        }
        int tx = c.hasDestination() ? (int) Math.round(c.destX()) : (int) Math.round(c.x());
        int ty = c.hasDestination() ? (int) Math.round(c.destY()) : (int) Math.round(c.y());
        ActionResult r = controller.issueOrder(c.id(), type, tx, ty);
        lastMessage = r == null ? (type.label() + " ordered") : r.message();
        repaint();
    }

    private void cyclePosture(boolean up) {
        CaptainView c = selected();
        if (c == null) { lastMessage = "No captain selected."; repaint(); return; }
        Posture p = c.posture() == null ? Posture.NEUTRAL : c.posture();
        Posture next = up ? p.cycleUp() : p.cycleDown();
        ActionResult r = controller.setPosture(c.id(), next);
        lastMessage = r == null ? ("Posture: " + next) : r.message();
        repaint();
    }

    // ---- command icon button -------------------------------------------

    private final class IconButton extends JComponent {
        private final CommandType type;
        private boolean hover;

        IconButton(CommandType type) {
            this.type = type;
            setPreferredSize(new Dimension(40, 34));
            setToolTipText(type.label());
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { issue(type); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setColor(hover ? UiPalette.lighten(UiPalette.PANEL_BG_DARK, 0.18)
                             : UiPalette.PANEL_BG_DARK);
            g.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
            g.setColor(UiPalette.PANEL_EDGE);
            g.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
            g.setColor(UiPalette.PARCHMENT);
            drawIcon(g, type, w / 2, h / 2 - 2);
        }
    }

    /** Tiny procedural glyphs for each command. */
    private static void drawIcon(Graphics2D g, CommandType type, int cx, int cy) {
        g.setStroke(new BasicStroke(1.8f));
        switch (type) {
            case SCOUT: { // spyglass
                g.drawOval(cx - 6, cy - 6, 8, 8);
                g.drawLine(cx + 1, cy + 1, cx + 7, cy + 7);
                break;
            }
            case FIGHT: { // crossed swords
                g.drawLine(cx - 7, cy + 7, cx + 7, cy - 7);
                g.drawLine(cx - 7, cy - 7, cx + 7, cy + 7);
                break;
            }
            case GATHER_FOOD: { // wheat sheaf
                g.drawLine(cx, cy + 7, cx, cy - 7);
                g.drawLine(cx, cy - 3, cx - 4, cy - 7);
                g.drawLine(cx, cy - 3, cx + 4, cy - 7);
                g.drawLine(cx, cy + 1, cx - 4, cy - 3);
                g.drawLine(cx, cy + 1, cx + 4, cy - 3);
                break;
            }
            case SUPPLY_FOOD: { // basket / box with arrow up
                g.drawRect(cx - 6, cy, 12, 7);
                g.drawLine(cx, cy, cx, cy - 8);
                g.drawLine(cx, cy - 8, cx - 3, cy - 5);
                g.drawLine(cx, cy - 8, cx + 3, cy - 5);
                break;
            }
            case RECRUIT: { // person + plus
                g.drawOval(cx - 6, cy - 6, 5, 5);
                g.drawLine(cx - 4, cy - 1, cx - 4, cy + 6);
                g.drawLine(cx + 3, cy - 2, cx + 3, cy + 4);
                g.drawLine(cx, cy + 1, cx + 6, cy + 1);
                break;
            }
            case DISBAND: { // person + minus (broken)
                g.drawOval(cx - 6, cy - 6, 5, 5);
                g.drawLine(cx - 4, cy - 1, cx - 4, cy + 6);
                g.drawLine(cx, cy + 1, cx + 6, cy + 1);
                break;
            }
            case INVENT: { // lightbulb / cog
                g.drawOval(cx - 5, cy - 6, 10, 10);
                g.drawLine(cx - 3, cy + 6, cx + 3, cy + 6);
                break;
            }
            case TRADE: { // balance / coins
                g.drawLine(cx - 7, cy - 4, cx + 7, cy - 4);
                g.drawLine(cx, cy - 4, cx, cy + 4);
                g.drawOval(cx - 8, cy - 2, 4, 4);
                g.drawOval(cx + 4, cy - 2, 4, 4);
                break;
            }
            default:
                g.drawOval(cx - 5, cy - 5, 10, 10);
        }
    }

    // ---- posture toggle -------------------------------------------------

    private final class SwordToggle extends JComponent {
        SwordToggle() {
            setPreferredSize(new Dimension(190, 30));
            setToolTipText("Posture: click swords to raise, right-click to lower");
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    cyclePosture(!javax.swing.SwingUtilities.isRightMouseButton(e));
                }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setColor(UiPalette.PANEL_BG_DARK);
            g.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
            g.setColor(UiPalette.PANEL_EDGE);
            g.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

            CaptainView c = selected();
            Posture p = (c == null || c.posture() == null) ? Posture.NEUTRAL : c.posture();
            int active = p.swords();
            String[] names = {"Passive", "Neutral", "Aggressive"};
            for (int i = 0; i < 3; i++) {
                int x = 16 + i * 20;
                boolean on = (i + 1) <= active;
                g.setColor(on ? new Color(220, 220, 232) : UiPalette.TEXT_DIM);
                g.setStroke(new BasicStroke(on ? 2.4f : 1.4f));
                g.drawLine(x, h / 2 + 8, x, h / 2 - 8);         // blade
                g.setColor(on ? new Color(150, 120, 50) : UiPalette.TEXT_DIM);
                g.drawLine(x - 3, h / 2 + 4, x + 3, h / 2 + 4); // guard
            }
            g.setColor(UiPalette.TEXT_LIGHT);
            g.setFont(getFont().deriveFont(11f));
            g.drawString(names[active - 1], 84, h / 2 + 4);
        }
    }

    // ---- selected-captain read-out --------------------------------------

    private final class ReadOut extends JComponent {
        ReadOut() { setPreferredSize(new Dimension(190, 34)); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(UiPalette.TEXT_LIGHT);
            g.setFont(getFont().deriveFont(11f));
            CaptainView c = selected();
            if (c == null) {
                g.setColor(UiPalette.TEXT_DIM);
                g.drawString(lastMessage, 2, 14);
                return;
            }
            String line1 = c.name() + (c.supremeCommander() ? " (Cmdr)" : "");
            String line2 = "Str " + c.strength() + "   Food " + c.food()
                         + "   " + (c.currentOrder() == null ? "-" : c.currentOrder().label());
            g.drawString(line1, 2, 12);
            g.setColor(UiPalette.TEXT_DIM);
            g.drawString(line2, 2, 26);
        }
    }
}
