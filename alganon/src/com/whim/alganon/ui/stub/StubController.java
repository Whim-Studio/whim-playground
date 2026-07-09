package com.whim.alganon.ui.stub;

import com.whim.alganon.api.ActionResult;
import com.whim.alganon.api.Defs;
import com.whim.alganon.api.Enums.AbilityKind;
import com.whim.alganon.api.Enums.ChatChannel;
import com.whim.alganon.api.Enums.ClassId;
import com.whim.alganon.api.Enums.ControlState;
import com.whim.alganon.api.Enums.Direction;
import com.whim.alganon.api.Enums.EquipSlot;
import com.whim.alganon.api.Enums.Faction;
import com.whim.alganon.api.Enums.FamilyArchetype;
import com.whim.alganon.api.Enums.GameStateType;
import com.whim.alganon.api.Enums.ItemType;
import com.whim.alganon.api.Enums.QuestStatus;
import com.whim.alganon.api.Enums.ResourceType;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.Stance;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.api.Enums.TileType;
import com.whim.alganon.api.GameController;
import com.whim.alganon.api.GridPos;
import com.whim.alganon.api.Views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-contained {@link GameController} with canned {@link Views} so the entire UI runs and
 * demos before Task 2's engine lands. It fabricates a small content pack (races/families/
 * classes/abilities/items/zone), walks the title → creation → PLAYING state machine, and
 * mutates just enough (position, selected overlay, chat log, ability cooldowns) that every
 * panel shows live, believable data. Not a game engine — a fixture.
 */
public final class StubController implements GameController {

    private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();
    private final StubContent content = new StubContent();
    private final List<Views.ChatLineView> chat = new ArrayList<Views.ChatLineView>();

    private GameStateType state = GameStateType.TITLE;
    private GameStateType overlayReturn = GameStateType.PLAYING;
    private String toast = "";

    // wizard partial selection
    private int step = 0;
    private String raceId, familyId, classId, name = "";

    // committed character (post-creation), null until finishCreation
    private StubCharacter player;
    private final StubWorld world = new StubWorld();

    public StubController() {
        pushChat(ChatChannel.SYSTEM, "Welcome to Alganon (single-player recreation).");
        pushChat(ChatChannel.SYSTEM, "Running on the UI stub controller — no engine attached.");
    }

    // ================= change notification =================
    public void addChangeListener(ChangeListener l) { if (l != null) listeners.add(l); }
    public void removeChangeListener(ChangeListener l) { listeners.remove(l); }
    private void fire() { for (ChangeListener l : new ArrayList<ChangeListener>(listeners)) l.onStateChanged(); }

    // ================= state snapshot =================
    public Views.GameStateView state() { return new Snapshot(); }

    // ================= title / lifecycle =================
    public ActionResult selectMenuOption(int index) {
        if (state == GameStateType.TITLE) {
            switch (index) {
                case 0: beginCreation(); return ActionResult.ok("New game");
                case 1: toast = "No save slots in the stub."; fire(); return ActionResult.fail(toast);
                case 2: openState(GameStateType.SETTINGS); return ActionResult.ok();
                case 3: toast = "Quit requested."; fire(); return ActionResult.ok("quit");
                default: return ActionResult.fail("Unknown option");
            }
        }
        return ActionResult.ok();
    }

    public List<String> saveSlots() { return Collections.emptyList(); }
    public boolean saveGame(int slot) { toast = "Saved (stub no-op)."; fire(); return true; }
    public boolean loadGame(int slot) { toast = "No saves in the stub."; fire(); return false; }

    // ================= creation wizard =================
    public void beginCreation() {
        state = GameStateType.CHARACTER_CREATION;
        step = 0; raceId = null; familyId = null; classId = null; name = "";
        toast = "Choose your race.";
        fire();
    }

    public ActionResult chooseRace(String id) {
        if (content.race(id) == null) return ActionResult.fail("Unknown race");
        raceId = id; familyId = null; step = 1;
        toast = "Race set: " + content.race(id).name;
        fire();
        return ActionResult.ok(toast);
    }

    public ActionResult chooseFamily(String id) {
        if (content.family(id) == null) return ActionResult.fail("Unknown family");
        familyId = id; step = 2;
        toast = "Family set: " + content.family(id).name;
        fire();
        return ActionResult.ok(toast);
    }

    public ActionResult chooseClass(String id) {
        if (content.clazz(id) == null) return ActionResult.fail("Unknown class");
        classId = id; step = 3;
        toast = "Class set: " + content.clazz(id).name;
        fire();
        return ActionResult.ok(toast);
    }

    public ActionResult setName(String n) {
        name = n == null ? "" : n.trim();
        fire();
        return ActionResult.ok();
    }

    public ActionResult creationBack() {
        if (step > 0) step--;
        toast = "";
        fire();
        return ActionResult.ok();
    }

    public ActionResult finishCreation() {
        if (raceId == null || familyId == null || classId == null || name.isEmpty())
            return ActionResult.fail("Complete every step first.");
        player = new StubCharacter(content, raceId, familyId, classId, name);
        state = GameStateType.PLAYING;
        pushChat(ChatChannel.SYSTEM, "You awaken in " + world.zoneName + ".");
        pushChat(ChatChannel.FAMILY, "[" + content.family(familyId).name + "] Welcome home, " + name + ".");
        pushChat(ChatChannel.FACTION, "The war for the frontier grinds on.");
        toast = "Adventure begins!";
        fire();
        return ActionResult.ok(toast);
    }

    // ================= exploration =================
    public ActionResult move(Direction dir) {
        if (player == null || dir == null) return ActionResult.fail("Not playing");
        int nx = clamp(player.pos.x + dir.dx, 0, world.width - 1);
        int ny = clamp(player.pos.y + dir.dy, 0, world.height - 1);
        if (world.tileAt(nx, ny) == TileType.WATER || world.tileAt(nx, ny) == TileType.WALL) {
            toast = "Blocked."; fire(); return ActionResult.fail(toast);
        }
        player.pos = new GridPos(nx, ny);
        toast = "";
        fire();
        return ActionResult.ok();
    }

    public ActionResult interact() {
        if (player == null) return ActionResult.fail("Not playing");
        pushChat(ChatChannel.SAY, "You look around, but find nothing to interact with here.");
        fire();
        return ActionResult.ok();
    }

    // ================= combat =================
    public ActionResult useAbility(String abilityId, int targetIndex) {
        if (player == null) return ActionResult.fail("Not playing");
        StubAbility a = player.ability(abilityId);
        if (a == null) return ActionResult.fail("Unknown ability");
        if (a.cooldownRemaining > 0) { toast = a.name + " is recharging."; fire(); return ActionResult.fail(toast); }
        if (player.resource < a.cost) { toast = "Not enough " + player.resourceType(); fire(); return ActionResult.fail(toast); }
        player.resource -= a.cost;
        a.cooldownRemaining = a.cooldownSec;
        pushChat(ChatChannel.COMBAT, "You use " + a.name + (a.kind == AbilityKind.HEAL ? " and mend your wounds." : " on your foe."));
        toast = a.name + "!";
        fire();
        return ActionResult.ok(toast);
    }

    public ActionResult setStance(Stance s) {
        if (player != null) { player.stance = s; pushChat(ChatChannel.SYSTEM, "Stance: " + s); fire(); }
        return ActionResult.ok();
    }

    public ActionResult setSchool(School s) {
        if (player != null) { player.school = s; pushChat(ChatChannel.SYSTEM, "School: " + s); fire(); }
        return ActionResult.ok();
    }

    // ================= quests / study / craft / auction =================
    public ActionResult acceptQuest(String id) { toast = "Quest accepted (stub)."; pushChat(ChatChannel.SYSTEM, toast); fire(); return ActionResult.ok(); }
    public ActionResult turnInQuest(String id) { toast = "Quest complete (stub)."; pushChat(ChatChannel.SYSTEM, toast); fire(); return ActionResult.ok(); }
    public ActionResult generateProceduralQuest() { toast = "A new task appears (stub)."; pushChat(ChatChannel.SYSTEM, toast); fire(); return ActionResult.ok(); }

    public ActionResult assignStudy(SkillType s) { if (player != null) { player.studying = s; pushChat(ChatChannel.SYSTEM, "Now studying " + s + "."); fire(); } return ActionResult.ok(); }
    public ActionResult clearStudy() { if (player != null) { player.studying = null; fire(); } return ActionResult.ok(); }

    public ActionResult gather(String nodeId) { toast = "Gathered materials (stub)."; pushChat(ChatChannel.LOOT, toast); fire(); return ActionResult.ok(); }
    public ActionResult craft(String recipeId) { toast = "Crafted an item (stub)."; pushChat(ChatChannel.LOOT, toast); fire(); return ActionResult.ok(); }
    public ActionResult auctionBuy(String listingId) { toast = "Purchased (stub)."; fire(); return ActionResult.ok(); }
    public ActionResult auctionPost(String itemId, int qty, long price) { toast = "Listed (stub)."; fire(); return ActionResult.ok(); }

    // ================= inventory / equip =================
    public ActionResult equip(String itemId) { toast = "Equipped (stub)."; fire(); return ActionResult.ok(); }
    public ActionResult unequip(EquipSlot slot) { toast = "Unequipped (stub)."; fire(); return ActionResult.ok(); }
    public ActionResult useItem(String itemId) { toast = "Used item (stub)."; pushChat(ChatChannel.LOOT, toast); fire(); return ActionResult.ok(); }

    // ================= overlays =================
    public void openState(GameStateType s) {
        if (s == null) return;
        if (state == GameStateType.PLAYING) overlayReturn = GameStateType.PLAYING;
        state = s;
        fire();
    }

    public void closeOverlay() {
        state = player != null ? GameStateType.PLAYING : GameStateType.TITLE;
        fire();
    }

    /** Test/demo hook: advance stub cooldowns & regen a step so the action bar animates. */
    public void tickDemo(double dt) {
        if (player == null) return;
        boolean changed = false;
        for (StubAbility a : player.abilities) {
            if (a.cooldownRemaining > 0) { a.cooldownRemaining = Math.max(0, a.cooldownRemaining - dt); changed = true; }
        }
        if (player.resource < player.maxResource) { player.resource = Math.min(player.maxResource, player.resource + 2); changed = true; }
        if (changed) fire();
    }

    // ================= helpers =================
    private void pushChat(ChatChannel ch, String text) {
        chat.add(new ChatLine(ch, text));
        while (chat.size() > 100) chat.remove(0);
    }

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

    StubContent content() { return content; }

    // ==================================================================
    // View implementations (immutable projections over stub state)
    // ==================================================================

    private final class Snapshot implements Views.GameStateView {
        public GameStateType state() { return state; }
        public Views.CharacterView player() { return player; }
        public Views.WorldView world() { return player == null ? null : world; }
        public Views.CombatView combat() { return null; }
        public Views.CreationView creation() { return state == GameStateType.CHARACTER_CREATION ? new CreationSnapshot() : null; }
        public List<Views.QuestView> quests() { return player == null ? Collections.<Views.QuestView>emptyList() : content.demoQuests(); }
        public Views.StudyView study() { return player == null ? null : player.studyView(); }
        public Views.CraftingView crafting() { return player == null ? null : content.craftingView(); }
        public Views.AuctionView auction() { return state == GameStateType.AUCTION && player != null ? content.auctionView(player.gold) : null; }
        public Views.FamilyView family() { return player == null ? null : content.familyView(familyId); }
        public List<Views.ChatLineView> chat() { return new ArrayList<Views.ChatLineView>(chat); }
        public List<String> menuOptions() {
            if (state == GameStateType.TITLE) return java.util.Arrays.asList("New Game", "Load Game", "Settings", "Quit");
            if (state == GameStateType.SETTINGS) return java.util.Arrays.asList("Toggle Mute", "Back");
            return Collections.emptyList();
        }
        public String toastMessage() { return toast; }
    }

    private final class CreationSnapshot implements Views.CreationView {
        public List<Defs.RaceDef> races() { return content.races(); }
        public List<Defs.FamilyDef> familiesFor(String rid) { return content.familiesForRace(rid); }
        public List<Defs.ClassDef> classes() { return content.classes(); }
        public String selectedRaceId() { return raceId; }
        public String selectedFamilyId() { return familyId; }
        public String selectedClassId() { return classId; }
        public String enteredName() { return name; }
        public int step() { return step; }
    }

    private static final class ChatLine implements Views.ChatLineView {
        private final ChatChannel ch; private final String text;
        ChatLine(ChatChannel ch, String text) { this.ch = ch; this.text = text; }
        public ChatChannel channel() { return ch; }
        public String text() { return text; }
    }
}
