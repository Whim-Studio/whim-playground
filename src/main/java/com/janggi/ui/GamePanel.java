package com.janggi.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import com.janggi.ai.JanggiAI;
import com.janggi.ai.MinimaxAI;
import com.janggi.core.GameState;
import com.janggi.core.Move;
import com.janggi.core.Position;
import com.janggi.core.SetupChoice;
import com.janggi.core.Side;

/**
 * The running-game screen: the board, a status line, and game controls. All
 * game logic is delegated to {@link GameState}; the computer player (if any)
 * runs off the EDT via {@link SwingWorker}.
 */
public class GamePanel extends JPanel {

    private final JanggiFrame frame;
    private final GameConfig config;
    private final JanggiAI ai;

    private final BoardPanel boardPanel = new BoardPanel();
    private final JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton passButton = new JButton("Pass");
    private final JButton newGameButton = new JButton("New game (setup)");

    private GameState state;
    private Position selected;
    private boolean aiThinking;

    public GamePanel(JanggiFrame frame, GameConfig config, SetupChoice choSetup, SetupChoice hanSetup) {
        super(new BorderLayout(0, 8));
        this.frame = frame;
        this.config = config;
        this.ai = config.mode() == GameMode.VS_COMPUTER ? new MinimaxAI(config.aiDepth()) : null;
        this.state = GameState.initial(choSetup, hanSetup);

        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 17f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));

        boardPanel.setListener(new BoardPanel.IntersectionListener() {
            @Override
            public void onIntersectionClicked(Position p) {
                handleClick(p);
            }
        });

        passButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doHumanPass();
            }
        });
        newGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GamePanel.this.frame.showSetup(GamePanel.this.config);
            }
        });

        JPanel controls = new JPanel(new GridLayout(1, 0, 8, 0));
        controls.add(passButton);
        controls.add(newGameButton);

        add(statusLabel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);

        refreshBoard();
        updateStatus();
        maybeTriggerComputer();
    }

    // --- input ------------------------------------------------------------

    private void handleClick(Position clicked) {
        if (aiThinking || state.isGameOver()) {
            return;
        }
        Side toMove = state.sideToMove();
        if (!config.isHuman(toMove)) {
            return; // not a human's turn
        }

        if (selected != null) {
            Move move = new Move(selected, clicked);
            if (state.isLegal(move)) {
                applyMove(move);
                return;
            }
        }

        // (Re)select if the clicked square holds a movable piece of the side to move.
        if (board().pieceAt(clicked) != null
                && board().pieceAt(clicked).side() == toMove
                && !destinationsFrom(clicked).isEmpty()) {
            selected = clicked;
            boardPanel.setSelection(selected, destinationsFrom(clicked));
        } else {
            selected = null;
            boardPanel.clearSelection();
        }
    }

    private void doHumanPass() {
        if (aiThinking || state.isGameOver()) {
            return;
        }
        if (!config.isHuman(state.sideToMove())) {
            return;
        }
        Move pass = findPassMove();
        if (pass != null) {
            applyMove(pass);
        }
    }

    private Move findPassMove() {
        List<Move> moves = state.legalMoves();
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i).isPass()) {
                return moves.get(i);
            }
        }
        return null;
    }

    private List<Position> destinationsFrom(Position from) {
        List<Position> dests = new ArrayList<Position>();
        List<Move> moves = state.legalMoves();
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            if (!m.isPass() && m.from().equals(from)) {
                dests.add(m.to());
            }
        }
        return dests;
    }

    // --- applying moves ---------------------------------------------------

    private void applyMove(Move move) {
        state = state.apply(move);
        selected = null;
        boardPanel.clearSelection();
        boardPanel.setLastMove(move.from(), move.to());
        refreshBoard();
        updateStatus();

        if (state.isGameOver()) {
            announceResult();
        } else {
            maybeTriggerComputer();
        }
    }

    private void maybeTriggerComputer() {
        if (ai == null || state.isGameOver()) {
            return;
        }
        if (config.isHuman(state.sideToMove())) {
            return;
        }
        startAiMove();
    }

    private void startAiMove() {
        aiThinking = true;
        setControlsEnabled(false);
        boardPanel.setInteractive(false);
        updateStatus();

        final GameState snapshot = state;
        new SwingWorker<Move, Void>() {
            @Override
            protected Move doInBackground() {
                return ai.chooseMove(snapshot);
            }

            @Override
            protected void done() {
                aiThinking = false;
                setControlsEnabled(true);
                boardPanel.setInteractive(true);
                Move chosen;
                try {
                    chosen = get();
                } catch (Exception ex) {
                    chosen = null;
                }
                if (chosen == null || !state.isLegal(chosen)) {
                    // Defensive: fall back to a pass if the AI returned nothing usable.
                    chosen = findPassMove();
                }
                if (chosen != null) {
                    applyMove(chosen);
                } else {
                    updateStatus();
                }
            }
        }.execute();
    }

    // --- view -------------------------------------------------------------

    private com.janggi.core.Board board() {
        return state.board();
    }

    private void refreshBoard() {
        boardPanel.setBoard(board());
        Side toMove = state.sideToMove();
        if (!state.isGameOver() && state.isInCheck(toMove)) {
            boardPanel.setCheckedGeneral(board().findGeneral(toMove));
        } else {
            boardPanel.setCheckedGeneral(null);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        passButton.setEnabled(enabled);
    }

    private void updateStatus() {
        if (state.isGameOver()) {
            Side w = state.winner();
            statusLabel.setForeground(Color.BLACK);
            statusLabel.setText(w == null ? "Game over." : (sideName(w) + " wins — checkmate!"));
            return;
        }
        Side toMove = state.sideToMove();
        StringBuilder sb = new StringBuilder();
        sb.append(sideName(toMove)).append(" to move");
        if (aiThinking && config.isComputer(toMove)) {
            sb.append(" — computer is thinking…");
        } else if (config.isComputer(toMove)) {
            sb.append(" (computer)");
        }
        if (state.isInCheck(toMove)) {
            sb.append("  ·  CHECK!");
            statusLabel.setForeground(new Color(0xC62828));
        } else {
            statusLabel.setForeground(toMove == Side.CHO ? new Color(0x0B6E3B) : new Color(0xB22222));
        }
        statusLabel.setText(sb.toString());
    }

    private void announceResult() {
        Side w = state.winner();
        String msg = (w == null) ? "Game over." : (sideName(w) + " wins by checkmate!");
        JOptionPane.showMessageDialog(this, msg, "Game over", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String sideName(Side side) {
        return side == Side.CHO ? "CHO (초, green)" : "HAN (한, red)";
    }
}
