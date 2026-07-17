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
        /** The campaign has been won (Alien Brain destroyed on Cydonia). */
        void onVictory(String message);
        /** The campaign has been lost (Council termination / funding collapse). */
        void onDefeat(String message);
    }

    private static final double INTERCEPT_DIST = 0.02;
    private static final long MONTH_SECONDS = 30L * 86400L;

    /** Research id that opens the final Cydonia assault. */
    public static final String CYDONIA_RESEARCH_ID = "cydonia_or_bust";
    /** A month whose net score is below this counts as "poor performance". */
    private static final int BAD_MONTH_SCORE = 0;
    /** Consecutive poor months (or a funding collapse) that trigger Council termination. */
    private static final int BAD_MONTHS_TO_LOSE = 2;

    /** Heavy score penalty inflicted when a terror site is ignored until it expires. */
    public static final int TERROR_IGNORE_PENALTY = -150;

    /** Candidate terror-mission cities (name, normalised x, y). Placeholder geography. */
    private static final String[][] TERROR_CITIES = {
        {"New York", "0.24", "0.30"}, {"Los Angeles", "0.13", "0.34"},
        {"London", "0.48", "0.24"}, {"Berlin", "0.52", "0.24"},
        {"Moscow", "0.62", "0.22"}, {"Tokyo", "0.88", "0.32"},
        {"Beijing", "0.80", "0.28"}, {"Rio de Janeiro", "0.30", "0.66"},
        {"Sydney", "0.88", "0.70"}, {"Cairo", "0.56", "0.40"},
    };

    private final Ruleset ruleset;
    private final Rng rng;
    private final GeoClock clock = new GeoClock();
    private final Base base;
    private final List<Interceptor> interceptors = new ArrayList<Interceptor>();
    private final List<Ufo> ufos = new ArrayList<Ufo>();
    private final List<TerrorSite> terrorSites = new ArrayList<TerrorSite>();
    private final List<FundingNation> nations = new ArrayList<FundingNation>();

    private long funds = 1_000_000L;
    private int totalScore;
    private int lastMonth = 0;
    private Campaign campaign;
    private Difficulty difficulty = Difficulty.EXPERIENCED;
    private long nextSpawnSeconds = 3600; // first UFO within the first game-hour
    private int ufoCounter;
    private long nextTerrorSeconds = -1;  // scheduled lazily on the first tick
    private int terrorCounter;

    // ---- endgame state ------------------------------------------------------
    private int monthStartScore;         // totalScore at the start of the current month
    private int consecutiveBadMonths;    // for the Council-termination loss condition
    private boolean gameWon;
    private boolean gameLost;

    private Listener listener = new Listener() {
        @Override public void onEvent(String message) { }
        @Override public void onChanged() { }
        @Override public void onCrashSite(Ufo ufo) { }
        @Override public void onVictory(String message) { }
        @Override public void onDefeat(String message) { }
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
    public List<TerrorSite> terrorSites() { return terrorSites; }
    public List<FundingNation> nations() { return nations; }
    public long funds() { return funds; }
    public int totalScore() { return totalScore; }
    public Campaign campaign() { return campaign; }
    public void setCampaign(Campaign c) { this.campaign = c; }
    public Difficulty difficulty() { return difficulty; }
    public void setDifficulty(Difficulty d) { if (d != null) { this.difficulty = d; } }

    // ---- endgame accessors --------------------------------------------------
    public boolean gameWon() { return gameWon; }
    public boolean gameLost() { return gameLost; }
    public boolean gameOver() { return gameWon || gameLost; }
    public int consecutiveBadMonths() { return consecutiveBadMonths; }

    /** True once "Cydonia or Bust!" is researched and the final assault can be launched. */
    public boolean cydoniaAvailable() {
        return campaign != null && !gameOver()
                && campaign.completedResearch().contains(CYDONIA_RESEARCH_ID);
    }

    /** Restore top-level state from a save (funds, score, clock). */
    public void restoreState(long funds, int score, long clockSeconds) {
        restoreState(funds, score, clockSeconds, 0, false, false);
    }

    /** Restore top-level state including the Phase 7 endgame flags. */
    public void restoreState(long funds, int score, long clockSeconds,
                             int consecutiveBadMonths, boolean gameWon, boolean gameLost) {
        this.funds = funds;
        this.totalScore = score;
        this.clock.setSeconds(clockSeconds);
        this.lastMonth = (int) (clockSeconds / MONTH_SECONDS);
        this.monthStartScore = score;
        this.consecutiveBadMonths = consecutiveBadMonths;
        this.gameWon = gameWon;
        this.gameLost = gameLost;
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
        if (gameOver()) {
            return;
        }
        int step = clock.tick();
        if (step <= 0) {
            return;
        }
        double hours = step / 3600.0;

        maybeSpawn();
        maybeSpawnTerror();
        flyUfos(hours);
        detectUfos(step);
        flyInterceptors(hours);
        expireUfos();
        expireTerror();
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
        // Weight toward smaller craft: take the lower of two rolls so scouts (the
        // early defs) dominate and unkillable capital ships stay rare. Higher
        // difficulty shifts the mix upward by biasing less.
        int a = rng.nextInt(defs.size());
        int b = rng.nextInt(defs.size());
        int idx = difficulty.level() >= 3 ? Math.max(a, b) : Math.min(a, b);
        UfoDef def = defs.get(Math.min(defs.size() - 1, idx));
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

    // ---- terror missions ----------------------------------------------------

    private void maybeSpawnTerror() {
        if (nextTerrorSeconds < 0) {
            // First terror threat lands a few game-days in.
            nextTerrorSeconds = clock.seconds() + (2 + rng.nextInt(3)) * 86400L;
            return;
        }
        if (clock.seconds() < nextTerrorSeconds) {
            return;
        }
        spawnTerrorSite();
        nextTerrorSeconds = scheduleNextTerror();
    }

    /** Gap to the next terror mission — tighter (more frequent) at higher difficulty. */
    private long scheduleNextTerror() {
        int baseDays = Math.max(4, 14 - difficulty.level() * 2); // Beginner 14 … Superhuman 6
        int jitter = rng.nextInt(5);
        return clock.seconds() + (baseDays + jitter) * 86400L;
    }

    private void spawnTerrorSite() {
        String[] c = TERROR_CITIES[rng.nextInt(TERROR_CITIES.length)];
        long lifetime = (24 + rng.nextInt(25)) * 3600L; // 24..48 game-hours to respond
        TerrorSite site = new TerrorSite("T" + (++terrorCounter), c[0],
                Double.parseDouble(c[1]), Double.parseDouble(c[2]),
                clock.seconds(), clock.seconds() + lifetime);
        terrorSites.add(site);
        listener.onEvent("ALERT: alien TERROR MISSION at " + c[0]
                + "! Assault it before the aliens finish, or lose Council standing.");
    }

    private void expireTerror() {
        Iterator<TerrorSite> it = terrorSites.iterator();
        while (it.hasNext()) {
            TerrorSite t = it.next();
            if (t.status() == TerrorSite.Status.ACTIVE && clock.seconds() >= t.expiresAtSeconds()) {
                addScore(TERROR_IGNORE_PENALTY, "terror mission ignored");
                listener.onEvent("The terror mission at " + t.cityName()
                        + " went unopposed — civilians massacred. The Council is furious ("
                        + TERROR_IGNORE_PENALTY + " score).");
                t.setStatus(TerrorSite.Status.EXPIRED);
                it.remove();
            } else if (t.status() != TerrorSite.Status.ACTIVE) {
                it.remove();
            }
        }
    }

    /** Deterministically place a terror site (for scripted scenarios and tests). */
    public TerrorSite deployTerrorSite(double x, double y, long lifetimeSeconds) {
        TerrorSite site = new TerrorSite("T" + (++terrorCounter), "Test City", x, y,
                clock.seconds(), clock.seconds() + lifetimeSeconds);
        terrorSites.add(site);
        return site;
    }

    /** Count of terror sites still awaiting a response. */
    public int activeTerrorCount() {
        int n = 0;
        for (TerrorSite t : terrorSites) {
            if (t.active()) {
                n++;
            }
        }
        return n;
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
        deployPlayerSquad(setup, rifle);
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

    /**
     * Deploy the player's squad into a setup: the persistent roster's fittest
     * soldiers with their chosen loadouts, falling back to a scratch squad when
     * there is no campaign or no <em>deployable</em> soldier (e.g. an all-wounded
     * roster) — so a mission never launches with an empty side and crashes.
     */
    private void deployPlayerSquad(BattleSetup setup, String rifle) {
        int deployed = 0;
        if (campaign != null && campaign.roster().size() > 0) {
            for (Soldier s : campaign.roster().deployable(6)) {
                String wid = ruleset.hasWeapon(s.weaponId()) ? s.weaponId() : rifle;
                String aid = ruleset.hasArmor(s.armorId()) ? s.armorId() : "none";
                setup.addSoldier(BattleSetup.UnitSpec.soldier(s.name(), wid, aid,
                        s.firingAccuracy(), s.timeUnits(), s.health(), s.reactions(), s.strength()));
                deployed++;
            }
        }
        if (deployed == 0) {
            String[] squad = {"Sgt. Vasquez", "Cpl. Tanaka", "Pvt. Novak", "Pvt. Adeyemi", "Pvt. Ilves"};
            for (int i = 0; i < squad.length; i++) {
                setup.addSoldier(BattleSetup.UnitSpec.soldier(squad[i], rifle, "none",
                        55 + (i % 3) * 5, 54 + (i % 4), 32 + (i % 5), 45 + (i % 4) * 3, 30));
            }
        }
    }

    /**
     * Build the tactical setup for a terror-mission assault: a larger, tougher
     * alien crew than a routine crash site (a psi leader plus a race mix that
     * escalates with difficulty), fought at night. Reuses the same squad
     * deployment and reward pipeline as a UFO assault.
     */
    public BattleSetup buildTerrorAssault(TerrorSite site, long seed) {
        BattleSetup setup = new BattleSetup().mapSize(16, 16).seed(seed)
                .difficulty(difficulty).night(true);
        String rifle = ruleset.hasWeapon("rifle") ? "rifle" : ruleset.weapons().iterator().next().id();
        deployPlayerSquad(setup, rifle);

        String soldierId = ruleset.alien("sectoid_soldier") != null ? "sectoid_soldier"
                : ruleset.aliens().iterator().next().id();
        String leader = ruleset.alien("sectoid_leader") != null ? "sectoid_leader" : soldierId;
        boolean hasFloater = ruleset.alien("floater_soldier") != null;
        boolean hasMuton = ruleset.alien("muton_soldier") != null;
        int crew = Math.min(9, 6 + difficulty.level()); // 6 (Beginner) … 9 (Genius+)
        setup.addAlien(BattleSetup.UnitSpec.alien(leader, rifle));
        for (int i = 1; i < crew; i++) {
            String race = soldierId;
            // Tougher races grow more common as difficulty rises.
            if (hasMuton && difficulty.level() >= 3 && i % 3 == 0) {
                race = "muton_soldier";
            } else if (hasFloater && difficulty.level() >= 1 && i % 2 == 0) {
                race = "floater_soldier";
            }
            setup.addAlien(BattleSetup.UnitSpec.alien(race, rifle));
        }
        return setup;
    }

    /**
     * Apply the result of a terror-mission assault. Winning defends the city for a
     * large score and salvage; losing costs score. The terror site is cleared
     * either way (a lost assault still ends the mission).
     */
    public void resolveTerror(TerrorSite site, BattleOutcome outcome) {
        if (site != null) {
            site.setStatus(TerrorSite.Status.ASSAULTED);
            terrorSites.remove(site);
        }
        boolean victory = outcome != null && outcome.xcomVictory();
        if (victory) {
            int pts = 80 + (outcome == null ? 0 : outcome.aliensKilled() * 10);
            addScore(pts, "terror mission defended");
            funds += 50_000L; // city grateful; salvage recovered
            listener.onEvent("Terror mission defeated: +" + pts + " score, the city is saved.");
            if (outcome != null) {
                recoverLiveAliens(outcome);
            }
        } else {
            addScore(-80, "terror mission lost");
            listener.onEvent("The terror mission overwhelmed the squad. The city is lost.");
        }
        applyRosterConsequences(outcome, victory, true);
        listener.onChanged();
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
            recoverLiveAliens(outcome);
        } else {
            addScore(-20, "ground assault failed");
            listener.onEvent("Mission failed.");
        }
        applyRosterConsequences(outcome, victory, true);
        listener.onChanged();
    }

    /**
     * Update the roster after a mission: KIA are removed, survivors gain experience.
     * When {@code logDeaths} is set each casualty is also written to the event log.
     */
    private void applyRosterConsequences(BattleOutcome outcome, boolean victory, boolean logDeaths) {
        if (campaign == null || outcome == null) {
            return;
        }
        for (String fallen : outcome.fallenSoldiers()) {
            campaign.roster().removeByName(fallen);
            if (logDeaths) {
                listener.onEvent(fallen + " was killed in action.");
            }
        }
        for (String name : outcome.survivingSoldiers()) {
            Soldier s = campaign.roster().byName(name);
            if (s != null) {
                s.onMissionSurvived(victory);
            }
        }
    }

    /**
     * Move any aliens stunned unconscious on a won field into the live-alien store,
     * gated on an Alien Containment facility with free capacity. Without containment
     * (or once full) the captive cannot be held and is lost. Live aliens are stored
     * as {@code "live_<alienId>"} items so interrogation research can consume them.
     */
    private void recoverLiveAliens(BattleOutcome outcome) {
        if (campaign == null || outcome.liveCaptures().isEmpty()) {
            return;
        }
        if (!base.hasFacility("alien_containment")) {
            listener.onEvent("No Alien Containment — " + outcome.liveCaptures().size()
                    + " live alien(s) could not be held and expired.");
            return;
        }
        int free = base.containmentCapacity() - liveAlienCount();
        for (String alienId : outcome.liveCaptures()) {
            if (free <= 0) {
                listener.onEvent("Alien Containment full — a live captive was lost.");
                continue;
            }
            campaign.addToStores("live_" + alienId, 1);
            free--;
            listener.onEvent("Live " + alienId + " secured in Alien Containment.");
        }
        addScore(15 * outcome.liveCaptures().size(), "live capture");
    }

    /** Count of live aliens currently held (store items keyed {@code "live_*"}). */
    public int liveAlienCount() {
        if (campaign == null) {
            return 0;
        }
        int total = 0;
        for (java.util.Map.Entry<String, Integer> e : campaign.stores().entrySet()) {
            if (e.getKey().startsWith("live_")) {
                total += e.getValue();
            }
        }
        return total;
    }

    // ---- Cydonia final assault ----------------------------------------------

    /**
     * Build a stage of the two-stage Cydonia assault. Stage 0 is the Martian
     * surface (an escort force); stage 1 is the alien base interior, which holds
     * the {@link #CYDONIA_RESEARCH_ID Alien Brain} — killing it wins the game.
     */
    public BattleSetup buildCydoniaAssault(long seed, int stage) {
        BattleSetup setup = new BattleSetup().mapSize(16, 16).seed(seed).difficulty(difficulty);
        String rifle = ruleset.hasWeapon("rifle") ? "rifle" : ruleset.weapons().iterator().next().id();
        String heavy = ruleset.hasWeapon("heavy_plasma") ? "heavy_plasma" : rifle;
        String stun = ruleset.hasWeapon("stun_rod") ? "stun_rod" : rifle;
        if (campaign != null && campaign.roster().size() > 0) {
            int i = 0;
            for (Soldier s : campaign.roster().deployable(6)) {
                String wid = ruleset.hasWeapon(s.weaponId()) ? s.weaponId() : rifle;
                String aid = ruleset.hasArmor(s.armorId()) ? s.armorId() : "none";
                setup.addSoldier(BattleSetup.UnitSpec.soldier(s.name(), wid, aid,
                        s.firingAccuracy(), s.timeUnits(), s.health(), s.reactions(), s.strength()));
                i++;
            }
            if (i == 0) {
                addDefaultCydoniaSquad(setup, rifle, stun);
            }
        } else {
            addDefaultCydoniaSquad(setup, rifle, stun);
        }
        String elite = ruleset.alien("muton_soldier") != null ? "muton_soldier"
                : ruleset.aliens().iterator().next().id();
        String leader = ruleset.alien("sectoid_leader") != null ? "sectoid_leader" : elite;
        if (stage <= 0) {
            // Martian surface: an escort screen.
            for (int k = 0; k < 5; k++) {
                setup.addAlien(BattleSetup.UnitSpec.alien(k == 0 ? leader : elite, heavy));
            }
        } else {
            // Alien base interior: guards plus the Brain objective.
            for (int k = 0; k < 4; k++) {
                setup.addAlien(BattleSetup.UnitSpec.alien(elite, heavy));
            }
            if (ruleset.alien("alien_brain") != null) {
                setup.addAlien(BattleSetup.UnitSpec.alien("alien_brain", rifle));
            }
        }
        return setup;
    }

    private void addDefaultCydoniaSquad(BattleSetup setup, String rifle, String stun) {
        String[] squad = {"Sgt. Vasquez", "Cpl. Tanaka", "Pvt. Novak", "Pvt. Adeyemi", "Pvt. Ilves"};
        for (int i = 0; i < squad.length; i++) {
            setup.addSoldier(BattleSetup.UnitSpec.soldier(squad[i], i == 0 ? stun : rifle, "none",
                    60, 58, 34, 50, 32));
        }
    }

    /**
     * Apply the result of the final Cydonia stage. Winning the alien-base stage
     * (the Brain destroyed) wins the campaign. Returns true if the game was won.
     */
    public boolean resolveCydonia(BattleOutcome outcome, boolean finalStage) {
        boolean victory = outcome != null && outcome.xcomVictory();
        if (campaign != null && outcome != null) {
            for (String fallen : outcome.fallenSoldiers()) {
                campaign.roster().removeByName(fallen);
            }
            for (String name : outcome.survivingSoldiers()) {
                Soldier s = campaign.roster().byName(name);
                if (s != null) {
                    s.onMissionSurvived(victory);
                }
            }
        }
        if (!victory) {
            addScore(-100, "Cydonia assault failed");
            listener.onEvent("The assault on Cydonia was repelled.");
            listener.onChanged();
            return false;
        }
        addScore(200, "Cydonia stage cleared");
        if (finalStage) {
            gameWon = true;
            clock.setSpeed(GeoClock.Speed.PAUSE);
            String msg = "The Alien Brain is destroyed. The alien threat is ended — X-COM is victorious!";
            listener.onEvent("*** " + msg + " ***");
            listener.onVictory(msg);
        } else {
            listener.onEvent("Martian surface cleared — advance into the alien base!");
        }
        listener.onChanged();
        return gameWon;
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

        int monthScore = totalScore - monthStartScore;
        monthStartScore = totalScore;
        listener.onEvent(String.format(
                "— Monthly Report — funding +$%,d, maintenance -$%,d (month score %d, total %d)",
                funding, maintenance, monthScore, totalScore));

        // Council review: a poor month (net-negative score) or a funding collapse
        // (bankruptcy) counts against X-COM; two in a row and the project is closed.
        boolean poorMonth = monthScore < BAD_MONTH_SCORE || funds < 0;
        if (poorMonth) {
            consecutiveBadMonths++;
            if (consecutiveBadMonths == 1) {
                listener.onEvent("The Council is dissatisfied with X-COM's performance. Improve, or be terminated.");
            }
        } else {
            consecutiveBadMonths = 0;
        }
        if (consecutiveBadMonths >= BAD_MONTHS_TO_LOSE && !gameOver()) {
            gameLost = true;
            clock.setSpeed(GeoClock.Speed.PAUSE);
            String msg = "The Council of Funding Nations has terminated Project X-COM. Defeat.";
            listener.onEvent("*** " + msg + " ***");
            listener.onDefeat(msg);
        }
        listener.onChanged();
    }

    private static double dist(double x0, double y0, double x1, double y1) {
        return Math.hypot(x1 - x0, y1 - y0);
    }
}
