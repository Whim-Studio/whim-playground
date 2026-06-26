package com.whim.monopoly.data;

import com.whim.monopoly.domain.Board;
import com.whim.monopoly.domain.Card;
import com.whim.monopoly.domain.ColorGroup;
import com.whim.monopoly.domain.DefaultPlayer;
import com.whim.monopoly.domain.OwnableSpace;
import com.whim.monopoly.domain.Player;
import com.whim.monopoly.domain.Space;
import com.whim.monopoly.domain.StandardBoard;
import com.whim.monopoly.domain.StreetSpace;

import java.awt.Color;
import java.util.List;

/** Optional standalone sanity check for the Task 1 domain + data layer. */
public final class DomainSanity {
    private DomainSanity() { }

    public static void main(String[] args) {
        int fails = 0;

        Board board = new StandardBoard();
        fails += check("board size 40", board.spaces().size() == 40);
        fails += check("railroads = 4", board.railroads().size() == 4);
        fails += check("utilities = 2", board.utilities().size() == 2);
        fails += check("RR at 5/15/25/35",
                board.railroads().get(0).getIndex() == 5
                        && board.railroads().get(3).getIndex() == 35);
        fails += check("Income Tax idx4 = 200",
                ((com.whim.monopoly.domain.TaxSpace) board.spaceAt(4)).getTaxAmount() == 200);
        fails += check("Luxury Tax idx38 = 100",
                ((com.whim.monopoly.domain.TaxSpace) board.spaceAt(38)).getTaxAmount() == 100);

        StreetSpace boardwalk = (StreetSpace) board.spaceAt(39);
        fails += check("Boardwalk 400", boardwalk.getPrice() == 400);
        fails += check("Boardwalk hotel rent 2000", boardwalk.getRentTable()[5] == 2000);
        fails += check("Boardwalk mortgage 200", boardwalk.getMortgageValue() == 200);
        fails += check("Boardwalk unmortgage 220", boardwalk.getUnmortgageCost() == 220);

        OwnableSpace reading = (OwnableSpace) board.spaceAt(5);
        fails += check("Reading RR price 200", reading.getPrice() == 200);
        fails += check("Electric utility price 150",
                ((OwnableSpace) board.spaceAt(12)).getPrice() == 150);

        fails += check("nextRailroadFrom(4) = 5", board.nextRailroadFrom(4) == 5);
        fails += check("nextRailroadFrom(35) wraps to 5", board.nextRailroadFrom(35) == 5);
        fails += check("nextUtilityFrom(28) wraps to 12", board.nextUtilityFrom(28) == 12);
        fails += check("nextUtilityFrom(7) = 12", board.nextUtilityFrom(7) == 12);

        List<StreetSpace> browns = board.streetsInGroup(ColorGroup.BROWN);
        fails += check("Brown group has 2 streets", browns.size() == 2);

        Player p = new DefaultPlayer(0, "Alice", Color.RED);
        fails += check("player starts $1500", p.getCash() == 1500);
        fails += check("player starts pos 0", p.getPosition() == 0);
        fails += check("player not jailed", !p.isInJail());
        fails += check("player token", p.getToken().equals(Color.RED));

        List<Card> chance = Cards.chance();
        List<Card> cc = Cards.communityChest();
        fails += check("16 chance cards", chance.size() == 16);
        fails += check("16 community chest cards", cc.size() == 16);
        fails += check("chance has GOOJF", hasGoojf(chance));
        fails += check("community chest has GOOJF", hasGoojf(cc));

        System.out.println(fails == 0
                ? "ALL DOMAIN SANITY CHECKS PASSED"
                : (fails + " CHECK(S) FAILED"));
        if (fails != 0) {
            System.exit(1);
        }
    }

    private static boolean hasGoojf(List<Card> cards) {
        for (Card card : cards) {
            if (card.getAction() == com.whim.monopoly.domain.CardAction.GET_OUT_OF_JAIL_FREE) {
                return true;
            }
        }
        return false;
    }

    private static int check(String label, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + label);
        return ok ? 0 : 1;
    }
}
