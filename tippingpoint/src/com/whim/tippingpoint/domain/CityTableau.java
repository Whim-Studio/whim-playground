package com.whim.tippingpoint.domain;

import java.util.ArrayList;
import java.util.List;

/** A player's city: the citizens and permanent developments they own. */
public final class CityTableau {
    private final List<CitizenCard> citizens = new ArrayList<CitizenCard>();
    private final List<DevelopmentCard> developments = new ArrayList<DevelopmentCard>();

    public CityTableau() {}

    public List<CitizenCard> getCitizens() { return citizens; }
    public List<DevelopmentCard> getDevelopments() { return developments; }

    public void addCitizen(CitizenCard c) { citizens.add(c); }
    public void addDevelopment(DevelopmentCard c) { developments.add(c); }

    public int populationCount() { return citizens.size(); }

    public int workerCount() {
        int n = 0;
        for (int i = 0; i < citizens.size(); i++) {
            if (citizens.get(i).getType() == CitizenType.WORKER) n++;
        }
        return n;
    }

    public int farmerCount() {
        int n = 0;
        for (int i = 0; i < citizens.size(); i++) {
            if (citizens.get(i).getType() == CitizenType.FARMER) n++;
        }
        return n;
    }

    /** Remove up to n citizens, WORKERS first then farmers. */
    public void removeCitizens(int n) {
        int remaining = n;
        // WORKERS first
        for (int i = citizens.size() - 1; i >= 0 && remaining > 0; i--) {
            if (citizens.get(i).getType() == CitizenType.WORKER) {
                citizens.remove(i);
                remaining--;
            }
        }
        // then farmers
        for (int i = citizens.size() - 1; i >= 0 && remaining > 0; i--) {
            if (citizens.get(i).getType() == CitizenType.FARMER) {
                citizens.remove(i);
                remaining--;
            }
        }
    }
}
