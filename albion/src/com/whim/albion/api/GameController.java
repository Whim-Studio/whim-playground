package com.whim.albion.api;

import com.whim.albion.api.Enums.CombatActionType;
import com.whim.albion.api.Enums.Direction;
import com.whim.albion.api.Enums.EquipSlot;
import com.whim.albion.api.Enums.GameStateType;

import java.util.List;

/**
 * THE single seam between the UI (Task 3) and the game engine (Task 2). The UI
 * holds a reference of exactly this type and reads {@link Views.GameStateView}
 * for rendering. Task 3 also ships a dev {@code StubController} implementing this
 * so the UI can run before the engine lands.
 *
 * Threading: all methods are called from the Swing EDT. The engine is
 * turn/menu-driven (no background sim thread required); after any state mutation
 * the engine fires {@link ChangeListener#onStateChanged()} so the UI repaints.
 */
public interface GameController {

    /** Current renderable snapshot. Never null. */
    Views.GameStateView state();

    // ----- lifecycle -----
    void newGame(long seed);
    List<String> saveSlots();
    boolean saveGame(int slot);
    boolean loadGame(int slot);

    // ----- title / menu -----
    /** Choose a menu option by index (TITLE / MENU / GAME_OVER states). */
    ActionResult selectMenuOption(int index);

    // ----- exploration -----
    /** Step/rotate in OVERWORLD (grid step) or DUNGEON (forward/back = step, left/right = turn). */
    ActionResult move(Direction dir);
    /** Click-to-move to an outdoor tile (engine paths toward it one step). */
    ActionResult moveTo(int x, int y);
    /** Look/Use/Talk on the tile the player faces (context sensitive). */
    ActionResult interact();

    // ----- dialogue -----
    ActionResult selectDialogueOption(int index);

    // ----- combat -----
    /**
     * Perform the current combatant's action.
     * @param type       action kind
     * @param targetIndex index into {@link Views.CombatView#combatants()} (or grid cell for MOVE), -1 if N/A
     * @param optionId   spell id (CAST) or item id (ITEM), else null
     */
    ActionResult combatAction(CombatActionType type, int targetIndex, String optionId);

    // ----- party / inventory -----
    void setActiveMember(int index);
    ActionResult equip(int memberIndex, String itemId);
    ActionResult unequip(int memberIndex, EquipSlot slot);
    ActionResult useItem(int memberIndex, String itemId);

    // ----- ui-driven overlay states (inventory, journal, character sheet, menu) -----
    void openState(GameStateType state);
    void closeOverlay();

    // ----- change notification -----
    void addChangeListener(ChangeListener listener);
    void removeChangeListener(ChangeListener listener);

    interface ChangeListener {
        void onStateChanged();
    }
}
