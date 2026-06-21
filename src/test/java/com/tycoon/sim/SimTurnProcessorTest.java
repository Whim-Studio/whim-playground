package com.tycoon.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.tycoon.core.AiStudio;
import com.tycoon.core.Employee;
import com.tycoon.core.Facility;
import com.tycoon.core.FacilityType;
import com.tycoon.core.FloorPlan;
import com.tycoon.core.GameProject;
import com.tycoon.core.GameState;
import com.tycoon.core.GameStudio;
import com.tycoon.core.GridPos;
import com.tycoon.core.Interrupt;
import com.tycoon.core.InterruptType;
import com.tycoon.core.ProjectPhase;
import com.tycoon.core.Room;
import com.tycoon.core.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class SimTurnProcessorTest {

    private static final double EPS = 1e-9;

    // ----------------------------------------------------------------------------------------
    // P_hourly formula
    // ----------------------------------------------------------------------------------------

    @Test
    public void pointsFollowBindingFormula() {
        // P = (S_base * mu_ws) * max(0.1, 1 - stress/100)
        // 50 * 1.0 * (1 - 0.20) = 40.0
        assertEquals(40.0, SimTurnProcessor.hourlyPoints(50, 1.0, 20.0), EPS);
        // 80 * 1.5 * (1 - 0.50) = 60.0
        assertEquals(60.0, SimTurnProcessor.hourlyPoints(80, 1.5, 50.0), EPS);
        // zero stress => full output
        assertEquals(50.0, SimTurnProcessor.hourlyPoints(50, 1.0, 0.0), EPS);
    }

    @Test
    public void stressFloorIsTenPercent() {
        // At max stress the multiplier floors at 0.1, NOT 0.
        assertEquals(50 * 1.0 * 0.1, SimTurnProcessor.hourlyPoints(50, 1.0, 100.0), EPS);
        // Even beyond clamp range the floor holds.
        assertEquals(50 * 1.0 * 0.1, SimTurnProcessor.hourlyPoints(50, 1.0, 150.0), EPS);
    }

    @Test
    public void noDeskProducesZeroPoints() {
        // mu_ws <= 0 represents "no DESK" => 0 points regardless of skill/stress.
        assertEquals(0.0, SimTurnProcessor.hourlyPoints(100, 0.0, 0.0), EPS);
        assertEquals(0.0, SimTurnProcessor.hourlyPoints(100, -1.0, 0.0), EPS);
    }

    @Test
    public void noDeskEmployeeProducesNoPointsInProcessHour() {
        GameState state = scenarioWithDeskedEmployee(false); // employee has NO desk
        GameProject project = state.player().projects().get(0);
        double before = project.developmentPoints();
        new SimTurnProcessor().processHour(state);
        assertEquals("no-desk employee must not generate points", before,
                project.developmentPoints(), EPS);
    }

    // ----------------------------------------------------------------------------------------
    // Stress: delta, clamp, crisis emission
    // ----------------------------------------------------------------------------------------

    @Test
    public void stressDeltaIsAccrualMinusRelief() {
        assertEquals(4.0, SimTurnProcessor.hourlyStressDelta(4.0, 0.0), EPS);
        assertEquals(1.5, SimTurnProcessor.hourlyStressDelta(4.0, 2.5), EPS);
        assertEquals(-1.0, SimTurnProcessor.hourlyStressDelta(2.0, 3.0), EPS);
    }

    @Test
    public void stressClampsToHundredAndCrisisFiresOnce() {
        GameState state = scenarioWithDeskedEmployee(true);
        Employee emp = state.player().employees().get(0);
        emp.setStress(89.0); // just below crisis; one working hour pushes it over

        SimTurnProcessor sim = new SimTurnProcessor();
        List<Interrupt> first = sim.processHour(state);
        assertTrue("stress must never exceed 100", emp.stress() <= 100.0 + EPS);
        assertTrue("crisis interrupt expected on crossing the threshold",
                hasType(first, InterruptType.EMPLOYEE_CRISIS));

        // Drive several more hours; stress saturates at 100 and crisis does NOT re-fire each hour.
        int extraCrises = 0;
        for (int i = 0; i < 5; i++) {
            state.advanceHourCounter();
            List<Interrupt> more = sim.processHour(state);
            if (hasType(more, InterruptType.EMPLOYEE_CRISIS)) {
                extraCrises++;
            }
        }
        assertEquals("crisis fires on the crossing, not repeatedly", 0, extraCrises);
        assertEquals("stress clamps at 100", 100.0, emp.stress(), EPS);
    }

    @Test
    public void stressNeverGoesNegative() {
        // Relief greatly exceeds accrual; stress floors at 0.
        double delta = SimTurnProcessor.hourlyStressDelta(SimTurnProcessor.BASE_STRESS_ACCRUAL, 50.0);
        double clamped = SimTurnProcessor.clamp(5.0 + delta, 0.0, 100.0);
        assertEquals(0.0, clamped, EPS);
    }

    // ----------------------------------------------------------------------------------------
    // Bug & polish dynamics
    // ----------------------------------------------------------------------------------------

    @Test
    public void productionBugsScaleWithPoints() {
        assertEquals(0.0, SimTurnProcessor.productionBugDelta(0.0), EPS);
        assertEquals(40.0 * SimTurnProcessor.BUG_GENERATION_RATE,
                SimTurnProcessor.productionBugDelta(40.0), EPS);
        assertTrue(SimTurnProcessor.productionBugDelta(40.0) > 0.0);
    }

    @Test
    public void polishBurnsBugsWithoutOvershoot() {
        // Plenty of bugs => burn the per-hour amount.
        assertEquals(SimTurnProcessor.BUG_BURN_PER_HOUR,
                SimTurnProcessor.polishBugBurn(100.0), EPS);
        // Few bugs => never burn more than remain.
        assertEquals(0.5, SimTurnProcessor.polishBugBurn(0.5), EPS);
        assertEquals(0.0, SimTurnProcessor.polishBugBurn(0.0), EPS);
    }

    @Test
    public void polishGainNeverOvershootsTarget() {
        assertEquals(SimTurnProcessor.POLISH_GAIN_PER_HOUR, SimTurnProcessor.polishGain(0.0), EPS);
        assertEquals(2.0, SimTurnProcessor.polishGain(98.0), EPS);
        assertEquals(0.0, SimTurnProcessor.polishGain(100.0), EPS);
    }

    @Test
    public void productionAccumulatesBugsAndPolishBurnsThemDown() {
        GameState state = scenarioWithDeskedEmployee(true);
        GameProject project = state.player().projects().get(0);
        project.setPhase(ProjectPhase.PRODUCTION);
        Employee emp = state.player().employees().get(0);
        emp.setStress(0.0);

        new SimTurnProcessor().processHour(state);
        assertTrue("bugs accumulate during PRODUCTION", project.bugs() > 0.0);

        // Switch to POLISH and confirm bugs fall and polish rises.
        double bugsAfterProduction = project.bugs();
        project.setPhase(ProjectPhase.POLISH);
        state.advanceHourCounter();
        new SimTurnProcessor().processHour(state);
        assertTrue("bugs burn down during POLISH", project.bugs() < bugsAfterProduction);
        assertTrue("polish rises during POLISH", project.polish() > 0.0);
    }

    // ----------------------------------------------------------------------------------------
    // Phase advancement & release
    // ----------------------------------------------------------------------------------------

    @Test
    public void phaseAdvancesAndReleaseEmitsInterrupts() {
        GameState state = scenarioWithDeskedEmployee(true);
        GameProject project = state.player().projects().get(0);
        // Push it to the brink of release: full points, no bugs, polish one tick away.
        project.setPhase(ProjectPhase.POLISH);
        project.addDevelopmentPoints(SimTurnProcessor.PRODUCTION_POINTS_THRESHOLD);
        project.addPolish(96.0);

        List<Interrupt> out = new ArrayList<Interrupt>();
        SimTurnProcessor sim = new SimTurnProcessor();
        for (int i = 0; i < 5 && project.phase() != ProjectPhase.RELEASED; i++) {
            out.addAll(sim.processHour(state));
            state.advanceHourCounter();
        }
        assertEquals(ProjectPhase.RELEASED, project.phase());
        assertTrue(hasType(out, InterruptType.DEVELOPMENT_MILESTONE));
        assertTrue(hasType(out, InterruptType.GAME_RELEASED));
        assertNotNull("released game must be scored", project.reviewScore());
    }

    // ----------------------------------------------------------------------------------------
    // Review scoring: range + bounded variance + monotonic-ish quality
    // ----------------------------------------------------------------------------------------

    @Test
    public void reviewScoreAlwaysInRange() {
        Random rng = new Random(12345L);
        for (int i = 0; i < 2000; i++) {
            double points = rng.nextDouble() * 1500.0;
            double bugs = rng.nextDouble() * 300.0;
            double polish = rng.nextDouble() * 100.0;
            int score = SimTurnProcessor.reviewScore(points, bugs, polish, rng);
            assertTrue("score >= 0 (was " + score + ")", score >= 0);
            assertTrue("score <= 100 (was " + score + ")", score <= 100);
        }
    }

    @Test
    public void reviewVarianceIsBounded() {
        // Pick a mid-range deterministic base so +/-10 stays strictly inside [0,100],
        // letting us observe the FULL variance window without clamping masking it.
        double points = SimTurnProcessor.POINTS_FOR_FULL_REVIEW * 0.5; // -> 35 points-component
        double polish = 50.0;                                          // -> 15 polish-component
        double bugs = 0.0;                                             // base == 50
        double base = 50.0;

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        Random rng = new Random(99L);
        for (int i = 0; i < 5000; i++) {
            int s = SimTurnProcessor.reviewScore(points, bugs, polish, rng);
            min = Math.min(min, s);
            max = Math.max(max, s);
        }
        assertTrue("variance must not exceed -" + SimTurnProcessor.REVIEW_RNG_VARIANCE,
                min >= base - SimTurnProcessor.REVIEW_RNG_VARIANCE);
        assertTrue("variance must not exceed +" + SimTurnProcessor.REVIEW_RNG_VARIANCE,
                max <= base + SimTurnProcessor.REVIEW_RNG_VARIANCE);
        // And the window is actually exercised (not a constant).
        assertTrue("expected observable spread", max - min >= SimTurnProcessor.REVIEW_RNG_VARIANCE);
    }

    @Test
    public void betterGamesUsuallyOutscoreWorseOnes() {
        Random rng = new Random(7L);
        int goodTotal = 0;
        int badTotal = 0;
        int n = 500;
        for (int i = 0; i < n; i++) {
            goodTotal += SimTurnProcessor.reviewScore(
                    SimTurnProcessor.POINTS_FOR_FULL_REVIEW, 0.0, 100.0, rng);
            badTotal += SimTurnProcessor.reviewScore(50.0, 200.0, 5.0, rng);
        }
        assertTrue("polished bug-free games should beat sloppy ones on average",
                goodTotal > badTotal);
    }

    @Test
    public void moreBugsLowerTheScore() {
        // Same RNG sequence, so the only difference is the bug penalty.
        int clean = SimTurnProcessor.reviewScore(500.0, 0.0, 80.0, new Random(3L));
        int buggy = SimTurnProcessor.reviewScore(500.0, 150.0, 80.0, new Random(3L));
        assertTrue("bugs must not raise the score", buggy <= clean);
    }

    // ----------------------------------------------------------------------------------------
    // Determinism & competitors
    // ----------------------------------------------------------------------------------------

    @Test
    public void sameSeedReplaysIdentically() {
        long releasesA = runCompetitorsAndCountReleases(42L);
        long releasesB = runCompetitorsAndCountReleases(42L);
        assertEquals("identical seed must replay identically", releasesA, releasesB);
    }

    @Test
    public void competitorsProgressOverTime() {
        GameState state = GameState.newGame(2024L);
        SimTurnProcessor sim = new SimTurnProcessor();
        for (int i = 0; i < 2000; i++) {
            sim.processHour(state);
            state.advanceHourCounter();
        }
        int totalReleased = 0;
        long totalCash = 0L;
        for (AiStudio ai : state.competitors()) {
            totalReleased += ai.releasedGames();
            totalCash += ai.cash();
        }
        assertTrue("competitors should ship games over time", totalReleased > 0);
        assertTrue("competitors should earn cash from releases", totalCash > 0L);
    }

    // ----------------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------------

    private long runCompetitorsAndCountReleases(long seed) {
        GameState state = GameState.newGame(seed);
        SimTurnProcessor sim = new SimTurnProcessor();
        for (int i = 0; i < 500; i++) {
            sim.processHour(state);
            state.advanceHourCounter();
        }
        long total = 0L;
        for (AiStudio ai : state.competitors()) {
            total += ai.releasedGames();
        }
        return total;
    }

    /** Build a minimal state: one DEVELOPMENT room with a desk, one employee, one project. */
    private GameState scenarioWithDeskedEmployee(boolean withDesk) {
        GameStudio player = new GameStudio("Test Studio");
        FloorPlan plan = player.floorPlan();
        Room dev = new Room(RoomType.DEVELOPMENT, 0, 0, 5, 5);
        plan.addRoom(dev);

        GridPos deskPos = GridPos.of(1, 1);
        if (withDesk) {
            plan.placeFacility(Facility.at(FacilityType.DESK, deskPos));
        }

        Employee emp = new Employee("e1", "Dev One", 50);
        if (withDesk) {
            emp.assignWorkstation(deskPos);
        }
        emp.assignProject("p1");
        player.employees().add(emp);

        GameProject project = new GameProject("p1", "Test Game");
        player.projects().add(project);

        List<AiStudio> ais = new ArrayList<AiStudio>();
        return new GameState(player, ais, 1L);
    }

    private static boolean hasType(List<Interrupt> interrupts, InterruptType type) {
        for (Interrupt i : interrupts) {
            if (i.type() == type) {
                return true;
            }
        }
        return false;
    }
}
