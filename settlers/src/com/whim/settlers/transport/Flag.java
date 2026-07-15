package com.whim.settlers.transport;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A transport hub at a tile. Goods waiting to move on sit in {@link #queue}; a
 * carrier on an outgoing road picks the front one up when free. Flags are the
 * relay points of the transport network — a good is set down at each flag and
 * picked up by the next segment's carrier.
 */
public final class Flag {

    private final int id;
    private final int x, y;
    private final Deque<Shipment> queue = new ArrayDeque<Shipment>();

    public Flag(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public int id() { return id; }
    public int x()  { return x; }
    public int y()  { return y; }

    public void enqueue(Shipment s) { queue.addLast(s); }
    public Shipment peek()          { return queue.peekFirst(); }
    public Shipment poll()          { return queue.pollFirst(); }
    public boolean hasWaiting()     { return !queue.isEmpty(); }
    public int waiting()            { return queue.size(); }
}
