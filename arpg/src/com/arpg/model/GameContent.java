package com.arpg.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static seed data for the game world. Builds every {@link Ability},
 * {@link BuffDebuff}, {@link Item}/{@link Equipment}, {@link Enemy}, {@link Pet}
 * and {@link Realm} the engine and UI need to run, and can assemble a fresh
 * starting {@link Character} for a chosen {@link CharacterClass}.
 *
 * <p>Pure construction — no gameplay logic. Everything is built once, eagerly,
 * into unmodifiable lookup maps. Templates (enemies, pets) are returned via
 * {@code copy()} so callers cannot corrupt the shared definitions.</p>
 */
public final class GameContent {

    private static final Map<String, Ability> ABILITIES = new LinkedHashMap<String, Ability>();
    private static final Map<String, BuffDebuff> BUFFS = new LinkedHashMap<String, BuffDebuff>();
    private static final Map<String, Equipment> ITEMS = new LinkedHashMap<String, Equipment>();
    private static final Map<String, Enemy> ENEMIES = new LinkedHashMap<String, Enemy>();
    private static final Map<String, Pet> PETS = new LinkedHashMap<String, Pet>();
    private static final Map<String, Realm> REALMS = new LinkedHashMap<String, Realm>();

    static {
        buildBuffs();
        buildAbilities();
        buildItems();
        buildPets();
        buildEnemies();
        buildRealms();
    }

    private GameContent() {
    }

    // ---------------------------------------------------------------- helpers

    private static Map<StatType, Integer> stats(Object... pairs) {
        Map<StatType, Integer> m = new HashMap<StatType, Integer>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            m.put((StatType) pairs[i], (Integer) pairs[i + 1]);
        }
        return m;
    }

    // ----------------------------------------------------------------- buffs

    private static void buildBuffs() {
        putBuff(new BuffDebuff("buff.rally", "Rallying Vigor",
                "Attack power raised by the war-cry.", true, 3,
                stats(StatType.ATTACK_POWER, 8), 0));
        putBuff(new BuffDebuff("buff.ironskin", "Ironskin",
                "Hardened hide soaks incoming blows.", true, 3,
                stats(StatType.ARMOR, 12), 0));
        putBuff(new BuffDebuff("buff.mark", "Hunter's Mark",
                "Marked prey takes extra damage.", false, 4,
                stats(StatType.ARMOR, -6), 0));
        putBuff(new BuffDebuff("buff.ward", "Arcane Ward",
                "A shimmering ward bolsters resilience.", true, 3,
                stats(StatType.ARMOR, 8, StatType.MAX_RESOURCE, 10), 0));
        putBuff(new BuffDebuff("buff.evasion", "Evasive Poise",
                "Nimble footing sharpens reflexes.", true, 2,
                stats(StatType.AGILITY, 6, StatType.CRIT_CHANCE, 10), 0));
        putBuff(new BuffDebuff("buff.blessing", "Wild Blessing",
                "Nature's favour steadies the body.", true, 4,
                stats(StatType.STRENGTH, 4, StatType.VITALITY, 4), 0));
        putBuff(new BuffDebuff("buff.frostbite", "Frostbite",
                "Chilled to the bone; bleeding cold each tick.", false, 3,
                stats(StatType.AGILITY, -5), -6));
        putBuff(new BuffDebuff("buff.thorns", "Thorn Poison",
                "Barbed toxin gnaws away vitality.", false, 3,
                null, -8));
        putBuff(new BuffDebuff("buff.regen", "Verdant Regrowth",
                "Living roots mend wounds over time.", true, 3,
                null, 7));
        putBuff(new BuffDebuff("buff.enrage", "Enrage",
                "The beast froths, hitting harder.", true, 4,
                stats(StatType.ATTACK_POWER, 10), 0));
    }

    private static void putBuff(BuffDebuff b) {
        BUFFS.put(b.getId(), b);
    }

    // ------------------------------------------------------------- abilities

    private static void buildAbilities() {
        // Ironclad Vanguard
        putAbility(new Ability("abil.van.cleave", "Cleave",
                "A wide sweep that carves all foes before you.", 12, 1,
                EffectType.DAMAGE, 18, TargetType.AOE_ENEMIES));
        putAbility(new Ability("abil.van.shieldbreak", "Shieldbreak",
                "A crushing blow that shatters guard.", 15, 2,
                EffectType.DAMAGE, 30, TargetType.SINGLE_ENEMY));
        putAbility(new Ability("abil.van.rallying_roar", "Rallying Roar",
                "A war-cry that swells your might.", 20, 4,
                EffectType.BUFF, 0, TargetType.SELF, BUFFS.get("buff.rally"), null));
        putAbility(new Ability("abil.van.ironskin", "Ironskin",
                "Brace behind hardened plate.", 18, 4,
                EffectType.BUFF, 0, TargetType.SELF, BUFFS.get("buff.ironskin"), null));

        // Gale Warden
        putAbility(new Ability("abil.war.piercing_shot", "Piercing Shot",
                "A single arrow driven clean through.", 10, 1,
                EffectType.DAMAGE, 22, TargetType.SINGLE_ENEMY));
        putAbility(new Ability("abil.war.volley", "Volley",
                "A rain of arrows over the field.", 16, 2,
                EffectType.DAMAGE, 15, TargetType.AOE_ENEMIES));
        putAbility(new Ability("abil.war.hunters_mark", "Hunter's Mark",
                "Brand a target to soften its defence.", 8, 3,
                EffectType.DEBUFF, 0, TargetType.SINGLE_ENEMY, BUFFS.get("buff.mark"), null));
        putAbility(new Ability("abil.war.evasive_roll", "Evasive Roll",
                "Tumble aside and sharpen your reflexes.", 12, 3,
                EffectType.BUFF, 0, TargetType.SELF, BUFFS.get("buff.evasion"), null));

        // Emberweaver
        putAbility(new Ability("abil.emb.ember_bolt", "Ember Bolt",
                "A darting mote of searing flame.", 10, 1,
                EffectType.DAMAGE, 20, TargetType.SINGLE_ENEMY));
        putAbility(new Ability("abil.emb.frost_nova", "Frost Nova",
                "A burst of cold that chills all nearby foes.", 22, 3,
                EffectType.DEBUFF, 12, TargetType.AOE_ENEMIES, BUFFS.get("buff.frostbite"), null));
        putAbility(new Ability("abil.emb.arcane_ward", "Arcane Ward",
                "Weave a protective sigil about yourself.", 18, 4,
                EffectType.BUFF, 0, TargetType.SELF, BUFFS.get("buff.ward"), null));
        putAbility(new Ability("abil.emb.meteor", "Meteor",
                "Call down a burning star upon your enemies.", 35, 5,
                EffectType.DAMAGE, 40, TargetType.AOE_ENEMIES));

        // Thornshepherd
        putAbility(new Ability("abil.shp.mend", "Mend",
                "Channel living energy to close wounds.", 14, 1,
                EffectType.HEAL, 26, TargetType.ALLY));
        putAbility(new Ability("abil.shp.thorn_lash", "Thorn Lash",
                "Lash a foe with venom-tipped brambles.", 12, 1,
                EffectType.DEBUFF, 14, TargetType.SINGLE_ENEMY, BUFFS.get("buff.thorns"), null));
        putAbility(new Ability("abil.shp.summon_grovling", "Summon Grovling",
                "Call a rootling companion to your side.", 25, 5,
                EffectType.SUMMON, 0, TargetType.SELF, null, "pet.grovling"));
        putAbility(new Ability("abil.shp.wild_blessing", "Wild Blessing",
                "Bless an ally with nature's vigour.", 20, 4,
                EffectType.BUFF, 0, TargetType.ALLY, BUFFS.get("buff.blessing"), null));

        // Shared enemy / pet abilities
        putAbility(new Ability("abil.mob.rend", "Rend",
                "A savage clawing strike.", 0, 1,
                EffectType.DAMAGE, 14, TargetType.SINGLE_ENEMY));
        putAbility(new Ability("abil.mob.howl", "Howl",
                "A bloodcurdling howl that enrages the pack.", 0, 3,
                EffectType.BUFF, 0, TargetType.SELF, BUFFS.get("buff.enrage"), null));
        putAbility(new Ability("abil.mob.venom_spit", "Venom Spit",
                "A gob of corrosive venom.", 0, 2,
                EffectType.DEBUFF, 10, TargetType.SINGLE_ENEMY, BUFFS.get("buff.thorns"), null));
        putAbility(new Ability("abil.mob.crush", "Crush",
                "A titanic overhead smash.", 0, 2,
                EffectType.DAMAGE, 34, TargetType.SINGLE_ENEMY));
        putAbility(new Ability("abil.pet.gnaw", "Gnaw",
                "The companion worries at a foe.", 0, 1,
                EffectType.DAMAGE, 12, TargetType.SINGLE_ENEMY));
        putAbility(new Ability("abil.pet.guard", "Guard",
                "The companion shields its master.", 0, 3,
                EffectType.BUFF, 0, TargetType.ALLY, BUFFS.get("buff.ironskin"), null));
    }

    private static void putAbility(Ability a) {
        ABILITIES.put(a.getId(), a);
    }

    // ----------------------------------------------------------------- items

    private static void buildItems() {
        putItem(new Equipment("item.rusted_blade", "Rusted Shortblade",
                "A pitted blade that has seen better centuries.", Rarity.COMMON, 5,
                EquipmentSlot.WEAPON, 1, stats(StatType.ATTACK_POWER, 4, StatType.STRENGTH, 1)));
        putItem(new Equipment("item.oak_bow", "Oakheart Bow",
                "A supple bow cut from storm-oak.", Rarity.UNCOMMON, 22,
                EquipmentSlot.WEAPON, 2, stats(StatType.ATTACK_POWER, 7, StatType.AGILITY, 3)));
        putItem(new Equipment("item.apprentice_wand", "Apprentice's Wand",
                "A humming rod of channelled aether.", Rarity.UNCOMMON, 24,
                EquipmentSlot.WEAPON, 2, stats(StatType.SPELL_POWER, 8, StatType.INTELLECT, 3)));
        putItem(new Equipment("item.emberfang", "Emberfang Greatsword",
                "A blade forever wreathed in low flame.", Rarity.EPIC, 120,
                EquipmentSlot.WEAPON, 5, stats(StatType.ATTACK_POWER, 18, StatType.STRENGTH, 6,
                        StatType.CRIT_CHANCE, 5)));
        putItem(new Equipment("item.tin_helm", "Tin Helm",
                "Dented, but better than nothing.", Rarity.COMMON, 4,
                EquipmentSlot.HELM, 1, stats(StatType.ARMOR, 3, StatType.MAX_HEALTH, 8)));
        putItem(new Equipment("item.warden_hood", "Warden's Hood",
                "A hood woven for silent hunters.", Rarity.RARE, 40,
                EquipmentSlot.HELM, 3, stats(StatType.ARMOR, 6, StatType.AGILITY, 4)));
        putItem(new Equipment("item.plate_cuirass", "Ironclad Cuirass",
                "Heavy plate that turns aside steel.", Rarity.RARE, 55,
                EquipmentSlot.CHEST, 3, stats(StatType.ARMOR, 14, StatType.MAX_HEALTH, 30,
                        StatType.VITALITY, 4)));
        putItem(new Equipment("item.silk_robe", "Woven Aether Robe",
                "Robes threaded with faint starlight.", Rarity.RARE, 50,
                EquipmentSlot.CHEST, 3, stats(StatType.SPELL_POWER, 10, StatType.MAX_RESOURCE, 25,
                        StatType.INTELLECT, 4)));
        putItem(new Equipment("item.leather_gloves", "Traveler's Gloves",
                "Worn but comfortable riding gloves.", Rarity.COMMON, 6,
                EquipmentSlot.GLOVES, 1, stats(StatType.ARMOR, 2, StatType.AGILITY, 2)));
        putItem(new Equipment("item.swift_boots", "Boots of the Swift",
                "Light boots that quicken the step.", Rarity.UNCOMMON, 18,
                EquipmentSlot.BOOTS, 2, stats(StatType.ARMOR, 3, StatType.AGILITY, 5)));
        putItem(new Equipment("item.band_of_vigor", "Band of Vigor",
                "A ring humming with vital warmth.", Rarity.RARE, 45,
                EquipmentSlot.RING, 3, stats(StatType.MAX_HEALTH, 24, StatType.VITALITY, 3)));
        putItem(new Equipment("item.sages_amulet", "Sage's Amulet",
                "An amulet that clears the channeling mind.", Rarity.EPIC, 90,
                EquipmentSlot.AMULET, 4, stats(StatType.MAX_RESOURCE, 30, StatType.INTELLECT, 5,
                        StatType.SPELL_POWER, 6)));
        putItem(new Equipment("item.aegis_of_dawn", "Aegis of Dawn",
                "A legendary bulwark said to hold back the night.", Rarity.LEGENDARY, 320,
                EquipmentSlot.OFFHAND, 6, stats(StatType.ARMOR, 22, StatType.MAX_HEALTH, 60,
                        StatType.VITALITY, 8, StatType.STRENGTH, 4)));
    }

    private static void putItem(Equipment e) {
        ITEMS.put(e.getId(), e);
    }

    // ------------------------------------------------------------------ pets

    private static void buildPets() {
        putPet(new Pet("pet.grovling", "Grovling", 1, 60, 0, 12, 40,
                abilityList("abil.pet.gnaw")));
        putPet(new Pet("pet.dawnstag", "Dawnstag", 3, 110, 40, 16, 55,
                abilityList("abil.pet.gnaw", "abil.pet.guard")));
        putPet(new Pet("pet.cinderling", "Cinderling", 2, 75, 30, 20, 50,
                abilityList("abil.pet.gnaw", "abil.mob.venom_spit")));
    }

    private static void putPet(Pet p) {
        PETS.put(p.getId(), p);
    }

    // --------------------------------------------------------------- enemies

    private static void buildEnemies() {
        putEnemy(new Enemy("mob.mire_rat", "Mire Rat", 1, 45, 0, 6, false,
                abilityList("abil.mob.rend"), lootCommon(), 15));
        putEnemy(new Enemy("mob.bog_lurker", "Bog Lurker", 2, 70, 20, 9, false,
                abilityList("abil.mob.rend", "abil.mob.venom_spit"), lootCommon(), 24));
        putEnemy(new Enemy("mob.thicket_wolf", "Thicket Wolf", 3, 90, 20, 12, false,
                abilityList("abil.mob.rend", "abil.mob.howl"), lootUncommon(), 33));
        putEnemy(new Enemy("mob.stone_golem", "Stone Sentinel", 4, 150, 0, 14, false,
                abilityList("abil.mob.crush"), lootUncommon(), 48));
        putEnemy(new Enemy("mob.ash_wraith", "Ash Wraith", 5, 120, 60, 16, false,
                abilityList("abil.mob.rend", "abil.mob.venom_spit"), lootRare(), 60));

        // Bosses
        putEnemy(new Enemy("boss.mire_matron", "The Mire Matron", 4, 320, 40, 18, true,
                abilityList("abil.mob.venom_spit", "abil.mob.crush", "abil.mob.howl"), lootBoss(), 200));
        putEnemy(new Enemy("boss.emberlord", "Emberlord Kavash", 7, 520, 120, 26, true,
                abilityList("abil.mob.crush", "abil.mob.howl", "abil.emb.meteor"), lootBoss(), 400));
    }

    private static void putEnemy(Enemy e) {
        ENEMIES.put(e.getId(), e);
    }

    private static LootTable lootCommon() {
        LootTable t = new LootTable("loot.common", 3, 12);
        t.addEntry(ITEMS.get("item.rusted_blade"), 40);
        t.addEntry(ITEMS.get("item.tin_helm"), 40);
        t.addEntry(ITEMS.get("item.leather_gloves"), 30);
        return t;
    }

    private static LootTable lootUncommon() {
        LootTable t = new LootTable("loot.uncommon", 8, 25);
        t.addEntry(ITEMS.get("item.oak_bow"), 25);
        t.addEntry(ITEMS.get("item.apprentice_wand"), 25);
        t.addEntry(ITEMS.get("item.swift_boots"), 25);
        t.addEntry(ITEMS.get("item.tin_helm"), 20);
        return t;
    }

    private static LootTable lootRare() {
        LootTable t = new LootTable("loot.rare", 20, 55);
        t.addEntry(ITEMS.get("item.warden_hood"), 20);
        t.addEntry(ITEMS.get("item.plate_cuirass"), 18);
        t.addEntry(ITEMS.get("item.silk_robe"), 18);
        t.addEntry(ITEMS.get("item.band_of_vigor"), 16);
        return t;
    }

    private static LootTable lootBoss() {
        LootTable t = new LootTable("loot.boss", 60, 160);
        t.addEntry(ITEMS.get("item.emberfang"), 10);
        t.addEntry(ITEMS.get("item.sages_amulet"), 10);
        t.addEntry(ITEMS.get("item.aegis_of_dawn"), 3);
        t.addEntry(ITEMS.get("item.plate_cuirass"), 20);
        return t;
    }

    // ---------------------------------------------------------------- realms

    private static void buildRealms() {
        Realm mire = new Realm("realm.sunken_mire", "The Sunken Mire",
                "A drowned lowland of black water and older, hungrier things.", 1, 1);
        mire.addSpawn(ENEMIES.get("mob.mire_rat"), 50);
        mire.addSpawn(ENEMIES.get("mob.bog_lurker"), 30);
        mire.addEncounter(new Realm.EncounterDef("enc.mire.1", "Reed Shallows",
                "Rats scatter through the reeds.", Realm.EncounterType.COMBAT, null));
        mire.addEncounter(new Realm.EncounterDef("enc.mire.2", "Sunken Cache",
                "A waterlogged chest glints below the surface.", Realm.EncounterType.TREASURE, null));
        mire.addEncounter(new Realm.EncounterDef("enc.mire.3", "Lurker's Pool",
                "Something vast shifts beneath the murk.", Realm.EncounterType.ELITE, null));
        mire.addEncounter(new Realm.EncounterDef("enc.mire.boss", "Heart of the Mire",
                "The Mire Matron rises from the deep.", Realm.EncounterType.BOSS, "boss.mire_matron"));
        REALMS.put(mire.getId(), mire);

        Realm thicket = new Realm("realm.thornwood", "The Thornwood",
                "A wild, overgrown forest where the trees remember teeth.", 2, 3);
        thicket.addSpawn(ENEMIES.get("mob.thicket_wolf"), 45);
        thicket.addSpawn(ENEMIES.get("mob.bog_lurker"), 25);
        thicket.addSpawn(ENEMIES.get("mob.stone_golem"), 20);
        thicket.addEncounter(new Realm.EncounterDef("enc.thorn.1", "Bramble Path",
                "Wolves pace among the thorns.", Realm.EncounterType.COMBAT, null));
        thicket.addEncounter(new Realm.EncounterDef("enc.thorn.2", "Mossy Clearing",
                "A still glade offers a moment's rest.", Realm.EncounterType.REST, null));
        thicket.addEncounter(new Realm.EncounterDef("enc.thorn.3", "Ancient Sentinel",
                "A stone guardian bars the way.", Realm.EncounterType.ELITE, null));
        REALMS.put(thicket.getId(), thicket);

        Realm ashfall = new Realm("realm.ashfall_reach", "Ashfall Reach",
                "A scorched highland beneath a sky of falling cinders.", 3, 5);
        ashfall.addSpawn(ENEMIES.get("mob.ash_wraith"), 40);
        ashfall.addSpawn(ENEMIES.get("mob.stone_golem"), 30);
        ashfall.addEncounter(new Realm.EncounterDef("enc.ash.1", "Cinder Fields",
                "Wraiths drift across the ash.", Realm.EncounterType.COMBAT, null));
        ashfall.addEncounter(new Realm.EncounterDef("enc.ash.2", "Emberforge Ruin",
                "A ruined forge still radiates heat.", Realm.EncounterType.EVENT, null));
        ashfall.addEncounter(new Realm.EncounterDef("enc.ash.boss", "The Ember Throne",
                "Emberlord Kavash awaits atop the pyre.", Realm.EncounterType.BOSS, "boss.emberlord"));
        REALMS.put(ashfall.getId(), ashfall);
    }

    // --------------------------------------------------------- lookup / build

    private static List<Ability> abilityList(String... ids) {
        List<Ability> out = new ArrayList<Ability>();
        for (int i = 0; i < ids.length; i++) {
            Ability a = ABILITIES.get(ids[i]);
            if (a != null) {
                out.add(a);
            }
        }
        return out;
    }

    public static Ability getAbility(String id) {
        return ABILITIES.get(id);
    }

    public static BuffDebuff getBuffTemplate(String id) {
        return BUFFS.get(id);
    }

    /** A fresh instance of a buff template (duration reset), or null if unknown. */
    public static BuffDebuff instanceBuff(String id) {
        BuffDebuff b = BUFFS.get(id);
        return b == null ? null : b.copy();
    }

    public static Equipment getItem(String id) {
        return ITEMS.get(id);
    }

    /** A fresh full-health enemy from a template id, or null if unknown. */
    public static Enemy spawnEnemy(String id) {
        Enemy e = ENEMIES.get(id);
        return e == null ? null : e.copy();
    }

    /** The shared enemy template (do not mutate) — use {@link #spawnEnemy(String)} for instances. */
    public static Enemy getEnemyTemplate(String id) {
        return ENEMIES.get(id);
    }

    /** A fresh full-health pet from a template id, or null if unknown. */
    public static Pet spawnPet(String id) {
        Pet p = PETS.get(id);
        return p == null ? null : p.copy();
    }

    public static Realm getRealm(String id) {
        return REALMS.get(id);
    }

    public static List<Realm> getRealms() {
        return new ArrayList<Realm>(REALMS.values());
    }

    public static List<Ability> getAllAbilities() {
        return new ArrayList<Ability>(ABILITIES.values());
    }

    public static List<Equipment> getAllItems() {
        return new ArrayList<Equipment>(ITEMS.values());
    }

    public static List<BuffDebuff> getAllBuffTemplates() {
        return new ArrayList<BuffDebuff>(BUFFS.values());
    }

    public static List<Enemy> getAllEnemyTemplates() {
        return new ArrayList<Enemy>(ENEMIES.values());
    }

    public static List<Pet> getAllPetTemplates() {
        return new ArrayList<Pet>(PETS.values());
    }

    /**
     * Assemble a fresh level-1 {@link Character} of the given class, wired with
     * that class's abilities and a small starter kit in its inventory.
     */
    public static Character createStartingCharacter(String name, CharacterClass cls) {
        List<Ability> abilities = new ArrayList<Ability>();
        List<String> ids = cls.getAbilityIds();
        for (int i = 0; i < ids.size(); i++) {
            Ability a = ABILITIES.get(ids.get(i));
            if (a != null) {
                abilities.add(a);
            }
        }
        Character c = new Character(name, cls, abilities);
        c.addGold(25);
        c.getInventory().add(ITEMS.get(starterWeaponId(cls)));
        c.getInventory().add(ITEMS.get("item.tin_helm"));
        return c;
    }

    private static String starterWeaponId(CharacterClass cls) {
        switch (cls) {
            case GALE_WARDEN:
                return "item.oak_bow";
            case EMBERWEAVER:
                return "item.apprentice_wand";
            case IRONCLAD_VANGUARD:
            case THORNSHEPHERD:
            default:
                return "item.rusted_blade";
        }
    }

    /** The default companion offered at the start of a run. */
    public static Pet defaultStartingPet() {
        return spawnPet("pet.dawnstag");
    }

    public static List<CharacterClass> getPlayableClasses() {
        List<CharacterClass> list = new ArrayList<CharacterClass>();
        Collections.addAll(list, CharacterClass.values());
        return list;
    }
}
