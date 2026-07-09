package com.whim.alganon.engine;

/** Mutable partial selection during the race → family → class → name wizard. */
final class CreationState {
    String raceId;
    String familyId;
    String classId;
    String name = "";
    int step; // 0=race, 1=family, 2=class, 3=name/confirm
}
