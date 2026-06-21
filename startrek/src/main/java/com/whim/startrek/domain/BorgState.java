package com.whim.startrek.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent Borg threat. The Borg engine mutates this each turn; it never
 * auto-resolves until the collective is eradicated. Intensity scales upward every
 * turn the threat is active.
 */
public class BorgState {

    private boolean active;
    private int cubeCount;
    private int intensity;
    private final List<int[]> controlledCells = new ArrayList<int[]>(); // {row,col} pairs

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean b) {
        this.active = b;
    }

    public int getCubeCount() {
        return cubeCount;
    }

    public void setCubeCount(int n) {
        this.cubeCount = n;
    }

    /** Mutable list of {row,col} cells the Borg currently control. */
    public List<int[]> getControlledCells() {
        return controlledCells;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int n) {
        this.intensity = n;
    }
}
