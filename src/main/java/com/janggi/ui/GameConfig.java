package com.janggi.ui;

import com.janggi.core.Side;

/**
 * Immutable choices made on the mode-selection screen, carried forward into the
 * setup screen and then the game itself.
 */
public final class GameConfig {

    private final GameMode mode;
    /** The side the human controls in {@link GameMode#VS_COMPUTER}; null for local 2-player. */
    private final Side humanSide;
    private final int aiDepth;

    public GameConfig(GameMode mode, Side humanSide, int aiDepth) {
        this.mode = mode;
        this.humanSide = humanSide;
        this.aiDepth = aiDepth;
    }

    public GameMode mode() {
        return mode;
    }

    public Side humanSide() {
        return humanSide;
    }

    public int aiDepth() {
        return aiDepth;
    }

    /** True when the given side is controlled by the computer in this configuration. */
    public boolean isComputer(Side side) {
        return mode == GameMode.VS_COMPUTER && side != humanSide;
    }

    /** True when the given side is controlled by a human in this configuration. */
    public boolean isHuman(Side side) {
        return !isComputer(side);
    }
}
