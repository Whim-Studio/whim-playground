package client.engine;

import client.domain.ChatMessage;
import client.domain.Conversation;
import client.domain.LLMClient;
import client.domain.LLMRequest;
import client.domain.LLMResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

/**
 * {@link LLMClient} that talks to a generic LLM HTTP endpoint using only
 * {@link HttpURLConnection} from the standard library.
 *
 * <p>The request is serialised to a small JSON object of the shape:</p>
 * <pre>
 * {
 *   "prompt": "...",
 *   "messages": [ {"role":"user","content":"..."}, ... ]
 * }
 * </pre>
 *
 * <p>The response body is scanned for the first string field named
 * {@code content}, {@code response}, {@code text}, {@code message} or
 * {@code completion}; the first one found becomes the reply. If none is found
 * the raw body is returned, so the client degrades gracefully against an
 * unfamiliar endpoint. All failures (network errors, non-2xx status, empty
 * bodies) are reported through {@link LLMResponse#failure(String)} rather than
 * by throwing.</p>
 */
public final class HttpLLMClient implements LLMClient {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String[] REPLY_FIELDS =
            {"content", "response", "text", "message", "completion"};

    private final String apiUrl;
    private final String apiKey;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    /**
     * Creates a client with default timeouts (10s connect, 60s read).
     *
     * @param apiUrl the endpoint URL; must not be {@code null}
     * @param apiKey the bearer token, or {@code null}/empty for no auth header
     */
    public HttpLLMClient(String apiUrl, String apiKey) {
        this(apiUrl, apiKey, 10000, 60000);
    }

    /**
     * Creates a client with explicit timeouts.
     *
     * @param apiUrl               the endpoint URL; must not be {@code null}
     * @param apiKey               the bearer token, or {@code null}/empty for none
     * @param connectTimeoutMillis connect timeout in milliseconds
     * @param readTimeoutMillis    read timeout in milliseconds
     */
    public HttpLLMClient(String apiUrl, String apiKey, int connectTimeoutMillis, int readTimeoutMillis) {
        if (apiUrl == null) {
            throw new IllegalArgumentException("apiUrl must not be null");
        }
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public LLMResponse sendRequest(LLMRequest request) {
        if (request == null) {
            return LLMResponse.failure("Request was null.");
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(connectTimeoutMillis);
            connection.setReadTimeout(readTimeoutMillis);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            }

            byte[] payload = buildRequestBody(request).getBytes(UTF_8);
            OutputStream out = connection.getOutputStream();
            try {
                out.write(payload);
                out.flush();
            } finally {
                out.close();
            }

            int status = connection.getResponseCode();
            String body = readBody(connection, status);

            if (status < 200 || status >= 300) {
                String detail = (body == null || body.isEmpty()) ? "" : ": " + body;
                return LLMResponse.failure("HTTP " + status + detail);
            }
            if (body == null || body.trim().isEmpty()) {
                return LLMResponse.failure("Empty response from server.");
            }

            String reply = extractReply(body);
            return LLMResponse.success(reply);
        } catch (IOException e) {
            return LLMResponse.failure("Network error: " + e.getMessage());
        } catch (RuntimeException e) {
            return LLMResponse.failure("Unexpected error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Serialises the request (prompt plus conversation context) to JSON. */
    private static String buildRequestBody(LLMRequest request) {
        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"prompt\":\"").append(Json.escape(request.getPrompt())).append('"');
        json.append(",\"messages\":[");
        Conversation context = request.getContext();
        if (context != null) {
            List<ChatMessage> messages = context.getMessages();
            for (int i = 0; i < messages.size(); i++) {
                ChatMessage message = messages.get(i);
                if (i > 0) {
                    json.append(',');
                }
                json.append("{\"role\":\"").append(Json.escape(message.getRole()))
                        .append("\",\"content\":\"").append(Json.escape(message.getContent()))
                        .append("\"}");
            }
        }
        json.append("]}");
        return json.toString();
    }

    /** Reads the response or error stream fully into a string. */
    private static String readBody(HttpURLConnection connection, int status) throws IOException {
        InputStream stream = (status >= 200 && status < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();
        if (stream == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, UTF_8));
        try {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                result.append(buffer, 0, read);
            }
        } finally {
            reader.close();
        }
        return result.toString();
    }

    /** Pulls the assistant reply out of the response body, with fallback. */
    private static String extractReply(String body) {
        for (int i = 0; i < REPLY_FIELDS.length; i++) {
            String value = Json.extractString(body, REPLY_FIELDS[i]);
            if (value != null) {
                return value;
            }
        }
        // No recognised field: return the trimmed raw body so the user still
        // sees something useful.
        return body.trim();
    }
}
