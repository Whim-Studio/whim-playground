package com.whim.tacticalnexus.domain;

/** Discriminator for the kind of {@link Entity} occupying a cell. */
public enum EntityType {
    WALL, DOOR, KEY, ENEMY, GEM, STAIR, PLAYER, EMPTY
}
