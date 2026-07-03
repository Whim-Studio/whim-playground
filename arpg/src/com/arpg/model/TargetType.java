package com.arpg.model;

/**
 * Who an {@link Ability} can be aimed at. The engine uses this to validate a
 * {@link PlayerAction} and to pick targets for enemy abilities.
 */
public enum TargetType {
    SELF,
    SINGLE_ENEMY,
    AOE_ENEMIES,
    ALLY
}
