package com.whim.capes.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.whim.capes.content.ClickLockData;
import com.whim.capes.content.ClickLockModule;
import com.whim.capes.engine.CharacterFactory;
import com.whim.capes.model.Ability;
import com.whim.capes.model.AbilityKind;
import com.whim.capes.model.Character;
import com.whim.capes.model.Drive;
import com.whim.capes.model.EventLogEntry;
import com.whim.capes.model.GameState;

/**
 * Phase 2 character creation. A single screen supports both methods (p.72,
 * p.80): choose Super-powered vs Mundane and Click-and-Lock vs Freeform. In
 * Click-and-Lock mode, picking a Power/Skill-Set + Persona and pressing "Load"
 * fills the three column editors with the combined 5/5/5 pool via
 * {@link CharacterFactory#combine}; the player then crosses out and renumbers.
 * Freeform starts blank. "Create Character" validates the shared shape rules
 * and registers the Character in the {@link GameState} roster.
 */
public final class CharacterCreationView extends JPanel {
    private final GameState state;
    private final CharacterCreatedListener listener;

    private final JRadioButton superBtn = new JRadioButton("Super-powered", true);
    private final JRadioButton mundaneBtn = new JRadioButton("Mundane");
    private final JRadioButton clickLockBtn = new JRadioButton("Click-and-Lock", true);
    private final JRadioButton freeformBtn = new JRadioButton("Freeform");

    private final JComboBox<ClickLockModule> setCombo = new JComboBox<ClickLockModule>();
    private final JComboBox<ClickLockModule> personaCombo = new JComboBox<ClickLockModule>();
    private final JButton loadBtn = new JButton("Load into editor ▼");

    private final JTextField nameField = new JTextField(18);
    private final JTextField conceptField = new JTextField(24);
    private final JLabel status = new JLabel(" ");

    private final JPanel primaryHolder = new JPanel(new BorderLayout());
    private AbilityColumnEditor primaryEditor;
    private final AbilityColumnEditor attitudeEditor = new AbilityColumnEditor("Attitudes", AbilityKind.ATTITUDE, false);
    private final AbilityColumnEditor styleEditor = new AbilityColumnEditor("Styles", AbilityKind.STYLE, false);
    private final DrivesEditor drivesEditor = new DrivesEditor();
    private final JPanel drivesHolder = new JPanel(new BorderLayout());

    private int nextId = 1;

    public CharacterCreationView(GameState state, CharacterCreatedListener listener) {
        this.state = state;
        this.listener = listener;
        setLayout(new BorderLayout());
        setBackground(Palette.PAPER);

        add(buildControls(), BorderLayout.NORTH);
        add(buildColumns(), BorderLayout.CENTER);

        drivesHolder.setOpaque(false);
        drivesHolder.add(drivesEditor, BorderLayout.NORTH);
        drivesHolder.setPreferredSize(new Dimension(320, 100));
        add(drivesHolder, BorderLayout.EAST);

        add(buildFooter(), BorderLayout.SOUTH);

        rebuildForType();
    }

    private JPanel buildControls() {
        JPanel north = new JPanel();
        north.setLayout(new java.awt.GridLayout(0, 1));
        north.setBackground(Palette.PAPER);
        north.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));

        ButtonGroup type = new ButtonGroup(); type.add(superBtn); type.add(mundaneBtn);
        ButtonGroup mode = new ButtonGroup(); mode.add(clickLockBtn); mode.add(freeformBtn);
        ActionListener typeL = new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { rebuildForType(); }
        };
        superBtn.addActionListener(typeL); mundaneBtn.addActionListener(typeL);
        ActionListener modeL = new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { syncMode(); }
        };
        clickLockBtn.addActionListener(modeL); freeformBtn.addActionListener(modeL);
        for (JRadioButton b : new JRadioButton[]{superBtn, mundaneBtn, clickLockBtn, freeformBtn}) b.setOpaque(false);

        JPanel row1 = flow();
        row1.add(bold("Type:")); row1.add(superBtn); row1.add(mundaneBtn);
        row1.add(sep()); row1.add(bold("Method:")); row1.add(clickLockBtn); row1.add(freeformBtn);

        DefaultListCellRenderer moduleRenderer = new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof ClickLockModule) setText(((ClickLockModule) v).name());
                return this;
            }
        };
        setCombo.setRenderer(moduleRenderer);
        personaCombo.setRenderer(moduleRenderer);
        for (ClickLockModule m : ClickLockData.personae()) personaCombo.addItem(m);
        loadBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { loadModules(); }
        });

        JPanel row2 = flow();
        row2.add(bold("Power/Skill-Set:")); row2.add(setCombo);
        row2.add(bold("Persona:")); row2.add(personaCombo);
        row2.add(loadBtn);

        JPanel row3 = flow();
        row3.add(bold("Name:")); row3.add(nameField);
        row3.add(bold("Concept:")); row3.add(conceptField);

        north.add(row1); north.add(row2); north.add(row3);
        return north;
    }

    private JScrollPane buildColumns() {
        JPanel cols = new JPanel(new GridLayout(1, 3, 10, 0));
        cols.setOpaque(false);
        cols.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        primaryHolder.setOpaque(false);
        cols.add(primaryHolder);
        cols.add(attitudeEditor);
        cols.add(styleEditor);
        JScrollPane sp = new JScrollPane(cols);
        sp.setBorder(null);
        return sp;
    }

    private JPanel buildFooter() {
        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(Palette.PAPER);
        south.setBorder(BorderFactory.createEmptyBorder(4, 12, 10, 12));
        status.setFont(Palette.BODY);
        JButton create = new JButton("Create Character ✓");
        create.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { create(); }
        });
        south.add(status, BorderLayout.CENTER);
        south.add(create, BorderLayout.EAST);
        return south;
    }

    // ---- behavior ----

    private void rebuildForType() {
        boolean superPowered = superBtn.isSelected();
        primaryEditor = new AbilityColumnEditor(superPowered ? "Powers" : "Skills",
                superPowered ? AbilityKind.POWER : AbilityKind.SKILL, superPowered);
        primaryHolder.removeAll();
        primaryHolder.add(primaryEditor, BorderLayout.CENTER);
        primaryHolder.revalidate();
        primaryHolder.repaint();

        setCombo.removeAllItems();
        List<ClickLockModule> sets = superPowered ? ClickLockData.powerSets() : ClickLockData.skillSets();
        for (ClickLockModule m : sets) setCombo.addItem(m);

        drivesHolder.setVisible(superPowered);
        drivesHolder.getParent().revalidate();
        syncMode();
    }

    private void syncMode() {
        boolean cl = clickLockBtn.isSelected();
        setCombo.setEnabled(cl);
        personaCombo.setEnabled(cl);
        loadBtn.setEnabled(cl);
        status.setText(cl
                ? "Pick a set + persona, press Load, then cross out 3 (not all one column) and rank 1-up."
                : "Freeform: add 3-5 abilities per column, numbered 1-up (p.72).");
        status.setForeground(Palette.MUTED);
    }

    private void loadModules() {
        ClickLockModule set = (ClickLockModule) setCombo.getSelectedItem();
        ClickLockModule persona = (ClickLockModule) personaCombo.getSelectedItem();
        if (set == null || persona == null) return;
        List<Ability> pool = CharacterFactory.combine(set, persona);
        primaryEditor.setAbilities(filter(pool, primaryEditor.kind()));
        attitudeEditor.setAbilities(filter(pool, AbilityKind.ATTITUDE));
        styleEditor.setAbilities(filter(pool, AbilityKind.STYLE));
        status.setText("Loaded " + set.name() + " + " + persona.name()
                + " — now cross out 3 (uncheck/✕) and set ranks.");
        status.setForeground(Palette.INK);
    }

    private static List<Ability> filter(List<Ability> pool, AbilityKind kind) {
        java.util.List<Ability> out = new java.util.ArrayList<Ability>();
        for (Ability a : pool) if (a.kind() == kind) out.add(a);
        return out;
    }

    private void create() {
        boolean superPowered = superBtn.isSelected();
        String name = nameField.getText().trim();
        if (name.isEmpty()) { fail("Give the character a name."); return; }

        Character c = new Character("ch" + (nextId), name, superPowered);
        c.setConcept(conceptField.getText().trim());
        for (Ability a : primaryEditor.getAbilities()) c.abilities().add(a);
        for (Ability a : attitudeEditor.getAbilities()) c.abilities().add(a);
        for (Ability a : styleEditor.getAbilities()) c.abilities().add(a);

        // If the player left ranks at 0, auto-number 1-up by row order (p.80).
        boolean anyUnranked = false;
        for (Ability a : c.abilities()) if (a.score() <= 0) anyUnranked = true;
        if (anyUnranked) CharacterFactory.renumberColumns(c);

        if (superPowered) {
            c.setUndifferentiated(drivesEditor.isUndifferentiated());
            if (!drivesEditor.isUndifferentiated()) {
                for (Drive d : drivesEditor.getDrives()) c.drives().add(d);
            }
        }

        String shapeErr = c.validateAbilityShape();
        if (shapeErr != null) { fail(shapeErr); return; }
        String driveErr = c.validateDrives();
        if (driveErr != null) { fail(driveErr); return; }

        state.roster().add(c);
        nextId++;
        state.eventLog().log(EventLogEntry.Category.SYSTEM,
                "Created " + (superPowered ? "super" : "mundane") + " character: " + c.name()
                + " (" + c.abilities().size() + " abilities).");
        status.setText("Created " + c.name() + " ✓  — opening character sheet.");
        status.setForeground(Palette.CONTROL);
        if (listener != null) listener.onCharacterCreated(c);
    }

    private void fail(String msg) {
        status.setText("Cannot create: " + msg);
        status.setForeground(Palette.VILLAIN_RED);
    }

    // ---- small helpers ----
    private static JPanel flow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        p.setOpaque(false);
        return p;
    }
    private static JLabel bold(String s) { JLabel l = new JLabel(s); l.setFont(Palette.HEADING); return l; }
    private static JLabel sep() { JLabel l = new JLabel("   |   "); l.setForeground(Palette.MUTED); return l; }
}
