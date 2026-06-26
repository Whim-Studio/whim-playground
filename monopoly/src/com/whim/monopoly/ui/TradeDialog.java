package com.whim.monopoly.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.whim.monopoly.domain.Player;
import com.whim.monopoly.domain.Space;
import com.whim.monopoly.engine.GameEngine;
import com.whim.monopoly.engine.GameState;
import com.whim.monopoly.engine.StandardTrade;
import com.whim.monopoly.engine.Trade;

/**
 * Modal trade builder. The current player (proposer) offers/requests cash,
 * deeds, and jail cards against a chosen recipient. Builds a {@link StandardTrade},
 * checks {@link GameEngine#isTradeValid(Trade)}, then {@code executeTrade}.
 */
public class TradeDialog extends JDialog {

    private final MonopolyFrame frame;
    private final GameEngine engine;
    private final Player proposer;
    private final List<Player> recipients = new ArrayList<Player>();

    private final JComboBox<String> recipientBox = new JComboBox<String>();
    private final JSpinner proposerCash;
    private final JSpinner recipientCash = new JSpinner();
    private final JSpinner proposerJail;
    private final JSpinner recipientJail = new JSpinner();

    private final JPanel proposerDeedPanel = new JPanel();
    private final JPanel recipientDeedPanel = new JPanel();
    private Map<Integer, JCheckBox> proposerDeedBoxes = new LinkedHashMap<Integer, JCheckBox>();
    private Map<Integer, JCheckBox> recipientDeedBoxes = new LinkedHashMap<Integer, JCheckBox>();

    public TradeDialog(MonopolyFrame frame, GameEngine engine) {
        super(frame, "Propose Trade", true);
        this.frame = frame;
        this.engine = engine;
        GameState s = engine.getState();
        this.proposer = s.getCurrentPlayer();

        for (int i = 0; i < s.getActivePlayers().size(); i++) {
            Player p = s.getActivePlayers().get(i);
            if (proposer != null && p.getId() != proposer.getId()) {
                recipients.add(p);
                recipientBox.addItem(p.getName());
            }
        }

        proposerCash = new JSpinner(new SpinnerNumberModel(0, 0,
                proposer != null ? proposer.getCash() : 0, 10));
        proposerJail = new JSpinner(new SpinnerNumberModel(0, 0,
                proposer != null ? proposer.getJailCards() : 0, 1));
        recipientCash.setModel(new SpinnerNumberModel(0, 0, 0, 10));
        recipientJail.setModel(new SpinnerNumberModel(0, 0, 0, 1));

        setLayout(new BorderLayout(8, 8));
        add(buildTop(), BorderLayout.NORTH);
        add(buildSides(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        recipientBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                rebuildRecipient();
            }
        });

        rebuildProposerDeeds();
        rebuildRecipient();
        pack();
        setMinimumSize(new Dimension(560, 460));
        setLocationRelativeTo(frame);
    }

    private JPanel buildTop() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(10, 12, 4, 12));
        JLabel l = new JLabel("Proposer: " + (proposer != null ? proposer.getName() : "—")
                + "    Recipient:");
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.add(l, BorderLayout.WEST);
        row.add(recipientBox, BorderLayout.CENTER);
        top.add(row, BorderLayout.CENTER);
        return top;
    }

    private JPanel buildSides() {
        JPanel sides = new JPanel(new GridLayout(1, 2, 10, 0));
        sides.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        sides.add(buildSide("You give", proposerCash, proposerJail, proposerDeedPanel));
        sides.add(buildSide("You receive", recipientCash, recipientJail, recipientDeedPanel));
        return sides;
    }

    private JPanel buildSide(String title, JSpinner cash, JSpinner jail, JPanel deedPanel) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder(title));

        JPanel top = new JPanel(new GridLayout(0, 2, 4, 4));
        top.add(new JLabel("Cash:"));
        top.add(cash);
        top.add(new JLabel("Jail cards:"));
        top.add(jail);
        panel.add(top, BorderLayout.NORTH);

        deedPanel.setLayout(new BoxLayout(deedPanel, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(deedPanel);
        sp.setPreferredSize(new Dimension(240, 240));
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildButtons() {
        JPanel buttons = new JPanel();
        JButton propose = new JButton("Propose & Execute");
        JButton cancel = new JButton("Cancel");
        buttons.add(propose);
        buttons.add(cancel);

        propose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                submit();
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        return buttons;
    }

    private void rebuildProposerDeeds() {
        proposerDeedBoxes = fillDeeds(proposerDeedPanel, proposer);
    }

    private void rebuildRecipient() {
        Player r = currentRecipient();
        recipientDeedBoxes = fillDeeds(recipientDeedPanel, r);
        int cashMax = r != null ? r.getCash() : 0;
        int jailMax = r != null ? r.getJailCards() : 0;
        recipientCash.setModel(new SpinnerNumberModel(0, 0, cashMax, 10));
        recipientJail.setModel(new SpinnerNumberModel(0, 0, jailMax, 1));
    }

    private Map<Integer, JCheckBox> fillDeeds(JPanel panel, Player owner) {
        panel.removeAll();
        Map<Integer, JCheckBox> boxes = new LinkedHashMap<Integer, JCheckBox>();
        if (owner != null) {
            GameState s = engine.getState();
            List<Integer> deeds = new ArrayList<Integer>(owner.getDeeds());
            Collections.sort(deeds);
            for (int i = 0; i < deeds.size(); i++) {
                int idx = deeds.get(i).intValue();
                Space space = s.getBoard().spaceAt(idx);
                String label = space.getName();
                if (s.holdingAt(idx) != null && s.holdingAt(idx).isMortgaged()) {
                    label += " (mortgaged)";
                }
                JCheckBox cb = new JCheckBox(label);
                boxes.put(Integer.valueOf(idx), cb);
                panel.add(cb);
            }
        }
        if (boxes.isEmpty()) {
            JLabel none = new JLabel("  (no deeds)");
            none.setForeground(Color.GRAY);
            panel.add(none);
        }
        panel.revalidate();
        panel.repaint();
        return boxes;
    }

    private Player currentRecipient() {
        int sel = recipientBox.getSelectedIndex();
        if (sel < 0 || sel >= recipients.size()) {
            return null;
        }
        return recipients.get(sel);
    }

    private Set<Integer> checkedDeeds(Map<Integer, JCheckBox> boxes) {
        Set<Integer> out = new HashSet<Integer>();
        for (Map.Entry<Integer, JCheckBox> e : boxes.entrySet()) {
            if (e.getValue().isSelected()) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    private void submit() {
        final Player recipient = currentRecipient();
        if (proposer == null || recipient == null) {
            JOptionPane.showMessageDialog(this, "No valid recipient.", "Trade",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int pCash = ((Number) proposerCash.getValue()).intValue();
        int rCash = ((Number) recipientCash.getValue()).intValue();
        int pJail = ((Number) proposerJail.getValue()).intValue();
        int rJail = ((Number) recipientJail.getValue()).intValue();
        Set<Integer> pDeeds = checkedDeeds(proposerDeedBoxes);
        Set<Integer> rDeeds = checkedDeeds(recipientDeedBoxes);

        final Trade trade = new StandardTrade(proposer, recipient, pCash, rCash,
                pDeeds, rDeeds, pJail, rJail);

        if (!engine.isTradeValid(trade)) {
            JOptionPane.showMessageDialog(this,
                    "The engine rejected this trade (ownership or balance issue).",
                    "Invalid trade", JOptionPane.ERROR_MESSAGE);
            return;
        }
        frame.submitEngineAction(new Runnable() {
            public void run() {
                engine.executeTrade(trade);
            }
        });
        dispose();
    }
}
