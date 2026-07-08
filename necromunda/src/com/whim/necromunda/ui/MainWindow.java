package com.whim.necromunda.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import com.whim.necromunda.engine.GameState;
import com.whim.necromunda.engine.TurnManager;
import com.whim.necromunda.model.Gang;

/**
 * The main application window: the board in the centre, the turn/phase controls
 * and action log on the right (Milestone 4), and a Gangs menu that opens the
 * roster editor (Milestone 3). Subscribes to {@link GameState} changes so every
 * panel refreshes whenever the {@link TurnManager} advances play.
 */
public final class MainWindow extends JFrame {

    private final GameState state;
    private final TurnManager turns;

    private final BoardPanel boardPanel;
    private final PhaseControlPanel phasePanel;
    private final LogPanel logPanel;

    public MainWindow(GameState state, TurnManager turns) {
        super("Necromunda — Underhive Skirmish");
        this.state = state;
        this.turns = turns;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        boardPanel = new BoardPanel(state);
        phasePanel = new PhaseControlPanel(state, turns);
        logPanel = new LogPanel(state);

        setLayout(new BorderLayout(6, 6));
        add(boardPanel, BorderLayout.CENTER);

        JPanel east = new JPanel(new BorderLayout(6, 6));
        east.setPreferredSize(new Dimension(370, 640));
        east.add(phasePanel, BorderLayout.NORTH);
        east.add(logPanel, BorderLayout.CENTER);
        add(east, BorderLayout.EAST);

        setJMenuBar(buildMenuBar());

        // Refresh all views on any engine change.
        state.addChangeListener(new Runnable() {
            @Override
            public void run() {
                boardPanel.repaint();
                phasePanel.refresh();
                logPanel.refresh();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu gangs = new JMenu("Gangs");
        for (final Gang g : state.gangs()) {
            JMenuItem edit = new JMenuItem("Edit roster — " + g.name());
            edit.addActionListener(e -> {
                RosterEditorFrame f = new RosterEditorFrame(g);
                f.setVisible(true);
            });
            gangs.add(edit);
        }
        bar.add(gangs);

        JMenu battle = new JMenu("Battle");
        JMenuItem next = new JMenuItem("Next Phase");
        next.addActionListener(e -> turns.advancePhase());
        JMenuItem end = new JMenuItem("End Turn");
        end.addActionListener(e -> turns.endTurn());
        battle.add(next);
        battle.add(end);
        bar.add(battle);

        return bar;
    }
}
