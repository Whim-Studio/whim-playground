package com.whim.xcom.app;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.whim.xcom.rng.Rng;
import com.whim.xcom.rng.SeededRng;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.Ruleset1994;
import com.whim.xcom.view.MainWindow;

/**
 * Application entry point. Builds the default 1994 {@link Ruleset}, a seeded
 * {@link Rng} and a (no-op) {@link AudioManager}, then opens the Swing main menu.
 *
 * <p>Runs headless-safe: in a display-less environment it prints a startup
 * summary and exits 0 instead of throwing, so CI/build machines can smoke-test
 * the wiring. Pass {@code --headless} to force that path.</p>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Ruleset ruleset = Ruleset1994.load();
        Rng rng = new SeededRng(defaultSeed(args));
        AudioManager audio = new NoopAudioManager();

        boolean forceHeadless = hasFlag(args, "--headless");
        if (forceHeadless || GraphicsEnvironment.isHeadless()) {
            printHeadlessSummary(ruleset, rng);
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // fall back to default L&F
                }
                new MainWindow(ruleset, audio).setVisible(true);
            }
        });
    }

    private static void printHeadlessSummary(Ruleset ruleset, Rng rng) {
        System.out.println("UFO: Enemy Unknown — clean-room recreation (Phase 0)");
        System.out.println("Ruleset : " + ruleset.displayName());
        System.out.println("Content : weapons=" + ruleset.weapons().size()
                + " armors=" + ruleset.armors().size()
                + " aliens=" + ruleset.aliens().size()
                + " facilities=" + ruleset.facilities().size()
                + " ufos=" + ruleset.ufos().size());
        System.out.println("RNG     : seeded, first roll(0..200)=" + rng.rollPercent0to200());
        System.out.println("Headless environment — Swing window not shown. Run with a display to see the menu.");
    }

    private static long defaultSeed(String[] args) {
        for (String a : args) {
            if (a != null && a.startsWith("--seed=")) {
                try {
                    return Long.parseLong(a.substring("--seed=".length()));
                } catch (NumberFormatException ignored) {
                    // fall through to default
                }
            }
        }
        return 0xC0FFEEL;
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String a : args) {
            if (flag.equals(a)) {
                return true;
            }
        }
        return false;
    }
}
