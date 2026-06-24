package com.dglz.engine;

import com.dglz.domain.Card;
import com.dglz.domain.Combination;
import com.dglz.domain.Deck;
import com.dglz.domain.Play;
import com.dglz.domain.Player;
import com.dglz.domain.PlayerStrategy;
import com.dglz.domain.Rank;
import com.dglz.domain.Road;
import com.dglz.domain.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Drives the game: dealing, turn order, trick resolution, human/AI play. */
public final class GameEngine {
    private static final int PLAYERS = 6;
    private static final int PER_PLAYER = 27;
    private static final int HUMAN_SEAT = 0;

    private long seed;
    private final String[] playerNames;
    private final ComboValidator validator = new ComboValidator();

    private GameState state;
    private PlayerStrategy strategy;
    /** Seats that have passed during the current trick (a pass is permanent until the trick resolves). */
    private final Set<Integer> passedThisTrick = new HashSet<Integer>();

    public GameEngine(long seed, String[] playerNames) {
        if (playerNames == null || playerNames.length != PLAYERS) {
            throw new IllegalArgumentException("playerNames must have size " + PLAYERS);
        }
        this.seed = seed;
        this.playerNames = playerNames.clone();
    }

    /** Re-seed and deal a fresh game (used by the UI's "New Game" so each deal differs). */
    public void start(long newSeed) {
        this.seed = newSeed;
        start();
    }

    /** Build deck, shuffle with seed, deal 27 each, set first leader (seat 0). */
    public void start() {
        Deck deck = new Deck();
        deck.shuffle(new Random(seed));
        List<List<Card>> hands = deck.deal(PLAYERS, PER_PLAYER);

        List<Player> players = new ArrayList<Player>(PLAYERS);
        for (int seat = 0; seat < PLAYERS; seat++) {
            List<Card> hand = hands.get(seat);
            sortHand(hand);
            boolean human = (seat == HUMAN_SEAT);
            players.add(new Player(seat, playerNames[seat], human, Team.forSeat(seat), hand));
        }
        state = new GameState(players);
        passedThisTrick.clear();
        state.setLeaderSeat(HUMAN_SEAT);
        state.setCurrentSeat(HUMAN_SEAT);
        state.setCurrentRoad(null);
        state.setCurrentBest(null);
        state.setCurrentBestSeat(-1);
        state.addLog("Game started. " + players.get(HUMAN_SEAT).name() + " leads the first trick.");
    }

    public GameState state() {
        return state;
    }

    public List<Card> humanHand() {
        return state.playerAt(HUMAN_SEAT).hand();
    }

    public void setStrategy(PlayerStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Identify the human's selection and check legality vs the current trick.
     * Returns the Combination if it is a legal play right now, else null.
     */
    public Combination validateSelection(List<Card> selected) {
        return validateForSeat(HUMAN_SEAT, selected);
    }

    /** If legal, apply the human's play and advance; else return false. */
    public boolean playHuman(List<Card> selected) {
        if (state.gameOver() || state.currentSeat() != HUMAN_SEAT) {
            return false;
        }
        Combination combo = validateForSeat(HUMAN_SEAT, selected);
        if (combo == null) {
            return false;
        }
        applyPlay(HUMAN_SEAT, combo);
        return true;
    }

    /** Pass if legal (cannot pass when leading). */
    public boolean passHuman() {
        if (state.gameOver() || state.currentSeat() != HUMAN_SEAT) {
            return false;
        }
        if (state.currentBest() == null) {
            return false; // cannot pass when leading
        }
        applyPass(HUMAN_SEAT);
        return true;
    }

    /**
     * If currentSeat is an AI seat, query the strategy, apply its move (validating;
     * PASS if it returns null/illegal), advance, and return true. False if it's the
     * human's turn or the game is over.
     */
    public boolean stepAI() {
        if (state.gameOver() || state.currentSeat() == HUMAN_SEAT) {
            return false;
        }
        int seat = state.currentSeat();
        Combination chosen = null;
        if (strategy != null) {
            Combination proposed = strategy.decideMove(state, seat);
            if (proposed != null) {
                chosen = validateForSeat(seat, proposed.cards());
            }
        }
        if (chosen == null) {
            if (state.currentBest() == null) {
                // Leading: a pass is not allowed, fall back to the lowest single.
                chosen = fallbackLead(seat);
                applyPlay(seat, chosen);
            } else {
                applyPass(seat);
            }
        } else {
            applyPlay(seat, chosen);
        }
        return true;
    }

    // ---- internal flow ----

    private Combination validateForSeat(int seat, List<Card> selected) {
        if (state == null || state.gameOver() || selected == null || selected.isEmpty()) {
            return null;
        }
        if (!handContainsAll(seat, selected)) {
            return null;
        }
        Combination combo = validator.identify(selected);
        if (combo == null) {
            return null;
        }
        if (state.currentBest() == null) {
            return combo; // leading: any valid combo is legal
        }
        if (combo.road() != state.currentRoad()) {
            return null;
        }
        if (!combo.beats(state.currentBest())) {
            return null;
        }
        return combo;
    }

    private void applyPlay(int seat, Combination combo) {
        Player player = state.playerAt(seat);
        if (state.currentBest() == null) {
            // Leading a new trick.
            state.setLeaderSeat(seat);
            state.setCurrentRoad(combo.road());
        }
        removeFromHand(seat, combo.cards());
        state.setCurrentBest(combo);
        state.setCurrentBestSeat(seat);
        state.addTrickPlay(new Play(seat, combo));
        state.addLog(player.name() + " plays " + describe(combo) + ".");
        if (player.isOut()) {
            state.addLog(player.name() + " is out!");
        }
        if (checkTeamWin()) {
            return;
        }
        advanceAfter(seat);
    }

    private void applyPass(int seat) {
        Player player = state.playerAt(seat);
        passedThisTrick.add(seat);
        state.addTrickPlay(new Play(seat, null));
        state.addLog(player.name() + " passes.");
        advanceAfter(seat);
    }

    /**
     * Find the next seat that must act this trick: active (has cards), not the current
     * best holder, and has not already passed this trick. If none remains, the trick is
     * over and the best holder wins.
     */
    private void advanceAfter(int seat) {
        for (int step = 1; step <= PLAYERS; step++) {
            int s = (seat + step) % PLAYERS;
            if (state.playerAt(s).isOut()) {
                continue;
            }
            if (s == state.currentBestSeat()) {
                continue; // best holder need not act against itself
            }
            if (passedThisTrick.contains(s)) {
                continue; // already passed this trick
            }
            state.setCurrentSeat(s);
            return;
        }
        // No eligible actor remains: every other active player has passed.
        resolveTrick();
    }

    private void resolveTrick() {
        int winner = state.currentBestSeat();
        state.addLog(state.playerAt(winner).name() + " wins the trick and leads.");
        state.setCurrentBest(null);
        state.setCurrentRoad(null);
        state.clearTrickPlays();
        passedThisTrick.clear();
        int leader = state.playerAt(winner).isOut() ? nextActiveSeat(winner) : winner;
        state.setCurrentBestSeat(-1);
        if (leader == -1) {
            return; // game over
        }
        state.setLeaderSeat(leader);
        state.setCurrentSeat(leader);
    }

    /** Next seat after `from` (cyclically) whose player still has cards, or -1 if none. */
    private int nextActiveSeat(int from) {
        for (int step = 1; step <= PLAYERS; step++) {
            int seat = (from + step) % PLAYERS;
            if (seat == from) {
                break;
            }
            if (!state.playerAt(seat).isOut()) {
                return seat;
            }
        }
        return -1;
    }

    private boolean checkTeamWin() {
        for (Team team : Team.values()) {
            boolean allOut = true;
            for (int seat = 0; seat < PLAYERS; seat++) {
                if (Team.forSeat(seat) == team && !state.playerAt(seat).isOut()) {
                    allOut = false;
                    break;
                }
            }
            if (allOut) {
                state.setGameOver(true);
                state.setWinningTeam(team);
                state.addLog(team.label() + " wins the game!");
                return true;
            }
        }
        return false;
    }

    private Combination fallbackLead(int seat) {
        List<Card> hand = state.playerAt(seat).hand();
        Card lowest = hand.get(0);
        for (int i = 1; i < hand.size(); i++) {
            if (hand.get(i).rank().order() < lowest.rank().order()) {
                lowest = hand.get(i);
            }
        }
        List<Card> single = new ArrayList<Card>(1);
        single.add(lowest);
        return validator.identify(single);
    }

    private boolean handContainsAll(int seat, List<Card> cards) {
        List<Card> work = new ArrayList<Card>(state.playerAt(seat).hand());
        for (Card c : cards) {
            if (!work.remove(c)) {
                return false;
            }
        }
        return true;
    }

    private void removeFromHand(int seat, List<Card> cards) {
        List<Card> hand = state.playerAt(seat).hand();
        for (Card c : cards) {
            hand.remove(c);
        }
    }

    private String describe(Combination combo) {
        StringBuilder sb = new StringBuilder();
        sb.append(combo.type());
        sb.append(" [");
        List<Card> cards = combo.cards();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(cards.get(i).shortName());
        }
        sb.append(']');
        return sb.toString();
    }

    private static void sortHand(List<Card> hand) {
        Collections.sort(hand, new Comparator<Card>() {
            @Override
            public int compare(Card a, Card b) {
                int byRank = Integer.compare(a.rank().order(), b.rank().order());
                if (byRank != 0) {
                    return byRank;
                }
                int bySuit = Integer.compare(a.suit().ordinal(), b.suit().ordinal());
                if (bySuit != 0) {
                    return bySuit;
                }
                return Integer.compare(a.deckId(), b.deckId());
            }
        });
    }
}
