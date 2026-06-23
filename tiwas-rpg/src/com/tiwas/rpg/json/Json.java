package com.tiwas.rpg.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-written, dependency-free JSON parser and writer.
 *
 * Parses to a tree of:
 *   - {@link java.util.Map}&lt;String,Object&gt; (object, insertion-ordered LinkedHashMap)
 *   - {@link java.util.List}&lt;Object&gt; (array, ArrayList)
 *   - {@link String}
 *   - {@link Double} (all numbers)
 *   - {@link Boolean}
 *   - {@code null}
 *
 * Round-trip safe: {@code Json.parse(Json.write(x))} reproduces an equal tree.
 */
public final class Json {

    private Json() {
    }

    // ---------------------------------------------------------------- parse

    public static Object parse(String text) {
        if (text == null) {
            throw new JsonException("Cannot parse null");
        }
        Parser p = new Parser(text);
        p.skipWhitespace();
        Object value = p.parseValue();
        p.skipWhitespace();
        if (!p.atEnd()) {
            throw new JsonException("Trailing characters at position " + p.pos);
        }
        return value;
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
            this.pos = 0;
        }

        boolean atEnd() {
            return pos >= s.length();
        }

        void skipWhitespace() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        char peek() {
            if (pos >= s.length()) {
                throw new JsonException("Unexpected end of input");
            }
            return s.charAt(pos);
        }

        Object parseValue() {
            skipWhitespace();
            char c = peek();
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseString();
                case 't':
                case 'f':
                    return parseBoolean();
                case 'n':
                    return parseNull();
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return parseNumber();
                    }
                    throw new JsonException("Unexpected character '" + c + "' at position " + pos);
            }
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            expect('{');
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw new JsonException("Expected string key at position " + pos);
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    pos++;
                } else if (c == '}') {
                    pos++;
                    break;
                } else {
                    throw new JsonException("Expected ',' or '}' at position " + pos);
                }
            }
            return map;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<Object>();
            expect('[');
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    pos++;
                } else if (c == ']') {
                    pos++;
                    break;
                } else {
                    throw new JsonException("Expected ',' or ']' at position " + pos);
                }
            }
            return list;
        }

        String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= s.length()) {
                    throw new JsonException("Unterminated string");
                }
                char c = s.charAt(pos++);
                if (c == '"') {
                    break;
                } else if (c == '\\') {
                    if (pos >= s.length()) {
                        throw new JsonException("Unterminated escape");
                    }
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"':
                            sb.append('"');
                            break;
                        case '\\':
                            sb.append('\\');
                            break;
                        case '/':
                            sb.append('/');
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u':
                            if (pos + 4 > s.length()) {
                                throw new JsonException("Invalid \\u escape");
                            }
                            String hex = s.substring(pos, pos + 4);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException nfe) {
                                throw new JsonException("Invalid \\u escape: " + hex);
                            }
                            pos += 4;
                            break;
                        default:
                            throw new JsonException("Invalid escape character: \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Double parseNumber() {
            int start = pos;
            if (pos < s.length() && s.charAt(pos) == '-') {
                pos++;
            }
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    pos++;
                } else {
                    break;
                }
            }
            String num = s.substring(start, pos);
            try {
                return Double.valueOf(Double.parseDouble(num));
            } catch (NumberFormatException nfe) {
                throw new JsonException("Invalid number: " + num);
            }
        }

        Boolean parseBoolean() {
            if (s.regionMatches(pos, "true", 0, 4)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (s.regionMatches(pos, "false", 0, 5)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new JsonException("Invalid literal at position " + pos);
        }

        Object parseNull() {
            if (s.regionMatches(pos, "null", 0, 4)) {
                pos += 4;
                return null;
            }
            throw new JsonException("Invalid literal at position " + pos);
        }

        void expect(char c) {
            if (pos >= s.length() || s.charAt(pos) != c) {
                throw new JsonException("Expected '" + c + "' at position " + pos);
            }
            pos++;
        }
    }

    // ---------------------------------------------------------------- write

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, false, 0);
        return sb.toString();
    }

    public static String writePretty(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, true, 0);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object value, boolean pretty, int indent) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Boolean) {
            sb.append(((Boolean) value).booleanValue() ? "true" : "false");
        } else if (value instanceof Number) {
            writeNumber(sb, (Number) value);
        } else if (value instanceof Map) {
            writeObject(sb, (Map<String, Object>) value, pretty, indent);
        } else if (value instanceof List) {
            writeArray(sb, (List<Object>) value, pretty, indent);
        } else {
            throw new JsonException("Cannot serialize type: " + value.getClass().getName());
        }
    }

    private static void writeNumber(StringBuilder sb, Number n) {
        double d = n.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new JsonException("Cannot serialize non-finite number: " + d);
        }
        // Render integral doubles without a trailing ".0" so round-trips and
        // human-facing output stay clean.
        if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
            sb.append(Long.toString((long) d));
        } else {
            sb.append(Double.toString(d));
        }
    }

    private static void writeObject(StringBuilder sb, Map<String, Object> map, boolean pretty, int indent) {
        if (map.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append('{');
        int childIndent = indent + 1;
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            if (pretty) {
                sb.append('\n');
                appendIndent(sb, childIndent);
            }
            writeString(sb, e.getKey());
            sb.append(pretty ? ": " : ":");
            writeValue(sb, e.getValue(), pretty, childIndent);
        }
        if (pretty) {
            sb.append('\n');
            appendIndent(sb, indent);
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, List<Object> list, boolean pretty, int indent) {
        if (list.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append('[');
        int childIndent = indent + 1;
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            if (pretty) {
                sb.append('\n');
                appendIndent(sb, childIndent);
            }
            writeValue(sb, item, pretty, childIndent);
        }
        if (pretty) {
            sb.append('\n');
            appendIndent(sb, indent);
        }
        sb.append(']');
    }

    private static void appendIndent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int p = hex.length(); p < 4; p++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------ coercions

    public static Map<String, Object> asObject(Object o) {
        if (o instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) o;
            return m;
        }
        throw new JsonException("Expected object but got " + typeName(o));
    }

    public static List<Object> asArray(Object o) {
        if (o instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> l = (List<Object>) o;
            return l;
        }
        throw new JsonException("Expected array but got " + typeName(o));
    }

    public static String asString(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return (String) o;
        }
        throw new JsonException("Expected string but got " + typeName(o));
    }

    /** Truncates a Double toward zero. */
    public static int asInt(Object o) {
        if (o instanceof Number) {
            return (int) ((Number) o).doubleValue();
        }
        if (o instanceof String) {
            try {
                return (int) Double.parseDouble((String) o);
            } catch (NumberFormatException nfe) {
                throw new JsonException("Cannot coerce string to int: " + o);
            }
        }
        throw new JsonException("Expected number but got " + typeName(o));
    }

    public static boolean asBoolean(Object o) {
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        throw new JsonException("Expected boolean but got " + typeName(o));
    }

    private static String typeName(Object o) {
        return o == null ? "null" : o.getClass().getSimpleName();
    }
}
