package com.whim.scg.api;

import javax.swing.event.ChangeListener;

/**
 * THE single UI-to-engine seam. The engine (Task 1) provides the concrete
 * implementation named exactly {@code com.whim.scg.engine.Engine} with a public
 * no-argument constructor; the app shell instantiates it reflectively and falls
 * back to a stub if it is not yet on the classpath. UI tasks call these intents
 * and re-read {@link Views.GameView} after each; they never mutate model state
 * directly.
 *
 * All intents are tolerant: an illegal request returns {@link ActionResult#fail}
 * rather than throwing.
 */
public interface GameController {

    // ---- lifecycle / global ----
    Views.GameView view();
    void addChangeListener(ChangeListener l);
    void newGame(String captainName, String shipName);
    ActionResult save(String slot);
    ActionResult load(String slot);
    void setMode(Enums.Mode mode);

    /** Advance simulation by dtSeconds. The shell calls this each Swing tick. */
    void tick(double dtSeconds);

    /** Real-time-with-pause toggle (space bar). */
    void togglePause();

    // ---- crew (ship interior + roster) ----
    ActionResult assignCrew(int crewId, int roomId);      // drag crew to a room
    ActionResult renameCrew(int crewId, String name);
    ActionResult setRole(int crewId, Enums.CrewRole role);

    // ---- power management ----
    ActionResult setRoomPower(int roomId, int power);
    ActionResult setWeaponPower(int slot, int power);

    // ---- galaxy / navigation ----
    ActionResult jumpTo(int systemId);                    // fly to a linked system
    ActionResult scanSystem();
    ActionResult resolveEvent(int choiceIndex);           // pick an event option
    ActionResult dock();                                  // enter starport

    // ---- economy / tech (starport) ----
    ActionResult repairAll();
    ActionResult buyTech(Enums.TechType type);
    ActionResult recruitCrew();
    ActionResult undock();

    // ---- space combat ----
    ActionResult setWeaponTarget(int slot, int enemyRoomId);
    ActionResult fireWeapon(int slot);                    // manual fire when charged
    ActionResult beginBoarding();                         // teleport a boarding party
    ActionResult fleeCombat();

    // ---- boarding / away mission ----
    ActionResult selectBoarder(int crewId);
    ActionResult moveBoarder(int crewId, GridPos to);
    ActionResult boarderAttack(int crewId, int targetCrewId);
    ActionResult endBoarding();                           // recall survivors
}
