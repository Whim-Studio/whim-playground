package com.whim.firetop.ui;

import com.whim.firetop.model.Character;
import com.whim.firetop.model.GameState;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** Victory / defeat summary shown when the game ends. */
public final class EndScreen extends JPanel {

    public interface NewGameHandler { void onNewGame(); }

    public EndScreen(GameState state, NewGameHandler handler) {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());

        boolean win = state.isVictory();
        JLabel title = new JLabel(win ? "VICTORY" : "DEFEAT", JLabel.CENTER);
        title.setFont(Theme.TITLE.deriveFont(52f));
        title.setForeground(win ? Theme.GOLD : Theme.BLOOD);
        title.setBorder(BorderFactory.createEmptyBorder(40, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel sub = new JLabel(win
                ? "Zagor is slain and the treasure of Firetop Mountain is claimed!"
                : "The mountain has swallowed the whole party. Firetop endures.");
        sub.setForeground(Theme.PARCHMENT);
        sub.setFont(Theme.HEADING);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(sub);
        center.add(javax.swing.Box.createVerticalStrut(18));

        for (Character c : state.getPlayers()) {
            JLabel l = new JLabel(c.getName() + " — "
                    + (c.isAlive() ? "survived" : "fell") + "; gold: " + c.getGold()
                    + "; STAMINA " + c.getStaminaCurrent() + "/" + c.getStaminaInitial());
            l.setForeground(c.isAlive() ? Theme.EMERALD : Theme.STONE_LIGHT);
            l.setFont(Theme.BODY_BOLD);
            l.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(l);
        }
        add(center, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        JButton again = new JButton("New Game");
        again.setMnemonic('N');
        again.setBackground(Theme.EMERALD);
        again.setForeground(Color.WHITE);
        again.setFont(Theme.HEADING);
        again.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { handler.onNewGame(); }
        });
        south.add(again);
        south.setBorder(BorderFactory.createEmptyBorder(0, 0, 40, 0));
        add(south, BorderLayout.SOUTH);
    }
}
