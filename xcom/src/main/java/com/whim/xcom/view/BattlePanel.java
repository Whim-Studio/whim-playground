package com.whim.xcom.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import com.whim.xcom.app.GameContext;
import com.whim.xcom.battle.BattleGame;
import com.whim.xcom.battle.BattleMap;
import com.whim.xcom.battle.BattleUnit;
import com.whim.xcom.battle.Side;
import com.whim.xcom.battle.Tile;
import com.whim.xcom.model.FireMode;

/**
 * The Battlescape screen. Renders the tactical map in an isometric projection
 * with Java2D placeholder art, shows a soldier HUD (TU/HP, fire modes, kneel,
 * end-turn) and an event log, and translates mouse/keyboard input into engine
 * calls. All game logic lives in {@link BattleGame}; this class only draws and
 * forwards intent.
 */
public final class BattlePanel extends JPanel implements BattleGame.Listener {

    private static final int TILE_W = 48;
    private static final int TILE_H = 24;
    private static final int MARGIN = 40;
    private static final int BLOCK_H = 22;

    private final transient GameContext ctx;
    private final transient BattleGame game;
    private final transient Runnable onExit;

    private final MapCanvas canvas = new MapCanvas();
    private final JTextArea log = new JTextArea();
    private final JLabel unitLabel = new JLabel(" ");
    private final JLabel statLabel = new JLabel(" ");
    private final JLabel hoverLabel = new JLabel(" ");
    private final JLabel turnLabel = new JLabel(" ");

    private transient BattleUnit selected;
    private FireMode mode = FireMode.SNAP;
    private int hoverX = -1;
    private int hoverY = -1;
    private boolean ended;

    public BattlePanel(GameContext ctx, BattleGame game, Runnable onExit) {
        this.ctx = ctx;
        this.game = game;
        this.onExit = onExit;
        setLayout(new BorderLayout());
        setBackground(new Color(8, 12, 16));

        game.setListener(this);
        add(buildTopBar(), BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);
        add(buildHud(), BorderLayout.SOUTH);

        selectFirstSoldier();
        installKeys();
        refreshHud();
    }

    // ---- UI construction ----------------------------------------------------

    private JComponent buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(16, 24, 20));
        turnLabel.setForeground(new Color(150, 230, 170));
        turnLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        turnLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        bar.add(turnLabel, BorderLayout.WEST);

        JButton exit = pixelButton("Abort Mission");
        exit.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (onExit != null) {
                    onExit.run();
                }
            }
        });
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.add(exit);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JComponent buildSidebar() {
        log.setEditable(false);
        log.setBackground(new Color(6, 10, 12));
        log.setForeground(new Color(170, 210, 180));
        log.setFont(new Font("Monospaced", Font.PLAIN, 12));
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(log);
        sp.setPreferredSize(new Dimension(240, 100));
        sp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 90, 70)), "Combat Log"));
        return sp;
    }

    private JComponent buildHud() {
        JPanel hud = new JPanel(new BorderLayout());
        hud.setBackground(new Color(16, 24, 20));
        hud.setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new javax.swing.BoxLayout(info, javax.swing.BoxLayout.Y_AXIS));
        for (JLabel l : new JLabel[] {unitLabel, statLabel, hoverLabel}) {
            l.setForeground(new Color(200, 230, 205));
            l.setFont(new Font("Monospaced", Font.PLAIN, 13));
            info.add(l);
        }
        hud.add(info, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.add(modeButton("Snap [1]", FireMode.SNAP));
        buttons.add(modeButton("Aimed [2]", FireMode.AIMED));
        buttons.add(modeButton("Auto [3]", FireMode.AUTO));

        JButton kneel = pixelButton("Kneel [K]");
        kneel.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { toggleKneel(); }
        });
        buttons.add(kneel);

        JButton next = pixelButton("Next [Space]");
        next.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { cycleSoldier(); }
        });
        buttons.add(next);

        JButton end = pixelButton("End Turn [Enter]");
        end.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { endTurn(); }
        });
        buttons.add(end);

        hud.add(buttons, BorderLayout.EAST);
        return hud;
    }

    private JButton modeButton(String text, final FireMode m) {
        JButton b = pixelButton(text);
        b.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                mode = m;
                refreshHud();
            }
        });
        return b;
    }

    private JButton pixelButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Monospaced", Font.BOLD, 12));
        b.setForeground(new Color(220, 240, 220));
        b.setBackground(new Color(24, 44, 32));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(90, 160, 110), 1));
        return b;
    }

    private void installKeys() {
        bindKey("SPACE", "next", new Runnable() { public void run() { cycleSoldier(); } });
        bindKey("K", "kneel", new Runnable() { public void run() { toggleKneel(); } });
        bindKey("ENTER", "end", new Runnable() { public void run() { endTurn(); } });
        bindKey("1", "snap", new Runnable() { public void run() { mode = FireMode.SNAP; refreshHud(); } });
        bindKey("2", "aimed", new Runnable() { public void run() { mode = FireMode.AIMED; refreshHud(); } });
        bindKey("3", "auto", new Runnable() { public void run() { mode = FireMode.AUTO; refreshHud(); } });
    }

    private void bindKey(String key, String name, final Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    // ---- intent -------------------------------------------------------------

    private void selectFirstSoldier() {
        java.util.List<BattleUnit> soldiers = game.living(Side.XCOM);
        if (!soldiers.isEmpty()) {
            selected = soldiers.get(0);
        }
    }

    private void cycleSoldier() {
        java.util.List<BattleUnit> soldiers = game.living(Side.XCOM);
        if (soldiers.isEmpty()) {
            return;
        }
        int idx = soldiers.indexOf(selected);
        selected = soldiers.get((idx + 1) % soldiers.size());
        refreshHud();
        canvas.repaint();
    }

    private void toggleKneel() {
        if (selected == null || game.currentSide() != Side.XCOM) {
            return;
        }
        selected.setKneeling(!selected.kneeling());
        refreshHud();
        canvas.repaint();
    }

    private void endTurn() {
        if (game.finished() || game.currentSide() != Side.XCOM) {
            return;
        }
        game.endTurn();
        // A fresh player turn — keep a live selection.
        if (selected == null || !selected.alive()) {
            selectFirstSoldier();
        }
        refreshHud();
        canvas.repaint();
        maybeShowResult();
    }

    private void onTileClicked(int tx, int ty) {
        if (game.finished() || game.currentSide() != Side.XCOM) {
            return;
        }
        BattleUnit u = game.unitAt(tx, ty);
        if (u != null && u.side() == Side.XCOM) {
            selected = u;                       // select own soldier
            refreshHud();
            canvas.repaint();
            return;
        }
        if (selected == null) {
            return;
        }
        if (u != null && u.alien() && game.visible(tx, ty)) {
            game.fire(selected, u, mode);       // shoot the alien
        } else if (u == null && game.map().walkable(tx, ty)) {
            game.moveUnit(selected, tx, ty);     // move there
        }
        refreshHud();
        canvas.repaint();
        maybeShowResult();
    }

    private void maybeShowResult() {
        if (!game.finished() || ended) {
            return;
        }
        ended = true;
        boolean win = game.winner() == Side.XCOM;
        String msg = win
                ? "All aliens eliminated.\nMission accomplished, Commander."
                : "The squad has been wiped out.\nMission failed.";
        JOptionPane.showMessageDialog(this, msg,
                win ? "Victory" : "Defeat",
                win ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        if (onExit != null) {
            onExit.run();
        }
    }

    private void refreshHud() {
        turnLabel.setText("Turn " + game.turn() + "   |   "
                + (game.currentSide() == Side.XCOM ? "X-COM turn" : "Alien turn")
                + (game.night() ? "   (night)" : "   (day)")
                + "   |   Aliens left: " + game.living(Side.ALIEN).size());
        if (selected == null) {
            unitLabel.setText("No soldier selected");
            statLabel.setText(" ");
        } else {
            unitLabel.setText(selected.name()
                    + "   " + (selected.weapon() != null ? selected.weapon().name() : "unarmed")
                    + (selected.weapon() != null && selected.weapon().clipSize() > 0
                        ? " (ammo " + selected.ammo() + ")" : "")
                    + (selected.kneeling() ? "   [kneeling]" : ""));
            statLabel.setText(String.format("TU %d/%d    HP %d/%d    Morale %d    Mode: %s",
                    selected.tu(), selected.maxTU(), selected.health(), selected.maxHealth(),
                    selected.morale(), mode));
        }
        BattleUnit hover = (hoverX >= 0) ? game.unitAt(hoverX, hoverY) : null;
        if (selected != null && hover != null && hover.alien() && game.visible(hoverX, hoverY)) {
            int chance = game.hitChance(selected, hover, mode);
            int cost = game.fireCost(selected, mode);
            hoverLabel.setText("Target " + hover.name() + "   hit " + chance + "%   cost "
                    + (cost == Integer.MAX_VALUE ? "-" : cost + " TU"));
        } else {
            hoverLabel.setText("Left-click: select soldier / move / fire at alien");
        }
    }

    // ---- BattleGame.Listener ------------------------------------------------

    @Override public void onEvent(String message) {
        log.append(message + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    @Override public void onChanged() {
        refreshHud();
        canvas.repaint();
    }

    // ---- rendering ----------------------------------------------------------

    /** Isometric map canvas + input translation. */
    private final class MapCanvas extends JComponent {

        MapCanvas() {
            setPreferredSize(new Dimension(760, 460));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    int[] t = toTile(e.getX(), e.getY());
                    if (t != null) {
                        onTileClicked(t[0], t[1]);
                    }
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int[] t = toTile(e.getX(), e.getY());
                    int nx = t == null ? -1 : t[0];
                    int ny = t == null ? -1 : t[1];
                    if (nx != hoverX || ny != hoverY) {
                        hoverX = nx;
                        hoverY = ny;
                        refreshHud();
                        repaint();
                    }
                }
            });
        }

        private int originX() {
            return MARGIN + (game.map().height() - 1) * (TILE_W / 2);
        }

        private int originY() {
            return MARGIN + TILE_H;
        }

        private int sx(int x, int y) {
            return originX() + (x - y) * (TILE_W / 2);
        }

        private int sy(int x, int y) {
            return originY() + (x + y) * (TILE_H / 2);
        }

        private int[] toTile(int px, int py) {
            double u = (px - originX()) / (double) (TILE_W / 2);
            double v = (py - originY()) / (double) (TILE_H / 2);
            int x = (int) Math.round((u + v) / 2.0);
            int y = (int) Math.round((v - u) / 2.0);
            if (game.map().inBounds(x, y)) {
                return new int[] {x, y};
            }
            return null;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(8, 12, 16));
            g2.fillRect(0, 0, getWidth(), getHeight());

            BattleMap map = game.map();
            // Draw tiles back-to-front (increasing x+y) so raised blocks overlap.
            for (int sum = 0; sum <= map.width() + map.height(); sum++) {
                for (int x = 0; x < map.width(); x++) {
                    int y = sum - x;
                    if (y < 0 || y >= map.height()) {
                        continue;
                    }
                    drawTile(g2, map, x, y);
                }
            }
            // Units, same ordering.
            for (int sum = 0; sum <= map.width() + map.height(); sum++) {
                for (int x = 0; x < map.width(); x++) {
                    int y = sum - x;
                    if (y < 0 || y >= map.height()) {
                        continue;
                    }
                    BattleUnit u = game.unitAt(x, y);
                    if (u != null) {
                        drawUnit(g2, u);
                    }
                }
            }
        }

        private void drawTile(Graphics2D g2, BattleMap map, int x, int y) {
            if (!game.discovered(x, y)) {
                return; // unseen — leave black
            }
            boolean vis = game.visible(x, y);
            int cx = sx(x, y);
            int cy = sy(x, y);
            Tile.Kind k = map.tile(x, y).kind();
            Color top = tileColor(k);
            if (!vis) {
                top = top.darker().darker();
            }
            Polygon dia = diamond(cx, cy);
            g2.setColor(top);
            g2.fillPolygon(dia);
            g2.setColor(new Color(0, 0, 0, vis ? 70 : 110));
            g2.drawPolygon(dia);

            if (map.tile(x, y).blocksSight() || !map.tile(x, y).walkable()) {
                drawBlock(g2, cx, cy, k, vis);
            }
            // hover / selection highlight
            if (x == hoverX && y == hoverY) {
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fillPolygon(dia);
            }
        }

        private Polygon diamond(int cx, int cy) {
            Polygon p = new Polygon();
            p.addPoint(cx, cy - TILE_H / 2);
            p.addPoint(cx + TILE_W / 2, cy);
            p.addPoint(cx, cy + TILE_H / 2);
            p.addPoint(cx - TILE_W / 2, cy);
            return p;
        }

        private void drawBlock(Graphics2D g2, int cx, int cy, Tile.Kind k, boolean vis) {
            Color side = blockColor(k);
            Color side2 = side.darker();
            if (!vis) {
                side = side.darker().darker();
                side2 = side2.darker();
            }
            int hw = TILE_W / 2;
            int hh = TILE_H / 2;
            // left face
            Polygon left = new Polygon();
            left.addPoint(cx - hw, cy);
            left.addPoint(cx, cy + hh);
            left.addPoint(cx, cy + hh - BLOCK_H);
            left.addPoint(cx - hw, cy - BLOCK_H);
            g2.setColor(side2);
            g2.fillPolygon(left);
            // right face
            Polygon right = new Polygon();
            right.addPoint(cx + hw, cy);
            right.addPoint(cx, cy + hh);
            right.addPoint(cx, cy + hh - BLOCK_H);
            right.addPoint(cx + hw, cy - BLOCK_H);
            g2.setColor(side);
            g2.fillPolygon(right);
            // raised top
            Polygon topFace = new Polygon();
            topFace.addPoint(cx, cy - hh - BLOCK_H);
            topFace.addPoint(cx + hw, cy - BLOCK_H);
            topFace.addPoint(cx, cy + hh - BLOCK_H);
            topFace.addPoint(cx - hw, cy - BLOCK_H);
            g2.setColor(vis ? side.brighter() : side);
            g2.fillPolygon(topFace);
            g2.setColor(new Color(0, 0, 0, 90));
            g2.drawPolygon(topFace);
        }

        private void drawUnit(Graphics2D g2, BattleUnit u) {
            if (u.alien() && !game.visible(u.x(), u.y())) {
                return; // hidden by fog
            }
            int cx = sx(u.x(), u.y());
            int cy = sy(u.x(), u.y());
            int top = cy - (u.kneeling() ? 8 : 16);
            int r = 9;

            if (u == selected) {
                g2.setColor(new Color(255, 230, 90));
                g2.drawOval(cx - TILE_W / 2 + 6, cy - 6, TILE_W - 12, 12);
            }
            // shadow
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillOval(cx - 8, cy - 3, 16, 6);
            // body
            Color body = u.alien() ? new Color(190, 70, 200) : new Color(70, 190, 120);
            g2.setColor(body);
            g2.fillRoundRect(cx - r, top, 2 * r, cy - top + 4, 8, 8);
            g2.setColor(body.brighter());
            g2.fillOval(cx - 7, top - 8, 14, 14); // head
            // facing tick
            int[] dxTick = {0, 6, 8, 6, 0, -6, -8, -6};
            int[] dyTick = {-10, -8, 0, 8, 10, 8, 0, -8};
            g2.setColor(Color.WHITE);
            int f = u.facing();
            g2.drawLine(cx, top, cx + dxTick[f], top + dyTick[f] / 2);
            // HP bar
            int barW = 22;
            int hp = (int) Math.round(barW * (u.health() / (double) u.maxHealth()));
            g2.setColor(new Color(40, 0, 0));
            g2.fillRect(cx - barW / 2, top - 16, barW, 3);
            g2.setColor(u.alien() ? new Color(230, 90, 90) : new Color(90, 220, 120));
            g2.fillRect(cx - barW / 2, top - 16, hp, 3);
        }

        private Color tileColor(Tile.Kind k) {
            switch (k) {
                case ROAD: return new Color(70, 70, 78);
                case DIRT: return new Color(96, 78, 54);
                case BUSH: return new Color(38, 78, 44);
                case ROCK: return new Color(90, 90, 96);
                case WALL: return new Color(110, 100, 92);
                case UFO_HULL: return new Color(60, 110, 90);
                case UFO_FLOOR: return new Color(48, 92, 78);
                case GRASS:
                default: return new Color(46, 92, 52);
            }
        }

        private Color blockColor(Tile.Kind k) {
            switch (k) {
                case UFO_HULL: return new Color(70, 150, 120);
                case WALL: return new Color(120, 110, 96);
                case ROCK:
                default: return new Color(96, 96, 104);
            }
        }
    }
}
