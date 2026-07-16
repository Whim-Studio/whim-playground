package com.whim.tippingpoint.engine;

import com.whim.tippingpoint.domain.CardData;
import com.whim.tippingpoint.domain.CitizenCard;
import com.whim.tippingpoint.domain.CitizenType;
import com.whim.tippingpoint.domain.CityTableau;
import com.whim.tippingpoint.domain.DevelopmentCard;
import com.whim.tippingpoint.domain.DevelopmentType;
import com.whim.tippingpoint.domain.GameMode;
import com.whim.tippingpoint.domain.GameState;
import com.whim.tippingpoint.domain.Market;
import com.whim.tippingpoint.domain.Phase;
import com.whim.tippingpoint.domain.Player;
import com.whim.tippingpoint.domain.Rules;
import com.whim.tippingpoint.domain.StatusBoard;
import com.whim.tippingpoint.domain.TimelineTrack;
import com.whim.tippingpoint.domain.WeatherCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Default rule-enforcing engine. Shuffling and AI decisions are driven by a seeded
 * {@link java.util.Random} for full reproducibility.
 */
public final class DefaultGameEngine implements GameEngine {

    /** AI keeps investing in cash-flow developments until its cashFlow reaches this. */
    private static final int AI_CASHFLOW_RAMP_TARGET = 8;

    private final GameState state;
    private final Random rng;

    /** Whether each player (by index) has already received income this Development Phase. */
    private final boolean[] incomeGranted;
    /** Number of completed Development sub-turns in the current Development Phase. */
    private int devTurnsTaken;
    /** Used weather cards, recycled back into the deck when it runs dry. */
    private final List<WeatherCard> weatherDiscard;

    private DefaultGameEngine(GameState state, Random rng) {
        this.state = state;
        this.rng = rng;
        this.incomeGranted = new boolean[state.getPlayers().size()];
        this.weatherDiscard = new ArrayList<WeatherCard>();
    }

    /**
     * Builds a fresh game: seeded shuffled market + weather deck, players with starting cash,
     * timeline at START_YEAR, Development Phase, current player 0, round 1. Grants the first
     * player's income before returning.
     */
    public static GameEngine newGame(List<String> playerNames, List<Boolean> isAi, GameMode mode, long seed) {
        Random rng = new Random(seed);

        List<Player> players = new ArrayList<Player>();
        for (int i = 0; i < playerNames.size(); i++) {
            boolean ai = isAi != null && i < isAi.size() && isAi.get(i) != null && isAi.get(i).booleanValue();
            players.add(new Player(playerNames.get(i), ai));
        }

        List<DevelopmentCard> devDeck = new ArrayList<DevelopmentCard>(CardData.developmentDeck());
        Collections.shuffle(devDeck, rng);
        Market market = new Market(devDeck);
        market.refill();

        TimelineTrack timeline = new TimelineTrack();

        List<WeatherCard> weatherDeck = new ArrayList<WeatherCard>(CardData.weatherDeck());
        Collections.shuffle(weatherDeck, rng);

        GameState state = new GameState(players, market, timeline, weatherDeck, mode);

        DefaultGameEngine engine = new DefaultGameEngine(state, rng);
        engine.enterDevelopmentPhase();
        engine.beginDevelopmentPhase();
        return engine;
    }

    @Override
    public GameState state() {
        return state;
    }

    // ---------------------------------------------------------------------
    // Development Phase
    // ---------------------------------------------------------------------

    /** Resets per-phase bookkeeping and positions the table at the start of a Development Phase. */
    private void enterDevelopmentPhase() {
        state.setPhase(Phase.DEVELOPMENT);
        state.setCurrentPlayerIndex(0);
        devTurnsTaken = 0;
        for (int i = 0; i < incomeGranted.length; i++) {
            incomeGranted[i] = false;
        }
    }

    @Override
    public void beginDevelopmentPhase() {
        if (state.getPhase() != Phase.DEVELOPMENT) {
            return;
        }
        int idx = state.getCurrentPlayerIndex();
        if (idx < 0 || idx >= incomeGranted.length || incomeGranted[idx]) {
            return;
        }
        Player p = state.getPlayers().get(idx);
        p.addCash(p.getBoard().getCashFlow());
        incomeGranted[idx] = true;
    }

    @Override
    public boolean canBuyDevelopment(Player p, int row, int col) {
        if (p == null || state.getPhase() != Phase.DEVELOPMENT || p != state.getCurrentPlayer()) {
            return false;
        }
        if (row < 0 || row >= state.getMarket().rows() || col < 0 || col >= state.getMarket().cols()) {
            return false;
        }
        DevelopmentCard c = state.getMarket().get(row, col);
        return c != null && p.canAfford(c.getCost());
    }

    @Override
    public void buyDevelopment(Player p, int row, int col) {
        if (!canBuyDevelopment(p, row, col)) {
            throw new IllegalStateException("Illegal development purchase");
        }
        DevelopmentCard c = state.getMarket().take(row, col);
        p.spendCash(c.getCost());
        p.getBoard().applyDevelopment(c);
        p.getCity().addDevelopment(c);
        state.getMarket().refill();
    }

    @Override
    public boolean canRecruit(Player p, CitizenType type) {
        return p != null
                && type != null
                && state.getPhase() == Phase.DEVELOPMENT
                && p == state.getCurrentPlayer()
                && p.canAfford(Rules.CITIZEN_COST);
    }

    @Override
    public void recruit(Player p, CitizenType type) {
        if (!canRecruit(p, type)) {
            throw new IllegalStateException("Illegal recruit");
        }
        p.spendCash(Rules.CITIZEN_COST);
        CitizenCard citizen = CardData.newCitizen(type);
        p.getCity().addCitizen(citizen);
        if (type == CitizenType.FARMER) {
            p.getBoard().addFoodProduction(Rules.FARMER_FOOD_YIELD);
        }
    }

    @Override
    public void endDevelopmentTurn() {
        if (state.getPhase() != Phase.DEVELOPMENT) {
            return;
        }
        // Refill any slots emptied during this sub-turn.
        state.getMarket().refill();
        devTurnsTaken++;

        if (devTurnsTaken >= state.getPlayers().size()) {
            // All players have acted: fully refill the market and move to the Weather Phase.
            state.getMarket().refill();
            state.setPhase(Phase.WEATHER);
        } else {
            int next = (state.getCurrentPlayerIndex() + 1) % state.getPlayers().size();
            state.setCurrentPlayerIndex(next);
            beginDevelopmentPhase();
        }
    }

    @Override
    public boolean developmentPhaseComplete() {
        return devTurnsTaken >= state.getPlayers().size();
    }

    // ---------------------------------------------------------------------
    // AI
    // ---------------------------------------------------------------------

    @Override
    public void runAiDevelopmentTurn(Player p) {
        if (p != state.getCurrentPlayer() || state.getPhase() != Phase.DEVELOPMENT) {
            return;
        }
        beginDevelopmentPhase();
        int guard = 0;
        while (guard++ < 256 && aiOneAction(p)) {
            // keep acting until no useful action remains
        }
        endDevelopmentTurn();
    }

    /** Performs a single best AI action; returns false when nothing worthwhile remains. */
    private boolean aiOneAction(Player p) {
        StatusBoard board = p.getBoard();
        CityTableau city = p.getCity();
        int workers = city.workerCount();
        int food = board.getFoodProduction();
        int co2 = board.getCo2();
        int globalCo2 = state.getGlobalCo2();
        boolean canCitizen = p.canAfford(Rules.CITIZEN_COST);

        // 1. Starvation guard: never let workers exceed available food.
        if (workers > food) {
            if (canCitizen) {
                recruit(p, CitizenType.FARMER);
                return true;
            }
            int[] fd = findBestFoodDev(p);
            if (fd != null) {
                buyDevelopment(p, fd[0], fd[1]);
                return true;
            }
            return false;
        }

        boolean preferGreen = co2 >= Rules.RISK_X2_AT - 1
                || globalCo2 >= (Rules.TIPPING_POINT_CO2 * 2) / 3;

        // 2. Cut CO2 when our own risk or the global level is dangerous.
        if (preferGreen) {
            int[] g = findBestGreenDev(p);
            if (g != null) {
                buyDevelopment(p, g[0], g[1]);
                return true;
            }
        }

        // 2b. Ramp cash flow early so the city can actually afford to grow. Industrial
        //     developments give the strongest cash flow but emit CO2 -> this is what
        //     drives the global-CO2 feedback loop into the danger zone over time.
        if (board.getCashFlow() < AI_CASHFLOW_RAMP_TARGET) {
            int[] eco = findBestEconomyDev(p, preferGreen);
            if (eco != null) {
                buyDevelopment(p, eco[0], eco[1]);
                return true;
            }
        }

        // 3. Grow population toward the target while keeping everyone fed.
        if (city.populationCount() < Rules.TARGET_POPULATION && canCitizen) {
            if (food >= workers + 1) {
                recruit(p, CitizenType.WORKER);
                return true;
            }
            // Not enough food headroom for another worker: add a farmer (raises food + population).
            recruit(p, CitizenType.FARMER);
            return true;
        }

        // 4. Otherwise invest in the economy (cash-flow growth), avoiding risky emitters.
        int[] e = findBestEconomyDev(p, preferGreen);
        if (e != null) {
            buyDevelopment(p, e[0], e[1]);
            return true;
        }

        return false;
    }

    private int[] findBestGreenDev(Player p) {
        Market m = state.getMarket();
        int bestR = -1, bestC = -1, bestDelta = 0;
        for (int r = 0; r < m.rows(); r++) {
            for (int c = 0; c < m.cols(); c++) {
                DevelopmentCard card = m.get(r, c);
                if (card == null || !p.canAfford(card.getCost())) {
                    continue;
                }
                if (card.getType() == DevelopmentType.GREEN && card.getCo2Delta() < bestDelta) {
                    bestDelta = card.getCo2Delta();
                    bestR = r;
                    bestC = c;
                }
            }
        }
        return bestR < 0 ? null : new int[] {bestR, bestC};
    }

    private int[] findBestFoodDev(Player p) {
        Market m = state.getMarket();
        int bestR = -1, bestC = -1, bestDelta = 0;
        for (int r = 0; r < m.rows(); r++) {
            for (int c = 0; c < m.cols(); c++) {
                DevelopmentCard card = m.get(r, c);
                if (card == null || !p.canAfford(card.getCost())) {
                    continue;
                }
                if (card.getFoodDelta() > bestDelta) {
                    bestDelta = card.getFoodDelta();
                    bestR = r;
                    bestC = c;
                }
            }
        }
        return bestR < 0 ? null : new int[] {bestR, bestC};
    }

    /** Best affordable economy card scored by cash-flow gain minus a CO2 penalty; skips risky emitters. */
    private int[] findBestEconomyDev(Player p, boolean avoidEmitters) {
        Market m = state.getMarket();
        int bestR = -1, bestC = -1;
        int bestScore = 0;
        for (int r = 0; r < m.rows(); r++) {
            for (int c = 0; c < m.cols(); c++) {
                DevelopmentCard card = m.get(r, c);
                if (card == null || !p.canAfford(card.getCost())) {
                    continue;
                }
                if (avoidEmitters && card.getCo2Delta() > 0) {
                    continue;
                }
                int score = card.getCashFlowDelta() - Math.max(0, card.getCo2Delta());
                if (score > bestScore) {
                    bestScore = score;
                    bestR = r;
                    bestC = c;
                }
            }
        }
        return bestR < 0 ? null : new int[] {bestR, bestC};
    }

    // ---------------------------------------------------------------------
    // Weather Phase
    // ---------------------------------------------------------------------

    @Override
    public WeatherReport resolveWeatherPhase() {
        List<Player> players = state.getPlayers();
        List<String> lines = new ArrayList<String>();

        // 1. Feed citizens: workers starve on shortfall; farmers only if no workers remain.
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            int need = p.getCity().workerCount();
            int supply = p.getBoard().getFoodProduction();
            int shortfall = Math.max(0, need - supply);
            if (shortfall > 0) {
                p.getCity().removeCitizens(shortfall);
                lines.add(p.getName() + " starves: -" + shortfall + " citizen(s) (food "
                        + supply + " < workers " + need + ")");
            }
        }

        // 2. Clear the previous round's revealed weather into the discard pile.
        List<WeatherCard> revealed = state.getRevealedWeather();
        weatherDiscard.addAll(revealed);
        revealed.clear();

        // 3. Reveal count scales with global CO2.
        int globalCo2 = state.getGlobalCo2();
        int count = Math.min(Rules.MAX_WEATHER_CARDS, 1 + globalCo2 / Rules.CO2_PER_EXTRA_CARD);

        // 4. Reveal and resolve each card against every player, scaled by their Risk Factor.
        List<WeatherCard> deck = state.getWeatherDeck();
        for (int i = 0; i < count; i++) {
            WeatherCard w = drawWeather(deck);
            if (w == null) {
                break;
            }
            revealed.add(w);
            lines.add("Weather: [" + w.getSeverity() + "] " + w.getName());
            for (int j = 0; j < players.size(); j++) {
                Player p = players.get(j);
                int mult = p.getBoard().getRiskFactor().multiplier();
                int citLost = w.getCitizensLost() * mult;
                int cashLost = w.getCashLost() * mult;
                int foodLost = w.getFoodProductionLost() * mult;

                if (citLost > 0) {
                    p.getCity().removeCitizens(citLost);
                }
                if (cashLost > 0) {
                    p.spendCash(Math.min(cashLost, p.getCash()));
                }
                if (foodLost > 0) {
                    p.getBoard().loseFoodProduction(foodLost);
                }

                if (citLost > 0 || cashLost > 0 || foodLost > 0) {
                    lines.add("  " + p.getName() + " (x" + mult + "): -" + citLost
                            + " pop, -$" + cashLost + ", -" + foodLost + " food");
                }
            }
        }

        // 5. Advance the timeline and set up the next round's Development Phase.
        state.getTimeline().advance();
        state.incrementRound();
        enterDevelopmentPhase();
        beginDevelopmentPhase();

        return new WeatherReport(revealed, globalCo2, lines);
    }

    /** Draws the top weather card, recycling the discard pile (reshuffled) when the deck is empty. */
    private WeatherCard drawWeather(List<WeatherCard> deck) {
        if (deck.isEmpty()) {
            if (weatherDiscard.isEmpty()) {
                return null;
            }
            deck.addAll(weatherDiscard);
            weatherDiscard.clear();
            Collections.shuffle(deck, rng);
        }
        return deck.remove(deck.size() - 1);
    }

    // ---------------------------------------------------------------------
    // Win / loss
    // ---------------------------------------------------------------------

    @Override
    public WinStatus checkStatus() {
        List<Player> players = state.getPlayers();
        int globalCo2 = state.getGlobalCo2();

        // Collective defeat: crossing the tipping point ends the game for everyone, any mode.
        if (globalCo2 >= Rules.TIPPING_POINT_CO2) {
            return new WinStatus(Outcome.TEAM_LOSS_TIPPING, null,
                    "Tipping point crossed (global CO2 " + globalCo2 + " >= "
                            + Rules.TIPPING_POINT_CO2 + "). Everyone loses.");
        }

        boolean atEnd = state.getTimeline().isAtEnd();

        if (state.getMode() == GameMode.COMPETITIVE) {
            List<Player> reached = new ArrayList<Player>();
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).getPopulation() >= Rules.TARGET_POPULATION) {
                    reached.add(players.get(i));
                }
            }
            if (!reached.isEmpty()) {
                Player w = best(reached);
                return new WinStatus(Outcome.PLAYER_WIN, w,
                        w.getName() + " reached " + Rules.TARGET_POPULATION + " population and wins!");
            }
            if (atEnd) {
                Player w = best(players);
                return new WinStatus(Outcome.PLAYER_WIN, w,
                        "Year " + Rules.END_YEAR + ": " + w.getName()
                                + " wins with the highest population (" + w.getPopulation() + ").");
            }
            return new WinStatus(Outcome.IN_PROGRESS, null, "In progress.");
        }

        // COOPERATIVE: team wins only if everyone reached the target by the end year.
        if (atEnd) {
            boolean allReached = true;
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).getPopulation() < Rules.TARGET_POPULATION) {
                    allReached = false;
                    break;
                }
            }
            if (allReached) {
                return new WinStatus(Outcome.TEAM_WIN, null,
                        "The team wins! Every city reached " + Rules.TARGET_POPULATION
                                + " population by " + Rules.END_YEAR + ".");
            }
            return new WinStatus(Outcome.TEAM_LOSS_TIME, null,
                    "Out of time: not every city reached " + Rules.TARGET_POPULATION
                            + " by " + Rules.END_YEAR + ".");
        }
        return new WinStatus(Outcome.IN_PROGRESS, null, "In progress.");
    }

    /** Highest population, ties broken by most cash, then lowest CO2. */
    private Player best(List<Player> list) {
        Player winner = null;
        for (int i = 0; i < list.size(); i++) {
            Player p = list.get(i);
            if (winner == null || isBetter(p, winner)) {
                winner = p;
            }
        }
        return winner;
    }

    private boolean isBetter(Player a, Player b) {
        if (a.getPopulation() != b.getPopulation()) {
            return a.getPopulation() > b.getPopulation();
        }
        if (a.getCash() != b.getCash()) {
            return a.getCash() > b.getCash();
        }
        return a.getBoard().getCo2() < b.getBoard().getCo2();
    }
}
