package com.whim.swd6.ui;

import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.DifficultyTier;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.RollResult;
import com.whim.swd6.api.Skill;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dice roller: enter a dice code or pick the active character's skill, toggle the
 * Wild Die, set a difficulty tier or exact target, optionally add Character-Point
 * bonus dice or spend a Force Point (doubling the code). Shows every die face, the
 * total, success/failure, and calls out complications / exploding sixes with a
 * short narration line.
 *
 * Owned by Task 3 (ui).
 */
public final class DiceRollerPanel extends HubPanel {

    private final JTextField codeField = new JTextField("3D+2", 8);
    private final JComboBox<SkillItem> skillBox = new JComboBox<SkillItem>();
    private final JCheckBox wildBox = new JCheckBox("Wild Die", true);
    private final JComboBox<String> targetMode = new JComboBox<String>(
            new String[]{"Difficulty Tier", "Target Number", "Untargeted"});
    private final JComboBox<DifficultyTier> tierBox = new JComboBox<DifficultyTier>(DifficultyTier.values());
    private final JSpinner targetSpin = new JSpinner(new SpinnerNumberModel(10, 1, 60, 1));
    private final JSpinner bonusDice = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
    private final JCheckBox forcePointBox = new JCheckBox("Spend Force Point (double the code)");

    private final DiceStripView strip = new DiceStripView();
    private final JLabel totalLabel = Ui.title("—");
    private final JLabel verdictLabel = Ui.head(" ");
    private final JLabel flagsLabel = Ui.body(" ");
    private final JLabel narration = Ui.dim(" ");

    public DiceRollerPanel(AppContext ctx) {
        super(ctx);
        build();
    }

    private static final class SkillItem {
        final Skill skill; // null for "manual code"
        final String label;
        final DiceCode code;
        SkillItem(Skill s, String label, DiceCode code) {
            this.skill = s; this.label = label; this.code = code;
        }
        @Override public String toString() { return label; }
    }

    private void build() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        header.setOpaque(false);
        header.add(Ui.title("Dice Roller"));
        header.add(Ui.dim("D6 · Wild Die explodes on 6 · complication on 1"));
        add(header, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        form.add(row(Ui.head("Skill:"), skillBox, Ui.dim("(auto-fills code)")));
        skillBox.setPreferredSize(new Dimension(240, 26));
        skillBox.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                SkillItem it = (SkillItem) skillBox.getSelectedItem();
                if (it != null && it.code != null) {
                    codeField.setText(it.code.toString());
                }
            }
        });

        form.add(row(Ui.head("Dice code:"), codeField, wildBox));

        JPanel tgt = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        tgt.setOpaque(false);
        tgt.add(Ui.head("Difficulty:"));
        tgt.add(targetMode);
        tgt.add(tierBox);
        tgt.add(new JLabel());
        JLabel tnLbl = Ui.body("TN:");
        tgt.add(tnLbl);
        tgt.add(targetSpin);
        form.add(tgt);
        targetMode.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { syncTargetMode(); }
        });
        syncTargetMode();

        form.add(row(Ui.head("Char-Point bonus dice:"), bonusDice, forcePointBox));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        actions.setOpaque(false);
        actions.add(Ui.button("Roll"));
        ((javax.swing.JButton) actions.getComponent(0)).addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { doRoll(); }
        });
        form.add(actions);

        add(form, BorderLayout.CENTER);

        JPanel out = new JPanel();
        out.setOpaque(false);
        out.setLayout(new BoxLayout(out, BoxLayout.Y_AXIS));
        out.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(6, 16, 12, 16),
                BorderFactory.createLineBorder(Palette.GRID_LINE, 1)));
        strip.setAlignmentX(Component.LEFT_ALIGNMENT);
        out.add(Ui.scroll(strip));
        JPanel tl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        tl.setOpaque(false);
        tl.add(Ui.head("Total:"));
        tl.add(totalLabel);
        tl.add(Box.createHorizontalStrut(16));
        tl.add(verdictLabel);
        out.add(tl);
        out.add(pad(flagsLabel));
        out.add(pad(narration));
        add(out, BorderLayout.SOUTH);
    }

    private JPanel pad(Component c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    private JPanel row(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        p.setOpaque(false);
        for (Component c : comps) {
            p.add(c);
        }
        return p;
    }

    private void syncTargetMode() {
        int m = targetMode.getSelectedIndex();
        tierBox.setVisible(m == 0);
        targetSpin.setVisible(m == 1);
    }

    @Override
    public void onShow() {
        DefaultComboBoxModel<SkillItem> model = new DefaultComboBoxModel<SkillItem>();
        model.addElement(new SkillItem(null, "— manual code —", null));
        PlayerCharacter pc = ctx.character();
        if (pc != null) {
            for (Skill s : pc.getSkills()) {
                DiceCode code = pc.skillCode(s);
                model.addElement(new SkillItem(s, s.getName() + "  (" + code + ")", code));
            }
        }
        skillBox.setModel(model);
    }

    private int resolveTarget() {
        int m = targetMode.getSelectedIndex();
        if (m == 0) {
            return ((DifficultyTier) tierBox.getSelectedItem()).representativeTarget();
        } else if (m == 1) {
            return ((Number) targetSpin.getValue()).intValue();
        }
        return -1;
    }

    private void doRoll() {
        DiceCode code;
        try {
            code = DiceCode.parse(codeField.getText());
        } catch (RuntimeException ex) {
            verdictLabel.setForeground(Palette.DANGER);
            verdictLabel.setText("Bad dice code — try e.g. 3D+2");
            return;
        }
        int bonus = ((Number) bonusDice.getValue()).intValue();
        if (bonus > 0) {
            code = code.addDice(bonus);
        }
        boolean force = forcePointBox.isSelected();
        if (force) {
            code = code.doubled();
        }
        int target = resolveTarget();
        RollResult r = ctx.engine().roll(code, wildBox.isSelected(), target);
        strip.setResult(r);
        totalLabel.setText(String.valueOf(r.getTotal()));

        if (r.hasTarget()) {
            if (r.isSuccess()) {
                verdictLabel.setForeground(Palette.OK);
                verdictLabel.setText("SUCCESS  (vs " + r.getTarget() + ")");
            } else {
                verdictLabel.setForeground(Palette.DANGER);
                verdictLabel.setText("FAILURE  (vs " + r.getTarget() + ")");
            }
        } else {
            verdictLabel.setForeground(Palette.TEXT_DIM);
            verdictLabel.setText("(untargeted)");
        }

        StringBuilder flags = new StringBuilder();
        Color fc = Palette.TEXT_DIM;
        if (r.isComplication()) {
            flags.append("Complication! (Wild Die rolled a 1) ");
            fc = Palette.DANGER;
        }
        if (r.isWildExploded()) {
            flags.append("Wild Die exploded! (rolled 6) ");
            fc = Palette.AMBER;
        }
        if (force) {
            flags.append("· Force Point spent ");
        }
        if (bonus > 0) {
            flags.append("· +").append(bonus).append("D Character Points ");
        }
        flagsLabel.setForeground(fc);
        flagsLabel.setText(flags.length() == 0 ? "Clean roll." : flags.toString());

        narration.setText(narrate(r));

        // spend the points from the active character, if any
        PlayerCharacter pc = ctx.character();
        if (pc != null) {
            if (force && pc.getForcePoints() > 0) {
                pc.setForcePoints(pc.getForcePoints() - 1);
                forcePointBox.setSelected(false);
            }
            if (bonus > 0 && pc.getCharacterPoints() > 0) {
                pc.setCharacterPoints(Math.max(0, pc.getCharacterPoints() - bonus));
                bonusDice.setValue(0);
            }
        }
    }

    private String narrate(RollResult r) {
        if (r.isComplication()) {
            return "The dice betray you — something goes wrong at the worst moment.";
        }
        if (r.isWildExploded() && r.isSuccess()) {
            return "A surge of fortune! The action lands with room to spare.";
        }
        if (r.isSuccess()) {
            return "Clean and controlled — the attempt succeeds.";
        }
        if (r.hasTarget()) {
            return "Not enough. The moment slips past.";
        }
        return "The dice settle. Total " + r.getTotal() + ".";
    }
}
