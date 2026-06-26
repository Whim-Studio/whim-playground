package com.whim.monopoly.domain;

public interface Card {
    Deck getDeck();
    String getText();          // the printed card text
    CardAction getAction();
    int getAmount();           // primary amount (per CardAction; per-house for STREET_REPAIRS)
    int getAmount2();          // secondary (per-hotel for STREET_REPAIRS; else 0)
    int getTargetIndex();      // for MOVE_TO_SPACE (else -1)
}
