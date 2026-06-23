package client.domain;

/**
 * An immutable single message in a conversation.
 *
 * <p>A message has a {@code role} (for example {@code "user"} or
 * {@code "assistant"}), the textual {@code content}, and the {@code timestamp}
 * at which it was created (epoch milliseconds).</p>
 */
public final class ChatMessage {

    /** Conventional role for messages authored by the human user. */
    public static final String ROLE_USER = "user";
    /** Conventional role for messages authored by the assistant. */
    public static final String ROLE_ASSISTANT = "assistant";
    /** Conventional role for system / instruction messages. */
    public static final String ROLE_SYSTEM = "system";

    private final String role;
    private final String content;
    private final long timestamp;

    /**
     * Creates a message with an explicit timestamp.
     *
     * @param role      the author role; must not be {@code null}
     * @param content   the message text; must not be {@code null}
     * @param timestamp creation time in epoch milliseconds
     */
    public ChatMessage(String role, String content, long timestamp) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    /**
     * Creates a message stamped with the current time.
     *
     * @param role    the author role; must not be {@code null}
     * @param content the message text; must not be {@code null}
     */
    public ChatMessage(String role, String content) {
        this(role, content, System.currentTimeMillis());
    }

    /** @return the author role of this message. */
    public String getRole() {
        return role;
    }

    /** @return the textual content of this message. */
    public String getContent() {
        return content;
    }

    /** @return the creation time in epoch milliseconds. */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return role + ": " + content;
    }
}
