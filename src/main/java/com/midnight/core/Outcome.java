package com.midnight.core;

/**
 * The state of the war. The player may win along the Adventure path (Morkin
 * destroys the Ice Crown) or the Wargame path (a Free lord captures Ushgarak).
 * Doomdark wins if Luxor is slain or the Citadel of the Moon falls.
 */
public enum Outcome {
    ONGOING,
    FREE_ADVENTURE_WIN,
    FREE_WARGAME_WIN,
    DOOMDARK_WIN
}
