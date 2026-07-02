package com.whimkit.css;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolved, absolute style values for one element after the CSS cascade.
 *
 * <p>This is the single most important cross-subsystem contract in the engine.
 * The <b>CSS engine</b> produces one {@code ComputedStyle} per element (applying
 * selector matching, specificity, the cascade, and inheritance). The <b>layout
 * engine</b> and <b>renderer</b> consume it and must never re-parse CSS. Because
 * it is a plain data holder in the foundation package, all three subsystems
 * compile against it independently.</p>
 *
 * <h3>Value conventions</h3>
 * <ul>
 *   <li>All lengths are resolved to CSS pixels as {@code float}.</li>
 *   <li>{@link #width}/{@link #height} use {@code null} to mean {@code auto}.</li>
 *   <li>Colors use {@link java.awt.Color}; {@code null} background means
 *       transparent, {@code null} border color falls back to the text color.</li>
 *   <li>{@link #display} is a lower-case keyword: {@code block}, {@code inline},
 *       {@code inline-block}, {@code list-item}, {@code none}, {@code table}
 *       (treated as block).</li>
 * </ul>
 *
 * <p>A raw string map ({@link #raw}) is kept for any declared property the typed
 * fields do not cover, so the CSS engine can round-trip {@code getPropertyValue}
 * for the JS bindings without losing data.</p>
 */
public class ComputedStyle {

    // ---- box / display ----
    public String display = "inline";
    public String position = "static";           // static | relative | absolute | fixed
    public String floatValue = "none";           // none | left | right
    public String clear = "none";

    // ---- geometry (px; null width/height == auto) ----
    public Float width = null;
    public Float height = null;

    public float marginTop, marginRight, marginBottom, marginLeft;
    public float paddingTop, paddingRight, paddingBottom, paddingLeft;
    public float borderTopWidth, borderRightWidth, borderBottomWidth, borderLeftWidth;

    // ---- colors ----
    public Color color = Color.BLACK;             // inherited
    public Color backgroundColor = null;          // null == transparent
    public Color borderColor = null;              // null == use text color

    // ---- text / font (all inherited) ----
    public String fontFamily = "SansSerif";
    public float fontSize = 16f;                  // px
    public int fontWeight = 400;                  // 100..900; >=700 == bold
    public boolean italic = false;
    public float lineHeight = 19f;                // px
    public String textAlign = "left";            // left | right | center | justify
    public boolean underline = false;
    public boolean lineThrough = false;
    public String whiteSpace = "normal";          // normal | nowrap | pre
    public String visibility = "visible";
    public String listStyleType = "disc";

    /** Any declared property not modelled by a typed field above. */
    public final Map<String, String> raw = new HashMap<String, String>();

    /** @return {@code true} when the element (and its subtree) must not generate boxes. */
    public boolean isNone() {
        return "none".equals(display);
    }

    public boolean isBlockLevel() {
        return "block".equals(display) || "list-item".equals(display) || "table".equals(display);
    }

    public boolean isBold() {
        return fontWeight >= 700;
    }

    /**
     * Produces a fresh style for a child, seeding CSS <em>inherited</em>
     * properties from this (the parent) style and leaving non-inherited
     * properties at their initial values. The CSS engine then overlays declared
     * values on top of the returned object.
     *
     * @return a new {@code ComputedStyle} pre-populated with inherited values.
     */
    public ComputedStyle deriveChild() {
        ComputedStyle c = new ComputedStyle();
        // Inherited properties (per CSS 2.1 inheritance rules).
        c.color = this.color;
        c.fontFamily = this.fontFamily;
        c.fontSize = this.fontSize;
        c.fontWeight = this.fontWeight;
        c.italic = this.italic;
        c.lineHeight = this.lineHeight;
        c.textAlign = this.textAlign;
        c.whiteSpace = this.whiteSpace;
        c.visibility = this.visibility;
        c.listStyleType = this.listStyleType;
        c.underline = this.underline;
        c.lineThrough = this.lineThrough;
        // Non-inherited stay at initial defaults (display resets to inline).
        return c;
    }

    /** @return an independent copy of this style. */
    public ComputedStyle copy() {
        ComputedStyle c = new ComputedStyle();
        c.display = display; c.position = position; c.floatValue = floatValue; c.clear = clear;
        c.width = width; c.height = height;
        c.marginTop = marginTop; c.marginRight = marginRight; c.marginBottom = marginBottom; c.marginLeft = marginLeft;
        c.paddingTop = paddingTop; c.paddingRight = paddingRight; c.paddingBottom = paddingBottom; c.paddingLeft = paddingLeft;
        c.borderTopWidth = borderTopWidth; c.borderRightWidth = borderRightWidth;
        c.borderBottomWidth = borderBottomWidth; c.borderLeftWidth = borderLeftWidth;
        c.color = color; c.backgroundColor = backgroundColor; c.borderColor = borderColor;
        c.fontFamily = fontFamily; c.fontSize = fontSize; c.fontWeight = fontWeight; c.italic = italic;
        c.lineHeight = lineHeight; c.textAlign = textAlign; c.underline = underline; c.lineThrough = lineThrough;
        c.whiteSpace = whiteSpace; c.visibility = visibility; c.listStyleType = listStyleType;
        c.raw.putAll(raw);
        return c;
    }

    /** @return the initial style used for the root before any author rules apply. */
    public static ComputedStyle initial() {
        return new ComputedStyle();
    }
}
