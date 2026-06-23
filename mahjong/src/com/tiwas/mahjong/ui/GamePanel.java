package com.tiwas.mahjong.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;

import com.tiwas.mahjong.engine.GameEngine;
import com.tiwas.mahjong.engine.HandResult;
import com.tiwas.mahjong.model.GameState;
import com.tiwas.mahjong.model.Player;
import com.tiwas.mahjong.model.Tile;
import com.tiwas.mahjong.model.TileSuit;
import com.tiwas.mahjong.model.Constants;
import com.tiwas.mahjong.engine.TurnStatus;

/**
 * The whole playing surface and the controller that drives the engine loop.
 * The UI only calls public engine methods; it never touches engine internals.
 */
public final class GamePanel extends JPanel {

    private static final Color FELT = new Color(24, 92, 60);

    private final GameEngine engine;

    private final WallPanel wallPanel = new WallPanel();
    private final ScorePanel scorePanel = new ScorePanel();
    private final HandPanel humanHand = new HandPanel(true);
    private final MeldPanel humanMeld = new MeldPanel();

    // Three AI seats, mapped to player indices 1,2,3.
    private final int[] aiIndex = { 1, 2, 3 };
    private final HandPanel[] aiHand = new HandPanel[3];
    private final MeldPanel[] aiMeld = new MeldPanel[3];
    private final JLabel[] aiTitle = new JLabel[3];

    private final JPanel discardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
    private final JTextArea logArea = new JTextArea(8, 30);
    private final JLabel instruction = new JLabel(" ");

    private final JButton drawBtn = new JButton("Draw Tile");
    private final JButton mahjongBtn = new JButton("Declare Mahjong");
    private final JButton kongBtn = new JButton("Kong");
    private final JPanel claimBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
    private final JButton pungBtn = new JButton("Pung");
    private final JButton claimMjBtn = new JButton("Mahjong");
    private final JButton passBtn = new JButton("Pass");
    private final JLabel claimTimerLabel = new JLabel();

    private Timer claimTimer;
    private int claimSecondsLeft;

    public GamePanel(GameEngine engine) {
        this.engine = engine;
        setLayout(new BorderLayout(6, 6));
        setBackground(FELT);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        add(buildTitleBar(), BorderLayout.NORTH);
        add(buildSideColumn(), BorderLayout.WEST);
        add(buildTable(), BorderLayout.CENTER);
        add(buildHumanArea(), BorderLayout.SOUTH);

        wireButtons();
    }

    // ---------------- layout ----------------

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JLabel title = new JLabel("Tiwa's Mah Jong", JLabel.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 26));
        title.setForeground(new Color(255, 235, 160));
        JLabel demo = new JLabel("DEMO VERSION", JLabel.CENTER);
        demo.setFont(new Font("SansSerif", Font.BOLD, 14));
        demo.setForeground(Color.WHITE);
        demo.setOpaque(true);
        demo.setBackground(new Color(180, 40, 40));
        demo.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        bar.add(title, BorderLayout.CENTER);
        bar.add(demo, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildSideColumn() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.add(wallPanel);
        col.add(Box.createVerticalStrut(8));
        col.add(scorePanel);
        return col;
    }

    private JPanel buildTable() {
        JPanel table = new JPanel(new BorderLayout(4, 4));
        table.setOpaque(false);

        aiHand[1] = new HandPanel(false); // across (top)
        aiHand[0] = new HandPanel(false); // left
        aiHand[2] = new HandPanel(false); // right
        for (int i = 0; i < 3; i++) {
            aiMeld[i] = new MeldPanel();
            aiTitle[i] = new JLabel();
            aiTitle[i].setForeground(Color.WHITE);
            aiTitle[i].setFont(new Font("SansSerif", Font.BOLD, 12));
            if (aiHand[i] == null) {
                aiHand[i] = new HandPanel(false);
            }
        }

        table.add(seatPanel(1, BorderLayout.NORTH), BorderLayout.NORTH);
        table.add(seatPanel(0, BorderLayout.WEST), BorderLayout.WEST);
        table.add(seatPanel(2, BorderLayout.EAST), BorderLayout.EAST);

        // centre: discards + log
        JPanel centre = new JPanel(new BorderLayout(4, 4));
        centre.setOpaque(false);
        JLabel dt = new JLabel("Discards");
        dt.setForeground(new Color(220, 230, 220));
        dt.setFont(new Font("SansSerif", Font.BOLD, 12));
        discardsPanel.setOpaque(true);
        discardsPanel.setBackground(new Color(18, 70, 46));
        discardsPanel.setBorder(BorderFactory.createLineBorder(new Color(12, 50, 32)));
        discardsPanel.setPreferredSize(new Dimension(420, 150));
        JPanel dWrap = new JPanel(new BorderLayout());
        dWrap.setOpaque(false);
        dWrap.add(dt, BorderLayout.NORTH);
        dWrap.add(discardsPanel, BorderLayout.CENTER);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(420, 130));

        centre.add(dWrap, BorderLayout.CENTER);
        centre.add(logScroll, BorderLayout.SOUTH);
        table.add(centre, BorderLayout.CENTER);
        return table;
    }

    private JPanel seatPanel(int aiSlot, String region) {
        JPanel p = new JPanel(new BorderLayout(2, 2));
        p.setOpaque(false);
        p.add(aiTitle[aiSlot], BorderLayout.NORTH);
        p.add(aiHand[aiSlot], BorderLayout.CENTER);
        p.add(aiMeld[aiSlot], BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildHumanArea() {
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        instruction.setForeground(new Color(255, 245, 200));
        instruction.setFont(new Font("SansSerif", Font.BOLD, 15));
        instruction.setAlignmentX(LEFT_ALIGNMENT);

        JLabel youLabel = new JLabel("Your hand (You)");
        youLabel.setForeground(Color.WHITE);
        youLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        youLabel.setAlignmentX(LEFT_ALIGNMENT);

        humanMeld.setAlignmentX(LEFT_ALIGNMENT);
        humanHand.setAlignmentX(LEFT_ALIGNMENT);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        buttons.setOpaque(false);
        buttons.add(drawBtn);
        buttons.add(mahjongBtn);
        buttons.add(kongBtn);
        claimBar.setOpaque(false);
        claimBar.add(new JLabel("Claim the discard:"));
        claimBar.add(pungBtn);
        claimBar.add(claimMjBtn);
        claimBar.add(passBtn);
        claimBar.add(claimTimerLabel);
        claimTimerLabel.setForeground(new Color(255, 220, 120));
        claimTimerLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        buttons.add(claimBar);
        buttons.setAlignmentX(LEFT_ALIGNMENT);

        south.add(instruction);
        south.add(youLabel);
        south.add(humanMeld);
        south.add(humanHand);
        south.add(buttons);
        return south;
    }

    // ---------------- control ----------------

    private void wireButtons() {
        drawBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                engine.humanDraw();
                runEngine();
            }
        });
        mahjongBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                engine.humanSelfMahjong();
                runEngine();
            }
        });
        kongBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doKong();
            }
        });
        humanHand.setTileSelectListener(new HandPanel.TileSelectListener() {
            public void tileSelected(Tile t) {
                engine.humanDiscard(t);
                humanHand.setSelectable(false);
                render();
                Timer t2 = new Timer(350, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ((Timer) e.getSource()).stop();
                        runEngine();
                    }
                });
                t2.setRepeats(false);
                t2.start();
            }
        });
        pungBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopClaimTimer();
                engine.humanClaimPung();
                runEngine();
            }
        });
        claimMjBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopClaimTimer();
                engine.humanClaimMahjong();
                runEngine();
            }
        });
        passBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopClaimTimer();
                engine.humanPass();
                runEngine();
            }
        });
    }

    /** Begin a new game. */
    public void startGame() {
        engine.startGame();
        appendLog(engine.drainLog());
        render();
        runEngine();
    }

    private void runEngine() {
        TurnStatus st = engine.advance();
        appendLog(engine.drainLog());
        render();
        handle(st);
    }

    private void handle(TurnStatus st) {
        setClaimBarVisible(false);
        switch (st.kind) {
            case AWAIT_HUMAN_DRAW:
                instruction.setText("Your turn — draw a tile from the wall.");
                drawBtn.setEnabled(true);
                mahjongBtn.setEnabled(false);
                kongBtn.setEnabled(false);
                humanHand.setSelectable(false);
                break;
            case AWAIT_HUMAN_DISCARD:
                instruction.setText("Click a tile to discard"
                        + (engine.canHumanSelfMahjong() ? " — or declare Mahjong!" : "."));
                drawBtn.setEnabled(false);
                mahjongBtn.setEnabled(engine.canHumanSelfMahjong());
                kongBtn.setEnabled(!engine.humanConcealedKongOptions().isEmpty()
                        || !engine.humanUpgradeKongOptions().isEmpty());
                humanHand.setSelectable(true);
                break;
            case AWAIT_HUMAN_CLAIM:
                instruction.setText("You may claim " + st.claimableDiscard.displayName()
                        + " discarded by " + engine.getState().getPlayer(st.discardBy).getName() + ".");
                drawBtn.setEnabled(false);
                mahjongBtn.setEnabled(false);
                kongBtn.setEnabled(false);
                humanHand.setSelectable(false);
                pungBtn.setEnabled(st.canPung);
                claimMjBtn.setEnabled(st.canMahjong);
                setClaimBarVisible(true);
                startClaimTimer();
                break;
            case HAND_OVER:
                handleHandOver();
                break;
            default:
                break;
        }
        render();
    }

    private void handleHandOver() {
        instruction.setText("Hand over.");
        drawBtn.setEnabled(false);
        mahjongBtn.setEnabled(false);
        kongBtn.setEnabled(false);
        humanHand.setSelectable(false);
        HandResult result = engine.getLastResult();
        render();
        DialogUtils.showHandResult(this, engine.getState(), result);

        engine.nextHand();
        if (engine.isGameOver()) {
            String standings = DialogUtils.finalStandings(engine.getState());
            int again = JOptionPane.showConfirmDialog(this,
                    standings + "\n\nPlay another game?", "Game over",
                    JOptionPane.YES_NO_OPTION);
            if (again == JOptionPane.YES_OPTION) {
                startGame();
            } else {
                instruction.setText("Thanks for playing the demo!");
            }
            return;
        }
        appendLog(engine.drainLog());
        render();
        runEngine();
    }

    private void doKong() {
        List<Tile> concealed = engine.humanConcealedKongOptions();
        List<Tile> upgrades = engine.humanUpgradeKongOptions();
        if (concealed.isEmpty() && upgrades.isEmpty()) {
            return;
        }
        java.util.List<String> opts = new java.util.ArrayList<String>();
        java.util.List<Tile> tiles = new java.util.ArrayList<Tile>();
        java.util.List<Boolean> isConcealed = new java.util.ArrayList<Boolean>();
        for (int i = 0; i < concealed.size(); i++) {
            opts.add("Concealed kong of " + concealed.get(i).displayName());
            tiles.add(concealed.get(i));
            isConcealed.add(Boolean.TRUE);
        }
        for (int i = 0; i < upgrades.size(); i++) {
            opts.add("Upgrade pung to kong: " + upgrades.get(i).displayName());
            tiles.add(upgrades.get(i));
            isConcealed.add(Boolean.FALSE);
        }
        String chosen = (String) JOptionPane.showInputDialog(this, "Declare which kong?",
                "Kong", JOptionPane.QUESTION_MESSAGE, null,
                opts.toArray(), opts.get(0));
        if (chosen == null) {
            return;
        }
        int idx = opts.indexOf(chosen);
        if (isConcealed.get(idx).booleanValue()) {
            engine.humanConcealedKong(tiles.get(idx));
        } else {
            engine.humanUpgradeKong(tiles.get(idx));
        }
        appendLog(engine.drainLog());
        runEngine();
    }

    // ---------------- claim timer ----------------

    private void startClaimTimer() {
        stopClaimTimer();
        claimSecondsLeft = Constants.CLAIM_TIMEOUT_SECONDS;
        claimTimerLabel.setText("  " + claimSecondsLeft + "s");
        claimTimer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                claimSecondsLeft--;
                if (claimSecondsLeft <= 0) {
                    stopClaimTimer();
                    engine.humanPass();
                    runEngine();
                } else {
                    claimTimerLabel.setText("  " + claimSecondsLeft + "s");
                }
            }
        });
        claimTimer.start();
    }

    private void stopClaimTimer() {
        if (claimTimer != null) {
            claimTimer.stop();
            claimTimer = null;
        }
        claimTimerLabel.setText("");
    }

    private void setClaimBarVisible(boolean v) {
        claimBar.setVisible(v);
    }

    // ---------------- rendering ----------------

    private void render() {
        GameState state = engine.getState();
        wallPanel.update(state);
        scorePanel.update(state);

        Player human = state.getPlayer(0);
        humanHand.update(human.getHand());
        humanMeld.update(human.getHand().getMelds(), human.getHand().getBonus());

        for (int i = 0; i < 3; i++) {
            Player p = state.getPlayer(aiIndex[i]);
            aiHand[i].update(p.getHand());
            aiMeld[i].update(p.getHand().getMelds(), p.getHand().getBonus());
            String mark = "";
            if (aiIndex[i] == state.getDealer()) {
                mark += " [Dealer]";
            }
            if (aiIndex[i] == state.getCurrentTurn()) {
                mark += " <- turn";
            }
            aiTitle[i].setText(p.getName() + " (" + p.getSeatWind().label()
                    + ", AI)" + mark + "  tiles:" + p.getHand().getTiles().size());
        }

        discardsPanel.removeAll();
        List<Tile> discards = state.getDiscards();
        for (int i = 0; i < discards.size(); i++) {
            discardsPanel.add(smallTile(discards.get(i)));
        }
        discardsPanel.revalidate();
        discardsPanel.repaint();
    }

    private JLabel smallTile(Tile t) {
        JLabel l = new JLabel(t.code(), JLabel.CENTER);
        l.setOpaque(true);
        l.setBackground(Color.WHITE);
        l.setForeground(colorFor(t));
        l.setFont(new Font("Monospaced", Font.BOLD, 12));
        l.setPreferredSize(new Dimension(30, 38));
        l.setBorder(BorderFactory.createLineBorder(new Color(150, 120, 70)));
        return l;
    }

    private Color colorFor(Tile t) {
        TileSuit s = t.getSuit();
        if (s == TileSuit.DOTS) return new Color(20, 70, 170);
        if (s == TileSuit.BAMBOO) return new Color(20, 120, 40);
        if (s == TileSuit.CHARACTERS) return new Color(170, 30, 30);
        if (s == TileSuit.WIND) return new Color(70, 40, 120);
        if (s == TileSuit.DRAGON) return new Color(150, 90, 0);
        return new Color(200, 120, 0);
    }

    private void appendLog(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            logArea.append(lines.get(i) + "\n");
        }
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
