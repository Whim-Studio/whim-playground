package com.whim.coda.ui;

import com.whim.coda.data.DataRepository;
import com.whim.coda.data.JsonWriter;
import com.whim.coda.engine.AttributeGenerator;
import com.whim.coda.engine.PackageBuilder;
import com.whim.coda.engine.RulesEngine;
import com.whim.coda.model.Attribute;
import com.whim.coda.model.AttributeSet;
import com.whim.coda.model.CharacterSheet;
import com.whim.coda.model.Edge;
import com.whim.coda.model.Flaw;
import com.whim.coda.model.Reaction;
import com.whim.coda.model.Skill;
import com.whim.coda.model.SkillRank;
import com.whim.coda.model.Species;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Random;

/**
 * Main window for the Star Trek RPG (Coda System) character creator.
 *
 * <p>This class is pure presentation: it owns a single {@link CharacterSheet}
 * and drives it entirely through the engine / data contract signatures
 * ({@link DataRepository}, {@link AttributeGenerator}, {@link RulesEngine},
 * {@link PackageBuilder}, {@link JsonWriter}). No game rules live here.</p>
 */
public class MainFrame extends JFrame {

    /** The single source of truth, mutated through the engine contract. */
    private final CharacterSheet sheet = new CharacterSheet();

    private final Random rng = new Random();

    /** Set true while we programmatically mutate widgets so listeners no-op. */
    private boolean updating = false;

    // -- Identity --------------------------------------------------------
    private final JTextField nameField = new JTextField(20);
    private final JComboBox<Species> speciesCombo = new JComboBox<Species>();

    // -- Attributes ------------------------------------------------------
    private final JRadioButton randomMode = new JRadioButton("Random roll");
    private final JRadioButton pointBuyMode = new JRadioButton("Point-buy");
    private final JButton rollButton = new JButton("Roll 2d6 ×9, keep 6");
    private final JSpinner[] attrSpinners = new JSpinner[Attribute.values().length];
    private final JLabel pointBuyStatus = new JLabel(" ");

    // -- Live derived panel ---------------------------------------------
    private final JLabel[] adjustedLabels = new JLabel[Attribute.values().length];
    private final JLabel[] modifierLabels = new JLabel[Attribute.values().length];
    private final JLabel healthLabel = new JLabel("-");
    private final JLabel defenseLabel = new JLabel("-");
    private final JLabel courageLabel = new JLabel("-");
    private final JLabel renownLabel = new JLabel("-");
    private final JLabel quicknessLabel = new JLabel("-");
    private final JLabel savvyLabel = new JLabel("-");
    private final JLabel staminaLabel = new JLabel("-");
    private final JLabel willpowerLabel = new JLabel("-");

    // -- Skills ----------------------------------------------------------
    private final JComboBox<Skill> skillCombo = new JComboBox<Skill>();
    private final JSpinner rankSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 7, 1));
    private final JTextField specialtyField = new JTextField(12);
    private final SkillTableModel skillModel = new SkillTableModel();
    private final JTable skillTable = new JTable(skillModel);

    // -- Edges / Flaws ---------------------------------------------------
    private final JComboBox<Edge> edgeCombo = new JComboBox<Edge>();
    private final DefaultListModel<Edge> chosenEdges = new DefaultListModel<Edge>();
    private final JList<Edge> edgeList = new JList<Edge>(chosenEdges);
    private final JComboBox<Flaw> flawCombo = new JComboBox<Flaw>();
    private final DefaultListModel<Flaw> chosenFlaws = new DefaultListModel<Flaw>();
    private final JList<Flaw> flawList = new JList<Flaw>(chosenFlaws);

    // -- JSON preview ----------------------------------------------------
    private final JTextArea jsonArea = new JTextArea();

    public MainFrame() {
        super("Star Trek RPG (Coda System) — Character Creator");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        buildWidgets();
        layoutFrame();
        wireListeners();

        // Seed the sheet from the initial widget state and render once.
        syncIdentityToSheet();
        recomputeAndRefresh();

        setMinimumSize(new Dimension(1024, 720));
        pack();
        setLocationRelativeTo(null);
    }

    // ====================================================================
    // Construction
    // ====================================================================

    private void buildWidgets() {
        // Species roster from the data contract.
        DefaultComboBoxModel<Species> sModel = new DefaultComboBoxModel<Species>();
        List<Species> species = DataRepository.species();
        for (int i = 0; i < species.size(); i++) {
            sModel.addElement(species.get(i));
        }
        speciesCombo.setModel(sModel);
        speciesCombo.setRenderer(new NamedRenderer());

        // Attribute spinners: base cap 12 enforced here in the UI (min 1).
        for (int i = 0; i < attrSpinners.length; i++) {
            JSpinner sp = new JSpinner(new SpinnerNumberModel(6, 1,
                    RulesEngine.MAX_START_ATTRIBUTE, 1));
            attrSpinners[i] = sp;
        }

        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(randomMode);
        modeGroup.add(pointBuyMode);
        pointBuyMode.setSelected(true);

        for (int i = 0; i < adjustedLabels.length; i++) {
            adjustedLabels[i] = new JLabel("-", SwingConstants.CENTER);
            modifierLabels[i] = new JLabel("-", SwingConstants.CENTER);
        }

        // Skill / edge / flaw pickers from the data contract.
        fillCombo(skillCombo, DataRepository.skills());
        skillCombo.setRenderer(new NamedRenderer());
        fillCombo(edgeCombo, DataRepository.edges());
        edgeCombo.setRenderer(new NamedRenderer());
        fillCombo(flawCombo, DataRepository.flaws());
        flawCombo.setRenderer(new NamedRenderer());

        skillTable.setFillsViewportHeight(true);
        skillTable.getSelectionModel().setSelectionMode(
                javax.swing.ListSelectionModel.SINGLE_SELECTION);

        jsonArea.setEditable(false);
        jsonArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        jsonArea.setLineWrap(false);
    }

    private static <T> void fillCombo(JComboBox<T> combo, List<T> items) {
        DefaultComboBoxModel<T> m = new DefaultComboBoxModel<T>();
        for (int i = 0; i < items.size(); i++) {
            m.addElement(items.get(i));
        }
        combo.setModel(m);
    }

    private void layoutFrame() {
        setLayout(new BorderLayout(8, 8));

        // LEFT: identity + attributes + skills/edges/flaws (the editors).
        JPanel left = new JPanel();
        left.setLayout(new BorderLayout(6, 6));
        left.add(buildIdentityPanel(), BorderLayout.NORTH);
        left.add(buildAttributePanel(), BorderLayout.CENTER);
        left.add(buildPackagePanel(), BorderLayout.SOUTH);

        // RIGHT: live derived stats.
        JPanel right = buildLivePanel();

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(left, BorderLayout.CENTER);
        top.add(right, BorderLayout.EAST);

        add(top, BorderLayout.CENTER);
        add(buildPreviewPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildIdentityPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Character"));
        GridBagConstraints c = gbc();
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST;
        p.add(new JLabel("Name:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        p.add(nameField, c);
        c.gridx = 0; c.gridy = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        p.add(new JLabel("Species:"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        p.add(speciesCombo, c);
        return p;
    }

    private JPanel buildAttributePanel() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createTitledBorder("Attributes (base, cap 12)"));

        JPanel modeRow = new JPanel();
        modeRow.add(pointBuyMode);
        modeRow.add(randomMode);
        modeRow.add(rollButton);
        p.add(modeRow, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(Attribute.values().length, 2, 6, 4));
        Attribute[] attrs = Attribute.values();
        for (int i = 0; i < attrs.length; i++) {
            grid.add(new JLabel(pretty(attrs[i].name()) + ":"));
            grid.add(attrSpinners[i]);
        }
        p.add(grid, BorderLayout.CENTER);

        pointBuyStatus.setFont(pointBuyStatus.getFont().deriveFont(Font.ITALIC));
        p.add(pointBuyStatus, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildPackagePanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Skills", buildSkillsTab());
        tabs.addTab("Edges", buildEdgesTab());
        tabs.addTab("Flaws", buildFlawsTab());

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBorder(BorderFactory.createTitledBorder("Custom Package"));
        wrap.add(tabs, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildSkillsTab() {
        JPanel p = new JPanel(new BorderLayout(4, 4));

        JPanel entry = new JPanel(new GridBagLayout());
        GridBagConstraints c = gbc();
        c.gridx = 0; c.gridy = 0;
        entry.add(new JLabel("Skill:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        entry.add(skillCombo, c);
        c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        entry.add(new JLabel("Rank:"), c);
        c.gridx = 3;
        entry.add(rankSpinner, c);
        c.gridx = 0; c.gridy = 1;
        entry.add(new JLabel("Specialty:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        entry.add(specialtyField, c);
        c.gridx = 2; c.gridwidth = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        JButton addSkill = new JButton("Add Skill");
        entry.add(addSkill, c);

        JButton removeSkill = new JButton("Remove Selected");

        p.add(entry, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(skillTable);
        sc.setPreferredSize(new Dimension(420, 130));
        p.add(sc, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(removeSkill, BorderLayout.EAST);
        p.add(south, BorderLayout.SOUTH);

        addSkill.addActionListener(e -> onAddSkill());
        removeSkill.addActionListener(e -> onRemoveSkill());
        return p;
    }

    private JPanel buildEdgesTab() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        JPanel row = new JPanel(new BorderLayout(4, 4));
        row.add(edgeCombo, BorderLayout.CENTER);
        JButton add = new JButton("Add Edge");
        row.add(add, BorderLayout.EAST);
        p.add(row, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(edgeList);
        sc.setPreferredSize(new Dimension(420, 90));
        p.add(sc, BorderLayout.CENTER);
        JButton remove = new JButton("Remove Selected");
        JPanel south = new JPanel(new BorderLayout());
        south.add(remove, BorderLayout.EAST);
        p.add(south, BorderLayout.SOUTH);

        add.addActionListener(e -> onAddEdge());
        remove.addActionListener(e -> onRemoveEdge());
        return p;
    }

    private JPanel buildFlawsTab() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        JPanel row = new JPanel(new BorderLayout(4, 4));
        row.add(flawCombo, BorderLayout.CENTER);
        JButton add = new JButton("Add Flaw");
        row.add(add, BorderLayout.EAST);
        p.add(row, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(flawList);
        sc.setPreferredSize(new Dimension(420, 90));
        p.add(sc, BorderLayout.CENTER);
        JButton remove = new JButton("Remove Selected");
        JPanel south = new JPanel(new BorderLayout());
        south.add(remove, BorderLayout.EAST);
        p.add(south, BorderLayout.SOUTH);

        add.addActionListener(e -> onAddFlaw());
        remove.addActionListener(e -> onRemoveFlaw());
        return p;
    }

    private JPanel buildLivePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Live Stats"));
        GridBagConstraints c = gbc();

        c.gridx = 0; c.gridy = 0;
        p.add(header("Attribute"), c);
        c.gridx = 1; p.add(header("Adj"), c);
        c.gridx = 2; p.add(header("Mod"), c);

        Attribute[] attrs = Attribute.values();
        for (int i = 0; i < attrs.length; i++) {
            c.gridy = i + 1;
            c.gridx = 0; c.anchor = GridBagConstraints.WEST;
            p.add(new JLabel(pretty(attrs[i].name())), c);
            c.anchor = GridBagConstraints.CENTER;
            c.gridx = 1; p.add(adjustedLabels[i], c);
            c.gridx = 2; p.add(modifierLabels[i], c);
        }

        int y = attrs.length + 1;
        c.gridx = 0; c.gridy = y++; c.gridwidth = 3;
        c.anchor = GridBagConstraints.WEST;
        p.add(sep("Reactions"), c);
        c.gridwidth = 1;
        y = derivedRow(p, c, y, "Quickness", quicknessLabel);
        y = derivedRow(p, c, y, "Savvy", savvyLabel);
        y = derivedRow(p, c, y, "Stamina", staminaLabel);
        y = derivedRow(p, c, y, "Willpower", willpowerLabel);

        c.gridx = 0; c.gridy = y++; c.gridwidth = 3;
        p.add(sep("Derived"), c);
        c.gridwidth = 1;
        y = derivedRow(p, c, y, "Health", healthLabel);
        y = derivedRow(p, c, y, "Defense", defenseLabel);
        y = derivedRow(p, c, y, "Courage", courageLabel);
        y = derivedRow(p, c, y, "Renown", renownLabel);

        return p;
    }

    private int derivedRow(JPanel p, GridBagConstraints c, int y, String name, JLabel value) {
        c.gridx = 0; c.gridy = y; c.gridwidth = 2; c.anchor = GridBagConstraints.WEST;
        p.add(new JLabel(name + ":"), c);
        c.gridx = 2; c.gridwidth = 1; c.anchor = GridBagConstraints.CENTER;
        value.setFont(value.getFont().deriveFont(Font.BOLD));
        p.add(value, c);
        return y + 1;
    }

    private JPanel buildPreviewPanel() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createTitledBorder("Character Sheet (JSON preview)"));
        JScrollPane sc = new JScrollPane(jsonArea);
        sc.setPreferredSize(new Dimension(960, 200));
        p.add(sc, BorderLayout.CENTER);

        JButton copy = new JButton("Copy to Clipboard");
        JButton save = new JButton("Save to File…");
        copy.addActionListener(e -> onCopy());
        save.addActionListener(e -> onSave());
        JPanel buttons = new JPanel();
        buttons.add(copy);
        buttons.add(save);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    // ====================================================================
    // Listener wiring
    // ====================================================================

    private void wireListeners() {
        nameField.getDocument().addDocumentListener(new SimpleDoc() {
            protected void changed() { if (!updating) { syncIdentityToSheet(); refreshPreview(); } }
        });

        speciesCombo.addActionListener(e -> {
            if (updating) return;
            syncIdentityToSheet();
            recomputeAndRefresh();
        });

        ChangeListener attrListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (updating) return;
                recomputeAndRefresh();
            }
        };
        for (int i = 0; i < attrSpinners.length; i++) {
            attrSpinners[i].addChangeListener(attrListener);
        }

        randomMode.addActionListener(e -> applyMode());
        pointBuyMode.addActionListener(e -> applyMode());
        rollButton.addActionListener(e -> onRoll());

        applyMode();
    }

    private void applyMode() {
        boolean random = randomMode.isSelected();
        rollButton.setEnabled(random);
        // In random mode spinners are filled by the dice; lock manual edits.
        for (int i = 0; i < attrSpinners.length; i++) {
            attrSpinners[i].setEnabled(!random);
        }
        refreshPointBuyStatus();
    }

    // ====================================================================
    // Actions
    // ====================================================================

    private void onRoll() {
        List<Integer> scores = AttributeGenerator.rollScores(rng);
        updating = true;
        try {
            Attribute[] attrs = Attribute.values();
            for (int i = 0; i < attrs.length; i++) {
                int v = i < scores.size() ? scores.get(i).intValue() : 6;
                if (v > RulesEngine.MAX_START_ATTRIBUTE) v = RulesEngine.MAX_START_ATTRIBUTE;
                if (v < 1) v = 1;
                attrSpinners[i].setValue(Integer.valueOf(v));
            }
        } finally {
            updating = false;
        }
        recomputeAndRefresh();
    }

    private void onAddSkill() {
        Skill skill = (Skill) skillCombo.getSelectedItem();
        if (skill == null) return;
        String specialty = specialtyField.getText().trim();
        int rank = ((Integer) rankSpinner.getValue()).intValue();
        SkillRank sr = new SkillRank(skill, rank, specialty.isEmpty() ? null : specialty);
        try {
            PackageBuilder.addSkill(sheet, sr);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage() == null ? "Cannot add that skill." : ex.getMessage(),
                    "Invalid Skill", JOptionPane.WARNING_MESSAGE);
            return;
        }
        specialtyField.setText("");
        skillModel.fireTableDataChanged();
        refreshPreview();
    }

    private void onRemoveSkill() {
        int row = skillTable.getSelectedRow();
        if (row < 0 || row >= sheet.getSkills().size()) return;
        sheet.getSkills().remove(row);
        skillModel.fireTableDataChanged();
        refreshPreview();
    }

    private void onAddEdge() {
        Edge edge = (Edge) edgeCombo.getSelectedItem();
        if (edge == null) return;
        PackageBuilder.addEdge(sheet, edge);
        chosenEdges.addElement(edge);
        refreshPreview();
    }

    private void onRemoveEdge() {
        int idx = edgeList.getSelectedIndex();
        if (idx < 0 || idx >= sheet.getEdges().size()) return;
        sheet.getEdges().remove(idx);
        chosenEdges.remove(idx);
        refreshPreview();
    }

    private void onAddFlaw() {
        Flaw flaw = (Flaw) flawCombo.getSelectedItem();
        if (flaw == null) return;
        PackageBuilder.addFlaw(sheet, flaw);
        chosenFlaws.addElement(flaw);
        refreshPreview();
    }

    private void onRemoveFlaw() {
        int idx = flawList.getSelectedIndex();
        if (idx < 0 || idx >= sheet.getFlaws().size()) return;
        sheet.getFlaws().remove(idx);
        chosenFlaws.remove(idx);
        refreshPreview();
    }

    private void onCopy() {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        cb.setContents(new StringSelection(jsonArea.getText()), null);
        JOptionPane.showMessageDialog(this, "Character sheet JSON copied to clipboard.",
                "Copied", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onSave() {
        JFileChooser chooser = new JFileChooser();
        String suggested = sheet.getName() == null || sheet.getName().trim().isEmpty()
                ? "character" : sheet.getName().trim().replaceAll("[^A-Za-z0-9_-]+", "_");
        chooser.setSelectedFile(new File(suggested + ".json"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), Charset.forName("UTF-8")));
            w.write(jsonArea.getText());
            JOptionPane.showMessageDialog(this, "Saved to " + file.getAbsolutePath(),
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save: " + ex.getMessage(),
                    "Save Failed", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (w != null) {
                try { w.close(); } catch (IOException ignore) { /* best effort */ }
            }
        }
    }

    // ====================================================================
    // State sync + refresh
    // ====================================================================

    private void syncIdentityToSheet() {
        sheet.setName(nameField.getText());
        sheet.setSpecies((Species) speciesCombo.getSelectedItem());
    }

    /** Push spinner values to the sheet, run the engine, repaint everything. */
    private void recomputeAndRefresh() {
        AttributeSet attrs = sheet.getAttributes();
        Attribute[] all = Attribute.values();
        for (int i = 0; i < all.length; i++) {
            int v = ((Integer) attrSpinners[i].getValue()).intValue();
            attrs.setBase(all[i], v);
        }
        RulesEngine.applySpecies(sheet);
        RulesEngine.recomputeDerived(sheet);
        refreshLivePanel();
        refreshPointBuyStatus();
        refreshPreview();
    }

    private void refreshLivePanel() {
        AttributeSet attrs = sheet.getAttributes();
        Attribute[] all = Attribute.values();
        for (int i = 0; i < all.length; i++) {
            adjustedLabels[i].setText(Integer.toString(attrs.getAdjusted(all[i])));
            modifierLabels[i].setText(signed(attrs.getModifier(all[i])));
        }
        quicknessLabel.setText(signed(sheet.getReaction(Reaction.QUICKNESS)));
        savvyLabel.setText(signed(sheet.getReaction(Reaction.SAVVY)));
        staminaLabel.setText(signed(sheet.getReaction(Reaction.STAMINA)));
        willpowerLabel.setText(signed(sheet.getReaction(Reaction.WILLPOWER)));
        healthLabel.setText(Integer.toString(sheet.getHealth()));
        defenseLabel.setText(Integer.toString(sheet.getDefense()));
        courageLabel.setText(Integer.toString(sheet.getCourage()));
        renownLabel.setText(Integer.toString(sheet.getRenown()));
    }

    private void refreshPointBuyStatus() {
        if (!pointBuyMode.isSelected()) {
            pointBuyStatus.setText("Random roll mode — use the Roll button.");
            pointBuyStatus.setForeground(Color.GRAY);
            return;
        }
        boolean ok = AttributeGenerator.validatePointBuy(sheet.getAttributes());
        pointBuyStatus.setText(ok
                ? "Point-buy valid (budget " + AttributeGenerator.POINT_BUY_BUDGET + ")."
                : "Point-buy OVER budget (" + AttributeGenerator.POINT_BUY_BUDGET + ") or out of range.");
        pointBuyStatus.setForeground(ok ? new Color(0, 128, 0) : Color.RED);
    }

    private void refreshPreview() {
        jsonArea.setText(JsonWriter.toJson(sheet));
        jsonArea.setCaretPosition(0);
    }

    // ====================================================================
    // Small helpers
    // ====================================================================

    private static GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private static JLabel header(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static JLabel sep(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        l.setForeground(new Color(60, 60, 120));
        return l;
    }

    private static String signed(int v) {
        return v > 0 ? "+" + v : Integer.toString(v);
    }

    /** "STRENGTH" -> "Strength". */
    private static String pretty(String enumName) {
        if (enumName.isEmpty()) return enumName;
        return enumName.charAt(0) + enumName.substring(1).toLowerCase();
    }

    // ====================================================================
    // Inner types
    // ====================================================================

    /** Renders model objects that expose getName() via reflection-free casts. */
    private static final class NamedRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setText(displayName(value));
            return this;
        }
    }

    private static String displayName(Object value) {
        if (value instanceof Species) return ((Species) value).getName();
        if (value instanceof Skill) return ((Skill) value).getName();
        if (value instanceof Edge) return ((Edge) value).getName();
        if (value instanceof Flaw) return ((Flaw) value).getName();
        return value == null ? "" : value.toString();
    }

    /** Table view over the sheet's live skill list (read straight from the model). */
    private final class SkillTableModel extends AbstractTableModel {
        private final String[] cols = { "Skill", "Key", "Rank", "Specialty" };

        public int getRowCount() { return sheet.getSkills().size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }

        public Object getValueAt(int row, int col) {
            SkillRank sr = sheet.getSkills().get(row);
            switch (col) {
                case 0: return sr.getSkill() == null ? "" : sr.getSkill().getName();
                case 1: return sr.getSkill() == null || sr.getSkill().getKey() == null
                        ? "" : pretty(sr.getSkill().getKey().name());
                case 2: return Integer.valueOf(sr.getRank());
                case 3: return sr.getSpecialty() == null ? "" : sr.getSpecialty();
                default: return "";
            }
        }
    }

    /** Minimal DocumentListener whose three callbacks funnel into one method. */
    private abstract static class SimpleDoc implements DocumentListener {
        protected abstract void changed();
        public void insertUpdate(DocumentEvent e) { changed(); }
        public void removeUpdate(DocumentEvent e) { changed(); }
        public void changedUpdate(DocumentEvent e) { changed(); }
    }
}
