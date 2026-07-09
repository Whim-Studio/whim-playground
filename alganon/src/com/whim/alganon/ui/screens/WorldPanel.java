package com.whim.alganon.ui.screens;

import com.whim.alganon.api.GameController;
import com.whim.alganon.api.GridPos;
import com.whim.alganon.api.Views;
import com.whim.alganon.ui.UiTheme;
import com.whim.alganon.ui.render.WorldRenderer;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The top-down world viewport. Renders the zone via {@link WorldRenderer} and turns clicks into
 * a click-to-target selection (nearest mob to the clicked tile) that the HUD reads for the
 * action bar's target index. Right-click / click on empty tile clears the selection.
 */
public final class WorldPanel extends JPanel {

    private final GameController controller;
    private final WorldRenderer renderer = new WorldRenderer();
    private GridPos selectedTile;
    private int selectedMobIndex = -1;

    public WorldPanel(GameController controller) {
        this.controller = controller;
        setBackground(UiTheme.tileColor(null));
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { onClick(e); }
        });
    }

    public int selectedMobIndex() { return selectedMobIndex; }

    private void onClick(MouseEvent e) {
        Views.GameStateView v = controller.state();
        Views.WorldView w = v.world();
        if (w == null) return;
        GridPos t = renderer.screenToTile(w, e.getX(), e.getY(), getWidth(), getHeight());
        if (e.getButton() != MouseEvent.BUTTON1 || t == null) {
            selectedTile = null; selectedMobIndex = -1; repaint(); return;
        }
        selectedTile = t;
        selectedMobIndex = mobAt(w, t);
        if (selectedMobIndex < 0) {
            // fall through: try interacting when adjacent to an npc/portal
            controller.interact();
        }
        repaint();
    }

    private int mobAt(Views.WorldView w, GridPos t) {
        for (int i = 0; i < w.mobs().size(); i++) {
            GridPos p = w.mobs().get(i).pos();
            if (p != null && p.equals(t)) return i;
        }
        return -1;
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        Views.GameStateView v = controller.state();
        renderer.render(g, v.world(), v.player(), selectedTile, getWidth(), getHeight(), System.currentTimeMillis());

        // zone banner
        Views.WorldView w = v.world();
        if (w != null) {
            g.setFont(UiTheme.FONT_H2);
            String s = w.zoneName();
            int tw = g.getFontMetrics().stringWidth(s);
            g.setColor(new java.awt.Color(0, 0, 0, 130));
            g.fillRoundRect(10, 8, tw + 16, 24, 8, 8);
            g.setColor(UiTheme.ACCENT);
            g.drawString(s, 18, 25);
        }
    }
}
