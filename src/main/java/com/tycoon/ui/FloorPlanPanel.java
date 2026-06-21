package com.tycoon.ui;

import com.tycoon.core.Facility;
import com.tycoon.core.FacilityType;
import com.tycoon.core.FloorPlan;
import com.tycoon.core.GameState;
import com.tycoon.core.GridPos;
import com.tycoon.core.Room;
import com.tycoon.core.RoomType;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Renders the {@link FloorPlan} 2D grid and hosts the freeform building phase.
 *
 * <p>While the floor plan is unlocked (paused state) the player may drag to draw a
 * {@link Room} or click to drop a {@link Facility} from the palette. Each successful
 * placement calls the core mutators and deducts the player's cash. All edits are rejected
 * when {@code floorPlan.isLocked()}.</p>
 */
public class FloorPlanPanel extends JPanel {

    /** Fixed pixel size of a single grid tile. */
    public static final int CELL = 18;

    /** Editing modes for the building phase. */
    public enum Mode { DRAW_ROOM, PLACE_FACILITY }

    private final GameState state;
    private final FloorPlan floorPlan;

    private Mode mode = Mode.DRAW_ROOM;
    private RoomType selectedRoom = RoomType.DEVELOPMENT;
    private FacilityType selectedFacility = FacilityType.DESK;

    /** Drag state, in tile coordinates. -1 means "no drag in progress". */
    private int dragStartX = -1, dragStartY = -1, dragCurX = -1, dragCurY = -1;

    /** Called whenever an edit changes cash, so the host frame can refresh its labels. */
    private Runnable onChange;
    /** Called with a human-readable message when an edit is rejected. */
    private java.util.function.Consumer<String> onReject;

    public FloorPlanPanel(GameState state) {
        this.state = state;
        this.floorPlan = state.player().floorPlan();
        setBackground(new Color(30, 30, 36));
        int w = floorPlan.width() * CELL + 1;
        int h = floorPlan.height() * CELL + 1;
        setPreferredSize(new Dimension(w, h));

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handlePress(e); }
            @Override public void mouseDragged(MouseEvent e) { handleDrag(e); }
            @Override public void mouseReleased(MouseEvent e) { handleRelease(e); }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public void setOnChange(Runnable r) { this.onChange = r; }
    public void setOnReject(java.util.function.Consumer<String> r) { this.onReject = r; }

    public void setMode(Mode m) { this.mode = m; }
    public Mode mode() { return mode; }
    public void setSelectedRoom(RoomType t) { this.selectedRoom = t; }
    public void setSelectedFacility(FacilityType t) { this.selectedFacility = t; }

    // ---- mouse handling -----------------------------------------------------

    private boolean editable(String action) {
        if (floorPlan.isLocked()) {
            reject("Floor plan is locked while the turn is running — " + action + " rejected.");
            return false;
        }
        return true;
    }

    private void handlePress(MouseEvent e) {
        int tx = e.getX() / CELL;
        int ty = e.getY() / CELL;
        if (!inBounds(tx, ty)) return;

        if (mode == Mode.DRAW_ROOM) {
            if (!editable("room draw")) return;
            dragStartX = dragCurX = tx;
            dragStartY = dragCurY = ty;
            repaint();
        } else {
            placeFacilityAt(tx, ty);
        }
    }

    private void handleDrag(MouseEvent e) {
        if (mode != Mode.DRAW_ROOM || dragStartX < 0) return;
        dragCurX = clamp(e.getX() / CELL, 0, floorPlan.width() - 1);
        dragCurY = clamp(e.getY() / CELL, 0, floorPlan.height() - 1);
        repaint();
    }

    private void handleRelease(MouseEvent e) {
        if (mode != Mode.DRAW_ROOM || dragStartX < 0) return;
        int x = Math.min(dragStartX, dragCurX);
        int y = Math.min(dragStartY, dragCurY);
        int w = Math.abs(dragCurX - dragStartX) + 1;
        int h = Math.abs(dragCurY - dragStartY) + 1;
        dragStartX = dragStartY = dragCurX = dragCurY = -1;

        if (!editable("room draw")) { repaint(); return; }

        int cost = selectedRoom.floorCostPerTile() * w * h;
        if (state.player().cash() < cost) {
            reject("Not enough cash for " + selectedRoom + " room ($" + cost + ").");
            repaint();
            return;
        }
        try {
            floorPlan.addRoom(new Room(selectedRoom, x, y, w, h));
            state.player().addCash(-cost);
            changed();
        } catch (IllegalStateException ex) {
            reject("Room rejected: " + ex.getMessage());
        }
        repaint();
    }

    private void placeFacilityAt(int tx, int ty) {
        if (!editable("facility placement")) return;
        GridPos p = GridPos.of(tx, ty);
        int cost = selectedFacility.cost();
        if (state.player().cash() < cost) {
            reject("Not enough cash for " + selectedFacility + " ($" + cost + ").");
            return;
        }
        boolean ok = floorPlan.placeFacility(Facility.at(selectedFacility, p));
        if (ok) {
            state.player().addCash(-cost);
            changed();
        } else {
            reject("Cannot place " + selectedFacility + " at (" + tx + "," + ty + ") — occupied/out of bounds.");
        }
        repaint();
    }

    private void changed() {
        if (onChange != null) onChange.run();
    }

    private void reject(String msg) {
        if (onReject != null) onReject.accept(msg);
    }

    private boolean inBounds(int tx, int ty) {
        return tx >= 0 && ty >= 0 && tx < floorPlan.width() && ty < floorPlan.height();
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    // ---- rendering ----------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Rooms
        List<Room> rooms = floorPlan.rooms();
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            Rectangle px = tileRect(r.x(), r.y(), r.width(), r.height());
            g.setColor(roomColor(r.type()));
            g.fillRect(px.x, px.y, px.width, px.height);
            g.setColor(roomColor(r.type()).darker());
            g.drawRect(px.x, px.y, px.width, px.height);
            g.setColor(new Color(255, 255, 255, 200));
            g.setFont(getFont().deriveFont(Font.BOLD, 10f));
            g.drawString(r.type().name(), px.x + 3, px.y + 12);
        }

        // Grid lines
        g.setColor(new Color(255, 255, 255, 28));
        for (int x = 0; x <= floorPlan.width(); x++) {
            g.drawLine(x * CELL, 0, x * CELL, floorPlan.height() * CELL);
        }
        for (int y = 0; y <= floorPlan.height(); y++) {
            g.drawLine(0, y * CELL, floorPlan.width() * CELL, y * CELL);
        }

        // Facilities (glyphs)
        for (int i = 0; i < rooms.size(); i++) {
            for (Facility f : rooms.get(i).facilities()) {
                drawFacility(g, f);
            }
        }
        // Also draw any facilities tracked directly by the floor plan (defensive scan).
        for (int x = 0; x < floorPlan.width(); x++) {
            for (int y = 0; y < floorPlan.height(); y++) {
                Facility f = floorPlan.facilityAt(GridPos.of(x, y));
                if (f != null) drawFacility(g, f);
            }
        }

        // Drag preview
        if (dragStartX >= 0) {
            int x = Math.min(dragStartX, dragCurX);
            int y = Math.min(dragStartY, dragCurY);
            int w = Math.abs(dragCurX - dragStartX) + 1;
            int h = Math.abs(dragCurY - dragStartY) + 1;
            Rectangle px = tileRect(x, y, w, h);
            g.setColor(new Color(roomColor(selectedRoom).getRed(),
                    roomColor(selectedRoom).getGreen(),
                    roomColor(selectedRoom).getBlue(), 110));
            g.fillRect(px.x, px.y, px.width, px.height);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
            g.drawRect(px.x, px.y, px.width, px.height);
            g.drawString(w + "x" + h + "  $" + (selectedRoom.floorCostPerTile() * w * h), px.x + 3, px.y + 24);
        }

        if (floorPlan.isLocked()) {
            g.setColor(new Color(200, 60, 60, 50));
            g.fillRect(0, 0, floorPlan.width() * CELL, floorPlan.height() * CELL);
        }
    }

    private void drawFacility(Graphics2D g, Facility f) {
        Rectangle c = tileRect(f.pos().x(), f.pos().y(), 1, 1);
        g.setColor(facilityBg(f.type()));
        g.fillRoundRect(c.x + 2, c.y + 2, c.width - 4, c.height - 4, 5, 5);
        g.setColor(Color.WHITE);
        g.setFont(getFont().deriveFont(Font.BOLD, 11f));
        String glyph = facilityGlyph(f.type());
        int gw = g.getFontMetrics().stringWidth(glyph);
        g.drawString(glyph, c.x + (CELL - gw) / 2, c.y + CELL - 5);
    }

    private static Rectangle tileRect(int tx, int ty, int w, int h) {
        return new Rectangle(tx * CELL, ty * CELL, w * CELL, h * CELL);
    }

    private static Color roomColor(RoomType t) {
        switch (t) {
            case DEVELOPMENT: return new Color(58, 96, 158);
            case RESEARCH:    return new Color(120, 80, 160);
            case QA:          return new Color(170, 110, 50);
            case LOUNGE:      return new Color(60, 140, 110);
            case SERVER:      return new Color(90, 90, 110);
            case MARKETING:   return new Color(170, 70, 110);
            default:          return new Color(80, 80, 80);
        }
    }

    private static Color facilityBg(FacilityType t) {
        switch (t) {
            case DESK:           return new Color(40, 40, 48);
            case COFFEE_MACHINE: return new Color(110, 70, 40);
            case HEATER:         return new Color(150, 60, 40);
            case PLANT:          return new Color(40, 120, 60);
            case ARCADE_CABINET: return new Color(120, 50, 130);
            default:             return new Color(50, 50, 50);
        }
    }

    private static String facilityGlyph(FacilityType t) {
        switch (t) {
            case DESK:           return "D";
            case COFFEE_MACHINE: return "C";
            case HEATER:         return "H";
            case PLANT:          return "P";
            case ARCADE_CABINET: return "A";
            default:             return "?";
        }
    }
}
