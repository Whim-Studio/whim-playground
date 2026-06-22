package com.whim.babylon5.engine;

import java.util.ArrayList;
import java.util.List;

import com.whim.babylon5.domain.Card;
import com.whim.babylon5.domain.CardType;
import com.whim.babylon5.domain.ConflictType;
import com.whim.babylon5.domain.GameListener;
import com.whim.babylon5.domain.GameState;
import com.whim.babylon5.domain.Phase;
import com.whim.babylon5.domain.PlayerState;
import com.whim.babylon5.domain.Zone;
import com.whim.babylon5.domain.ZoneType;

/**
 * The Babylon 5 CCG turn loop and rule validator.
 *
 * <p>Drives the strict round sequence from the rulebook ("Playing the Game"):
 * READY &rarr; CONFLICT &rarr; ACTION &rarr; RESOLUTION (Aftermath play happens here) &rarr; DRAW,
 * after which play passes to the next player and a fresh READY begins. All mutation funnels
 * through here so the UI (Task 3) and the AI ({@link AIPlayer}) share one rule authority.
 *
 * <p><b>Threading:</b> {@link #runAiTurn(int)} is pure logic with no Swing dependency and is
 * safe to call off the EDT, as required by the contract. Listener callbacks are invoked
 * synchronously on whatever thread drives the engine; the UI is responsible for marshalling
 * them onto the EDT.
 *
 * <h3>Deterministic prototype rulings (rulebook silent / per-card in the paper game)</h3>
 * <ul>
 *   <li><b>Conflict fallout damage.</b> In the paper game, damage is dealt by explicit
 *       <i>Attack</i> actions and per-card text, not by the bare support-vs-opposition tally.
 *       For a self-contained single-player engine we model the strain of a decided conflict
 *       as: the winning margin is dealt as 1-point damage tokens spread round-robin across the
 *       losing side's committed cards. (A 0-margin tie deals no damage.) This keeps committed
 *       characters meaningfully at risk without needing the full Attack sub-system.</li>
 *   <li><b>Neutralization threshold.</b> Grounded in the rulebook ("The Effects of Damage"):
 *       a card is Neutralized when its total damage is &ge; its highest printed ability rating
 *       (and it has at least 1 damage). Damage also reduces the ability it applies point-for-point,
 *       never below zero.</li>
 *   <li><b>Loyalty cost.</b> Rulebook: a character "loyal to a different race" costs double to
 *       sponsor; same-race (and neutral) characters cost their printed value. With the domain
 *       model exposing only a single {@code FactionId}, we treat a character as same-race iff its
 *       faction equals the sponsoring faction; otherwise the cost is doubled.</li>
 * </ul>
 */
public final class GameEngine {

    public static final int VICTORY_POWER = 20;

    /** Rulebook: "A faction's Influence Rating may never be reduced below three." */
    public static final int MIN_INFLUENCE_RATING = 3;

    private final GameState state;
    private final List<GameListener> listeners = new ArrayList<GameListener>();
    /** AI controllers indexed by player; index 0 (the human) is null. */
    private final AIPlayer[] ai;

    public GameEngine(GameState state) {
        this.state = state;
        int n = state.getPlayers().size();
        this.ai = new AIPlayer[n];
        // Default ladder: opponents escalate in skill. May be overridden via setAiDifficulty.
        AiDifficulty[] ladder = { AiDifficulty.EASY, AiDifficulty.MEDIUM, AiDifficulty.HARD };
        for (int i = 1; i < n; i++) {
            ai[i] = new AIPlayer(ladder[(i - 1) % ladder.length]);
        }
    }

    public GameState getState() {
        return state;
    }

    public void addListener(GameListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    /** Assign a difficulty to an AI seat (1..n-1). The human seat (0) is ignored. */
    public void setAiDifficulty(int playerIndex, AiDifficulty difficulty) {
        if (playerIndex > 0 && playerIndex < ai.length && difficulty != null) {
            ai[playerIndex] = new AIPlayer(difficulty);
        }
    }

    public AIPlayer aiFor(int playerIndex) {
        return (playerIndex >= 0 && playerIndex < ai.length) ? ai[playerIndex] : null;
    }

    // ------------------------------------------------------------------ phase loop

    /**
     * Advance exactly one phase along READY&rarr;CONFLICT&rarr;ACTION&rarr;RESOLUTION&rarr;DRAW; after
     * DRAW, pass play to the next player and begin their READY. Applies the mechanical effects
     * of each transition and fires {@link GameListener} events.
     */
    public void advancePhase() {
        Phase phase = state.getPhase();
        switch (phase) {
            case READY:
                // The Ready round just completed: ready all cards & restore applied influence.
                readyAndRestore(state.getActivePlayer());
                state.setPhase(Phase.CONFLICT);
                break;
            case CONFLICT:
                state.setPhase(Phase.ACTION);
                break;
            case ACTION:
                state.setPhase(Phase.RESOLUTION);
                break;
            case RESOLUTION:
                // Aftermath play is part of RESOLUTION and handled before we leave it.
                state.setPhase(Phase.DRAW);
                break;
            case DRAW:
            default:
                drawRound(state.getActivePlayer());
                passToNextPlayer();
                break;
        }
        log("Phase -> " + state.getPhase() + " (player " + state.getActiveIndex() + ")");
        firePhaseChanged(state.getPhase(), state.getActiveIndex());
        fireStateChanged();
    }

    /** Ready round, steps 1 & 2: un-rotate every card the active player controls and restore influence. */
    private void readyAndRestore(PlayerState p) {
        for (ZoneType zt : new ZoneType[] { ZoneType.INNER_CIRCLE, ZoneType.SUPPORTING }) {
            for (Card c : p.zone(zt).getCards()) {
                // A neutralized card (damage >= highest ability) stays rotated/face-down.
                if (!isNeutralized(c)) {
                    c.setReady(true);
                }
            }
        }
        // Applied influence is restored: the spendable pool returns to the full Influence Rating.
        p.setInfluencePool(p.getInfluenceRating());
    }

    /** Draw round: discard neutralized supporting cards, then draw one free card. */
    private void drawRound(PlayerState p) {
        // Step 1: discard neutralized supporting cards (Inner Circle neutralized cards survive).
        Zone supporting = p.zone(ZoneType.SUPPORTING);
        List<Card> doomed = new ArrayList<Card>();
        for (Card c : supporting.getCards()) {
            if (isNeutralized(c)) {
                doomed.add(c);
            }
        }
        for (Card c : doomed) {
            supporting.remove(c);
            p.zone(ZoneType.DISCARD).add(c);
        }
        // Step 3: draw one free card from the top of the draw deck (if any remain).
        Card drawn = p.zone(ZoneType.DRAW_DECK).draw();
        if (drawn != null) {
            p.zone(ZoneType.HAND).add(drawn);
        }
    }

    private void passToNextPlayer() {
        int n = state.getPlayers().size();
        int next = (state.getActiveIndex() + 1) % n;
        state.setActiveIndex(next);
        if (next == 0) {
            state.incrementTurn();
        }
        state.setPhase(Phase.READY);
    }

    // ------------------------------------------------------------------ sponsoring

    /**
     * Pay influence and bring a CHARACTER (into SUPPORTING) or AMBASSADOR (into INNER_CIRCLE) into
     * play from the player's hand. Validates the rulebook's sponsoring requirements:
     * <ul>
     *   <li>It is the Action round.</li>
     *   <li>The card is a CHARACTER or AMBASSADOR currently in the player's HAND.</li>
     *   <li>The faction has a ready, un-neutralized Inner Circle character to rotate.</li>
     *   <li>The faction can pay the influence cost (doubled for a different-race character).</li>
     * </ul>
     * @return false (and no mutation) if any precondition fails.
     */
    public boolean sponsorCharacter(int playerIndex, Card character) {
        if (playerIndex < 0 || playerIndex >= state.getPlayers().size() || character == null) {
            return false;
        }
        if (state.getPhase() != Phase.ACTION) {
            return false;
        }
        PlayerState p = state.getPlayers().get(playerIndex);
        if (character.getType() != CardType.CHARACTER && character.getType() != CardType.AMBASSADOR) {
            return false;
        }
        Zone hand = p.zone(ZoneType.HAND);
        if (!hand.getCards().contains(character)) {
            return false;
        }
        // Need a ready, un-neutralized Inner Circle character to rotate as the sponsor.
        Card sponsor = findReadyInnerCircle(p);
        if (sponsor == null) {
            return false;
        }
        int cost = sponsorCost(p, character);
        if (p.getInfluencePool() < cost) {
            return false;
        }

        // Commit: pay, rotate the sponsor, move the card into play ready to act.
        p.adjustInfluencePool(-cost);
        sponsor.setReady(false);
        hand.remove(character);
        ZoneType dest = character.getType() == CardType.AMBASSADOR
                ? ZoneType.INNER_CIRCLE : ZoneType.SUPPORTING;
        p.zone(dest).add(character);
        character.setReady(true);

        log(p.getName() + " sponsors " + character.getName() + " for " + cost + " influence");
        fireStateChanged();
        return true;
    }

    /** Rulebook: same-race/neutral cost = printed; different-race "loyal" cost = doubled. */
    public int sponsorCost(PlayerState p, Card character) {
        int cost = character.getCost();
        if (character.getFaction() != p.getFaction()) {
            cost *= 2;
        }
        return cost;
    }

    private Card findReadyInnerCircle(PlayerState p) {
        for (Card c : p.zone(ZoneType.INNER_CIRCLE).getCards()) {
            if (c.isReady() && !isNeutralized(c)) {
                return c;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ conflict resolution

    /**
     * Resolve a conflict's core math. Modified support is compared with modified opposition;
     * the initiator succeeds iff support STRICTLY exceeds opposition (rulebook, Resolution Step 1).
     * Applies fallout damage to the losing side and neutralizes any card pushed past its threshold.
     */
    public ConflictResult resolveConflict(Conflict c) {
        ConflictType t = c.getType();
        int support = modifiedTotal(c.getSupport(), t);
        int opposition = modifiedTotal(c.getOpposition(), t);
        boolean won = support > opposition; // STRICT exceed

        // Fallout: spread the winning margin as 1-point damage tokens across the losing side.
        List<Card> losers = won ? c.getOpposition() : c.getSupport();
        int margin = Math.abs(support - opposition);
        List<Card> neutralized = applyFallout(losers, margin, t);

        String summary = describe(t, support, opposition, won, neutralized.size());
        ConflictResult result = new ConflictResult(won, support, opposition, t, neutralized, summary);
        log(summary);
        fireConflictResolved(result);
        fireStateChanged();
        return result;
    }

    /** Sum of each committed card's modified ability for the conflict type (neutralized = 0). */
    private int modifiedTotal(List<Card> cards, ConflictType t) {
        int total = 0;
        for (Card c : cards) {
            total += effectiveAbility(c, t);
        }
        return total;
    }

    /** A card's ability for this conflict after damage; 0 if neutralized or reduced below zero. */
    public int effectiveAbility(Card c, ConflictType t) {
        if (isNeutralized(c)) {
            return 0;
        }
        return Math.max(0, c.support(t) - c.getDamage());
    }

    private List<Card> applyFallout(List<Card> losers, int margin, ConflictType t) {
        List<Card> neutralized = new ArrayList<Card>();
        if (losers.isEmpty() || margin <= 0) {
            return neutralized;
        }
        // Round-robin one point of damage at a time until the margin is spent.
        for (int i = 0; i < margin; i++) {
            Card c = losers.get(i % losers.size());
            c.addDamage(1);
        }
        for (Card c : losers) {
            if (isNeutralized(c) && !neutralized.contains(c)) {
                c.setReady(false); // flipped face-down / rotated out
                neutralized.add(c);
            }
        }
        return neutralized;
    }

    /** Rulebook: neutralized when total damage &ge; highest printed ability (and &ge; 1 damage). */
    public boolean isNeutralized(Card c) {
        int dmg = c.getDamage();
        return dmg >= 1 && dmg >= highestAbility(c);
    }

    private int highestAbility(Card c) {
        int h = c.getDiplomacy();
        h = Math.max(h, c.getIntrigue());
        h = Math.max(h, c.getPsi());
        h = Math.max(h, c.getMilitary());
        return h;
    }

    private String describe(ConflictType t, int support, int opposition, boolean won, int neut) {
        StringBuilder sb = new StringBuilder();
        sb.append(t).append(" conflict: support ").append(support)
          .append(" vs opposition ").append(opposition)
          .append(" — initiator ").append(won ? "WON" : "LOST");
        if (neut > 0) {
            sb.append("; neutralized ").append(neut).append(" card(s)");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ power & victory

    /**
     * Total Power for a player: base Power equals the current Influence Rating
     * (rulebook, "Victory"), plus any card-specified Power bonuses. The computed value is
     * cached on the {@link PlayerState}.
     */
    public int computePower(PlayerState p) {
        int power = p.getInfluenceRating() + powerBonus(p);
        p.setPower(power);
        return power;
    }

    /**
     * Sum of card-text Power bonuses for cards the player has in play. The base/Premiere
     * prototype card set carries no static Power riders, so this is 0 today; the hook keeps
     * {@link #computePower} honest to the rulebook ("Other cards in play may add additional
     * points to a player's Power total") and gives Task 1 a place to wire bonuses in.
     */
    private int powerBonus(PlayerState p) {
        return 0;
    }

    /**
     * Standard Victory: the player with &ge; {@link #VICTORY_POWER} Power AND strictly more Power
     * than every other player. Returns null if no one qualifies or the lead is tied.
     */
    public PlayerState checkVictory() {
        PlayerState leader = null;
        int best = Integer.MIN_VALUE;
        boolean tiedAtTop = false;
        for (PlayerState p : state.getPlayers()) {
            int power = computePower(p);
            if (power > best) {
                best = power;
                leader = p;
                tiedAtTop = false;
            } else if (power == best) {
                tiedAtTop = true;
            }
        }
        if (leader != null && !tiedAtTop && best >= VICTORY_POWER) {
            return leader;
        }
        return null;
    }

    // ------------------------------------------------------------------ AI driver

    /**
     * Run an AI player's full turn using pure logic only (no Swing) &mdash; safe off the EDT.
     * Expects to be called when it is {@code playerIndex}'s READY phase; drives the player
     * through every round and passes play to the next player at the end.
     */
    public void runAiTurn(int playerIndex) {
        if (state.getActiveIndex() != playerIndex) {
            return;
        }
        AIPlayer brain = aiFor(playerIndex);
        if (brain == null) {
            // Not an AI seat: just walk the phases so the loop never stalls.
            for (int i = 0; i < 5; i++) {
                advancePhase();
            }
            return;
        }
        if (state.getPhase() != Phase.READY) {
            // Defensive: only orchestrate a clean turn from the top of the round.
            state.setPhase(Phase.READY);
        }

        // READY -> CONFLICT (readies cards, restores influence).
        advancePhase();

        // CONFLICT round: decide whether (and what) to initiate.
        Conflict pending = safeChooseConflict(brain, playerIndex);
        advancePhase(); // CONFLICT -> ACTION

        // ACTION round: sponsor a character, then commit supporters to any pending conflict.
        try {
            Card toSponsor = brain.chooseCharacterToSponsor(state, playerIndex);
            if (toSponsor != null) {
                sponsorCharacter(playerIndex, toSponsor);
            }
        } catch (RuntimeException ex) {
            log("AI sponsor decision skipped: " + ex.getMessage());
        }
        if (pending != null) {
            commitSupport(brain, playerIndex, pending);
            commitOpposition(playerIndex, pending);
        }
        advancePhase(); // ACTION -> RESOLUTION

        // RESOLUTION round (includes aftermath): resolve the conflict if one was initiated.
        if (pending != null) {
            resolveConflict(pending);
        }
        advancePhase(); // RESOLUTION -> DRAW

        // DRAW round, then pass play onward.
        advancePhase(); // DRAW -> next player's READY
    }

    private Conflict safeChooseConflict(AIPlayer brain, int playerIndex) {
        try {
            return brain.chooseConflict(state, playerIndex);
        } catch (RuntimeException ex) {
            log("AI conflict decision skipped: " + ex.getMessage());
            return null;
        }
    }

    /** Rotate the initiator's ready, type-capable characters that the AI elects to commit. */
    private void commitSupport(AIPlayer brain, int playerIndex, Conflict pending) {
        PlayerState p = state.getPlayers().get(playerIndex);
        for (ZoneType zt : new ZoneType[] { ZoneType.INNER_CIRCLE, ZoneType.SUPPORTING }) {
            for (Card c : p.zone(zt).getCards()) {
                if (!c.isReady() || isNeutralized(c)) {
                    continue;
                }
                if (effectiveAbility(c, pending.getType()) <= 0) {
                    continue; // cannot contribute to this conflict type
                }
                if (brain.willCommit(c, pending, true)) {
                    pending.getSupport().add(c);
                    c.setReady(false); // rotated to support
                }
            }
        }
    }

    /**
     * The defending faction reflexively opposes with its strongest ready, type-capable cards.
     * (The human/UI does this interactively; for an all-AI driver we give the target a sensible
     * automatic defense so conflicts are not free.)
     */
    private void commitOpposition(int initiatorIndex, Conflict pending) {
        int target = pending.getTarget();
        if (target < 0 || target >= state.getPlayers().size() || target == initiatorIndex) {
            return;
        }
        PlayerState d = state.getPlayers().get(target);
        AIPlayer defBrain = aiFor(target);
        for (ZoneType zt : new ZoneType[] { ZoneType.INNER_CIRCLE, ZoneType.SUPPORTING }) {
            for (Card c : d.zone(zt).getCards()) {
                if (!c.isReady() || isNeutralized(c)) {
                    continue;
                }
                if (effectiveAbility(c, pending.getType()) <= 0) {
                    continue;
                }
                boolean commit = (defBrain == null) || defBrain.willCommit(c, pending, false);
                if (commit) {
                    pending.getOpposition().add(c);
                    c.setReady(false);
                }
            }
        }
    }

    // ------------------------------------------------------------------ listeners

    private void firePhaseChanged(Phase phase, int activeIndex) {
        for (GameListener l : listeners) {
            l.onPhaseChanged(phase, activeIndex);
        }
    }

    private void fireConflictResolved(ConflictResult r) {
        for (GameListener l : listeners) {
            l.onConflictResolved(r);
        }
    }

    private void fireStateChanged() {
        for (GameListener l : listeners) {
            l.onStateChanged();
        }
    }

    private void log(String message) {
        for (GameListener l : listeners) {
            l.onLog(message);
        }
    }
}
