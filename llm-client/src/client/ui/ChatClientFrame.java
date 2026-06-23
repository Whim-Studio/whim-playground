package client.ui;

import client.domain.ChatMessage;
import client.domain.ConversationModel;
import client.domain.LLMClient;
import client.domain.LLMRequest;
import client.domain.LLMResponse;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * The main application window: a scrollable transcript, a multi-line input box
 * and a Send button.
 *
 * <p>The frame talks to the model exclusively through the {@link LLMClient} and
 * {@link ConversationModel} contracts from the domain layer, so it works
 * unchanged against a real HTTP client or the mock. Network calls run on a
 * {@link SwingWorker} background thread; all widget updates happen on the Event
 * Dispatch Thread.</p>
 */
public final class ChatClientFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final LLMClient client;
    private final ConversationModel model;

    private final JTextArea transcriptArea = new JTextArea();
    private final JTextArea inputArea = new JTextArea(3, 40);
    private final JButton sendButton = new JButton("Send");
    private final JLabel statusLabel = new JLabel("Ready");

    /**
     * Builds the window.
     *
     * @param client the LLM client used to send requests; must not be {@code null}
     * @param model  the conversation model that tracks history; must not be {@code null}
     */
    public ChatClientFrame(LLMClient client, ConversationModel model) {
        super("LLM Chat Client");
        if (client == null || model == null) {
            throw new IllegalArgumentException("client and model must not be null");
        }
        this.client = client;
        this.model = model;

        buildUi();
        renderTranscript();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        transcriptArea.setEditable(false);
        transcriptArea.setLineWrap(true);
        transcriptArea.setWrapStyleWord(true);
        transcriptArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        transcriptArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane transcriptScroll = new JScrollPane(transcriptArea);
        transcriptScroll.setPreferredSize(new Dimension(560, 360));
        add(transcriptScroll, BorderLayout.CENTER);

        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JScrollPane inputScroll = new JScrollPane(inputArea);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSend();
            }
        });

        // Ctrl+Enter sends as well, which feels natural in a multi-line box.
        inputArea.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "send");
        inputArea.getActionMap().put("send", new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                onSend();
            }
        });

        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        inputPanel.add(inputScroll, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        statusLabel.setForeground(new Color(0x44, 0x44, 0x44));
        add(statusLabel, BorderLayout.NORTH);

        pack();
        setLocationRelativeTo(null);
    }

    /** Handles a send action: validate input, dispatch, then await the reply. */
    private void onSend() {
        final String prompt = inputArea.getText().trim();
        if (prompt.isEmpty()) {
            return;
        }
        inputArea.setText("");
        setBusy(true);
        setStatus("Sending…");

        // Record the user turn and build the request on the EDT (cheap, no I/O).
        final LLMRequest request = model.addUserMessage(prompt);
        renderTranscript();

        new SwingWorker<LLMResponse, Void>() {
            @Override
            protected LLMResponse doInBackground() {
                return client.sendRequest(request);
            }

            @Override
            protected void done() {
                LLMResponse response;
                try {
                    response = get();
                } catch (Exception e) {
                    response = LLMResponse.failure(e.getMessage());
                }
                handleResponse(response);
            }
        }.execute();
    }

    /** Applies a response on the EDT (SwingWorker.done runs there). */
    private void handleResponse(LLMResponse response) {
        if (response != null && response.isSuccess()) {
            model.addAssistantMessage(response.getContent());
            setStatus("Ready");
        } else {
            String error = (response == null || response.getErrorMessage() == null)
                    ? "Unknown error" : response.getErrorMessage();
            model.addAssistantMessage("[error] " + error);
            setStatus("Error: " + error);
        }
        renderTranscript();
        setBusy(false);
        inputArea.requestFocusInWindow();
    }

    /** Rebuilds the transcript text from the conversation history. */
    private void renderTranscript() {
        StringBuilder sb = new StringBuilder();
        List<ChatMessage> history = model.getHistory();
        for (int i = 0; i < history.size(); i++) {
            ChatMessage message = history.get(i);
            sb.append(label(message.getRole())).append('\n');
            sb.append(message.getContent()).append('\n');
            if (i < history.size() - 1) {
                sb.append('\n');
            }
        }
        transcriptArea.setText(sb.toString());
        transcriptArea.setCaretPosition(transcriptArea.getDocument().getLength());
    }

    private static String label(String role) {
        if (ChatMessage.ROLE_USER.equals(role)) {
            return "You:";
        }
        if (ChatMessage.ROLE_ASSISTANT.equals(role)) {
            return "Assistant:";
        }
        return role + ":";
    }

    private void setBusy(boolean busy) {
        sendButton.setEnabled(!busy);
        inputArea.setEnabled(!busy);
    }

    private void setStatus(final String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            statusLabel.setText(text);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    statusLabel.setText(text);
                }
            });
        }
    }
}
