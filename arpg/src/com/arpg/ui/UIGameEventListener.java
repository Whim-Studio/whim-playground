package com.arpg.ui;

import javax.swing.SwingUtilities;

import com.arpg.model.Ability;
import com.arpg.model.BuffDebuff;
import com.arpg.model.Character;
import com.arpg.model.CombatParticipant;
import com.arpg.model.GameEventListener;
import com.arpg.model.GameStateSnapshot;
import com.arpg.model.Item;

/**
 * The UI's {@link GameEventListener}. It translates engine events into
 * color-coded combat-log lines and refreshes every panel from the authoritative
 * snapshot. Engine events may arrive on any thread, so EVERY method marshals its
 * work onto the EDT via {@link SwingUtilities#invokeLater}.
 */
public class UIGameEventListener implements GameEventListener {

    private final CombatLogPanel log;
    private final SnapshotConsumer consumer;

    public UIGameEventListener(CombatLogPanel log, SnapshotConsumer consumer) {
        this.log = log;
        this.consumer = consumer;
    }

    @Override
    public void onDamageDealt(final CombatParticipant src, final CombatParticipant tgt,
            final int amt, final Ability ability) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String via = ability == null ? "" : " with " + ability.getName();
                log.logDamage(name(src) + " hits " + name(tgt) + via + " for " + amt + " damage.");
            }
        });
    }

    @Override
    public void onHealed(final CombatParticipant src, final CombatParticipant tgt, final int amt) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                log.logHeal(name(src) + " heals " + name(tgt) + " for " + amt + ".");
            }
        });
    }

    @Override
    public void onBuffApplied(final CombatParticipant tgt, final BuffDebuff buff) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                log.logBuff(name(tgt) + " gains " + buffName(buff)
                        + " (" + (buff == null ? 0 : buff.getDurationTicks()) + " ticks).");
            }
        });
    }

    @Override
    public void onBuffExpired(final CombatParticipant tgt, final BuffDebuff buff) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                log.logDebuff(buffName(buff) + " fades from " + name(tgt) + ".");
            }
        });
    }

    @Override
    public void onLevelUp(final Character character, final int newLevel) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                log.logLoot(name(character) + " reached level " + newLevel + "!");
            }
        });
    }

    @Override
    public void onLootDropped(final Item item) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                log.logLoot("Loot: " + (item == null ? "something" : item.getName()) + "!");
            }
        });
    }

    @Override
    public void onCombatLog(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                log.log(message);
            }
        });
    }

    @Override
    public void onParticipantDeath(final CombatParticipant participant) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                log.logDeath(name(participant) + " has been defeated!");
            }
        });
    }

    @Override
    public void onGameStateChanged(final GameStateSnapshot snapshot) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                consumer.refresh(snapshot);
            }
        });
    }

    private static String name(CombatParticipant p) {
        return p == null ? "Someone" : p.getName();
    }

    private static String buffName(BuffDebuff b) {
        return b == null ? "an effect" : b.getName();
    }
}
