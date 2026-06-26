package com.whim.monopoly.domain;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Set;

public class DefaultPlayer implements Player {
    private final int id;
    private final String name;
    private final Color token;
    private int cash = 1500;
    private int position = 0;
    private boolean inJail = false;
    private int jailTurns = 0;
    private int jailCards = 0;
    private boolean bankrupt = false;
    private final Set<Integer> deeds = new LinkedHashSet<Integer>();

    public DefaultPlayer(int id, String name, Color token) {
        this.id = id;
        this.name = name;
        this.token = token;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Color getToken() { return token; }
    public int getCash() { return cash; }
    public void setCash(int cash) { this.cash = cash; }
    public void addCash(int delta) { this.cash += delta; }
    public int getPosition() { return position; }
    public void setPosition(int index) { this.position = index; }
    public boolean isInJail() { return inJail; }
    public void setInJail(boolean jailed) { this.inJail = jailed; }
    public int getJailTurns() { return jailTurns; }
    public void setJailTurns(int n) { this.jailTurns = n; }
    public int getJailCards() { return jailCards; }
    public void setJailCards(int n) { this.jailCards = n; }
    public boolean isBankrupt() { return bankrupt; }
    public void setBankrupt(boolean b) { this.bankrupt = b; }
    public Set<Integer> getDeeds() { return deeds; }
}
