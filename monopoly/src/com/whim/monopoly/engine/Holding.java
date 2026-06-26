package com.whim.monopoly.engine;

import com.whim.monopoly.domain.OwnableSpace;
import com.whim.monopoly.domain.Player;

// Read-only view of one ownable space's live ownership state.
public interface Holding {
    OwnableSpace getSpace();
    Player getOwner();         // null => bank-owned (unowned)
    boolean isMortgaged();
    int getHouseCount();       // 0..4 (hotel reported separately)
    boolean hasHotel();        // true == 5th building
}
