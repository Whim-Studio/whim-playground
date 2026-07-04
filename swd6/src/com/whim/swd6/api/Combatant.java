package com.whim.swd6.api;

/**
 * A participant in the combat tracker. Wraps either a full {@link PlayerCharacter}
 * (for PCs) or a lightweight stat line (for NPCs/adversaries). The engine's combat
 * tracker orders these by {@link #getInitiative()} and mutates {@link #woundLevel}
 * and the action count each round.
 *
 * Owned by the orchestrator (api). Mutable per-encounter state.
 */
public final class Combatant {

    private String name;
    private boolean playerCharacter;
    private PlayerCharacter pc;      // non-null for PCs; may be null for NPCs

    // NPC quick stats (used when pc == null)
    private DiceCode attackCode = DiceCode.parse("3D");
    private DiceCode damageCode = DiceCode.parse("4D");
    private DiceCode resistCode = DiceCode.parse("2D");

    private int initiative;          // rolled each encounter
    private int declaredActions = 1; // actions this round (drives multi-action penalty)
    private WoundLevel woundLevel = WoundLevel.HEALTHY;

    public Combatant() {
        this.name = "";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? "" : name; }

    public boolean isPlayerCharacter() { return playerCharacter; }
    public void setPlayerCharacter(boolean playerCharacter) { this.playerCharacter = playerCharacter; }

    public PlayerCharacter getPc() { return pc; }
    public void setPc(PlayerCharacter pc) { this.pc = pc; }

    public DiceCode getAttackCode() { return attackCode; }
    public void setAttackCode(DiceCode attackCode) { this.attackCode = attackCode == null ? DiceCode.ZERO : attackCode; }

    public DiceCode getDamageCode() { return damageCode; }
    public void setDamageCode(DiceCode damageCode) { this.damageCode = damageCode == null ? DiceCode.ZERO : damageCode; }

    public DiceCode getResistCode() { return resistCode; }
    public void setResistCode(DiceCode resistCode) { this.resistCode = resistCode == null ? DiceCode.ZERO : resistCode; }

    public int getInitiative() { return initiative; }
    public void setInitiative(int initiative) { this.initiative = initiative; }

    public int getDeclaredActions() { return declaredActions; }
    public void setDeclaredActions(int declaredActions) { this.declaredActions = Math.max(1, declaredActions); }

    public WoundLevel getWoundLevel() { return woundLevel; }
    public void setWoundLevel(WoundLevel woundLevel) { this.woundLevel = woundLevel == null ? WoundLevel.HEALTHY : woundLevel; }
}
