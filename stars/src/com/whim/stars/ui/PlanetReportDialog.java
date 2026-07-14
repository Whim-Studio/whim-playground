package com.whim.stars.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.whim.stars.model.Galaxy;
import com.whim.stars.model.Mineral;
import com.whim.stars.model.Planet;
import com.whim.stars.model.Player;

/**
 * A summary table of every planet the human player owns — the Planet Reports
 * screen. Read-only.
 */
public final class PlanetReportDialog extends JDialog {

    public PlanetReportDialog(Frame owner, Galaxy galaxy, Player human) {
        super(owner, "Planet Report — " + human.name(), true);
        setLayout(new BorderLayout(8, 8));

        String[] cols = { "Planet", "Pop", "Factories", "Mines", "Def", "Iron", "Bor", "Germ", "Hab%" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (Planet p : galaxy.planetsOf(human)) {
            model.addRow(new Object[] {
                    p.name() + (p.isHomeworld() ? " *" : ""),
                    p.population(),
                    p.factories(),
                    p.mines(),
                    p.defenses(),
                    p.surface(Mineral.IRONIUM),
                    p.surface(Mineral.BORANIUM),
                    p.surface(Mineral.GERMANIUM),
                    Math.round(p.habitability(human.race()) * 100)
            });
        }
        JTable table = new JTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(640, 260));
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel();
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        south.add(close);
        add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }
}
