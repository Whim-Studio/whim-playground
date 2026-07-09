package com.whim.alganon.api;

import com.whim.alganon.api.Enums.Direction;
import com.whim.alganon.api.Enums.EquipSlot;
import com.whim.alganon.api.Enums.GameStateType;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.Stance;

import java.util.List;

/**
 * THE single seam between the UI (Task 3) and the game engine (Task 2). The UI holds
 * exactly this type and reads {@link Views.GameStateView} for rendering.
 *
 * <p>Threading: all methods are called on the Swing EDT. The engine runs a
 * {@code javax.swing.Timer} tick (fixed logic step) for cooldowns/mob AI/regen and
 * fires {@link ChangeListener#onStateChanged()} after every tick or intent so the UI
 * repaints. Task 3 ships a {@code StubController} implementing this interface so the UI
 * can be built and demoed before the engine lands.</p>
 */
public interface GameController {

    /** Current renderable snapshot. Never null. */
    Views.GameStateView state();

    // ----- lifecycle / title -----
    ActionResult selectMenuOption(int index);
    List<String> saveSlots();
    boolean saveGame(int slot);
    boolean loadGame(int slot);

    // ----- character creation wizard (race -> family -> class -> name) -----
    void beginCreation();
    ActionResult chooseRace(String raceId);
    ActionResult chooseFamily(String familyId);
    ActionResult chooseClass(String classId);
    ActionResult setName(String name);
    ActionResult creationBack();
    ActionResult finishCreation();      // commits the new character, enters PLAYING

    // ----- exploration (top-down, tick-driven) -----
    ActionResult move(Direction dir);
    ActionResult interact();            // talk/vendor/gather/portal on the faced/adjacent tile

    // ----- combat / abilities -----
    /** Use an ability. targetIndex is an index into CombatView.combatants() or -1 for self/none. */
    ActionResult useAbility(String abilityId, int targetIndex);
    ActionResult setStance(Stance stance);   // Champion
    ActionResult setSchool(School school);    // Magus

    // ----- quests -----
    ActionResult acceptQuest(String questId);
    ActionResult turnInQuest(String questId);
    ActionResult generateProceduralQuest();   // dynamic quest generator

    // ----- study / offline progression -----
    ActionResult assignStudy(SkillType skill);
    ActionResult clearStudy();

    // ----- crafting / tradeskills / auction -----
    ActionResult gather(String nodeId);
    ActionResult craft(String recipeId);
    ActionResult auctionBuy(String listingId);
    ActionResult auctionPost(String itemId, int quantity, long price);

    // ----- inventory / equipment -----
    ActionResult equip(String itemId);
    ActionResult unequip(EquipSlot slot);
    ActionResult useItem(String itemId);

    // ----- overlay states (inventory, study, crafting, family, library, settings...) -----
    void openState(GameStateType state);
    void closeOverlay();

    // ----- change notification -----
    void addChangeListener(ChangeListener listener);
    void removeChangeListener(ChangeListener listener);

    interface ChangeListener { void onStateChanged(); }
}
