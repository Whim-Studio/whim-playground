package com.whim.hbdi.scoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable outcome of validating a survey: whether every question was
 * answered, and the (sorted) ids of any that were not.
 */
public final class ValidationResult {

    private final boolean complete;
    private final List<Integer> unansweredIds;

    public ValidationResult(boolean complete, List<Integer> unansweredIds) {
        this.complete = complete;
        List<Integer> copy = new ArrayList<Integer>();
        if (unansweredIds != null) {
            copy.addAll(unansweredIds);
        }
        this.unansweredIds = Collections.unmodifiableList(copy);
    }

    public boolean isComplete() {
        return complete;
    }

    public List<Integer> getUnansweredIds() {
        return unansweredIds;
    }

    @Override
    public String toString() {
        return "ValidationResult{complete=" + complete
                + ", unansweredIds=" + unansweredIds + "}";
    }
}
