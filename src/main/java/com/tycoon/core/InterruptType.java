package com.tycoon.core;

public enum InterruptType {
    DEVELOPMENT_MILESTONE,  // a phase boundary reached
    EMPLOYEE_CRISIS,        // an employee stress >= crisis threshold
    MARKET_SHIFT,           // competitor chart upheaval
    GAME_RELEASED,          // player project reviewed & shipped
    MANUAL_PAUSE;           // player asked to stop / turn budget exhausted
}
