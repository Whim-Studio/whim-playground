package com.whim.capes.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One side of a {@link Conflict} (p.26). A side owns one or more dice (it
 * begins with a single die at value 1), an optional "Resolution" statement
 * describing what this side wants, the set of Stakes backing it, and the set of
 * players who have Claimed it this Page.
 *
 * <p>Control is decided by comparing {@link #total()} across sides (p.26). The
 * number of dice may not exceed the number of Stakes when Splitting (p.37);
 * that check lives in the engine, but {@link #stakeCount()} exposes the cap.
 */
public final class ConflictSide implements java.io.Serializable {
    private String resolutionStatement;
    private final List<Die> dice = new ArrayList<Die>();
    private final List<Stake> stakes = new ArrayList<Stake>();
    private final List<String> alliedCharacterIds = new ArrayList<String>();
    private final List<String> claimingPlayerIds = new ArrayList<String>();

    public ConflictSide(String resolutionStatement) {
        this.resolutionStatement = resolutionStatement;
        this.dice.add(new Die(1)); // each side starts with one die valued at 1 (p.26)
    }

    public String resolutionStatement() { return resolutionStatement; }
    public void setResolutionStatement(String s) { this.resolutionStatement = s; }

    public List<Die> dice() { return dice; }
    public List<Stake> stakes() { return stakes; }
    public List<String> alliedCharacterIds() { return alliedCharacterIds; }
    public List<String> claimingPlayerIds() { return claimingPlayerIds; }

    /** Sum of all die faces on this side; the higher total Controls (p.26). */
    public int total() {
        int t = 0;
        for (Die d : dice) t += d.value();
        return t;
    }

    /** Total Debt Staked on this side (across all characters/drives). */
    public int stakedDebt() {
        int t = 0;
        for (Stake s : stakes) t += s.amount();
        return t;
    }

    /**
     * Max dice this side may Split into = the number of Debt <em>points</em>
     * Staked on it (p.37; the Maximus example splits into two dice off two
     * Staked tokens). Equal to {@link #stakedDebt()}.
     */
    public int stakeCount() { return stakedDebt(); }

    public void ally(String characterId) {
        if (!alliedCharacterIds.contains(characterId)) alliedCharacterIds.add(characterId);
    }

    public void claim(String playerId) {
        if (!claimingPlayerIds.contains(playerId)) claimingPlayerIds.add(playerId);
    }

    /** Finds an existing Stake for (character, drive) so a character can add to it across Actions. */
    public Stake findStake(String characterId, DriveType drive) {
        for (Stake s : stakes) {
            if (s.characterId().equals(characterId) && s.driveType() == drive) return s;
        }
        return null;
    }

    /** True if this character already Staked a (different) Drive here — only one Drive per Conflict (p.36). */
    public boolean hasStakeFromOtherDrive(String characterId, DriveType drive) {
        for (Stake s : stakes) {
            if (s.characterId().equals(characterId) && s.driveType() != drive) return true;
        }
        return false;
    }
}
