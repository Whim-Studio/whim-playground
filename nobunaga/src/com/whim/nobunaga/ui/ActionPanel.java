package com.whim.nobunaga.ui;

import com.whim.nobunaga.domain.BattleState;
import com.whim.nobunaga.domain.GameState;
import com.whim.nobunaga.domain.Province;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * SOUTH component: the action bar. Each button routes through
 * {@link GameController} to the matching engine / loop call, appends the returned
 * one-line result(s) to a scrolling log, and asks the {@link GameFrame} to
 * refresh. <b>War</b> on an adjacent enemy/neutral province launches a
 * {@link BattlePanel}.
 */
public final class ActionPanel extends JPanel {

    private final GameController controller;
    private final GameFrame frame;
    private final JTextArea log;

    public ActionPanel(GameController controller, GameFrame frame) {
        this.controller = controller;
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(new Color(28, 30, 36));
        setPreferredSize(new Dimension(1100, 130));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        buttons.setBackground(new Color(28, 30, 36));
        buttons.add(action("Tax", new ActionListener() {
            public void actionPerformed(ActionEvent e) { doTax(); }
        }));
        buttons.add(action("Cultivate", new ActionListener() {
            public void actionPerformed(ActionEvent e) { doCultivate(); }
        }));
        buttons.add(action("Flood Control", new ActionListener() {
            public void actionPerformed(ActionEvent e) { doFlood(); }
        }));
        buttons.add(action("Recruit", new ActionListener() {
            public void actionPerformed(ActionEvent e) { doRecruit(); }
        }));
        buttons.add(action("Transfer", new ActionListener() {
            public void actionPerformed(ActionEvent e) { doTransfer(); }
        }));
        buttons.add(action("War", new ActionListener() {
            public void actionPerformed(ActionEvent e) { doWar(); }
        }));
        buttons.add(Box.createHorizontalStrut(16));
        buttons.add(action("End Season", new ActionListener() {
            public void actionPerformed(ActionEvent e) { doEndSeason(); }
        }));

        log = new JTextArea(4, 60);
        log.setEditable(false);
        log.setFont(new Font("Monospaced", Font.PLAIN, 12));
        log.setBackground(new Color(16, 18, 22));
        log.setForeground(new Color(200, 220, 200));
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(60, 70, 60)));

        add(buttons, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private JButton action(String label, ActionListener l) {
        JButton b = new JButton(label);
        b.setFocusable(false);
        b.addActionListener(l);
        return b;
    }

    // --- Action handlers ---------------------------------------------------

    private Province requireSelection() {
        Province p = controller.selectedProvince();
        if (p == null) {
            emit("Select a province first.");
        }
        return p;
    }

    private void doTax() {
        Province p = requireSelection();
        if (p == null) {
            return;
        }
        String input = JOptionPane.showInputDialog(this,
                "New tax rate (0-100) for " + p.getName() + ":",
                String.valueOf(p.getTaxRate()));
        Integer rate = parse(input);
        if (rate == null) {
            return;
        }
        emit(controller.setTax(p.getId(), rate.intValue()));
        frame.refresh();
    }

    private void doCultivate() {
        Province p = requireSelection();
        if (p == null) {
            return;
        }
        emit(controller.cultivate(p.getId()));
        frame.refresh();
    }

    private void doFlood() {
        Province p = requireSelection();
        if (p == null) {
            return;
        }
        emit(controller.floodControl(p.getId()));
        frame.refresh();
    }

    private void doRecruit() {
        Province p = requireSelection();
        if (p == null) {
            return;
        }
        String input = JOptionPane.showInputDialog(this,
                "Soldiers to recruit in " + p.getName() + ":", "100");
        Integer n = parse(input);
        if (n == null) {
            return;
        }
        emit(controller.recruit(p.getId(), n.intValue()));
        frame.refresh();
    }

    private void doTransfer() {
        Province from = requireSelection();
        if (from == null) {
            return;
        }
        List<Province> targets = ownedAdjacent(from);
        if (targets.isEmpty()) {
            emit("No friendly adjacent province to transfer to.");
            return;
        }
        Province to = chooseProvince("Transfer to which province?", targets);
        if (to == null) {
            return;
        }
        Integer gold = parse(JOptionPane.showInputDialog(this, "Gold to send:", "0"));
        if (gold == null) {
            return;
        }
        Integer rice = parse(JOptionPane.showInputDialog(this, "Rice to send:", "0"));
        if (rice == null) {
            return;
        }
        Integer sol = parse(JOptionPane.showInputDialog(this, "Soldiers to send:", "0"));
        if (sol == null) {
            return;
        }
        emit(controller.transfer(from.getId(), to.getId(),
                gold.intValue(), rice.intValue(), sol.intValue()));
        frame.refresh();
    }

    private void doWar() {
        Province target = requireSelection();
        if (target == null) {
            return;
        }
        GameState s = controller.state();
        if (target.getOwnerId() == s.playerDaimyoId) {
            emit("Cannot attack your own province.");
            return;
        }
        List<Province> sources = playerAdjacent(target);
        if (sources.isEmpty()) {
            emit("No adjacent province of yours borders " + target.getName() + ".");
            return;
        }
        Province source = chooseProvince("Attack from which province?", sources);
        if (source == null) {
            return;
        }
        Integer soldiers = parse(JOptionPane.showInputDialog(this,
                "Soldiers to commit from " + source.getName()
                        + " (have " + source.getSoldiers() + "):",
                String.valueOf(source.getSoldiers())));
        if (soldiers == null) {
            return;
        }
        Integer rice = parse(JOptionPane.showInputDialog(this,
                "Rice (supplies) to carry (have " + source.getRice() + "):",
                String.valueOf(Math.min(source.getRice(), 300))));
        if (rice == null) {
            return;
        }
        BattleState battle = controller.startBattle(source.getId(), target.getId(),
                soldiers.intValue(), rice.intValue());
        if (battle == null) {
            emit("The attack could not be launched.");
            return;
        }
        emit("War! " + source.getName() + " marches on " + target.getName() + ".");
        BattlePanel.fight(frame, controller, battle);
        frame.refresh();
    }

    private void doEndSeason() {
        List<String> events = controller.endSeason();
        emit("=== " + controller.header() + " ===");
        if (events == null || events.isEmpty()) {
            emit("A quiet season passes.");
        } else {
            for (String line : events) {
                emit(line);
            }
        }
        frame.refresh();
    }

    // --- Helpers -----------------------------------------------------------

    private List<Province> ownedAdjacent(Province from) {
        GameState s = controller.state();
        List<Province> out = new ArrayList<Province>();
        for (Integer id : from.getAdjacent()) {
            Province q = s.province(id.intValue());
            if (q.getOwnerId() == s.playerDaimyoId) {
                out.add(q);
            }
        }
        return out;
    }

    private List<Province> playerAdjacent(Province target) {
        GameState s = controller.state();
        List<Province> out = new ArrayList<Province>();
        for (Integer id : target.getAdjacent()) {
            Province q = s.province(id.intValue());
            if (q.getOwnerId() == s.playerDaimyoId && q.getSoldiers() > 0) {
                out.add(q);
            }
        }
        return out;
    }

    private Province chooseProvince(String prompt, List<Province> options) {
        String[] labels = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            Province p = options.get(i);
            labels[i] = p.getName() + " (" + p.getSoldiers() + " sol)";
        }
        JComboBox<String> combo = new JComboBox<String>(labels);
        int ok = JOptionPane.showConfirmDialog(this, combo, prompt,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return null;
        }
        return options.get(combo.getSelectedIndex());
    }

    private Integer parse(String input) {
        if (input == null) {
            return null; // cancelled
        }
        try {
            return Integer.valueOf(Integer.parseInt(input.trim()));
        } catch (NumberFormatException ex) {
            emit("'" + input + "' is not a whole number.");
            return null;
        }
    }

    private void emit(String message) {
        if (message == null || message.length() == 0) {
            return;
        }
        log.append(message + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }
}
