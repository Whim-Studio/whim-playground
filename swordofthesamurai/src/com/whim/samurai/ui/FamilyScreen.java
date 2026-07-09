package com.whim.samurai.ui;

import com.whim.samurai.app.Game;
import com.whim.samurai.app.Screen;
import com.whim.samurai.engine.FamilyEngine;
import com.whim.samurai.model.FamilyMember;
import com.whim.samurai.model.Samurai;
import com.whim.samurai.render.Palette;

import javax.swing.JButton;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * The Household (design ref §5) — wife, children and heir status, plus a Marriage
 * action, notice of any kidnapped members (with the rescue path), and a short note
 * on succession. Marriage proposes a match via {@link FamilyEngine#marry}. Rescues
 * themselves are carried out through the map/action screens, not here.
 */
public class FamilyScreen extends Screen {

    private final JButton marryBtn;
    private String message = "";

    public FamilyScreen(Game game) {
        super(game);
        setLayout(null);

        marryBtn = UiKit.button("Propose Marriage");
        marryBtn.setBounds(60, 470, 240, 44);
        marryBtn.addActionListener(e -> {
            message = FamilyEngine.marry(game);
            refresh();
        });
        add(marryBtn);

        JButton back = UiKit.button("Back to Map  (Esc)");
        back.setBounds(320, 470, 220, 44);
        back.addActionListener(e -> game.screens.show(Game.MAP));
        add(back);

        Keys.bind(this, "ESCAPE", () -> game.screens.show(Game.MAP));
    }

    public String name() { return Game.FAMILY; }

    @Override public void onShow() { message = ""; refresh(); }

    private void refresh() {
        marryBtn.setEnabled(FamilyEngine.canMarry(game));
        repaint();
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        UiKit.aa(g);
        UiKit.paperBackground(g, getWidth(), getHeight());

        g.setColor(Palette.INK);
        g.setFont(UiKit.TITLE);
        g.drawString("The Household", 60, 80);

        Samurai p = (game.state != null) ? game.state.player : null;
        if (p == null) {
            g.setFont(UiKit.BODY);
            g.setColor(Palette.DIM);
            g.drawString("No household — begin a new game.", 62, 120);
            return;
        }

        int x = 62, y = 128;
        g.setFont(UiKit.HEAD);
        g.setColor(Palette.CINNABAR_DK);
        g.drawString("Wife", x, y);
        g.setFont(UiKit.BODY);
        g.setColor(Palette.INK);
        y += 26;
        if (p.isMarried()) {
            g.drawString(p.wife.name + "  (age " + p.wife.age + ")"
                    + (p.wife.kidnapped ? "  — HELD HOSTAGE" : ""), x, y);
        } else {
            g.setColor(Palette.DIM);
            g.drawString("Unmarried. A samurai needs a wife to manage his house and bear an heir.", x, y);
            g.setColor(Palette.INK);
        }

        // Children
        y += 44;
        g.setFont(UiKit.HEAD);
        g.setColor(Palette.CINNABAR_DK);
        g.drawString("Children", x, y);
        g.setFont(UiKit.BODY);
        g.setColor(Palette.INK);
        y += 26;
        if (p.children.isEmpty()) {
            g.setColor(Palette.DIM);
            g.drawString("No children yet — your house awaits an heir.", x, y);
            g.setColor(Palette.INK);
            y += 24;
        } else {
            for (FamilyMember c : p.children) {
                String kind = c.relation == FamilyMember.Relation.SON ? "Son" : "Daughter";
                String tag = "";
                if (!c.alive) tag = "  (deceased)";
                else if (c.kidnapped) tag = "  — HELD HOSTAGE";
                else if (c.isHeirEligible()) tag = "  ← heir";
                g.setColor(c.kidnapped ? Palette.CINNABAR : Palette.INK);
                g.drawString("• " + kind + " " + c.name + ", age " + c.age + tag, x, y);
                y += 24;
            }
            g.setColor(Palette.INK);
        }

        // Heir / succession note
        y += 20;
        g.setFont(UiKit.HEAD);
        g.setColor(Palette.CINNABAR_DK);
        g.drawString("Succession", x, y);
        g.setFont(UiKit.BODY);
        g.setColor(Palette.INK);
        y += 26;
        if (p.hasHeir()) {
            g.drawString("Your heir is " + p.heir().name + ". Should you fall, he takes your seat and the", x, y);
            y += 22;
            g.drawString("quest continues — a weaker man at first, but building on your legacy.", x, y);
        } else {
            g.setColor(Palette.CINNABAR_DK);
            g.drawString("You have NO eligible heir. Dying or retiring now ends the dynasty (design ref §5.4).", x, y);
            g.setColor(Palette.INK);
            y += 22;
            g.drawString("Sons become eligible heirs at age 15; daughters may wed peers to seal alliances.", x, y);
        }

        // Kidnap / rescue notice
        if (anyKidnapped(p)) {
            y += 40;
            g.setFont(UiKit.HEAD);
            g.setColor(Palette.CINNABAR);
            g.drawString("A member of your house is held hostage!", x, y);
            g.setFont(UiKit.BODY);
            g.setColor(Palette.INK_SOFT);
            y += 24;
            g.drawString("Mount a rescue from the map: infiltrate the captor's castle disguised (design ref §5.3).", x, y);
        }

        // transient message from the last action
        if (message != null && !message.isEmpty()) {
            g.setFont(UiKit.BODY);
            g.setColor(Palette.JADE);
            g.drawString(message, x, 448);
        }
    }

    private boolean anyKidnapped(Samurai p) {
        if (p.wife != null && p.wife.kidnapped) return true;
        for (FamilyMember c : p.children) if (c.alive && c.kidnapped) return true;
        return false;
    }
}
