package com.whim.samurai.ui;

import com.whim.samurai.app.Game;
import com.whim.samurai.app.Screen;
import com.whim.samurai.render.Palette;

import java.awt.Graphics;
import java.awt.Graphics2D;

/** [SKELETON STUB] Replace with the real Sword of the Samurai screen. Keep the (Game) ctor and name(). */
public class MainMenuScreen extends Screen {
    public MainMenuScreen(Game game) { super(game); }
    public String name() { return Game.MENU; }
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        UiKit.aa(g);
        UiKit.paperBackground(g, getWidth(), getHeight());
        g.setColor(Palette.INK);
        g.setFont(UiKit.TITLE);
        g.drawString("Sword of the Samurai", 60, 120);
        g.setFont(UiKit.BODY);
        g.setColor(Palette.DIM);
        g.drawString("[ stub — under construction ]", 60, 160);
    }
}
