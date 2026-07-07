package com.whim.albion.entities;

import com.whim.albion.api.Defs.SpellDef;
import com.whim.albion.api.Enums.SpellSchool;
import com.whim.albion.api.Views.SpellView;

/** Read-only projection of one known spell for a specific caster. */
final class CharacterSpellView implements SpellView {

    private final SpellDef def;
    private final Character owner;

    CharacterSpellView(SpellDef def, Character owner) {
        this.def = def;
        this.owner = owner;
    }

    @Override public String id() { return def.id; }
    @Override public String name() { return def.name; }
    @Override public SpellSchool school() { return def.school; }
    @Override public int spCost() { return def.spCost; }
    @Override public boolean castable() { return owner.canCastNow(def); }
    @Override public String description() { return def.description; }
}
