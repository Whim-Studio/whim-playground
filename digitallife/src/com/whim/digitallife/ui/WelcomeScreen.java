package com.whim.digitallife.ui;

import com.whim.digitallife.io.ResultsIO;
import com.whim.digitallife.model.ResultProfile;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * The opening screen: a drawn logo mark, the app title, a short description,
 * a "Start Quiz" call-to-action, and an offline "Load Previous Results" option.
 */
public final class WelcomeScreen extends JPanel {

    private final QuizFrame frame;

    /**
     * @param frame the hosting frame used for navigation
     */
    public WelcomeScreen(QuizFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new GridBagLayout()); // centers the single content column

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        LogoMark logo = new LogoMark();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(logo);
        column.add(Box.createVerticalStrut(18));

        JLabel title = new JLabel("thisisyourdigitallife");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(title);
        column.add(Box.createVerticalStrut(8));

        JLabel subtitle = new JLabel("A playful personality snapshot");
        subtitle.setFont(Theme.HEADING_FONT);
        subtitle.setForeground(Theme.ACCENT_SOFT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(subtitle);
        column.add(Box.createVerticalStrut(20));

        JLabel description = new JLabel(
                "<html><div style='text-align:center; width:460px;'>"
                        + "Answer 12 quick questions and discover which of five personality "
                        + "dimensions stands out for you today. Everything runs locally on "
                        + "your own machine &mdash; no accounts, no network, no data about "
                        + "anyone but you.</div></html>",
                SwingConstants.CENTER);
        description.setFont(Theme.BODY_FONT);
        description.setForeground(Theme.TEXT_MUTED);
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(description);
        column.add(Box.createVerticalStrut(28));

        JButton startButton = Theme.primaryButton("Start Quiz  →");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.setMaximumSize(new Dimension(220, 46));
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.getController().reset();
                frame.startQuiz();
            }
        });
        column.add(startButton);
        column.add(Box.createVerticalStrut(12));

        JButton loadButton = Theme.secondaryButton("Load Previous Results");
        loadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadButton.setMaximumSize(new Dimension(220, 40));
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadPreviousResults();
            }
        });
        column.add(loadButton);

        add(column);
    }

    /** Opens a file chooser and loads a previously saved results file. */
    private void loadPreviousResults() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load a previously saved results file");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        int outcome = chooser.showOpenDialog(this);
        if (outcome != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            ResultProfile profile = ResultsIO.load(file);
            frame.showLoadedResults(profile);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Couldn't load that file:\n" + ex.getMessage(),
                    "Load failed", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * A small Graphics2D logo: overlapping translucent circles suggesting a
     * friendly "profile" mark. Pure placeholder art, no external images.
     */
    private static final class LogoMark extends JPanel {
        LogoMark() {
            setOpaque(false);
            setPreferredSize(new Dimension(110, 110));
            setMaximumSize(new Dimension(110, 110));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(0x6C, 0x5C, 0xE7, 210));
                g2.fillOval(w / 2 - 42, h / 2 - 30, 52, 52);
                g2.setColor(new Color(0x00, 0xB8, 0x94, 190));
                g2.fillOval(w / 2 - 8, h / 2 - 34, 52, 52);
                g2.setColor(new Color(0xE1, 0x70, 0x55, 170));
                g2.fillOval(w / 2 - 18, h / 2 + 2, 40, 40);
            } finally {
                g2.dispose();
            }
        }
    }
}
