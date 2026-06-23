package com.tiwas.mahjong.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import com.tiwas.mahjong.model.Tile;
import com.tiwas.mahjong.model.TileSuit;

/**
 * A small clickable representation of one tile, drawn as a coloured code such as
 * "5D" (5 of Dots). Used both for the human's interactive hand and for showing
 * melds / discards.
 */
public final class TileView extends JButton {

    public final Tile tile;

    public TileView(Tile tile, boolean interactive) {
        super(tile.code());
        this.tile = tile;
        setFont(new Font("Monospaced", Font.BOLD, 16));
        setForeground(colorFor(tile));
        setBackground(Color.WHITE);
        setOpaque(true);
        setFocusable(false);
        setToolTipText(tile.displayName());
        setMargin(new java.awt.Insets(2, 2, 2, 2));
        setPreferredSize(new Dimension(46, 60));
        setBorder(BorderFactory.createLineBorder(new Color(120, 90, 40), 2, true));
        setEnabled(interactive);
    }

    private static Color colorFor(Tile t) {
        TileSuit s = t.getSuit();
        if (s == TileSuit.DOTS) {
            return new Color(20, 70, 170);
        }
        if (s == TileSuit.BAMBOO) {
            return new Color(20, 120, 40);
        }
        if (s == TileSuit.CHARACTERS) {
            return new Color(170, 30, 30);
        }
        if (s == TileSuit.WIND) {
            return new Color(70, 40, 120);
        }
        if (s == TileSuit.DRAGON) {
            return new Color(150, 90, 0);
        }
        return new Color(200, 120, 0); // flowers / seasons
    }
}
