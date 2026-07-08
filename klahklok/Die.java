package klahklok;

import java.util.Random;

/**
 * A single Klah Klok die that produces a uniformly random {@link Symbol}.
 */
public class Die {

    private final Random random;

    /** Creates a die with its own internal random source. */
    public Die() {
        this(new Random());
    }

    /**
     * Creates a die using the supplied random source.
     *
     * @param random the random source to draw rolls from
     */
    public Die(Random random) {
        this.random = random;
    }

    /**
     * Rolls the die.
     *
     * @return a uniformly chosen symbol (each with probability 1/6)
     */
    public Symbol roll() {
        return Symbol.values()[random.nextInt(6)];
    }
}
