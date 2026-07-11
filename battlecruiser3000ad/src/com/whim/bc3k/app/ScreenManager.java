package com.whim.bc3k.app;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.api.GameController;
import com.whim.bc3k.api.Screen;

import java.util.EnumMap;
import java.util.Map;

/**
 * Owns the Screen registry and switches the active screen to match the
 * controller's current mode. Phase 2 builds every screen directly; Phase 3+ can
 * swap a richer console in for any {@link Enums.Mode} without touching the shell.
 */
public final class ScreenManager {

    /** Bridge consoles in function-key order (F1..F8). Shared by the tab bar and GameFrame. */
    public static final Enums.Mode[] CONSOLES = {
        Enums.Mode.NAV, Enums.Mode.TACTICAL, Enums.Mode.ENGINEERING, Enums.Mode.POWER,
        Enums.Mode.COMMS, Enums.Mode.CARGO, Enums.Mode.PERSONNEL, Enums.Mode.FLIGHTDECK
    };

    private final Map<Enums.Mode, Screen> screens = new EnumMap<Enums.Mode, Screen>(Enums.Mode.class);
    private final GameController controller;
    private Screen active;

    public ScreenManager(GameController controller) {
        this.controller = controller;
        screens.put(Enums.Mode.MENU, new MenuScreen(controller));
        screens.put(Enums.Mode.GAME_OVER, new EndScreen(controller));
        for (Enums.Mode m : CONSOLES) {
            screens.put(m, new ConsoleScreen(controller, m));
        }
    }

    /** Return the screen matching the controller's current mode, switching if needed. */
    public Screen current() {
        Enums.Mode m = controller.view().mode();
        Screen next = screens.get(m);
        if (next == null) next = screens.get(Enums.Mode.MENU);
        if (next != active) {
            if (active != null) active.onExit();
            active = next;
            active.onEnter();
        }
        return active;
    }
}
