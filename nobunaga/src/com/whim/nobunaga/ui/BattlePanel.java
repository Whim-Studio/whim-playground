package com.whim.nobunaga.ui;

import com.whim.nobunaga.domain.BattleState;
import com.whim.nobunaga.domain.BattleUnit;
import com.whim.nobunaga.domain.GameState;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modal tactical battle view: renders the {@code cols×rows} grid, draws each
 * {@link BattleUnit} as a colored square with abbreviation + troop count, and
 * shows the day, both rice supplies and the running log.
 *
 * <p>The player clicks one of their (attacker) units, then a target tile, to
 * queue an order via {@link GameController#issueOrder}; "Next Day" resolves a day
 * via {@link GameController#battleAdvanceDay}. When the battle resolves, the
 * outcome is applied with {@link GameController#applyBattleOutcome}, a result is
 * shown, and the dialog closes.
 */
public final class BattlePanel extends JDialog {

    private static final int TILE = 40;

    private final GameController controller;
    private final BattleState battle;
    private final int playerDaimyoId;

    private final GridView grid;
    private final JLabel header;
    private final JTextArea logArea;
    private final JButton nextDay;

    private int selectedUnitId = -1;

    private BattlePanel(Frame owner, GameController controller, BattleState battle) {
        super(owner, "Battle", true);
        this.controller = controller;
        this.battle = battle;
        this.playerDaimyoId = controller.state().playerDaimyoId;

        setLayout(new BorderLayout());

        header = new JLabel();
        header.setFont(new Font("Monospaced", Font.BOLD, 14));
        header.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        add(header, BorderLayout.NORTH);

        grid = new GridView();
        add(grid, BorderLayout.CENTER);

        logArea = new JTextArea(6, 28);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(16, 18, 22));
        logArea.setForeground(new Color(210, 220, 210));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(260, 100));
        logScroll.setBorder(BorderFactory.createTitledBorder("Field report"));
        add(logScroll, BorderLayout.EAST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        nextDay = new JButton("Next Day »");
        nextDay.setFocusable(false);
        nextDay.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                advance();
            }
        });
        JButton retreat = new JButton("Retreat (close)");
        retreat.setFocusable(false);
        retreat.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        controls.add(retreat);
        controls.add(nextDay);
        add(controls, BorderLayout.SOUTH);

        refresh();
        pack();
        setLocationRelativeTo(owner);
    }

    /** Build, show modally, and (on resolution) apply the outcome. */
    public static void fight(Frame owner, GameController controller, BattleState battle) {
        BattlePanel panel = new BattlePanel(owner, controller, battle);
        panel.setVisible(true);
    }

    private void advance() {
        if (controller.battleResolved(battle)) {
            return;
        }
        controller.battleAdvanceDay(battle);
        selectedUnitId = -1;
        refresh();
        if (controller.battleResolved(battle)) {
            finish();
        }
    }

    private void finish() {
        controller.applyBattleOutcome(battle);
        boolean playerWon = battle.winnerDaimyoId != null
                && battle.winnerDaimyoId.intValue() == playerDaimyoId;
        String msg = playerWon
                ? "Victory! The province is yours."
                : "Defeat. Your army withdraws.";
        JOptionPane.showMessageDialog(this, msg + "\n\n" + battle.log,
                "Battle resolved", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private void refresh() {
        header.setText("Day " + battle.day
                + "    Attacker rice: " + battle.attackerRice
                + "    Defender rice: " + battle.defenderRice
                + (selectedUnitId >= 0 ? "    [unit #" + selectedUnitId + " selected]" : ""));
        if (battle.log != null && battle.log.length() > 0) {
            logArea.setText(battle.log);
            logArea.setCaretPosition(0);
        }
        nextDay.setEnabled(!controller.battleResolved(battle));
        grid.repaint();
    }

    private boolean isFriendly(BattleUnit u) {
        return u.getDaimyoId() == playerDaimyoId;
    }

    /** The procedural battle grid. */
    private final class GridView extends JPanel {

        GridView() {
            setPreferredSize(new Dimension(battle.cols * TILE, battle.rows * TILE));
            setBackground(new Color(58, 78, 50));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    onClick(e.getX(), e.getY());
                }
            });
        }

        private void onClick(int mx, int my) {
            if (controller.battleResolved(battle)) {
                return;
            }
            int col = mx / TILE;
            int row = my / TILE;
            if (!battle.inBounds(col, row)) {
                return;
            }
            BattleUnit here = battle.unitAt(col, row);
            if (selectedUnitId < 0) {
                if (here != null && here.isAlive() && isFriendly(here)) {
                    selectedUnitId = here.getId();
                    refresh();
                }
                return;
            }
            // A friendly unit is selected: clicking another friendly re-selects;
            // clicking any other tile issues an order toward it.
            if (here != null && here.isAlive() && isFriendly(here)
                    && here.getId() != selectedUnitId) {
                selectedUnitId = here.getId();
                refresh();
                return;
            }
            controller.issueOrder(battle, selectedUnitId, col, row);
            refresh();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            for (int r = 0; r < battle.rows; r++) {
                for (int c = 0; c < battle.cols; c++) {
                    int x = c * TILE;
                    int y = r * TILE;
                    g2.setColor(((c + r) % 2 == 0)
                            ? new Color(64, 86, 56) : new Color(58, 78, 50));
                    g2.fillRect(x, y, TILE, TILE);
                    g2.setColor(new Color(44, 60, 40));
                    g2.drawRect(x, y, TILE, TILE);
                }
            }

            for (BattleUnit u : battle.units) {
                if (u.isAlive()) {
                    drawUnit(g2, u);
                }
            }
            g2.dispose();
        }

        private void drawUnit(Graphics2D g2, BattleUnit u) {
            int x = u.getCol() * TILE;
            int y = u.getRow() * TILE;
            Color base = u.getColor() != null ? u.getColor() : Color.GRAY;
            g2.setColor(base.darker());
            g2.fillRect(x + 3, y + 3, TILE - 6, TILE - 6);
            g2.setColor(base);
            g2.fillRect(x + 5, y + 5, TILE - 10, TILE - 10);

            if (u.getId() == selectedUnitId) {
                Stroke old = g2.getStroke();
                g2.setStroke(new BasicStroke(2.4f));
                g2.setColor(new Color(250, 240, 150));
                g2.drawRect(x + 2, y + 2, TILE - 4, TILE - 4);
                g2.setStroke(old);
            }
            if (u.isCommander()) {
                g2.setColor(Color.WHITE);
                g2.drawRect(x + 5, y + 5, TILE - 10, TILE - 10);
            }

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            drawCentered(g2, u.getAbbrev(), x + TILE / 2, y + TILE / 2 - 1);

            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(x + 3, y + TILE - 12, TILE - 6, 10);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
            drawCentered(g2, String.valueOf(u.getTroops()), x + TILE / 2, y + TILE - 3);
        }

        private void drawCentered(Graphics2D g2, String text, int cx, int baselineY) {
            int w = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, cx - w / 2, baselineY);
        }
    }
}
