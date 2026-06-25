package com.whim.tarot.engine;

import com.whim.tarot.data.CardRepository;
import com.whim.tarot.data.TarotDeckData;
import com.whim.tarot.domain.Card;
import com.whim.tarot.domain.DefaultDrawnCard;
import com.whim.tarot.domain.DrawnCard;
import com.whim.tarot.domain.Orientation;
import com.whim.tarot.domain.SpreadPosition;
import com.whim.tarot.domain.SpreadType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds and shuffles a 78-card Tarot deck and deals readings.
 *
 * <p>Not thread-safe: a single engine owns mutable deck order. Construct one
 * per worker, or confine access to a single thread.
 */
public final class TarotEngine {

    private final List<Card> deck;
    private final ReadingInterpreter interpreter;
    private Random random;

    /** Builds the deck from a fresh {@link TarotDeckData}. */
    public TarotEngine() {
        this(new TarotDeckData());
    }

    /** Builds the deck from an injectable repository (testing). */
    public TarotEngine(CardRepository repo) {
        if (repo == null) {
            throw new IllegalArgumentException("repo must not be null");
        }
        this.deck = new ArrayList<Card>(repo.getAllCards());
        this.interpreter = new ReadingInterpreter();
        this.random = new Random();
    }

    /** Fisher-Yates shuffle over the 78-card deck using a fresh random source. */
    public void shuffle() {
        this.random = new Random();
        fisherYates(this.random);
    }

    /** Deterministic Fisher-Yates shuffle seeded for reproducible tests. */
    public void shuffle(long seed) {
        this.random = new Random(seed);
        fisherYates(this.random);
    }

    private void fisherYates(Random rnd) {
        for (int i = deck.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            Card tmp = deck.get(i);
            deck.set(i, deck.get(j));
            deck.set(j, tmp);
        }
    }

    /**
     * Deals a reading for the given spread: draws {@code getCardCount()}
     * distinct cards off the top of the (current) deck order, assigns each an
     * independent 50/50 orientation, maps them to the spread's positions in
     * order, interprets, and returns a fully-populated {@link Reading}.
     */
    public Reading deal(SpreadType type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        List<SpreadPosition> positions = type.getPositions();
        int count = type.getCardCount();
        if (deck.size() < count) {
            throw new IllegalStateException("deck too small for spread " + type);
        }

        List<PositionedCard> placed = new ArrayList<PositionedCard>(count);
        for (int i = 0; i < count; i++) {
            Card card = deck.get(i);
            Orientation orientation = random.nextBoolean()
                    ? Orientation.UPRIGHT : Orientation.REVERSED;
            DrawnCard drawn = new DefaultDrawnCard(card, orientation);
            placed.add(new DefaultPositionedCard(positions.get(i), drawn));
        }

        DefaultReading reading = new DefaultReading(type, placed);
        String synthesis = interpreter.interpret(reading);
        return reading.withSynthesis(synthesis);
    }
}
