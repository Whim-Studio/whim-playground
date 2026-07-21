package com.railroad.ui;

import com.railroad.logic.GameClock;
import com.railroad.logic.MapGenerator;
import com.railroad.model.Company;
import com.railroad.model.GameState;
import com.railroad.model.World;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

/**
 * Top-level window. Wires the generated world, company, clock and controller to
 * the {@link MapPanel} (inside a scroll pane) and the {@link HudPanel}. The clock
 * tick listener repaints the map and refreshes the HUD each frame.
 */
public final class GameFrame extends JFrame {

    private static final int MAP_WIDTH = 40;
    private static final int MAP_HEIGHT = 30;

    public GameFrame(long seed) {
        super("Railroad Tycoon (Phase 1) — seed " + seed);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        World world = new MapGenerator(seed, MAP_WIDTH, MAP_HEIGHT).generate();
        Company company = new Company("Player Rail Co.", GameState.STARTING_CASH);
        GameState state = new GameState(world, company);
        GameClock clock = new GameClock(state);
        GameController controller = new GameController(state, clock);

        // The map's build-action callback refreshes the HUD; we route it through a
        // holder because the HUD is created just after the map.
        final HudPanel[] hudHolder = new HudPanel[1];
        MapPanel wiredMap = new MapPanel(controller, new Runnable() {
            @Override
            public void run() {
                if (hudHolder[0] != null) {
                    hudHolder[0].refresh();
                }
            }
        });

        HudPanel hud = new HudPanel(controller, new Runnable() {
            @Override
            public void run() {
                wiredMap.repaint();
            }
        });
        hudHolder[0] = hud;

        clock.setTickListener(new GameClock.TickListener() {
            @Override
            public void onTick() {
                wiredMap.repaint();
                hud.refresh();
            }
        });

        JScrollPane scroll = new JScrollPane(wiredMap);
        scroll.getVerticalScrollBar().setUnitIncrement(22);
        scroll.getHorizontalScrollBar().setUnitIncrement(22);

        setLayout(new BorderLayout());
        add(hud, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        setSize(1000, 760);
        setLocationRelativeTo(null);
    }
}
