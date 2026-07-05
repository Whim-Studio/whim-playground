package com.taipan.view;

import com.taipan.controller.GameController;
import com.taipan.model.GameConstants;
import com.taipan.model.GameState;
import com.taipan.model.Good;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

/** The port hub: status, trading table, and access to travel / bank / shipyard. */
public class PortPanel extends JPanel {

    private final GameFrame frame;

    private final JLabel headerLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final HarborCanvas harbor = new HarborCanvas();
    private final JPanel goodsPanel = new JPanel();

    private final JButton bankButton = new JButton("Bank & Debt (Hong Kong)");
    private final JButton shipyardButton = new JButton("Shipyard (Hong Kong)");

    public PortPanel(GameFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // North: title + harbour art.
        headerLabel.setFont(new Font("Serif", Font.BOLD, 26));
        JPanel north = new JPanel(new BorderLayout());
        north.add(headerLabel, BorderLayout.WEST);
        north.add(harbor, BorderLayout.EAST);
        add(north, BorderLayout.NORTH);

        // Center: status (left) + goods (right).
        statusLabel.setVerticalAlignment(SwingConstants.TOP);
        statusLabel.setBorder(BorderFactory.createTitledBorder("Firm & Ship"));

        goodsPanel.setBorder(BorderFactory.createTitledBorder("Goods for trade"));
        goodsPanel.setLayout(new GridBagLayout());

        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));
        center.add(statusLabel);
        center.add(goodsPanel);
        add(center, BorderLayout.CENTER);

        // South: navigation.
        JButton travel = new JButton("Set Sail (Travel)");
        travel.addActionListener(e -> frame.showTravel());
        bankButton.addActionListener(e -> openBank());
        shipyardButton.addActionListener(e -> openShipyard());
        JButton retire = new JButton("Retire");
        retire.addActionListener(e -> confirmRetire());

        JPanel south = new JPanel(new GridLayout(1, 4, 8, 0));
        south.add(travel);
        south.add(bankButton);
        south.add(shipyardButton);
        south.add(retire);
        add(south, BorderLayout.SOUTH);
    }

    public void refresh() {
        GameController c = frame.getController();
        GameState s = c.getState();

        headerLabel.setText(s.getLocation().display() + "   —   "
                + monthName(s.getMonth()) + " " + s.getYear());
        harbor.setState(s);

        statusLabel.setText("<html><div style='padding:6px;font-family:monospace'>"
                + "Taipan: <b>" + esc(s.getTaipanName()) + "</b><br>"
                + "Firm: <b>" + esc(s.getFirmName()) + "</b><br><br>"
                + "Cash: $" + s.getCash() + "<br>"
                + "Bank: $" + s.getBank() + "<br>"
                + "Debt: $" + s.getDebt() + "<br>"
                + "Net worth: $" + s.netWorth() + "<br><br>"
                + "Hold: " + s.getShip().usedHold() + " / " + s.getShip().getCapacity()
                + " (free " + s.getShip().freeHold() + ")<br>"
                + "Guns: " + s.getShip().getGuns() + "<br>"
                + "Damage: " + s.getShip().getDamage() + "%"
                + "</div></html>");

        boolean home = s.getLocation().isHome();
        bankButton.setEnabled(home);
        shipyardButton.setEnabled(home);

        rebuildGoods(c, s);
        revalidate();
        repaint();
    }

    private void rebuildGoods(final GameController c, GameState s) {
        goodsPanel.removeAll();
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        String[] heads = {"Good", "Price", "Held", "", ""};
        for (int col = 0; col < heads.length; col++) {
            gc.gridx = col;
            gc.gridy = 0;
            JLabel h = new JLabel(heads[col]);
            h.setFont(h.getFont().deriveFont(Font.BOLD));
            goodsPanel.add(h, gc);
        }

        int row = 1;
        for (final Good g : Good.values()) {
            gc.gridy = row;
            gc.gridx = 0;
            goodsPanel.add(new JLabel(g.display()), gc);
            gc.gridx = 1;
            goodsPanel.add(new JLabel("$" + s.getPrice(g)), gc);
            gc.gridx = 2;
            goodsPanel.add(new JLabel(String.valueOf(s.getShip().getCargo(g))), gc);

            JButton buy = new JButton("Buy");
            buy.addActionListener(e -> doTrade(c, g, true));
            gc.gridx = 3;
            goodsPanel.add(buy, gc);

            JButton sell = new JButton("Sell");
            sell.addActionListener(e -> doTrade(c, g, false));
            gc.gridx = 4;
            goodsPanel.add(sell, gc);
            row++;
        }
        goodsPanel.revalidate();
        goodsPanel.repaint();
    }

    private void doTrade(GameController c, Good g, boolean buy) {
        while (true) {
            String verb = buy ? "buy" : "sell";
            Integer qty = promptInt("How many units of " + g.display() + " to " + verb + "?");
            if (qty == null) {
                return;
            }
            String err = buy ? c.buy(g, qty) : c.sell(g, qty);
            if (err == null) {
                refresh();
                return;
            }
            JOptionPane.showMessageDialog(this, err, "Try again", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ---------------------------------------------------------------- bank

    private void openBank() {
        GameController c = frame.getController();
        while (true) {
            String[] opts = {"Deposit", "Withdraw", "Borrow", "Repay debt", "Done"};
            int choice = JOptionPane.showOptionDialog(this,
                    bankSummary(c) , "Elder Brother Wu & the Bank",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, opts, opts[4]);
            String err = null;
            if (choice == 0) {
                Long a = promptLong("Deposit how much into the bank?");
                if (a != null) err = c.deposit(a);
            } else if (choice == 1) {
                Long a = promptLong("Withdraw how much from the bank?");
                if (a != null) err = c.withdraw(a);
            } else if (choice == 2) {
                Long a = promptLong("Borrow how much from Elder Brother Wu?");
                if (a != null) err = c.borrow(a);
            } else if (choice == 3) {
                Long a = promptLong("Repay how much of your debt?");
                if (a != null) err = c.repay(a);
            } else {
                break;
            }
            if (err != null) {
                JOptionPane.showMessageDialog(this, err, "Try again", JOptionPane.WARNING_MESSAGE);
            }
            refresh();
        }
    }

    private String bankSummary(GameController c) {
        GameState s = c.getState();
        return "Cash: $" + s.getCash() + "\nBank: $" + s.getBank()
                + "\nDebt: $" + s.getDebt()
                + "\n\nWu's debt grows " + (int) (GameConstants.DEBT_INTEREST * 100)
                + "% each voyage. Money in the bank is safe from pirates.";
    }

    // ---------------------------------------------------------------- shipyard

    private void openShipyard() {
        GameController c = frame.getController();
        while (true) {
            String info = "Ship damage: " + c.getState().getShip().getDamage() + "%\n"
                    + "Repair cost: $" + c.repairCost() + "\n"
                    + "Hold: " + c.getState().getShip().getCapacity() + " units\n"
                    + "Bigger hold: +" + GameConstants.CAPACITY_UPGRADE_AMOUNT
                    + " for $" + GameConstants.CAPACITY_UPGRADE_PRICE + "\n"
                    + "Guns: " + c.getState().getShip().getGuns()
                    + "   (a gun costs $" + GameConstants.GUN_PRICE
                    + " and " + GameConstants.GUN_HOLD_SPACE + " hold units)";
            String[] opts = {"Repair ship", "Enlarge hold", "Buy gun", "Done"};
            int choice = JOptionPane.showOptionDialog(this, info, "McHenry's Shipyard",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, opts, opts[3]);
            String err = null;
            if (choice == 0) {
                err = c.repairShip();
            } else if (choice == 1) {
                err = c.upgradeCapacity();
            } else if (choice == 2) {
                err = c.buyGun();
            } else {
                break;
            }
            if (err != null) {
                JOptionPane.showMessageDialog(this, err, "Shipyard", JOptionPane.WARNING_MESSAGE);
            }
            refresh();
        }
    }

    // ---------------------------------------------------------------- retire

    private void confirmRetire() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Retire from the trade now with a net worth of $"
                        + frame.getController().getState().netWorth() + "?",
                "Retire", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            frame.retire();
        }
    }

    // ---------------------------------------------------------------- helpers

    private Integer promptInt(String message) {
        Long v = promptLong(message);
        if (v == null) {
            return null;
        }
        if (v > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return v.intValue();
    }

    private Long promptLong(String message) {
        while (true) {
            String in = JOptionPane.showInputDialog(this, message);
            if (in == null) {
                return null; // cancelled
            }
            in = in.trim();
            try {
                long v = Long.parseLong(in);
                if (v < 0) {
                    JOptionPane.showMessageDialog(this, "Enter a non-negative number.",
                            "Invalid", JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                return v;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "\"" + in + "\" is not a number.",
                        "Invalid", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private static String monthName(int m) {
        String[] names = {"", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return (m >= 1 && m <= 12) ? names[m] : "?";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
