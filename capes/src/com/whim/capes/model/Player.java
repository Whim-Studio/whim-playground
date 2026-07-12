package com.whim.capes.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A human player at the shared table (pass-and-play). Players sit in a fixed
 * clockwise order (their index in {@link GameState}). A player holds two
 * player-level resources — Story Tokens and Inspirations — and may control any
 * number of characters in a Scene (p.20). Debt is tracked per character, not
 * here.
 */
public final class Player implements java.io.Serializable {
    private final String id;
    private String name;
    private int storyTokens;
    private final List<Inspiration> inspirations = new ArrayList<Inspiration>();

    /** Characters this player is currently playing in the active Scene. */
    private final List<String> controlledCharacterIds = new ArrayList<String>();

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }

    public int storyTokens() { return storyTokens; }
    public void addStoryTokens(int n) { storyTokens += n; }
    public boolean spendStoryToken() {
        if (storyTokens <= 0) return false;
        storyTokens--;
        return true;
    }

    public List<Inspiration> inspirations() { return inspirations; }
    public List<String> controlledCharacterIds() { return controlledCharacterIds; }

    @Override public String toString() { return name + " (ST:" + storyTokens + ")"; }
}
