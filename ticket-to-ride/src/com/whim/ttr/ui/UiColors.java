package com.whim.ttr.ui;

import com.whim.ttr.api.CardColor;

import java.awt.Color;

/**
 * Maps the abstract {@link CardColor} palette to concrete AWT colors used
 * consistently across the board, the card market and the dashboard.
 */
final class UiColors {

    private UiColors() { }

    /** Fill color for a card / route of the given color. {@code null} == GRAY route. */
    static Color of(CardColor c) {
        if (c == null) {
            return new Color(150, 150, 150);
        }
        switch (c) {
            case PURPLE:     return new Color(155, 60, 160);
            case BLUE:       return new Color(40, 90, 200);
            case ORANGE:     return new Color(235, 140, 30);
            case WHITE:      return new Color(240, 240, 240);
            case GREEN:      return new Color(40, 155, 70);
            case YELLOW:     return new Color(240, 205, 40);
            case BLACK:      return new Color(45, 45, 50);
            case RED:        return new Color(200, 45, 45);
            case LOCOMOTIVE: return new Color(215, 215, 220);
            default:         return new Color(150, 150, 150);
        }
    }

    /** A readable text color to paint on top of {@link #of(CardColor)}. */
    static Color textOn(CardColor c) {
        if (c == null) {
            return Color.BLACK;
        }
        switch (c) {
            case WHITE:
            case YELLOW:
            case LOCOMOTIVE:
                return Color.BLACK;
            default:
                return Color.WHITE;
        }
    }

    /** Short label for a card color used on compact chips. */
    static String label(CardColor c) {
        if (c == null) {
            return "GRAY";
        }
        if (c == CardColor.LOCOMOTIVE) {
            return "LOCO";
        }
        return c.name();
    }
}
