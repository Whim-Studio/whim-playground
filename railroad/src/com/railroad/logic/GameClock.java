package com.railroad.logic;

import com.railroad.model.GameState;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The game clock: a Swing {@link Timer} that repeatedly advances the
 * {@link GameState} on the Event Dispatch Thread. Owns start/pause and a speed
 * multiplier; after each tick it notifies a listener so the UI can repaint and
 * refresh the HUD.
 *
 * <p>Keeping the clock in the logic layer (rather than the UI) means Phase 2+
 * can drive the same {@code tick} from tests or a headless loop.
 */
public final class GameClock {

    /** Called after every tick so the view can refresh. */
    public interface TickListener {
        void onTick();
    }

    private static final int TIMER_INTERVAL_MS = 50;
    private static final double DAYS_PER_TICK = 0.20; // in-game days per timer fire at 1x

    private final GameState state;
    private final Timer timer;
    private int speedMultiplier = 1; // 1x / 2x / 4x
    private TickListener listener;

    public GameClock(final GameState state) {
        this.state = state;
        this.timer = new Timer(TIMER_INTERVAL_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                state.tick(DAYS_PER_TICK * speedMultiplier);
                if (listener != null) {
                    listener.onTick();
                }
            }
        });
    }

    public void setTickListener(TickListener listener) {
        this.listener = listener;
    }

    public void start() {
        timer.start();
    }

    public void pause() {
        timer.stop();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }

    public int getSpeedMultiplier() {
        return speedMultiplier;
    }

    /** Sets the speed multiplier (clamped to 1..4). */
    public void setSpeedMultiplier(int multiplier) {
        this.speedMultiplier = Math.max(1, Math.min(4, multiplier));
    }
}
