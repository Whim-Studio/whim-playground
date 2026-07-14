package com.whim.necromunda.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.whim.necromunda.model.Gang;
import com.whim.necromunda.persistence.SaveManager;

/**
 * A standalone window hosting the {@link RosterEditorPanel} plus Save/Load
 * (dependency-free JSON) controls. Reachable from the main window's Gangs menu.
 */
public final class RosterEditorFrame extends JFrame {

    private RosterEditorPanel panel;

    public RosterEditorFrame(Gang gang) {
        super("Roster Editor — " + gang.name());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.panel = new RosterEditorPanel(gang);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save…");
        JButton loadBtn = new JButton("Load…");
        saveBtn.addActionListener(e -> save());
        loadBtn.addActionListener(e -> load());
        bottom.add(loadBtn);
        bottom.add(saveBtn);
        add(bottom, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void save() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(panel.gang().name().replaceAll("\\s+", "_") + ".gang.json"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                SaveManager.saveGang(panel.gang(), fc.getSelectedFile().getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void load() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Gang loaded = SaveManager.loadGang(fc.getSelectedFile().getAbsolutePath());
                RosterEditorFrame f = new RosterEditorFrame(loaded);
                f.setVisible(true);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
