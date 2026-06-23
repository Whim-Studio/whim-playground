package client.domain;

/**
 * An immutable request to an {@link LLMClient}.
 *
 * <p>A request always carries a {@code prompt} (the new user message) and may
 * optionally carry a {@link Conversation} that supplies prior context. The
 * engine decides how to combine the prompt with the context when building the
 * outgoing API payload.</p>
 */
public final class LLMRequest {

    private final String prompt;
    private final Conversation context;

    /**
     * Creates a request with optional conversation context.
     *
     * @param prompt  the new user prompt; must not be {@code null}
     * @param context prior conversation context, or {@code null} for none
     */
    public LLMRequest(String prompt, Conversation context) {
        if (prompt == null) {
            throw new IllegalArgumentException("prompt must not be null");
        }
        this.prompt = prompt;
        this.context = context;
    }

    /**
     * Creates a context-free request.
     *
     * @param prompt the user prompt; must not be {@code null}
     */
    public LLMRequest(String prompt) {
        this(prompt, null);
    }

    /** @return the new user prompt. */
    public String getPrompt() {
        return prompt;
    }

    /** @return the conversation context, or {@code null} if none was supplied. */
    public Conversation getContext() {
        return context;
    }

    /** @return {@code true} if a non-null conversation context was supplied. */
    public boolean hasContext() {
        return context != null;
    }
}
