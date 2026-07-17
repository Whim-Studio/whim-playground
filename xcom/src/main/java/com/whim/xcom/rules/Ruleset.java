package com.whim.xcom.rules;

import java.util.Collection;

import com.whim.xcom.rules.def.AlienDef;
import com.whim.xcom.rules.def.ArmorDef;
import com.whim.xcom.rules.def.FacilityDef;
import com.whim.xcom.rules.def.ManufactureNode;
import com.whim.xcom.rules.def.ResearchNode;
import com.whim.xcom.rules.def.UfoDef;
import com.whim.xcom.rules.def.WeaponDef;
import com.whim.xcom.rules.model.AccuracyModel;
import com.whim.xcom.rules.model.DamageModel;
import com.whim.xcom.rules.model.PsiModel;
import com.whim.xcom.rules.model.ReactionModel;
import com.whim.xcom.rules.model.TimeUnitModel;

/**
 * The single seam between the engine and "the rules of the game". Everything the
 * simulation needs — content definitions AND the formula strategies — is fetched
 * from here, so a variant/mod is a matter of supplying a different {@code Ruleset}
 * (or loading a different data pack) with no engine changes.
 *
 * <p>Content is addressed by string id; the formula strategies are pluggable
 * {@link AccuracyModel} / {@link ReactionModel} / {@link DamageModel} /
 * {@link TimeUnitModel} implementations. {@link Ruleset1994} is the default.</p>
 */
public interface Ruleset {

    /** Human-readable name of this ruleset ("1994 (X-COM: UFO Defense)"). */
    String displayName();

    // ---- content registries -------------------------------------------------

    WeaponDef weapon(String id);

    ArmorDef armor(String id);

    AlienDef alien(String id);

    FacilityDef facility(String id);

    ResearchNode research(String id);

    ManufactureNode manufacture(String id);

    UfoDef ufo(String id);

    Collection<WeaponDef> weapons();

    Collection<ArmorDef> armors();

    Collection<AlienDef> aliens();

    Collection<FacilityDef> facilities();

    Collection<ResearchNode> researchTree();

    Collection<ManufactureNode> manufactureProjects();

    Collection<UfoDef> ufos();

    // ---- pluggable formula strategies ---------------------------------------

    AccuracyModel accuracy();

    ReactionModel reactions();

    DamageModel damage();

    TimeUnitModel timeUnits();

    PsiModel psi();
}
