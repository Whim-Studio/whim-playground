package com.whim.merchantprince.ui;

import com.whim.merchantprince.app.Game;
import com.whim.merchantprince.app.Screen;
import com.whim.merchantprince.engine.PricingEngine;
import com.whim.merchantprince.model.City;
import com.whim.merchantprince.model.Family;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Good;
import com.whim.merchantprince.model.TransportUnit;
import com.whim.merchantprince.render.Palette;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * The city market (GAME_DESIGN_REFERENCE §3): a per-good table of local stock and
 * live buy/sell prices, with buy/sell controls that move goods between the city and
 * a chosen docked unit of the player's. Prices come from {@link PricingEngine};
 * a closed city refuses all trade until it is opened via Venice/politics.
 */
public class MarketScreen extends Screen {

    private final JLabel header = UiKit.label("", UiKit.HEAD, Palette.INK);
    private final JLabel purse = UiKit.label("", UiKit.BODY, Palette.INK);
    private final JLabel notice = UiKit.label(" ", UiKit.SMALL, Palette.CRIMSON);
    private final JComboBox<UnitItem> unitBox = new JComboBox<UnitItem>();
    private final JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JPanel rows = new JPanel(new GridLayout(0, 1, 0, 4));

    public MarketScreen(Game game) {
        super(game);
        setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        nav.setBackground(Palette.PARCHMENT_DK);
        JButton back = UiKit.button("Back to Map");
        back.addActionListener(e -> game.screens.show(Game.MAP));
        nav.add(back);
        nav.add(header);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        bar.setBackground(Palette.PARCHMENT_DK);
        bar.add(purse);
        bar.add(UiKit.label("Trading with:", UiKit.SMALL, Palette.INK));
        bar.add(unitBox);
        bar.add(UiKit.label("Qty:", UiKit.SMALL, Palette.INK));
        bar.add(qty);
        bar.add(notice);
        top.add(nav);
        top.add(bar);
        add(top, BorderLayout.NORTH);

        rows.setOpaque(false);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(rows, BorderLayout.NORTH);
        add(wrap, BorderLayout.CENTER);

        unitBox.addActionListener(e -> refreshRows());
    }

    private City currentCity() {
        GameState s = game.state;
        City c = s.city(game.focusCityId);
        return c != null ? c : s.city(0);
    }

    private TransportUnit selectedUnit() {
        UnitItem it = (UnitItem) unitBox.getSelectedItem();
        return it == null ? null : it.unit;
    }

    @Override public void onShow() {
        GameState s = game.state;
        if (s == null) return;
        City c = currentCity();
        header.setText("Market of " + c.name + (c.open ? "" : "  (CLOSED to foreigners)"));
        // Populate the player's units docked at this city.
        DefaultComboBoxModel<UnitItem> m = new DefaultComboBoxModel<UnitItem>();
        for (TransportUnit u : s.unitsOf(s.playerId)) {
            if (!u.inTransit() && u.locationCityId == c.id) m.addElement(new UnitItem(u));
        }
        unitBox.setModel(m);
        notice.setText(" ");
        refreshRows();
    }

    private void refreshRows() {
        GameState s = game.state;
        if (s == null) return;
        City c = currentCity();
        Family p = s.player();
        TransportUnit u = selectedUnit();
        purse.setText(p.florins + " florins    "
                + (u == null ? "no ship docked here" : u.displayName() + " cargo " + u.cargoUsed() + "/" + u.type.capacity));

        rows.removeAll();
        // Column header.
        rows.add(rowPanel("Good", "Stock", "Buy", "Sell", "Hold", null, null));
        for (Good g : Good.ALL) {
            long buy = PricingEngine.buyPrice(c, g);
            long sell = PricingEngine.sellPrice(c, g);
            int hold = u == null ? 0 : u.cargo[g.ordinal()];
            JButton bBuy = UiKit.button("Buy");
            JButton bSell = UiKit.button("Sell");
            bBuy.setFont(UiKit.SMALL);
            bSell.setFont(UiKit.SMALL);
            final Good good = g;
            bBuy.addActionListener(e -> doBuy(good));
            bSell.addActionListener(e -> doSell(good));
            rows.add(rowPanel(g.label, String.valueOf(c.stock[g.ordinal()]),
                    String.valueOf(buy), String.valueOf(sell), String.valueOf(hold), bBuy, bSell));
        }
        rows.revalidate();
        rows.repaint();
    }

    private JPanel rowPanel(String name, String stock, String buy, String sell, String hold,
                            JButton bBuy, JButton bSell) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        row.setOpaque(false);
        row.add(fixed(name, 150));
        row.add(fixed(stock, 60));
        row.add(fixed(buy, 70));
        row.add(fixed(sell, 70));
        row.add(fixed(hold, 60));
        if (bBuy != null) row.add(bBuy);
        if (bSell != null) row.add(bSell);
        return row;
    }

    private JLabel fixed(String text, int w) {
        JLabel l = UiKit.label(text, UiKit.BODY, Palette.INK);
        l.setPreferredSize(new Dimension(w, 24));
        return l;
    }

    private int amount() { return (Integer) qty.getValue(); }

    private void doBuy(Good g) {
        GameState s = game.state;
        City c = currentCity();
        Family p = s.player();
        TransportUnit u = selectedUnit();
        if (u == null) { notice.setText("No ship docked here to load."); return; }
        if (!c.open) { notice.setText(c.name + " is closed — open it through Venetian influence first."); return; }
        int want = amount();
        int can = Math.min(want, Math.min(u.cargoFree(), c.stock[g.ordinal()]));
        if (can <= 0) { notice.setText("Cannot buy: no cargo space or no stock."); return; }
        long total = 0;
        int bought = 0;
        // Buy unit-by-unit so the price responds to falling stock (supply/demand).
        for (int i = 0; i < can; i++) {
            long price = PricingEngine.buyPrice(c, g);
            if (p.florins < price) break;
            p.florins -= price;
            c.stock[g.ordinal()]--;
            u.cargo[g.ordinal()]++;
            total += price;
            bought++;
        }
        notice.setText(bought == 0 ? "Not enough florins."
                : "Bought " + bought + " " + g.label + " for " + total + " florins.");
        refreshRows();
    }

    private void doSell(Good g) {
        GameState s = game.state;
        City c = currentCity();
        Family p = s.player();
        TransportUnit u = selectedUnit();
        if (u == null) { notice.setText("No ship docked here to unload."); return; }
        if (!c.open) { notice.setText(c.name + " is closed to foreign trade."); return; }
        int want = Math.min(amount(), u.cargo[g.ordinal()]);
        if (want <= 0) { notice.setText("You hold none of that good here."); return; }
        long total = 0;
        for (int i = 0; i < want; i++) {
            long price = PricingEngine.sellPrice(c, g);
            p.florins += price;
            c.stock[g.ordinal()]++;
            u.cargo[g.ordinal()]--;
            total += price;
        }
        notice.setText("Sold " + want + " " + g.label + " for " + total + " florins.");
        refreshRows();
    }

    @Override public String name() { return Game.MARKET; }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(Palette.PARCHMENT);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override public Dimension getPreferredSize() { return new Dimension(900, 680); }

    /** Combo item wrapping a unit with a readable label. */
    private static final class UnitItem {
        final TransportUnit unit;
        UnitItem(TransportUnit u) { this.unit = u; }
        @Override public String toString() {
            return unit.displayName() + " (" + unit.cargoUsed() + "/" + unit.type.capacity + ")";
        }
    }
}
