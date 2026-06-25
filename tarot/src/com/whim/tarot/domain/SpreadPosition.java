package com.whim.tarot.domain;

/** One slot within a spread. */
public interface SpreadPosition {
    int getIndex();        // 0-based, matches deal order
    String getName();      // e.g. "The Present", "Hopes & Fears"
    String getMeaning();   // what this slot represents in a reading
}
