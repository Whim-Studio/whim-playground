package com.whim.ytmeta;

import com.whim.ytmeta.ui.MetadataPanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * Application entry point.
 *
 * Wires the UI together on the Event Dispatch Thread and shows the main window.
 * The three concerns stay cleanly separated:
 *   - {@code com.whim.ytmeta.model}  : data definitions (VideoMetadata)
 *   - {@code com.whim.ytmeta.logic}  : networking + extraction engine
 *   - {@code com.whim.ytmeta.ui}     : Swing presentation layer
 *
 * Java 8 only — no var, text blocks, or post-8 language features.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Fall back to the default cross-platform look and feel.
                }

                JFrame frame = new JFrame("YouTube Metadata Extractor");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.setContentPane(new MetadataPanel());
                frame.pack();
                frame.setMinimumSize(frame.getSize());
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
