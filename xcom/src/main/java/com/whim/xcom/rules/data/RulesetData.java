package com.whim.xcom.rules.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain DTO mirroring the on-disk data-pack JSON. Gson deserializes into these
 * lists; {@link DataRulesetLoader} then registers them. Adding a weapon/alien/mod
 * is a matter of appending to the JSON — no code change.
 */
public final class RulesetData {

    public String displayName;
    public List<DataWeaponDef> weapons = new ArrayList<DataWeaponDef>();
    public List<DataArmorDef> armors = new ArrayList<DataArmorDef>();
    public List<DataAlienDef> aliens = new ArrayList<DataAlienDef>();
    public List<DataFacilityDef> facilities = new ArrayList<DataFacilityDef>();
    public List<DataResearchNode> research = new ArrayList<DataResearchNode>();
    public List<DataManufactureNode> manufacture = new ArrayList<DataManufactureNode>();
    public List<DataUfoDef> ufos = new ArrayList<DataUfoDef>();
}
