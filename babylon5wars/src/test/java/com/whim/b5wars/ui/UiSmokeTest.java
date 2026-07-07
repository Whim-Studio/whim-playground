package com.whim.b5wars.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.whim.b5wars.data.DataLoader;
import com.whim.b5wars.engine.TurnPhase;
import com.whim.b5wars.model.Faction;
import com.whim.b5wars.model.Scenario;

import java.util.List;

import javax.swing.JPanel;

import org.junit.Test;

/**
 * Headless sanity: build the controller from the bundled scenario and instantiate every Swing
 * <em>panel</em> (all {@link JPanel}s — headless-safe, unlike a {@code JFrame}) WITHOUT ever
 * calling {@code setVisible}. Also drives one phase advance to confirm the panels tolerate state
 * changes. Runs under {@code -Djava.awt.headless=true}; no display is required.
 */
public final class UiSmokeTest {

    private static final String SCENARIO = "/scenarios/border-skirmish.json";

    @Test
    public void panelsBuildAndReactHeadless() {
        List<Faction> factions = DataLoader.loadFactions();
        Scenario scenario = DataLoader.loadScenario(SCENARIO);
        GameController controller = new GameController(scenario, factions, 424242L);

        assertFalse("scenario should place ships", controller.state().getShips().isEmpty());
        assertNotNull("a ship should be selected by default", controller.selectedShip());

        // Instantiate all panels (no frame, no setVisible).
        JPanel play = new PlayAreaPanel(controller);
        JPanel sheet = new ShipSheetPanel(controller);
        JPanel turnBar = new TurnBarPanel(controller);
        JPanel powerEw = new PowerEwPanel(controller);
        JPanel log = new LogPanel();
        controller.addListener((GameListener) log);
        assertNotNull(play);
        assertNotNull(sheet);
        assertNotNull(turnBar);
        assertNotNull(powerEw);
        assertNotNull(log);

        // Drive the FSM one step: INITIATIVE -> POWER, and confirm listeners were notified safely.
        assertTrue(controller.state().getPhase() == TurnPhase.INITIATIVE);
        controller.advancePhase();
        assertTrue("phase should advance past INITIATIVE",
                controller.state().getPhase() != TurnPhase.INITIATIVE);
    }
}
