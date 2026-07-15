package com.whim.settlers.engine;

import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.transport.Flag;
import com.whim.settlers.ui.BuildMenu;
import com.whim.settlers.ui.EconomyPanel;
import com.whim.settlers.ui.Minimap;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashSet;
import java.util.Set;

/**
 * Collects raw AWT input for the camera and (later) gameplay. Drives the
 * {@link Camera}: WASD / arrow keys pan, the mouse wheel zooms to the cursor,
 * and dragging with the right mouse button pans directly.
 *
 * <p>All AWT callbacks run on the Event Dispatch Thread; the game loop reads the
 * accumulated state from its own thread. State is limited to primitives and a
 * small key-set, which is safe enough for this read/write pattern; anything
 * richer would need explicit synchronisation.
 */
public final class InputHandler
        implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private final Camera camera;
    private final World world;
    private final Minimap minimap;
    private final BuildMenu buildMenu;
    private final EconomyPanel economyPanel;
    private final Set<Integer> keysDown = new HashSet<Integer>();

    private volatile int mouseX, mouseY;
    private boolean dragging;
    private int lastDragX, lastDragY;

    /** Transport tools; mutually exclusive with building placement. */
    public enum Tool { NONE, FLAG, ROAD }

    /** Building armed for placement, or null. Read by the renderer for the ghost. */
    private volatile BuildingType selectedType;
    private volatile Tool tool = Tool.NONE;
    private volatile int roadStartFlag = -1; // first flag picked in ROAD mode

    // Zoom is buffered here and drained by the loop so wheel events never race.
    private volatile int pendingZoomSteps;
    private volatile int zoomAnchorX, zoomAnchorY;

    public InputHandler(World world, Minimap minimap, BuildMenu buildMenu,
                        EconomyPanel economyPanel) {
        this.world = world;
        this.camera = world.camera();
        this.minimap = minimap;
        this.buildMenu = buildMenu;
        this.economyPanel = economyPanel;
    }

    public BuildingType selectedType() { return selectedType; }
    public Tool tool()                 { return tool; }
    public int roadStartFlag()         { return roadStartFlag; }

    /** Hovered tile under the cursor (may be off-map); for the placement ghost. */
    public Point hoveredTile() {
        Point2D.Double w = camera.screenToWorld(mouseX, mouseY);
        return new Point((int) Math.floor(w.x), (int) Math.floor(w.y));
    }

    /** Apply continuous (held-key) panning; call once per update tick. */
    public void applyContinuous(double dtSeconds) {
        double speed = 12.0 * Camera.TILE_SIZE * dtSeconds; // pixels/sec at zoom 1
        double dx = 0, dy = 0;
        if (down(KeyEvent.VK_A) || down(KeyEvent.VK_LEFT))  dx += speed;
        if (down(KeyEvent.VK_D) || down(KeyEvent.VK_RIGHT)) dx -= speed;
        if (down(KeyEvent.VK_W) || down(KeyEvent.VK_UP))    dy += speed;
        if (down(KeyEvent.VK_S) || down(KeyEvent.VK_DOWN))  dy -= speed;
        if (dx != 0 || dy != 0) camera.panPixels(dx, dy);

        int steps = pendingZoomSteps;
        if (steps != 0) {
            pendingZoomSteps = 0;
            camera.zoomAt(steps, zoomAnchorX, zoomAnchorY);
        }
    }

    private boolean down(int keyCode) { return keysDown.contains(keyCode); }

    public Point mouse() { return new Point(mouseX, mouseY); }
    public int mouseX()  { return mouseX; }
    public int mouseY()  { return mouseY; }

    // --- KeyListener ---
    @Override public void keyPressed(KeyEvent e)  {
        keysDown.add(e.getKeyCode());
        switch (e.getKeyCode()) {
            case KeyEvent.VK_E: economyPanel.toggle(); break;
            case KeyEvent.VK_F: tool = Tool.FLAG; selectedType = null; roadStartFlag = -1; break;
            case KeyEvent.VK_R: tool = Tool.ROAD; selectedType = null; roadStartFlag = -1; break;
            case KeyEvent.VK_ESCAPE: tool = Tool.NONE; selectedType = null; roadStartFlag = -1; break;
            default: break;
        }
    }
    @Override public void keyReleased(KeyEvent e) { keysDown.remove(e.getKeyCode()); }
    @Override public void keyTyped(KeyEvent e)    { }

    // --- MouseListener ---
    @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3 || e.getButton() == MouseEvent.BUTTON2) {
            dragging = true;
            lastDragX = e.getX();
            lastDragY = e.getY();
        }
    }
    @Override public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3 || e.getButton() == MouseEvent.BUTTON2) {
            dragging = false;
        }
    }
    @Override public void mouseClicked(MouseEvent e) {
        int vh = camera.viewportH();
        int vw = camera.viewportW();
        if (e.getButton() == MouseEvent.BUTTON1) {
            // 0) Economy panel buttons (when open) take precedence.
            if (economyPanel.contains(e.getX(), e.getY(), vw, vh)) {
                economyPanel.handleClick(e.getX(), e.getY());
                return;
            }
            // 1) Build-menu click arms/toggles a placement type.
            if (buildMenu.contains(e.getX(), e.getY(), vh)) {
                BuildingType t = buildMenu.typeAt(e.getX(), e.getY());
                if (t != null) { selectedType = (t == selectedType) ? null : t; tool = Tool.NONE; }
                return;
            }
            // 2) Minimap click recentres the camera.
            Point2D.Double target = minimap.worldAt(e.getX(), e.getY(), world);
            if (target != null) {
                camera.centreOn(target.x, target.y);
                return;
            }
            // 3) World click: transport tool, else building placement.
            Point tile = hoveredTile();
            if (tool == Tool.FLAG) {
                world.transport().placeFlag(tile.x, tile.y);
            } else if (tool == Tool.ROAD) {
                handleRoadClick(tile);
            } else if (selectedType != null) {
                world.buildings().place(selectedType, tile.x, tile.y, World.PLAYER_ID);
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            // Right-click cancels the current tool / placement (also drag-pans).
            selectedType = null;
            tool = Tool.NONE;
            roadStartFlag = -1;
        }
    }

    /** ROAD tool: pick a start flag, then a second flag to lay a road between them. */
    private void handleRoadClick(Point tile) {
        Flag f = world.transport().network().flagAt(tile.x, tile.y);
        if (f == null) return; // must click on a flag
        if (roadStartFlag < 0) {
            roadStartFlag = f.id();
        } else {
            world.transport().buildRoad(roadStartFlag, f.id());
            roadStartFlag = -1;
        }
    }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e)  { }

    // --- MouseMotionListener ---
    @Override public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (dragging) {
            camera.panPixels(e.getX() - lastDragX, e.getY() - lastDragY);
            lastDragX = e.getX();
            lastDragY = e.getY();
        }
    }
    @Override public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    // --- MouseWheelListener ---
    @Override public void mouseWheelMoved(MouseWheelEvent e) {
        pendingZoomSteps -= e.getWheelRotation(); // wheel up = zoom in
        zoomAnchorX = e.getX();
        zoomAnchorY = e.getY();
    }
}
