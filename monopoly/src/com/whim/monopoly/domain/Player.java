package com.whim.monopoly.domain;

public interface Player {
    int getId();
    String getName();
    java.awt.Color getToken();         // distinct token color
    int getCash();
    void setCash(int cash);
    void addCash(int delta);           // may go negative transiently during debt resolution
    int getPosition();                 // 0..39
    void setPosition(int index);
    boolean isInJail();
    void setInJail(boolean jailed);
    int getJailTurns();                // failed doubles attempts so far (0..3)
    void setJailTurns(int n);
    int getJailCards();                // Get-Out-of-Jail-Free cards held
    void setJailCards(int n);
    boolean isBankrupt();
    void setBankrupt(boolean b);
    java.util.Set<Integer> getDeeds(); // mutable set of owned OwnableSpace indices
}
