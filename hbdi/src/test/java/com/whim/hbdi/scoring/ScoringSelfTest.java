package com.whim.hbdi.scoring;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.whim.hbdi.domain.Quadrant;
import com.whim.hbdi.domain.QuadrantScore;
import com.whim.hbdi.domain.Question;
import com.whim.hbdi.domain.Response;

/**
 * Dependency-free self-check for the scoring + validation classes. Run with
 * {@code java com.whim.hbdi.scoring.ScoringSelfTest}. Prints per-assertion
 * PASS/FAIL lines and exits non-zero if any assertion fails.
 */
public final class ScoringSelfTest {

    private static int failures = 0;

    public static void main(String[] args) {
        testValidationComplete();
        testValidationIncomplete();
        testFourScoresAndSumTo100();
        testAllOneQuadrant();
        testEmptySurveyNoCrash();
        testWeightedSplit();

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    // ---- tests -------------------------------------------------------------

    private static void testValidationComplete() {
        List<Question> questions = sampleQuestions();
        Map<Integer, Response> responses = new HashMap<Integer, Response>();
        for (Question q : questions) {
            responses.put(q.getId(), resp(q.getId(), 3));
        }
        ValidationResult vr = new DefaultSurveyValidator().validate(questions, responses);
        check("validation: complete survey flagged complete", vr.isComplete());
        check("validation: complete survey has no unanswered ids",
                vr.getUnansweredIds().isEmpty());
    }

    private static void testValidationIncomplete() {
        List<Question> questions = sampleQuestions();
        Map<Integer, Response> responses = new HashMap<Integer, Response>();
        // answer all but ids 2 and 4
        for (Question q : questions) {
            if (q.getId() != 2 && q.getId() != 4) {
                responses.put(q.getId(), resp(q.getId(), 4));
            }
        }
        ValidationResult vr = new DefaultSurveyValidator().validate(questions, responses);
        check("validation: incomplete survey flagged incomplete", !vr.isComplete());
        check("validation: exactly two unanswered ids",
                vr.getUnansweredIds().size() == 2);
        check("validation: unanswered ids are [2, 4]",
                vr.getUnansweredIds().contains(2) && vr.getUnansweredIds().contains(4));
    }

    private static void testFourScoresAndSumTo100() {
        List<Question> questions = sampleQuestions();
        Map<Integer, Response> responses = new HashMap<Integer, Response>();
        for (Question q : questions) {
            responses.put(q.getId(), resp(q.getId(), (q.getId() % 5) + 1));
        }
        List<QuadrantScore> scores = new DefaultScoringEngine().score(questions, responses);
        check("scoring: exactly four scores returned", scores.size() == 4);

        // ordered A, B, C, D
        Quadrant[] order = Quadrant.values();
        boolean ordered = true;
        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i).getQuadrant() != order[i]) {
                ordered = false;
            }
        }
        check("scoring: scores ordered A,B,C,D", ordered);

        double sum = 0.0;
        for (QuadrantScore s : scores) {
            sum += s.getPercentage();
        }
        check("scoring: percentages sum to ~100 (got " + round(sum) + ")",
                Math.abs(sum - 100.0) < 1e-6);
    }

    private static void testAllOneQuadrant() {
        // questions whose weight is entirely in quadrant A
        List<Question> questions = new ArrayList<Question>();
        for (int id = 1; id <= 6; id++) {
            Map<Quadrant, Integer> w = new EnumMap<Quadrant, Integer>(Quadrant.class);
            w.put(Quadrant.A, 1);
            w.put(Quadrant.B, 0);
            w.put(Quadrant.C, 0);
            w.put(Quadrant.D, 0);
            questions.add(question(id, "cat", "q" + id, w));
        }
        Map<Integer, Response> responses = new HashMap<Integer, Response>();
        for (Question q : questions) {
            responses.put(q.getId(), resp(q.getId(), 5));
        }
        List<QuadrantScore> scores = new DefaultScoringEngine().score(questions, responses);
        QuadrantScore a = byQuadrant(scores, Quadrant.A);
        check("scoring: all-one-quadrant input yields 100% in A (got "
                + round(a.getPercentage()) + ")",
                Math.abs(a.getPercentage() - 100.0) < 1e-6);
        boolean othersZero = byQuadrant(scores, Quadrant.B).getPercentage() == 0.0
                && byQuadrant(scores, Quadrant.C).getPercentage() == 0.0
                && byQuadrant(scores, Quadrant.D).getPercentage() == 0.0;
        check("scoring: all-one-quadrant input yields 0% elsewhere", othersZero);
    }

    private static void testEmptySurveyNoCrash() {
        List<Question> questions = sampleQuestions();
        Map<Integer, Response> responses = new HashMap<Integer, Response>();
        List<QuadrantScore> scores = new DefaultScoringEngine().score(questions, responses);
        check("scoring: no responses still returns four scores", scores.size() == 4);
        double sum = 0.0;
        for (QuadrantScore s : scores) {
            sum += s.getPercentage();
        }
        check("scoring: no responses -> all percentages 0 (no div-by-zero)", sum == 0.0);
    }

    private static void testWeightedSplit() {
        // one question, weights A=3 B=1 C=0 D=0, likert 2 -> raw A=6, B=2 -> 75/25
        Map<Quadrant, Integer> w = new EnumMap<Quadrant, Integer>(Quadrant.class);
        w.put(Quadrant.A, 3);
        w.put(Quadrant.B, 1);
        w.put(Quadrant.C, 0);
        w.put(Quadrant.D, 0);
        List<Question> questions = new ArrayList<Question>();
        questions.add(question(1, "cat", "q1", w));
        Map<Integer, Response> responses = new HashMap<Integer, Response>();
        responses.put(1, resp(1, 2));
        List<QuadrantScore> scores = new DefaultScoringEngine().score(questions, responses);
        QuadrantScore a = byQuadrant(scores, Quadrant.A);
        QuadrantScore b = byQuadrant(scores, Quadrant.B);
        check("scoring: weighted raw A == 6 (got " + round(a.getRawScore()) + ")",
                Math.abs(a.getRawScore() - 6.0) < 1e-9);
        check("scoring: weighted A == 75% (got " + round(a.getPercentage()) + ")",
                Math.abs(a.getPercentage() - 75.0) < 1e-9);
        check("scoring: weighted B == 25% (got " + round(b.getPercentage()) + ")",
                Math.abs(b.getPercentage() - 25.0) < 1e-9);
    }

    // ---- helpers -----------------------------------------------------------

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS: " : "FAIL: ") + name);
        if (!ok) {
            failures++;
        }
    }

    private static QuadrantScore byQuadrant(List<QuadrantScore> scores, Quadrant q) {
        for (QuadrantScore s : scores) {
            if (s.getQuadrant() == q) {
                return s;
            }
        }
        throw new IllegalStateException("no score for " + q);
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static List<Question> sampleQuestions() {
        List<Question> questions = new ArrayList<Question>();
        for (int id = 1; id <= 8; id++) {
            Map<Quadrant, Integer> w = new EnumMap<Quadrant, Integer>(Quadrant.class);
            w.put(Quadrant.A, (id % 4 == 0) ? 2 : 1);
            w.put(Quadrant.B, (id % 3 == 0) ? 2 : 1);
            w.put(Quadrant.C, 1);
            w.put(Quadrant.D, (id % 2 == 0) ? 1 : 0);
            questions.add(question(id, "category" + id, "Question " + id, w));
        }
        return questions;
    }

    private static Question question(final int id, final String category,
                                     final String text,
                                     final Map<Quadrant, Integer> weights) {
        return new Question() {
            @Override
            public int getId() {
                return id;
            }

            @Override
            public String getText() {
                return text;
            }

            @Override
            public String getCategory() {
                return category;
            }

            @Override
            public Map<Quadrant, Integer> getQuadrantWeights() {
                return weights;
            }
        };
    }

    private static Response resp(final int questionId, final int value) {
        return new Response() {
            @Override
            public int getQuestionId() {
                return questionId;
            }

            @Override
            public int getValue() {
                return value;
            }
        };
    }

    private ScoringSelfTest() {
    }
}
