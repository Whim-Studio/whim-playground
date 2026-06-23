package com.tiwas.rpg.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import com.tiwas.rpg.domain.AdventureModule;
import com.tiwas.rpg.domain.Character;
import com.tiwas.rpg.domain.Npc;
import com.tiwas.rpg.domain.Scene;
import com.tiwas.rpg.domain.Skill;
import com.tiwas.rpg.engine.ActionResolver;
import com.tiwas.rpg.engine.ActionResult;
import com.tiwas.rpg.engine.Dice;

/**
 * Adventure Loader / Session view: load an adventure module, select a character,
 * begin a live session, and resolve skill actions against the engine.
 */
public final class AdventurePanel extends JPanel {

    private final ActionResolver resolver = new ActionResolver(new Dice());

    private final JTextArea moduleInfo = new JTextArea(8, 30);
    private final JLabel characterLabel = new JLabel("No character selected.");

    private final JProgressBar hpBar = poolBar(new Color(190, 60, 60));
    private final JProgressBar peBar = poolBar(new Color(70, 140, 70));
    private final JProgressBar mpBar = poolBar(new Color(70, 90, 190));

    private final JComboBox<Skill> skillCombo = new JComboBox<Skill>();
    private final JSpinner dmSpinner = new JSpinner(new SpinnerNumberModel(0, -50, 30, 1));
    private final JButton attemptButton = new JButton("Attempt Action");
    private final JTextArea log = new JTextArea(12, 30);

    private final JButton beginButton = new JButton("Begin Session");

    private AdventureModule module;
    private Character character;

    public AdventurePanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildLoaderColumn(), BorderLayout.WEST);
        add(buildSessionColumn(), BorderLayout.CENTER);

        setSessionEnabled(false);
    }

    private JPanel buildLoaderColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 8));
        col.setPreferredSize(new Dimension(360, 0));

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        JButton loadAdv = new JButton("Load Adventure JSON");
        loadAdv.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadAdventure();
            }
        });
        JButton selectChar = new JButton("Select Character JSON");
        selectChar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadCharacter("Selected");
            }
        });
        buttons.add(loadAdv);
        buttons.add(Box.createHorizontalStrut(6));
        buttons.add(selectChar);
        col.add(buttons, BorderLayout.NORTH);

        moduleInfo.setEditable(false);
        moduleInfo.setLineWrap(true);
        moduleInfo.setWrapStyleWord(true);
        moduleInfo.setText("Load an adventure module to see its title, author, scenes and NPCs.");
        col.add(new JScrollPane(moduleInfo), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.add(characterLabel, BorderLayout.NORTH);
        beginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                beginSession();
            }
        });
        south.add(beginButton, BorderLayout.SOUTH);
        col.add(south, BorderLayout.SOUTH);
        return col;
    }

    private JPanel buildSessionColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 8));

        JLabel header = new JLabel("Live Session", SwingConstants.LEFT);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 15f));
        col.add(header, BorderLayout.NORTH);

        // Pools
        JPanel pools = new JPanel(new GridLayout(3, 2, 8, 4));
        pools.setBorder(BorderFactory.createTitledBorder("Pools (current / max)"));
        pools.add(new JLabel("HP"));
        pools.add(hpBar);
        pools.add(new JLabel("Physical Energy"));
        pools.add(peBar);
        pools.add(new JLabel("MP"));
        pools.add(mpBar);

        // Action controls
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.setBorder(BorderFactory.createTitledBorder("Resolve Action"));
        skillCombo.setRenderer(new SkillRenderer());
        controls.add(new JLabel("Skill: "));
        controls.add(skillCombo);
        controls.add(Box.createHorizontalStrut(10));
        controls.add(new JLabel("Difficulty Modifier: "));
        dmSpinner.setMaximumSize(new Dimension(70, 28));
        controls.add(dmSpinner);
        controls.add(Box.createHorizontalStrut(10));
        attemptButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                attemptAction();
            }
        });
        controls.add(attemptButton);

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(pools, BorderLayout.NORTH);
        top.add(controls, BorderLayout.SOUTH);
        col.add(top, BorderLayout.NORTH);

        // Log
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel logWrap = new JPanel(new BorderLayout());
        logWrap.setBorder(BorderFactory.createTitledBorder("Action Log"));
        logWrap.add(new JScrollPane(log), BorderLayout.CENTER);
        col.add(logWrap, BorderLayout.CENTER);

        // Session-scoped save/load
        JPanel sessionFiles = new JPanel();
        sessionFiles.setLayout(new BoxLayout(sessionFiles, BoxLayout.X_AXIS));
        JButton save = new JButton("Save JSON");
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveCharacter();
            }
        });
        JButton load = new JButton("Load JSON");
        load.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadCharacter("Loaded");
            }
        });
        sessionFiles.add(Box.createHorizontalGlue());
        sessionFiles.add(save);
        sessionFiles.add(Box.createHorizontalStrut(6));
        sessionFiles.add(load);
        col.add(sessionFiles, BorderLayout.SOUTH);
        return col;
    }

    private void loadAdventure() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            module = AdventureModule.load(chooser.getSelectedFile());
            moduleInfo.setText(describeModule(module));
            moduleInfo.setCaretPosition(0);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not load adventure: " + ex.getMessage(),
                    "Load failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String describeModule(AdventureModule m) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title:  ").append(m.getTitle()).append('\n');
        sb.append("Author: ").append(m.getAuthor()).append('\n');
        sb.append('\n').append(m.getDescription()).append('\n');

        List<Scene> scenes = m.getScenes();
        sb.append("\nScenes (").append(scenes.size()).append("):\n");
        for (int i = 0; i < scenes.size(); i++) {
            Scene s = scenes.get(i);
            sb.append("  ").append(i + 1).append(". ").append(s.getTitle());
            List<String> npcNames = s.getNpcNames();
            if (npcNames != null && !npcNames.isEmpty()) {
                sb.append("  [NPCs: ").append(String.join(", ", npcNames)).append(']');
            }
            sb.append('\n');
        }

        List<Npc> npcs = m.getNpcs();
        sb.append("\nNPCs (").append(npcs.size()).append("):\n");
        for (int i = 0; i < npcs.size(); i++) {
            Npc n = npcs.get(i);
            sb.append("  - ").append(n.getName()).append(" (").append(n.getTier())
              .append(", HP ").append(n.getHp()).append(")\n");
        }
        return sb.toString();
    }

    private void beginSession() {
        if (character == null) {
            JOptionPane.showMessageDialog(this, "Select a character JSON first.",
                    "No character", JOptionPane.WARNING_MESSAGE);
            return;
        }
        rebuildSkillCombo();
        refreshPools();
        setSessionEnabled(true);
        appendLog("=== Session begun for " + character.getName() + " ===");
    }

    private void rebuildSkillCombo() {
        DefaultComboBoxModel<Skill> model = new DefaultComboBoxModel<Skill>();
        for (Skill s : character.getSkills().values()) {
            model.addElement(s);
        }
        skillCombo.setModel(model);
    }

    private void attemptAction() {
        if (character == null) {
            return;
        }
        Skill skill = (Skill) skillCombo.getSelectedItem();
        if (skill == null) {
            JOptionPane.showMessageDialog(this, "This character has no skills to use.",
                    "No skill", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int dm = ((Integer) dmSpinner.getValue()).intValue();
        int hpBefore = character.getCurrentHP();
        ActionResult result = resolver.resolve(character, skill, dm);
        appendLog(result.describe());
        int hpAfter = character.getCurrentHP();
        if (hpAfter < hpBefore) {
            appendLog("   ! Overflow: " + (hpBefore - hpAfter)
                    + " damage to HP (" + hpBefore + " -> " + hpAfter + ")");
        }
        refreshPools();
    }

    private void refreshPools() {
        setBar(hpBar, character.getCurrentHP(), character.getMaxHP(), "HP");
        setBar(peBar, character.getCurrentPE(), character.getMaxPhysicalEnergy(), "PE");
        setBar(mpBar, character.getCurrentMP(), character.getMaxMP(), "MP");
    }

    private void setBar(JProgressBar bar, int current, int max, String label) {
        bar.setMinimum(0);
        bar.setMaximum(Math.max(max, 1));
        bar.setValue(Math.max(0, Math.min(current, Math.max(max, 1))));
        bar.setString(label + "  " + current + " / " + max);
    }

    private void saveCharacter() {
        if (character == null) {
            JOptionPane.showMessageDialog(this, "No character to save.",
                    "Nothing to save", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(character.getName().replaceAll("\\s+", "_") + ".json"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            character.save(chooser.getSelectedFile());
            appendLog("Saved character to " + chooser.getSelectedFile().getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save: " + ex.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadCharacter(String verb) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            character = Character.load(chooser.getSelectedFile());
            characterLabel.setText("Character: " + character.getName());
            if (attemptButton.isEnabled()) {
                rebuildSkillCombo();
                refreshPools();
            }
            appendLog(verb + " character \"" + character.getName() + "\".");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not load: " + ex.getMessage(),
                    "Load failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setSessionEnabled(boolean enabled) {
        skillCombo.setEnabled(enabled);
        dmSpinner.setEnabled(enabled);
        attemptButton.setEnabled(enabled);
    }

    private void appendLog(String line) {
        log.append(line + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private static JProgressBar poolBar(Color color) {
        JProgressBar bar = new JProgressBar();
        bar.setStringPainted(true);
        bar.setForeground(color);
        bar.setString("—");
        return bar;
    }

    /** Renders a Skill in the combo box as "Name (Tier N, value V)". */
    private static final class SkillRenderer extends javax.swing.DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Skill) {
                Skill s = (Skill) value;
                setText(s.getName() + "  (Tier " + s.getTier() + ", value " + s.getValue() + ")");
            }
            return this;
        }
    }
}
