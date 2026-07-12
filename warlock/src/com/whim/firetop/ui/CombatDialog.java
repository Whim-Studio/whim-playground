package com.whim.firetop.ui;

import com.whim.firetop.engine.Combat;
import com.whim.firetop.engine.Dice;
import com.whim.firetop.model.Character;
import com.whim.firetop.model.Monster;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Modal combat screen. The player resolves attack rounds; after wounding or being
 * wounded, a "Test Your Luck" button becomes available at the correct moment.
 * The dialog closes when the monster is defeated or the adventurer falls.
 */
public final class CombatDialog extends JDialog {

    /** Terminal result of the fight. */
    public enum Result { PLAYER_WON, PLAYER_DIED }

    private enum PendingLuck { NONE, ATTACK, DEFENSE }

    private final Character player;
    private final Monster monster;
    private final Dice dice;

    private Result result = Result.PLAYER_DIED;
    private PendingLuck pending = PendingLuck.NONE;

    private final JLabel playerAsLabel = new JLabel("—", SwingConstants.CENTER);
    private final JLabel monsterAsLabel = new JLabel("—", SwingConstants.CENTER);
    private final JLabel playerStamLabel = new JLabel();
    private final JLabel monsterStamLabel = new JLabel();
    private final JTextArea logArea = new JTextArea(12, 34);
    private final JButton attackButton = new JButton("Attack Round");
    private final JButton luckButton = new JButton("Test Your Luck");
    private final JButton provisionButton = new JButton("Eat Provision");

    public CombatDialog(Window owner, Character player, Monster monster, Dice dice) {
        super(owner, "Combat — " + monster.getName(), ModalityType.APPLICATION_MODAL);
        this.player = player;
        this.monster = monster;
        this.dice = dice;
        build();
        appendLog("A " + monster.getName() + " blocks your path!");
        appendLog(monster.getDescription());
        appendLog("");
        updateStamina();
        setSize(560, 520);
        setLocationRelativeTo(owner);
    }

    public Result getResult() { return result; }

    private void build() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(Theme.BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel arena = new JPanel(new GridLayout(1, 3, 8, 0));
        arena.setOpaque(false);
        arena.add(fighterCard(player.getName(), Theme.ROYAL, playerAsLabel, playerStamLabel));
        JLabel vs = new JLabel("VS", SwingConstants.CENTER);
        vs.setForeground(Theme.GOLD);
        vs.setFont(Theme.TITLE);
        arena.add(vs);
        arena.add(fighterCard(monster.getName(), Theme.BLOOD, monsterAsLabel, monsterStamLabel));
        root.add(arena, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setBackground(Theme.BG_PANEL);
        logArea.setForeground(Theme.PARCHMENT);
        logArea.setFont(Theme.MONO);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(BorderFactory.createLineBorder(Theme.STONE));
        root.add(sp, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        buttons.setOpaque(false);
        style(attackButton, Theme.EMERALD);
        style(luckButton, Theme.GOLD);
        style(provisionButton, Theme.STONE);
        attackButton.setMnemonic('A');
        luckButton.setMnemonic('L');
        provisionButton.setMnemonic('P');
        luckButton.setEnabled(false);
        buttons.add(attackButton);
        buttons.add(luckButton);
        buttons.add(provisionButton);
        root.add(buttons, BorderLayout.SOUTH);

        attackButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doRound(); }
        });
        luckButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doLuck(); }
        });
        provisionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doProvision(); }
        });

        getRootPane().setDefaultButton(attackButton);
        setContentPane(root);
    }

    private JPanel fighterCard(String name, Color accent, JLabel asLabel, JLabel stamLabel) {
        JPanel p = new JPanel(new BorderLayout(2, 2));
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(BorderFactory.createLineBorder(accent, 2));
        JLabel title = new JLabel(name, SwingConstants.CENTER);
        title.setForeground(Theme.PARCHMENT);
        title.setFont(Theme.BODY_BOLD);
        p.add(title, BorderLayout.NORTH);
        asLabel.setForeground(accent);
        asLabel.setFont(new Font("Serif", Font.BOLD, 34));
        p.add(asLabel, BorderLayout.CENTER);
        stamLabel.setForeground(Theme.STONE_LIGHT);
        stamLabel.setHorizontalAlignment(SwingConstants.CENTER);
        stamLabel.setFont(Theme.BODY);
        p.add(stamLabel, BorderLayout.SOUTH);
        return p;
    }

    private void style(JButton b, Color c) {
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(true);
        b.setFont(Theme.BODY_BOLD);
    }

    private void doRound() {
        pending = PendingLuck.NONE;
        luckButton.setEnabled(false);
        Combat.RoundResult r = Combat.resolveRound(player, monster, dice);
        playerAsLabel.setText(String.valueOf(r.getPlayerAttackStrength()));
        monsterAsLabel.setText(String.valueOf(r.getMonsterAttackStrength()));
        String head = "Round: you " + r.getPlayerAttackStrength()
                + " vs " + monster.getName() + " " + r.getMonsterAttackStrength() + " — ";
        switch (r.getOutcome()) {
            case PLAYER_WINS:
                appendLog(head + "you land a blow! (-2 STAMINA)");
                pending = PendingLuck.ATTACK;
                break;
            case MONSTER_WINS:
                appendLog(head + "it wounds you! (-2 STAMINA)");
                pending = PendingLuck.DEFENSE;
                break;
            default:
                appendLog(head + "blades clash, no damage.");
                break;
        }
        updateStamina();
        if (checkEnd()) {
            return;
        }
        if (pending != PendingLuck.NONE && player.getLuckCurrent() > 0) {
            luckButton.setEnabled(true);
            luckButton.setText(pending == PendingLuck.ATTACK
                    ? "Test Luck (press attack)" : "Test Luck (soften blow)");
        } else {
            luckButton.setText("Test Your Luck");
        }
    }

    private void doLuck() {
        if (pending == PendingLuck.ATTACK) {
            boolean lucky = Combat.applyLuckToAttack(player, monster, dice);
            appendLog(lucky ? "  LUCKY — a telling strike! (2 extra damage)"
                    : "  UNLUCKY — only a graze. (monster recovers 1)");
        } else if (pending == PendingLuck.DEFENSE) {
            boolean lucky = Combat.applyLuckToDefense(player, dice);
            appendLog(lucky ? "  LUCKY — you roll with it. (recover 1 STAMINA)"
                    : "  UNLUCKY — it hits home. (lose 1 more STAMINA)");
        }
        pending = PendingLuck.NONE;
        luckButton.setEnabled(false);
        luckButton.setText("Test Your Luck");
        updateStamina();
        checkEnd();
    }

    private void doProvision() {
        if (player.eatProvision(com.whim.firetop.engine.GameEngine.PROVISION_HEAL)) {
            appendLog("You eat a provision (+" + com.whim.firetop.engine.GameEngine.PROVISION_HEAL
                    + " STAMINA). Provisions left: " + player.getProvisions());
            updateStamina();
        } else {
            appendLog("No provisions left to eat.");
        }
    }

    private boolean checkEnd() {
        if (monster.isDefeated()) {
            appendLog("");
            appendLog("The " + monster.getName() + " is slain!");
            result = Result.PLAYER_WON;
            finish();
            return true;
        }
        if (!player.isAlive()) {
            appendLog("");
            appendLog(player.getName() + " falls...");
            result = Result.PLAYER_DIED;
            finish();
            return true;
        }
        return false;
    }

    private void finish() {
        attackButton.setEnabled(false);
        luckButton.setEnabled(false);
        provisionButton.setEnabled(false);
        JButton close = new JButton("Continue");
        style(close, Theme.ROYAL);
        close.setMnemonic('C');
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });
        JPanel south = (JPanel) ((BorderLayout) ((JPanel) getContentPane()).getLayout())
                .getLayoutComponent(BorderLayout.SOUTH);
        south.removeAll();
        south.add(close);
        south.revalidate();
        south.repaint();
        getRootPane().setDefaultButton(close);
    }

    private void updateStamina() {
        playerStamLabel.setText("STAMINA " + player.getStaminaCurrent() + "/" + player.getStaminaInitial()
                + "   LUCK " + player.getLuckCurrent());
        monsterStamLabel.setText("STAMINA " + monster.getStamina() + "/" + monster.getMaxStamina()
                + "   SKILL " + monster.getSkill());
    }

    private void appendLog(String s) {
        logArea.append(s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
