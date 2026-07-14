package com.whim.b5db.ui;

import com.whim.b5db.app.Catalog;
import com.whim.b5db.engine.Seat;
import com.whim.b5db.model.Faction;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-level window. Uses a {@link CardLayout} to switch between the Main Menu,
 * the New Game setup screen, and the game board. Accessible (large) fonts and
 * button mnemonics are applied throughout.
 */
public final class MainFrame extends JFrame {

    static final Font UI_FONT = new Font("SansSerif", Font.PLAIN, 16);
    static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 30);

    private final Catalog catalog;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    public MainFrame(Catalog catalog) {
        super("Babylon 5: The Shadow War — Deck-Building Game");
        this.catalog = catalog;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 640));
        root.add(menuPanel(), "MENU");
        root.add(setupPanel(), "SETUP");
        add(root);
        show("MENU");
        setLocationRelativeTo(null);
    }

    void show(String name) {
        cards.show(root, name);
    }

    private JPanel menuPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Babylon 5: The Shadow War", SwingConstants.CENTER);
        title.setFont(TITLE_FONT);
        title.setBorder(BorderFactory.createEmptyBorder(60, 20, 20, 20));
        p.add(title, BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setBorder(BorderFactory.createEmptyBorder(20, 300, 120, 300));

        JButton newGame = big(new JButton("New Game"));
        newGame.setMnemonic('N');
        newGame.addActionListener(e -> show("SETUP"));

        JButton quit = big(new JButton("Quit"));
        quit.setMnemonic('Q');
        quit.addActionListener(e -> dispose());

        buttons.add(newGame);
        buttons.add(javax.swing.Box.createVerticalStrut(16));
        buttons.add(quit);
        p.add(buttons, BorderLayout.CENTER);

        JLabel hint = new JLabel(catalog.cards().size()
                + " market cards loaded. Shortcuts: Alt+N New, Alt+Q Quit.", SwingConstants.CENTER);
        hint.setFont(UI_FONT);
        p.add(hint, BorderLayout.SOUTH);
        return p;
    }

    private JPanel setupPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JLabel title = new JLabel("New Game Setup", SwingConstants.CENTER);
        title.setFont(TITLE_FONT);
        title.setBorder(BorderFactory.createEmptyBorder(24, 12, 12, 12));
        p.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(12, 120, 12, 120));

        JPanel countRow = new JPanel(new GridLayout(1, 2, 8, 8));
        JLabel countLabel = big(new JLabel("Number of players (2–5):"));
        JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(2, 2, 5, 1));
        countSpinner.setFont(UI_FONT);
        countRow.add(countLabel);
        countRow.add(countSpinner);
        form.add(countRow);
        form.add(javax.swing.Box.createVerticalStrut(12));

        // Five seat rows; visibility follows the player count.
        List<SeatRow> seatRows = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            SeatRow row = new SeatRow(i);
            seatRows.add(row);
            form.add(row.panel);
            form.add(javax.swing.Box.createVerticalStrut(6));
        }
        Runnable refreshVisibility = () -> {
            int n = (Integer) countSpinner.getValue();
            for (int i = 0; i < seatRows.size(); i++) {
                seatRows.get(i).panel.setVisible(i < n);
            }
        };
        countSpinner.addChangeListener(e -> refreshVisibility.run());
        refreshVisibility.run();

        p.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        JButton back = big(new JButton("Back"));
        back.setMnemonic('B');
        back.addActionListener(e -> show("MENU"));
        JButton start = big(new JButton("Start Game"));
        start.setMnemonic('S');
        start.addActionListener(e -> {
            int n = (Integer) countSpinner.getValue();
            List<Seat> seats = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                seats.add(seatRows.get(i).toSeat());
            }
            startGame(seats);
        });
        actions.add(back);
        actions.add(start);
        p.add(actions, BorderLayout.SOUTH);
        return p;
    }

    private void startGame(List<Seat> seats) {
        long seed = seats.hashCode() * 2654435761L + seats.size();
        MatchController controller = new MatchController(catalog.cards(), seats, seed);
        GamePanel board = new GamePanel(this, controller);
        root.add(board, "GAME");
        show("GAME");
        board.onShown();
    }

    static <T extends javax.swing.JComponent> T big(T c) {
        c.setFont(UI_FONT);
        return c;
    }

    /** One configurable seat: name is implicit, faction + human/AI selectable. */
    private static final class SeatRow {
        final JPanel panel = new JPanel(new GridLayout(1, 3, 8, 8));
        final JComboBox<Faction> faction = new JComboBox<>(Faction.playable());
        final JComboBox<String> kind = new JComboBox<>(new String[]{"Human", "AI"});
        final int index;

        SeatRow(int index) {
            this.index = index;
            faction.setSelectedIndex(index % Faction.playable().length);
            kind.setSelectedIndex(index == 0 ? 0 : 1); // seat 1 human by default
            JLabel label = big(new JLabel("Seat " + (index + 1) + ":"));
            faction.setFont(UI_FONT);
            kind.setFont(UI_FONT);
            panel.add(label);
            panel.add(faction);
            panel.add(kind);
        }

        Seat toSeat() {
            Faction f = (Faction) faction.getSelectedItem();
            boolean ai = "AI".equals(kind.getSelectedItem());
            String name = (ai ? "AI " : "Player ") + (index + 1) + " (" + f.display() + ")";
            return new Seat(name, f, ai);
        }
    }
}
