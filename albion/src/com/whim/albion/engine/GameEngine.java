package com.whim.albion.engine;

import com.whim.albion.api.ActionResult;
import com.whim.albion.api.Combatant;
import com.whim.albion.api.Content;
import com.whim.albion.api.GameController;
import com.whim.albion.api.GameModel;
import com.whim.albion.api.ModelFactory;
import com.whim.albion.api.PartyModel;
import com.whim.albion.api.WorldModel;
import com.whim.albion.api.Enums.CombatActionType;
import com.whim.albion.api.Enums.Direction;
import com.whim.albion.api.Enums.EquipSlot;
import com.whim.albion.api.Enums.GameStateType;
import com.whim.albion.api.Enums.MapType;
import com.whim.albion.api.Views.GameStateView;
import com.whim.albion.combat.CombatEngine;
import com.whim.albion.dialogue.DialogueRunner;
import com.whim.albion.persistence.SaveManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * The game brain. Implements {@link GameController}: owns the
 * {@link GameStateType} state machine, drives Task 1's model through the
 * {@code api} seams, runs combat and dialogue via its own sub-systems, and
 * fires {@link ChangeListener#onStateChanged()} after every mutation.
 *
 * <p>Depends on {@code com.whim.albion.api} only. The concrete model is obtained
 * through a {@link ModelFactory} injected in the constructor, so the engine never
 * imports a Task 1 class.</p>
 */
public final class GameEngine implements GameController {

    private final ModelFactory factory;
    private final SaveManager saves;
    private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();

    private GameModel model;
    private Random rng = new Random();
    private long seed;

    private GameStateType state = GameStateType.TITLE;
    /** Exploration state to return to after combat/dialogue/overlays. */
    private GameStateType exploreState = GameStateType.OVERWORLD;
    /** Overlay (INVENTORY/JOURNAL/CHARACTER_SHEET/MENU) return target. */
    private GameStateType overlayReturn = GameStateType.OVERWORLD;

    private CombatEngine combat;
    private DialogueRunner dialogue;
    private GameContextImpl context;

    private String statusMessage = "";

    /** Cell whose encounter triggered the current battle, to clear on victory. */
    private int encounterX = -1;
    private int encounterY = -1;

    // Persistence shadow: flags/quests seen via GameContext, enumerable for save
    // (the journal view exposes neither raw flag keys nor quest ids).
    final java.util.Map<String, Boolean> shadowFlags = new java.util.LinkedHashMap<String, Boolean>();
    final java.util.Map<String, com.whim.albion.api.Enums.QuestStatus> shadowQuests =
            new java.util.LinkedHashMap<String, com.whim.albion.api.Enums.QuestStatus>();
    final java.util.Map<String, String> shadowQuestTitles = new java.util.LinkedHashMap<String, String>();
    final java.util.Map<String, java.util.List<String>> shadowQuestObjectives =
            new java.util.LinkedHashMap<String, java.util.List<String>>();

    public GameEngine(ModelFactory factory) {
        if (factory == null) throw new IllegalArgumentException("factory required");
        this.factory = factory;
        this.saves = new SaveManager();
    }

    // ------------------------------------------------------------ accessors

    GameModel model() { return model; }
    Random rng() { return rng; }
    void setStatus(String msg) { this.statusMessage = msg == null ? "" : msg; }

    // ------------------------------------------------------------ lifecycle

    @Override
    public GameStateView state() { return new Views_State(); }

    @Override
    public void newGame(long seed) {
        this.seed = seed;
        this.rng = new Random(seed);
        this.model = factory.newGame(seed);
        this.context = new GameContextImpl(this);
        this.combat = null;
        this.dialogue = null;
        this.statusMessage = "";
        clearShadow();
        syncExploreState();
        this.state = exploreState;
        fire();
    }

    /** Set exploreState from the loaded map type (OVERWORLD vs DUNGEON). */
    private void syncExploreState() {
        WorldModel w = model.world();
        exploreState = (w != null && w.mapType() == MapType.INDOOR_3D)
                ? GameStateType.DUNGEON : GameStateType.OVERWORLD;
    }

    @Override
    public List<String> saveSlots() { return saves.slotLabels(); }

    @Override
    public boolean saveGame(int slot) {
        if (model == null) return false;
        boolean ok = saves.write(slot, captureSnapshot());
        setStatus(ok ? "Game saved to slot " + slot + "." : "Save failed.");
        fire();
        return ok;
    }

    @Override
    public boolean loadGame(int slot) {
        SaveManager.Snapshot s = saves.read(slot);
        boolean ok = s != null;
        if (ok) {
            applySnapshot(s);
            this.combat = null;
            this.dialogue = null;
            syncExploreState();
            this.state = exploreState;
            setStatus("Game loaded from slot " + slot + ".");
        } else {
            setStatus("Load failed.");
        }
        fire();
        return ok;
    }

    // ---- Persistence: capture / apply a snapshot (SaveManager owns file format) ----

    private void clearShadow() {
        shadowFlags.clear();
        shadowQuests.clear();
        shadowQuestTitles.clear();
        shadowQuestObjectives.clear();
    }

    /** Gather everything savable into a persistence Snapshot. */
    private SaveManager.Snapshot captureSnapshot() {
        SaveManager.Snapshot s = new SaveManager.Snapshot();
        s.seed = seed;
        WorldModel w = model.world();
        s.mapId = w.mapId();
        s.px = w.player().x();
        s.py = w.player().y();
        s.facing = w.player().facing().name();
        s.gold = model.party().gold();
        List<Combatant> pc = model.party().asCombatants();
        for (int i = 0; i < pc.size(); i++) s.vitals.add(new int[]{ pc.get(i).lp(), pc.get(i).sp() });
        s.flags.putAll(shadowFlags);
        for (java.util.Map.Entry<String, com.whim.albion.api.Enums.QuestStatus> e : shadowQuests.entrySet()) {
            SaveManager.QuestRec q = new SaveManager.QuestRec();
            q.id = e.getKey();
            q.status = e.getValue().name();
            q.title = shadowQuestTitles.containsKey(q.id) ? shadowQuestTitles.get(q.id) : q.id;
            java.util.List<String> objs = shadowQuestObjectives.get(q.id);
            if (objs != null) q.objectives.addAll(objs);
            s.quests.add(q);
        }
        return s;
    }

    /** Rebuild a fresh model from the snapshot's seed and re-apply savable deltas. */
    private void applySnapshot(SaveManager.Snapshot s) {
        this.seed = s.seed;
        this.rng = new Random(s.seed);
        this.model = factory.newGame(s.seed);
        this.context = new GameContextImpl(this);
        clearShadow();

        if (s.mapId != null) model.world().loadMap(s.mapId, s.px, s.py, parseFacing(s.facing));

        int goldDelta = s.gold - model.party().gold();
        if (goldDelta > 0) model.party().addGold(goldDelta);
        else if (goldDelta < 0) model.party().spendGold(-goldDelta);

        for (java.util.Map.Entry<String, Boolean> e : s.flags.entrySet()) {
            model.journal().setFlag(e.getKey(), e.getValue());
            shadowFlags.put(e.getKey(), e.getValue());
        }
        for (int i = 0; i < s.quests.size(); i++) {
            SaveManager.QuestRec q = s.quests.get(i);
            String first = q.objectives.isEmpty() ? "Quest started." : q.objectives.get(0);
            model.journal().startQuest(q.id, q.title == null ? q.id : q.title, first);
            for (int j = 1; j < q.objectives.size(); j++) model.journal().addObjective(q.id, q.objectives.get(j));
            com.whim.albion.api.Enums.QuestStatus st = parseStatus(q.status);
            if (st == com.whim.albion.api.Enums.QuestStatus.COMPLETED) model.journal().completeQuest(q.id);
            shadowQuests.put(q.id, st);
            shadowQuestTitles.put(q.id, q.title == null ? q.id : q.title);
            shadowQuestObjectives.put(q.id, new ArrayList<String>(q.objectives));
        }
        // Re-apply LP/SP: SP exactly (spend/restore), LP best-effort (no api setter).
        List<Combatant> pc = model.party().asCombatants();
        for (int i = 0; i < pc.size() && i < s.vitals.size(); i++) {
            Combatant c = pc.get(i);
            int lp = s.vitals.get(i)[0], sp = s.vitals.get(i)[1];
            int missing = c.lp() - lp;
            if (missing > 0) c.takeDamage(missing);
            int spDelta = c.sp() - sp;
            if (spDelta > 0) c.spendSp(spDelta);
            else if (spDelta < 0) c.restoreSp(-spDelta);
        }
    }

    private static Direction parseFacing(String s) {
        try { return Direction.valueOf(s); } catch (RuntimeException e) { return Direction.SOUTH; }
    }
    private static com.whim.albion.api.Enums.QuestStatus parseStatus(String s) {
        try { return com.whim.albion.api.Enums.QuestStatus.valueOf(s); }
        catch (RuntimeException e) { return com.whim.albion.api.Enums.QuestStatus.ACTIVE; }
    }

    // ------------------------------------------------------------ menus

    @Override
    public ActionResult selectMenuOption(int index) {
        switch (state) {
            case TITLE:
            case GAME_OVER:
                if (index == 0) { newGame(rng.nextLong() ^ System.identityHashCode(this)); return ActionResult.ok("New game."); }
                if (index == 1) { return loadGame(0) ? ActionResult.ok("Loaded.") : ActionResult.fail("No save in slot 0."); }
                return ActionResult.ok("Quit requested.");
            case MENU:
                if (index == 0) { closeOverlay(); return ActionResult.ok("Resumed."); }
                if (index == 1) { return saveGame(0) ? ActionResult.ok("Saved.") : ActionResult.fail("Save failed."); }
                if (index == 2) { return loadGame(0) ? ActionResult.ok("Loaded.") : ActionResult.fail("No save."); }
                this.state = GameStateType.TITLE; fire(); return ActionResult.ok("Returned to title.");
            default:
                return ActionResult.fail("No menu here.");
        }
    }

    // ------------------------------------------------------------ exploration

    @Override
    public ActionResult move(Direction dir) {
        if (model == null) return ActionResult.fail("No game.");
        if (state != GameStateType.OVERWORLD && state != GameStateType.DUNGEON)
            return ActionResult.fail("Cannot move now.");
        WorldModel w = model.world();
        boolean stepped;
        if (state == GameStateType.DUNGEON) {
            // First-person: forward/back = step, left/right = turn.
            Direction facing = w.player().facing();
            if (dir == Direction.WEST) { w.turnPlayer(facing.left()); fire(); return ActionResult.ok(); }
            if (dir == Direction.EAST) { w.turnPlayer(facing.right()); fire(); return ActionResult.ok(); }
            Direction step = (dir == Direction.SOUTH) ? facing.opposite() : facing;
            stepped = w.stepPlayer(step);
        } else {
            stepped = w.stepPlayer(dir);
        }
        if (!stepped) { setStatus("Blocked."); fire(); return ActionResult.fail("Blocked."); }
        setStatus("");
        ActionResult r = resolveTileEvents();
        fire();
        return r;
    }

    @Override
    public ActionResult moveTo(int x, int y) {
        if (model == null) return ActionResult.fail("No game.");
        if (state != GameStateType.OVERWORLD) return ActionResult.fail("Click-move only outdoors.");
        WorldModel w = model.world();
        int px = w.player().x(), py = w.player().y();
        int dx = Integer.compare(x, px), dy = Integer.compare(y, py);
        // Prefer the larger-magnitude axis; fall back to the other if blocked.
        Direction primary, secondary;
        if (Math.abs(x - px) >= Math.abs(y - py)) {
            primary = dx != 0 ? (dx > 0 ? Direction.EAST : Direction.WEST) : null;
            secondary = dy != 0 ? (dy > 0 ? Direction.SOUTH : Direction.NORTH) : null;
        } else {
            primary = dy != 0 ? (dy > 0 ? Direction.SOUTH : Direction.NORTH) : null;
            secondary = dx != 0 ? (dx > 0 ? Direction.EAST : Direction.WEST) : null;
        }
        boolean stepped = false;
        if (primary != null) stepped = w.stepPlayer(primary);
        if (!stepped && secondary != null) stepped = w.stepPlayer(secondary);
        if (!stepped) { setStatus("Blocked."); fire(); return ActionResult.fail("Cannot path there."); }
        setStatus("");
        ActionResult r = resolveTileEvents();
        fire();
        return r;
    }

    /** After a successful step, fire encounter / transition on the new cell. */
    private ActionResult resolveTileEvents() {
        WorldModel w = model.world();
        int px = w.player().x(), py = w.player().y();
        String enc = w.encounterAt(px, py);
        if (enc != null) {
            encounterX = px; encounterY = py;
            startCombat(enc);
            return ActionResult.ok("Ambush!");
        }
        WorldModel.Transition t = w.transitionAt(px, py);
        if (t != null) {
            w.loadMap(t.targetMapId(), t.targetX(), t.targetY(), t.targetFacing());
            syncExploreState();
            state = exploreState;
            setStatus("Entered " + w.mapName() + ".");
            return ActionResult.ok("Map changed.");
        }
        return ActionResult.ok();
    }

    @Override
    public ActionResult interact() {
        if (model == null) return ActionResult.fail("No game.");
        if (state != GameStateType.OVERWORLD && state != GameStateType.DUNGEON)
            return ActionResult.fail("Nothing to do.");
        WorldModel w = model.world();
        Direction facing = w.player().facing();
        int tx = w.player().x() + facing.dx();
        int ty = w.player().y() + facing.dy();

        WorldModel.Interactable it = w.interactableAt(tx, ty);
        if (it == null) it = w.interactableAt(w.player().x(), w.player().y());
        if (it != null) {
            if (it.dialogueId() != null) {
                return openDialogue(it.dialogueId());
            }
            if (!it.consumed() && it.loot() != null && !it.loot().isEmpty()) {
                StringBuilder sb = new StringBuilder("Found: ");
                List<String> loot = it.loot();
                for (int i = 0; i < loot.size(); i++) {
                    model.party().giveItem(model.party().activeIndex(), loot.get(i), 1);
                    if (i > 0) sb.append(", ");
                    sb.append(loot.get(i));
                }
                it.consume();
                setStatus(sb.toString());
                fire();
                return ActionResult.ok(sb.toString());
            }
            setStatus(it.name() + ": nothing happens.");
            fire();
            return ActionResult.ok(it.name());
        }
        // Doors: interacting with a facing transition also travels.
        WorldModel.Transition t = w.transitionAt(tx, ty);
        if (t != null) {
            w.loadMap(t.targetMapId(), t.targetX(), t.targetY(), t.targetFacing());
            syncExploreState();
            state = exploreState;
            setStatus("Entered " + w.mapName() + ".");
            fire();
            return ActionResult.ok("Map changed.");
        }
        setStatus("Nothing here.");
        fire();
        return ActionResult.fail("Nothing to interact with.");
    }

    // ------------------------------------------------------------ dialogue

    private ActionResult openDialogue(String dialogueId) {
        Content.DialogueTree tree = model.content().dialogue(dialogueId);
        if (tree == null) { setStatus("They have nothing to say."); fire(); return ActionResult.fail("No dialogue."); }
        this.dialogue = new DialogueRunner(tree, context);
        this.state = GameStateType.DIALOGUE;
        fire();
        return ActionResult.ok("Talking.");
    }

    @Override
    public ActionResult selectDialogueOption(int index) {
        if (state != GameStateType.DIALOGUE || dialogue == null)
            return ActionResult.fail("Not in dialogue.");
        boolean ended = dialogue.select(index);
        // A dialogue option may have triggered combat / teleport via GameContext,
        // which changes state directly; only fall back to explore if still talking.
        if (state == GameStateType.DIALOGUE && ended) {
            dialogue = null;
            state = exploreState;
        }
        fire();
        return ActionResult.ok();
    }

    // ------------------------------------------------------------ combat

    /** Begin a battle; called from exploration triggers and dialogue scripting. */
    public void startCombat(String encounterId) {
        List<Combatant> enemies = model.content().spawnEncounter(encounterId);
        if (enemies == null || enemies.isEmpty()) { setStatus("The threat vanishes."); return; }
        this.dialogue = null;
        this.combat = new CombatEngine(model, enemies, rng);
        this.state = GameStateType.COMBAT;
        setStatus("Battle begins!");
    }

    @Override
    public ActionResult combatAction(CombatActionType type, int targetIndex, String optionId) {
        if (state != GameStateType.COMBAT || combat == null)
            return ActionResult.fail("Not in combat.");
        ActionResult r = combat.playerAction(type, targetIndex, optionId);
        if (combat.finished()) endCombat();
        fire();
        return r;
    }

    private void endCombat() {
        boolean victory = combat.victory();
        if (victory) {
            model.party().awardXp(combat.totalXp());
            model.party().addGold(combat.totalGold());
            List<String> loot = combat.loot();
            for (int i = 0; i < loot.size(); i++)
                model.party().giveItem(model.party().activeIndex(), loot.get(i), 1);
            if (encounterX >= 0) model.world().clearEncounter(encounterX, encounterY);
            setStatus("Victory! +" + combat.totalXp() + " xp, +" + combat.totalGold() + " gold.");
            state = exploreState;
        } else if (model.party().wiped()) {
            setStatus("The party has fallen...");
            state = GameStateType.GAME_OVER;
        } else {
            // Fled.
            if (encounterX >= 0) model.world().clearEncounter(encounterX, encounterY);
            setStatus("You escaped.");
            state = exploreState;
        }
        encounterX = encounterY = -1;
        combat = null;
    }

    // ------------------------------------------------------------ party / inventory

    @Override
    public void setActiveMember(int index) {
        if (model != null) { model.party().setActiveIndex(index); fire(); }
    }

    @Override
    public ActionResult equip(int memberIndex, String itemId) {
        if (model == null) return ActionResult.fail("No game.");
        ActionResult r = model.party().equip(memberIndex, itemId);
        setStatus(r.message());
        fire();
        return r;
    }

    @Override
    public ActionResult unequip(int memberIndex, EquipSlot slot) {
        if (model == null) return ActionResult.fail("No game.");
        ActionResult r = model.party().unequip(memberIndex, slot);
        setStatus(r.message());
        fire();
        return r;
    }

    @Override
    public ActionResult useItem(int memberIndex, String itemId) {
        if (model == null) return ActionResult.fail("No game.");
        ActionResult r = model.party().useItem(memberIndex, itemId);
        setStatus(r.message());
        fire();
        return r;
    }

    // ------------------------------------------------------------ overlays

    @Override
    public void openState(GameStateType s) {
        if (s == GameStateType.INVENTORY || s == GameStateType.CHARACTER_SHEET
                || s == GameStateType.JOURNAL || s == GameStateType.MENU) {
            if (state == GameStateType.OVERWORLD || state == GameStateType.DUNGEON)
                overlayReturn = state;
            state = s;
            fire();
        }
    }

    @Override
    public void closeOverlay() {
        if (state == GameStateType.INVENTORY || state == GameStateType.CHARACTER_SHEET
                || state == GameStateType.JOURNAL || state == GameStateType.MENU) {
            state = overlayReturn;
            fire();
        }
    }

    // ------------------------------------------------------------ listeners

    @Override
    public void addChangeListener(ChangeListener l) { if (l != null) listeners.add(l); }

    @Override
    public void removeChangeListener(ChangeListener l) { listeners.remove(l); }

    void fire() {
        for (int i = 0; i < listeners.size(); i++) listeners.get(i).onStateChanged();
    }

    GameContextImpl context() { return context; }

    /** Re-derive OVERWORLD/DUNGEON after content teleports the player. */
    void afterWorldChanged() {
        syncExploreState();
        if (state == GameStateType.OVERWORLD || state == GameStateType.DUNGEON)
            state = exploreState;
    }

    // ------------------------------------------------------------ state view

    /** Live snapshot backed by the current model + engine sub-systems. */
    final class Views_State implements GameStateView {
        @Override public GameStateType current() { return state; }
        @Override public com.whim.albion.api.Views.WorldView world() {
            return (state == GameStateType.OVERWORLD || state == GameStateType.DUNGEON
                    || state == GameStateType.INVENTORY || state == GameStateType.CHARACTER_SHEET
                    || state == GameStateType.JOURNAL || state == GameStateType.MENU)
                    && model != null ? model.world() : null;
        }
        @Override public com.whim.albion.api.Views.PartyView party() {
            return model != null ? model.party() : null;
        }
        @Override public com.whim.albion.api.Views.CombatView combat() {
            return state == GameStateType.COMBAT ? combat : null;
        }
        @Override public com.whim.albion.api.Views.DialogueView dialogue() {
            return state == GameStateType.DIALOGUE && GameEngine.this.dialogue != null
                    ? GameEngine.this.dialogue.view() : null;
        }
        @Override public com.whim.albion.api.Views.JournalView journal() {
            return model != null ? model.journal() : null;
        }
        @Override public int gold() { return model != null ? model.party().gold() : 0; }
        @Override public String statusMessage() { return statusMessage; }
        @Override public List<String> menuOptions() {
            switch (state) {
                case TITLE:
                case GAME_OVER:
                    return new ArrayList<String>(Arrays.asList("New Game", "Load Game", "Quit"));
                case MENU:
                    return new ArrayList<String>(Arrays.asList("Resume", "Save Game", "Load Game", "Quit to Title"));
                default:
                    return new ArrayList<String>();
            }
        }
    }
}
