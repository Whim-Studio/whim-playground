package com.whim.tippingpoint.app;

import com.whim.tippingpoint.ui.GameFrame;

/**
 * Entry point for the Tipping Point desktop app.
 *
 * <p>A clean-room, Java&nbsp;8 / Swing digital adaptation of the 2020 board game
 * <i>Tipping Point</i> (Ryan Smith / Treecer): city-building economy coupled to a
 * compounding global-CO&#8322; climate feedback loop. All rules live in
 * {@code com.whim.tippingpoint.engine}; all rendering is procedural
 * {@code Graphics2D} in {@code com.whim.tippingpoint.ui}.
 *
 * <p>Build: {@code javac --release 8 -d out $(find tippingpoint/src -name '*.java')}<br>
 * Run:&nbsp;&nbsp; {@code java -cp out com.whim.tippingpoint.app.Main}
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        // Shows the frame on the Event Dispatch Thread via SwingUtilities.invokeLater.
        GameFrame.run();
    }
}
