package com.whim.xcom.geo;

import com.whim.xcom.rng.SeededRng;
import com.whim.xcom.rules.Ruleset;

/**
 * Builds a default Geoscape campaign: one fully-equipped base, two interceptors
 * and the Council funding nations. Content (facilities) is pulled from the
 * ruleset so a data pack can retune the base loadout.
 */
public final class GeoFactory {

    private GeoFactory() {
    }

    public static GeoGame defaultCampaign(Ruleset ruleset, long seed) {
        Base base = new Base("X-COM HQ", 0.50, 0.42);
        String[] facilityIds = {
            "access_lift", "living_quarters", "laboratory", "workshop",
            "small_radar", "large_radar", "hangar", "general_stores"
        };
        for (String id : facilityIds) {
            base.addFacility(ruleset.facility(id));
        }

        GeoGame game = new GeoGame(ruleset, new SeededRng(seed), base);
        game.addInterceptor(new Interceptor("Skyranger-1", base.x(), base.y(), 0.60, 20, 100));
        game.addInterceptor(new Interceptor("Interceptor-1", base.x(), base.y(), 0.75, 24, 100));

        String[][] nationData = {
            {"USA", "400000"}, {"Russia", "380000"}, {"UK", "240000"},
            {"France", "260000"}, {"Germany", "300000"}, {"Japan", "320000"},
            {"China", "220000"}, {"Brazil", "180000"}
        };
        for (String[] n : nationData) {
            game.addNation(new FundingNation(n[0], Integer.parseInt(n[1])));
        }
        return game;
    }
}
