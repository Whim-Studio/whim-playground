package com.whim.monopoly.domain;

public enum CardAction {
    COLLECT,                // +amount from bank
    PAY,                    // -amount to bank
    MOVE_TO_SPACE,          // advance to targetIndex; collect $200 if passing GO (engine decides)
    MOVE_BACK,              // move amount spaces backward; no GO pay
    GO_TO_JAIL,             // straight to jail, no GO
    GET_OUT_OF_JAIL_FREE,   // keep card
    NEAREST_RAILROAD,       // advance to next railroad; pay owner 2x rail rent (or buy/auction if unowned)
    NEAREST_UTILITY,        // advance to next utility; pay owner 10x a fresh dice roll (or buy/auction)
    STREET_REPAIRS,         // pay bank: amount per house + amount2 per hotel
    COLLECT_FROM_EACH,      // +amount from every other solvent player
    PAY_EACH                // -amount to every other solvent player
}
