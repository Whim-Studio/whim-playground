package com.whim.tacticalnexus.domain;

/**
 * Immutable player: stats, key inventory, score, and grid position. Every
 * mutator returns a new instance so {@link com.whim.tacticalnexus.state.GameState}
 * snapshots can share structure.
 */
public final class Player {
    private final int hp;
    private final int atk;
    private final int def;
    private final int yellowKeys;
    private final int blueKeys;
    private final int redKeys;
    private final int gold;
    private final int exp;
    private final int level;
    private final Position position;

    /** Full constructor; {@code level} is derived from {@code exp}. */
    public Player(int hp, int atk, int def, int yellowKeys, int blueKeys,
                  int redKeys, int gold, int exp, Position position) {
        this.hp = hp;
        this.atk = atk;
        this.def = def;
        this.yellowKeys = yellowKeys;
        this.blueKeys = blueKeys;
        this.redKeys = redKeys;
        this.gold = gold;
        this.exp = exp;
        this.level = computeLevel(exp);
        this.position = position;
    }

    /** Convenience starting state: HP 1000 / ATK 10 / DEF 10, no keys, no score. */
    public static Player starting(Position position) {
        return new Player(1000, 10, 10, 0, 0, 0, 0, 0, position);
    }

    private static int computeLevel(int exp) {
        return 1 + exp / 10;
    }

    public int hp() {
        return hp;
    }

    public int atk() {
        return atk;
    }

    public int def() {
        return def;
    }

    public int yellowKeys() {
        return yellowKeys;
    }

    public int blueKeys() {
        return blueKeys;
    }

    public int redKeys() {
        return redKeys;
    }

    public int gold() {
        return gold;
    }

    public int exp() {
        return exp;
    }

    public int level() {
        return level;
    }

    public Position position() {
        return position;
    }

    public int keyCount(KeyColor c) {
        switch (c) {
            case YELLOW:
                return yellowKeys;
            case BLUE:
                return blueKeys;
            case RED:
                return redKeys;
            default:
                return 0;
        }
    }

    public Player withPosition(Position p) {
        return new Player(hp, atk, def, yellowKeys, blueKeys, redKeys, gold, exp, p);
    }

    public Player withHp(int newHp) {
        return new Player(newHp, atk, def, yellowKeys, blueKeys, redKeys, gold, exp, position);
    }

    public Player addStats(int dHp, int dAtk, int dDef) {
        return new Player(hp + dHp, atk + dAtk, def + dDef, yellowKeys, blueKeys,
                redKeys, gold, exp, position);
    }

    /** Adds {@code n} keys of color {@code c} ({@code n} may be negative when spending). */
    public Player addKey(KeyColor c, int n) {
        int y = yellowKeys;
        int b = blueKeys;
        int r = redKeys;
        switch (c) {
            case YELLOW:
                y += n;
                break;
            case BLUE:
                b += n;
                break;
            case RED:
                r += n;
                break;
            default:
                break;
        }
        return new Player(hp, atk, def, y, b, r, gold, exp, position);
    }

    public Player addGold(int n) {
        return new Player(hp, atk, def, yellowKeys, blueKeys, redKeys, gold + n, exp, position);
    }

    /** Adds EXP and recomputes level deterministically (level = 1 + exp/10). */
    public Player addExp(int n) {
        return new Player(hp, atk, def, yellowKeys, blueKeys, redKeys, gold, exp + n, position);
    }
}
