package com.whim.swd6.ui;

import com.whim.swd6.api.Attribute;
import com.whim.swd6.api.Combatant;
import com.whim.swd6.api.CombatTracker;
import com.whim.swd6.api.DamageResult;
import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.RollResult;
import com.whim.swd6.api.Skill;
import com.whim.swd6.api.Weapon;
import com.whim.swd6.api.WoundLevel;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Combat tracker: add the active PC and NPC adversaries, roll initiative, view the
 * ordered turn list with wound status, resolve attacks (to-hit opposed roll, then
 * {@link com.whim.swd6.api.RpgEngine#resolveDamage} and
 * {@link CombatTracker#applyHit}), advance turns, and spend Character / Force points
 * on the PC's strike.
 *
 * Owned by Task 3 (ui).
 */
public final class CombatTrackerPanel extends HubPanel {

    private CombatTracker tracker;

    private final JComboBox<CItem> attackerBox = new JComboBox<CItem>();
    private final JComboBox<CItem> defenderBox = new JComboBox<CItem>();
    private final JTextArea orderArea = mono();
    private final JTextArea log = Ui.logArea();

    private final JCheckBox useCp = new JCheckBox("+1D (Character Point)");
    private final JCheckBox useFp = new JCheckBox("Force Point (double attack)");

    private final JTextField npcName = new JTextField("Stormtrooper", 12);
    private final JComboBox<String> npcPreset = new JComboBox<String>(
            new String[]{"Thug (3D/4D/2D)", "Trooper (4D/5D/2D+2)", "Elite (4D+2/5D/3D)"});

    public CombatTrackerPanel(AppContext ctx) {
        super(ctx);
        build();
    }

    private static final class CItem {
        final Combatant c;
        CItem(Combatant c) { this.c = c; }
        @Override public String toString() { return c.getName(); }
    }

    private JTextArea mono() {
        JTextArea a = Ui.logArea();
        a.setFont(Palette.MONO);
        return a;
    }

    private void build() {
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        head.setOpaque(false);
        head.add(Ui.title("Combat Tracker"));
        JButton reset = Ui.ghost("New Encounter");
        reset.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { newEncounter(); }
        });
        head.add(reset);
        add(head, BorderLayout.NORTH);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new javax.swing.BoxLayout(left, javax.swing.BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 6));

        JPanel addPc = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        addPc.setOpaque(false);
        JButton addPcBtn = Ui.button("Add Active PC");
        addPcBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { addActivePc(); }
        });
        addPc.add(addPcBtn);
        left.add(addPc);

        JPanel addNpc = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        addNpc.setOpaque(false);
        addNpc.add(Ui.dim("NPC:"));
        addNpc.add(npcName);
        addNpc.add(npcPreset);
        JButton addNpcBtn = Ui.ghost("Add NPC");
        addNpcBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { addNpc(); }
        });
        addNpc.add(addNpcBtn);
        left.add(addNpc);

        JPanel initRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        initRow.setOpaque(false);
        JButton initBtn = Ui.button("Roll Initiative");
        initBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { rollInit(); }
        });
        JButton nextBtn = Ui.ghost("Next Turn ▸");
        nextBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { nextTurn(); }
        });
        initRow.add(initBtn);
        initRow.add(nextBtn);
        left.add(initRow);

        JPanel atkRow = new JPanel(new GridLayout(0, 1, 2, 2));
        atkRow.setOpaque(false);
        JPanel a1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 1));
        a1.setOpaque(false);
        a1.add(Ui.dim("Attacker:"));
        a1.add(attackerBox);
        a1.add(Ui.dim("Defender:"));
        a1.add(defenderBox);
        atkRow.add(a1);
        JPanel a2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 1));
        a2.setOpaque(false);
        styleCheck(useCp);
        styleCheck(useFp);
        a2.add(useCp);
        a2.add(useFp);
        JButton attackBtn = Ui.button("Resolve Attack");
        attackBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { resolveAttack(); }
        });
        a2.add(attackBtn);
        atkRow.add(a2);
        left.add(atkRow);

        left.add(Ui.head("Turn Order"));
        orderArea.setPreferredSize(Ui.dim(360, 150));
        left.add(Ui.scroll(orderArea));

        add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 12));
        right.add(Ui.head("Combat Log"), BorderLayout.NORTH);
        right.add(Ui.scroll(log), BorderLayout.CENTER);
        add(right, BorderLayout.CENTER);

        newEncounter();
    }

    private void styleCheck(JCheckBox cb) {
        cb.setOpaque(false);
        cb.setForeground(Palette.TEXT_DIM);
        cb.setFont(Palette.SMALL);
    }

    private void newEncounter() {
        tracker = ctx.newTracker();
        log.setText("New encounter. Add combatants, then roll initiative.\n");
        refreshOrder();
        refreshBoxes();
    }

    @Override
    public void onShow() {
        refreshBoxes();
    }

    private void addActivePc() {
        PlayerCharacter pc = ctx.character();
        if (pc == null) {
            append("No active character — create or load one first.\n");
            return;
        }
        Combatant c = new Combatant();
        c.setName(pc.getName().isEmpty() ? "Player" : pc.getName());
        c.setPlayerCharacter(true);
        c.setPc(pc);
        c.setAttackCode(deriveAttack(pc));
        c.setDamageCode(deriveDamage(pc));
        c.setResistCode(pc.getAttribute(Attribute.STRENGTH));
        c.setWoundLevel(pc.getWoundLevel());
        tracker.add(c);
        append("Added " + c.getName() + "  (atk " + c.getAttackCode()
                + ", dmg " + c.getDamageCode() + ", resist " + c.getResistCode() + ")\n");
        refreshOrder();
        refreshBoxes();
    }

    private DiceCode deriveAttack(PlayerCharacter pc) {
        // best of: a weapon's governing skill code, else 'blaster'/'melee combat', else DEX
        String skillName = null;
        if (!pc.getWeapons().isEmpty()) {
            skillName = pc.getWeapons().get(0).getSkill();
        }
        DiceCode best = pc.getAttribute(Attribute.DEXTERITY);
        for (Skill s : pc.getSkills()) {
            if (skillName != null && s.getName().equalsIgnoreCase(skillName)) {
                return pc.skillCode(s);
            }
            if (s.getName().equalsIgnoreCase("blaster") || s.getName().equalsIgnoreCase("melee combat")) {
                DiceCode code = pc.skillCode(s);
                if (code.pipValue() > best.pipValue()) {
                    best = code;
                }
            }
        }
        return best;
    }

    private DiceCode deriveDamage(PlayerCharacter pc) {
        if (!pc.getWeapons().isEmpty()) {
            Weapon w = pc.getWeapons().get(0);
            if (w.getDamage().pipValue() > 0) {
                return w.getDamage();
            }
        }
        // unarmed: Strength damage
        return pc.getAttribute(Attribute.STRENGTH);
    }

    private void addNpc() {
        Combatant c = new Combatant();
        String nm = npcName.getText().trim();
        c.setName(nm.isEmpty() ? "NPC" : nm);
        c.setPlayerCharacter(false);
        switch (npcPreset.getSelectedIndex()) {
            case 1:
                c.setAttackCode(DiceCode.parse("4D"));
                c.setDamageCode(DiceCode.parse("5D"));
                c.setResistCode(DiceCode.parse("2D+2"));
                break;
            case 2:
                c.setAttackCode(DiceCode.parse("4D+2"));
                c.setDamageCode(DiceCode.parse("5D"));
                c.setResistCode(DiceCode.parse("3D"));
                break;
            default:
                c.setAttackCode(DiceCode.parse("3D"));
                c.setDamageCode(DiceCode.parse("4D"));
                c.setResistCode(DiceCode.parse("2D"));
                break;
        }
        tracker.add(c);
        append("Added " + c.getName() + "  (atk " + c.getAttackCode()
                + ", dmg " + c.getDamageCode() + ", resist " + c.getResistCode() + ")\n");
        refreshOrder();
        refreshBoxes();
    }

    private void rollInit() {
        if (tracker.order().isEmpty()) {
            append("Add at least one combatant first.\n");
            return;
        }
        tracker.rollInitiative();
        append("\n--- Initiative rolled (round " + tracker.round() + ") ---\n");
        refreshOrder();
        refreshBoxes();
    }

    private void nextTurn() {
        if (tracker.order().isEmpty()) {
            return;
        }
        tracker.next();
        Combatant cur = tracker.current();
        if (tracker.isOver()) {
            append("The encounter is over.\n");
        } else if (cur != null) {
            append("It is now " + cur.getName() + "'s turn (round " + tracker.round() + ").\n");
        }
        refreshOrder();
    }

    private void resolveAttack() {
        CItem ai = (CItem) attackerBox.getSelectedItem();
        CItem di = (CItem) defenderBox.getSelectedItem();
        if (ai == null || di == null || ai.c == di.c) {
            append("Pick a distinct attacker and defender.\n");
            return;
        }
        Combatant atk = ai.c;
        Combatant def = di.c;
        if (atk.getWoundLevel().incapacitatedOrWorse()) {
            append(atk.getName() + " can't act (" + atk.getWoundLevel().display() + ").\n");
            return;
        }

        DiceCode atkCode = ctx.engine().effectiveAttackCode(atk);
        boolean pcAttacker = atk.isPlayerCharacter() && atk.getPc() != null;
        PlayerCharacter pc = atk.getPc();
        boolean spentFp = false;
        int spentCp = 0;
        if (pcAttacker && useCp.isSelected() && pc.getCharacterPoints() > 0) {
            atkCode = atkCode.addDice(1);
            spentCp = 1;
        }
        if (pcAttacker && useFp.isSelected() && pc.getForcePoints() > 0) {
            atkCode = atkCode.doubled();
            spentFp = true;
        }

        // to-hit: opposed vs defender's reaction (use resist code as a stand-in reaction pool)
        DiceCode reaction = def.getResistCode();
        List<RollResult> opp = ctx.engine().opposedRoll(atkCode, reaction);
        RollResult hit = opp.get(0);
        RollResult dodge = opp.get(1);
        StringBuilder sb = new StringBuilder();
        sb.append(atk.getName()).append(" attacks ").append(def.getName())
                .append("  [").append(atkCode).append("]: ").append(hit.getTotal())
                .append(hit.isComplication() ? " (complication!)" : "")
                .append(hit.isWildExploded() ? " (wild 6!)" : "")
                .append("  vs reaction ").append(dodge.getTotal()).append("\n");

        if (hit.getTotal() >= dodge.getTotal()) {
            DamageResult dr = ctx.engine().resolveDamage(atk.getDamageCode(), def.getResistCode());
            sb.append("  HIT! damage ").append(dr.getDamageRoll().getTotal())
                    .append(" vs resist ").append(dr.getResistRoll().getTotal())
                    .append("  → margin ").append(dr.getMargin())
                    .append(" (").append(dr.getInflicted().display()).append(")\n");
            WoundLevel now = tracker.applyHit(def, dr);
            sb.append("  ").append(def.getName()).append(" is now ").append(now.display()).append(".\n");
            if (tracker.isOver()) {
                sb.append("  *** The fight is decided. ***\n");
            }
        } else {
            sb.append("  Miss — the attack is evaded.\n");
        }

        if (spentCp > 0) {
            pc.setCharacterPoints(Math.max(0, pc.getCharacterPoints() - spentCp));
            sb.append("  (spent ").append(spentCp).append(" Character Point)\n");
            useCp.setSelected(false);
        }
        if (spentFp) {
            pc.setForcePoints(Math.max(0, pc.getForcePoints() - 1));
            sb.append("  (spent 1 Force Point)\n");
            useFp.setSelected(false);
        }
        append(sb.toString());
        refreshOrder();
    }

    private void refreshBoxes() {
        DefaultComboBoxModel<CItem> am = new DefaultComboBoxModel<CItem>();
        DefaultComboBoxModel<CItem> dm = new DefaultComboBoxModel<CItem>();
        if (tracker != null) {
            for (Combatant c : tracker.order()) {
                am.addElement(new CItem(c));
                dm.addElement(new CItem(c));
            }
        }
        attackerBox.setModel(am);
        defenderBox.setModel(dm);
        if (dm.getSize() > 1) {
            defenderBox.setSelectedIndex(1);
        }
    }

    private void refreshOrder() {
        StringBuilder b = new StringBuilder();
        if (tracker == null || tracker.order().isEmpty()) {
            orderArea.setText("(no combatants)");
            return;
        }
        Combatant cur = tracker.current();
        int i = 1;
        for (Combatant c : tracker.order()) {
            boolean isCur = c == cur;
            b.append(isCur ? "▶ " : "  ");
            b.append(i++).append(". ");
            b.append(pad(c.getName(), 16));
            b.append(" init ").append(pad2(c.getInitiative()));
            b.append("  ").append(c.getWoundLevel().display());
            b.append(c.isPlayerCharacter() ? "  [PC]" : "");
            b.append("\n");
        }
        b.append("\nRound ").append(tracker.round());
        if (tracker.isOver()) {
            b.append("  — OVER");
        }
        orderArea.setText(b.toString());
    }

    private void append(String s) {
        log.append(s);
        log.setCaretPosition(log.getDocument().getLength());
    }

    private String pad(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        StringBuilder b = new StringBuilder(s);
        while (b.length() < n) b.append(' ');
        return b.toString();
    }

    private String pad2(int v) {
        String s = String.valueOf(v);
        return s.length() < 2 ? " " + s : s;
    }
}
