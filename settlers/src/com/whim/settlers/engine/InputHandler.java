package com.whim.settlers.engine;

import com.whim.settlers.buildings.Building;
import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.military.MilitarySystem;
import com.whim.settlers.transport.Flag;
import com.whim.settlers.ui.BuildMenu;
import com.whim.settlers.ui.EconomyPanel;
import com.whim.settlers.ui.MetaScreen;
import com.whim.settlers.ui.MilitaryPanel;
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
 * Collects raw AWT input and routes it by {@link Game.State}. In the menu / setup
 * / end screens clicks go to the {@link MetaScreen}; during founding a click
 * places the Castle; in play it drives the camera, build menu, transport tools,
 * panels, and attack selection. Only primitives and a small key-set are shared
 * with the game-loop thread, which is safe enough for this read/write pattern.
 */
public final class InputHandler
        implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private final Game game;
    private final MetaScreen meta;
    private final Minimap minimap;
    private final BuildMenu buildMenu;
    private final EconomyPanel economyPanel;
    private final MilitaryPanel militaryPanel;
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

    public InputHandler(Game game, MetaScreen meta, Minimap minimap, BuildMenu buildMenu,
                        EconomyPanel economyPanel, MilitaryPanel militaryPanel) {
        this.game = game;
        this.meta = meta;
        this.minimap = minimap;
        this.buildMenu = buildMenu;
        this.economyPanel = economyPanel;
        this.militaryPanel = militaryPanel;
    }

    public BuildingType selectedType() { return selectedType; }
    public Tool tool()                 { return tool; }
    public int roadStartFlag()         { return roadStartFlag; }

    private World world()   { return game.world(); }
    private Camera camera() { World w = world(); return w == null ? null : w.camera(); }

    /** Hovered tile under the cursor (may be off-map); for the placement ghost. */
    public Point hoveredTile() {
        Camera c = camera();
        if (c == null) return new Point(-1, -1);
        Point2D.Double w = c.screenToWorld(mouseX, mouseY);
        return new Point((int) Math.floor(w.x), (int) Math.floor(w.y));
    }

    /** Apply continuous (held-key) panning; call once per update tick. */
    public void applyContinuous(double dtSeconds) {
        Camera camera = camera();
        if (camera == null || !game.inPlay()) return;
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
        Game.State st = game.state();
        int code = e.getKeyCode();
        if (st == Game.State.PLACING_CASTLE) {
            if (code == KeyEvent.VK_ESCAPE) game.openMenu();
            return;
        }
        if (st == Game.State.PAUSED) {
            if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_P) game.togglePause();
            else if (code == KeyEvent.VK_M) game.openMenu();
            else if (code == KeyEvent.VK_H) game.toggleHelp();
            return;
        }
        if (st != Game.State.PLAYING) {
            if (code == KeyEvent.VK_ESCAPE && st == Game.State.SETUP) game.openMenu();
            return;
        }
        // --- in play ---
        switch (code) {
            case KeyEvent.VK_E: economyPanel.toggle(); break;
            case KeyEvent.VK_F: tool = Tool.FLAG; selectedType = null; roadStartFlag = -1; break;
            case KeyEvent.VK_R: tool = Tool.ROAD; selectedType = null; roadStartFlag = -1; break;
            case KeyEvent.VK_H: game.toggleHelp(); break;
            case KeyEvent.VK_SLASH: game.toggleHelp(); break;
            case KeyEvent.VK_P: game.togglePause(); break;
            case KeyEvent.VK_M: game.openMenu(); break;
            case KeyEvent.VK_ESCAPE:
                // Cancel an armed tool/placement first; otherwise pause.
                if (tool != Tool.NONE || selectedType != null) {
                    tool = Tool.NONE; selectedType = null; roadStartFlag = -1;
                } else {
                    game.togglePause();
                }
                break;
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
        Game.State st = game.state();
        // Meta screens (menu / setup / end) own the whole surface.
        if (st == Game.State.MENU || st == Game.State.SETUP
                || st == Game.State.VICTORY || st == Game.State.DEFEAT) {
            if (e.getButton() == MouseEvent.BUTTON1) meta.handleClick(e.getX(), e.getY());
            return;
        }
        if (st == Game.State.PLACING_CASTLE) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                Point tile = hoveredTile();
                game.tryFoundCastle(tile.x, tile.y);
            }
            return;
        }
        if (st != Game.State.PLAYING) return; // paused: ignore world clicks

        World world = world();
        Camera camera = camera();
        int vh = camera.viewportH();
        int vw = camera.viewportW();
        if (e.getButton() == MouseEvent.BUTTON1) {
            // 0) Panel buttons (when shown) take precedence.
            if (economyPanel.contains(e.getX(), e.getY(), vw, vh)) {
                economyPanel.handleClick(e.getX(), e.getY());
                return;
            }
            if (militaryPanel.contains(e.getX(), e.getY(), vw, vh)) {
                militaryPanel.handleClick(e.getX(), e.getY());
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
            // 3) World click: transport tool, building placement, or attack select.
            Point tile = hoveredTile();
            if (tool == Tool.FLAG) {
                world.transport().placeFlag(tile.x, tile.y);
            } else if (tool == Tool.ROAD) {
                handleRoadClick(tile);
            } else if (selectedType != null) {
                if (world.canPlayerPlace(selectedType, tile.x, tile.y, World.PLAYER_ID)) {
                    world.buildings().place(selectedType, tile.x, tile.y, World.PLAYER_ID);
                }
            } else {
                selectAttackTarget(tile);
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
        Flag f = world().transport().network().flagAt(tile.x, tile.y);
        if (f == null) return; // must click on a flag
        if (roadStartFlag < 0) {
            roadStartFlag = f.id();
        } else {
            world().transport().buildRoad(roadStartFlag, f.id());
            roadStartFlag = -1;
        }
    }

    /** Click an enemy fort to select it as an attack target (opens the attack panel). */
    private void selectAttackTarget(Point tile) {
        Building b = world().buildings().at(tile.x, tile.y);
        if (b != null && MilitarySystem.isFort(b.type())
                && b.ownerId() != World.PLAYER_ID && b.ownerId() != -1) {
            militaryPanel.setTarget(b);
        } else {
            militaryPanel.setTarget(null);
        }
    }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e)  { }

    // --- MouseMotionListener ---
    @Override public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        Camera camera = camera();
        if (dragging && camera != null && game.inPlay()) {
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
