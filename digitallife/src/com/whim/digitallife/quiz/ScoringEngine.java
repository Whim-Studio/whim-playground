package com.whim.digitallife.quiz;

import com.whim.digitallife.model.Choice;
import com.whim.digitallife.model.Question;
import com.whim.digitallife.model.ResultProfile;
import com.whim.digitallife.model.Trait;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Pure scoring logic: turns a set of selected answers into a {@link ResultProfile}.
 *
 * <p>Kept free of any UI so it can be unit-reasoned and reused. The engine also
 * computes the theoretical per-trait maximum, which the results view uses to
 * normalize each bar to a 0-100% scale.</p>
 */
public final class ScoringEngine {

    private ScoringEngine() {
        // Utility class; not instantiable.
    }

    /**
     * Computes the profile for a completed quiz.
     *
     * @param questions the ordered question list
     * @param selected  parallel array of chosen choice indices (-1 = unanswered)
     * @return the computed {@link ResultProfile}
     */
    public static ResultProfile score(List<Question> questions, int[] selected) {
        Map<Trait, Integer> totals = new EnumMap<Trait, Integer>(Trait.class);
        for (Trait trait : Trait.values()) {
            totals.put(trait, 0);
        }

        for (int i = 0; i < questions.size(); i++) {
            int choiceIndex = i < selected.length ? selected[i] : -1;
            if (choiceIndex < 0) {
                continue;
            }
            Choice choice = questions.get(i).getChoices().get(choiceIndex);
            for (Map.Entry<Trait, Integer> entry : choice.getWeights().entrySet()) {
                totals.put(entry.getKey(), totals.get(entry.getKey()) + entry.getValue());
            }
        }

        return new ResultProfile(totals, maxPossiblePerTrait(questions));
    }

    /**
     * Determines the largest score any single trait could reach if the user
     * always picked that trait's best-weighted option on every question.
     *
     * @param questions the question list
     * @return the maximum achievable per-trait score (at least 1)
     */
    public static int maxPossiblePerTrait(List<Question> questions) {
        // For a fair 0-100% scale we take the single most-achievable trait ceiling:
        // for each trait, sum the best per-question weight, then take the largest.
        Map<Trait, Integer> ceilings = new EnumMap<Trait, Integer>(Trait.class);
        for (Trait trait : Trait.values()) {
            ceilings.put(trait, 0);
        }
        for (Question question : questions) {
            Map<Trait, Integer> bestForQuestion = new EnumMap<Trait, Integer>(Trait.class);
            for (Choice choice : question.getChoices()) {
                for (Map.Entry<Trait, Integer> entry : choice.getWeights().entrySet()) {
                    Trait trait = entry.getKey();
                    int current = bestForQuestion.containsKey(trait) ? bestForQuestion.get(trait) : 0;
                    bestForQuestion.put(trait, Math.max(current, entry.getValue()));
                }
            }
            for (Map.Entry<Trait, Integer> entry : bestForQuestion.entrySet()) {
                ceilings.put(entry.getKey(), ceilings.get(entry.getKey()) + entry.getValue());
            }
        }
        int max = 1;
        for (Integer ceiling : ceilings.values()) {
            max = Math.max(max, ceiling);
        }
        return max;
    }
}
