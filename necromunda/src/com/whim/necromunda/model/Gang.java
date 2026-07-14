package com.whim.necromunda.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A gang: a House, a name, a roster of fighters, a credit balance, and a list of
 * controlled territories (campaign). {@link #rating()} is the summed power score
 * used for matchmaking / underdog bonuses.
 */
public final class Gang {

    private String name;
    private House house;
    private final List<Fighter> roster = new ArrayList<Fighter>();
    private int credits;
    private final List<String> territories = new ArrayList<String>();

    public Gang(String name, House house) {
        this.name = name;
        this.house = house;
    }

    public String name() { return name; }
    public void setName(String name) { this.name = name; }

    public House house() { return house; }
    public void setHouse(House house) { this.house = house; }

    public List<Fighter> roster() { return roster; }
    public void add(Fighter f) { roster.add(f); }
    public void remove(Fighter f) { roster.remove(f); }

    public int credits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public List<String> territories() { return territories; }

    /** Gang rating = Σ fighter rating values (base + gear + xp). */
    public int rating() {
        int total = 0;
        for (Fighter f : roster) {
            total += f.ratingValue();
        }
        return total;
    }

    /** Fighters still in play (not out of action / fled). */
    public List<Fighter> inPlay() {
        List<Fighter> out = new ArrayList<Fighter>();
        for (Fighter f : roster) {
            if (f.status().inPlay()) {
                out.add(f);
            }
        }
        return out;
    }

    /** Count of fighters currently Down or Out of Action (for the bottle test). */
    public int downOrOutCount() {
        int n = 0;
        for (Fighter f : roster) {
            if (f.status() == FighterStatus.DOWN
                    || f.status() == FighterStatus.OUT_OF_ACTION) {
                n++;
            }
        }
        return n;
    }

    /** Reset every fighter to full battle-readiness (deployment). */
    public void resetForBattle() {
        for (Fighter f : roster) {
            f.resetForBattle();
        }
    }

    @Override
    public String toString() {
        return name + " [" + house.label() + "]";
    }
}
