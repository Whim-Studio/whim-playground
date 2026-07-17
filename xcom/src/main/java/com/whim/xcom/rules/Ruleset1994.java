package com.whim.xcom.rules;

import java.io.IOException;
import java.util.Collection;

import com.whim.xcom.model.DamageType;
import com.whim.xcom.rules.data.DataAlienDef;
import com.whim.xcom.rules.data.DataArmorDef;
import com.whim.xcom.rules.data.DataFacilityDef;
import com.whim.xcom.rules.data.DataRulesetLoader;
import com.whim.xcom.rules.data.DataUfoDef;
import com.whim.xcom.rules.data.DataWeaponDef;
import com.whim.xcom.rules.data.RulesetData;
import com.whim.xcom.rules.def.AlienDef;
import com.whim.xcom.rules.def.ArmorDef;
import com.whim.xcom.rules.def.FacilityDef;
import com.whim.xcom.rules.def.ManufactureNode;
import com.whim.xcom.rules.def.ResearchNode;
import com.whim.xcom.rules.def.UfoDef;
import com.whim.xcom.rules.def.WeaponDef;
import com.whim.xcom.rules.model.AccuracyModel;
import com.whim.xcom.rules.model.DamageModel;
import com.whim.xcom.rules.model.ReactionModel;
import com.whim.xcom.rules.model.Ruleset1994Accuracy;
import com.whim.xcom.rules.model.Ruleset1994Damage;
import com.whim.xcom.rules.model.Ruleset1994Reactions;
import com.whim.xcom.rules.model.Ruleset1994TimeUnits;
import com.whim.xcom.rules.model.TimeUnitModel;
import com.whim.xcom.model.FireMode;

/**
 * The default 1994 ruleset. Content is loaded from the bundled data pack
 * {@code data/rules1994.json} when present; if that resource is missing it falls
 * back to a tiny built-in set so headless tests never depend on packaging.
 *
 * <p>The four formula strategies are the {@code Ruleset1994*} implementations, so
 * a variant can subclass this and swap any single model while keeping the rest.</p>
 */
public class Ruleset1994 implements Ruleset {

    private static final String DEFAULT_PACK = "data/rules1994.json";

    private final DefRegistry<WeaponDef> weapons = new DefRegistry<WeaponDef>("weapon");
    private final DefRegistry<ArmorDef> armors = new DefRegistry<ArmorDef>("armor");
    private final DefRegistry<AlienDef> aliens = new DefRegistry<AlienDef>("alien");
    private final DefRegistry<FacilityDef> facilities = new DefRegistry<FacilityDef>("facility");
    private final DefRegistry<ResearchNode> research = new DefRegistry<ResearchNode>("research");
    private final DefRegistry<ManufactureNode> manufacture = new DefRegistry<ManufactureNode>("manufacture");
    private final DefRegistry<UfoDef> ufos = new DefRegistry<UfoDef>("ufo");

    private final AccuracyModel accuracy = new Ruleset1994Accuracy();
    private final ReactionModel reactions = new Ruleset1994Reactions();
    private final DamageModel damage = new Ruleset1994Damage();
    private final TimeUnitModel timeUnits = new Ruleset1994TimeUnits();

    private String displayName = "1994 (X-COM: UFO Defense)";

    /**
     * Loads the bundled data pack, falling back to built-in defaults if the
     * resource is unavailable or unreadable. Never throws for missing content.
     */
    public static Ruleset1994 load() {
        Ruleset1994 rs = new Ruleset1994();
        try {
            RulesetData data = DataRulesetLoader.create().loadFromClasspath(DEFAULT_PACK);
            rs.applyData(data);
        } catch (IOException | RuntimeException e) {
            rs.applyBuiltInDefaults();
        }
        if (rs.weapons.size() == 0) {
            rs.applyBuiltInDefaults();
        }
        return rs;
    }

    /** Builds a ruleset purely from a supplied data pack (used for mods/tests). */
    public static Ruleset1994 fromData(RulesetData data) {
        Ruleset1994 rs = new Ruleset1994();
        rs.applyData(data);
        return rs;
    }

    protected Ruleset1994() {
    }

    private void applyData(RulesetData data) {
        if (data.displayName != null) {
            displayName = data.displayName;
        }
        for (WeaponDef w : data.weapons) weapons.add(w);
        for (ArmorDef a : data.armors) armors.add(a);
        for (AlienDef a : data.aliens) aliens.add(a);
        for (FacilityDef f : data.facilities) facilities.add(f);
        for (ResearchNode r : data.research) research.add(r);
        for (ManufactureNode m : data.manufacture) manufacture.add(m);
        for (UfoDef u : data.ufos) ufos.add(u);
    }

    /**
     * Minimal, hand-coded 1994 values used when no data pack is on the classpath.
     * These mirror the documented tables (see DESIGN.md) so the pure layer and its
     * tests are self-contained.
     */
    private void applyBuiltInDefaults() {
        // Rifle: Snap 60%/25%TU, Aimed 110%/80%TU, Auto 35%/35%TU (3 shots), power 30 AP.
        weapons.add(new DataWeaponDef("rifle", "Rifle", 30, DamageType.ARMOR_PIERCING,
                true, 8, 20, 60, 25, 110, 80, 35, 35, 3));
        // Pistol: Snap 60%/18%TU, Aimed 78%/30%TU, no auto, power 26 AP.
        weapons.add(new DataWeaponDef("pistol", "Pistol", 26, DamageType.ARMOR_PIERCING,
                false, 5, 12, 60, 18, 78, 30, -1, -1, 1));
        // Heavy Cannon (AP): Snap 60%/33%TU, Aimed 90%/80%TU, power 56 AP.
        weapons.add(new DataWeaponDef("heavy_cannon", "Heavy Cannon", 56, DamageType.ARMOR_PIERCING,
                true, 18, 6, 60, 33, 90, 80, -1, -1, 1));

        armors.add(new DataArmorDef("none", "None (jumpsuit)", 0, 0, 0, 0, null));
        armors.add(new DataArmorDef("personal_armor", "Personal Armour", 50, 40, 30, 30, null));

        aliens.add(new DataAlienDef("sectoid_soldier", "Sectoid Soldier",
                54, 90, 30, 63, 52, 30, 40, 5, 10));

        facilities.add(new DataFacilityDef("access_lift", "Access Lift", 300000, 1, 4000, 1, 0, 0, 0));
        facilities.add(new DataFacilityDef("living_quarters", "Living Quarters", 400000, 16, 10000, 1, 0, 0, 50));
        facilities.add(new DataFacilityDef("small_radar", "Small Radar System", 500000, 12, 10000, 1, 450, 10, 0));

        ufos.add(new DataUfoDef("small_scout", "Small Scout", 50, 2200, 0, 0, 40, 1, 3));
        ufos.add(new DataUfoDef("battleship", "Battleship", 3200, 5000, 148, 65, 60, 20, 30));
    }

    @Override public String displayName() { return displayName; }

    @Override public WeaponDef weapon(String id) { return weapons.get(id); }
    @Override public ArmorDef armor(String id) { return armors.get(id); }
    @Override public AlienDef alien(String id) { return aliens.get(id); }
    @Override public FacilityDef facility(String id) { return facilities.get(id); }
    @Override public ResearchNode research(String id) { return research.get(id); }
    @Override public ManufactureNode manufacture(String id) { return manufacture.get(id); }
    @Override public UfoDef ufo(String id) { return ufos.get(id); }

    @Override public Collection<WeaponDef> weapons() { return weapons.all(); }
    @Override public Collection<ArmorDef> armors() { return armors.all(); }
    @Override public Collection<AlienDef> aliens() { return aliens.all(); }
    @Override public Collection<FacilityDef> facilities() { return facilities.all(); }
    @Override public Collection<ResearchNode> researchTree() { return research.all(); }
    @Override public Collection<ManufactureNode> manufactureProjects() { return manufacture.all(); }
    @Override public Collection<UfoDef> ufos() { return ufos.all(); }

    @Override public AccuracyModel accuracy() { return accuracy; }
    @Override public ReactionModel reactions() { return reactions; }
    @Override public DamageModel damage() { return damage; }
    @Override public TimeUnitModel timeUnits() { return timeUnits; }

    /** Convenience for UI: is the given weapon+mode combination legal? */
    public boolean supports(String weaponId, FireMode mode) {
        return weapons.has(weaponId) && weapons.get(weaponId).supports(mode);
    }
}
