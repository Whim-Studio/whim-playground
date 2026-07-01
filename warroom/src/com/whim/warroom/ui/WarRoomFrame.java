package com.whim.warroom.ui;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Top-level window. A {@link BorderLayout} with the {@link EditorPanel} WEST,
 * {@link BattlefieldPanel} CENTER, and {@link PlaybackBar} SOUTH. Switches
 * between SIMULATION (editor visible) and BATTLE (playback) presentation, and
 * hosts Cinema Mode which hides all chrome for a clean full-field view. Chrome
 * returns via the {@code C} hotkey or a hover at the very top edge of the field.
 */
public final class WarRoomFrame extends JFrame {

    private final SandboxController ctl;
    private final BattlefieldPanel field;
    private final EditorPanel editor;
    private final PlaybackBar bar;

    private boolean cinema;

    public WarRoomFrame(SandboxController ctl, BattlefieldPanel field, EditorPanel editor, PlaybackBar bar) {
        super("War Room: Tactical Sandbox");
        this.ctl = ctl;
        this.field = field;
        this.editor = editor;
        this.bar = bar;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                ctl.shutdown();
                dispose();
                System.exit(0);
            }
        });

        setLayout(new BorderLayout());
        add(editor, BorderLayout.WEST);
        add(field, BorderLayout.CENTER);
        add(bar, BorderLayout.SOUTH);

        bar.setFrame(this);
        field.setChromeRestore(new Runnable() {
            public void run() { if (cinema) setCinema(false); }
        });

        installHotkeys();
        setSize(1180, 760);
        setLocationRelativeTo(null);
    }

    private void installHotkeys() {
        JComponent root = getRootPane();
        bindKey(root, "SPACE", "toggle-play", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (ctl.getMode() == SandboxController.Mode.BATTLE && ctl.getEngine().isPlaying()) ctl.pause();
                else ctl.play();
            }
        });
        bindKey(root, "C", "toggle-cinema", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { toggleCinema(); }
        });
        bindKey(root, "ESCAPE", "exit-cinema", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (cinema) setCinema(false); }
        });
        bindKey(root, "F", "fit", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { field.fitToMap(); }
        });
    }

    private void bindKey(JComponent c, String key, String name, AbstractAction action) {
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        c.getActionMap().put(name, action);
    }

    // ---- mode presentation ----
    public void showSimulationMode() {
        if (!cinema) editor.setVisible(true);
        field.repaint();
    }

    public void showBattleMode() {
        // editor palette is irrelevant during playback; hide to give the field room
        editor.setVisible(false);
        field.repaint();
        revalidate();
    }

    // ---- cinema mode ----
    public void toggleCinema() { setCinema(!cinema); }

    public void setCinema(boolean on) {
        cinema = on;
        if (on) {
            editor.setVisible(false);
            bar.setVisible(false);
        } else {
            bar.setVisible(true);
            editor.setVisible(ctl.getMode() == SandboxController.Mode.SIMULATION);
        }
        revalidate();
        repaint();
    }

    public boolean isCinema() { return cinema; }
}
