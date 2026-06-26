package com.whim.monopoly.engine;

import com.whim.monopoly.domain.Board;
import com.whim.monopoly.domain.Card;
import com.whim.monopoly.domain.CardAction;
import com.whim.monopoly.domain.ColorGroup;
import com.whim.monopoly.domain.Deck;
import com.whim.monopoly.domain.OwnableSpace;
import com.whim.monopoly.domain.Player;
import com.whim.monopoly.domain.RailroadSpace;
import com.whim.monopoly.domain.Space;
import com.whim.monopoly.domain.SpaceType;
import com.whim.monopoly.domain.StreetSpace;
import com.whim.monopoly.domain.TaxSpace;
import com.whim.monopoly.domain.UtilitySpace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Reference implementation of the Monopoly rules engine. Owns all mutable
 * game state and drives every change through {@link GameListener} callbacks.
 *
 * <p>Official standard ruleset only — no house rules.</p>
 */
public final class StandardGameEngine implements GameEngine {

    private static final int BOARD_SIZE = 40;
    private static final int GO_SALARY = 200;
    private static final int JAIL_INDEX = 10;
    private static final int GO_TO_JAIL_INDEX = 30;
    private static final int JAIL_FINE = 50;
    private static final int TOTAL_HOUSES = 32;
    private static final int TOTAL_HOTELS = 12;

    private final Board board;
    private final List<Player> players;
    private final Random rng;
    private final List<GameListener> listeners = new ArrayList<GameListener>();

    // ownership / building state, indexed by board position 0..39
    private final Player[] owner = new Player[BOARD_SIZE];
    private final int[] level = new int[BOARD_SIZE];       // 0..5 (5 == hotel)
    private final boolean[] mortgaged = new boolean[BOARD_SIZE];

    private int houseSupply = TOTAL_HOUSES;
    private int hotelSupply = TOTAL_HOTELS;

    // decks
    private final DeckState chance;
    private final DeckState community;
    // jail-free cards currently held by players (so they can be returned to the right deck)
    private final List<List<Card>> heldJailCards = new ArrayList<List<Card>>();

    // turn / phase state
    private int currentIndex = 0;
    private TurnPhase phase = TurnPhase.AWAITING_ROLL;
    private final int[] lastDice = new int[]{0, 0};
    private int doublesCount = 0;
    private boolean pendingExtraRoll = false;
    private int pendingBuyIndex = -1;
    private boolean gameOver = false;
    private Player winner = null;

    // outstanding debt for the interactive (current) player
    private Player debtor = null;
    private Player debtCreditor = null;   // null => bank
    private int debtAmount = 0;

    // auction state
    private OwnableSpace auctionSpace = null;
    private int auctionHighBid = 0;
    private Player auctionHighBidder = null;
    private final List<Player> auctionActive = new ArrayList<Player>();
    private boolean auctionFromBankruptcy = false;
    private final LinkedList<Integer> pendingBankAuctions = new LinkedList<Integer>();
    private boolean resumeTurnAfterAuction = false;

    private final StateView stateView = new StateView();

    public StandardGameEngine(List<Player> players, Board board,
                              List<Card> chance, List<Card> communityChest, Random rng) {
        if (players == null || players.size() < 2 || players.size() > 6) {
            throw new IllegalArgumentException("Monopoly requires 2-6 players");
        }
        this.board = board;
        this.players = new ArrayList<Player>(players);
        this.rng = rng;
        this.chance = new DeckState(chance, rng);
        this.community = new DeckState(communityChest, rng);
        for (int i = 0; i < this.players.size(); i++) {
            heldJailCards.add(new ArrayList<Card>());
        }
    }

    // ---------------------------------------------------------------- listeners

    public void addListener(GameListener l) {
        if (l != null) listeners.add(l);
    }

    private void log(String msg) {
        for (GameListener l : listeners) l.onLog(msg);
    }

    private void fireStateChanged() {
        for (GameListener l : listeners) l.onStateChanged();
    }

    private void fireGameOver(Player w) {
        for (GameListener l : listeners) l.onGameOver(w);
    }

    public GameState getState() { return stateView; }

    // ---------------------------------------------------------------- turn flow

    public void rollDice() {
        if (gameOver) return;
        if (phase != TurnPhase.AWAITING_ROLL) {
            throw new IllegalStateException("Cannot roll in phase " + phase);
        }
        Player p = current();
        int d1 = rng.nextInt(6) + 1;
        int d2 = rng.nextInt(6) + 1;
        lastDice[0] = d1;
        lastDice[1] = d2;
        boolean doubles = d1 == d2;
        log(p.getName() + " rolls " + d1 + " + " + d2 + " = " + (d1 + d2) + (doubles ? " (doubles)" : ""));

        if (p.isInJail()) {
            rollInJail(p, d1, d2, doubles);
            return;
        }

        if (doubles) {
            doublesCount++;
            if (doublesCount >= 3) {
                log(p.getName() + " rolled three doubles in a row -> Go to Jail!");
                sendToJail(p);
                pendingExtraRoll = false;
                phase = TurnPhase.AWAITING_END_TURN;
                fireStateChanged();
                return;
            }
            pendingExtraRoll = true;
        } else {
            pendingExtraRoll = false;
        }

        moveBy(p, d1 + d2, true);
        resolveLanding(p, d1 + d2);
    }

    private void rollInJail(Player p, int d1, int d2, boolean doubles) {
        if (doubles) {
            log(p.getName() + " rolled doubles and leaves Jail (no extra turn).");
            p.setInJail(false);
            p.setJailTurns(0);
            pendingExtraRoll = false;
            moveBy(p, d1 + d2, true);
            resolveLanding(p, d1 + d2);
            return;
        }
        int attempts = p.getJailTurns() + 1;
        p.setJailTurns(attempts);
        if (attempts >= 3) {
            log(p.getName() + " failed a 3rd time and must pay $" + JAIL_FINE + " to leave Jail.");
            boolean paid = charge(p, JAIL_FINE, null, true);
            if (!paid) {
                // could not pay the fine; remains for debt resolution
                phase = TurnPhase.AWAITING_END_TURN;
                fireStateChanged();
                return;
            }
            p.setInJail(false);
            p.setJailTurns(0);
            moveBy(p, d1 + d2, true);
            resolveLanding(p, d1 + d2);
        } else {
            log(p.getName() + " stays in Jail (attempt " + attempts + " of 3).");
            phase = TurnPhase.AWAITING_END_TURN;
            fireStateChanged();
        }
    }

    public void endTurn() {
        if (gameOver) return;
        if (phase != TurnPhase.AWAITING_END_TURN) {
            throw new IllegalStateException("Cannot end turn in phase " + phase);
        }
        if (debtor != null) {
            throw new IllegalStateException("Resolve outstanding debt before ending turn");
        }
        doublesCount = 0;
        pendingExtraRoll = false;
        advanceToNextActive();
        phase = TurnPhase.AWAITING_ROLL;
        log("--- " + current().getName() + "'s turn ---");
        fireStateChanged();
    }

    private void advanceToNextActive() {
        for (int i = 0; i < players.size(); i++) {
            currentIndex = (currentIndex + 1) % players.size();
            if (!players.get(currentIndex).isBankrupt()) return;
        }
    }

    private void finishTurnSegment() {
        if (checkGameOver()) return;
        if (debtor != null) {
            phase = TurnPhase.AWAITING_END_TURN;
            fireStateChanged();
            return;
        }
        if (pendingExtraRoll) {
            pendingExtraRoll = false;
            phase = TurnPhase.AWAITING_ROLL;
            log(current().getName() + " rolled doubles and rolls again.");
        } else {
            phase = TurnPhase.AWAITING_END_TURN;
        }
        fireStateChanged();
    }

    // ------------------------------------------------------------ movement / landing

    private void moveBy(Player p, int steps, boolean collectGo) {
        int from = p.getPosition();
        int to = (from + steps) % BOARD_SIZE;
        if (to < 0) to += BOARD_SIZE;
        if (collectGo && steps > 0 && from + steps >= BOARD_SIZE) {
            p.addCash(GO_SALARY);
            log(p.getName() + " passes GO and collects $" + GO_SALARY + ".");
        }
        p.setPosition(to);
        log(p.getName() + " moves to " + board.spaceAt(to).getName() + " (" + to + ").");
    }

    private void moveToIndex(Player p, int target, boolean collectGo) {
        int steps = ((target - p.getPosition()) % BOARD_SIZE + BOARD_SIZE) % BOARD_SIZE;
        moveBy(p, steps, collectGo);
    }

    private void resolveLanding(Player p, int diceSum) {
        Space s = board.spaceAt(p.getPosition());
        SpaceType type = s.getType();
        switch (type) {
            case GO:
            case FREE_PARKING:
            case JAIL:
                finishTurnSegment();
                break;
            case GO_TO_JAIL:
                log(p.getName() + " landed on Go To Jail.");
                sendToJail(p);
                pendingExtraRoll = false;
                phase = TurnPhase.AWAITING_END_TURN;
                fireStateChanged();
                break;
            case TAX: {
                int amt = ((TaxSpace) s).getTaxAmount();
                log(p.getName() + " pays $" + amt + " in tax.");
                charge(p, amt, null, true);
                finishTurnSegment();
                break;
            }
            case STREET:
            case RAILROAD:
            case UTILITY:
                resolveOwnable(p, p.getPosition(), diceSum, 1, false, 0);
                break;
            case CHANCE:
            case COMMUNITY_CHEST:
                drawAndApply(p, type == SpaceType.CHANCE ? chance : community);
                break;
            default:
                finishTurnSegment();
        }
    }

    /**
     * Resolve landing on an ownable space.
     *
     * @param railMult    rent multiplier for railroads (2 for the Chance card)
     * @param utilForce10  force 10x utility rent (Chance "nearest utility")
     * @param utilDice     dice value to use for forced utility rent
     */
    private void resolveOwnable(Player p, int idx, int diceSum,
                                int railMult, boolean utilForce10, int utilDice) {
        OwnableSpace os = (OwnableSpace) board.spaceAt(idx);
        Player o = owner[idx];
        if (o == null) {
            pendingBuyIndex = idx;
            phase = TurnPhase.AWAITING_BUY;
            log(os.getName() + " is unowned ($" + os.getPrice() + ").");
            fireStateChanged();
            return;
        }
        if (o == p) {
            log(p.getName() + " owns " + os.getName() + ".");
            finishTurnSegment();
            return;
        }
        if (mortgaged[idx]) {
            log(os.getName() + " is mortgaged; no rent due.");
            finishTurnSegment();
            return;
        }
        int rent = computeRent(idx, diceSum, railMult, utilForce10, utilDice);
        log(p.getName() + " pays $" + rent + " rent to " + o.getName() + " for " + os.getName() + ".");
        charge(p, rent, o, true);
        finishTurnSegment();
    }

    private int computeRent(int idx, int diceSum, int railMult, boolean utilForce10, int utilDice) {
        Space s = board.spaceAt(idx);
        Player o = owner[idx];
        if (s instanceof StreetSpace) {
            StreetSpace st = (StreetSpace) s;
            int[] table = st.getRentTable();
            int lv = level[idx];
            if (lv >= 5) return table[5];
            if (lv >= 1) return table[lv];
            int base = table[0];
            if (ownsWholeGroup(o, st.getColorGroup())) base *= 2;
            return base;
        }
        if (s instanceof RailroadSpace) {
            int count = countRailroads(o);
            int rent = 25 * (1 << (count - 1)); // 25,50,100,200
            return rent * railMult;
        }
        if (s instanceof UtilitySpace) {
            if (utilForce10) return 10 * utilDice;
            int count = countUtilities(o);
            int mult = (count >= 2) ? 10 : 4;
            return mult * diceSum;
        }
        return 0;
    }

    // ------------------------------------------------------------ cards

    private void drawAndApply(Player p, DeckState deck) {
        Card c = deck.draw();
        log(p.getName() + " draws: \"" + c.getText() + "\"");
        applyCard(p, c, deck);
    }

    private void applyCard(Player p, Card c, DeckState deck) {
        switch (c.getAction()) {
            case COLLECT:
                p.addCash(c.getAmount());
                log(p.getName() + " collects $" + c.getAmount() + ".");
                finishTurnSegment();
                break;
            case PAY:
                charge(p, c.getAmount(), null, true);
                finishTurnSegment();
                break;
            case MOVE_TO_SPACE:
                moveToIndex(p, c.getTargetIndex(), true);
                resolveLanding(p, lastDice[0] + lastDice[1]);
                break;
            case MOVE_BACK:
                moveBy(p, -c.getAmount(), false);
                resolveLanding(p, lastDice[0] + lastDice[1]);
                break;
            case GO_TO_JAIL:
                sendToJail(p);
                pendingExtraRoll = false;
                phase = TurnPhase.AWAITING_END_TURN;
                fireStateChanged();
                break;
            case GET_OUT_OF_JAIL_FREE:
                p.setJailCards(p.getJailCards() + 1);
                heldJailCards.get(indexOf(p)).add(c);
                log(p.getName() + " keeps a Get Out of Jail Free card.");
                finishTurnSegment();
                break;
            case NEAREST_RAILROAD: {
                int target = board.nextRailroadFrom(p.getPosition());
                moveToIndex(p, target, true);
                resolveOwnable(p, target, lastDice[0] + lastDice[1], 2, false, 0);
                break;
            }
            case NEAREST_UTILITY: {
                int target = board.nextUtilityFrom(p.getPosition());
                moveToIndex(p, target, true);
                int r1 = rng.nextInt(6) + 1;
                int r2 = rng.nextInt(6) + 1;
                log(p.getName() + " re-rolls " + r1 + " + " + r2 + " for utility rent.");
                resolveOwnable(p, target, r1 + r2, 1, true, r1 + r2);
                break;
            }
            case STREET_REPAIRS: {
                int houses = 0, hotels = 0;
                for (Integer idx : p.getDeeds()) {
                    if (level[idx] == 5) hotels++;
                    else houses += level[idx];
                }
                int amt = houses * c.getAmount() + hotels * c.getAmount2();
                log(p.getName() + " pays $" + amt + " for repairs (" + houses + "h/" + hotels + " hotels).");
                charge(p, amt, null, true);
                finishTurnSegment();
                break;
            }
            case COLLECT_FROM_EACH: {
                for (Player other : players) {
                    if (other != p && !other.isBankrupt()) {
                        charge(other, c.getAmount(), p, false);
                    }
                }
                finishTurnSegment();
                break;
            }
            case PAY_EACH: {
                int total = 0;
                for (Player other : players) {
                    if (other != p && !other.isBankrupt()) total += c.getAmount();
                }
                log(p.getName() + " pays $" + c.getAmount() + " to each opponent.");
                // pay each individually so a creditor receives exactly their share
                for (Player other : players) {
                    if (other != p && !other.isBankrupt()) {
                        charge(p, c.getAmount(), other, true);
                        if (debtor != null) break;
                    }
                }
                if (total == 0) { /* nothing */ }
                finishTurnSegment();
                break;
            }
            default:
                finishTurnSegment();
        }
    }

    // ------------------------------------------------------------ buy / auction

    public void buyProperty() {
        if (phase != TurnPhase.AWAITING_BUY) {
            throw new IllegalStateException("Not awaiting a buy decision");
        }
        Player p = current();
        OwnableSpace os = (OwnableSpace) board.spaceAt(pendingBuyIndex);
        if (p.getCash() < os.getPrice()) {
            throw new IllegalStateException("Insufficient cash to buy " + os.getName());
        }
        p.addCash(-os.getPrice());
        setOwner(pendingBuyIndex, p);
        log(p.getName() + " buys " + os.getName() + " for $" + os.getPrice() + ".");
        pendingBuyIndex = -1;
        finishTurnSegment();
    }

    public void declineProperty() {
        if (phase != TurnPhase.AWAITING_BUY) {
            throw new IllegalStateException("Not awaiting a buy decision");
        }
        int idx = pendingBuyIndex;
        pendingBuyIndex = -1;
        log(current().getName() + " declines " + board.spaceAt(idx).getName() + " -> auction.");
        startAuction(idx, false);
    }

    private void startAuction(int idx, boolean fromBankruptcy) {
        auctionSpace = (OwnableSpace) board.spaceAt(idx);
        auctionHighBid = 0;
        auctionHighBidder = null;
        auctionFromBankruptcy = fromBankruptcy;
        auctionActive.clear();
        for (Player p : players) {
            if (!p.isBankrupt()) auctionActive.add(p);
        }
        phase = TurnPhase.AUCTION;
        log("Auction for " + auctionSpace.getName() + " begins. Bidding starts at $0.");
        fireStateChanged();
        if (auctionActive.size() <= 1) resolveAuctionIfDone();
    }

    public void placeBid(Player p, int amount) {
        if (phase != TurnPhase.AUCTION) throw new IllegalStateException("No auction in progress");
        if (!auctionActive.contains(p)) throw new IllegalStateException(p.getName() + " is not in the auction");
        if (amount <= auctionHighBid) throw new IllegalStateException("Bid must exceed $" + auctionHighBid);
        if (amount > p.getCash()) throw new IllegalStateException(p.getName() + " cannot afford that bid");
        auctionHighBid = amount;
        auctionHighBidder = p;
        log(p.getName() + " bids $" + amount + ".");
        fireStateChanged();
    }

    public void passAuction(Player p) {
        if (phase != TurnPhase.AUCTION) throw new IllegalStateException("No auction in progress");
        auctionActive.remove(p);
        log(p.getName() + " passes.");
        resolveAuctionIfDone();
    }

    private void resolveAuctionIfDone() {
        int in = auctionActive.size();
        if (in >= 2) { fireStateChanged(); return; }
        if (in == 1 && auctionHighBidder == null) {
            // single remaining bidder who has not yet bid: wait for a bid or pass
            fireStateChanged();
            return;
        }
        // award or unowned
        OwnableSpace os = auctionSpace;
        Player w = auctionHighBidder;
        int bid = auctionHighBid;
        boolean fromBank = auctionFromBankruptcy;
        int idx = os.getIndex();
        auctionSpace = null;
        auctionHighBidder = null;
        auctionHighBid = 0;
        auctionActive.clear();
        if (w != null) {
            w.addCash(-bid);
            setOwner(idx, w);
            log(w.getName() + " wins " + os.getName() + " at auction for $" + bid + ".");
        } else {
            log("No bids; " + os.getName() + " remains unowned.");
        }
        fireStateChanged();
        afterAuction(fromBank);
    }

    private void afterAuction(boolean fromBank) {
        if (fromBank && !pendingBankAuctions.isEmpty()) {
            startAuction(pendingBankAuctions.removeFirst(), true);
            return;
        }
        if (fromBank) {
            // bankruptcy auctions complete
            if (checkGameOver()) return;
            if (resumeTurnAfterAuction) {
                resumeTurnAfterAuction = false;
                phase = TurnPhase.AWAITING_ROLL;
                log("--- " + current().getName() + "'s turn ---");
                fireStateChanged();
            }
            return;
        }
        finishTurnSegment();
    }

    // ------------------------------------------------------------ building

    public boolean canBuildHouse(int spaceIndex) {
        if (phase != TurnPhase.AWAITING_END_TURN) return false;
        Space s = board.spaceAt(spaceIndex);
        if (!(s instanceof StreetSpace)) return false;
        Player p = current();
        if (owner[spaceIndex] != p) return false;
        StreetSpace st = (StreetSpace) s;
        ColorGroup g = st.getColorGroup();
        if (!ownsWholeGroup(p, g)) return false;
        if (groupHasMortgage(p, g)) return false;
        if (level[spaceIndex] >= 5) return false;
        // even build: cannot exceed the minimum level in the group
        int min = minGroupLevel(g);
        if (level[spaceIndex] != min) return false;
        if (level[spaceIndex] == 4) {
            return hotelSupply >= 1;          // building the hotel
        }
        return houseSupply >= 1;              // building a house
    }

    public void buildHouse(int spaceIndex) {
        if (!canBuildHouse(spaceIndex)) {
            throw new IllegalStateException("Illegal build at index " + spaceIndex);
        }
        Player p = current();
        StreetSpace st = (StreetSpace) board.spaceAt(spaceIndex);
        if (p.getCash() < st.getHouseCost()) {
            throw new IllegalStateException("Insufficient cash to build");
        }
        p.addCash(-st.getHouseCost());
        if (level[spaceIndex] == 4) {
            level[spaceIndex] = 5;
            houseSupply += 4;   // four houses return to the bank
            hotelSupply -= 1;
            log(p.getName() + " builds a HOTEL on " + st.getName() + ".");
        } else {
            level[spaceIndex] += 1;
            houseSupply -= 1;
            log(p.getName() + " builds a house on " + st.getName() + " (now " + level[spaceIndex] + ").");
        }
        fireStateChanged();
    }

    public void sellHouse(int spaceIndex) {
        Space s = board.spaceAt(spaceIndex);
        if (!(s instanceof StreetSpace)) throw new IllegalStateException("Not a street");
        Player p = ownerForAction(spaceIndex);
        StreetSpace st = (StreetSpace) s;
        ColorGroup g = st.getColorGroup();
        if (level[spaceIndex] <= 0) throw new IllegalStateException("Nothing to sell");
        if (level[spaceIndex] != maxGroupLevel(g)) {
            throw new IllegalStateException("Must sell evenly (highest first)");
        }
        int refund = st.getHouseCost() / 2;
        if (level[spaceIndex] == 5) {
            if (houseSupply < 4) throw new IllegalStateException("Not enough houses in bank to break the hotel");
            level[spaceIndex] = 4;
            hotelSupply += 1;
            houseSupply -= 4;
            log(p.getName() + " sells the hotel on " + st.getName() + " for $" + refund + ".");
        } else {
            level[spaceIndex] -= 1;
            houseSupply += 1;
            log(p.getName() + " sells a house on " + st.getName() + " for $" + refund + ".");
        }
        p.addCash(refund);
        tryAutoSettleDebt();
        fireStateChanged();
    }

    // ------------------------------------------------------------ mortgage

    public boolean canMortgage(int spaceIndex) {
        Space s = board.spaceAt(spaceIndex);
        if (!(s instanceof OwnableSpace)) return false;
        Player p = current();
        if (owner[spaceIndex] != p) return false;
        if (mortgaged[spaceIndex]) return false;
        if (s instanceof StreetSpace) {
            ColorGroup g = ((StreetSpace) s).getColorGroup();
            if (groupHasBuildings(g)) return false;   // sell buildings first (whole group)
        }
        return true;
    }

    public void mortgage(int spaceIndex) {
        if (!canMortgage(spaceIndex)) throw new IllegalStateException("Cannot mortgage index " + spaceIndex);
        OwnableSpace os = (OwnableSpace) board.spaceAt(spaceIndex);
        mortgaged[spaceIndex] = true;
        Player p = current();
        p.addCash(os.getMortgageValue());
        log(p.getName() + " mortgages " + os.getName() + " for $" + os.getMortgageValue() + ".");
        tryAutoSettleDebt();
        fireStateChanged();
    }

    public void unmortgage(int spaceIndex) {
        Space s = board.spaceAt(spaceIndex);
        if (!(s instanceof OwnableSpace)) throw new IllegalStateException("Not ownable");
        Player p = current();
        if (owner[spaceIndex] != p) throw new IllegalStateException("Not your property");
        if (!mortgaged[spaceIndex]) throw new IllegalStateException("Not mortgaged");
        OwnableSpace os = (OwnableSpace) s;
        int cost = os.getUnmortgageCost();
        if (p.getCash() < cost) throw new IllegalStateException("Insufficient cash to unmortgage");
        p.addCash(-cost);
        mortgaged[spaceIndex] = false;
        log(p.getName() + " unmortgages " + os.getName() + " for $" + cost + ".");
        fireStateChanged();
    }

    // ------------------------------------------------------------ jail

    public void payJailFine() {
        Player p = current();
        if (!p.isInJail()) throw new IllegalStateException("Not in jail");
        if (phase != TurnPhase.AWAITING_ROLL) throw new IllegalStateException("Cannot pay now");
        if (p.getCash() < JAIL_FINE) throw new IllegalStateException("Insufficient cash for fine");
        p.addCash(-JAIL_FINE);
        p.setInJail(false);
        p.setJailTurns(0);
        log(p.getName() + " pays $" + JAIL_FINE + " and is released from Jail.");
        fireStateChanged();
    }

    public void useJailCard() {
        Player p = current();
        if (!p.isInJail()) throw new IllegalStateException("Not in jail");
        if (p.getJailCards() <= 0) throw new IllegalStateException("No jail card");
        if (phase != TurnPhase.AWAITING_ROLL) throw new IllegalStateException("Cannot use card now");
        List<Card> held = heldJailCards.get(indexOf(p));
        if (!held.isEmpty()) {
            Card c = held.remove(held.size() - 1);
            deckFor(c.getDeck()).discard(c);
        }
        p.setJailCards(p.getJailCards() - 1);
        p.setInJail(false);
        p.setJailTurns(0);
        log(p.getName() + " uses a Get Out of Jail Free card.");
        fireStateChanged();
    }

    // ------------------------------------------------------------ trading

    public boolean isTradeValid(Trade t) {
        if (t == null) return false;
        Player a = t.getProposer();
        Player b = t.getRecipient();
        if (a == null || b == null || a == b) return false;
        if (a.isBankrupt() || b.isBankrupt()) return false;
        if (t.getProposerCash() < 0 || t.getRecipientCash() < 0) return false;
        if (t.getProposerJailCards() < 0 || t.getRecipientJailCards() < 0) return false;
        if (a.getCash() < t.getProposerCash()) return false;
        if (b.getCash() < t.getRecipientCash()) return false;
        if (a.getJailCards() < t.getProposerJailCards()) return false;
        if (b.getJailCards() < t.getRecipientJailCards()) return false;
        if (!ownsAllUnimproved(a, t.getProposerDeeds())) return false;
        if (!ownsAllUnimproved(b, t.getRecipientDeeds())) return false;
        return true;
    }

    private boolean ownsAllUnimproved(Player p, java.util.Set<Integer> deeds) {
        if (deeds == null) return true;
        for (Integer idx : deeds) {
            if (idx == null || idx < 0 || idx >= BOARD_SIZE) return false;
            if (owner[idx] != p) return false;
            if (level[idx] > 0) return false;   // cannot trade improved property
        }
        return true;
    }

    public void executeTrade(Trade t) {
        if (!isTradeValid(t)) throw new IllegalStateException("Invalid trade");
        Player a = t.getProposer();
        Player b = t.getRecipient();
        a.addCash(-t.getProposerCash());
        b.addCash(t.getProposerCash());
        b.addCash(-t.getRecipientCash());
        a.addCash(t.getRecipientCash());
        for (Integer idx : t.getProposerDeeds()) setOwner(idx, b);
        for (Integer idx : t.getRecipientDeeds()) setOwner(idx, a);
        transferJailCards(a, b, t.getProposerJailCards());
        transferJailCards(b, a, t.getRecipientJailCards());
        log("Trade executed between " + a.getName() + " and " + b.getName() + ".");
        tryAutoSettleDebt();
        fireStateChanged();
    }

    private void transferJailCards(Player from, Player to, int n) {
        List<Card> fromHeld = heldJailCards.get(indexOf(from));
        List<Card> toHeld = heldJailCards.get(indexOf(to));
        for (int i = 0; i < n && !fromHeld.isEmpty(); i++) {
            toHeld.add(fromHeld.remove(fromHeld.size() - 1));
        }
        from.setJailCards(from.getJailCards() - n);
        to.setJailCards(to.getJailCards() + n);
    }

    // ------------------------------------------------------------ debt / bankruptcy

    /**
     * Charge {@code payer} {@code amount}, paying {@code creditor} (null = bank).
     * Returns true if settled immediately. If interactive and the payer lacks cash,
     * records an outstanding debt (the player must mortgage/sell or declare bankruptcy).
     * If non-interactive, auto-liquidates or bankrupts the payer at once.
     */
    private boolean charge(Player payer, int amount, Player creditor, boolean interactive) {
        if (amount <= 0) return true;
        if (payer.getCash() >= amount) {
            payer.addCash(-amount);
            if (creditor != null) creditor.addCash(amount);
            return true;
        }
        if (interactive) {
            debtor = payer;
            debtCreditor = creditor;
            debtAmount = amount;
            log(payer.getName() + " owes $" + amount + " to "
                    + (creditor == null ? "the bank" : creditor.getName())
                    + " but only has $" + payer.getCash()
                    + ". Mortgage/sell or declare bankruptcy.");
            return false;
        }
        // non-interactive forced collection
        if (netWorth(payer) >= amount) {
            autoLiquidate(payer, amount);
            payer.addCash(-amount);
            if (creditor != null) creditor.addCash(amount);
            return true;
        }
        performBankruptcy(payer, creditor);
        return false;
    }

    private void tryAutoSettleDebt() {
        if (debtor != null && debtor.getCash() >= debtAmount) {
            Player d = debtor;
            Player c = debtCreditor;
            int amt = debtAmount;
            debtor = null;
            debtCreditor = null;
            debtAmount = 0;
            d.addCash(-amt);
            if (c != null) c.addCash(amt);
            log(d.getName() + " settles the $" + amt + " debt to "
                    + (c == null ? "the bank" : c.getName()) + ".");
        }
    }

    public void declareBankruptcy() {
        Player d = (debtor != null) ? debtor : current();
        Player c = debtCreditor;
        log(d.getName() + " declares bankruptcy.");
        debtor = null;
        debtAmount = 0;
        debtCreditor = null;
        performBankruptcy(d, c);
    }

    private void performBankruptcy(Player d, Player creditor) {
        d.setBankrupt(true);
        if (creditor != null) {
            // sell buildings back to the bank, the cash goes to the creditor
            for (Integer idx : new ArrayList<Integer>(d.getDeeds())) {
                if (level[idx] > 0) {
                    int refund = buildingRefund(idx);
                    returnBuildings(idx);
                    creditor.addCash(refund);
                }
            }
            // transfer remaining cash
            if (d.getCash() > 0) {
                creditor.addCash(d.getCash());
            }
            d.setCash(0);
            // transfer deeds (mortgaged stay mortgaged)
            for (Integer idx : new ArrayList<Integer>(d.getDeeds())) {
                setOwner(idx, creditor);
            }
            // transfer jail cards
            List<Card> dHeld = heldJailCards.get(indexOf(d));
            List<Card> cHeld = heldJailCards.get(indexOf(creditor));
            cHeld.addAll(dHeld);
            dHeld.clear();
            creditor.setJailCards(creditor.getJailCards() + d.getJailCards());
            d.setJailCards(0);
            log(d.getName() + "'s assets transfer to " + creditor.getName() + ".");
        } else {
            // debt to the bank: sell buildings, return jail cards, auction deeds
            List<Integer> deeds = new ArrayList<Integer>(d.getDeeds());
            for (Integer idx : deeds) {
                if (level[idx] > 0) returnBuildings(idx);
            }
            List<Card> dHeld = heldJailCards.get(indexOf(d));
            for (Card c : dHeld) deckFor(c.getDeck()).discard(c);
            dHeld.clear();
            d.setJailCards(0);
            d.setCash(0);
            pendingBankAuctions.clear();
            for (Integer idx : deeds) {
                owner[idx] = null;
                d.getDeeds().remove(idx);
                pendingBankAuctions.add(idx);
            }
            log(d.getName() + "'s deeds return to the bank for auction.");
        }
        d.getDeeds().clear();

        boolean wasCurrent = (d == current());
        if (wasCurrent) {
            // the bankrupt player's turn is over; remember to advance afterwards
            doublesCount = 0;
            pendingExtraRoll = false;
            advanceToNextActive();
            resumeTurnAfterAuction = true;
        }

        if (creditor == null && !pendingBankAuctions.isEmpty()) {
            // run the bank auctions; turn resumes once they complete
            fireStateChanged();
            startAuction(pendingBankAuctions.removeFirst(), true);
            return;
        }

        if (checkGameOver()) return;
        if (wasCurrent) {
            resumeTurnAfterAuction = false;
            phase = TurnPhase.AWAITING_ROLL;
            log("--- " + current().getName() + "'s turn ---");
        }
        fireStateChanged();
    }

    private int buildingRefund(int idx) {
        StreetSpace st = (StreetSpace) board.spaceAt(idx);
        int half = st.getHouseCost() / 2;
        if (level[idx] == 5) return half * 5;
        return half * level[idx];
    }

    private void returnBuildings(int idx) {
        if (level[idx] == 5) {
            hotelSupply += 1;
        } else {
            houseSupply += level[idx];
        }
        level[idx] = 0;
    }

    private int netWorth(Player p) {
        int worth = p.getCash();
        for (Integer idx : p.getDeeds()) {
            OwnableSpace os = (OwnableSpace) board.spaceAt(idx);
            if (!mortgaged[idx]) worth += os.getMortgageValue();
            if (level[idx] > 0) worth += buildingRefund(idx);
        }
        return worth;
    }

    private void autoLiquidate(Player p, int target) {
        // sell buildings first (highest level streets), then mortgage
        boolean progress = true;
        while (p.getCash() < target && progress) {
            progress = false;
            for (Integer idx : p.getDeeds()) {
                if (p.getCash() >= target) break;
                if (level[idx] > 0 && level[idx] == maxGroupLevel(((StreetSpace) board.spaceAt(idx)).getColorGroup())) {
                    int refund = ((StreetSpace) board.spaceAt(idx)).getHouseCost() / 2;
                    returnBuildings(idx); // crude: drops the whole stack (non-interactive fallback only)
                    p.addCash(refund);
                    progress = true;
                }
            }
            for (Integer idx : p.getDeeds()) {
                if (p.getCash() >= target) break;
                if (!mortgaged[idx] && level[idx] == 0) {
                    OwnableSpace os = (OwnableSpace) board.spaceAt(idx);
                    mortgaged[idx] = true;
                    p.addCash(os.getMortgageValue());
                    progress = true;
                }
            }
        }
    }

    // ------------------------------------------------------------ helpers

    private boolean checkGameOver() {
        List<Player> active = activePlayers();
        if (active.size() <= 1 && !gameOver) {
            gameOver = true;
            winner = active.isEmpty() ? null : active.get(0);
            phase = TurnPhase.GAME_OVER;
            if (winner != null) log("GAME OVER — " + winner.getName() + " wins!");
            fireStateChanged();
            fireGameOver(winner);
            return true;
        }
        return gameOver;
    }

    private void sendToJail(Player p) {
        p.setPosition(JAIL_INDEX);
        p.setInJail(true);
        p.setJailTurns(0);
        log(p.getName() + " is sent to Jail.");
    }

    private void setOwner(int idx, Player p) {
        Player prev = owner[idx];
        if (prev != null) prev.getDeeds().remove(idx);
        owner[idx] = p;
        if (p != null) p.getDeeds().add(idx);
    }

    private Player ownerForAction(int idx) {
        Player p = owner[idx];
        if (p == null) throw new IllegalStateException("Unowned property");
        return p;
    }

    private boolean ownsWholeGroup(Player p, ColorGroup g) {
        if (p == null) return false;
        for (StreetSpace st : board.streetsInGroup(g)) {
            if (owner[st.getIndex()] != p) return false;
        }
        return true;
    }

    private boolean groupHasMortgage(Player p, ColorGroup g) {
        for (StreetSpace st : board.streetsInGroup(g)) {
            if (mortgaged[st.getIndex()]) return true;
        }
        return false;
    }

    private boolean groupHasBuildings(ColorGroup g) {
        for (StreetSpace st : board.streetsInGroup(g)) {
            if (level[st.getIndex()] > 0) return true;
        }
        return false;
    }

    private int minGroupLevel(ColorGroup g) {
        int min = Integer.MAX_VALUE;
        for (StreetSpace st : board.streetsInGroup(g)) {
            min = Math.min(min, level[st.getIndex()]);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private int maxGroupLevel(ColorGroup g) {
        int max = 0;
        for (StreetSpace st : board.streetsInGroup(g)) {
            max = Math.max(max, level[st.getIndex()]);
        }
        return max;
    }

    private int countRailroads(Player p) {
        int n = 0;
        for (RailroadSpace r : board.railroads()) {
            if (owner[r.getIndex()] == p) n++;
        }
        return n;
    }

    private int countUtilities(Player p) {
        int n = 0;
        for (UtilitySpace u : board.utilities()) {
            if (owner[u.getIndex()] == p) n++;
        }
        return n;
    }

    private Player current() { return players.get(currentIndex); }

    private int indexOf(Player p) { return players.indexOf(p); }

    private List<Player> activePlayers() {
        List<Player> out = new ArrayList<Player>();
        for (Player p : players) if (!p.isBankrupt()) out.add(p);
        return out;
    }

    private DeckState deckFor(Deck d) { return d == Deck.CHANCE ? chance : community; }

    // ------------------------------------------------------------ inner types

    private static final class DeckState {
        private final LinkedList<Card> draw = new LinkedList<Card>();
        private final LinkedList<Card> discardPile = new LinkedList<Card>();
        private final Random rng;

        DeckState(List<Card> cards, Random rng) {
            this.rng = rng;
            if (cards != null) draw.addAll(cards);
            Collections.shuffle(draw, rng);
        }

        Card draw() {
            if (draw.isEmpty()) {
                draw.addAll(discardPile);
                discardPile.clear();
                Collections.shuffle(draw, rng);
            }
            Card c = draw.removeFirst();
            if (c.getAction() != CardAction.GET_OUT_OF_JAIL_FREE) {
                discardPile.addLast(c);
            }
            return c;
        }

        void discard(Card c) { discardPile.addLast(c); }
    }

    /** Live read-only view backed directly by the engine fields. */
    private final class StateView implements GameState {
        public Board getBoard() { return board; }
        public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
        public List<Player> getActivePlayers() { return activePlayers(); }
        public Player getCurrentPlayer() { return current(); }
        public Holding holdingAt(int spaceIndex) {
            Space s = board.spaceAt(spaceIndex);
            if (!(s instanceof OwnableSpace)) return null;
            return new HoldingView(spaceIndex);
        }
        public TurnPhase getPhase() { return phase; }
        public boolean isGameOver() { return gameOver; }
        public Player getWinner() { return winner; }
        public int[] getLastDice() { return new int[]{lastDice[0], lastDice[1]}; }
        public OwnableSpace getAuctionSpace() { return auctionSpace; }
        public int getAuctionHighBid() { return auctionHighBid; }
        public Player getAuctionHighBidder() { return auctionHighBidder; }
    }

    private final class HoldingView implements Holding {
        private final int idx;
        HoldingView(int idx) { this.idx = idx; }
        public OwnableSpace getSpace() { return (OwnableSpace) board.spaceAt(idx); }
        public Player getOwner() { return owner[idx]; }
        public boolean isMortgaged() { return mortgaged[idx]; }
        public int getHouseCount() { return level[idx] >= 5 ? 0 : level[idx]; }
        public boolean hasHotel() { return level[idx] >= 5; }
    }
}
