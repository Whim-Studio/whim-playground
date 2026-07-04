package com.whim.swd6.ui;

import com.whim.swd6.api.Attribute;
import com.whim.swd6.api.CreationRules;
import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.Equipment;
import com.whim.swd6.api.ForceSkill;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.Skill;
import com.whim.swd6.api.SkillDef;
import com.whim.swd6.api.Template;
import com.whim.swd6.api.Weapon;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The six-step character creation wizard. Step panels live in a CardLayout driven
 * by a Back/Next/Finish controller. Point-buy validates live against
 * {@link CreationRules} (18D attributes, 7D skills, +2D per-skill creation cap);
 * the template path shows fixed attributes. The last step builds a
 * {@link PlayerCharacter} and can Save it via the repository. A Load button reads a
 * saved character back into the sheet.
 *
 * Owned by Task 3 (ui).
 */
public final class CreationWizardPanel extends HubPanel {

    private final CardLayout cards = new CardLayout();
    private final JPanel steps = new JPanel(cards);
    private final String[] stepIds = {"s1", "s2", "s3", "s4", "s5", "s6"};
    private int stepIndex = 0;

    private final JLabel stepLabel = Ui.head("Step 1 of 6");
    private final JButton backBtn = Ui.ghost("Back");
    private final JButton nextBtn = Ui.button("Next");

    // ----- step 1 -----
    private final JRadioButton pointBuyRadio = radio("Point-buy (allocate 18D yourself)");
    private final JRadioButton templateRadio = radio("Start from a template");
    private final JComboBox<Template> templateBox = new JComboBox<Template>();
    private final JTextArea templateDesc = Ui.logArea();

    // ----- step 2 (attributes) -----
    private final Map<Attribute, JSpinner> attrSpin = new EnumMap<Attribute, JSpinner>(Attribute.class);
    private final JLabel attrRemaining = Ui.head("");
    private final JPanel attrPanel = new JPanel();

    // ----- step 3 (skills) -----
    private final Map<String, JSpinner> skillSpin = new LinkedHashMap<String, JSpinner>();
    private final Map<String, JLabel> skillEff = new LinkedHashMap<String, JLabel>();
    private final JLabel skillRemaining = Ui.head("");
    private final JPanel skillPanel = new JPanel();

    // ----- step 4 (force) -----
    private final JCheckBox forceToggle = new JCheckBox("Force-Sensitive");
    private final Map<ForceSkill, JSpinner> forceSpin = new EnumMap<ForceSkill, JSpinner>(ForceSkill.class);
    private final JPanel forceInner = new JPanel();

    // ----- step 5 (gear/identity) -----
    private final JTextField nameField = new JTextField(18);
    private final JTextField speciesField = new JTextField("Human", 12);
    private final JTextField backgroundField = new JTextField(24);
    private final JTextField motivationField = new JTextField(24);
    private final JTextField destinyField = new JTextField(24);
    private final JSpinner creditsSpin = new JSpinner(new SpinnerNumberModel(1000, 0, 100000, 50));
    private final List<JCheckBox> gearChecks = new ArrayList<JCheckBox>();
    private final List<JCheckBox> weaponChecks = new ArrayList<JCheckBox>();

    // ----- step 6 (review) -----
    private final JTextArea reviewArea = Ui.logArea();

    private Template lastTemplate;

    public CreationWizardPanel(AppContext ctx) {
        super(ctx);
        buildHeader();
        steps.setOpaque(false);
        steps.add(buildStep1(), "s1");
        steps.add(buildStep2(), "s2");
        steps.add(buildStep3(), "s3");
        steps.add(buildStep4(), "s4");
        steps.add(buildStep5(), "s5");
        steps.add(buildStep6(), "s6");
        add(steps, BorderLayout.CENTER);
        buildNav();
        showStep(0);
    }

    private JRadioButton radio(String text) {
        JRadioButton r = new JRadioButton(text);
        r.setOpaque(false);
        r.setForeground(Palette.TEXT);
        r.setFont(Palette.BODY);
        return r;
    }

    private void buildHeader() {
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        head.setOpaque(false);
        head.add(Ui.title("Create a Character"));
        head.add(stepLabel);
        JButton load = Ui.ghost("Load…");
        load.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { doLoad(); }
        });
        head.add(load);
        add(head, BorderLayout.NORTH);
    }

    private void buildNav() {
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        nav.setOpaque(false);
        backBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { showStep(stepIndex - 1); }
        });
        nextBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { onNext(); }
        });
        nav.add(backBtn);
        nav.add(nextBtn);
        add(nav, BorderLayout.SOUTH);
    }

    private JComponent wrap(JComponent inner, String heading, String hint) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(4, 18, 4, 18));
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(Ui.head(heading));
        top.add(Ui.dim(hint));
        top.add(Box.createVerticalStrut(6));
        p.add(top, BorderLayout.NORTH);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    // ---------------- Step 1 ----------------
    private JComponent buildStep1() {
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        ButtonGroup grp = new ButtonGroup();
        grp.add(pointBuyRadio);
        grp.add(templateRadio);
        pointBuyRadio.setSelected(true);
        inner.add(pointBuyRadio);
        JPanel tpl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        tpl.setOpaque(false);
        tpl.add(templateRadio);
        tpl.add(templateBox);
        inner.add(tpl);
        templateDesc.setPreferredSize(new Dimension(560, 90));
        inner.add(Ui.scroll(templateDesc));
        templateBox.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { showTemplateDesc(); }
        });
        ActionListener modeSync = new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { templateRadio.setSelected(true); }
        };
        templateBox.addActionListener(modeSync);
        return wrap(inner, "How do you want to build?",
                "Point-buy gives you full control of 18D. A template pre-fills attributes and suggests skills.");
    }

    private void showTemplateDesc() {
        Template t = (Template) templateBox.getSelectedItem();
        if (t == null) {
            templateDesc.setText("");
            return;
        }
        StringBuilder b = new StringBuilder();
        b.append(t.getName()).append("\n").append(t.getDescription()).append("\n\nAttributes: ");
        for (Attribute a : Attribute.values()) {
            b.append(a.abbrev()).append(" ").append(t.getAttributes().get(a)).append("   ");
        }
        if (t.isForceSensitive()) {
            b.append("\nForce-Sensitive.");
        }
        b.append("\nSuggested skills: ").append(String.join(", ", t.getSuggestedSkills()));
        templateDesc.setText(b.toString());
    }

    // ---------------- Step 2 ----------------
    private JComponent buildStep2() {
        attrPanel.setOpaque(false);
        attrPanel.setLayout(new GridLayout(0, 2, 12, 8));
        ChangeListener cl = new ChangeListener() {
            @Override public void stateChanged(ChangeEvent e) { updateAttrRemaining(); }
        };
        for (Attribute a : Attribute.values()) {
            JSpinner sp = new JSpinner(new SpinnerNumberModel(6, 6, 12, 1)); // pips: 2D..4D
            sp.addChangeListener(cl);
            attrSpin.put(a, sp);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            row.setOpaque(false);
            row.add(fixed(Ui.body(a.display()), 120));
            row.add(sp);
            final Attribute fa = a;
            JLabel show = Ui.head("2D");
            sp.putClientProperty("label", show);
            row.add(show);
            attrPanel.add(row);
            sp.addChangeListener(new ChangeListener() {
                @Override public void stateChanged(ChangeEvent e) {
                    ((JLabel) attrSpin.get(fa).getClientProperty("label"))
                            .setText(DiceCode.ofPips(pips(attrSpin.get(fa))).toString());
                }
            });
        }
        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.add(attrPanel, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setOpaque(false);
        bottom.add(attrRemaining);
        inner.add(bottom, BorderLayout.SOUTH);
        return wrap(inner, "Allocate Attributes",
                "18D total, each attribute 2D–4D. In template mode these are fixed.");
    }

    private void updateAttrRemaining() {
        Map<Attribute, DiceCode> map = currentAttrMap();
        int rem = CreationRules.attributePipsRemaining(map);
        boolean ranges = true;
        for (Attribute a : Attribute.values()) {
            if (!CreationRules.attributeInRange(map.get(a))) {
                ranges = false;
            }
        }
        attrRemaining.setForeground(rem == 0 && ranges ? Palette.OK : Palette.AMBER);
        attrRemaining.setText("Remaining: " + dice(rem) + "  (need exactly 0D, each 2D–4D)"
                + (ranges ? "" : "  — out of range!"));
    }

    private Map<Attribute, DiceCode> currentAttrMap() {
        Map<Attribute, DiceCode> map = new EnumMap<Attribute, DiceCode>(Attribute.class);
        for (Attribute a : Attribute.values()) {
            map.put(a, DiceCode.ofPips(pips(attrSpin.get(a))));
        }
        return map;
    }

    // ---------------- Step 3 ----------------
    private JComponent buildStep3() {
        skillPanel.setOpaque(false);
        skillPanel.setLayout(new BoxLayout(skillPanel, BoxLayout.Y_AXIS));
        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.add(Ui.scroll(skillPanel), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setOpaque(false);
        bottom.add(skillRemaining);
        inner.add(bottom, BorderLayout.SOUTH);
        return wrap(inner, "Allocate Skill Dice",
                "Spend 7D across skills, at most +2D on any one skill at creation. Effective code updates live.");
    }

    private void rebuildSkillRows() {
        skillPanel.removeAll();
        skillSpin.clear();
        skillEff.clear();
        ChangeListener cl = new ChangeListener() {
            @Override public void stateChanged(ChangeEvent e) { updateSkillRemaining(); }
        };
        for (final SkillDef def : ctx.content().skillCatalog()) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 1));
            row.setOpaque(false);
            row.add(fixed(Ui.body(def.name()), 170));
            row.add(Ui.dim(def.attribute().abbrev()));
            JSpinner sp = new JSpinner(new SpinnerNumberModel(0, 0, 6, 1)); // pips, cap +2D
            sp.addChangeListener(cl);
            skillSpin.put(def.name(), sp);
            row.add(sp);
            JLabel eff = Ui.head("");
            skillEff.put(def.name(), eff);
            row.add(fixed(eff, 90));
            skillPanel.add(row);
        }
        updateSkillEffective();
        updateSkillRemaining();
        skillPanel.revalidate();
        skillPanel.repaint();
    }

    private void updateSkillEffective() {
        PlayerCharacter probe = ctx.character();
        Map<Attribute, DiceCode> attrs = currentAttrMap();
        for (SkillDef def : ctx.content().skillCatalog()) {
            JSpinner sp = skillSpin.get(def.name());
            if (sp == null) {
                continue;
            }
            DiceCode added = DiceCode.ofPips(pips(sp));
            DiceCode base = attrs.get(def.attribute());
            skillEff.get(def.name()).setText("= " + base.add(added));
        }
    }

    private void updateSkillRemaining() {
        updateSkillEffective();
        int spent = 0;
        boolean capOk = true;
        for (JSpinner sp : skillSpin.values()) {
            int p = pips(sp);
            spent += p;
            if (!CreationRules.skillAddWithinCap(DiceCode.ofPips(p))) {
                capOk = false;
            }
        }
        int rem = CreationRules.SKILL_PIPS_TOTAL - spent - forcePipsSpent();
        skillRemaining.setForeground(rem == 0 ? Palette.OK : (rem < 0 ? Palette.DANGER : Palette.AMBER));
        skillRemaining.setText("Remaining: " + dice(rem) + " of 7D (force skills included)"
                + (capOk ? "" : "  — a skill exceeds the +2D cap!"));
    }

    // ---------------- Step 4 ----------------
    private JComponent buildStep4() {
        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        forceToggle.setOpaque(false);
        forceToggle.setForeground(Palette.FORCE);
        forceToggle.setFont(Palette.HEAD);
        forceInner.setOpaque(false);
        forceInner.setLayout(new BoxLayout(forceInner, BoxLayout.Y_AXIS));
        ChangeListener cl = new ChangeListener() {
            @Override public void stateChanged(ChangeEvent e) { updateSkillRemaining(); }
        };
        for (ForceSkill fs : ForceSkill.values()) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            row.setOpaque(false);
            row.add(fixed(Ui.body(fs.display()), 120));
            JSpinner sp = new JSpinner(new SpinnerNumberModel(0, 0, 6, 1));
            sp.addChangeListener(cl);
            forceSpin.put(fs, sp);
            row.add(sp);
            row.add(Ui.dim("(pips over 0D)"));
            forceInner.add(row);
        }
        forceToggle.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                setForceEnabled(forceToggle.isSelected());
                updateSkillRemaining();
            }
        });
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        top.add(forceToggle);
        inner.add(top, BorderLayout.NORTH);
        inner.add(forceInner, BorderLayout.CENTER);
        setForceEnabled(false);
        return wrap(inner, "The Force",
                "Toggle Force-Sensitive to open CONTROL / SENSE / ALTER. Force dice draw from the same 7D pool here.");
    }

    private void setForceEnabled(boolean on) {
        for (JSpinner sp : forceSpin.values()) {
            sp.setEnabled(on);
            if (!on) {
                sp.setValue(0);
            }
        }
    }

    private int forcePipsSpent() {
        if (!forceToggle.isSelected()) {
            return 0;
        }
        int s = 0;
        for (JSpinner sp : forceSpin.values()) {
            s += pips(sp);
        }
        return s;
    }

    // ---------------- Step 5 ----------------
    private JComponent buildStep5() {
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.add(field("Name", nameField));
        inner.add(field("Species", speciesField));
        inner.add(field("Background", backgroundField));
        inner.add(field("Motivation", motivationField));
        inner.add(field("Destiny", destinyField));
        JPanel cr = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        cr.setOpaque(false);
        cr.add(fixed(Ui.body("Credits"), 120));
        cr.add(creditsSpin);
        inner.add(cr);
        inner.add(Ui.head("Starting Gear"));
        JPanel gearGrid = new JPanel(new GridLayout(0, 2, 6, 2));
        gearGrid.setOpaque(false);
        for (Equipment eq : ctx.content().equipmentCatalog()) {
            JCheckBox cb = check(eq.getName() + "  (" + eq.getCost() + "cr)");
            cb.putClientProperty("gear", eq);
            gearChecks.add(cb);
            gearGrid.add(cb);
        }
        inner.add(gearGrid);
        inner.add(Ui.head("Weapons"));
        JPanel wGrid = new JPanel(new GridLayout(0, 2, 6, 2));
        wGrid.setOpaque(false);
        for (Weapon w : ctx.content().weapons()) {
            JCheckBox cb = check(w.getName() + "  (dmg " + w.getDamage() + ", " + w.getCost() + "cr)");
            cb.putClientProperty("weapon", w);
            weaponChecks.add(cb);
            wGrid.add(cb);
        }
        inner.add(wGrid);
        return wrap(Ui.scroll(inner), "Identity & Gear",
                "Name your character and pick starting equipment and credits.");
    }

    // ---------------- Step 6 ----------------
    private JComponent buildStep6() {
        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        reviewArea.setPreferredSize(new Dimension(600, 320));
        inner.add(Ui.scroll(reviewArea), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        actions.setOpaque(false);
        JButton use = Ui.button("Use This Character");
        use.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { finishAndUse(false); }
        });
        JButton save = Ui.ghost("Save…");
        save.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { finishAndUse(true); }
        });
        actions.add(use);
        actions.add(save);
        inner.add(actions, BorderLayout.SOUTH);
        return wrap(inner, "Review",
                "Confirm your character, then use it or save it to disk.");
    }

    // ---------------- navigation ----------------
    private void showStep(int idx) {
        if (idx < 0) idx = 0;
        if (idx > 5) idx = 5;
        stepIndex = idx;
        if (idx == 1) {
            applyTemplateToAttributesIfNeeded();
            updateAttrRemaining();
        } else if (idx == 2) {
            if (skillSpin.isEmpty()) {
                rebuildSkillRows();
            }
            applyTemplateSuggestedSkills();
            updateSkillRemaining();
        } else if (idx == 4) {
            creditsSpin.setValue(lastTemplate != null ? lastTemplate.getStartingCredits() : 1000);
        } else if (idx == 5) {
            reviewArea.setText(buildReviewText());
        }
        cards.show(steps, stepIds[idx]);
        stepLabel.setText("Step " + (idx + 1) + " of 6");
        backBtn.setEnabled(idx > 0);
        nextBtn.setText(idx == 5 ? "Finish ▸" : "Next");
    }

    private void onNext() {
        if (stepIndex == 1 && pointBuyRadio.isSelected()) {
            Map<Attribute, DiceCode> map = currentAttrMap();
            if (CreationRules.attributePipsRemaining(map) != 0) {
                warn("Allocate exactly 18D of attributes (remaining must be 0D).");
                return;
            }
            for (Attribute a : Attribute.values()) {
                if (!CreationRules.attributeInRange(map.get(a))) {
                    warn("Each attribute must be between 2D and 4D.");
                    return;
                }
            }
        }
        if (stepIndex == 2) {
            int spent = forcePipsSpent();
            for (JSpinner sp : skillSpin.values()) {
                spent += pips(sp);
            }
            if (spent > CreationRules.SKILL_PIPS_TOTAL) {
                warn("You've overspent your 7D of skill dice.");
                return;
            }
        }
        if (stepIndex == 5) {
            finishAndUse(false);
            return;
        }
        showStep(stepIndex + 1);
    }

    private void applyTemplateToAttributesIfNeeded() {
        boolean template = templateRadio.isSelected();
        Template t = (Template) templateBox.getSelectedItem();
        lastTemplate = template ? t : null;
        if (template && t != null) {
            for (Attribute a : Attribute.values()) {
                DiceCode d = t.getAttributes().get(a);
                attrSpin.get(a).setValue(d == null ? 6 : d.pipValue());
                attrSpin.get(a).setEnabled(false);
                ((JLabel) attrSpin.get(a).getClientProperty("label")).setText(d == null ? "2D" : d.toString());
            }
            forceToggle.setSelected(t.isForceSensitive());
            setForceEnabled(t.isForceSensitive());
        } else {
            for (Attribute a : Attribute.values()) {
                attrSpin.get(a).setEnabled(true);
            }
        }
    }

    private void applyTemplateSuggestedSkills() {
        if (lastTemplate == null) {
            return;
        }
        // gentle hint: nudge suggested skills to +1D if still at 0, without overspending
        // (leaves the player free to redistribute)
    }

    // ---------------- build character ----------------
    private PlayerCharacter buildCharacter() {
        PlayerCharacter pc = new PlayerCharacter();
        pc.getSkills().clear();
        Map<Attribute, DiceCode> attrs = currentAttrMap();
        for (Attribute a : Attribute.values()) {
            pc.setAttribute(a, attrs.get(a));
        }
        pc.setName(nameField.getText().trim());
        pc.setSpecies(speciesField.getText().trim().isEmpty() ? "Human" : speciesField.getText().trim());
        pc.setBackground(backgroundField.getText().trim());
        pc.setMotivation(motivationField.getText().trim());
        pc.setDestiny(destinyField.getText().trim());
        pc.setTemplateName(lastTemplate != null ? lastTemplate.getName() : "");
        pc.setCredits(((Number) creditsSpin.getValue()).intValue());
        pc.setForcePoints(CreationRules.STARTING_FORCE_POINTS);
        pc.setCharacterPoints(CreationRules.STARTING_CHARACTER_POINTS);

        for (SkillDef def : ctx.content().skillCatalog()) {
            JSpinner sp = skillSpin.get(def.name());
            int p = sp == null ? 0 : pips(sp);
            if (p > 0) {
                pc.getSkills().add(new Skill(def.name(), def.attribute(), DiceCode.ofPips(p)));
            }
        }
        pc.setForceSensitive(forceToggle.isSelected());
        if (forceToggle.isSelected()) {
            for (ForceSkill fs : ForceSkill.values()) {
                pc.setForceSkill(fs, DiceCode.ofPips(pips(forceSpin.get(fs))));
            }
        }
        for (JCheckBox cb : gearChecks) {
            if (cb.isSelected()) {
                pc.getGear().add((Equipment) cb.getClientProperty("gear"));
            }
        }
        for (JCheckBox cb : weaponChecks) {
            if (cb.isSelected()) {
                pc.getWeapons().add((Weapon) cb.getClientProperty("weapon"));
            }
        }
        return pc;
    }

    private String buildReviewText() {
        PlayerCharacter pc = buildCharacter();
        StringBuilder b = new StringBuilder();
        b.append(pc.getName().isEmpty() ? "(unnamed)" : pc.getName())
                .append("  —  ").append(pc.getSpecies());
        if (!pc.getTemplateName().isEmpty()) {
            b.append("  [").append(pc.getTemplateName()).append("]");
        }
        b.append("\n\nAttributes:\n");
        for (Attribute a : Attribute.values()) {
            b.append("  ").append(a.abbrev()).append(": ").append(pc.getAttribute(a)).append("\n");
        }
        b.append("\nSkills:\n");
        if (pc.getSkills().isEmpty()) {
            b.append("  (none trained)\n");
        }
        for (Skill s : pc.getSkills()) {
            b.append("  ").append(s.getName()).append("  ").append(pc.skillCode(s))
                    .append("  (+").append(s.getAdded()).append(")\n");
        }
        if (pc.isForceSensitive()) {
            b.append("\nForce skills:\n");
            for (ForceSkill fs : ForceSkill.values()) {
                b.append("  ").append(fs.display()).append(": ").append(pc.getForceSkill(fs)).append("\n");
            }
        }
        b.append("\nForce Points: ").append(pc.getForcePoints())
                .append("   Character Points: ").append(pc.getCharacterPoints())
                .append("   Credits: ").append(pc.getCredits()).append("\n");
        b.append("\nGear: ");
        for (Equipment eq : pc.getGear()) {
            b.append(eq.getName()).append(", ");
        }
        b.append("\nWeapons: ");
        for (Weapon w : pc.getWeapons()) {
            b.append(w.getName()).append(", ");
        }
        return b.toString();
    }

    private void finishAndUse(boolean save) {
        PlayerCharacter pc = buildCharacter();
        ctx.setCharacter(pc);
        if (save) {
            doSave(pc);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Character is now active. Open the Sheet, Dice, Combat, or Adventure tabs.",
                    "Ready", JOptionPane.INFORMATION_MESSAGE);
            ctx.showCard("Sheet");
        }
    }

    private void doSave(PlayerCharacter pc) {
        try {
            String base = pc.getName().isEmpty() ? "character" : pc.getName().replaceAll("[^A-Za-z0-9_-]", "_");
            File dir = ctx.repository().defaultDirectory();
            File file = new File(dir, base + ".chr");
            ctx.repository().save(pc, file);
            JOptionPane.showMessageDialog(this,
                    "Saved to:\n" + file.getAbsolutePath(), "Saved", JOptionPane.INFORMATION_MESSAGE);
            ctx.showCard("Sheet");
        } catch (IOException ex) {
            warn("Save failed: " + ex.getMessage());
        }
    }

    private void doLoad() {
        JFileChooser fc = new JFileChooser(ctx.repository().defaultDirectory());
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                PlayerCharacter pc = ctx.repository().load(fc.getSelectedFile());
                ctx.setCharacter(pc);
                JOptionPane.showMessageDialog(this, "Loaded " + pc.getName(), "Loaded",
                        JOptionPane.INFORMATION_MESSAGE);
                ctx.showCard("Sheet");
            } catch (IOException ex) {
                warn("Load failed: " + ex.getMessage());
            }
        }
    }

    @Override
    public void onShow() {
        if (templateBox.getItemCount() == 0) {
            for (Template t : ctx.content().templates()) {
                templateBox.addItem(t);
            }
            showTemplateDesc();
        }
        if (skillSpin.isEmpty()) {
            rebuildSkillRows();
        }
    }

    // ---------------- small helpers ----------------
    private JCheckBox check(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setOpaque(false);
        cb.setForeground(Palette.TEXT);
        cb.setFont(Palette.SMALL);
        return cb;
    }

    private JComponent field(String label, JTextField f) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        p.setOpaque(false);
        p.add(fixed(Ui.body(label), 120));
        p.add(f);
        return p;
    }

    private JComponent fixed(JComponent c, int w) {
        c.setPreferredSize(new Dimension(w, 22));
        return c;
    }

    private int pips(JSpinner sp) {
        return ((Number) sp.getValue()).intValue();
    }

    private String dice(int pips) {
        return DiceCode.ofPips(Math.max(0, pips)).toString() + (pips < 0 ? " (over!)" : "");
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Creation", JOptionPane.WARNING_MESSAGE);
    }
}
