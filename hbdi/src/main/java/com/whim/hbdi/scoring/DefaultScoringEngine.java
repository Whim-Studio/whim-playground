package com.whim.hbdi.scoring;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.whim.hbdi.domain.Quadrant;
import com.whim.hbdi.domain.QuadrantScore;
import com.whim.hbdi.domain.Question;
import com.whim.hbdi.domain.Response;

/**
 * Default HBDI scoring. For each answered question and each quadrant, adds
 * {@code likertValue * quadrantWeight} to that quadrant's raw score. The
 * percentage for a quadrant is {@code raw / totalRaw * 100}. Always returns
 * exactly four {@link QuadrantScore}s ordered A, B, C, D.
 */
public class DefaultScoringEngine implements ScoringEngine {

    @Override
    public List<QuadrantScore> score(List<Question> questions,
                                     Map<Integer, Response> responses) {
        Map<Quadrant, Double> raw = new EnumMap<Quadrant, Double>(Quadrant.class);
        for (Quadrant quadrant : Quadrant.values()) {
            raw.put(quadrant, 0.0);
        }

        if (questions != null) {
            for (Question q : questions) {
                if (q == null) {
                    continue;
                }
                Response r = (responses == null) ? null : responses.get(q.getId());
                if (r == null) {
                    continue;
                }
                int likert = r.getValue();
                Map<Quadrant, Integer> weights = q.getQuadrantWeights();
                if (weights == null) {
                    continue;
                }
                for (Quadrant quadrant : Quadrant.values()) {
                    Integer w = weights.get(quadrant);
                    if (w == null) {
                        continue;
                    }
                    raw.put(quadrant, raw.get(quadrant) + (double) likert * w);
                }
            }
        }

        double total = 0.0;
        for (Quadrant quadrant : Quadrant.values()) {
            total += raw.get(quadrant);
        }

        List<QuadrantScore> result = new ArrayList<QuadrantScore>(4);
        for (Quadrant quadrant : Quadrant.values()) {
            double rawScore = raw.get(quadrant);
            double pct = (total > 0.0) ? (rawScore / total * 100.0) : 0.0;
            result.add(new ComputedQuadrantScore(quadrant, rawScore, pct));
        }
        return result;
    }

    /**
     * Self-contained QuadrantScore implementation so scoring does not depend on
     * a particular domain constructor signature.
     */
    private static final class ComputedQuadrantScore implements QuadrantScore {
        private final Quadrant quadrant;
        private final double rawScore;
        private final double percentage;

        ComputedQuadrantScore(Quadrant quadrant, double rawScore, double percentage) {
            this.quadrant = quadrant;
            this.rawScore = rawScore;
            this.percentage = percentage;
        }

        @Override
        public Quadrant getQuadrant() {
            return quadrant;
        }

        @Override
        public double getRawScore() {
            return rawScore;
        }

        @Override
        public double getPercentage() {
            return percentage;
        }

        @Override
        public String toString() {
            return quadrant + "{raw=" + rawScore + ", pct=" + percentage + "}";
        }
    }
}
