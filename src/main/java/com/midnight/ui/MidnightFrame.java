package com.midnight.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;

import com.midnight.ai.DoomdarkAI;
import com.midnight.ai.NightReport;
import com.midnight.ai.NightResolver;
import com.midnight.core.Character;
import com.midnight.core.Direction;
import com.midnight.core.GameState;
import com.midnight.core.Location;
import com.midnight.core.Outcome;
import com.midnight.core.Side;

/**
 * The single game window and controller. Owns the {@link GameState} and drives
 * everything through it &mdash; it never re-implements a rule. It wires together:
 *
 * <ul>
 *   <li>the {@link LandscapePanel} 2.5D first-person view and the toggleable
 *       {@link MapPanel} strategic overlay (a {@link CardLayout} centre);</li>
 *   <li>the {@link CompassPanel} 8-way look/move control;</li>
 *   <li>a character-selection menu cycling {@code state.playerLords()};</li>
 *   <li>an "End Day" turn control that runs Doomdark's night and reports it.</li>
 * </ul>
 */
final class MidnightFrame extends JFrame implements CompassPanel.Listener {

    private GameState state;

    private final LandscapePanel landscapePanel;
    private final MapPanel mapPanel;
    private final CardLayout centreCards;
    private final JPanel centre;
    private final CompassPanel compass;

    private final JToggleButton mapToggle;
    private final JLabel lordLabel;
    private final JLabel statsLabel;
    private final JLabel turnLabel;
    private final JLabel bannerLabel;
    private final JTextArea messageArea;
    private final JButton recruitButton;
    private final JButton destroyCrownButton;

    private static final String CARD_LANDSCAPE = "landscape";
    private static final String CARD_MAP = "map";

    private boolean outcomeAnnounced;

    MidnightFrame() {
        super("The Lords of Midnight");
        this.state = GameState.newGame();
        if (state.selected() == null) {
            java.util.List<Character> lords = state.playerLords();
            if (!lords.isEmpty()) {
                state.select(lords.get(0));
            }
        }

        this.landscapePanel = new LandscapePanel(state);
        this.mapPanel = new MapPanel(state);
        this.centreCards = new CardLayout();
        this.centre = new JPanel(centreCards);
        centre.add(landscapePanel, CARD_LANDSCAPE);
        centre.add(mapPanel, CARD_MAP);

        this.compass = new CompassPanel(this);
        this.mapToggle = new JToggleButton("Strategic Map");
        this.lordLabel = new JLabel();
        this.statsLabel = new JLabel();
        this.turnLabel = new JLabel();
        this.bannerLabel = new JLabel(" ");
        this.messageArea = new JTextArea();
        this.recruitButton = new JButton("Recruit lord here");
        this.destroyCrownButton = new JButton("Destroy the Ice Crown");

        setLayout(new BorderLayout());
        add(centre, BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);

        setMinimumSize(new Dimension(980, 600));
        setPreferredSize(new Dimension(1080, 700));

        refresh();
    }

    // ----- side panel construction ------------------------------------------

    private JPanel buildSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        side.setPreferredSize(new Dimension(320, 0));

        JLabel title = new JLabel("The Lords of Midnight");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(title);

        turnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD, 14f));
        side.add(turnLabel);

        bannerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bannerLabel.setFont(bannerLabel.getFont().deriveFont(Font.BOLD, 14f));
        bannerLabel.setForeground(new Color(0x117A2B));
        side.add(bannerLabel);
        side.add(Box.createVerticalStrut(10));

        mapToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        mapToggle.setToolTipText("Toggle the top-down strategic map overlay.");
        mapToggle.addActionListener(e -> onToggleMap());
        side.add(mapToggle);
        side.add(Box.createVerticalStrut(12));

        side.add(buildCharacterMenu());
        side.add(Box.createVerticalStrut(8));

        lordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        lordLabel.setFont(lordLabel.getFont().deriveFont(Font.BOLD, 15f));
        side.add(lordLabel);
        statsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(statsLabel);
        side.add(Box.createVerticalStrut(10));

        compass.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(compass);
        side.add(Box.createVerticalStrut(6));

        recruitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        recruitButton.addActionListener(e -> onRecruit());
        side.add(recruitButton);
        side.add(Box.createVerticalStrut(4));
        destroyCrownButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        destroyCrownButton.addActionListener(e -> onDestroyCrown());
        side.add(destroyCrownButton);
        side.add(Box.createVerticalStrut(12));

        JButton endDay = new JButton("End Day → Doomdark's Night");
        endDay.setAlignmentX(Component.LEFT_ALIGNMENT);
        endDay.setFont(endDay.getFont().deriveFont(Font.BOLD, 13f));
        endDay.addActionListener(e -> onEndDay());
        side.add(endDay);
        side.add(Box.createVerticalStrut(10));

        JLabel msgHeader = new JLabel("Chronicle:");
        msgHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(msgHeader);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(messageArea.getFont().deriveFont(12f));
        messageArea.setText("Dawn breaks over the Lands of Midnight. Choose a lord, look about, and march.");
        JScrollPane scroll = new JScrollPane(messageArea);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setPreferredSize(new Dimension(296, 160));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        side.add(scroll);

        return side;
    }

    private JPanel buildCharacterMenu() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.X_AXIS));
        menu.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.setBorder(BorderFactory.createTitledBorder("Lord"));
        menu.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JButton prev = new JButton("◀ Prev");
        prev.addActionListener(e -> cycleLord(-1));
        JButton next = new JButton("Next ▶");
        next.addActionListener(e -> cycleLord(1));
        menu.add(prev);
        menu.add(Box.createHorizontalGlue());
        menu.add(next);
        return menu;
    }

    // ----- character selection ----------------------------------------------

    private void cycleLord(int delta) {
        java.util.List<Character> lords = state.playerLords();
        if (lords.isEmpty()) {
            return;
        }
        Character cur = state.selected();
        int idx = 0;
        for (int i = 0; i < lords.size(); i++) {
            if (lords.get(i) == cur) {
                idx = i;
                break;
            }
        }
        idx = ((idx + delta) % lords.size() + lords.size()) % lords.size();
        state.select(lords.get(idx));
        refresh();
    }

    // ----- compass callbacks -------------------------------------------------

    @Override
    public void onLook(Direction d) {
        Character me = state.selected();
        if (me == null) {
            return;
        }
        state.look(me, d);
        refresh();
    }

    @Override
    public void onMove() {
        Character me = state.selected();
        if (me == null) {
            return;
        }
        Direction d = me.facing();
        boolean ok = state.move(me, d);
        if (!ok) {
            appendMessage(me.name() + " cannot move " + d + " right now.");
        } else {
            appendMessage(me.name() + " marches " + d + " to " + me.location() + ".");
        }
        refresh();
    }

    // ----- other actions -----------------------------------------------------

    private void onRecruit() {
        Character me = state.selected();
        if (me == null) {
            return;
        }
        boolean ok = state.tryRecruit(me);
        appendMessage(ok
                ? "A lord pledges to your cause and joins the Free!"
                : "There is no lord here willing to be recruited.");
        refresh();
    }

    private void onDestroyCrown() {
        boolean ok = state.tryDestroyIceCrown();
        appendMessage(ok
                ? "Morkin casts the Ice Crown into the fire — its power is broken!"
                : "The Ice Crown cannot be destroyed from here (only Morkin, at its resting place).");
        refresh();
    }

    private void onToggleMap() {
        centreCards.show(centre, mapToggle.isSelected() ? CARD_MAP : CARD_LANDSCAPE);
        mapToggle.setText(mapToggle.isSelected() ? "First-person View" : "Strategic Map");
    }

    private void onEndDay() {
        if (state.isOver()) {
            return;
        }
        // The contract declares GameState.endDay(NightResolver) as void, yet the UI
        // must surface the night's NightReport.narrative(). We wrap a DoomdarkAI in a
        // capturing resolver so the engine still drives the night through DoomdarkAI
        // while we keep a handle on the report it returns.
        final NightReport[] captured = new NightReport[1];
        NightResolver resolver = new NightResolver() {
            private final DoomdarkAI ai = new DoomdarkAI();

            @Override
            public NightReport resolveNight(GameState s) {
                NightReport r = ai.resolveNight(s);
                captured[0] = r;
                return r;
            }
        };
        state.endDay(resolver);

        StringBuilder sb = new StringBuilder();
        sb.append("=== Night falls (now Day ").append(state.day()).append(") ===\n");
        if (captured[0] != null) {
            sb.append(captured[0].narrative());
        } else {
            sb.append("The night passed without word.");
        }
        appendMessage(sb.toString());
        refresh();
    }

    // ----- refresh / view sync ----------------------------------------------

    private void refresh() {
        landscapePanel.setState(state);
        mapPanel.setState(state);

        Character me = state.selected();
        turnLabel.setText("Day " + state.day() + " — " + (state.isDay() ? "DAY" : "NIGHT"));

        if (me != null) {
            lordLabel.setText(me.name() + (me.carriesIceCrown() ? "  ♦ Ice Crown" : ""));
            StringBuilder s = new StringBuilder("<html>");
            s.append("Energy: ").append(me.energy()).append("  ·  Courage: ").append(me.courage()).append("<br>");
            s.append("Hours left: ").append(me.hoursRemaining()).append("<br>");
            s.append("Army: ").append(me.warriors()).append(" warriors, ")
                    .append(me.riders()).append(" riders");
            s.append(me.isMounted() ? " (mounted)" : "");
            s.append("<br>At: ").append(me.location());
            s.append("</html>");
            statsLabel.setText(s.toString());
            compass.setFacing(me.facing());
        } else {
            lordLabel.setText("(no lord)");
            statsLabel.setText(" ");
        }

        boolean canAct = state.isDay() && me != null && me.isAlive()
                && me.hoursRemaining() > 0 && !state.isOver();
        compass.setControlsEnabled(canAct);
        if (canAct) {
            compass.setMoveAllowed(state.canMove(me, me.facing()));
        }

        // Recruit / destroy-crown availability.
        recruitButton.setEnabled(canAct);
        boolean crownHere = me != null && me.isMorkin() && me.carriesIceCrown()
                && me.location().equals(state.iceCrownLocation()) && state.isDay() && !state.isOver();
        destroyCrownButton.setEnabled(crownHere);

        updateBanner();
    }

    private void updateBanner() {
        Outcome o = state.outcome();
        if (o == null || o == Outcome.ONGOING) {
            bannerLabel.setText(" ");
            return;
        }
        String text;
        Color color;
        switch (o) {
            case FREE_ADVENTURE_WIN:
                text = "VICTORY — Morkin has destroyed the Ice Crown!";
                color = new Color(0x117A2B);
                break;
            case FREE_WARGAME_WIN:
                text = "VICTORY — Ushgarak has fallen to the Free!";
                color = new Color(0x117A2B);
                break;
            case DOOMDARK_WIN:
                text = "DEFEAT — Darkness covers the Lands of Midnight.";
                color = new Color(0xC0392B);
                break;
            default:
                text = " ";
                color = Color.BLACK;
        }
        bannerLabel.setText(text);
        bannerLabel.setForeground(color);
        compass.setControlsEnabled(false);
        if (!outcomeAnnounced) {
            outcomeAnnounced = true;
            JOptionPane.showMessageDialog(this, text, "The Lords of Midnight",
                    o == Outcome.DOOMDARK_WIN ? JOptionPane.WARNING_MESSAGE
                            : JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void appendMessage(String msg) {
        String existing = messageArea.getText();
        messageArea.setText((existing == null || existing.isEmpty() ? "" : existing + "\n\n") + msg);
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }
}
