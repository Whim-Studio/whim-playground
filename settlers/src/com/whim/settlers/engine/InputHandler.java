package com.whim.settlers.engine;

import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.ui.BuildMenu;
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
    private final Set<Integer> keysDown = new HashSet<Integer>();

    private volatile int mouseX, mouseY;
    private boolean dragging;
    private int lastDragX, lastDragY;

    /** Building armed for placement, or null. Read by the renderer for the ghost. */
    private volatile BuildingType selectedType;

    // Zoom is buffered here and drained by the loop so wheel events never race.
    private volatile int pendingZoomSteps;
    private volatile int zoomAnchorX, zoomAnchorY;

    public InputHandler(World world, Minimap minimap, BuildMenu buildMenu) {
        this.world = world;
        this.camera = world.camera();
        this.minimap = minimap;
        this.buildMenu = buildMenu;
    }

    public BuildingType selectedType() { return selectedType; }

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
    @Override public void keyPressed(KeyEvent e)  { keysDown.add(e.getKeyCode()); }
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
        if (e.getButton() == MouseEvent.BUTTON1) {
            // 1) Build-menu click arms/toggles a placement type.
            if (buildMenu.contains(e.getX(), e.getY(), vh)) {
                BuildingType t = buildMenu.typeAt(e.getX(), e.getY());
                if (t != null) selectedType = (t == selectedType) ? null : t;
                return;
            }
            // 2) Minimap click recentres the camera.
            Point2D.Double target = minimap.worldAt(e.getX(), e.getY(), world);
            if (target != null) {
                camera.centreOn(target.x, target.y);
                return;
            }
            // 3) World click places the armed building, if the spot is valid.
            if (selectedType != null) {
                Point tile = hoveredTile();
                boolean placed = world.buildings()
                        .place(selectedType, tile.x, tile.y, World.PLAYER_ID) != null;
                // Keep the tool armed on success (rapid placement); Shift is not
                // required. Right-click clears it.
                if (!placed) { /* invalid spot: leave armed, no-op */ }
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            // Right-click cancels placement mode (in addition to drag-panning).
            selectedType = null;
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
