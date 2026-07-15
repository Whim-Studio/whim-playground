package com.whim.settlers.transport;

/** Predicate over tile coordinates; used to describe walkable terrain for roads. */
public interface TilePredicate {
    boolean test(int x, int y);
}
