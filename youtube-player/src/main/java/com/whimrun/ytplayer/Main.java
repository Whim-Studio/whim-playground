package com.whimrun.ytplayer;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.whimrun.ytplayer.player.PlaybackCallback;
import com.whimrun.ytplayer.player.VideoPlayer;
import com.whimrun.ytplayer.ui.PlayerFrame;

import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

/**
 * Entry point for the Whim.Run YouTube player.
 *
 * <p>Startup order matters:
 * <ol>
 *   <li>Run native discovery so we can give a clean "install VLC" message instead
 *       of a JNA stack trace if libVLC is missing.</li>
 *   <li>Build the vlcj-backed {@link VideoPlayer} (this loads native libs).</li>
 *   <li>Build and show the Swing window — all on the EDT.</li>
 * </ol>
 */
public final class Main {

    public static void main(String[] args) {
        // Native discovery is a plain library probe; safe to run before the EDT.
        final boolean nativeFound = new NativeDiscovery().discover();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Non-fatal: fall back to the default look and feel.
                }

                if (!nativeFound) {
                    JOptionPane.showMessageDialog(null,
                            "Native VLC library (libVLC) was not found.\n\n"
                                    + "Install VLC for your OS so vlcj can locate libvlc:\n"
                                    + "  Windows: install VLC (64-bit) from videolan.org\n"
                                    + "  macOS:   install VLC.app into /Applications\n"
                                    + "  Linux:   install the 'vlc' / 'libvlc' package\n\n"
                                    + "The application will now exit.",
                            "libVLC not found", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                    return;
                }

                VideoPlayer player;
                try {
                    // A temporary no-op callback until the frame (the real callback)
                    // is wired; construction is what actually loads native libVLC.
                    final PlaybackCallback[] delegate = new PlaybackCallback[1];
                    player = new VideoPlayer(new PlaybackCallback() {
                        public void onLengthKnown(long l) { if (delegate[0] != null) delegate[0].onLengthKnown(l); }
                        public void onTimeChanged(long t, long l) { if (delegate[0] != null) delegate[0].onTimeChanged(t, l); }
                        public void onPlaying() { if (delegate[0] != null) delegate[0].onPlaying(); }
                        public void onPaused() { if (delegate[0] != null) delegate[0].onPaused(); }
                        public void onStopped() { if (delegate[0] != null) delegate[0].onStopped(); }
                        public void onFinished() { if (delegate[0] != null) delegate[0].onFinished(); }
                        public void onError() { if (delegate[0] != null) delegate[0].onError(); }
                    });
                    PlayerFrame frame = new PlayerFrame(player);
                    delegate[0] = frame; // frame implements PlaybackCallback
                    frame.setVisible(true);
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(null,
                            "Failed to initialize the native media player:\n" + ex.getMessage()
                                    + "\n\nEnsure a compatible VLC / libVLC is installed.",
                            "Player initialization failed", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        });
    }

    private Main() {
    }
}
