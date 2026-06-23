package com.spacemines;

import java.util.Random;

/**
 * GameEngine — the Model logic for the Java 8 Swing port of the C64 BASIC
 * game "Space Mines".
 *
 * ALL game math lives here; the UI (Task 3) and the domain value classes
 * (Task 1) hold no game logic. This class advances the colony simulation
 * exactly one year per {@link #processYear(PlayerActions)} call, faithfully
 * reproducing the original turn-resolution order and the legacy INT()
 * truncation behaviour (integer division / casts to int truncate toward zero
 * for the non-negative quantities used here, matching BASIC's INT(RND*X+Y)).
 *
 * Domain contract (owned by Task 1, assumed to exist exactly):
 *   ColonyState   : public int year,population,money,storedOre,mines,
 *                   satisfaction,foodPrice,orePerMine; ColonyState copy();
 *   GameConstants : TOTAL_YEARS=10, MIN_PEOPLE_PER_MINE=10,
 *                   SATISFACTION_REVOLT_THRESHOLD, INITIAL_* fields, MINE_COST;
 *                   static ColonyState newGame();
 *   RandomEvents  : RandomEvents(Random rng); int rnd(int range,int base);
 *                   int nextFoodPrice(); int nextOrePerMine();
 *   PlayerActions : public int minesToBuy, oreToSell, foodToBuy;
 *   TurnResult    : public String narrative; public boolean gameOver;
 *                   public String gameOverReason;
 */
public class GameEngine {

    private final ColonyState state;
    private final RandomEvents events;

    private boolean gameOver;
    private boolean victory;
    private String gameOverReason;

    public GameEngine(ColonyState state, Random rng) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        if (rng == null) {
            throw new IllegalArgumentException("rng must not be null");
        }
        this.state = state;
        this.events = new RandomEvents(rng);
        this.gameOver = false;
        this.victory = false;
        this.gameOverReason = null;
    }

    /** Live colony state held by this engine. */
    public ColonyState getState() {
        return state;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /** True only once the colony has survived all TOTAL_YEARS without a game over. */
    public boolean isVictory() {
        return victory;
    }

    /**
     * Advance the simulation exactly one year (the original's V = V + 1).
     *
     * Resolution order (faithful to the original mechanics):
     *   1. Apply player actions (buy mines, sell ore, buy food), clamping
     *      anything unaffordable and noting the clamp in the narrative.
     *   2. Mine production.
     *   3. Food consumption and the resulting satisfaction swing.
     *   4. Market: roll next food price.
     *   5. Population dynamics driven by satisfaction.
     *   6. Critical triggers (revolt / depopulation) that end the game.
     *   7. year++ ; a year past TOTAL_YEARS with no game over is victory.
     */
    public TurnResult processYear(PlayerActions actions) {
        TurnResult result = new TurnResult();

        if (gameOver) {
            // Defensive: never advance a finished game.
            result.gameOver = true;
            result.gameOverReason = gameOverReason;
            result.narrative = "The game is already over: " + gameOverReason;
            return result;
        }

        PlayerActions act = (actions == null) ? new PlayerActions() : actions;
        StringBuilder log = new StringBuilder();
        log.append("Year ").append(state.year).append(":\n");

        // --- 1. Player actions ------------------------------------------------

        // Buy mines.
        int wantMines = Math.max(0, act.minesToBuy);
        if (wantMines > 0) {
            int affordable = (GameConstants.MINE_COST > 0)
                    ? state.money / GameConstants.MINE_COST
                    : wantMines;
            int buy = Math.min(wantMines, Math.max(0, affordable));
            if (buy < wantMines) {
                log.append("You could only afford ").append(buy).append(" of ")
                   .append(wantMines).append(" mines.\n");
            }
            state.money -= buy * GameConstants.MINE_COST;
            state.mines += buy;
            if (buy > 0) {
                log.append("Bought ").append(buy).append(" mine(s).\n");
            }
        }

        // Sell ore (priced at the current food price, per the original market).
        int wantSell = Math.max(0, act.oreToSell);
        if (wantSell > 0) {
            int sell = Math.min(wantSell, Math.max(0, state.storedOre));
            if (sell < wantSell) {
                log.append("You only had ").append(sell).append(" ore to sell.\n");
            }
            state.storedOre -= sell;
            state.money += sell * state.foodPrice;
            if (sell > 0) {
                log.append("Sold ").append(sell).append(" ore for ")
                   .append(sell * state.foodPrice).append(" credits.\n");
            }
        }

        // Buy food (also priced at the current food price).
        int wantFood = Math.max(0, act.foodToBuy);
        int foodBought = 0;
        if (wantFood > 0) {
            int affordable = (state.foodPrice > 0)
                    ? state.money / state.foodPrice
                    : wantFood;
            foodBought = Math.min(wantFood, Math.max(0, affordable));
            if (foodBought < wantFood) {
                log.append("You could only afford ").append(foodBought)
                   .append(" of ").append(wantFood).append(" food.\n");
            }
            state.money -= foodBought * state.foodPrice;
            if (foodBought > 0) {
                log.append("Bought ").append(foodBought).append(" food.\n");
            }
        }

        // --- 2. Mine production ----------------------------------------------

        state.orePerMine = events.nextOrePerMine();
        int produced = state.mines * state.orePerMine;
        state.storedOre += produced;
        log.append("Mines produced ").append(produced).append(" ore (")
           .append(state.orePerMine).append("/mine).\n");

        // --- 3. Food consumption & satisfaction ------------------------------

        // Every colonist eats one unit of food per year. Surplus food lifts
        // satisfaction; a shortfall (hunger) drives it down, scaled by the
        // size of the gap relative to the population. INT() truncation applies.
        int need = state.population;
        int balance = foodBought - need;          // >0 surplus, <0 shortfall
        int swing;
        if (balance >= 0) {
            // Diminishing reward for stockpiling: one point per 10% surplus.
            swing = (need > 0) ? (balance * 10) / need : 10;
            if (swing > 20) {
                swing = 20;
            }
        } else {
            // Hunger bites harder: lose points per 10% of the population unfed.
            int shortfall = -balance;
            swing = -((shortfall * 20) / Math.max(1, need));
            if (swing < -50) {
                swing = -50;
            }
        }
        state.satisfaction += swing;
        if (state.satisfaction < 0) {
            state.satisfaction = 0;
        }
        if (state.satisfaction > 100) {
            state.satisfaction = 100;
        }
        if (balance < 0) {
            log.append("Food ran short — the colonists are unhappy.\n");
        } else if (swing > 0) {
            log.append("Bellies are full — morale rises.\n");
        }

        // --- 4. Market -------------------------------------------------------

        state.foodPrice = events.nextFoodPrice();
        log.append("Food now trades at ").append(state.foodPrice)
           .append(" credits.\n");

        // --- 5. Population dynamics ------------------------------------------

        // Content colonies attract/breed new workers; miserable ones bleed
        // people. Growth/loss is a percentage of current population (INT trunc).
        if (state.satisfaction >= 75) {
            int growth = (state.population * (state.satisfaction - 50)) / 100;
            state.population += growth;
            if (growth > 0) {
                log.append(growth).append(" new colonists arrived.\n");
            }
        } else if (state.satisfaction < 40) {
            int loss = (state.population * (50 - state.satisfaction)) / 100;
            state.population -= loss;
            if (state.population < 0) {
                state.population = 0;
            }
            if (loss > 0) {
                log.append(loss).append(" colonists left the colony.\n");
            }
        }

        // --- 6. Critical triggers (game over) --------------------------------

        if (state.satisfaction <= GameConstants.SATISFACTION_REVOLT_THRESHOLD) {
            gameOver = true;
            gameOverReason = "The people revolted!";
        } else if (state.population < state.mines * GameConstants.MIN_PEOPLE_PER_MINE) {
            gameOver = true;
            gameOverReason = "Not enough people to work the mines.";
        }

        // --- 7. Advance the year & check for victory -------------------------

        state.year++;
        if (!gameOver && state.year > GameConstants.TOTAL_YEARS) {
            gameOver = true;
            victory = true;
            gameOverReason = "You guided the colony through all "
                    + GameConstants.TOTAL_YEARS + " years!";
        }

        if (gameOver) {
            log.append("\n").append(gameOverReason).append("\n");
        }

        result.narrative = log.toString();
        result.gameOver = gameOver;
        result.gameOverReason = gameOver ? gameOverReason : null;
        return result;
    }
}
