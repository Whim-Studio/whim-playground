package com.tiwas.rpg.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.tiwas.rpg.domain.AdvancedSkill;
import com.tiwas.rpg.domain.AttributeCode;
import com.tiwas.rpg.domain.Character;
import com.tiwas.rpg.domain.Skill;
import com.tiwas.rpg.engine.Dice;

/**
 * Modal forge for a player-driven Epiphany. Pops when a failed doubles roll
 * unlocks an Advanced Skill; lets the player name it, pick the one additional
 * attribute, and choose the starting value method. On OK it builds the skill
 * via {@link AdvancedSkill}; on Cancel nothing is created.
 */
public final class AdvancedSkillDialog extends JDialog {

    private final Character character;
    private final Skill base;
    private final Dice dice;
    private final int newTier;

    private final JTextField nameField;
    private final JComboBox<AttributeCode> attributeCombo;
    private final JRadioButton level1Radio = new JRadioButton("Start at Level 1", true);
    private final JRadioButton randomRadio = new JRadioButton("Random roll (capped at new max)");
    private final JLabel tierLabel = new JLabel();
    private final JLabel capLabel = new JLabel();

    private Skill created; // null until OK

    public AdvancedSkillDialog(Window owner, Character character, Skill base, Dice dice) {
        super(owner, "Epiphany! Forge an Advanced Skill", ModalityType.APPLICATION_MODAL);
        this.character = character;
        this.base = base;
        this.dice = dice == null ? new Dice() : dice;
        this.newTier = base.getTier() + 1;

        this.nameField = new JTextField(base.getName() + " Mastery", 18);
        this.attributeCombo = new JComboBox<AttributeCode>(buildAttributeModel());
        this.attributeCombo.setRenderer(new AttrRenderer());

        buildUi();
        refreshProjection();
        pack();
        setLocationRelativeTo(owner);
    }

    /** The forged skill, or null if the player cancelled. */
    public Skill getCreatedSkill() {
        return created;
    }

    private DefaultComboBoxModel<AttributeCode> buildAttributeModel() {
        DefaultComboBoxModel<AttributeCode> model = new DefaultComboBoxModel<AttributeCode>();
        for (AttributeCode a : AttributeCode.values()) {
            if (!base.getAttributeCodes().contains(a.code())) {
                model.addElement(a);
            }
        }
        return model;
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        form.add(new JLabel("A failed doubles roll on \"" + base.getName()
                + "\" sparked an Epiphany."));

        JPanel nameRow = new JPanel(new BorderLayout(6, 0));
        nameRow.add(new JLabel("Skill name:"), BorderLayout.WEST);
        nameRow.add(nameField, BorderLayout.CENTER);
        form.add(nameRow);

        JPanel attrRow = new JPanel(new BorderLayout(6, 0));
        attrRow.add(new JLabel("Add 1 attribute:"), BorderLayout.WEST);
        attrRow.add(attributeCombo, BorderLayout.CENTER);
        form.add(attrRow);
        attributeCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshProjection();
            }
        });

        JPanel startPanel = new JPanel(new GridLayout(0, 1));
        startPanel.setBorder(BorderFactory.createTitledBorder("Starting value"));
        ButtonGroup group = new ButtonGroup();
        group.add(level1Radio);
        group.add(randomRadio);
        startPanel.add(level1Radio);
        startPanel.add(randomRadio);
        form.add(startPanel);

        tierLabel.setHorizontalAlignment(SwingConstants.LEFT);
        capLabel.setHorizontalAlignment(SwingConstants.LEFT);
        form.add(tierLabel);
        form.add(capLabel);

        JPanel buttons = new JPanel();
        JButton ok = new JButton("Forge");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                created = null;
                dispose();
            }
        });
        buttons.add(ok);
        buttons.add(cancel);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setMinimumSize(new Dimension(360, 0));
    }

    /** Recompute and show the projected Tier and cap for the current attribute pick. */
    private void refreshProjection() {
        AttributeCode extra = (AttributeCode) attributeCombo.getSelectedItem();
        int cap = projectedCap(extra);
        tierLabel.setText("New Tier: " + newTier);
        capLabel.setText("Projected max (cap): " + cap
                + (extra == null ? "" : "   [" + base.getName() + " + " + extra.fullName() + "]"));
    }

    /** sum(base attrs + chosen extra) / newTier, rounded down — mirrors Skill.maxCap. */
    private int projectedCap(AttributeCode extra) {
        int sum = 0;
        for (String code : base.getAttributeCodes()) {
            sum += character.getAttribute(AttributeCode.fromCode(code));
        }
        if (extra != null) {
            sum += character.getAttribute(extra);
        }
        int tier = newTier <= 0 ? 1 : newTier;
        return sum / tier;
    }

    private void onOk() {
        AttributeCode extra = (AttributeCode) attributeCombo.getSelectedItem();
        String extraCode = extra == null ? null : extra.code();
        int cap = projectedCap(extra);

        int startValue;
        if (randomRadio.isSelected()) {
            int rolled = dice.d100();           // roll up to the new max...
            startValue = rolled > cap ? cap : rolled; // ...clamp down to the cap
            if (startValue < 1) {
                startValue = 1;                 // clamp up to [1, maxCap]
            }
        } else {
            startValue = 1;
        }

        created = AdvancedSkill.create(base, extraCode, nameField.getText(),
                startValue, character, base.getWeaponClass());
        character.putSkill(created);
        dispose();
    }

    /** Renders an AttributeCode as its full name, e.g. "Mind Speed Speed". */
    private static final class AttrRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof AttributeCode) {
                AttributeCode a = (AttributeCode) value;
                setText(a.fullName() + " (" + a.code() + ")");
            }
            return this;
        }
    }
}
