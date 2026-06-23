package client.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ordered, mutable collection of {@link ChatMessage} objects representing a
 * single chat session's history.
 *
 * <p>This class is a plain data holder. It is not thread-safe; callers that
 * mutate a conversation from multiple threads must provide their own
 * synchronisation.</p>
 */
public final class Conversation {

    private final List<ChatMessage> messages = new ArrayList<ChatMessage>();

    /**
     * Appends a message to the end of the history.
     *
     * @param message the message to add; must not be {@code null}
     */
    public void addMessage(ChatMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        messages.add(message);
    }

    /**
     * Convenience method that creates and appends a message stamped with the
     * current time.
     *
     * @param role    the author role
     * @param content the message text
     * @return the message that was added
     */
    public ChatMessage addMessage(String role, String content) {
        ChatMessage message = new ChatMessage(role, content);
        addMessage(message);
        return message;
    }

    /**
     * @return an unmodifiable snapshot of the conversation history, oldest
     *         message first.
     */
    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(new ArrayList<ChatMessage>(messages));
    }

    /** @return the number of messages currently in the history. */
    public int size() {
        return messages.size();
    }

    /** @return {@code true} if the conversation has no messages. */
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    /** Removes all messages from the history. */
    public void clear() {
        messages.clear();
    }
}
