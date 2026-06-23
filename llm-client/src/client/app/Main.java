package client.app;

import client.domain.Conversation;
import client.domain.ConversationModel;
import client.domain.LLMClient;
import client.engine.DefaultConversationModel;
import client.engine.HttpLLMClient;
import client.engine.MockLLMClient;
import client.ui.ChatClientFrame;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point. Wires together an {@link LLMClient}, a
 * {@link ConversationModel} and the {@link ChatClientFrame}, then shows the
 * window on the Event Dispatch Thread.
 *
 * <h2>How to run</h2>
 * <pre>
 *   # Mock mode (no network), the default:
 *   javac -d out $(find src -name "*.java")
 *   java -cp out client.app.Main
 *
 *   # Real endpoint:
 *   java -cp out client.app.Main https://your-endpoint/v1/chat YOUR_API_KEY
 * </pre>
 *
 * <p>Arguments: {@code [apiUrl] [apiKey]}. With no arguments the application
 * starts with the {@link MockLLMClient} so it is usable offline.</p>
 */
public final class Main {

    private Main() {
    }

    /**
     * Launches the chat client.
     *
     * @param args optional {@code [apiUrl] [apiKey]}; empty uses the mock client
     */
    public static void main(String[] args) {
        final LLMClient client = buildClient(args);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Fall back to the default look and feel.
                }
                Conversation conversation = new Conversation();
                ConversationModel model = new DefaultConversationModel(conversation);
                ChatClientFrame frame = new ChatClientFrame(client, model);
                frame.setExtendedState(JFrame.NORMAL);
                frame.setVisible(true);
            }
        });
    }

    /** Chooses a real or mock client based on the command-line arguments. */
    private static LLMClient buildClient(String[] args) {
        if (args != null && args.length >= 1 && args[0] != null && !args[0].trim().isEmpty()) {
            String apiUrl = args[0].trim();
            String apiKey = (args.length >= 2) ? args[1] : null;
            return new HttpLLMClient(apiUrl, apiKey);
        }
        return new MockLLMClient();
    }
}
