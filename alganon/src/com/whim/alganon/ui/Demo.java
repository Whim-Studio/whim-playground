package com.whim.alganon.ui;

import com.whim.alganon.api.Enums.Direction;
import com.whim.alganon.api.Enums.GameStateType;
import com.whim.alganon.api.GameController;
import com.whim.alganon.api.Views;
import com.whim.alganon.ui.screens.CreationScreen;
import com.whim.alganon.ui.screens.HudScreen;
import com.whim.alganon.ui.screens.OverlayPanel;
import com.whim.alganon.ui.screens.TitleScreen;
import com.whim.alganon.ui.stub.StubController;

import javax.swing.JComponent;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

/**
 * Standalone entry point for the UI built on the {@link StubController} — no engine required.
 * With a display it opens the real {@link GameFrame}. Headless (e.g. CI containers) it instead
 * walks the whole title → wizard → HUD → every overlay state machine and paints each screen into
 * an off-screen image, proving all panels render without throwing. Task 1/2 supply the real
 * controller in {@code com.whim.alganon.app.Main}; this class only wires the stub.
 */
public final class Demo {
    private Demo() {}

    public static void main(String[] args) {
        StubController controller = new StubController();
        if (!GraphicsEnvironment.isHeadless() && !hasFlag(args, "--headless")) {
            GameFrame.launch(controller);
            return;
        }
        headlessSmoke(controller);
    }

    private static boolean hasFlag(String[] args, String f) {
        for (String a : args) if (f.equals(a)) return true;
        return false;
    }

    /** Drive the controller through every state and paint each screen off-screen. */
    private static void headlessSmoke(StubController c) {
        TitleScreen title = new TitleScreen(c);
        CreationScreen creation = new CreationScreen(c);
        HudScreen hud = new HudScreen(c);
        OverlayPanel overlay = new OverlayPanel(c);

        BufferedImage img = new BufferedImage(1180, 760, BufferedImage.TYPE_INT_ARGB);

        // TITLE
        paint(title, img);
        require(c.state().state() == GameStateType.TITLE, "starts on TITLE");

        // creation wizard
        c.beginCreation();
        creation.refresh(); paint(creation, img);
        c.chooseRace(c.state().creation().races().get(0).id);
        creation.refresh(); paint(creation, img);
        c.chooseFamily(c.state().creation().familiesFor(c.state().creation().selectedRaceId()).get(0).id);
        creation.refresh(); paint(creation, img);
        c.chooseClass(c.state().creation().classes().get(3).id.name().toLowerCase()); // Magus (schools)
        creation.refresh(); paint(creation, img);
        c.setName("Kaelen");
        c.finishCreation();
        require(c.state().state() == GameStateType.PLAYING, "reaches PLAYING");
        require(c.state().player() != null, "has a player");
        require(c.state().world() != null, "has a world");

        // HUD
        paint(hud, img);

        // move + ability
        c.move(Direction.EAST); c.move(Direction.SOUTH);
        Views.CharacterView p = c.state().player();
        require(!p.abilities().isEmpty(), "player has abilities");
        c.useAbility(p.abilities().get(0).id(), -1);
        c.tickDemo(0.5);
        paint(hud, img);

        // every overlay state
        GameStateType[] overlays = {
                GameStateType.INVENTORY, GameStateType.CHARACTER_SHEET, GameStateType.QUEST_LOG,
                GameStateType.STUDY, GameStateType.CRAFTING, GameStateType.AUCTION,
                GameStateType.FAMILY, GameStateType.LIBRARY, GameStateType.SETTINGS};
        for (GameStateType st : overlays) {
            c.openState(st);
            require(c.state().state() == st, "opened " + st);
            paint(overlay, img);
            c.closeOverlay();
        }

        System.out.println("UI stub smoke OK: title, wizard, HUD, and " + overlays.length + " overlays rendered.");
    }

    private static void paint(JComponent comp, BufferedImage img) {
        comp.setSize(img.getWidth(), img.getHeight());
        java.awt.Graphics2D g = img.createGraphics();
        try { comp.paint(g); } finally { g.dispose(); }
    }

    private static void require(boolean cond, String what) {
        if (!cond) throw new IllegalStateException("smoke check failed: " + what);
    }
}
