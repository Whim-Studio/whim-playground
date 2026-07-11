package com.whim.bc3k.app;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.api.GameController;
import com.whim.bc3k.api.Screen;
import com.whim.bc3k.render.Palette;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Owns the window, the ~60 FPS Swing Timer game loop, and input dispatch to the
 * active screen. Global "always available from the bridge" keys are handled here
 * so every console shares them; everything else is delegated to the active screen.
 */
public final class GameFrame extends JFrame {
    private static final int FPS = 60;
    private final GameController controller;
    private final ScreenManager screens;
    private final Canvas canvas = new Canvas();
    private long last = System.nanoTime();

    public GameFrame(GameController controller) {
        super("Battlecruiser 3000AD");
        this.controller = controller;
        this.screens = new ScreenManager(controller);

        canvas.setPreferredSize(new Dimension(1120, 720));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(canvas);
        pack();
        setLocationRelativeTo(null);
        setResizable(true);

        canvas.setFocusable(true);
        canvas.requestFocusInWindow();
        wireInput();

        Timer timer = new Timer(1000 / FPS, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { step(); }
        });
        timer.start();
    }

    private void step() {
        long now = System.nanoTime();
        double dt = Math.min(0.05, (now - last) / 1_000_000_000.0);
        last = now;
        if (!controller.view().paused()) controller.tick(dt);
        screens.current().update(dt);
        canvas.repaint();
    }

    private void wireInput() {
        canvas.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (handleGlobal(e)) return;
                screens.current().keyPressed(e);
            }
            @Override public void keyReleased(KeyEvent e) { screens.current().keyReleased(e); }
        });
        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { canvas.requestFocusInWindow(); screens.current().mousePressed(e); }
            @Override public void mouseReleased(MouseEvent e) { screens.current().mouseReleased(e); }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) { screens.current().mouseDragged(e); }
            @Override public void mouseMoved(MouseEvent e) { screens.current().mouseMoved(e); }
        });
    }

    /** Bridge-wide shortcuts. Returns true if the key was consumed here. */
    private boolean handleGlobal(KeyEvent e) {
        int code = e.getKeyCode();

        // ESC returns to the main menu from anywhere.
        if (code == KeyEvent.VK_ESCAPE) { controller.setMode(Enums.Mode.MENU); return true; }

        // Global keys only apply once a game is running.
        if (!controller.view().started()) return false;

        // F1..F8 switch bridge consoles.
        if (code >= KeyEvent.VK_F1 && code <= KeyEvent.VK_F8) {
            int idx = code - KeyEvent.VK_F1;
            if (idx < ScreenManager.CONSOLES.length) controller.setMode(ScreenManager.CONSOLES[idx]);
            return true;
        }

        if (code == KeyEvent.VK_P) { controller.togglePause(); return true; }

        // Verified original shortcuts: Shift+R restart reactor, Ctrl+S request tow.
        if (code == KeyEvent.VK_R && e.isShiftDown()) { controller.restartReactor(); return true; }
        if (code == KeyEvent.VK_S && e.isControlDown()) { controller.requestTow(); return true; }

        return false;
    }

    private final class Canvas extends JPanel {
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            int w = getWidth(), h = getHeight();
            g.setColor(Palette.BG);
            g.fillRect(0, 0, w, h);
            Screen s = screens.current();
            s.render(g, w, h);
        }
    }
}
