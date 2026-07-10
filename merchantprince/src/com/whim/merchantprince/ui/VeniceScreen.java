package com.whim.merchantprince.ui;

import com.whim.merchantprince.app.Game;
import com.whim.merchantprince.app.Screen;
import com.whim.merchantprince.engine.PoliticsEngine;
import com.whim.merchantprince.model.Family;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Office;
import com.whim.merchantprince.render.Palette;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Venice — politics, the Church, and intrigue (GAME_DESIGN_REFERENCE §6). Bribe the
 * Council of Ten for offices and the dogeship, influence cardinals toward the papacy,
 * build a den of iniquities, and run dirty tricks against rival houses. All effects
 * route through {@link PoliticsEngine}; bribed senators/cardinals and offices count
 * toward final net worth (§7).
 */
public class VeniceScreen extends Screen {

    private final JLabel purse = UiKit.label("", UiKit.HEAD, Palette.INK);
    private final JLabel standing = UiKit.label("", UiKit.BODY, Palette.INK);
    private final JLabel officesLabel = UiKit.label("", UiKit.BODY, Palette.INK);
    private final JLabel scoreboard = UiKit.label("", UiKit.SMALL, Palette.INK);
    private final JLabel notice = UiKit.label(" ", UiKit.BODY, Palette.CRIMSON);

    private final JComboBox<Office> officeBox = new JComboBox<Office>(Office.ALL);
    private final JComboBox<RivalItem> rivalBox = new JComboBox<RivalItem>();
    private final JComboBox<String> trickBox = new JComboBox<String>(
            new String[] { "arson", "rumour", "assassination" });

    public VeniceScreen(Game game) {
        super(game);
        setLayout(new BorderLayout());

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        nav.setBackground(Palette.PARCHMENT_DK);
        JButton back = UiKit.button("Back to Map");
        back.addActionListener(e -> game.screens.show(Game.MAP));
        nav.add(back);
        nav.add(UiKit.label("Venice — Politics & Intrigue", UiKit.HEAD, Palette.INK));
        add(nav, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 12, 16));

        body.add(purse);
        body.add(standing);
        body.add(officesLabel);
        body.add(Box.createVerticalStrut(10));

        // Offices.
        JPanel officeRow = row();
        officeRow.add(UiKit.label("Bribe the Council of Ten for office:", UiKit.BODY, Palette.INK));
        officeRow.add(officeBox);
        JButton buyOffice = smallBtn("Acquire");
        buyOffice.addActionListener(e -> {
            Office o = (Office) officeBox.getSelectedItem();
            boolean ok = PoliticsEngine.buyOffice(game.state, game.state.player(), o);
            notice.setText(ok ? "You secured the office of " + o.label + "."
                    : "The Council spurns you (cannot afford " + o.label + " or already held).");
            refresh();
        });
        officeRow.add(buyOffice);
        body.add(officeRow);

        // Senators / cardinals.
        JPanel bribeRow = row();
        JButton senator = smallBtn("Bribe a Senator");
        senator.addActionListener(e -> {
            boolean ok = PoliticsEngine.bribeSenator(game.state, game.state.player());
            notice.setText(ok ? "A senator now serves your interests." : "You lack the florins for that bribe.");
            refresh();
        });
        JButton cardinal = smallBtn("Influence a Cardinal");
        cardinal.addActionListener(e -> {
            boolean ok = PoliticsEngine.bribeCardinal(game.state, game.state.player());
            notice.setText(ok ? "A cardinal inclines toward your house." : "You lack the florins for that gift.");
            refresh();
        });
        bribeRow.add(senator);
        bribeRow.add(cardinal);
        body.add(bribeRow);

        // Den + dirty tricks.
        JPanel denRow = row();
        JButton den = smallBtn("Build Den of Iniquities");
        den.addActionListener(e -> {
            boolean ok = PoliticsEngine.buildDen(game.state, game.state.player());
            notice.setText(ok ? "Your den of iniquities is ready — for a price to your soul."
                    : "Already built, or you cannot afford it.");
            refresh();
        });
        denRow.add(den);
        body.add(denRow);

        JPanel trickRow = row();
        trickRow.add(UiKit.label("Dirty trick vs.", UiKit.BODY, Palette.INK));
        trickRow.add(rivalBox);
        trickRow.add(trickBox);
        JButton doTrick = smallBtn("Execute");
        doTrick.addActionListener(e -> {
            RivalItem it = (RivalItem) rivalBox.getSelectedItem();
            if (it == null) { notice.setText("No rival to target."); return; }
            String kind = (String) trickBox.getSelectedItem();
            String out = PoliticsEngine.dirtyTrick(game.state, game.state.player(), it.family, kind, game.rng);
            notice.setText(out);
            refresh();
        });
        trickRow.add(doTrick);
        body.add(trickRow);

        body.add(Box.createVerticalStrut(10));
        body.add(notice);
        body.add(Box.createVerticalStrut(10));
        body.add(UiKit.label("The Great Houses:", UiKit.HEAD, Palette.INK));
        body.add(scoreboard);

        add(body, BorderLayout.CENTER);
    }

    private JPanel row() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        return p;
    }

    private JButton smallBtn(String text) {
        JButton b = UiKit.button(text);
        b.setFont(UiKit.SMALL);
        return b;
    }

    @Override public void onShow() { refresh(); }

    private void refresh() {
        GameState s = game.state;
        if (s == null) return;
        Family p = s.player();
        purse.setText(p.florins + " florins");
        standing.setText("Reputation " + p.reputation + "/100    Senators bribed: " + p.senatorsBribed
                + "    Cardinals: " + p.cardinalsBribed + (p.denOfIniquities ? "    [Den active]" : ""));
        StringBuilder off = new StringBuilder("Offices held: ");
        if (p.offices.isEmpty()) off.append("none");
        else { boolean first = true; for (Office o : p.offices) { if (!first) off.append(", "); off.append(o.label); first = false; } }
        officesLabel.setText(off.toString());

        DefaultComboBoxModel<RivalItem> rm = new DefaultComboBoxModel<RivalItem>();
        for (Family f : s.families) if (f.id != s.playerId && !f.eliminated) rm.addElement(new RivalItem(f));
        rivalBox.setModel(rm);

        StringBuilder sb = new StringBuilder("<html>");
        for (Family f : s.families) {
            sb.append("House ").append(f.surname).append(": ").append(f.florins).append(" florins")
              .append(f.eliminated ? " (fallen)" : "").append("<br>");
        }
        sb.append("</html>");
        scoreboard.setText(sb.toString());
    }

    @Override public String name() { return Game.VENICE; }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(Palette.PARCHMENT);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override public Dimension getPreferredSize() { return new Dimension(900, 680); }

    private static final class RivalItem {
        final Family family;
        RivalItem(Family f) { this.family = f; }
        @Override public String toString() { return "House " + family.surname; }
    }
}
