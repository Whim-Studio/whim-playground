package com.arpg.ui;

import com.arpg.model.PlayerAction;

/**
 * Central factory for every {@link PlayerAction} the UI fires.
 *
 * <p>All UI panels create actions ONLY through this class. This is deliberate:
 * the {@code PlayerAction} field set is fixed by the shared contract
 * (abilityId, itemId, realmId, targetIndex, attribute) and constructed via the
 * single all-args constructor
 * {@code PlayerAction(Type, abilityId, itemId, realmId, targetIndex, attribute)}.
 * If Task&nbsp;1 exposes a different constructor/builder shape, only this one
 * file needs to change at consolidation — the panels stay untouched.</p>
 */
final class UiActions {

    private UiActions() {
    }

    /** Sentinel for "no target index supplied". */
    static final int NO_TARGET = -1;

    static PlayerAction useAbility(String abilityId, int targetIndex) {
        return new PlayerAction(PlayerAction.Type.USE_ABILITY, abilityId, null, null, targetIndex, null);
    }

    static PlayerAction basicAttack(int targetIndex) {
        return new PlayerAction(PlayerAction.Type.BASIC_ATTACK, null, null, null, targetIndex, null);
    }

    static PlayerAction moveToRealm(String realmId) {
        return new PlayerAction(PlayerAction.Type.MOVE_TO_REALM, null, null, realmId, NO_TARGET, null);
    }

    static PlayerAction equipItem(String itemId) {
        return new PlayerAction(PlayerAction.Type.EQUIP_ITEM, null, itemId, null, NO_TARGET, null);
    }

    static PlayerAction unequipItem(String itemId) {
        return new PlayerAction(PlayerAction.Type.UNEQUIP_ITEM, null, itemId, null, NO_TARGET, null);
    }

    static PlayerAction usePetAbility(String abilityId, int targetIndex) {
        return new PlayerAction(PlayerAction.Type.USE_PET_ABILITY, abilityId, null, null, targetIndex, null);
    }

    static PlayerAction reforgeItem(String itemId) {
        return new PlayerAction(PlayerAction.Type.REFORGE_ITEM, null, itemId, null, NO_TARGET, null);
    }

    static PlayerAction advanceEncounter() {
        return new PlayerAction(PlayerAction.Type.ADVANCE_ENCOUNTER, null, null, null, NO_TARGET, null);
    }

    static PlayerAction allocateAttribute(String attribute) {
        return new PlayerAction(PlayerAction.Type.ALLOCATE_ATTRIBUTE, null, null, null, NO_TARGET, attribute);
    }
}
