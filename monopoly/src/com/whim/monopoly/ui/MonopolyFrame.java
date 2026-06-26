package com.whim.monopoly.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.whim.monopoly.domain.Player;
import com.whim.monopoly.engine.GameEngine;
import com.whim.monopoly.engine.GameListener;
import com.whim.monopoly.engine.GameState;
import com.whim.monopoly.engine.TurnPhase;

/**
 * Top-level window. Wires a {@link BoardPanel} (center), {@link PropertyPanel}
 * (east), {@link ControlPanel} + scrolling log (south), and pumps a modal
 * {@link AuctionDialog} during the AUCTION phase.
 *
 * <p>All state mutation goes through {@link GameEngine}. Engine calls run on a
 * dedicated single-thread executor (off the EDT); the engine fires
 * {@link GameListener} callbacks synchronously on that thread, which this frame
 * marshals back onto the EDT for rendering.</p>
 */
public class MonopolyFrame extends JFrame implements GameListener {

    private final GameEngine engine;
    private final BoardPanel boardPanel;
    private final ControlPanel controlPanel;
    private final PropertyPanel propertyPanel;
    private final JTextArea logArea = new JTextArea(8, 40);

    private final ExecutorService engineExec = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "monopoly-engine");
            t.setDaemon(true);
            return t;
        }
    });

    private AuctionDialog auctionDialog;
    private boolean gameOverShown;

    public MonopolyFrame(GameEngine engine) {
        super("Monopoly");
        this.engine = engine;
        this.boardPanel = new BoardPanel(engine);
        this.controlPanel = new ControlPanel(this, engine);
        this.propertyPanel = new PropertyPanel(this, engine);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel boardWrap = new JPanel(new BorderLayout());
        boardWrap.setBackground(BoardColors.BOARD_BG);
        boardWrap.add(boardPanel, BorderLayout.CENTER);
        add(boardWrap, BorderLayout.CENTER);

        add(propertyPanel, BorderLayout.EAST);

        add(buildSouth(), BorderLayout.SOUTH);

        engine.addListener(this);
        refreshUI();

        setMinimumSize(new Dimension(1040, 820));
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildSouth() {
        JPanel south = new JPanel(new BorderLayout(8, 0));
        south.add(controlPanel, BorderLayout.WEST);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Game Log"));
        logScroll.setPreferredSize(new Dimension(560, 170));
        south.add(logScroll, BorderLayout.CENTER);
        return south;
    }

    // --- engine action plumbing -------------------------------------------

    /**
     * Run an engine-mutating action off the EDT. Listener callbacks triggered by
     * the action are marshaled back to the EDT by this frame.
     */
    void submitEngineAction(final Runnable action) {
        engineExec.submit(new Runnable() {
            public void run() {
                try {
                    action.run();
                } catch (final RuntimeException ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            JOptionPane.showMessageDialog(MonopolyFrame.this,
                                    String.valueOf(ex.getMessage()),
                                    "Action rejected", JOptionPane.WARNING_MESSAGE);
                        }
                    });
                }
            }
        });
    }

    void openTradeDialog() {
        GameState s = engine.getState();
        if (s.getActivePlayers().size() < 2) {
            return;
        }
        new TradeDialog(this, engine).setVisible(true);
    }

    // --- GameListener (callbacks arrive off the EDT) ----------------------

    public void onLog(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                logArea.append(message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    public void onStateChanged() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                refreshUI();
            }
        });
    }

    public void onGameOver(final Player winner) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                refreshUI();
                announceWinner(winner);
            }
        });
    }

    // --- rendering (EDT only) ---------------------------------------------

    private void refreshUI() {
        GameState s = engine.getState();
        boardPanel.repaint();
        controlPanel.refresh();
        propertyPanel.refresh();
        manageAuction(s);
        if (s.getPhase() == TurnPhase.GAME_OVER) {
            announceWinner(s.getWinner());
        }
    }

    private void manageAuction(GameState s) {
        if (s.getPhase() == TurnPhase.AUCTION) {
            if (auctionDialog == null) {
                auctionDialog = new AuctionDialog(this, engine);
                // Modal: blocks here but the EDT keeps pumping events, so later
                // onStateChanged refreshes still run and can dispose this dialog.
                auctionDialog.setVisible(true);
            } else {
                auctionDialog.refresh();
            }
        } else if (auctionDialog != null) {
            auctionDialog.dispose();
            auctionDialog = null;
        }
    }

    private void announceWinner(Player winner) {
        if (gameOverShown) {
            return;
        }
        gameOverShown = true;
        String msg = winner != null
                ? winner.getName() + " wins the game!"
                : "Game over.";
        JLabel label = new JLabel(msg);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        if (winner != null && winner.getToken() != null) {
            label.setForeground(winner.getToken().darker());
        } else {
            label.setForeground(Color.DARK_GRAY);
        }
        JOptionPane.showMessageDialog(this, label, "Monopoly", JOptionPane.INFORMATION_MESSAGE);
    }
}
