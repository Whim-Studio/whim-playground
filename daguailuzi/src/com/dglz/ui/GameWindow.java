package com.dglz.ui;

import com.dglz.domain.Card;
import com.dglz.domain.Combination;
import com.dglz.domain.MoveAdvisor;
import com.dglz.domain.MoveSuggestion;
import com.dglz.domain.Play;
import com.dglz.domain.Player;
import com.dglz.domain.Road;
import com.dglz.domain.Team;
import com.dglz.engine.GameEngine;
import com.dglz.engine.GameState;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Task 3 — main Swing view for Da Guai Lu Zi.
 *
 * 6-seat round table: the human hand sits at the BOTTOM as clickable card
 * components, the five AI seats are arranged around the table, and a center
 * overlay shows the current trick. AI turns are driven by a javax.swing.Timer
 * that repeatedly calls engine.stepAI() until it is the human's turn or the
 * game is over. All engine interaction stays on the EDT.
 */
public class GameWindow extends JFrame {

    // ----- team / table colors -------------------------------------------------
    private static final Color TABLE_GREEN = new Color(0x0b6e3b);
    private static final Color TEAM_A_COLOR = new Color(0x2f6fdb); // human's team
    private static final Color TEAM_B_COLOR = new Color(0xd24b4b);
    private static final Color SELECT_BORDER = new Color(0x1565c0);
    private static final Color COACH_BORDER = new Color(0xffb300);
    private static final Color WINNING_MARK = new Color(0xffd54f);

    private static final int AI_TICK_MS = 750;

    private final GameEngine engine;
    private final MoveAdvisor advisor;

    // hand
    private final JPanel handPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 6));
    private final List<CardComponent> cardComponents = new ArrayList<CardComponent>();

    // ai seats keyed by seat index (1..5)
    private final SeatPanel[] seatPanels = new SeatPanel[6];

    // center trick overlay
    private final JPanel trickPanel = new JPanel();
    private final JLabel roadLabel = new JLabel("", SwingConstants.CENTER);

    // controls / messages
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JToggleButton coachToggle = new JToggleButton("Coach: OFF");

    // coach side panel
    private final JPanel coachPanel = new JPanel(new BorderLayout());
    private final JTextArea coachText = new JTextArea();

    // ai turn driver
    private final Timer aiTimer;

    public GameWindow(GameEngine engine, MoveAdvisor advisor) {
        super("大怪路子 — Da Guai Lu Zi");
        this.engine = engine;
        this.advisor = advisor;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        add(buildTopSeat(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildSouth(), BorderLayout.SOUTH);
        add(buildCoachPanel(), BorderLayout.EAST);

        this.aiTimer = new Timer(AI_TICK_MS, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onAiTick();
            }
        });
        this.aiTimer.setRepeats(true);

        refreshAll();
        // If the game opens on an AI lead, let them play.
        maybeStartAiDriver();

        setMinimumSize(new Dimension(1000, 720));
        pack();
        setLocationRelativeTo(null);
    }

    // --------------------------------------------------------------------------
    // layout construction
    // --------------------------------------------------------------------------

    private JComponent buildTopSeat() {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        wrap.setOpaque(false);
        SeatPanel sp = new SeatPanel(3);
        seatPanels[3] = sp;
        wrap.add(sp);
        return wrap;
    }

    private JComponent buildTable() {
        JPanel table = new JPanel(new BorderLayout(10, 10));
        table.setBackground(TABLE_GREEN);
        table.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // left column: seats 4 (upper) and 5 (lower)
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        seatPanels[4] = new SeatPanel(4);
        seatPanels[5] = new SeatPanel(5);
        left.add(seatPanels[4]);
        left.add(Box.createVerticalStrut(40));
        left.add(seatPanels[5]);

        // right column: seats 2 (upper) and 1 (lower)
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        seatPanels[2] = new SeatPanel(2);
        seatPanels[1] = new SeatPanel(1);
        right.add(seatPanels[2]);
        right.add(Box.createVerticalStrut(40));
        right.add(seatPanels[1]);

        // center overlay: current trick
        trickPanel.setOpaque(true);
        trickPanel.setBackground(new Color(0x0a5530));
        trickPanel.setBorder(BorderFactory.createLineBorder(new Color(0xffffff), 1, true));
        trickPanel.setLayout(new BorderLayout(4, 4));
        roadLabel.setForeground(Color.WHITE);
        roadLabel.setFont(roadLabel.getFont().deriveFont(Font.BOLD, 16f));
        roadLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        trickPanel.add(roadLabel, BorderLayout.NORTH);

        table.add(left, BorderLayout.WEST);
        table.add(right, BorderLayout.EAST);
        table.add(trickPanel, BorderLayout.CENTER);
        return table;
    }

    private JComponent buildSouth() {
        JPanel south = new JPanel(new BorderLayout(6, 6));

        // human seat header
        seatPanels[0] = new SeatPanel(0);
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        header.add(seatPanels[0]);
        south.add(header, BorderLayout.NORTH);

        // hand
        handPanel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        JScrollPane handScroll = new JScrollPane(handPanel,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        handScroll.setBorder(null);
        handScroll.setPreferredSize(new Dimension(960, CardComponent.H + 28));
        south.add(handScroll, BorderLayout.CENTER);

        // controls + message
        JPanel controls = new JPanel(new BorderLayout());
        messageLabel.setForeground(new Color(0xb71c1c));
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD));
        controls.add(messageLabel, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        JButton playBtn = new JButton("Play");
        JButton passBtn = new JButton("Pass");
        JButton newGameBtn = new JButton("New Game");
        playBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { onPlay(); }
        });
        passBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { onPass(); }
        });
        newGameBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { onNewGame(); }
        });
        coachToggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { onCoachToggle(); }
        });
        buttons.add(playBtn);
        buttons.add(passBtn);
        buttons.add(newGameBtn);
        buttons.add(coachToggle);
        controls.add(buttons, BorderLayout.CENTER);

        south.add(controls, BorderLayout.SOUTH);
        return south;
    }

    private JComponent buildCoachPanel() {
        coachPanel.setBorder(BorderFactory.createTitledBorder("Coach"));
        coachPanel.setPreferredSize(new Dimension(280, 100));
        coachText.setEditable(false);
        coachText.setLineWrap(true);
        coachText.setWrapStyleWord(true);
        coachText.setFont(coachText.getFont().deriveFont(13f));
        coachText.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        coachPanel.add(new JScrollPane(coachText), BorderLayout.CENTER);
        coachPanel.setVisible(false); // hidden until coach is turned ON
        return coachPanel;
    }

    // --------------------------------------------------------------------------
    // button handlers
    // --------------------------------------------------------------------------

    private void onPlay() {
        if (engine.state().gameOver() || engine.state().currentSeat() != 0) {
            return;
        }
        List<Card> selected = selectedCards();
        if (selected.isEmpty()) {
            showMessage("Select one or more cards to play.");
            return;
        }
        Combination combo = engine.validateSelection(selected);
        if (combo == null) {
            showMessage("That selection is not a legal play right now.");
            return;
        }
        clearMessage();
        engine.playHuman(selected);
        refreshAll();
        maybeStartAiDriver();
    }

    private void onPass() {
        if (engine.state().gameOver() || engine.state().currentSeat() != 0) {
            return;
        }
        if (!engine.passHuman()) {
            showMessage("You cannot pass while leading the trick.");
            return;
        }
        clearMessage();
        refreshAll();
        maybeStartAiDriver();
    }

    private void onNewGame() {
        aiTimer.stop();
        engine.start(); // re-deal and reset the same engine (keeps injected strategy)
        clearMessage();
        coachText.setText("");
        refreshAll();
        maybeStartAiDriver();
    }

    private void onCoachToggle() {
        boolean on = coachToggle.isSelected();
        coachToggle.setText(on ? "Coach: ON" : "Coach: OFF");
        coachPanel.setVisible(on);
        refreshCoach();
        revalidate();
        repaint();
    }

    // --------------------------------------------------------------------------
    // AI turn driver
    // --------------------------------------------------------------------------

    private void maybeStartAiDriver() {
        GameState st = engine.state();
        if (!st.gameOver() && !isHumansTurn()) {
            if (!aiTimer.isRunning()) {
                aiTimer.start();
            }
        } else {
            // human's turn (or game over): refresh coach guidance
            refreshCoach();
        }
    }

    private void onAiTick() {
        GameState st = engine.state();
        if (st.gameOver()) {
            aiTimer.stop();
            refreshAll();
            return;
        }
        if (isHumansTurn()) {
            aiTimer.stop();
            refreshAll();
            refreshCoach();
            return;
        }
        final boolean acted = engine.stepAI();
        refreshAll();
        if (!acted) {
            // nothing advanced — avoid a busy loop
            aiTimer.stop();
            refreshCoach();
        }
    }

    private boolean isHumansTurn() {
        GameState st = engine.state();
        Player human = st.players().get(0);
        return st.currentSeat() == 0 && !human.isOut();
    }

    // --------------------------------------------------------------------------
    // refresh / rendering
    // --------------------------------------------------------------------------

    private void refreshAll() {
        rebuildHand();
        refreshSeats();
        refreshTrick();
        refreshCoach();
        revalidate();
        repaint();
    }

    private void rebuildHand() {
        handPanel.removeAll();
        cardComponents.clear();
        List<Card> hand = sortedHand(engine.humanHand());
        for (int i = 0; i < hand.size(); i++) {
            CardComponent cc = new CardComponent(hand.get(i));
            cardComponents.add(cc);
            handPanel.add(cc);
        }
        handPanel.revalidate();
        handPanel.repaint();
    }

    private List<Card> sortedHand(List<Card> hand) {
        List<Card> copy = new ArrayList<Card>(hand);
        copy.sort(new java.util.Comparator<Card>() {
            public int compare(Card a, Card b) {
                int byRank = a.rank().order() - b.rank().order();
                if (byRank != 0) {
                    return byRank;
                }
                int bySuit = a.suit().ordinal() - b.suit().ordinal();
                if (bySuit != 0) {
                    return bySuit;
                }
                return a.deckId() - b.deckId();
            }
        });
        return copy;
    }

    private void refreshSeats() {
        GameState st = engine.state();
        List<Player> players = st.players();
        for (int seat = 0; seat < players.size() && seat < seatPanels.length; seat++) {
            SeatPanel sp = seatPanels[seat];
            if (sp != null) {
                boolean winning = st.currentBestSeat() == seat && st.currentBest() != null;
                boolean active = st.currentSeat() == seat && !st.gameOver();
                sp.update(players.get(seat), winning, active);
            }
        }
    }

    private void refreshTrick() {
        GameState st = engine.state();
        Road road = st.currentRoad();
        if (st.gameOver()) {
            Team win = st.winningTeam();
            roadLabel.setText("Game Over — " + (win == null ? "no winner" : win.label() + " wins!"));
        } else if (road == null) {
            roadLabel.setText("New trick — seat " + st.leaderSeat() + " to lead");
        } else {
            roadLabel.setText("Active Road " + road.size() + "  (" + roadName(road) + ")");
        }

        // rebuild the per-play list
        trickPanel.removeAll();
        trickPanel.add(roadLabel, BorderLayout.NORTH);

        JPanel plays = new JPanel(new GridLayout(0, 1, 2, 2));
        plays.setOpaque(false);
        plays.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        List<Play> trick = st.trickPlays();
        if (trick.isEmpty()) {
            JLabel none = new JLabel("(no plays yet)");
            none.setForeground(new Color(0xd0e8d8));
            plays.add(none);
        } else {
            List<Player> players = st.players();
            for (int i = 0; i < trick.size(); i++) {
                Play p = trick.get(i);
                String who = (p.seat() >= 0 && p.seat() < players.size())
                        ? players.get(p.seat()).name() : ("Seat " + p.seat());
                String what = p.isPass() ? "PASS" : describeCombo(p.combo());
                JLabel row = new JLabel(who + ":  " + what);
                row.setForeground(Color.WHITE);
                boolean isBest = st.currentBestSeat() == p.seat() && !p.isPass();
                if (isBest) {
                    row.setFont(row.getFont().deriveFont(Font.BOLD));
                    row.setForeground(WINNING_MARK);
                }
                plays.add(row);
            }
        }
        trickPanel.add(plays, BorderLayout.CENTER);
        trickPanel.revalidate();
        trickPanel.repaint();
    }

    private void refreshCoach() {
        if (!coachToggle.isSelected()) {
            coachPanel.setVisible(false);
            // clear any stale coach highlight
            for (int i = 0; i < cardComponents.size(); i++) {
                cardComponents.get(i).setCoachHighlight(false);
            }
            return;
        }
        coachPanel.setVisible(true);

        // clear previous highlight first
        for (int i = 0; i < cardComponents.size(); i++) {
            cardComponents.get(i).setCoachHighlight(false);
        }

        GameState st = engine.state();
        if (st.gameOver() || !isHumansTurn()) {
            coachText.setText("Coach is waiting for your turn...");
            handPanel.repaint();
            return;
        }

        MoveSuggestion sug = advisor.advise(st, 0);
        if (sug == null) {
            coachText.setText("No suggestion available.");
            handPanel.repaint();
            return;
        }
        coachText.setText(sug.explanation() == null ? "" : sug.explanation());
        coachText.setCaretPosition(0);
        highlightCoachCards(sug.highlightCards());
        handPanel.repaint();
    }

    private void highlightCoachCards(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return;
        }
        // multiset match so duplicate cards across decks highlight the right count
        List<Card> remaining = new LinkedList<Card>(cards);
        for (int i = 0; i < cardComponents.size(); i++) {
            CardComponent cc = cardComponents.get(i);
            if (remaining.remove(cc.card())) {
                cc.setCoachHighlight(true);
            }
        }
    }

    // --------------------------------------------------------------------------
    // selection helpers
    // --------------------------------------------------------------------------

    private List<Card> selectedCards() {
        List<Card> out = new ArrayList<Card>();
        for (int i = 0; i < cardComponents.size(); i++) {
            CardComponent cc = cardComponents.get(i);
            if (cc.isSelected()) {
                out.add(cc.card());
            }
        }
        return out;
    }

    private void showMessage(String msg) {
        messageLabel.setText(msg);
    }

    private void clearMessage() {
        messageLabel.setText(" ");
    }

    // --------------------------------------------------------------------------
    // text helpers
    // --------------------------------------------------------------------------

    private static String roadName(Road road) {
        if (road == Road.SINGLE) return "single";
        if (road == Road.PAIR) return "pair";
        if (road == Road.TRIPLE) return "triple";
        return "five-card";
    }

    private static String describeCombo(Combination c) {
        if (c == null) {
            return "PASS";
        }
        StringBuilder sb = new StringBuilder();
        List<Card> cards = c.cards();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(cards.get(i).shortName());
        }
        sb.append("  [").append(prettyType(c.type().name())).append(']');
        return sb.toString();
    }

    private static String prettyType(String enumName) {
        String s = enumName.toLowerCase().replace('_', ' ');
        return s;
    }

    // --------------------------------------------------------------------------
    // inner component: an AI / human seat box
    // --------------------------------------------------------------------------

    private static final class SeatPanel extends JPanel {
        private final JLabel nameLabel = new JLabel("", SwingConstants.CENTER);
        private final JLabel countLabel = new JLabel("", SwingConstants.CENTER);
        private final JLabel markLabel = new JLabel(" ", SwingConstants.CENTER);

        SeatPanel(int seat) {
            super(new BorderLayout(2, 2));
            setPreferredSize(new Dimension(150, 78));
            setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1, true));
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));
            nameLabel.setForeground(Color.WHITE);
            countLabel.setForeground(Color.WHITE);
            markLabel.setForeground(Color.BLACK);
            markLabel.setFont(markLabel.getFont().deriveFont(Font.BOLD, 12f));
            add(nameLabel, BorderLayout.NORTH);
            add(countLabel, BorderLayout.CENTER);
            add(markLabel, BorderLayout.SOUTH);
            Team t = Team.forSeat(seat);
            setBackground(t == Team.TEAM_A ? TEAM_A_COLOR : TEAM_B_COLOR);
        }

        void update(Player p, boolean winning, boolean active) {
            nameLabel.setText(p.name() + (p.isHuman() ? " (You)" : ""));
            countLabel.setText(p.cardCount() + " cards" + (p.isOut() ? " — OUT" : ""));
            markLabel.setText(winning ? "★ winning trick" : (active ? "— to play —" : " "));
            markLabel.setForeground(winning ? new Color(0x4a2d00) : Color.WHITE);
            Border base = BorderFactory.createLineBorder(
                    winning ? WINNING_MARK : Color.DARK_GRAY, winning ? 3 : 1, true);
            if (active) {
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.WHITE, 2, true), base));
            } else {
                setBorder(base);
            }
        }
    }

    // --------------------------------------------------------------------------
    // inner component: a clickable hand card
    // --------------------------------------------------------------------------

    private final class CardComponent extends JComponent {
        static final int W = 58;
        static final int H = 84;
        private static final int LIFT = 12;

        private final Card card;
        private boolean selected;
        private boolean coachHighlight;

        CardComponent(Card card) {
            this.card = card;
            setPreferredSize(new Dimension(W + 4, H + LIFT + 4));
            setToolTipText(card.shortName());
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    selected = !selected;
                    clearMessage();
                    repaint();
                }
            });
        }

        Card card() {
            return card;
        }

        boolean isSelected() {
            return selected;
        }

        void setCoachHighlight(boolean on) {
            if (coachHighlight != on) {
                coachHighlight = on;
                repaint();
            }
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = 2;
            int y = selected ? 2 : (LIFT + 2);

            // card body
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x, y, W, H, 10, 10);

            // border: coach highlight (gold) is distinct from manual selection (blue)
            if (coachHighlight) {
                g2.setColor(COACH_BORDER);
                g2.setStroke(new java.awt.BasicStroke(3f));
                g2.drawRoundRect(x + 1, y + 1, W - 2, H - 2, 10, 10);
            } else if (selected) {
                g2.setColor(SELECT_BORDER);
                g2.setStroke(new java.awt.BasicStroke(3f));
                g2.drawRoundRect(x + 1, y + 1, W - 2, H - 2, 10, 10);
            } else {
                g2.setColor(Color.GRAY);
                g2.setStroke(new java.awt.BasicStroke(1f));
                g2.drawRoundRect(x, y, W, H, 10, 10);
            }

            // text
            boolean red = isRed(card);
            g2.setColor(red ? new Color(0xc62828) : Color.BLACK);
            g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
            String label = card.shortName();
            g2.drawString(label, x + 6, y + 22);
            g2.drawString(label, x + 6, y + H - 10);

            g2.dispose();
        }

        private boolean isRed(Card c) {
            String sym = c.suit().symbol();
            return "♥".equals(sym) || "♦".equals(sym);
        }
    }
}
