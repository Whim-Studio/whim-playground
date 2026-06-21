package com.whim.hbdi.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import com.whim.hbdi.domain.Question;

/**
 * Renders a single HBDI question with a 1..5 Likert radio scale.
 * Notifies a listener whenever the selected value changes so the wizard
 * can enable/disable navigation.
 */
public class QuestionPanel extends JPanel {

    /** Callback fired when the user picks (or changes) a Likert value. */
    public interface AnswerListener {
        void onAnswered(int questionId, int value);
    }

    private static final String[] SCALE_LABELS = {
        "1 - Strongly disagree",
        "2 - Disagree",
        "3 - Neutral",
        "4 - Agree",
        "5 - Strongly agree"
    };

    private final Question question;
    private final ButtonGroup group = new ButtonGroup();
    private final JRadioButton[] buttons = new JRadioButton[5];

    public QuestionPanel(Question question, int indexOneBased, int total, final AnswerListener listener) {
        this.question = question;
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);

        JLabel category = new JLabel("Question " + indexOneBased + " of " + total
                + "   •   " + safe(question.getCategory()));
        category.setFont(category.getFont().deriveFont(Font.PLAIN, 12f));
        category.setForeground(new Color(0x66, 0x66, 0x66));
        header.add(category, BorderLayout.NORTH);

        JLabel text = new JLabel("<html><div style='width:520px'>" + escape(question.getText()) + "</div></html>");
        text.setFont(text.getFont().deriveFont(Font.BOLD, 18f));
        header.add(text, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);

        JPanel scale = new JPanel(new GridLayout(5, 1, 0, 8));
        scale.setOpaque(false);
        scale.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        for (int i = 0; i < 5; i++) {
            final int value = i + 1;
            JRadioButton rb = new JRadioButton(SCALE_LABELS[i]);
            rb.setOpaque(false);
            rb.setFont(rb.getFont().deriveFont(Font.PLAIN, 14f));
            rb.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (listener != null) {
                        listener.onAnswered(QuestionPanel.this.question.getId(), value);
                    }
                }
            });
            group.add(rb);
            buttons[i] = rb;
            scale.add(rb);
        }

        JPanel scaleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        scaleWrap.setOpaque(false);
        scaleWrap.add(scale);
        add(scaleWrap, BorderLayout.CENTER);

        JLabel hint = new JLabel("Select the option that best matches you.", SwingConstants.LEFT);
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 12f));
        hint.setForeground(new Color(0x88, 0x88, 0x88));
        add(hint, BorderLayout.SOUTH);
    }

    /** Pre-select a previously chosen value (1..5), e.g. when navigating Back. */
    public void setSelectedValue(int value) {
        if (value >= 1 && value <= 5) {
            buttons[value - 1].setSelected(true);
        } else {
            group.clearSelection();
        }
    }

    public Question getQuestion() {
        return question;
    }

    public Dimension getPreferredScaleSize() {
        return new Dimension(560, 320);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
