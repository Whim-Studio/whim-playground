package com.whim.nextrun.domain;

/**
 * The eight starting hero classes of Next Run. Each carries distinct base
 * stats and a passive that reshapes the action economy (turn costs) or one of
 * the three paths of resolution (combat / economy / stealth-magic).
 *
 * Stats are intentionally low integers; the engine derives turn costs and
 * resolution odds from them, so small differences matter a great deal.
 */
public enum HeroClass {

    //          label          hp  atk def dex mag gold passive
    WARRIOR    ("Warrior",     30,  8,  5,  3,  0,   5, "Bloodlust: +2 damage in combat."),
    MAGE       ("Mage",        18,  3,  2,  4,  8,   5, "Arcana: sneaking past enemies is far easier."),
    LORD       ("Lord",        24,  5,  4,  3,  2,  20, "Coffers: starts wealthy; bribes cost less gold."),
    ARTISAN    ("Artisan",     22,  4,  3,  6,  1,   8, "Master Hand: crafting and building cost fewer turns."),
    LEADER     ("Leader",      26,  5,  4,  4,  1,  12, "Rally: structures finish faster and grant more."),
    NECROMANCER("Necromancer", 20,  6,  2,  3,  6,   5, "Dread: enemies are weakened; combat hits harder."),
    PEASANT    ("Peasant",     20,  3,  3,  5,  0,   3, "Scavenger: gathers extra materials each harvest."),
    DRUID      ("Druid",       24,  4,  4,  4,  5,   6, "Wild Step: moving and gathering cost fewer turns.");

    private final String label;
    private final int hp;
    private final int attack;
    private final int defense;
    private final int dexterity;
    private final int magic;
    private final int gold;
    private final String passive;

    HeroClass(String label, int hp, int attack, int defense, int dexterity,
              int magic, int gold, String passive) {
        this.label = label;
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.dexterity = dexterity;
        this.magic = magic;
        this.gold = gold;
        this.passive = passive;
    }

    public String label() { return label; }
    public int baseHp() { return hp; }
    public int baseAttack() { return attack; }
    public int baseDefense() { return defense; }
    public int baseDexterity() { return dexterity; }
    public int baseMagic() { return magic; }
    public int baseGold() { return gold; }
    public String passive() { return passive; }
}
