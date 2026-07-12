package com.whim.capes.engine;

import java.util.ArrayList;
import java.util.List;

import com.whim.capes.content.ClickLockModule;
import com.whim.capes.content.NonPersonTemplate;
import com.whim.capes.model.Ability;
import com.whim.capes.model.AbilityKind;
import com.whim.capes.model.Character;

/**
 * Swing-free construction helpers for both character-creation methods, so the
 * builder logic is unit-testable without the UI.
 *
 * <p>Click-and-Lock (p.80): combine a Power/Skill-Set with a Persona to get a
 * candidate pool of five Powers/Skills, five Attitudes and five Styles (three
 * Styles from the set, two from the Persona). Super/mundane is fixed here: a
 * Power-Set's Powers and Styles are super-powered; a Skill-Set's or Persona's
 * items are mundane (p.69). The player then crosses out three and ranks the
 * rest; {@link #renumberColumns(Character)} assigns the 1-up ranks.
 */
public final class CharacterFactory {
    private CharacterFactory() {}

    /**
     * Builds the candidate ability pool for Click-and-Lock. Scores are left at
     * 0 (unranked); the UI lets the player drop three and set the final order,
     * after which {@link #renumberColumns} produces legal 1-up numbering.
     *
     * @param primary a POWER_SET or SKILL_SET module
     * @param persona a PERSONA module
     */
    public static List<Ability> combine(ClickLockModule primary, ClickLockModule persona) {
        if (persona.type() != ClickLockModule.Type.PERSONA) {
            throw new IllegalMoveException("Second module must be a Persona.");
        }
        boolean superSet = primary.type() == ClickLockModule.Type.POWER_SET;
        AbilityKind primaryKind = superSet ? AbilityKind.POWER : AbilityKind.SKILL;

        List<Ability> pool = new ArrayList<Ability>();
        for (String n : primary.primary()) pool.add(new Ability(n, primaryKind, 0, superSet));
        for (String n : persona.primary()) pool.add(new Ability(n, AbilityKind.ATTITUDE, 0, false));
        // Styles: from the set inherit its super/mundane; from the Persona always mundane.
        for (String n : primary.styles()) pool.add(new Ability(n, AbilityKind.STYLE, 0, superSet));
        for (String n : persona.styles()) pool.add(new Ability(n, AbilityKind.STYLE, 0, false));
        return pool;
    }

    /**
     * Numbers each ability column 1..n in the order the abilities currently
     * appear on the character (p.80 "Number each category from one up"). Leaves
     * the character otherwise untouched. Returns the character for chaining.
     */
    public static Character renumberColumns(Character c) {
        renumber(c, AbilityKind.POWER);
        renumber(c, AbilityKind.SKILL);
        renumber(c, AbilityKind.ATTITUDE);
        renumber(c, AbilityKind.STYLE);
        return c;
    }

    private static void renumber(Character c, AbilityKind kind) {
        int i = 1;
        for (Ability a : c.abilities()) {
            if (a.kind() == kind) a.setScore(i++);
        }
    }

    /**
     * Convenience for Freeform (p.72): create an empty character of the given
     * type. Abilities and Drives are added by the caller; validation via
     * {@link Character#validateAbilityShape()} / {@link Character#validateDrives()}.
     */
    public static Character freeform(String id, String name, boolean superPowered) {
        return new Character(id, name, superPowered);
    }

    /**
     * Builds a Chapter 5 non-person participant (p.102): a mundane character
     * (no Powers, no Drives) whose Actions become Skills, plus Attitudes and
     * Styles, each column capped at five and numbered 1-up. Validation of the
     * standard 3-5 shape is skipped — non-persons may legitimately run with two
     * columns (p.102).
     */
    public static Character fromNonPerson(String id, NonPersonTemplate t) {
        Character c = new Character(id, t.name(), false);
        c.setNonPerson(true);
        c.setConcept(t.category().name().charAt(0) + t.category().name().substring(1).toLowerCase());
        addColumn(c, AbilityKind.SKILL, t.actions());
        addColumn(c, AbilityKind.ATTITUDE, t.attitudes());
        addColumn(c, AbilityKind.STYLE, t.styles());
        return c;
    }

    private static void addColumn(Character c, AbilityKind kind, java.util.List<String> names) {
        int rank = 1;
        for (String n : names) {
            if (rank > 5) break; // a column holds at most five (p.72)
            c.abilities().add(new Ability(n, kind, rank++, false));
        }
    }
}
