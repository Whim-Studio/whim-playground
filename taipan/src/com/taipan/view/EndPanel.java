package com.taipan.view;

import com.taipan.controller.GameController;
import com.taipan.model.GameState;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Font;

/** The retirement / end-of-game report with the final net-worth summary. */
public class EndPanel extends JPanel {

    private final GameFrame frame;
    private final JLabel headline = new JLabel();
    private final JLabel body = new JLabel();

    public EndPanel(GameFrame frame) {
        this.frame = frame;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(50, 60, 50, 60));

        headline.setFont(new Font("Serif", Font.BOLD, 34));
        headline.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton again = new JButton("Play Again");
        again.setAlignmentX(Component.CENTER_ALIGNMENT);
        again.addActionListener(e -> frame.backToNewGame());

        add(headline);
        add(Box.createVerticalStrut(24));
        add(body);
        add(Box.createVerticalStrut(36));
        add(again);
    }

    public void refresh() {
        GameController c = frame.getController();
        GameState s = c.getState();

        boolean sunk = s.getShip().isSunk() && !s.isRetired();
        headline.setText(sunk ? "Your ship is lost." : "The " + esc(s.getFirmName()) + " retires.");

        long score = c.finalScore();
        int months = s.monthsElapsed();
        long perMonth = months > 0 ? score / months : score;

        StringBuilder sb = new StringBuilder("<html><div style='text-align:center;font-family:monospace;font-size:13px'>");
        sb.append("Taipan ").append(esc(s.getTaipanName())).append("<br><br>");
        if (sunk) {
            sb.append("You went down with your ship in the Far East trade.<br><br>");
        } else {
            sb.append("You retire after ").append(months).append(" month(s) at sea.<br><br>");
        }
        sb.append("Cash:        $").append(s.getCash()).append("<br>");
        sb.append("Bank:        $").append(s.getBank()).append("<br>");
        sb.append("Debt:        $").append(s.getDebt()).append("<br>");
        sb.append("<b>Net worth:   $").append(score).append("</b><br>");
        sb.append("Per month:   $").append(perMonth).append("<br><br>");
        sb.append("Final rank:  <b>").append(c.rank()).append("</b>");
        sb.append("</div></html>");
        body.setText(sb.toString());
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
