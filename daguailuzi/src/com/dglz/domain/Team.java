package com.dglz.domain;

/** Two alternating teams of three around the table. */
public enum Team {
    TEAM_A("Team A"), TEAM_B("Team B");

    private final String label;

    Team(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Even seats (0,2,4) = TEAM_A, odd seats (1,3,5) = TEAM_B. */
    public static Team forSeat(int seat) {
        return (seat % 2 == 0) ? TEAM_A : TEAM_B;
    }
}
