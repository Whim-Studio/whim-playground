package com.whim.tacticalnexus.domain;

/** A blocking enemy. Combat math lives in the engine (Task 2). */
public final class Enemy implements Entity {
    private final String name;
    private final int hp;
    private final int atk;
    private final int def;
    private final int goldReward;
    private final int expReward;
    private final java.awt.Color color;

    public Enemy(String name, int hp, int atk, int def, int goldReward,
                 int expReward, java.awt.Color color) {
        this.name = name;
        this.hp = hp;
        this.atk = atk;
        this.def = def;
        this.goldReward = goldReward;
        this.expReward = expReward;
        this.color = color;
    }

    public String name() {
        return name;
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

    public int goldReward() {
        return goldReward;
    }

    public int expReward() {
        return expReward;
    }

    public java.awt.Color color() {
        return color;
    }

    @Override
    public EntityType type() {
        return EntityType.ENEMY;
    }

    @Override
    public boolean blocksMovement() {
        return true;
    }

    @Override
    public char glyph() {
        return 'E';
    }
}
