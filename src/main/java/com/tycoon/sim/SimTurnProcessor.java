package com.tycoon.sim;

import com.tycoon.core.AiStudio;
import com.tycoon.core.Employee;
import com.tycoon.core.Facility;
import com.tycoon.core.FacilityType;
import com.tycoon.core.FloorPlan;
import com.tycoon.core.GameProject;
import com.tycoon.core.GameState;
import com.tycoon.core.GameStudio;
import com.tycoon.core.GenreTopicMatch;
import com.tycoon.core.GridPos;
import com.tycoon.core.Interrupt;
import com.tycoon.core.InterruptType;
import com.tycoon.core.ProjectPhase;
import com.tycoon.core.Room;
import com.tycoon.core.RoomType;
import com.tycoon.core.TurnProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simulation / math engine for the turn-based Mad Games Tycoon adaptation.
 *
 * <p>Resolves exactly one in-game hour (one turn) for the player studio and all ~100 AI
 * competitors. Every source of randomness comes from {@link GameState#rng()} so a given seed
 * replays identically.</p>
 *
 * <p>The point-generation, stress, bug/polish and review-scoring math is exposed as pure static
 * helpers so it can be unit-tested without standing up a full {@link GameState}.</p>
 */
public final class SimTurnProcessor implements TurnProcessor {

    // ---- Tunable constants (binding ones are documented in the contract) ----------------------

    /** Stress at/above which an EMPLOYEE_CRISIS interrupt fires. */
    public static final double STRESS_CRISIS_THRESHOLD = 90.0;

    /** Baseline hourly stress accrual for an employee actively working a project. */
    public static final double BASE_STRESS_ACCRUAL = 4.0;

    /** Baseline workstation/room quality multiplier (mu_ws) for a plain DESK. */
    public static final double BASE_WORKSTATION_MULTIPLIER = 1.0;

    /** Extra mu_ws per non-desk facility in the employee's development room (capped). */
    public static final double WORKSTATION_FACILITY_BONUS = 0.05;

    /** Maximum mu_ws achievable from room facilities. */
    public static final double MAX_WORKSTATION_MULTIPLIER = 2.0;

    /** Cumulative development points needed to leave DESIGN for PRODUCTION. */
    public static final double DESIGN_POINTS_THRESHOLD = 100.0;

    /** Cumulative development points needed to leave PRODUCTION for POLISH. */
    public static final double PRODUCTION_POINTS_THRESHOLD = 600.0;

    /** Polish (0..100) needed to leave POLISH and RELEASE. */
    public static final double POLISH_TARGET = 100.0;

    /** Bugs generated per development point earned during PRODUCTION. */
    public static final double BUG_GENERATION_RATE = 0.05;

    /** Bugs removed per hour during POLISH. */
    public static final double BUG_BURN_PER_HOUR = 2.0;

    /** Polish gained per hour during POLISH. */
    public static final double POLISH_GAIN_PER_HOUR = 5.0;

    /** Bounded Reviewer-RNG variance amplitude: score gets +/- this many points. */
    public static final int REVIEW_RNG_VARIANCE = 10;

    /** Development points that map to a full points-component in the review base. */
    public static final double POINTS_FOR_FULL_REVIEW = PRODUCTION_POINTS_THRESHOLD;

    /** Per-hour probability that the competitor field produces a market shift. */
    public static final double MARKET_SHIFT_CHANCE = 0.01;

    /** Per-hour base probability that a single AI studio ships a game. */
    public static final double AI_RELEASE_BASE_CHANCE = 0.004;

    // ============================================================================================
    // Static math helpers (pure, unit-tested)
    // ============================================================================================

    /**
     * Binding point formula: {@code P_hourly = (S_base * mu_ws) * max(0.1, 1 - sigma_stress/100)}.
     * An employee with no DESK has {@code workstationMultiplier <= 0} and produces 0 points.
     */
    public static double hourlyPoints(int baseSkill, double workstationMultiplier, double stress) {
        if (workstationMultiplier <= 0.0) {
            return 0.0; // no DESK => no points
        }
        double s = clamp(stress, 0.0, 100.0);
        double stressFactor = Math.max(0.1, 1.0 - s / 100.0);
        return (baseSkill * workstationMultiplier) * stressFactor;
    }

    /** Net hourly stress delta BEFORE clamping: base accrual minus facility relief. */
    public static double hourlyStressDelta(double baseAccrual, double facilityRelief) {
        return baseAccrual - facilityRelief;
    }

    /** Bugs introduced this hour for the given development points earned during PRODUCTION. */
    public static double productionBugDelta(double pointsThisHour) {
        if (pointsThisHour <= 0.0) {
            return 0.0;
        }
        return pointsThisHour * BUG_GENERATION_RATE;
    }

    /** Bugs burned this hour during POLISH (never more than what remains). */
    public static double polishBugBurn(double currentBugs) {
        if (currentBugs <= 0.0) {
            return 0.0;
        }
        return Math.min(currentBugs, BUG_BURN_PER_HOUR);
    }

    /** Polish gained this hour during POLISH (never overshooting the target). */
    public static double polishGain(double currentPolish) {
        double remaining = POLISH_TARGET - currentPolish;
        if (remaining <= 0.0) {
            return 0.0;
        }
        return Math.min(remaining, POLISH_GAIN_PER_HOUR);
    }

    /**
     * Final review score in [0,100]. Base is built from development points (up to 70) and polish
     * (up to 30), penalized by accumulated bugs, then perturbed by a BOUNDED Reviewer-RNG variance
     * of +/- {@link #REVIEW_RNG_VARIANCE} so good games usually score well while charts stay
     * unpredictable.
     */
    public static int reviewScore(double developmentPoints, double bugs, double polish, Random rng) {
        return reviewScore(developmentPoints, bugs, polish, 1.0, 1.0, rng);
    }

    /**
     * Review score with the authentic Mad Games Tycoon quality modifiers applied. The bug-penalised
     * base (points + polish) is scaled by the genre/topic fit multiplier (see
     * {@link com.tycoon.core.GenreTopicMatch}) and the engine quality bonus (see
     * {@link com.tycoon.core.Technology}) BEFORE the bounded Reviewer-RNG variance, so a great
     * pairing on a modern engine usually outscores a mismatched one built on the same effort.
     */
    public static int reviewScore(double developmentPoints, double bugs, double polish,
                                  double matchMultiplier, double techMultiplier, Random rng) {
        double pointsComponent = 70.0 * clamp(developmentPoints / POINTS_FOR_FULL_REVIEW, 0.0, 1.0);
        double polishComponent = 30.0 * clamp(polish / 100.0, 0.0, 1.0);
        double base = pointsComponent + polishComponent;

        // Bugs erode the score; the penalty is bounded by the base so it can't drive things wild.
        double bugPenalty = Math.min(base, Math.max(0.0, bugs) * 0.5);
        double scored = base - bugPenalty;

        // Authentic fit/engine modifiers: a perfect genre/topic match on a modern engine lifts the
        // ceiling; a poor match drags it down. Applied multiplicatively to the (post-bug) quality.
        scored *= matchMultiplier;
        scored *= techMultiplier;

        // Bounded variance in [-REVIEW_RNG_VARIANCE, +REVIEW_RNG_VARIANCE].
        int variance = rng.nextInt(2 * REVIEW_RNG_VARIANCE + 1) - REVIEW_RNG_VARIANCE;
        scored += variance;

        return (int) Math.round(clamp(scored, 0.0, 100.0));
    }

    static double clamp(double v, double lo, double hi) {
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    }

    // ============================================================================================
    // Per-turn resolution
    // ============================================================================================

    @Override
    public List<Interrupt> processHour(GameState state) {
        List<Interrupt> interrupts = new ArrayList<Interrupt>();
        long hour = state.hour();

        processPlayer(state, hour, interrupts);
        processCompetitors(state, hour, interrupts);

        return interrupts;
    }

    private void processPlayer(GameState state, long hour, List<Interrupt> interrupts) {
        GameStudio player = state.player();
        FloorPlan plan = player.floorPlan();

        // --- Employees: produce points/bugs/polish into their assigned project, update stress. ---
        for (Employee emp : player.employees()) {
            String projectId = emp.assignedProjectId();
            if (projectId == null) {
                // Idle employees still slowly unwind their stress (relief in their room, if any).
                applyStress(emp, plan, 0.0, hour, interrupts);
                continue;
            }
            GameProject project = player.project(projectId);
            if (project == null || project.phase() == ProjectPhase.RELEASED) {
                applyStress(emp, plan, 0.0, hour, interrupts);
                continue;
            }

            double muWs = workstationMultiplier(emp, plan);
            double points = hourlyPoints(emp.baseSkill(), muWs, emp.stress());
            boolean working = muWs > 0.0;

            if (working && project.phase() == ProjectPhase.DESIGN) {
                project.addDevelopmentPoints(points);
            } else if (working && project.phase() == ProjectPhase.PRODUCTION) {
                project.addDevelopmentPoints(points);
                project.addBugs(productionBugDelta(points));
            } else if (working && project.phase() == ProjectPhase.POLISH) {
                project.addBugs(-polishBugBurn(project.bugs()));
                project.addPolish(polishGain(project.polish()));
            }

            // Only an employee actually at a desk accrues the full working stress.
            double accrual = working ? BASE_STRESS_ACCRUAL : 0.0;
            applyStress(emp, plan, accrual, hour, interrupts);
        }

        // --- Phase advancement for every live player project. ---
        for (GameProject project : player.projects()) {
            advancePhase(state, project, hour, interrupts);
        }
    }

    /** Apply hourly stress with facility relief, clamp [0,100], and fire crisis on threshold crossing. */
    private void applyStress(Employee emp, FloorPlan plan, double baseAccrual, long hour,
                             List<Interrupt> interrupts) {
        double relief = facilityRelief(emp, plan);
        // Idle employees don't accrue, but they still benefit from relief in their room.
        double delta = hourlyStressDelta(baseAccrual, relief);
        double oldStress = emp.stress();
        double newStress = clamp(oldStress + delta, 0.0, 100.0);
        emp.setStress(newStress);

        if (newStress >= STRESS_CRISIS_THRESHOLD && oldStress < STRESS_CRISIS_THRESHOLD) {
            interrupts.add(new Interrupt(InterruptType.EMPLOYEE_CRISIS, hour,
                    emp.name() + " is burning out (stress " + Math.round(newStress) + ")."));
        }
    }

    private void advancePhase(GameState state, GameProject project, long hour,
                              List<Interrupt> interrupts) {
        ProjectPhase phase = project.phase();
        if (phase == ProjectPhase.RELEASED) {
            return;
        }

        if (phase == ProjectPhase.DESIGN && project.developmentPoints() >= DESIGN_POINTS_THRESHOLD) {
            project.setPhase(ProjectPhase.PRODUCTION);
            interrupts.add(new Interrupt(InterruptType.DEVELOPMENT_MILESTONE, hour,
                    project.title() + " entered PRODUCTION."));
        } else if (phase == ProjectPhase.PRODUCTION
                && project.developmentPoints() >= PRODUCTION_POINTS_THRESHOLD) {
            project.setPhase(ProjectPhase.POLISH);
            interrupts.add(new Interrupt(InterruptType.DEVELOPMENT_MILESTONE, hour,
                    project.title() + " entered POLISH."));
        } else if (phase == ProjectPhase.POLISH && project.polish() >= POLISH_TARGET) {
            project.setPhase(ProjectPhase.RELEASED);
            double matchMult = GenreTopicMatch.multiplier(project.genre(), project.topic());
            double techMult = project.technology() != null ? project.technology().qualityBonus() : 1.0;
            int score = reviewScore(project.developmentPoints(), project.bugs(), project.polish(),
                    matchMult, techMult, state.rng());
            project.setReviewScore(score);
            interrupts.add(new Interrupt(InterruptType.DEVELOPMENT_MILESTONE, hour,
                    project.title() + " finished POLISH."));
            String fit = project.genre() != null && project.topic() != null
                    ? " [" + project.genre().display() + " / " + project.topic().display()
                          + " — " + project.matchRating().label() + "]"
                    : "";
            interrupts.add(new Interrupt(InterruptType.GAME_RELEASED, hour,
                    project.title() + " released to a review score of " + score + "/100." + fit));
        }
    }

    // ---- Floor-plan derived inputs -------------------------------------------------------------

    /**
     * mu_ws for an employee: 0 if they have no DESK at all, otherwise a baseline of
     * {@link #BASE_WORKSTATION_MULTIPLIER} plus a small bonus per non-desk facility in the room the
     * desk sits in, capped at {@link #MAX_WORKSTATION_MULTIPLIER}.
     */
    static double workstationMultiplier(Employee emp, FloorPlan plan) {
        GridPos desk = emp.workstation();
        if (desk == null) {
            return 0.0; // no DESK => no points
        }
        Facility f = plan.facilityAt(desk);
        if (f == null || f.type() != FacilityType.DESK) {
            return 0.0; // assigned cell isn't actually a workstation
        }
        double mult = BASE_WORKSTATION_MULTIPLIER;
        Room room = plan.roomAt(desk);
        if (room != null) {
            int bonusFacilities = 0;
            for (Facility rf : room.facilities()) {
                if (rf.type() != FacilityType.DESK) {
                    bonusFacilities++;
                }
            }
            if (room.type() == RoomType.DEVELOPMENT) {
                mult += WORKSTATION_FACILITY_BONUS * bonusFacilities;
            }
        }
        return clamp(mult, 0.0, MAX_WORKSTATION_MULTIPLIER);
    }

    /** Sum of stressRelief() for stress facilities in the room containing the employee's desk. */
    static double facilityRelief(Employee emp, FloorPlan plan) {
        GridPos desk = emp.workstation();
        if (desk == null) {
            return 0.0;
        }
        Room room = plan.roomAt(desk);
        if (room == null) {
            return 0.0;
        }
        double relief = 0.0;
        for (Facility f : room.facilities()) {
            relief += f.type().stressRelief();
        }
        return relief;
    }

    // ============================================================================================
    // AI competitors
    // ============================================================================================

    private void processCompetitors(GameState state, long hour, List<Interrupt> interrupts) {
        Random rng = state.rng();
        List<AiStudio> competitors = state.competitors();
        boolean marketShiftFired = false;

        for (AiStudio ai : competitors) {
            // Stronger studios ship more often and score higher.
            double releaseChance = AI_RELEASE_BASE_CHANCE * (0.5 + ai.strength());
            if (rng.nextDouble() < releaseChance) {
                int score = aiReviewScore(ai.strength(), rng);
                ai.setLastReviewScore(score);
                ai.incrementReleasedGames();
                // Revenue scales with quality; even a flop earns a little.
                long revenue = (long) (1000L + score * 200L);
                ai.addCash(revenue);

                // A breakout hit upends the charts.
                if (!marketShiftFired && score >= 90) {
                    interrupts.add(new Interrupt(InterruptType.MARKET_SHIFT, hour,
                            ai.name() + " topped the charts with a " + score + "/100 hit."));
                    marketShiftFired = true;
                }
            }
        }

        // A baseline, rare market wobble independent of any single release.
        if (!marketShiftFired && rng.nextDouble() < MARKET_SHIFT_CHANCE) {
            interrupts.add(new Interrupt(InterruptType.MARKET_SHIFT, hour,
                    "Player tastes shifted across the market."));
        }
    }

    /** AI review score from strength with the same bounded-variance idea, using state.rng(). */
    static int aiReviewScore(double strength, Random rng) {
        double base = clamp(strength, 0.0, 1.0) * 100.0;
        int variance = rng.nextInt(2 * REVIEW_RNG_VARIANCE + 1) - REVIEW_RNG_VARIANCE;
        return (int) Math.round(clamp(base + variance, 0.0, 100.0));
    }
}
