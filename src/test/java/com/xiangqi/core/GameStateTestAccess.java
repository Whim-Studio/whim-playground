package com.xiangqi.core;

/** Bridges tests to the package-private {@link GameState#fromBoard} factory. */
final class GameStateTestAccess {
    private GameStateTestAccess() {
    }

    static GameState of(Board board, Side sideToMove) {
        return GameState.fromBoard(board, sideToMove);
    }
}
