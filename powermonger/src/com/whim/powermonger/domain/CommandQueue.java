package com.whim.powermonger.domain;

import java.util.ArrayDeque;
import java.util.Deque;

/** A per-captain FIFO of pending {@link Order}s. The engine drains it each tick. */
public final class CommandQueue {

    private final Deque<Order> orders = new ArrayDeque<Order>();

    public void enqueue(Order order) {
        if (order != null) orders.addLast(order);
    }

    /** Remove and return the next order, or null if empty. */
    public Order poll() {
        return orders.pollFirst();
    }

    /** Peek without removing, or null if empty. */
    public Order peek() {
        return orders.peekFirst();
    }

    public boolean isEmpty() { return orders.isEmpty(); }
    public int size() { return orders.size(); }
    public void clear() { orders.clear(); }
}
