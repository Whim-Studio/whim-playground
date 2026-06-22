package com.whim.babylon5.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny, dependency-free JSON parser sufficient for the card data files.
 * Supports objects, arrays, strings (with the standard escapes), numbers,
 * {@code true}/{@code false}/{@code null}. Not a general-purpose parser — it
 * exists solely so {@link CardDatabase} can read {@code cards/*.json} without
 * any external library (Java 8, zero deps).
 *
 * <p>Parsed values map to: object -> {@code Map<String,Object>},
 * array -> {@code List<Object>}, string -> {@code String},
 * number -> {@code Double}, boolean -> {@code Boolean}, null -> {@code null}.</p>
 */
final class MiniJson {

    private final String s;
    private int i;

    private MiniJson(String s) {
        this.s = s;
        this.i = 0;
    }

    /** Parse a JSON document and return the root value. */
    static Object parse(String json) {
        MiniJson p = new MiniJson(json);
        p.skipWs();
        Object v = p.readValue();
        p.skipWs();
        return v;
    }

    private Object readValue() {
        skipWs();
        if (i >= s.length()) throw err("unexpected end of input");
        char c = s.charAt(i);
        switch (c) {
            case '{': return readObject();
            case '[': return readArray();
            case '"': return readString();
            case 't': case 'f': return readBoolean();
            case 'n': return readNull();
            default:  return readNumber();
        }
    }

    private Map<String, Object> readObject() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        expect('{');
        skipWs();
        if (peek() == '}') { i++; return m; }
        while (true) {
            skipWs();
            String key = readString();
            skipWs();
            expect(':');
            Object val = readValue();
            m.put(key, val);
            skipWs();
            char c = next();
            if (c == ',') continue;
            if (c == '}') break;
            throw err("expected ',' or '}' but got '" + c + "'");
        }
        return m;
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<Object>();
        expect('[');
        skipWs();
        if (peek() == ']') { i++; return list; }
        while (true) {
            Object val = readValue();
            list.add(val);
            skipWs();
            char c = next();
            if (c == ',') continue;
            if (c == ']') break;
            throw err("expected ',' or ']' but got '" + c + "'");
        }
        return list;
    }

    private String readString() {
        skipWs();
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (i >= s.length()) throw err("unterminated string");
            char c = s.charAt(i++);
            if (c == '"') break;
            if (c == '\\') {
                char e = s.charAt(i++);
                switch (e) {
                    case '"':  sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/'); break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'u':
                        String hex = s.substring(i, i + 4);
                        i += 4;
                        sb.append((char) Integer.parseInt(hex, 16));
                        break;
                    default: throw err("bad escape \\" + e);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Boolean readBoolean() {
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        throw err("invalid literal");
    }

    private Object readNull() {
        if (s.startsWith("null", i)) { i += 4; return null; }
        throw err("invalid literal");
    }

    private Double readNumber() {
        int start = i;
        while (i < s.length()) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                i++;
            } else {
                break;
            }
        }
        if (start == i) throw err("invalid number");
        return Double.valueOf(s.substring(start, i));
    }

    // ---- low-level helpers ----

    private void skipWs() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++;
            else break;
        }
    }

    private char peek() {
        skipWs();
        if (i >= s.length()) throw err("unexpected end of input");
        return s.charAt(i);
    }

    private char next() {
        if (i >= s.length()) throw err("unexpected end of input");
        return s.charAt(i++);
    }

    private void expect(char c) {
        char got = next();
        if (got != c) throw err("expected '" + c + "' but got '" + got + "'");
    }

    private RuntimeException err(String msg) {
        return new IllegalArgumentException("JSON parse error at index " + i + ": " + msg);
    }
}
