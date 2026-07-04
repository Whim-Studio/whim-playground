package com.whim.swd6.api;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A character-creation template: an original archetype with pre-assigned attribute
 * dice codes, suggested starting skills, starting gear, and flavor. Templates are
 * original creations (no copyrighted WEG template text). Attribute codes must sum
 * to the standard 18D of attribute dice.
 *
 * Owned by the orchestrator (api). Supplied by the rules layer via
 * {@link ContentProvider#templates()}.
 */
public final class Template {

    private String name;
    private String description;
    private boolean forceSensitive;
    private final Map<Attribute, DiceCode> attributes = new EnumMap<Attribute, DiceCode>(Attribute.class);
    private final List<String> suggestedSkills = new ArrayList<String>(); // skill names for the 7D hint
    private final List<Equipment> startingGear = new ArrayList<Equipment>();
    private final List<Weapon> startingWeapons = new ArrayList<Weapon>();
    private int startingCredits;
    private int move = 10; // metres/round, human default

    public Template() {
        this.name = "";
        this.description = "";
        for (Attribute a : Attribute.values()) {
            attributes.put(a, DiceCode.parse("2D"));
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isForceSensitive() { return forceSensitive; }
    public void setForceSensitive(boolean forceSensitive) { this.forceSensitive = forceSensitive; }

    public Map<Attribute, DiceCode> getAttributes() { return attributes; }

    public List<String> getSuggestedSkills() { return suggestedSkills; }

    public List<Equipment> getStartingGear() { return startingGear; }

    public List<Weapon> getStartingWeapons() { return startingWeapons; }

    public int getStartingCredits() { return startingCredits; }
    public void setStartingCredits(int startingCredits) { this.startingCredits = startingCredits; }

    public int getMove() { return move; }
    public void setMove(int move) { this.move = move; }
}
