package com.whim.swd6.api;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A complete player character: the central mutable model object shared by the
 * creation wizard, the sheet, the engine, and persistence. Named PlayerCharacter
 * (not "Character") to avoid clashing with java.lang.Character.
 *
 * Design notes:
 *  - Attribute values are dice codes keyed by {@link Attribute}.
 *  - {@link Skill#getAdded()} stores only dice added over the attribute; call
 *    {@link #skillCode(Skill)} for the effective total.
 *  - Force skills are stored only when the character is Force-Sensitive.
 *  - Wound state escalates via {@link WoundLevel#escalate}.
 *
 * Owned by the orchestrator (api). Plain mutable data holder; all rules logic lives
 * in the engine and model layers.
 */
public final class PlayerCharacter {

    private String name = "";
    private String templateName = "";       // "" when built via point-buy
    private String species = "Human";
    private String background = "";
    private String motivation = "";
    private String destiny = "";

    private final Map<Attribute, DiceCode> attributes = new EnumMap<Attribute, DiceCode>(Attribute.class);
    private final List<Skill> skills = new ArrayList<Skill>();

    private boolean forceSensitive;
    private final Map<ForceSkill, DiceCode> forceSkills = new EnumMap<ForceSkill, DiceCode>(ForceSkill.class);

    private int forcePoints = 1;
    private int characterPoints = 5;
    private int darkSidePoints = 0;

    private int move = 10;                   // metres/round
    private int credits = 0;

    private final List<Weapon> weapons = new ArrayList<Weapon>();
    private final List<Armor> armor = new ArrayList<Armor>();
    private final List<Equipment> gear = new ArrayList<Equipment>();

    private WoundLevel woundLevel = WoundLevel.HEALTHY;

    public PlayerCharacter() {
        for (Attribute a : Attribute.values()) {
            attributes.put(a, DiceCode.parse("2D"));
        }
    }

    // ----- identity -----
    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? "" : name; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName == null ? "" : templateName; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species == null ? "" : species; }

    public String getBackground() { return background; }
    public void setBackground(String background) { this.background = background == null ? "" : background; }

    public String getMotivation() { return motivation; }
    public void setMotivation(String motivation) { this.motivation = motivation == null ? "" : motivation; }

    public String getDestiny() { return destiny; }
    public void setDestiny(String destiny) { this.destiny = destiny == null ? "" : destiny; }

    // ----- attributes & skills -----
    public Map<Attribute, DiceCode> getAttributes() { return attributes; }

    public DiceCode getAttribute(Attribute a) {
        DiceCode d = attributes.get(a);
        return d == null ? DiceCode.ZERO : d;
    }

    public void setAttribute(Attribute a, DiceCode code) {
        attributes.put(a, code == null ? DiceCode.ZERO : code);
    }

    public List<Skill> getSkills() { return skills; }

    /** Effective skill code = governing attribute + dice added to the skill. */
    public DiceCode skillCode(Skill s) {
        return getAttribute(s.getAttribute()).add(s.getAdded());
    }

    // ----- Force -----
    public boolean isForceSensitive() { return forceSensitive; }
    public void setForceSensitive(boolean forceSensitive) { this.forceSensitive = forceSensitive; }

    public Map<ForceSkill, DiceCode> getForceSkills() { return forceSkills; }

    public DiceCode getForceSkill(ForceSkill fs) {
        DiceCode d = forceSkills.get(fs);
        return d == null ? DiceCode.ZERO : d;
    }

    public void setForceSkill(ForceSkill fs, DiceCode code) {
        forceSkills.put(fs, code == null ? DiceCode.ZERO : code);
    }

    // ----- point economies -----
    public int getForcePoints() { return forcePoints; }
    public void setForcePoints(int forcePoints) { this.forcePoints = forcePoints; }

    public int getCharacterPoints() { return characterPoints; }
    public void setCharacterPoints(int characterPoints) { this.characterPoints = characterPoints; }

    public int getDarkSidePoints() { return darkSidePoints; }
    public void setDarkSidePoints(int darkSidePoints) { this.darkSidePoints = darkSidePoints; }

    // ----- physical -----
    public int getMove() { return move; }
    public void setMove(int move) { this.move = move; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public List<Weapon> getWeapons() { return weapons; }
    public List<Armor> getArmor() { return armor; }
    public List<Equipment> getGear() { return gear; }

    public WoundLevel getWoundLevel() { return woundLevel; }
    public void setWoundLevel(WoundLevel woundLevel) { this.woundLevel = woundLevel == null ? WoundLevel.HEALTHY : woundLevel; }
}
