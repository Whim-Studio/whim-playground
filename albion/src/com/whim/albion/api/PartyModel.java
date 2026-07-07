package com.whim.albion.api;

import com.whim.albion.api.Enums.EquipSlot;

import java.util.List;

/**
 * Mutable party state owned by the model (Task 1), driven by the engine (Task 2).
 * Extends the read-only {@link Views.PartyView} the UI renders.
 */
public interface PartyModel extends Views.PartyView {

    void setActiveIndex(int index);

    int gold();
    void addGold(int amount);
    boolean spendGold(int amount);

    /** Party members as combat participants (living and dead, ordered as displayed). */
    List<Combatant> asCombatants();

    /** Add an item (by content id) to a member's pack. */
    boolean giveItem(int memberIndex, String itemId, int quantity);
    /** Remove an item; false if not present. */
    boolean takeItem(String itemId, int quantity);
    boolean hasItem(String itemId);

    ActionResult equip(int memberIndex, String itemId);
    ActionResult unequip(int memberIndex, EquipSlot slot);
    ActionResult useItem(int memberIndex, String itemId);

    /** Award XP to the whole party and apply any level-ups. */
    void awardXp(int xp);

    /** True if every member has 0 LP (triggers GAME_OVER). */
    boolean wiped();
}
