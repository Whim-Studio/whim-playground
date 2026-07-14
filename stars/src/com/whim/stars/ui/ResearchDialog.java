package com.whim.stars.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import com.whim.stars.model.Player;
import com.whim.stars.model.TechField;
import com.whim.stars.model.formulas.Formulas;

/**
 * The Research screen: a read-out of all six fields (level, banked points, cost
 * to the next level) plus controls to set the current field, budget and
 * next-field policy for the human player. Applies straight to the model.
 */
public final class ResearchDialog extends JDialog {

    private final Player human;

    public ResearchDialog(Frame owner, Player human) {
        super(owner, "Research — " + human.name(), true);
        this.human = human;
        setLayout(new BorderLayout(8, 8));

        add(new JScrollPane(buildTable()), BorderLayout.CENTER);
        add(buildControls(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private JTable buildTable() {
        String[] cols = { "Field", "Level", "Banked pts", "Cost to next" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        int total = human.tech().total();
        for (TechField f : TechField.values()) {
            int level = human.tech().get(f);
            long cost = Formulas.researchCost(f, level, total, human.race().researchCostFactor(f));
            model.addRow(new Object[] {
                    f.label(), level, human.researchPoints(f), cost
            });
        }
        return new JTable(model);
    }

    private JPanel buildControls() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        final JComboBox<TechField> field = new JComboBox<TechField>(TechField.values());
        field.setSelectedItem(human.currentResearch());
        final JSpinner budget = new JSpinner(new SpinnerNumberModel(human.researchBudgetPercent(), 0, 100, 5));
        final JComboBox<Player.NextFieldPolicy> policy =
                new JComboBox<Player.NextFieldPolicy>(Player.NextFieldPolicy.values());
        policy.setSelectedItem(human.nextFieldPolicy());

        panel.add(new JLabel("Current field:"));
        panel.add(field);
        panel.add(new JLabel("Budget (% of resources):"));
        panel.add(budget);
        panel.add(new JLabel("When a field levels up:"));
        panel.add(policy);

        JButton apply = new JButton("Apply & Close");
        apply.addActionListener(e -> {
            human.setCurrentResearch((TechField) field.getSelectedItem());
            human.setResearchBudgetPercent((Integer) budget.getValue());
            human.setNextFieldPolicy((Player.NextFieldPolicy) policy.getSelectedItem());
            dispose();
        });
        panel.add(new JLabel());
        panel.add(apply);
        return panel;
    }
}
