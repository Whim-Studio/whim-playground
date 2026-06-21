package com.xiangqi.ui;

/** How the two sides are controlled. Chosen once at startup. */
enum GameMode {
    /** Both RED and BLACK are played by humans sharing the window. */
    TWO_PLAYER,
    /** One human side; the other is driven by {@code com.xiangqi.ai.MinimaxAI}. */
    VS_COMPUTER
}
