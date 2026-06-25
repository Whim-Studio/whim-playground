package com.whim.tarot.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The available spread shapes, with their authentic ordered positions. */
public enum SpreadType {
    SINGLE("Daily Focus", 1),
    THREE_CARD("Past, Present, Future", 3),
    CELTIC_CROSS("Celtic Cross", 10);

    private final String label;
    private final int size;

    SpreadType(String label, int size) {
        this.label = label;
        this.size = size;
    }

    public String getLabel() { return label; }

    public int getCardCount() { return size; }

    /** Ordered positions for this spread; list size == getCardCount(). */
    public List<SpreadPosition> getPositions() {
        List<SpreadPosition> p = new ArrayList<SpreadPosition>();
        switch (this) {
            case SINGLE:
                p.add(new DefaultSpreadPosition(0, "Daily Focus",
                        "The single guiding theme, lesson, or energy to carry through the day."));
                break;
            case THREE_CARD:
                p.add(new DefaultSpreadPosition(0, "The Past",
                        "Roots of the situation: past events and influences still shaping it."));
                p.add(new DefaultSpreadPosition(1, "The Present",
                        "The current state of affairs and the energy at work right now."));
                p.add(new DefaultSpreadPosition(2, "The Future",
                        "The likely direction the situation is heading if the present course holds."));
                break;
            case CELTIC_CROSS:
                p.add(new DefaultSpreadPosition(0, "The Present / Significator",
                        "The heart of the matter — the querent and the present situation."));
                p.add(new DefaultSpreadPosition(1, "The Challenge (Crossing)",
                        "The crossing card: the obstacle or force directly opposing the present."));
                p.add(new DefaultSpreadPosition(2, "The Foundation / Past",
                        "The distant past and underlying basis on which the situation is built."));
                p.add(new DefaultSpreadPosition(3, "The Recent Past",
                        "Events just passing away, leaving the foreground of the matter."));
                p.add(new DefaultSpreadPosition(4, "The Crown / Possible Outcome",
                        "What may come — the best that can be achieved or the conscious goal."));
                p.add(new DefaultSpreadPosition(5, "The Near Future",
                        "The energy or events arriving next, in the immediate days ahead."));
                p.add(new DefaultSpreadPosition(6, "Self / Your Attitude",
                        "The querent's own stance, attitude, and role within the situation."));
                p.add(new DefaultSpreadPosition(7, "External Influences / Environment",
                        "People and circumstances in the surroundings affecting the matter."));
                p.add(new DefaultSpreadPosition(8, "Hopes & Fears",
                        "The querent's inner hopes and the fears that shadow them."));
                p.add(new DefaultSpreadPosition(9, "The Final Outcome",
                        "The culmination — where all these currents ultimately lead."));
                break;
            default:
                break;
        }
        return Collections.unmodifiableList(p);
    }
}
