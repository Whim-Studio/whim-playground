package com.rampart.ui;

import com.rampart.engine.GameApi;
import com.rampart.model.GameStateView;
import com.rampart.model.Phase;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Translates raw Swing mouse/keyboard events on the {@link GamePanel} into
 * {@link GameApi} calls, choosing the action by the snapshot's current
 * {@link Phase}. It performs NO validation of its own — it converts pixels to a
 * grid cell and forwards the intent; the engine accepts or rejects it.
 *
 * <p>Bindings:
 * <ul>
 *   <li>BUILD: left-click &rarr; {@code placeCannon(col,row)}</li>
 *   <li>BATTLE: left-click &rarr; {@code fireCannonAt(col,row)}</li>
 *   <li>REPAIR: move &rarr; track hover; left-click &rarr; {@code placePieceAt};
 *       right-click / {@code R} &rarr; {@code rotatePiece}</li>
 *   <li>SPACE / ENTER &rarr; {@code endPhaseEarly}</li>
 * </ul>
 */
public final class InputHandler implements MouseListener, MouseMotionListener, KeyListener {

    private final GameApi api;
    private final GamePanel panel;

    public InputHandler(GameApi api, GamePanel panel) {
        this.api = api;
        this.panel = panel;
    }

    /** Registers this handler for mouse, motion, and key events on the panel. */
    public void attach() {
        panel.addMouseListener(this);
        panel.addMouseMotionListener(this);
        panel.addKeyListener(this);
    }

    // ---- Pixel → cell --------------------------------------------------------

    private int colAt(int px) { return px / panel.cellSize(); }
    private int rowAt(int py) { return py / panel.cellSize(); }

    /** True while the pixel lies inside the playfield (not the HUD strip). */
    private boolean inPlayfield(int px, int py) {
        return px >= 0 && px < GamePanel.GRID_W && py >= 0 && py < GamePanel.GRID_H;
    }

    // ---- Mouse ---------------------------------------------------------------

    @Override
    public void mousePressed(MouseEvent e) {
        panel.requestFocusInWindow();
        int px = e.getX();
        int py = e.getY();
        if (!inPlayfield(px, py)) return;
        int col = colAt(px);
        int row = rowAt(py);

        GameStateView s = api.state();
        Phase phase = s.phase();
        boolean right = e.getButton() == MouseEvent.BUTTON3;

        if (phase == Phase.BUILD) {
            if (!right) api.placeCannon(col, row);
        } else if (phase == Phase.BATTLE) {
            if (!right) api.fireCannonAt(col, row);
        } else if (phase == Phase.REPAIR) {
            if (right) {
                api.rotatePiece();
            } else {
                api.placePieceAt(col, row);
            }
            panel.setHover(col, row);
        }
        panel.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateHover(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        updateHover(e);
    }

    private void updateHover(MouseEvent e) {
        if (api.state().phase() != Phase.REPAIR) return;
        int px = e.getX();
        int py = e.getY();
        if (inPlayfield(px, py)) {
            panel.setHover(colAt(px), rowAt(py));
        } else {
            panel.setHover(-1, -1);
        }
        panel.repaint();
    }

    @Override public void mouseExited(MouseEvent e) {
        panel.setHover(-1, -1);
        panel.repaint();
    }

    @Override public void mouseClicked(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }

    // ---- Keyboard ------------------------------------------------------------

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_R) {
            api.rotatePiece();
            panel.repaint();
        } else if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_ENTER) {
            api.endPhaseEarly();
            panel.repaint();
        }
    }

    @Override public void keyReleased(KeyEvent e) { }
    @Override public void keyTyped(KeyEvent e) { }
}
