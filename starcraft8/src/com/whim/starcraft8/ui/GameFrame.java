package com.whim.starcraft8.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import com.whim.starcraft8.engine.Simulation;

/**
 * Application window for 8-Bit StarCraft. Constructed with a {@link Simulation}; owns a
 * Swing {@link Timer} (~33 fps) that snapshots render data inside
 * {@code simulation.readState(...)} and repaints on the EDT. It NEVER mutates domain
 * state inside the callback — it only copies primitives into {@link RenderState}.
 */
public final class GameFrame extends JFrame {

    private static final int FPS_MS = 30; // ~33 fps render tick (engine ticks at 60/s)
    private static final int PAN_STEP = 64;

    private final Simulation sim;
    private final UiContext ctx;
    private final GamePanel panel;
    private final Hud hud;
    private final Timer timer;

    public GameFrame(Simulation sim) {
        super("8-Bit StarCraft");
        this.sim = sim;
        this.ctx = new UiContext(sim, sim.humanPlayerId());
        this.panel = new GamePanel(ctx);
        this.hud = new Hud(ctx, panel);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(960, 600));
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(hud, BorderLayout.SOUTH);

        installKeyBindings();
        pack();
        setLocationRelativeTo(null);

        this.timer = new Timer(FPS_MS, new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tick();
            }
        });
        this.timer.start();
    }

    /** Snapshot world state under the engine lock, then repaint. */
    private void tick() {
        if (sim.isRunning()) {
            sim.readState(new java.util.function.Consumer<com.whim.starcraft8.engine.WorldReader>() {
                public void accept(com.whim.starcraft8.engine.WorldReader reader) {
                    // ONLY read + copy primitives here. Never mutate domain.
                    ctx.render = RenderState.snapshot(reader, ctx.humanId);
                }
            });
        }
        pruneSelection();
        panel.repaint();
        hud.repaint();
    }

    /** Drop ids that no longer exist (died) so selection stays valid for the UI. */
    private void pruneSelection() {
        RenderState rs = ctx.render;
        if (rs == null) return;
        java.util.Iterator<Long> it = ctx.selectedUnits.iterator();
        while (it.hasNext()) {
            long id = it.next().longValue();
            boolean found = false;
            for (int i = 0; i < rs.units.size(); i++) {
                if (rs.units.get(i).id == id) { found = true; break; }
            }
            if (!found) it.remove();
        }
        if (ctx.selectedUnits.isEmpty() && ctx.selectedBuilding < 0) {
            hud.resetMenus();
        }
        if (ctx.selectedBuilding >= 0) {
            boolean found = false;
            for (int i = 0; i < rs.buildings.size(); i++) {
                if (rs.buildings.get(i).id == ctx.selectedBuilding) { found = true; break; }
            }
            if (!found) ctx.selectedBuilding = -1;
        }
    }

    private void installKeyBindings() {
        InputMap im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = panel.getActionMap();

        bind(im, am, "B", KeyStroke.getKeyStroke('B'), new AbstractAction() {
            public void actionPerformed(ActionEvent e) { hud.openBuildMenu(); }
        });
        bind(im, am, "A", KeyStroke.getKeyStroke('A'), new AbstractAction() {
            public void actionPerformed(ActionEvent e) { panel.hotkeyAttackMove(); }
        });
        bind(im, am, "S", KeyStroke.getKeyStroke('S'), new AbstractAction() {
            public void actionPerformed(ActionEvent e) { panel.hotkeyStop(); }
        });
        bind(im, am, "ESC", KeyStroke.getKeyStroke("ESCAPE"), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ctx.cancelMode();
                hud.resetMenus();
                ctx.clearSelection();
                panel.repaint();
            }
        });

        // camera panning
        bind(im, am, "panL", KeyStroke.getKeyStroke("LEFT"), panAction(-PAN_STEP, 0));
        bind(im, am, "panR", KeyStroke.getKeyStroke("RIGHT"), panAction(PAN_STEP, 0));
        bind(im, am, "panU", KeyStroke.getKeyStroke("UP"), panAction(0, -PAN_STEP));
        bind(im, am, "panD", KeyStroke.getKeyStroke("DOWN"), panAction(0, PAN_STEP));
    }

    private AbstractAction panAction(final int dx, final int dy) {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ctx.camera.pan(dx, dy);
                panel.repaint();
            }
        };
    }

    private static void bind(InputMap im, ActionMap am, String name, KeyStroke ks,
                             AbstractAction action) {
        im.put(ks, name);
        am.put(name, action);
    }

    /** Stop the render timer (engine lifecycle is managed by the caller). */
    public void dispose() {
        if (timer != null) timer.stop();
        super.dispose();
    }
}
