package com.whim.digitallife.quiz;

import com.whim.digitallife.model.Question;
import com.whim.digitallife.model.ResultProfile;

import java.util.Arrays;
import java.util.List;

/**
 * Owns quiz state and navigation: the current question index and the user's
 * selected answers. Acts as the "C" in this small MVC-ish design, sitting
 * between the {@link Question} model and the Swing views.
 *
 * <p>Answers are preserved when navigating backward and forward, so returning to
 * an earlier question re-shows the previous selection.</p>
 */
public final class QuizController {

    private final List<Question> questions;
    private final int[] selected;
    private int currentIndex;

    /** Creates a controller over the standard {@link QuizData} question set. */
    public QuizController() {
        this.questions = QuizData.buildQuestions();
        this.selected = new int[questions.size()];
        Arrays.fill(this.selected, -1);
        this.currentIndex = 0;
    }

    /** @return the immutable list of questions. */
    public List<Question> getQuestions() {
        return questions;
    }

    /** @return the total number of questions. */
    public int getQuestionCount() {
        return questions.size();
    }

    /** @return a defensive copy of the selected choice indices (-1 = unanswered). */
    public int[] getSelectedAnswers() {
        return Arrays.copyOf(selected, selected.length);
    }

    /** @return the zero-based index of the current question. */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /** @return the {@link Question} currently being shown. */
    public Question getCurrentQuestion() {
        return questions.get(currentIndex);
    }

    /** @return the chosen choice index for the current question, or -1 if none. */
    public int getSelectedForCurrent() {
        return selected[currentIndex];
    }

    /**
     * Records the user's selection for the current question.
     *
     * @param choiceIndex index into the current question's choice list
     */
    public void selectForCurrent(int choiceIndex) {
        selected[currentIndex] = choiceIndex;
    }

    /** @return true if the current question is the first one. */
    public boolean isFirst() {
        return currentIndex == 0;
    }

    /** @return true if the current question is the last one. */
    public boolean isLast() {
        return currentIndex == questions.size() - 1;
    }

    /** Advances to the next question if one exists. */
    public void next() {
        if (currentIndex < questions.size() - 1) {
            currentIndex++;
        }
    }

    /** Moves back to the previous question if one exists. */
    public void back() {
        if (currentIndex > 0) {
            currentIndex--;
        }
    }

    /** @return true when every question has a recorded answer. */
    public boolean isComplete() {
        for (int value : selected) {
            if (value < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the index of the first unanswered question, or -1 if all answered
     */
    public int firstUnansweredIndex() {
        for (int i = 0; i < selected.length; i++) {
            if (selected[i] < 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Computes the personality profile from the current answers.
     *
     * @return a freshly scored {@link ResultProfile}
     */
    public ResultProfile computeProfile() {
        return ScoringEngine.score(questions, selected);
    }

    /** Clears all answers and returns to the first question. */
    public void reset() {
        Arrays.fill(selected, -1);
        currentIndex = 0;
    }
}
