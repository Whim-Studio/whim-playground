package com.whim.monopoly.domain;

// Anything that can be owned: streets, railroads, utilities.
public interface OwnableSpace extends Space {
    int getPrice();
    int getMortgageValue();    // == price / 2
    int getUnmortgageCost();   // == Math.round(getMortgageValue() * 1.10)
}
