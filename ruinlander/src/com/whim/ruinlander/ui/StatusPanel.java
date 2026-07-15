package com.whim.ruinlander.ui;

import com.whim.ruinlander.domain.Armor;
import com.whim.ruinlander.domain.GameStateManager;
import com.whim.ruinlander.domain.Player;
import com.whim.ruinlander.domain.StatType;
import com.whim.ruinlander.domain.Weapon;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.EnumMap;
import java.util.Map;

/** Side dashboard: a {@link JProgressBar} per survival meter, plus equipped gear. */
public class StatusPanel extends JPanel {

    private static final Color BG = new Color(24, 22, 20);
    private static final Color FG = new Color(200, 210, 180);

    private final GameStateManager gsm;
    private final Map<StatType, JProgressBar> bars = new EnumMap<StatType, JProgressBar>(StatType.class);
    private final JLabel weaponLabel = new JLabel();
    private final JLabel armorLabel = new JLabel();
    private final JLabel modeLabel = new JLabel();

    public StatusPanel(GameStateManager gsm) {
        this.gsm = gsm;
        setBackground(BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(240, 480));

        addHeading("SURVIVOR STATUS");
        for (StatType t : StatType.values()) {
            add(buildBar(t));
            add(Box.createVerticalStrut(6));
        }
        add(Box.createVerticalStrut(10));
        addHeading("EQUIPMENT");
        styleInfo(weaponLabel);
        styleInfo(armorLabel);
        styleInfo(modeLabel);
        add(weaponLabel);
        add(armorLabel);
        add(Box.createVerticalStrut(8));
        add(modeLabel);
        refresh();
    }

    private void addHeading(String text) {
        JLabel h = new JLabel(text);
        h.setForeground(new Color(150, 170, 120));
        h.setFont(new Font("SansSerif", Font.BOLD, 13));
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(h);
        add(Box.createVerticalStrut(8));
    }

    private JPanel buildBar(StatType t) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JLabel name = new JLabel(label(t));
        name.setForeground(FG);
        name.setFont(new Font("SansSerif", Font.PLAIN, 11));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        bars.put(t, bar);

        row.add(name);
        row.add(bar);
        return row;
    }

    private void styleInfo(JLabel l) {
        l.setForeground(FG);
        l.setFont(new Font("Monospaced", Font.PLAIN, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private String label(StatType t) {
        switch (t) {
            case HEALTH: return "Health ♥";
            case HUNGER: return "Hunger ⚑";
            case THIRST: return "Thirst ☔";
            case FATIGUE: return "Fatigue ☾";
            case RADIATION: return "Radiation ☢";
            case TEMPERATURE: return "Warmth ♨";
            default: return t.name();
        }
    }

    /** Re-read player state into the bars/labels. Call on the EDT. */
    public void refresh() {
        Player p = gsm.getPlayer();
        for (StatType t : StatType.values()) {
            JProgressBar bar = bars.get(t);
            int v = p.getStat(t);
            bar.setValue(v);
            bar.setString(v + " / " + p.getMaxStat(t));
            bar.setForeground(colorFor(t, v));
        }
        Weapon w = p.getEquippedWeapon();
        weaponLabel.setText("Wpn: " + (w == null ? "(none)" : w.getName()
                + " d" + w.getDamage()));
        Armor a = p.getEquippedArmor();
        armorLabel.setText("Arm: " + (a == null ? "(none)"
                : a.getName() + " -" + Math.round(a.getDamageReduction() * 100) + "%"));
        modeLabel.setText("Mode: " + gsm.getMode());
        modeLabel.setForeground(gsm.getMode() == com.whim.ruinlander.domain.GameMode.GAME_OVER
                ? new Color(220, 80, 70) : FG);
    }

    /** Higher-is-worse meters flip to red as they rise; health/warmth redden as they fall. */
    private Color colorFor(StatType t, int v) {
        boolean higherIsBad = (t == StatType.HUNGER || t == StatType.THIRST
                || t == StatType.FATIGUE || t == StatType.RADIATION);
        int severity = higherIsBad ? v : (100 - v);
        if (severity >= 75) return new Color(210, 70, 60);
        if (severity >= 45) return new Color(210, 160, 60);
        return new Color(90, 170, 90);
    }
}
