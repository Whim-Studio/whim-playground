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
                startSkirmish();
            }
        };
    }

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
