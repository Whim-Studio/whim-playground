package com.whim.alganon.engine;

import com.whim.alganon.api.ActionResult;
import com.whim.alganon.api.CharacterModel;
import com.whim.alganon.api.Content;
import com.whim.alganon.api.Defs;
import com.whim.alganon.api.Enums.ChatChannel;
import com.whim.alganon.api.Enums.Direction;
import com.whim.alganon.api.Enums.EquipSlot;
import com.whim.alganon.api.Enums.FamilyArchetype;
import com.whim.alganon.api.Enums.GameStateType;
import com.whim.alganon.api.Enums.ObjectiveType;
import com.whim.alganon.api.Enums.QuestStatus;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.Stance;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.api.GameContext;
import com.whim.alganon.api.GameController;
import com.whim.alganon.api.GameModel;
import com.whim.alganon.api.GridPos;
import com.whim.alganon.api.ModelFactory;
import com.whim.alganon.api.Views;
import com.whim.alganon.api.WorldModel;
import com.whim.alganon.combat.CombatSystem;
import com.whim.alganon.craft.CraftSystem;
import com.whim.alganon.craft.Listing;
import com.whim.alganon.persistence.SaveGame;
import com.whim.alganon.persistence.SaveManager;
import com.whim.alganon.quest.QuestRun;
import com.whim.alganon.quest.QuestSystem;
import com.whim.alganon.study.StudySystem;
import com.whim.alganon.worldsim.FactionWarSim;
import com.whim.alganon.worldsim.WarObjective;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The engine: implements {@link GameController} (the UI seam) and {@link GameContext}
 * (the side-effect surface handed to subsystems). Owns the top-level state machine, the
 * fixed-step {@code javax.swing.Timer} tick, character-creation flow, and delegates rules
 * to the combat/quest/study/craft/worldsim subsystems. Builds the immutable
 * {@link Views.GameStateView} snapshot the UI renders. Works purely through {@code api}.
 */
public final class GameEngine implements GameController, GameContext {

    private static final int TICK_MS = 100;
    private static final double DT = TICK_MS / 1000.0;
    private static final int CHAT_MAX = 120;

    private static final List<String> TITLE_MENU =
            Arrays.asList("New Game", "Load Game", "Settings", "Quit");
    private static final List<String> GAME_OVER_MENU =
            Arrays.asList("Return to Title");

    private final ModelFactory factory;
    private final Content content;
    private final SaveManager saves;
    private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();
    private final Deque<Views.ChatLineView> chat = new ArrayDeque<Views.ChatLineView>();

    private GameStateType state = GameStateType.TITLE;
    private GameModel model;
    private Random rng = new Random(0);
    private String toast = "";
    private boolean autoTick = true;
    private javax.swing.Timer timer;

    private CombatSystem combat;
    private QuestSystem quests;
    private StudySystem study;
    private CraftSystem craft;
    private FactionWarSim war;

    private CreationState creation;
    /** Blank creation state so state().creation() can always expose reference lists. */
    private static final CreationState REFERENCE_CREATION = new CreationState();

    public GameEngine(ModelFactory factory) {
        this(factory, new SaveManager());
    }

    public GameEngine(ModelFactory factory, SaveManager saves) {
        if (factory == null) throw new IllegalArgumentException("factory");
        this.factory = factory;
        this.content = factory.content();
        this.saves = saves;
        log(ChatChannel.SYSTEM, "Welcome to Alganon.");
    }

    /** Disable the background Timer (used by headless smoke/unit tests that tick manually). */
    public void setAutoTick(boolean enabled) { this.autoTick = enabled; }

    /** Advance the simulation one fixed step. Public so tests can drive it deterministically. */
    public void tickOnce(double dt) {
        if (model == null) return;
        if (state == GameStateType.TITLE || state == GameStateType.CHARACTER_CREATION
                || state == GameStateType.GAME_OVER) return;
        combat.tick(model, dt);
        craft.tick(model, dt);
        war.tick(model, dt);
        fireChanged();
    }

    // ================= GameContext =================

    public void log(ChatChannel channel, String text) {
        chat.addLast(new EngineViews.ChatLineViewImpl(channel, text));
        while (chat.size() > CHAT_MAX) chat.removeFirst();
    }

    public void toast(String text) { this.toast = text == null ? "" : text; }

    public Random rng() { return rng; }

    // ================= lifecycle / title =================

    public ActionResult selectMenuOption(int index) {
        if (state == GameStateType.TITLE) {
            switch (index) {
                case 0: beginCreation(); return ActionResult.ok("New game");
                case 1:
                    for (int s = 0; s < SaveManager.SLOT_COUNT; s++) {
                        if (saves.exists(s)) { boolean ok = loadGame(s); return ok ? ActionResult.ok("Loaded") : ActionResult.fail("Load failed"); }
                    }
                    return ActionResult.fail("No saved games.");
                case 2: openState(GameStateType.SETTINGS); return ActionResult.ok();
                case 3: default: return ActionResult.ok("Quit");
            }
        }
        if (state == GameStateType.GAME_OVER) {
            state = GameStateType.TITLE;
            stopTimer();
            fireChanged();
            return ActionResult.ok("Returned to title");
        }
        return ActionResult.ok();
    }

    public List<String> saveSlots() { return saves.slotLabels(); }

    public boolean saveGame(int slot) {
        if (model == null) return false;
        long now = System.currentTimeMillis();
        model.setLastSaveEpochMillis(now);
        SaveGame sg = buildSave();
        sg.lastSaveEpochMillis = now;
        try {
            saves.write(slot, sg);
            log(ChatChannel.SYSTEM, "Game saved to slot " + (slot + 1) + ".");
            toast("Game saved.");
            fireChanged();
            return true;
        } catch (Exception e) {
            log(ChatChannel.SYSTEM, "Save failed: " + e.getMessage());
            return false;
        }
    }

    public boolean loadGame(int slot) {
        SaveGame sg;
        try {
            sg = saves.read(slot);
        } catch (Exception e) {
            log(ChatChannel.SYSTEM, "Load failed: " + e.getMessage());
            return false;
        }
        applyLoad(sg);
        return true;
    }

    // ================= character creation =================

    public void beginCreation() {
        creation = new CreationState();
        state = GameStateType.CHARACTER_CREATION;
        fireChanged();
    }

    public ActionResult chooseRace(String raceId) {
        if (creation == null) return ActionResult.fail("Not creating a character.");
        if (content.race(raceId) == null) return ActionResult.fail("Unknown race.");
        creation.raceId = raceId;
        // reset a now-invalid family selection (families are faction-locked)
        creation.familyId = null;
        creation.step = Math.max(creation.step, 1);
        fireChanged();
        return ActionResult.ok();
    }

    public ActionResult chooseFamily(String familyId) {
        if (creation == null) return ActionResult.fail("Not creating a character.");
        Defs.FamilyDef fam = content.family(familyId);
        if (fam == null) return ActionResult.fail("Unknown family.");
        creation.familyId = familyId;
        creation.step = Math.max(creation.step, 2);
        fireChanged();
        return ActionResult.ok();
    }

    public ActionResult chooseClass(String classId) {
        if (creation == null) return ActionResult.fail("Not creating a character.");
        if (content.clazz(classId) == null) return ActionResult.fail("Unknown class.");
        creation.classId = classId;
        creation.step = Math.max(creation.step, 3);
        fireChanged();
        return ActionResult.ok();
    }

    public ActionResult setName(String name) {
        if (creation == null) return ActionResult.fail("Not creating a character.");
        if (name == null || name.trim().isEmpty()) return ActionResult.fail("Enter a name.");
        creation.name = name.trim();
        creation.step = 3;
        fireChanged();
        return ActionResult.ok();
    }

    public ActionResult creationBack() {
        if (creation == null) return ActionResult.fail("Not creating a character.");
        if (creation.step > 0) creation.step--;
        fireChanged();
        return ActionResult.ok();
    }

    public ActionResult finishCreation() {
        if (creation == null) return ActionResult.fail("Not creating a character.");
        if (creation.raceId == null) return ActionResult.fail("Choose a race.");
        if (creation.familyId == null) return ActionResult.fail("Choose a family.");
        if (creation.classId == null) return ActionResult.fail("Choose a class.");
        if (creation.name == null || creation.name.isEmpty()) return ActionResult.fail("Enter a name.");

        long seed = System.currentTimeMillis() ^ (long) creation.name.hashCode();
        model = factory.newGame(seed);
        rng = new Random(seed);
        factory.applyCreation(model, creation.raceId, creation.familyId, creation.classId, creation.name);
        // ensure a world is loaded
        if (model.world() == null) {
            model.loadZone(content.startingZoneId(creation.raceId));
        }
        initSystems();
        war.seed();
        craft.seedListings();
        creation = null;
        state = GameStateType.PLAYING;
        model.setLastSaveEpochMillis(System.currentTimeMillis());
        log(ChatChannel.SYSTEM, "Your journey begins in " + model.world().zoneName() + ".");
        startTimer();
        fireChanged();
        return ActionResult.ok("Welcome to Alganon!");
    }

    private void initSystems() {
        combat = new CombatSystem(content, this, new CombatSystem.Listener() {
            public void onMobKilled(Defs.MobDef def, WorldModel.MobEntity mob) {
                if (def != null) quests.onKill(def.id);
            }
            public void onPlayerDeath() {
                state = GameStateType.GAME_OVER;
                toast("You have fallen. Your journey ends here.");
                stopTimer();
            }
            public void onAbilityUsed(Defs.AbilityDef ability) {
                study.onUse(model, skillLineFor(ability), 1.0);
            }
            public void onLoot(String itemId, int qty) { /* loot handled in-place */ }
        });
        quests = new QuestSystem(content, this);
        study = new StudySystem(this);
        craft = new CraftSystem(content, this, new CraftSystem.Callbacks() {
            public void onGathered(String itemId, int qty) { quests.onGather(itemId, qty); }
            public void onSkillUsed(SkillType skill, double amount) { study.onUse(model, skill, amount); }
        });
        war = new FactionWarSim(this);
    }

    private SkillType skillLineFor(Defs.AbilityDef a) {
        switch (a.kind) {
            case HEAL: case HOT: return SkillType.HEALING;
            case BUFF: return SkillType.DEFENSE;
            default:
                return a.damageType == com.whim.alganon.api.Enums.DamageType.PHYSICAL
                        ? SkillType.WEAPON : SkillType.CASTING;
        }
    }

    // ================= exploration =================

    public ActionResult move(Direction dir) {
        if (state != GameStateType.PLAYING) return ActionResult.fail("You can't move right now.");
        CharacterModel p = model.player();
        if (!p.alive()) return ActionResult.fail("You are defeated.");
        GridPos to = p.pos().translate(dir.dx, dir.dy);
        WorldModel w = model.world();
        if (!w.walkable(to.x, to.y)) return ActionResult.fail("Blocked.");
        if (occupiedByMob(to)) return ActionResult.fail("A creature bars your way.");
        p.setPos(to);
        // auto-travel if stepping onto a portal
        for (WorldModel.Portal pt : w.portals()) {
            if (pt.pos().equals(to)) { fireChanged(); return travel(pt.targetZoneId()); }
        }
        fireChanged();
        return ActionResult.ok();
    }

    public ActionResult interact() {
        if (state != GameStateType.PLAYING) return ActionResult.fail("Nothing to interact with.");
        CharacterModel p = model.player();
        WorldModel w = model.world();
        GridPos here = p.pos();

        // portals under/adjacent
        for (WorldModel.Portal pt : w.portals()) {
            if (pt.pos().manhattan(here) <= 1) return travel(pt.targetZoneId());
        }
        // gather nodes adjacent
        for (WorldModel.NodeEntity n : w.nodes()) {
            if (n.pos().manhattan(here) <= 1 && !n.depleted()) {
                ActionResult r = craft.gather(model, n.id());
                fireChanged();
                return r;
            }
        }
        // NPCs adjacent
        for (WorldModel.NpcEntity npc : w.npcs()) {
            if (npc.pos().manhattan(here) <= 1) {
                return interactNpc(npc);
            }
        }
        return ActionResult.fail("There's nothing here to interact with.");
    }

    private ActionResult interactNpc(WorldModel.NpcEntity npc) {
        quests.onTalk(npc.id());
        // turn in any ready quest for this NPC
        for (QuestRun run : quests.runs()) {
            if (run.status == QuestStatus.READY_TO_TURN_IN
                    && npc.id().equals(run.def.turnInNpcId)) {
                ActionResult r = quests.turnIn(model, run.def.id);
                fireChanged();
                return r;
            }
        }
        // accept an available static quest from this giver
        for (Defs.QuestDef q : content.staticQuests()) {
            if (npc.id().equals(q.giverNpcId) && quests.get(q.id) == null
                    && model.player().level() >= q.levelReq) {
                ActionResult r = quests.accept(model, q.id);
                fireChanged();
                return r;
            }
        }
        if (npc.vendor()) {
            openState(GameStateType.AUCTION);
            return ActionResult.ok("You browse " + npc.name() + "'s wares.");
        }
        log(ChatChannel.SAY, npc.name() + ": Well met, traveler.");
        fireChanged();
        return ActionResult.ok(npc.name() + " greets you.");
    }

    private ActionResult travel(String zoneId) {
        WorldModel w = model.loadZone(zoneId);
        CharacterModel p = model.player();
        p.setZoneId(zoneId);
        p.setPos(spawnPoint(w));
        combat.reset();
        quests.onTravel(zoneId);
        log(ChatChannel.SYSTEM, "You arrive in " + w.zoneName() + ".");
        fireChanged();
        return ActionResult.ok("Entered " + w.zoneName() + ".");
    }

    private GridPos spawnPoint(WorldModel w) {
        for (int y = 0; y < w.height(); y++) {
            for (int x = 0; x < w.width(); x++) {
                if (w.walkable(x, y)) return new GridPos(x, y);
            }
        }
        return new GridPos(0, 0);
    }

    private boolean occupiedByMob(GridPos pos) {
        for (WorldModel.MobEntity m : model.world().mobs()) {
            if (m.alive() && m.pos().equals(pos)) return true;
        }
        return false;
    }

    // ================= combat / abilities =================

    public ActionResult useAbility(String abilityId, int targetIndex) {
        if (model == null) return ActionResult.fail("No game in progress.");
        ActionResult r = combat.useAbility(model, abilityId, targetIndex);
        fireChanged();
        return r;
    }

    public ActionResult setStance(Stance stance) {
        if (model == null) return ActionResult.fail("No game in progress.");
        model.player().setStance(stance);
        log(ChatChannel.SYSTEM, "Stance: " + stance + ".");
        fireChanged();
        return ActionResult.ok("Stance set to " + stance + ".");
    }

    public ActionResult setSchool(School school) {
        if (model == null) return ActionResult.fail("No game in progress.");
        model.player().setSchool(school);
        log(ChatChannel.SYSTEM, "School: " + school + ".");
        fireChanged();
        return ActionResult.ok("School set to " + school + ".");
    }

    // ================= quests =================

    public ActionResult acceptQuest(String questId) {
        ActionResult r = quests.accept(model, questId);
        fireChanged();
        return r;
    }

    public ActionResult turnInQuest(String questId) {
        ActionResult r = quests.turnIn(model, questId);
        fireChanged();
        return r;
    }

    public ActionResult generateProceduralQuest() {
        if (model == null) return ActionResult.fail("No game in progress.");
        FamilyArchetype arch = archetype();
        ActionResult r = quests.generate(model, arch);
        fireChanged();
        return r;
    }

    // ================= study =================

    public ActionResult assignStudy(SkillType skill) {
        ActionResult r = study.assign(model, skill);
        fireChanged();
        return r;
    }

    public ActionResult clearStudy() {
        ActionResult r = study.clear(model);
        fireChanged();
        return r;
    }

    // ================= crafting / auction =================

    public ActionResult gather(String nodeId) {
        ActionResult r = craft.gather(model, nodeId);
        fireChanged();
        return r;
    }

    public ActionResult craft(String recipeId) {
        ActionResult r = craft.craft(model, recipeId);
        fireChanged();
        return r;
    }

    public ActionResult auctionBuy(String listingId) {
        ActionResult r = craft.buy(model, listingId);
        fireChanged();
        return r;
    }

    public ActionResult auctionPost(String itemId, int quantity, long price) {
        ActionResult r = craft.post(model, itemId, quantity, price);
        fireChanged();
        return r;
    }

    // ================= inventory / equipment =================

    public ActionResult equip(String itemId) {
        if (model == null) return ActionResult.fail("No game in progress.");
        Defs.ItemDef d = content.item(itemId);
        if (d == null || d.slot == null) return ActionResult.fail("That can't be equipped.");
        CharacterModel p = model.player();
        int have = p.inventory().containsKey(itemId) ? p.inventory().get(itemId) : 0;
        if (have <= 0) return ActionResult.fail("You don't have that item.");
        p.removeItem(itemId, 1);
        String prev = p.unequip(d.slot);
        if (prev != null) p.addItem(prev, 1);
        p.equip(d.slot, itemId);
        log(ChatChannel.SYSTEM, "Equipped " + d.name + ".");
        fireChanged();
        return ActionResult.ok("Equipped " + d.name + ".");
    }

    public ActionResult unequip(EquipSlot slot) {
        if (model == null) return ActionResult.fail("No game in progress.");
        CharacterModel p = model.player();
        String itemId = p.unequip(slot);
        if (itemId == null) return ActionResult.fail("Nothing equipped there.");
        p.addItem(itemId, 1);
        log(ChatChannel.SYSTEM, "Unequipped " + slot + ".");
        fireChanged();
        return ActionResult.ok("Unequipped.");
    }

    public ActionResult useItem(String itemId) {
        if (model == null) return ActionResult.fail("No game in progress.");
        Defs.ItemDef d = content.item(itemId);
        if (d == null) return ActionResult.fail("Unknown item.");
        CharacterModel p = model.player();
        int have = p.inventory().containsKey(itemId) ? p.inventory().get(itemId) : 0;
        if (have <= 0) return ActionResult.fail("You don't have that item.");
        if (d.type != com.whim.alganon.api.Enums.ItemType.CONSUMABLE) {
            return ActionResult.fail(d.name + " can't be used.");
        }
        p.removeItem(itemId, 1);
        int amount = Math.max(1, d.power);
        p.heal(amount);
        log(ChatChannel.SYSTEM, "You use " + d.name + " and recover " + amount + " health.");
        fireChanged();
        return ActionResult.ok("Used " + d.name + ".");
    }

    // ================= overlays =================

    public void openState(GameStateType s) {
        this.state = s;
        fireChanged();
    }

    public void closeOverlay() {
        if (model != null && model.player() != null && model.player().alive()) {
            state = GameStateType.PLAYING;
        } else if (model == null) {
            state = GameStateType.TITLE;
        }
        fireChanged();
    }

    // ================= change notification =================

    public void addChangeListener(ChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) { listeners.remove(listener); }

    private void fireChanged() {
        for (ChangeListener l : new ArrayList<ChangeListener>(listeners)) l.onStateChanged();
    }

    // ================= timer =================

    private void startTimer() {
        if (!autoTick) return;
        if (timer == null) {
            timer = new javax.swing.Timer(TICK_MS, new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) { tickOnce(DT); }
            });
        }
        if (!timer.isRunning()) timer.start();
    }

    private void stopTimer() {
        if (timer != null && timer.isRunning()) timer.stop();
    }

    // ================= state snapshot =================

    public Views.GameStateView state() {
        return new RootView();
    }

    private boolean inWorld() {
        return model != null && state != GameStateType.TITLE
                && state != GameStateType.CHARACTER_CREATION;
    }

    private final class RootView implements Views.GameStateView {
        public GameStateType state() { return state; }
        public Views.CharacterView player() {
            return model == null ? null : new EngineViews.CharacterViewImpl(model.player(), content, combat);
        }
        public Views.WorldView world() {
            if (!inWorld()) return null;
            return new EngineViews.WorldViewImpl(model.world(), factionWarView());
        }
        public Views.CombatView combat() {
            if (model == null || combat == null || !combat.isActive()) return null;
            return new EngineViews.CombatViewImpl(combat, model.player());
        }
        public Views.CreationView creation() {
            // Always non-null so the UI's Library/Codex can read the reference lists
            // (races/families/classes) even while PLAYING. During creation the
            // selected*/step fields reflect the live wizard; otherwise they are blank.
            CreationState c = (state == GameStateType.CHARACTER_CREATION && creation != null)
                    ? creation : REFERENCE_CREATION;
            return new CreationViewImpl(c);
        }
        public List<Views.QuestView> quests() {
            List<Views.QuestView> out = new ArrayList<Views.QuestView>();
            if (quests != null) {
                for (QuestRun r : quests.runs()) out.add(new EngineViews.QuestViewImpl(r));
            }
            return out;
        }
        public Views.StudyView study() {
            return model == null ? null : new EngineViews.StudyViewImpl(study, model);
        }
        public Views.CraftingView crafting() {
            return model == null ? null : new EngineViews.CraftingViewImpl(craft, model, content);
        }
        public Views.AuctionView auction() {
            if (model == null || state != GameStateType.AUCTION) return null;
            return new EngineViews.AuctionViewImpl(craft, model, content);
        }
        public Views.FamilyView family() {
            return model == null ? null : familyView();
        }
        public List<Views.ChatLineView> chat() {
            return new ArrayList<Views.ChatLineView>(chat);
        }
        public List<String> menuOptions() {
            if (state == GameStateType.TITLE) return TITLE_MENU;
            if (state == GameStateType.GAME_OVER) return GAME_OVER_MENU;
            if (state == GameStateType.SETTINGS) return Arrays.asList("Back");
            return new ArrayList<String>();
        }
        public String toastMessage() { return toast; }
    }

    private Views.FactionWarView factionWarView() {
        if (war == null) return null;
        return new EngineViews.FactionWarViewImpl(
                war.objectives(), model.asharrWarScore(), model.kujixWarScore());
    }

    private FamilyArchetype archetype() {
        Defs.FamilyDef f = content.family(model.player().familyId());
        return f != null ? f.archetype : FamilyArchetype.ACHIEVER;
    }

    private Views.FamilyView familyView() {
        Defs.FamilyDef fam = content.family(model.player().familyId());
        FamilyArchetype arch = fam != null ? fam.archetype : FamilyArchetype.ACHIEVER;
        List<String> members = familyMembers(fam);
        String vendorId = null;
        if (model.world() != null) {
            for (WorldModel.NpcEntity n : model.world().npcs()) {
                if (n.vendor()) { vendorId = n.id(); break; }
            }
        }
        return new EngineViews.FamilyViewImpl(fam, members, archetypeBonus(arch), vendorId);
    }

    private List<String> familyMembers(Defs.FamilyDef fam) {
        // Flavor NPC "family" members (single-player substitute for the family channel). [Gap]
        List<String> out = new ArrayList<String>();
        String[] pool = {"Aldric", "Bryn", "Cael", "Dara", "Eun", "Fenn", "Gwyn", "Hale"};
        int base = fam == null ? 0 : Math.abs(fam.id.hashCode());
        for (int i = 0; i < 3; i++) out.add(pool[(base + i) % pool.length]);
        return out;
    }

    private static String archetypeBonus(FamilyArchetype a) {
        switch (a) {
            case ACHIEVER: return "Achiever: a small bonus to XP and study progress.";
            case COMPETITOR: return "Competitor: a small bonus to combat prowess.";
            case EXPLORER: return "Explorer: biased toward discovery and travel quests.";
            case SOCIALIZER: return "Socializer: better vendor prices and reputation.";
            case CRAFTER: default: return "Crafter: a bonus to tradeskill progress.";
        }
    }

    // ================= save / load =================

    private SaveGame buildSave() {
        CharacterModel p = model.player();
        SaveGame s = new SaveGame();
        s.seed = model.seed();
        s.raceId = p.raceId();
        s.familyId = p.familyId();
        s.classId = p.classId();
        s.name = p.getName();
        s.level = p.level();
        s.xp = p.xp();
        s.hp = p.hp();
        s.maxHp = p.maxHp();
        s.resource = p.resource();
        s.maxResource = p.maxResource();
        s.stance = p.stance() == null ? Stance.BALANCE.name() : p.stance().name();
        s.school = p.school() == null ? School.NONE.name() : p.school().name();
        s.gold = p.gold();
        for (Map.Entry<StatType, Integer> e : p.stats().entrySet()) s.stats.put(e.getKey(), e.getValue());
        for (SkillType sk : SkillType.values()) s.skills.put(sk, p.skill(sk));
        for (Map.Entry<String, Integer> e : p.inventory().entrySet()) s.inventory.put(e.getKey(), e.getValue());
        for (Map.Entry<EquipSlot, String> e : p.equipped().entrySet()) s.equipped.put(e.getKey(), e.getValue());
        s.studyAssignment = p.studyAssignment() == null ? null : p.studyAssignment().name();
        s.bankedStudy = p.bankedStudyProgress();
        s.posX = p.pos().x;
        s.posY = p.pos().y;
        s.zoneId = p.zoneId();
        s.lastSaveEpochMillis = model.lastSaveEpochMillis();
        s.asharrWarScore = model.asharrWarScore();
        s.kujixWarScore = model.kujixWarScore();

        for (QuestRun r : quests.runs()) {
            SaveGame.QuestSave q = new SaveGame.QuestSave();
            q.id = r.def.id; q.name = r.def.name; q.description = r.def.description;
            q.giverNpcId = r.def.giverNpcId; q.turnInNpcId = r.def.turnInNpcId;
            q.levelReq = r.def.levelReq; q.xpReward = r.def.xpReward; q.goldReward = r.def.goldReward;
            q.procedural = r.def.procedural; q.status = r.status.name();
            q.rewardItemIds = new ArrayList<String>(r.def.rewardItemIds);
            for (int i = 0; i < r.def.objectives.size(); i++) {
                Defs.ObjectiveDef o = r.def.objectives.get(i);
                SaveGame.ObjSave os = new SaveGame.ObjSave();
                os.type = o.type.name(); os.targetId = o.targetId == null ? "" : o.targetId;
                os.count = o.count; os.progress = r.progress[i]; os.text = o.text == null ? "" : o.text;
                q.objectives.add(os);
            }
            s.quests.add(q);
        }
        for (WarObjective o : war.objectives()) {
            SaveGame.WarObjSave w = new SaveGame.WarObjSave();
            w.name = o.name; w.control = o.control.name(); w.influence = o.influence; w.nextTick = o.nextTick;
            s.warObjectives.add(w);
        }
        for (Listing l : craft.exportPlayerListings()) {
            SaveGame.ListingSave ls = new SaveGame.ListingSave();
            ls.listingId = l.listingId; ls.itemId = l.itemId; ls.quantity = l.quantity; ls.price = l.price;
            s.listings.add(ls);
        }
        return s;
    }

    private void applyLoad(SaveGame s) {
        model = factory.newGame(s.seed);
        rng = new Random(s.seed);
        factory.applyCreation(model, s.raceId, s.familyId, s.classId, s.name);
        CharacterModel p = model.player();

        p.setLevel(s.level);
        p.setXp(s.xp);
        p.setMaxHp(s.maxHp);
        p.setHp(s.hp);
        p.setMaxResource(s.maxResource);
        p.setResource(s.resource);
        if (s.stance != null) p.setStance(Stance.valueOf(s.stance));
        if (s.school != null) p.setSchool(School.valueOf(s.school));
        p.addGold(s.gold - p.gold());

        for (Map.Entry<StatType, Integer> e : s.stats.entrySet()) p.stats().put(e.getKey(), e.getValue());
        for (Map.Entry<SkillType, Integer> e : s.skills.entrySet()) {
            int cur = p.skill(e.getKey());
            int delta = e.getValue() - cur;
            if (delta != 0) p.addSkillProgress(e.getKey(), delta);
        }
        p.inventory().clear();
        for (Map.Entry<String, Integer> e : s.inventory.entrySet()) p.addItem(e.getKey(), e.getValue());
        p.equipped().clear();
        for (Map.Entry<EquipSlot, String> e : s.equipped.entrySet()) p.equipped().put(e.getKey(), e.getValue());

        p.setStudyAssignment(s.studyAssignment == null ? null : SkillType.valueOf(s.studyAssignment));
        p.setBankedStudyProgress(s.bankedStudy);

        model.setWarScores(s.asharrWarScore, s.kujixWarScore);
        model.setLastSaveEpochMillis(s.lastSaveEpochMillis);

        // load the saved zone, then place the player where they were
        model.loadZone(s.zoneId);
        p.setZoneId(s.zoneId);
        p.setPos(new GridPos(s.posX, s.posY));

        initSystems();

        // restore quests
        for (SaveGame.QuestSave q : s.quests) {
            List<Defs.ObjectiveDef> objs = new ArrayList<Defs.ObjectiveDef>();
            for (SaveGame.ObjSave o : q.objectives) {
                objs.add(new Defs.ObjectiveDef(ObjectiveType.valueOf(o.type), o.targetId, o.count, o.text));
            }
            Defs.QuestDef def = new Defs.QuestDef(q.id, q.name, q.giverNpcId, q.turnInNpcId, q.levelReq,
                    q.xpReward, q.goldReward, objs, q.rewardItemIds, q.procedural, q.description);
            quests.acceptDef(def);
            QuestRun run = quests.get(q.id);
            if (run != null) {
                for (int i = 0; i < q.objectives.size() && i < run.progress.length; i++) {
                    run.progress[i] = q.objectives.get(i).progress;
                }
                run.status = QuestStatus.valueOf(q.status);
            }
        }
        // restore faction war
        war.reset();
        for (SaveGame.WarObjSave w : s.warObjectives) {
            war.objectives().add(new WarObjective(w.name,
                    com.whim.alganon.api.Enums.ControlState.valueOf(w.control), w.influence, w.nextTick));
        }
        if (war.objectives().isEmpty()) war.seed();
        // reseed NPC auction + restore player listings
        craft.seedListings();
        List<Listing> playerListings = new ArrayList<Listing>();
        for (SaveGame.ListingSave l : s.listings) {
            playerListings.add(new Listing(l.listingId, l.itemId, l.quantity, l.price, true));
        }
        craft.importListings(playerListings);

        // grant offline Study progress based on real elapsed wall-clock time
        long elapsed = System.currentTimeMillis() - model.lastSaveEpochMillis();
        study.grantOffline(model, elapsed);

        state = model.player().alive() ? GameStateType.PLAYING : GameStateType.GAME_OVER;
        log(ChatChannel.SYSTEM, "Loaded " + p.getName() + " (level " + p.level() + ").");
        if (state == GameStateType.PLAYING) startTimer();
        fireChanged();
    }

    // ================= creation view =================

    private final class CreationViewImpl implements Views.CreationView {
        private final CreationState c;
        CreationViewImpl(CreationState c) { this.c = c; }
        public List<Defs.RaceDef> races() { return content.races(); }
        public List<Defs.FamilyDef> familiesFor(String raceId) {
            Defs.RaceDef r = content.race(raceId);
            List<Defs.FamilyDef> out = new ArrayList<Defs.FamilyDef>();
            if (r == null) return out;
            for (Defs.FamilyDef f : content.families()) {
                if (f.faction == r.faction) out.add(f);
            }
            return out;
        }
        public List<Defs.ClassDef> classes() { return content.classes(); }
        public String selectedRaceId() { return c.raceId; }
        public String selectedFamilyId() { return c.familyId; }
        public String selectedClassId() { return c.classId; }
        public String enteredName() { return c.name; }
        public int step() { return c.step; }
    }
}
