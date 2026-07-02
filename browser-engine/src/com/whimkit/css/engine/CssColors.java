package com.whimkit.css.engine;

import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Parses CSS colour values into {@link java.awt.Color}.
 *
 * <p>Supports {@code #rgb}, {@code #rrggbb}, {@code #rgba}/{@code #rrggbbaa},
 * {@code rgb()}/{@code rgba()}, {@code hsl()}/{@code hsla()}, the
 * {@code transparent} keyword, and the standard CSS named-colour palette. Any
 * value it cannot understand yields {@code null} so callers can leave the
 * corresponding typed field untouched.</p>
 */
final class CssColors {

    /** Sentinel returned for {@code transparent} so callers can distinguish it from "unparsed". */
    static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    private static final Map<String, Color> NAMED = new HashMap<String, Color>();

    private CssColors() { }

    /** @return the colour, {@link #TRANSPARENT} for {@code transparent}, or {@code null} if unparsable. */
    static Color parse(String value) {
        if (value == null) return null;
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.isEmpty()) return null;
        if (v.equals("transparent")) return TRANSPARENT;
        if (v.equals("currentcolor")) return null; // resolved by caller from text colour
        if (v.charAt(0) == '#') return parseHex(v.substring(1));
        if (v.startsWith("rgb")) return parseRgb(v);
        if (v.startsWith("hsl")) return parseHsl(v);
        return NAMED.get(v);
    }

    private static Color parseHex(String h) {
        try {
            if (h.length() == 3 || h.length() == 4) {
                int r = hx(h.charAt(0)) * 17;
                int g = hx(h.charAt(1)) * 17;
                int b = hx(h.charAt(2)) * 17;
                int a = h.length() == 4 ? hx(h.charAt(3)) * 17 : 255;
                return new Color(r, g, b, a);
            }
            if (h.length() == 6 || h.length() == 8) {
                int r = Integer.parseInt(h.substring(0, 2), 16);
                int g = Integer.parseInt(h.substring(2, 4), 16);
                int b = Integer.parseInt(h.substring(4, 6), 16);
                int a = h.length() == 8 ? Integer.parseInt(h.substring(6, 8), 16) : 255;
                return new Color(r, g, b, a);
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return null;
    }

    private static int hx(char c) {
        int d = Character.digit(c, 16);
        if (d < 0) throw new NumberFormatException("bad hex digit " + c);
        return d;
    }

    private static Color parseRgb(String v) {
        String[] parts = argsOf(v);
        if (parts == null || parts.length < 3) return null;
        try {
            int r = channel(parts[0]);
            int g = channel(parts[1]);
            int b = channel(parts[2]);
            int a = parts.length >= 4 ? alpha(parts[3]) : 255;
            return new Color(clamp(r), clamp(g), clamp(b), clamp(a));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Color parseHsl(String v) {
        String[] parts = argsOf(v);
        if (parts == null || parts.length < 3) return null;
        try {
            float h = Float.parseFloat(parts[0].replace("deg", "").trim());
            float s = pct(parts[1]);
            float l = pct(parts[2]);
            int a = parts.length >= 4 ? alpha(parts[3]) : 255;
            int[] rgb = hslToRgb(((h % 360) + 360) % 360, s, l);
            return new Color(rgb[0], rgb[1], rgb[2], clamp(a));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String[] argsOf(String v) {
        int open = v.indexOf('(');
        int close = v.lastIndexOf(')');
        if (open < 0 || close < open) return null;
        String inner = v.substring(open + 1, close).trim();
        // Accept both comma and whitespace separation.
        return inner.split("[,\\s]+");
    }

    private static int channel(String s) {
        s = s.trim();
        if (s.endsWith("%")) {
            return Math.round(pct(s) * 255f);
        }
        return Math.round(Float.parseFloat(s));
    }

    private static int alpha(String s) {
        s = s.trim();
        if (s.endsWith("%")) return Math.round(pct(s) * 255f);
        return Math.round(Float.parseFloat(s) * 255f);
    }

    private static float pct(String s) {
        s = s.trim();
        if (s.endsWith("%")) s = s.substring(0, s.length() - 1);
        return Float.parseFloat(s) / 100f;
    }

    private static int clamp(int c) {
        return c < 0 ? 0 : (c > 255 ? 255 : c);
    }

    /** Converts HSL (h in [0,360), s/l in [0,1]) to 8-bit RGB. */
    private static int[] hslToRgb(float h, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = l - c / 2;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        return new int[]{
            clamp(Math.round((r + m) * 255)),
            clamp(Math.round((g + m) * 255)),
            clamp(Math.round((b + m) * 255))
        };
    }

    static {
        Map<String, Color> m = NAMED;
        m.put("black", Color.BLACK);
        m.put("white", Color.WHITE);
        m.put("red", new Color(0xFF0000));
        m.put("green", new Color(0x008000));
        m.put("blue", new Color(0x0000FF));
        m.put("yellow", new Color(0xFFFF00));
        m.put("cyan", new Color(0x00FFFF));
        m.put("aqua", new Color(0x00FFFF));
        m.put("magenta", new Color(0xFF00FF));
        m.put("fuchsia", new Color(0xFF00FF));
        m.put("gray", new Color(0x808080));
        m.put("grey", new Color(0x808080));
        m.put("silver", new Color(0xC0C0C0));
        m.put("maroon", new Color(0x800000));
        m.put("olive", new Color(0x808000));
        m.put("lime", new Color(0x00FF00));
        m.put("teal", new Color(0x008080));
        m.put("navy", new Color(0x000080));
        m.put("purple", new Color(0x800080));
        m.put("orange", new Color(0xFFA500));
        m.put("pink", new Color(0xFFC0CB));
        m.put("brown", new Color(0xA52A2A));
        m.put("gold", new Color(0xFFD700));
        m.put("indigo", new Color(0x4B0082));
        m.put("violet", new Color(0xEE82EE));
        m.put("darkred", new Color(0x8B0000));
        m.put("darkgreen", new Color(0x006400));
        m.put("darkblue", new Color(0x00008B));
        m.put("lightgray", new Color(0xD3D3D3));
        m.put("lightgrey", new Color(0xD3D3D3));
        m.put("lightblue", new Color(0xADD8E6));
        m.put("lightgreen", new Color(0x90EE90));
        m.put("dimgray", new Color(0x696969));
        m.put("dimgrey", new Color(0x696969));
        m.put("whitesmoke", new Color(0xF5F5F5));
        m.put("gainsboro", new Color(0xDCDCDC));
        m.put("beige", new Color(0xF5F5DC));
        m.put("ivory", new Color(0xFFFFF0));
        m.put("coral", new Color(0xFF7F50));
        m.put("salmon", new Color(0xFA8072));
        m.put("tomato", new Color(0xFF6347));
        m.put("crimson", new Color(0xDC143C));
        m.put("skyblue", new Color(0x87CEEB));
        m.put("steelblue", new Color(0x4682B4));
        m.put("royalblue", new Color(0x4169E1));
        m.put("dodgerblue", new Color(0x1E90FF));
        m.put("cornflowerblue", new Color(0x6495ED));
        m.put("turquoise", new Color(0x40E0D0));
        m.put("khaki", new Color(0xF0E68C));
        m.put("tan", new Color(0xD2B48C));
        m.put("chocolate", new Color(0xD2691E));
        m.put("firebrick", new Color(0xB22222));
        m.put("goldenrod", new Color(0xDAA520));
        m.put("darkgray", new Color(0xA9A9A9));
        m.put("darkgrey", new Color(0xA9A9A9));
        m.put("lightyellow", new Color(0xFFFFE0));
        m.put("lightpink", new Color(0xFFB6C1));
        m.put("hotpink", new Color(0xFF69B4));
        m.put("seagreen", new Color(0x2E8B57));
        m.put("forestgreen", new Color(0x228B22));
        m.put("slategray", new Color(0x708090));
        m.put("slategrey", new Color(0x708090));
    }
}
