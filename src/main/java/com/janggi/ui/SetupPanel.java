package com.janggi.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import com.janggi.core.SetupChoice;
import com.janggi.core.Side;

/**
 * Pre-game setup. Each human player picks their Horse/Elephant transposition
 * ({@link SetupChoice.Arrangement}). A computer-controlled side is fixed to a
 * sensible default (orthodox MSSM).
 */
public class SetupPanel extends JPanel {

    private static final SetupChoice.Arrangement DEFAULT_ARRANGEMENT = SetupChoice.Arrangement.MSSM;

    private final ArrangementChooser choChooser;
    private final ArrangementChooser hanChooser;

    public SetupPanel(final JanggiFrame frame, final GameConfig config) {
        super(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel title = new JLabel("Choose your formation", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JLabel hint = new JLabel(
                "<html><center>Pick how each Horse (馬) and Elephant (象) sit on the back rank "
                        + "(columns 1,2 / 6,7).<br>M = Horse, S = Elephant.</center></html>",
                SwingConstants.CENTER);
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.add(title);
        header.add(hint);

        choChooser = new ArrangementChooser("CHO (초 — green)", new Color(0x1f6f43),
                config.isHuman(Side.CHO));
        hanChooser = new ArrangementChooser("HAN (한 — red)", new Color(0xb22222),
                config.isHuman(Side.HAN));

        JPanel choosers = new JPanel(new GridLayout(1, 2, 24, 0));
        choosers.add(choChooser);
        choosers.add(hanChooser);

        JButton back = new JButton("← Back");
        back.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.showModeSelection();
            }
        });
        JButton start = new JButton("Start game ▶");
        start.setFont(start.getFont().deriveFont(Font.BOLD, 15f));
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.startGame(config,
                        new SetupChoice(choChooser.selected()),
                        new SetupChoice(hanChooser.selected()));
            }
        });
        JPanel buttons = new JPanel(new BorderLayout());
        buttons.add(back, BorderLayout.WEST);
        buttons.add(start, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(choosers, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    /** A titled group of radio buttons for the four arrangements. */
    private static final class ArrangementChooser extends JPanel {
        private final ButtonGroup group = new ButtonGroup();
        private final JRadioButton[] buttons;
        private final SetupChoice.Arrangement[] values = SetupChoice.Arrangement.values();

        ArrangementChooser(String title, Color color, boolean humanControlled) {
            super(new GridLayout(0, 1, 0, 8));
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(color, 2), title));

            buttons = new JRadioButton[values.length];
            for (int i = 0; i < values.length; i++) {
                SetupChoice.Arrangement a = values[i];
                JRadioButton b = new JRadioButton(describe(a), a == DEFAULT_ARRANGEMENT);
                b.setEnabled(humanControlled);
                group.add(b);
                buttons[i] = b;
                add(b);
            }
            if (!humanControlled) {
                JLabel cpu = new JLabel("(computer — using " + DEFAULT_ARRANGEMENT + ")");
                cpu.setForeground(Color.GRAY);
                add(cpu);
            }
        }

        SetupChoice.Arrangement selected() {
            for (int i = 0; i < buttons.length; i++) {
                if (buttons[i].isSelected()) {
                    return values[i];
                }
            }
            return DEFAULT_ARRANGEMENT;
        }

        private static String describe(SetupChoice.Arrangement a) {
            switch (a) {
                case MSSM:
                    return "MSSM — both Elephants inside (orthodox)";
                case SMMS:
                    return "SMMS — both Horses inside";
                case MSMS:
                    return "MSMS — left Elephant, right Horse inside";
                case SMSM:
                    return "SMSM — left Horse, right Elephant inside";
                default:
                    return a.name();
            }
        }
    }
}
