package client.domain;

/**
 * An immutable response returned from an {@link LLMClient}.
 *
 * <p>On success, {@link #isSuccess()} is {@code true} and {@link #getContent()}
 * holds the assistant's reply. On failure, {@link #isSuccess()} is {@code false}
 * and {@link #getErrorMessage()} describes what went wrong; the content is
 * typically empty in that case.</p>
 *
 * <p>Instances are created through the static {@link #success(String)} and
 * {@link #failure(String)} factory methods.</p>
 */
public final class LLMResponse {

    private final String content;
    private final boolean success;
    private final String errorMessage;

    private LLMResponse(String content, boolean success, String errorMessage) {
        this.content = content == null ? "" : content;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    /**
     * Builds a successful response.
     *
     * @param content the assistant reply text
     * @return a successful {@code LLMResponse}
     */
    public static LLMResponse success(String content) {
        return new LLMResponse(content, true, null);
    }

    /**
     * Builds a failed response.
     *
     * @param errorMessage a human-readable description of the failure
     * @return a failed {@code LLMResponse}
     */
    public static LLMResponse failure(String errorMessage) {
        return new LLMResponse("", false, errorMessage);
    }

    /** @return the assistant reply text (never {@code null}; empty on failure). */
    public String getContent() {
        return content;
    }

    /** @return {@code true} if the request succeeded. */
    public boolean isSuccess() {
        return success;
    }

    /** @return the failure description, or {@code null} when successful. */
    public String getErrorMessage() {
        return errorMessage;
    }
}
