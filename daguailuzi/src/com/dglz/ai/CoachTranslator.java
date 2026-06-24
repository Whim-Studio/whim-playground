package com.dglz.ai;

import com.dglz.domain.Card;
import com.dglz.domain.Combination;
import com.dglz.domain.ComboType;
import com.dglz.domain.MoveAdvisor;
import com.dglz.domain.MoveSuggestion;
import com.dglz.domain.Player;
import com.dglz.domain.Rank;
import com.dglz.domain.Road;
import com.dglz.domain.Team;
import com.dglz.engine.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Coach Mode advisor. Computes the objectively best legal move for the human (reusing
 * {@link AiStrategy}'s reasoning), then translates it into a plain-English suggestion
 * covering WHAT to play, WHY now, and the TEAM/board effect.
 *
 * <p>No-arg constructible so {@code app.Main} can {@code new} it directly.</p>
 */
public class CoachTranslator implements MoveAdvisor {

    private final AiStrategy strategy;

    public CoachTranslator() {
        this.strategy = new AiStrategy();
    }

    @Override
    public MoveSuggestion advise(GameState state, int humanSeat) {
        Combination play = strategy.chooseBest(state, humanSeat);
        List<Card> highlight;
        if (play == null) {
            highlight = Collections.emptyList();
        } else {
            highlight = new ArrayList<Card>(play.cards());
        }
        String explanation = buildExplanation(state, humanSeat, play);
        return new MoveSuggestion(play, highlight, explanation);
    }

    // ------------------------------------------------------------------
    // Explanation building.
    // ------------------------------------------------------------------

    private String buildExplanation(GameState state, int seat, Combination play) {
        Combination toBeat = state.currentBest();
        boolean leading = (toBeat == null);

        if (play == null) {
            return passExplanation(state, seat, leading);
        }
        if (leading) {
            return leadExplanation(state, seat, play);
        }
        return followExplanation(state, seat, play, toBeat);
    }

    private String passExplanation(GameState state, int seat, boolean leading) {
        if (leading) {
            // A leader cannot legally pass; this branch is defensive only.
            return "There is no clearly good lead here, but you must lead. Play your "
                    + "weakest, most awkward cards and keep your bombs and jokers for later.";
        }
        if (HandEvaluator.teammateWinning(state, seat)) {
            int winner = state.currentBestSeat();
            return "Pass. Your teammate at seat " + winner + " is already winning this trick, "
                    + "so playing now would only overtake your own partner and waste cards. "
                    + "Hold everything and let your team keep control — you stay strong for "
                    + "the tricks where the opponents are ahead.";
        }
        return "Pass. The only way to win this trick would be to break up a pair or stronger "
                + "combination, or to spend a bomb/joker on a minor card — that is a bad trade. "
                + "Let this small trick go and keep your strong cards intact for a trick that "
                + "actually matters to your team.";
    }

    private String leadExplanation(GameState state, int seat, Combination play) {
        String what = describe(play);
        StringBuilder sb = new StringBuilder();
        sb.append("Lead ").append(what).append(". ");
        if (AiStrategy.isStrongFive(play.type())) {
            sb.append("This is a powerful hand, but you hold the lead, so it sets the pace of the trick. ");
        } else {
            sb.append("You are leading, so shed your weakest, most awkward cards now while keeping your "
                    + "bombs and jokers in reserve. ");
        }
        sb.append("Playing your lowest cards first clears the dead weight from your hand and forces the "
                + "opponents to spend stronger cards to answer. ");
        sb.append(teamEffect(seat));
        return sb.toString();
    }

    private String followExplanation(GameState state, int seat, Combination play, Combination toBeat) {
        int loser = state.currentBestSeat();
        String what = describe(play);
        String beaten = describe(toBeat);
        StringBuilder sb = new StringBuilder();
        sb.append("Play ").append(what).append(" to take this trick as cheaply as possible, beating ")
                .append("seat ").append(loser).append("'s ").append(beaten).append(". ");
        if (AiStrategy.isStrongFive(play.type())) {
            sb.append("This trick is worth committing a strong combination to — winning it now denies the "
                    + "opponents control. ");
        } else {
            sb.append("It wins with the smallest combination that does the job, so your bombs and jokers "
                    + "stay untouched for a bigger fight later. ");
        }
        sb.append("Taking the trick means you lead next, ");
        sb.append(teamEffectAsLeader(seat));
        return sb.toString();
    }

    private String teamEffect(int seat) {
        List<Integer> mates = teammates(seat);
        return "Winning or forcing out high cards here helps your partners (seats " + join(mates)
                + ") take control of the following tricks.";
    }

    private String teamEffectAsLeader(int seat) {
        List<Integer> mates = teammates(seat);
        return "so you can steer the next trick toward a road your partners (seats " + join(mates)
                + ") are strong in and drive your team toward going out first.";
    }

    private List<Integer> teammates(int seat) {
        Team myTeam = Team.forSeat(seat);
        List<Integer> mates = new ArrayList<Integer>();
        for (int s = 0; s < 6; s++) {
            if (s != seat && Team.forSeat(s) == myTeam) {
                mates.add(s);
            }
        }
        return mates;
    }

    private String join(List<Integer> seats) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seats.size(); i++) {
            if (i > 0) {
                sb.append(i == seats.size() - 1 ? " and " : ", ");
            }
            sb.append(seats.get(i));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Plain-English rendering of a combination.
    // ------------------------------------------------------------------

    private String describe(Combination c) {
        ComboType t = c.type();
        String r = rankWord(c.primaryRank());
        if (t == ComboType.SINGLE) {
            return "your " + r;
        }
        if (t == ComboType.PAIR) {
            return "your pair of " + r + "s";
        }
        if (t == ComboType.TRIPLE) {
            return "your three " + r + "s";
        }
        if (t == ComboType.STRAIGHT) {
            return "a straight up to the " + r;
        }
        if (t == ComboType.FLUSH) {
            return "a flush topped by the " + r;
        }
        if (t == ComboType.FULL_HOUSE) {
            return "a full house (three " + r + "s)";
        }
        if (t == ComboType.FOUR_PLUS_ONE) {
            return "a bomb — four " + r + "s";
        }
        if (t == ComboType.STRAIGHT_FLUSH) {
            return "a straight flush up to the " + r;
        }
        if (t == ComboType.FIVE_OF_A_KIND) {
            return "five " + r + "s";
        }
        return "your " + r;
    }

    private String rankWord(Rank rank) {
        if (rank == Rank.JACK) return "Jack";
        if (rank == Rank.QUEEN) return "Queen";
        if (rank == Rank.KING) return "King";
        if (rank == Rank.ACE) return "Ace";
        if (rank == Rank.SMALL_JOKER) return "Small Joker";
        if (rank == Rank.BIG_JOKER) return "Big Joker";
        return rank.label();
    }
}
