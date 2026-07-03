package com.arpg.model;

/**
 * Callbacks the engine fires as the simulation advances. The UI implements this
 * to drive animations, the combat log, and stat panels. Binding contract.
 */
public interface GameEventListener {

    void onDamageDealt(CombatParticipant source, CombatParticipant target, int amount, Ability ability);

    void onHealed(CombatParticipant source, CombatParticipant target, int amount);

    void onBuffApplied(CombatParticipant target, BuffDebuff buff);

    void onBuffExpired(CombatParticipant target, BuffDebuff buff);

    void onLevelUp(Character character, int newLevel);

    void onLootDropped(Item item);

    void onCombatLog(String message);

    void onParticipantDeath(CombatParticipant participant);

    void onGameStateChanged(GameStateSnapshot snapshot);
}
