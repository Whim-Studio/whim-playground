package com.arpg.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.arpg.model.Ability;
import com.arpg.model.Character;

/**
 * Row of ability buttons plus a basic-attack button.
 *
 * <p>The shared contract exposes each ability's total cooldown but not its
 * remaining cooldown, so the bar tracks cooldowns locally: firing an ability
 * seeds a countdown of {@code getCooldown()} ticks that decrements on every
 * {@link #update} (one game tick). Buttons are disabled while on cooldown or
 * when the player lacks the resource, and a Graphics2D sweep overlay shows
 * remaining cooldown.</p>
 */
public class AbilityBarPanel extends JPanel {

    private final ActionSink sink;
    private final JPanel abilityRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    private final Map<String, Integer> cooldownRemaining = new HashMap<String, Integer>();

    private int targetIndex = 0;
    private int currentResource = 0;

    public AbilityBarPanel(ActionSink sink) {
        super(new BorderLayout());
        this.sink = sink;
        setBackground(UiTheme.BG_PANEL);
        setBorder(new EmptyBorder(6, 8, 6, 8));

        abilityRow.setOpaque(false);
        add(abilityRow, BorderLayout.CENTER);

        JButton basic = new JButton("Basic Attack");
        basic.setFocusable(false);
        basic.setFont(UiTheme.BODY_BOLD);
        basic.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sink.submit(UiActions.basicAttack(targetIndex));
            }
        });
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        east.setOpaque(false);
        east.add(basic);
        add(east, BorderLayout.EAST);
    }

    /** Combat view calls this when the player picks a different enemy. */
    public void setTargetIndex(int targetIndex) {
        this.targetIndex = Math.max(0, targetIndex);
    }

    /** Note that an ability was just fired, starting its local cooldown. */
    public void noteAbilityUsed(Ability ability) {
        if (ability != null) {
            cooldownRemaining.put(ability.getId(), ability.getCooldown());
        }
    }

    /**
     * Rebuild/refresh the buttons from the player's abilities and resource,
     * advancing local cooldowns by one tick.
     */
    public void update(Character player) {
        if (player == null) {
            return;
        }
        currentResource = player.getCurrentResource();

        // Advance cooldowns one tick.
        java.util.Iterator<Map.Entry<String, Integer>> it = cooldownRemaining.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            int left = entry.getValue() - 1;
            if (left <= 0) {
                it.remove();
            } else {
                entry.setValue(left);
            }
        }

        List<Ability> abilities = player.getAbilities();
        abilityRow.removeAll();
        if (abilities != null) {
            for (int i = 0; i < abilities.size(); i++) {
                abilityRow.add(new AbilityButton(abilities.get(i)));
            }
        }
        abilityRow.revalidate();
        abilityRow.repaint();
    }

    /** A single ability control with a painted cooldown sweep and cost badge. */
    private final class AbilityButton extends JButton {
        private final Ability ability;

        AbilityButton(final Ability ability) {
            this.ability = ability;
            setFocusable(false);
            setPreferredSize(new Dimension(112, 56));
            setToolTipText(buildTooltip());
            setText("<html><center>" + escape(ability.getName())
                    + "<br><font size='2'>cost " + ability.getResourceCost() + "</font></center></html>");
            setForeground(UiTheme.FG_TEXT);
            setBackground(UiTheme.BG_SLOT);

            boolean onCd = cooldownRemaining.containsKey(ability.getId());
            boolean affordable = currentResource >= ability.getResourceCost();
            setEnabled(!onCd && affordable);

            addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    sink.submit(UiActions.useAbility(ability.getId(), targetIndex));
                    // Optimistic local cooldown; corrected by the next snapshot refresh.
                    cooldownRemaining.put(ability.getId(), ability.getCooldown());
                }
            });
        }

        private String buildTooltip() {
            StringBuilder sb = new StringBuilder("<html>");
            sb.append("<b>").append(escape(ability.getName())).append("</b><br>");
            sb.append("Cost: ").append(ability.getResourceCost()).append("<br>");
            sb.append("Cooldown: ").append(ability.getCooldown()).append(" ticks<br>");
            if (ability.getEffectType() != null) {
                sb.append("Effect: ").append(escape(String.valueOf(ability.getEffectType())))
                        .append(" (").append(ability.getMagnitude()).append(")<br>");
            }
            if (ability.getTargetType() != null) {
                sb.append("Target: ").append(escape(String.valueOf(ability.getTargetType())));
            }
            sb.append("</html>");
            return sb.toString();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Integer left = cooldownRemaining.get(ability.getId());
            if (left == null || left <= 0) {
                return;
            }
            int total = Math.max(1, ability.getCooldown());
            double ratio = Math.max(0.0, Math.min(1.0, (double) left / (double) total));

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 150));
            int size = Math.min(getWidth(), getHeight()) - 8;
            int cx = (getWidth() - size) / 2;
            int cy = (getHeight() - size) / 2;
            // Clockwise sweep shrinking as the cooldown elapses.
            int angle = (int) Math.round(360 * ratio);
            g2.fillArc(cx, cy, size, size, 90, -angle);

            String label = String.valueOf(left);
            g2.setFont(UiTheme.BODY_BOLD);
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(label, (getWidth() - fm.stringWidth(label)) / 2,
                    (getHeight() + fm.getAscent()) / 2 - 2);
            g2.dispose();
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
