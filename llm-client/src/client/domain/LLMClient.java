package client.domain;

/**
 * Contract for sending a request to a large language model and obtaining a
 * reply.
 *
 * <p>Implementations live in the engine layer and must depend only on the
 * domain types and the standard Java SE library. The UI layer programs against
 * this interface and never against a concrete implementation, so that a real
 * HTTP client and a mock client are fully interchangeable.</p>
 */
public interface LLMClient {

    /**
     * Sends a request to the model and returns its reply.
     *
     * <p>This call is expected to block until a result is available, so callers
     * in a UI context should invoke it from a background thread. Implementations
     * must never throw for ordinary failures (network errors, non-success HTTP
     * status, empty responses); they should instead return
     * {@link LLMResponse#failure(String)}.</p>
     *
     * @param request the request to send; must not be {@code null}
     * @return a response describing either the reply or the failure
     */
    LLMResponse sendRequest(LLMRequest request);
}
