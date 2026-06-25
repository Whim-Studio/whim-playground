package com.whim.tarot.engine;

import com.whim.tarot.domain.SpreadType;

import java.util.HashSet;
import java.util.Set;

/**
 * Seeded smoke test for the engine. Not a unit-test framework harness — just a
 * {@code main} that deals every spread and asserts the core invariants, then
 * prints a sample reading so a human can eyeball the prose.
 *
 * <p>Run: {@code java -cp out com.whim.tarot.engine.EngineSmokeTest}
 */
public final class EngineSmokeTest {

    private EngineSmokeTest() {
    }

    public static void main(String[] args) {
        int failures = 0;
        for (SpreadType type : SpreadType.values()) {
            failures += check(type);
        }

        // Determinism: same seed must yield the same first card name.
        TarotEngine a = new TarotEngine();
        TarotEngine b = new TarotEngine();
        a.shuffle(42L);
        b.shuffle(42L);
        Reading ra = a.deal(SpreadType.THREE_CARD);
        Reading rb = b.deal(SpreadType.THREE_CARD);
        String na = ra.getPositionedCards().get(0).getDrawnCard().getCard().getName();
        String nb = rb.getPositionedCards().get(0).getDrawnCard().getCard().getName();
        if (!na.equals(nb)) {
            System.out.println("FAIL: seeded shuffle not deterministic (" + na + " vs " + nb + ")");
            failures++;
        } else {
            System.out.println("PASS: seeded shuffle deterministic -> " + na);
        }

        // Sample output for human inspection.
        TarotEngine engine = new TarotEngine();
        engine.shuffle(7L);
        Reading sample = engine.deal(SpreadType.CELTIC_CROSS);
        System.out.println("\n===== SAMPLE CELTIC CROSS =====\n");
        System.out.println(sample.getSynthesis());

        System.out.println("\n" + (failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED"));
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static int check(SpreadType type) {
        int failures = 0;
        TarotEngine engine = new TarotEngine();
        engine.shuffle(123L);
        Reading reading = engine.deal(type);

        if (reading.getPositionedCards().size() != type.getCardCount()) {
            System.out.println("FAIL: " + type + " card count");
            failures++;
        }

        // Distinct cards.
        Set<Integer> ids = new HashSet<Integer>();
        for (PositionedCard pc : reading.getPositionedCards()) {
            ids.add(pc.getDrawnCard().getCard().getId());
        }
        if (ids.size() != type.getCardCount()) {
            System.out.println("FAIL: " + type + " has duplicate cards");
            failures++;
        }

        // Positions assigned in order.
        for (int i = 0; i < reading.getPositionedCards().size(); i++) {
            if (reading.getPositionedCards().get(i).getPosition().getIndex() != i) {
                System.out.println("FAIL: " + type + " position order at " + i);
                failures++;
            }
        }

        if (reading.getSynthesis() == null || reading.getSynthesis().trim().isEmpty()) {
            System.out.println("FAIL: " + type + " empty synthesis");
            failures++;
        }

        if (failures == 0) {
            System.out.println("PASS: " + type + " (" + type.getCardCount() + " cards, distinct, ordered, synthesized)");
        }
        return failures;
    }
}
