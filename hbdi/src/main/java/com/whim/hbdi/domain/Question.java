package com.whim.hbdi.domain;

import java.util.Map;

/** A single HBDI survey question with per-quadrant weights. */
public interface Question {
    int getId();                          // 1..116, unique
    String getText();
    String getCategory();
    Map<Quadrant, Integer> getQuadrantWeights(); // weight per quadrant, >=0
}
