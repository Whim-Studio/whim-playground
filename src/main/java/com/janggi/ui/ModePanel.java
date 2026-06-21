package com.janggi.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import com.janggi.core.Side;

/** Opening screen: pick the game mode, the human's side, and AI strength. */
public class ModePanel extends JPanel {

    private final JRadioButton localMode = new JRadioButton("Local 2-player", true);
    private final JRadioButton vsComputer = new JRadioButton("Player vs Computer");

    private final JRadioButton playCho = new JRadioButton("Play as CHO (초, green — moves first)", true);
    private final JRadioButton playHan = new JRadioButton("Play as HAN (한, red)");

    private final JComboBox<String> difficulty =
            new JComboBox<String>(new String[] {"Easy (depth 1)", "Normal (depth 2)", "Hard (depth 3)"});

    private final JPanel computerOptions;

    public ModePanel(final JanggiFrame frame) {
        super(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(32, 48, 32, 48));

        JLabel title = new JLabel("장기  Janggi", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 34f));
        JLabel subtitle = new JLabel("Korean Chess", SwingConstants.CENTER);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 16f));
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.add(title);
        header.add(subtitle);

        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(localMode);
        modeGroup.add(vsComputer);

        ButtonGroup sideGroup = new ButtonGroup();
        sideGroup.add(playCho);
        sideGroup.add(playHan);

        JPanel modeBox = new JPanel(new GridLayout(0, 1, 0, 6));
        modeBox.setBorder(BorderFactory.createTitledBorder("Mode"));
        modeBox.add(localMode);
        modeBox.add(vsComputer);

        computerOptions = new JPanel(new GridLayout(0, 1, 0, 6));
        computerOptions.setBorder(BorderFactory.createTitledBorder("Computer options"));
        computerOptions.add(new JLabel("You play:"));
        computerOptions.add(playCho);
        computerOptions.add(playHan);
        JPanel diffRow = new JPanel(new BorderLayout(8, 0));
        diffRow.add(new JLabel("Difficulty:"), BorderLayout.WEST);
        diffRow.add(difficulty, BorderLayout.CENTER);
        computerOptions.add(diffRow);
        setComputerOptionsEnabled(false);

        ActionListener modeToggle = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setComputerOptionsEnabled(vsComputer.isSelected());
            }
        };
        localMode.addActionListener(modeToggle);
        vsComputer.addActionListener(modeToggle);

        JButton next = new JButton("Continue to setup →");
        next.setFont(next.getFont().deriveFont(Font.BOLD, 15f));
        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.showSetup(buildConfig());
            }
        });

        JPanel center = new JPanel();
        center.setLayout(new java.awt.GridBagLayout());
        JPanel stack = new JPanel(new GridLayout(0, 1, 0, 16));
        stack.setPreferredSize(new Dimension(440, 360));
        stack.add(modeBox);
        stack.add(computerOptions);
        stack.add(next);
        center.add(stack);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    private void setComputerOptionsEnabled(boolean enabled) {
        for (Component c : computerOptions.getComponents()) {
            c.setEnabled(enabled);
        }
        playCho.setEnabled(enabled);
        playHan.setEnabled(enabled);
        difficulty.setEnabled(enabled);
    }

    private GameConfig buildConfig() {
        if (vsComputer.isSelected()) {
            Side humanSide = playHan.isSelected() ? Side.HAN : Side.CHO;
            int depth = difficulty.getSelectedIndex() + 1;
            return new GameConfig(GameMode.VS_COMPUTER, humanSide, depth);
        }
        return new GameConfig(GameMode.LOCAL_TWO_PLAYER, null, 0);
    }
}
