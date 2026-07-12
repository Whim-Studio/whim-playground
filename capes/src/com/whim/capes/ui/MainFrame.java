package com.whim.capes.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.whim.capes.engine.GameEngine;
import com.whim.capes.engine.Roller;
import com.whim.capes.io.Persistence;
import com.whim.capes.model.EventLogEntry;
import com.whim.capes.model.GameState;

/**
 * Main application window and view router. Uses {@link CardLayout} to switch
 * between the top-level {@link View}s, with a nav bar (North), the active view
 * (Center) and the shared event log (East). A File menu saves/loads the whole
 * {@link GameState}; loading rebuilds every view against the restored state.
 */
public final class MainFrame extends JFrame {
    private GameState state;
    private GameEngine engine;

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private CharacterSheetView sheetView;
    private EventLogPanel logPanel;

    public MainFrame(GameState state, GameEngine engine) {
        super("CapesTabletop — Super Roleplaying");
        this.state = state;
        this.engine = engine;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 700));
        setLayout(new BorderLayout());
        setJMenuBar(buildMenuBar());
        add(buildNavBar(), BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);

        buildViews();
        show(View.TABLE);
        state.eventLog().log(EventLogEntry.Category.SYSTEM, "CapesTabletop ready.");
    }

    /** (Re)creates all views and the log panel against the current state/engine. */
    private void buildViews() {
        cardPanel.removeAll();
        if (logPanel != null) { remove(logPanel); }

        sheetView = new CharacterSheetView(state);
        CharacterCreationView creationView = new CharacterCreationView(state, new CharacterCreatedListener() {
            @Override public void onCharacterCreated(com.whim.capes.model.Character c) {
                sheetView.selectCharacter(c);
                show(View.CHARACTER_SHEET);
            }
        });
        cardPanel.add(creationView, View.CHARACTER_CREATION.key());
        cardPanel.add(new TableView(state, engine), View.TABLE.key());
        cardPanel.add(sheetView, View.CHARACTER_SHEET.key());
        cardPanel.add(new ComicsCodeView(state), View.COMICS_CODE.key());
        cardPanel.add(new RulesHelpView(), View.RULES_HELP.key());

        logPanel = new EventLogPanel(state.eventLog());
        add(logPanel, BorderLayout.EAST);

        cardPanel.revalidate();
        cardPanel.repaint();
        revalidate();
        repaint();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem save = new JMenuItem("Save Game…");
        save.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { save(); }
        });
        JMenuItem load = new JMenuItem("Load Game…");
        load.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { load(); }
        });
        file.add(save);
        file.add(load);
        bar.add(file);
        return bar;
    }

    private void save() {
        JFileChooser fc = chooser();
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().endsWith(Persistence.EXTENSION)) f = new File(f.getParentFile(), f.getName() + Persistence.EXTENSION);
        try {
            Persistence.save(state, f);
            JOptionPane.showMessageDialog(this, "Saved to " + f.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void load() {
        JFileChooser fc = chooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            GameState loaded = Persistence.load(fc.getSelectedFile());
            this.state = loaded;
            this.engine = new GameEngine(loaded, new Roller.SeededRoller());
            buildViews();
            show(View.TABLE);
            state.eventLog().log(EventLogEntry.Category.SYSTEM, "Loaded game from " + fc.getSelectedFile().getName() + ".");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JFileChooser chooser() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Capes save (*" + Persistence.EXTENSION + ")", "capes"));
        return fc;
    }

    private JPanel buildNavBar() {
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        nav.setBackground(Palette.INK);
        nav.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Palette.GOLD));
        for (final View v : View.values()) {
            JButton b = new JButton(v.label());
            b.setFocusPainted(false);
            b.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) { show(v); }
            });
            nav.add(b);
        }
        return nav;
    }

    public void show(View v) {
        if (v == View.CHARACTER_SHEET && sheetView != null) sheetView.refresh();
        cards.show(cardPanel, v.key());
        state.eventLog().log(EventLogEntry.Category.SYSTEM, "View → " + v.label());
        repaint();
    }
}
