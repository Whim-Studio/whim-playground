package com.whim.hbdi.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Default {@link QuestionBank}. Loads the 116-item question set from the
 * classpath resource {@code com/whim/hbdi/questions.csv}. If that resource is
 * missing or unusable, falls back to a deterministically generated 116-item
 * bank so the application always has a complete survey.
 */
public final class DefaultQuestionBank implements QuestionBank {

    /** Total number of questions the bank must always expose. */
    public static final int EXPECTED_COUNT = 116;

    private static final String RESOURCE = "com/whim/hbdi/questions.csv";
    private static final String[] CATEGORIES = {
        "Work Style", "Problem Solving", "Communication",
        "Learning", "Decision Making", "Creativity"
    };

    private final List<Question> questions;

    public DefaultQuestionBank() {
        List<Question> loaded = loadFromClasspath();
        if (loaded == null || loaded.size() != EXPECTED_COUNT) {
            loaded = generateFallback();
        }
        Collections.sort(loaded, new Comparator<Question>() {
            public int compare(Question a, Question b) {
                return Integer.compare(a.getId(), b.getId());
            }
        });
        this.questions = Collections.unmodifiableList(loaded);
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public int size() {
        return questions.size();
    }

    // ---- CSV loading -----------------------------------------------------

    private static List<Question> loadFromClasspath() {
        InputStream in = DefaultQuestionBank.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (in == null) {
            return null;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<Question> result = new ArrayList<Question>();
            boolean headerSeen = false;
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (!headerSeen) {
                    // First non-blank, non-comment line is the header.
                    headerSeen = true;
                    if (startsWithIgnoreCase(trimmed, "id,")) {
                        continue;
                    }
                    // No recognizable header: treat this line as data.
                }
                Question q = parseLine(line);
                if (q != null) {
                    result.add(q);
                }
            }
            return result;
        } catch (IOException e) {
            return null;
        } finally {
            closeQuietly(reader);
            closeQuietly(in);
        }
    }

    /** Parses one CSV data row: id,category,text,weightA,weightB,weightC,weightD */
    private static Question parseLine(String line) {
        // Split into exactly 7 fields; the text field is field index 2 and may
        // itself contain commas, so we split from the left for the first two
        // fields and from the right for the last four.
        int firstComma = line.indexOf(',');
        if (firstComma < 0) {
            return null;
        }
        int secondComma = line.indexOf(',', firstComma + 1);
        if (secondComma < 0) {
            return null;
        }

        // Right side: the last four commas separate the four weights.
        int[] rightCommas = new int[4];
        int idx = line.length();
        for (int i = 3; i >= 0; i--) {
            idx = line.lastIndexOf(',', idx - 1);
            if (idx <= secondComma) {
                return null;
            }
            rightCommas[i] = idx;
        }

        try {
            int id = Integer.parseInt(line.substring(0, firstComma).trim());
            String category = unquote(line.substring(firstComma + 1, secondComma).trim());
            String text = unquote(line.substring(secondComma + 1, rightCommas[0]).trim());
            int wA = parseWeight(line.substring(rightCommas[0] + 1, rightCommas[1]));
            int wB = parseWeight(line.substring(rightCommas[1] + 1, rightCommas[2]));
            int wC = parseWeight(line.substring(rightCommas[2] + 1, rightCommas[3]));
            int wD = parseWeight(line.substring(rightCommas[3] + 1));
            return new DefaultQuestion(id, category, text, wA, wB, wC, wD);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int parseWeight(String s) {
        int w = Integer.parseInt(s.trim());
        return (w < 0) ? 0 : w;
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            return s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s;
    }

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        return s.length() >= prefix.length()
                && s.substring(0, prefix.length()).equalsIgnoreCase(prefix);
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
                // nothing to do
            }
        }
    }

    // ---- Fallback generation --------------------------------------------

    /**
     * Generates a deterministic 116-item bank. Each question is dominant in one
     * quadrant (rotating A,B,C,D) with light spillover into the others so the
     * scoring engine always has well-formed, non-degenerate data.
     */
    private static List<Question> generateFallback() {
        List<Question> result = new ArrayList<Question>(EXPECTED_COUNT);
        Quadrant[] quads = Quadrant.values();
        for (int id = 1; id <= EXPECTED_COUNT; id++) {
            int qi = (id - 1) % quads.length;
            Quadrant dominant = quads[qi];
            String category = CATEGORIES[(id - 1) % CATEGORIES.length];
            String text = phraseFor(dominant, id);
            int wA = baseWeight(Quadrant.A, dominant);
            int wB = baseWeight(Quadrant.B, dominant);
            int wC = baseWeight(Quadrant.C, dominant);
            int wD = baseWeight(Quadrant.D, dominant);
            result.add(new DefaultQuestion(id, category, text, wA, wB, wC, wD));
        }
        return result;
    }

    private static int baseWeight(Quadrant q, Quadrant dominant) {
        return (q == dominant) ? 3 : 1;
    }

    private static String phraseFor(Quadrant q, int id) {
        String stem;
        switch (q) {
            case A:
                stem = "I enjoy analyzing data and reasoning through problems logically";
                break;
            case B:
                stem = "I prefer organized plans, clear steps, and following procedures";
                break;
            case C:
                stem = "I value working with people and understanding how others feel";
                break;
            case D:
                stem = "I like exploring new ideas, imagining possibilities, and the big picture";
                break;
            default:
                stem = "I approach tasks in my own way";
        }
        return stem + " (item " + id + ")";
    }
}
