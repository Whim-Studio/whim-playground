package com.whim.scg.render;

import com.whim.scg.api.Enums;
import java.awt.Color;

/** Shared colour tokens so all screens read as one system. Read-only for tasks. */
public final class Palette {
    private Palette() {}

    public static final Color BG          = new Color(0x0A0E17);
    public static final Color BG_PANEL    = new Color(0x141A27);
    public static final Color GRID        = new Color(0x223049);
    public static final Color INK         = new Color(0xE6ECF5);
    public static final Color INK_DIM     = new Color(0x8797AE);
    public static final Color ACCENT      = new Color(0x39C7C7); // Whim teal
    public static final Color ACCENT_WARM = new Color(0xF0A93B);
    public static final Color GOOD        = new Color(0x4FD08A);
    public static final Color BAD         = new Color(0xE0556B);
    public static final Color HULL        = new Color(0x9BB0CC);
    public static final Color SHIELD      = new Color(0x5B8DEF);
    public static final Color FIRE        = new Color(0xF2703A);
    public static final Color BREACH      = new Color(0x1B2233);

    public static Color room(Enums.RoomType t) {
        switch (t) {
            case BRIDGE:     return new Color(0x2E6E8E);
            case ENGINES:    return new Color(0x8E5A2E);
            case WEAPONS:    return new Color(0x8E2E3E);
            case SHIELDS:    return new Color(0x2E4C8E);
            case MEDBAY:     return new Color(0x2E8E5A);
            case TELEPORTER: return new Color(0x6E2E8E);
            case OXYGEN:     return new Color(0x2E8E8E);
            case SENSORS:    return new Color(0x5A2E8E);
            case QUARTERS:   return new Color(0x3A4258);
            case CARGO:      return new Color(0x4A4030);
            default:         return new Color(0x1D2534);
        }
    }

    public static Color faction(Enums.Faction f) {
        switch (f) {
            case FEDERATION: return ACCENT;
            case PIRATE:     return BAD;
            case ALIEN_SWARM:return new Color(0x8ED04F);
            case MERCHANT:   return ACCENT_WARM;
            default:         return INK_DIM;
        }
    }
}
