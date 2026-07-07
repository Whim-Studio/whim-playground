package com.whim.albion.magic;

import com.whim.albion.api.Content;
import com.whim.albion.api.Defs.SpellDef;
import com.whim.albion.api.Enums.SpellSchool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The spells a single caster knows, plus the set of magic {@link SpellSchool}s
 * their profession permits. {@link #canCast(SpellSchool)} is the profession gate
 * used by the UI and combat engine; individual spell readiness (SP / level /
 * talent) is evaluated by the owning character.
 */
public final class SpellBook {

    private final Content content;
    private final Set<SpellSchool> schools = new LinkedHashSet<SpellSchool>();
    private final Set<String> known = new LinkedHashSet<String>();

    public SpellBook(Content content, Set<SpellSchool> allowedSchools) {
        this.content = content;
        if (allowedSchools != null) this.schools.addAll(allowedSchools);
    }

    /** Profession gate: may this caster ever wield the given school? */
    public boolean canCast(SpellSchool school) {
        return schools.contains(school);
    }

    /** Learn a spell id (ignored if its school is not permitted or id unknown). */
    public boolean learn(String spellId) {
        SpellDef d = content.spell(spellId);
        if (d == null || !canCast(d.school)) return false;
        known.add(spellId);
        return true;
    }

    public boolean knows(String spellId) { return known.contains(spellId); }

    /** Known spell ids in learn order. */
    public List<String> knownIds() {
        return Collections.unmodifiableList(new ArrayList<String>(known));
    }

    /** Resolved known spell definitions (skips ids the content no longer defines). */
    public List<SpellDef> knownDefs() {
        List<SpellDef> out = new ArrayList<SpellDef>();
        for (String id : known) {
            SpellDef d = content.spell(id);
            if (d != null) out.add(d);
        }
        return out;
    }
}
