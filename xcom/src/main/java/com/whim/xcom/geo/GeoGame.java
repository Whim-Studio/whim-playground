package com.whim.xcom.geo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.whim.xcom.battle.BattleOutcome;
import com.whim.xcom.battle.BattleSetup;
import com.whim.xcom.meta.Campaign;
import com.whim.xcom.meta.Soldier;
import com.whim.xcom.model.Difficulty;
import com.whim.xcom.rng.Rng;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.def.ManufactureNode;
import com.whim.xcom.rules.def.UfoDef;

/**
 * The Geoscape campaign engine: advances the clock, spawns and flies UFOs,
 * detects them by radar, resolves interceptor air combat, tracks funds/score and
 * runs the monthly Council review. Pure and deterministic (seeded {@link Rng}) —
 * the Swing view drives it via {@link #tick()} on a timer and observes through a
 * {@link Listener}. Ground assaults are handed off through the listener so the
 * app can switch to the Battlescape.
 */
public final class GeoGame {

    /** Observer hook for the view. */
    public interface Listener {
        void onEvent(String message);
        void onChanged();
        /** A crash/landing site is ready to assault; the view may launch a battle. */
        void onCrashSite(Ufo ufo);
    }

    private static final double INTERCEPT_DIST = 0.02;
    private static final long MONTH_SECONDS = 30L * 86400L;

    private final Ruleset ruleset;
    private final Rng rng;
    private final GeoClock clock = new GeoClock();
    private final Base base;
    private final List<Interceptor> interceptors = new ArrayList<Interceptor>();
    private final List<Ufo> ufos = new ArrayList<Ufo>();
    private final List<FundingNation> nations = new ArrayList<FundingNation>();

    private long funds = 1_000_000L;
    private int totalScore;
    private int lastMonth = 0;
    private Campaign campaign;
    private Difficulty difficulty = Difficulty.EXPERIENCED;
    private long nextSpawnSeconds = 3600; // first UFO within the first game-hour
    private int ufoCounter;

    private Listener listener = new Listener() {
        @Override public void onEvent(String message) { }
        @Override public void onChanged() { }
        @Override public void onCrashSite(Ufo ufo) { }
    };

    public GeoGame(Ruleset ruleset, Rng rng, Base base) {
        this.ruleset = ruleset;
        this.rng = rng;
        this.base = base;
    }

    public void setListener(Listener l) {
        this.listener = l == null ? this.listener : l;
    }

    // ---- accessors ----------------------------------------------------------

    public GeoClock clock() { return clock; }
    public Base base() { return base; }
    public List<Interceptor> interceptors() { return interceptors; }
    public List<Ufo> ufos() { return ufos; }
    public List<FundingNation> nations() { return nations; }
    public long funds() { return funds; }
    public int totalScore() { return totalScore; }
    public Campaign campaign() { return campaign; }
    public void setCampaign(Campaign c) { this.campaign = c; }
    public Difficulty difficulty() { return difficulty; }
    public void setDifficulty(Difficulty d) { if (d != null) { this.difficulty = d; } }

    /** Restore top-level state from a save (funds, score, clock). */
    public void restoreState(long funds, int score, long clockSeconds) {
        this.funds = funds;
        this.totalScore = score;
        this.clock.setSeconds(clockSeconds);
        this.lastMonth = (int) (clockSeconds / MONTH_SECONDS);
    }

    /**
     * Order manufacture of {@code quantity} units of {@code node}: deducts the
     * up-front dollar cost and queues the job. Returns false if unaffordable, the
     * research is not unlocked, or there is no campaign.
     */
    public boolean orderManufacture(ManufactureNode node, int quantity, int engineers) {
        if (campaign == null || node == null
                || !campaign.researchUnlocksManufacture(node)) {
            return false;
        }
        long cost = (long) node.costDollars() * quantity;
        if (funds < cost) {
            listener.onEvent("Insufficient funds to manufacture " + node.name() + ".");
            return false;
        }
        funds -= cost;
        campaign.startManufacture(node, engineers, quantity);
        listener.onEvent("Manufacture ordered: " + quantity + "x " + node.name());
        listener.onChanged();
        return true;
    }

    public void addInterceptor(Interceptor i) { interceptors.add(i); }
    public void addNation(FundingNation n) { nations.add(n); }

    /** Deterministically place a UFO (for scripted scenarios and tests). */
    public Ufo deployUfo(UfoDef def, double x, double y) {
        Ufo ufo = new Ufo("U" + (++ufoCounter), def, x, y, clock.seconds());
        ufo.setDest(x, y);
        ufos.add(ufo);
        return ufo;
    }

    // ---- main loop ----------------------------------------------------------

    /** Advance the world by the current clock speed. No-op while paused. */
    public void tick() {
        int step = clock.tick();
        if (step <= 0) {
            return;
        }
        double hours = step / 3600.0;

        maybeSpawn();
        flyUfos(hours);
        detectUfos(step);
        flyInterceptors(hours);
        expireUfos();
        if (campaign != null) {
            for (String ev : campaign.advance(step)) {
                listener.onEvent(ev);
            }
        }
        checkMonthRollover();

        listener.onChanged();
    }

    private void maybeSpawn() {
        if (clock.seconds() < nextSpawnSeconds) {
            return;
        }
        spawnUfo();
        // Next UFO in 4..12 game-hours.
        nextSpawnSeconds = clock.seconds() + (4 + rng.nextInt(9)) * 3600L;
    }

    private void spawnUfo() {
        List<UfoDef> defs = new ArrayList<UfoDef>(ruleset.ufos());
        if (defs.isEmpty()) {
            return;
        }
        // Weight toward smaller craft early: bias the pick low.
        int idx = Math.min(defs.size() - 1, rng.nextInt(defs.size()));
        UfoDef def = defs.get(idx);
        double x = rng.nextDouble();
        double y = 0.1 + rng.nextDouble() * 0.8;
        Ufo ufo = new Ufo("U" + (++ufoCounter), def, x, y, clock.seconds());
        ufo.setDest(rng.nextDouble(), 0.1 + rng.nextDouble() * 0.8);
        ufos.add(ufo);
        listener.onEvent("Radar contact lost... a " + def.name() + " has appeared.");
    }

    private void flyUfos(double hours) {
        for (Ufo u : ufos) {
            if (u.status() != Ufo.Status.FLYING) {
                continue;
            }
            double sp = u.def().speed() / 8000.0 * hours; // world-fraction this step
            double dx = u.destX() - u.x();
            double dy = u.destY() - u.y();
            double dist = Math.hypot(dx, dy);
            if (dist < 1e-4 || sp >= dist) {
                u.setPos(u.destX(), u.destY());
                // pick a new wandering destination
                u.setDest(rng.nextDouble(), 0.1 + rng.nextDouble() * 0.8);
            } else {
                u.setPos(u.x() + dx / dist * sp, u.y() + dy / dist * sp);
            }
        }
    }

    private void detectUfos(int stepSeconds) {
        double range = base.radarRangeNorm();
        int chance = base.detectionChancePercent();
        if (range <= 0 || chance <= 0) {
            return;
        }
        for (Ufo u : ufos) {
            if (u.detected() || u.status() != Ufo.Status.FLYING) {
                continue;
            }
            if (dist(u.x(), u.y(), base.x(), base.y()) > range) {
                continue;
            }
            double prob = chance / 100.0 * (stepSeconds / 1800.0);
            if (rng.chance(Math.min(1.0, prob))) {
                u.setDetected(true);
                listener.onEvent("UFO-" + u.id() + " (" + u.def().name() + ") detected!");
            }
        }
    }

    private void flyInterceptors(double hours) {
        for (Interceptor c : interceptors) {
            if (c.status() == Interceptor.Status.PURSUING) {
                Ufo t = c.target();
                if (t == null || !t.active() || !t.detected()) {
                    recall(c);
                    continue;
                }
                moveToward(c, t.x(), t.y(), hours);
                if (dist(c.x(), c.y(), t.x(), t.y()) <= INTERCEPT_DIST) {
                    resolveAirCombat(c, t);
                }
            } else if (c.status() == Interceptor.Status.RETURNING) {
                moveToward(c, c.baseX(), c.baseY(), hours);
                if (c.atBase()) {
                    c.setStatus(Interceptor.Status.READY);
                    c.repairFull();
                }
            }
        }
    }

    private void moveToward(Interceptor c, double tx, double ty, double hours) {
        double sp = c.speed() * hours;
        double dx = tx - c.x();
        double dy = ty - c.y();
        double d = Math.hypot(dx, dy);
        if (d <= sp || d < 1e-6) {
            c.setPos(tx, ty);
        } else {
            c.setPos(c.x() + dx / d * sp, c.y() + dy / d * sp);
        }
    }

    private void resolveAirCombat(Interceptor c, Ufo u) {
        listener.onEvent(c.name() + " engages UFO-" + u.id() + "!");
        for (int round = 0; round < 12 && u.active() && c.hp() > 0; round++) {
            int dmg = c.cannonDamage() + rng.rangeInclusive(-5, 5);
            if (u.damageHull(Math.max(1, dmg))) {
                listener.onEvent("UFO-" + u.id() + " shot down!");
                u.setDetected(true);
                recall(c);
                addScore(30, "UFO destroyed");
                listener.onCrashSite(u);
                return;
            }
            if (u.def().weaponPower() > 0) {
                c.damage(u.def().weaponPower() / 4 + rng.nextInt(4));
                if (c.hp() <= 0) {
                    listener.onEvent(c.name() + " was shot down by UFO-" + u.id() + ".");
                    interceptors.remove(c);
                    addScore(-30, "interceptor lost");
                    return;
                }
            }
        }
        // Stalemate — UFO outruns the craft.
        listener.onEvent("UFO-" + u.id() + " escaped the interception.");
        recall(c);
    }

    private void recall(Interceptor c) {
        c.setStatus(Interceptor.Status.RETURNING);
        c.setTarget(null);
    }

    private void expireUfos() {
        Iterator<Ufo> it = ufos.iterator();
        while (it.hasNext()) {
            Ufo u = it.next();
            if (u.status() == Ufo.Status.FLYING
                    && clock.seconds() - u.spawnedAtSeconds() > 10 * 3600L) {
                // Completed its mission and left.
                it.remove();
                if (!u.detected()) {
                    addScore(-15, "UFO mission unopposed");
                }
            } else if (u.status() == Ufo.Status.DESTROYED
                    || u.status() == Ufo.Status.ESCAPED) {
                it.remove();
            }
        }
    }

    // ---- interception command ----------------------------------------------

    /** Send the first ready interceptor after a detected UFO. Returns true if launched. */
    public boolean intercept(Ufo ufo) {
        if (ufo == null || !ufo.detected() || !ufo.active()) {
            return false;
        }
        for (Interceptor c : interceptors) {
            if (c.ready()) {
                c.setStatus(Interceptor.Status.PURSUING);
                c.setTarget(ufo);
                listener.onEvent(c.name() + " launched to intercept UFO-" + ufo.id() + ".");
                listener.onChanged();
                return true;
            }
        }
        listener.onEvent("No interceptor available.");
        return false;
    }

    // ---- ground assault handoff --------------------------------------------

    /** Build the tactical setup for assaulting a crash/landing site. */
    public BattleSetup buildAssault(Ufo ufo, long seed) {
        int mapDim = Math.max(12, Math.min(18, ufo.def().mapSize() / 3));
        BattleSetup setup = new BattleSetup().mapSize(mapDim, mapDim).seed(seed).difficulty(difficulty);
        String rifle = ruleset.weapon("rifle") != null ? "rifle"
                : ruleset.weapons().iterator().next().id();
        // Squad: the persistent roster if we have a campaign, else a default squad.
        if (campaign != null && campaign.roster().size() > 0) {
            for (Soldier s : campaign.roster().deployable(6)) {
                setup.addSoldier(BattleSetup.UnitSpec.soldier(s.name(), rifle, "none",
                        s.firingAccuracy(), s.timeUnits(), s.health(), s.reactions(), s.strength()));
            }
        } else {
            String[] squad = {"Sgt. Vasquez", "Cpl. Tanaka", "Pvt. Novak", "Pvt. Adeyemi", "Pvt. Ilves"};
            for (int i = 0; i < 5; i++) {
                setup.addSoldier(BattleSetup.UnitSpec.soldier(squad[i], rifle, "none",
                        55 + (i % 3) * 5, 54 + (i % 4), 32 + (i % 5), 45 + (i % 4) * 3, 30));
            }
        }
        // Crew scaled from the UFO type, capped for a playable slice.
        int crew = Math.min(6, Math.max(ufo.def().minCrew(),
                ufo.def().minCrew() + rng.nextInt(Math.max(1, ufo.def().maxCrew() - ufo.def().minCrew() + 1))));
        String soldierId = ruleset.alien("sectoid_soldier") != null ? "sectoid_soldier"
                : ruleset.aliens().iterator().next().id();
        // Mixed crew: a psi leader plus soldiers, with tougher races at higher difficulty.
        List<String> races = new ArrayList<String>();
        races.add(ruleset.alien("sectoid_leader") != null ? "sectoid_leader" : soldierId);
        for (int i = 1; i < crew; i++) {
            races.add(soldierId);
        }
        if (difficulty.level() >= 2 && ruleset.alien("floater_soldier") != null && races.size() > 1) {
            races.set(1, "floater_soldier");
        }
        if (difficulty.level() >= 3 && ruleset.alien("muton_soldier") != null && races.size() > 2) {
            races.set(races.size() - 1, "muton_soldier");
        }
        for (String race : races) {
            setup.addAlien(BattleSetup.UnitSpec.alien(race, rifle));
        }
        return setup;
    }

    /** Apply the result of a ground assault, update the roster and remove the site. */
    public void resolveMission(Ufo ufo, BattleOutcome outcome) {
        ufos.remove(ufo);
        boolean victory = outcome != null && outcome.xcomVictory();
        if (victory) {
            int pts = 40 + outcome.aliensKilled() * 10;
            addScore(pts, "ground assault success");
            funds += 30_000L; // salvage value (placeholder)
            listener.onEvent("Mission success: +" + pts + " score, salvage recovered.");
        } else {
            addScore(-20, "ground assault failed");
            listener.onEvent("Mission failed.");
        }
        // Roster consequences: KIA are removed, survivors gain experience.
        if (campaign != null && outcome != null) {
            for (String fallen : outcome.fallenSoldiers()) {
                campaign.roster().removeByName(fallen);
                listener.onEvent(fallen + " was killed in action.");
            }
            for (String name : outcome.survivingSoldiers()) {
                Soldier s = campaign.roster().byName(name);
                if (s != null) {
                    s.onMissionSurvived(victory);
                }
            }
        }
        listener.onChanged();
    }

    // ---- scoring & funding --------------------------------------------------

    private void addScore(int points, String reason) {
        totalScore += points;
        for (FundingNation n : nations) {
            n.addScore(points);
        }
    }

    private void checkMonthRollover() {
        int m = (int) (clock.seconds() / MONTH_SECONDS);
        if (m <= lastMonth) {
            return;
        }
        lastMonth = m;
        monthlyReport();
    }

    private void monthlyReport() {
        long funding = 0;
        for (FundingNation n : nations) {
            funding += n.applyMonthlyReview();
            n.resetMonthlyScore();
        }
        long maintenance = base.monthlyMaintenance();
        funds += funding - maintenance;
        listener.onEvent(String.format("— Monthly Report — funding +$%,d, maintenance -$%,d (score %d)",
                funding, maintenance, totalScore));
        listener.onChanged();
    }

    private static double dist(double x0, double y0, double x1, double y1) {
        return Math.hypot(x1 - x0, y1 - y0);
    }
}
