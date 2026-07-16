package com.whim.settlers.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Lightweight, time-decaying event log ("building finished", "under attack",
 * "out of trees", …). Messages fade after a few seconds; the most recent few are
 * drawn stacked near the top of the play area. Kept deliberately tiny — it is an
 * awareness aid, not a full message system.
 */
public final class Notifications {

    private static final float LIFETIME = 6f;   // seconds a message stays up
    private static final int   MAX_SHOWN = 5;

    private static final class Note {
        final String text;
        float age;
        Note(String text) { this.text = text; }
    }

    private final Deque<Note> notes = new ArrayDeque<Note>();
    private final Font font = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    /** Post a message (deduped against the newest to avoid spam). */
    public void push(String text) {
        if (text == null || text.isEmpty()) return;
        Note newest = notes.peekLast();
        if (newest != null && newest.text.equals(text) && newest.age < 1.5f) return;
        notes.addLast(new Note(text));
        while (notes.size() > 24) notes.pollFirst();
    }

    public void update(float dt) {
        for (Iterator<Note> it = notes.iterator(); it.hasNext(); ) {
            Note n = it.next();
            n.age += dt;
            if (n.age > LIFETIME) it.remove();
        }
    }

    public void render(Graphics2D g, int centreX, int topY) {
        g.setFont(font);
        int shown = 0;
        int y = topY;
        // Newest at the top.
        Note[] arr = notes.toArray(new Note[0]);
        for (int i = arr.length - 1; i >= 0 && shown < MAX_SHOWN; i--, shown++) {
            Note n = arr[i];
            float alpha = Math.max(0f, 1f - n.age / LIFETIME);
            int a = (int) (alpha * 235);
            int w = g.getFontMetrics().stringWidth(n.text) + 18;
            int x = centreX - w / 2;
            g.setColor(new Color(0, 0, 0, (int) (alpha * 150)));
            g.fillRoundRect(x, y, w, 20, 8, 8);
            g.setColor(new Color(255, 235, 170, a));
            g.drawString(n.text, x + 9, y + 14);
            y += 24;
        }
    }
}
