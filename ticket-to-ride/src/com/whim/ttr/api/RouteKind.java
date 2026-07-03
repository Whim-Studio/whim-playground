package com.whim.ttr.api;

/**
 * Europe-specific route flavors.
 *
 * <ul>
 *   <li>{@link #NORMAL} — an ordinary land route.</li>
 *   <li>{@link #FERRY} — requires the indicated number of LOCOMOTIVE cards as
 *       part of the payment (see {@code Route.locomotivesRequired}).</li>
 *   <li>{@link #TUNNEL} — after payment the engine flips the top 3 deck cards;
 *       each flipped card that matches the paid color (or is a LOCOMOTIVE)
 *       demands one additional matching card, or the claim is cancelled.</li>
 * </ul>
 */
public enum RouteKind {
    NORMAL, FERRY, TUNNEL
}
