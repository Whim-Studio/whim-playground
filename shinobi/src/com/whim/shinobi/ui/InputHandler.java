package com.whim.shinobi.ui;

import com.whim.shinobi.api.GameController;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Non-blocking {@link java.awt.event.KeyListener} that translates keys into
 * {@link GameController} calls. Held movement (left/right/crouch) is driven by
 * press=true / release=false so it stays smooth; discrete actions (jump, plane
 * shift, attack, ninjutsu, pause) fire once per press using an edge-guard so
 * auto-repeat doesn't spam the engine. Every call is cheap and returns on the EDT.
 *
 * Bindings:
 *   Move left   : A / Left
 *   Move right  : D / Right
 *   Crouch      : S / Down
 *   Jump        : Space / W / Up
 *   Plane-shift : L / Shift
 *   Attack      : J / Z
 *   Ninjutsu    : K / X
 *   Pause       : P
 *   New game    : Enter
 */
public final class InputHandler extends KeyAdapter {

    private final GameController controller;

    // edge-guards for discrete actions so key-repeat doesn't retrigger
    private boolean jumpDown, shiftDown, attackDown, ninjutsuDown, pauseDown;
    private boolean leftHeld, rightHeld, crouchHeld;

    public InputHandler(GameController controller) {
        this.controller = controller;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch (k) {
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                if (!leftHeld) { leftHeld = true; controller.setLeft(true); }
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                if (!rightHeld) { rightHeld = true; controller.setRight(true); }
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                if (!crouchHeld) { crouchHeld = true; controller.setCrouch(true); }
                break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                if (!jumpDown) { jumpDown = true; controller.jump(); }
                break;
            case KeyEvent.VK_L:
            case KeyEvent.VK_SHIFT:
                if (!shiftDown) { shiftDown = true; controller.shiftPlane(); }
                break;
            case KeyEvent.VK_J:
            case KeyEvent.VK_Z:
                if (!attackDown) { attackDown = true; controller.attack(); }
                break;
            case KeyEvent.VK_K:
            case KeyEvent.VK_X:
                if (!ninjutsuDown) { ninjutsuDown = true; controller.ninjutsu(); }
                break;
            case KeyEvent.VK_P:
                if (!pauseDown) { pauseDown = true; controller.togglePause(); }
                break;
            case KeyEvent.VK_ENTER:
                controller.newGame();
                break;
            default:
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        switch (k) {
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                leftHeld = false; controller.setLeft(false); break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                rightHeld = false; controller.setRight(false); break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                crouchHeld = false; controller.setCrouch(false); break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                jumpDown = false; break;
            case KeyEvent.VK_L:
            case KeyEvent.VK_SHIFT:
                shiftDown = false; break;
            case KeyEvent.VK_J:
            case KeyEvent.VK_Z:
                attackDown = false; break;
            case KeyEvent.VK_K:
            case KeyEvent.VK_X:
                ninjutsuDown = false; break;
            case KeyEvent.VK_P:
                pauseDown = false; break;
            default:
                break;
        }
    }
}
