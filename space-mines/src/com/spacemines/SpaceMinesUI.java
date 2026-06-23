package com.spacemines;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Swing View for the Space Mines game. Contains no game logic; it reads
 * {@link ColonyState} fields for display and delegates all calculation to
 * {@link GameEngine}.
 */
public class SpaceMinesUI extends JFrame {

    private static final Color BG = new Color(10, 12, 20);
    private static final Color PANEL_BG = new Color(18, 22, 34);
    private static final Color FG = new Color(120, 230, 160);
    private static final Color ACCENT = new Color(230, 200, 90);
    private static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    private static final Font MONO_BOLD = new Font(Font.MONOSPACED, Font.BOLD, 14);

    private final GameEngine engine;

    // Stats value labels.
    private final JLabel yearValue = makeValueLabel();
    private final JLabel populationValue = makeValueLabel();
    private final JLabel minesValue = makeValueLabel();
    private final JLabel storedOreValue = makeValueLabel();
    private final JLabel moneyValue = makeValueLabel();
    private final JLabel foodPriceValue = makeValueLabel();
    private final JLabel satisfactionValue = makeValueLabel();

    private final JTextArea narrativeArea = new JTextArea();

    private final JTextField minesField = new JTextField("0", 6);
    private final JTextField oreField = new JTextField("0", 6);
    private final JTextField foodField = new JTextField("0", 6);
    private final JButton processButton = new JButton("Process Year");

    public SpaceMinesUI(GameEngine engine) {
        super("Space Mines");
        this.engine = engine;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(640, 560));
        setSize(720, 620);
        setLocationByPlatform(true);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        root.add(buildStatsPanel(), BorderLayout.NORTH);
        root.add(buildNarrativePanel(), BorderLayout.CENTER);
        root.add(buildInputPanel(), BorderLayout.SOUTH);

        setContentPane(root);

        processButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onProcessYear();
            }
        });

        refreshStats();
    }

    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 50, 70)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        addStat(panel, c, 0, 0, "Year:", yearValue);
        addStat(panel, c, 2, 0, "Population:", populationValue);
        addStat(panel, c, 0, 1, "Mines:", minesValue);
        addStat(panel, c, 2, 1, "Stored Ore:", storedOreValue);
        addStat(panel, c, 0, 2, "Money:", moneyValue);
        addStat(panel, c, 2, 2, "Food Price:", foodPriceValue);
        addStat(panel, c, 0, 3, "Satisfaction:", satisfactionValue);

        return panel;
    }

    private void addStat(JPanel panel, GridBagConstraints c, int x, int y,
            String caption, JLabel value) {
        JLabel label = new JLabel(caption);
        label.setFont(MONO);
        label.setForeground(FG);

        c.gridx = x;
        c.gridy = y;
        c.weightx = 0.0;
        panel.add(label, c);

        c.gridx = x + 1;
        c.weightx = 1.0;
        panel.add(value, c);
    }

    private JScrollPane buildNarrativePanel() {
        narrativeArea.setEditable(false);
        narrativeArea.setLineWrap(true);
        narrativeArea.setWrapStyleWord(true);
        narrativeArea.setFont(MONO);
        narrativeArea.setBackground(new Color(8, 10, 16));
        narrativeArea.setForeground(FG);
        narrativeArea.setCaretColor(FG);
        narrativeArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(narrativeArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(40, 50, 70)));
        scroll.setPreferredSize(new Dimension(680, 280));
        return scroll;
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 50, 70)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        addInput(panel, c, 0, "Mines to buy:", minesField);
        addInput(panel, c, 1, "Ore to sell:", oreField);
        addInput(panel, c, 2, "Food to buy:", foodField);

        styleField(minesField);
        styleField(oreField);
        styleField(foodField);

        processButton.setFont(MONO_BOLD);
        processButton.setForeground(BG);
        processButton.setBackground(ACCENT);
        processButton.setFocusPainted(false);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 6, 4, 6);
        panel.add(processButton, c);

        return panel;
    }

    private void addInput(JPanel panel, GridBagConstraints c, int row,
            String caption, JTextField field) {
        JLabel label = new JLabel(caption);
        label.setFont(MONO);
        label.setForeground(FG);

        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        panel.add(label, c);

        c.gridx = 1;
        panel.add(field, c);
    }

    private void styleField(JTextField field) {
        field.setFont(MONO);
        field.setBackground(new Color(8, 10, 16));
        field.setForeground(FG);
        field.setCaretColor(FG);
        field.setHorizontalAlignment(SwingConstants.RIGHT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 50, 70)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
    }

    private static JLabel makeValueLabel() {
        JLabel label = new JLabel("-");
        label.setFont(MONO_BOLD);
        label.setForeground(ACCENT);
        return label;
    }

    private void onProcessYear() {
        PlayerActions actions = new PlayerActions();
        actions.minesToBuy = parseField(minesField);
        actions.oreToSell = parseField(oreField);
        actions.foodToBuy = parseField(foodField);

        TurnResult result = engine.processYear(actions);

        if (result != null && result.narrative != null) {
            appendLine(result.narrative);
        }

        refreshStats();
        resetInputs();

        boolean ended = engine.isGameOver() || engine.isVictory();
        if (ended) {
            if (result != null && result.gameOverReason != null
                    && result.gameOverReason.length() > 0) {
                appendLine(result.gameOverReason);
            }
            if (engine.isVictory()) {
                appendLine("Victory! Your colony has thrived through the decade.");
            } else {
                appendLine("Game over.");
            }
            disableInputs();
        }
    }

    private int parseField(JTextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void resetInputs() {
        minesField.setText("0");
        oreField.setText("0");
        foodField.setText("0");
    }

    private void disableInputs() {
        minesField.setEnabled(false);
        oreField.setEnabled(false);
        foodField.setEnabled(false);
        processButton.setEnabled(false);
    }

    private void appendLine(String text) {
        narrativeArea.append(text);
        narrativeArea.append("\n");
        narrativeArea.setCaretPosition(narrativeArea.getDocument().getLength());
    }

    private void refreshStats() {
        ColonyState state = engine.getState();
        yearValue.setText(state.year + " / " + GameConstants.TOTAL_YEARS);
        populationValue.setText(String.valueOf(state.population));
        minesValue.setText(String.valueOf(state.mines));
        storedOreValue.setText(String.valueOf(state.storedOre));
        moneyValue.setText(String.valueOf(state.money));
        foodPriceValue.setText(String.valueOf(state.foodPrice));
        satisfactionValue.setText(String.valueOf(state.satisfaction));
    }
}
