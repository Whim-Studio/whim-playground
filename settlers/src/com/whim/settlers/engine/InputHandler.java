package com.whim.settlers.engine;

import com.whim.settlers.ui.Minimap;

import java.awt.Point;
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
    private final Set<Integer> keysDown = new HashSet<Integer>();

    private volatile int mouseX, mouseY;
    private boolean dragging;
    private int lastDragX, lastDragY;

    // Zoom is buffered here and drained by the loop so wheel events never race.
    private volatile int pendingZoomSteps;
    private volatile int zoomAnchorX, zoomAnchorY;

    public InputHandler(World world, Minimap minimap) {
        this.world = world;
        this.camera = world.camera();
        this.minimap = minimap;
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
        if (e.getButton() == MouseEvent.BUTTON1) {
            // Left-click on the minimap recentres the camera there.
            java.awt.geom.Point2D.Double target =
                    minimap.worldAt(e.getX(), e.getY(), world);
            if (target != null) camera.centreOn(target.x, target.y);
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
