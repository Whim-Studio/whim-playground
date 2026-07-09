package com.whim.samurai.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The player character (or an heir who continues the dynasty). Tracks the two
 * central currencies of the original — HONOR (bushido/virtue) and POWER (land,
 * armies, wealth/koku) — plus the arcade skills used in the action sequences.
 * See GAME_DESIGN_REFERENCE.md §3 and §4.
 */
public class Samurai implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name;
    public String clanName;
    public int age = 20;
    public boolean alive = true;

    public Rank rank = Rank.SAMURAI;

    // Dual-axis reputation. Range roughly 0..1000 each; label bands in HonorEngine.
    public int honor = 100;
    public int power = 50;

    // Arcade / action skills, 1..20. Improve through training and by winning.
    public int swordsmanship = 8;   // sword duels
    public int generalship   = 8;   // open-field battle
    public int stealth       = 6;   // ninja / infiltration

    // Economy
    public int koku = 200;          // rice-wealth, the era's currency

    // Land held directly by this samurai (province ids).
    public final List<Integer> fiefs = new ArrayList<Integer>();

    // Household
    public FamilyMember wife;
    public final List<FamilyMember> children = new ArrayList<FamilyMember>();

    public Samurai() { }

    public boolean isMarried() { return wife != null && wife.alive; }

    public FamilyMember heir() {
        FamilyMember best = null;
        for (FamilyMember c : children) {
            if (c.isHeirEligible() && (best == null || c.age > best.age)) best = c;
        }
        return best;
    }

    public boolean hasHeir() { return heir() != null; }
}
