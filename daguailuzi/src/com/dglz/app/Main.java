package com.dglz.app;

import com.dglz.domain.MoveAdvisor;
import com.dglz.engine.GameEngine;
import com.dglz.ui.GameWindow;

import javax.swing.SwingUtilities;

/**
 * Task 3 — application entry point. This is the ONLY place in Task 3's tree
 * that references the concrete com.dglz.ai implementations by name; the UI
 * depends only on the domain.MoveAdvisor / domain.PlayerStrategy interfaces.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        long seed = 20260624L;
        String[] names = new String[] {
                "You", "Ling", "Bao", "Chen", "Dai", "Fang"
        };

        final GameEngine engine = new GameEngine(seed, names);
        engine.setStrategy(new com.dglz.ai.AiStrategy());
        engine.start();

        final MoveAdvisor advisor = new com.dglz.ai.CoachTranslator();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new GameWindow(engine, advisor).setVisible(true);
            }
        });
    }
}
