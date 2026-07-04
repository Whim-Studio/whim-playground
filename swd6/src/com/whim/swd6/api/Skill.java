package com.whim.swd6.api;

import java.util.ArrayList;
import java.util.List;

/**
 * A character's trained value in one skill. Stores only the dice <b>added</b> on
 * top of the governing attribute; the effective code is computed by the model as
 * (attribute code + added). Untrained skills simply have {@code added == ZERO} and
 * default to the raw attribute.
 *
 * Owned by the orchestrator (api). Mutable per-character state.
 */
public final class Skill {

    private String name;            // matches a SkillDef name
    private Attribute attribute;    // governing attribute
    private DiceCode added;         // dice added over the attribute
    private final List<Specialization> specializations = new ArrayList<Specialization>();

    public Skill() {
        this.name = "";
        this.attribute = Attribute.DEXTERITY;
        this.added = DiceCode.ZERO;
    }

    public Skill(String name, Attribute attribute, DiceCode added) {
        this.name = name;
        this.attribute = attribute;
        this.added = added == null ? DiceCode.ZERO : added;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    public DiceCode getAdded() {
        return added;
    }

    public void setAdded(DiceCode added) {
        this.added = added == null ? DiceCode.ZERO : added;
    }

    public List<Specialization> getSpecializations() {
        return specializations;
    }
}
