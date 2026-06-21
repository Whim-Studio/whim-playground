package com.whim.hbdi.domain;

/** A respondent's answer to a single question on a 5-point Likert scale. */
public interface Response {
    int getQuestionId();
    int getValue();                       // Likert 1..5
}
