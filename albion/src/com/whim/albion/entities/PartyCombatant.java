package com.whim.albion.entities;

import com.whim.albion.api.Combatant;
import com.whim.albion.api.Defs.SpellDef;
import com.whim.albion.api.Enums.DamageType;
import com.whim.albion.api.Enums.EnemyBehaviorType;
import com.whim.albion.api.Enums.SkillType;
import com.whim.albion.api.Enums.StatType;
import com.whim.albion.api.GridPos;

import java.util.List;

/**
 * Adapts a {@link Character} to the {@link Combatant} seam the combat engine
 * drives. All mutable pools delegate straight to the character so that damage
 * and SP spent in battle persist afterwards. Only the transient battlefield
 * position and the defend flag live here.
 */
public final class PartyCombatant implements Combatant {

    /** Extra mitigation granted while defending. */
    private static final int DEFEND_BONUS = 3;

    private final String id;
    private final Character character;
    private GridPos pos = new GridPos(0, 0);
    private boolean defending;

    public PartyCombatant(String id, Character character) {
        this.id = id;
        this.character = character;
    }

    public Character character() { return character; }

    @Override public String id() { return id; }
    @Override public String name() { return character.name(); }
    @Override public boolean playerSide() { return true; }
    @Override public String spriteKey() { return character.portraitKey(); }
    @Override public EnemyBehaviorType behavior() { return null; }

    @Override public int stat(StatType type) { return character.stat(type); }
    @Override public int skill(SkillType type) { return character.skill(type); }

    @Override public int lp() { return character.lp(); }
    @Override public int maxLp() { return character.maxLp(); }
    @Override public int sp() { return character.sp(); }
    @Override public int maxSp() { return character.maxSp(); }
    @Override public boolean alive() { return character.alive(); }

    @Override public GridPos pos() { return pos; }
    @Override public void setPos(GridPos pos) { this.pos = pos; }

    @Override public int attackPower() { return character.attackPower(); }
    @Override public DamageType attackType() { return character.attackType(); }
    @Override public int defense() { return character.defense(); }
    @Override public boolean ranged() { return character.ranged(); }

    @Override public int takeDamage(int amount) {
        int mitigation = defense() + (defending ? DEFEND_BONUS : 0);
        int dealt = Math.max(1, amount - mitigation);
        int before = character.lp();
        character.damage(dealt);
        return before - character.lp();
    }

    @Override public int heal(int amount) { return character.healLp(amount); }
    @Override public boolean spendSp(int amount) { return character.spendSp(amount); }
    @Override public void restoreSp(int amount) { character.restoreSp(amount); }

    @Override public List<SpellDef> knownSpells() { return character.spellBook().knownDefs(); }

    @Override public boolean defending() { return defending; }
    @Override public void setDefending(boolean defending) { this.defending = defending; }
}
