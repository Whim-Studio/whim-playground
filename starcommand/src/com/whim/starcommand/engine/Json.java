package com.whim.starcommand.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal, dependency-free JSON reader — just enough to load the game's data
 * files (objects, arrays, strings, numbers, booleans, null). Not a general
 * JSON library: no streaming, no comments, no big-number precision handling.
 *
 * <p>Parsed types: object → {@code Map<String,Object>}, array → {@code List<Object>},
 * string → {@code String}, number → {@code Double}, and {@code Boolean}/{@code null}.
 */
public final class Json {

    private final String src;
    private int pos;

    private Json(String src) { this.src = src; }

    /** Parse a JSON document into Maps/Lists/Strings/Doubles/Booleans/null. */
    public static Object parse(String text) {
        Json j = new Json(text);
        j.skipWs();
        Object v = j.readValue();
        j.skipWs();
        if (j.pos != j.src.length()) throw j.err("trailing characters");
        return v;
    }

    private Object readValue() {
        char ch = peek();
        switch (ch) {
            case '{': return readObject();
            case '[': return readArray();
            case '"': return readString();
            case 't': case 'f': return readBool();
            case 'n': return readNull();
            default:  return readNumber();
        }
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        expect('{');
        skipWs();
        if (peek() == '}') { pos++; return map; }
        while (true) {
            skipWs();
            String key = readString();
            skipWs();
            expect(':');
            skipWs();
            map.put(key, readValue());
            skipWs();
            char ch = next();
            if (ch == '}') break;
            if (ch != ',') throw err("expected ',' or '}'");
        }
        return map;
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<Object>();
        expect('[');
        skipWs();
        if (peek() == ']') { pos++; return list; }
        while (true) {
            skipWs();
            list.add(readValue());
            skipWs();
            char ch = next();
            if (ch == ']') break;
            if (ch != ',') throw err("expected ',' or ']'");
        }
        return list;
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char ch = next();
            if (ch == '"') break;
            if (ch == '\\') {
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
                        sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default: throw err("bad escape \\" + e);
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private Double readNumber() {
        int start = pos;
        while (pos < src.length() && "+-0123456789.eE".indexOf(src.charAt(pos)) >= 0) pos++;
        if (pos == start) throw err("invalid value");
        return Double.parseDouble(src.substring(start, pos));
    }

    private Boolean readBool() {
        if (src.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
        if (src.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
        throw err("invalid literal");
    }

    private Object readNull() {
        if (src.startsWith("null", pos)) { pos += 4; return null; }
        throw err("invalid literal");
    }

    private void skipWs() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private char peek() {
        if (pos >= src.length()) throw err("unexpected end of input");
        return src.charAt(pos);
    }

    private char next() {
        if (pos >= src.length()) throw err("unexpected end of input");
        return src.charAt(pos++);
    }

    private void expect(char c) {
        char ch = next();
        if (ch != c) throw err("expected '" + c + "' but got '" + ch + "'");
    }

    private RuntimeException err(String msg) {
        return new IllegalArgumentException("JSON parse error at " + pos + ": " + msg);
    }
}
