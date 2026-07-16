package com.whim.tippingpoint.domain;

public abstract class Card {
    protected final String id;
    protected final String name;
    protected final String description;
    protected Card(String id, String name, String description) { this.id=id; this.name=name; this.description=description; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
