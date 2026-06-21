package com.xiangqi.ai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.xiangqi.core.GameState;
import com.xiangqi.core.Move;

/**
 * Smoke tests guarding that the AI package compiles and links against
 * {@code com.xiangqi.core} and behaves to contract on the opening position.
 */
public class XiangqiAITest {

    @Test
    public void minimaxReturnsLegalOpeningMove() {
        GameState state = GameState.initial();
        XiangqiAI ai = new MinimaxAI(3);
        Move move = ai.chooseMove(state);
        assertNotNull("AI must return a move from the opening", move);
        assertTrue("AI move must be legal", state.isLegal(move));
    }

    @Test
    public void coachReturnsRankedAdvice() {
        GameState state = GameState.initial();
        Coach coach = new XiangqiCoach(3);
        List<MoveAdvice> top = coach.topMoves(state, 3);
        assertNotNull("topMoves must never return null", top);
        assertFalse("opening has candidate moves", top.isEmpty());
        assertTrue("must respect the requested cap", top.size() <= 3);
        for (int i = 0; i < top.size(); i++) {
            MoveAdvice a = top.get(i);
            assertTrue("advised move must be legal", state.isLegal(a.move()));
            assertNotNull("explanation must be present", a.explanation());
            assertFalse("explanation must be non-empty", a.explanation().isEmpty());
            if (i > 0) {
                assertTrue("advice must be sorted best-first",
                        top.get(i - 1).score() >= a.score());
            }
        }
    }

    @Test
    public void evaluatorIsSymmetricAtStart() {
        GameState state = GameState.initial();
        Evaluator eval = new XiangqiEvaluator();
        int red = eval.evaluate(state, com.xiangqi.core.Side.RED);
        int black = eval.evaluate(state, com.xiangqi.core.Side.BLACK);
        // From a symmetric opening the only asymmetry is the side-to-move
        // mobility bonus, so the scores should be exact negatives plus that.
        assertTrue("evaluations should be near-opposite at the symmetric start",
                Math.abs(red + black) <= 4 * state.legalMoves().size());
    }
}
