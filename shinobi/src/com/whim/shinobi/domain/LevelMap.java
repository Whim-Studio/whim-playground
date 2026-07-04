// TODO integrate domain — PLACEHOLDER (Task 2 engine stub; Task 1 replaces this file). See PLACEHOLDER_README.md.
package com.whim.shinobi.domain;

import java.util.ArrayList;
import java.util.List;

/** Static geometry container: platforms + hostage spawns for both planes. */
public class LevelMap {
    public final List<Platform> platforms = new ArrayList<Platform>();
    public final List<Hostage> hostages = new ArrayList<Hostage>();
    public final int width;

    public LevelMap(int width) { this.width = width; }
}
