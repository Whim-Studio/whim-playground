package com.whim.ebs.spi;

import com.whim.ebs.domain.SessionState;

/**
 * Validates a {@link SessionState} and reports any errors.
 */
public interface ValidationService {

    /**
     * Validates the given session state.
     *
     * @param state the session state to validate
     * @return the validation result
     */
    ValidationResult validate(SessionState state);
}
