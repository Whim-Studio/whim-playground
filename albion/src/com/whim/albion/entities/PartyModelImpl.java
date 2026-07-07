package com.whim.albion.entities;

import com.whim.albion.api.ActionResult;
import com.whim.albion.api.Combatant;
import com.whim.albion.api.Enums.EquipSlot;
import com.whim.albion.api.PartyModel;
import com.whim.albion.api.Views.CharacterView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The adventuring party: an ordered list of {@link Character}s, shared gold, and
 * the active-member cursor. Adapts to {@link PartyModel} for the engine and
 * exposes stable {@link PartyCombatant} adapters so a battle's LP/SP changes
 * flow back into the persistent characters.
 */
public final class PartyModelImpl implements PartyModel {

    private final List<Character> members = new ArrayList<Character>();
    private final List<PartyCombatant> combatants = new ArrayList<PartyCombatant>();
    private int activeIndex;
    private int gold;

    public void addMember(Character c) {
        members.add(c);
        combatants.add(new PartyCombatant("pc_" + members.size(), c));
    }

    // ----------------------------------------------------------- PartyView

    @Override public List<CharacterView> members() {
        return Collections.<CharacterView>unmodifiableList(new ArrayList<CharacterView>(members));
    }

    @Override public int activeIndex() { return activeIndex; }

    @Override public void setActiveIndex(int index) {
        if (index >= 0 && index < members.size()) activeIndex = index;
    }

    // --------------------------------------------------------------- economy

    @Override public int gold() { return gold; }
    @Override public void addGold(int amount) { gold = Math.max(0, gold + amount); }

    @Override public boolean spendGold(int amount) {
        if (amount < 0 || gold < amount) return false;
        gold -= amount;
        return true;
    }

    // ---------------------------------------------------------------- combat

    @Override public List<Combatant> asCombatants() {
        return Collections.<Combatant>unmodifiableList(new ArrayList<Combatant>(combatants));
    }

    // ------------------------------------------------------------- inventory

    @Override public boolean giveItem(int memberIndex, String itemId, int quantity) {
        if (memberIndex < 0 || memberIndex >= members.size()) return false;
        return members.get(memberIndex).inventoryModel().add(itemId, quantity);
    }

    @Override public boolean takeItem(String itemId, int quantity) {
        for (Character c : members) {
            if (c.inventoryModel().has(itemId) && c.inventoryModel().remove(itemId, quantity)) {
                return true;
            }
        }
        return false;
    }

    @Override public boolean hasItem(String itemId) {
        for (Character c : members) {
            if (c.inventoryModel().has(itemId)) return true;
        }
        return false;
    }

    @Override public ActionResult equip(int memberIndex, String itemId) {
        if (memberIndex < 0 || memberIndex >= members.size()) return ActionResult.fail("No such member.");
        return members.get(memberIndex).equip(itemId);
    }

    @Override public ActionResult unequip(int memberIndex, EquipSlot slot) {
        if (memberIndex < 0 || memberIndex >= members.size()) return ActionResult.fail("No such member.");
        return members.get(memberIndex).unequip(slot);
    }

    @Override public ActionResult useItem(int memberIndex, String itemId) {
        if (memberIndex < 0 || memberIndex >= members.size()) return ActionResult.fail("No such member.");
        return members.get(memberIndex).useItem(itemId);
    }

    // --------------------------------------------------------------- xp/state

    @Override public void awardXp(int xp) {
        for (Character c : members) {
            if (c.alive()) c.addXp(xp);
        }
    }

    @Override public boolean wiped() {
        for (Character c : members) {
            if (c.alive()) return false;
        }
        return true;
    }

    /** Direct access for the factory when authoring the starting party. */
    public List<Character> characters() { return members; }
}
