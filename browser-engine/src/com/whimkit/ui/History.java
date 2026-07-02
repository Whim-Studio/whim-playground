package com.whimkit.ui;

import java.util.ArrayList;
import java.util.List;

/** A per-tab back/forward navigation stack. Not shared across tabs. */
public final class History {

    private final List<String> entries = new ArrayList<String>();
    private int index = -1;

    /** Adds a new entry, discarding any forward history (standard browser behavior). */
    public void push(String url) {
        if (url == null) return;
        if (index >= 0 && url.equals(entries.get(index))) return; // no-op reload
        while (entries.size() > index + 1) entries.remove(entries.size() - 1);
        entries.add(url);
        index = entries.size() - 1;
    }

    public boolean canBack() { return index > 0; }
    public boolean canForward() { return index >= 0 && index < entries.size() - 1; }

    public String back() { if (canBack()) index--; return current(); }
    public String forward() { if (canForward()) index++; return current(); }

    public String current() {
        return (index >= 0 && index < entries.size()) ? entries.get(index) : null;
    }
}
