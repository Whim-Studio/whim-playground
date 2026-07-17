package com.whim.xcom.view;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.whim.xcom.app.GameContext;
import com.whim.xcom.app.ScreenManager;
import com.whim.xcom.battle.BattleFactory;
import com.whim.xcom.battle.BattleGame;
import com.whim.xcom.battle.BattleSetup;
import com.whim.xcom.geo.GeoGame;
import com.whim.xcom.geo.Ufo;
import com.whim.xcom.geo.view.GeoScreen;

/**
 * Top-level application window. Hosts a {@link ScreenManager} that swaps between
 * the main menu and the Battlescape. "New Game" launches a skirmish mission; the
 * Geoscape screen (Phase 2) can be registered on the same manager later without
 * touching the gameplay screens.
 */
public final class MainWindow extends JFrame {

    private final transient GameContext ctx;
    private final ScreenManager screens = new ScreenManager();
    private int missionSeed = 1;

    public MainWindow(GameContext ctx) {
        super("UFO: Enemy Unknown — clean-room recreation");
        this.ctx = ctx;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        screens.setScreen(ScreenManager.MAIN_MENU, buildMenu());
        screens.setPreferredSize(new Dimension(900, 660));
        setContentPane(screens);
        screens.show(ScreenManager.MAIN_MENU);
        pack();
        setLocationRelativeTo(null);
    }

    private MainMenuPanel buildMenu() {
        return new MainMenuPanel(ctx.ruleset(), newGame(), options(), quit());
    }

    private ActionListener newGame() {
        return new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                ctx.audio().playSfx("menu_select");
                startCampaign();
            }
        };
    }

    /** Launch the Geoscape campaign; crash sites hand off to the Battlescape. */
    private void startCampaign() {
        final GeoScreen[] geoRef = new GeoScreen[1];
        Runnable toMenu = new Runnable() {
            @Override public void run() {
                screens.setScreen(ScreenManager.MAIN_MENU, buildMenu());
                screens.show(ScreenManager.MAIN_MENU);
            }
        };
        GeoScreen.AssaultHandler assault = new GeoScreen.AssaultHandler() {
            @Override public void assault(GeoGame game, Ufo ufo) {
                launchAssault(geoRef[0], game, ufo);
            }
        };
        GeoScreen geo = new GeoScreen(ctx, missionSeed++, assault, toMenu);
        geoRef[0] = geo;
        screens.setScreen(ScreenManager.GEOSCAPE, geo);
        screens.show(ScreenManager.GEOSCAPE);
    }

    private void launchAssault(final GeoScreen geo, final GeoGame game, final Ufo ufo) {
        geo.suspend();
        BattleSetup setup = game.buildAssault(ufo, missionSeed++);
        final BattleGame bg = BattleFactory.build(ctx.ruleset(), setup);
        Runnable onDone = new Runnable() {
            @Override public void run() {
                game.resolveMission(ufo, bg.outcome());
                screens.show(ScreenManager.GEOSCAPE);
                geo.resume();
            }
        };
        BattlePanel battle = new BattlePanel(ctx, bg, onDone);
        screens.setScreen(ScreenManager.BATTLE, battle);
        screens.show(ScreenManager.BATTLE);
        battle.requestFocusInWindow();
    }

    /** A direct tactical skirmish (kept for quick testing). */
    private void startSkirmish() {
        BattleGame game = BattleFactory.defaultSkirmish(ctx.ruleset(), missionSeed++);
        Runnable toMenu = new Runnable() {
            @Override public void run() {
                screens.setScreen(ScreenManager.MAIN_MENU, buildMenu());
                screens.show(ScreenManager.MAIN_MENU);
            }
        };
        BattlePanel battle = new BattlePanel(ctx, game, toMenu);
        screens.setScreen(ScreenManager.BATTLE, battle);
        screens.show(ScreenManager.BATTLE);
        battle.requestFocusInWindow();
    }

    private ActionListener options() {
        return new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                ctx.audio().playSfx("menu_select");
                JOptionPane.showMessageDialog(MainWindow.this,
                        "Options (audio, difficulty, display) arrive with the meta layer.\n"
                                + "Battlescape controls: click to select/move, click an alien to fire,\n"
                                + "1/2/3 fire mode, K kneel, Space next soldier, Enter end turn.",
                        "Options", JOptionPane.INFORMATION_MESSAGE);
            }
        };
    }

    private ActionListener quit() {
        return new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                dispose();
                System.exit(0);
            }
        };
    }
}
