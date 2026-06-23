package com.tiwas.mahjong.model;

/** The three kinds of set that can be formed. */
public enum MeldType {
    PUNG,   // three identical tiles
    KONG,   // four identical tiles
    CHOW;   // three-tile run in one suit (always concealed in these rules)
}
