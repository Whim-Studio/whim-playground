package com.whim.albion.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.whim.albion.api.ActionResult;
import com.whim.albion.api.GameController;
import com.whim.albion.api.Views;
import com.whim.albion.api.Views.CharacterView;
import com.whim.albion.api.Views.CombatView;
import com.whim.albion.api.Views.CombatantView;
import com.whim.albion.api.Views.DialogueView;
import com.whim.albion.api.Views.GameStateView;
import com.whim.albion.api.Views.ItemView;
import com.whim.albion.api.Views.JournalView;
import com.whim.albion.api.Views.NpcView;
import com.whim.albion.api.Views.PartyView;
import com.whim.albion.api.Views.PlayerView;
import com.whim.albion.api.Views.QuestEntryView;
import com.whim.albion.api.Views.SpellView;
import com.whim.albion.api.Views.TileView;
import com.whim.albion.api.Views.WorldView;
import com.whim.albion.api.Enums.CombatActionType;
import com.whim.albion.api.Enums.Direction;
import com.whim.albion.api.Enums.EquipSlot;
import com.whim.albion.api.Enums.GameStateType;
import com.whim.albion.api.Enums.ItemType;
import com.whim.albion.api.Enums.MapType;
import com.whim.albion.api.Enums.QuestStatus;
import com.whim.albion.api.Enums.SkillType;
import com.whim.albion.api.Enums.SpellSchool;
import com.whim.albion.api.Enums.StatType;
import com.whim.albion.api.Enums.TileType;

/**
 * Development-only {@link GameController} returning hand-built fake state so the whole
 * Swing UI can be exercised before the real engine (Task 2) and model (Task 1) land.
 *
 * <p>It is deliberately stateful and mildly interactive: you can walk the town, click to
 * move, step through a first-person dungeon, open dialogue, poke at a scripted combat, and
 * flip between the inventory / character-sheet / journal overlays. It is NOT the game — it
 * exists purely so a human can visually verify layout, renderers and wiring. The real entry
 * point ({@code app.Main}) injects a {@code GameEngine}; this class is discarded then.
 */
public final class StubController implements GameController {

    private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();

    private GameStateType current = GameStateType.TITLE;
    private GameStateType overlayReturn = GameStateType.OVERWORLD;
    private String status = "Welcome to the demo shell.";
    private int gold = 275;
    private int activeMember = 0;

    // ---- maps -------------------------------------------------------------
    private final StubWorld town = buildTown();
    private final StubWorld dungeon = buildDungeon();
    private StubWorld world = town;

    // ---- party ------------------------------------------------------------
    private final List<StubChar> party = buildParty();

    // ---- journal ----------------------------------------------------------
    private final List<StubQuest> quests = buildQuests();

    // ---- dialogue ---------------------------------------------------------
    private int dialogueNode = -1;
    private String dialogueSpeaker = "";
    private String dialoguePortrait = "";

    // ---- combat -----------------------------------------------------------
    private StubCombat combat;

    // ======================================================================

    @Override
    public GameStateView state() {
        return new StubState();
    }

    @Override
    public void newGame(long seed) {
        world = town;
        world.px = 4; world.py = 6; world.facing = Direction.NORTH;
        current = GameStateType.OVERWORLD;
        status = "A new adventure begins in Karnthal village.";
        fire();
    }

    @Override
    public List<String> saveSlots() {
        List<String> s = new ArrayList<String>();
        s.add("Slot 1 — Karnthal, Lv3 (demo)");
        s.add("Slot 2 — <empty>");
        s.add("Slot 3 — <empty>");
        return s;
    }

    @Override
    public boolean saveGame(int slot) { status = "Saved to slot " + (slot + 1) + " (stub)."; fire(); return true; }

    @Override
    public boolean loadGame(int slot) { status = "Loaded slot " + (slot + 1) + " (stub)."; newGame(0); return true; }

    @Override
    public ActionResult selectMenuOption(int index) {
        if (current == GameStateType.TITLE) {
            if (index == 0) { newGame(0); return ActionResult.ok("New game"); }
            if (index == 1) { current = GameStateType.MENU; overlayReturn = GameStateType.TITLE; fire(); return ActionResult.ok("Load"); }
            System.exit(0);
        } else if (current == GameStateType.GAME_OVER) {
            if (index == 0) { newGame(0); return ActionResult.ok(); }
            current = GameStateType.TITLE; fire(); return ActionResult.ok();
        } else if (current == GameStateType.MENU) {
            // save/load slot list
            loadGame(index);
            return ActionResult.ok();
        }
        return ActionResult.ok();
    }

    @Override
    public ActionResult move(Direction dir) {
        if (current == GameStateType.OVERWORLD) return stepOutdoor(dir);
        if (current == GameStateType.DUNGEON) return stepDungeon(dir);
        return ActionResult.fail("Cannot move now.");
    }

    private ActionResult stepOutdoor(Direction dir) {
        world.facing = dir;
        int nx = world.px + dir.dx(), ny = world.py + dir.dy();
        if (!world.walkable(nx, ny)) { status = "Blocked."; fire(); return ActionResult.fail("Blocked."); }
        world.px = nx; world.py = ny;
        // dungeon entrance?
        if (world == town && world.tile(nx, ny).type() == TileType.STAIRS) {
            enterDungeon();
            return ActionResult.ok("You descend into the crypt.");
        }
        status = "";
        fire();
        return ActionResult.ok();
    }

    private ActionResult stepDungeon(Direction dir) {
        // forward/back = step along facing/opposite; left/right = turn
        if (dir == Direction.WEST) { world.facing = world.facing.left(); fire(); return ActionResult.ok(); }
        if (dir == Direction.EAST) { world.facing = world.facing.right(); fire(); return ActionResult.ok(); }
        Direction step = (dir == Direction.NORTH) ? world.facing : world.facing.opposite();
        int nx = world.px + step.dx(), ny = world.py + step.dy();
        if (!world.walkable(nx, ny)) { status = "The wall blocks your path."; fire(); return ActionResult.fail("Wall."); }
        world.px = nx; world.py = ny;
        // step onto stairs back to town
        if (world.tile(nx, ny).type() == TileType.STAIRS) {
            world = town; current = GameStateType.OVERWORLD;
            status = "You climb back to the surface.";
            fire();
            return ActionResult.ok();
        }
        // scripted encounter cell
        if (nx == dungeon.encounterX && ny == dungeon.encounterY && combat == null) {
            startStubCombat();
            return ActionResult.ok("Ambush!");
        }
        status = "";
        fire();
        return ActionResult.ok();
    }

    private void enterDungeon() {
        world = dungeon;
        world.px = 3; world.py = 8; world.facing = Direction.NORTH;
        current = GameStateType.DUNGEON;
        status = "The air grows cold and damp.";
        fire();
    }

    @Override
    public ActionResult moveTo(int x, int y) {
        if (current != GameStateType.OVERWORLD) return ActionResult.fail("Only outdoors.");
        int dx = Integer.compare(x, world.px);
        int dy = Integer.compare(y, world.py);
        // prefer the larger axis for a single step
        if (Math.abs(x - world.px) >= Math.abs(y - world.py) && dx != 0) {
            return stepOutdoor(dx > 0 ? Direction.EAST : Direction.WEST);
        } else if (dy != 0) {
            return stepOutdoor(dy > 0 ? Direction.SOUTH : Direction.NORTH);
        }
        return ActionResult.ok();
    }

    @Override
    public ActionResult interact() {
        if (current == GameStateType.OVERWORLD) {
            int fx = world.px + world.facing.dx(), fy = world.py + world.facing.dy();
            for (StubNpc n : world.npcs) {
                if (n.x == fx && n.y == fy) { openDialogue(n); return ActionResult.ok("Talking to " + n.name); }
            }
            if (world.tile(fx, fy).type() == TileType.STAIRS) { enterDungeon(); return ActionResult.ok(); }
            status = "You see nothing of interest.";
            fire();
            return ActionResult.ok();
        }
        if (current == GameStateType.DUNGEON) {
            int fx = world.px + world.facing.dx(), fy = world.py + world.facing.dy();
            if (fx == dungeon.chestX && fy == dungeon.chestY && !dungeon.chestLooted) {
                dungeon.chestLooted = true;
                gold += 120;
                completeQuest();
                status = "You recover the Sunstone Shard and 120 gold!";
                fire();
                return ActionResult.ok();
            }
            status = "Cold stone. Nothing here.";
            fire();
            return ActionResult.ok();
        }
        return ActionResult.fail("Nothing to do.");
    }

    private void openDialogue(StubNpc n) {
        dialogueSpeaker = n.name;
        dialoguePortrait = n.portrait;
        dialogueNode = 0;
        current = GameStateType.DIALOGUE;
        status = "";
        fire();
    }

    @Override
    public ActionResult selectDialogueOption(int index) {
        // tiny branching tree: node 0 root -> quest / shop / bye
        if (dialogueNode == 0) {
            if (index == 0) { dialogueNode = 1; activateQuest(); }
            else if (index == 1) { dialogueNode = 2; }
            else { endDialogue(); }
        } else {
            endDialogue();
        }
        fire();
        return ActionResult.ok();
    }

    private void endDialogue() {
        dialogueNode = -1;
        current = GameStateType.OVERWORLD;
        status = "";
    }

    // ---- combat ----------------------------------------------------------

    private void startStubCombat() {
        combat = new StubCombat();
        current = GameStateType.COMBAT;
        status = "Combat! Choose an action.";
        fire();
    }

    @Override
    public ActionResult combatAction(CombatActionType type, int targetIndex, String optionId) {
        if (combat == null) return ActionResult.fail("Not in combat.");
        String msg = combat.act(type, targetIndex, optionId);
        if (combat.finished) {
            boolean win = combat.victory;
            combat = null;
            if (win) {
                gold += 60;
                current = GameStateType.DUNGEON;
                status = "Victory! +60 gold.";
            } else {
                current = GameStateType.GAME_OVER;
                status = "Your party has fallen...";
            }
        }
        fire();
        return ActionResult.ok(msg);
    }

    // ---- party / inventory ----------------------------------------------

    @Override public void setActiveMember(int index) {
        if (index >= 0 && index < party.size()) { activeMember = index; fire(); }
    }

    @Override public ActionResult equip(int memberIndex, String itemId) {
        status = "Equipped " + itemId + " (stub)."; fire(); return ActionResult.ok();
    }

    @Override public ActionResult unequip(int memberIndex, EquipSlot slot) {
        status = "Unequipped " + slot + " (stub)."; fire(); return ActionResult.ok();
    }

    @Override public ActionResult useItem(int memberIndex, String itemId) {
        StubChar c = party.get(Math.max(0, Math.min(memberIndex, party.size() - 1)));
        if (itemId != null && itemId.contains("potion")) { c.lp = Math.min(c.maxLp, c.lp + 25); }
        status = "Used " + itemId + "."; fire(); return ActionResult.ok();
    }

    @Override public void openState(GameStateType s) {
        if (current != GameStateType.INVENTORY && current != GameStateType.CHARACTER_SHEET
                && current != GameStateType.JOURNAL && current != GameStateType.MENU) {
            overlayReturn = current;
        }
        current = s;
        fire();
    }

    @Override public void closeOverlay() {
        current = overlayReturn;
        fire();
    }

    @Override public void addChangeListener(ChangeListener l) { if (l != null) listeners.add(l); }
    @Override public void removeChangeListener(ChangeListener l) { listeners.remove(l); }

    private void fire() {
        for (ChangeListener l : new ArrayList<ChangeListener>(listeners)) l.onStateChanged();
    }

    // ---- quest helpers ---------------------------------------------------

    private void activateQuest() {
        if (quests.get(0).status == QuestStatus.ACTIVE) return;
        quests.get(0).status = QuestStatus.ACTIVE;
        status = "New quest: The Sunstone Shard.";
    }

    private void completeQuest() {
        quests.get(0).status = QuestStatus.COMPLETED;
        quests.get(0).objectives.set(0, "[x] Recover the Sunstone Shard");
    }

    // ======================================================================
    // Fake data builders
    // ======================================================================

    private static List<StubChar> buildParty() {
        List<StubChar> p = new ArrayList<StubChar>();
        p.add(new StubChar("Brannor", "portrait.warrior", "Warrior", 4, 340, 500, 96, 96, 12, 12, false));
        p.add(new StubChar("Sela", "portrait.ranger", "Ranger", 3, 210, 400, 74, 80, 34, 40, false));
        p.add(new StubChar("Ioric", "portrait.mage", "Mage", 3, 260, 400, 52, 55, 60, 70, true));
        p.add(new StubChar("Vryna", "portrait.healer", "Healer", 3, 180, 400, 61, 66, 55, 64, true));
        return p;
    }

    private static List<StubQuest> buildQuests() {
        List<StubQuest> q = new ArrayList<StubQuest>();
        List<String> obj = new ArrayList<String>();
        obj.add("[ ] Recover the Sunstone Shard");
        obj.add("[ ] Return it to Elder Maevis");
        q.add(new StubQuest("The Sunstone Shard", QuestStatus.ACTIVE, obj));
        List<String> obj2 = new ArrayList<String>();
        obj2.add("[x] Reach Karnthal village");
        q.add(new StubQuest("Arrival", QuestStatus.COMPLETED, obj2));
        return q;
    }

    private static StubWorld buildTown() {
        int w = 12, h = 10;
        TileType[][] t = new TileType[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                t[y][x] = TileType.GRASS;
            }
        }
        // border obstacles
        for (int x = 0; x < w; x++) { t[0][x] = TileType.OBSTACLE; t[h - 1][x] = TileType.OBSTACLE; }
        for (int y = 0; y < h; y++) { t[y][0] = TileType.OBSTACLE; t[y][w - 1] = TileType.OBSTACLE; }
        // a path down the middle
        for (int y = 1; y < h - 1; y++) t[y][4] = TileType.PATH;
        for (int x = 4; x < 9; x++) t[5][x] = TileType.PATH;
        // pond
        t[2][7] = TileType.WATER; t[2][8] = TileType.WATER; t[3][7] = TileType.WATER;
        // dungeon entrance
        t[1][8] = TileType.STAIRS;
        // a building
        t[7][7] = TileType.WALL; t[7][8] = TileType.WALL; t[8][7] = TileType.WALL;
        t[7][6] = TileType.DOOR;

        StubWorld sw = new StubWorld("Karnthal Village", MapType.OUTDOOR_2D, t);
        sw.decor.put(key(2, 2), "tree.oak");
        sw.decor.put(key(3, 3), "tree.oak");
        sw.decor.put(key(9, 3), "rock.boulder");
        sw.decor.put(key(8, 1), "stairs.dungeon");
        sw.decor.put(key(6, 8), "chest.town");
        sw.npcs.add(new StubNpc(6, 5, "Elder Maevis", "npc.elder", "portrait.elder", false));
        sw.npcs.add(new StubNpc(8, 5, "Doran the Smith", "npc.smith", "portrait.smith", false));
        sw.npcs.add(new StubNpc(3, 7, "Wary Guard", "npc.guard", "portrait.guard", false));
        sw.px = 4; sw.py = 6; sw.facing = Direction.NORTH;
        return sw;
    }

    private static StubWorld buildDungeon() {
        int w = 9, h = 11;
        TileType[][] t = new TileType[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                t[y][x] = TileType.WALL;
        // carve a simple corridor + rooms
        for (int y = 1; y < 10; y++) t[y][3] = TileType.FLOOR;   // main vertical corridor
        for (int x = 1; x < 8; x++) t[5][x] = TileType.FLOOR;     // cross corridor
        t[1][3] = TileType.STAIRS;                                 // exit up
        t[9][3] = TileType.FLOOR;
        t[5][1] = TileType.FLOOR;
        t[5][7] = TileType.FLOOR;
        t[6][7] = TileType.FLOOR; t[7][7] = TileType.FLOOR;
        t[7][6] = TileType.DOOR;                                   // locked door to treasure
        t[8][7] = TileType.FLOOR;

        StubWorld sw = new StubWorld("Karnthal Crypt", MapType.INDOOR_3D, t);
        sw.encounterX = 3; sw.encounterY = 5;
        sw.chestX = 7; sw.chestY = 8;
        sw.decor.put(key(7, 8), "chest.treasure");
        sw.px = 3; sw.py = 8; sw.facing = Direction.NORTH;
        return sw;
    }

    private static String key(int x, int y) { return x + "," + y; }

    // ======================================================================
    // View adapters over the mutable stub state
    // ======================================================================

    private final class StubState implements GameStateView {
        @Override public GameStateType current() { return current; }
        @Override public WorldView world() {
            return (current == GameStateType.OVERWORLD || current == GameStateType.DUNGEON
                    || current == GameStateType.COMBAT || current == GameStateType.DIALOGUE) ? worldView(world) : null;
        }
        @Override public PartyView party() { return new StubPartyView(); }
        @Override public CombatView combat() { return current == GameStateType.COMBAT ? combat : null; }
        @Override public DialogueView dialogue() { return current == GameStateType.DIALOGUE ? new StubDialogueView() : null; }
        @Override public JournalView journal() { return new StubJournalView(); }
        @Override public int gold() { return gold; }
        @Override public String statusMessage() { return status == null ? "" : status; }
        @Override public List<String> menuOptions() {
            if (current == GameStateType.TITLE) return Arrays.asList("New Game", "Load Game", "Quit");
            if (current == GameStateType.GAME_OVER) return Arrays.asList("Retry", "Title Screen");
            if (current == GameStateType.MENU) return saveSlots();
            return new ArrayList<String>();
        }
    }

    private final class StubPartyView implements PartyView {
        @Override public List<CharacterView> members() {
            List<CharacterView> m = new ArrayList<CharacterView>();
            for (StubChar c : party) m.add(new StubCharView(c));
            return m;
        }
        @Override public int activeIndex() { return activeMember; }
    }

    private final class StubJournalView implements JournalView {
        @Override public List<QuestEntryView> quests() {
            List<QuestEntryView> l = new ArrayList<QuestEntryView>();
            for (StubQuest q : quests) l.add(new StubQuestView(q));
            return l;
        }
    }

    private final class StubDialogueView implements DialogueView {
        @Override public String speaker() { return dialogueSpeaker; }
        @Override public String portraitKey() { return dialoguePortrait; }
        @Override public String text() {
            if (dialogueNode == 0) return "Traveller! The crypt beneath our village has stirred. "
                    + "A shard of the old Sunstone lies within — will you retrieve it?";
            if (dialogueNode == 1) return "Bless you. Seek the locked vault past the cross-corridor. Take care of the guardian.";
            if (dialogueNode == 2) return "My wares? Alas, this is only a demo shell — the shop opens once the engine lands.";
            return "Safe travels.";
        }
        @Override public List<String> options() {
            if (dialogueNode == 0) return Arrays.asList("Accept the quest", "Ask about trade", "Leave");
            return Arrays.asList("Farewell");
        }
    }

    // ---- plain data holders ---------------------------------------------

    private static final class StubChar {
        final String name, portrait, profession; final int level, xp, xpNext, maxLp, maxSp;
        int lp, sp; final boolean caster;
        StubChar(String n, String p, String prof, int lv, int xp, int xpNext, int maxLp, int maxSp,
                 int lp, int sp, boolean caster) {
            this.name = n; this.portrait = p; this.profession = prof; this.level = lv; this.xp = xp;
            this.xpNext = xpNext; this.maxLp = maxLp; this.maxSp = maxSp; this.lp = lp; this.sp = sp;
            this.caster = caster;
        }
    }

    private final class StubCharView implements CharacterView {
        private final StubChar c;
        StubCharView(StubChar c) { this.c = c; }
        @Override public String name() { return c.name; }
        @Override public String portraitKey() { return c.portrait; }
        @Override public String profession() { return c.profession; }
        @Override public int level() { return c.level; }
        @Override public int xp() { return c.xp; }
        @Override public int xpToNext() { return c.xpNext; }
        @Override public int lp() { return c.lp; }
        @Override public int maxLp() { return c.maxLp; }
        @Override public int sp() { return c.sp; }
        @Override public int maxSp() { return c.maxSp; }
        @Override public boolean alive() { return c.lp > 0; }
        @Override public int stat(StatType t) {
            int base = 10 + (t.ordinal() * 3 + c.name.length()) % 18;
            if (t == StatType.STRENGTH && c.profession.equals("Warrior")) base += 8;
            if (t == StatType.MAGIC_TALENT && c.caster) base += 10;
            return base;
        }
        @Override public int skill(SkillType t) { return 20 + (t.ordinal() * 7 + c.level * 5) % 50; }
        @Override public List<ItemView> inventory() {
            List<ItemView> inv = new ArrayList<ItemView>();
            inv.add(new StubItem("potion.heal", "Healing Draught", ItemType.CONSUMABLE, null, 3, 15,
                    "Restores 25 LP.", "potion.heal"));
            inv.add(new StubItem("sword.iron", "Iron Sword", ItemType.WEAPON, EquipSlot.WEAPON, 1, 40,
                    "A dependable blade.", "weapon.sword"));
            inv.add(new StubItem("scroll.spark", "Scroll of Sparks", ItemType.SCROLL, null, 1, 25,
                    "One-shot Destruction bolt.", "scroll.spark"));
            if (c.name.equals("Brannor")) inv.add(new StubItem("key.crypt", "Crypt Key", ItemType.KEY, null, 1, 0,
                    "Opens the vault door.", "key.crypt"));
            return inv;
        }
        @Override public ItemView equipped(EquipSlot slot) {
            if (slot == EquipSlot.WEAPON) return new StubItem("sword.iron", "Iron Sword", ItemType.WEAPON,
                    EquipSlot.WEAPON, 1, 40, "A dependable blade.", "weapon.sword");
            if (slot == EquipSlot.BODY) return new StubItem("armor.leather", "Leather Jerkin", ItemType.ARMOR,
                    EquipSlot.BODY, 1, 30, "Light protection.", "armor.leather");
            return null;
        }
        @Override public List<SpellView> spells() {
            List<SpellView> s = new ArrayList<SpellView>();
            if (c.caster) {
                s.add(new StubSpell("spell.spark", "Spark", SpellSchool.DESTRUCTION, 6, c.sp >= 6, "Fire bolt, 8-14 dmg."));
                s.add(new StubSpell("spell.mend", "Mend", SpellSchool.RESTORATION, 8, c.sp >= 8, "Heal an ally 20 LP."));
                s.add(new StubSpell("spell.ward", "Ward", SpellSchool.PSIONIC, 5, c.sp >= 5, "Raise defense one round."));
            }
            return s;
        }
        @Override public boolean canCast(SpellSchool school) { return c.caster; }
    }

    private static final class StubItem implements ItemView {
        final String id, name, desc, sprite; final ItemType type; final EquipSlot slot; final int qty, val;
        StubItem(String id, String name, ItemType type, EquipSlot slot, int qty, int val, String desc, String sprite) {
            this.id = id; this.name = name; this.type = type; this.slot = slot; this.qty = qty; this.val = val;
            this.desc = desc; this.sprite = sprite;
        }
        @Override public String id() { return id; }
        @Override public String name() { return name; }
        @Override public ItemType type() { return type; }
        @Override public EquipSlot slot() { return slot; }
        @Override public int quantity() { return qty; }
        @Override public int value() { return val; }
        @Override public String description() { return desc; }
        @Override public String spriteKey() { return sprite; }
    }

    private static final class StubSpell implements SpellView {
        final String id, name, desc; final SpellSchool school; final int cost; final boolean castable;
        StubSpell(String id, String name, SpellSchool school, int cost, boolean castable, String desc) {
            this.id = id; this.name = name; this.school = school; this.cost = cost; this.castable = castable; this.desc = desc;
        }
        @Override public String id() { return id; }
        @Override public String name() { return name; }
        @Override public SpellSchool school() { return school; }
        @Override public int spCost() { return cost; }
        @Override public boolean castable() { return castable; }
        @Override public String description() { return desc; }
    }

    private static final class StubQuest {
        final String title; QuestStatus status; final List<String> objectives;
        StubQuest(String t, QuestStatus s, List<String> o) { this.title = t; this.status = s; this.objectives = o; }
    }

    private static final class StubQuestView implements QuestEntryView {
        private final StubQuest q;
        StubQuestView(StubQuest q) { this.q = q; }
        @Override public String title() { return q.title; }
        @Override public QuestStatus status() { return q.status; }
        @Override public List<String> objectives() { return q.objectives; }
    }

    private static final class StubNpc {
        final int x, y; final String name, sprite, portrait; final boolean hostile;
        StubNpc(int x, int y, String name, String sprite, String portrait, boolean hostile) {
            this.x = x; this.y = y; this.name = name; this.sprite = sprite; this.portrait = portrait; this.hostile = hostile;
        }
    }

    private static final class StubWorld {
        final String name; final MapType type; final TileType[][] tiles; final int w, h;
        final java.util.Map<String, String> decor = new java.util.HashMap<String, String>();
        final List<StubNpc> npcs = new ArrayList<StubNpc>();
        int px, py; Direction facing = Direction.NORTH;
        int encounterX = -1, encounterY = -1, chestX = -1, chestY = -1;
        boolean chestLooted = false;
        StubWorld(String name, MapType type, TileType[][] tiles) {
            this.name = name; this.type = type; this.tiles = tiles;
            this.h = tiles.length; this.w = tiles[0].length;
        }
        boolean inBounds(int x, int y) { return x >= 0 && y >= 0 && x < w && y < h; }
        TileView tile(int x, int y) { return new StubTile(inBounds(x, y) ? tiles[y][x] : TileType.VOID,
                decor.get(x + "," + y)); }
        boolean walkable(int x, int y) {
            if (!inBounds(x, y)) return false;
            TileType t = tiles[y][x];
            return t != TileType.WALL && t != TileType.OBSTACLE && t != TileType.WATER
                    && t != TileType.VOID && t != TileType.DOOR;
        }
    }

    // WorldView adapter (StubWorld itself doesn't implement it so build a light wrapper)
    private static final class StubTile implements TileView {
        final TileType type; final String decor;
        StubTile(TileType type, String decor) { this.type = type; this.decor = decor; }
        @Override public TileType type() { return type; }
        @Override public boolean walkable() {
            return type != TileType.WALL && type != TileType.OBSTACLE && type != TileType.WATER
                    && type != TileType.VOID;
        }
        @Override public boolean blocksSight() { return type == TileType.WALL || type == TileType.OBSTACLE; }
        @Override public String decorKey() { return decor == null ? "" : decor; }
    }

    // The StubWorld needs to be exposed as WorldView; wrap on demand.
    {
        // no-op initializer to keep structure clear
    }

    // Expose current world as a WorldView via an adapter created in StubState.world()
    // (we cannot make StubWorld implement WorldView statically because npc adaptation
    //  differs) — provide the adapter here:
    private WorldView worldView(final StubWorld sw) {
        return new WorldView() {
            @Override public String mapName() { return sw.name; }
            @Override public MapType mapType() { return sw.type; }
            @Override public int width() { return sw.w; }
            @Override public int height() { return sw.h; }
            @Override public TileView tileAt(int x, int y) { return sw.tile(x, y); }
            @Override public PlayerView player() {
                return new PlayerView() {
                    @Override public int x() { return sw.px; }
                    @Override public int y() { return sw.py; }
                    @Override public Direction facing() { return sw.facing; }
                };
            }
            @Override public List<NpcView> npcs() {
                List<NpcView> l = new ArrayList<NpcView>();
                for (final StubNpc n : sw.npcs) {
                    l.add(new NpcView() {
                        @Override public int x() { return n.x; }
                        @Override public int y() { return n.y; }
                        @Override public String name() { return n.name; }
                        @Override public String spriteKey() { return n.sprite; }
                        @Override public boolean hostile() { return n.hostile; }
                    });
                }
                return l;
            }
        };
    }

    // ---- combat model ----------------------------------------------------

    private final class StubCombat implements CombatView {
        final int cols = 6, rows = 5;
        final List<Combi> units = new ArrayList<Combi>();
        final List<String> log = new ArrayList<String>();
        int turn = 0;
        boolean finished = false, victory = false;

        StubCombat() {
            units.add(new Combi("Brannor", true, 1, 4, 96, 96, 0, 0, "portrait.warrior"));
            units.add(new Combi("Sela", true, 2, 4, 74, 80, 34, 40, "portrait.ranger"));
            units.add(new Combi("Ioric", true, 3, 4, 52, 55, 60, 70, "portrait.mage"));
            units.add(new Combi("Crypt Ghoul", false, 2, 0, 60, 60, 0, 0, "enemy.ghoul"));
            units.add(new Combi("Crypt Ghoul", false, 4, 0, 55, 55, 0, 0, "enemy.ghoul"));
            log.add("A pair of ghouls lurches from the dark.");
        }

        String act(CombatActionType type, int targetIndex, String optionId) {
            String actor = units.get(turn).name;
            String msg;
            switch (type) {
                case ATTACK:
                    msg = resolveAttack(targetIndex, 8 + (turn * 5) % 10);
                    break;
                case CAST:
                    msg = actor + " casts " + (optionId == null ? "a spell" : optionId) + "!";
                    resolveAttack(targetIndex, 14);
                    break;
                case ITEM:
                    msg = actor + " uses an item.";
                    break;
                case DEFEND:
                    msg = actor + " braces for impact.";
                    break;
                case FLEE:
                    msg = "The party flees the fight!";
                    finished = true; victory = false;
                    return msg;
                default:
                    msg = actor + " repositions.";
            }
            log.add(msg);
            advanceTurn();
            checkEnd();
            return msg;
        }

        private String resolveAttack(int targetIndex, int dmg) {
            if (targetIndex < 0 || targetIndex >= units.size()) {
                // auto-pick first living enemy
                for (int i = 0; i < units.size(); i++) if (!units.get(i).side && units.get(i).lp > 0) { targetIndex = i; break; }
            }
            if (targetIndex < 0) return "No target.";
            Combi t = units.get(targetIndex);
            t.lp = Math.max(0, t.lp - dmg);
            return units.get(turn).name + " hits " + t.name + " for " + dmg + (t.lp == 0 ? " — it falls!" : ".");
        }

        private void advanceTurn() {
            for (int i = 0; i < units.size(); i++) {
                turn = (turn + 1) % units.size();
                if (units.get(turn).lp > 0) break;
            }
            // simple enemy auto-turns
            int guard = 0;
            while (!units.get(turn).side && units.get(turn).lp > 0 && !finished && guard++ < 8) {
                // enemy strikes a random living player
                int target = -1;
                for (int i = 0; i < units.size(); i++) if (units.get(i).side && units.get(i).lp > 0) { target = i; break; }
                if (target >= 0) {
                    int dmg = 6 + (turn * 3) % 8;
                    units.get(target).lp = Math.max(0, units.get(target).lp - dmg);
                    log.add(units.get(turn).name + " claws " + units.get(target).name + " for " + dmg + ".");
                }
                checkEnd();
                if (finished) break;
                for (int i = 0; i < units.size(); i++) { turn = (turn + 1) % units.size(); if (units.get(turn).lp > 0) break; }
            }
        }

        private void checkEnd() {
            boolean anyEnemy = false, anyPlayer = false;
            for (Combi c : units) { if (c.lp > 0) { if (c.side) anyPlayer = true; else anyEnemy = true; } }
            if (!anyEnemy) { finished = true; victory = true; log.add("The ghouls crumble to dust. Victory!"); }
            else if (!anyPlayer) { finished = true; victory = false; log.add("The party is overwhelmed..."); }
        }

        @Override public int cols() { return cols; }
        @Override public int rows() { return rows; }
        @Override public List<CombatantView> combatants() {
            List<CombatantView> l = new ArrayList<CombatantView>();
            for (int i = 0; i < units.size(); i++) l.add(units.get(i).view(i == turn));
            return l;
        }
        @Override public int currentTurnIndex() { return turn; }
        @Override public List<CombatActionType> availableActions() {
            List<CombatActionType> a = new ArrayList<CombatActionType>();
            a.add(CombatActionType.ATTACK);
            if (units.get(turn).maxSp > 0) a.add(CombatActionType.CAST);
            a.add(CombatActionType.ITEM);
            a.add(CombatActionType.DEFEND);
            a.add(CombatActionType.MOVE);
            a.add(CombatActionType.FLEE);
            return a;
        }
        @Override public List<String> log() {
            // keep last ~8 lines
            int from = Math.max(0, log.size() - 8);
            return new ArrayList<String>(log.subList(from, log.size()));
        }
        @Override public boolean finished() { return finished; }
        @Override public boolean victory() { return victory; }
    }

    private static final class Combi {
        final String name, sprite; final boolean side; final int gx, gy, maxLp, maxSp; int lp, sp;
        Combi(String name, boolean side, int gx, int gy, int maxLp, int maxSp, int lp, int sp, String sprite) {
            this.name = name; this.side = side; this.gx = gx; this.gy = gy; this.maxLp = maxLp; this.maxSp = maxSp;
            this.lp = maxLp; this.sp = maxSp; this.sprite = sprite;
        }
        CombatantView view(final boolean cur) {
            final Combi c = this;
            return new CombatantView() {
                @Override public String name() { return c.name; }
                @Override public boolean playerSide() { return c.side; }
                @Override public int gridX() { return c.gx; }
                @Override public int gridY() { return c.gy; }
                @Override public int lp() { return c.lp; }
                @Override public int maxLp() { return c.maxLp; }
                @Override public int sp() { return c.sp; }
                @Override public int maxSp() { return c.maxSp; }
                @Override public boolean alive() { return c.lp > 0; }
                @Override public boolean current() { return cur; }
                @Override public String spriteKey() { return c.sprite; }
            };
        }
    }

    // Re-route StubState.world() through the adapter (fields referenced above).
    // Because StubState is an inner class, it calls worldView(world) — patch here:
    // (Adapter access provided by making world() return worldView(world).)
}
