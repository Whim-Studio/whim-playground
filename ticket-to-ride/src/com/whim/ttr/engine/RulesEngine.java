package com.whim.ttr.engine;

import com.whim.ttr.api.ActionOutcome;
import com.whim.ttr.api.CardColor;
import com.whim.ttr.api.GameConstants;
import com.whim.ttr.api.GameEngine;
import com.whim.ttr.api.GamePhase;
import com.whim.ttr.api.RouteKind;
import com.whim.ttr.domain.Board;
import com.whim.ttr.domain.DestinationTicket;
import com.whim.ttr.domain.Deck;
import com.whim.ttr.domain.GameState;
import com.whim.ttr.domain.Player;
import com.whim.ttr.domain.PlayerScore;
import com.whim.ttr.domain.Route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The rules engine for Ticket to Ride: Europe — the sole implementation of
 * {@link GameEngine}. It owns all validation, the tunnel mechanic, station
 * building, endgame arming and the final DFS ticket / longest-path scoring.
 *
 * <p>All graph work is synchronous; the UI is expected to invoke these methods
 * off the Swing EDT and marshal results back with {@code invokeLater}.</p>
 *
 * <p>Turn model: a legal turn performs exactly one of — draw (up to two train
 * cards), claim a route, build a station, or draw/keep tickets — then ends with
 * {@link #endTurn()}. Phase transitions are managed lazily so the UI does not
 * need a separate "begin play" call: any board action promotes {@code SETUP} to
 * {@code PLAYING}, and reaching the train threshold arms {@code LAST_ROUND}.</p>
 */
public final class RulesEngine implements GameEngine {

    private final GameState state;
    private final Board board;
    private final Deck deck;
    private final int playerCount;

    // ---- per-turn bookkeeping ----------------------------------------------
    private int drawsThisTurn;        // train cards drawn this turn (0..2)
    private boolean primaryActionTaken; // claim / build / ticket action done
    private List<DestinationTicket> offered; // tickets offered, awaiting keep

    // ---- pending tunnel claim ----------------------------------------------
    private PendingTunnel pending;

    // ---- endgame -----------------------------------------------------------
    private int lastRoundTurnsRemaining = -1;

    public RulesEngine(GameState state) {
        this.state = state;
        this.board = state.board();
        this.deck = state.deck();
        this.playerCount = state.players().size();
        if (state.phase() == null) {
            state.setPhase(GamePhase.SETUP);
        }
    }

    // ========================================================================
    //  Read-only accessors
    // ========================================================================

    @Override public GameState state() { return state; }
    @Override public int currentPlayerId() { return state.currentPlayerId(); }
    @Override public GamePhase phase() { return state.phase(); }
    @Override public boolean isGameOver() { return state.phase() == GamePhase.GAME_OVER; }

    private Player current() { return state.player(state.currentPlayerId()); }

    // ========================================================================
    //  Draw train cards
    // ========================================================================

    @Override
    public ActionOutcome drawTrainCard(int faceUpIndex) {
        if (isGameOver()) return fail("The game is over.");
        if (pending != null) return fail("Resolve the pending tunnel first.");
        if (primaryActionTaken) return fail("You already took an action this turn.");
        promoteSetup();
        if (drawsThisTurn >= 2) return fail("You have already drawn two cards.");

        if (faceUpIndex < 0) {
            CardColor c = deck.draw();
            if (c == null) return fail("The deck is empty.");
            current().addCard(c);
            drawsThisTurn++;
            return ok("Drew a train card from the deck.");
        }

        List<CardColor> faceUp = deck.faceUp();
        if (faceUpIndex >= faceUp.size()) return fail("No face-up card in that slot.");
        CardColor picked = faceUp.get(faceUpIndex);
        if (picked == null) return fail("That market slot is empty.");
        if (picked.isLocomotive() && drawsThisTurn == 1) {
            return fail("A face-up locomotive cannot be taken as your second card.");
        }
        // Take the card and let the deck top up the market slot.
        faceUp.remove(faceUpIndex);
        deck.refillFaceUp();
        current().addCard(picked);
        if (picked.isLocomotive()) {
            drawsThisTurn = 2; // a face-up loco counts as the whole draw action
            return ok("Took a face-up locomotive (counts as both draws).");
        }
        drawsThisTurn++;
        return ok("Took a face-up " + picked + " card.");
    }

    // ========================================================================
    //  Claim route (NORMAL / FERRY resolve now; TUNNEL suspends)
    // ========================================================================

    @Override
    public ActionOutcome beginClaimRoute(String routeId, List<CardColor> cards) {
        if (isGameOver()) return fail("The game is over.");
        if (pending != null) return fail("Resolve the pending tunnel first.");
        if (primaryActionTaken || drawsThisTurn > 0) {
            return fail("You have already taken an action this turn.");
        }
        Route route = board.route(routeId);
        if (route == null) return fail("Unknown route: " + routeId);
        if (route.ownerId() != null) return fail("That route is already claimed.");

        Player p = current();
        int len = route.length();
        if (p.trainsLeft() < len) return fail("Not enough trains (" + p.trainsLeft() + "/" + len + ").");

        String dbl = checkDoubleRoute(route, p.id());
        if (dbl != null) return fail(dbl);

        Payment pay = validatePayment(route, cards, p);
        if (pay.error != null) return fail(pay.error);

        if (route.kind() == RouteKind.TUNNEL) {
            return beginTunnel(route, cards, pay, p);
        }

        // NORMAL / FERRY: commit immediately.
        commitRoute(route, cards, p);
        return ok(p.name() + " claimed " + route.cityA() + "–" + route.cityB()
                + " (+" + points(len) + ").");
    }

    /**
     * The Europe double-route rule: a player may never own both halves of a
     * double route, and in 2–3 player games the second half is blocked to
     * everyone once one half is claimed.
     */
    private String checkDoubleRoute(Route route, int pid) {
        List<Route> between = board.routesBetween(route.cityA(), route.cityB());
        if (between == null || between.size() < 2) return null;
        for (Route other : between) {
            if (other.id().equals(route.id())) continue;
            Integer owner = other.ownerId();
            if (owner == null) continue;
            if (owner == pid) {
                return "You already own the parallel route " + route.cityA()
                        + "–" + route.cityB() + ".";
            }
            if (playerCount <= 3) {
                return "In 2–3 player games the parallel route is blocked.";
            }
        }
        return null;
    }

    /**
     * Validate that {@code cards} legally pay for {@code route} from the hand.
     * On success {@link Payment#paidColor} is the single non-wild color used
     * (or {@code null} when the payment is entirely locomotives).
     */
    private Payment validatePayment(Route route, List<CardColor> cards, Player p) {
        Payment r = new Payment();
        if (cards == null || cards.isEmpty()) { r.error = "No cards offered."; return r; }
        if (cards.size() != route.length()) {
            r.error = "Pay exactly " + route.length() + " card(s); you offered " + cards.size() + ".";
            return r;
        }
        if (!handContains(p, cards)) { r.error = "You don't hold those cards."; return r; }

        int locos = 0;
        Set<CardColor> colors = new HashSet<CardColor>();
        for (CardColor c : cards) {
            if (c == null) { r.error = "Null card in payment."; return r; }
            if (c.isLocomotive()) locos++; else colors.add(c);
        }
        if (colors.size() > 1) { r.error = "A route must be paid in a single color (plus wilds)."; return r; }
        CardColor paid = colors.isEmpty() ? null : colors.iterator().next();

        if (route.color() != null && paid != null && paid != route.color()) {
            r.error = "This route needs " + route.color() + " (or wilds), not " + paid + ".";
            return r;
        }
        if (route.kind() == RouteKind.FERRY && locos < route.locomotivesRequired()) {
            r.error = "This ferry needs at least " + route.locomotivesRequired() + " locomotive(s).";
            return r;
        }
        r.paidColor = paid;
        r.locomotives = locos;
        return r;
    }

    private void commitRoute(Route route, List<CardColor> cards, Player p) {
        removeFromHand(p, cards);
        for (CardColor c : cards) deck.discard(c);
        route.setOwner(p.id());
        p.useTrains(route.length());
        p.addScore(points(route.length()));
        primaryActionTaken = true;
        armEndgameIfNeeded(p);
    }

    // ========================================================================
    //  Tunnel mechanic
    // ========================================================================

    private ActionOutcome beginTunnel(Route route, List<CardColor> cards, Payment pay, Player p) {
        // Stage the base payment out of the hand so it can't be reused.
        removeFromHand(p, cards);

        List<CardColor> flipped = new ArrayList<CardColor>();
        for (int i = 0; i < GameConstants.TUNNEL_FLIP; i++) {
            CardColor c = deck.draw();
            if (c == null) break;
            flipped.add(c);
        }
        int extra = 0;
        for (CardColor c : flipped) {
            if (tunnelMatches(c, pay.paidColor)) extra++;
        }
        // The flipped cards are revealed then discarded regardless of outcome.
        for (CardColor c : flipped) deck.discard(c);

        if (extra == 0) {
            // No surcharge: finish the claim right away.
            for (CardColor c : cards) deck.discard(c);
            route.setOwner(p.id());
            p.useTrains(route.length());
            p.addScore(points(route.length()));
            primaryActionTaken = true;
            armEndgameIfNeeded(p);
            return ActionOutcome.of(true, "Tunnel clear — claimed " + route.cityA()
                    + "–" + route.cityB() + " (+" + points(route.length()) + ").");
        }

        pending = new PendingTunnel(route, new ArrayList<CardColor>(cards), pay.paidColor, extra);
        return ActionOutcome.tunnel(flipped, extra);
    }

    /** A flipped card matches if it is a wild or equals the color that was paid. */
    private boolean tunnelMatches(CardColor flipped, CardColor paidColor) {
        if (flipped.isLocomotive()) return true;
        return paidColor != null && flipped == paidColor;
    }

    @Override
    public ActionOutcome confirmTunnel(List<CardColor> extraCards) {
        if (pending == null) return fail("No tunnel is awaiting confirmation.");
        Player p = current();
        PendingTunnel pt = pending;

        if (extraCards == null || extraCards.size() < pt.extra) {
            return cancelTunnel(); // insufficient list cancels per the contract
        }
        // Every surcharge card must itself match, and be held in hand.
        List<CardColor> use = new ArrayList<CardColor>();
        for (CardColor c : extraCards) {
            if (c == null || !tunnelMatches(c, pt.paidColor)) {
                return cancelTunnel();
            }
            use.add(c);
            if (use.size() == pt.extra) break;
        }
        if (!handContains(p, use)) return cancelTunnel();

        // Pay: base cards (already staged out of hand) + the surcharge.
        removeFromHand(p, use);
        for (CardColor c : pt.baseCards) deck.discard(c);
        for (CardColor c : use) deck.discard(c);

        pt.route.setOwner(p.id());
        p.useTrains(pt.route.length());
        p.addScore(points(pt.route.length()));
        pending = null;
        primaryActionTaken = true;
        armEndgameIfNeeded(p);
        return ok(p.name() + " pushed through the tunnel – claimed " + pt.route.cityA()
                + "–" + pt.route.cityB() + " (+" + points(pt.route.length()) + ").");
    }

    @Override
    public ActionOutcome cancelTunnel() {
        if (pending == null) return fail("No tunnel is awaiting confirmation.");
        Player p = current();
        for (CardColor c : pending.baseCards) p.addCard(c); // return staged cards
        Route r = pending.route;
        pending = null;
        // The claim never completed; the turn is still available.
        return ok("Tunnel abandoned at " + r.cityA() + "–" + r.cityB()
                + "; cards returned.");
    }

    // ========================================================================
    //  Tickets
    // ========================================================================

    @Override
    public List<DestinationTicket> offerTickets() {
        if (isGameOver()) return Collections.emptyList();
        if (pending != null || primaryActionTaken || drawsThisTurn > 0) {
            return Collections.emptyList();
        }
        int n = GameConstants.TICKETS_DEALT;
        List<DestinationTicket> out = new ArrayList<DestinationTicket>();
        for (int i = 0; i < n; i++) {
            DestinationTicket t = deck.drawTicket();
            if (t == null) break;
            out.add(t);
        }
        offered = out;
        return new ArrayList<DestinationTicket>(out);
    }

    @Override
    public ActionOutcome keepTickets(List<DestinationTicket> kept) {
        if (offered == null || offered.isEmpty()) return fail("No tickets have been offered.");
        boolean setup = state.phase() == GamePhase.SETUP;
        int minKeep = setup ? GameConstants.START_TICKETS_MIN_KEEP : GameConstants.TICKETS_MIN_KEEP;
        if (kept == null || kept.size() < minKeep) {
            return fail("You must keep at least " + minKeep + " ticket(s).");
        }
        // Kept must be a subset of what was offered.
        List<DestinationTicket> pool = new ArrayList<DestinationTicket>(offered);
        for (DestinationTicket t : kept) {
            if (!pool.remove(t)) return fail("Ticket was not among those offered.");
        }
        Player p = current();
        for (DestinationTicket t : kept) p.addTicket(t);
        offered = null;
        if (!setup) primaryActionTaken = true;
        return ok(p.name() + " kept " + kept.size() + " ticket(s).");
    }

    // ========================================================================
    //  Stations
    // ========================================================================

    @Override
    public ActionOutcome buildStation(String cityId, List<CardColor> cards) {
        if (isGameOver()) return fail("The game is over.");
        if (pending != null) return fail("Resolve the pending tunnel first.");
        if (primaryActionTaken || drawsThisTurn > 0) {
            return fail("You have already taken an action this turn.");
        }
        promoteSetup();
        Player p = current();
        if (p.stationsLeft() <= 0) return fail("No stations left to build.");
        if (board.city(cityId) == null) return fail("Unknown city: " + cityId);
        if (p.stationCities().contains(cityId)) return fail("You already have a station there.");

        int used = GameConstants.STATIONS_PER_PLAYER - p.stationsLeft();
        int cost = GameConstants.STATION_COST[used];

        if (cards == null || cards.size() != cost) {
            return fail("A station here costs " + cost + " card(s) of one color.");
        }
        if (!handContains(p, cards)) return fail("You don't hold those cards.");
        Set<CardColor> colors = new HashSet<CardColor>();
        for (CardColor c : cards) if (!c.isLocomotive()) colors.add(c);
        if (colors.size() > 1) return fail("A station must be paid in a single color (plus wilds).");

        removeFromHand(p, cards);
        for (CardColor c : cards) deck.discard(c);
        p.useStation(cityId);
        primaryActionTaken = true;
        return ok(p.name() + " built a station in " + cityId + " (cost " + cost + ").");
    }

    // ========================================================================
    //  End of turn + endgame
    // ========================================================================

    @Override
    public ActionOutcome endTurn() {
        if (isGameOver()) return fail("The game is over.");
        if (pending != null) return fail("Resolve the pending tunnel before ending your turn.");

        // Count down the final round; when it expires the game is over.
        if (state.phase() == GamePhase.LAST_ROUND) {
            lastRoundTurnsRemaining--;
            if (lastRoundTurnsRemaining <= 0) {
                state.setPhase(GamePhase.GAME_OVER);
                resetTurnState();
                state.setLastMessage("Game over — final scoring.");
                return ok("Game over.");
            }
        }

        int next = (state.currentPlayerId() + 1) % playerCount;
        state.setCurrentPlayerId(next);
        resetTurnState();
        String msg = state.player(next).name() + "'s turn.";
        state.setLastMessage(msg);
        return ok(msg);
    }

    /** Arm the last round the moment a player's supply drops to the threshold. */
    private void armEndgameIfNeeded(Player p) {
        if (state.phase() == GamePhase.LAST_ROUND || state.phase() == GamePhase.GAME_OVER) return;
        if (p.trainsLeft() <= GameConstants.ENDGAME_TRAIN_THRESHOLD) {
            state.setPhase(GamePhase.LAST_ROUND);
            // Each remaining player gets exactly one more turn.
            lastRoundTurnsRemaining = playerCount;
            state.setLastMessage("Final round! " + p.name()
                    + " is down to " + p.trainsLeft() + " trains.");
        }
    }

    private void promoteSetup() {
        if (state.phase() == GamePhase.SETUP) state.setPhase(GamePhase.PLAYING);
    }

    private void resetTurnState() {
        drawsThisTurn = 0;
        primaryActionTaken = false;
        offered = null;
    }

    // ========================================================================
    //  Final scoring
    // ========================================================================

    @Override
    public List<PlayerScore> finalScores() {
        // Longest continuous path per player, to award the European Express.
        int bestLen = -1;
        Map<Integer, Integer> longestByPlayer = new HashMap<Integer, Integer>();
        for (Player p : state.players()) {
            int len = longestPath(playerEdges(p.id()));
            longestByPlayer.put(p.id(), len);
            if (len > bestLen) bestLen = len;
        }

        List<PlayerScore> scores = new ArrayList<PlayerScore>();
        for (Player p : state.players()) {
            int routePts = p.score(); // running claimed-route total
            TicketResult tr = scoreTickets(p);
            int stationBonus = p.stationsLeft() * GameConstants.UNUSED_STATION_BONUS;
            boolean longest = bestLen > 0 && longestByPlayer.get(p.id()) == bestLen;
            scores.add(new PlayerScore(
                    p.id(), routePts, tr.ticketPoints, stationBonus,
                    longest, tr.completed, tr.failed));
        }
        return scores;
    }

    // ---- ticket scoring (DFS with optional per-station borrowed edge) -------

    private TicketResult scoreTickets(Player p) {
        List<Edge> owned = playerEdges(p.id());

        // Candidate borrow edges: one opponent route incident to each station.
        List<List<Edge>> stationOptions = new ArrayList<List<Edge>>();
        for (String city : p.stationCities()) {
            List<Edge> opts = new ArrayList<Edge>();
            opts.add(null); // "borrow nothing" is always allowed
            for (Route r : board.routesFrom(city)) {
                Integer owner = r.ownerId();
                if (owner != null && owner != p.id()) {
                    opts.add(new Edge(r.cityA(), r.cityB(), r.length()));
                }
            }
            stationOptions.add(opts);
        }

        // Enumerate every combination of borrow choices; keep the best delta.
        List<DestinationTicket> tickets = p.tickets();
        int[] pick = new int[stationOptions.size()];
        TicketResult best = null;
        do {
            List<Edge> edges = new ArrayList<Edge>(owned);
            for (int i = 0; i < pick.length; i++) {
                Edge e = stationOptions.get(i).get(pick[i]);
                if (e != null) edges.add(e);
            }
            TicketResult tr = evaluateTickets(tickets, edges);
            if (best == null || tr.ticketPoints > best.ticketPoints) best = tr;
        } while (advance(pick, stationOptions));

        if (best == null) best = evaluateTickets(tickets, owned);
        return best;
    }

    /** Odometer-style increment over the per-station option indices. */
    private boolean advance(int[] pick, List<List<Edge>> options) {
        for (int i = 0; i < pick.length; i++) {
            pick[i]++;
            if (pick[i] < options.get(i).size()) return true;
            pick[i] = 0;
        }
        return false;
    }

    private TicketResult evaluateTickets(List<DestinationTicket> tickets, List<Edge> edges) {
        DisjointSet ds = new DisjointSet();
        for (Edge e : edges) ds.union(e.a, e.b);
        TicketResult tr = new TicketResult();
        for (DestinationTicket t : tickets) {
            String label = t.from() + "→" + t.to() + " (" + t.points() + ")";
            if (ds.connected(t.from(), t.to())) {
                tr.ticketPoints += t.points();
                tr.completed.add(label);
            } else {
                tr.ticketPoints -= t.points();
                tr.failed.add(label);
            }
        }
        return tr;
    }

    // ---- longest continuous path -------------------------------------------

    /** Longest trail (each edge used at most once) over the player's own edges. */
    private int longestPath(List<Edge> edges) {
        if (edges.isEmpty()) return 0;
        Map<String, List<Edge>> adj = new HashMap<String, List<Edge>>();
        Set<String> vertices = new HashSet<String>();
        for (Edge e : edges) {
            adj.computeIfAbsent(e.a, k -> new ArrayList<Edge>()).add(e);
            adj.computeIfAbsent(e.b, k -> new ArrayList<Edge>()).add(e);
            vertices.add(e.a);
            vertices.add(e.b);
        }
        int best = 0;
        Set<Edge> used = new HashSet<Edge>();
        for (String v : vertices) {
            best = Math.max(best, dfsTrail(v, adj, used));
        }
        return best;
    }

    private int dfsTrail(String v, Map<String, List<Edge>> adj, Set<Edge> used) {
        int local = 0;
        for (Edge e : adj.get(v)) {
            if (used.contains(e)) continue;
            used.add(e);
            String other = e.a.equals(v) ? e.b : e.a;
            local = Math.max(local, e.length + dfsTrail(other, adj, used));
            used.remove(e);
        }
        return local;
    }

    private List<Edge> playerEdges(int pid) {
        List<Edge> out = new ArrayList<Edge>();
        for (Route r : board.routes()) {
            Integer owner = r.ownerId();
            if (owner != null && owner == pid) {
                out.add(new Edge(r.cityA(), r.cityB(), r.length()));
            }
        }
        return out;
    }

    // ========================================================================
    //  Hand helpers
    // ========================================================================

    private boolean handContains(Player p, List<CardColor> cards) {
        Map<CardColor, Integer> need = tally(cards);
        Map<CardColor, Integer> have = tally(p.hand());
        for (Map.Entry<CardColor, Integer> e : need.entrySet()) {
            Integer got = have.get(e.getKey());
            if (got == null || got < e.getValue()) return false;
        }
        return true;
    }

    private void removeFromHand(Player p, List<CardColor> cards) {
        Map<CardColor, Integer> byColor = tally(cards);
        for (Map.Entry<CardColor, Integer> e : byColor.entrySet()) {
            p.removeCards(e.getKey(), e.getValue());
        }
    }

    private Map<CardColor, Integer> tally(List<CardColor> cards) {
        Map<CardColor, Integer> m = new EnumMap<CardColor, Integer>(CardColor.class);
        for (CardColor c : cards) {
            if (c == null) continue;
            Integer n = m.get(c);
            m.put(c, n == null ? 1 : n + 1);
        }
        return m;
    }

    private int points(int length) {
        int[] pts = GameConstants.ROUTE_POINTS;
        return length >= 0 && length < pts.length ? pts[length] : 0;
    }

    private ActionOutcome ok(String msg) { state.setLastMessage(msg); return ActionOutcome.of(true, msg); }
    private ActionOutcome fail(String msg) { return ActionOutcome.of(false, msg); }

    // ========================================================================
    //  Small value/holder types
    // ========================================================================

    private static final class Payment {
        String error;
        CardColor paidColor;
        int locomotives;
    }

    private static final class PendingTunnel {
        final Route route;
        final List<CardColor> baseCards;
        final CardColor paidColor;
        final int extra;
        PendingTunnel(Route route, List<CardColor> baseCards, CardColor paidColor, int extra) {
            this.route = route;
            this.baseCards = baseCards;
            this.paidColor = paidColor;
            this.extra = extra;
        }
    }

    /** An undirected weighted edge used by the scoring graphs. */
    private static final class Edge {
        final String a, b;
        final int length;
        Edge(String a, String b, int length) { this.a = a; this.b = b; this.length = length; }
    }

    private static final class TicketResult {
        int ticketPoints;
        final List<String> completed = new ArrayList<String>();
        final List<String> failed = new ArrayList<String>();
    }

    /** Union-find over city ids for ticket connectivity. */
    private static final class DisjointSet {
        private final Map<String, String> parent = new HashMap<String, String>();
        private String find(String x) {
            String r = parent.get(x);
            if (r == null) { parent.put(x, x); return x; }
            while (!r.equals(parent.get(r))) {
                parent.put(r, parent.get(parent.get(r)));
                r = parent.get(r);
            }
            return r;
        }
        void union(String a, String b) {
            String ra = find(a), rb = find(b);
            if (!ra.equals(rb)) parent.put(ra, rb);
        }
        boolean connected(String a, String b) {
            if (!parent.containsKey(a) || !parent.containsKey(b)) return false;
            return find(a).equals(find(b));
        }
    }
}
