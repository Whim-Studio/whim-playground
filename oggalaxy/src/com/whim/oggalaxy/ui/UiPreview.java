package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.GameController;
import com.whim.oggalaxy.api.demo.DemoController;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.Color;

/**
 * Standalone entry point for the UI. Wires a {@link DemoController} (canned world) to
 * either the {@link MainFrame} directly (default) or the {@link StartScreen} (pass the
 * argument {@code start}). A small preview-only clock driver advances the demo when the
 * clock is "running", standing in for the real engine's background tick thread.
 *
 * <p>The production wiring lives in {@code app.Main}, which swaps the real
 * {@code GameEngine} in for the {@code DemoController}.
 */
public final class UiPreview {

    private UiPreview() {
    }

    public static void main(String[] args) {
        final boolean showStart = args.length > 0 && "start".equalsIgnoreCase(args[0]);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                applyDarkTheme();
                GameController controller = new DemoController();
                startPreviewClock(controller);
                if (showStart) {
                    new StartScreen(controller).setVisible(true);
                } else {
                    MainFrame frame = new MainFrame(controller);
                    frame.launch();
                }
            }
        });
    }

    /** Preview-only driver: emulates the engine's background clock for the demo controller. */
    private static void startPreviewClock(final GameController controller) {
        Timer driver = new Timer(1000, new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (controller.isClockRunning()) {
                    controller.advance(Math.max(1, controller.getSpeed()));
                }
            }
        });
        driver.start();
    }

    /** Configure Swing defaults for a dark space theme without any external L&F library. */
    public static void applyDarkTheme() {
        Color bg = Palette.BG_PANEL;
        Color bgHi = Palette.BG_PANEL_HI;
        Color text = Palette.TEXT;
        Color sel = Palette.mix(Palette.BG_PANEL, Palette.ACCENT, 0.30);

        put("Panel.background", bg);
        put("OptionPane.background", bg);
        put("OptionPane.messageForeground", text);
        put("Label.foreground", text);
        put("TabbedPane.background", bg);
        put("TabbedPane.foreground", text);
        put("TabbedPane.selected", bgHi);
        put("TabbedPane.contentAreaColor", bg);
        put("TabbedPane.light", Palette.BORDER);
        put("TabbedPane.darkShadow", Palette.BG_DEEP);
        put("ComboBox.background", bgHi);
        put("ComboBox.foreground", text);
        put("ComboBox.selectionBackground", sel);
        put("ComboBox.selectionForeground", text);
        put("Spinner.background", bgHi);
        put("Spinner.foreground", text);
        put("TextField.background", bgHi);
        put("TextField.foreground", text);
        put("TextField.caretForeground", Palette.ACCENT);
        put("FormattedTextField.background", bgHi);
        put("FormattedTextField.foreground", text);
        put("TextArea.background", Palette.BG_DEEP);
        put("TextArea.foreground", text);
        put("List.background", bg);
        put("List.foreground", text);
        put("List.selectionBackground", sel);
        put("List.selectionForeground", text);
        put("ScrollPane.background", bg);
        put("Viewport.background", bg);
        put("ScrollBar.thumb", Palette.BORDER_HI);
        put("ScrollBar.track", Palette.BG_DEEP);
        put("Button.background", bgHi);
        put("Button.foreground", text);
        put("Button.select", sel);
        put("SplitPane.background", bg);
        put("SplitPaneDivider.draggingColor", Palette.ACCENT);
        put("CheckBox.background", bg);
        put("CheckBox.foreground", text);
        put("ToolTip.background", Palette.BG_DEEP);
        put("ToolTip.foreground", text);
        put("ToolTip.foregroundInactive", text);
        put("Slider.background", bg);
        put("Slider.foreground", Palette.ACCENT);
    }

    private static void put(String key, Object value) {
        try {
            UIManager.put(key, value);
        } catch (RuntimeException ignore) {
            // Some keys may be absent under headless L&F; safe to skip.
        }
    }
}
