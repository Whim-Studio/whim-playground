package com.whim.b5db.ui;

import com.whim.b5db.engine.PlayerState;
import com.whim.b5db.model.Card;
import com.whim.b5db.model.ContestType;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * The game board: market on the right (buyable RIM + CORRIDOR cards), the
 * current player's tableau on the left, a live scoreboard, and a log. Human
 * turns pause here for purchases; AI turns are played automatically between
 * human turns.
 */
public final class GamePanel extends JPanel {

    private final MainFrame frame;
    private final MatchController controller;

    private final JLabel status = new JLabel("", SwingConstants.LEFT);
    private final JTextArea tableau = new JTextArea();
    private final JTextArea scoreboard = new JTextArea();
    private final JTextArea log = new JTextArea();
    private final DefaultListModel<Card> marketModel = new DefaultListModel<>();
    private final JList<Card> marketList = new JList<>(marketModel);
    private final JButton buyButton = MainFrame.big(new JButton("Buy Selected"));
    private final JButton endButton = MainFrame.big(new JButton("End Turn"));

    public GamePanel(MainFrame frame, MatchController controller) {
        super(new BorderLayout(8, 8));
        this.frame = frame;
        this.controller = controller;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        build();
    }

    private void build() {
        status.setFont(MainFrame.TITLE_FONT.deriveFont(20f));
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        add(status, BorderLayout.NORTH);

        tableau.setEditable(false);
        tableau.setFont(MainFrame.UI_FONT);
        scoreboard.setEditable(false);
        scoreboard.setFont(MainFrame.UI_FONT);

        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.add(labelled("Your Tableau", new JScrollPane(tableau)), BorderLayout.CENTER);
        left.add(labelled("Scoreboard", new JScrollPane(scoreboard)), BorderLayout.SOUTH);

        marketList.setFont(MainFrame.UI_FONT);
        marketList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        marketList.setCellRenderer((list, value, index, sel, focus) -> {
            JLabel l = new JLabel(render(value));
            l.setFont(MainFrame.UI_FONT);
            l.setOpaque(true);
            if (sel) {
                l.setBackground(list.getSelectionBackground());
                l.setForeground(list.getSelectionForeground());
            }
            return l;
        });
        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.add(labelled("Market (THE RIM + Corridor)", new JScrollPane(marketList)), BorderLayout.CENTER);

        JPanel center = new JPanel(new GridLayout(1, 2, 8, 8));
        center.add(left);
        center.add(right);
        add(center, BorderLayout.CENTER);

        log.setEditable(false);
        log.setFont(MainFrame.UI_FONT.deriveFont(13f));
        log.setRows(5);
        JScrollPane logScroll = new JScrollPane(log);

        buyButton.setMnemonic('U');
        buyButton.addActionListener(e -> onBuy());
        endButton.setMnemonic('E');
        endButton.addActionListener(e -> onEndTurn());
        JButton menu = MainFrame.big(new JButton("Main Menu"));
        menu.addActionListener(e -> frame.show("MENU"));

        JPanel buttons = new JPanel();
        buttons.add(buyButton);
        buttons.add(endButton);
        buttons.add(menu);

        JPanel south = new JPanel(new BorderLayout());
        south.add(labelled("Log", logScroll), BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel labelled(String title, java.awt.Component c) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel l = new JLabel(title);
        l.setFont(MainFrame.UI_FONT.deriveFont(14f));
        p.add(l, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    /** Called once when the board is first shown. */
    public void onShown() {
        appendLog("Game started. Prestige target: " + controller.state().prestigeTarget() + ".");
        process();
    }

    /** Advance AI seats, then either end the game or set up the human's turn. */
    private void process() {
        int ai = controller.runAiUntilHumanOrOver();
        if (ai > 0) {
            appendLog("Played " + ai + " AI turn(s).");
        }
        if (controller.isOver()) {
            showEnd();
            return;
        }
        controller.beginHumanTurn();
        refresh();
    }

    private void onBuy() {
        Card c = marketList.getSelectedValue();
        if (c == null) {
            return;
        }
        if (c.cost() > controller.current().influence()) {
            appendLog("Not enough Influence for " + c.name() + " (cost " + c.cost() + ").");
            return;
        }
        if (controller.buy(c)) {
            appendLog("Bought " + c.name() + " for " + c.cost() + " Influence.");
            refresh();
        }
    }

    private void onEndTurn() {
        controller.endHumanTurn();
        process();
    }

    private void refresh() {
        PlayerState p = controller.current();
        status.setText("Turn " + controller.state().turn() + " — " + p.name()
                + "  |  Influence: " + p.influence() + "  |  Prestige: " + p.prestige());

        StringBuilder t = new StringBuilder();
        t.append("Faction: ").append(p.faction().display()).append('\n');
        t.append("Influence available: ").append(p.influence()).append('\n');
        t.append("Attribute pools: ");
        for (ContestType ct : ContestType.values()) {
            t.append(ct).append('=').append(p.pool(ct)).append("  ");
        }
        t.append('\n');
        t.append("Prestige: ").append(p.prestige()).append("  (target ")
                .append(controller.state().prestigeTarget()).append(")\n\n");
        t.append("COMMAND_ROW (permanents in play):\n");
        for (Card c : p.commandRow()) {
            t.append("  • ").append(render(c)).append('\n');
        }
        t.append("\nPlayed this turn:\n");
        for (Card c : p.playArea()) {
            t.append("  • ").append(c.name()).append('\n');
        }
        t.append("\nDeck: ").append(p.drawDeck().size())
                .append("  Discard: ").append(p.discard().size())
                .append("  Trashed: ").append(p.outOfGame().size()).append('\n');
        tableau.setText(t.toString());
        tableau.setCaretPosition(0);

        StringBuilder s = new StringBuilder();
        for (PlayerState pp : controller.state().players()) {
            s.append(String.format("%-28s %3d prestige%n", pp.name(), pp.prestige()));
        }
        scoreboard.setText(s.toString());

        marketModel.clear();
        for (Card c : controller.state().market().rim()) {
            marketModel.addElement(c);
        }
        for (Card c : controller.state().market().corridor()) {
            marketModel.addElement(c);
        }

        boolean human = controller.isHumanTurn() && !controller.isOver();
        buyButton.setEnabled(human);
        endButton.setEnabled(human);
    }

    private String render(Card c) {
        String stats = "D" + c.attribute(ContestType.DIPLOMACY)
                + " I" + c.attribute(ContestType.INTRIGUE)
                + " M" + c.attribute(ContestType.MILITARY)
                + " P" + c.attribute(ContestType.PSI);
        String conflict = c.contest() == null ? "" : "  {" + c.contest() + " ≥ " + c.difficulty() + "}";
        String prestige = c.prestige() != 0 ? "  +" + c.prestige() + "VP" : "";
        return c.name() + "  (cost " + c.cost() + ")  [" + c.faction().display() + " / "
                + c.type() + "]  " + stats + prestige + conflict;
    }

    private void showEnd() {
        PlayerState winner = controller.leader();
        removeAll();
        setLayout(new BorderLayout());
        JLabel title = new JLabel("Game Over — Winner: " + winner.name(), SwingConstants.CENTER);
        title.setFont(MainFrame.TITLE_FONT);
        title.setBorder(BorderFactory.createEmptyBorder(40, 20, 20, 20));
        add(title, BorderLayout.NORTH);

        JTextArea summary = new JTextArea();
        summary.setEditable(false);
        summary.setFont(MainFrame.UI_FONT);
        List<PlayerState> ranked = new ArrayList<>(controller.state().players());
        ranked.sort((a, b) -> b.prestige() - a.prestige());
        StringBuilder sb = new StringBuilder("Final standings:\n\n");
        int rank = 1;
        for (PlayerState p : ranked) {
            sb.append(rank++).append(". ").append(p.name())
                    .append("  —  ").append(p.prestige()).append(" prestige, ")
                    .append(p.commandRow().size()).append(" permanents\n");
        }
        sb.append("\nGame length: ").append(controller.state().turn()).append(" turns.");
        summary.setText(sb.toString());
        add(new JScrollPane(summary), BorderLayout.CENTER);

        JButton menu = MainFrame.big(new JButton("Main Menu"));
        menu.addActionListener(e -> frame.show("MENU"));
        JPanel south = new JPanel();
        south.add(menu);
        add(south, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }
}
