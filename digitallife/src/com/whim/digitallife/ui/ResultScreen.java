package com.whim.digitallife.ui;

import com.whim.digitallife.io.ResultsIO;
import com.whim.digitallife.model.ResultProfile;
import com.whim.digitallife.model.Trait;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * The results screen: shows the dominant trait, a generated summary, a
 * Graphics2D bar chart of all five dimensions, and offline actions to save the
 * results to a local text file or retake the quiz.
 */
public final class ResultScreen extends JPanel {

    private final QuizFrame frame;
    private final JLabel dominantLabel = new JLabel();
    private final JLabel summaryLabel = new JLabel();
    private final DominantBadge badge = new DominantBadge();
    private final BarChartPanel chart = new BarChartPanel();

    private ResultProfile profile;
    private boolean live;

    /**
     * @param frame the hosting frame used for navigation
     */
    public ResultScreen(QuizFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(28, 48, 28, 48));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));

        badge.setAlignmentY(Component.TOP_ALIGNMENT);
        header.add(badge);
        header.add(Box.createHorizontalStrut(18));

        JPanel textCol = new JPanel();
        textCol.setOpaque(false);
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));

        JLabel kicker = new JLabel("YOUR RESULT");
        kicker.setFont(Theme.SMALL_FONT.deriveFont((float) 13));
        kicker.setForeground(Theme.ACCENT_SOFT);
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        textCol.add(kicker);
        textCol.add(Box.createVerticalStrut(4));

        dominantLabel.setFont(Theme.TITLE_FONT);
        dominantLabel.setForeground(Theme.TEXT);
        dominantLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textCol.add(dominantLabel);

        header.add(textCol);
        header.add(Box.createHorizontalGlue());
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        return header;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        summaryLabel.setFont(Theme.BODY_FONT);
        summaryLabel.setForeground(Theme.TEXT_MUTED);
        summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(summaryLabel);
        center.add(Box.createVerticalStrut(20));

        chart.setAlignmentX(Component.LEFT_ALIGNMENT);
        chart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        center.add(chart);
        return center;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));

        JButton saveButton = Theme.secondaryButton("💾  Save My Results");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveResults();
            }
        });

        JButton retakeButton = Theme.primaryButton("↻  Retake Quiz");
        retakeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.retake();
            }
        });

        footer.add(saveButton, BorderLayout.WEST);
        footer.add(retakeButton, BorderLayout.EAST);
        return footer;
    }

    /**
     * Shows a freshly computed profile from the current quiz session (the answer
     * transcript is available and will be included when saving).
     *
     * @param profile the computed results
     */
    public void showLiveProfile(ResultProfile profile) {
        this.live = true;
        render(profile);
    }

    /**
     * Shows a profile loaded from disk (no per-question transcript available).
     *
     * @param profile the reconstructed results
     */
    public void showLoadedProfile(ResultProfile profile) {
        this.live = false;
        render(profile);
    }

    private void render(ResultProfile profile) {
        this.profile = profile;
        Trait dominant = profile.getDominant();
        dominantLabel.setText(dominant.getDisplayName());
        badge.setColor(dominant.getColor());
        summaryLabel.setText("<html><div style='width:620px;'>"
                + profile.getSummary() + "</div></html>");
        chart.setProfile(profile);
        revalidate();
        repaint();
    }

    /** Saves the current profile to a user-chosen local text file. */
    private void saveResults() {
        if (profile == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save your results");
        chooser.setSelectedFile(new File("my-digitallife-results.txt"));
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        int outcome = chooser.showSaveDialog(this);
        if (outcome != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = ensureTxt(chooser.getSelectedFile());
        try {
            if (live) {
                ResultsIO.save(file, profile,
                        frame.getController().getQuestions(),
                        frame.getController().getSelectedAnswers());
            } else {
                ResultsIO.save(file, profile);
            }
            JOptionPane.showMessageDialog(this,
                    "Saved to:\n" + file.getAbsolutePath(),
                    "Results saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Couldn't save the file:\n" + ex.getMessage(),
                    "Save failed", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static File ensureTxt(File file) {
        if (file.getName().toLowerCase().endsWith(".txt")) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + ".txt");
    }

    /** A round color badge echoing the dominant trait's theme color. */
    private static final class DominantBadge extends JPanel {
        private java.awt.Color color = Theme.ACCENT;

        DominantBadge() {
            setOpaque(false);
            setPreferredSize(new Dimension(64, 64));
            setMaximumSize(new Dimension(64, 64));
        }

        void setColor(java.awt.Color color) {
            this.color = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(6, 6, 50, 50);
                g2.setColor(java.awt.Color.WHITE);
                g2.fillOval(24, 24, 14, 14);
            } finally {
                g2.dispose();
            }
        }
    }
}
