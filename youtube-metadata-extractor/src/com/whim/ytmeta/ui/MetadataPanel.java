package com.whim.ytmeta.ui;

import com.whim.ytmeta.logic.YouTubeMetadataExtractor;
import com.whim.ytmeta.model.VideoMetadata;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The presentation layer. A single panel with a URL input, a fetch button, a
 * scrolling result area and a copy-to-clipboard button. All network/parse work
 * is offloaded from the Event Dispatch Thread via {@link SwingWorker} so the UI
 * never freezes while a page is being fetched.
 *
 * This layer depends only on the model and the extractor's public API.
 */
public final class MetadataPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final YouTubeMetadataExtractor extractor = new YouTubeMetadataExtractor();

    private final JTextField urlField = new JTextField(36);
    private final JButton fetchButton = new JButton("Fetch Metadata");
    private final JButton copyButton = new JButton("Copy to Clipboard");
    private final JTextArea outputArea = new JTextArea(6, 36);
    private final JLabel statusLabel = new JLabel("Enter a YouTube URL and click Fetch Metadata.");

    public MetadataPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildInputRow(), BorderLayout.NORTH);
        add(buildOutputArea(), BorderLayout.CENTER);
        add(buildBottomRow(), BorderLayout.SOUTH);

        wireActions();
        copyButton.setEnabled(false);
    }

    private JPanel buildInputRow() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("YouTube URL:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(urlField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(fetchButton, gbc);

        return panel;
    }

    private JScrollPane buildOutputArea() {
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(false);
        outputArea.setBorder(BorderFactory.createTitledBorder("Tab-delimited output"));
        return new JScrollPane(outputArea);
    }

    private JPanel buildBottomRow() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(copyButton, BorderLayout.EAST);
        return panel;
    }

    private void wireActions() {
        ActionListener fetchAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startFetch();
            }
        };
        fetchButton.addActionListener(fetchAction);
        urlField.addActionListener(fetchAction); // Enter key triggers fetch.

        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyToClipboard();
            }
        });
    }

    private void startFetch() {
        final String url = urlField.getText().trim();
        if (url.isEmpty()) {
            statusLabel.setText("Please enter a YouTube URL first.");
            return;
        }

        fetchButton.setEnabled(false);
        copyButton.setEnabled(false);
        outputArea.setText("");
        statusLabel.setText("Fetching metadata ...");

        // Offload the blocking network + parse work onto a background thread.
        SwingWorker<VideoMetadata, Void> worker = new SwingWorker<VideoMetadata, Void>() {
            @Override
            protected VideoMetadata doInBackground() throws Exception {
                return extractor.extract(url);
            }

            @Override
            protected void done() {
                try {
                    VideoMetadata metadata = get();
                    outputArea.setText(metadata.toTabDelimitedString());
                    copyButton.setEnabled(true);
                    statusLabel.setText("Done. Review the row, then copy it into your spreadsheet.");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    statusLabel.setText("Error: " + cause.getMessage());
                } finally {
                    fetchButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void copyToClipboard() {
        String text = outputArea.getText();
        if (text == null || text.isEmpty()) {
            statusLabel.setText("Nothing to copy yet.");
            return;
        }
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
        statusLabel.setText("Copied to clipboard. Paste directly into a spreadsheet.");
    }
}
