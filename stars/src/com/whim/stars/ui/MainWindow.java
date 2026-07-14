package com.whim.stars.ui;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.whim.stars.app.DemoGalaxy;
import com.whim.stars.app.GalaxyFactory;
import com.whim.stars.app.GameSetup;
import com.whim.stars.io.SaveGame;
import com.whim.stars.model.Galaxy;
import com.whim.stars.model.Planet;
import com.whim.stars.model.Player;
import com.whim.stars.sim.TurnEngine;
import com.whim.stars.sim.ai.SimpleAi;

/**
 * The application's main window: menu bar, the {@link GalaxyMapPanel} in the
 * centre, the {@link CommandPanel} on the east, and a status bar. It owns the
 * live {@link Galaxy} and wires the "Generate Turn" button to the
 * {@link TurnEngine}. This is the Controller in MVC — it mediates between the
 * Swing views and the pure model/engine.
 */
public final class MainWindow extends JFrame {

    private Galaxy galaxy;
    private Player human;
    private File currentFile;

    private GalaxyMapPanel mapPanel;
    private CommandPanel commandPanel;
    private final JLabel statusBar = new JLabel();

    public MainWindow(Galaxy galaxy) {
        super("Stars! — Clean-room Java/Swing Recreation");
        this.galaxy = galaxy;
        this.human = galaxy.player(DemoGalaxy.HUMAN_ID);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(buildMenuBar());

        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusBar, BorderLayout.SOUTH);

        installGalaxy(galaxy);
        pack();
        setLocationRelativeTo(null);
    }

    /** Build (or rebuild) the map + command views for a galaxy. */
    private void installGalaxy(Galaxy g) {
        this.galaxy = g;
        this.human = g.player(DemoGalaxy.HUMAN_ID);

        if (mapPanel != null) {
            remove(mapPanel);
        }
        if (commandPanel != null) {
            remove(commandPanel);
        }

        mapPanel = new GalaxyMapPanel();
        mapPanel.setGalaxy(g);
        commandPanel = new CommandPanel(human, new CommandPanel.TurnCallback() {
            @Override
            public void onGenerateTurn() {
                generateTurn();
            }
        });
        mapPanel.setSelectionListener(new GalaxyMapPanel.SelectionListener() {
            @Override
            public void onPlanetSelected(Planet planet) {
                commandPanel.showPlanet(planet);
            }
        });

        add(mapPanel, BorderLayout.CENTER);
        add(commandPanel, BorderLayout.EAST);
        commandPanel.showPlanet(null);
        commandPanel.log("Welcome, Commander. Year " + galaxy.year() + ".");
        updateStatus();
        revalidate();
        repaint();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu game = new JMenu("Game");

        JMenuItem newGame = new JMenuItem("New Game");
        newGame.addActionListener(e -> newGame());
        JMenuItem save = new JMenuItem("Save…");
        save.setAccelerator(KeyStroke.getKeyStroke("control S"));
        save.addActionListener(e -> save());
        JMenuItem load = new JMenuItem("Load…");
        load.setAccelerator(KeyStroke.getKeyStroke("control O"));
        load.addActionListener(e -> load());
        JMenuItem nextTurn = new JMenuItem("Generate Turn");
        nextTurn.setAccelerator(KeyStroke.getKeyStroke("F9"));
        nextTurn.addActionListener(e -> generateTurn());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispose());

        game.add(newGame);
        game.addSeparator();
        game.add(save);
        game.add(load);
        game.addSeparator();
        game.add(nextTurn);
        game.addSeparator();
        game.add(exit);
        bar.add(game);

        JMenu commands = new JMenu("Commands");
        JMenuItem research = new JMenuItem("Research…");
        research.setAccelerator(KeyStroke.getKeyStroke("F5"));
        research.addActionListener(e -> openResearch());
        JMenuItem designs = new JMenuItem("Ship Designs…");
        designs.addActionListener(e -> openDesigns());
        JMenuItem production = new JMenuItem("Production (selected planet)…");
        production.addActionListener(e -> openProduction());
        JMenuItem fleets = new JMenuItem("Fleets…");
        fleets.addActionListener(e -> openFleets());
        JMenuItem planets = new JMenuItem("Planet Report…");
        planets.addActionListener(e -> openPlanetReport());
        JMenuItem relations = new JMenuItem("Relations / Battle Plans…");
        relations.addActionListener(e -> openRelations());
        commands.add(research);
        commands.add(designs);
        commands.add(production);
        commands.add(fleets);
        commands.add(planets);
        commands.addSeparator();
        commands.add(relations);
        bar.add(commands);
        return bar;
    }

    // --- Command screens -------------------------------------------------------
    private void openResearch() {
        new ResearchDialog(this, human).setVisible(true);
        commandPanel.refreshResearchControls();
        afterModelChange();
    }

    private void openDesigns() {
        new ShipDesignDialog(this, human).setVisible(true);
        afterModelChange();
    }

    private void openProduction() {
        Planet sel = mapPanel.selected();
        if (sel == null || sel.ownerId() != human.id()) {
            JOptionPane.showMessageDialog(this,
                    "Select one of your planets on the map first.",
                    "Production", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new ProductionDialog(this, human, sel).setVisible(true);
        afterModelChange();
    }

    private void openFleets() {
        new FleetDialog(this, galaxy, human).setVisible(true);
        afterModelChange();
    }

    private void openPlanetReport() {
        new PlanetReportDialog(this, galaxy, human).setVisible(true);
    }

    private void openRelations() {
        new RelationsDialog(this, galaxy, human).setVisible(true);
    }

    /** Repaint views after a dialog may have edited the model. */
    private void afterModelChange() {
        Planet sel = mapPanel.selected();
        if (sel != null) {
            commandPanel.showPlanet(galaxy.planet(sel.id()));
        }
        mapPanel.repaint();
        updateStatus();
    }

    private void generateTurn() {
        int before = galaxy.year();
        new SimpleAi(galaxy).planAll(); // AI issues its orders, then the turn resolves
        new TurnEngine(galaxy).generateTurn();
        commandPanel.log("Year " + before + " → " + galaxy.year()
                + ": " + galaxy.planetsOf(human).size() + " planets, "
                + galaxy.fleetsOf(human).size() + " fleets.");
        // Keep the current selection's report fresh if it still exists.
        Planet sel = mapPanel.selected();
        if (sel != null) {
            commandPanel.showPlanet(galaxy.planet(sel.id()));
        }
        mapPanel.repaint();
        updateStatus();
    }

    private void newGame() {
        GameSetup setup = new NewGameDialog(this).showDialog();
        if (setup == null) {
            return; // cancelled
        }
        installGalaxy(GalaxyFactory.build(setup));
        currentFile = null;
    }

    private void save() {
        JFileChooser chooser = fileChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = SaveGame.withExtension(chooser.getSelectedFile());
        try {
            SaveGame.save(galaxy, file);
            currentFile = file;
            commandPanel.log("Saved to " + file.getName());
        } catch (IOException ex) {
            error("Could not save game", ex);
        }
    }

    private void load() {
        JFileChooser chooser = fileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            Galaxy loaded = SaveGame.load(file);
            installGalaxy(loaded);
            currentFile = file;
            commandPanel.log("Loaded " + file.getName() + " (year " + loaded.year() + ")");
        } catch (IOException ex) {
            error("Could not load game", ex);
        }
    }

    private JFileChooser fileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Stars! save (*" + SaveGame.EXTENSION + ")", "starsave"));
        if (currentFile != null) {
            chooser.setCurrentDirectory(currentFile.getParentFile());
        }
        return chooser;
    }

    private void updateStatus() {
        statusBar.setText("Year " + galaxy.year()
                + "    |    Player: " + human.name()
                + "    |    Tech " + human.tech().toString()
                + "    |    Planets " + galaxy.planets().size()
                + "    Fleets " + galaxy.fleets().size());
    }

    private void error(String title, Exception ex) {
        commandPanel.log(title + ": " + ex.getMessage());
        JOptionPane.showMessageDialog(this, ex.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }
}
