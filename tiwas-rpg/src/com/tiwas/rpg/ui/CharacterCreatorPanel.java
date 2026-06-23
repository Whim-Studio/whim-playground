package com.tiwas.rpg.ui;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import com.tiwas.rpg.domain.AttributeCode;
import com.tiwas.rpg.domain.Character;
import com.tiwas.rpg.engine.CharacterGenerator;
import com.tiwas.rpg.engine.Dice;

/**
 * Character Creator view: roll a fresh character, inspect the 24 attributes
 * (Body then Mind) and derived stats, and save/load the character as JSON.
 */
public final class CharacterCreatorPanel extends JPanel {

    private final JTextField nameField = new JTextField(18);
    private final DefaultTableModel bodyModel = newAttributeModel();
    private final DefaultTableModel mindModel = newAttributeModel();
    private final JPanel derivedPanel = new JPanel(new GridLayout(0, 2, 12, 4));
    private final JLabel statusLabel = new JLabel("Enter a name and roll a character.");

    private Character character;

    public CharacterCreatorPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildAttributeGrids(), BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.X_AXIS));

        bar.add(new JLabel("Character name: "));
        nameField.setMaximumSize(new Dimension(260, 28));
        bar.add(nameField);
        bar.add(Box.createHorizontalStrut(10));

        JButton roll = new JButton("Roll Character");
        roll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                rollCharacter();
            }
        });
        bar.add(roll);
        bar.add(Box.createHorizontalStrut(10));

        JButton save = new JButton("Save JSON");
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveCharacter();
            }
        });
        bar.add(save);
        bar.add(Box.createHorizontalStrut(6));

        JButton load = new JButton("Load JSON");
        load.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadCharacter();
            }
        });
        bar.add(load);
        bar.add(Box.createHorizontalGlue());
        return bar;
    }

    private JPanel buildAttributeGrids() {
        JPanel grids = new JPanel(new GridLayout(1, 2, 10, 0));
        grids.add(buildAttributeTable("Body Attributes", bodyModel, AttributeCode.bodyAttributes()));
        grids.add(buildAttributeTable("Mind Attributes", mindModel, AttributeCode.mindAttributes()));
        return grids;
    }

    private JPanel buildAttributeTable(String title, DefaultTableModel model, List<AttributeCode> codes) {
        // Seed the rows once; values are filled in/refreshed when a character exists.
        for (int i = 0; i < codes.size(); i++) {
            AttributeCode a = codes.get(i);
            model.addRow(new Object[] { a.animal(), a.fullName(), a.tier1Skill(), "" });
        }

        JTable table = new JTable(model) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(50);

        JLabel header = new JLabel(title, SwingConstants.CENTER);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(header, BorderLayout.NORTH);
        wrap.add(new JScrollPane(table), BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildSidePanel() {
        JPanel side = new JPanel(new BorderLayout(0, 8));
        side.setPreferredSize(new Dimension(230, 0));

        JLabel header = new JLabel("Derived Stats", SwingConstants.CENTER);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
        side.add(header, BorderLayout.NORTH);

        derivedPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        side.add(derivedPanel, BorderLayout.CENTER);
        renderDerived();
        return side;
    }

    private void rollCharacter() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            name = "Unnamed Hero";
        }
        character = new CharacterGenerator(new Dice()).generate(name);
        renderCharacter();
        statusLabel.setText("Rolled \"" + character.getName() + "\".");
    }

    private void renderCharacter() {
        fillValues(bodyModel, AttributeCode.bodyAttributes());
        fillValues(mindModel, AttributeCode.mindAttributes());
        renderDerived();
    }

    private void fillValues(DefaultTableModel model, List<AttributeCode> codes) {
        for (int i = 0; i < codes.size(); i++) {
            int value = character == null ? 0 : character.getAttribute(codes.get(i));
            model.setValueAt(character == null ? "" : Integer.valueOf(value), i, 3);
        }
    }

    private void renderDerived() {
        derivedPanel.removeAll();
        if (character == null) {
            addDerivedRow("Max HP", "—");
            addDerivedRow("Max MP", "—");
            addDerivedRow("Physical Energy", "—");
            addDerivedRow("Speed", "—");
            addDerivedRow("Energy Regen", "—");
            addDerivedRow("MP Regen", "—");
            addDerivedRow("Movement Speed", "—");
        } else {
            addDerivedRow("Max HP", String.valueOf(character.getMaxHP()));
            addDerivedRow("Max MP", String.valueOf(character.getMaxMP()));
            addDerivedRow("Physical Energy", String.valueOf(character.getMaxPhysicalEnergy()));
            addDerivedRow("Speed", String.valueOf(character.getSpeed()));
            addDerivedRow("Energy Regen", String.valueOf(character.getEnergyRegen()));
            addDerivedRow("MP Regen", String.valueOf(character.getMpRegen()));
            addDerivedRow("Movement Speed", String.valueOf(character.getMovementSpeed()));
        }
        derivedPanel.revalidate();
        derivedPanel.repaint();
    }

    private void addDerivedRow(String label, String value) {
        JLabel name = new JLabel(label + ":");
        JLabel val = new JLabel(value);
        val.setForeground(new Color(40, 28, 120));
        val.setFont(val.getFont().deriveFont(Font.BOLD));
        derivedPanel.add(name);
        derivedPanel.add(val);
    }

    private void saveCharacter() {
        if (character == null) {
            JOptionPane.showMessageDialog(this, "Roll or load a character first.",
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
            statusLabel.setText("Saved to " + chooser.getSelectedFile().getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save: " + ex.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadCharacter() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            character = Character.load(chooser.getSelectedFile());
            nameField.setText(character.getName());
            renderCharacter();
            statusLabel.setText("Loaded \"" + character.getName() + "\".");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not load: " + ex.getMessage(),
                    "Load failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static DefaultTableModel newAttributeModel() {
        return new DefaultTableModel(new Object[] { "Animal", "Attribute", "Tier-1 Skill", "Value" }, 0);
    }
}
