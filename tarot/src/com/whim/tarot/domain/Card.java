package com.whim.tarot.domain;

/** A single tarot card and its authentic Rider-Waite-Smith data. */
public interface Card {
    int getId();                  // 0..77, unique, stable
    String getName();             // e.g. "The Fool", "Three of Cups"
    Suit getSuit();
    int getNumber();              // Major: 0..21 (Roman numeral order). Minor: 1..14 (Ace=1..King=14)
    String getUprightMeaning();   // 1-3 sentences, keyword-rich
    String getReversedMeaning();  // 1-3 sentences
    String getDescription();      // high-level imagery / symbolism, 1-3 sentences
    String getImageUrl();         // public-domain RWS image URL (Wikimedia Commons direct file URL)
    default boolean isMajor() { return getSuit().isMajor(); }
}
