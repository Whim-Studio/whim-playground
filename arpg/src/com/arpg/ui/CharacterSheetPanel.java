package com.arpg.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.arpg.model.Character;
import com.arpg.model.CharacterClass;
import com.arpg.model.Equipment;
import com.arpg.model.EquipmentSlot;

/**
 * Character sheet: identity, resource bars, primary combat stats, the equipped
 * loadout and attribute-allocation controls. Rebuilt from a {@link Character}
 * on every refresh so it always mirrors the latest snapshot.
 */
public class CharacterSheetPanel extends JPanel {

    /** Attribute buttons offered for allocation. The engine validates/ignores. */
    private static final String[] ATTRIBUTES = {"Strength", "Dexterity", "Intelligence", "Vitality"};

    private final ActionSink sink;

    private final JLabel nameLabel = new JLabel();
    private final JLabel classLabel = new JLabel();
    private final JLabel currencyLabel = new JLabel();
    private final StatBar hpBar = new StatBar(UiTheme.HP_BAR);
    private final StatBar resourceBar = new StatBar(UiTheme.RESOURCE_BAR);
    private final StatBar xpBar = new StatBar(UiTheme.XP_BAR);
    private final JLabel attackLabel = new JLabel();
    private final JLabel defenseLabel = new JLabel();
    private final JPanel equipmentPanel = new JPanel();

    public CharacterSheetPanel(ActionSink sink) {
        super(new BorderLayout(8, 8));
        this.sink = sink;
        setBackground(UiTheme.BG_PANEL);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildAttributeControls(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        style(nameLabel, UiTheme.TITLE, UiTheme.FG_TEXT);
        style(classLabel, UiTheme.BODY, UiTheme.FG_MUTED);
        style(currencyLabel, UiTheme.BODY_BOLD, UiTheme.LOOT);
        header.add(nameLabel);
        header.add(classLabel);
        header.add(currencyLabel);
        return header;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        center.add(labeled("Health", hpBar));
        center.add(Box.createVerticalStrut(4));
        center.add(labeled("Resource", resourceBar));
        center.add(Box.createVerticalStrut(4));
        center.add(labeled("XP", xpBar));
        center.add(Box.createVerticalStrut(8));

        JPanel stats = new JPanel(new GridLayout(1, 2, 6, 0));
        stats.setOpaque(false);
        style(attackLabel, UiTheme.BODY_BOLD, UiTheme.FG_TEXT);
        style(defenseLabel, UiTheme.BODY_BOLD, UiTheme.FG_TEXT);
        stats.add(attackLabel);
        stats.add(defenseLabel);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        center.add(stats);
        center.add(Box.createVerticalStrut(8));

        JLabel eqTitle = new JLabel("Equipped");
        style(eqTitle, UiTheme.BODY_BOLD, UiTheme.ACCENT);
        center.add(eqTitle);
        equipmentPanel.setOpaque(false);
        equipmentPanel.setLayout(new BoxLayout(equipmentPanel, BoxLayout.Y_AXIS));
        center.add(equipmentPanel);
        center.add(Box.createVerticalGlue());
        return center;
    }

    private JPanel buildAttributeControls() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        JLabel title = new JLabel("Allocate attribute point");
        style(title, UiTheme.BODY, UiTheme.FG_MUTED);
        wrap.add(title, BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        row.setOpaque(false);
        for (int i = 0; i < ATTRIBUTES.length; i++) {
            final String attr = ATTRIBUTES[i];
            JButton b = new JButton("+ " + attr);
            b.setFont(UiTheme.BODY);
            b.setFocusable(false);
            b.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    sink.submit(UiActions.allocateAttribute(attr));
                }
            });
            row.add(b);
        }
        wrap.add(row, BorderLayout.CENTER);
        return wrap;
    }

    /** Refresh all widgets from the given player character. */
    public void update(Character player) {
        if (player == null) {
            return;
        }
        nameLabel.setText(player.getName() + "   (Lv " + player.getLevel() + ")");
        CharacterClass clazz = player.getCharacterClass();
        classLabel.setText(clazz == null ? "" : clazz.getName());
        currencyLabel.setText("Gold: " + player.getCurrency());

        hpBar.setValues(player.getCurrentHealth(), player.getMaxHealth());
        resourceBar.setValues(player.getCurrentResource(), player.getMaxResource());
        xpBar.setValues(player.getXp(), xpForNext(player));

        attackLabel.setText("Attack: " + player.getAttackPower());
        defenseLabel.setText("Defense: " + player.getDefense());

        rebuildEquipment(player);
        revalidate();
        repaint();
    }

    private void rebuildEquipment(Character player) {
        equipmentPanel.removeAll();
        Map<EquipmentSlot, Equipment> equipped = player.getEquipped();
        EquipmentSlot[] slots = EquipmentSlot.values();
        for (int i = 0; i < slots.length; i++) {
            EquipmentSlot slot = slots[i];
            Equipment item = equipped == null ? null : equipped.get(slot);
            equipmentPanel.add(equipmentRow(slot, item));
        }
    }

    private JPanel equipmentRow(EquipmentSlot slot, final Equipment item) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel slotLabel = new JLabel(pretty(slot.name()));
        style(slotLabel, UiTheme.BODY, UiTheme.FG_MUTED);
        slotLabel.setPreferredSize(new Dimension(80, 20));
        row.add(slotLabel, BorderLayout.WEST);

        if (item == null) {
            JLabel empty = new JLabel("— empty —");
            style(empty, UiTheme.BODY, new Color(0x66666E));
            row.add(empty, BorderLayout.CENTER);
        } else {
            JLabel nameLbl = new JLabel(item.getName());
            style(nameLbl, UiTheme.BODY_BOLD, UiTheme.rarityColor(item.getRarity()));
            row.add(nameLbl, BorderLayout.CENTER);

            JButton unequip = new JButton("Unequip");
            unequip.setFont(UiTheme.BODY);
            unequip.setFocusable(false);
            unequip.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    sink.submit(UiActions.unequipItem(item.getId()));
                }
            });
            row.add(unequip, BorderLayout.EAST);
        }
        return row;
    }

    /**
     * Best-effort "XP needed for next level". The contract does not expose a
     * threshold getter, so we use a simple, monotonic curve purely for the bar's
     * visual fill; it never drives game logic.
     */
    private long xpForNext(Character player) {
        int lvl = Math.max(1, player.getLevel());
        return 100L * lvl * lvl;
    }

    private JPanel labeled(String text, StatBar bar) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel l = new JLabel(text);
        style(l, UiTheme.BODY, UiTheme.FG_MUTED);
        l.setPreferredSize(new Dimension(70, 18));
        p.add(l, BorderLayout.WEST);
        p.add(bar, BorderLayout.CENTER);
        return p;
    }

    private static void style(JLabel l, java.awt.Font f, Color c) {
        l.setFont(f);
        l.setForeground(c);
        l.setHorizontalAlignment(SwingConstants.LEFT);
    }

    private static String pretty(String enumName) {
        String lower = enumName.toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder(lower.length());
        boolean up = true;
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            // java.lang.Character is fully qualified because com.arpg.model.Character is imported.
            sb.append(up ? java.lang.Character.toUpperCase(ch) : ch);
            up = ch == ' ';
        }
        return sb.toString();
    }
}
