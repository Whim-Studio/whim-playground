package com.arpg.engine;

import com.arpg.model.Ability;
import com.arpg.model.BuffDebuff;
import com.arpg.model.Character;
import com.arpg.model.CombatParticipant;
import com.arpg.model.GameEventListener;
import com.arpg.model.GameStateSnapshot;
import com.arpg.model.Item;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Central fan-out for {@link GameEventListener} callbacks plus a rolling combat-log buffer.
 * All engine sub-systems fire through this so the facade wires listeners exactly once.
 */
final class EventBus {
    private static final int LOG_CAPACITY = 60;

    private final List<GameEventListener> listeners = new ArrayList<GameEventListener>();
    private final Deque<String> log = new ArrayDeque<String>();

    void addListener(GameEventListener l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }

    void damageDealt(CombatParticipant src, CombatParticipant tgt, int amount, Ability ability) {
        for (int i = 0; i < listeners.size(); i++) listeners.get(i).onDamageDealt(src, tgt, amount, ability);
    }

    void healed(CombatParticipant src, CombatParticipant tgt, int amount) {
        for (int i = 0; i < listeners.size(); i++) listeners.get(i).onHealed(src, tgt, amount);
    }

    void buffApplied(CombatParticipant tgt, BuffDebuff buff) {
        for (int i = 0; i < listeners.size(); i++) listeners.get(i).onBuffApplied(tgt, buff);
    }

    void buffExpired(CombatParticipant tgt, BuffDebuff buff) {
        for (int i = 0; i < listeners.size(); i++) listeners.get(i).onBuffExpired(tgt, buff);
    }

    void levelUp(Character c, int newLevel) {
        for (int i = 0; i < listeners.size(); i++) listeners.get(i).onLevelUp(c, newLevel);
    }

    void lootDropped(Item item) {
        for (int i = 0; i < listeners.size(); i++) listeners.get(i).onLootDropped(item);
    }

    void death(CombatParticipant p) {
        for (int i = 0; i < listeners.size(); i++) listeners.get(i).onParticipantDeath(p);
    }

    void stateChanged(GameStateSnapshot snapshot) {
        for (int i = 0; i < listeners.size(); i++) listeners.get(i).onGameStateChanged(snapshot);
    }

    /** Appends a line to the rolling buffer and fires onCombatLog. */
    void log(String line) {
        log.addLast(line);
        while (log.size() > LOG_CAPACITY) log.removeFirst();
        for (int i = 0; i < listeners.size(); i++) listeners.get(i).onCombatLog(line);
    }

    List<String> recentLog() {
        return new ArrayList<String>(log);
    }
}
