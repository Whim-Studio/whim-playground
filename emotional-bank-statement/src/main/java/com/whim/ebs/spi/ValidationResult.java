package com.whim.ebs.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable result of a validation: a validity flag and an unmodifiable list of
 * error messages.
 */
public final class ValidationResult {

    private final boolean valid;
    private final List<String> errors;

    /**
     * Creates a validation result.
     *
     * @param valid  whether the validated subject is valid
     * @param errors the error messages (null is treated as empty); stored as an
     *               unmodifiable copy
     */
    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        List<String> copy = new ArrayList<String>();
        if (errors != null) {
            copy.addAll(errors);
        }
        this.errors = Collections.unmodifiableList(copy);
    }

    /**
     * Returns whether the validated subject is valid.
     *
     * @return true if valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns the unmodifiable list of error messages.
     *
     * @return the errors (never null)
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Returns a successful result with no errors.
     *
     * @return a valid result
     */
    public static ValidationResult ok() {
        return new ValidationResult(true, Collections.<String>emptyList());
    }

    /**
     * Returns a failed result with the given errors.
     *
     * @param errors the error messages
     * @return an invalid result
     */
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }
}
