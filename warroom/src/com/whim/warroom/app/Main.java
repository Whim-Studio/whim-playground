package com.whim.warroom.app;

import com.whim.warroom.domain.Biome;
import com.whim.warroom.domain.Faction;
import com.whim.warroom.domain.MapMarker;
import com.whim.warroom.domain.MapState;
import com.whim.warroom.domain.Route;
import com.whim.warroom.domain.SandboxState;
import com.whim.warroom.domain.SimEngine;
import com.whim.warroom.domain.Stance;
import com.whim.warroom.domain.Unit;
import com.whim.warroom.domain.UnitCatalog;
import com.whim.warroom.domain.UnitType;
import com.whim.warroom.domain.Vec2;
import com.whim.warroom.domain.Waypoint;
import com.whim.warroom.engine.SimEngineImpl;
import com.whim.warroom.ui.BattlefieldPanel;
import com.whim.warroom.ui.EditorPanel;
import com.whim.warroom.ui.PlaybackBar;
import com.whim.warroom.ui.SandboxController;
import com.whim.warroom.ui.WarRoomFrame;

import javax.swing.SwingUtilities;
import java.awt.Color;

/**
 * EDT bootstrap. Builds a small, interesting demo scenario (generated terrain +
 * a few BLUE vs RED units with drawn routes and a synchronized detonation),
 * constructs the {@link SimEngineImpl}, wires the controller and panels, and
 * shows the {@link WarRoomFrame}.
 */
public final class Main {

    private static final long DEMO_SEED = 20260701L;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { launch(); }
        });
    }

    private static void launch() {
        MapState map = MapState.generate(40, 28, DEMO_SEED, Biome.GRASSLAND);
        SandboxState state = new SandboxState(map, DEMO_SEED);

        buildDemoScenario(state);

        SimEngine engine = new SimEngineImpl();
        SandboxController ctl = new SandboxController(state, engine);

        BattlefieldPanel field = new BattlefieldPanel(ctl);
        EditorPanel editor = new EditorPanel(ctl);
        PlaybackBar bar = new PlaybackBar(ctl);
        WarRoomFrame frame = new WarRoomFrame(ctl, field, editor, bar);
        ctl.attach(frame, field, editor, bar);

        bar.syncFromEngine();
        frame.setVisible(true);
        field.fitToMap();
    }

    /** A few units per side with routes converging on a central objective. */
    private static void buildDemoScenario(SandboxState state) {
        MapState map = state.getMap();
        double w = map.worldWidth(), h = map.worldHeight();
        double cx = w / 2, cy = h / 2;

        state.getMarkers().add(new MapMarker(new Vec2(cx, cy), "Objective", new Color(240, 220, 120)));

        UnitType blueMelee = pick("knight", "roman-legionary");
        UnitType blueRanged = pick("longbowman", "archer");
        UnitType redMelee = pick("man-at-arms", "greek-hoplite");
        UnitType redRanged = pick("rifle-infantry", "mg-team");
        UnitType heavy = pick("mbt", "knight");

        // BLUE advances from the left toward the objective.
        addWithRoute(state, blueMelee, Faction.BLUE, Stance.OFFENSIVE,
                new Vec2(w * 0.12, cy - 60), new Vec2[]{ new Vec2(cx - 40, cy - 30), new Vec2(cx, cy) }, false);
        addWithRoute(state, blueMelee, Faction.BLUE, Stance.OFFENSIVE,
                new Vec2(w * 0.12, cy + 60), new Vec2[]{ new Vec2(cx - 40, cy + 30), new Vec2(cx, cy + 10) }, false);
        addWithRoute(state, blueRanged, Faction.BLUE, Stance.DEFENSIVE,
                new Vec2(w * 0.08, cy), new Vec2[]{ new Vec2(w * 0.30, cy) }, false);

        // RED holds/defends from the right, one unit charges the center with a detonation.
        addWithRoute(state, redMelee, Faction.RED, Stance.DEFENSIVE,
                new Vec2(w * 0.88, cy - 50), new Vec2[]{ new Vec2(cx + 40, cy - 25) }, false);
        addWithRoute(state, redRanged, Faction.RED, Stance.DEFENSIVE,
                new Vec2(w * 0.90, cy + 50), new Vec2[]{ new Vec2(cx + 60, cy + 30) }, false);
        addWithRoute(state, heavy, Faction.RED, Stance.OFFENSIVE,
                new Vec2(w * 0.92, cy), new Vec2[]{ new Vec2(cx + 20, cy) }, true);
    }

    private static UnitType pick(String preferred, String fallback) {
        UnitType t = UnitCatalog.byId(preferred);
        if (t == null) t = UnitCatalog.byId(fallback);
        if (t == null && !UnitCatalog.all().isEmpty()) t = UnitCatalog.all().get(0);
        return t;
    }

    private static void addWithRoute(SandboxState state, UnitType type, Faction fac, Stance stance,
                                     Vec2 start, Vec2[] waypoints, boolean lastIsDetonation) {
        if (type == null) return;
        Unit u = new Unit(state.nextUnitId(), type, fac, start);
        u.setStance(stance);
        Route route = new Route();
        double spd = Math.max(1.0, type.getSpeed());
        Vec2 prev = start;
        int tick = 0;
        for (int i = 0; i < waypoints.length; i++) {
            Vec2 p = waypoints[i];
            int dt = (int) Math.ceil(prev.dist(p) / spd * SimEngine.TICKS_PER_SECOND);
            tick += Math.max(1, dt);
            Waypoint wp = new Waypoint(p, tick);
            if (lastIsDetonation && i == waypoints.length - 1) {
                wp.setDetonation(true);
                wp.setBlastRadius(70);
                wp.setBlastDamage(60);
            }
            route.add(wp);
            prev = p;
        }
        u.setRoute(route);
        state.addUnit(u);
    }
}
