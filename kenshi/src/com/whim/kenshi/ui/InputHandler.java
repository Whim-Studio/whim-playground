package com.whim.kenshi.ui;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums;
import com.whim.kenshi.api.GameController;
import com.whim.kenshi.api.Views;

import javax.swing.Timer;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Translates raw Swing mouse/keyboard events on the {@link WorldPanel} into
 * {@link GameController} commands: left-click / left-drag selection, right-click
 * context orders (move / attack / interact), wheel zoom, arrow &amp; edge pan,
 * Space to pause and 1/2/4 to change game speed. Holds no world state of its
 * own — it reads the panel's latest snapshot for picking.
 */
public final class InputHandler {

    private static final int EDGE = 24;      // px from edge that triggers edge-pan
    private static final double PAN_SPEED = 14.0; // screen px per pan tick

    private final WorldPanel panel;
    private final Camera camera;
    private final GameController controller;

    private Point dragStart;      // screen px where a left-drag began
    private Point dragCurrent;
    private boolean dragging;

    private final Set<Integer> keysDown = new HashSet<Integer>();
    private Point mousePos;        // last known mouse position for edge-pan
    private boolean mouseInside;

    private final Timer panTimer;

    public InputHandler(WorldPanel panel, Camera camera, GameController controller) {
        this.panel = panel;
        this.camera = camera;
        this.controller = controller;
        this.panTimer = new Timer(16, new ActionListener() {
            public void actionPerformed(ActionEvent e) { panStep(); }
        });
    }

    /** Attach all listeners and start the pan timer. */
    public void install() {
        MouseAdapter mouse = new MouseAdapter() {
            public void mousePressed(MouseEvent e)  { onPressed(e); }
            public void mouseDragged(MouseEvent e)  { onDragged(e); }
            public void mouseReleased(MouseEvent e) { onReleased(e); }
            public void mouseMoved(MouseEvent e)    { mousePos = e.getPoint(); }
            public void mouseEntered(MouseEvent e)  { mouseInside = true; mousePos = e.getPoint(); }
            public void mouseExited(MouseEvent e)   { mouseInside = false; }
            public void mouseWheelMoved(MouseWheelEvent e) { onWheel(e); }
        };
        panel.addMouseListener(mouse);
        panel.addMouseMotionListener(mouse);
        panel.addMouseWheelListener(mouse);
        panel.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e)  { onKey(e, true); }
            public void keyReleased(KeyEvent e) { keysDown.remove(e.getKeyCode()); }
        });
        panTimer.start();
    }

    // ---- mouse -----------------------------------------------------------
    private void onPressed(MouseEvent e) {
        panel.requestFocusInWindow();
        if (e.getButton() == MouseEvent.BUTTON1) {
            dragStart = e.getPoint();
            dragCurrent = e.getPoint();
            dragging = false;
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            rightClickOrder(e.getPoint());
        }
    }

    private void onDragged(MouseEvent e) {
        mousePos = e.getPoint();
        if (dragStart == null) return;
        dragCurrent = e.getPoint();
        if (!dragging && dragStart.distance(dragCurrent) > 4) dragging = true;
        if (dragging) panel.setDragRect(rectOf(dragStart, dragCurrent));
    }

    private void onReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1 || dragStart == null) return;
        if (dragging) {
            boxSelect(rectOf(dragStart, dragCurrent));
        } else {
            clickSelect(e.getPoint());
        }
        dragStart = null;
        dragging = false;
        panel.setDragRect(null);
    }

    private void onWheel(MouseWheelEvent e) {
        double factor = e.getWheelRotation() < 0 ? 1.12 : 1 / 1.12;
        int steps = Math.abs(e.getWheelRotation());
        for (int i = 0; i < steps; i++) {
            camera.zoomBy(factor, e.getX(), e.getY());
        }
    }

    // ---- selection -------------------------------------------------------
    private void clickSelect(Point p) {
        Views.CharacterView hit = pickCharacter(p);
        List<String> sel = new ArrayList<String>();
        if (hit != null && hit.playerControlled()) sel.add(hit.id());
        controller.setSelection(sel);
    }

    private void boxSelect(Rectangle screenRect) {
        Views.GameStateView s = panel.currentState();
        List<String> sel = new ArrayList<String>();
        if (s != null) {
            List<Views.CharacterView> chars = s.characters();
            for (int i = 0; i < chars.size(); i++) {
                Views.CharacterView ch = chars.get(i);
                if (!ch.playerControlled()) continue;
                int sx = (int) camera.toScreenX(ch.x());
                int sy = (int) camera.toScreenY(ch.y());
                if (screenRect.contains(sx, sy)) sel.add(ch.id());
            }
        }
        controller.setSelection(sel);
    }

    // ---- orders ----------------------------------------------------------
    private void rightClickOrder(Point p) {
        List<String> sel = currentSelection();
        if (sel.isEmpty()) return;
        double wx = camera.toWorldX(p.x);
        double wy = camera.toWorldY(p.y);

        Views.CharacterView tgt = pickCharacter(p);
        if (tgt != null && !tgt.playerControlled() && tgt.alive()) {
            controller.orderAttack(sel, tgt.id());
            panel.flashOrder(tgt.x(), tgt.y(), Enums.OrderType.ATTACK);
            return;
        }
        Views.NodeView node = pickNode(p);
        if (node != null) {
            controller.orderInteract(sel, node.id());
            panel.flashOrder(node.x(), node.y(), Enums.OrderType.INTERACT);
            return;
        }
        controller.orderMove(sel, wx, wy);
        panel.flashOrder(wx, wy, Enums.OrderType.MOVE);
    }

    private List<String> currentSelection() {
        Views.GameStateView s = panel.currentState();
        List<String> out = new ArrayList<String>();
        if (s != null && s.selectedIds() != null) out.addAll(s.selectedIds());
        return out;
    }

    // ---- picking ---------------------------------------------------------
    private Views.CharacterView pickCharacter(Point p) {
        Views.GameStateView s = panel.currentState();
        if (s == null) return null;
        double wx = camera.toWorldX(p.x);
        double wy = camera.toWorldY(p.y);
        double slack = Config.CHAR_RADIUS + Config.PICK_SLACK;
        Views.CharacterView best = null;
        double bestD = slack * slack;
        List<Views.CharacterView> chars = s.characters();
        for (int i = 0; i < chars.size(); i++) {
            Views.CharacterView ch = chars.get(i);
            double dx = ch.x() - wx;
            double dy = ch.y() - wy;
            double d = dx * dx + dy * dy;
            if (d <= bestD) { bestD = d; best = ch; }
        }
        return best;
    }

    private Views.NodeView pickNode(Point p) {
        Views.GameStateView s = panel.currentState();
        if (s == null) return null;
        double wx = camera.toWorldX(p.x);
        double wy = camera.toWorldY(p.y);
        List<Views.NodeView> nodes = s.nodes();
        for (int i = 0; i < nodes.size(); i++) {
            Views.NodeView n = nodes.get(i);
            double dx = n.x() - wx;
            double dy = n.y() - wy;
            if (dx * dx + dy * dy <= n.radius() * n.radius()) return n;
        }
        return null;
    }

    // ---- keyboard --------------------------------------------------------
    private void onKey(KeyEvent e, boolean down) {
        int code = e.getKeyCode();
        keysDown.add(code);
        switch (code) {
            case KeyEvent.VK_SPACE:
                controller.togglePause();
                break;
            case KeyEvent.VK_1:
                controller.setGameSpeed(1);
                break;
            case KeyEvent.VK_2:
                controller.setGameSpeed(2);
                break;
            case KeyEvent.VK_4:
                controller.setGameSpeed(4);
                break;
            case KeyEvent.VK_3:
                controller.setGameSpeed(3);
                break;
            default:
                break;
        }
    }

    // ---- panning ---------------------------------------------------------
    private void panStep() {
        double dx = 0, dy = 0;
        if (keysDown.contains(KeyEvent.VK_LEFT)  || keysDown.contains(KeyEvent.VK_A)) dx += PAN_SPEED;
        if (keysDown.contains(KeyEvent.VK_RIGHT) || keysDown.contains(KeyEvent.VK_D)) dx -= PAN_SPEED;
        if (keysDown.contains(KeyEvent.VK_UP)    || keysDown.contains(KeyEvent.VK_W)) dy += PAN_SPEED;
        if (keysDown.contains(KeyEvent.VK_DOWN)  || keysDown.contains(KeyEvent.VK_S)) dy -= PAN_SPEED;

        // Edge pan when the mouse hovers near a viewport border.
        if (mouseInside && mousePos != null && !dragging) {
            if (mousePos.x < EDGE)                    dx += PAN_SPEED;
            if (mousePos.x > panel.getWidth() - EDGE) dx -= PAN_SPEED;
            if (mousePos.y < EDGE)                    dy += PAN_SPEED;
            if (mousePos.y > panel.getHeight() - EDGE) dy -= PAN_SPEED;
        }
        if (dx != 0 || dy != 0) camera.panScreen(dx, dy);
    }

    private static Rectangle rectOf(Point a, Point b) {
        int x = Math.min(a.x, b.x);
        int y = Math.min(a.y, b.y);
        int w = Math.abs(a.x - b.x);
        int h = Math.abs(a.y - b.y);
        return new Rectangle(x, y, w, h);
    }
}
