package com.whimkit.css.engine;

/**
 * One {@code property: value} pair from a declaration block.
 *
 * <p>The property name is stored lower-cased; the value keeps its original case
 * (colours, font names and URLs can be case-sensitive). The {@code !important}
 * flag is parsed out so the cascade can honour it.</p>
 */
final class Declaration {

    final String property;
    final String value;
    final boolean important;

    private Declaration(String property, String value, boolean important) {
        this.property = property;
        this.value = value;
        this.important = important;
    }

    /**
     * Parses a single raw declaration such as {@code "color: red !important"}.
     *
     * @return the parsed declaration, or {@code null} when there is no {@code ':'}
     *         separator or the property/value is empty (malformed — skipped).
     */
    static Declaration parse(String raw) {
        if (raw == null) return null;
        int colon = raw.indexOf(':');
        if (colon <= 0) return null;
        String prop = raw.substring(0, colon).trim().toLowerCase(java.util.Locale.ROOT);
        String val = raw.substring(colon + 1).trim();
        if (prop.isEmpty() || val.isEmpty()) return null;
        boolean important = false;
        int bang = val.toLowerCase(java.util.Locale.ROOT).lastIndexOf("!important");
        if (bang >= 0) {
            important = true;
            val = val.substring(0, bang).trim();
        }
        if (val.isEmpty()) return null;
        return new Declaration(prop, val, important);
    }

    @Override
    public String toString() {
        return property + ": " + value + (important ? " !important" : "");
    }
}
