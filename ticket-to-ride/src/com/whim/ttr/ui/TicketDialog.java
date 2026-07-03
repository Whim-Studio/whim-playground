package com.whim.ttr.ui;

import com.whim.ttr.domain.DestinationTicket;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal destination-ticket picker. Presents the offered tickets as check boxes
 * and forces the player to keep at least {@code minKeep} of them before the OK
 * button enables.
 */
final class TicketDialog {

    private TicketDialog() { }

    /** @return the kept subset (never fewer than {@code minKeep}); never null. */
    static List<DestinationTicket> prompt(Frame owner, String title,
                                          List<DestinationTicket> offered, int minKeep) {
        final JDialog dialog = new JDialog(owner, title, true);
        dialog.setLayout(new BorderLayout(8, 8));

        JLabel header = new JLabel("Keep at least " + minKeep + " ticket(s):");
        header.setBorder(BorderFactory.createEmptyBorder(10, 12, 2, 12));
        dialog.add(header, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        final List<JCheckBox> boxes = new ArrayList<JCheckBox>();
        for (DestinationTicket t : offered) {
            JCheckBox cb = new JCheckBox(t.from() + "  →  " + t.to() + "   (" + t.points() + " pts)", true);
            cb.setFont(cb.getFont().deriveFont(Font.PLAIN, 13f));
            boxes.add(cb);
            list.add(cb);
        }
        dialog.add(list, BorderLayout.CENTER);

        final JButton ok = new JButton("Keep selected");
        Runnable validate = () -> {
            int checked = 0;
            for (JCheckBox b : boxes) {
                if (b.isSelected()) {
                    checked++;
                }
            }
            ok.setEnabled(checked >= minKeep);
        };
        for (JCheckBox b : boxes) {
            b.addActionListener(e -> validate.run());
        }
        validate.run();

        final List<DestinationTicket> kept = new ArrayList<DestinationTicket>();
        ok.addActionListener(e -> {
            kept.clear();
            for (int i = 0; i < boxes.size(); i++) {
                if (boxes.get(i).isSelected()) {
                    kept.add(offered.get(i));
                }
            }
            dialog.dispose();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        dialog.add(buttons, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        // Fallback: if the player forced the window closed, keep the first minKeep.
        if (kept.isEmpty() && !offered.isEmpty()) {
            for (int i = 0; i < offered.size() && i < Math.max(1, minKeep); i++) {
                kept.add(offered.get(i));
            }
        }
        return kept;
    }
}
