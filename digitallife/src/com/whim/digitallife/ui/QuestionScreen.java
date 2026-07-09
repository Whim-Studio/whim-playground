package com.whim.digitallife.ui;

import com.whim.digitallife.model.Choice;
import com.whim.digitallife.model.Question;
import com.whim.digitallife.quiz.QuizController;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Shows a single question at a time with a progress indicator, selectable answer
 * choices, and Back/Next navigation. Selections are pushed into the
 * {@link QuizController} immediately so they survive navigating away and back.
 */
public final class QuestionScreen extends JPanel {

    private final QuizFrame frame;
    private final JLabel progressLabel = new JLabel();
    private final ProgressBar progressBar = new ProgressBar();
    private final JLabel questionLabel = new JLabel();
    private final JPanel choicesPanel = new JPanel();
    private final JButton backButton = Theme.secondaryButton("←  Back");
    private final JButton nextButton = Theme.primaryButton("Next  →");

    private ButtonGroup group;

    /**
     * @param frame the hosting frame used for navigation
     */
    public QuestionScreen(QuizFrame frame) {
        this.frame = frame;
        setBackground(Theme.BG);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(28, 48, 28, 48));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        progressLabel.setFont(Theme.SMALL_FONT.deriveFont((float) 14));
        progressLabel.setForeground(Theme.ACCENT_SOFT);
        progressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(progressLabel);
        header.add(Box.createVerticalStrut(8));

        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(progressBar);
        header.add(Box.createVerticalStrut(24));
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        questionLabel.setForeground(Theme.TEXT);
        questionLabel.setFont(Theme.HEADING_FONT);
        questionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(questionLabel);
        body.add(Box.createVerticalStrut(20));

        choicesPanel.setOpaque(false);
        choicesPanel.setLayout(new BoxLayout(choicesPanel, BoxLayout.Y_AXIS));
        choicesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(choicesPanel);
        return body;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.goBack();
            }
        });
        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onNext();
            }
        });

        footer.add(backButton, BorderLayout.WEST);
        footer.add(nextButton, BorderLayout.EAST);
        return footer;
    }

    private void onNext() {
        QuizController controller = frame.getController();
        if (controller.getSelectedForCurrent() < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please pick an answer to continue.",
                    "Choose an option", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        frame.goNext();
    }

    /**
     * Rebuilds the screen for the controller's current question, restoring any
     * previously chosen answer.
     */
    public void refresh() {
        QuizController controller = frame.getController();
        int index = controller.getCurrentIndex();
        int total = controller.getQuestionCount();

        progressLabel.setText("Question " + (index + 1) + " of " + total);
        progressBar.setProgress((index + 1) / (float) total);

        Question question = controller.getCurrentQuestion();
        questionLabel.setText("<html><div style='width:600px;'>"
                + escape(question.getPrompt()) + "</div></html>");

        choicesPanel.removeAll();
        group = new ButtonGroup();
        List<Choice> choices = question.getChoices();
        int previouslySelected = controller.getSelectedForCurrent();

        for (int i = 0; i < choices.size(); i++) {
            final int choiceIndex = i;
            JRadioButton radio = new JRadioButton(choices.get(i).getLabel());
            radio.setFont(Theme.BODY_FONT);
            radio.setForeground(Theme.TEXT);
            radio.setOpaque(true);
            radio.setBackground(Theme.SURFACE);
            radio.setFocusPainted(false);
            radio.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            radio.setAlignmentX(Component.LEFT_ALIGNMENT);
            radio.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            if (i == previouslySelected) {
                radio.setSelected(true);
            }
            radio.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    frame.getController().selectForCurrent(choiceIndex);
                }
            });
            group.add(radio);
            choicesPanel.add(radio);
            choicesPanel.add(Box.createVerticalStrut(10));
        }

        backButton.setEnabled(!controller.isFirst());
        nextButton.setText(controller.isLast() ? "See Results  ★" : "Next  →");

        revalidate();
        repaint();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** A slim, rounded progress bar painted with Graphics2D. */
    private static final class ProgressBar extends JPanel {
        private float progress;

        ProgressBar() {
            setOpaque(false);
            setPreferredSize(new Dimension(600, 10));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        }

        void setProgress(float value) {
            this.progress = Math.max(0f, Math.min(1f, value));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int h = getHeight();
                int w = getWidth();
                g2.setColor(new Color(0x2C, 0x30, 0x44));
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(0, 0, (int) (w * progress), h, h, h);
            } finally {
                g2.dispose();
            }
        }
    }
}
