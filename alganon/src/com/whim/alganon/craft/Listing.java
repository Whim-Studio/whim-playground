package com.whim.alganon.craft;

/** One auction/requisition-house listing (NPC-seeded or player-posted). */
public final class Listing {
    public final String listingId;
    public final String itemId;
    public final int quantity;
    public final long price;
    public final boolean sellerIsPlayer;

    public Listing(String listingId, String itemId, int quantity, long price, boolean sellerIsPlayer) {
        this.listingId = listingId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.price = price;
        this.sellerIsPlayer = sellerIsPlayer;
    }
}
