package com.whim.starcraft8.engine;

/**
 * Marker interface for player/AI orders. The UI never implements this directly; it
 * constructs commands only through the {@link Commands} static factory so it never
 * depends on the concrete subclasses. The engine dispatches them internally.
 */
public interface Command {
}
