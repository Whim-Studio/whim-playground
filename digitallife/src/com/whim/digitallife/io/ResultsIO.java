package com.whim.digitallife.io;

import com.whim.digitallife.model.Choice;
import com.whim.digitallife.model.Question;
import com.whim.digitallife.model.ResultProfile;
import com.whim.digitallife.model.Trait;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Local, offline persistence for quiz results.
 *
 * <p>Everything this class touches is a plain {@code .txt} file on the user's own
 * machine — chosen explicitly by them via a file chooser. There is no networking,
 * no upload, and no data about anyone other than the single local user. The file
 * is written in a human-readable format that also round-trips back into a
 * {@link ResultProfile} for the "Load Previous Results" feature.</p>
 */
public final class ResultsIO {

    private static final String MAX_TAG = "Max-Per-Trait:";
    private static final String SCORE_SECTION = "-- Scores --";
    private static final String END_SCORE_SECTION = "-- Dominant --";

    private ResultsIO() {
        // Utility class; not instantiable.
    }

    /**
     * Writes a profile to a text file without an answer transcript. Used when the
     * profile was loaded from disk and the original per-question answers are not
     * available in this session.
     *
     * @param file    the destination chosen by the user
     * @param profile the results to write
     * @throws IOException if the file cannot be written
     */
    public static void save(File file, ResultProfile profile) throws IOException {
        save(file, profile, null, null);
    }

    /**
     * Writes a completed profile to a text file.
     *
     * @param file      the destination chosen by the user
     * @param profile   the computed results
     * @param questions the question list (for echoing the answers back), or null
     * @param selected  parallel array of chosen choice indices, or null
     * @throws IOException if the file cannot be written
     */
    public static void save(File file, ResultProfile profile,
                            List<Question> questions, int[] selected) throws IOException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(file, "UTF-8");
            out.println("=== thisisyourdigitallife — Personality Snapshot ===");
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            out.println("Saved: " + fmt.format(new Date()));
            out.println("This file contains only your own answers, created on your own device.");
            out.println("No network, no accounts, no other people's data.");
            out.println();

            // Machine-readable normalization ceiling so the file round-trips exactly.
            out.println(MAX_TAG + " " + profile.getMaxPossiblePerTrait());
            out.println();

            out.println(SCORE_SECTION);
            for (Trait trait : Trait.values()) {
                int raw = profile.getScores().containsKey(trait)
                        ? profile.getScores().get(trait) : 0;
                out.println(trait.getDisplayName() + ": " + raw
                        + "  (" + profile.getPercent(trait) + "%)");
            }
            out.println();

            out.println(END_SCORE_SECTION);
            out.println(profile.getDominant().getDisplayName());
            out.println();

            out.println("-- Summary --");
            out.println(wrap(profile.getSummary(), 78));
            out.println();

            if (questions != null && selected != null) {
                out.println("-- Your Answers --");
                for (int i = 0; i < questions.size(); i++) {
                    Question question = questions.get(i);
                    out.println((i + 1) + ". " + question.getPrompt());
                    int idx = i < selected.length ? selected[i] : -1;
                    if (idx >= 0) {
                        Choice choice = question.getChoices().get(idx);
                        out.println("   -> " + choice.getLabel());
                    } else {
                        out.println("   -> (no answer)");
                    }
                }
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Reads a previously saved results file back into a {@link ResultProfile}.
     *
     * <p>The dominant trait and summary are recomputed deterministically from the
     * saved scores, so a loaded profile matches what was originally shown.</p>
     *
     * @param file the file to read
     * @return the reconstructed profile
     * @throws IOException if the file is missing, unreadable, or not a valid
     *                     results file
     */
    public static ResultProfile load(File file) throws IOException {
        Map<Trait, Integer> scores = new EnumMap<Trait, Integer>(Trait.class);
        int maxPerTrait = -1;
        boolean inScores = false;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith(MAX_TAG)) {
                    maxPerTrait = parseIntSafe(trimmed.substring(MAX_TAG.length()).trim(), -1);
                    continue;
                }
                if (trimmed.equals(SCORE_SECTION)) {
                    inScores = true;
                    continue;
                }
                if (trimmed.equals(END_SCORE_SECTION)) {
                    inScores = false;
                    continue;
                }
                if (inScores && trimmed.contains(":")) {
                    parseScoreLine(trimmed, scores);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        if (scores.isEmpty() || maxPerTrait <= 0) {
            throw new IOException("This does not look like a valid results file.");
        }
        // Ensure every trait has an entry so the chart always renders all five.
        for (Trait trait : Trait.values()) {
            if (!scores.containsKey(trait)) {
                scores.put(trait, 0);
            }
        }
        return new ResultProfile(scores, maxPerTrait);
    }

    private static void parseScoreLine(String line, Map<Trait, Integer> scores) {
        int colon = line.indexOf(':');
        if (colon < 0) {
            return;
        }
        String name = line.substring(0, colon).trim();
        String rest = line.substring(colon + 1).trim();
        // rest looks like: "14  (78%)" — the raw score is the leading integer.
        int space = rest.indexOf(' ');
        String rawToken = space < 0 ? rest : rest.substring(0, space);
        int raw = parseIntSafe(rawToken.trim(), 0);
        for (Trait trait : Trait.values()) {
            if (trait.getDisplayName().equalsIgnoreCase(name)) {
                scores.put(trait, raw);
                return;
            }
        }
    }

    private static int parseIntSafe(String token, int fallback) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /** Simple word-wrap so the summary paragraph stays readable in a text editor. */
    private static String wrap(String text, int width) {
        StringBuilder sb = new StringBuilder();
        int lineLen = 0;
        for (String word : text.split(" ")) {
            if (lineLen + word.length() + 1 > width && lineLen > 0) {
                sb.append(System.lineSeparator());
                lineLen = 0;
            } else if (lineLen > 0) {
                sb.append(' ');
                lineLen++;
            }
            sb.append(word);
            lineLen += word.length();
        }
        return sb.toString();
    }
}
