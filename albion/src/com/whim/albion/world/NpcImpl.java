package com.whim.albion.world;

import com.whim.albion.api.Views.NpcView;

/** A stationary map character the player can Talk to. */
public final class NpcImpl implements NpcView {

    private final int x;
    private final int y;
    private final String name;
    private final String spriteKey;
    private final boolean hostile;

    public NpcImpl(int x, int y, String name, String spriteKey, boolean hostile) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.spriteKey = spriteKey;
        this.hostile = hostile;
    }

    @Override public int x() { return x; }
    @Override public int y() { return y; }
    @Override public String name() { return name; }
    @Override public String spriteKey() { return spriteKey; }
    @Override public boolean hostile() { return hostile; }
}
