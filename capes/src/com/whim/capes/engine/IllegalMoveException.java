package com.whim.capes.engine;

/**
 * Thrown by the engine when a requested move violates the Capes rules
 * (e.g. Staking a second Drive on one Conflict, Splitting past the Stake count,
 * using a blocked Ability). The UI catches these to keep play legal-moves-only
 * while still surfacing a clear reason.
 */
public class IllegalMoveException extends RuntimeException {
    public IllegalMoveException(String message) { super(message); }
}
