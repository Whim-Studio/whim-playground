package com.tiwas.mahjong.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.tiwas.mahjong.model.Constants;
import com.tiwas.mahjong.model.Dragon;
import com.tiwas.mahjong.model.GameState;
import com.tiwas.mahjong.model.Hand;
import com.tiwas.mahjong.model.Meld;
import com.tiwas.mahjong.model.MeldType;
import com.tiwas.mahjong.model.Playable;
import com.tiwas.mahjong.model.Player;
import com.tiwas.mahjong.model.ScoreSheet;
import com.tiwas.mahjong.model.Tile;
import com.tiwas.mahjong.model.TileSuit;
import com.tiwas.mahjong.model.Wall;
import com.tiwas.mahjong.model.Wind;

/**
 * Drives a full single-player game: builds the wall, seats players, deals,
 * runs the draw/claim/discard loop against three AI opponents, detects wins and
 * drawn games, scores hands, and advances dealer/round between hands.
 *
 * The UI talks to the engine only through {@link Playable}, {@link #advance()},
 * and the human-input methods; it never reaches into engine internals.
 */
public final class GameEngine implements Playable {

    private enum Phase { DRAW, DISCARD, CLAIM, OVER }

    private final GameState state = new GameState();
    private final Random rng;
    private final AIPlayerLogic ai;
    private final ScoringEngine scoring = new ScoringEngine();
    private final List<String> log = new ArrayList<String>();

    private Phase phase = Phase.OVER;
    private Tile lastDrawnTile;

    // Claim-window bookkeeping.
    private boolean claimWindowAsked;
    private AIPlayerLogic.ClaimDecision humanClaimChoice;

    // Per-hand rotation bookkeeping.
    private int dealerPasses;
    private HandResult lastResult;
    private boolean gameOver;

    public GameEngine(Random rng) {
        this.rng = rng;
        this.ai = new AIPlayerLogic(rng);
    }

    public GameState getState() {
        return state;
    }

    public HandResult getLastResult() {
        return lastResult;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Tile getLastDrawnTile() {
        return lastDrawnTile;
    }

    /** Drain and return the AI/event log accumulated since the last call. */
    public List<String> drainLog() {
        List<String> copy = new ArrayList<String>(log);
        log.clear();
        return copy;
    }

    // ============================ game / hand setup ============================

    /** Begin a brand-new game: seat players (dice), reset scores, deal hand 1. */
    public void startGame() {
        state.getPlayers().clear();
        state.getPlayers().add(new Player("You", false, Wind.EAST));
        state.getPlayers().add(new Player("Rina", true, Wind.SOUTH));
        state.getPlayers().add(new Player("Ken", true, Wind.WEST));
        state.getPlayers().add(new Player("Mei", true, Wind.NORTH));

        // Roll three dice to choose the East seat (the dealer).
        int dealer = rollForEast();
        state.setDealer(dealer);
        state.setRoundWind(Wind.EAST);
        state.setHandNumber(1);
        state.setLimit(Constants.DEFAULT_LIMIT);
        dealerPasses = 0;
        gameOver = false;
        assignSeatWinds(dealer);
        log.add("Dice rolled: " + state.getPlayer(dealer).getName() + " takes the East seat (dealer).");
        dealHand();
    }

    private int rollForEast() {
        int total = roll() + roll() + roll();
        // Count around the table from the human seat (0), as is traditional.
        return (total - 1) % Constants.NUM_PLAYERS;
    }

    private int roll() {
        return rng.nextInt(6) + 1;
    }

    private void assignSeatWinds(int dealer) {
        int seat = dealer;
        Wind[] winds = Wind.values();
        for (int k = 0; k < Constants.NUM_PLAYERS; k++) {
            state.getPlayer(seat).setSeatWind(winds[k]);
            seat = state.nextSeat(seat);
        }
    }

    /** Build, shuffle and deal a fresh hand; dealer ends on 14 tiles. */
    private void dealHand() {
        for (int i = 0; i < state.getPlayers().size(); i++) {
            state.getPlayer(i).getHand().clear();
        }
        state.getDiscards().clear();
        state.clearLastDiscard();
        state.setFirstGoAround(true);
        state.resetTilesPlayed();
        lastDrawnTile = null;
        claimWindowAsked = false;
        humanClaimChoice = null;

        state.setWall(buildWall());

        // 13 tiles each.
        for (int n = 0; n < Constants.STARTING_HAND_SIZE; n++) {
            for (int i = 0; i < state.getPlayers().size(); i++) {
                state.getPlayer(i).getHand().addTile(state.getWall().draw());
            }
        }
        // Dealer draws to 14.
        state.getPlayer(state.getDealer()).getHand().addTile(state.getWall().draw());

        // Replace any bonus tiles dealt into hands.
        for (int i = 0; i < state.getPlayers().size(); i++) {
            replaceInitialBonus(i);
        }

        state.setCurrentTurn(state.getDealer());
        phase = Phase.DISCARD; // dealer already holds 14, so begins by discarding
        log.add("--- Hand " + state.getHandNumber() + " of " + Constants.TOTAL_HANDS
                + " | Round wind: " + state.getRoundWind().label()
                + " | Dealer: " + state.getPlayer(state.getDealer()).getName() + " ---");
    }

    private Wall buildWall() {
        List<Tile> tiles = new ArrayList<Tile>();
        TileSuit[] suits = { TileSuit.DOTS, TileSuit.BAMBOO, TileSuit.CHARACTERS };
        for (int s = 0; s < suits.length; s++) {
            for (int r = 1; r <= 9; r++) {
                for (int c = 0; c < 4; c++) {
                    tiles.add(Tile.suited(suits[s], r));
                }
            }
        }
        Wind[] winds = Wind.values();
        for (int w = 0; w < winds.length; w++) {
            for (int c = 0; c < 4; c++) {
                tiles.add(Tile.wind(winds[w]));
            }
        }
        Dragon[] dragons = Dragon.values();
        for (int d = 0; d < dragons.length; d++) {
            for (int c = 0; c < 4; c++) {
                tiles.add(Tile.dragon(dragons[d]));
            }
        }
        for (int f = 1; f <= 4; f++) {
            tiles.add(Tile.flower(f));
        }
        for (int s = 1; s <= 4; s++) {
            tiles.add(Tile.season(s));
        }
        Collections.shuffle(tiles, rng);
        return new Wall(tiles);
    }

    private void replaceInitialBonus(int playerIndex) {
        Hand hand = state.getPlayer(playerIndex).getHand();
        boolean again = true;
        while (again) {
            again = false;
            for (int i = 0; i < hand.getTiles().size(); i++) {
                Tile t = hand.getTiles().get(i);
                if (t.isBonus()) {
                    hand.getTiles().remove(i);
                    hand.addBonus(t);
                    Tile rep = state.getWall().drawReplacement();
                    if (rep != null) {
                        hand.addTile(rep);
                    }
                    again = true;
                    break;
                }
            }
        }
    }

    // ============================ main loop ============================

    /**
     * Advance the game until the next point that needs human input, or until the
     * hand finishes. AI turns are played out internally.
     */
    public TurnStatus advance() {
        while (true) {
            if (phase == Phase.OVER) {
                return new TurnStatus(TurnStatus.Kind.HAND_OVER);
            }
            Player current = state.getCurrentPlayer();

            if (phase == Phase.DRAW) {
                if (current.isHuman()) {
                    return new TurnStatus(TurnStatus.Kind.AWAIT_HUMAN_DRAW);
                }
                // AI draws (with bonus replacement and concealed kongs).
                Tile drawn = drawWithBonus(state.getCurrentTurn());
                if (drawn == null) {
                    resolveDrawnGame();
                    return new TurnStatus(TurnStatus.Kind.HAND_OVER);
                }
                aiHandleKongs(state.getCurrentTurn());
                if (isSelfWin(current.getHand())) {
                    resolveWin(state.getCurrentTurn(), true, lastDrawnTile);
                    return new TurnStatus(TurnStatus.Kind.HAND_OVER);
                }
                if (phase == Phase.OVER) {
                    return new TurnStatus(TurnStatus.Kind.HAND_OVER);
                }
                phase = Phase.DISCARD;
                continue;
            }

            if (phase == Phase.DISCARD) {
                if (current.isHuman()) {
                    return new TurnStatus(TurnStatus.Kind.AWAIT_HUMAN_DISCARD);
                }
                Tile d = ai.chooseDiscard(current.getHand(), current.getSeatWind(), state.getRoundWind());
                performDiscard(state.getCurrentTurn(), d);
                log.add(current.getName() + " discards " + d.displayName() + ".");
                phase = Phase.CLAIM;
                continue;
            }

            if (phase == Phase.CLAIM) {
                TurnStatus s = resolveClaimWindow();
                if (s != null) {
                    return s;
                }
                continue;
            }
        }
    }

    private TurnStatus resolveClaimWindow() {
        int discarder = state.getLastDiscardBy();
        Tile discard = state.getLastDiscard();
        if (discard == null) {
            phase = Phase.DRAW;
            state.setCurrentTurn(state.nextSeat(discarder < 0 ? state.getCurrentTurn() : discarder));
            return null;
        }
        int human = state.humanIndex();
        boolean humanCanClaim = human != -1 && human != discarder
                && (ClaimResolver.canPung(state.getPlayer(human).getHand(), discard)
                    || ClaimResolver.canMahjong(state.getPlayer(human).getHand(), discard));

        if (humanCanClaim && !claimWindowAsked) {
            claimWindowAsked = true;
            TurnStatus s = new TurnStatus(TurnStatus.Kind.AWAIT_HUMAN_CLAIM);
            s.claimableDiscard = discard;
            s.discardBy = discarder;
            s.canPung = ClaimResolver.canPung(state.getPlayer(human).getHand(), discard);
            s.canMahjong = ClaimResolver.canMahjong(state.getPlayer(human).getHand(), discard);
            return s;
        }

        List<ClaimResolver.Claim> claims = new ArrayList<ClaimResolver.Claim>();
        if (humanCanClaim && humanClaimChoice != null) {
            if (humanClaimChoice == AIPlayerLogic.ClaimDecision.CLAIM_MAHJONG) {
                claims.add(new ClaimResolver.Claim(human, null, true));
            } else if (humanClaimChoice == AIPlayerLogic.ClaimDecision.CLAIM_PUNG) {
                claims.add(new ClaimResolver.Claim(human, MeldType.PUNG, false));
            }
        }
        for (int p = 0; p < state.getPlayers().size(); p++) {
            if (p == discarder || p == human) {
                continue;
            }
            Player ap = state.getPlayer(p);
            AIPlayerLogic.ClaimDecision d = ai.decideClaim(ap.getHand(), discard,
                    ap.getSeatWind(), state.getRoundWind());
            if (d == AIPlayerLogic.ClaimDecision.CLAIM_MAHJONG) {
                claims.add(new ClaimResolver.Claim(p, null, true));
            } else if (d == AIPlayerLogic.ClaimDecision.CLAIM_PUNG) {
                claims.add(new ClaimResolver.Claim(p, MeldType.PUNG, false));
            }
        }

        // reset for the next window
        claimWindowAsked = false;
        humanClaimChoice = null;

        ClaimResolver.Claim winner = ClaimResolver.resolve(state, discarder, claims);
        if (winner == null) {
            state.setCurrentTurn(state.nextSeat(discarder));
            phase = Phase.DRAW;
            return null;
        }
        if (winner.mahjong) {
            resolveWin(winner.playerIndex, false, discard);
            return new TurnStatus(TurnStatus.Kind.HAND_OVER);
        }
        // Pung.
        applyPung(winner.playerIndex, discard);
        Player claimant = state.getPlayer(winner.playerIndex);
        log.add(claimant.getName() + " pungs " + discard.displayName() + ".");
        state.setCurrentTurn(winner.playerIndex);
        state.setFirstGoAround(false);
        phase = Phase.DISCARD;
        return null;
    }

    private void applyPung(int playerIndex, Tile discard) {
        Hand hand = state.getPlayer(playerIndex).getHand();
        hand.removeTile(discard);
        hand.removeTile(discard);
        List<Tile> meldTiles = new ArrayList<Tile>();
        meldTiles.add(discard);
        meldTiles.add(discard);
        meldTiles.add(discard);
        hand.addMeld(new Meld(MeldType.PUNG, meldTiles, false));
        hand.setClaimedDiscard(true);
        // remove the tile from the discard pile (it is now melded)
        List<Tile> discards = state.getDiscards();
        for (int i = discards.size() - 1; i >= 0; i--) {
            if (discards.get(i).equals(discard)) {
                discards.remove(i);
                break;
            }
        }
        state.clearLastDiscard();
    }

    // ============================ draws / kongs ============================

    private Tile drawWithBonus(int playerIndex) {
        Hand hand = state.getPlayer(playerIndex).getHand();
        while (true) {
            Tile t = state.getWall().draw();
            if (t == null) {
                return null;
            }
            if (t.isBonus()) {
                hand.addBonus(t);
                log.add(state.getPlayer(playerIndex).getName() + " reveals "
                        + t.displayName() + " and draws a replacement.");
                continue;
            }
            hand.addTile(t);
            lastDrawnTile = t;
            return t;
        }
    }

    /** AI declares any concealed kong it can (free improvement) and redraws. */
    private void aiHandleKongs(int playerIndex) {
        Hand hand = state.getPlayer(playerIndex).getHand();
        boolean again = true;
        while (again) {
            again = false;
            Tile four = findConcealedKong(hand);
            if (four != null) {
                if (!declareConcealedKong(playerIndex, four)) {
                    return; // drawn game (no replacement)
                }
                log.add(state.getPlayer(playerIndex).getName() + " declares a concealed kong of "
                        + four.displayName() + ".");
                again = true;
            }
        }
    }

    private Tile findConcealedKong(Hand hand) {
        List<Tile> tiles = hand.getTiles();
        for (int i = 0; i < tiles.size(); i++) {
            if (hand.count(tiles.get(i)) >= 4) {
                return tiles.get(i);
            }
        }
        return null;
    }

    /**
     * Form a concealed kong from four held tiles and draw a replacement.
     * Returns false (and ends the hand as drawn) if no replacement exists.
     */
    private boolean declareConcealedKong(int playerIndex, Tile face) {
        Hand hand = state.getPlayer(playerIndex).getHand();
        List<Tile> meldTiles = new ArrayList<Tile>();
        for (int k = 0; k < 4; k++) {
            hand.removeTile(face);
            meldTiles.add(face);
        }
        hand.addMeld(new Meld(MeldType.KONG, meldTiles, true));
        Tile rep = drawWithBonus(playerIndex);
        if (rep == null) {
            resolveDrawnGame();
            return false;
        }
        return true;
    }

    /** Upgrade an exposed pung to a kong using the just-drawn fourth tile. */
    private boolean upgradeKong(int playerIndex, Tile face) {
        Hand hand = state.getPlayer(playerIndex).getHand();
        Meld target = null;
        for (int i = 0; i < hand.getMelds().size(); i++) {
            Meld m = hand.getMelds().get(i);
            if (m.isPung() && m.representative().equals(face)) {
                target = m;
                break;
            }
        }
        if (target == null || hand.count(face) < 1) {
            return false;
        }
        hand.removeTile(face);
        List<Tile> meldTiles = target.getTiles();
        meldTiles.add(face);
        hand.getMelds().remove(target);
        hand.addMeld(new Meld(MeldType.KONG, meldTiles, false));
        Tile rep = drawWithBonus(playerIndex);
        if (rep == null) {
            resolveDrawnGame();
            return false;
        }
        return true;
    }

    // ============================ discards ============================

    private void performDiscard(int playerIndex, Tile tile) {
        Hand hand = state.getPlayer(playerIndex).getHand();
        hand.removeTile(tile);
        state.getDiscards().add(tile);
        state.setLastDiscard(tile, playerIndex);
        state.incTilesPlayed();
        if (state.getTilesPlayed() >= Constants.NUM_PLAYERS) {
            state.setFirstGoAround(false);
        }
    }

    // ============================ win / score ============================

    private boolean isSelfWin(Hand hand) {
        List<Tile> concealed = hand.getTiles();
        if (HandAnalyzer.isThirteenOrphans(concealed, hand.getMelds())) {
            return true;
        }
        if (ScoringEngine.isAllFlowersAndSeasons(hand)) {
            return true;
        }
        return HandAnalyzer.isStandardWin(hand.getMelds(), concealed);
    }

    private void resolveWin(int winnerIndex, boolean selfDraw, Tile winningTile) {
        Player winner = state.getPlayer(winnerIndex);
        Hand hand = winner.getHand();

        if (!selfDraw && winningTile != null) {
            // The claimed discard becomes part of the winning hand.
            hand.addTile(winningTile);
            List<Tile> discards = state.getDiscards();
            for (int i = discards.size() - 1; i >= 0; i--) {
                if (discards.get(i).equals(winningTile)) {
                    discards.remove(i);
                    break;
                }
            }
            state.clearLastDiscard();
        }

        WinContext ctx = new WinContext();
        ctx.selfDraw = selfDraw;
        ctx.fullyConcealed = !hand.hasClaimedDiscard() && selfDraw;
        ctx.dealer = winnerIndex == state.getDealer();
        ctx.seatWind = winner.getSeatWind();
        ctx.roundWind = state.getRoundWind();
        ctx.winningTile = winningTile;
        ctx.lastTile = selfDraw && state.getWall().isEmpty();
        ctx.finalDiscard = !selfDraw && state.getWall().isEmpty();
        ctx.firstGoAround = state.isFirstGoAround();

        boolean heavenly = selfDraw && ctx.dealer && state.getDiscards().isEmpty();
        boolean earthly = !selfDraw && !ctx.dealer
                && state.getDiscards().size() <= 1 && state.isFirstGoAround();
        boolean human = selfDraw && !ctx.dealer && state.isFirstGoAround()
                && state.getTilesPlayed() < Constants.NUM_PLAYERS;
        ctx.firstTile = heavenly || earthly || human;

        ScoreSheet sheet = scoring.scoreWin(hand, ctx, state.getLimit());

        HandResult result = new HandResult(state.getPlayers().size());
        result.winner = winnerIndex;
        result.sheet = sheet;
        int value = sheet.getFinalScore();
        for (int p = 0; p < state.getPlayers().size(); p++) {
            if (p == winnerIndex) {
                result.deltas[p] = value * (state.getPlayers().size() - 1);
            } else {
                result.deltas[p] = -value;
            }
            state.getPlayer(p).addScore(result.deltas[p]);
        }
        result.message = winner.getName() + " wins with " + sheet.getTitle()
                + " for " + value + " points!";
        log.add(result.message);
        lastResult = result;
        phase = Phase.OVER;
    }

    private void resolveDrawnGame() {
        HandResult result = new HandResult(state.getPlayers().size());
        result.drawnGame = true;
        result.message = "Drawn game — the wall is exhausted. No score.";
        log.add(result.message);
        lastResult = result;
        phase = Phase.OVER;
    }

    private void resolveFalseMahjong(int offender) {
        HandResult result = new HandResult(state.getPlayers().size());
        result.falseMahjong = true;
        result.offender = offender;
        int penalty = Constants.FALSE_MAHJONG_PENALTY;
        for (int p = 0; p < state.getPlayers().size(); p++) {
            if (p == offender) {
                result.deltas[p] = -penalty * (state.getPlayers().size() - 1);
            } else {
                result.deltas[p] = penalty;
            }
            state.getPlayer(p).addScore(result.deltas[p]);
        }
        result.message = state.getPlayer(offender).getName()
                + " declared a false mahjong and pays a " + penalty + " penalty to each player.";
        log.add(result.message);
        lastResult = result;
        phase = Phase.OVER;
    }

    // ============================ round / hand progression ============================

    /** Advance to the next hand (or end the game). Call after HAND_OVER. */
    public void nextHand() {
        boolean dealerWon = lastResult != null && lastResult.winner == state.getDealer()
                && !lastResult.drawnGame && !lastResult.falseMahjong;

        int next = state.getHandNumber() + 1;
        if (next > Constants.TOTAL_HANDS) {
            gameOver = true;
            phase = Phase.OVER;
            return;
        }
        state.setHandNumber(next);

        if (!dealerWon) {
            state.setDealer(state.nextSeat(state.getDealer()));
            assignSeatWinds(state.getDealer());
            dealerPasses++;
            if (dealerPasses >= Constants.NUM_PLAYERS) {
                state.setRoundWind(state.getRoundWind().next());
                dealerPasses = 0;
            }
        }
        dealHand();
    }

    // ============================ human input ============================

    public boolean isAwaitingHuman() {
        Player current = state.getCurrentPlayer();
        return current.isHuman() && (phase == Phase.DRAW || phase == Phase.DISCARD);
    }

    /** Human draws from the wall (handling bonus replacement). */
    public void humanDraw() {
        int human = state.humanIndex();
        if (phase != Phase.DRAW || state.getCurrentTurn() != human) {
            return;
        }
        Tile drawn = drawWithBonus(human);
        if (drawn == null) {
            resolveDrawnGame();
            return;
        }
        phase = Phase.DISCARD;
    }

    public boolean canHumanSelfMahjong() {
        int human = state.humanIndex();
        return phase == Phase.DISCARD && state.getCurrentTurn() == human
                && isSelfWin(state.getPlayer(human).getHand());
    }

    public List<Tile> humanConcealedKongOptions() {
        List<Tile> out = new ArrayList<Tile>();
        int human = state.humanIndex();
        if (phase != Phase.DISCARD || state.getCurrentTurn() != human) {
            return out;
        }
        Hand hand = state.getPlayer(human).getHand();
        for (int i = 0; i < hand.getTiles().size(); i++) {
            Tile t = hand.getTiles().get(i);
            if (hand.count(t) >= 4 && !out.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    public List<Tile> humanUpgradeKongOptions() {
        List<Tile> out = new ArrayList<Tile>();
        int human = state.humanIndex();
        if (phase != Phase.DISCARD || state.getCurrentTurn() != human) {
            return out;
        }
        Hand hand = state.getPlayer(human).getHand();
        for (int i = 0; i < hand.getMelds().size(); i++) {
            Meld m = hand.getMelds().get(i);
            if (m.isPung() && hand.count(m.representative()) >= 1) {
                out.add(m.representative());
            }
        }
        return out;
    }

    public void humanConcealedKong(Tile face) {
        int human = state.humanIndex();
        if (phase != Phase.DISCARD || state.getCurrentTurn() != human) {
            return;
        }
        declareConcealedKong(human, face);
    }

    public void humanUpgradeKong(Tile face) {
        int human = state.humanIndex();
        if (phase != Phase.DISCARD || state.getCurrentTurn() != human) {
            return;
        }
        upgradeKong(human, face);
    }

    /** Human discards, ending the turn. */
    public void humanDiscard(Tile tile) {
        int human = state.humanIndex();
        if (phase != Phase.DISCARD || state.getCurrentTurn() != human) {
            return;
        }
        performDiscard(human, tile);
        log.add("You discard " + tile.displayName() + ".");
        phase = Phase.CLAIM;
    }

    /** Human declares mahjong on their own drawn tile. */
    public void humanSelfMahjong() {
        int human = state.humanIndex();
        if (phase != Phase.DISCARD || state.getCurrentTurn() != human) {
            return;
        }
        if (isSelfWin(state.getPlayer(human).getHand())) {
            resolveWin(human, true, lastDrawnTile);
        } else {
            resolveFalseMahjong(human);
        }
    }

    // claim-window responses
    public void humanClaimPung() {
        humanClaimChoice = AIPlayerLogic.ClaimDecision.CLAIM_PUNG;
    }

    public void humanClaimMahjong() {
        humanClaimChoice = AIPlayerLogic.ClaimDecision.CLAIM_MAHJONG;
    }

    public void humanPass() {
        humanClaimChoice = AIPlayerLogic.ClaimDecision.PASS;
    }

    // ============================ Playable interface ============================

    public Tile drawTile(int playerIndex) {
        return drawWithBonus(playerIndex);
    }

    public void discardTile(int playerIndex, Tile tile) {
        performDiscard(playerIndex, tile);
    }

    public boolean claimMeld(int playerIndex, MeldType type) {
        Tile discard = state.getLastDiscard();
        if (discard == null || type != MeldType.PUNG) {
            return false; // only pung claims are legal from a discard
        }
        if (!ClaimResolver.canPung(state.getPlayer(playerIndex).getHand(), discard)) {
            return false;
        }
        applyPung(playerIndex, discard);
        state.setCurrentTurn(playerIndex);
        phase = Phase.DISCARD;
        return true;
    }

    public boolean declareMahjong(int playerIndex) {
        Hand hand = state.getPlayer(playerIndex).getHand();
        Tile discard = state.getLastDiscard();
        boolean onDiscard = discard != null && state.getCurrentTurn() != playerIndex;
        if (onDiscard) {
            if (ClaimResolver.canMahjong(hand, discard)) {
                resolveWin(playerIndex, false, discard);
                return true;
            }
        } else if (isSelfWin(hand)) {
            resolveWin(playerIndex, true, lastDrawnTile);
            return true;
        }
        resolveFalseMahjong(playerIndex);
        return false;
    }
}
