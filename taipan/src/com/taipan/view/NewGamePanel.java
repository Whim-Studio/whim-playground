package com.taipan.view;

import com.taipan.model.GameConstants;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/** The opening screen: enter your name and firm and set sail. */
public class NewGamePanel extends JPanel {

    private final JTextField taipanField = new JTextField(GameConstants.DEFAULT_TAIPAN, 18);
    private final JTextField firmField = new JTextField(GameConstants.DEFAULT_FIRM, 18);
    private final JTextField seedField = new JTextField("", 18);
    private final JCheckBox useSeed = new JCheckBox("Use fixed random seed (debug)");

    public NewGamePanel(final GameFrame frame) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        JLabel title = new JLabel("T A I P A N !");
        title.setFont(new Font("Serif", Font.BOLD, 48));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Make your fortune in the Far East trade, circa 1860.");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 10));
        form.setMaximumSize(new Dimension(460, 150));
        form.add(new JLabel("Your name, Taipan:"));
        form.add(taipanField);
        form.add(new JLabel("Name of your firm:"));
        form.add(firmField);
        form.add(useSeed);
        form.add(seedField);
        form.add(new JLabel(""));
        form.add(new JLabel(""));

        JButton start = new JButton("Set Sail");
        start.setAlignmentX(Component.CENTER_ALIGNMENT);
        start.addActionListener(e -> {
            String taipan = taipanField.getText().trim();
            String firm = firmField.getText().trim();
            if (taipan.isEmpty()) {
                taipan = GameConstants.DEFAULT_TAIPAN;
            }
            if (firm.isEmpty()) {
                firm = GameConstants.DEFAULT_FIRM;
            }
            Long seed = null;
            if (useSeed.isSelected()) {
                try {
                    seed = Long.parseLong(seedField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Seed must be a whole number.", "Invalid seed",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            frame.startNewGame(taipan, firm, seed);
        });

        add(title);
        add(Box.createVerticalStrut(10));
        add(subtitle);
        add(Box.createVerticalStrut(30));
        add(center(form));
        add(Box.createVerticalStrut(24));
        add(start);
    }

    private JPanel center(JPanel inner) {
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.X_AXIS));
        wrap.add(Box.createHorizontalGlue());
        wrap.add(inner);
        wrap.add(Box.createHorizontalGlue());
        return wrap;
    }

    public void reset() {
        taipanField.setText(GameConstants.DEFAULT_TAIPAN);
        firmField.setText(GameConstants.DEFAULT_FIRM);
        seedField.setText("");
        useSeed.setSelected(false);
    }
}
