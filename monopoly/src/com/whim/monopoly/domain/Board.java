package com.whim.monopoly.domain;

public interface Board {
    int SIZE = 40;
    Space spaceAt(int index);                          // 0..39
    java.util.List<Space> spaces();                    // size 40, index order
    java.util.List<RailroadSpace> railroads();
    java.util.List<UtilitySpace> utilities();
    java.util.List<StreetSpace> streetsInGroup(ColorGroup group);
    int nextRailroadFrom(int index);                   // wraps; for NEAREST_RAILROAD
    int nextUtilityFrom(int index);                    // wraps; for NEAREST_UTILITY
}
