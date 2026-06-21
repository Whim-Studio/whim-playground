package com.whim.hbdi.domain;

import java.util.List;

/** Source of the HBDI question set. */
public interface QuestionBank {
    List<Question> getQuestions();        // exactly 116, ordered by id
    int size();
}
