package com.arpg.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.arpg.model.BuffDebuff;
import com.arpg.model.Enemy;
import com.arpg.model.GameStateSnapshot;
import com.arpg.model.Pet;

/**
 * Combat arena: shows the current realm, each enemy as a selectable card with a
 * health bar and buff row, and the active pet. Selecting an enemy sets the
 * target index used by the ability bar.
 */
public class CombatViewPanel extends JPanel {

    private final AbilityBarPanel abilityBar;

    private final JLabel realmLabel = new JLabel();
    private final JLabel stateLabel = new JLabel();
    private final JPanel enemyPanel = new JPanel();
    private final JPanel petPanel = new JPanel(new BorderLayout(6, 0));
    private final StatBar petHp = new StatBar(UiTheme.HP_BAR);
    private final JLabel petLabel = new JLabel();

    private int selectedIndex = 0;

    public CombatViewPanel(AbilityBarPanel abilityBar) {
        super(new BorderLayout(8, 8));
        this.abilityBar = abilityBar;
        setBackground(UiTheme.BG_PANEL);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildHeader(), BorderLayout.NORTH);

        enemyPanel.setOpaque(false);
        enemyPanel.setLayout(new BoxLayout(enemyPanel, BoxLayout.Y_AXIS));
        add(enemyPanel, BorderLayout.CENTER);

        add(buildPetPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setOpaque(false);
        realmLabel.setFont(UiTheme.TITLE);
        realmLabel.setForeground(UiTheme.FG_TEXT);
        stateLabel.setFont(UiTheme.BODY);
        stateLabel.setForeground(UiTheme.FG_MUTED);
        header.add(realmLabel);
        header.add(stateLabel);
        return header;
    }

    private JPanel buildPetPanel() {
        petPanel.setOpaque(false);
        petPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiTheme.BG_SLOT), "Companion"));
        petLabel.setFont(UiTheme.BODY_BOLD);
        petLabel.setForeground(UiTheme.HEAL);
        petLabel.setPreferredSize(new Dimension(140, 20));
        petPanel.add(petLabel, BorderLayout.WEST);
        petPanel.add(petHp, BorderLayout.CENTER);
        return petPanel;
    }

    /** Refresh from a full snapshot. */
    public void update(GameStateSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        if (snapshot.getCurrentRealm() != null) {
            realmLabel.setText(snapshot.getCurrentRealm().getName()
                    + "   (Tier " + snapshot.getCurrentRealm().getDifficultyTier() + ")");
        } else {
            realmLabel.setText("Wilderness");
        }
        stateLabel.setText(snapshot.isInCombat() ? "In combat" : "Explore — advance to find enemies");

        rebuildEnemies(snapshot.getEnemies());
        updatePet(snapshot.getActivePet());
        revalidate();
        repaint();
    }

    private void rebuildEnemies(List<Enemy> enemies) {
        enemyPanel.removeAll();
        if (enemies == null || enemies.isEmpty()) {
            JLabel none = new JLabel("No enemies present.");
            none.setForeground(UiTheme.FG_MUTED);
            none.setFont(UiTheme.BODY);
            enemyPanel.add(none);
            selectedIndex = 0;
            abilityBar.setTargetIndex(0);
            return;
        }
        if (selectedIndex >= enemies.size()) {
            selectedIndex = 0;
        }
        for (int i = 0; i < enemies.size(); i++) {
            enemyPanel.add(enemyCard(enemies.get(i), i));
            enemyPanel.add(Box.createVerticalStrut(6));
        }
        abilityBar.setTargetIndex(selectedIndex);
    }

    private JPanel enemyCard(final Enemy enemy, final int index) {
        JPanel card = new JPanel(new BorderLayout(6, 2));
        card.setOpaque(true);
        card.setBackground(index == selectedIndex ? UiTheme.BG_SLOT : UiTheme.BG_DARK);
        boolean boss = isBoss(enemy);
        Color borderColor = boss ? UiTheme.LOOT : (index == selectedIndex ? UiTheme.ACCENT : UiTheme.BG_SLOT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, index == selectedIndex ? 2 : 1),
                new EmptyBorder(6, 8, 6, 8)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        JLabel name = new JLabel((boss ? "★ " : "") + enemy.getName()
                + (enemy.isAlive() ? "" : "  (defeated)"));
        name.setFont(UiTheme.BODY_BOLD);
        name.setForeground(boss ? UiTheme.LOOT : UiTheme.FG_TEXT);
        card.add(name, BorderLayout.NORTH);

        StatBar hp = new StatBar(UiTheme.HP_BAR);
        hp.setValues(enemy.getCurrentHealth(), enemy.getMaxHealth());
        card.add(hp, BorderLayout.CENTER);

        String buffs = describeBuffs(enemy.getActiveBuffs());
        if (buffs.length() > 0) {
            JLabel buffLbl = new JLabel(buffs);
            buffLbl.setFont(UiTheme.BODY);
            buffLbl.setForeground(UiTheme.BUFF);
            card.add(buffLbl, BorderLayout.SOUTH);
        }

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                selectedIndex = index;
                abilityBar.setTargetIndex(index);
                // Repaint selection highlight.
                for (int i = 0; i < enemyPanel.getComponentCount(); i++) {
                    enemyPanel.getComponent(i).repaint();
                }
                revalidate();
                repaint();
            }
        });
        return card;
    }

    private void updatePet(Pet pet) {
        if (pet == null) {
            petLabel.setText("— none —");
            petLabel.setForeground(UiTheme.FG_MUTED);
            petHp.setValues(0, 1);
            return;
        }
        petLabel.setText(pet.getName());
        petLabel.setForeground(UiTheme.HEAL);
        petHp.setValues(pet.getCurrentHealth(), pet.getMaxHealth());
    }

    private String describeBuffs(List<BuffDebuff> buffs) {
        if (buffs == null || buffs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buffs.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            BuffDebuff b = buffs.get(i);
            sb.append(b.getName()).append(" (").append(b.getDurationTicks()).append(")");
        }
        return sb.toString();
    }

    /**
     * Reads the boss flag. The contract names a "boss flag getter" but not its
     * exact name, so we probe the two most likely spellings reflectively and
     * default to false. Consolidation can replace this with a direct call.
     */
    private boolean isBoss(Enemy enemy) {
        try {
            java.lang.reflect.Method m = enemy.getClass().getMethod("isBoss");
            Object r = m.invoke(enemy);
            return r instanceof Boolean && ((Boolean) r).booleanValue();
        } catch (Exception ignored) {
            // fall through
        }
        try {
            java.lang.reflect.Method m = enemy.getClass().getMethod("getBoss");
            Object r = m.invoke(enemy);
            return r instanceof Boolean && ((Boolean) r).booleanValue();
        } catch (Exception ignored) {
            return false;
        }
    }
}
