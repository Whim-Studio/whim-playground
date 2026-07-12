package com.whim.capes.content;

import java.util.Arrays;
import java.util.List;

/**
 * A Click-and-Lock module (Ch.4): a reusable fragment of a character sheet.
 * <ul>
 *   <li>A <b>Power-Set</b> / <b>Skill-Set</b> supplies its list of Powers (or
 *       Skills) plus a half-set of Styles.</li>
 *   <li>A <b>Persona</b> supplies Attitudes plus the complementary Styles.</li>
 * </ul>
 * Combining one of each yields five Powers/Skills, five Attitudes and five
 * Styles; the player crosses out three and ranks the rest (p.80). The full
 * catalogue captured from Ch.4 is populated in Phase 2; this class defines the
 * shape and {@link ClickLockData} seeds a couple of examples for the Phase 1
 * shell.
 */
public final class ClickLockModule {
    public enum Type { POWER_SET, SKILL_SET, PERSONA }

    private final String name;
    private final Type type;
    private final List<String> primary; // Powers (Power-Set), Skills (Skill-Set), or Attitudes (Persona)
    private final List<String> styles;

    public ClickLockModule(String name, Type type, List<String> primary, List<String> styles) {
        this.name = name;
        this.type = type;
        this.primary = primary;
        this.styles = styles;
    }

    public String name() { return name; }
    public Type type() { return type; }
    public List<String> primary() { return primary; }
    public List<String> styles() { return styles; }

    static List<String> list(String... items) { return Arrays.asList(items); }

    @Override public String toString() { return name + " (" + type + ")"; }
}
