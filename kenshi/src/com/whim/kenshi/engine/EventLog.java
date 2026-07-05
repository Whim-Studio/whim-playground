package com.whim.kenshi.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A bounded ring buffer of engine events (combat, deaths, orders). Owned by the
 * engine and confined to the tick thread; the {@link Snapshot} copies its
 * contents into immutable {@code LogView}s for the EDT. Keeping the log here (as
 * opposed to in the domain) keeps the engine's dependency on Task 1 minimal.
 */
final class EventLog {

    private static final int MAX_LINES = 200;

    private final Deque<Line> lines = new ArrayDeque<Line>();

    static final class Line {
        final long tick;
        final String text;
        Line(long tick, String text) {
            this.tick = tick;
            this.text = text;
        }
    }

    void add(long tick, String text) {
        lines.addLast(new Line(tick, text));
        while (lines.size() > MAX_LINES) {
            lines.removeFirst();
        }
    }

    /** Snapshot copy in chronological order (oldest first, most recent last). */
    List<Line> copy() {
        return new ArrayList<Line>(lines);
    }

    void clear() {
        lines.clear();
    }
}
