package com.whim.tarot.engine;

import com.whim.tarot.domain.Card;
import com.whim.tarot.domain.DrawnCard;
import com.whim.tarot.domain.Orientation;
import com.whim.tarot.domain.SpreadPosition;
import com.whim.tarot.domain.SpreadType;
import com.whim.tarot.domain.Suit;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a dealt {@link Reading} into a cohesive, multi-paragraph plain-English
 * interpretation.
 *
 * <p>The output opens with a framing paragraph, walks each position in order —
 * naming the slot, the card, its orientation, and weaving the slot's meaning
 * with the card's active meaning — and closes with a synthesized conclusion
 * that observes Major-Arcana density, suit dominance, and reversed weighting.
 */
public final class ReadingInterpreter {

    public ReadingInterpreter() {
    }

    /** Produces the full synthesized reading text. */
    public String interpret(Reading reading) {
        if (reading == null) {
            throw new IllegalArgumentException("reading must not be null");
        }
        List<PositionedCard> cards = reading.getPositionedCards();
        StringBuilder sb = new StringBuilder();

        String question = reading.getQuestion();
        if (question != null && question.trim().length() > 0) {
            sb.append("You asked: \"").append(question.trim()).append("\"\n\n");
        }

        sb.append(opening(reading.getSpreadType(), cards));
        sb.append("\n\n");

        for (int i = 0; i < cards.size(); i++) {
            sb.append(positionParagraph(cards.get(i), i, cards.size()));
            if (i < cards.size() - 1) {
                sb.append("\n\n");
            }
        }

        String conclusion = conclusion(reading.getSpreadType(), cards);
        if (conclusion.length() > 0) {
            sb.append("\n\n");
            sb.append(conclusion);
        }

        String positional = positionalNote(reading.getSpreadType(), cards);
        if (positional.length() > 0) {
            sb.append(" ").append(positional);
        }

        if (question != null && question.trim().length() > 0) {
            sb.append("\n\nAs for your question — \"").append(question.trim())
                    .append("\" — let the arc above be the answer's shape rather than a yes or no; "
                    + "the cards describe the terrain you are asking about, and the choice within it stays yours.");
        }
        return sb.toString();
    }

    // ---- Opening -----------------------------------------------------------

    private String opening(SpreadType type, List<PositionedCard> cards) {
        StringBuilder sb = new StringBuilder();
        sb.append("This reading uses the ").append(type.getLabel())
                .append(" spread");
        if (cards.size() == 1) {
            sb.append(", a single card meant to focus the day's attention. ");
        } else {
            sb.append(", drawing ").append(cards.size())
                    .append(" cards that speak to one another in sequence. ");
        }
        if (cards.size() > 1) {
            PositionedCard first = cards.get(0);
            PositionedCard last = cards.get(cards.size() - 1);
            sb.append("It opens at \"").append(first.getPosition().getName())
                    .append("\" and resolves at \"")
                    .append(last.getPosition().getName())
                    .append(",\" so read it as a single arc rather than a stack of separate omens.");
        } else {
            sb.append("Let it set the tone you carry forward.");
        }
        return sb.toString();
    }

    // ---- Per-position paragraph -------------------------------------------

    private String positionParagraph(PositionedCard pc, int index, int total) {
        SpreadPosition pos = pc.getPosition();
        DrawnCard drawn = pc.getDrawnCard();
        Card card = drawn.getCard();
        boolean reversed = drawn.isReversed();

        StringBuilder sb = new StringBuilder();
        sb.append("In the position of \"").append(pos.getName()).append("\"")
                .append(" — ").append(lowerFirst(pos.getMeaning()))
                .append(reversed ? " — falls " : " — sits ")
                .append(card.getName()).append(", ")
                .append(reversed ? "reversed." : "upright.");
        sb.append(" ");

        sb.append(orientationGloss(card, reversed));
        sb.append(" ");

        sb.append("Here that energy speaks to ")
                .append(lowerFirst(stripTrailingPeriod(pos.getMeaning())))
                .append(": ").append(ensureSentence(drawn.getActiveMeaning()));

        sb.append(" ").append(connective(index, total, pos));
        return sb.toString().trim();
    }

    private String orientationGloss(Card card, boolean reversed) {
        if (reversed) {
            return "Drawn reversed, " + lowerFirst(card.getName())
                    + " turns its current inward or works against the grain, "
                    + "asking you to notice where this force is blocked, delayed, or overdone.";
        }
        return "Standing upright, " + lowerFirst(card.getName())
                + " brings its meaning plainly and outwardly into play.";
    }

    private String connective(int index, int total, SpreadPosition pos) {
        if (total == 1) {
            return "Carry this single insight as the day's quiet instruction.";
        }
        if (index == 0) {
            return "This is the ground the rest of the reading is built upon.";
        }
        if (index == total - 1) {
            return "As the closing card, it gathers the threads above into where things are tending.";
        }
        return "It hands the thread to what follows.";
    }

    // ---- Conclusion / synthesis -------------------------------------------

    private String conclusion(SpreadType type, List<PositionedCard> cards) {
        if (cards.isEmpty()) {
            return "";
        }
        int total = cards.size();
        int majors = 0;
        int reversedCount = 0;
        Map<Suit, Integer> suitCounts = new EnumMap<Suit, Integer>(Suit.class);
        for (PositionedCard pc : cards) {
            Card card = pc.getDrawnCard().getCard();
            if (card.isMajor()) {
                majors++;
            }
            if (pc.getDrawnCard().isReversed()) {
                reversedCount++;
            }
            Suit suit = card.getSuit();
            Integer prev = suitCounts.get(suit);
            suitCounts.put(suit, prev == null ? 1 : prev + 1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Drawing the threads together: ");

        // Single-card readings get a lighter close.
        if (total == 1) {
            PositionedCard only = cards.get(0);
            sb.append("with just one card on the table, the whole of the message lives in ")
                    .append(only.getDrawnCard().getCard().getName())
                    .append(only.getDrawnCard().isReversed() ? " reversed" : "")
                    .append(". Sit with it rather than over-reading it — a single card is a nudge, not a verdict.");
            return sb.toString();
        }

        // Major arcana density.
        if (majors == 0) {
            sb.append("there is no Major Arcana here, which keeps the reading grounded in everyday, "
                    + "workable matters rather than fated turning points. ");
        } else if (majors == 1) {
            sb.append("a single Major Arcana card anchors the reading, marking one genuinely "
                    + "significant theme amid otherwise day-to-day currents. ");
        } else if (majors >= total - majors && majors >= 3) {
            sb.append("Major Arcana cards dominate (")
                    .append(majors).append(" of ").append(total)
                    .append("), so this reads as a chapter of real consequence — larger forces and "
                    + "life lessons are at work, not just passing moods. ");
        } else {
            sb.append(majors).append(" Major Arcana cards raise the stakes, threading weightier "
                    + "themes through the more ordinary cards around them. ");
        }

        // Suit dominance among minors.
        Suit dominant = null;
        int dominantCount = 0;
        for (Map.Entry<Suit, Integer> e : suitCounts.entrySet()) {
            if (e.getKey() == Suit.MAJOR) {
                continue;
            }
            if (e.getValue() > dominantCount) {
                dominantCount = e.getValue();
                dominant = e.getKey();
            }
        }
        if (dominant != null && dominantCount >= 2 && dominantCount >= (total + 1) / 2) {
            sb.append("The suit of ").append(dominant.getLabel())
                    .append(" runs strongest (").append(dominantCount).append(" cards), ")
                    .append(suitTheme(dominant)).append(" ");
        } else if (dominant != null && dominantCount >= 2) {
            sb.append(dominant.getLabel()).append(" appears more than once, lending a current of ")
                    .append(suitKeyword(dominant)).append(" to the spread. ");
        }

        // Reversed weighting.
        if (reversedCount == 0) {
            sb.append("Every card stands upright, so the energies move openly and outwardly — "
                    + "little is hidden or stalled.");
        } else if (reversedCount == total) {
            sb.append("Every card is reversed, an unusually inward, blocked, or in-process reading: "
                    + "the work right now is internal, and progress may feel held back until something is released.");
        } else if (reversedCount >= (total + 1) / 2) {
            sb.append("With ").append(reversedCount).append(" of ").append(total)
                    .append(" cards reversed, the reading leans inward — much of this is unfinished, "
                    + "delayed, or asking to be reconsidered before it can move freely.");
        } else {
            sb.append(reversedCount).append(" reversed card")
                    .append(reversedCount == 1 ? "" : "s")
                    .append(" mark where energy is snagged or turned inward, "
                    + "while the upright majority keeps the overall current moving forward.");
        }

        return sb.toString();
    }

    /**
     * Position-aware framework notes for spreads whose slots carry a temporal
     * arc (Past / Present / Future). Reads the reversal pattern across those
     * slots — a classic reading heuristic — and names what it tends to signify.
     */
    private String positionalNote(SpreadType type, List<PositionedCard> cards) {
        if (type != SpreadType.THREE_CARD || cards.size() < 3) {
            return "";
        }
        boolean pastRev = cards.get(0).getDrawnCard().isReversed();
        boolean presentRev = cards.get(1).getDrawnCard().isReversed();
        boolean futureRev = cards.get(2).getDrawnCard().isReversed();

        if (pastRev && futureRev && !presentRev) {
            return "With both the Past and the Future reversed while the Present stands upright, "
                    + "the framework reads this as a hinge: you are stepping clear of something that "
                    + "stalled behind you, but the road ahead is not yet settled — act in the present, "
                    + "where the one clear card sits.";
        }
        if (pastRev && presentRev && futureRev) {
            return "All three time-cards reversed mark a fully internal passage — the situation is "
                    + "working itself out beneath the surface across past, present, and future alike.";
        }
        if (!pastRev && !presentRev && futureRev) {
            return "Only the Future is reversed, hinting that the path so far has been clear but the "
                    + "outcome is still unformed or asks to be reconsidered before it lands.";
        }
        if (pastRev && !presentRev && !futureRev) {
            return "With only the Past reversed, an old block is releasing, freeing the present and "
                    + "future to move openly.";
        }
        return "";
    }

    private String suitTheme(Suit suit) {
        switch (suit) {
            case WANDS:
                return "so drive, creativity, and momentum are the engine of this situation.";
            case CUPS:
                return "so feeling, relationship, and intuition are where the heart of this lies.";
            case SWORDS:
                return "so thought, conflict, and hard truths set the terms here.";
            case PENTACLES:
                return "so work, money, body, and the material world are the proving ground.";
            default:
                return "so its themes color the whole reading.";
        }
    }

    private String suitKeyword(Suit suit) {
        switch (suit) {
            case WANDS:
                return "energy and ambition";
            case CUPS:
                return "emotion and connection";
            case SWORDS:
                return "intellect and tension";
            case PENTACLES:
                return "practicality and resources";
            default:
                return "its themes";
        }
    }

    // ---- Small text helpers -----------------------------------------------

    private String lowerFirst(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private String stripTrailingPeriod(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        while (t.endsWith(".") || t.endsWith("!") || t.endsWith("?")) {
            t = t.substring(0, t.length() - 1).trim();
        }
        return t;
    }

    private String ensureSentence(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "";
        }
        String t = s.trim();
        char last = t.charAt(t.length() - 1);
        if (last != '.' && last != '!' && last != '?') {
            t = t + ".";
        }
        return t;
    }
}
