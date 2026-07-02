package com.whimkit.app;

import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Tiny color/length parser for the fallback style engine (the CSS subsystem has its own). */
final class Colors {

    private static final Map<String, Color> NAMED = new HashMap<String, Color>();
    static {
        NAMED.put("black", Color.BLACK); NAMED.put("white", Color.WHITE);
        NAMED.put("red", Color.RED); NAMED.put("green", new Color(0, 128, 0));
        NAMED.put("blue", Color.BLUE); NAMED.put("gray", Color.GRAY); NAMED.put("grey", Color.GRAY);
        NAMED.put("silver", new Color(0xC0C0C0)); NAMED.put("navy", new Color(0, 0, 128));
        NAMED.put("orange", Color.ORANGE); NAMED.put("yellow", Color.YELLOW);
        NAMED.put("purple", new Color(0x800080)); NAMED.put("teal", new Color(0, 128, 128));
        NAMED.put("maroon", new Color(0x800000)); NAMED.put("lime", new Color(0, 255, 0));
        NAMED.put("transparent", null);
    }

    private Colors() {}

    static Color parse(String v, Color def) {
        if (v == null) return def;
        v = v.trim().toLowerCase(Locale.ROOT);
        if (v.isEmpty()) return def;
        if (NAMED.containsKey(v)) return NAMED.get(v);
        try {
            if (v.startsWith("#")) {
                String h = v.substring(1);
                if (h.length() == 3) {
                    int r = Integer.parseInt("" + h.charAt(0) + h.charAt(0), 16);
                    int g = Integer.parseInt("" + h.charAt(1) + h.charAt(1), 16);
                    int b = Integer.parseInt("" + h.charAt(2) + h.charAt(2), 16);
                    return new Color(r, g, b);
                }
                if (h.length() >= 6) return new Color(Integer.parseInt(h.substring(0, 6), 16));
            }
            if (v.startsWith("rgb")) {
                String inner = v.substring(v.indexOf('(') + 1, v.indexOf(')'));
                String[] parts = inner.split(",");
                int r = clamp(parts[0]), g = clamp(parts[1]), b = clamp(parts[2]);
                return new Color(r, g, b);
            }
        } catch (RuntimeException ignore) { }
        return def;
    }

    static float px(String v, float def) {
        if (v == null) return def;
        v = v.trim().toLowerCase(Locale.ROOT);
        try {
            if (v.endsWith("px")) return Float.parseFloat(v.substring(0, v.length() - 2).trim());
            if (v.endsWith("pt")) return Float.parseFloat(v.substring(0, v.length() - 2).trim()) * 1.333f;
            return Float.parseFloat(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int clamp(String s) {
        int x = (int) Float.parseFloat(s.trim());
        return Math.max(0, Math.min(255, x));
    }
}
