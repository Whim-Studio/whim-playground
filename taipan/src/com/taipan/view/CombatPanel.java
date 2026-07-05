package com.taipan.view;

import com.taipan.controller.CombatSession;
import com.taipan.model.GameState;
import com.taipan.model.Good;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/** Interactive pirate battle screen. */
public class CombatPanel extends JPanel {

    private final GameFrame frame;

    private final JLabel status = new JLabel();
    private final JTextArea log = new JTextArea();
    private final JButton fight = new JButton("Fight");
    private final JButton run = new JButton("Run");
    private final JButton throwCargo = new JButton("Throw Cargo");
    private final JButton cont = new JButton("Continue");

    private CombatSession session;

    public CombatPanel(GameFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel title = new JLabel("Pirates attack!");
        title.setFont(new Font("Serif", Font.BOLD, 26));
        JPanel north = new JPanel(new BorderLayout());
        north.add(title, BorderLayout.NORTH);
        status.setFont(new Font("Monospaced", Font.PLAIN, 13));
        north.add(status, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(new Font("Monospaced", Font.PLAIN, 13));
        add(new JScrollPane(log), BorderLayout.CENTER);

        fight.addActionListener(e -> act(session.fight()));
        run.addActionListener(e -> act(session.run()));
        throwCargo.addActionListener(e -> doThrow());
        cont.addActionListener(e -> {
            if (session.isPlayerSunk()) {
                frame.showEnd();
            } else {
                frame.finishVoyage();
            }
        });

        JPanel south = new JPanel(new GridLayout(1, 4, 8, 0));
        south.add(fight);
        south.add(run);
        south.add(throwCargo);
        south.add(cont);
        add(south, BorderLayout.SOUTH);
    }

    public void begin(CombatSession session) {
        this.session = session;
        log.setText("");
        appendLog("You are set upon by " + session.getEnemyShips() + " hostile ship(s)!");
        updateControls();
    }

    private void act(List<String> lines) {
        for (String l : lines) {
            appendLog(l);
        }
        updateControls();
    }

    private void doThrow() {
        GameState s = frame.getController().getState();
        List<Good> have = new ArrayList<Good>();
        for (Good g : Good.values()) {
            if (s.getShip().getCargo(g) > 0) {
                have.add(g);
            }
        }
        if (have.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You have no cargo to throw overboard.",
                    "Throw Cargo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Good[] arr = have.toArray(new Good[0]);
        Good chosen = (Good) JOptionPane.showInputDialog(this, "Throw which cargo overboard?",
                "Throw Cargo", JOptionPane.QUESTION_MESSAGE, null, arr, arr[0]);
        if (chosen == null) {
            return;
        }
        String in = JOptionPane.showInputDialog(this,
                "How many units of " + chosen.display() + " (you have "
                        + s.getShip().getCargo(chosen) + ")?");
        if (in == null) {
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(in.trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "That was not a number.", "Throw Cargo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (qty <= 0) {
            return;
        }
        act(session.throwCargo(chosen, qty));
    }

    private void appendLog(String line) {
        log.append(line + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void updateControls() {
        GameState s = frame.getController().getState();
        status.setText("  Enemy ships: " + session.getEnemyShips()
                + "     Your guns: " + s.getShip().getGuns()
                + "     Hull damage: " + s.getShip().getDamage() + "%");

        boolean active = !session.isOver();
        fight.setEnabled(active);
        run.setEnabled(active);
        throwCargo.setEnabled(active);
        cont.setEnabled(!active);

        if (session.isOver() && !session.isPlayerSunk()) {
            if (session.isVictory()) {
                appendLog("Victory! The sea is yours.");
            } else if (session.isEscaped()) {
                appendLog("You have escaped. Press Continue to sail on.");
            }
        }
    }
}
