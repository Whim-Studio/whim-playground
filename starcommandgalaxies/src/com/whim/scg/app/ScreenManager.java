package com.whim.scg.app;

import com.whim.scg.api.Enums;
import com.whim.scg.api.GameController;
import com.whim.scg.api.Screen;

import java.util.EnumMap;
import java.util.Map;

/**
 * Owns the Screen registry. Screens are discovered REFLECTIVELY by the fixed
 * class names in the CONTRACT, so the shell compiles and runs before the UI
 * tasks land their screens — a missing screen falls back to a Placeholder that
 * explains what belongs there. Every screen must expose a public constructor
 * taking a single {@link GameController}.
 */
public final class ScreenManager {

    /** mode -> concrete screen class name (see CONTRACT "Screen ownership"). */
    private static final Map<Enums.Mode, String> CLASSES = new EnumMap<Enums.Mode, String>(Enums.Mode.class);
    static {
        CLASSES.put(Enums.Mode.MENU,          "com.whim.scg.app.MenuScreen");
        CLASSES.put(Enums.Mode.SHIP_INTERIOR, "com.whim.scg.ui.ship.ShipInteriorScreen");
        CLASSES.put(Enums.Mode.GALAXY_MAP,    "com.whim.scg.ui.galaxy.GalaxyMapScreen");
        CLASSES.put(Enums.Mode.STARPORT,      "com.whim.scg.ui.galaxy.StarportScreen");
        CLASSES.put(Enums.Mode.SPACE_COMBAT,  "com.whim.scg.ui.combat.SpaceCombatScreen");
        CLASSES.put(Enums.Mode.BOARDING,      "com.whim.scg.ui.boarding.BoardingScreen");
        CLASSES.put(Enums.Mode.GAME_OVER,     "com.whim.scg.app.EndScreen");
        CLASSES.put(Enums.Mode.VICTORY,       "com.whim.scg.app.EndScreen");
    }

    private final Map<Enums.Mode, Screen> screens = new EnumMap<Enums.Mode, Screen>(Enums.Mode.class);
    private final GameController controller;
    private Screen active;

    public ScreenManager(GameController controller) {
        this.controller = controller;
        for (Enums.Mode m : Enums.Mode.values()) {
            screens.put(m, build(m));
        }
    }

    private Screen build(Enums.Mode mode) {
        String cn = CLASSES.get(mode);
        try {
            Class<?> c = Class.forName(cn);
            return (Screen) c.getConstructor(GameController.class).newInstance(controller);
        } catch (Throwable t) {
            return new PlaceholderScreen(mode, cn);
        }
    }

    /** Return the screen matching the controller's current mode, switching if needed. */
    public Screen current() {
        Enums.Mode m = controller.view().mode();
        Screen next = screens.get(m);
        if (next != active) {
            if (active != null) active.onExit();
            active = next;
            active.onEnter();
        }
        return active;
    }
}
