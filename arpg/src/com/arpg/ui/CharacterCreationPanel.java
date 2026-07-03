package com.arpg.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import com.arpg.model.CharacterClass;

/**
 * First card: enter a hero name and pick a class, then start the game.
 * Classes are supplied by the caller from {@code GameEngine.getAvailableClasses()}.
 */
public class CharacterCreationPanel extends JPanel {

    /** Callback fired when the player commits to a new game. */
    public interface CreationListener {
        void onStartGame(String playerName, CharacterClass clazz);
    }

    private final CreationListener listener;
    private final JTextField nameField = new JTextField("Hero", 18);
    private final DefaultListModel<CharacterClass> classModel = new DefaultListModel<CharacterClass>();
    private final JList<CharacterClass> classList = new JList<CharacterClass>(classModel);
    private final JTextArea classInfo = new JTextArea(5, 24);
    private final JButton startButton = new JButton("Begin Adventure");
    private final JLabel error = new JLabel(" ");

    public CharacterCreationPanel(CreationListener listener) {
        super(new GridBagLayout());
        this.listener = listener;
        setBackground(UiTheme.BG_DARK);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;

        JLabel title = new JLabel("Create Your Hero");
        title.setFont(UiTheme.TITLE.deriveFont(24f));
        title.setForeground(UiTheme.FG_TEXT);
        add(title, c);

        c.gridy++;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        JLabel nameLbl = new JLabel("Name:");
        nameLbl.setForeground(UiTheme.FG_TEXT);
        add(nameLbl, c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        add(nameField, c);

        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        JLabel classLbl = new JLabel("Class:");
        classLbl.setForeground(UiTheme.FG_TEXT);
        add(classLbl, c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        classList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        classList.setVisibleRowCount(4);
        classList.setBackground(UiTheme.BG_PANEL);
        classList.setForeground(UiTheme.FG_TEXT);
        classList.setCellRenderer(new ClassRenderer());
        classList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    showClassInfo();
                }
            }
        });
        JScrollPane classScroll = new JScrollPane(classList);
        classScroll.setPreferredSize(new Dimension(220, 96));
        add(classScroll, c);

        c.gridy++;
        classInfo.setEditable(false);
        classInfo.setLineWrap(true);
        classInfo.setWrapStyleWord(true);
        classInfo.setBackground(UiTheme.BG_PANEL);
        classInfo.setForeground(UiTheme.FG_MUTED);
        classInfo.setBorder(new EmptyBorder(6, 6, 6, 6));
        add(new JScrollPane(classInfo), c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        error.setForeground(UiTheme.DAMAGE);
        add(error, c);

        c.gridy++;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.setOpaque(false);
        startButton.setFont(UiTheme.BODY_BOLD);
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                start();
            }
        });
        buttons.add(startButton);
        add(buttons, c);
    }

    /** Populate the class list (called by MainFrame from the engine). */
    public void setClasses(List<CharacterClass> classes) {
        classModel.clear();
        List<CharacterClass> list = classes == null ? new ArrayList<CharacterClass>() : classes;
        for (int i = 0; i < list.size(); i++) {
            classModel.addElement(list.get(i));
        }
        if (!classModel.isEmpty()) {
            classList.setSelectedIndex(0);
        }
    }

    private void showClassInfo() {
        CharacterClass clazz = classList.getSelectedValue();
        if (clazz == null) {
            classInfo.setText("");
            return;
        }
        classInfo.setText(clazz.getDisplayName());
    }

    private void start() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        CharacterClass clazz = classList.getSelectedValue();
        if (name.isEmpty()) {
            error.setText("Please enter a name.");
            return;
        }
        if (clazz == null) {
            error.setText("Please choose a class.");
            return;
        }
        error.setText(" ");
        listener.onStartGame(name, clazz);
    }

    private static final class ClassRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof CharacterClass) {
                label.setText(((CharacterClass) value).getDisplayName());
            }
            if (!isSelected) {
                label.setBackground(UiTheme.BG_PANEL);
                label.setForeground(UiTheme.FG_TEXT);
            }
            return label;
        }
    }
}
