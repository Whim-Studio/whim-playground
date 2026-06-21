package com.whim.ebs.logic;

import java.util.ArrayList;
import java.util.List;

import com.whim.ebs.domain.SessionState;
import com.whim.ebs.spi.ValidationResult;
import com.whim.ebs.spi.ValidationService;

/**
 * Default {@link ValidationService} for the Emotional Bank Statement exercise.
 *
 * <p>The exercise can only be finalized when a belief is selected, all three
 * proofs are non-blank, and a daily action is provided. Each failed rule
 * contributes a separate human-readable error message.</p>
 */
public class DefaultValidationService implements ValidationService {

    @Override
    public ValidationResult validate(SessionState state) {
        List<String> errors = new ArrayList<String>();

        if (state == null) {
            errors.add("No session state is available to validate.");
            return ValidationResult.failure(errors);
        }

        if (state.getSelectedBelief() == null) {
            errors.add("Please select a core belief before finalizing.");
        }

        for (int i = 0; i < 3; i++) {
            String proof = state.getProof(i);
            if (isBlank(proof)) {
                errors.add("Proof #" + (i + 1) + " must not be empty.");
            }
        }

        if (isBlank(state.getDailyAction())) {
            errors.add("Please describe one action you will take today.");
        }

        if (errors.isEmpty()) {
            return ValidationResult.ok();
        }
        return ValidationResult.failure(errors);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
