package com.whim.monopoly.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.whim.monopoly.domain.OwnableSpace;
import com.whim.monopoly.domain.Player;
import com.whim.monopoly.engine.GameEngine;
import com.whim.monopoly.engine.GameState;
import com.whim.monopoly.engine.TurnPhase;

/**
 * Modal auction for the AUCTION phase. One row per active player with a bid
 * spinner plus Bid / Pass. Bids must exceed the current high bid; the engine
 * resolves the auction (and the dialog is disposed) once one bidder remains.
 */
public class AuctionDialog extends JDialog {

    private final MonopolyFrame frame;
    private final GameEngine engine;

    private final JLabel titleLabel = new JLabel();
    private final JLabel highLabel = new JLabel();
    private final Set<Integer> passed = new HashSet<Integer>();

    private final List<Player> bidders;
    private final JSpinner[] spinners;
    private final JButton[] bidButtons;
    private final JButton[] passButtons;

    public AuctionDialog(MonopolyFrame frame, GameEngine engine) {
        super(frame, "Auction", true);
        this.frame = frame;
        this.engine = engine;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // auction must resolve via engine

        GameState s = engine.getState();
        this.bidders = s.getActivePlayers();
        this.spinners = new JSpinner[bidders.size()];
        this.bidButtons = new JButton[bidders.size()];
        this.passButtons = new JButton[bidders.size()];

        setLayout(new BorderLayout(8, 8));
        JPanel head = new JPanel(new BorderLayout());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 15f));
        highLabel.setFont(highLabel.getFont().deriveFont(13f));
        head.setBorder(BorderFactory.createEmptyBorder(10, 12, 4, 12));
        head.add(titleLabel, BorderLayout.NORTH);
        head.add(highLabel, BorderLayout.SOUTH);
        add(head, BorderLayout.NORTH);

        JPanel rows = new JPanel(new GridBagLayout());
        rows.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 3, 3, 3);
        gc.anchor = GridBagConstraints.WEST;

        for (int i = 0; i < bidders.size(); i++) {
            final Player p = bidders.get(i);
            gc.gridy = i;

            gc.gridx = 0;
            JLabel name = new JLabel("  " + p.getName());
            name.setOpaque(true);
            name.setBackground(p.getToken() != null ? p.getToken() : Color.LIGHT_GRAY);
            name.setForeground(Color.WHITE);
            name.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 8));
            rows.add(name, gc);

            gc.gridx = 1;
            JLabel cash = new JLabel("$" + p.getCash());
            rows.add(cash, gc);

            gc.gridx = 2;
            JSpinner sp = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1));
            ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setColumns(6);
            spinners[i] = sp;
            rows.add(sp, gc);

            gc.gridx = 3;
            final int rowIdx = i;
            JButton bid = new JButton("Bid");
            bid.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final int amount = ((Number) spinners[rowIdx].getValue()).intValue();
                    frame.submitEngineAction(new Runnable() {
                        public void run() {
                            engine.placeBid(p, amount);
                        }
                    });
                }
            });
            bidButtons[i] = bid;
            rows.add(bid, gc);

            gc.gridx = 4;
            JButton pass = new JButton("Pass");
            pass.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    passed.add(Integer.valueOf(p.getId()));
                    frame.submitEngineAction(new Runnable() {
                        public void run() {
                            engine.passAuction(p);
                        }
                    });
                }
            });
            passButtons[i] = pass;
            rows.add(pass, gc);
        }
        add(rows, BorderLayout.CENTER);

        refresh();
        pack();
        setLocationRelativeTo(frame);
    }

    /** Sync the high-bid display and per-row enablement from live state. */
    public void refresh() {
        GameState s = engine.getState();
        if (s.getPhase() != TurnPhase.AUCTION) {
            return;
        }
        OwnableSpace space = s.getAuctionSpace();
        titleLabel.setText("Auctioning: " + (space != null ? space.getName() : "?")
                + (space != null ? "  (list $" + space.getPrice() + ")" : ""));
        Player high = s.getAuctionHighBidder();
        int highBid = s.getAuctionHighBid();
        if (high != null) {
            highLabel.setText("High bid: $" + highBid + " by " + high.getName());
        } else {
            highLabel.setText("No bids yet. Bidding opens at $1.");
        }

        int minNext = highBid + 1;
        for (int i = 0; i < bidders.size(); i++) {
            Player p = bidders.get(i);
            boolean out = passed.contains(Integer.valueOf(p.getId()));
            boolean canAfford = p.getCash() >= minNext;
            spinners[i].setEnabled(!out);
            bidButtons[i].setEnabled(!out && canAfford);
            passButtons[i].setEnabled(!out);
            SpinnerNumberModel m = (SpinnerNumberModel) spinners[i].getModel();
            m.setMinimum(Integer.valueOf(minNext));
            if (((Number) spinners[i].getValue()).intValue() < minNext) {
                spinners[i].setValue(Integer.valueOf(minNext));
            }
        }
    }
}
