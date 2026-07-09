package com.whim.alganon.study;

import com.whim.alganon.api.ActionResult;
import com.whim.alganon.api.CharacterModel;
import com.whim.alganon.api.Enums.ChatChannel;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.GameContext;
import com.whim.alganon.api.GameModel;

/**
 * The offline "Study" progression system (Alganon's signature hybrid mechanic).
 *
 * <p>Model [Anchor for the concept; Gap for the numbers]: the player assigns one skill
 * to study. While the game is closed, that skill banks progress at a fixed rate per real
 * hour, capped at {@link #CAP_HOURS} so idle play cannot outpace active play. On load the
 * engine calls {@link #grantOffline} with the real elapsed millis since the last save.</p>
 *
 * <p>Banked fractional progress lives on the model ({@code bankedStudyProgress}); whole
 * points are flushed into the skill via {@code addSkillProgress}.</p>
 */
public final class StudySystem {

    /** Offline accrual cap: leaving the game closed longer than this banks nothing extra. */
    public static final double CAP_HOURS = 8.0;
    /** Skill points banked per real hour of offline study. [Gap — my design] */
    public static final double POINTS_PER_HOUR = 6.0;
    /** Study slots in v1. [Anchor] one to start. */
    public static final int SLOTS = 1;
    /** Skill points granted per point of active use (see {@link #onUse}). [Gap] */
    private static final double USE_GAIN = 0.25;

    private final GameContext ctx;

    public StudySystem(GameContext ctx) {
        this.ctx = ctx;
    }

    public ActionResult assign(GameModel model, SkillType skill) {
        if (skill == null) return ActionResult.fail("Pick a skill to study.");
        model.player().setStudyAssignment(skill);
        ctx.log(ChatChannel.SYSTEM, "Now studying " + label(skill) + " while away.");
        return ActionResult.ok("Studying " + label(skill) + ".");
    }

    public ActionResult clear(GameModel model) {
        model.player().setStudyAssignment(null);
        ctx.log(ChatChannel.SYSTEM, "Study assignment cleared.");
        return ActionResult.ok("Study cleared.");
    }

    /**
     * Grant banked offline progress. {@code elapsedMillis} is real wall-clock time since
     * the last save; it is clamped to the {@link #CAP_HOURS} cap.
     */
    public void grantOffline(GameModel model, long elapsedMillis) {
        CharacterModel p = model.player();
        SkillType skill = p.studyAssignment();
        if (skill == null || elapsedMillis <= 0) return;

        double hours = elapsedMillis / 3_600_000.0;
        double capped = Math.min(hours, CAP_HOURS);
        double gained = capped * POINTS_PER_HOUR;
        if (gained <= 0) return;

        double banked = p.bankedStudyProgress() + gained;
        int whole = (int) Math.floor(banked);
        double frac = banked - whole;
        if (whole > 0) p.addSkillProgress(skill, whole);
        p.setBankedStudyProgress(frac);

        ctx.log(ChatChannel.SYSTEM, String.format(
                "Offline study: %.1f banked hour(s) of %s granted %d skill point(s).",
                capped, label(skill), whole));
        ctx.toast("Welcome back! Study granted " + whole + " " + label(skill) + " point(s).");
    }

    /** Active use-based skill gain — call when the player exercises a skill line. */
    public void onUse(GameModel model, SkillType skill, double amount) {
        if (skill == null) return;
        CharacterModel p = model.player();
        double banked = p.bankedStudyProgress();
        // Use-gain shares the banked accumulator only when it targets the studied skill;
        // otherwise it converts directly into skill points. [Gap — my design]
        double points = amount * USE_GAIN;
        if (points >= 1.0) {
            p.addSkillProgress(skill, (int) Math.floor(points));
        } else if (ctx.rng().nextDouble() < points) {
            p.addSkillProgress(skill, 1);
        }
        // keep banked untouched here; banked is reserved for offline study.
        if (banked < 0) p.setBankedStudyProgress(0);
    }

    /** 0..1 progress toward the studied skill's next point (from banked fraction). */
    public double progressToNextPoint(GameModel model) {
        return Math.max(0.0, Math.min(1.0, model.player().bankedStudyProgress()));
    }

    public double bankedHours(GameModel model) {
        // Report the currently-banked fraction expressed back in hours-equivalent.
        return model.player().bankedStudyProgress() / POINTS_PER_HOUR;
    }

    private static String label(SkillType s) {
        String n = s.name().toLowerCase();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }
}
