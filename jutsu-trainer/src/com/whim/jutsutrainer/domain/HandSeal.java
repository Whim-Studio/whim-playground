package com.whim.jutsutrainer.domain;

/**
 * Hand seals (印, In) used to mould chakra when performing a jutsu.
 *
 * <p>The first twelve constants are the basic seals of the Chinese zodiac, the
 * standard set ninja weave in sequence. The remaining constants cover the
 * unusual / improvised seals seen in the series: the crossed-finger Clone Seal,
 * the clasped-hand "clap" used for Summoning and Edo Tensei, the one-handed
 * variants pioneered by ninja such as Haku, and a sentinel for techniques that
 * require no formal seal at all.
 */
public enum HandSeal {
    // 12 Zodiac (basic)
    RAT("Rat", true),
    OX("Ox", true),
    TIGER("Tiger", true),
    HARE("Hare", true),
    DRAGON("Dragon", true),
    SNAKE("Snake", true),
    HORSE("Horse", true),
    RAM("Ram", true),
    MONKEY("Monkey", true),
    BIRD("Bird", true),
    DOG("Dog", true),
    BOAR("Boar", true),

    // Unusual / custom seals
    CLONE_SEAL("Clone Seal", false),    // crossed fingers (Shadow Clone etc.)
    CLAP("Clap", false),                // clasped/clapping hands (Summoning, Edo Tensei)
    HALF_RAM("Half Ram", false),        // one-handed Ram
    SNAKE_HALF("Snake (One-Handed)", false), // one-handed (Haku-style single-hand seals)
    HALF_TIGER("Half Tiger", false),    // one-handed Tiger
    NONE_FREEFORM("Freeform", false);   // technique cast with no formal seal / channeled

    private final String displayName;
    private final boolean zodiac;

    HandSeal(String displayName, boolean zodiac) {
        this.displayName = displayName;
        this.zodiac = zodiac;
    }

    /** Human-readable, proper-cased name, e.g. {@code RAT -> "Rat"}, {@code CLONE_SEAL -> "Clone Seal"}. */
    public String getDisplayName() {
        return displayName;
    }

    /** True only for the twelve basic Chinese-zodiac seals. */
    public boolean isZodiac() {
        return zodiac;
    }
}
