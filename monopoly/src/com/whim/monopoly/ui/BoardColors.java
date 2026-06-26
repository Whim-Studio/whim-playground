package com.whim.monopoly.ui;

import java.awt.Color;

import com.whim.monopoly.domain.ColorGroup;

/**
 * Central palette for the board UI. Maps each street {@link ColorGroup} to its
 * printed band color and holds a few shared chrome colors.
 */
final class BoardColors {

    private BoardColors() {
    }

    static final Color BOARD_BG = new Color(196, 224, 199);
    static final Color CELL_BG = new Color(252, 250, 244);
    static final Color BORDER = new Color(20, 20, 20);
    static final Color MORTGAGE_TINT = new Color(120, 120, 120, 90);
    static final Color HOUSE = new Color(36, 160, 76);
    static final Color HOTEL = new Color(214, 38, 38);

    static Color of(ColorGroup group) {
        if (group == null) {
            return Color.GRAY;
        }
        switch (group) {
            case BROWN:
                return new Color(149, 84, 54);
            case LIGHT_BLUE:
                return new Color(170, 224, 250);
            case PINK:
                return new Color(217, 58, 150);
            case ORANGE:
                return new Color(247, 148, 29);
            case RED:
                return new Color(237, 27, 36);
            case YELLOW:
                return new Color(254, 242, 0);
            case GREEN:
                return new Color(31, 160, 76);
            case DARK_BLUE:
                return new Color(0, 114, 187);
            default:
                return Color.GRAY;
        }
    }
}
