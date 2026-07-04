package com.whim.swd6.api;

/**
 * Immutable Star Wars D6 dice code, e.g. {@code 3D+2}.
 *
 * A dice code is a number of six-sided dice plus a pip modifier (0..2).
 * The system's fundamental identity is <b>3 pips = 1 die</b>, so all
 * arithmetic normalizes pips into whole dice. A "pip value" (dice*3 + pips)
 * gives a single comparable integer used for ordering and cost math.
 *
 * Owned by the orchestrator (api). Do not modify in child tasks.
 */
public final class DiceCode implements Comparable<DiceCode> {

    public static final DiceCode ZERO = new DiceCode(0, 0);

    private final int dice;   // number of D6, always >= 0
    private final int pips;   // 0, 1, or 2 after normalization

    private DiceCode(int dice, int pips) {
        this.dice = dice;
        this.pips = pips;
    }

    /** Build from a total number of pips (dice*3 + pips). Negatives clamp to ZERO. */
    public static DiceCode ofPips(int totalPips) {
        if (totalPips < 0) {
            totalPips = 0;
        }
        return new DiceCode(totalPips / 3, totalPips % 3);
    }

    /** Build from dice and pips; pips may be any value and will be normalized. */
    public static DiceCode of(int dice, int pips) {
        return ofPips(dice * 3 + pips);
    }

    /**
     * Parse a canonical dice code such as "3D", "3D+1", "3D+2" or "0D".
     * Whitespace tolerant and case-insensitive on the 'D'. Throws
     * IllegalArgumentException on malformed input.
     */
    public static DiceCode parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("dice code is null");
        }
        String s = text.trim().toUpperCase();
        int dIndex = s.indexOf('D');
        if (dIndex < 0) {
            throw new IllegalArgumentException("missing 'D' in dice code: " + text);
        }
        String dicePart = s.substring(0, dIndex).trim();
        String pipPart = s.substring(dIndex + 1).trim();
        int d;
        try {
            d = dicePart.isEmpty() ? 0 : Integer.parseInt(dicePart);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("bad dice count in: " + text);
        }
        int p = 0;
        if (!pipPart.isEmpty()) {
            // Accept "+2", "2", or "+0".
            if (pipPart.charAt(0) == '+') {
                pipPart = pipPart.substring(1).trim();
            }
            if (!pipPart.isEmpty()) {
                try {
                    p = Integer.parseInt(pipPart);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("bad pip modifier in: " + text);
                }
            }
        }
        return of(d, p);
    }

    public int getDice() {
        return dice;
    }

    public int getPips() {
        return pips;
    }

    /** Single comparable magnitude: dice*3 + pips. */
    public int pipValue() {
        return dice * 3 + pips;
    }

    /** Add whole dice (may be negative; result clamps at ZERO). */
    public DiceCode addDice(int d) {
        return ofPips(pipValue() + d * 3);
    }

    /** Add pips (may be negative; result clamps at ZERO). */
    public DiceCode addPips(int p) {
        return ofPips(pipValue() + p);
    }

    /** Sum two dice codes. */
    public DiceCode add(DiceCode other) {
        return ofPips(pipValue() + other.pipValue());
    }

    /** Subtract another code (clamps at ZERO). */
    public DiceCode subtract(DiceCode other) {
        return ofPips(pipValue() - other.pipValue());
    }

    /** Double every die and pip (used for Force Point spending). */
    public DiceCode doubled() {
        return ofPips(pipValue() * 2);
    }

    @Override
    public int compareTo(DiceCode o) {
        return Integer.compare(pipValue(), o.pipValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DiceCode)) {
            return false;
        }
        DiceCode other = (DiceCode) o;
        return dice == other.dice && pips == other.pips;
    }

    @Override
    public int hashCode() {
        return pipValue();
    }

    /** Canonical string: "3D" when pips==0, otherwise "3D+2". */
    @Override
    public String toString() {
        if (pips == 0) {
            return dice + "D";
        }
        return dice + "D+" + pips;
    }
}
