package com.whim.alganon.content;

import com.whim.alganon.api.Content;
import com.whim.alganon.api.Defs.AbilityDef;
import com.whim.alganon.api.Defs.ClassDef;
import com.whim.alganon.api.Defs.FamilyDef;
import com.whim.alganon.api.Defs.GatherNodeDef;
import com.whim.alganon.api.Defs.ItemDef;
import com.whim.alganon.api.Defs.LootDrop;
import com.whim.alganon.api.Defs.MobDef;
import com.whim.alganon.api.Defs.ObjectiveDef;
import com.whim.alganon.api.Defs.QuestDef;
import com.whim.alganon.api.Defs.RaceDef;
import com.whim.alganon.api.Defs.RecipeDef;
import com.whim.alganon.api.Defs.ZoneMeta;
import com.whim.alganon.api.Enums.AbilityKind;
import com.whim.alganon.api.Enums.ClassId;
import com.whim.alganon.api.Enums.DamageType;
import com.whim.alganon.api.Enums.EquipSlot;
import com.whim.alganon.api.Enums.Faction;
import com.whim.alganon.api.Enums.FamilyArchetype;
import com.whim.alganon.api.Enums.ItemType;
import com.whim.alganon.api.Enums.ObjectiveType;
import com.whim.alganon.api.Enums.ResourceType;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.api.Enums.TargetType;
import com.whim.alganon.data.ZoneBlueprint;
import com.whim.alganon.data.ZoneFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The immutable content registry for the single-player Alganon recreation: races,
 * families, classes, abilities, items, mobs, quests, recipes, gather nodes and the
 * three zone blueprints. All flavor text is authored fresh (clean-room per the build
 * contract) — Alganon terminology is mirrored for authenticity but no marketing copy
 * or lore is reproduced.
 *
 * <p>Everything is built once in the constructor and then read-only. Concrete
 * accessors ({@link #blueprint}) exist for Task 1's own model layer; the engine and
 * UI only ever see the {@link Content} interface.</p>
 */
public final class AlganonContent implements Content {

    private final Map<String, RaceDef> races = new LinkedHashMap<String, RaceDef>();
    private final Map<String, FamilyDef> families = new LinkedHashMap<String, FamilyDef>();
    private final Map<ClassId, ClassDef> classes = new LinkedHashMap<ClassId, ClassDef>();
    private final Map<String, ClassDef> classesById = new LinkedHashMap<String, ClassDef>();
    private final Map<String, AbilityDef> abilities = new LinkedHashMap<String, AbilityDef>();
    private final Map<String, ItemDef> items = new LinkedHashMap<String, ItemDef>();
    private final Map<String, MobDef> mobs = new LinkedHashMap<String, MobDef>();
    private final Map<String, QuestDef> quests = new LinkedHashMap<String, QuestDef>();
    private final Map<String, RecipeDef> recipes = new LinkedHashMap<String, RecipeDef>();
    private final Map<String, GatherNodeDef> gatherNodes = new LinkedHashMap<String, GatherNodeDef>();
    private final Map<String, ZoneBlueprint> blueprints;

    public AlganonContent() {
        buildRaces();
        buildFamilies();
        buildAbilities();
        buildClasses();
        buildItems();
        buildMobs();
        buildGatherNodes();
        buildRecipes();
        buildStaticQuests();
        this.blueprints = ZoneFactory.buildAll();
    }

    // ================================================================= races

    private void buildRaces() {
        RaceDef asharr = new RaceDef("race_asharr", "Human (Asharr)", Faction.ASHARR,
                sm(StatType.FINESSE, 1, StatType.INTELLECT, 1),
                "Disciplined city-dwellers of the Order, the Asharr bind themselves with law "
                + "and craft. They favour patience and precision over raw ferocity, and their "
                + "hold-cities have stood behind stone and oath for generations.");
        RaceDef kujix = new RaceDef("race_kujix", "Talrok (Kujix)", Faction.KUJIX,
                sm(StatType.MIGHT, 2),
                "Warborn clans of the Conquest, the Kujix measure worth by what a hand can take "
                + "and hold. Raised on the march, they are stronger of arm and quicker to anger, "
                + "and they treat every frontier as a door left unlocked.");
        races.put(asharr.id, asharr);
        races.put(kujix.id, kujix);
    }

    // =============================================================== families

    private void buildFamilies() {
        // Asharr (Order) — five families, one per archetype.
        fam("fam_ashheart", "House Ashheart", Faction.ASHARR, FamilyArchetype.ACHIEVER,
                "A line of decorated soldiers and scholars who prize measurable progress; its "
                + "members chase mastery and record every milestone in the hold ledgers.");
        fam("fam_ironvow", "House Ironvow", Faction.ASHARR, FamilyArchetype.COMPETITOR,
                "Duelists and champions of the arena circuit who live for the contest and the "
                + "ranking board; a rival bested is a debt repaid.");
        fam("fam_farwatch", "House Farwatch", Faction.ASHARR, FamilyArchetype.EXPLORER,
                "Cartographers and outriders who map the edges of the realm; a blank space on a "
                + "chart is an insult they take personally.");
        fam("fam_goldhearth", "House Goldhearth", Faction.ASHARR, FamilyArchetype.SOCIALIZER,
                "Merchant hosts and diplomats whose parlours smooth every deal; they know a "
                + "friend in every ward and a discount at every stall.");
        fam("fam_forgewright", "House Forgewright", Faction.ASHARR, FamilyArchetype.CRAFTER,
                "Smiths, tanners and alchemists whose workshops supply the hold; they believe "
                + "a realm is only as strong as the tools it can make.");

        // Kujix (Conquest) — five families, one per archetype.
        fam("kuj_ashfang", "Clan Ashfang", Faction.KUJIX, FamilyArchetype.ACHIEVER,
                "Relentless climbers of the war-ladder who count kills, keeps and trophies; "
                + "standing still is the one thing an Ashfang cannot abide.");
        fam("kuj_bloodmark", "Clan Bloodmark", Faction.KUJIX, FamilyArchetype.COMPETITOR,
                "Blooded champions who settle every question with a challenge; their scars are "
                + "a résumé and their grudges are hereditary.");
        fam("kuj_dustroam", "Clan Dustroam", Faction.KUJIX, FamilyArchetype.EXPLORER,
                "Nomad raiders who follow the horizon for its own sake; they know every dry "
                + "wash and hidden pass in the contested lands.");
        fam("kuj_emberkin", "Clan Emberkin", Faction.KUJIX, FamilyArchetype.SOCIALIZER,
                "Firetenders and story-keepers who bind the warbands together; a word from an "
                + "Emberkin can raise a host or cool a feud.");
        fam("kuj_boneforge", "Clan Boneforge", Faction.KUJIX, FamilyArchetype.CRAFTER,
                "Grim artisans who make war-gear from what the frontier gives up; nothing is "
                + "wasted, and everything is a weapon in the making.");
    }

    private void fam(String id, String name, Faction f, FamilyArchetype a, String desc) {
        families.put(id, new FamilyDef(id, name, f, a, desc));
    }

    // ============================================================== abilities

    private void buildAbilities() {
        // ---- Champion (Fury; stances) ----
        ab("ch_strike", "Sure Strike", ClassId.CHAMPION, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.PHYSICAL,
                1, 0, 0.0, 0.0, 8, 0.0, School.NONE,
                "A reliable weapon blow that costs nothing and builds Fury for heavier strikes.");
        ab("ch_stance_power", "Power Stance", ClassId.CHAMPION, AbilityKind.STANCE, TargetType.SELF, DamageType.PHYSICAL,
                3, 0, 1.0, 0.0, 0, 0.0, School.NONE,
                "Shift your footing for aggression: more damage dealt, but thinner armour.");
        ab("ch_stance_defense", "Defense Stance", ClassId.CHAMPION, AbilityKind.STANCE, TargetType.SELF, DamageType.PHYSICAL,
                3, 0, 1.0, 0.0, 0, 0.0, School.NONE,
                "Shift into a guarded posture: more armour and threat, but softer blows.");
        ab("ch_cleave", "Cleave", ClassId.CHAMPION, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.PHYSICAL,
                2, 25, 6.0, 0.0, 14, 0.0, School.NONE,
                "A sweeping cut that spends Fury for a burst of extra damage.");
        ab("ch_bulwark", "Bulwark", ClassId.CHAMPION, AbilityKind.BUFF, TargetType.SELF, DamageType.PHYSICAL,
                4, 20, 20.0, 0.0, 10, 10.0, School.NONE,
                "Brace behind your guard, raising armour for a short time.");
        ab("ch_execute", "Execute", ClassId.CHAMPION, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.PHYSICAL,
                6, 40, 10.0, 0.0, 30, 0.0, School.NONE,
                "A finishing blow that hits far harder against wounded foes.");

        // ---- Reaver (Fury; bleeds/execute) ----
        ab("rv_rend", "Rend", ClassId.REAVER, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.PHYSICAL,
                1, 0, 0.0, 0.0, 7, 0.0, School.NONE,
                "A raking strike that costs nothing and feeds your Fury.");
        ab("rv_gash", "Gash", ClassId.REAVER, AbilityKind.DOT, TargetType.ENEMY, DamageType.PHYSICAL,
                2, 20, 8.0, 0.0, 5, 9.0, School.NONE,
                "Open a wound that bleeds the target over several seconds.");
        ab("rv_reap", "Reap", ClassId.REAVER, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.PHYSICAL,
                3, 30, 6.0, 0.0, 16, 0.0, School.NONE,
                "A brutal two-handed swing spent from banked Fury.");
        ab("rv_darkpact", "Dark Pact", ClassId.REAVER, AbilityKind.BUFF, TargetType.SELF, DamageType.SHADOW,
                5, 0, 30.0, 0.0, 8, 12.0, School.NONE,
                "Draw on a grim bargain to lifesteal from your blows for a time.");
        ab("rv_annihilate", "Annihilate", ClassId.REAVER, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.PHYSICAL,
                7, 45, 12.0, 0.0, 34, 0.0, School.NONE,
                "An all-in execution that crushes a faltering enemy.");

        // ---- Ranger (Focus; pet/trap/track) ----
        ab("rn_shot", "Quick Shot", ClassId.RANGER, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.PHYSICAL,
                1, 10, 0.0, 0.0, 8, 0.0, School.NONE,
                "A snap shot at range for a small Focus cost.");
        ab("rn_pet_wolf", "Call Companion", ClassId.RANGER, AbilityKind.PET_SUMMON, TargetType.SELF, DamageType.PHYSICAL,
                1, 0, 30.0, 1.0, 20, 0.0, School.NONE,
                "Summon a loyal beast that fights at your side and holds threat.");
        ab("rn_track", "Track", ClassId.RANGER, AbilityKind.UTILITY, TargetType.SELF, DamageType.PHYSICAL,
                2, 0, 20.0, 0.0, 0, 30.0, School.NONE,
                "Read the ground to reveal nearby creatures and quest marks for a while.");
        ab("rn_snaretrap", "Snare Trap", ClassId.RANGER, AbilityKind.TRAP, TargetType.GROUND, DamageType.PHYSICAL,
                3, 15, 12.0, 0.0, 10, 15.0, School.NONE,
                "Set a trap that roots and wounds the first foe to blunder into it.");
        ab("rn_aimedshot", "Aimed Shot", ClassId.RANGER, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.PHYSICAL,
                4, 25, 8.0, 1.5, 22, 0.0, School.NONE,
                "A drawn-out, carefully placed shot that hits hard on release.");

        // ---- Magus (Mana; Flame/Frost/Storm) ----
        ab("mg_flamebolt", "Flamebolt", ClassId.MAGUS, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.FLAME,
                1, 12, 0.0, 1.5, 12, 0.0, School.FLAME,
                "A bolt of conjured fire — the Flame school's bread and butter.");
        ab("mg_ignite", "Ignite", ClassId.MAGUS, AbilityKind.DOT, TargetType.ENEMY, DamageType.FLAME,
                2, 15, 6.0, 0.0, 6, 9.0, School.FLAME,
                "Set the target alight, burning them over time.");
        ab("mg_frostshard", "Frost Shard", ClassId.MAGUS, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.FROST,
                3, 14, 4.0, 1.0, 14, 0.0, School.FROST,
                "A lance of ice that chills as it strikes.");
        ab("mg_frostnova", "Frost Nova", ClassId.MAGUS, AbilityKind.DEBUFF, TargetType.ENEMY, DamageType.FROST,
                4, 20, 15.0, 0.0, 0, 6.0, School.FROST,
                "Flash-freeze the ground, rooting a nearby foe in place.");
        ab("mg_stormstrike", "Storm Strike", ClassId.MAGUS, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.STORM,
                5, 22, 8.0, 0.0, 24, 0.0, School.STORM,
                "Call down a whip of lightning for instant, heavy damage.");

        // ---- Mystic (Mana; Words & Touches) ----
        ab("my_wordmend", "Word of Mending", ClassId.MYSTIC, AbilityKind.HEAL, TargetType.ALLY, DamageType.HOLY,
                1, 12, 0.0, 2.0, 16, 0.0, School.NONE,
                "A spoken Word of power — a measured, cast-time heal.");
        ab("my_touchheal", "Healing Touch", ClassId.MYSTIC, AbilityKind.HEAL, TargetType.ALLY, DamageType.HOLY,
                2, 18, 6.0, 0.0, 22, 0.0, School.NONE,
                "A Touch of power — an instant surge of healing at close range.");
        ab("my_renewingword", "Renewing Word", ClassId.MYSTIC, AbilityKind.HOT, TargetType.ALLY, DamageType.HOLY,
                3, 16, 8.0, 0.0, 5, 12.0, School.NONE,
                "A lingering Word that mends steadily over several seconds.");
        ab("my_smite", "Smite", ClassId.MYSTIC, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.HOLY,
                2, 14, 0.0, 1.5, 12, 0.0, School.NONE,
                "Channel holy light offensively to burn an enemy.");
        ab("my_sanctuary", "Sanctuary", ClassId.MYSTIC, AbilityKind.BUFF, TargetType.SELF, DamageType.HOLY,
                5, 20, 25.0, 0.0, 8, 10.0, School.NONE,
                "Wrap yourself in warding light, reducing damage taken briefly.");

        // ---- Cabalist (Mana; curses/summon/drain) ----
        ab("cb_shadowbolt", "Shadow Bolt", ClassId.CABALIST, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.SHADOW,
                1, 12, 0.0, 1.5, 12, 0.0, School.NONE,
                "A hurled mote of gathered darkness.");
        ab("cb_curse", "Withering Curse", ClassId.CABALIST, AbilityKind.DOT, TargetType.ENEMY, DamageType.SHADOW,
                2, 16, 6.0, 0.0, 6, 12.0, School.NONE,
                "Lay a curse that rots the target from within over time.");
        ab("cb_summon_imp", "Summon Minion", ClassId.CABALIST, AbilityKind.PET_SUMMON, TargetType.SELF, DamageType.SHADOW,
                1, 0, 30.0, 1.5, 16, 0.0, School.NONE,
                "Bind a lesser fiend to fight for you until it is slain.");
        ab("cb_drainlife", "Drain Life", ClassId.CABALIST, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.SHADOW,
                3, 18, 8.0, 2.0, 18, 0.0, School.NONE,
                "Siphon vitality from a foe, wounding them and mending yourself.");
        ab("cb_hex", "Hex", ClassId.CABALIST, AbilityKind.DEBUFF, TargetType.ENEMY, DamageType.SHADOW,
                4, 15, 12.0, 0.0, 0, 8.0, School.NONE,
                "Hobble an enemy's strength with a debilitating hex.");
    }

    private void ab(String id, String name, ClassId owner, AbilityKind kind, TargetType target, DamageType dmg,
                    int levelReq, int cost, double cd, double cast, int power, double dur, School school, String desc) {
        abilities.put(id, new AbilityDef(id, name, owner, kind, target, dmg, levelReq, cost, cd, cast, power, dur, school, desc));
    }

    // ================================================================ classes

    private void buildClasses() {
        clazz(ClassId.CHAMPION, "Champion", ResourceType.FURY,
                list("ch_strike", "ch_stance_power", "ch_stance_defense", "ch_cleave", "ch_bulwark", "ch_execute"),
                "A front-line warrior who shifts between Balance, Power and Defense stances, "
                + "building Fury with each blow and spending it on decisive strikes.");
        clazz(ClassId.REAVER, "Reaver", ResourceType.FURY,
                list("rv_rend", "rv_gash", "rv_reap", "rv_darkpact", "rv_annihilate"),
                "A dark melee aggressor who stacks bleeds and grows lethal as an enemy weakens, "
                + "spending Fury to reap the faltering.");
        clazz(ClassId.RANGER, "Ranger", ResourceType.FOCUS,
                list("rn_shot", "rn_pet_wolf", "rn_track", "rn_snaretrap", "rn_aimedshot"),
                "A ranged hunter who fights beside a summoned beast, lays traps, and tracks prey "
                + "across the wilds, managing a pool of Focus.");
        clazz(ClassId.MAGUS, "Magus", ResourceType.MANA,
                list("mg_flamebolt", "mg_ignite", "mg_frostshard", "mg_frostnova", "mg_stormstrike"),
                "An elemental caster who leans into one of three schools — Flame for burning "
                + "damage, Frost for control, Storm for burst.");
        clazz(ClassId.MYSTIC, "Mystic", ResourceType.MANA,
                list("my_wordmend", "my_touchheal", "my_renewingword", "my_smite", "my_sanctuary"),
                "A healer who mends allies with spoken Words and close-range Touches of power, "
                + "and can turn that same light against the wicked.");
        clazz(ClassId.CABALIST, "Cabalist", ResourceType.MANA,
                list("cb_shadowbolt", "cb_curse", "cb_summon_imp", "cb_drainlife", "cb_hex"),
                "A shadow caster who curses foes to rot, binds a minion to its will, and drains "
                + "the life from its enemies to sustain the fight.");
    }

    private void clazz(ClassId id, String name, ResourceType res, List<String> abilityIds, String desc) {
        ClassDef d = new ClassDef(id, name, res, abilityIds, desc);
        classes.put(id, d);
        classesById.put(id.name(), d);
    }

    // ================================================================== items

    private void buildItems() {
        // Weapons
        item("itm_worncudgel", "Worn Cudgel", ItemType.WEAPON, EquipSlot.WEAPON, sm(StatType.MIGHT, 1), 4, 8, 1,
                "A plain starter club — better than bare fists, if only just.");
        item("itm_rustyblade", "Rusty Blade", ItemType.WEAPON, EquipSlot.WEAPON, sm(StatType.MIGHT, 1), 5, 10, 1,
                "A pitted short sword handed out to recruits.");
        item("itm_ironsword", "Iron Sword", ItemType.WEAPON, EquipSlot.WEAPON, sm(StatType.MIGHT, 2), 10, 40, 1,
                "A soldier's dependable blade of forged iron.");
        item("itm_huntingbow", "Hunting Bow", ItemType.WEAPON, EquipSlot.WEAPON, sm(StatType.FINESSE, 2), 8, 35, 1,
                "A supple bow suited to patient marksmen.");
        item("itm_novicewand", "Novice Wand", ItemType.WEAPON, EquipSlot.WEAPON, sm(StatType.INTELLECT, 2), 6, 35, 1,
                "A slim focus rod for those who bend the elements or the shadows.");

        // Armour (one per slot family)
        item("itm_leathercap", "Leather Cap", ItemType.ARMOR, EquipSlot.HEAD, sm(StatType.STAMINA, 1), 2, 12, 1,
                "Simple boiled-leather headgear.");
        item("itm_leathervest", "Leather Vest", ItemType.ARMOR, EquipSlot.CHEST, sm(StatType.STAMINA, 2), 4, 20, 1,
                "A sturdy leather chestpiece.");
        item("itm_leathergloves", "Leather Gloves", ItemType.ARMOR, EquipSlot.HANDS, null, 1, 8, 1,
                "Well-worn work gloves.");
        item("itm_leatherleggings", "Leather Leggings", ItemType.ARMOR, EquipSlot.LEGS, sm(StatType.STAMINA, 1), 3, 15, 1,
                "Padded leggings of stitched hide.");
        item("itm_leatherboots", "Leather Boots", ItemType.ARMOR, EquipSlot.FEET, sm(StatType.FINESSE, 1), 1, 10, 1,
                "Broken-in travelling boots.");
        item("itm_woodshield", "Wooden Shield", ItemType.ARMOR, EquipSlot.OFFHAND, sm(StatType.STAMINA, 1), 3, 14, 1,
                "A round shield of banded oak.");
        item("itm_luckcharm", "Luck Charm", ItemType.TRINKET, EquipSlot.TRINKET, sm(StatType.SPIRIT, 2), 0, 50, 1,
                "A little braided token said to steady the spirit.");

        // Consumables
        item("itm_healpotion", "Healing Draught", ItemType.CONSUMABLE, null, null, 30, 15, 20,
                "A bitter red tonic that closes wounds quickly.");
        item("itm_manapotion", "Mana Draught", ItemType.CONSUMABLE, null, null, 30, 15, 20,
                "A cool blue tonic that restores a caster's reserves.");

        // Materials
        item("itm_copperore", "Copper Ore", ItemType.MATERIAL, null, null, 0, 3, 50,
                "Raw copper, ready for the smelter.");
        item("itm_copperbar", "Copper Bar", ItemType.MATERIAL, null, null, 0, 8, 50,
                "A refined bar of copper.");
        item("itm_bloodroot", "Bloodroot", ItemType.MATERIAL, null, null, 0, 3, 50,
                "A crimson herb prized by potion-makers.");
        item("itm_pelt", "Rough Pelt", ItemType.MATERIAL, null, null, 0, 4, 50,
                "An untreated animal hide.");
        item("itm_leatherstrip", "Leather Strip", ItemType.MATERIAL, null, null, 0, 6, 50,
                "Tanned leather cut for the workbench.");

        // Quest items
        item("itm_wolffang", "Wolf Fang", ItemType.QUEST, null, null, 0, 0, 10,
                "A curved fang — proof of a culled wolf.");
        item("itm_relicshard", "Relic Shard", ItemType.QUEST, null, null, 0, 0, 10,
                "A humming sliver of old power recovered from the Vault.");
    }

    private void item(String id, String name, ItemType type, EquipSlot slot, Map<StatType, Integer> mods,
                      int power, int value, int stackMax, String desc) {
        items.put(id, new ItemDef(id, name, type, slot, mods, power, value, stackMax, desc));
    }

    // =================================================================== mobs

    private void buildMobs() {
        mob("mob_rabbit", "Meadow Hare", 1, 20, 3, 0, 10, com.whim.alganon.api.Enums.MobBehavior.PASSIVE,
                DamageType.PHYSICAL, "mob_rabbit", loot(drop("itm_pelt", 0.6, 1, 1)));
        mob("mob_boar", "Tusked Boar", 2, 45, 7, 2, 20, com.whim.alganon.api.Enums.MobBehavior.DEFENSIVE,
                DamageType.PHYSICAL, "mob_boar", loot(drop("itm_pelt", 0.8, 1, 2)));
        mob("mob_wolf", "Frontier Wolf", 3, 60, 10, 3, 30, com.whim.alganon.api.Enums.MobBehavior.AGGRESSIVE,
                DamageType.PHYSICAL, "mob_wolf", loot(drop("itm_pelt", 0.7, 1, 2), drop("itm_wolffang", 0.5, 1, 1)));
        mob("mob_spider", "Cave Spider", 3, 50, 9, 2, 28, com.whim.alganon.api.Enums.MobBehavior.DEFENSIVE,
                DamageType.SHADOW, "mob_spider", loot(drop("itm_bloodroot", 0.5, 1, 2)));
        mob("mob_bandit", "Frontier Bandit", 4, 80, 13, 4, 45, com.whim.alganon.api.Enums.MobBehavior.AGGRESSIVE,
                DamageType.PHYSICAL, "mob_bandit", loot(drop("itm_healpotion", 0.4, 1, 1), drop("itm_copperore", 0.6, 1, 3)));
        mob("mob_vaultguard", "Vault Sentinel", 6, 120, 18, 6, 70, com.whim.alganon.api.Enums.MobBehavior.AGGRESSIVE,
                DamageType.PHYSICAL, "mob_vaultguard", loot(drop("itm_copperbar", 0.5, 1, 2), drop("itm_relicshard", 0.2, 1, 1)));
        mob("mob_vaultwarden", "Warden of the Vault", 7, 220, 24, 8, 160, com.whim.alganon.api.Enums.MobBehavior.AGGRESSIVE,
                DamageType.STORM, "mob_vaultwarden", loot(drop("itm_relicshard", 1.0, 1, 1), drop("itm_ironsword", 0.5, 1, 1)));
    }

    private void mob(String id, String name, int level, int hp, int ap, int def, int xp,
                     com.whim.alganon.api.Enums.MobBehavior behavior, DamageType dmg, String sprite, List<LootDrop> loot) {
        mobs.put(id, new MobDef(id, name, level, hp, ap, def, xp, behavior, dmg, sprite, loot));
    }

    private static LootDrop drop(String itemId, double chance, int min, int max) {
        return new LootDrop(itemId, chance, min, max);
    }

    private static List<LootDrop> loot(LootDrop... drops) {
        return new ArrayList<LootDrop>(Arrays.asList(drops));
    }

    // ============================================================ gather nodes

    private void buildGatherNodes() {
        gatherNodes.put("node_copper", new GatherNodeDef("node_copper", "Copper Vein", "itm_copperore", 0, 1, 2, 20.0));
        gatherNodes.put("node_richcopper", new GatherNodeDef("node_richcopper", "Rich Copper Vein", "itm_copperore", 20, 2, 4, 30.0));
        gatherNodes.put("node_bloodroot", new GatherNodeDef("node_bloodroot", "Bloodroot Cluster", "itm_bloodroot", 0, 1, 2, 18.0));
    }

    // ================================================================ recipes

    private void buildRecipes() {
        // Process: raw -> refined
        recipe("rec_smeltcopper", "Smelt Copper Bar", SkillType.PROCESSING, 0,
                inputs("itm_copperore", 2), "itm_copperbar", 1, 2.0);
        recipe("rec_tanpelt", "Tan Leather Strip", SkillType.PROCESSING, 0,
                inputs("itm_pelt", 2), "itm_leatherstrip", 1, 2.0);
        // Craft: refined -> goods
        recipe("rec_healpotion", "Brew Healing Draught", SkillType.CRAFTING, 0,
                inputs("itm_bloodroot", 2), "itm_healpotion", 1, 3.0);
        recipe("rec_leathervest", "Stitch Leather Vest", SkillType.CRAFTING, 10,
                inputs("itm_leatherstrip", 4), "itm_leathervest", 1, 4.0);
        recipe("rec_luckcharm", "Fashion Luck Charm", SkillType.CRAFTING, 15,
                inputs("itm_copperbar", 2, "itm_bloodroot", 1), "itm_luckcharm", 1, 4.0);
        recipe("rec_ironsword", "Forge Iron Sword", SkillType.CRAFTING, 20,
                inputs("itm_copperbar", 3, "itm_leatherstrip", 2), "itm_ironsword", 1, 5.0);
    }

    private void recipe(String id, String name, SkillType skill, int skillReq, Map<String, Integer> inputs,
                        String out, int outQty, double craftSec) {
        recipes.put(id, new RecipeDef(id, name, skill, skillReq, inputs, out, outQty, craftSec));
    }

    // =========================================================== static quests

    private void buildStaticQuests() {
        quest("q_firststeps", "First Steps", "npc_elder", "npc_elder", 1, 40, 10,
                objs(obj(ObjectiveType.KILL, "mob_rabbit", 3, "Thin the meadow hares (0/3)")),
                list("itm_healpotion"), false,
                "Elder Maren wants to see you can handle yourself. Cull a few hares in the vale.");
        quest("q_supplies", "Short on Supplies", "npc_vendor", "npc_vendor", 2, 50, 15,
                objs(obj(ObjectiveType.GATHER, "itm_copperore", 5, "Mine copper ore (0/5)")),
                list("itm_manapotion"), false,
                "Quartermaster Brole needs raw copper for the forges. Bring back five ore.");
        quest("q_wolfcull", "Wolves at the Line", "npc_scout", "npc_scout", 3, 80, 25,
                objs(obj(ObjectiveType.KILL, "mob_wolf", 4, "Slay frontier wolves (0/4)"),
                     obj(ObjectiveType.GATHER, "itm_wolffang", 3, "Collect wolf fangs (0/3)")),
                list("itm_leathervest"), false,
                "Scout Hedda reports wolves harrying the frontier road. Break up the pack.");
        quest("q_vaultrelic", "The Sunken Vault", "npc_scout", "npc_scout", 6, 200, 60,
                objs(obj(ObjectiveType.TRAVEL, "zone_dungeon", 1, "Enter the Sunken Vault"),
                     obj(ObjectiveType.KILL, "mob_vaultwarden", 1, "Defeat the Warden (0/1)"),
                     obj(ObjectiveType.GATHER, "itm_relicshard", 1, "Recover the relic shard (0/1)")),
                list("itm_ironsword"), false,
                "Something old still stirs beneath the frontier. Descend, end the Warden, and "
                + "bring back the shard it guards.");
    }

    private void quest(String id, String name, String giver, String turnIn, int levelReq, int xp, int gold,
                       List<ObjectiveDef> objectives, List<String> rewardItems, boolean procedural, String desc) {
        quests.put(id, new QuestDef(id, name, giver, turnIn, levelReq, xp, gold, objectives, rewardItems, procedural, desc));
    }

    private static ObjectiveDef obj(ObjectiveType t, String target, int count, String text) {
        return new ObjectiveDef(t, target, count, text);
    }

    private static List<ObjectiveDef> objs(ObjectiveDef... o) {
        return new ArrayList<ObjectiveDef>(Arrays.asList(o));
    }

    // =========================================================== procedural quests

    /**
     * [Gap — my design] Dynamic quest generator. The objective type is biased by the
     * player's family archetype (Explorers travel, Crafters gather, Competitors/Achievers
     * hunt, Socializers get a mix), then a level-appropriate target and scaled reward are
     * chosen. Ids embed an rng draw so repeated generations stay distinct; giver/turn-in
     * default to the frontier scout who runs the bounty board.
     */
    @Override
    public QuestDef generateQuest(int level, FamilyArchetype archetype, Random rng) {
        if (rng == null) rng = new Random();
        int lvl = Math.max(1, level);
        ObjectiveType type = pickObjectiveType(archetype, rng);
        int tag = rng.nextInt(1000000);
        int xp = 30 + lvl * 15;
        int gold = 5 + lvl * 3;

        ObjectiveDef objective;
        String name;
        String desc;
        List<String> rewardItems = new ArrayList<String>();

        if (type == ObjectiveType.KILL) {
            MobDef m = pickMobForLevel(lvl);
            int count = 3 + rng.nextInt(4); // 3..6
            name = "Bounty: " + m.name;
            objective = obj(ObjectiveType.KILL, m.id, count, "Defeat " + m.name + " (0/" + count + ")");
            desc = "A standing bounty on " + m.name + " in the field. Bring the count down.";
            rewardItems.add("itm_healpotion");
        } else if (type == ObjectiveType.GATHER) {
            String[] mats = { "itm_copperore", "itm_bloodroot", "itm_pelt" };
            String mat = mats[rng.nextInt(mats.length)];
            int count = 4 + rng.nextInt(5); // 4..8
            ItemDef it = items.get(mat);
            name = "Requisition: " + it.name;
            objective = obj(ObjectiveType.GATHER, mat, count, "Gather " + it.name + " (0/" + count + ")");
            desc = "The quartermasters are short on " + it.name + ". Collect what you can.";
            rewardItems.add("itm_manapotion");
        } else { // TRAVEL
            String[] zoneIds = { ZoneFactory.START_ID, ZoneFactory.FRONTIER_ID, ZoneFactory.DUNGEON_ID };
            String zid = zoneIds[rng.nextInt(zoneIds.length)];
            ZoneMeta zm = blueprintMeta(zid);
            name = "Scout: " + zm.name;
            objective = obj(ObjectiveType.TRAVEL, zid, 1, "Travel to " + zm.name);
            desc = "Eyes are needed on " + zm.name + ". Make the trip and report what you see.";
            rewardItems.add("itm_healpotion");
        }

        String id = "proc_" + type.name().toLowerCase() + "_L" + lvl + "_" + tag;
        return new QuestDef(id, name, "npc_scout", "npc_scout", lvl, xp, gold,
                objs(objective), rewardItems, true, desc);
    }

    private ObjectiveType pickObjectiveType(FamilyArchetype a, Random rng) {
        // Weighted bag by archetype. [Gap — my design]
        List<ObjectiveType> bag = new ArrayList<ObjectiveType>();
        add(bag, ObjectiveType.KILL, 1);
        add(bag, ObjectiveType.GATHER, 1);
        add(bag, ObjectiveType.TRAVEL, 1);
        if (a != null) {
            switch (a) {
                case COMPETITOR: add(bag, ObjectiveType.KILL, 3); break;
                case ACHIEVER:   add(bag, ObjectiveType.KILL, 2); break;
                case CRAFTER:    add(bag, ObjectiveType.GATHER, 3); break;
                case EXPLORER:   add(bag, ObjectiveType.TRAVEL, 3); break;
                case SOCIALIZER: add(bag, ObjectiveType.GATHER, 1); add(bag, ObjectiveType.TRAVEL, 1); break;
                default: break;
            }
        }
        return bag.get(rng.nextInt(bag.size()));
    }

    private static void add(List<ObjectiveType> bag, ObjectiveType t, int n) {
        for (int i = 0; i < n; i++) bag.add(t);
    }

    private MobDef pickMobForLevel(int level) {
        MobDef best = mobs.get("mob_rabbit");
        int bestDelta = Integer.MAX_VALUE;
        for (MobDef m : mobs.values()) {
            if (m.level > level + 1) continue;              // don't send players at over-level foes
            int delta = Math.abs(m.level - level);
            if (delta < bestDelta) { bestDelta = delta; best = m; }
        }
        return best;
    }

    private ZoneMeta blueprintMeta(String zoneId) {
        ZoneBlueprint b = blueprints.get(zoneId);
        return b != null ? b.meta : blueprints.values().iterator().next().meta;
    }

    // ================================================================ accessors

    @Override public List<RaceDef> races() { return new ArrayList<RaceDef>(races.values()); }
    @Override public List<FamilyDef> families() { return new ArrayList<FamilyDef>(families.values()); }
    @Override public List<ClassDef> classes() { return new ArrayList<ClassDef>(classes.values()); }

    @Override public RaceDef race(String id) { return races.get(id); }
    @Override public FamilyDef family(String id) { return families.get(id); }
    @Override public ClassDef clazz(String classId) { return classesById.get(classId); }
    @Override public AbilityDef ability(String id) { return abilities.get(id); }
    @Override public ItemDef item(String id) { return items.get(id); }
    @Override public MobDef mob(String id) { return mobs.get(id); }
    @Override public QuestDef quest(String id) { return quests.get(id); }
    @Override public RecipeDef recipe(String id) { return recipes.get(id); }
    @Override public GatherNodeDef gatherNode(String id) { return gatherNodes.get(id); }

    @Override public ZoneMeta zone(String id) {
        ZoneBlueprint b = blueprints.get(id);
        return b != null ? b.meta : null;
    }

    @Override public List<QuestDef> staticQuests() { return new ArrayList<QuestDef>(quests.values()); }
    @Override public List<RecipeDef> recipes() { return new ArrayList<RecipeDef>(recipes.values()); }

    @Override public List<ZoneMeta> zones() {
        List<ZoneMeta> out = new ArrayList<ZoneMeta>();
        for (ZoneBlueprint b : blueprints.values()) out.add(b.meta);
        return out;
    }

    @Override public String startingZoneId(String raceId) {
        // [Gap — my design] Both factions share the tutorial start zone in v1 (DESIGN.md §5).
        return ZoneFactory.START_ID;
    }

    // ----- concrete accessors for Task 1's own model layer (not part of Content) -----

    /** The live blueprint for a zone id, used by the model to build a WorldModel. */
    public ZoneBlueprint blueprint(String zoneId) { return blueprints.get(zoneId); }

    /** All zone blueprints (insertion order). */
    public Map<String, ZoneBlueprint> blueprints() { return Collections.unmodifiableMap(blueprints); }

    // ================================================================== helpers

    private static List<String> list(String... s) { return new ArrayList<String>(Arrays.asList(s)); }

    private static Map<StatType, Integer> sm(Object... kv) {
        EnumMap<StatType, Integer> m = new EnumMap<StatType, Integer>(StatType.class);
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((StatType) kv[i], (Integer) kv[i + 1]);
        return m;
    }

    private static Map<String, Integer> inputs(Object... kv) {
        LinkedHashMap<String, Integer> m = new LinkedHashMap<String, Integer>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((String) kv[i], (Integer) kv[i + 1]);
        return m;
    }
}
