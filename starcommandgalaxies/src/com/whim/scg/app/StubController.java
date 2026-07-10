package com.whim.scg.app;

import com.whim.scg.api.ActionResult;
import com.whim.scg.api.Enums;
import com.whim.scg.api.GameController;
import com.whim.scg.api.GridPos;
import com.whim.scg.api.Views;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fallback controller used when the real engine ({@code com.whim.scg.engine.Engine})
 * is not on the classpath yet. Lets the shell launch and navigate between the
 * placeholder screens so integration is trivial once Task 1 lands.
 */
public final class StubController implements GameController {
    private Enums.Mode mode = Enums.Mode.MENU;
    private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();
    private final List<String> log = Collections.singletonList("[stub] engine not loaded");

    private void fire() {
        for (ChangeListener l : listeners) l.stateChanged(new ChangeEvent(this));
    }

    private static final ActionResult NO_ENGINE = ActionResult.fail("engine not loaded");

    @Override public Views.GameView view() {
        return new Views.GameView() {
            @Override public Enums.Mode mode() { return mode; }
            @Override public int credits() { return 0; }
            @Override public int day() { return 0; }
            @Override public Views.ShipView playerShip() { return null; }
            @Override public Views.GalaxyView galaxy() { return null; }
            @Override public Views.CombatView combat() { return null; }
            @Override public Views.BoardingView boarding() { return null; }
            @Override public List<Views.TechView> techTree() { return Collections.emptyList(); }
            @Override public List<String> log() { return log; }
            @Override public boolean paused() { return false; }
            @Override public String flash() { return ""; }
        };
    }

    @Override public void addChangeListener(ChangeListener l) { listeners.add(l); }
    @Override public void newGame(String captainName, String shipName) { /* no-op */ }
    @Override public ActionResult save(String slot) { return NO_ENGINE; }
    @Override public ActionResult load(String slot) { return NO_ENGINE; }
    @Override public void setMode(Enums.Mode m) { this.mode = m; fire(); }
    @Override public void tick(double dt) { }
    @Override public void togglePause() { }
    @Override public ActionResult assignCrew(int c, int r) { return NO_ENGINE; }
    @Override public ActionResult renameCrew(int c, String n) { return NO_ENGINE; }
    @Override public ActionResult setRole(int c, Enums.CrewRole r) { return NO_ENGINE; }
    @Override public ActionResult setRoomPower(int r, int p) { return NO_ENGINE; }
    @Override public ActionResult setWeaponPower(int s, int p) { return NO_ENGINE; }
    @Override public ActionResult jumpTo(int s) { return NO_ENGINE; }
    @Override public ActionResult scanSystem() { return NO_ENGINE; }
    @Override public ActionResult resolveEvent(int i) { return NO_ENGINE; }
    @Override public ActionResult dock() { return NO_ENGINE; }
    @Override public ActionResult repairAll() { return NO_ENGINE; }
    @Override public ActionResult buyTech(Enums.TechType t) { return NO_ENGINE; }
    @Override public ActionResult recruitCrew() { return NO_ENGINE; }
    @Override public ActionResult undock() { return NO_ENGINE; }
    @Override public ActionResult setWeaponTarget(int s, int r) { return NO_ENGINE; }
    @Override public ActionResult fireWeapon(int s) { return NO_ENGINE; }
    @Override public ActionResult beginBoarding() { return NO_ENGINE; }
    @Override public ActionResult fleeCombat() { return NO_ENGINE; }
    @Override public ActionResult selectBoarder(int c) { return NO_ENGINE; }
    @Override public ActionResult moveBoarder(int c, GridPos to) { return NO_ENGINE; }
    @Override public ActionResult boarderAttack(int c, int t) { return NO_ENGINE; }
    @Override public ActionResult endBoarding() { return NO_ENGINE; }
}
