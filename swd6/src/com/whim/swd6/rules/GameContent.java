package com.whim.swd6.rules;

import com.whim.swd6.api.Armor;
import com.whim.swd6.api.Attribute;
import com.whim.swd6.api.Combatant;
import com.whim.swd6.api.ContentProvider;
import com.whim.swd6.api.CreationRules;
import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.DifficultyTier;
import com.whim.swd6.api.Equipment;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.Scenario;
import com.whim.swd6.api.Skill;
import com.whim.swd6.api.SkillDef;
import com.whim.swd6.api.Template;
import com.whim.swd6.api.Weapon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * All static game content for the D6 recreation: the Revised &amp; Expanded skill
 * catalog, six original character templates, the weapon / armor / equipment
 * catalogs, and an original bundled test adventure.
 *
 * Original flavor text throughout — no copyrighted rulebook or published-module
 * text, no Lucasfilm/Disney IP. Skill names and stat mechanics are generic system
 * vocabulary; all prose is written fresh for this project.
 */
public final class GameContent implements ContentProvider {

    /**
     * Skill names that accept specializations (combat and craft/repair skills, plus
     * the vehicle-operation family). Kept as a name set so the catalog builder can
     * flip the flag consistently.
     */
    private static final Set<String> SPECIALIZABLE = new HashSet<String>(Arrays.asList(
            // Dexterity combat
            "blaster", "brawling parry", "dodge", "grenade", "melee combat",
            "melee parry", "thrown weapons", "vehicle blasters",
            // Mechanical operation / gunnery
            "astrogation", "beast riding", "communications", "ground vehicle operation",
            "repulsorlift operation", "sensors", "space transports",
            "starfighter piloting", "starship gunnery", "starship shields",
            // Strength combat
            "brawling",
            // Technical craft / repair
            "armor repair", "blaster repair", "computer programming/repair",
            "demolitions", "droid programming/repair", "first aid", "security"
    ));

    // ------------------------------------------------------------------
    // Skill catalog
    // ------------------------------------------------------------------

    @Override
    public List<SkillDef> skillCatalog() {
        List<SkillDef> out = new ArrayList<SkillDef>();

        addSkills(out, Attribute.DEXTERITY,
                "blaster", "brawling parry", "dodge", "grenade", "melee combat",
                "melee parry", "pick pocket", "running", "thrown weapons", "vehicle blasters");

        addSkills(out, Attribute.KNOWLEDGE,
                "alien species", "bureaucracy", "business", "cultures", "intimidation",
                "languages", "planetary systems", "streetwise", "survival", "value", "willpower");

        addSkills(out, Attribute.MECHANICAL,
                "astrogation", "beast riding", "communications", "ground vehicle operation",
                "repulsorlift operation", "sensors", "space transports", "starfighter piloting",
                "starship gunnery", "starship shields");

        addSkills(out, Attribute.PERCEPTION,
                "bargain", "command", "con", "gambling", "hide", "investigation",
                "persuasion", "search", "sneak");

        addSkills(out, Attribute.STRENGTH,
                "brawling", "climbing/jumping", "lifting", "stamina", "swimming");

        addSkills(out, Attribute.TECHNICAL,
                "armor repair", "blaster repair", "computer programming/repair", "demolitions",
                "droid programming/repair", "first aid", "security");

        return out;
    }

    private static void addSkills(List<SkillDef> out, Attribute attr, String... names) {
        for (String n : names) {
            out.add(new SkillDef(n, attr, SPECIALIZABLE.contains(n)));
        }
    }

    // ------------------------------------------------------------------
    // Templates (6 original archetypes, each 18D of attributes = 54 pips)
    // ------------------------------------------------------------------

    @Override
    public List<Template> templates() {
        List<Template> out = new ArrayList<Template>();
        out.add(fringeSmuggler());
        out.add(exImperialSoldier());
        out.add(alienBountyHunter());
        out.add(minorForceAdept());
        out.add(techSlicer());
        out.add(wanderingBrawler());
        return out;
    }

    private Template fringeSmuggler() {
        Template t = new Template();
        t.setName("Fringe Smuggler");
        t.setDescription("You run cargo the customs droids would rather not scan. A fast "
                + "ship, a faster mouth, and a talent for being somewhere else when the "
                + "blaster bolts start flying keep you one jump ahead of your debts.");
        setAttrs(t, "3D", "2D", "4D", "3D+1", "2D+2", "3D");
        t.getSuggestedSkills().addAll(Arrays.asList(
                "blaster", "dodge", "con", "space transports", "astrogation", "starship gunnery", "bargain"));
        t.getStartingWeapons().add(holdOutBlaster());
        t.getStartingGear().add(new Equipment("Comlink", 1, 25, "Short-range encrypted handset"));
        t.getStartingGear().add(new Equipment("Datapad", 1, 75, "Forged manifests, mostly"));
        t.setStartingCredits(1200);
        t.setMove(10);
        return t;
    }

    private Template exImperialSoldier() {
        Template t = new Template();
        t.setName("Ex-Imperial Soldier");
        t.setDescription("You wore the grey once and followed orders until one order was one "
                + "too many. Now your discipline and drill are for hire — and the old uniform "
                + "is buried at the bottom of a canyon on some backwater moon.");
        setAttrs(t, "3D+2", "2D+2", "3D", "2D+1", "3D+1", "3D");
        t.getSuggestedSkills().addAll(Arrays.asList(
                "blaster", "brawling", "dodge", "command", "grenade", "melee combat", "stamina"));
        t.getStartingWeapons().add(heavyBlasterPistol());
        t.getStartingGear().add(new Equipment("Blast Vest", 1, 300, "Kept from the service"));
        t.getStartingGear().add(new Equipment("Field Rations", 5, 10, "Three days each"));
        t.setStartingCredits(700);
        t.setMove(10);
        return t;
    }

    private Template alienBountyHunter() {
        Template t = new Template();
        t.setName("Alien Bounty Hunter");
        t.setDescription("Your homeworld is a rumor to most of the beings you hunt, and that "
                + "suits you. You read a target the way others read a starchart — patiently, "
                + "completely, and only once. Payment on delivery, alive preferred.");
        setAttrs(t, "4D", "2D", "2D+1", "3D+1", "3D", "3D+1");
        t.getSuggestedSkills().addAll(Arrays.asList(
                "blaster", "melee combat", "search", "hide", "sneak", "intimidation", "survival"));
        t.getStartingWeapons().add(blasterRifle());
        t.getStartingWeapons().add(vibroblade());
        t.getStartingGear().add(new Equipment("Macrobinoculars", 1, 100, "Low-light optics"));
        t.getStartingGear().add(new Equipment("Binder Cuffs", 2, 15, "For the ones worth more alive"));
        t.setStartingCredits(500);
        t.setMove(11);
        return t;
    }

    private Template minorForceAdept() {
        Template t = new Template();
        t.setName("Minor Force Adept");
        t.setDescription("You feel the current beneath the surface of things — a nudge, a "
                + "warning, a stillness others cannot name. No temple trained you; a wandering "
                + "hermit only told you to listen. So you listen, and you try not to be afraid.");
        setAttrs(t, "2D+2", "3D", "2D+1", "4D", "2D", "4D");
        t.setForceSensitive(true);
        t.getSuggestedSkills().addAll(Arrays.asList(
                "dodge", "melee combat", "willpower", "persuasion", "investigation", "survival", "alien species"));
        t.getStartingWeapons().add(vibroblade());
        t.getStartingGear().add(new Equipment("Traveler's Cloak", 1, 40, "Homespun, weatherproofed"));
        t.getStartingGear().add(new Equipment("Medpac", 1, 100, "Two uses left"));
        t.setStartingCredits(300);
        t.setMove(10);
        return t;
    }

    private Template techSlicer() {
        Template t = new Template();
        t.setName("Tech Slicer");
        t.setDescription("Locked doors are a suggestion; encrypted networks are an invitation. "
                + "You would rather talk to a droid than a person and you have never met a "
                + "security grid you could not eventually charm, bribe, or simply outthink.");
        setAttrs(t, "2D+1", "3D+1", "3D", "3D", "2D+1", "4D");
        t.getSuggestedSkills().addAll(Arrays.asList(
                "computer programming/repair", "security", "droid programming/repair",
                "blaster repair", "dodge", "con", "investigation"));
        t.getStartingWeapons().add(holdOutBlaster());
        t.getStartingGear().add(new Equipment("Tool Kit", 1, 200, "Micro-drivers and probes"));
        t.getStartingGear().add(new Equipment("Datapad", 1, 75, "Loaded with slicer utilities"));
        t.getStartingGear().add(new Equipment("Code Cylinder", 1, 50, "Cracked, semi-functional"));
        t.setStartingCredits(900);
        t.setMove(10);
        return t;
    }

    private Template wanderingBrawler() {
        Template t = new Template();
        t.setName("Wandering Brawler");
        t.setDescription("Cantina to cantina, world to world, you settle arguments the honest "
                + "way — with your fists and an unbreakable grin. You own what you can carry and "
                + "owe money on three planets, but nobody has ever called you a coward.");
        setAttrs(t, "3D+2", "2D", "2D+1", "3D+1", "4D", "2D+2");
        t.getSuggestedSkills().addAll(Arrays.asList(
                "brawling", "brawling parry", "dodge", "lifting", "stamina", "intimidation", "climbing/jumping"));
        t.getStartingWeapons().add(knife());
        t.getStartingGear().add(new Equipment("Bottle of Spiced Ale", 2, 8, "Courage, occasionally a weapon"));
        t.getStartingGear().add(new Equipment("Worn Jacket", 1, 20, "More patches than jacket"));
        t.setStartingCredits(150);
        t.setMove(10);
        return t;
    }

    /** Assign the six attribute codes in Attribute order; validates the 54-pip total. */
    private static void setAttrs(Template t, String dex, String kno, String mec,
                                 String per, String str, String tec) {
        t.getAttributes().put(Attribute.DEXTERITY, DiceCode.parse(dex));
        t.getAttributes().put(Attribute.KNOWLEDGE, DiceCode.parse(kno));
        t.getAttributes().put(Attribute.MECHANICAL, DiceCode.parse(mec));
        t.getAttributes().put(Attribute.PERCEPTION, DiceCode.parse(per));
        t.getAttributes().put(Attribute.STRENGTH, DiceCode.parse(str));
        t.getAttributes().put(Attribute.TECHNICAL, DiceCode.parse(tec));
        int total = CreationRules.totalAttributePips(t.getAttributes());
        if (total != CreationRules.ATTRIBUTE_PIPS_TOTAL) {
            throw new IllegalStateException("Template '" + t.getName() + "' attributes sum to "
                    + total + " pips, expected " + CreationRules.ATTRIBUTE_PIPS_TOTAL);
        }
        for (Attribute a : Attribute.values()) {
            if (!CreationRules.attributeInRange(t.getAttributes().get(a))) {
                throw new IllegalStateException("Template '" + t.getName() + "' attribute " + a
                        + " out of 2D-4D range");
            }
        }
    }

    // ------------------------------------------------------------------
    // Weapon catalog
    // ------------------------------------------------------------------

    @Override
    public List<Weapon> weapons() {
        List<Weapon> out = new ArrayList<Weapon>();
        out.add(holdOutBlaster());
        out.add(blasterPistol());
        out.add(heavyBlasterPistol());
        out.add(blasterRifle());
        out.add(vibroblade());
        out.add(knife());
        out.add(fragGrenade());
        return out;
    }

    private static Weapon rangedWeapon(String name, String skill, String damage,
                                       int sRange, int mRange, int lRange, int cost, String notes) {
        Weapon w = new Weapon();
        w.setName(name);
        w.setSkill(skill);
        w.setDamage(DiceCode.parse(damage));
        w.setMelee(false);
        w.setShortRange(sRange);
        w.setMediumRange(mRange);
        w.setLongRange(lRange);
        w.setShortDifficulty(DifficultyTier.VERY_EASY);
        w.setMediumDifficulty(DifficultyTier.EASY);
        w.setLongDifficulty(DifficultyTier.MODERATE);
        w.setCost(cost);
        w.setNotes(notes);
        return w;
    }

    private static Weapon meleeWeapon(String name, String skill, String damage, int cost, String notes) {
        Weapon w = new Weapon();
        w.setName(name);
        w.setSkill(skill);
        w.setDamage(DiceCode.parse(damage));
        w.setMelee(true);
        w.setShortDifficulty(DifficultyTier.VERY_EASY);
        w.setMediumDifficulty(DifficultyTier.VERY_EASY);
        w.setLongDifficulty(DifficultyTier.VERY_EASY);
        w.setCost(cost);
        w.setNotes(notes);
        return w;
    }

    private Weapon holdOutBlaster() {
        return rangedWeapon("Hold-out Blaster", "blaster", "3D", 3, 8, 15, 300,
                "Palm-sized; easy to conceal, short on range");
    }

    private Weapon blasterPistol() {
        return rangedWeapon("Blaster Pistol", "blaster", "4D", 10, 30, 120, 500,
                "The workhorse sidearm of the outer systems");
    }

    private Weapon heavyBlasterPistol() {
        return rangedWeapon("Heavy Blaster Pistol", "blaster", "5D", 10, 35, 150, 750,
                "Overbuilt and unsubtle; drains power packs fast");
    }

    private Weapon blasterRifle() {
        return rangedWeapon("Blaster Rifle", "blaster", "5D", 30, 100, 300, 1000,
                "Two-handed, folding stock, dependable");
    }

    private Weapon vibroblade() {
        return meleeWeapon("Vibroblade", "melee combat", "2D", 250,
                "Ultrasonic edge; damage adds to the wielder's Strength");
    }

    private Weapon knife() {
        return meleeWeapon("Survival Knife", "melee combat", "1D", 25,
                "Plain durasteel; damage adds to the wielder's Strength");
    }

    private Weapon fragGrenade() {
        Weapon w = rangedWeapon("Frag Grenade", "grenade", "5D", 5, 10, 20, 200,
                "Thrown; blast radius punishes tight formations");
        return w;
    }

    // ------------------------------------------------------------------
    // Armor catalog
    // ------------------------------------------------------------------

    @Override
    public List<Armor> armorCatalog() {
        List<Armor> out = new ArrayList<Armor>();
        out.add(armor("Blast Vest", "+1D", "+1", "+0", 300, "Padded torso protection, easy to wear under clothes"));
        out.add(armor("Blast Vest and Helmet", "+1D+1", "+1D", "+0", 450, "Adds head coverage for a small mobility cost"));
        out.add(armor("Reinforced Fatigues", "+1", "+1", "+0", 120, "Woven ballistic cloth; better than nothing"));
        out.add(armor("Combat Jumpsuit", "+1D+2", "+1D", "+1", 800, "Full-body plating; slightly restricts movement"));
        out.add(armor("Heavy Battle Armor", "+2D", "+2D", "+1D", 2500, "Sealed plate; heavy enough to slow a sprint"));
        return out;
    }

    private static Armor armor(String name, String physical, String energy, String penalty, int cost, String notes) {
        Armor a = new Armor();
        a.setName(name);
        a.setPhysicalBonus(DiceCode.parse(normalizeBonus(physical)));
        a.setEnergyBonus(DiceCode.parse(normalizeBonus(energy)));
        a.setDexPenalty(DiceCode.parse(normalizeBonus(penalty)));
        a.setCost(cost);
        a.setNotes(notes);
        return a;
    }

    /** Turn a "+1D+1" / "+1" / "+0" shorthand into a parseable dice code string. */
    private static String normalizeBonus(String s) {
        String v = s.trim();
        if (v.startsWith("+")) {
            v = v.substring(1);
        }
        if (v.indexOf('D') < 0) {
            // pure pip modifier like "1" or "0"
            return "0D+" + v;
        }
        return v;
    }

    // ------------------------------------------------------------------
    // Equipment catalog
    // ------------------------------------------------------------------

    @Override
    public List<Equipment> equipmentCatalog() {
        List<Equipment> out = new ArrayList<Equipment>();
        out.add(new Equipment("Comlink", 1, 25, "Short-range encrypted handset"));
        out.add(new Equipment("Datapad", 1, 75, "Portable computer and reader"));
        out.add(new Equipment("Medpac", 1, 100, "Field first-aid kit, several uses"));
        out.add(new Equipment("Macrobinoculars", 1, 100, "Magnifying, low-light optics"));
        out.add(new Equipment("Glow Rod", 1, 10, "Rugged handheld lamp"));
        out.add(new Equipment("Tool Kit", 1, 200, "General repair drivers and probes"));
        out.add(new Equipment("Breath Mask", 1, 200, "Filters thin or foul atmospheres"));
        out.add(new Equipment("Grappling Line", 1, 40, "20m synthrope with magnetic hook"));
        out.add(new Equipment("Ration Pack", 1, 5, "Two days of unremarkable calories"));
        out.add(new Equipment("Binder Cuffs", 1, 15, "Restraints for the uncooperative"));
        out.add(new Equipment("Code Cylinder", 1, 50, "Access token for secured systems"));
        return out;
    }

    // ------------------------------------------------------------------
    // instantiate
    // ------------------------------------------------------------------

    @Override
    public PlayerCharacter instantiate(Template template) {
        PlayerCharacter pc = new PlayerCharacter();
        pc.setTemplateName(template.getName());
        pc.setName("");
        pc.setBackground(template.getDescription());

        // Copy attribute codes.
        for (Attribute a : Attribute.values()) {
            DiceCode code = template.getAttributes().get(a);
            pc.setAttribute(a, code == null ? DiceCode.parse("2D") : code);
        }

        // Seed 0-added skills for suggested skills, looking up each governing attribute.
        List<SkillDef> catalog = skillCatalog();
        for (String skillName : template.getSuggestedSkills()) {
            Attribute governing = attributeForSkill(catalog, skillName);
            pc.getSkills().add(new Skill(skillName, governing, DiceCode.ZERO));
        }

        // Force flag + Force skills seeded at 0D when sensitive.
        pc.setForceSensitive(template.isForceSensitive());
        if (template.isForceSensitive()) {
            pc.setForceSkill(com.whim.swd6.api.ForceSkill.CONTROL, DiceCode.ZERO);
            pc.setForceSkill(com.whim.swd6.api.ForceSkill.SENSE, DiceCode.ZERO);
            pc.setForceSkill(com.whim.swd6.api.ForceSkill.ALTER, DiceCode.ZERO);
        }

        // Copy starting gear/weapons (fresh instances so edits don't mutate the template).
        for (Weapon w : template.getStartingWeapons()) {
            pc.getWeapons().add(copyWeapon(w));
        }
        for (Equipment e : template.getStartingGear()) {
            pc.getGear().add(new Equipment(e.getName(), e.getQuantity(), e.getCost(), e.getNotes()));
        }

        pc.setCredits(template.getStartingCredits());
        pc.setMove(template.getMove());

        // Starting economies per the shared creation rules.
        pc.setForcePoints(CreationRules.STARTING_FORCE_POINTS);
        pc.setCharacterPoints(CreationRules.STARTING_CHARACTER_POINTS);
        pc.setDarkSidePoints(0);

        return pc;
    }

    private static Attribute attributeForSkill(List<SkillDef> catalog, String skillName) {
        for (SkillDef def : catalog) {
            if (def.name().equalsIgnoreCase(skillName)) {
                return def.attribute();
            }
        }
        // Unknown suggested skill: default to Dexterity rather than fail character creation.
        return Attribute.DEXTERITY;
    }

    private static Weapon copyWeapon(Weapon src) {
        Weapon w = new Weapon();
        w.setName(src.getName());
        w.setSkill(src.getSkill());
        w.setDamage(src.getDamage());
        w.setMelee(src.isMelee());
        w.setShortRange(src.getShortRange());
        w.setMediumRange(src.getMediumRange());
        w.setLongRange(src.getLongRange());
        w.setShortDifficulty(src.getShortDifficulty());
        w.setMediumDifficulty(src.getMediumDifficulty());
        w.setLongDifficulty(src.getLongDifficulty());
        w.setCost(src.getCost());
        w.setNotes(src.getNotes());
        return w;
    }

    // ------------------------------------------------------------------
    // Scenario (original test adventure)
    // ------------------------------------------------------------------

    @Override
    public Scenario scenario() {
        Scenario s = new Scenario();
        s.setTitle("The Ash-Wind Contract");
        s.setSynopsis("A quiet courier job on the dust-choked moon of Verrund goes loud when "
                + "the package turns out to be a witness somebody wants silenced. Get them off "
                + "the moon alive — and decide who you are when the credits run out.");
        s.setStartSceneId("dockside");

        // Scene 1: NARRATIVE intro -> leads to the skill check.
        Scenario.Scene dockside = new Scenario.Scene();
        dockside.setId("dockside");
        dockside.setTitle("Dockside, Port Verrund");
        dockside.setType(Scenario.SceneType.NARRATIVE);
        dockside.setText("Grit hisses against the landing struts as the ash-wind picks up. Your "
                + "contact never showed; instead a shaking dockworker presses a data-spike into "
                + "your palm and whispers that the 'package' is a person hiding in Bay 12 — and "
                + "that the harbor wardens have been paid to look the other way tonight.");
        dockside.getChoices().add(new Scenario.Choice("Slip toward Bay 12", "sneak_to_bay"));
        s.getScenes().add(dockside);

        // Scene 2: SKILL_CHECK (sneak) -> success/failure.
        Scenario.Scene sneak = new Scenario.Scene();
        sneak.setId("sneak_to_bay");
        sneak.setTitle("The Long Gantry");
        sneak.setType(Scenario.SceneType.SKILL_CHECK);
        sneak.setText("Two wardens loiter under the only working floodlight between you and Bay "
                + "12, stun batons clipped lazily to their belts. The catwalk above is dark, "
                + "narrow, and one loose grating away from announcing you to the whole port.");
        sneak.setSkillName("sneak");
        sneak.setTargetNumber(DifficultyTier.MODERATE.representativeTarget());
        sneak.setSuccessNext("the_witness");
        sneak.setFailureNext("ambush");
        s.getScenes().add(sneak);

        // Scene 3: COMBAT (failure branch) -> victory/defeat.
        Scenario.Scene ambush = new Scenario.Scene();
        ambush.setId("ambush");
        ambush.setTitle("Made");
        ambush.setType(Scenario.SceneType.COMBAT);
        ambush.setText("The grating rings like a bell. Floodlights swing toward you and the "
                + "wardens draw — no more pretense of not seeing you. If you want Bay 12, you "
                + "are going to have to go through them.");
        ambush.getEnemies().add(npc("Harbor Warden", "3D+1", "4D", "3D"));
        ambush.getEnemies().add(npc("Harbor Warden", "3D", "4D", "2D+2"));
        ambush.setVictoryNext("the_witness");
        ambush.setDefeatNext("captured_ending");
        s.getScenes().add(ambush);

        // Scene 4: DECISION (2+ branches).
        Scenario.Scene witness = new Scenario.Scene();
        witness.setId("the_witness");
        witness.setTitle("Bay 12");
        witness.setType(Scenario.SceneType.DECISION);
        witness.setText("She is barely more than a kid, clutching a ledger-spike that names every "
                + "official on Verrund taking bribes. She offers you double your fee to fly her "
                + "off-moon — but a warden gang-boss is already hailing your comlink, offering the "
                + "same sum just to walk away and forget her face.");
        witness.getChoices().add(new Scenario.Choice("Fly her out — a deal is a deal", "clean_getaway"));
        witness.getChoices().add(new Scenario.Choice("Take the bribe and vanish", "sellout_ending"));
        witness.getChoices().add(new Scenario.Choice("Stall the boss, then run for the ship", "clean_getaway"));
        s.getScenes().add(witness);

        // Scene 5: ENDING (heroic).
        Scenario.Scene clean = new Scenario.Scene();
        clean.setId("clean_getaway");
        clean.setTitle("Ash Falls Behind");
        clean.setType(Scenario.SceneType.ENDING);
        clean.setText("The repulsors bite and Verrund's grey haze drops away beneath you. The kid "
                + "watches her homeworld shrink, ledger clutched to her chest, and for the first "
                + "time all night she breathes. Wherever that ledger lands, a lot of comfortable "
                + "people are about to have a very bad quarter. You earned every credit.");
        s.getScenes().add(clean);

        // Scene 6: ENDING (sellout).
        Scenario.Scene sellout = new Scenario.Scene();
        sellout.setId("sellout_ending");
        sellout.setTitle("Clean Hands, Heavy Pockets");
        sellout.setType(Scenario.SceneType.ENDING);
        sellout.setText("The credits hit your account before you have cleared the gantry. You do "
                + "not look back, and you tell yourself that is a kind of mercy. The ash-wind "
                + "swallows Bay 12 behind you, and whatever happens there, happens without you.");
        s.getScenes().add(sellout);

        // Scene 7: ENDING (captured).
        Scenario.Scene captured = new Scenario.Scene();
        captured.setId("captured_ending");
        captured.setTitle("Guest of the Wardens");
        captured.setType(Scenario.SceneType.ENDING);
        captured.setText("You wake to a throbbing skull and a holding cell that smells of rust and "
                + "recycled air. Somewhere above, a ship you do not own is being stripped for parts. "
                + "The witness is gone, the contract is blown — but a resourceful pilot has walked "
                + "out of worse cells than this. Eventually.");
        s.getScenes().add(captured);

        return s;
    }

    private static Combatant npc(String name, String attack, String damage, String resist) {
        Combatant c = new Combatant();
        c.setName(name);
        c.setPlayerCharacter(false);
        c.setAttackCode(DiceCode.parse(attack));
        c.setDamageCode(DiceCode.parse(damage));
        c.setResistCode(DiceCode.parse(resist));
        c.setDeclaredActions(1);
        return c;
    }
}
