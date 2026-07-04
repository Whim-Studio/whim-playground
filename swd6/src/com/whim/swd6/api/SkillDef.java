package com.whim.swd6.api;

/**
 * Definition of a skill in the catalog: its name, the attribute it hangs off,
 * and whether it accepts specializations. This is reference data, not per-character
 * state (see {@link Skill} for the character's trained value).
 *
 * Owned by the orchestrator (api). Concrete instances are supplied by the rules
 * layer via {@link ContentProvider#skillCatalog()}.
 */
public final class SkillDef {

    private final String name;
    private final Attribute attribute;
    private final boolean allowsSpecialization;

    public SkillDef(String name, Attribute attribute, boolean allowsSpecialization) {
        this.name = name;
        this.attribute = attribute;
        this.allowsSpecialization = allowsSpecialization;
    }

    public String name() {
        return name;
    }

    public Attribute attribute() {
        return attribute;
    }

    public boolean allowsSpecialization() {
        return allowsSpecialization;
    }

    @Override
    public String toString() {
        return name;
    }
}
