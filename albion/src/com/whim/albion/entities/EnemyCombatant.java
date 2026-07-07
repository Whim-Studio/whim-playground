package com.whim.albion.entities;

import com.whim.albion.api.Combatant;
import com.whim.albion.api.Defs.MonsterDef;
import com.whim.albion.api.Defs.SpellDef;
import com.whim.albion.api.Enums.DamageType;
import com.whim.albion.api.Enums.EnemyBehaviorType;
import com.whim.albion.api.Enums.SkillType;
import com.whim.albion.api.Enums.StatType;
import com.whim.albion.api.GridPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A spawned enemy adapting a {@link MonsterDef} to {@link Combatant}. Its
 * {@link #id()} is {@code "<monsterId>#<index>"} so the engine can recover the
 * source {@link MonsterDef} (for xp/gold rewards) via {@code content.monster(...)}
 * after stripping the {@code #index} suffix.
 */
public final class EnemyCombatant implements Combatant {

    private static final int DEFEND_BONUS = 3;

    private final String id;
    private final MonsterDef def;
    private final List<SpellDef> spells;

    private int lp;
    private int sp;
    private GridPos pos = new GridPos(0, 0);
    private boolean defending;

    public EnemyCombatant(MonsterDef def, int index, List<SpellDef> spells) {
        this.def = def;
        this.id = def.id + "#" + index;
        this.lp = def.maxLp;
        this.sp = def.maxSp;
        this.spells = spells == null ? Collections.<SpellDef>emptyList() : new ArrayList<SpellDef>(spells);
    }

    /** The monster id without the spawn-index suffix. */
    public String monsterId() { return def.id; }
    public MonsterDef def() { return def; }

    @Override public String id() { return id; }
    @Override public String name() { return def.name; }
    @Override public boolean playerSide() { return false; }
    @Override public String spriteKey() { return def.spriteKey; }
    @Override public EnemyBehaviorType behavior() { return def.behavior; }

    @Override public int stat(StatType type) {
        Integer v = def.stats.get(type);
        return v == null ? 6 : v;
    }

    @Override public int skill(SkillType type) {
        switch (type) {
            case MELEE:    return def.behavior == EnemyBehaviorType.AGGRESSIVE ? 50 : 30;
            case RANGED:   return def.behavior == EnemyBehaviorType.RANGED ? 55 : 10;
            case CRITICAL: return 8;
            default:       return 0;
        }
    }

    @Override public int lp() { return lp; }
    @Override public int maxLp() { return def.maxLp; }
    @Override public int sp() { return sp; }
    @Override public int maxSp() { return def.maxSp; }
    @Override public boolean alive() { return lp > 0; }

    @Override public GridPos pos() { return pos; }
    @Override public void setPos(GridPos pos) { this.pos = pos; }

    @Override public int attackPower() { return def.attack; }
    @Override public DamageType attackType() { return def.damageType; }
    @Override public int defense() { return def.defense; }
    @Override public boolean ranged() { return def.behavior == EnemyBehaviorType.RANGED; }

    @Override public int takeDamage(int amount) {
        int mitigation = def.defense + (defending ? DEFEND_BONUS : 0);
        int dealt = Math.max(1, amount - mitigation);
        int before = lp;
        lp = Math.max(0, lp - dealt);
        return before - lp;
    }

    @Override public int heal(int amount) {
        int gain = Math.min(Math.max(0, amount), def.maxLp - lp);
        lp += gain;
        return gain;
    }

    @Override public boolean spendSp(int amount) {
        if (amount <= 0) return true;
        if (sp < amount) return false;
        sp -= amount;
        return true;
    }

    @Override public void restoreSp(int amount) { sp = Math.min(def.maxSp, sp + Math.max(0, amount)); }

    @Override public List<SpellDef> knownSpells() { return Collections.unmodifiableList(spells); }

    @Override public boolean defending() { return defending; }
    @Override public void setDefending(boolean defending) { this.defending = defending; }
}
