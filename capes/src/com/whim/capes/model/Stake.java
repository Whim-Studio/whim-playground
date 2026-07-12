package com.whim.capes.model;

/**
 * Debt a character has Staked onto one side of a Conflict (pp.36-37).
 *
 * <p>Rules encoded by the owning {@link ConflictSide}:
 * <ul>
 *   <li>A character may Stake only ONE Drive per Conflict (p.36), so a Stake
 *       is keyed by (character, driveType).</li>
 *   <li>The staked amount may never exceed that Drive's Strength (p.36).</li>
 *   <li>Number of dice a side may Split into is capped by its total Stakes
 *       (p.37).</li>
 * </ul>
 * On Resolve, a losing Stake returns to its Drive doubled; a winning Stake is
 * given away as Story Tokens (p.30).
 */
public final class Stake implements java.io.Serializable {
    private final String characterId;
    private final DriveType driveType;
    private int amount;

    public Stake(String characterId, DriveType driveType, int amount) {
        this.characterId = characterId;
        this.driveType = driveType;
        this.amount = amount;
    }

    public String characterId() { return characterId; }
    public DriveType driveType() { return driveType; }
    public int amount() { return amount; }

    public void add(int n) { amount += n; }

    @Override public String toString() {
        return amount + " " + driveType.displayName() + " (" + characterId + ")";
    }
}
