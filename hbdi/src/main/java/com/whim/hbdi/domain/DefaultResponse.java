package com.whim.hbdi.domain;

/** Immutable {@link Response} implementation for a 5-point Likert answer. */
public final class DefaultResponse implements Response {

    private final int questionId;
    private final int value;

    public DefaultResponse(int questionId, int value) {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("Likert value must be in 1..5, was " + value);
        }
        this.questionId = questionId;
        this.value = value;
    }

    public int getQuestionId() {
        return questionId;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Response{q=" + questionId + ", value=" + value + "}";
    }
}
