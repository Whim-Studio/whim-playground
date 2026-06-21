package com.xiangqi.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import com.xiangqi.ai.Coach;
import com.xiangqi.ai.MinimaxAI;
import com.xiangqi.ai.MoveAdvice;
import com.xiangqi.ai.XiangqiCoach;
import com.xiangqi.core.GameState;
import com.xiangqi.core.Move;
import com.xiangqi.core.Piece;
import com.xiangqi.core.Position;
import com.xiangqi.core.Side;

/**
 * The single resizable game window. Owns the {@link GameState}, drives turns,
 * spawns the computer move off the EDT, and powers Coach mode. The board itself
 * is drawn by {@link BoardPanel}; this class is the controller.
 */
final class XiangqiFrame extends JFrame {

    private static final int AI_DEPTH = 3;
    private static final int COACH_DEPTH = 3;
    private static final int COACH_TOP_K = 3;

    private GameState state;
    private final GameMode mode;
    private final Side humanSide; // only meaningful in VS_COMPUTER

    private final BoardPanel boardPanel;
    private final JLabel statusLabel;
    private final JCheckBox coachToggle;
    private final JTextArea coachText;
    private final JButton newGameButton;

    private Position selected;
    private List<Move> legalForSelected = new ArrayList<Move>();
    private boolean aiThinking;

    XiangqiFrame(GameMode mode, Side humanSide) {
        super("Xiangqi — Chinese Chess");
        this.mode = mode;
        this.humanSide = humanSide;
        this.state = GameState.initial();

        this.boardPanel = new BoardPanel(state, new java.util.function.Consumer<Position>() {
            @Override
            public void accept(Position p) {
                onBoardClick(p);
            }
        });
        this.statusLabel = new JLabel();
        this.coachToggle = new JCheckBox("Coach mode");
        this.coachText = new JTextArea();
        this.newGameButton = new JButton("New Game");

        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);

        setMinimumSize(new Dimension(820, 660));
        setPreferredSize(new Dimension(900, 720));

        refreshStatus();
        maybeTriggerAi();
    }

    private JPanel buildSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        side.setPreferredSize(new Dimension(280, 0));

        JLabel title = new JLabel("Xiangqi");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(title);

        JLabel sub = new JLabel(modeDescription());
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(sub);
        side.add(Box.createVerticalStrut(14));

        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 15f));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(statusLabel);
        side.add(Box.createVerticalStrut(14));

        coachToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        coachToggle.setEnabled(mode == GameMode.VS_COMPUTER);
        coachToggle.setToolTipText(mode == GameMode.VS_COMPUTER
                ? "Highlight the strongest move and explain why, on your turn."
                : "Coach mode is available in Player vs Computer games.");
        coachToggle.addActionListener(e -> onCoachToggled());
        side.add(coachToggle);
        side.add(Box.createVerticalStrut(8));

        JLabel coachHeader = new JLabel("Coach says:");
        coachHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(coachHeader);

        coachText.setEditable(false);
        coachText.setLineWrap(true);
        coachText.setWrapStyleWord(true);
        coachText.setFont(coachText.getFont().deriveFont(13f));
        coachText.setText("Turn on Coach mode for move suggestions.");
        JScrollPane scroll = new JScrollPane(coachText);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setPreferredSize(new Dimension(256, 180));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        side.add(scroll);

        side.add(Box.createVerticalStrut(14));
        side.add(buildLegend());

        side.add(Box.createVerticalGlue());
        newGameButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        newGameButton.addActionListener(e -> onNewGame());
        side.add(newGameButton);

        return side;
    }

    private JPanel buildLegend() {
        JPanel legend = new JPanel(new GridLayout(0, 1, 0, 2));
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        legend.setBorder(BorderFactory.createTitledBorder("Legend"));
        legend.add(swatch(new Color(0x2E86DE), "Selected piece"));
        legend.add(swatch(new Color(0x27AE60), "Legal move"));
        legend.add(swatch(new Color(0x8E44AD), "Coach suggestion"));
        legend.add(swatch(new Color(0xE67E22), "Last move"));
        legend.setMaximumSize(new Dimension(Integer.MAX_VALUE, legend.getPreferredSize().height));
        return legend;
    }

    private JLabel swatch(Color c, String text) {
        JLabel l = new JLabel("●  " + text);
        l.setForeground(c);
        return l;
    }

    private String modeDescription() {
        if (mode == GameMode.TWO_PLAYER) {
            return "Two players, local";
        }
        return "You are " + (humanSide == Side.RED ? "RED (bottom)" : "BLACK (top)");
    }

    // ----- interaction -------------------------------------------------------

    private void onBoardClick(Position p) {
        if (aiThinking || state.isGameOver()) {
            return;
        }
        if (mode == GameMode.VS_COMPUTER && state.sideToMove() != humanSide) {
            return; // not the human's turn
        }

        Piece clicked = state.board().pieceAt(p);

        // Selecting / re-selecting one of your own pieces.
        if (clicked != null && clicked.side() == state.sideToMove()) {
            select(p);
            return;
        }

        // Attempting a move to a highlighted target.
        if (selected != null) {
            Move move = findMove(selected, p);
            if (move != null && state.isLegal(move)) {
                applyMove(move);
            } else {
                // Illegal click: clear selection gracefully, no error popup.
                clearSelection();
            }
            return;
        }

        clearSelection();
    }

    private void select(Position p) {
        selected = p;
        legalForSelected = new ArrayList<Move>();
        Set<Position> targets = new HashSet<Position>();
        for (Move m : state.legalMoves()) {
            if (m.from().equals(p)) {
                legalForSelected.add(m);
                targets.add(m.to());
            }
        }
        boardPanel.setSelection(p, targets);
    }

    private void clearSelection() {
        selected = null;
        legalForSelected = new ArrayList<Move>();
        boardPanel.setSelection(null, null);
    }

    private Move findMove(Position from, Position to) {
        for (Move m : legalForSelected) {
            if (m.to().equals(to)) {
                return m;
            }
        }
        return null;
    }

    private void applyMove(Move move) {
        state = state.apply(move);
        clearSelection();
        boardPanel.setCoachTargets(null);
        boardPanel.setLastMove(move.from(), move.to());
        boardPanel.setState(state);
        refreshStatus();
        if (!state.isGameOver()) {
            maybeTriggerAi();
            maybeRunCoach();
        } else {
            coachText.setText("Game over.");
        }
    }

    // ----- computer player ---------------------------------------------------

    private void maybeTriggerAi() {
        if (mode != GameMode.VS_COMPUTER || state.isGameOver()) {
            return;
        }
        if (state.sideToMove() == humanSide) {
            return;
        }
        aiThinking = true;
        refreshStatus();
        final GameState snapshot = state;
        new SwingWorker<Move, Void>() {
            @Override
            protected Move doInBackground() {
                return new MinimaxAI(AI_DEPTH).chooseMove(snapshot);
            }

            @Override
            protected void done() {
                aiThinking = false;
                Move chosen = null;
                try {
                    chosen = get();
                } catch (Exception ex) {
                    chosen = null;
                }
                // Guard against a stale worker (e.g. New Game pressed mid-think).
                if (snapshot == state && chosen != null && state.isLegal(chosen)) {
                    applyMove(chosen);
                } else {
                    refreshStatus();
                }
            }
        }.execute();
    }

    // ----- coach mode --------------------------------------------------------

    private void onCoachToggled() {
        if (coachToggle.isSelected()) {
            maybeRunCoach();
        } else {
            boardPanel.setCoachTargets(null);
            coachText.setText("Coach mode off.");
        }
    }

    private void maybeRunCoach() {
        if (!coachToggle.isSelected() || mode != GameMode.VS_COMPUTER) {
            return;
        }
        if (state.isGameOver() || state.sideToMove() != humanSide || aiThinking) {
            boardPanel.setCoachTargets(null);
            return;
        }
        coachText.setText("Thinking about your best moves...");
        final GameState snapshot = state;
        new SwingWorker<List<MoveAdvice>, Void>() {
            @Override
            protected List<MoveAdvice> doInBackground() {
                Coach coach = new XiangqiCoach(COACH_DEPTH);
                return coach.topMoves(snapshot, COACH_TOP_K);
            }

            @Override
            protected void done() {
                if (snapshot != state) {
                    return; // position moved on; drop stale advice
                }
                List<MoveAdvice> advice;
                try {
                    advice = get();
                } catch (Exception ex) {
                    coachText.setText("Coach unavailable: " + ex.getMessage());
                    return;
                }
                showCoachAdvice(advice);
            }
        }.execute();
    }

    private void showCoachAdvice(List<MoveAdvice> advice) {
        if (advice == null || advice.isEmpty()) {
            boardPanel.setCoachTargets(null);
            coachText.setText("No suggestions available in this position.");
            return;
        }
        Set<Position> targets = new HashSet<Position>();
        for (MoveAdvice a : advice) {
            targets.add(a.move().to());
        }
        boardPanel.setCoachTargets(targets);

        MoveAdvice best = advice.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("Best: ").append(describe(best.move()))
                .append("  (score ").append(best.score()).append(")\n\n");
        sb.append(best.explanation()).append("\n");
        if (advice.size() > 1) {
            sb.append("\nOther ideas:\n");
            for (int i = 1; i < advice.size(); i++) {
                MoveAdvice a = advice.get(i);
                sb.append("  ").append(i + 1).append(". ").append(describe(a.move()))
                        .append(" — ").append(a.explanation()).append("\n");
            }
        }
        coachText.setText(sb.toString());
        coachText.setCaretPosition(0);
    }

    private String describe(Move m) {
        Piece p = state.board().pieceAt(m.from());
        String name = p == null ? "Piece" : pieceName(p);
        return name + " " + coord(m.from()) + "→" + coord(m.to());
    }

    private static String pieceName(Piece p) {
        switch (p.type()) {
            case GENERAL: return "General";
            case ADVISOR: return "Advisor";
            case ELEPHANT: return "Elephant";
            case HORSE: return "Horse";
            case CHARIOT: return "Chariot";
            case CANNON: return "Cannon";
            case SOLDIER: return "Soldier";
            default: return "Piece";
        }
    }

    private static String coord(Position p) {
        return "(" + p.row() + "," + p.col() + ")";
    }

    // ----- status & lifecycle ------------------------------------------------

    private void refreshStatus() {
        String text;
        if (state.isGameOver()) {
            Side w = state.winner();
            if (w == null) {
                text = "Game over — draw.";
            } else {
                text = w + " wins!";
            }
        } else if (aiThinking) {
            text = "Computer is thinking…";
        } else {
            Side stm = state.sideToMove();
            StringBuilder sb = new StringBuilder();
            sb.append(stm).append(" to move");
            if (mode == GameMode.VS_COMPUTER) {
                sb.append(stm == humanSide ? " (you)" : " (computer)");
            }
            if (state.isInCheck(stm)) {
                sb.append(" — CHECK!");
            }
            text = sb.toString();
        }
        statusLabel.setText(text);
        boardPanel.repaint();
    }

    private void onNewGame() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Start a new game with the same settings?", "New Game",
                JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        state = GameState.initial();
        clearSelection();
        boardPanel.setCoachTargets(null);
        boardPanel.setLastMove(null, null);
        boardPanel.setState(state);
        coachText.setText(coachToggle.isSelected()
                ? "Coach mode on." : "Turn on Coach mode for move suggestions.");
        refreshStatus();
        maybeTriggerAi();
        maybeRunCoach();
    }
}
