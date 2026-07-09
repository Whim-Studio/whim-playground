package com.whim.samurai.model;

/** The four seasons; time on the strategic layer advances one season per step. */
public enum Season {
    SPRING("Spring", "Haru"),
    SUMMER("Summer", "Natsu"),
    AUTUMN("Autumn", "Aki"),
    WINTER("Winter", "Fuyu");

    public final String en;
    public final String jp;
    Season(String en, String jp) { this.en = en; this.jp = jp; }

    public Season next() { return values()[(ordinal() + 1) % 4]; }
    public boolean rolloverToNext() { return this == WINTER; }
}
