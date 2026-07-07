package com.whim.albion.engine.dev;

import com.whim.albion.api.ActionResult;
import com.whim.albion.api.Combatant;
import com.whim.albion.api.Content;
import com.whim.albion.api.Defs.ItemDef;
import com.whim.albion.api.Defs.MonsterDef;
import com.whim.albion.api.Defs.SpellDef;
import com.whim.albion.api.GameContext;
import com.whim.albion.api.GameModel;
import com.whim.albion.api.GridPos;
import com.whim.albion.api.ModelFactory;
import com.whim.albion.api.PartyModel;
import com.whim.albion.api.WorldModel;
import com.whim.albion.api.Enums.DamageType;
import com.whim.albion.api.Enums.Direction;
import com.whim.albion.api.Enums.EnemyBehaviorType;
import com.whim.albion.api.Enums.EquipSlot;
import com.whim.albion.api.Enums.ItemType;
import com.whim.albion.api.Enums.MapType;
import com.whim.albion.api.Enums.QuestStatus;
import com.whim.albion.api.Enums.SkillType;
import com.whim.albion.api.Enums.SpellEffectType;
import com.whim.albion.api.Enums.SpellSchool;
import com.whim.albion.api.Enums.StatType;
import com.whim.albion.api.Enums.TargetType;
import com.whim.albion.api.Enums.TileType;
import com.whim.albion.api.Views;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal, in-package fake {@link ModelFactory} used ONLY to exercise the engine
 * before Task 1's real {@code AlbionModelFactory} lands. It is intentionally tiny
 * (a 6x6 town, a small dungeon, three party members, two content spells/items and a
 * couple of monsters) and lives under the engine's own {@code dev} sub-package, so
 * it satisfies the "depend on api only" rule. It is NOT the production model.
 */
public final class FakeModelFactory implements ModelFactory {

    @Override public GameModel newGame(long seed) { return new FakeGameModel(); }
}

// ===================================================================== model

final class FakeGameModel implements GameModel {
    private final FakeContent content = new FakeContent();
    private final FakeJournal journal = new FakeJournal();
    private final FakeParty party = new FakeParty(content);
    private final FakeWorld world = new FakeWorld();

    @Override public WorldModel world() { return world; }
    @Override public PartyModel party() { return party; }
    @Override public Content content() { return content; }
    @Override public JournalModel journal() { return journal; }
}

// =============================================================== combatant/char

/** Doubles as a party member (CharacterView) and a combat participant (Combatant). */
final class FakeChar implements Views.CharacterView, Combatant {
    final String id, name, profession, portraitKey, spriteKey;
    final EnumMap<StatType, Integer> stats = new EnumMap<StatType, Integer>(StatType.class);
    final EnumMap<SkillType, Integer> skills = new EnumMap<SkillType, Integer>(SkillType.class);
    final boolean playerSide;
    final EnemyBehaviorType behavior;
    final boolean ranged;
    final int attack, defense;
    final DamageType damageType;
    int level = 1, xp = 0;
    int lp, maxLp, sp, maxSp;
    GridPos pos = new GridPos(0, 0);
    boolean defending;
    final List<SpellDef> spells = new ArrayList<SpellDef>();
    final List<ItemStack> pack = new ArrayList<ItemStack>();
    final FakeContent content;

    FakeChar(FakeContent content, String id, String name, String profession, boolean playerSide,
             EnemyBehaviorType behavior, boolean ranged, int attack, int defense,
             DamageType dt, int maxLp, int maxSp) {
        this.content = content;
        this.id = id; this.name = name; this.profession = profession;
        this.portraitKey = "portrait:" + id; this.spriteKey = "sprite:" + id;
        this.playerSide = playerSide; this.behavior = behavior; this.ranged = ranged;
        this.attack = attack; this.defense = defense; this.damageType = dt;
        this.maxLp = maxLp; this.lp = maxLp; this.maxSp = maxSp; this.sp = maxSp;
    }

    FakeChar stat(StatType s, int v) { stats.put(s, v); return this; }
    FakeChar skill(SkillType s, int v) { skills.put(s, v); return this; }

    // ---- shared ----
    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public int stat(StatType type) { Integer v = stats.get(type); return v == null ? 10 : v; }
    @Override public int skill(SkillType type) { Integer v = skills.get(type); return v == null ? 0 : v; }
    @Override public int lp() { return lp; }
    @Override public int maxLp() { return maxLp; }
    @Override public int sp() { return sp; }
    @Override public int maxSp() { return maxSp; }
    @Override public boolean alive() { return lp > 0; }

    // ---- CharacterView ----
    @Override public String portraitKey() { return portraitKey; }
    @Override public String profession() { return profession; }
    @Override public int level() { return level; }
    @Override public int xp() { return xp; }
    @Override public int xpToNext() { return level * 100 - xp; }
    @Override public List<Views.ItemView> inventory() {
        List<Views.ItemView> out = new ArrayList<Views.ItemView>();
        for (ItemStack st : pack) out.add(new FakeItemView(content.item(st.itemId), st.qty));
        return out;
    }
    @Override public Views.ItemView equipped(EquipSlot slot) { return null; }
    @Override public List<Views.SpellView> spells() {
        List<Views.SpellView> out = new ArrayList<Views.SpellView>();
        for (SpellDef sd : spells) out.add(new FakeSpellView(sd, this));
        return out;
    }
    @Override public boolean canCast(SpellSchool school) {
        for (SpellDef sd : spells) if (sd.school == school) return true;
        return false;
    }

    // ---- Combatant ----
    @Override public boolean playerSide() { return playerSide; }
    @Override public String spriteKey() { return spriteKey; }
    @Override public EnemyBehaviorType behavior() { return behavior; }
    @Override public GridPos pos() { return pos; }
    @Override public void setPos(GridPos p) { this.pos = p; }
    @Override public int attackPower() { return attack; }
    @Override public DamageType attackType() { return damageType; }
    @Override public int defense() { return defense; }
    @Override public boolean ranged() { return ranged; }
    @Override public int takeDamage(int amount) {
        int mit = defense + (defending ? 2 : 0);
        int applied = Math.max(1, amount - mit);
        int before = lp;
        lp = Math.max(0, lp - applied);
        return before - lp;
    }
    @Override public int heal(int amount) {
        int before = lp; lp = Math.min(maxLp, lp + Math.max(0, amount)); return lp - before;
    }
    @Override public boolean spendSp(int amount) {
        if (sp < amount) return false; sp -= amount; return true;
    }
    @Override public void restoreSp(int amount) { sp = Math.min(maxSp, sp + Math.max(0, amount)); }
    @Override public List<SpellDef> knownSpells() { return spells; }
    @Override public boolean defending() { return defending; }
    @Override public void setDefending(boolean d) { this.defending = d; }
}

final class ItemStack { final String itemId; int qty; ItemStack(String id, int q) { itemId = id; qty = q; } }

final class FakeItemView implements Views.ItemView {
    private final ItemDef def; private final int qty;
    FakeItemView(ItemDef def, int qty) { this.def = def; this.qty = qty; }
    @Override public String id() { return def == null ? "?" : def.id; }
    @Override public String name() { return def == null ? "?" : def.name; }
    @Override public ItemType type() { return def == null ? ItemType.MISC : def.type; }
    @Override public EquipSlot slot() { return def == null ? null : def.slot; }
    @Override public int quantity() { return qty; }
    @Override public int value() { return def == null ? 0 : def.value; }
    @Override public String description() { return def == null ? "" : def.description; }
    @Override public String spriteKey() { return def == null ? "" : def.spriteKey; }
}

final class FakeSpellView implements Views.SpellView {
    private final SpellDef def; private final FakeChar caster;
    FakeSpellView(SpellDef def, FakeChar caster) { this.def = def; this.caster = caster; }
    @Override public String id() { return def.id; }
    @Override public String name() { return def.name; }
    @Override public SpellSchool school() { return def.school; }
    @Override public int spCost() { return def.spCost; }
    @Override public boolean castable() { return caster.sp() >= def.spCost && caster.level() >= def.levelReq; }
    @Override public String description() { return def.description; }
}

// ===================================================================== party

final class FakeParty implements PartyModel {
    private final List<FakeChar> members = new ArrayList<FakeChar>();
    private int activeIndex = 0;
    private int gold = 120;
    private final FakeContent content;

    FakeParty(FakeContent content) {
        this.content = content;
        FakeChar scout = new FakeChar(content, "scout", "Ranger Vell", "Ranger", true, null, true,
                6, 2, DamageType.PHYSICAL, 26, 6)
                .stat(StatType.STRENGTH, 12).stat(StatType.DEXTERITY, 16).stat(StatType.SPEED, 15)
                .stat(StatType.LUCK, 8).skill(SkillType.RANGED, 55).skill(SkillType.CRITICAL, 20);
        FakeChar mage = new FakeChar(content, "mage", "Adept Iri", "Mage", true, null, false,
                3, 1, DamageType.PHYSICAL, 18, 20)
                .stat(StatType.STRENGTH, 8).stat(StatType.DEXTERITY, 11).stat(StatType.SPEED, 12)
                .stat(StatType.INTELLIGENCE, 16).stat(StatType.MAGIC_TALENT, 16);
        mage.spells.add(content.spell("bolt"));
        FakeChar healer = new FakeChar(content, "healer", "Sister Ona", "Healer", true, null, false,
                4, 2, DamageType.PHYSICAL, 22, 16)
                .stat(StatType.STRENGTH, 10).stat(StatType.DEXTERITY, 12).stat(StatType.SPEED, 11)
                .stat(StatType.MAGIC_TALENT, 12).skill(SkillType.MELEE, 30);
        healer.spells.add(content.spell("mend"));
        members.add(scout); members.add(mage); members.add(healer);
        scout.pack.add(new ItemStack("potion_heal", 2));
    }

    @Override public List<Views.CharacterView> members() {
        return new ArrayList<Views.CharacterView>(members);
    }
    @Override public int activeIndex() { return activeIndex; }
    @Override public void setActiveIndex(int index) { if (index >= 0 && index < members.size()) activeIndex = index; }
    @Override public int gold() { return gold; }
    @Override public void addGold(int amount) { gold += Math.max(0, amount); }
    @Override public boolean spendGold(int amount) { if (amount < 0 || gold < amount) return false; gold -= amount; return true; }

    @Override public List<Combatant> asCombatants() { return new ArrayList<Combatant>(members); }

    @Override public boolean giveItem(int memberIndex, String itemId, int quantity) {
        if (memberIndex < 0 || memberIndex >= members.size() || content.item(itemId) == null) return false;
        List<ItemStack> pack = members.get(memberIndex).pack;
        for (ItemStack st : pack) if (st.itemId.equals(itemId)) { st.qty += quantity; return true; }
        pack.add(new ItemStack(itemId, quantity));
        return true;
    }
    @Override public boolean takeItem(String itemId, int quantity) {
        for (FakeChar m : members) {
            for (ItemStack st : m.pack) if (st.itemId.equals(itemId) && st.qty >= quantity) {
                st.qty -= quantity; if (st.qty == 0) m.pack.remove(st); return true;
            }
        }
        return false;
    }
    @Override public boolean hasItem(String itemId) {
        for (FakeChar m : members) for (ItemStack st : m.pack) if (st.itemId.equals(itemId) && st.qty > 0) return true;
        return false;
    }
    @Override public ActionResult equip(int memberIndex, String itemId) { return ActionResult.ok("Equipped."); }
    @Override public ActionResult unequip(int memberIndex, EquipSlot slot) { return ActionResult.ok("Unequipped."); }
    @Override public ActionResult useItem(int memberIndex, String itemId) {
        if (memberIndex < 0 || memberIndex >= members.size()) return ActionResult.fail("No member.");
        ItemDef def = content.item(itemId);
        if (def == null || def.type != ItemType.CONSUMABLE) return ActionResult.fail("Cannot use that.");
        if (!hasItem(itemId)) return ActionResult.fail("None left.");
        FakeChar m = members.get(memberIndex);
        int healed = m.heal(def.healAmount); m.restoreSp(def.manaAmount);
        takeItem(itemId, 1);
        return ActionResult.ok(m.name + " recovers " + healed + " LP.");
    }
    @Override public void awardXp(int xp) {
        for (FakeChar m : members) if (m.alive()) {
            m.xp += xp;
            while (m.xp >= m.level * 100) { m.xp -= m.level * 100; m.level++; m.maxLp += 4; m.lp = m.maxLp; m.maxSp += 2; }
        }
    }
    @Override public boolean wiped() {
        for (FakeChar m : members) if (m.alive()) return false;
        return true;
    }
}

// =================================================================== content

final class FakeContent implements Content {
    private final Map<String, ItemDef> items = new LinkedHashMap<String, ItemDef>();
    private final Map<String, SpellDef> spells = new LinkedHashMap<String, SpellDef>();
    private final Map<String, MonsterDef> monsters = new LinkedHashMap<String, MonsterDef>();

    FakeContent() {
        items.put("potion_heal", ItemDef.builder("potion_heal", "Salve Flask", ItemType.CONSUMABLE)
                .heal(12).value(15).description("Restores 12 LP.").sprite("item:potion").build());
        items.put("dungeon_key", ItemDef.builder("dungeon_key", "Iron Key", ItemType.KEY)
                .value(0).description("Opens the vault.").sprite("item:key").build());
        items.put("relic", ItemDef.builder("relic", "Sun Sigil", ItemType.QUEST)
                .value(0).description("A quest relic.").sprite("item:relic").build());

        spells.put("bolt", SpellDef.builder("bolt", "Arc Bolt", SpellSchool.DESTRUCTION)
                .effect(SpellEffectType.DAMAGE).target(TargetType.SINGLE_ENEMY)
                .damageType(DamageType.FIRE).spCost(3).magnitude(8).description("A searing bolt.").build());
        spells.put("mend", SpellDef.builder("mend", "Mend", SpellSchool.RESTORATION)
                .effect(SpellEffectType.HEAL).target(TargetType.SINGLE_ALLY)
                .spCost(3).magnitude(10).description("Knit a wound.").build());
    }

    @Override public ItemDef item(String id) { return items.get(id); }
    @Override public SpellDef spell(String id) { return spells.get(id); }
    @Override public MonsterDef monster(String id) { return monsters.get(id); }

    @Override public List<Combatant> spawnEncounter(String encounterId) {
        List<Combatant> out = new ArrayList<Combatant>();
        if ("rats".equals(encounterId)) {
            out.add(rat("rat")); out.add(rat("rat"));
        } else if ("guard".equals(encounterId)) {
            out.add(rat("rat")); out.add(rat("rat")); out.add(rat("rat"));
        }
        return out;
    }

    private FakeChar rat(String id) {
        FakeChar c = new FakeChar(this, id, "Cave Rat", "beast", false, EnemyBehaviorType.AGGRESSIVE,
                false, 3, 0, DamageType.PHYSICAL, 6, 0)
                .stat(StatType.STRENGTH, 8).stat(StatType.DEXTERITY, 9).stat(StatType.SPEED, 10)
                .stat(StatType.LUCK, 2);
        monsters.put(id, MonsterDef.builder(id, "Cave Rat").behavior(EnemyBehaviorType.AGGRESSIVE)
                .maxLp(6).attack(3).xp(20).gold(6).build());
        return c;
    }

    @Override public DialogueTree dialogue(String dialogueId) { return new FakeDialogue(); }
}

// One tiny tree: the elder offers a quest, then farewell.
final class FakeDialogue implements Content.DialogueTree {
    @Override public String rootId() { return "root"; }
    @Override public Node node(String nodeId) {
        if ("done".equals(nodeId)) return new N("done", "Elder", "portrait:elder",
                "Bring back the Sun Sigil. Go carefully.", new ArrayList<Option>());
        List<Option> opts = new ArrayList<Option>();
        opts.add(new O("Accept the task", "done", true) {
            @Override public void apply(GameContext ctx) {
                ctx.startQuest("recover_relic");
                ctx.addObjective("recover_relic", "Find the Sun Sigil in the vault.");
                ctx.setFlag("elder_quest", true);
                ctx.giveItem("dungeon_key", 1);
                ctx.notify("Quest started: Recover Relic.");
            }
        });
        opts.add(new O("Not now", null, true) { @Override public void apply(GameContext ctx) { } });
        return new N("root", "Elder", "portrait:elder", "Traveler, our vault has been robbed. Will you help?", opts);
    }

    static final class N implements Node {
        final String id, speaker, portrait, text; final List<Option> opts;
        N(String id, String sp, String pt, String tx, List<Option> o) { this.id = id; this.speaker = sp; this.portrait = pt; this.text = tx; this.opts = o; }
        @Override public String id() { return id; }
        @Override public String speaker() { return speaker; }
        @Override public String portraitKey() { return portrait; }
        @Override public String text() { return text; }
        @Override public List<Option> options() { return opts; }
    }
    abstract static class O implements Option {
        final String label, next; final boolean avail;
        O(String label, String next, boolean avail) { this.label = label; this.next = next; this.avail = avail; }
        @Override public String label() { return label; }
        @Override public String next() { return next; }
        @Override public boolean available(GameContext ctx) { return avail; }
    }
}

// ==================================================================== journal

final class FakeJournal implements GameModel.JournalModel {
    private final Map<String, Boolean> flags = new LinkedHashMap<String, Boolean>();
    private final Map<String, Quest> quests = new LinkedHashMap<String, Quest>();

    @Override public boolean flag(String key) { Boolean b = flags.get(key); return b != null && b; }
    @Override public void setFlag(String key, boolean value) { flags.put(key, value); }
    @Override public void startQuest(String questId, String title, String firstObjective) {
        Quest q = new Quest(title);
        if (firstObjective != null && !firstObjective.isEmpty()) q.objectives.add(firstObjective);
        quests.put(questId, q);
    }
    @Override public void addObjective(String questId, String objective) {
        Quest q = quests.get(questId); if (q != null) q.objectives.add(objective);
    }
    @Override public void completeQuest(String questId) {
        Quest q = quests.get(questId); if (q != null) q.status = QuestStatus.COMPLETED;
    }
    @Override public List<Views.QuestEntryView> quests() {
        List<Views.QuestEntryView> out = new ArrayList<Views.QuestEntryView>();
        for (Quest q : quests.values()) out.add(q);
        return out;
    }
    static final class Quest implements Views.QuestEntryView {
        final String title; QuestStatus status = QuestStatus.ACTIVE; final List<String> objectives = new ArrayList<String>();
        Quest(String title) { this.title = title; }
        @Override public String title() { return title; }
        @Override public QuestStatus status() { return status; }
        @Override public List<String> objectives() { return objectives; }
    }
}

// ===================================================================== world

final class FakeWorld implements WorldModel {
    // Two maps: a 6x6 town (OUTDOOR_2D) and a 5x5 vault (INDOOR_3D).
    private String mapId = "town";
    private int px = 1, py = 1;
    private Direction facing = Direction.SOUTH;
    private final Map<String, Boolean> firedEncounters = new LinkedHashMap<String, Boolean>();
    private boolean chestLooted = false;

    private boolean town() { return "town".equals(mapId); }

    @Override public String mapId() { return mapId; }
    @Override public String mapName() { return town() ? "Riverbrook Town" : "Old Vault"; }
    @Override public MapType mapType() { return town() ? MapType.OUTDOOR_2D : MapType.INDOOR_3D; }
    @Override public int width() { return town() ? 6 : 5; }
    @Override public int height() { return town() ? 6 : 5; }

    @Override public Views.TileView tileAt(int x, int y) {
        boolean border = x <= 0 || y <= 0 || x >= width() - 1 || y >= height() - 1;
        TileType t;
        if (border) t = town() ? TileType.OBSTACLE : TileType.WALL;
        else t = town() ? TileType.GRASS : TileType.FLOOR;
        return new Tile(t, !border, border);
    }

    @Override public Views.PlayerView player() {
        return new Views.PlayerView() {
            @Override public int x() { return px; }
            @Override public int y() { return py; }
            @Override public Direction facing() { return facing; }
        };
    }

    @Override public List<Views.NpcView> npcs() {
        List<Views.NpcView> out = new ArrayList<Views.NpcView>();
        if (town()) out.add(npc(2, 1, "Elder", "sprite:elder"));
        return out;
    }
    private Views.NpcView npc(final int x, final int y, final String name, final String sprite) {
        return new Views.NpcView() {
            @Override public int x() { return x; }
            @Override public int y() { return y; }
            @Override public String name() { return name; }
            @Override public String spriteKey() { return sprite; }
            @Override public boolean hostile() { return false; }
        };
    }

    @Override public boolean stepPlayer(Direction dir) {
        facing = dir;                                   // turn even if blocked (blobber feel)
        int nx = px + dir.dx(), ny = py + dir.dy();
        if (!inBounds(nx, ny)) return false;
        if (!tileAt(nx, ny).walkable()) return false;
        if (interactableAt(nx, ny) != null) return false;  // NPC/chest block the tile
        px = nx; py = ny; return true;
    }
    @Override public void turnPlayer(Direction newFacing) { this.facing = newFacing; }
    @Override public void loadMap(String mapId, int x, int y, Direction facing) {
        this.mapId = mapId; this.px = x; this.py = y; this.facing = facing;
    }

    @Override public Transition transitionAt(final int x, final int y) {
        if (town() && x == 4 && y == 4) return link("vault", 2, 1, Direction.SOUTH);
        if (!town() && x == 2 && y == 1) return link("town", 4, 4, Direction.NORTH);
        return null;
    }
    private Transition link(final String map, final int tx, final int ty, final Direction tf) {
        return new Transition() {
            @Override public String targetMapId() { return map; }
            @Override public int targetX() { return tx; }
            @Override public int targetY() { return ty; }
            @Override public Direction targetFacing() { return tf; }
        };
    }

    @Override public Interactable interactableAt(int x, int y) {
        if (town() && x == 2 && y == 1) return npcInteract("elder", "Elder", "elder");
        if (!town() && x == 2 && y == 3 && !chestLooted) return chest();
        return null;
    }
    private Interactable npcInteract(final String id, final String name, final String dlg) {
        return new Interactable() {
            @Override public String id() { return id; }
            @Override public String name() { return name; }
            @Override public String dialogueId() { return dlg; }
            @Override public List<String> loot() { return new ArrayList<String>(); }
            @Override public boolean consumed() { return false; }
            @Override public void consume() { }
        };
    }
    private Interactable chest() {
        final List<String> loot = new ArrayList<String>(); loot.add("relic");
        return new Interactable() {
            @Override public String id() { return "vault_chest"; }
            @Override public String name() { return "Vault Chest"; }
            @Override public String dialogueId() { return null; }
            @Override public List<String> loot() { return loot; }
            @Override public boolean consumed() { return chestLooted; }
            @Override public void consume() { chestLooted = true; }
        };
    }

    @Override public String encounterAt(int x, int y) {
        String key = mapId + ":" + x + ":" + y;
        if (firedEncounters.containsKey(key)) return null;
        if (town() && x == 1 && y == 2) return "rats";
        if (!town() && x == 2 && y == 2) return "guard";
        return null;
    }
    @Override public void clearEncounter(int x, int y) { firedEncounters.put(mapId + ":" + x + ":" + y, true); }

    private boolean inBounds(int x, int y) { return x >= 0 && y >= 0 && x < width() && y < height(); }

    static final class Tile implements Views.TileView {
        final TileType t; final boolean walk, sight;
        Tile(TileType t, boolean walk, boolean sight) { this.t = t; this.walk = walk; this.sight = sight; }
        @Override public TileType type() { return t; }
        @Override public boolean walkable() { return walk; }
        @Override public boolean blocksSight() { return sight; }
        @Override public String decorKey() { return ""; }
    }
}
