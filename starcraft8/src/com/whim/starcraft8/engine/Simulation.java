package com.whim.starcraft8.engine;

import java.util.function.Consumer;

/**
 * The engine entry point the UI drives. The simulation runs at 60 ticks/sec on a
 * background thread it owns. UI input becomes {@link Command} objects pushed via
 * {@link #enqueue}; the engine drains the queue at the start of each tick. All reading
 * of world state by the UI happens inside {@link #readState}, which runs the supplied
 * consumer while holding the engine lock.
 */
public interface Simulation {
    void start();
    void stop();
    void enqueue(Command c);
    void readState(Consumer<WorldReader> reader);
    boolean isRunning();
    int humanPlayerId();
}
