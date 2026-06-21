package com.whim.hbdi.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import com.whim.hbdi.domain.DefaultQuestionBank;
import com.whim.hbdi.domain.QuadrantScore;
import com.whim.hbdi.domain.Question;
import com.whim.hbdi.domain.QuestionBank;
import com.whim.hbdi.domain.Response;
import com.whim.hbdi.scoring.DefaultScoringEngine;
import com.whim.hbdi.scoring.DefaultSurveyValidator;
import com.whim.hbdi.scoring.ScoringEngine;
import com.whim.hbdi.scoring.SurveyValidator;
import com.whim.hbdi.scoring.ValidationResult;

/**
 * Main application window for the HBDI survey.
 *
 * Builds a CardLayout wizard over the 116 questions (one question per card)
 * with a progress indicator, Back/Next navigation that blocks advancing past
 * an unanswered question, and a final results card with a custom Graphics2D
 * quadrant chart plus a "Save Report" export.
 *
 * Wires the real domain + scoring implementations:
 * {@link DefaultQuestionBank}, {@link DefaultSurveyValidator},
 * {@link DefaultScoringEngine}.
 */
public class MainFrame extends JFrame {

    private static final String CARD_INTRO = "intro";
    private static final String CARD_SURVEY = "survey";
    private static final String CARD_RESULTS = "results";

    private final QuestionBank questionBank;
    private final SurveyValidator validator;
    private final ScoringEngine scoringEngine;

    private final List<Question> questions;
    /** questionId -> chosen Likert value (1..5). */
    private final Map<Integer, Integer> answers = new HashMap<Integer, Integer>();

    private final CardLayout rootLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(rootLayout);

    private final CardLayout questionLayout = new CardLayout();
    private final JPanel questionCards = new JPanel(questionLayout);
    private final List<QuestionPanel> questionPanels = new ArrayList<QuestionPanel>();

    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel progressLabel = new JLabel();
    private final JButton backButton = new JButton("◀ Back");
    private final JButton nextButton = new JButton("Next ▶");

    private final ResultsPanel resultsPanel;

    private int currentIndex = 0;

    public MainFrame() {
        this(new DefaultQuestionBank(), new DefaultSurveyValidator(), new DefaultScoringEngine());
    }

    /** Package/explicit constructor mainly for testing with custom collaborators. */
    public MainFrame(QuestionBank questionBank, SurveyValidator validator, ScoringEngine scoringEngine) {
        super("HBDI — Whole Brain Thinking Survey");
        this.questionBank = questionBank;
        this.validator = validator;
        this.scoringEngine = scoringEngine;
        this.questions = new ArrayList<Question>(questionBank.getQuestions());

        this.resultsPanel = new ResultsPanel(new Runnable() {
            public void run() {
                restart();
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 620));
        setLocationByPlatform(true);

        rootPanel.add(buildIntroCard(), CARD_INTRO);
        rootPanel.add(buildSurveyCard(), CARD_SURVEY);
        rootPanel.add(wrapScroll(resultsPanel), CARD_RESULTS);

        setContentPane(rootPanel);
        rootLayout.show(rootPanel, CARD_INTRO);
        pack();
        setSize(new Dimension(820, 680));
    }

    private JScrollPane wrapScroll(JPanel inner) {
        JScrollPane sp = new JScrollPane(inner,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JPanel buildIntroCard() {
        JPanel panel = new JPanel(new BorderLayout(0, 24));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(48, 56, 48, 56));

        JLabel title = new JLabel("Herrmann Brain Dominance Instrument");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));

        int total = questions.size();
        JLabel body = new JLabel("<html><div style='width:560px'>"
                + "This short survey contains <b>" + total + " questions</b>. "
                + "For each statement, choose how strongly you agree on a 1–5 scale. "
                + "Your answers are scored across four thinking-style quadrants:<br><br>"
                + "&nbsp;&nbsp;<b>A</b> — Analytical &nbsp;&nbsp; <b>B</b> — Sequential<br>"
                + "&nbsp;&nbsp;<b>C</b> — Interpersonal &nbsp;&nbsp; <b>D</b> — Imaginative<br><br>"
                + "You must answer each question before moving on. At the end you'll see "
                + "your whole-brain profile and can save a report."
                + "</div></html>");
        body.setFont(body.getFont().deriveFont(Font.PLAIN, 15f));

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);
        center.add(title, BorderLayout.NORTH);
        center.add(body, BorderLayout.CENTER);
        panel.add(center, BorderLayout.NORTH);

        JButton start = new JButton("Start Survey ▶");
        start.setFont(start.getFont().deriveFont(Font.BOLD, 15f));
        start.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                currentIndex = 0;
                showQuestion(0);
                rootLayout.show(rootPanel, CARD_SURVEY);
            }
        });
        JPanel startWrap = new JPanel(new BorderLayout());
        startWrap.setOpaque(false);
        startWrap.add(start, BorderLayout.WEST);
        panel.add(startWrap, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildSurveyCard() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(Color.WHITE);

        // Progress header.
        JPanel header = new JPanel(new BorderLayout(12, 6));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(18, 28, 8, 28));
        progressLabel.setFont(progressLabel.getFont().deriveFont(Font.BOLD, 14f));
        progressBar.setMinimum(0);
        progressBar.setMaximum(questions.size());
        progressBar.setStringPainted(true);
        header.add(progressLabel, BorderLayout.NORTH);
        header.add(progressBar, BorderLayout.SOUTH);
        panel.add(header, BorderLayout.NORTH);

        // One QuestionPanel per question inside an inner CardLayout.
        QuestionPanel.AnswerListener answerListener = new QuestionPanel.AnswerListener() {
            public void onAnswered(int questionId, int value) {
                answers.put(questionId, value);
                updateNavState();
            }
        };
        int total = questions.size();
        for (int i = 0; i < total; i++) {
            Question q = questions.get(i);
            QuestionPanel qp = new QuestionPanel(q, i + 1, total, answerListener);
            questionPanels.add(qp);
            questionCards.add(wrapScroll(qp), Integer.toString(i));
        }
        questionCards.setBackground(Color.WHITE);
        panel.add(questionCards, BorderLayout.CENTER);

        // Navigation footer.
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 28, 20, 28));

        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                goBack();
            }
        });
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                goNext();
            }
        });
        nextButton.setFont(nextButton.getFont().deriveFont(Font.BOLD));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(backButton, BorderLayout.WEST);
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(nextButton, BorderLayout.EAST);
        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);

        JLabel center = new JLabel("Answer every question to continue", SwingConstants.CENTER);
        center.setForeground(new Color(0x88, 0x88, 0x88));
        footer.add(center, BorderLayout.CENTER);

        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private void showQuestion(int index) {
        currentIndex = index;
        questionLayout.show(questionCards, Integer.toString(index));
        QuestionPanel qp = questionPanels.get(index);
        Integer prior = answers.get(qp.getQuestion().getId());
        qp.setSelectedValue(prior == null ? 0 : prior.intValue());
        updateNavState();
    }

    private void updateNavState() {
        int total = questions.size();
        int oneBased = currentIndex + 1;
        progressLabel.setText("Question " + oneBased + " / " + total
                + "   (" + answers.size() + " answered)");
        progressBar.setValue(oneBased);
        progressBar.setString(oneBased + " / " + total);

        backButton.setEnabled(currentIndex > 0);

        boolean answered = answers.containsKey(questions.get(currentIndex).getId());
        nextButton.setEnabled(answered);
        nextButton.setText(currentIndex == total - 1 ? "See Results ▶" : "Next ▶");
    }

    private void goBack() {
        if (currentIndex > 0) {
            showQuestion(currentIndex - 1);
        }
    }

    private void goNext() {
        Question current = questions.get(currentIndex);
        if (!answers.containsKey(current.getId())) {
            JOptionPane.showMessageDialog(this,
                    "Please choose an answer before continuing.",
                    "Question unanswered", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentIndex < questions.size() - 1) {
            showQuestion(currentIndex + 1);
        } else {
            finishSurvey();
        }
    }

    private void finishSurvey() {
        Map<Integer, Response> responses = buildResponses();
        ValidationResult validation = validator.validate(questions, responses);
        if (!validation.isComplete()) {
            List<Integer> missing = validation.getUnansweredIds();
            int firstMissingIndex = indexOfQuestionId(missing == null || missing.isEmpty()
                    ? -1 : missing.get(0).intValue());
            JOptionPane.showMessageDialog(this,
                    "Some questions are still unanswered ("
                            + (missing == null ? 0 : missing.size()) + "). "
                            + "Returning to the first one.",
                    "Survey incomplete", JOptionPane.WARNING_MESSAGE);
            if (firstMissingIndex >= 0) {
                showQuestion(firstMissingIndex);
            }
            return;
        }
        List<QuadrantScore> scores = scoringEngine.score(questions, responses);
        resultsPanel.setScores(scores);
        rootLayout.show(rootPanel, CARD_RESULTS);
    }

    /** Build the responses map expected by the scoring engine. */
    private Map<Integer, Response> buildResponses() {
        Map<Integer, Response> responses = new HashMap<Integer, Response>();
        for (Map.Entry<Integer, Integer> entry : answers.entrySet()) {
            final int questionId = entry.getKey().intValue();
            final int value = entry.getValue().intValue();
            responses.put(questionId, new Response() {
                public int getQuestionId() { return questionId; }
                public int getValue() { return value; }
            });
        }
        return responses;
    }

    private int indexOfQuestionId(int questionId) {
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getId() == questionId) {
                return i;
            }
        }
        return -1;
    }

    private void restart() {
        answers.clear();
        for (QuestionPanel qp : questionPanels) {
            qp.setSelectedValue(0);
        }
        currentIndex = 0;
        rootLayout.show(rootPanel, CARD_INTRO);
    }
}
