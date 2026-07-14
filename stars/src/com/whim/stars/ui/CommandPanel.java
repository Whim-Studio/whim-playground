package com.whim.stars.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

import com.whim.stars.model.Mineral;
import com.whim.stars.model.Planet;
import com.whim.stars.model.Player;
import com.whim.stars.model.TechField;
import com.whim.stars.model.race.Race;

/**
 * The right-hand control column: a planet report for the current selection, the
 * human player's research controls, an event log, and the "Generate Turn"
 * button. It reads and lightly edits the model (research field/budget) but all
 * simulation happens in the engine via the {@link TurnCallback}.
 */
public final class CommandPanel extends JPanel {

    /** Fired when the user presses "Generate Turn". */
    public interface TurnCallback {
        void onGenerateTurn();
    }

    private final Player human;
    private final TurnCallback turnCallback;

    private final JLabel planetTitle = new JLabel("No planet selected");
    private final JTextArea planetReport = new JTextArea(9, 20);
    private final JComboBox<TechField> researchField = new JComboBox<TechField>(TechField.values());
    private final JSpinner researchBudget = new JSpinner(new SpinnerNumberModel(15, 0, 100, 5));
    private final JTextArea eventLog = new JTextArea(8, 20);

    public CommandPanel(Player human, TurnCallback turnCallback) {
        this.human = human;
        this.turnCallback = turnCallback;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(300, 720));

        add(buildPlanetSection(), BorderLayout.NORTH);
        add(buildResearchSection(), BorderLayout.CENTER);
        add(buildSouthSection(), BorderLayout.SOUTH);

        // Initialize research controls from the human player.
        researchField.setSelectedItem(human.currentResearch());
        researchBudget.setValue(human.researchBudgetPercent());
        researchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                human.setCurrentResearch((TechField) researchField.getSelectedItem());
            }
        });
        researchBudget.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                human.setResearchBudgetPercent((Integer) researchBudget.getValue());
            }
        });
    }

    private JPanel buildPlanetSection() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Planet Report"));
        planetTitle.setFont(planetTitle.getFont().deriveFont(Font.BOLD, 14f));
        planetReport.setEditable(false);
        planetReport.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        panel.add(planetTitle, BorderLayout.NORTH);
        panel.add(new JScrollPane(planetReport), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildResearchSection() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Research (You)"));
        panel.add(new JLabel("Current field:"));
        panel.add(researchField);
        panel.add(new JLabel("Budget (% of resources):"));
        panel.add(researchBudget);
        return panel;
    }

    private JPanel buildSouthSection() {
        JPanel south = new JPanel(new BorderLayout(4, 4));

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Event Log"));
        eventLog.setEditable(false);
        eventLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logPanel.add(new JScrollPane(eventLog), BorderLayout.CENTER);
        south.add(logPanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton generate = new JButton("Generate Turn ▶");
        generate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (turnCallback != null) {
                    turnCallback.onGenerateTurn();
                }
            }
        });
        buttons.add(generate);
        south.add(buttons, BorderLayout.SOUTH);
        return south;
    }

    /** Update the planet report for the current selection (null clears it). */
    public void showPlanet(Planet planet) {
        if (planet == null) {
            planetTitle.setText("No planet selected");
            planetReport.setText("Click a planet on the map to inspect it.");
            return;
        }
        planetTitle.setText(planet.name() + (planet.isHomeworld() ? "  (Homeworld)" : ""));
        StringBuilder sb = new StringBuilder();
        sb.append("Owner       : ").append(ownerLabel(planet)).append('\n');
        sb.append("Environment : g").append(planet.gravity())
                .append(" t").append(planet.temperature())
                .append(" r").append(planet.radiation()).append('\n');
        if (planet.isColonized()) {
            sb.append("Population  : ").append(planet.population()).append('\n');
            sb.append("Factories   : ").append(planet.factories()).append('\n');
            sb.append("Mines       : ").append(planet.mines()).append('\n');
            sb.append("Defenses    : ").append(planet.defenses()).append('\n');
        } else {
            Race race = human.race();
            double hab = planet.habitability(race);
            sb.append("Habitability: ").append(Math.round(hab * 100)).append("% (for You)\n");
        }
        sb.append("Surface  I/B/G: ")
                .append(planet.surface(Mineral.IRONIUM)).append(" / ")
                .append(planet.surface(Mineral.BORANIUM)).append(" / ")
                .append(planet.surface(Mineral.GERMANIUM)).append('\n');
        sb.append("Concen.  I/B/G: ")
                .append(planet.concentration(Mineral.IRONIUM)).append(" / ")
                .append(planet.concentration(Mineral.BORANIUM)).append(" / ")
                .append(planet.concentration(Mineral.GERMANIUM)).append('\n');
        planetReport.setText(sb.toString());
        planetReport.setCaretPosition(0);
    }

    private String ownerLabel(Planet planet) {
        if (!planet.isColonized()) {
            return "(uncolonized)";
        }
        return planet.ownerId() == human.id() ? "You" : "Player " + planet.ownerId();
    }

    /** Append a line to the event log. */
    public void log(String message) {
        eventLog.append(message + "\n");
        eventLog.setCaretPosition(eventLog.getDocument().getLength());
    }

    /** Re-sync the research controls from the model (after load). */
    public void refreshResearchControls() {
        researchField.setSelectedItem(human.currentResearch());
        researchBudget.setValue(human.researchBudgetPercent());
    }
}
