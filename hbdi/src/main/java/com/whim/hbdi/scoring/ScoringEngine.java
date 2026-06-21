package com.whim.hbdi.scoring;

import java.util.List;
import java.util.Map;

import com.whim.hbdi.domain.QuadrantScore;
import com.whim.hbdi.domain.Question;
import com.whim.hbdi.domain.Response;

/**
 * Computes per-quadrant HBDI scores from questions and their responses.
 */
public interface ScoringEngine {

    /**
     * Returns exactly 4 QuadrantScore (one per Quadrant, ordered A,B,C,D),
     * whose percentages sum to ~100.
     */
    List<QuadrantScore> score(List<Question> questions,
                              Map<Integer, Response> responses);
}
