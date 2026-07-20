package com.heroquest.ui;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** Top-level window: dungeon board in the centre, dashboard on the right. */
public final class GameFrame extends JFrame {

    public GameFrame() {
        super("HeroQuest — The Trial");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        GameController controller = new GameController();
        BoardPanel board = new BoardPanel(controller);
        SidePanel side = new SidePanel(controller);

        JScrollPane boardScroll = new JScrollPane(board,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        boardScroll.getVerticalScrollBar().setUnitIncrement(16);
        boardScroll.setPreferredSize(new Dimension(760, 540));

        add(boardScroll, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);

        controller.attachView(board, side, this);

        pack();
        setMinimumSize(new Dimension(880, 560));
        setLocationRelativeTo(null);
    }
}
