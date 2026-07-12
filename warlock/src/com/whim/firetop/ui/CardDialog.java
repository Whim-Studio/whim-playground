package com.whim.firetop.ui;

import com.whim.firetop.model.Card;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Modal card-draw display: shows the drawn card's name, deck, flavor text and the
 * resolved effect (log lines produced when the engine applied it).
 */
public final class CardDialog extends JDialog {

    public CardDialog(Window owner, Card card, List<String> outcomeLines) {
        super(owner, "You draw a card", ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(Theme.BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        Color accent;
        switch (card.getType()) {
            case TREASURE: accent = Theme.GOLD; break;
            case ENCOUNTER: accent = Theme.BLOOD; break;
            default: accent = Theme.ROYAL; break;
        }

        JPanel cardFace = new JPanel(new BorderLayout(6, 6));
        cardFace.setBackground(Theme.BG_PANEL);
        cardFace.setBorder(BorderFactory.createLineBorder(accent, 3));

        JLabel deck = new JLabel(card.getType().name() + " CARD", SwingConstants.CENTER);
        deck.setForeground(accent);
        deck.setFont(Theme.BODY_BOLD);
        deck.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        cardFace.add(deck, BorderLayout.NORTH);

        JLabel name = new JLabel(card.getName(), SwingConstants.CENTER);
        name.setForeground(Theme.PARCHMENT);
        name.setFont(Theme.HEADING);
        cardFace.add(name, BorderLayout.CENTER);

        JTextArea flavor = new JTextArea(card.getDescription());
        flavor.setEditable(false);
        flavor.setLineWrap(true);
        flavor.setWrapStyleWord(true);
        flavor.setBackground(Theme.BG_PANEL);
        flavor.setForeground(Theme.STONE_LIGHT);
        flavor.setFont(Theme.BODY);
        flavor.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));
        cardFace.add(flavor, BorderLayout.SOUTH);
        root.add(cardFace, BorderLayout.NORTH);

        StringBuilder sb = new StringBuilder();
        for (String l : outcomeLines) {
            sb.append(l).append("\n");
        }
        JTextArea outcome = new JTextArea(sb.toString().trim());
        outcome.setEditable(false);
        outcome.setLineWrap(true);
        outcome.setWrapStyleWord(true);
        outcome.setBackground(Theme.BG_DARK);
        outcome.setForeground(Theme.EMERALD);
        outcome.setFont(Theme.MONO);
        outcome.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        root.add(outcome, BorderLayout.CENTER);

        JButton ok = new JButton("Continue");
        ok.setMnemonic('C');
        ok.setBackground(accent);
        ok.setForeground(Color.WHITE);
        ok.setFont(Theme.BODY_BOLD);
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });
        root.add(ok, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(420, 360);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(ok);
    }
}
