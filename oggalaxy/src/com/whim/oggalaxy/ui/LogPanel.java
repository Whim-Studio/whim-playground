package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.Views;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

/**
 * Bottom event feed: a colour-coded, auto-scrolling list of the most recent log
 * entries, tinted by {@link com.whim.oggalaxy.api.Ids.LogCategory}.
 */
public final class LogPanel extends JPanel implements Refreshable {

    private final DefaultListModel<Views.LogEntryView> model = new DefaultListModel<Views.LogEntryView>();
    private final JList<Views.LogEntryView> list = new JList<Views.LogEntryView>(model);
    private int lastSize = -1;

    public LogPanel() {
        setOpaque(true);
        setBackground(Palette.BG_PANEL);
        setLayout(new BorderLayout());
        setBorder(UiUtil.panelBorder("Event Log"));

        list.setBackground(Palette.BG_DEEP);
        list.setCellRenderer(new Renderer());
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        sp.getViewport().setBackground(Palette.BG_DEEP);
        add(sp, BorderLayout.CENTER);
    }

    @Override
    public void refresh(Views.GameStateView state) {
        if (state == null) return;
        List<Views.LogEntryView> log = state.log();
        if (log.size() == lastSize) return;
        lastSize = log.size();
        model.clear();
        for (Views.LogEntryView e : log) model.addElement(e);
        if (!model.isEmpty()) {
            int last = model.size() - 1;
            list.ensureIndexIsVisible(last);
        }
    }

    private static final class Renderer extends javax.swing.JLabel
            implements ListCellRenderer<Views.LogEntryView> {
        Renderer() {
            setOpaque(true);
            setBorder(UiUtil.padded(1, 8, 1, 8));
            setFont(Palette.FONT_SMALL);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Views.LogEntryView> list,
                Views.LogEntryView value, int index, boolean selected, boolean focus) {
            java.awt.Color c = Palette.logColor(value.category());
            setText("<html><span style='color:#606a84'>" + value.timeText() + "</span> "
                    + "<span style='color:" + hex(c) + "'>[" + value.category() + "]</span> "
                    + escape(value.message()) + "</html>");
            setBackground(selected ? Palette.BG_PANEL_HI : Palette.BG_DEEP);
            return this;
        }

        private static String hex(java.awt.Color c) {
            return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
