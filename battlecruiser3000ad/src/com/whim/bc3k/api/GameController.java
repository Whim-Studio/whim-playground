package com.whim.bc3k.api;

/**
 * THE single UI-to-engine seam. The engine provides the concrete implementation
 * named exactly {@code com.whim.bc3k.engine.Engine} with a public no-argument
 * constructor.
 *
 * All intents are tolerant: an illegal request returns {@link ActionResult#fail}
 * rather than throwing.
 */
public interface GameController {

    // ---- lifecycle / global ----
    Views.GameView view();
    void newGame(Enums.GameMode mode, String shipName);
    void setMode(Enums.Mode mode);
    void tick(double dtSeconds);
    void togglePause();

    // ---- save / load ----
    ActionResult save(String slot);
    ActionResult load(String slot);
    boolean hasSave(String slot);

    // ---- power management (PWR console) ----
    ActionResult setPower(Enums.PowerSystem s, int delta);

    // ---- engineering ----
    ActionResult restartReactor();               // Shift+R (verified original)
    ActionResult requestTow();                    // Ctrl+S (verified original)
    ActionResult repair(Enums.PowerSystem s);     // repair one subsystem

    // ---- navigation (NAV console) ----
    ActionResult jumpTo(int systemId);            // jump to a linked system

    // ---- personnel (PERSONNEL console) ----
    ActionResult orderCrew(int crewId, Enums.CrewOrder order);
    ActionResult cloneCrew();                      // clone from stored DNA

    // ---- combat (TACTICAL console, Xtreme Carnage) ----
    ActionResult fireWeapons();                    // fire a volley at the enemy

    // ---- ground combat (FLIGHT DECK -> GROUND screen) ----
    ActionResult deployAtv();                      // start a planetary ground skirmish
    ActionResult assaultGround();                  // order a ground assault burst

    // ---- logistics (CARGO console) ----
    ActionResult refuel();                         // refuel at a starstation

    // ---- comms (COMMS console) ----
    ActionResult hail();                           // hail the nearest station
    ActionResult resolveObjective();               // mark the campaign objective complete

    // ---- flight deck (FLIGHTDECK console) ----
    ActionResult launchCraft(Enums.CraftType type);
    ActionResult recallCraft(Enums.CraftType type);
}
