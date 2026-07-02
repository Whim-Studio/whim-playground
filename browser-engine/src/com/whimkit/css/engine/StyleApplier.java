package com.whimkit.css.engine;

import com.whimkit.css.ComputedStyle;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Applies a single CSS {@code property: value} declaration onto a
 * {@link ComputedStyle}, translating keywords, colours, lengths and shorthands
 * into the style object's typed fields.
 *
 * <p>The applier is intentionally forgiving: an unrecognised property, or a
 * value it cannot resolve, is stashed in {@link ComputedStyle#raw} rather than
 * throwing, so {@code getPropertyValue} can still round-trip it and the cascade
 * never aborts on bad input.</p>
 *
 * <p>Length resolution needs font metrics, so callers pass a {@link Context}
 * carrying the element's own font-size (for {@code em}), the parent font-size
 * (for {@code font-size: Nem}) and the root font-size (for {@code rem}).</p>
 */
final class StyleApplier {

    /** Font/length resolution context for one element. */
    static final class Context {
        float ownFontSize;    // em basis for non-font-size properties
        float parentFontSize; // em basis for font-size itself
        float rootFontSize;   // rem basis
        Context(float own, float parent, float root) {
            this.ownFontSize = own;
            this.parentFontSize = parent;
            this.rootFontSize = root;
        }
    }

    private static final Set<String> DISPLAY_KEYWORDS = new HashSet<String>(Arrays.asList(
        "block", "inline", "inline-block", "list-item", "none", "table",
        "inline-table", "table-row", "table-cell", "flex", "inline-flex",
        "grid", "inline-grid"));

    private static final Set<String> BORDER_STYLES = new HashSet<String>(Arrays.asList(
        "none", "hidden", "solid", "dotted", "dashed", "double", "groove",
        "ridge", "inset", "outset"));

    private StyleApplier() { }

    /**
     * Applies one declaration to {@code cs}.
     *
     * @param cs   the style being built (mutated in place)
     * @param prop lower-cased property name
     * @param val  raw value text (without {@code !important})
     * @param ctx  length/font resolution context
     */
    static void apply(ComputedStyle cs, String prop, String val, Context ctx) {
        try {
            if (!applyKnown(cs, prop, val, ctx)) {
                cs.raw.put(prop, val);
            }
        } catch (RuntimeException ex) {
            // Never let a single bad declaration break the cascade.
            cs.raw.put(prop, val);
        }
    }

    private static boolean applyKnown(ComputedStyle cs, String prop, String val, Context ctx) {
        String v = val.trim();
        String lv = v.toLowerCase(Locale.ROOT);
        switch (prop) {
            case "display":
                if (DISPLAY_KEYWORDS.contains(lv)) { cs.display = normalizeDisplay(lv); return true; }
                return false;
            case "position":
                if (lv.equals("static") || lv.equals("relative")
                        || lv.equals("absolute") || lv.equals("fixed") || lv.equals("sticky")) {
                    cs.position = lv; return true;
                }
                return false;
            case "float":
                if (lv.equals("none") || lv.equals("left") || lv.equals("right")) {
                    cs.floatValue = lv; return true;
                }
                return false;
            case "clear":
                if (lv.equals("none") || lv.equals("left") || lv.equals("right") || lv.equals("both")) {
                    cs.clear = lv; return true;
                }
                return false;

            case "width":  cs.width  = dimension(lv, ctx); return true;
            case "height": cs.height = dimension(lv, ctx); return true;

            case "color": {
                Color c = CssColors.parse(v);
                if (c != null && c != CssColors.TRANSPARENT) { cs.color = c; return true; }
                if (c == CssColors.TRANSPARENT) { cs.color = c; return true; }
                return false;
            }
            case "background-color": {
                Color c = CssColors.parse(v);
                if (c != null) { cs.backgroundColor = (c == CssColors.TRANSPARENT) ? null : c; return true; }
                return false;
            }
            case "background":
                return applyBackground(cs, v);

            case "font-size":
                return applyFontSize(cs, lv, ctx);
            case "font-weight":
                return applyFontWeight(cs, lv);
            case "font-style":
                if (lv.equals("normal")) { cs.italic = false; return true; }
                if (lv.equals("italic") || lv.equals("oblique")) { cs.italic = true; return true; }
                return false;
            case "font-family":
                cs.fontFamily = firstFamily(v); return true;
            case "font":
                return applyFontShorthand(cs, v, ctx);
            case "line-height":
                return applyLineHeight(cs, lv, ctx);

            case "text-align":
                if (lv.equals("left") || lv.equals("right") || lv.equals("center")
                        || lv.equals("justify") || lv.equals("start") || lv.equals("end")) {
                    cs.textAlign = normalizeAlign(lv); return true;
                }
                return false;
            case "text-decoration":
            case "text-decoration-line":
                return applyTextDecoration(cs, lv);
            case "white-space":
                if (lv.equals("normal") || lv.equals("nowrap") || lv.equals("pre")
                        || lv.equals("pre-wrap") || lv.equals("pre-line")) {
                    cs.whiteSpace = lv; return true;
                }
                return false;
            case "visibility":
                if (lv.equals("visible") || lv.equals("hidden") || lv.equals("collapse")) {
                    cs.visibility = lv; return true;
                }
                return false;
            case "list-style-type":
                cs.listStyleType = lv; return true;
            case "list-style":
                return applyListStyle(cs, lv);

            case "margin":            return applyBox(cs, lv, ctx, BoxTarget.MARGIN);
            case "margin-top":        cs.marginTop = len(lv, ctx); return true;
            case "margin-right":      cs.marginRight = len(lv, ctx); return true;
            case "margin-bottom":     cs.marginBottom = len(lv, ctx); return true;
            case "margin-left":       cs.marginLeft = len(lv, ctx); return true;

            case "padding":           return applyBox(cs, lv, ctx, BoxTarget.PADDING);
            case "padding-top":       cs.paddingTop = nonNeg(len(lv, ctx)); return true;
            case "padding-right":     cs.paddingRight = nonNeg(len(lv, ctx)); return true;
            case "padding-bottom":    cs.paddingBottom = nonNeg(len(lv, ctx)); return true;
            case "padding-left":      cs.paddingLeft = nonNeg(len(lv, ctx)); return true;

            case "border":            return applyBorderShorthand(cs, v, ctx, null);
            case "border-top":        return applyBorderShorthand(cs, v, ctx, "top");
            case "border-right":      return applyBorderShorthand(cs, v, ctx, "right");
            case "border-bottom":     return applyBorderShorthand(cs, v, ctx, "bottom");
            case "border-left":       return applyBorderShorthand(cs, v, ctx, "left");
            case "border-width":      return applyBorderWidth(cs, lv, ctx);
            case "border-top-width":    cs.borderTopWidth = borderWidth(lv, ctx); return true;
            case "border-right-width":  cs.borderRightWidth = borderWidth(lv, ctx); return true;
            case "border-bottom-width": cs.borderBottomWidth = borderWidth(lv, ctx); return true;
            case "border-left-width":   cs.borderLeftWidth = borderWidth(lv, ctx); return true;
            case "border-color": {
                Color c = CssColors.parse(firstToken(v));
                if (c != null) { cs.borderColor = (c == CssColors.TRANSPARENT) ? null : c; return true; }
                return false;
            }
            case "border-style":
                // Recognised but not modelled as a typed field; keep raw for round-trip.
                return false;
            default:
                return false;
        }
    }

    // --- font -------------------------------------------------------------

    private static boolean applyFontSize(ComputedStyle cs, String lv, Context ctx) {
        Float named = namedFontSize(lv, ctx.parentFontSize);
        if (named != null) { cs.fontSize = named; return true; }
        Float px = CssLength.resolve(lv, ctx.parentFontSize, ctx.rootFontSize, ctx.parentFontSize);
        if (px != null && px >= 0) {
            cs.fontSize = px;
            ctx.ownFontSize = px; // keep em basis in sync for later properties
            return true;
        }
        return false;
    }

    private static Float namedFontSize(String lv, float parent) {
        switch (lv) {
            case "xx-small": return 9f;
            case "x-small":  return 10f;
            case "small":    return 13f;
            case "medium":   return 16f;
            case "large":    return 18f;
            case "x-large":  return 24f;
            case "xx-large": return 32f;
            case "smaller":  return parent * 0.833f;
            case "larger":   return parent * 1.2f;
            default:         return null;
        }
    }

    private static boolean applyFontWeight(ComputedStyle cs, String lv) {
        switch (lv) {
            case "normal": cs.fontWeight = 400; return true;
            case "bold":   cs.fontWeight = 700; return true;
            case "bolder": cs.fontWeight = Math.min(900, cs.fontWeight + 300); return true;
            case "lighter": cs.fontWeight = Math.max(100, cs.fontWeight - 300); return true;
            default:
                Float n = CssLength.asNumber(lv);
                if (n != null) {
                    int w = Math.round(n);
                    if (w >= 1 && w <= 1000) { cs.fontWeight = w; return true; }
                }
                return false;
        }
    }

    private static boolean applyLineHeight(ComputedStyle cs, String lv, Context ctx) {
        if (lv.equals("normal")) { cs.lineHeight = cs.fontSize * 1.2f; return true; }
        // A unitless number multiplies the font size.
        if (!lv.endsWith("%") && !hasUnit(lv)) {
            Float n = CssLength.asNumber(lv);
            if (n != null && n >= 0) { cs.lineHeight = n * cs.fontSize; return true; }
            return false;
        }
        Float px = CssLength.resolve(lv, ctx.ownFontSize, ctx.rootFontSize, cs.fontSize);
        if (px != null && px >= 0) { cs.lineHeight = px; return true; }
        return false;
    }

    private static boolean applyFontShorthand(ComputedStyle cs, String v, Context ctx) {
        // Simplified: [style|weight]* size[/line-height] family...
        String[] toks = v.trim().split("\\s+");
        int i = 0;
        boolean any = false;
        while (i < toks.length) {
            String t = toks[i].toLowerCase(Locale.ROOT);
            if (t.equals("italic") || t.equals("oblique")) { cs.italic = true; any = true; i++; }
            else if (t.equals("normal")) { i++; }
            else if (t.equals("bold") || CssLength.asNumber(t) != null && !t.contains(".") && isWeight(t)) {
                applyFontWeight(cs, t); any = true; i++;
            } else break;
        }
        if (i >= toks.length) return any;
        // size[/line-height]
        String sizeTok = toks[i];
        String sizePart = sizeTok;
        String lhPart = null;
        int slash = sizeTok.indexOf('/');
        if (slash >= 0) { sizePart = sizeTok.substring(0, slash); lhPart = sizeTok.substring(slash + 1); }
        if (applyFontSize(cs, sizePart.toLowerCase(Locale.ROOT), ctx)) any = true;
        if (lhPart != null) applyLineHeight(cs, lhPart.toLowerCase(Locale.ROOT), ctx);
        i++;
        if (i < toks.length) {
            StringBuilder fam = new StringBuilder();
            for (; i < toks.length; i++) { if (fam.length() > 0) fam.append(' '); fam.append(toks[i]); }
            cs.fontFamily = firstFamily(fam.toString());
            any = true;
        }
        return any;
    }

    private static boolean isWeight(String t) {
        Float n = CssLength.asNumber(t);
        return n != null && n >= 100 && n <= 900;
    }

    private static String firstFamily(String v) {
        String first = v.split(",")[0].trim();
        if (first.length() >= 2) {
            char a = first.charAt(0), z = first.charAt(first.length() - 1);
            if ((a == '"' || a == '\'') && a == z) first = first.substring(1, first.length() - 1);
        }
        return first.isEmpty() ? "SansSerif" : mapGenericFamily(first);
    }

    /** Maps CSS generic families onto Java logical font names Java2D understands. */
    private static String mapGenericFamily(String f) {
        switch (f.toLowerCase(Locale.ROOT)) {
            case "sans-serif": return "SansSerif";
            case "serif":      return "Serif";
            case "monospace":  return "Monospaced";
            case "cursive":    return "Serif";
            case "fantasy":    return "SansSerif";
            default:           return f;
        }
    }

    // --- text-decoration / background / list-style ------------------------

    private static boolean applyTextDecoration(ComputedStyle cs, String lv) {
        if (lv.equals("none")) { cs.underline = false; cs.lineThrough = false; return true; }
        boolean any = false;
        for (String t : lv.split("\\s+")) {
            if (t.equals("underline")) { cs.underline = true; any = true; }
            else if (t.equals("line-through")) { cs.lineThrough = true; any = true; }
            else if (t.equals("overline") || t.equals("blink")) { any = true; /* ignored */ }
        }
        return any;
    }

    private static boolean applyBackground(ComputedStyle cs, String v) {
        // Extract a colour token from the shorthand; ignore images/position/repeat.
        boolean found = false;
        for (String t : v.split("\\s+")) {
            if (t.toLowerCase(Locale.ROOT).startsWith("url(")) continue;
            Color c = CssColors.parse(t);
            if (c != null) {
                cs.backgroundColor = (c == CssColors.TRANSPARENT) ? null : c;
                found = true;
            }
        }
        // Whole-value forms like rgb(...) with internal spaces:
        if (!found) {
            Color c = CssColors.parse(v.trim());
            if (c != null) { cs.backgroundColor = (c == CssColors.TRANSPARENT) ? null : c; found = true; }
        }
        return found;
    }

    private static boolean applyListStyle(ComputedStyle cs, String lv) {
        boolean any = false;
        for (String t : lv.split("\\s+")) {
            if (t.startsWith("url(") || t.equals("inside") || t.equals("outside")) continue;
            if (t.equals("none") || t.equals("disc") || t.equals("circle") || t.equals("square")
                    || t.equals("decimal") || t.equals("decimal-leading-zero")
                    || t.equals("lower-alpha") || t.equals("upper-alpha")
                    || t.equals("lower-roman") || t.equals("upper-roman")
                    || t.equals("lower-latin") || t.equals("upper-latin")) {
                cs.listStyleType = t; any = true;
            }
        }
        return any;
    }

    // --- box shorthands (margin/padding) ----------------------------------

    private enum BoxTarget { MARGIN, PADDING }

    private static boolean applyBox(ComputedStyle cs, String lv, Context ctx, BoxTarget target) {
        String[] toks = lv.split("\\s+");
        float[] e = new float[4]; // top right bottom left
        boolean auto = false;
        for (int k = 0; k < toks.length && k < 4; k++) {
            if (toks[k].equals("auto")) { e[k] = 0f; auto = true; }
            else {
                Float px = CssLength.resolve(toks[k], ctx.ownFontSize, ctx.rootFontSize, Float.NaN);
                e[k] = px == null ? 0f : px;
            }
        }
        float top, right, bottom, left;
        switch (Math.min(toks.length, 4)) {
            case 1: top = right = bottom = left = e[0]; break;
            case 2: top = bottom = e[0]; right = left = e[1]; break;
            case 3: top = e[0]; right = left = e[1]; bottom = e[2]; break;
            default: top = e[0]; right = e[1]; bottom = e[2]; left = e[3]; break;
        }
        if (target == BoxTarget.MARGIN) {
            cs.marginTop = top; cs.marginRight = right; cs.marginBottom = bottom; cs.marginLeft = left;
        } else {
            cs.paddingTop = nonNeg(top); cs.paddingRight = nonNeg(right);
            cs.paddingBottom = nonNeg(bottom); cs.paddingLeft = nonNeg(left);
        }
        return true;
    }

    // --- border -----------------------------------------------------------

    private static boolean applyBorderShorthand(ComputedStyle cs, String v, Context ctx, String side) {
        // border: <width> || <style> || <color>  (any order, any subset)
        float width = 3f;         // "medium" default when a style is present
        boolean haveWidth = false, haveStyleNone = false, haveStyle = false;
        Color color = null;
        for (String t : v.trim().split("\\s+")) {
            String lt = t.toLowerCase(Locale.ROOT);
            if (BORDER_STYLES.contains(lt)) {
                haveStyle = true;
                if (lt.equals("none") || lt.equals("hidden")) haveStyleNone = true;
                continue;
            }
            Float w = borderWidthKeyword(lt);
            if (w == null) w = CssLength.resolve(lt, ctx.ownFontSize, ctx.rootFontSize, Float.NaN);
            if (w != null) { width = Math.max(0f, w); haveWidth = true; continue; }
            Color c = CssColors.parse(t);
            if (c != null) { color = c; continue; }
        }
        float finalWidth;
        if (haveStyleNone) finalWidth = 0f;
        else if (haveWidth) finalWidth = width;
        else if (haveStyle) finalWidth = width; // style present, default medium width
        else finalWidth = 0f;
        setBorderWidth(cs, side, finalWidth);
        if (color != null && color != CssColors.TRANSPARENT) cs.borderColor = color;
        return true;
    }

    private static boolean applyBorderWidth(ComputedStyle cs, String lv, Context ctx) {
        String[] toks = lv.split("\\s+");
        float[] e = new float[Math.min(toks.length, 4)];
        for (int k = 0; k < e.length; k++) e[k] = borderWidth(toks[k], ctx);
        float top, right, bottom, left;
        switch (e.length) {
            case 1: top = right = bottom = left = e[0]; break;
            case 2: top = bottom = e[0]; right = left = e[1]; break;
            case 3: top = e[0]; right = left = e[1]; bottom = e[2]; break;
            default: top = e[0]; right = e[1]; bottom = e[2]; left = e[3]; break;
        }
        cs.borderTopWidth = top; cs.borderRightWidth = right;
        cs.borderBottomWidth = bottom; cs.borderLeftWidth = left;
        return true;
    }

    private static void setBorderWidth(ComputedStyle cs, String side, float w) {
        if (side == null) {
            cs.borderTopWidth = cs.borderRightWidth = cs.borderBottomWidth = cs.borderLeftWidth = w;
        } else if (side.equals("top")) cs.borderTopWidth = w;
        else if (side.equals("right")) cs.borderRightWidth = w;
        else if (side.equals("bottom")) cs.borderBottomWidth = w;
        else if (side.equals("left")) cs.borderLeftWidth = w;
    }

    private static float borderWidth(String lv, Context ctx) {
        Float kw = borderWidthKeyword(lv);
        if (kw != null) return kw;
        Float px = CssLength.resolve(lv, ctx.ownFontSize, ctx.rootFontSize, Float.NaN);
        return px == null ? 0f : Math.max(0f, px);
    }

    private static Float borderWidthKeyword(String lv) {
        switch (lv) {
            case "thin":   return 1f;
            case "medium": return 3f;
            case "thick":  return 5f;
            default:       return null;
        }
    }

    // --- length helpers ---------------------------------------------------

    /** A length property that also accepts {@code auto} / {@code none} -> {@code null}. */
    private static Float dimension(String lv, Context ctx) {
        if (lv.equals("auto") || lv.equals("none") || lv.equals("normal")) return null;
        Float px = CssLength.resolve(lv, ctx.ownFontSize, ctx.rootFontSize, Float.NaN);
        if (px != null && px >= 0) return px;
        return null; // percentages / unresolved -> auto (raw map keeps the source)
    }

    private static float len(String lv, Context ctx) {
        Float px = CssLength.resolve(lv, ctx.ownFontSize, ctx.rootFontSize, Float.NaN);
        return px == null ? 0f : px;
    }

    private static float nonNeg(float v) {
        return v < 0 ? 0 : v;
    }

    private static boolean hasUnit(String lv) {
        return lv.endsWith("px") || lv.endsWith("em") || lv.endsWith("rem") || lv.endsWith("pt")
                || lv.endsWith("pc") || lv.endsWith("in") || lv.endsWith("cm") || lv.endsWith("mm")
                || lv.endsWith("ex") || lv.endsWith("%");
    }

    private static String firstToken(String v) {
        String t = v.trim();
        int sp = t.indexOf(' ');
        return sp < 0 ? t : t.substring(0, sp);
    }

    private static String normalizeDisplay(String lv) {
        // Collapse table/flex/grid variants the layout engine treats as block/inline.
        if (lv.equals("inline-table")) return "inline-block";
        if (lv.equals("flex") || lv.equals("grid") || lv.equals("table-row") || lv.equals("table-cell")) return "block";
        if (lv.equals("inline-flex") || lv.equals("inline-grid")) return "inline-block";
        return lv;
    }

    private static String normalizeAlign(String lv) {
        if (lv.equals("start")) return "left";
        if (lv.equals("end")) return "right";
        return lv;
    }
}
