package com.whim.albion.api;

import com.whim.albion.api.Defs.SpellDef;
import com.whim.albion.api.Enums.DamageType;
import com.whim.albion.api.Enums.EnemyBehaviorType;
import com.whim.albion.api.Enums.StatType;

import java.util.List;

/**
 * Mutable combat participant. The model (Task 1) adapts party members and spawned
 * enemies to this interface; the combat engine (Task 2) drives combat purely
 * through it, and projects {@link Views.CombatantView} from it for the UI.
 */
public interface Combatant {

    String id();
    String name();
    boolean playerSide();
    String spriteKey();

    /** Tactical archetype; null for player-side combatants (driven by the player). */
    EnemyBehaviorType behavior();

    int stat(StatType type);
    /** Skill percentage 0..100 (melee/ranged/critical); 0 if not applicable. */
    int skill(Enums.SkillType type);

    int lp();
    int maxLp();
    int sp();
    int maxSp();
    boolean alive();

    /** Grid position on the battlefield. */
    GridPos pos();
    void setPos(GridPos pos);

    /** Base weapon/attack damage and its channel (before defender mitigation). */
    int attackPower();
    DamageType attackType();
    /** Total defense from armor/shield/stats. */
    int defense();
    /** True if this combatant fights at range (back-row viable). */
    boolean ranged();

    /** Apply damage after mitigation; returns LP actually lost. */
    int takeDamage(int amount);
    /** Restore LP (clamped to maxLp); returns LP actually gained. */
    int heal(int amount);
    /** Spend spell points; returns false (and spends nothing) if insufficient. */
    boolean spendSp(int amount);
    /** Restore SP (clamped). */
    void restoreSp(int amount);

    /** Spells this combatant may cast in battle (empty if none). */
    List<SpellDef> knownSpells();

    /** Transient defend flag (raises mitigation until this combatant's next turn). */
    boolean defending();
    void setDefending(boolean defending);
}
