package client.engine;

/**
 * A minimal, hand-written JSON helper with just enough capability for this
 * client: escaping strings when building a request body, and extracting a
 * single top-level string field from a flat or lightly-nested response body.
 *
 * <p>This is deliberately not a full JSON parser. It avoids any third-party
 * dependency while covering the common shapes returned by simple LLM HTTP
 * endpoints (for example {@code {"content":"..."}} or
 * {@code {"choices":[{"text":"..."}]}}).</p>
 */
final class Json {

    private Json() {
    }

    /**
     * Escapes a string so it can be embedded inside a JSON double-quoted value.
     *
     * @param raw the raw string (may be {@code null}, treated as empty)
     * @return the escaped string, without surrounding quotes
     */
    static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    /**
     * Finds the first occurrence of a string field with the given key and
     * returns its unescaped value.
     *
     * <p>For example, given the body {@code {"a":1,"content":"hi\nthere"}} and
     * the key {@code content}, this returns {@code hi<newline>there}. Returns
     * {@code null} if the key is not present as a string value.</p>
     *
     * @param body the JSON text to search
     * @param key  the field name to look for
     * @return the decoded string value, or {@code null} if not found
     */
    static String extractString(String body, String key) {
        if (body == null || key == null) {
            return null;
        }
        String needle = "\"" + key + "\"";
        int from = 0;
        while (true) {
            int keyIndex = body.indexOf(needle, from);
            if (keyIndex < 0) {
                return null;
            }
            int i = keyIndex + needle.length();
            // Skip whitespace and the colon separator.
            while (i < body.length() && Character.isWhitespace(body.charAt(i))) {
                i++;
            }
            if (i >= body.length() || body.charAt(i) != ':') {
                from = keyIndex + needle.length();
                continue;
            }
            i++;
            while (i < body.length() && Character.isWhitespace(body.charAt(i))) {
                i++;
            }
            if (i < body.length() && body.charAt(i) == '"') {
                return decode(body, i + 1);
            }
            // Value for this key is not a string; keep searching for another
            // occurrence of the key.
            from = keyIndex + needle.length();
        }
    }

    /**
     * Decodes a JSON string starting just after the opening quote, stopping at
     * the matching unescaped closing quote.
     */
    private static String decode(String body, int start) {
        StringBuilder out = new StringBuilder();
        int i = start;
        while (i < body.length()) {
            char c = body.charAt(i);
            if (c == '"') {
                break;
            }
            if (c == '\\' && i + 1 < body.length()) {
                char next = body.charAt(i + 1);
                switch (next) {
                    case '"':
                        out.append('"');
                        break;
                    case '\\':
                        out.append('\\');
                        break;
                    case '/':
                        out.append('/');
                        break;
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case 'b':
                        out.append('\b');
                        break;
                    case 'f':
                        out.append('\f');
                        break;
                    case 'u':
                        if (i + 5 < body.length()) {
                            try {
                                int code = Integer.parseInt(body.substring(i + 2, i + 6), 16);
                                out.append((char) code);
                                i += 6;
                                continue;
                            } catch (NumberFormatException ignored) {
                                out.append(next);
                            }
                        } else {
                            out.append(next);
                        }
                        break;
                    default:
                        out.append(next);
                }
                i += 2;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}
