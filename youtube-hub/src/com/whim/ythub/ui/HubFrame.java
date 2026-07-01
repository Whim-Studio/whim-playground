package com.whim.ythub.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import com.whim.ythub.io.LibraryManager;
import com.whim.ythub.logic.UrlValidator;
import com.whim.ythub.logic.VideoLauncher;
import com.whim.ythub.model.VideoRecord;

/**
 * Main application window for the YouTube Hub &amp; Launcher.
 *
 * <p>Layout is a {@link BorderLayout}: a form panel (built with
 * {@link GridBagLayout}) at the top for adding videos, a filterable
 * {@link JTable} in the centre showing the saved library, and an action bar at
 * the bottom with a prominent "Watch Video" button. Double-clicking a table row
 * also launches that video.</p>
 *
 * <p>All persistence is delegated to {@link LibraryManager}; this class performs
 * no direct file access. URL validation is delegated to {@link UrlValidator} and
 * browser launching to {@link VideoLauncher} (the launch runs on a background
 * {@link SwingWorker} so a slow browser call never freezes the UI).</p>
 *
 * <p>Strict Java 8: no {@code var}, no text blocks, no post-8 APIs.</p>
 */
public class HubFrame extends JFrame {

    private static final String[] CATEGORIES =
            { "Music", "Tutorials", "Gaming", "News", "Other" };

    private final LibraryManager libraryManager = new LibraryManager();
    private final VideoLauncher videoLauncher = new VideoLauncher();

    private final VideoTableModel tableModel = new VideoTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<VideoTableModel> sorter =
            new TableRowSorter<VideoTableModel>(tableModel);

    private final JTextField urlField = new JTextField(28);
    private final JTextField titleField = new JTextField(28);
    private final JComboBox<String> categoryBox = new JComboBox<String>(CATEGORIES);
    private final JTextField searchField = new JTextField(20);

    public HubFrame() {
        super("YouTube Hub & Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        add(buildFormPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        loadLibrary();

        setSize(820, 520);
        setMinimumSize(new Dimension(640, 400));
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    // UI construction
    // ------------------------------------------------------------------

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xCCCCCC)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        // Row 0: URL
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(new JLabel("YouTube URL:"), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        form.add(urlField, c);

        // Row 1: Title
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(new JLabel("Title:"), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        form.add(titleField, c);

        // Row 2: Category
        c.gridx = 0;
        c.gridy = 2;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(new JLabel("Category:"), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(categoryBox, c);

        // Add button spanning to the right, prominent
        JButton addButton = new JButton("Add to Hub");
        addButton.setFont(addButton.getFont().deriveFont(Font.BOLD));
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAdd();
            }
        });
        c.gridx = 2;
        c.gridy = 0;
        c.gridheight = 3;
        c.fill = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(4, 16, 4, 4);
        form.add(addButton, c);

        return form;
    }

    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setBorder(BorderFactory.createEmptyBorder(10, 14, 6, 14));

        // Search / filter toolbar
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.add(new JLabel("Filter:"));
        toolbar.add(Box.createHorizontalStrut(8));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, searchField.getPreferredSize().height));
        toolbar.add(searchField);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        center.add(toolbar, BorderLayout.NORTH);

        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(Math.max(22, table.getRowHeight()));
        table.getTableHeader().setReorderingAllowed(false);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    onWatch();
                }
            }
        });

        center.add(new JScrollPane(table), BorderLayout.CENTER);
        return center;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 14, 12, 14));

        JButton watchButton = new JButton("Watch Video");
        watchButton.setFont(watchButton.getFont().deriveFont(Font.BOLD, 14f));
        watchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onWatch();
            }
        });

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(Box.createHorizontalGlue());
        right.add(watchButton);
        bar.add(right, BorderLayout.CENTER);
        return bar;
    }

    // ------------------------------------------------------------------
    // Behaviour
    // ------------------------------------------------------------------

    private void loadLibrary() {
        try {
            List<VideoRecord> records = libraryManager.load();
            tableModel.setRecords(records);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load the saved library:\n" + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAdd() {
        String url = urlField.getText().trim();
        String title = titleField.getText().trim();
        Object selectedCategory = categoryBox.getSelectedItem();
        String category = selectedCategory == null ? "Other" : selectedCategory.toString();

        if (!UrlValidator.isValidYouTubeUrl(url)) {
            JOptionPane.showMessageDialog(this,
                    UrlValidator.describe(url),
                    "Invalid YouTube URL", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Title is optional: default to the URL when left blank.
        if (title.isEmpty()) {
            title = url;
        }

        String dateAdded = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        VideoRecord record = new VideoRecord(url, title, category, dateAdded);

        try {
            libraryManager.add(record);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save the video to the library:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        tableModel.addRecord(record);
        urlField.setText("");
        titleField.setText("");
        categoryBox.setSelectedIndex(0);
        urlField.requestFocusInWindow();
    }

    private void onWatch() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a video from the list first.",
                    "No Video Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        final VideoRecord record = tableModel.getRecordAt(modelRow);

        // Launch off the EDT so a slow browser call never freezes the UI.
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return videoLauncher.open(record);
            }

            @Override
            protected void done() {
                String error;
                try {
                    error = get();
                } catch (Exception ex) {
                    error = "Unexpected error launching the video: " + ex.getMessage();
                }
                if (error != null) {
                    JOptionPane.showMessageDialog(HubFrame.this,
                            error, "Cannot Open Video", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void applyFilter() {
        final String query = searchField.getText().trim();
        if (query.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        // Case-insensitive match against Title (col 0) and Category (col 1).
        sorter.setRowFilter(new RowFilter<VideoTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends VideoTableModel, ? extends Integer> entry) {
                String needle = query.toLowerCase();
                String title = String.valueOf(entry.getValue(0)).toLowerCase();
                String category = String.valueOf(entry.getValue(1)).toLowerCase();
                return title.contains(needle) || category.contains(needle);
            }
        });
    }
}
