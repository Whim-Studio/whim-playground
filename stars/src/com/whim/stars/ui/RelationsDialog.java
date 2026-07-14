package com.whim.stars.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.whim.stars.model.Galaxy;
import com.whim.stars.model.Player;

/**
 * Diplomacy / Battle Plans screen (simplified): set the human player's stance —
 * Friend / Neutral / Enemy — toward each other player. Only Enemy stance drives
 * combat in the current engine; treaties are a later phase.
 */
public final class RelationsDialog extends JDialog {

    public RelationsDialog(Frame owner, Galaxy galaxy, final Player human) {
        super(owner, "Relations & Battle Plans", true);
        setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        final List<Player> others = new ArrayList<Player>();
        final List<JComboBox<Player.Relation>> combos = new ArrayList<JComboBox<Player.Relation>>();
        for (Player p : galaxy.players()) {
            if (p.id() == human.id()) {
                continue;
            }
            others.add(p);
            JComboBox<Player.Relation> combo = new JComboBox<Player.Relation>(Player.Relation.values());
            combo.setSelectedItem(human.relationTo(p.id()));
            combos.add(combo);
            form.add(new JLabel(p.name() + " (player " + p.id() + "):"));
            form.add(combo);
        }
        if (others.isEmpty()) {
            form.add(new JLabel("No other players."));
            form.add(new JLabel());
        }
        add(form, BorderLayout.CENTER);

        JPanel south = new JPanel();
        JButton ok = new JButton("Apply & Close");
        ok.addActionListener(e -> {
            for (int i = 0; i < others.size(); i++) {
                human.setRelation(others.get(i).id(),
                        (Player.Relation) combos.get(i).getSelectedItem());
            }
            dispose();
        });
        south.add(ok);
        add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }
}
