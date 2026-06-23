package client.engine;

import client.domain.LLMClient;
import client.domain.LLMRequest;
import client.domain.LLMResponse;

/**
 * In-memory {@link LLMClient} that returns canned replies without any network
 * access. Useful for trying out the UI offline and for tests.
 *
 * <p>By default it echoes the prompt back with a short preamble and simulates a
 * little latency so the asynchronous UI behaviour is visible. A fixed reply can
 * be supplied instead, and latency can be disabled for fast tests.</p>
 */
public final class MockLLMClient implements LLMClient {

    private final String fixedReply;
    private final long latencyMillis;

    /** Creates an echoing mock with a small (400ms) simulated latency. */
    public MockLLMClient() {
        this(null, 400);
    }

    /**
     * Creates a mock with explicit behaviour.
     *
     * @param fixedReply    a reply to always return, or {@code null} to echo
     * @param latencyMillis artificial delay before responding, in milliseconds
     */
    public MockLLMClient(String fixedReply, long latencyMillis) {
        this.fixedReply = fixedReply;
        this.latencyMillis = latencyMillis;
    }

    @Override
    public LLMResponse sendRequest(LLMRequest request) {
        if (request == null) {
            return LLMResponse.failure("Request was null.");
        }
        if (latencyMillis > 0) {
            try {
                Thread.sleep(latencyMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return LLMResponse.failure("Interrupted while generating reply.");
            }
        }
        if (fixedReply != null) {
            return LLMResponse.success(fixedReply);
        }
        String prompt = request.getPrompt();
        return LLMResponse.success("(mock) You said: " + prompt);
    }
}
