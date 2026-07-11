package com.whim.bc3k.api;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * A single full-window bridge console (or menu/end) screen. The shell owns the
 * JFrame, the Swing Timer tick loop, and event dispatch; it hands every screen a
 * live {@link GameController} at construction and calls these methods.
 *
 * Screens are pure views over the controller: draw from {@link GameController#view()},
 * translate input into controller intents. Do not keep authoritative game state
 * in a Screen.
 */
public interface Screen {

    /** Which mode this screen renders. The shell shows it when view().mode() == mode(). */
    Enums.Mode mode();

    void onEnter();
    void onExit();

    /** Fixed-step animation hook. Game sim advances via {@link GameController#tick}. */
    void update(double dtSeconds);

    /** Render into a viewport of the given pixel size. */
    void render(Graphics2D g, int width, int height);

    // Input — default no-ops keep implementations small.
    default void keyPressed(KeyEvent e) {}
    default void keyReleased(KeyEvent e) {}
    default void mousePressed(MouseEvent e) {}
    default void mouseReleased(MouseEvent e) {}
    default void mouseDragged(MouseEvent e) {}
    default void mouseMoved(MouseEvent e) {}
}
