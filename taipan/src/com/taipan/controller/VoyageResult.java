package com.taipan.controller;

import com.taipan.model.PortCity;

import java.util.ArrayList;
import java.util.List;

/**
 * Outcome of setting sail toward a destination. Non-interactive events (storms,
 * opium seizures, price flavour, bank interest) are pre-resolved into {@link #log}.
 * Two interactive follow-ups may be attached for the view to handle:
 *   - {@link #liYuenDemand} &gt; 0 : Li Yuen demands tribute (pay or refuse).
 *   - {@link #combat} != null    : a sea battle must be fought.
 */
public class VoyageResult {

    public final List<String> log = new ArrayList<String>();
    public PortCity destination;
    public PortCity actualArrival; // may differ if blown off course
    public long liYuenDemand = 0L;
    public CombatSession combat = null;
    public boolean gameOver = false;

    public VoyageResult(PortCity destination) {
        this.destination = destination;
        this.actualArrival = destination;
    }
}
