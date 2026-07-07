package com.whim.b5wars.ui;

import com.whim.b5wars.model.Ship;
import com.whim.b5wars.model.Weapon;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Pick a weapon and a target, preview the computed to-hit and whether the target is in arc/range,
 * then resolve the shot. Results are appended to the log via the controller (which fires the
 * {@code CombatEngine.fire} events to every {@link GameListener}).
 */
public final class WeaponFireDialog extends JDialog {

    private final GameController controller;
    private final Ship attacker;

    private final JComboBox<String> weaponCombo = new JComboBox<String>();
    private final JComboBox<String> targetCombo = new JComboBox<String>();
    private final JLabel info = new JLabel();
    private final JButton fireBtn = new JButton("Fire");

    private final List<Ship> targets = new ArrayList<Ship>();

    public WeaponFireDialog(Window owner, GameController controller, Ship attacker) {
        super(owner, "Weapon Fire", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.attacker = attacker;

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.setBackground(UiTheme.PANEL_BG);

        JLabel head = new JLabel(attacker == null ? "No attacker selected"
                : "Attacker: " + attacker.getType().getName() + " (Side " + attacker.getSide() + ")");
        head.setFont(UiTheme.FONT_HEADER);
        head.setForeground(UiTheme.TEXT);
        content.add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        form.add(label("Weapon"), c);
        c.gridx = 1;
        form.add(weaponCombo, c);
        c.gridx = 0;
        c.gridy = 1;
        form.add(label("Target"), c);
        c.gridx = 1;
        form.add(targetCombo, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        info.setForeground(UiTheme.TEXT);
        form.add(info, c);
        content.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        buttons.add(fireBtn);
        buttons.add(close);
        content.add(buttons, BorderLayout.SOUTH);

        setContentPane(content);
        populate();

        weaponCombo.addActionListener(e -> refreshInfo());
        targetCombo.addActionListener(e -> refreshInfo());
        fireBtn.addActionListener(e -> doFire());

        refreshInfo();
        setPreferredSize(new Dimension(440, 240));
        pack();
        if (owner != null) {
            setLocationRelativeTo(owner);
        }
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UiTheme.TEXT);
        return l;
    }

    private void populate() {
        DefaultComboBoxModel<String> wm = new DefaultComboBoxModel<String>();
        if (attacker != null) {
            for (Weapon w : attacker.getType().getWeapons()) {
                wm.addElement(w.getName());
            }
        }
        weaponCombo.setModel(wm);
        int sel = controller.selectedWeaponIndex();
        if (sel >= 0 && sel < wm.getSize()) {
            weaponCombo.setSelectedIndex(sel);
        }

        DefaultComboBoxModel<String> tm = new DefaultComboBoxModel<String>();
        targets.clear();
        if (attacker != null) {
            for (Ship s : controller.state().getShips()) {
                if (s.getSide() != attacker.getSide() && !s.isDestroyed()) {
                    targets.add(s);
                    tm.addElement(s.getType().getName() + " (Side " + s.getSide() + ")");
                }
            }
        }
        targetCombo.setModel(tm);
    }

    private Ship selectedTarget() {
        int i = targetCombo.getSelectedIndex();
        return (i >= 0 && i < targets.size()) ? targets.get(i) : null;
    }

    private void refreshInfo() {
        int wi = weaponCombo.getSelectedIndex();
        Ship target = selectedTarget();
        if (attacker == null || wi < 0 || target == null) {
            info.setText("<html><font color='#96a0b4'>No valid weapon/target.</font></html>");
            fireBtn.setEnabled(false);
            return;
        }
        boolean inAr = controller.combat().inArcAndRange(attacker, wi, target);
        int dist = attacker.getPos().distance(target.getPos());
        Weapon w = attacker.getType().getWeapons().get(wi);
        boolean reloading = attacker.getReloadReadyTurn(wi) > controller.state().getTurn();
        String toHit = inAr
                ? String.valueOf(controller.combat().toHitTarget(attacker, wi, target))
                : "—";
        String arcColor = inAr ? "#78dca0" : "#ff8080";
        info.setText("<html>"
                + "Range: <b>" + dist + "</b> (max " + maxRange(w) + ")<br>"
                + "In arc &amp; range: <font color='" + arcColor + "'><b>" + (inAr ? "YES" : "NO")
                + "</b></font><br>"
                + "Modified to-hit (d20 &ge;): <b>" + toHit + "</b>"
                + (reloading ? "<br><font color='#ffb040'>Weapon reloading until turn "
                        + attacker.getReloadReadyTurn(wi) + "</font>" : "")
                + "</html>");
        fireBtn.setEnabled(inAr && !reloading);
    }

    private static int maxRange(Weapon w) {
        int[] b = w.getRangeBrackets();
        return b.length == 0 ? 0 : b[b.length - 1];
    }

    private void doFire() {
        int wi = weaponCombo.getSelectedIndex();
        Ship target = selectedTarget();
        if (attacker == null || wi < 0 || target == null) {
            return;
        }
        controller.selectWeapon(wi);
        controller.fire(attacker, wi, target);
        refreshInfo();
        populate();      // targets may now be destroyed
    }
}
