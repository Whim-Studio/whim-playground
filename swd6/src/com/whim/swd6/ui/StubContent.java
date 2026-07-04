package com.whim.swd6.ui;

import com.whim.swd6.api.Attribute;
import com.whim.swd6.api.Combatant;
import com.whim.swd6.api.ContentProvider;
import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.Equipment;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.Scenario;
import com.whim.swd6.api.Skill;
import com.whim.swd6.api.SkillDef;
import com.whim.swd6.api.Template;
import com.whim.swd6.api.Weapon;
import com.whim.swd6.api.Armor;

import java.util.ArrayList;
import java.util.List;

/**
 * DEV STUB, replaced by Main (GameContent) at runtime.
 *
 * A tiny original {@link ContentProvider} so the UI compiles and runs standalone
 * without Task 1. Provides a handful of skills, three templates, a few weapons /
 * armor / gear, and a minimal 4-scene branching scenario (skill check + combat +
 * decision + ending). All flavor is original.
 *
 * Owned by Task 3 (ui). Not shipped in the wired app.
 */
public final class StubContent implements ContentProvider {

    private final List<SkillDef> skills = new ArrayList<SkillDef>();
    private final List<Template> templates = new ArrayList<Template>();
    private final List<Weapon> weapons = new ArrayList<Weapon>();
    private final List<Armor> armor = new ArrayList<Armor>();
    private final List<Equipment> gear = new ArrayList<Equipment>();
    private final Scenario scenario;

    public StubContent() {
        buildSkills();
        buildGear();
        buildTemplates();
        this.scenario = buildScenario();
    }

    private void buildSkills() {
        // DEXTERITY
        skills.add(new SkillDef("blaster", Attribute.DEXTERITY, true));
        skills.add(new SkillDef("dodge", Attribute.DEXTERITY, false));
        skills.add(new SkillDef("melee combat", Attribute.DEXTERITY, true));
        skills.add(new SkillDef("brawling", Attribute.DEXTERITY, false));
        // KNOWLEDGE
        skills.add(new SkillDef("streetwise", Attribute.KNOWLEDGE, false));
        skills.add(new SkillDef("survival", Attribute.KNOWLEDGE, false));
        skills.add(new SkillDef("alien species", Attribute.KNOWLEDGE, false));
        // MECHANICAL
        skills.add(new SkillDef("starship piloting", Attribute.MECHANICAL, true));
        skills.add(new SkillDef("starship gunnery", Attribute.MECHANICAL, false));
        skills.add(new SkillDef("repulsorlift operation", Attribute.MECHANICAL, false));
        // PERCEPTION
        skills.add(new SkillDef("persuasion", Attribute.PERCEPTION, false));
        skills.add(new SkillDef("search", Attribute.PERCEPTION, false));
        skills.add(new SkillDef("sneak", Attribute.PERCEPTION, false));
        skills.add(new SkillDef("command", Attribute.PERCEPTION, false));
        // STRENGTH
        skills.add(new SkillDef("stamina", Attribute.STRENGTH, false));
        skills.add(new SkillDef("climbing/jumping", Attribute.STRENGTH, false));
        // TECHNICAL
        skills.add(new SkillDef("first aid", Attribute.TECHNICAL, false));
        skills.add(new SkillDef("security", Attribute.TECHNICAL, false));
        skills.add(new SkillDef("starship repair", Attribute.TECHNICAL, false));
    }

    private void buildGear() {
        weapons.add(rangedWeapon("Blaster Pistol", "blaster", "4D", 10, 30, 120, 500));
        weapons.add(rangedWeapon("Hold-out Blaster", "blaster", "3D", 4, 8, 12, 300));
        weapons.add(rangedWeapon("Blaster Rifle", "blaster", "5D", 30, 100, 300, 1000));
        weapons.add(meleeWeapon("Vibroblade", "melee combat", "5D", 250));
        weapons.add(meleeWeapon("Combat Staff", "melee combat", "STR+2", 75));

        armor.add(armorPiece("Padded Vest", "1D", "1D", 250));
        armor.add(armorPiece("Blast Vest & Helmet", "1D+1", "1D", 500));
        armor.add(armorPiece("Heavy Trooper Plate", "2D", "1D+2", 2000));

        gear.add(new Equipment("Comlink", 1, 25, "short-range voice"));
        gear.add(new Equipment("Medpac", 2, 100, "+1D to first aid"));
        gear.add(new Equipment("Datapad", 1, 75, "notes & slicing terminal"));
        gear.add(new Equipment("Glow Rod", 1, 10, "portable light"));
        gear.add(new Equipment("Security Kit", 1, 750, "+1D to security"));
        gear.add(new Equipment("Grappling Line", 1, 40, "20m synthrope"));
    }

    private void buildTemplates() {
        // 1) Smuggler-style scoundrel
        Template scoundrel = new Template();
        scoundrel.setName("Fringe Pilot");
        scoundrel.setDescription("A quick-talking freighter jockey who trusts a fast ship and a faster draw.");
        setAttrs(scoundrel, "3D+1", "3D", "3D+2", "3D", "2D+2", "2D+1"); // 54 pips = 18D
        scoundrel.getSuggestedSkills().add("blaster");
        scoundrel.getSuggestedSkills().add("starship piloting");
        scoundrel.getSuggestedSkills().add("dodge");
        scoundrel.getSuggestedSkills().add("streetwise");
        scoundrel.getStartingWeapons().add(weapons.get(0));
        scoundrel.getStartingGear().add(gear.get(0));
        scoundrel.getStartingGear().add(gear.get(2));
        scoundrel.setStartingCredits(1000);

        // 2) Tough soldier
        Template soldier = new Template();
        soldier.setName("Frontier Soldier");
        soldier.setDescription("A disciplined trooper who holds the line when everyone else runs.");
        setAttrs(soldier, "3D+2", "2D+1", "2D+2", "3D", "3D+2", "2D+2"); // 54 pips = 18D
        soldier.getSuggestedSkills().add("blaster");
        soldier.getSuggestedSkills().add("dodge");
        soldier.getSuggestedSkills().add("brawling");
        soldier.getSuggestedSkills().add("command");
        soldier.getStartingWeapons().add(weapons.get(2));
        soldier.getStartingGear().add(gear.get(0));
        soldier.setStartingCredits(500);

        // 3) Force-sensitive wanderer
        Template mystic = new Template();
        mystic.setName("Wandering Adept");
        mystic.setDescription("A seeker attuned to the living energy that binds the galaxy together.");
        mystic.setForceSensitive(true);
        setAttrs(mystic, "2D+2", "3D", "3D", "3D+1", "2D+2", "3D+1"); // 54 pips = 18D
        mystic.getSuggestedSkills().add("melee combat");
        mystic.getSuggestedSkills().add("survival");
        mystic.getSuggestedSkills().add("persuasion");
        mystic.getStartingWeapons().add(weapons.get(3));
        mystic.getStartingGear().add(gear.get(3));
        mystic.setStartingCredits(200);

        templates.add(scoundrel);
        templates.add(soldier);
        templates.add(mystic);
    }

    private void setAttrs(Template t, String dex, String kno, String mec, String per, String str, String tec) {
        t.getAttributes().put(Attribute.DEXTERITY, DiceCode.parse(dex));
        t.getAttributes().put(Attribute.KNOWLEDGE, DiceCode.parse(kno));
        t.getAttributes().put(Attribute.MECHANICAL, DiceCode.parse(mec));
        t.getAttributes().put(Attribute.PERCEPTION, DiceCode.parse(per));
        t.getAttributes().put(Attribute.STRENGTH, DiceCode.parse(str));
        t.getAttributes().put(Attribute.TECHNICAL, DiceCode.parse(tec));
    }

    private Weapon rangedWeapon(String name, String skill, String dmg, int s, int m, int l, int cost) {
        Weapon w = new Weapon();
        w.setName(name);
        w.setSkill(skill);
        w.setDamage(parseDamage(dmg));
        w.setShortRange(s);
        w.setMediumRange(m);
        w.setLongRange(l);
        w.setCost(cost);
        return w;
    }

    private Weapon meleeWeapon(String name, String skill, String dmg, int cost) {
        Weapon w = new Weapon();
        w.setName(name);
        w.setSkill(skill);
        w.setDamage(parseDamage(dmg));
        w.setMelee(true);
        w.setCost(cost);
        return w;
    }

    private Armor armorPiece(String name, String phys, String energy, int cost) {
        Armor a = new Armor();
        a.setName(name);
        a.setPhysicalBonus(DiceCode.parse(phys));
        a.setEnergyBonus(DiceCode.parse(energy));
        a.setCost(cost);
        return a;
    }

    /** Accepts "4D" style codes; "STR+2" degrades gracefully to a flat 4D fallback. */
    private DiceCode parseDamage(String s) {
        try {
            return DiceCode.parse(s);
        } catch (RuntimeException ex) {
            return DiceCode.parse("4D");
        }
    }

    // ---- the minimal scenario: NARRATIVE -> SKILL_CHECK -> COMBAT -> DECISION -> ENDING ----
    private Scenario buildScenario() {
        Scenario sc = new Scenario();
        sc.setTitle("Signal in the Dust");
        sc.setSynopsis("A distress beacon pulls your crew down onto a wind-scoured moon.");
        sc.setStartSceneId("intro");

        Scenario.Scene intro = new Scenario.Scene();
        intro.setId("intro");
        intro.setTitle("Dust and Silence");
        intro.setType(Scenario.SceneType.NARRATIVE);
        intro.setText("Your ship settles onto a ridge of grey dust. A repeating distress pulse "
                + "leads to a half-buried transport ahead. The hatch is jammed and the wind is rising.");
        intro.getChoices().add(new Scenario.Choice("Approach the wreck", "hatch"));
        sc.getScenes().add(intro);

        Scenario.Scene hatch = new Scenario.Scene();
        hatch.setId("hatch");
        hatch.setTitle("The Jammed Hatch");
        hatch.setType(Scenario.SceneType.SKILL_CHECK);
        hatch.setText("Corrosion has fused the hatch controls. You can try to slice the lock open "
                + "before the sandstorm buries the transport for good.");
        hatch.setSkillName("security");
        hatch.setTargetNumber(12);
        hatch.setSuccessNext("ambush");
        hatch.setFailureNext("ambush"); // either way something is waiting inside
        sc.getScenes().add(hatch);

        Scenario.Scene ambush = new Scenario.Scene();
        ambush.setId("ambush");
        ambush.setTitle("Not Alone");
        ambush.setType(Scenario.SceneType.COMBAT);
        ambush.setText("The hatch grinds open. Two scavengers who set the false beacon lunge from the dark hold, "
                + "vibroblades drawn.");
        ambush.getEnemies().add(npc("Scavenger", "3D", "4D", "2D"));
        ambush.getEnemies().add(npc("Scav Boss", "3D+2", "4D+1", "2D+1"));
        ambush.setVictoryNext("choice");
        ambush.setDefeatNext("badEnd");
        sc.getScenes().add(ambush);

        Scenario.Scene choice = new Scenario.Scene();
        choice.setId("choice");
        choice.setTitle("The Beacon");
        choice.setType(Scenario.SceneType.DECISION);
        choice.setText("With the scavengers down, you find their signal rig — and a cargo of stolen medical supplies "
                + "bound for a struggling colony. Do you return the cargo, or keep it?");
        choice.getChoices().add(new Scenario.Choice("Return the supplies to the colony", "goodEnd"));
        choice.getChoices().add(new Scenario.Choice("Keep the cargo and sell it", "greyEnd"));
        sc.getScenes().add(choice);

        Scenario.Scene goodEnd = new Scenario.Scene();
        goodEnd.setId("goodEnd");
        goodEnd.setTitle("A Debt Repaid");
        goodEnd.setType(Scenario.SceneType.ENDING);
        goodEnd.setText("The colony's gratitude spreads through the sector. You leave richer in something "
                + "credits can't buy — a name people trust.");
        sc.getScenes().add(goodEnd);

        Scenario.Scene greyEnd = new Scenario.Scene();
        greyEnd.setId("greyEnd");
        greyEnd.setTitle("A Full Hold");
        greyEnd.setType(Scenario.SceneType.ENDING);
        greyEnd.setText("The supplies fetch a fine price at the next port. Your hold is full, your conscience less so, "
                + "and somewhere a colony waits for a shipment that never comes.");
        sc.getScenes().add(greyEnd);

        Scenario.Scene badEnd = new Scenario.Scene();
        badEnd.setId("badEnd");
        badEnd.setTitle("Buried in the Dust");
        badEnd.setType(Scenario.SceneType.ENDING);
        badEnd.setText("The scavengers were faster than they looked. If your crew is lucky, they drag you out "
                + "before the storm finishes what the blades started.");
        sc.getScenes().add(badEnd);

        return sc;
    }

    private Combatant npc(String name, String atk, String dmg, String res) {
        Combatant c = new Combatant();
        c.setName(name);
        c.setPlayerCharacter(false);
        c.setAttackCode(DiceCode.parse(atk));
        c.setDamageCode(DiceCode.parse(dmg));
        c.setResistCode(DiceCode.parse(res));
        return c;
    }

    @Override public List<SkillDef> skillCatalog() { return skills; }
    @Override public List<Template> templates() { return templates; }
    @Override public List<Weapon> weapons() { return weapons; }
    @Override public List<Armor> armorCatalog() { return armor; }
    @Override public List<Equipment> equipmentCatalog() { return gear; }
    @Override public Scenario scenario() { return scenario; }

    @Override
    public PlayerCharacter instantiate(Template template) {
        PlayerCharacter pc = new PlayerCharacter();
        pc.setTemplateName(template.getName());
        pc.setBackground(template.getDescription());
        for (Attribute a : Attribute.values()) {
            DiceCode d = template.getAttributes().get(a);
            pc.setAttribute(a, d == null ? DiceCode.parse("2D") : d);
        }
        pc.setForceSensitive(template.isForceSensitive());
        pc.setMove(template.getMove());
        pc.setCredits(template.getStartingCredits());
        for (String sname : template.getSuggestedSkills()) {
            SkillDef def = findSkill(sname);
            if (def != null) {
                pc.getSkills().add(new Skill(def.name(), def.attribute(), DiceCode.ZERO));
            }
        }
        pc.getWeapons().addAll(template.getStartingWeapons());
        pc.getGear().addAll(template.getStartingGear());
        return pc;
    }

    private SkillDef findSkill(String name) {
        for (SkillDef d : skills) {
            if (d.name().equalsIgnoreCase(name)) {
                return d;
            }
        }
        return null;
    }
}
