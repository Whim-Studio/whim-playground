package com.whim.ebs.ui;

import com.whim.ebs.domain.Belief;
import com.whim.ebs.domain.Beliefs;
import com.whim.ebs.domain.SessionState;
import com.whim.ebs.spi.ExportService;
import com.whim.ebs.spi.ValidationResult;
import com.whim.ebs.spi.ValidationService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.BorderLayout;
import java.awt.CardLayout;
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

/**
 * The Emotional Bank Statement desktop UI.
 *
 * A three-step CardLayout wizard:
 *   1. Select a Belief   (searchable combo)
 *   2. Collect Proof     (three real-life examples)
 *   3. Build Proof Through Action (one daily action) + Save / Export
 *
 * This class depends ONLY on the injected interfaces and the domain types.
 * The concrete logic implementations are wired up by the orchestrator's Main class.
 */
public class MainFrame extends JFrame {

    private static final Color ACCENT = new Color(0x2E, 0x7D, 0x32);
    private static final Color BG = new Color(0xF7, 0xF8, 0xFA);
    private static final Color CARD_BG = Color.WHITE;

    private final SessionState state;
    private final ValidationService validator;
    private final ExportService exporter;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    // Step 1
    private JTextField searchField;
    private JComboBox<Belief> beliefCombo;
    private boolean suppressComboEvents = false;

    // Step 2
    private JTextArea[] proofAreas = new JTextArea[3];

    // Step 3
    private JTextArea actionArea;

    // Navigation
    private JButton backButton;
    private JButton nextButton;
    private JButton saveButton;
    private JLabel stepLabel;

    private int currentStep = 0;
    private static final int STEP_COUNT = 3;
    private static final String[] STEP_TITLES = {
            "Select a Belief",
            "Collect Proof",
            "Build Proof Through Action"
    };

    public MainFrame(SessionState state, ValidationService validator, ExportService exporter) {
        super("The Emotional Bank Statement");
        this.state = state;
        this.validator = validator;
        this.exporter = exporter;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUi();
        loadFromState();
        showStep(0);

        setMinimumSize(new Dimension(640, 520));
        setPreferredSize(new Dimension(720, 600));
        pack();
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ build

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        root.add(buildHeader(), BorderLayout.NORTH);

        cards.setOpaque(false);
        cards.add(buildStep1(), "step0");
        cards.add(buildStep2(), "step1");
        cards.add(buildStep3(), "step2");
        root.add(cards, BorderLayout.CENTER);

        root.add(buildNavBar(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel title = new JLabel("The Emotional Bank Statement");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(ACCENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        stepLabel = new JLabel();
        stepLabel.setFont(stepLabel.getFont().deriveFont(Font.PLAIN, 13f));
        stepLabel.setForeground(new Color(0x60, 0x66, 0x6E));
        stepLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        stepLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        header.add(title);
        header.add(stepLabel);
        return header;
    }

    private JComponent buildCard(String heading, String subText) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2, 0xE5, 0xEA), 1, true),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));

        JLabel h = new JLabel(heading);
        h.setFont(h.getFont().deriveFont(Font.BOLD, 17f));
        h.setForeground(new Color(0x22, 0x26, 0x2B));
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(h);

        if (subText != null) {
            JLabel sub = new JLabel("<html><body style='width:520px'>" + subText + "</body></html>");
            sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12f));
            sub.setForeground(new Color(0x60, 0x66, 0x6E));
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            sub.setBorder(BorderFactory.createEmptyBorder(4, 0, 12, 0));
            card.add(sub);
        }
        return card;
    }

    // ----------------------------------------------------------------- step 1

    private JComponent buildStep1() {
        JPanel card = (JPanel) buildCard("Select a Belief",
                "Choose the empowering belief you want to deposit into your emotional bank. "
                        + "Type below to filter the list, then pick one.");

        JLabel searchLbl = new JLabel("Search beliefs");
        searchLbl.setFont(searchLbl.getFont().deriveFont(Font.BOLD, 12f));
        searchLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchField = new JTextField();
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterBeliefs(); }
            public void removeUpdate(DocumentEvent e) { filterBeliefs(); }
            public void changedUpdate(DocumentEvent e) { filterBeliefs(); }
        });

        JLabel comboLbl = new JLabel("Belief");
        comboLbl.setFont(comboLbl.getFont().deriveFont(Font.BOLD, 12f));
        comboLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboLbl.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        beliefCombo = new JComboBox<Belief>();
        beliefCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        beliefCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        rebuildComboModel(Beliefs.all());
        beliefCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (suppressComboEvents) {
                    return;
                }
                Object sel = beliefCombo.getSelectedItem();
                if (sel instanceof Belief) {
                    state.setSelectedBelief((Belief) sel);
                }
            }
        });

        card.add(searchLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(searchField);
        card.add(comboLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(beliefCombo);
        card.add(Box.createVerticalGlue());

        return wrapTop(card);
    }

    private void rebuildComboModel(List<Belief> items) {
        DefaultComboBoxModel<Belief> model = new DefaultComboBoxModel<Belief>();
        for (Belief b : items) {
            model.addElement(b);
        }
        beliefCombo.setModel(model);
    }

    private void filterBeliefs() {
        String q = searchField.getText().trim().toLowerCase();
        Belief previouslySelected = (Belief) beliefCombo.getSelectedItem();

        DefaultComboBoxModel<Belief> model = new DefaultComboBoxModel<Belief>();
        for (Belief b : Beliefs.all()) {
            String name = b.getName();
            if (q.isEmpty() || (name != null && name.toLowerCase().contains(q))) {
                model.addElement(b);
            }
        }

        suppressComboEvents = true;
        beliefCombo.setModel(model);
        if (previouslySelected != null && model.getIndexOf(previouslySelected) >= 0) {
            beliefCombo.setSelectedItem(previouslySelected);
        } else if (model.getSize() > 0) {
            beliefCombo.setSelectedIndex(-1);
        }
        suppressComboEvents = false;

        // If exactly one match, treat it as selected so state stays in sync.
        if (model.getSize() == 1) {
            beliefCombo.setSelectedIndex(0);
        }
    }

    // ----------------------------------------------------------------- step 2

    private JComponent buildStep2() {
        JPanel card = (JPanel) buildCard("Collect Proof",
                "Write three concrete, real-life examples that prove this belief is true for you. "
                        + "Be specific — actual moments, people and outcomes carry the most weight.");

        JPanel grid = new JPanel(new GridLayout(3, 1, 0, 12));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (int i = 0; i < 3; i++) {
            JTextArea area = new JTextArea(3, 20);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setFont(area.getFont().deriveFont(13f));
            area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            proofAreas[i] = area;

            JPanel block = new JPanel(new BorderLayout(0, 4));
            block.setOpaque(false);
            JLabel lbl = new JLabel("Example #" + (i + 1));
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
            block.add(lbl, BorderLayout.NORTH);

            JScrollPane scroll = new JScrollPane(area);
            scroll.setBorder(BorderFactory.createLineBorder(new Color(0xD5, 0xD9, 0xDF)));
            block.add(scroll, BorderLayout.CENTER);

            grid.add(block);
        }

        card.add(grid);
        return wrapTop(card);
    }

    // ----------------------------------------------------------------- step 3

    private JComponent buildStep3() {
        JPanel card = (JPanel) buildCard("Build Proof Through Action",
                "Beliefs are reinforced by action. Name one concrete thing you will do TODAY "
                        + "to add a fresh deposit of proof.");

        JLabel lbl = new JLabel("Today's action");
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        actionArea = new JTextArea(4, 20);
        actionArea.setLineWrap(true);
        actionArea.setWrapStyleWord(true);
        actionArea.setFont(actionArea.getFont().deriveFont(13f));
        actionArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JScrollPane scroll = new JScrollPane(actionArea);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xD5, 0xD9, 0xDF)));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        card.add(lbl);
        card.add(Box.createVerticalStrut(4));
        card.add(scroll);
        card.add(Box.createVerticalGlue());

        return wrapTop(card);
    }

    private JComponent wrapTop(JComponent inner) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(inner, BorderLayout.NORTH);
        return holder;
    }

    // ------------------------------------------------------------- navigation

    private JComponent buildNavBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        backButton = new JButton("← Back");
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                syncCurrentStep();
                if (currentStep > 0) {
                    showStep(currentStep - 1);
                }
            }
        });

        nextButton = new JButton("Next →");
        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                syncCurrentStep();
                if (currentStep < STEP_COUNT - 1) {
                    showStep(currentStep + 1);
                }
            }
        });

        saveButton = new JButton("Save / Export");
        saveButton.setBackground(ACCENT);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD));
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSaveExport();
            }
        });

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(nextButton);
        right.add(Box.createHorizontalStrut(8));
        right.add(saveButton);

        bar.add(backButton, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void showStep(int step) {
        currentStep = step;
        cardLayout.show(cards, "step" + step);
        stepLabel.setText("Step " + (step + 1) + " of " + STEP_COUNT + "  ·  " + STEP_TITLES[step]);
        backButton.setEnabled(step > 0);
        nextButton.setVisible(step < STEP_COUNT - 1);
        saveButton.setVisible(step == STEP_COUNT - 1);
    }

    // --------------------------------------------------------------- state IO

    private void loadFromState() {
        Belief selected = state.getSelectedBelief();
        if (selected != null) {
            suppressComboEvents = true;
            beliefCombo.setSelectedItem(selected);
            suppressComboEvents = false;
        }
        for (int i = 0; i < 3; i++) {
            String p = state.getProof(i);
            if (p != null) {
                proofAreas[i].setText(p);
            }
        }
        String action = state.getDailyAction();
        if (action != null) {
            actionArea.setText(action);
        }
    }

    private void syncCurrentStep() {
        switch (currentStep) {
            case 0:
                Object sel = beliefCombo.getSelectedItem();
                if (sel instanceof Belief) {
                    state.setSelectedBelief((Belief) sel);
                }
                break;
            case 1:
                for (int i = 0; i < 3; i++) {
                    state.setProof(i, proofAreas[i].getText());
                }
                break;
            case 2:
                state.setDailyAction(actionArea.getText());
                break;
            default:
                break;
        }
    }

    private void syncAll() {
        Object sel = beliefCombo.getSelectedItem();
        if (sel instanceof Belief) {
            state.setSelectedBelief((Belief) sel);
        }
        for (int i = 0; i < 3; i++) {
            state.setProof(i, proofAreas[i].getText());
        }
        state.setDailyAction(actionArea.getText());
    }

    // -------------------------------------------------------------- save flow

    private void onSaveExport() {
        syncAll();

        ValidationResult result = validator.validate(state);
        if (result == null || !result.isValid()) {
            List<String> errors = (result == null) ? null : result.getErrors();
            showValidationErrors(errors);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Emotional Bank Statement");
        chooser.setSelectedFile(new File("emotional-bank-statement.txt"));
        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File target = chooser.getSelectedFile();
        try {
            exporter.exportToFile(state, target);
            JOptionPane.showMessageDialog(this,
                    "Your Emotional Bank Statement was exported to:\n" + target.getAbsolutePath(),
                    "Export complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not export the file:\n" + ex.getMessage(),
                    "Export failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showValidationErrors(List<String> errors) {
        StringBuilder sb = new StringBuilder();
        sb.append("Please fix the following before exporting:\n\n");
        if (errors == null || errors.isEmpty()) {
            sb.append("• The statement is incomplete.");
        } else {
            for (String err : errors) {
                sb.append("• ").append(err).append("\n");
            }
        }
        JOptionPane.showMessageDialog(this, sb.toString().trim(),
                "Cannot export yet", JOptionPane.WARNING_MESSAGE);
    }
}
