package com.whim.monopoly.engine;

import com.whim.monopoly.data.Cards;
import com.whim.monopoly.domain.Board;
import com.whim.monopoly.domain.Card;
import com.whim.monopoly.domain.DefaultPlayer;
import com.whim.monopoly.domain.Player;
import com.whim.monopoly.domain.StandardBoard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Deterministic, scripted smoke test for {@link StandardGameEngine}. No test framework:
 * plain {@code System.out} + {@code if} checks printing PASS/FAIL lines.
 *
 * <p>Dice are forced through {@link ScriptedRandom} so every landing is deterministic;
 * deck shuffles still use the same seeded stream (only {@code nextInt(6)} draws are scripted).</p>
 *
 * <p>Run: {@code java -cp out com.whim.monopoly.engine.EngineSmokeTest}</p>
 */
public final class EngineSmokeTest {

    private static int passes = 0;
    private static int failures = 0;
    private static ScriptedRandom rng;

    public static void main(String[] args) {
        Board board = new StandardBoard();
        Player alice = new DefaultPlayer(0, "Alice", Color.RED);
        Player bob = new DefaultPlayer(1, "Bob", Color.BLUE);
        List<Player> players = new ArrayList<Player>();
        players.add(alice);
        players.add(bob);

        rng = new ScriptedRandom(424242L);
        List<Card> chance = Cards.chance();
        List<Card> cc = Cards.communityChest();
        StandardGameEngine eng = new StandardGameEngine(players, board, chance, cc, rng);
        eng.addListener(new SilentListener());
        GameState st = eng.getState();

        System.out.println("=== Monopoly Engine Smoke Test ===");

        // ---- 1. Buy + Pass GO -------------------------------------------------
        // Alice starts at 0; move her behind GO so a 3-step roll wraps past GO.
        alice.setPosition(38);
        queue(1, 2);                                  // sum 3, not doubles -> idx 1 Mediterranean
        eng.rollDice();
        check("pass-GO awards $200", alice.getCash() == 1700);
        check("landed on Mediterranean (idx1)", alice.getPosition() == 1);
        check("phase AWAITING_BUY on unowned", st.getPhase() == TurnPhase.AWAITING_BUY);
        eng.buyProperty();
        check("buy deducts price ($60)", alice.getCash() == 1640);
        check("Alice owns Mediterranean", st.holdingAt(1).getOwner() == alice);
        check("phase AWAITING_END_TURN after buy", st.getPhase() == TurnPhase.AWAITING_END_TURN);
        eng.endTurn();

        // ---- 2. Auction with a winning bid (Bob declines, Alice bids) ---------
        check("current player is Bob", st.getCurrentPlayer() == bob);
        bob.setPosition(5);
        queue(1, 2);                                  // -> idx 8 Vermont (light blue, $100) unowned
        eng.rollDice();
        check("Bob landed on Vermont (idx8)", bob.getPosition() == 8);
        eng.declineProperty();
        check("phase AUCTION after decline", st.getPhase() == TurnPhase.AUCTION);
        eng.placeBid(alice, 50);
        check("auction high bid is $50 by Alice", st.getAuctionHighBid() == 50 && st.getAuctionHighBidder() == alice);
        eng.passAuction(bob);
        check("Alice wins auction for Vermont", st.holdingAt(8).getOwner() == alice);
        check("auction winner charged $50", alice.getCash() == 1590);
        check("phase returns to AWAITING_END_TURN", st.getPhase() == TurnPhase.AWAITING_END_TURN);
        eng.endTurn();

        // ---- 3. Give Alice the full BROWN group via a doubles chain ----------
        // Alice already owns Mediterranean(1); now grab Baltic(3) so she has a monopoly.
        check("current player is Alice", st.getCurrentPlayer() == alice);
        alice.setPosition(1);
        queue(1, 1);                                  // doubles, sum 2 -> idx 3 Baltic
        eng.rollDice();
        check("Alice landed on Baltic (idx3)", alice.getPosition() == 3);
        check("phase AWAITING_BUY (mid doubles)", st.getPhase() == TurnPhase.AWAITING_BUY);
        eng.buyProperty();
        check("doubles grants another roll (AWAITING_ROLL)", st.getPhase() == TurnPhase.AWAITING_ROLL);
        check("Alice owns full brown group",
                st.holdingAt(1).getOwner() == alice && st.holdingAt(3).getOwner() == alice);
        // second roll of the chain: land somewhere harmless and end
        alice.setPosition(15);
        queue(2, 3);                                  // sum 5 -> idx 20 Free Parking
        eng.rollDice();
        check("Free Parking awards nothing", alice.getPosition() == 20);
        check("phase AWAITING_END_TURN to finish turn", st.getPhase() == TurnPhase.AWAITING_END_TURN);

        // ---- 4. Even-build validation + double base rent ---------------------
        // Alice (current, AWAITING_END_TURN) owns brown monopoly, unmortgaged.
        check("canBuildHouse on Baltic", eng.canBuildHouse(3));
        eng.buildHouse(3);                            // Baltic -> 1 house
        check("even-build rejects 2nd Baltic house before Med has one", !eng.canBuildHouse(3));
        check("canBuildHouse on Mediterranean (even)", eng.canBuildHouse(1));
        eng.buildHouse(1);                            // Mediterranean -> 1 house
        check("now Baltic can take a 2nd house", eng.canBuildHouse(3));
        int beforeSupply = 32 - 2;                    // two houses placed
        eng.endTurn();

        // Bob lands on Baltic (1 house) -> rent table[1] = 20 (not doubled, has a house)
        check("current player is Bob", st.getCurrentPlayer() == bob);
        int bobBefore = bob.getCash();
        int aliceBefore = alice.getCash();
        bob.setPosition(0);
        queue(1, 2);                                  // -> idx 3 Baltic
        eng.rollDice();
        check("Bob pays $20 rent (1 house) on Baltic",
                bob.getCash() == bobBefore - 20 && alice.getCash() == aliceBefore + 20);
        eng.endTurn();

        // ---- 5. Sell houses (even) then mortgage / unmortgage ----------------
        check("current player is Alice", st.getCurrentPlayer() == alice);
        alice.setPosition(15);
        queue(2, 3);                                  // -> Free Parking, AWAITING_END_TURN
        eng.rollDice();
        // sell evenly: Baltic(1)=max, then Mediterranean(1)
        eng.sellHouse(3);
        eng.sellHouse(1);
        check("both brown houses sold (level 0)",
                st.holdingAt(1).getHouseCount() == 0 && st.holdingAt(3).getHouseCount() == 0);
        int cashPreMortgage = alice.getCash();
        check("canMortgage Baltic after houses sold", eng.canMortgage(3));
        eng.mortgage(3);                              // Baltic value 60/2 = 30
        check("mortgage credits $30", alice.getCash() == cashPreMortgage + 30);
        check("Baltic flagged mortgaged", st.holdingAt(3).isMortgaged());
        eng.unmortgage(3);                            // cost round(30*1.10) = 33
        check("unmortgage costs $33", alice.getCash() == cashPreMortgage + 30 - 33);
        check("Baltic no longer mortgaged", !st.holdingAt(3).isMortgaged());
        eng.endTurn();

        // ---- 6. Jail entry (Go To Jail) + exit by paying the fine -----------
        advanceTo(eng, st, alice);
        check("current player is Alice (jail test)", st.getCurrentPlayer() == alice);
        alice.setPosition(25);
        queue(2, 3);                                  // sum 5 -> idx 30 Go To Jail
        eng.rollDice();
        check("Go To Jail sends Alice to idx10", alice.getPosition() == 10);
        check("Alice is in jail", alice.isInJail());
        check("phase AWAITING_END_TURN after jailing", st.getPhase() == TurnPhase.AWAITING_END_TURN);
        eng.endTurn();
        advanceTo(eng, st, alice);                    // play Bob, return to Alice (still jailed)
        check("Alice still jailed at start of next turn", alice.isInJail());
        check("jailed player phase is AWAITING_ROLL", st.getPhase() == TurnPhase.AWAITING_ROLL);
        int cashPreFine = alice.getCash();
        eng.payJailFine();
        check("pay-fine deducts $50 and releases", alice.getCash() == cashPreFine - 50 && !alice.isInJail());
        alice.setPosition(10);
        queue(1, 2);                                  // move out of jail area -> idx13 States
        eng.rollDice();
        check("Alice moves after paying fine", alice.getPosition() == 13);
        resolveToEndTurn(eng, st, alice, bob);

        // ---- 7. Jail exit by rolling doubles --------------------------------
        // Put Bob in jail directly, then roll doubles to leave (no extra turn).
        advanceTo(eng, st, bob);
        bob.setInJail(true); bob.setPosition(10); bob.setJailTurns(0);
        queue(3, 3);                                  // doubles -> leave jail, move 6 to idx16
        eng.rollDice();
        check("doubles releases Bob from jail", !bob.isInJail());
        check("Bob moved 6 to idx16 (St. James)", bob.getPosition() == 16);
        check("doubles-from-jail grants NO extra roll", st.getPhase() != TurnPhase.AWAITING_ROLL);
        resolveToEndTurn(eng, st, alice, bob);

        // ---- 8. Bankruptcy (debt to a player ends the 2-player game) --------
        // Force Bob to land on Alice's Baltic with too little cash to pay rent.
        advanceTo(eng, st, bob);
        bob.setCash(5);                               // can't cover rent and has no other assets
        // strip any deeds Bob may have acquired so his net worth < rent
        bob.getDeeds().clear();
        bob.setPosition(0);
        queue(1, 2);                                  // -> idx 3 Baltic (Alice, monopoly, 0 houses -> rent 8)
        eng.rollDice();
        check("Bob owes rent he cannot pay (debt recorded)", st.getPhase() == TurnPhase.AWAITING_END_TURN);
        int aliceCashPreBk = alice.getCash();
        eng.declareBankruptcy();
        check("Bob is bankrupt", bob.isBankrupt());
        check("game over after last opponent bankrupt", st.isGameOver());
        check("Alice declared winner", st.getWinner() == alice);
        check("creditor received bankrupt's cash", alice.getCash() >= aliceCashPreBk);

        // ---- summary --------------------------------------------------------
        System.out.println("==================================");
        System.out.println("RESULT: " + passes + " passed, " + failures + " failed");
        if (failures == 0) {
            System.out.println("OVERALL: PASS");
        } else {
            System.out.println("OVERALL: FAIL");
        }
        if (failures != 0) System.exit(1);
    }

    private static void queue(int d1, int d2) {
        rng.queueDie(d1);
        rng.queueDie(d2);
    }

    /** Play harmless turns for whoever is current until {@code target} is the current player. */
    private static void advanceTo(GameEngine eng, GameState st, Player target) {
        int guard = 0;
        while (st.getCurrentPlayer() != target && !st.isGameOver()) {
            if (guard++ > 50) throw new IllegalStateException("advanceTo stuck");
            Player cur = st.getCurrentPlayer();
            if (cur.isInJail()) { eng.payJailFine(); }
            cur.setPosition(15);
            queue(2, 3);                              // sum 5 -> idx 20 Free Parking (no buy/auction)
            eng.rollDice();
            resolveToEndTurn(eng, st, null, null);
        }
    }

    /** Drive the current segment to a completed turn, declining any buy/auction. */
    private static void resolveToEndTurn(GameEngine eng, GameState st, Player a, Player b) {
        int guard = 0;
        while (!st.isGameOver()) {
            if (guard++ > 50) throw new IllegalStateException("resolveToEndTurn stuck");
            TurnPhase ph = st.getPhase();
            if (ph == TurnPhase.AWAITING_BUY) {
                eng.declineProperty();
            } else if (ph == TurnPhase.AUCTION) {
                // everyone passes -> stays unowned
                for (Player p : new java.util.ArrayList<Player>(st.getActivePlayers())) {
                    if (st.getPhase() == TurnPhase.AUCTION) eng.passAuction(p);
                }
            } else if (ph == TurnPhase.AWAITING_ROLL) {
                // a doubles bonus roll: take a harmless one
                st.getCurrentPlayer().setPosition(15);
                queue(2, 3);
                eng.rollDice();
            } else if (ph == TurnPhase.AWAITING_END_TURN) {
                eng.endTurn();
                return;
            } else {
                return;
            }
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) {
            passes++;
            System.out.println("PASS: " + label);
        } else {
            failures++;
            System.out.println("FAIL: " + label);
        }
    }

    /** Seeded Random whose {@code nextInt(6)} draws can be scripted for deterministic dice. */
    private static final class ScriptedRandom extends Random {
        private final java.util.ArrayDeque<Integer> dieQueue = new java.util.ArrayDeque<Integer>();
        ScriptedRandom(long seed) { super(seed); }
        void queueDie(int face) { dieQueue.addLast(face); }
        @Override
        public int nextInt(int bound) {
            if (bound == 6 && !dieQueue.isEmpty()) {
                return dieQueue.removeFirst() - 1;   // engine adds +1 to get the face
            }
            return super.nextInt(bound);
        }
    }

    private static final class SilentListener implements GameListener {
        public void onLog(String message) { /* System.out.println("  " + message); */ }
        public void onStateChanged() { }
        public void onGameOver(Player winner) { }
    }

    // keep imports used
    static { Set<Integer> s = new HashSet<Integer>(); s.size(); }
}
