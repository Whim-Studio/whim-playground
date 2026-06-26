package com.whim.monopoly.domain;

public interface StreetSpace extends OwnableSpace {
    ColorGroup getColorGroup();
    int getHouseCost();        // cost per house AND per hotel
    int[] getRentTable();      // length 6: base,1h,2h,3h,4h,hotel (base = undoubled, unimproved)
}
