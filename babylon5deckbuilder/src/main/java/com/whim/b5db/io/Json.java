package com.whim.b5db.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny, dependency-free JSON parser and writer supporting the subset used by
 * the card DSL and save files: objects, arrays, strings, numbers, booleans,
 * and null. Numbers are returned as {@link Double}. This keeps the project free
 * of any runtime dependency (JDK-only), as required by the technical constraints.
 */
public final class Json {

    private final String s;
    private int i;

    private Json(String s) {
        this.s = s;
    }

    /** Parse a JSON document into Map/List/String/Double/Boolean/null. */
    public static Object parse(String text) {
        Json j = new Json(text);
        j.ws();
        Object v = j.value();
        j.ws();
        if (j.i < j.s.length()) {
            throw new IllegalArgumentException("Trailing characters at index " + j.i);
        }
        return v;
    }

    private Object value() {
        char c = peek();
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': case 'f': return bool();
            case 'n': expect("null"); return null;
            default: return number();
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        expect('{');
        ws();
        if (peek() == '}') { i++; return m; }
        while (true) {
            ws();
            String key = string();
            ws();
            expect(':');
            ws();
            m.put(key, value());
            ws();
            char c = next();
            if (c == '}') break;
            if (c != ',') throw err("',' or '}'");
        }
        return m;
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        expect('[');
        ws();
        if (peek() == ']') { i++; return list; }
        while (true) {
            ws();
            list.add(value());
            ws();
            char c = next();
            if (c == ']') break;
            if (c != ',') throw err("',' or ']'");
        }
        return list;
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') break;
            if (c == '\\') {
                char e = next();
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default: throw err("valid escape");
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Double number() {
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
            i++;
        }
        return Double.parseDouble(s.substring(start, i));
    }

    private Boolean bool() {
        if (peek() == 't') { expect("true"); return Boolean.TRUE; }
        expect("false");
        return Boolean.FALSE;
    }

    // --- low-level helpers ---
    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private char peek() {
        if (i >= s.length()) throw err("more input");
        return s.charAt(i);
    }

    private char next() {
        if (i >= s.length()) throw err("more input");
        return s.charAt(i++);
    }

    private void expect(char c) {
        if (next() != c) throw err("'" + c + "'");
    }

    private void expect(String word) {
        if (!s.regionMatches(i, word, 0, word.length())) throw err("'" + word + "'");
        i += word.length();
    }

    private IllegalArgumentException err(String expected) {
        return new IllegalArgumentException("JSON parse error at index " + i + ": expected " + expected);
    }

    // --- convenience accessors for parsed maps ---
    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object o) {
        return (List<Object>) o;
    }

    public static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v == null ? def : v.toString();
    }

    public static int intv(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        return v == null ? def : (int) Math.round(((Number) v).doubleValue());
    }

    // --- minimal writer (for save/report output) ---

    /** Quote and escape a string as a JSON literal. */
    public static String quote(String v) {
        StringBuilder sb = new StringBuilder("\"");
        for (int k = 0; k < v.length(); k++) {
            char c = v.charAt(k);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\t': sb.append("\\t"); break;
                case '\r': sb.append("\\r"); break;
                default: sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
