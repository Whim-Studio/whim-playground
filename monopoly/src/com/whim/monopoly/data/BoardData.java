package com.whim.monopoly.data;

import com.whim.monopoly.domain.ColorGroup;
import com.whim.monopoly.domain.Deck;
import com.whim.monopoly.domain.Space;
import com.whim.monopoly.domain.SpaceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Canonical US-edition 40-space board data. Internal data-layer helper for
 * {@code StandardBoard}. Prices, color groups, rent tables and house costs follow
 * the standard Monopoly deeds.
 */
public final class BoardData {
    private BoardData() { }

    /** Builds the 40 spaces in index order (0..39). */
    public static List<Space> buildSpaces() {
        List<Space> s = new ArrayList<Space>(40);

        s.add(Spaces.simple(0, "GO", SpaceType.GO));
        s.add(Spaces.street(1, "Mediterranean Avenue", ColorGroup.BROWN, 60, 50,
                new int[]{2, 10, 30, 90, 160, 250}));
        s.add(Spaces.card(2, "Community Chest", Deck.COMMUNITY_CHEST));
        s.add(Spaces.street(3, "Baltic Avenue", ColorGroup.BROWN, 60, 50,
                new int[]{4, 20, 60, 180, 320, 450}));
        s.add(Spaces.tax(4, "Income Tax", 200));
        s.add(Spaces.railroad(5, "Reading Railroad", 200));
        s.add(Spaces.street(6, "Oriental Avenue", ColorGroup.LIGHT_BLUE, 100, 50,
                new int[]{6, 30, 90, 270, 400, 550}));
        s.add(Spaces.card(7, "Chance", Deck.CHANCE));
        s.add(Spaces.street(8, "Vermont Avenue", ColorGroup.LIGHT_BLUE, 100, 50,
                new int[]{6, 30, 90, 270, 400, 550}));
        s.add(Spaces.street(9, "Connecticut Avenue", ColorGroup.LIGHT_BLUE, 120, 50,
                new int[]{8, 40, 100, 300, 450, 600}));
        s.add(Spaces.simple(10, "Jail / Just Visiting", SpaceType.JAIL));
        s.add(Spaces.street(11, "St. Charles Place", ColorGroup.PINK, 140, 100,
                new int[]{10, 50, 150, 450, 625, 750}));
        s.add(Spaces.utility(12, "Electric Company", 150));
        s.add(Spaces.street(13, "States Avenue", ColorGroup.PINK, 140, 100,
                new int[]{10, 50, 150, 450, 625, 750}));
        s.add(Spaces.street(14, "Virginia Avenue", ColorGroup.PINK, 160, 100,
                new int[]{12, 60, 180, 500, 700, 900}));
        s.add(Spaces.railroad(15, "Pennsylvania Railroad", 200));
        s.add(Spaces.street(16, "St. James Place", ColorGroup.ORANGE, 180, 100,
                new int[]{14, 70, 200, 550, 750, 950}));
        s.add(Spaces.card(17, "Community Chest", Deck.COMMUNITY_CHEST));
        s.add(Spaces.street(18, "Tennessee Avenue", ColorGroup.ORANGE, 180, 100,
                new int[]{14, 70, 200, 550, 750, 950}));
        s.add(Spaces.street(19, "New York Avenue", ColorGroup.ORANGE, 200, 100,
                new int[]{16, 80, 220, 600, 800, 1000}));
        s.add(Spaces.simple(20, "Free Parking", SpaceType.FREE_PARKING));
        s.add(Spaces.street(21, "Kentucky Avenue", ColorGroup.RED, 220, 150,
                new int[]{18, 90, 250, 700, 875, 1050}));
        s.add(Spaces.card(22, "Chance", Deck.CHANCE));
        s.add(Spaces.street(23, "Indiana Avenue", ColorGroup.RED, 220, 150,
                new int[]{18, 90, 250, 700, 875, 1050}));
        s.add(Spaces.street(24, "Illinois Avenue", ColorGroup.RED, 240, 150,
                new int[]{20, 100, 300, 750, 925, 1100}));
        s.add(Spaces.railroad(25, "B&O Railroad", 200));
        s.add(Spaces.street(26, "Atlantic Avenue", ColorGroup.YELLOW, 260, 150,
                new int[]{22, 110, 330, 800, 975, 1150}));
        s.add(Spaces.street(27, "Ventnor Avenue", ColorGroup.YELLOW, 260, 150,
                new int[]{22, 110, 330, 800, 975, 1150}));
        s.add(Spaces.utility(28, "Water Works", 150));
        s.add(Spaces.street(29, "Marvin Gardens", ColorGroup.YELLOW, 280, 150,
                new int[]{24, 120, 360, 850, 1025, 1200}));
        s.add(Spaces.simple(30, "Go To Jail", SpaceType.GO_TO_JAIL));
        s.add(Spaces.street(31, "Pacific Avenue", ColorGroup.GREEN, 300, 200,
                new int[]{26, 130, 390, 900, 1100, 1275}));
        s.add(Spaces.street(32, "North Carolina Avenue", ColorGroup.GREEN, 300, 200,
                new int[]{26, 130, 390, 900, 1100, 1275}));
        s.add(Spaces.card(33, "Community Chest", Deck.COMMUNITY_CHEST));
        s.add(Spaces.street(34, "Pennsylvania Avenue", ColorGroup.GREEN, 320, 200,
                new int[]{28, 150, 450, 1000, 1200, 1400}));
        s.add(Spaces.railroad(35, "Short Line Railroad", 200));
        s.add(Spaces.card(36, "Chance", Deck.CHANCE));
        s.add(Spaces.street(37, "Park Place", ColorGroup.DARK_BLUE, 350, 200,
                new int[]{35, 175, 500, 1100, 1300, 1500}));
        s.add(Spaces.tax(38, "Luxury Tax", 100));
        s.add(Spaces.street(39, "Boardwalk", ColorGroup.DARK_BLUE, 400, 200,
                new int[]{50, 200, 600, 1400, 1700, 2000}));

        return Collections.unmodifiableList(s);
    }
}
