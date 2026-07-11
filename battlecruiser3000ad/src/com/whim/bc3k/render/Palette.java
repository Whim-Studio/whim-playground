package com.whim.bc3k.render;

import com.whim.bc3k.api.Enums;
import java.awt.Color;

/** Shared colour tokens so all consoles read as one system. Read-only for tasks. */
public final class Palette {
    private Palette() {}

    public static final Color BG          = new Color(0x060A12);
    public static final Color BG_PANEL    = new Color(0x101827);
    public static final Color GRID        = new Color(0x22324C);
    public static final Color INK         = new Color(0xE6ECF5);
    public static final Color INK_DIM     = new Color(0x8797AE);
    public static final Color ACCENT      = new Color(0x39C7C7); // Whim teal
    public static final Color ACCENT_WARM = new Color(0xF0A93B);
    public static final Color GOOD        = new Color(0x4FD08A);
    public static final Color BAD         = new Color(0xE0556B);
    public static final Color HULL        = new Color(0x9BB0CC);
    public static final Color SHIELD      = new Color(0x5B8DEF);

    public static Color alert(Enums.Alert a) {
        switch (a) {
            case RED:    return BAD;
            case YELLOW: return ACCENT_WARM;
            default:     return GOOD;
        }
    }

    /** A signature accent per bridge console, used for headers/console tabs. */
    public static Color console(Enums.Mode m) {
        switch (m) {
            case NAV:         return new Color(0x2E6E8E);
            case TACTICAL:    return new Color(0x8E2E3E);
            case ENGINEERING: return new Color(0x8E5A2E);
            case POWER:       return new Color(0x2E4C8E);
            case COMMS:       return new Color(0x2E8E8E);
            case CARGO:       return new Color(0x4A4030);
            case PERSONNEL:   return new Color(0x2E8E5A);
            case FLIGHTDECK:  return new Color(0x6E2E8E);
            default:          return GRID;
        }
    }
}
