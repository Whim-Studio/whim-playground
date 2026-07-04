package com.whim.swd6.app;

import com.whim.swd6.api.CombatTracker;
import com.whim.swd6.api.ContentProvider;
import com.whim.swd6.api.RpgEngine;
import com.whim.swd6.api.CharacterRepository;
import com.whim.swd6.engine.D6Engine;
import com.whim.swd6.engine.Encounter;
import com.whim.swd6.persistence.JsonCharacterRepository;
import com.whim.swd6.rules.GameContent;
import com.whim.swd6.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.util.function.Supplier;

/**
 * Application entry point. Wires the real implementations of the three service
 * seams — {@link GameContent} (rules content), {@link D6Engine} (rules engine),
 * {@link JsonCharacterRepository} (persistence) — into the Swing {@link MainFrame}.
 *
 * The UI is written entirely against the api interfaces, so this class is the only
 * place the concrete layers meet.
 *
 * Owned by the orchestrator (app).
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Star Wars D6 Digital Tabletop requires a graphical display.");
            System.out.println("Run this on a desktop environment (or via X forwarding).");
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
            // fall back to the cross-platform look and feel
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final ContentProvider content = new GameContent();
                final RpgEngine engine = new D6Engine();
                final CharacterRepository repository = new JsonCharacterRepository();
                // Each encounter gets a fresh tracker bound to the shared engine.
                Supplier<CombatTracker> trackerSupplier = new Supplier<CombatTracker>() {
                    @Override
                    public CombatTracker get() {
                        return new Encounter(engine);
                    }
                };

                MainFrame frame = new MainFrame(content, engine, repository, trackerSupplier);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
