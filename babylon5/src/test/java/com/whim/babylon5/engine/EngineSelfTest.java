package com.whim.babylon5.engine;

import java.util.ArrayList;
import java.util.List;

import com.whim.babylon5.domain.Card;
import com.whim.babylon5.domain.CardType;
import com.whim.babylon5.domain.ConflictType;
import com.whim.babylon5.domain.FactionId;
import com.whim.babylon5.domain.GameState;
import com.whim.babylon5.domain.Phase;
import com.whim.babylon5.domain.PlayerState;
import com.whim.babylon5.domain.ZoneType;

/**
 * Standalone, dependency-free smoke test for the Babylon 5 engine. Run its {@code main} to print
 * PASS/FAIL lines for the rule-fidelity behaviours called out by the contract: the strict
 * support&gt;opposition conflict boundary, phase progression, influence spend on sponsoring, and the
 * Standard Victory condition. Also exercises neutralization fallout and the AI sponsor decision.
 *
 * <p>Intentionally a plain {@code main} (no JUnit) so the app builds and self-verifies with the JDK
 * alone, mirroring {@code startrek/.../EngineSelfTest.java}.
 */
public class EngineSelfTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("== Babylon5 EngineSelfTest ==");
        testStrictExceedBoundary();
        testNeutralizationFallout();
        testPhaseProgression();
        testInfluenceSpendOnSponsor();
        testVictoryCondition();
        testAiSponsorDecision();
        System.out.println("----------------------------");
        System.out.println("PASSED: " + passed + "  FAILED: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---- Core rule: modified support must STRICTLY exceed modified opposition --------------

    private static void testStrictExceedBoundary() {
        GameEngine engine = new GameEngine(newGame(2, 0));

        // Equal totals -> initiator LOSES (tie is not a strict exceed).
        Conflict tie = new Conflict(0, ConflictType.DIPLOMACY, 1);
        tie.getSupport().add(card("s1", "Sup", CardType.CHARACTER, FactionId.HUMAN, 0, 3, 0, 0, 0));
        tie.getOpposition().add(card("o1", "Opp", CardType.CHARACTER, FactionId.MINBARI, 0, 3, 0, 0, 0));
        ConflictResult tieRes = engine.resolveConflict(tie);
        check("tie (3 vs 3) -> initiator does NOT win", !tieRes.initiatorWon());
        check("tie totals reported correctly", tieRes.supportTotal() == 3 && tieRes.oppositionTotal() == 3);

        // One more support point -> initiator WINS.
        Conflict win = new Conflict(0, ConflictType.DIPLOMACY, 1);
        win.getSupport().add(card("s2", "Sup", CardType.CHARACTER, FactionId.HUMAN, 0, 4, 0, 0, 0));
        win.getOpposition().add(card("o2", "Opp", CardType.CHARACTER, FactionId.MINBARI, 0, 3, 0, 0, 0));
        ConflictResult winRes = engine.resolveConflict(win);
        check("4 vs 3 -> initiator wins (strict exceed)", winRes.initiatorWon());

        // Wrong-discipline cards contribute nothing (a PSI-only card in a DIPLOMACY conflict).
        Conflict mismatch = new Conflict(0, ConflictType.DIPLOMACY, 1);
        mismatch.getSupport().add(card("s3", "Telepath", CardType.CHARACTER, FactionId.PSI_CORPS, 0, 0, 0, 5, 0));
        ConflictResult mmRes = engine.resolveConflict(mismatch);
        check("psi card adds 0 to a diplomacy conflict", mmRes.supportTotal() == 0 && !mmRes.initiatorWon());
    }

    private static void testNeutralizationFallout() {
        GameEngine engine = new GameEngine(newGame(2, 0));
        // Big support beats a lone defender by a wide margin; the defender's only ability is 2,
        // so >=2 fallout damage neutralizes it.
        Conflict c = new Conflict(0, ConflictType.MILITARY, 1);
        c.getSupport().add(card("fleetA", "Armada", CardType.CHARACTER, FactionId.NARN, 0, 0, 0, 0, 9));
        Card defender = card("fleetB", "Picket", CardType.CHARACTER, FactionId.CENTAURI, 0, 0, 0, 0, 2);
        c.getOpposition().add(defender);
        ConflictResult res = engine.resolveConflict(c);
        check("lopsided military conflict is won", res.initiatorWon());
        check("out-classed defender is neutralized", res.neutralized().contains(defender));
        check("neutralized card carries damage >= its highest ability", defender.getDamage() >= 2);
    }

    // ---- Phase progression: READY->CONFLICT->ACTION->RESOLUTION->DRAW, then next player ----

    private static void testPhaseProgression() {
        GameState s = newGame(2, 0);
        s.setActiveIndex(0);
        s.setPhase(Phase.READY);
        GameEngine engine = new GameEngine(s);
        int startTurn = s.getTurn();

        engine.advancePhase();
        check("READY -> CONFLICT", s.getPhase() == Phase.CONFLICT && s.getActiveIndex() == 0);
        engine.advancePhase();
        check("CONFLICT -> ACTION", s.getPhase() == Phase.ACTION);
        engine.advancePhase();
        check("ACTION -> RESOLUTION", s.getPhase() == Phase.RESOLUTION);
        engine.advancePhase();
        check("RESOLUTION -> DRAW", s.getPhase() == Phase.DRAW);
        engine.advancePhase();
        check("DRAW -> next player's READY", s.getPhase() == Phase.READY && s.getActiveIndex() == 1);

        // Cycle back to player 0 should bump the turn counter.
        for (int i = 0; i < 5; i++) {
            engine.advancePhase();
        }
        check("wrapping back to player 0 increments the turn", s.getActiveIndex() == 0 && s.getTurn() == startTurn + 1);

        // READY restores the spendable pool to the full Influence Rating.
        PlayerState p0 = s.getPlayers().get(0);
        p0.setInfluenceRating(7);
        p0.setInfluencePool(0);
        engine.advancePhase(); // READY -> CONFLICT applies the Ready round restore
        check("Ready round restores influence pool to the rating", p0.getInfluencePool() == 7);
    }

    // ---- Influence spend: sponsoring pays from the pool and moves the card into play ------

    private static void testInfluenceSpendOnSponsor() {
        GameState s = newGame(2, 0);
        GameEngine engine = new GameEngine(s);
        PlayerState me = s.getPlayers().get(0);
        s.setActiveIndex(0);
        s.setPhase(Phase.ACTION);
        me.setInfluencePool(5);

        Card recruit = card("rec", "Talia", CardType.CHARACTER, FactionId.HUMAN, 3, 2, 1, 0, 0);
        me.zone(ZoneType.HAND).add(recruit);

        me.getAmbassador().setReady(true); // a ready Inner Circle char must exist to rotate as sponsor
        boolean ok = engine.sponsorCharacter(0, recruit);
        check("sponsor succeeds when affordable & legal", ok);
        check("influence pool debited by the printed cost", me.getInfluencePool() == 2);
        check("sponsored character left hand", !me.zone(ZoneType.HAND).getCards().contains(recruit));
        check("sponsored character entered SUPPORTING", me.zone(ZoneType.SUPPORTING).getCards().contains(recruit));

        // Too expensive now -> rejected, no further spend.
        Card pricey = card("big", "Bester", CardType.CHARACTER, FactionId.HUMAN, 9, 0, 0, 6, 0);
        me.zone(ZoneType.HAND).add(pricey);
        me.getAmbassador().setReady(true); // isolate the rejection cause to cost, not a missing sponsor
        boolean rejected = !engine.sponsorCharacter(0, pricey);
        check("sponsor rejected when pool is insufficient", rejected);
        check("pool unchanged after a rejected sponsor", me.getInfluencePool() == 2);

        // Wrong phase -> rejected.
        s.setPhase(Phase.READY);
        Card later = card("late", "Lyta", CardType.CHARACTER, FactionId.HUMAN, 1, 0, 0, 3, 0);
        me.zone(ZoneType.HAND).add(later);
        check("sponsor rejected outside the Action round", !engine.sponsorCharacter(0, later));

        // Different-race character costs double.
        s.setPhase(Phase.ACTION);
        me.setInfluencePool(5);
        Card foreign = card("for", "Lennier", CardType.CHARACTER, FactionId.MINBARI, 3, 4, 0, 0, 0);
        me.zone(ZoneType.HAND).add(foreign);
        check("loyalty doubling makes cost-3 foreign char cost 6 (rejected at pool 5)",
                !engine.sponsorCharacter(0, foreign));
        me.setInfluencePool(6);
        me.getAmbassador().setReady(true); // re-ready the sponsor (rotated by the earlier sponsor)
        check("foreign char affordable at pool 6", engine.sponsorCharacter(0, foreign));
        check("foreign sponsor debits doubled cost", me.getInfluencePool() == 0);
    }

    // ---- Victory: >= 20 Power AND strictly more than every other player -------------------

    private static void testVictoryCondition() {
        GameState s = newGame(3, 0);
        GameEngine engine = new GameEngine(s);
        s.getPlayers().get(0).setInfluenceRating(20);
        s.getPlayers().get(1).setInfluenceRating(15);
        s.getPlayers().get(2).setInfluenceRating(10);
        check("base Power equals influence rating", engine.computePower(s.getPlayers().get(0)) == 20);
        check("clear leader at 20 wins", engine.checkVictory() == s.getPlayers().get(0));

        // Below threshold -> no winner even if strictly highest.
        s.getPlayers().get(0).setInfluenceRating(19);
        check("19 Power does not win", engine.checkVictory() == null);

        // Tie at the top (both at 20) -> no Standard Victory.
        s.getPlayers().get(0).setInfluenceRating(20);
        s.getPlayers().get(1).setInfluenceRating(20);
        check("tie at 20 yields no winner (not strictly highest)", engine.checkVictory() == null);
    }

    private static void testAiSponsorDecision() {
        GameState s = newGame(2, 0);
        PlayerState ai = s.getPlayers().get(1);
        ai.setInfluencePool(6);
        // Two affordable candidates; the high-ability one is the value pick.
        Card weak = card("w", "Aide", CardType.CHARACTER, FactionId.MINBARI, 2, 1, 0, 0, 0);
        Card strong = card("str", "Warleader", CardType.CHARACTER, FactionId.MINBARI, 3, 5, 2, 0, 0);
        ai.zone(ZoneType.HAND).add(weak);
        ai.zone(ZoneType.HAND).add(strong);

        AIPlayer medium = new AIPlayer(AiDifficulty.MEDIUM);
        Card pick = medium.chooseCharacterToSponsor(s, 1);
        check("MEDIUM AI sponsors the higher value-per-influence character", pick == strong);

        AIPlayer easy = new AIPlayer(AiDifficulty.EASY);
        Card anyPick = easy.chooseCharacterToSponsor(s, 1);
        check("EASY AI picks some affordable character", anyPick == weak || anyPick == strong);

        // Nothing affordable -> decline (null).
        ai.setInfluencePool(0);
        check("AI declines when nothing is affordable", medium.chooseCharacterToSponsor(s, 1) == null);
    }

    // ---------------------------------------------------------------- helpers

    private static Card card(String id, String name, CardType type, FactionId faction,
                            int cost, int dip, int intr, int psi, int mil) {
        return new Card(id, name, type, faction, cost, 0, dip, intr, psi, mil, "", null);
    }

    /**
     * Build a bare {@link GameState} with {@code n} players, each holding a ready Ambassador in
     * the Inner Circle (so sponsoring has a card to rotate) and a starting influence rating of 4.
     */
    private static GameState newGame(int n, long seed) {
        List<PlayerState> players = new ArrayList<PlayerState>();
        FactionId[] f = { FactionId.HUMAN, FactionId.MINBARI, FactionId.CENTAURI, FactionId.NARN };
        for (int i = 0; i < n; i++) {
            PlayerState p = new PlayerState("P" + i, f[i % f.length], i == 0);
            p.setInfluenceRating(4);
            p.setInfluencePool(4);
            Card amb = new Card("amb" + i, "Ambassador" + i, CardType.AMBASSADOR, f[i % f.length],
                    0, 4, 3, 2, 1, 0, "", null);
            amb.setReady(true);
            p.zone(ZoneType.INNER_CIRCLE).add(amb);
            players.add(p);
        }
        return new GameState(players, seed);
    }

    private static void check(String label, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("PASS: " + label);
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }
}
