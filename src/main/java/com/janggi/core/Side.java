package com.janggi.core;

/** The two players. CHO (초, green) moves first; HAN (한, red) moves second. */
public enum Side {
    CHO,   // 초 (green) — moves first
    HAN;   // 한 (red)

    public Side opponent() {
        return this == CHO ? HAN : CHO;
    }
}
