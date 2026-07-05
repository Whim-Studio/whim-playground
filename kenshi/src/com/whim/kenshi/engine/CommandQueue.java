package com.whim.kenshi.engine;

import com.whim.kenshi.domain.WorldState;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe queue of player commands. UI/EDT threads {@link #submit} commands;
 * the tick thread {@link #drainInto drains} and applies them against the live
 * {@link WorldState} at the top of each tick, so the world is only ever mutated
 * on the tick thread.
 */
final class CommandQueue {

    /** A single deferred mutation applied on the tick thread. */
    interface Command {
        void apply(WorldState world);
    }

    private final ConcurrentLinkedQueue<Command> queue = new ConcurrentLinkedQueue<Command>();

    void submit(Command cmd) {
        if (cmd != null) {
            queue.add(cmd);
        }
    }

    /** Apply and remove every queued command, in submission order. */
    void drainInto(WorldState world) {
        Command cmd;
        while ((cmd = queue.poll()) != null) {
            cmd.apply(world);
        }
    }

    void clear() {
        queue.clear();
    }
}
