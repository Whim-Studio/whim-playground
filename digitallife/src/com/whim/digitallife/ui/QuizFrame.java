package com.whim.digitallife.ui;

import com.whim.digitallife.model.ResultProfile;
import com.whim.digitallife.quiz.QuizController;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;

/**
 * The main application window. Hosts every screen in a {@link CardLayout} and
 * exposes navigation methods the screens call to move between cards.
 *
 * <p>This is the composition root that wires the {@link QuizController} (state)
 * to the three views (welcome, question, result).</p>
 */
public final class QuizFrame extends JFrame {

    private static final String CARD_WELCOME = "welcome";
    private static final String CARD_QUESTION = "question";
    private static final String CARD_RESULT = "result";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private final QuizController controller;
    private final WelcomeScreen welcomeScreen;
    private final QuestionScreen questionScreen;
    private final ResultScreen resultScreen;

    /** Builds the window, all screens, and shows the welcome card. */
    public QuizFrame() {
        super("thisisyourdigitallife — Personality Quiz");
        this.controller = new QuizController();

        this.welcomeScreen = new WelcomeScreen(this);
        this.questionScreen = new QuestionScreen(this);
        this.resultScreen = new ResultScreen(this);

        cards.add(welcomeScreen, CARD_WELCOME);
        cards.add(questionScreen, CARD_QUESTION);
        cards.add(resultScreen, CARD_RESULT);

        setContentPane(cards);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 620));
        setPreferredSize(new Dimension(820, 680));
        pack();
        setLocationRelativeTo(null);
        showWelcome();
    }

    /** @return the controller backing the current quiz session. */
    public QuizController getController() {
        return controller;
    }

    /** Shows the welcome/start screen. */
    public void showWelcome() {
        cardLayout.show(cards, CARD_WELCOME);
    }

    /** Begins the quiz at the first question. */
    public void startQuiz() {
        questionScreen.refresh();
        cardLayout.show(cards, CARD_QUESTION);
    }

    /** Advances to the next question, or to the results if on the last one. */
    public void goNext() {
        if (controller.isLast()) {
            showResults();
        } else {
            controller.next();
            questionScreen.refresh();
        }
    }

    /** Moves back one question. */
    public void goBack() {
        controller.back();
        questionScreen.refresh();
    }

    /** Scores the quiz and shows the results screen. */
    public void showResults() {
        ResultProfile profile = controller.computeProfile();
        resultScreen.showLiveProfile(profile);
        cardLayout.show(cards, CARD_RESULT);
    }

    /**
     * Displays a profile loaded from disk on the results screen.
     *
     * @param profile the reconstructed profile to show
     */
    public void showLoadedResults(ResultProfile profile) {
        resultScreen.showLoadedProfile(profile);
        cardLayout.show(cards, CARD_RESULT);
    }

    /** Clears all answers and returns to the welcome screen for a fresh run. */
    public void retake() {
        controller.reset();
        showWelcome();
    }
}
