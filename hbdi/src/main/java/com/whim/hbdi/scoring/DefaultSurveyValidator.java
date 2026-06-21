package com.whim.hbdi.scoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.whim.hbdi.domain.Question;
import com.whim.hbdi.domain.Response;

/**
 * Default validator: a survey is complete iff every question id has a
 * corresponding (non-null) Response in the response map. Unanswered ids are
 * returned in question order.
 */
public class DefaultSurveyValidator implements SurveyValidator {

    @Override
    public ValidationResult validate(List<Question> questions,
                                     Map<Integer, Response> responses) {
        List<Integer> unanswered = new ArrayList<Integer>();
        if (questions != null) {
            for (Question q : questions) {
                if (q == null) {
                    continue;
                }
                int id = q.getId();
                Response r = (responses == null) ? null : responses.get(id);
                if (r == null) {
                    unanswered.add(id);
                }
            }
        }
        return new ValidationResult(unanswered.isEmpty(), unanswered);
    }
}
