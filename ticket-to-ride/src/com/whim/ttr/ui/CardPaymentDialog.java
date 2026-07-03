package com.whim.ttr.ui;

import com.whim.ttr.api.CardColor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Modal card-payment picker. Shows a spinner (0..count) for each color the
 * player holds, letting them choose exactly which cards to spend. Used for
 * claiming routes, paying the extra tunnel cost, and building stations.
 *
 * <p>The engine performs all real validation; this dialog only constrains the
 * player to cards they actually own.</p>
 */
final class CardPaymentDialog {

    private CardPaymentDialog() { }

    /**
     * @return the chosen list of cards, or {@code null} if the player cancelled.
     */
    static List<CardColor> prompt(Frame owner, String title, String prompt,
                                  List<CardColor> hand) {
        final JDialog dialog = new JDialog(owner, title, true);
        dialog.setLayout(new BorderLayout(8, 8));

        JLabel header = new JLabel("<html><body style='width:320px'>" + prompt + "</body></html>");
        header.setBorder(BorderFactory.createEmptyBorder(10, 12, 4, 12));
        dialog.add(header, BorderLayout.NORTH);

        // Count each color in hand.
        Map<CardColor, Integer> counts = new EnumMap<CardColor, Integer>(CardColor.class);
        for (CardColor c : hand) {
            Integer cur = counts.get(c);
            counts.put(c, (cur == null ? 0 : cur) + 1);
        }

        JPanel grid = new JPanel(new GridLayout(0, 1, 4, 4));
        grid.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        final Map<CardColor, JSpinner> spinners = new EnumMap<CardColor, JSpinner>(CardColor.class);

        // Stable order: train colors then LOCOMOTIVE.
        List<CardColor> order = new ArrayList<CardColor>();
        for (CardColor c : CardColor.trainColors()) {
            order.add(c);
        }
        order.add(CardColor.LOCOMOTIVE);

        boolean any = false;
        for (CardColor c : order) {
            int have = counts.containsKey(c) ? counts.get(c) : 0;
            if (have <= 0) {
                continue;
            }
            any = true;
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            JLabel chip = new JLabel("  " + UiColors.label(c) + " ×" + have + "  ");
            chip.setOpaque(true);
            chip.setBackground(UiColors.of(c));
            chip.setForeground(UiColors.textOn(c));
            chip.setFont(chip.getFont().deriveFont(Font.BOLD));
            chip.setPreferredSize(new Dimension(140, 24));
            JSpinner sp = new JSpinner(new SpinnerNumberModel(0, 0, have, 1));
            sp.setPreferredSize(new Dimension(60, 24));
            spinners.put(c, sp);
            row.add(chip);
            row.add(new JLabel("pay:"));
            row.add(sp);
            grid.add(row);
        }
        if (!any) {
            grid.add(new JLabel("You have no cards to spend."));
        }
        dialog.add(grid, BorderLayout.CENTER);

        final List<CardColor> result = new ArrayList<CardColor>();
        final boolean[] confirmed = { false };

        JButton pay = new JButton("Pay");
        JButton cancel = new JButton("Cancel");
        pay.addActionListener(e -> {
            result.clear();
            for (Map.Entry<CardColor, JSpinner> en : spinners.entrySet()) {
                int n = (Integer) en.getValue().getValue();
                for (int i = 0; i < n; i++) {
                    result.add(en.getKey());
                }
            }
            confirmed[0] = true;
            dialog.dispose();
        });
        cancel.addActionListener(e -> {
            confirmed[0] = false;
            dialog.dispose();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(pay);
        dialog.add(buttons, BorderLayout.SOUTH);

        alignLabels(grid);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        return confirmed[0] ? result : null;
    }

    private static void alignLabels(JPanel grid) {
        for (Component comp : grid.getComponents()) {
            comp.setBackground(new Color(245, 245, 245));
        }
    }
}
