package com.whim.babylon5.domain;

/**
 * A card: an immutable printed definition plus a small amount of mutable
 * in-play physical state. The single-player engine treats each {@code Card}
 * instance as one physical card on the table, so {@code ready} and
 * {@code damage} describe that physical card's current condition.
 *
 * <p>The printed fields (id, name, type, faction, cost and the ability ratings)
 * never change once constructed. Use {@link com.whim.babylon5.data.CardDatabase#copyOf}
 * to obtain a fresh in-play instance with reset flags.</p>
 */
public final class Card {

    // ---- immutable printed definition ----
    private final String id;
    private final String name;
    private final CardType type;
    private final FactionId faction;
    private final int cost;
    private final int influence;
    private final int diplomacy;
    private final int intrigue;
    private final int psi;
    private final int military;
    private final String text;
    private final String imageUrl;

    // ---- mutable in-play physical state ----
    private boolean ready = true;
    private int damage = 0;

    public Card(String id, String name, CardType type, FactionId faction, int cost,
                int influence, int diplomacy, int intrigue, int psi, int military,
                String text, String imageUrl) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.faction = faction;
        this.cost = cost;
        this.influence = influence;
        this.diplomacy = diplomacy;
        this.intrigue = intrigue;
        this.psi = psi;
        this.military = military;
        this.text = text == null ? "" : text;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public CardType getType() { return type; }
    public FactionId getFaction() { return faction; }
    public int getCost() { return cost; }
    public int getInfluence() { return influence; }
    public int getDiplomacy() { return diplomacy; }
    public int getIntrigue() { return intrigue; }
    public int getPsi() { return psi; }
    public int getMilitary() { return military; }
    public String getText() { return text; }
    public String getImageUrl() { return imageUrl; }

    /**
     * The attribute used as support/opposition for the given conflict type.
     * (Rulebook "Conflict Cards": each conflict type is resolved using the
     * corresponding ability.)
     */
    public int support(ConflictType t) {
        if (t == null) return 0;
        switch (t) {
            case DIPLOMACY: return diplomacy;
            case INTRIGUE:  return intrigue;
            case PSI:       return psi;
            case MILITARY:  return military;
            default:        return 0;
        }
    }

    // ---- in-play physical state ----

    public boolean isReady() { return ready; }

    /** {@code false} == marked / exhausted / rotated (tapped). */
    public void setReady(boolean ready) { this.ready = ready; }

    public int getDamage() { return damage; }

    public void addDamage(int d) { this.damage += d; }

    public void clearDamage() { this.damage = 0; }

    @Override
    public String toString() {
        return name + " [" + id + "]";
    }
}
