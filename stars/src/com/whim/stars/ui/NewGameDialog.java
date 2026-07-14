package com.whim.stars.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.whim.stars.app.GameSetup;
import com.whim.stars.model.Galaxy;
import com.whim.stars.model.race.PRT;

/**
 * Modal "New Game" wizard: pick universe size, race name + primary trait, and
 * the number of AI opponents. On OK it produces a {@link GameSetup}; on Cancel
 * it produces {@code null}. Pure View — it builds no galaxy itself.
 */
public final class NewGameDialog extends JDialog {

    private final JComboBox<Galaxy.UniverseSize> size =
            new JComboBox<Galaxy.UniverseSize>(Galaxy.UniverseSize.values());
    private final JTextField raceName = new JTextField("Human");
    private final JComboBox<PRT> prt = new JComboBox<PRT>(PRT.values());
    private final JSpinner aiCount = new JSpinner(new SpinnerNumberModel(1, 1, 3, 1));

    private GameSetup result;

    public NewGameDialog(Frame owner) {
        super(owner, "New Game", true);
        setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        form.add(new JLabel("Universe size:"));
        form.add(size);
        form.add(new JLabel("Race name:"));
        form.add(raceName);
        form.add(new JLabel("Primary Racial Trait:"));
        form.add(prt);
        form.add(new JLabel("AI opponents:"));
        form.add(aiCount);
        size.setSelectedItem(Galaxy.UniverseSize.SMALL);
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton ok = new JButton("Start");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result = collect();
                dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result = null;
                dispose();
            }
        });
        buttons.add(ok);
        buttons.add(cancel);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private GameSetup collect() {
        GameSetup s = new GameSetup();
        s.size = (Galaxy.UniverseSize) size.getSelectedItem();
        String name = raceName.getText().trim();
        s.humanRaceName = name.isEmpty() ? "Human" : name;
        s.humanPrt = (PRT) prt.getSelectedItem();
        s.aiOpponents = (Integer) aiCount.getValue();
        // Vary the seed by the chosen options so different setups differ, but stay
        // reproducible within a run (no wall-clock randomness in the model).
        s.seed = 20250714L + s.size.ordinal() * 131L + s.aiOpponents * 17L + name.hashCode();
        return s;
    }

    /** Show the dialog and return the chosen setup, or null if cancelled. */
    public GameSetup showDialog() {
        setVisible(true);
        return result;
    }
}
