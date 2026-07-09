package com.whim.samurai.model;

import java.io.Serializable;

/**
 * A rival samurai. Rivals inside your own clan compete for the daimyo's favour
 * (you must out-honor and out-power them, or duel them); rivals in enemy clans
 * lead the garrisons you besiege. Rivals can insult you, ambush you, or kidnap
 * your family — triggering duels and rescue quests (design ref §2a, §5, §6).
 */
public class Rival implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name;
    public int clanId;
    public boolean alive = true;

    public int swordsmanship = 8;
    public int honor = 100;
    public int power = 60;

    /** -100 (mortal enemy) .. +100 (friendly). Insults drive this negative. */
    public int hostility = 0;

    public Rival() { }
    public Rival(String name, int clanId) { this.name = name; this.clanId = clanId; }
}
