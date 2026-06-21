package com.whim.hbdi.scoring;

import java.util.List;
import java.util.Map;

import com.whim.hbdi.domain.Question;
import com.whim.hbdi.domain.Response;

/**
 * Validates that a set of responses fully covers a list of questions.
 */
public interface SurveyValidator {

    ValidationResult validate(List<Question> questions,
                              Map<Integer, Response> responses);
}
