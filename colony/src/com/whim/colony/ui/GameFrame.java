package com.whim.colony.ui;

import com.whim.colony.ColonyState;
import com.whim.colony.domain.Colonist;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Top-level window for the colony sim. Lays out the {@link MapPanel} in the
 * CENTER, a {@link HudPanel} on the EAST, and a thin status/message bar on the
 * SOUTH. Owns a Swing {@link Timer} that repaints the view at a steady frame
 * rate — this is <em>view refresh only</em> and is fully decoupled from the
 * simulation tick, which the engine (Task 2) drives.
 *
 * <p>The frame is constructed with a {@link ColonyState}: the engine keeps
 * mutating that object, and the UI simply reads whatever it currently holds.
 * The ONLY state the UI mutates is {@link ColonyState#setPaused(boolean)} — a
 * pure view/pause flag toggled with SPACE.
 */
public class GameFrame extends JFrame {

    /** View refresh rate. NOT a simulation tick. */
    public static final int REFRESH_FPS = 30;

    private ColonyState state;
    private final MapPanel mapPanel;
    private final HudPanel hudPanel;
    private final JLabel statusBar;
    private final Timer refreshTimer;

    public GameFrame(ColonyState state) {
        super("Colony Sim");
        this.state = state;

        this.mapPanel = new MapPanel(state);
        this.hudPanel = new HudPanel(state, mapPanel);
        this.statusBar = new JLabel(" ");
        statusBar.setOpaque(true);
        statusBar.setBackground(new Color(24, 25, 30));
        statusBar.setForeground(new Color(200, 202, 210));
        statusBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(mapPanel, BorderLayout.CENTER);
        add(hudPanel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        installMouseInput();
        installKeyInput();

        pack();
        setSize(900, 680);
        setLocationRelativeTo(null);

        // ---- view refresh timer: repaint + HUD refresh, no simulation ----
        int delayMs = Math.max(1, 1000 / REFRESH_FPS);
        this.refreshTimer = new Timer(delayMs, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshView();
            }
        });
    }

    /** Start the view refresh timer. Call after the frame is visible. */
    public void startRefresh() {
        if (!refreshTimer.isRunning()) {
            refreshTimer.start();
        }
    }

    public void stopRefresh() {
        refreshTimer.stop();
    }

    /** Swap in a new state to view (rarely needed; engine mutates in place). */
    public void setState(ColonyState state) {
        this.state = state;
        mapPanel.setState(state);
        hudPanel.setState(state);
        refreshView();
    }

    public ColonyState getColonyState() {
        return state;
    }

    public MapPanel getMapPanel() {
        return mapPanel;
    }

    public HudPanel getHudPanel() {
        return hudPanel;
    }

    /** Repaint the map and refresh the HUD from current state. Idempotent. */
    public void refreshView() {
        mapPanel.repaint();
        hudPanel.refresh();
        updateStatusBar();
    }

    private void updateStatusBar() {
        if (state == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(state.isPaused() ? "PAUSED" : "RUNNING");
        sb.append("   |   tick ").append(state.getTick());
        sb.append("   |   colonists ").append(state.getColonists().size());
        sb.append("   |   zoom ").append(mapPanel.getTileSize()).append("px");
        sb.append("   |   [WASD/arrows] pan  [+/-] zoom  [SPACE] pause  [click] select");
        java.util.List<String> log = state.getMessageLog();
        if (!log.isEmpty()) {
            sb.append("   |   ").append(log.get(log.size() - 1));
        }
        statusBar.setText(sb.toString());
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    private void installMouseInput() {
        mapPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mapPanel.requestFocusInWindow();
                Colonist selected = mapPanel.selectAtPixel(e.getX(), e.getY());
                hudPanel.refresh();
                updateStatusBar();
                // selected may be null (a bare tile was picked); HUD handles both
                if (selected != null) {
                    // no-op: selection is already stored on the map panel
                }
            }
        });
    }

    private void installKeyInput() {
        mapPanel.setFocusable(true);
        mapPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode());
            }
        });
    }

    private void handleKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                mapPanel.panTiles(-1, 0);
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                mapPanel.panTiles(1, 0);
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                mapPanel.panTiles(0, -1);
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                mapPanel.panTiles(0, 1);
                break;
            case KeyEvent.VK_EQUALS: // '+' (unshifted) and '='
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_ADD:
                mapPanel.zoom(+2);
                break;
            case KeyEvent.VK_MINUS:
            case KeyEvent.VK_SUBTRACT:
                mapPanel.zoom(-2);
                break;
            case KeyEvent.VK_SPACE:
                // the ONLY simulation-state mutation the UI performs: a view/pause flag
                if (state != null) {
                    state.setPaused(!state.isPaused());
                }
                break;
            default:
                return;
        }
        updateStatusBar();
    }
}
