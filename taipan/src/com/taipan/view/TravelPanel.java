package com.taipan.view;

import com.taipan.model.PortCity;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;

/** Choose the next port of call. Sailing anywhere advances one month. */
public class TravelPanel extends JPanel {

    private final GameFrame frame;
    private final JPanel portsPanel = new JPanel();
    private final JLabel header = new JLabel();

    public TravelPanel(GameFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JLabel title = new JLabel("Where will you sail?");
        title.setFont(new Font("Serif", Font.BOLD, 24));
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(title);
        north.add(Box.createVerticalStrut(6));
        north.add(header);
        add(north, BorderLayout.NORTH);

        portsPanel.setLayout(new GridLayout(0, 2, 12, 12));
        add(portsPanel, BorderLayout.CENTER);

        JButton back = new JButton("Never mind — stay in port");
        back.addActionListener(e -> frame.showPort());
        add(back, BorderLayout.SOUTH);
    }

    public void refresh() {
        PortCity here = frame.getController().getState().getLocation();
        header.setText("You are currently in " + here.display()
                + ". Sailing anywhere takes one month.");
        portsPanel.removeAll();
        for (final PortCity p : PortCity.values()) {
            JButton b = new JButton(p.display() + (p == here ? "  (here)" : ""));
            if (p == here) {
                b.setEnabled(false);
            } else {
                b.addActionListener(e -> frame.startVoyage(p));
            }
            portsPanel.add(b);
        }
        portsPanel.revalidate();
        portsPanel.repaint();
    }
}
