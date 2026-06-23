package client.engine;

import client.domain.ChatMessage;
import client.domain.Conversation;
import client.domain.ConversationModel;
import client.domain.LLMRequest;

import java.util.List;

/**
 * Default {@link ConversationModel} backed by a single {@link Conversation}.
 *
 * <p>Each user prompt is appended to the conversation before the request is
 * built, so the request's context always includes the latest user turn. The
 * assistant reply is appended once it arrives.</p>
 */
public final class DefaultConversationModel implements ConversationModel {

    private final Conversation conversation;

    /** Creates a model backed by a fresh, empty conversation. */
    public DefaultConversationModel() {
        this(new Conversation());
    }

    /**
     * Creates a model backed by the supplied conversation.
     *
     * @param conversation the conversation to manage; must not be {@code null}
     */
    public DefaultConversationModel(Conversation conversation) {
        if (conversation == null) {
            throw new IllegalArgumentException("conversation must not be null");
        }
        this.conversation = conversation;
    }

    @Override
    public LLMRequest addUserMessage(String prompt) {
        String text = prompt == null ? "" : prompt;
        conversation.addMessage(ChatMessage.ROLE_USER, text);
        return new LLMRequest(text, conversation);
    }

    @Override
    public ChatMessage addAssistantMessage(String content) {
        return conversation.addMessage(ChatMessage.ROLE_ASSISTANT, content == null ? "" : content);
    }

    @Override
    public List<ChatMessage> getHistory() {
        return conversation.getMessages();
    }

    @Override
    public Conversation getConversation() {
        return conversation;
    }
}
