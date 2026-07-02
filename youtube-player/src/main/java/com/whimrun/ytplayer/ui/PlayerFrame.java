package com.whimrun.ytplayer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.whimrun.ytplayer.media.ExtractionException;
import com.whimrun.ytplayer.media.ResolvedStream;
import com.whimrun.ytplayer.media.StreamResolver;
import com.whimrun.ytplayer.player.PlaybackCallback;
import com.whimrun.ytplayer.player.VideoPlayer;

/**
 * The single application window: address bar + Play on top, video surface in the
 * center, transport controls and a status line on the bottom.
 *
 * <p>All UI mutation happens on the EDT. The only blocking work — the yt-dlp
 * subprocess — is pushed onto a {@link SwingWorker} background thread and its
 * result is applied back on the EDT in {@code done()}.
 */
public final class PlayerFrame extends JFrame implements PlaybackCallback {

    private final StreamResolver resolver = new StreamResolver();
    private final VideoPlayer player;

    private final JTextField addressField = new JTextField();
    private final JButton playUrlButton = new JButton("Play");
    private final JButton pauseButton = new JButton("Pause");
    private final JButton fullscreenButton = new JButton("Fullscreen");
    private final JSlider seekSlider = new JSlider(0, 1000, 0);
    private final JSlider volumeSlider = new JSlider(0, 100, 80);
    private final JLabel timeLabel = new JLabel("00:00 / 00:00");
    private final JLabel statusLabel = new JLabel(" ");

    /** True while the user is dragging the seek bar, so ticks don't fight the drag. */
    private boolean seeking = false;
    private long lengthMillis = 0;
    private boolean fullscreen = false;

    public PlayerFrame(VideoPlayer player) {
        super("Whim.Run — YouTube Player");
        this.player = player;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        setLayout(new BorderLayout());
        add(buildTopBar(), BorderLayout.NORTH);
        add(player.videoSurface(), BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);

        setPreferredSize(new Dimension(960, 620));
        pack();
        setLocationRelativeTo(null);

        // Apply the initial volume once the player exists.
        player.setVolume(volumeSlider.getValue());
        wireActions();
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(6, 6));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JLabel label = new JLabel("YouTube URL or ID:");
        bar.add(label, BorderLayout.WEST);
        bar.add(addressField, BorderLayout.CENTER);
        bar.add(playUrlButton, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildBottomBar() {
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 6));

        pauseButton.setEnabled(false);

        controls.add(pauseButton);
        controls.add(Box.createHorizontalStrut(8));
        seekSlider.setEnabled(false);
        controls.add(seekSlider);
        controls.add(Box.createHorizontalStrut(8));
        controls.add(timeLabel);
        controls.add(Box.createHorizontalStrut(12));
        controls.add(new JLabel("Vol"));
        volumeSlider.setMaximumSize(new Dimension(120, 24));
        controls.add(volumeSlider);
        controls.add(Box.createHorizontalStrut(8));
        controls.add(fullscreenButton);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));

        JPanel south = new JPanel(new BorderLayout());
        south.add(controls, BorderLayout.CENTER);
        south.add(statusLabel, BorderLayout.SOUTH);
        return south;
    }

    private void wireActions() {
        ActionListener playAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startPlayback(addressField.getText());
            }
        };
        playUrlButton.addActionListener(playAction);
        addressField.addActionListener(playAction); // Enter in the field plays

        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                player.togglePause();
            }
        });

        fullscreenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFullscreen();
            }
        });

        volumeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                player.setVolume(volumeSlider.getValue());
            }
        });

        // Track drag state so timeChanged ticks don't yank the thumb around.
        seekSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (seekSlider.getValueIsAdjusting()) {
                    seeking = true;
                } else if (seeking) {
                    seeking = false;
                    float pos = seekSlider.getValue() / 1000f;
                    player.setPosition(pos);
                }
            }
        });
    }

    /**
     * Kick off extraction on a background worker, then hand the resolved stream to
     * the player. Every failure path lands in {@code done()} and is shown in the
     * status line — never swallowed.
     */
    private void startPlayback(final String input) {
        if (input == null || input.trim().isEmpty()) {
            setStatus("Enter a YouTube URL or video ID.", true);
            return;
        }
        setStatus("Resolving stream with yt-dlp…", false);
        playUrlButton.setEnabled(false);

        new SwingWorker<ResolvedStream, Void>() {
            private ExtractionException failure;

            @Override
            protected ResolvedStream doInBackground() {
                try {
                    return resolver.resolve(input.trim());
                } catch (ExtractionException ex) {
                    failure = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                playUrlButton.setEnabled(true);
                if (failure != null) {
                    setStatus(failure.userMessage(), true);
                    return;
                }
                ResolvedStream stream;
                try {
                    stream = get();
                } catch (Exception ex) {
                    setStatus("Unexpected error: " + ex.getMessage(), true);
                    return;
                }
                if (stream == null) {
                    setStatus("No stream resolved.", true);
                    return;
                }
                if (stream.getTitle() != null && !stream.getTitle().isEmpty()) {
                    setTitle("Whim.Run — " + stream.getTitle());
                }
                setStatus(stream.hasSeparateAudio()
                        ? "Playing adaptive stream (video + audio)…"
                        : "Playing muxed stream…", false);
                seekSlider.setEnabled(true);
                pauseButton.setEnabled(true);
                player.play(stream);
            }
        }.execute();
    }

    private void toggleFullscreen() {
        GraphicsDevice device =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (!fullscreen && device.isFullScreenSupported()) {
            device.setFullScreenWindow(this);
            fullscreen = true;
            fullscreenButton.setText("Windowed");
        } else {
            device.setFullScreenWindow(null);
            fullscreen = false;
            fullscreenButton.setText("Fullscreen");
        }
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setForeground(isError ? Color.RED.darker() : Color.DARK_GRAY);
        statusLabel.setText(message);
    }

    private void shutdown() {
        try {
            player.release();
        } finally {
            dispose();
            System.exit(0);
        }
    }

    private static String formatTime(long millis) {
        if (millis < 0) {
            millis = 0;
        }
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // ---- PlaybackCallback (all invoked on the EDT by VideoPlayer) ----

    @Override
    public void onLengthKnown(long lengthMillis) {
        this.lengthMillis = lengthMillis;
        updateTimeLabel(0, lengthMillis);
    }

    @Override
    public void onTimeChanged(long timeMillis, long lengthMillis) {
        this.lengthMillis = lengthMillis;
        if (!seeking && lengthMillis > 0) {
            int pos = (int) (1000L * timeMillis / lengthMillis);
            seekSlider.setValue(pos);
        }
        updateTimeLabel(timeMillis, lengthMillis);
    }

    private void updateTimeLabel(long timeMillis, long lengthMillis) {
        timeLabel.setText(formatTime(timeMillis) + " / " + formatTime(lengthMillis));
    }

    @Override
    public void onPlaying() {
        pauseButton.setText("Pause");
        pauseButton.setEnabled(true);
    }

    @Override
    public void onPaused() {
        pauseButton.setText("Play");
    }

    @Override
    public void onStopped() {
        pauseButton.setText("Play");
    }

    @Override
    public void onFinished() {
        setStatus("Playback finished.", false);
        seekSlider.setValue(0);
    }

    @Override
    public void onError() {
        setStatus("Playback error — the stream could not be decoded or opened.", true);
        pauseButton.setEnabled(false);
        seekSlider.setEnabled(false);
    }
}
