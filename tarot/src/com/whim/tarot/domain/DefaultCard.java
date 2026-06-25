package com.whim.tarot.domain;

/** Immutable concrete {@link Card}. */
public final class DefaultCard implements Card {
    private final int id;
    private final String name;
    private final Suit suit;
    private final int number;
    private final String uprightMeaning;
    private final String reversedMeaning;
    private final String description;
    private final String imageUrl;

    public DefaultCard(int id, String name, Suit suit, int number,
                       String uprightMeaning, String reversedMeaning,
                       String description, String imageUrl) {
        if (name == null || suit == null) {
            throw new IllegalArgumentException("name and suit must not be null");
        }
        this.id = id;
        this.name = name;
        this.suit = suit;
        this.number = number;
        this.uprightMeaning = uprightMeaning;
        this.reversedMeaning = reversedMeaning;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Suit getSuit() { return suit; }
    public int getNumber() { return number; }
    public String getUprightMeaning() { return uprightMeaning; }
    public String getReversedMeaning() { return reversedMeaning; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultCard)) return false;
        return id == ((DefaultCard) o).id;
    }

    @Override
    public int hashCode() { return id; }

    @Override
    public String toString() { return name + " (#" + id + ")"; }
}
