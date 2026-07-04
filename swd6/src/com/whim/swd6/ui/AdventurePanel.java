package com.whim.swd6.ui;

import com.whim.swd6.api.Attribute;
import com.whim.swd6.api.Combatant;
import com.whim.swd6.api.CombatTracker;
import com.whim.swd6.api.DamageResult;
import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.RollResult;
import com.whim.swd6.api.Scenario;
import com.whim.swd6.api.Skill;
import com.whim.swd6.api.Weapon;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Adventure player: walks the {@link Scenario} graph from the content provider.
 * Narrative and decision scenes present choice buttons; skill-check scenes roll the
 * active character's skill against the target through the engine; combat scenes
 * auto-resolve a fight via the combat tracker. The graph is followed to an ending.
 * Requires an active character (offers to jump to Create).
 *
 * Owned by Task 3 (ui).
 */
public final class AdventurePanel extends HubPanel {

    private final JTextArea storyArea = Ui.logArea();
    private final JPanel choices = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    private final JTextArea rollLog = Ui.logArea();

    private Scenario scenario;
    private Scenario.Scene current;

    public AdventurePanel(AppContext ctx) {
        super(ctx);
        build();
    }

    private void build() {
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        head.setOpaque(false);
        head.add(Ui.title("Adventure"));
        JButton restart = Ui.ghost("Restart");
        restart.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { start(); }
        });
        head.add(restart);
        add(head, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));
        storyArea.setFont(Palette.BODY);
        storyArea.setPreferredSize(Ui.dim(640, 220));
        center.add(Ui.scroll(storyArea), BorderLayout.CENTER);
        choices.setOpaque(false);
        center.add(choices, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
        south.add(Ui.head("Dice & Combat Log"), BorderLayout.NORTH);
        rollLog.setPreferredSize(Ui.dim(640, 150));
        south.add(Ui.scroll(rollLog), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    @Override
    public void onShow() {
        if (ctx.character() == null) {
            requireCharacter();
            return;
        }
        if (scenario == null) {
            start();
        }
    }

    private void requireCharacter() {
        scenario = null;
        current = null;
        storyArea.setText("You need an active character to play the adventure.\n\n"
                + "Create one on the Create tab, or load a saved character, then return here.");
        choices.removeAll();
        JButton go = Ui.button("Go to Create");
        go.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { ctx.showCard("Create"); }
        });
        choices.add(go);
        choices.revalidate();
        choices.repaint();
    }

    private void start() {
        if (ctx.character() == null) {
            requireCharacter();
            return;
        }
        scenario = ctx.content().scenario();
        rollLog.setText("");
        current = scenario.sceneById(scenario.getStartSceneId());
        if (current == null && !scenario.getScenes().isEmpty()) {
            current = scenario.getScenes().get(0);
        }
        renderScene();
    }

    private void go(String id) {
        Scenario.Scene next = scenario.sceneById(id);
        if (next == null) {
            storyArea.append("\n\n(The trail ends here.)");
            choices.removeAll();
            choices.revalidate();
            choices.repaint();
            return;
        }
        current = next;
        renderScene();
    }

    private void renderScene() {
        if (current == null) {
            return;
        }
        StringBuilder b = new StringBuilder();
        b.append("« ").append(scenario.getTitle()).append(" »\n\n");
        b.append(current.getTitle().toUpperCase()).append("\n\n");
        b.append(current.getText());
        storyArea.setText(b.toString());
        storyArea.setCaretPosition(0);
        choices.removeAll();

        switch (current.getType()) {
            case NARRATIVE:
                addChoiceButtons();
                break;
            case DECISION:
                addChoiceButtons();
                break;
            case SKILL_CHECK:
                addSkillCheck();
                break;
            case COMBAT:
                addCombat();
                break;
            case ENDING:
                addEnding();
                break;
            default:
                addChoiceButtons();
                break;
        }
        choices.revalidate();
        choices.repaint();
    }

    private void addChoiceButtons() {
        if (current.getChoices().isEmpty()) {
            JButton cont = Ui.button("Continue");
            cont.setEnabled(false);
            choices.add(cont);
            return;
        }
        for (final Scenario.Choice ch : current.getChoices()) {
            JButton b = Ui.ghost(ch.getLabel());
            b.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) { go(ch.getNextSceneId()); }
            });
            choices.add(b);
        }
    }

    private void addSkillCheck() {
        final String skillName = current.getSkillName();
        final int tn = current.getTargetNumber();
        JButton roll = Ui.button("Roll " + skillName + " vs " + tn);
        roll.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { doSkillCheck(skillName, tn); }
        });
        choices.add(roll);
    }

    private void doSkillCheck(String skillName, int tn) {
        PlayerCharacter pc = ctx.character();
        DiceCode code = skillCodeFor(pc, skillName);
        RollResult r = ctx.engine().roll(code, true, tn);
        StringBuilder b = new StringBuilder();
        b.append("Skill check — ").append(skillName).append(" [").append(code).append("] vs ").append(tn).append("\n");
        b.append("  rolled ").append(r.getTotal());
        if (r.isComplication()) b.append("  (complication!)");
        if (r.isWildExploded()) b.append("  (wild 6!)");
        b.append(r.isSuccess() ? "  → SUCCESS\n" : "  → failure\n");
        rollLog.append(b.toString());
        rollLog.setCaretPosition(rollLog.getDocument().getLength());

        String nextId = r.isSuccess() ? current.getSuccessNext() : current.getFailureNext();
        choices.removeAll();
        final String target = nextId;
        JButton cont = Ui.ghost("Continue ▸");
        cont.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { go(target); }
        });
        choices.add(cont);
        choices.revalidate();
        choices.repaint();
    }

    private DiceCode skillCodeFor(PlayerCharacter pc, String skillName) {
        for (Skill s : pc.getSkills()) {
            if (s.getName().equalsIgnoreCase(skillName)) {
                return pc.skillCode(s);
            }
        }
        // untrained: fall back to the governing attribute if we can find it in the catalog
        for (com.whim.swd6.api.SkillDef def : ctx.content().skillCatalog()) {
            if (def.name().equalsIgnoreCase(skillName)) {
                return pc.getAttribute(def.attribute());
            }
        }
        return pc.getAttribute(Attribute.PERCEPTION);
    }

    private void addCombat() {
        JButton fight = Ui.button("Fight!");
        fight.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { doCombat(); }
        });
        choices.add(fight);
    }

    private void doCombat() {
        PlayerCharacter pc = ctx.character();
        CombatTracker t = ctx.newTracker();
        Combatant hero = new Combatant();
        hero.setName(pc.getName().isEmpty() ? "You" : pc.getName());
        hero.setPlayerCharacter(true);
        hero.setPc(pc);
        hero.setAttackCode(heroAttack(pc));
        hero.setDamageCode(heroDamage(pc));
        hero.setResistCode(pc.getAttribute(Attribute.STRENGTH));
        hero.setWoundLevel(pc.getWoundLevel());
        t.add(hero);
        List<Combatant> enemies = new ArrayList<Combatant>();
        for (Combatant e : current.getEnemies()) {
            Combatant copy = copyNpc(e);
            enemies.add(copy);
            t.add(copy);
        }
        t.rollInitiative();

        StringBuilder log = new StringBuilder();
        log.append("\n=== Combat: ").append(current.getTitle()).append(" ===\n");
        int guard = 0;
        while (!t.isOver() && guard < 60) {
            Combatant actor = t.current();
            if (actor == null) {
                break;
            }
            if (!actor.getWoundLevel().incapacitatedOrWorse()) {
                Combatant target = pickTarget(t, actor);
                if (target != null) {
                    strike(t, actor, target, log);
                }
            }
            t.next();
            guard++;
        }

        boolean victory = heroStanding(hero);
        log.append(victory ? "\nYou stand victorious.\n" : "\nYou fall in battle.\n");
        rollLog.append(log.toString());
        rollLog.setCaretPosition(rollLog.getDocument().getLength());

        final String target = victory ? current.getVictoryNext() : current.getDefeatNext();
        choices.removeAll();
        JButton cont = Ui.ghost("Continue ▸");
        cont.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { go(target); }
        });
        choices.add(cont);
        choices.revalidate();
        choices.repaint();
    }

    private void strike(CombatTracker t, Combatant atk, Combatant def, StringBuilder log) {
        DiceCode atkCode = ctx.engine().effectiveAttackCode(atk);
        List<RollResult> opp = ctx.engine().opposedRoll(atkCode, def.getResistCode());
        int hit = opp.get(0).getTotal();
        int dodge = opp.get(1).getTotal();
        if (hit >= dodge) {
            DamageResult dr = ctx.engine().resolveDamage(atk.getDamageCode(), def.getResistCode());
            com.whim.swd6.api.WoundLevel now = t.applyHit(def, dr);
            log.append("  ").append(atk.getName()).append(" hits ").append(def.getName())
                    .append(" (margin ").append(dr.getMargin()).append(") → ").append(now.display()).append("\n");
        } else {
            log.append("  ").append(atk.getName()).append(" misses ").append(def.getName()).append("\n");
        }
    }

    private Combatant pickTarget(CombatTracker t, Combatant actor) {
        for (Combatant c : t.order()) {
            if (c.isPlayerCharacter() != actor.isPlayerCharacter()
                    && !c.getWoundLevel().incapacitatedOrWorse()) {
                return c;
            }
        }
        return null;
    }

    private boolean heroStanding(Combatant hero) {
        return !hero.getWoundLevel().incapacitatedOrWorse();
    }

    private Combatant copyNpc(Combatant e) {
        Combatant c = new Combatant();
        c.setName(e.getName());
        c.setPlayerCharacter(false);
        c.setAttackCode(e.getAttackCode());
        c.setDamageCode(e.getDamageCode());
        c.setResistCode(e.getResistCode());
        return c;
    }

    private DiceCode heroAttack(PlayerCharacter pc) {
        DiceCode best = pc.getAttribute(Attribute.DEXTERITY);
        for (Skill s : pc.getSkills()) {
            if (s.getName().equalsIgnoreCase("blaster") || s.getName().equalsIgnoreCase("melee combat")) {
                DiceCode code = pc.skillCode(s);
                if (code.pipValue() > best.pipValue()) {
                    best = code;
                }
            }
        }
        return best;
    }

    private DiceCode heroDamage(PlayerCharacter pc) {
        if (!pc.getWeapons().isEmpty()) {
            Weapon w = pc.getWeapons().get(0);
            if (w.getDamage().pipValue() > 0) {
                return w.getDamage();
            }
        }
        return pc.getAttribute(Attribute.STRENGTH);
    }

    private void addEnding() {
        storyArea.append("\n\n— THE END —");
        JButton again = Ui.button("Play Again");
        again.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { start(); }
        });
        choices.add(again);
    }
}
