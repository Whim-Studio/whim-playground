package com.whim.firetop.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * "How to Play" screen. All rules text here is written in my own words; it is not
 * copied from any published material.
 */
public final class HowToPlayDialog extends JDialog {

    private static final String TEXT =
            "THE WARLOCK OF FIRETOP MOUNTAIN — How to Play\n"
            + "(An unofficial, fan-made educational recreation. Not affiliated with,\n"
            + " endorsed by, or connected to Games Workshop or the original creators.)\n"
            + "\n"
            + "GOAL\n"
            + "  Lead your adventurers through the dungeon beneath Firetop Mountain,\n"
            + "  survive its monsters and traps, and defeat the warlock Zagor in his\n"
            + "  throne room to claim the treasure. If every adventurer dies, the\n"
            + "  party loses.\n"
            + "\n"
            + "YOUR ADVENTURER\n"
            + "  SKILL    (7-12)  your prowess in a fight.\n"
            + "  STAMINA  (14-24) your health; at 0 you die.\n"
            + "  LUCK     (7-12)  your fortune; spent whenever you Test Your Luck.\n"
            + "  Each value has an Initial maximum it can never exceed.\n"
            + "\n"
            + "A TURN\n"
            + "  1. Roll to Move: roll one die and step that many rooms along the\n"
            + "     corridors. Reachable rooms glow green — click one (or use the\n"
            + "     destination buttons).\n"
            + "  2. Resolve the room you enter: fight a monster, draw a treasure or\n"
            + "     event card, spring a trap, or receive a blessing.\n"
            + "  3. End your turn and pass to the next adventurer.\n"
            + "\n"
            + "COMBAT\n"
            + "  Each round, you and the monster roll 2 dice and add SKILL to get an\n"
            + "  Attack Strength. The higher score wounds the other for 2 STAMINA;\n"
            + "  a tie means no damage. Fight round by round until one side falls.\n"
            + "\n"
            + "TESTING YOUR LUCK\n"
            + "  When you wound a monster you may Test Your Luck to press the attack\n"
            + "  (Lucky: 2 extra damage; Unlucky: the monster recovers 1). When a\n"
            + "  monster wounds you, you may Test Your Luck to soften the blow\n"
            + "  (Lucky: recover 1; Unlucky: lose 1 more). Each test rolls 2 dice: you\n"
            + "  are Lucky if the roll is at most your current LUCK, and every test\n"
            + "  costs you 1 LUCK.\n"
            + "\n"
            + "PROVISIONS & ITEMS\n"
            + "  Eat a provision (outside or during combat) to restore 4 STAMINA.\n"
            + "  Potions found in the dungeon restore STAMINA when used.\n"
            + "\n"
            + "CONTROLS\n"
            + "  Mouse: click reachable rooms and buttons.\n"
            + "  Keyboard: Alt+R Roll, Alt+E End Turn, Alt+P Eat Provision,\n"
            + "  Alt+U Use Item; in combat Alt+A Attack, Alt+L Test Luck.\n"
            + "  Menu: New / Save / Load / How to Play / Exit.\n";

    public HowToPlayDialog(Window owner) {
        super(owner, "How to Play", ModalityType.APPLICATION_MODAL);
        JTextArea area = new JTextArea(TEXT);
        area.setEditable(false);
        area.setBackground(Theme.BG_PANEL);
        area.setForeground(Theme.PARCHMENT);
        area.setFont(Theme.MONO);
        area.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JScrollPane sp = new JScrollPane(area);

        JButton close = new JButton("Close");
        close.setMnemonic('C');
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });

        setLayout(new BorderLayout());
        add(sp, BorderLayout.CENTER);
        add(close, BorderLayout.SOUTH);
        getContentPane().setBackground(Theme.BG_DARK);
        setSize(680, 620);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(close);
    }
}
