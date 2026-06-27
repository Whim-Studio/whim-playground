package com.whim.tacticalnexus.ui;

import com.whim.tacticalnexus.state.StateManager;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Top-level window. {@link BorderLayout} with the {@link GamePanel} in the CENTER
 * and the {@link StatusPanel} on the EAST. Keyboard control:
 *
 * <ul>
 *   <li>Arrow keys → {@link GameController#move}</li>
 *   <li>Ctrl+Z → undo</li>
 *   <li>Ctrl+Y or Ctrl+Shift+Z → redo</li>
 * </ul>
 *
 * After any handled key the game panel repaints and the status dashboard
 * refreshes. The frame is focusable and grabs focus so the {@link KeyListener}
 * receives events.
 */
public final class GameFrame extends JFrame implements KeyListener {

    private final GameController controller;
    private final GamePanel gamePanel;
    private final StatusPanel statusPanel;

    public GameFrame(StateManager stateManager) {
        super("Tactical Nexus");
        this.controller = new GameController(stateManager);
        this.gamePanel = new GamePanel(stateManager);
        this.statusPanel = new StatusPanel(stateManager);

        setLayout(new BorderLayout());
        add(gamePanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.EAST);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setFocusable(true);
        addKeyListener(this);
        pack();
        setLocationRelativeTo(null);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            requestFocusInWindow();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        boolean handled = false;
        boolean ctrl = e.isControlDown();
        int code = e.getKeyCode();

        if (ctrl && code == KeyEvent.VK_Z && e.isShiftDown()) {
            handled = controller.redo();
        } else if (ctrl && code == KeyEvent.VK_Z) {
            handled = controller.undo();
        } else if (ctrl && code == KeyEvent.VK_Y) {
            handled = controller.redo();
        } else if (!ctrl) {
            switch (code) {
                case KeyEvent.VK_UP:
                    handled = controller.move(-1, 0);
                    break;
                case KeyEvent.VK_DOWN:
                    handled = controller.move(1, 0);
                    break;
                case KeyEvent.VK_LEFT:
                    handled = controller.move(0, -1);
                    break;
                case KeyEvent.VK_RIGHT:
                    handled = controller.move(0, 1);
                    break;
                default:
                    break;
            }
        }

        if (handled) {
            gamePanel.repaint();
            statusPanel.refresh();
            statusPanel.setMessage(controller.lastMessage());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // No-op.
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No-op.
    }
}
