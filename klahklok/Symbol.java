package klahklok;

/**
 * The six symbols used on a Klah Klok die.
 */
public enum Symbol {
    TIGER("Tiger", "🐯"),
    GOURD("Gourd", "🍐"),
    ROOSTER("Rooster", "🐓"),
    FISH("Fish", "🐟"),
    CRAB("Crab", "🦀"),
    SHRIMP("Shrimp", "🦐");

    private final String displayName;
    private final String glyph;

    Symbol(String displayName, String glyph) {
        this.displayName = displayName;
        this.glyph = glyph;
    }

    /** @return the human-readable name of this symbol. */
    public String getDisplayName() {
        return displayName;
    }

    /** @return the emoji glyph representing this symbol. */
    public String getGlyph() {
        return glyph;
    }
}
