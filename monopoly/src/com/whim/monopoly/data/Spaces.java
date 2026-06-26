package com.whim.monopoly.data;

import com.whim.monopoly.domain.CardSpace;
import com.whim.monopoly.domain.ColorGroup;
import com.whim.monopoly.domain.Deck;
import com.whim.monopoly.domain.RailroadSpace;
import com.whim.monopoly.domain.Space;
import com.whim.monopoly.domain.SpaceType;
import com.whim.monopoly.domain.StreetSpace;
import com.whim.monopoly.domain.TaxSpace;
import com.whim.monopoly.domain.UtilitySpace;

/**
 * Concrete {@link Space} implementations for the standard board. Internal data-layer
 * helper used by {@code StandardBoard}; kept package-scoped via static factory methods.
 */
public final class Spaces {
    private Spaces() { }

    public static Space simple(int index, String name, SpaceType type) {
        return new SimpleSpace(index, name, type);
    }

    public static StreetSpace street(int index, String name, ColorGroup group,
                                     int price, int houseCost, int[] rentTable) {
        return new StreetSpaceImpl(index, name, group, price, houseCost, rentTable);
    }

    public static RailroadSpace railroad(int index, String name, int price) {
        return new RailroadSpaceImpl(index, name, price);
    }

    public static UtilitySpace utility(int index, String name, int price) {
        return new UtilitySpaceImpl(index, name, price);
    }

    public static TaxSpace tax(int index, String name, int amount) {
        return new TaxSpaceImpl(index, name, amount);
    }

    public static CardSpace card(int index, String name, Deck deck) {
        return new CardSpaceImpl(index, name, deck);
    }

    // ---- base ----

    static abstract class AbstractSpace implements Space {
        private final int index;
        private final String name;
        private final SpaceType type;

        AbstractSpace(int index, String name, SpaceType type) {
            this.index = index;
            this.name = name;
            this.type = type;
        }

        public int getIndex() { return index; }
        public String getName() { return name; }
        public SpaceType getType() { return type; }
    }

    static abstract class AbstractOwnable extends AbstractSpace
            implements com.whim.monopoly.domain.OwnableSpace {
        private final int price;

        AbstractOwnable(int index, String name, SpaceType type, int price) {
            super(index, name, type);
            this.price = price;
        }

        public int getPrice() { return price; }
        public int getMortgageValue() { return price / 2; }
        public int getUnmortgageCost() {
            return (int) Math.round(getMortgageValue() * 1.10);
        }
    }

    static final class SimpleSpace extends AbstractSpace {
        SimpleSpace(int index, String name, SpaceType type) {
            super(index, name, type);
        }
    }

    static final class StreetSpaceImpl extends AbstractOwnable implements StreetSpace {
        private final ColorGroup group;
        private final int houseCost;
        private final int[] rentTable;

        StreetSpaceImpl(int index, String name, ColorGroup group,
                        int price, int houseCost, int[] rentTable) {
            super(index, name, SpaceType.STREET, price);
            this.group = group;
            this.houseCost = houseCost;
            this.rentTable = rentTable;
        }

        public ColorGroup getColorGroup() { return group; }
        public int getHouseCost() { return houseCost; }
        public int[] getRentTable() { return rentTable.clone(); }
    }

    static final class RailroadSpaceImpl extends AbstractOwnable implements RailroadSpace {
        RailroadSpaceImpl(int index, String name, int price) {
            super(index, name, SpaceType.RAILROAD, price);
        }
    }

    static final class UtilitySpaceImpl extends AbstractOwnable implements UtilitySpace {
        UtilitySpaceImpl(int index, String name, int price) {
            super(index, name, SpaceType.UTILITY, price);
        }
    }

    static final class TaxSpaceImpl extends AbstractSpace implements TaxSpace {
        private final int amount;
        TaxSpaceImpl(int index, String name, int amount) {
            super(index, name, SpaceType.TAX);
            this.amount = amount;
        }
        public int getTaxAmount() { return amount; }
    }

    static final class CardSpaceImpl extends AbstractSpace implements CardSpace {
        private final Deck deck;
        CardSpaceImpl(int index, String name, Deck deck) {
            super(index, name, deck == Deck.CHANCE ? SpaceType.CHANCE : SpaceType.COMMUNITY_CHEST);
            this.deck = deck;
        }
        public Deck getDeck() { return deck; }
    }
}
