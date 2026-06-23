package com.tiwas.rpg.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

/**
 * Top-level window for the Tiwas RPG demo. Hosts a {@link CharacterCreatorPanel}
 * and an {@link AdventurePanel} in a tabbed layout, with a prominent
 * "Demo Version" banner.
 */
public final class MainFrame extends JFrame {

    public MainFrame() {
        super("Tiwas RPG — Demo Version");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(buildBanner(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Character Creator", new CharacterCreatorPanel());
        tabs.addTab("Adventure / Session", new AdventurePanel());
        add(tabs, BorderLayout.CENTER);

        setSize(960, 720);
        setMinimumSize(new java.awt.Dimension(760, 560));
        setLocationRelativeTo(null);
    }

    /** Makes the frame visible. Must be called on the EDT. */
    public void showFrame() {
        setVisible(true);
    }

    private JPanel buildBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(new Color(40, 28, 70));
        banner.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        JLabel title = new JLabel("Tiwas RPG");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JLabel demo = new JLabel("DEMO VERSION", SwingConstants.RIGHT);
        demo.setForeground(new Color(255, 210, 90));
        demo.setFont(demo.getFont().deriveFont(Font.BOLD, 16f));

        banner.add(title, BorderLayout.WEST);
        banner.add(demo, BorderLayout.EAST);
        return banner;
    }
}
