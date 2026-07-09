package com.whim.alganon.enginetest;

import com.whim.alganon.api.CharacterModel;
import com.whim.alganon.api.Content;
import com.whim.alganon.api.Defs;
import com.whim.alganon.api.Enums;
import com.whim.alganon.api.Enums.AbilityKind;
import com.whim.alganon.api.Enums.ClassId;
import com.whim.alganon.api.Enums.DamageType;
import com.whim.alganon.api.Enums.EquipSlot;
import com.whim.alganon.api.Enums.Faction;
import com.whim.alganon.api.Enums.FamilyArchetype;
import com.whim.alganon.api.Enums.ItemType;
import com.whim.alganon.api.Enums.MobBehavior;
import com.whim.alganon.api.Enums.ObjectiveType;
import com.whim.alganon.api.Enums.ResourceType;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.Stance;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.api.Enums.TargetType;
import com.whim.alganon.api.Enums.TileType;
import com.whim.alganon.api.GameModel;
import com.whim.alganon.api.GridPos;
import com.whim.alganon.api.ModelFactory;
import com.whim.alganon.api.WorldModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A trivial in-package fake implementing the api model/content interfaces, used ONLY to
 * exercise the engine in {@link SmokeMain}. Production runs against Task 1's real
 * ModelFactory; the engine never depends on this.
 */
final class FakeModel {}

/** Fake content registry + factory. */
final class FakeFactory implements ModelFactory {
    private final FakeContent content = new FakeContent();

    public Content content() { return content; }

    public GameModel newGame(long seed) { return new FakeGameModel(content, seed); }

    public void applyCreation(GameModel model, String raceId, String familyId, String classId, String name) {
        FakeCharacter p = (FakeCharacter) model.player();
        p.raceId = raceId; p.familyId = familyId; p.classId = classId; p.name = name;
        p.faction = content.race(raceId).faction;
        Defs.ClassDef c = content.clazz(classId);
        p.resourceType = c.resource;
        p.stats.put(StatType.MIGHT, 6);
        p.stats.put(StatType.FINESSE, 5);
        p.stats.put(StatType.INTELLECT, 5);
        p.stats.put(StatType.SPIRIT, 5);
        p.stats.put(StatType.STAMINA, 6);
        p.maxHp = 30; p.hp = 30;
        p.maxResource = 100;
        p.resource = c.resource == ResourceType.FURY ? 0 : 100;
        for (String a : c.abilityIds) p.learnAbility(a);
        p.addItem("sword", 1);
        p.addItem("potion", 3);
        p.addItem("ore", 2);
        p.equip(EquipSlot.WEAPON, "sword");
        p.removeItem("sword", 1);
        p.gold = 50;
        p.zoneId = content.startingZoneId(raceId);
        model.loadZone(p.zoneId);
        p.pos = new GridPos(1, 1);
    }
}

final class FakeContent implements Content {
    private final Map<String, Defs.AbilityDef> abilities = new LinkedHashMap<String, Defs.AbilityDef>();
    private final Map<String, Defs.ItemDef> items = new LinkedHashMap<String, Defs.ItemDef>();
    private final Map<String, Defs.MobDef> mobs = new LinkedHashMap<String, Defs.MobDef>();
    private final Map<String, Defs.QuestDef> quests = new LinkedHashMap<String, Defs.QuestDef>();
    private final Map<String, Defs.RecipeDef> recipes = new LinkedHashMap<String, Defs.RecipeDef>();
    private final Map<String, Defs.GatherNodeDef> nodes = new LinkedHashMap<String, Defs.GatherNodeDef>();
    private final Map<String, Defs.ZoneMeta> zones = new LinkedHashMap<String, Defs.ZoneMeta>();
    private final Defs.RaceDef race;
    private final Defs.FamilyDef family;
    private final Defs.ClassDef champion;
    private int procSeq = 1;

    FakeContent() {
        Map<StatType, Integer> mod = new EnumMap<StatType, Integer>(StatType.class);
        race = new Defs.RaceDef("human", "Human", Faction.ASHARR, mod, "A resilient people.");
        family = new Defs.FamilyDef("fam_ach", "House Meridian", Faction.ASHARR, FamilyArchetype.ACHIEVER, "Driven achievers.");
        abilities.put("strike", new Defs.AbilityDef("strike", "Strike", ClassId.CHAMPION, AbilityKind.DAMAGE,
                TargetType.ENEMY, DamageType.PHYSICAL, 1, 0, 0, 0, 6, 0, School.NONE, "A basic melee blow."));
        abilities.put("cleave", new Defs.AbilityDef("cleave", "Cleave", ClassId.CHAMPION, AbilityKind.DAMAGE,
                TargetType.ENEMY, DamageType.PHYSICAL, 1, 10, 2.0, 0, 12, 0, School.NONE, "A heavy strike."));
        abilities.put("rally", new Defs.AbilityDef("rally", "Rally", ClassId.CHAMPION, AbilityKind.BUFF,
                TargetType.SELF, DamageType.PHYSICAL, 1, 5, 6.0, 0, 3, 8.0, School.NONE, "Bolster yourself."));
        champion = new Defs.ClassDef(ClassId.CHAMPION, "Champion", ResourceType.FURY,
                Arrays.asList("strike", "cleave", "rally"), "A frontline warrior.");
        items.put("sword", new Defs.ItemDef("sword", "Iron Sword", ItemType.WEAPON, EquipSlot.WEAPON, null, 5, 10, 1, "A plain blade."));
        items.put("potion", new Defs.ItemDef("potion", "Healing Draught", ItemType.CONSUMABLE, null, null, 20, 5, 10, "Restores health."));
        items.put("ore", new Defs.ItemDef("ore", "Copper Ore", ItemType.MATERIAL, null, null, 0, 2, 99, "Raw ore."));
        items.put("ingot", new Defs.ItemDef("ingot", "Copper Ingot", ItemType.MATERIAL, null, null, 0, 6, 99, "Smelted metal."));
        mobs.put("rat", new Defs.MobDef("rat", "Sewer Rat", 1, 12, 3, 0, 30, MobBehavior.AGGRESSIVE, DamageType.PHYSICAL, "rat",
                Arrays.asList(new Defs.LootDrop("ore", 0.9, 1, 2))));
        mobs.put("boar", new Defs.MobDef("boar", "Wild Boar", 2, 20, 5, 1, 45, MobBehavior.DEFENSIVE, DamageType.PHYSICAL, "boar",
                Arrays.asList(new Defs.LootDrop("potion", 0.5, 1, 1))));
        quests.put("q_rats", new Defs.QuestDef("q_rats", "Rat Problem", "elder", "elder", 0, 50, 10,
                Arrays.asList(new Defs.ObjectiveDef(ObjectiveType.KILL, "rat", 1, "Slay a Sewer Rat")),
                new ArrayList<String>(), false, "The elder needs rats culled."));
        recipes.put("r_ingot", new Defs.RecipeDef("r_ingot", "Smelt Copper Ingot", SkillType.PROCESSING, 0,
                mapOf("ore", 2), "ingot", 1, 1.0));
        nodes.put("ore_node", new Defs.GatherNodeDef("ore_node", "Copper Vein", "ore", 0, 1, 2, 5.0));
        zones.put("start", new Defs.ZoneMeta("start", "Meridian Vale", 8, 8, Faction.ASHARR, false, 1, "A safe starting vale."));
    }

    private static Map<String, Integer> mapOf(String k, int v) {
        Map<String, Integer> m = new LinkedHashMap<String, Integer>(); m.put(k, v); return m;
    }

    public List<Defs.RaceDef> races() { return Arrays.asList(race); }
    public List<Defs.FamilyDef> families() { return Arrays.asList(family); }
    public List<Defs.ClassDef> classes() { return Arrays.asList(champion); }
    public Defs.RaceDef race(String id) { return "human".equals(id) ? race : null; }
    public Defs.FamilyDef family(String id) { return "fam_ach".equals(id) ? family : null; }
    public Defs.ClassDef clazz(String classId) { return "CHAMPION".equals(classId) || "Champion".equals(classId) ? champion : ("champion".equalsIgnoreCase(classId) ? champion : null); }
    public Defs.AbilityDef ability(String id) { return abilities.get(id); }
    public Defs.ItemDef item(String id) { return items.get(id); }
    public Defs.MobDef mob(String id) { return mobs.get(id); }
    public Defs.QuestDef quest(String id) { return quests.get(id); }
    public Defs.RecipeDef recipe(String id) { return recipes.get(id); }
    public Defs.GatherNodeDef gatherNode(String id) { return nodes.get(id); }
    public Defs.ZoneMeta zone(String id) { return zones.get(id); }
    public List<Defs.QuestDef> staticQuests() { return new ArrayList<Defs.QuestDef>(quests.values()); }
    public List<Defs.RecipeDef> recipes() { return new ArrayList<Defs.RecipeDef>(recipes.values()); }
    public List<Defs.ZoneMeta> zones() { return new ArrayList<Defs.ZoneMeta>(zones.values()); }
    public String startingZoneId(String raceId) { return "start"; }

    public Defs.QuestDef generateQuest(int level, FamilyArchetype archetype, Random rng) {
        String id = "proc_" + (procSeq++);
        return new Defs.QuestDef(id, "Cull the Vermin", "elder", "elder", 0, 40, 8,
                Arrays.asList(new Defs.ObjectiveDef(ObjectiveType.KILL, "rat", 2, "Slay 2 Sewer Rats")),
                new ArrayList<String>(), true, "A dynamically generated task.");
    }
}

final class FakeGameModel implements GameModel {
    private final Content content;
    private final long seed;
    private final FakeCharacter player;
    private FakeWorld world;
    private long lastSave;
    private int asharr, kujix;

    FakeGameModel(Content content, long seed) {
        this.content = content; this.seed = seed; this.player = new FakeCharacter(content);
    }
    public Content content() { return content; }
    public CharacterModel player() { return player; }
    public WorldModel world() { return world; }
    public WorldModel loadZone(String zoneId) { world = new FakeWorld((FakeContent) content, zoneId); return world; }
    public long seed() { return seed; }
    public long lastSaveEpochMillis() { return lastSave; }
    public void setLastSaveEpochMillis(long millis) { lastSave = millis; }
    public int asharrWarScore() { return asharr; }
    public int kujixWarScore() { return kujix; }
    public void setWarScores(int a, int k) { asharr = a; kujix = k; }
}

final class FakeCharacter implements CharacterModel {
    private final Content content;
    String name = "";
    Faction faction = Faction.ASHARR;
    String raceId, familyId, classId;
    int level = 1;
    long xp;
    ResourceType resourceType = ResourceType.FURY;
    int resource, maxResource = 100, hp = 30, maxHp = 30;
    Stance stance = Stance.BALANCE;
    School school = School.NONE;
    final Map<StatType, Integer> stats = new EnumMap<StatType, Integer>(StatType.class);
    final Map<SkillType, Integer> skills = new EnumMap<SkillType, Integer>(SkillType.class);
    final List<String> abilities = new ArrayList<String>();
    long gold;
    final Map<String, Integer> inventory = new LinkedHashMap<String, Integer>();
    final Map<EquipSlot, String> equipped = new EnumMap<EquipSlot, String>(EquipSlot.class);
    SkillType studyAssignment;
    double bankedStudy;
    GridPos pos = new GridPos(0, 0);
    String zoneId;

    FakeCharacter(Content content) { this.content = content; }

    public String name() { return name; }
    public String getName() { return name; }
    public void setName(String n) { name = n; }
    public Faction faction() { return faction; }
    public String raceId() { return raceId; }
    public String familyId() { return familyId; }
    public String classId() { return classId; }
    public int level() { return level; }
    public void setLevel(int l) { level = l; }
    public long xp() { return xp; }
    public void setXp(long v) { xp = v; }
    public ResourceType resourceType() { return resourceType; }
    public int resource() { return resource; }
    public int maxResource() { return maxResource; }
    public void setResource(int v) { resource = Math.max(0, Math.min(maxResource, v)); }
    public void setMaxResource(int v) { maxResource = Math.max(1, v); }
    public void setMaxHp(int v) { maxHp = Math.max(1, v); }
    public void setHp(int v) { hp = Math.max(0, Math.min(maxHp, v)); }
    public Stance stance() { return stance; }
    public void setStance(Stance s) { stance = s; }
    public School school() { return school; }
    public void setSchool(School s) { school = s; }
    public Map<StatType, Integer> stats() { return stats; }
    public int skill(SkillType s) { Integer v = skills.get(s); return v == null ? 0 : v; }
    public void addSkillProgress(SkillType s, int points) { skills.put(s, skill(s) + points); }
    public List<String> knownAbilityIds() { return abilities; }
    public void learnAbility(String id) { if (!abilities.contains(id)) abilities.add(id); }
    public long gold() { return gold; }
    public void addGold(long d) { gold = Math.max(0, gold + d); }
    public Map<String, Integer> inventory() { return inventory; }
    public void addItem(String id, int qty) { inventory.put(id, (inventory.containsKey(id) ? inventory.get(id) : 0) + qty); }
    public boolean removeItem(String id, int qty) {
        int have = inventory.containsKey(id) ? inventory.get(id) : 0;
        if (have < qty) return false;
        if (have == qty) inventory.remove(id); else inventory.put(id, have - qty);
        return true;
    }
    public Map<EquipSlot, String> equipped() { return equipped; }
    public void equip(EquipSlot slot, String itemId) { equipped.put(slot, itemId); }
    public String unequip(EquipSlot slot) { return equipped.remove(slot); }
    public SkillType studyAssignment() { return studyAssignment; }
    public void setStudyAssignment(SkillType s) { studyAssignment = s; }
    public double bankedStudyProgress() { return bankedStudy; }
    public void setBankedStudyProgress(double v) { bankedStudy = v; }
    public GridPos pos() { return pos; }
    public void setPos(GridPos p) { pos = p; }
    public String zoneId() { return zoneId; }
    public void setZoneId(String z) { zoneId = z; }

    // Combatant
    public boolean isPlayer() { return true; }
    public int hp() { return hp; }
    public int maxHp() { return maxHp; }
    public boolean alive() { return hp > 0; }
    public int attackPower() {
        int might = stats.containsKey(StatType.MIGHT) ? stats.get(StatType.MIGHT) : 0;
        int wpn = 0;
        String w = equipped.get(EquipSlot.WEAPON);
        if (w != null) { Defs.ItemDef d = content.item(w); if (d != null) wpn = d.power; }
        return might + wpn;
    }
    public int defense() {
        int sta = stats.containsKey(StatType.STAMINA) ? stats.get(StatType.STAMINA) : 0;
        return sta / 2;
    }
    public int takeDamage(int amount, DamageType type) {
        int dealt = Math.max(1, amount - defense());
        hp = Math.max(0, hp - dealt);
        return dealt;
    }
    public void heal(int amount) { hp = Math.min(maxHp, hp + Math.max(0, amount)); }
}

final class FakeWorld implements WorldModel {
    private final String zoneId;
    private final Defs.ZoneMeta meta;
    private final TileType[][] tiles;
    private final List<NpcEntity> npcs = new ArrayList<NpcEntity>();
    private final List<MobEntity> mobs = new ArrayList<MobEntity>();
    private final List<NodeEntity> nodes = new ArrayList<NodeEntity>();
    private final List<Portal> portals = new ArrayList<Portal>();

    FakeWorld(FakeContent content, String zoneId) {
        this.zoneId = zoneId;
        this.meta = content.zone(zoneId);
        int w = meta.width, h = meta.height;
        tiles = new TileType[w][h];
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                tiles[x][y] = (x == 0 || y == 0 || x == w - 1 || y == h - 1) ? TileType.WALL : TileType.GRASS;
        npcs.add(new FakeNpc("elder", "Elder Rowan", new GridPos(2, 2), true, false, "npc_elder"));
        npcs.add(new FakeNpc("merchant", "Merchant Vale", new GridPos(3, 2), false, true, "npc_merchant"));
        mobs.add(new FakeMob(content, "rat1", "rat", new GridPos(5, 5)));
        mobs.add(new FakeMob(content, "boar1", "boar", new GridPos(6, 6)));
        nodes.add(new FakeNode("ore_node", "Copper Vein", new GridPos(2, 5)));
        portals.add(new FakePortal(new GridPos(6, 6), "start", "Return"));
    }
    public String zoneId() { return zoneId; }
    public String zoneName() { return meta.name; }
    public int width() { return meta.width; }
    public int height() { return meta.height; }
    public TileType tileAt(int x, int y) {
        if (x < 0 || y < 0 || x >= meta.width || y >= meta.height) return TileType.VOID;
        return tiles[x][y];
    }
    public boolean walkable(int x, int y) {
        if (x < 0 || y < 0 || x >= meta.width || y >= meta.height) return false;
        return tiles[x][y] != TileType.WALL && tiles[x][y] != TileType.WATER && tiles[x][y] != TileType.VOID;
    }
    public List<NpcEntity> npcs() { return npcs; }
    public List<MobEntity> mobs() { return mobs; }
    public List<NodeEntity> nodes() { return nodes; }
    public List<Portal> portals() { return portals; }
}

final class FakeNpc implements WorldModel.NpcEntity {
    private final String id, name, sprite; private final GridPos pos; private final boolean qg, vend;
    FakeNpc(String id, String name, GridPos pos, boolean qg, boolean vend, String sprite) {
        this.id = id; this.name = name; this.pos = pos; this.qg = qg; this.vend = vend; this.sprite = sprite;
    }
    public String id() { return id; }
    public String name() { return name; }
    public GridPos pos() { return pos; }
    public boolean questGiver() { return qg; }
    public boolean vendor() { return vend; }
    public String spriteKey() { return sprite; }
}

final class FakeMob implements WorldModel.MobEntity {
    private final Content content; private final String id, defId; private GridPos pos;
    private final Defs.MobDef def; private int hp; private boolean inCombat;
    FakeMob(Content content, String id, String defId, GridPos pos) {
        this.content = content; this.id = id; this.defId = defId; this.pos = pos;
        this.def = content.mob(defId); this.hp = def.maxHp;
    }
    public String id() { return id; }
    public String defId() { return defId; }
    public GridPos pos() { return pos; }
    public void setPos(GridPos p) { pos = p; }
    public int level() { return def.level; }
    public String spriteKey() { return def.spriteKey; }
    public boolean inCombat() { return inCombat; }
    public void setInCombat(boolean v) { inCombat = v; }
    public String name() { return def.name; }
    public boolean isPlayer() { return false; }
    public int hp() { return hp; }
    public int maxHp() { return def.maxHp; }
    public boolean alive() { return hp > 0; }
    public int attackPower() { return def.attackPower; }
    public int defense() { return def.defense; }
    public int takeDamage(int amount, DamageType type) {
        int dealt = Math.max(1, amount - def.defense);
        hp = Math.max(0, hp - dealt);
        return dealt;
    }
    public void heal(int amount) { hp = Math.min(def.maxHp, hp + Math.max(0, amount)); }
}

final class FakeNode implements WorldModel.NodeEntity {
    private final String id, name; private final GridPos pos; private boolean depleted; private double respawn;
    FakeNode(String id, String name, GridPos pos) { this.id = id; this.name = name; this.pos = pos; }
    public String id() { return id; }
    public String name() { return name; }
    public GridPos pos() { return pos; }
    public boolean depleted() { return depleted; }
    public void setDepleted(boolean v) { depleted = v; }
    public double respawnRemaining() { return respawn; }
    public void setRespawnRemaining(double sec) { respawn = sec; }
}

final class FakePortal implements WorldModel.Portal {
    private final GridPos pos; private final String target, label;
    FakePortal(GridPos pos, String target, String label) { this.pos = pos; this.target = target; this.label = label; }
    public GridPos pos() { return pos; }
    public String targetZoneId() { return target; }
    public String label() { return label; }
}
