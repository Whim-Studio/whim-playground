package com.whim.capes.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.whim.capes.model.EventLogEntry;
import com.whim.capes.model.GameState;

/**
 * Manage the group's absolute Comics Code (pp.113-117): the list of things that
 * can never happen. When Resolving a Conflict would violate a line here, the
 * Claimant must Gloat instead (offered from the Table View's Resolve). The
 * rulebook stresses the Code is set between sessions and not changed mid-play
 * (p.116); this panel lets the group author it and seeds a 1950s example.
 */
public final class ComicsCodeView extends JPanel {
    private final GameState state;
    private final DefaultListModel<String> model = new DefaultListModel<String>();

    public ComicsCodeView(GameState state) {
        this.state = state;
        setLayout(new BorderLayout());
        setBackground(Palette.PAPER);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Comics Code");
        title.setFont(Palette.TITLE);
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);

        JTextArea note = new JTextArea(
                "Absolute lines that can never happen in your game. If Resolving a Conflict would make one "
              + "true, the Claimant Gloats instead: starting from their highest die, they turn dice down to 1, "
              + "and the Resolver gains a Story Token per die turned (p.41). Set the Code between sessions and "
              + "keep it fixed during play (p.116).");
        note.setEditable(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setOpaque(false);
        note.setFont(Palette.BODY);
        note.setPreferredSize(new Dimension(600, 66));
        center.add(note, BorderLayout.NORTH);

        refreshModel();
        JList<String> list = new JList<String>(model);
        list.setFont(Palette.BODY);
        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(600, 260));
        center.add(sp, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        buttons.setOpaque(false);
        JButton add = new JButton("Add line");
        add.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String s = JOptionPane.showInputDialog(ComicsCodeView.this, "New Comics Code line:");
                if (s != null && !s.trim().isEmpty()) {
                    state.comicsCode().add(s.trim());
                    state.eventLog().log(EventLogEntry.Category.SYSTEM, "Comics Code line added: \"" + s.trim() + "\".");
                    refreshModel();
                }
            }
        });
        final JButton remove = new JButton("Remove selected");
        remove.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                int i = list.getSelectedIndex();
                if (i >= 0) {
                    String removed = state.comicsCode().remove(i);
                    state.eventLog().log(EventLogEntry.Category.SYSTEM, "Comics Code line removed: \"" + removed + "\".");
                    refreshModel();
                }
            }
        });
        JButton example = new JButton("Load 1950s example");
        example.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { loadExample(); }
        });
        buttons.add(add); buttons.add(remove); buttons.add(example);
        center.add(buttons, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);
    }

    private void refreshModel() {
        model.clear();
        for (String line : state.comicsCode()) model.addElement("•  " + line);
        if (state.comicsCode().isEmpty()) model.addElement("(no Code — nothing triggers Gloating)");
    }

    private void loadExample() {
        // Example Code modeled on a 1950s sensibility (p.117).
        String[] lines = {
                "Super-heroes never die",
                "Exemplars never die",
                "Super-villains never die",
                "Good people are never responsible for anyone's death, even by omission",
                "The Root Conflict between characters and their Exemplars will not be resolved",
                "The Secret Identity of the hero will not be exposed",
                "The course of history will not diverge from real-world history"
        };
        for (String l : lines) if (!state.comicsCode().contains(l)) state.comicsCode().add(l);
        state.eventLog().log(EventLogEntry.Category.SYSTEM, "Loaded the 1950s example Comics Code.");
        refreshModel();
    }
}
