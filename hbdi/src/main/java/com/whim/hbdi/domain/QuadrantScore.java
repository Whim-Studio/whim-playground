package com.whim.hbdi.domain;

/** Computed score for a single quadrant. */
public interface QuadrantScore {
    Quadrant getQuadrant();
    double getRawScore();                 // weighted sum
    double getPercentage();               // 0..100, all four sum to ~100
}
