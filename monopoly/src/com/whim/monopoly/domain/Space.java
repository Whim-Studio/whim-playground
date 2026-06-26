package com.whim.monopoly.domain;

public interface Space {
    int getIndex();            // 0..39
    String getName();
    SpaceType getType();
}
