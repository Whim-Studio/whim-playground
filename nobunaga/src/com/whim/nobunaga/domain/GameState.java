package com.whim.nobunaga.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Whole-world snapshot: every province and daimyo (both index-aligned to their
 * id), the current calendar (year + season), who the human plays, and the
 * single shared, seeded {@link Random}. All randomness in the game must draw
 * from {@link #rng} so a seed reproduces a run exactly.
 */
public final class GameState {
    public final List<Province> provinces;
    public final List<Daimyo> daimyos;
    public int year = 1560;
    public Season season = Season.SPRING;
    public int playerDaimyoId;
    public final Random rng;

    public GameState(List<Province> provinces, List<Daimyo> daimyos, int playerDaimyoId, long seed) {
        this.provinces = new ArrayList<Province>(provinces);
        this.daimyos = new ArrayList<Daimyo>(daimyos);
        this.playerDaimyoId = playerDaimyoId;
        this.rng = new Random(seed);
    }

    public Province province(int id) {
        return provinces.get(id);
    }

    public Daimyo daimyo(int id) {
        return daimyos.get(id);
    }

    public Daimyo player() {
        return daimyos.get(playerDaimyoId);
    }

    /** Every province currently owned by the given daimyo, in id order. */
    public List<Province> provincesOf(int daimyoId) {
        List<Province> out = new ArrayList<Province>();
        for (int i = 0; i < provinces.size(); i++) {
            Province p = provinces.get(i);
            if (p.getOwnerId() == daimyoId) {
                out.add(p);
            }
        }
        return out;
    }

    /** Advance to the next season; bump the year when wrapping into SPRING. */
    public void advanceClock() {
        season = season.next();
        if (season == Season.SPRING) {
            year++;
        }
    }
}
