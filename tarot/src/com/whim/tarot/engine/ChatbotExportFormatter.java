package com.whim.tarot.engine;

import com.whim.tarot.domain.Card;
import com.whim.tarot.domain.DrawnCard;
import com.whim.tarot.domain.SpreadPosition;

import java.util.List;

/**
 * Formats a dealt {@link Reading} into a single clean, copy-pasteable string for
 * handing to an external AI chatbot for deeper analysis.
 *
 * <p>The output captures everything the model needs to interpret the draw on its
 * own: the querent's question, the spread name, and — for each slot — the
 * position meaning, the card name, and its orientation. A one-line shape:
 *
 * <pre>
 * Question: ... | Spread: ... | Position 1: [Meaning] - [Card] (Upright) | ...
 * </pre>
 *
 * and a multi-line, more readable variant are both available.
 */
public final class ChatbotExportFormatter {

    public ChatbotExportFormatter() {
    }

    /** Compact single-line export, pipe-delimited, per the agreed contract. */
    public String formatLine(Reading reading) {
        if (reading == null) {
            throw new IllegalArgumentException("reading must not be null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(questionOrNone(reading));
        sb.append(" | Spread: ").append(reading.getSpreadType().getLabel());

        List<PositionedCard> cards = reading.getPositionedCards();
        for (int i = 0; i < cards.size(); i++) {
            PositionedCard pc = cards.get(i);
            SpreadPosition pos = pc.getPosition();
            DrawnCard dc = pc.getDrawnCard();
            Card card = dc.getCard();
            sb.append(" | Position ").append(i + 1).append(": ")
                    .append(pos.getName())
                    .append(" - ").append(card.getName())
                    .append(" (").append(orientation(dc)).append(")");
        }
        return sb.toString();
    }

    /**
     * Multi-line export suitable for pasting into a chat box: a header, one line
     * per position, and a closing instruction prompting the chatbot to interpret.
     */
    public String formatPrompt(Reading reading) {
        if (reading == null) {
            throw new IllegalArgumentException("reading must not be null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("I drew a Tarot reading and would like your interpretation.\n\n");
        sb.append("Question: ").append(questionOrNone(reading)).append("\n");
        sb.append("Spread: ").append(reading.getSpreadType().getLabel()).append("\n\n");

        List<PositionedCard> cards = reading.getPositionedCards();
        for (int i = 0; i < cards.size(); i++) {
            PositionedCard pc = cards.get(i);
            SpreadPosition pos = pc.getPosition();
            DrawnCard dc = pc.getDrawnCard();
            Card card = dc.getCard();
            sb.append("Position ").append(i + 1).append(" — ").append(pos.getName())
                    .append(" (").append(pos.getMeaning()).append("): ")
                    .append(card.getName()).append(" (").append(orientation(dc)).append(")\n");
        }
        sb.append("\nPlease synthesize these cards, in their positions and orientations, "
                + "into a cohesive reading that speaks to my question.");
        return sb.toString();
    }

    private String questionOrNone(Reading reading) {
        String q = reading.getQuestion();
        if (q == null || q.trim().isEmpty()) {
            return "(no specific question — open reading)";
        }
        return q.trim();
    }

    private String orientation(DrawnCard dc) {
        return dc.isReversed() ? "Reversed" : "Upright";
    }
}
