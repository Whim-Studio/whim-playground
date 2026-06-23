package klahklok;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Swing UI for the Klah Klok dice game. Holds the betting board, dice area,
 * overlays (bankrolls / round / turn / status) and the Roll button. All game
 * logic lives in Task 1 (domain) and Task 2 (engine/AI) classes — this frame
 * only renders state and drives the turn flow.
 */
public class KlahKlokFrame extends JFrame {

    private static final Color FELT = new Color(13, 92, 58);
    private static final Color FELT_DARK = new Color(9, 66, 42);
    private static final Color GOLD = new Color(214, 178, 92);
    private static final Color CREAM = new Color(244, 240, 226);

    private final GameState state;
    private final ResolutionEngine engine;
    private final AIController ai;
    private final Die[] dice;
    private final boolean vsComputer;
    private final Random animRandom = new Random();

    private int chipIncrement = 10;

    private JLabel roundLabel;
    private JLabel turnLabel;
    private JLabel statusLabel;
    private final List<JLabel> bankrollLabels = new ArrayList<JLabel>();
    private final Map<Symbol, BetCell> cells = new LinkedHashMap<Symbol, BetCell>();
    private final DiceBox[] diceBoxes = new DiceBox[3];
    private JButton rollButton;
    private Timer animTimer;

    public KlahKlokFrame(GameState state, ResolutionEngine engine, AIController ai,
                         Die[] dice, boolean vsComputer) {
        super("Klah Klok — Cambodian Dice");
        this.state = state;
        this.engine = engine;
        this.ai = ai;
        this.dice = dice;
        this.vsComputer = vsComputer;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(FELT_DARK);

        add(buildOverlay(), BorderLayout.NORTH);
        add(buildBoard(), BorderLayout.CENTER);
        add(buildSouth(), BorderLayout.SOUTH);

        refresh();
        pack();
        setMinimumSize(new Dimension(760, 640));
        setLocationRelativeTo(null);
    }

    // ----- overlay (round / turn / bankrolls / status) -----

    private JComponent buildOverlay() {
        JPanel panel = new JPanel(new BorderLayout(6, 4));
        panel.setBackground(FELT_DARK);
        panel.setBorder(new EmptyBorder(8, 12, 4, 12));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        roundLabel = makeLabel("Round 1", 18, GOLD);
        turnLabel = makeLabel("", 18, CREAM);
        top.add(roundLabel, BorderLayout.WEST);
        top.add(turnLabel, BorderLayout.EAST);

        JPanel banks = new JPanel(new GridLayout(1, 0, 10, 0));
        banks.setOpaque(false);
        for (int i = 0; i < state.getPlayers().size(); i++) {
            Player p = (Player) state.getPlayers().get(i);
            JLabel bl = makeLabel("", 16, CREAM);
            bl.setHorizontalAlignment(SwingConstants.CENTER);
            bl.setBorder(new CompoundBorder(new LineBorder(GOLD, 1, true),
                    new EmptyBorder(4, 8, 4, 8)));
            bl.setOpaque(true);
            bl.setBackground(FELT);
            bankrollLabels.add(bl);
            banks.add(bl);
        }

        statusLabel = makeLabel("Place your bets!", 16, Color.WHITE);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(new EmptyBorder(6, 0, 2, 0));

        JPanel stack = new JPanel(new GridLayout(0, 1, 0, 6));
        stack.setOpaque(false);
        stack.add(top);
        stack.add(banks);
        stack.add(statusLabel);
        panel.add(stack, BorderLayout.CENTER);
        return panel;
    }

    // ----- betting board (2x3 grid of symbols) -----

    private JComponent buildBoard() {
        JPanel board = new JPanel(new GridLayout(2, 3, 8, 8));
        board.setBackground(FELT);
        board.setBorder(new CompoundBorder(new EmptyBorder(4, 12, 4, 12),
                new LineBorder(GOLD, 2, true)));
        Symbol[] syms = Symbol.values();
        for (int i = 0; i < syms.length; i++) {
            BetCell cell = new BetCell(syms[i]);
            cells.put(syms[i], cell);
            board.add(cell);
        }
        return board;
    }

    // ----- south: dice area + chip selector + roll button -----

    private JComponent buildSouth() {
        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.setBackground(FELT_DARK);
        south.setBorder(new EmptyBorder(4, 12, 12, 12));

        JPanel diceRow = new JPanel(new GridLayout(1, 3, 12, 0));
        diceRow.setOpaque(false);
        for (int i = 0; i < 3; i++) {
            diceBoxes[i] = new DiceBox();
            diceRow.add(diceBoxes[i]);
        }
        south.add(diceRow, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        controls.setOpaque(false);
        controls.add(makeLabel("Chip:", 14, CREAM));

        ButtonGroup grp = new ButtonGroup();
        int[] chips = { 10, 50, 100 };
        for (int i = 0; i < chips.length; i++) {
            final int amt = chips[i];
            JToggleButton tb = new JToggleButton("$" + amt);
            tb.setFocusPainted(false);
            tb.setSelected(amt == chipIncrement);
            tb.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    chipIncrement = amt;
                }
            });
            grp.add(tb);
            controls.add(tb);
        }

        rollButton = new JButton("Roll Dice");
        rollButton.setFont(new Font("SansSerif", Font.BOLD, 18));
        rollButton.setBackground(GOLD);
        rollButton.setForeground(FELT_DARK);
        rollButton.setFocusPainted(false);
        rollButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onRollButton();
            }
        });
        controls.add(rollButton);

        south.add(controls, BorderLayout.SOUTH);
        return south;
    }

    // ----- turn flow -----

    private boolean moreHumansToBetAfterActive() {
        if (vsComputer) {
            return false; // only the single human bets manually
        }
        return state.getActiveIndex() < state.getPlayers().size() - 1;
    }

    private void onRollButton() {
        if (moreHumansToBetAfterActive()) {
            state.nextPlayer();
            refresh();
            return;
        }
        if (vsComputer) {
            for (int i = 0; i < state.getPlayers().size(); i++) {
                Player p = (Player) state.getPlayers().get(i);
                if (p.isComputer()) {
                    ai.placeBets(p);
                }
            }
        }
        doRoll();
    }

    private void doRoll() {
        rollButton.setEnabled(false);
        state.setPhase(GameState.Phase.ROLLING);
        statusLabel.setText("Rolling…");

        final Symbol[] roll = new Symbol[3];
        for (int i = 0; i < 3; i++) {
            roll[i] = dice[i].roll();
        }
        state.setLastRoll(roll);

        final int[] ticks = { 0 };
        final int totalTicks = 12;
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        animTimer = new Timer(70, null);
        animTimer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                ticks[0]++;
                if (ticks[0] >= totalTicks) {
                    animTimer.stop();
                    for (int i = 0; i < 3; i++) {
                        diceBoxes[i].setSymbol(roll[i]);
                    }
                    resolveAndFinish(roll);
                } else {
                    Symbol[] all = Symbol.values();
                    for (int i = 0; i < 3; i++) {
                        diceBoxes[i].setSymbol(all[animRandom.nextInt(all.length)]);
                    }
                }
            }
        });
        animTimer.start();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void resolveAndFinish(Symbol[] roll) {
        state.setPhase(GameState.Phase.RESOLUTION);
        Map results = engine.resolve(state.getPlayers(), roll);

        // Build a status line from per-player net deltas (engine has already
        // applied the bankroll changes).
        StringBuilder sb = new StringBuilder();
        sb.append("Rolled: ");
        for (int i = 0; i < 3; i++) {
            sb.append(roll[i].getDisplayName());
            if (i < 2) sb.append(", ");
        }
        sb.append(".  ");

        for (int i = 0; i < state.getPlayers().size(); i++) {
            Player p = (Player) state.getPlayers().get(i);
            int net = 0;
            Object pm = results == null ? null : results.get(p);
            if (pm instanceof Map) {
                Iterator it = ((Map) pm).values().iterator();
                while (it.hasNext()) {
                    Object v = it.next();
                    if (v instanceof Integer) {
                        net += ((Integer) v).intValue();
                    }
                }
            }
            if (net > 0) {
                sb.append(p.getName()).append(" won $").append(net).append(".  ");
            } else if (net < 0) {
                sb.append(p.getName()).append(" lost $").append(-net).append(".  ");
            } else {
                sb.append(p.getName()).append(" broke even.  ");
            }
        }
        statusLabel.setText(sb.toString().trim());

        updateBankrolls();

        // Game over if anyone is broke.
        Player broke = null;
        for (int i = 0; i < state.getPlayers().size(); i++) {
            Player p = (Player) state.getPlayers().get(i);
            if (p.getBankroll() <= 0) {
                broke = p;
                break;
            }
        }
        if (broke != null) {
            state.setPhase(GameState.Phase.GAME_OVER);
            turnLabel.setText("GAME OVER");
            JOptionPane.showMessageDialog(this,
                    broke.getName() + " is out of money. Game over!",
                    "Game Over", JOptionPane.INFORMATION_MESSAGE);
            rollButton.setEnabled(false);
            return;
        }

        // New betting round.
        for (int i = 0; i < state.getPlayers().size(); i++) {
            ((Player) state.getPlayers().get(i)).clearBets();
        }
        state.incrementRound();
        while (state.getActiveIndex() != 0) {
            state.nextPlayer();
        }
        state.setPhase(GameState.Phase.BETTING);
        rollButton.setEnabled(true);
        refresh();
    }

    // ----- refresh helpers -----

    private void refresh() {
        roundLabel.setText("Round " + state.getRoundNumber());
        Player active = state.getActivePlayer();
        if (vsComputer) {
            turnLabel.setText("Your turn — place bets");
        } else {
            turnLabel.setText(active.getName() + "'s turn");
        }
        if (rollButton != null) {
            rollButton.setText(moreHumansToBetAfterActive()
                    ? "Lock Bets → Next Player" : "Roll Dice");
        }
        updateBankrolls();
        for (Iterator<Map.Entry<Symbol, BetCell>> it = cells.entrySet().iterator(); it.hasNext();) {
            it.next().getValue().refresh();
        }
    }

    @SuppressWarnings("rawtypes")
    private void updateBankrolls() {
        for (int i = 0; i < state.getPlayers().size(); i++) {
            Player p = (Player) state.getPlayers().get(i);
            JLabel lbl = bankrollLabels.get(i);
            String tag = p.equals(state.getActivePlayer()) ? "▶ " : "   ";
            lbl.setText(tag + p.getName() + ": $" + p.getBankroll());
            lbl.setBackground(p.equals(state.getActivePlayer()) ? new Color(20, 120, 78) : FELT);
        }
    }

    private int currentBet(Symbol s) {
        Map bets = state.getActivePlayer().getBets();
        if (bets == null) return 0;
        Object v = bets.get(s);
        return (v instanceof Integer) ? ((Integer) v).intValue() : 0;
    }

    private void adjustBet(Symbol s, int delta) {
        if (state.getPhase() != GameState.Phase.BETTING) return;
        Player active = state.getActivePlayer();
        int cur = currentBet(s);
        int target = cur + delta;
        if (target < 0) target = 0;
        // total wagered must not exceed bankroll
        int projectedTotal = active.getTotalWagered() - cur + target;
        if (projectedTotal > active.getBankroll()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        active.placeBet(s, target);
        refresh();
    }

    private static JLabel makeLabel(String text, int size, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, size));
        l.setForeground(fg);
        return l;
    }

    // ----- inner: a single betting cell -----

    private class BetCell extends JPanel {
        private final Symbol symbol;
        private final JLabel betLabel;

        BetCell(Symbol symbol) {
            super(new BorderLayout(2, 2));
            this.symbol = symbol;
            setBackground(CREAM);
            setBorder(new LineBorder(FELT_DARK, 2, true));

            JLabel glyph = new JLabel(symbol.getGlyph(), SwingConstants.CENTER);
            glyph.setFont(new Font("SansSerif", Font.PLAIN, 46));
            add(glyph, BorderLayout.CENTER);

            JLabel name = new JLabel(symbol.getDisplayName(), SwingConstants.CENTER);
            name.setFont(new Font("SansSerif", Font.BOLD, 15));
            name.setForeground(FELT_DARK);
            add(name, BorderLayout.NORTH);

            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setOpaque(false);
            JButton minus = new JButton("−");
            JButton plus = new JButton("+");
            styleStepper(minus);
            styleStepper(plus);
            betLabel = new JLabel("$0", SwingConstants.CENTER);
            betLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            betLabel.setForeground(new Color(150, 30, 30));

            minus.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    adjustBet(BetCell.this.symbol, -chipIncrement);
                }
            });
            plus.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    adjustBet(BetCell.this.symbol, chipIncrement);
                }
            });

            bottom.add(minus, BorderLayout.WEST);
            bottom.add(betLabel, BorderLayout.CENTER);
            bottom.add(plus, BorderLayout.EAST);
            add(bottom, BorderLayout.SOUTH);
        }

        private void styleStepper(JButton b) {
            b.setFont(new Font("SansSerif", Font.BOLD, 16));
            b.setMargin(new Insets(2, 10, 2, 10));
            b.setFocusPainted(false);
        }

        void refresh() {
            betLabel.setText("$" + currentBet(symbol));
        }
    }

    // ----- inner: a single revealed die box -----

    private class DiceBox extends JPanel {
        private final JLabel glyph;
        private final JLabel name;

        DiceBox() {
            super(new BorderLayout());
            setBackground(Color.WHITE);
            setBorder(new LineBorder(GOLD, 3, true));
            setPreferredSize(new Dimension(120, 130));
            glyph = new JLabel("?", SwingConstants.CENTER);
            glyph.setFont(new Font("SansSerif", Font.PLAIN, 56));
            name = new JLabel(" ", SwingConstants.CENTER);
            name.setFont(new Font("SansSerif", Font.BOLD, 15));
            name.setForeground(FELT_DARK);
            add(glyph, BorderLayout.CENTER);
            add(name, BorderLayout.SOUTH);
        }

        void setSymbol(Symbol s) {
            glyph.setText(s.getGlyph());
            name.setText(s.getDisplayName());
        }
    }
}
