package com.whimkit.layout.engine;

import com.whimkit.css.ComputedStyle;

import java.awt.Font;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps a {@link ComputedStyle} to a concrete AWT {@link Font}, with caching.
 *
 * <p>Layout and rendering must agree on the font used for a run of text, so text
 * measured during layout paints at the same width. Both stages resolve fonts
 * through this single helper (the renderer's own {@code FontFactory} mirrors the
 * same family mapping) to keep them consistent.</p>
 */
public final class Fonts {

    private static final Map<String, Font> CACHE = new HashMap<String, Font>();

    private Fonts() {}

    /** Resolves the AWT font family logical name for a CSS {@code font-family} list. */
    public static String family(String cssFamily) {
        if (cssFamily == null) return Font.SANS_SERIF;
        String f = cssFamily.toLowerCase(Locale.ROOT);
        if (f.contains("mono") || f.contains("courier") || f.contains("consol")) return Font.MONOSPACED;
        if (f.contains("serif") && !f.contains("sans")) return Font.SERIF;
        return Font.SANS_SERIF;
    }

    public static Font forStyle(ComputedStyle s) {
        if (s == null) return new Font(Font.SANS_SERIF, Font.PLAIN, 16);
        int style = Font.PLAIN;
        if (s.isBold()) style |= Font.BOLD;
        if (s.italic) style |= Font.ITALIC;
        int size = Math.max(1, Math.round(s.fontSize));
        String fam = family(s.fontFamily);
        String key = fam + '|' + style + '|' + size;
        Font f = CACHE.get(key);
        if (f == null) {
            f = new Font(fam, style, size);
            CACHE.put(key, f);
        }
        return f;
    }
}
