package com.whim.scg.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny hand-rolled JSON reader/writer (no external dependency, per contract).
 * Parses into: Map&lt;String,Object&gt; (objects), List&lt;Object&gt; (arrays),
 * String, Double, Boolean, null. Writing is done via {@link Writer}.
 *
 * Only the JSON subset the game needs is supported, but that subset is complete:
 * nested objects/arrays, strings with the standard escapes, numbers, true/false/null.
 */
public final class Json {
    private Json() {}

    // ---------------------------------------------------------------- parsing
    public static Object parse(String src) {
        Parser p = new Parser(src);
        p.skipWs();
        Object v = p.readValue();
        p.skipWs();
        if (!p.atEnd()) throw new JsonException("trailing content at " + p.pos);
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object o) {
        return (List<Object>) o;
    }

    public static String str(Map<String, Object> o, String k, String def) {
        Object v = o.get(k);
        return v == null ? def : String.valueOf(v);
    }

    public static int intVal(Map<String, Object> o, String k, int def) {
        Object v = o.get(k);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) { try { return (int) Double.parseDouble((String) v); } catch (Exception e) { return def; } }
        return def;
    }

    public static double dbl(Map<String, Object> o, String k, double def) {
        Object v = o.get(k);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) { try { return Double.parseDouble((String) v); } catch (Exception e) { return def; } }
        return def;
    }

    public static boolean bool(Map<String, Object> o, String k, boolean def) {
        Object v = o.get(k);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return def;
    }

    public static final class JsonException extends RuntimeException {
        JsonException(String m) { super(m); }
    }

    private static final class Parser {
        final String s;
        int pos;
        Parser(String s) { this.s = s; }

        boolean atEnd() { return pos >= s.length(); }

        void skipWs() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
                else break;
            }
        }

        Object readValue() {
            skipWs();
            if (atEnd()) throw new JsonException("unexpected end");
            char c = s.charAt(pos);
            switch (c) {
                case '{': return readObject();
                case '[': return readArray();
                case '"': return readString();
                case 't': case 'f': return readBool();
                case 'n': expect("null"); return null;
                default:  return readNumber();
            }
        }

        Map<String, Object> readObject() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            pos++; // {
            skipWs();
            if (peek() == '}') { pos++; return m; }
            while (true) {
                skipWs();
                String key = readString();
                skipWs();
                if (peek() != ':') throw new JsonException("expected : at " + pos);
                pos++;
                Object val = readValue();
                m.put(key, val);
                skipWs();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == '}') { pos++; break; }
                throw new JsonException("expected , or } at " + pos);
            }
            return m;
        }

        List<Object> readArray() {
            List<Object> a = new ArrayList<Object>();
            pos++; // [
            skipWs();
            if (peek() == ']') { pos++; return a; }
            while (true) {
                Object val = readValue();
                a.add(val);
                skipWs();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == ']') { pos++; break; }
                throw new JsonException("expected , or ] at " + pos);
            }
            return a;
        }

        String readString() {
            if (peek() != '"') throw new JsonException("expected string at " + pos);
            pos++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) throw new JsonException("unterminated string");
                char c = s.charAt(pos++);
                if (c == '"') break;
                if (c == '\\') {
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"':  sb.append('"');  break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/');  break;
                        case 'n':  sb.append('\n'); break;
                        case 't':  sb.append('\t'); break;
                        case 'r':  sb.append('\r'); break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'u':
                            String hex = s.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                            break;
                        default: sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Boolean readBool() {
            if (peek() == 't') { expect("true"); return Boolean.TRUE; }
            expect("false");
            return Boolean.FALSE;
        }

        Double readNumber() {
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') pos++;
                else break;
            }
            return Double.valueOf(s.substring(start, pos));
        }

        void expect(String lit) {
            if (!s.regionMatches(pos, lit, 0, lit.length())) throw new JsonException("expected " + lit + " at " + pos);
            pos += lit.length();
        }

        char peek() { return atEnd() ? '\0' : s.charAt(pos); }
    }

    // ------------------------------------------------------- generic write
    /** Serialize a Map/List/String/Number/Boolean/null tree to JSON text. */
    public static String write(Object o) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, o);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object o) {
        if (o == null) { sb.append("null"); return; }
        if (o instanceof String) { sb.append('"').append(Writer.escape((String) o)).append('"'); return; }
        if (o instanceof Boolean) { sb.append(((Boolean) o) ? "true" : "false"); return; }
        if (o instanceof Double || o instanceof Float) {
            double d = ((Number) o).doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) sb.append((long) d);
            else sb.append(d);
            return;
        }
        if (o instanceof Number) { sb.append(o.toString()); return; }
        if (o instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) o;
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(Writer.escape(e.getKey())).append('"').append(':');
                writeValue(sb, e.getValue());
            }
            sb.append('}');
            return;
        }
        if (o instanceof List) {
            List<Object> a = (List<Object>) o;
            sb.append('[');
            for (int i = 0; i < a.size(); i++) {
                if (i > 0) sb.append(',');
                writeValue(sb, a.get(i));
            }
            sb.append(']');
            return;
        }
        // fallback: stringify
        sb.append('"').append(Writer.escape(String.valueOf(o))).append('"');
    }

    // ---------------------------------------------------------------- writing
    /** Incremental JSON writer with light pretty-printing. */
    public static final class Writer {
        private final StringBuilder sb = new StringBuilder();

        public static String escape(String v) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < v.length(); i++) {
                char c = v.charAt(i);
                switch (c) {
                    case '"':  b.append("\\\""); break;
                    case '\\': b.append("\\\\"); break;
                    case '\n': b.append("\\n");  break;
                    case '\r': b.append("\\r");  break;
                    case '\t': b.append("\\t");  break;
                    default:
                        if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                        else b.append(c);
                }
            }
            return b.toString();
        }

        public Writer raw(String s) { sb.append(s); return this; }
        public Writer str(String s) { sb.append('"').append(s == null ? "" : escape(s)).append('"'); return this; }
        public Writer num(int n) { sb.append(n); return this; }
        public Writer num(double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d)) sb.append((long) d);
            else sb.append(d);
            return this;
        }
        public Writer bool(boolean b) { sb.append(b ? "true" : "false"); return this; }

        @Override public String toString() { return sb.toString(); }
    }
}
