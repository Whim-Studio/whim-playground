package com.whim.firetop.ui;

import com.whim.firetop.model.Board;
import com.whim.firetop.model.Character;
import com.whim.firetop.model.GameState;
import com.whim.firetop.model.Room;
import com.whim.firetop.model.RoomType;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Custom-painted view of the dungeon: rooms drawn as tiles on a grid, corridors
 * as connecting lines, adventurer tokens, and reachable-room highlighting.
 * Clicking a highlighted room requests a move.
 */
public final class BoardPanel extends JPanel {

    /** Notified when the player clicks a reachable room. */
    public interface MoveHandler {
        void onRoomClicked(int roomId);
    }

    private static final int CELL_W = 150;
    private static final int CELL_H = 108;
    private static final int MARGIN = 40;
    private static final int ROOM_W = 116;
    private static final int ROOM_H = 74;

    private GameState state;
    private Set<Integer> reachable = new HashSet<Integer>();
    private MoveHandler moveHandler;
    private int hoverRoom = -1;

    public BoardPanel() {
        setBackground(Theme.BG_DARK);
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int rid = roomAt(e.getX(), e.getY());
                if (rid >= 0 && reachable.contains(rid) && moveHandler != null) {
                    moveHandler.onRoomClicked(rid);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int rid = roomAt(e.getX(), e.getY());
                if (rid != hoverRoom) {
                    hoverRoom = rid;
                    repaint();
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public void setState(GameState state) {
        this.state = state;
        revalidate();
        repaint();
    }

    public void setReachable(Set<Integer> reachable) {
        this.reachable = reachable == null ? new HashSet<Integer>() : reachable;
        repaint();
    }

    public void setMoveHandler(MoveHandler h) { this.moveHandler = h; }

    @Override
    public Dimension getPreferredSize() {
        if (state == null) {
            return new Dimension(800, 520);
        }
        int maxX = 0;
        int maxY = 0;
        for (Room r : state.getBoard().getRooms()) {
            maxX = Math.max(maxX, r.getGridX());
            maxY = Math.max(maxY, r.getGridY());
        }
        return new Dimension(MARGIN * 2 + (maxX + 1) * CELL_W, MARGIN * 2 + (maxY + 1) * CELL_H);
    }

    private int cx(Room r) { return MARGIN + r.getGridX() * CELL_W + CELL_W / 2; }
    private int cy(Room r) { return MARGIN + r.getGridY() * CELL_H + CELL_H / 2; }

    private int roomAt(int px, int py) {
        if (state == null) {
            return -1;
        }
        for (Room r : state.getBoard().getRooms()) {
            int x = cx(r);
            int y = cy(r);
            if (px >= x - ROOM_W / 2 && px <= x + ROOM_W / 2
                    && py >= y - ROOM_H / 2 && py <= y + ROOM_H / 2) {
                return r.getId();
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (state == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Board board = state.getBoard();

        // Corridors first (draw each undirected edge once).
        g2.setStroke(new BasicStroke(4f));
        Set<Long> drawn = new HashSet<Long>();
        for (Room r : board.getRooms()) {
            for (Integer e : r.getExits()) {
                long key = Math.min(r.getId(), e) * 1000L + Math.max(r.getId(), e);
                if (drawn.contains(key)) {
                    continue;
                }
                drawn.add(key);
                Room o = board.getRoom(e);
                g2.setColor(Theme.STONE);
                g2.drawLine(cx(r), cy(r), cx(o), cy(o));
            }
        }

        // Rooms.
        for (Room r : board.getRooms()) {
            drawRoom(g2, r);
        }

        // Tokens (offset so multiple adventurers in one room don't fully overlap).
        List<Character> players = state.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            Character c = players.get(i);
            Room r = board.getRoom(c.getRoomId());
            if (r == null) {
                continue;
            }
            int ox = (i % 2) * 20 - 10;
            int oy = (i / 2) * 20 - 10;
            int tx = cx(r) + ox;
            int ty = cy(r) + oy + 8;
            g2.setColor(c.isAlive() ? Theme.PLAYER_COLORS[i % 4] : Color.DARK_GRAY);
            g2.fillOval(tx - 9, ty - 9, 18, 18);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(tx - 9, ty - 9, 18, 18);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            String tag = String.valueOf(i + 1);
            g2.drawString(tag, tx - 3, ty + 4);
        }
    }

    private void drawRoom(Graphics2D g2, Room r) {
        int x = cx(r) - ROOM_W / 2;
        int y = cy(r) - ROOM_H / 2;
        boolean isReachable = reachable.contains(r.getId());

        Color fill = roomColor(r);
        if (!r.isVisited()) {
            fill = fill.darker().darker();
        }
        g2.setColor(fill);
        g2.fillRoundRect(x, y, ROOM_W, ROOM_H, 14, 14);

        // Border: green + thick when reachable, gold on hover, else stone.
        if (isReachable) {
            g2.setColor(Theme.REACHABLE);
            g2.setStroke(new BasicStroke(r.getId() == hoverRoom ? 5f : 3.5f));
        } else if (r.getId() == hoverRoom) {
            g2.setColor(Theme.GOLD);
            g2.setStroke(new BasicStroke(2.5f));
        } else {
            g2.setColor(Theme.STONE_LIGHT);
            g2.setStroke(new BasicStroke(2f));
        }
        g2.drawRoundRect(x, y, ROOM_W, ROOM_H, 14, 14);

        g2.setColor(r.isVisited() ? Theme.PARCHMENT : Theme.STONE_LIGHT);
        g2.setFont(new Font("Serif", Font.BOLD, 13));
        String name = r.isVisited() ? r.getName() : "?";
        drawCentered(g2, name, cx(r), y + 20);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.setColor(r.isVisited() ? Theme.GOLD : Theme.STONE_LIGHT);
        String tag = roomTag(r);
        if (r.isVisited() && tag != null) {
            drawCentered(g2, tag, cx(r), y + 38);
        }
    }

    private String roomTag(Room r) {
        switch (r.getType()) {
            case MONSTER:
                return r.isResolved() ? "cleared" : (r.getMonster() != null ? r.getMonster().getName() : "monster");
            case LAIR:
                return r.isResolved() ? "cleared" : "ZAGOR";
            case TREASURE:
                return r.isResolved() ? "looted" : "treasure";
            case TRAP:
                return "trap";
            case EVENT:
                return "event";
            case SPECIAL:
                return "shrine";
            case ENTRANCE:
                return "gate";
            default:
                return null;
        }
    }

    private Color roomColor(Room r) {
        switch (r.getType()) {
            case LAIR: return Theme.BLOOD.darker();
            case MONSTER: return new Color(86, 52, 52);
            case TREASURE: return new Color(74, 66, 40);
            case TRAP: return new Color(70, 50, 60);
            case EVENT: return new Color(52, 58, 74);
            case SPECIAL: return new Color(48, 70, 62);
            case ENTRANCE: return new Color(52, 62, 52);
            default: return Theme.BG_PANEL;
        }
    }

    private void drawCentered(Graphics2D g2, String s, int cx, int y) {
        int w = g2.getFontMetrics().stringWidth(s);
        g2.drawString(s, cx - w / 2, y);
    }
}
