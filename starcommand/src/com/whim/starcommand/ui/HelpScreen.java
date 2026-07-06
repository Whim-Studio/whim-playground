package com.whim.starcommand.ui;

import com.whim.starcommand.app.Game;
import com.whim.starcommand.app.Screen;
import com.whim.starcommand.render.Palette;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Color;
import javax.swing.JPanel;

/** In-game reference: controls and the flow of play. */
public class HelpScreen extends Screen {

    public HelpScreen(Game game) {
        super(game);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(UiKit.label("CONTROLS & REFERENCE", UiKit.HEAD, Palette.ACCENT), BorderLayout.NORTH);

        JTextArea text = new JTextArea(HELP);
        text.setEditable(false);
        text.setFont(UiKit.MONO);
        text.setBackground(Palette.PANEL);
        text.setForeground(Palette.TEXT);
        add(new JScrollPane(text), BorderLayout.CENTER);

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        foot.setOpaque(false);
        JButton back = UiKit.button("Back (Esc)");
        back.addActionListener(e -> game.screens.show(Game.MENU));
        foot.add(back);
        add(foot, BorderLayout.SOUTH);
        Keys.bind(this, "ESCAPE", new Runnable() { public void run() { game.screens.show(Game.MENU); } });
    }

    @Override
    public String name() { return Game.HELP; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Palette.SPACE);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private static final String HELP =
        "STAR COMMAND — a spirit-and-mechanics recreation of the 1988 SSI RPG.\n\n" +
        "THE FLOW OF PLAY\n" +
        "  1. Recruit a crew of up to 8 (Pilot, Marine, Esper, Medic, Engineer, Scout).\n" +
        "  2. At Star Command HQ (Starport) buy weapons, upgrade your ship, accept missions.\n" +
        "  3. Launch to the Galaxy Map. Fly out into the Alpha/Beta frontiers.\n" +
        "  4. Fight pirates and aliens ship-to-ship. DISABLE a ship to board and capture it.\n" +
        "  5. Complete the mission ladder — the marquee target is the pirate Blackbeard.\n\n" +
        "GLOBAL\n" +
        "  Every screen supports both mouse and keyboard. Shortcut letters are shown in ().\n\n" +
        "MAIN MENU:  N new game   C continue   H help   Q quit\n" +
        "CREW:       R re-roll    A add        Enter launch\n" +
        "STARPORT:   click Buy/Accept   S save   G launch to galaxy\n" +
        "GALAXY:     Arrow keys move   S scan   D deploy drop ship   B dock at HQ\n" +
        "SHIP COMBAT: 1 beam  2 missile  3 shields  4 disable  5 flee\n" +
        "GROUND:     click/arrows move   A attack   Tab next unit   Space end turn\n" +
        "UNIQUE AREA: arrows/click move room   X extract (from the entrance)\n\n" +
        "TIPS\n" +
        "  • Shields soak damage before hull. 'Disable' aims for engines: less damage,\n" +
        "    but leaves the enemy boardable for a bigger payout.\n" +
        "  • Repair and save at HQ between sorties.\n";
}
