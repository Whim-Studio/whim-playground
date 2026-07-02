package com.whimkit.css.engine;

import java.util.Locale;

/**
 * Resolves CSS length values to absolute CSS pixels.
 *
 * <p>Supports {@code px}, {@code pt} (×4/3), {@code pc}, {@code in}, {@code cm},
 * {@code mm}, {@code em} (× the supplied em basis), {@code rem} (× the root font
 * size), {@code ex} (≈0.5em) and {@code %} (× the supplied percentage basis).
 * A bare {@code 0} resolves to {@code 0}. Anything else — including unitless
 * non-zero numbers — returns {@code null} so the caller can leave the target
 * field at its inherited/initial value.</p>
 */
final class CssLength {

    private static final float PX_PER_PT = 4f / 3f;   // 96dpi / 72pt
    private static final float PX_PER_IN = 96f;

    private CssLength() { }

    /**
     * @param value        raw length token, e.g. {@code "12px"}, {@code "1.5em"}, {@code "50%"}
     * @param emBasis      pixel value one {@code em} resolves to
     * @param rootFontSize pixel value one {@code rem} resolves to
     * @param percentBasis pixel value {@code 100%} resolves to (may be {@code NaN} if unknown)
     * @return the resolved pixel length, or {@code null} when unparsable/unsupported
     */
    static Float resolve(String value, float emBasis, float rootFontSize, float percentBasis) {
        if (value == null) return null;
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.isEmpty()) return null;
        try {
            if (v.endsWith("%")) {
                if (Float.isNaN(percentBasis)) return null;
                float p = Float.parseFloat(v.substring(0, v.length() - 1).trim());
                return percentBasis * p / 100f;
            }
            if (v.endsWith("px")) return Float.parseFloat(num(v, 2));
            if (v.endsWith("rem")) return Float.parseFloat(num(v, 3)) * rootFontSize;
            if (v.endsWith("em")) return Float.parseFloat(num(v, 2)) * emBasis;
            if (v.endsWith("ex")) return Float.parseFloat(num(v, 2)) * emBasis * 0.5f;
            if (v.endsWith("pt")) return Float.parseFloat(num(v, 2)) * PX_PER_PT;
            if (v.endsWith("pc")) return Float.parseFloat(num(v, 2)) * 16f; // 1pc = 12pt = 16px
            if (v.endsWith("in")) return Float.parseFloat(num(v, 2)) * PX_PER_IN;
            if (v.endsWith("cm")) return Float.parseFloat(num(v, 2)) * PX_PER_IN / 2.54f;
            if (v.endsWith("mm")) return Float.parseFloat(num(v, 2)) * PX_PER_IN / 25.4f;
            // Bare number: only 0 is a valid length.
            float f = Float.parseFloat(v);
            return f == 0f ? Float.valueOf(0f) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String num(String v, int unitLen) {
        return v.substring(0, v.length() - unitLen).trim();
    }

    /** @return {@code true} if the (trimmed, lower-cased) value is a parseable plain number. */
    static Float asNumber(String value) {
        if (value == null) return null;
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
