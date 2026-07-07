package com.whim.albion.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.whim.albion.api.GameController;
import com.whim.albion.api.Views.DialogueView;

/**
 * Topic-based dialogue screen: speaker portrait + name, the current line of text, and a
 * vertical list of numbered reply buttons routed to {@link GameController#selectDialogueOption}.
 */
final class DialoguePanel extends JPanel {

    private final GameController controller;
    private final JPanel options;
    private final TextArea textArea;

    DialoguePanel(GameController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        setBackground(new Color(16, 14, 20));
        textArea = new TextArea();
        options = new JPanel();
        options.setBackground(new Color(22, 20, 28));
        options.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(textArea, BorderLayout.CENTER);
        add(options, BorderLayout.SOUTH);
    }

    void refresh() {
        options.removeAll();
        DialogueView d = controller.state().dialogue();
        if (d == null) { options.revalidate(); options.repaint(); return; }
        List<String> opts = d.options();
        options.setLayout(new GridLayout(opts.size(), 1, 4, 4));
        for (int i = 0; i < opts.size(); i++) {
            final int idx = i;
            JButton b = new JButton((i + 1) + ". " + opts.get(i));
            b.setFocusable(false);
            b.setHorizontalAlignment(JButton.LEFT);
            b.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                    controller.selectDialogueOption(idx);
                }
            });
            options.add(b);
        }
        options.revalidate();
        options.repaint();
        textArea.repaint();
    }

    private final class TextArea extends JPanel {
        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(16, 14, 20));
            g.fillRect(0, 0, getWidth(), getHeight());
            DialogueView d = controller.state().dialogue();
            if (d == null) return;
            SpriteFactory.drawPortrait(g, d.portraitKey(), 20, 20, 96, 120);
            g.setColor(new Color(230, 210, 150));
            g.setFont(UiUtil.UI_BOLD.deriveFont(16f));
            g.drawString(d.speaker(), 130, 40);
            g.setColor(UiUtil.INK);
            g.setFont(UiUtil.UI_FONT.deriveFont(14f));
            drawWrapped(g, d.text(), 130, 64, getWidth() - 150);
        }

        private void drawWrapped(Graphics2D g, String text, int x, int y, int maxW) {
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            int ly = y;
            for (String word : words) {
                String test = line.length() == 0 ? word : line + " " + word;
                if (g.getFontMetrics().stringWidth(test) > maxW) {
                    g.drawString(line.toString(), x, ly);
                    ly += 20;
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(test);
                }
            }
            if (line.length() > 0) g.drawString(line.toString(), x, ly);
        }
    }

    @Override public Dimension getPreferredSize() { return new Dimension(640, 560); }
}
