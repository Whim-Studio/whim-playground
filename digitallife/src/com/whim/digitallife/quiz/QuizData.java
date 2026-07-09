package com.whim.digitallife.quiz;

import com.whim.digitallife.model.Choice;
import com.whim.digitallife.model.Question;
import com.whim.digitallife.model.Trait;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Static content for the quiz: the fixed set of 12 questions and their scored
 * answer choices.
 *
 * <p>All content lives here so the model, controller, and UI classes stay free
 * of hard-coded text. Each choice is built with {@link Choice#of} using
 * alternating {@link Trait}/points pairs.</p>
 */
public final class QuizData {

    private QuizData() {
        // Utility class; not instantiable.
    }

    /**
     * Builds the ordered list of quiz questions.
     *
     * @return an immutable list of 12 {@link Question}s
     */
    public static List<Question> buildQuestions() {
        List<Question> q = new ArrayList<Question>();

        q.add(new Question(
                "It's a free Saturday with no plans. What sounds best?",
                Arrays.asList(
                        Choice.of("Try a brand-new activity I've never done before",
                                Trait.OPENNESS, 2),
                        Choice.of("Get ahead on chores and errands",
                                Trait.CONSCIENTIOUSNESS, 2),
                        Choice.of("Host a get-together with friends",
                                Trait.EXTRAVERSION, 2),
                        Choice.of("Relax quietly and recharge",
                                Trait.STABILITY, 2))));

        q.add(new Question(
                "How do you usually approach a big project?",
                Arrays.asList(
                        Choice.of("Brainstorm lots of creative angles first",
                                Trait.OPENNESS, 2),
                        Choice.of("Make a detailed plan and checklist",
                                Trait.CONSCIENTIOUSNESS, 2),
                        Choice.of("Rally a team and divide the work",
                                Trait.EXTRAVERSION, 1, Trait.AGREEABLENESS, 1),
                        Choice.of("Stay flexible and adapt as I go",
                                Trait.STABILITY, 2))));

        q.add(new Question(
                "A friend cancels plans at the last minute. You feel...",
                Arrays.asList(
                        Choice.of("Totally fine — things happen",
                                Trait.STABILITY, 2, Trait.AGREEABLENESS, 1),
                        Choice.of("A little let down but I understand",
                                Trait.AGREEABLENESS, 2),
                        Choice.of("Annoyed my schedule got disrupted",
                                Trait.CONSCIENTIOUSNESS, 1),
                        Choice.of("Relieved — now I have time to myself",
                                Trait.OPENNESS, 1, Trait.STABILITY, 1))));

        q.add(new Question(
                "At a party where you know only a few people, you...",
                Arrays.asList(
                        Choice.of("Introduce myself to lots of new faces",
                                Trait.EXTRAVERSION, 2),
                        Choice.of("Stick with the people I already know",
                                Trait.AGREEABLENESS, 2),
                        Choice.of("Find one interesting person for a deep chat",
                                Trait.OPENNESS, 2),
                        Choice.of("Help the host and keep busy",
                                Trait.CONSCIENTIOUSNESS, 1, Trait.AGREEABLENESS, 1))));

        q.add(new Question(
                "Which compliment would mean the most to you?",
                Arrays.asList(
                        Choice.of("\"You're so imaginative.\"",
                                Trait.OPENNESS, 2),
                        Choice.of("\"I can always rely on you.\"",
                                Trait.CONSCIENTIOUSNESS, 2),
                        Choice.of("\"You light up every room.\"",
                                Trait.EXTRAVERSION, 2),
                        Choice.of("\"You're so kind and thoughtful.\"",
                                Trait.AGREEABLENESS, 2))));

        q.add(new Question(
                "When facing an unexpected problem, your first move is to...",
                Arrays.asList(
                        Choice.of("Look for a clever, unconventional fix",
                                Trait.OPENNESS, 2),
                        Choice.of("Work through it step by step",
                                Trait.CONSCIENTIOUSNESS, 2),
                        Choice.of("Talk it out with someone",
                                Trait.EXTRAVERSION, 2),
                        Choice.of("Take a breath and stay calm",
                                Trait.STABILITY, 2))));

        q.add(new Question(
                "Your ideal vacation looks like...",
                Arrays.asList(
                        Choice.of("Backpacking somewhere I've never been",
                                Trait.OPENNESS, 2),
                        Choice.of("A well-organized itinerary with reservations",
                                Trait.CONSCIENTIOUSNESS, 2),
                        Choice.of("A lively city trip with lots of people",
                                Trait.EXTRAVERSION, 2),
                        Choice.of("A peaceful cabin to unwind",
                                Trait.STABILITY, 2))));

        q.add(new Question(
                "How do you handle disagreements?",
                Arrays.asList(
                        Choice.of("Look for a creative compromise",
                                Trait.OPENNESS, 1, Trait.AGREEABLENESS, 1),
                        Choice.of("Stick to the facts and stay logical",
                                Trait.CONSCIENTIOUSNESS, 2),
                        Choice.of("Speak up clearly and directly",
                                Trait.EXTRAVERSION, 2),
                        Choice.of("Try to keep everyone comfortable",
                                Trait.AGREEABLENESS, 2))));

        q.add(new Question(
                "Which phrase best describes your workspace?",
                Arrays.asList(
                        Choice.of("Full of inspiration and interesting clutter",
                                Trait.OPENNESS, 2),
                        Choice.of("Tidy, labeled, and everything in its place",
                                Trait.CONSCIENTIOUSNESS, 2),
                        Choice.of("Wherever the people and energy are",
                                Trait.EXTRAVERSION, 2),
                        Choice.of("Calm, cozy, and stress-free",
                                Trait.STABILITY, 2))));

        q.add(new Question(
                "A stranger drops their wallet on the street. You...",
                Arrays.asList(
                        Choice.of("Chase them down to return it right away",
                                Trait.AGREEABLENESS, 2, Trait.EXTRAVERSION, 1),
                        Choice.of("Hand it to the nearest shop or official",
                                Trait.CONSCIENTIOUSNESS, 2),
                        Choice.of("Calmly figure out the best way to help",
                                Trait.STABILITY, 1, Trait.AGREEABLENESS, 1),
                        Choice.of("Think of a smart way to track them down",
                                Trait.OPENNESS, 2))));

        q.add(new Question(
                "How do you feel about routines?",
                Arrays.asList(
                        Choice.of("I get restless — I crave variety",
                                Trait.OPENNESS, 2),
                        Choice.of("I love them; structure keeps me sharp",
                                Trait.CONSCIENTIOUSNESS, 2),
                        Choice.of("Fine, as long as they involve people",
                                Trait.EXTRAVERSION, 2),
                        Choice.of("They keep me grounded and relaxed",
                                Trait.STABILITY, 2))));

        q.add(new Question(
                "Pick the emoji that feels most like you today.",
                Arrays.asList(
                        Choice.of("✨  Sparkles — always exploring",
                                Trait.OPENNESS, 2),
                        Choice.of("✅  Check — on top of it all",
                                Trait.CONSCIENTIOUSNESS, 2),
                        Choice.of("🎉  Party — bring the fun",
                                Trait.EXTRAVERSION, 2),
                        Choice.of("🧘  Zen — calm and centered",
                                Trait.STABILITY, 2))));

        return Collections.unmodifiableList(q);
    }
}
