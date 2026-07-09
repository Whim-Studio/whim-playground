package com.whim.digitallife.model;

import java.util.Collections;
import java.util.List;

/**
 * A single quiz question with a fixed list of {@link Choice} options.
 */
public final class Question {

    private final String prompt;
    private final List<Choice> choices;

    /**
     * @param prompt  the question text
     * @param choices the ordered list of answer options
     */
    public Question(String prompt, List<Choice> choices) {
        this.prompt = prompt;
        this.choices = Collections.unmodifiableList(choices);
    }

    /** @return the question text. */
    public String getPrompt() {
        return prompt;
    }

    /** @return the immutable, ordered list of answer choices. */
    public List<Choice> getChoices() {
        return choices;
    }
}
