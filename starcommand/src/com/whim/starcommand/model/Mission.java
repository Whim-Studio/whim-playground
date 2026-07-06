package com.whim.starcommand.model;

import java.io.Serializable;

/** A Star Command HQ assignment. v1 implements a small hand-authored ladder. */
public class Mission implements Serializable {
    private static final long serialVersionUID = 1L;

    public String id;
    public String title;
    public String briefing;
    public int reward;
    public boolean accepted = false;
    public boolean complete = false;
    /** Sector coordinates of the objective, encoded as x*100+y for simplicity. */
    public int targetSector = -1;

    public Mission() { }

    public Mission(String id, String title, String briefing, int reward, int targetSector) {
        this.id = id;
        this.title = title;
        this.briefing = briefing;
        this.reward = reward;
        this.targetSector = targetSector;
    }
}
