package com.whim.firetop.ui;

import com.whim.firetop.engine.Dice;
import com.whim.firetop.engine.GameEngine;
import com.whim.firetop.model.Card;
import com.whim.firetop.model.Character;
import com.whim.firetop.model.GameState;
import com.whim.firetop.model.Item;
import com.whim.firetop.model.ItemType;
import com.whim.firetop.model.Monster;
import com.whim.firetop.model.Room;
import com.whim.firetop.persistence.SaveGame;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Main application window. Owns the {@link GameEngine} and hosts the setup, play
 * and end screens in a {@link CardLayout}, plus the menu bar (New / Save / Load /
 * How to Play / Exit).
 */
public final class GameFrame extends javax.swing.JFrame implements GameEngine.Listener {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private GameEngine engine;

    // Play screen widgets.
    private final BoardPanel boardPanel = new BoardPanel();
    private final StatusPanel statusPanel = new StatusPanel();
    private final JTextArea logArea = new JTextArea(8, 40);
    private final JButton rollButton = new JButton("Roll to Move");
    private final JButton endTurnButton = new JButton("End Turn");
    private final JButton provisionButton = new JButton("Eat Provision");
    private final JButton itemButton = new JButton("Use Item");
    private final JPanel destPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
    private JPanel playScreen;

    public GameFrame() {
        super("The Warlock of Firetop Mountain — Fighting Fantasy Boardgame (unofficial)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setJMenuBar(buildMenuBar());
        buildPlayScreen();
        root.add(new SetupPanel(new SetupPanel.StartHandler() {
            public void onStart(List<String> names) { startNewGame(names); }
        }), "setup");
        root.add(playScreen, "play");
        setContentPane(root);
        cards.show(root, "setup");
        setSize(1180, 760);
        setLocationRelativeTo(null);
    }

    // ---- Menu ----------------------------------------------------------

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu game = new JMenu("Game");
        game.setMnemonic('G');

        JMenuItem newItem = new JMenuItem("New Game", 'N');
        newItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { cards.show(root, "setup"); }
        });
        JMenuItem save = new JMenuItem("Save...", 'S');
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doSave(); }
        });
        JMenuItem load = new JMenuItem("Load...", 'L');
        load.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doLoad(); }
        });
        JMenuItem rules = new JMenuItem("How to Play", 'H');
        rules.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { new HowToPlayDialog(GameFrame.this).setVisible(true); }
        });
        JMenuItem exit = new JMenuItem("Exit", 'X');
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dispose(); System.exit(0); }
        });

        game.add(newItem);
        game.add(save);
        game.add(load);
        game.addSeparator();
        game.add(rules);
        game.addSeparator();
        game.add(exit);
        bar.add(game);
        return bar;
    }

    // ---- Play screen ---------------------------------------------------

    private void buildPlayScreen() {
        playScreen = new JPanel(new BorderLayout(6, 6));
        playScreen.setBackground(Theme.BG_DARK);

        JScrollPane boardScroll = new JScrollPane(boardPanel);
        boardScroll.setBorder(BorderFactory.createLineBorder(Theme.STONE));
        boardScroll.getViewport().setBackground(Theme.BG_DARK);
        playScreen.add(boardScroll, BorderLayout.CENTER);

        JScrollPane statusScroll = new JScrollPane(statusPanel);
        statusScroll.setPreferredSize(new Dimension(300, 100));
        statusScroll.setBorder(BorderFactory.createLineBorder(Theme.STONE));
        playScreen.add(statusScroll, BorderLayout.EAST);

        // Bottom: toolbar + destinations + log.
        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.setBackground(Theme.BG_DARK);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.setBackground(Theme.BG_PANEL);
        style(rollButton, Theme.EMERALD, 'R');
        style(endTurnButton, Theme.ROYAL, 'E');
        style(provisionButton, Theme.STONE, 'P');
        style(itemButton, Theme.STONE, 'U');
        toolbar.add(rollButton);
        toolbar.add(endTurnButton);
        toolbar.add(provisionButton);
        toolbar.add(itemButton);
        destPanel.setBackground(Theme.BG_PANEL);
        toolbar.add(destPanel);
        bottom.add(toolbar, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setBackground(Theme.BG_PANEL);
        logArea.setForeground(Theme.PARCHMENT);
        logArea.setFont(Theme.MONO);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createLineBorder(Theme.STONE));
        logScroll.setPreferredSize(new Dimension(100, 150));
        bottom.add(logScroll, BorderLayout.CENTER);

        playScreen.add(bottom, BorderLayout.SOUTH);

        rollButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doRoll(); }
        });
        endTurnButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doEndTurn(); }
        });
        provisionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { engine.eatProvision(); }
        });
        itemButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { doUseItem(); }
        });

        boardPanel.setMoveHandler(new BoardPanel.MoveHandler() {
            public void onRoomClicked(int roomId) { doMove(roomId); }
        });

        // Keyboard: Enter triggers whichever primary action is enabled.
        registerKey("SPACE", new Runnable() {
            public void run() {
                if (rollButton.isEnabled()) {
                    doRoll();
                } else if (endTurnButton.isEnabled()) {
                    doEndTurn();
                }
            }
        });
    }

    private void registerKey(String key, final Runnable action) {
        playScreen.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key), key);
        playScreen.getActionMap().put(key, new javax.swing.AbstractAction() {
            public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void style(JButton b, Color c, char mnemonic) {
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFont(Theme.BODY_BOLD);
        b.setMnemonic(mnemonic);
    }

    // ---- Game lifecycle ------------------------------------------------

    private void startNewGame(List<String> names) {
        long seed = System.nanoTime();
        engine = GameEngine.newGame(names, seed);
        engine.setListener(this);
        logArea.setText("");
        boardPanel.setState(engine.getState());
        statusPanel.setState(engine.getState());
        for (String line : engine.getState().getLog()) {
            logArea.append(line + "\n");
        }
        cards.show(root, "play");
        beginTurn();
    }

    private void loadGame(GameState state) {
        engine = new GameEngine(state);
        engine.setListener(this);
        logArea.setText("");
        boardPanel.setState(state);
        statusPanel.setState(state);
        for (String line : state.getLog()) {
            logArea.append(line + "\n");
        }
        cards.show(root, "play");
        if (state.isGameOver()) {
            showEndScreen();
        } else {
            beginTurn();
        }
    }

    private void beginTurn() {
        if (engine.getState().isGameOver()) {
            showEndScreen();
            return;
        }
        boardPanel.setReachable(null);
        destPanel.removeAll();
        destPanel.revalidate();
        destPanel.repaint();
        rollButton.setEnabled(true);
        endTurnButton.setEnabled(false);
        updateSideButtons();
        statusPanel.refresh();
    }

    private void updateSideButtons() {
        boolean alive = engine.currentCharacter().isAlive();
        provisionButton.setEnabled(alive && engine.currentCharacter().getProvisions() > 0);
        itemButton.setEnabled(alive && hasUsableItem());
    }

    private boolean hasUsableItem() {
        for (Item it : engine.currentCharacter().getInventory()) {
            if (it.getType() == ItemType.POTION) {
                return true;
            }
        }
        return false;
    }

    private void doRoll() {
        if (engine == null || !rollButton.isEnabled()) {
            return;
        }
        engine.rollMovement();
        Set<Integer> reach = engine.reachableRooms();
        boardPanel.setReachable(reach);
        rollButton.setEnabled(false);
        buildDestinationButtons(reach);
        if (reach.isEmpty()) {
            appendLog("No corridors within reach — end your turn.");
            endTurnButton.setEnabled(true);
        }
    }

    private void buildDestinationButtons(Set<Integer> reach) {
        destPanel.removeAll();
        for (final Integer rid : reach) {
            Room r = engine.getBoard().getRoom(rid);
            JButton b = new JButton("→ " + (r.isVisited() ? r.getName() : "Unknown room"));
            b.setFont(Theme.BODY);
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { doMove(rid); }
            });
            destPanel.add(b);
        }
        destPanel.revalidate();
        destPanel.repaint();
    }

    private void doMove(int roomId) {
        if (engine.hasMoved() || !engine.reachableRooms().contains(roomId)) {
            return;
        }
        boardPanel.setReachable(null);
        destPanel.removeAll();
        destPanel.revalidate();
        destPanel.repaint();
        GameEngine.RoomResolution res = engine.moveTo(roomId);
        processResolution(res, true);
    }

    private void processResolution(GameEngine.RoomResolution res, boolean topLevel) {
        switch (res.getKind()) {
            case COMBAT:
                Room room = topLevel
                        ? engine.getBoard().getRoom(engine.currentCharacter().getRoomId())
                        : null;
                startCombat(res.getMonster(), room);
                break;
            case CARD:
                Card card = res.getCard();
                int before = engine.getState().getLog().size();
                GameEngine.RoomResolution follow =
                        engine.resolveCard(card, res.getSource(), engine.currentCharacter());
                List<String> outcome = new ArrayList<String>(
                        engine.getState().getLog().subList(before, engine.getState().getLog().size()));
                new CardDialog(this, card, outcome).setVisible(true);
                if (follow.getKind() == GameEngine.RoomResolution.Kind.COMBAT) {
                    startCombat(follow.getMonster(), null);
                } else {
                    afterAction();
                }
                break;
            default:
                afterAction();
                break;
        }
    }

    private void startCombat(Monster monster, Room room) {
        Character c = engine.currentCharacter();
        CombatDialog dlg = new CombatDialog(this, c, monster, engine.getDice());
        dlg.setVisible(true);
        if (dlg.getResult() == CombatDialog.Result.PLAYER_WON) {
            boolean victory = engine.onMonsterDefeated(room, monster);
            if (victory) {
                showEndScreen();
                return;
            }
        } else {
            engine.onCharacterDefeated(c);
            if (engine.getState().isGameOver()) {
                showEndScreen();
                return;
            }
        }
        afterAction();
    }

    private void afterAction() {
        statusPanel.refresh();
        boardPanel.repaint();
        if (engine.getState().isGameOver()) {
            showEndScreen();
            return;
        }
        updateSideButtons();
        endTurnButton.setEnabled(true);
        rollButton.setEnabled(false);
        if (!engine.currentCharacter().isAlive()) {
            appendLog(engine.currentCharacter().getName() + " is dead — end the turn to pass on.");
        }
    }

    private void doEndTurn() {
        if (engine == null || !endTurnButton.isEnabled()) {
            return;
        }
        boolean ok = engine.endTurn();
        if (!ok || engine.getState().isGameOver()) {
            showEndScreen();
            return;
        }
        beginTurn();
    }

    private void doUseItem() {
        List<Item> potions = new ArrayList<Item>();
        for (Item it : engine.currentCharacter().getInventory()) {
            if (it.getType() == ItemType.POTION) {
                potions.add(it);
            }
        }
        if (potions.isEmpty()) {
            return;
        }
        Item chosen = (Item) JOptionPane.showInputDialog(this, "Use which item?", "Use Item",
                JOptionPane.PLAIN_MESSAGE, null, potions.toArray(), potions.get(0));
        if (chosen != null) {
            engine.usePotion(chosen);
            updateSideButtons();
        }
    }

    private void showEndScreen() {
        EndScreen end = new EndScreen(engine.getState(), new EndScreen.NewGameHandler() {
            public void onNewGame() { cards.show(root, "setup"); }
        });
        root.add(end, "end");
        cards.show(root, "end");
    }

    // ---- Save / load ---------------------------------------------------

    private void doSave() {
        if (engine == null) {
            JOptionPane.showMessageDialog(this, "No game in progress.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("firetop.sav"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                engine.syncForSave();
                SaveGame.save(engine.getState(), fc.getSelectedFile());
                appendLog("Game saved to " + fc.getSelectedFile().getName() + ".");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
            }
        }
    }

    private void doLoad() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                GameState state = SaveGame.load(fc.getSelectedFile());
                loadGame(state);
                appendLog("Game loaded from " + fc.getSelectedFile().getName() + ".");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage());
            }
        }
    }

    // ---- GameEngine.Listener -------------------------------------------

    public void onLog(String line) {
        appendLog(line);
    }

    public void onStateChanged() {
        statusPanel.refresh();
        boardPanel.repaint();
    }

    private void appendLog(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
