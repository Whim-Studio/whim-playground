package com.whim.tarot.ui;

import com.whim.tarot.domain.Card;
import com.whim.tarot.domain.DrawnCard;
import com.whim.tarot.domain.Orientation;
import com.whim.tarot.domain.SpreadPosition;
import com.whim.tarot.domain.SpreadType;
import com.whim.tarot.engine.PositionedCard;
import com.whim.tarot.engine.Reading;
import com.whim.tarot.engine.TarotEngine;
import com.whim.tarot.image.ImageLoader;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Main application window for the Rider-Waite-Smith Tarot reader.
 *
 * <p>Owns a {@link TarotEngine}. The deal/interpret call and every image fetch
 * run off the EDT (via {@link SwingWorker}); results are marshalled back with
 * {@link SwingUtilities#invokeLater}, so the UI stays responsive throughout.
 */
public final class MainWindow extends JFrame {

    private static final Color FELT = new Color(0x14, 0x1E, 0x18);
    private static final Color PANEL = new Color(0x23, 0x30, 0x29);
    private static final Color INK = new Color(0xEC, 0xE6, 0xD4);

    private final TarotEngine engine;

    private final JComboBox<SpreadType> spreadSelector;
    private final JButton shuffleButton;
    private final JButton drawButton;
    private final JLabel statusLabel;

    private final SpreadPanel spreadPanel;

    private final JTextArea detailArea;
    private final JTextArea synthesisArea;

    public MainWindow() {
        super("Tarot Reader — Rider-Waite-Smith");
        this.engine = new TarotEngine();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setBackground(FELT);
        setLayout(new BorderLayout(8, 8));

        // --- top toolbar -------------------------------------------------
        spreadSelector = new JComboBox<SpreadType>(
                new DefaultComboBoxModel<SpreadType>(SpreadType.values()));
        spreadSelector.setRenderer(new SpreadTypeRenderer());

        shuffleButton = new JButton("Shuffle Deck");
        drawButton = new JButton("Draw Cards");
        statusLabel = new JLabel("Choose a spread, shuffle, then draw.");
        statusLabel.setForeground(INK);

        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBackground(PANEL);
        JLabel spreadLabel = new JLabel("Spread: ");
        spreadLabel.setForeground(INK);
        bar.add(spreadLabel);
        bar.add(spreadSelector);
        bar.addSeparator();
        bar.add(shuffleButton);
        bar.add(Box.createHorizontalStrut(6));
        bar.add(drawButton);
        bar.addSeparator();
        bar.add(statusLabel);
        add(bar, BorderLayout.NORTH);

        // --- centre: spread canvas in a scroll pane ----------------------
        spreadPanel = new SpreadPanel(new SpreadPanel.CardSelectionListener() {
            @Override
            public void onCardSelected(CardView view) {
                showCardDetail(view.getPositioned());
            }
        });
        JScrollPane canvasScroll = new JScrollPane(spreadPanel);
        canvasScroll.setBorder(BorderFactory.createEmptyBorder());
        canvasScroll.getVerticalScrollBar().setUnitIncrement(16);
        canvasScroll.getHorizontalScrollBar().setUnitIncrement(16);
        add(canvasScroll, BorderLayout.CENTER);

        // --- east: detail + synthesis ------------------------------------
        detailArea = makeReadOnlyArea();
        detailArea.setText("Click a card to inspect it.");
        synthesisArea = makeReadOnlyArea();
        synthesisArea.setText("Draw a spread to receive your reading.");

        JScrollPane detailScroll = titledScroll(detailArea, "Card Detail");
        detailScroll.setPreferredSize(new Dimension(320, 240));
        JScrollPane synthScroll = titledScroll(synthesisArea, "The Reading");
        synthScroll.setPreferredSize(new Dimension(320, 360));

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(FELT);
        side.add(detailScroll);
        side.add(Box.createVerticalStrut(8));
        side.add(synthScroll);
        side.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 4));
        add(side, BorderLayout.EAST);

        wireActions();
    }

    /** Packs and shows the window on the EDT. */
    public void showApp() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                pack();
                setLocationRelativeTo(null);
                setVisible(true);
            }
        });
    }

    // --- behaviour ------------------------------------------------------

    private void wireActions() {
        shuffleButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                doShuffle();
            }
        });
        drawButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                doDraw();
            }
        });
    }

    private void doShuffle() {
        setBusy(true, "Shuffling the deck…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                engine.shuffle();
                return null;
            }

            @Override
            protected void done() {
                setBusy(false, "Deck shuffled. Ready to draw.");
            }
        }.execute();
    }

    private void doDraw() {
        final SpreadType type = (SpreadType) spreadSelector.getSelectedItem();
        setBusy(true, "Dealing " + type.getLabel() + "…");
        new SwingWorker<Reading, Void>() {
            @Override
            protected Reading doInBackground() {
                // Deal + interpret entirely off the EDT.
                return engine.deal(type);
            }

            @Override
            protected void done() {
                Reading reading;
                try {
                    reading = get();
                } catch (Exception ex) {
                    setBusy(false, "Could not deal: " + ex.getMessage());
                    return;
                }
                renderReading(type, reading);
                setBusy(false, type.getLabel() + " dealt. Click any card.");
            }
        }.execute();
    }

    private void renderReading(SpreadType type, Reading reading) {
        List<PositionedCard> cards = reading.getPositionedCards();
        spreadPanel.showReading(type, cards);
        synthesisArea.setText(reading.getSynthesis());
        synthesisArea.setCaretPosition(0);
        loadImagesInBackground(cards);
    }

    /** Fetches each card's RWS artwork off the EDT, repainting as each arrives. */
    private void loadImagesInBackground(List<PositionedCard> cards) {
        final List<CardView> targets = spreadPanel.getViews();
        for (int i = 0; i < targets.size(); i++) {
            final CardView view = targets.get(i);
            final String url = view.getPositioned().getDrawnCard().getCard().getImageUrl();
            new SwingWorker<BufferedImage, Void>() {
                @Override
                protected BufferedImage doInBackground() {
                    return ImageLoader.getInstance().loadQuietly(url);
                }

                @Override
                protected void done() {
                    BufferedImage img;
                    try {
                        img = get();
                    } catch (Exception ex) {
                        img = null;
                    }
                    if (img != null) {
                        view.setImage(img); // repaints; placeholder stays if null
                    }
                }
            }.execute();
        }
    }

    private void showCardDetail(PositionedCard pc) {
        DrawnCard dc = pc.getDrawnCard();
        Card card = dc.getCard();
        SpreadPosition pos = pc.getPosition();
        Orientation orientation = dc.getOrientation();

        StringBuilder sb = new StringBuilder();
        sb.append(card.getName()).append("\n");
        sb.append(card.getSuit().getLabel());
        sb.append(dc.isReversed() ? "  •  Reversed" : "  •  Upright");
        sb.append("\n\n");
        sb.append("Position ").append(pos.getIndex() + 1).append(": ").append(pos.getName()).append("\n");
        sb.append(pos.getMeaning()).append("\n\n");
        sb.append(orientation == Orientation.REVERSED ? "Reversed meaning:" : "Upright meaning:").append("\n");
        sb.append(dc.getActiveMeaning()).append("\n\n");
        sb.append("Imagery:\n").append(card.getDescription());

        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }

    private void setBusy(boolean busy, String status) {
        shuffleButton.setEnabled(!busy);
        drawButton.setEnabled(!busy);
        spreadSelector.setEnabled(!busy);
        statusLabel.setText(status);
    }

    // --- small UI factories --------------------------------------------

    private JTextArea makeReadOnlyArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(PANEL);
        area.setForeground(INK);
        area.setCaretColor(INK);
        area.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return area;
    }

    private JScrollPane titledScroll(JTextArea area, String title) {
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0x44, 0x55, 0x4C)), title));
        return sp;
    }

    /** Renders {@link SpreadType} entries using their human label + card count. */
    private static final class SpreadTypeRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(
                javax.swing.JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof SpreadType) {
                SpreadType t = (SpreadType) value;
                setText(t.getLabel() + " (" + t.getCardCount() + ")");
            }
            return this;
        }
    }
}
