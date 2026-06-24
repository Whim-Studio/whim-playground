package com.dglz.domain;

import java.util.List;

/** A seated player with a mutable hand. */
public final class Player {
    private final int seat;
    private final String name;
    private final boolean human;
    private final Team team;
    private final List<Card> hand;

    public Player(int seat, String name, boolean human, Team team, List<Card> hand) {
        this.seat = seat;
        this.name = name;
        this.human = human;
        this.team = team;
        this.hand = hand;
    }

    public int seat() {
        return seat;
    }

    public String name() {
        return name;
    }

    public boolean isHuman() {
        return human;
    }

    public Team team() {
        return team;
    }

    public List<Card> hand() {
        return hand;
    }

    public int cardCount() {
        return hand.size();
    }

    public boolean isOut() {
        return hand.isEmpty();
    }
}
