package com.tiwa.mahjong.setup;

import com.tiwa.mahjong.api.Wind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Result of the opening dice roll. Each of the four players rolls three dice; the highest roll
 * takes East (the dealer), then South, West, North in descending order of roll. Ties break by the
 * original roll index so the outcome is deterministic.
 *
 * <p>The winning (East) roll also picks the wall break: counting counter-clockwise from East, the
 * break offset is {@code eastRoll.total() mod 4}. With no dead wall this is informational, but it
 * is exposed for completeness and for the (Task 2) loop.</p>
 */
public final class Seating {

    private final List<DiceRoll> rollsBySeat;
    private final int breakSeatOffset;

    private Seating(List<DiceRoll> rollsBySeat, int breakSeatOffset) {
        this.rollsBySeat = Collections.unmodifiableList(rollsBySeat);
        this.breakSeatOffset = breakSeatOffset;
    }

    /** Rolls four players' dice and orders them into seats (highest roll = East/seat 0). */
    public static Seating determine(Random random) {
        List<DiceRoll> raw = new ArrayList<DiceRoll>(4);
        for (int i = 0; i < 4; i++) {
            raw.add(DiceRoll.roll(random));
        }

        List<Integer> order = new ArrayList<Integer>();
        for (int i = 0; i < 4; i++) {
            order.add(i);
        }
        // Highest total first; ties break by original index ascending for determinism.
        order.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                int byTotal = raw.get(b).total() - raw.get(a).total();
                if (byTotal != 0) {
                    return byTotal;
                }
                return a - b;
            }
        });

        List<DiceRoll> bySeat = new ArrayList<DiceRoll>(4);
        for (int seat = 0; seat < 4; seat++) {
            bySeat.add(raw.get(order.get(seat)));
        }
        int offset = bySeat.get(0).total() % 4;
        return new Seating(bySeat, offset);
    }

    /** The dice roll assigned to a seat (seat 0 = East holds the highest roll). */
    public DiceRoll rollForSeat(int seatIndex) {
        return rollsBySeat.get(seatIndex);
    }

    /** Rolls ordered by seat (index 0 = East). Non-increasing by total. */
    public List<DiceRoll> rollsBySeat() {
        return rollsBySeat;
    }

    /** Seat wind for a seat index: East, South, West, North for 0..3. */
    public Wind windForSeat(int seatIndex) {
        return Wind.values()[seatIndex];
    }

    /** Counter-clockwise break offset (0..3) from East, derived from the East roll total. */
    public int getBreakSeatOffset() {
        return breakSeatOffset;
    }
}
