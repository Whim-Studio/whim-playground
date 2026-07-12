package com.whim.capes.model;

/**
 * The ten moral Drives of Capes (rulebook pp.34-35).
 * <p>Heroic and Villainous Drives are mechanically identical; the split is
 * thematic. A detailed super-powered character picks any five of these,
 * assigning Strengths 1-5 that total exactly nine (p.74).
 */
public enum DriveType {
    // Heroic Drives (p.34)
    JUSTICE(Alignment.HEROIC, "How much the story revolves around laws, codes of conduct and rebellion."),
    TRUTH(Alignment.HEROIC, "How much the story revolves around identity, honesty and secrets."),
    LOVE(Alignment.HEROIC, "How much the story revolves around friends, rivals, and romance."),
    HOPE(Alignment.HEROIC, "How much the story revolves around the safety, needs and doubts of the common man."),
    DUTY(Alignment.HEROIC, "How much the story revolves around the responsibilities that they alone can and must fulfill."),

    // Villainous Drives (p.35)
    FEAR(Alignment.VILLAINOUS, "How much the story revolves around fear and bravery."),
    OBSESSION(Alignment.VILLAINOUS, "How much the story revolves around a single plan or theory they keep harping on."),
    PRIDE(Alignment.VILLAINOUS, "How much the story revolves around proving their superiority."),
    POWER(Alignment.VILLAINOUS, "How much the story revolves around dominating and being dominated."),
    DESPAIR(Alignment.VILLAINOUS, "How much the story revolves around destroying the hope of others.");

    public enum Alignment { HEROIC, VILLAINOUS }

    private final Alignment alignment;
    private final String blurb;

    DriveType(Alignment alignment, String blurb) {
        this.alignment = alignment;
        this.blurb = blurb;
    }

    public Alignment alignment() { return alignment; }
    public String blurb() { return blurb; }

    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
