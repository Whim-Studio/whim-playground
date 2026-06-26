package com.whim.starcraft8.domain;

// 8-bit damage model: NORMAL = full vs all; EXPLOSIVE = full vs LARGE, 1/2 vs SMALL;
// CONCUSSIVE = full vs SMALL, 1/2 vs LARGE. (MEDIUM = full for both special types.)
public enum DamageType { NORMAL, EXPLOSIVE, CONCUSSIVE }
