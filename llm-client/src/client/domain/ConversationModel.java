package client.domain;

import java.util.List;

/**
 * Manages the state of a single chat session: it tracks the running
 * {@link Conversation} and turns a raw user prompt into an {@link LLMRequest},
 * recording both sides of each exchange.
 *
 * <p>This contract lets the UI delegate conversation bookkeeping to the engine
 * layer instead of mutating the {@link Conversation} directly.</p>
 */
public interface ConversationModel {

    /**
     * Records a user message in the conversation and builds the request to send
     * to the model, including the current conversation as context.
     *
     * @param prompt the user's prompt text
     * @return a request ready to be passed to {@link LLMClient#sendRequest}
     */
    LLMRequest addUserMessage(String prompt);

    /**
     * Records the assistant's reply in the conversation.
     *
     * @param content the assistant reply text
     * @return the message that was recorded
     */
    ChatMessage addAssistantMessage(String content);

    /** @return an unmodifiable snapshot of the conversation history. */
    List<ChatMessage> getHistory();

    /** @return the underlying conversation managed by this model. */
    Conversation getConversation();
}
