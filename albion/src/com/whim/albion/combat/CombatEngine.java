package com.whim.albion.combat;

import com.whim.albion.api.ActionResult;
import com.whim.albion.api.Combatant;
import com.whim.albion.api.Content;
import com.whim.albion.api.Defs.MonsterDef;
import com.whim.albion.api.Defs.SpellDef;
import com.whim.albion.api.GameModel;
import com.whim.albion.api.GridPos;
import com.whim.albion.api.Enums.CombatActionType;
import com.whim.albion.api.Enums.EnemyBehaviorType;
import com.whim.albion.api.Enums.SkillType;
import com.whim.albion.api.Enums.SpellEffectType;
import com.whim.albion.api.Enums.StatType;
import com.whim.albion.api.Enums.TargetType;
import com.whim.albion.api.Views.CombatView;
import com.whim.albion.api.Views.CombatantView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Turn-based tactical grid combat (6 columns x 5 rows). Party occupies the bottom
 * rows, enemies the top. Initiative is recomputed each round by {@link StatType#SPEED}.
 * The engine auto-resolves enemy turns and pauses on each living player-side
 * combatant awaiting {@link #playerAction}. Implements {@link CombatView} directly.
 *
 * <h3>Formulas (see docs/task2-notes.md)</h3>
 * <ul>
 *   <li><b>hit%</b> = clamp(55 + skill/2 + (attDEX - defDEX)*2, 5, 95), where skill is
 *       RANGED or MELEE (falls back to DEX*3 when a combatant has no skill).</li>
 *   <li><b>crit%</b> = clamp(CRITICAL/2 + LUCK, 0, 60); a crit doubles raw damage.</li>
 *   <li><b>raw damage</b> = max(1, attackPower + max(0,(STR-10)/2) + rand(0..2)).
 *       Final loss is computed by {@link Combatant#takeDamage} (defense + defending
 *       mitigation), so the engine never subtracts defense itself.</li>
 *   <li><b>spell</b> = magnitude + MAGIC_TALENT/4 (damage routed through takeDamage,
 *       heal through heal()).</li>
 *   <li><b>flee%</b> = clamp(40 + SPEED*2, 5, 95).</li>
 * </ul>
 */
public final class CombatEngine implements CombatView {

    private static final int COLS = 6;
    private static final int ROWS = 5;
    private static final int MELEE_REACH = 1;
    private static final int SUPPORT_MEND = 6;
    private static final int LOG_CAP = 60;

    private final GameModel model;
    private final Random rng;

    private final List<Combatant> party;    // same instances as party.asCombatants()
    private final List<Combatant> enemies;
    private final List<Combatant> all;       // party then enemies (combatants() order)

    private final List<String> log = new ArrayList<String>();

    private List<Combatant> roundOrder = new ArrayList<Combatant>();
    private int roundPointer;
    private int roundNumber;
    private Combatant current;

    private boolean finished;
    private boolean victory;

    private int totalXp;
    private int totalGold;
    private final List<String> loot = new ArrayList<String>();

    public CombatEngine(GameModel model, List<Combatant> enemies, Random rng) {
        this.model = model;
        this.rng = rng;
        this.party = model.party().asCombatants();
        this.enemies = new ArrayList<Combatant>(enemies);
        this.all = new ArrayList<Combatant>();
        this.all.addAll(party);
        this.all.addAll(this.enemies);
        placeCombatants();
        log("Battle begins!");
        startRound();
        advanceToPlayer();
    }

    // ------------------------------------------------------------ setup

    private void placeCombatants() {
        for (int i = 0; i < party.size(); i++) {
            int x = 1 + (i % 4);
            int y = 4 - (i / 4);
            party.get(i).setPos(new GridPos(clampX(x), clampY(y)));
        }
        for (int i = 0; i < enemies.size(); i++) {
            int x = 1 + (i % 4);
            int y = 0 + (i / 4);
            enemies.get(i).setPos(new GridPos(clampX(x), clampY(y)));
        }
    }

    private int clampX(int x) { return Math.max(0, Math.min(COLS - 1, x)); }
    private int clampY(int y) { return Math.max(0, Math.min(ROWS - 1, y)); }

    // ------------------------------------------------------------ turn order

    private void startRound() {
        roundNumber++;
        List<Combatant> alive = new ArrayList<Combatant>();
        for (int i = 0; i < all.size(); i++) if (all.get(i).alive()) alive.add(all.get(i));
        Collections.sort(alive, new Comparator<Combatant>() {
            @Override public int compare(Combatant a, Combatant b) {
                int sa = a.stat(StatType.SPEED), sb = b.stat(StatType.SPEED);
                if (sa != sb) return sb - sa;                       // faster first
                if (a.playerSide() != b.playerSide()) return a.playerSide() ? -1 : 1;
                return all.indexOf(a) - all.indexOf(b);
            }
        });
        roundOrder = alive;
        roundPointer = 0;
    }

    /** Next living actor this round, rolling into a fresh round when exhausted. */
    private Combatant nextActor() {
        while (roundPointer < roundOrder.size()) {
            Combatant c = roundOrder.get(roundPointer++);
            if (c.alive()) return c;
        }
        startRound();
        while (roundPointer < roundOrder.size()) {
            Combatant c = roundOrder.get(roundPointer++);
            if (c.alive()) return c;
        }
        return null;
    }

    /** Process turns until a living player combatant is up or combat ends. */
    private void advanceToPlayer() {
        while (true) {
            if (checkEnd()) { current = null; return; }
            Combatant actor = nextActor();
            if (actor == null) { checkEnd(); current = null; return; }
            current = actor;
            actor.setDefending(false);           // defend lasts until own next turn
            if (actor.playerSide()) return;      // await player input
            runEnemyTurn(actor);
            if (checkEnd()) { current = null; return; }
        }
    }

    private boolean checkEnd() {
        if (finished) return true;
        boolean anyEnemy = false, anyPlayer = false;
        for (int i = 0; i < enemies.size(); i++) if (enemies.get(i).alive()) { anyEnemy = true; break; }
        for (int i = 0; i < party.size(); i++) if (party.get(i).alive()) { anyPlayer = true; break; }
        if (!anyEnemy) { finished = true; victory = true; computeRewards(); log("Victory!"); return true; }
        if (!anyPlayer) { finished = true; victory = false; log("The party has fallen."); return true; }
        return false;
    }

    // ------------------------------------------------------------ player actions

    /** Resolve the current player combatant's chosen action. */
    public ActionResult playerAction(CombatActionType type, int targetIndex, String optionId) {
        if (finished) return ActionResult.fail("Combat is over.");
        if (current == null || !current.playerSide() || !current.alive())
            return ActionResult.fail("Not your turn.");
        Combatant actor = current;
        switch (type) {
            case ATTACK: {
                Combatant target = enemyTarget(targetIndex);
                if (target == null) return ActionResult.fail("Invalid target.");
                if (!actor.ranged() && actor.pos().manhattan(target.pos()) > MELEE_REACH)
                    return ActionResult.fail(target.name() + " is out of reach.");
                resolveAttack(actor, target);
                endTurn();
                return ActionResult.ok();
            }
            case CAST: {
                SpellDef spell = spellById(actor, optionId);
                if (spell == null) return ActionResult.fail("Unknown spell.");
                if (actor.sp() < spell.spCost) return ActionResult.fail("Not enough SP.");
                if (!actor.spendSp(spell.spCost)) return ActionResult.fail("Not enough SP.");
                castSpell(actor, spell, targetIndex);
                endTurn();
                return ActionResult.ok();
            }
            case ITEM: {
                int mi = party.indexOf(actor);
                if (mi < 0 || optionId == null) return ActionResult.fail("No item.");
                ActionResult r = model.party().useItem(mi, optionId);
                log(actor.name() + " uses " + optionId + (r.message().isEmpty() ? "." : ": " + r.message()));
                if (r.isSuccess()) endTurn();
                return r;
            }
            case MOVE: {
                int cx = targetIndex % COLS, cy = targetIndex / COLS;
                if (targetIndex < 0 || cx < 0 || cx >= COLS || cy < 0 || cy >= ROWS)
                    return ActionResult.fail("Off the battlefield.");
                if (occupantAt(cx, cy) != null) return ActionResult.fail("Cell occupied.");
                actor.setPos(new GridPos(cx, cy));
                log(actor.name() + " repositions.");
                endTurn();
                return ActionResult.ok();
            }
            case DEFEND: {
                actor.setDefending(true);
                log(actor.name() + " defends.");
                endTurn();
                return ActionResult.ok();
            }
            case FLEE: {
                int chance = clamp(40 + actor.stat(StatType.SPEED) * 2, 5, 95);
                if (rng.nextInt(100) < chance) {
                    finished = true; victory = false;
                    log("The party flees the battle.");
                    return ActionResult.ok("Fled.");
                }
                log(actor.name() + " fails to flee.");
                endTurn();
                return ActionResult.ok("Could not flee.");
            }
            default:
                return ActionResult.fail("Unsupported action.");
        }
    }

    private void endTurn() { advanceToPlayer(); }

    // ------------------------------------------------------------ resolution

    private void resolveAttack(Combatant att, Combatant def) {
        int hit = hitChance(att, def);
        if (rng.nextInt(100) >= hit) { log(att.name() + " misses " + def.name() + "."); return; }
        boolean crit = rng.nextInt(100) < critChance(att);
        int raw = rawDamage(att);
        if (crit) raw *= 2;
        int lost = def.takeDamage(raw);
        log(att.name() + (crit ? " crits " : " hits ") + def.name() + " for " + lost + ".");
        if (!def.alive()) log(def.name() + " is defeated.");
    }

    private int hitChance(Combatant att, Combatant def) {
        int skill = att.ranged() ? att.skill(SkillType.RANGED) : att.skill(SkillType.MELEE);
        if (skill <= 0) skill = att.stat(StatType.DEXTERITY) * 3;
        int dexDiff = att.stat(StatType.DEXTERITY) - def.stat(StatType.DEXTERITY);
        return clamp(55 + skill / 2 + dexDiff * 2, 5, 95);
    }

    private int critChance(Combatant att) {
        return clamp(att.skill(SkillType.CRITICAL) / 2 + att.stat(StatType.LUCK), 0, 60);
    }

    private int rawDamage(Combatant att) {
        int strMod = Math.max(0, (att.stat(StatType.STRENGTH) - 10) / 2);
        return Math.max(1, att.attackPower() + strMod + rng.nextInt(3));
    }

    private void castSpell(Combatant caster, SpellDef spell, int targetIndex) {
        int talentMod = caster.stat(StatType.MAGIC_TALENT) / 4;
        List<Combatant> targets = resolveTargets(caster, spell.target, targetIndex);
        SpellEffectType effect = spell.effect;
        if (effect == SpellEffectType.DAMAGE || effect == SpellEffectType.DEBUFF) {
            int base = spell.magnitude + talentMod;
            if (effect == SpellEffectType.DEBUFF) base = Math.max(1, base / 2);
            for (int i = 0; i < targets.size(); i++) {
                Combatant t = targets.get(i);
                int lost = t.takeDamage(Math.max(1, base));
                log(caster.name() + " casts " + spell.name + " on " + t.name() + " (" + lost + ").");
                if (!t.alive()) log(t.name() + " is defeated.");
            }
        } else if (effect == SpellEffectType.HEAL) {
            for (int i = 0; i < targets.size(); i++) {
                Combatant t = targets.get(i);
                int gained = t.heal(spell.magnitude + talentMod);
                log(caster.name() + " heals " + t.name() + " for " + gained + ".");
            }
        } else if (effect == SpellEffectType.BUFF) {
            for (int i = 0; i < targets.size(); i++) {
                targets.get(i).setDefending(true);
                log(caster.name() + " wards " + targets.get(i).name() + ".");
            }
        } else { // UTILITY
            log(caster.name() + " casts " + spell.name + ".");
        }
    }

    /** Resolve targets relative to the caster's side. */
    private List<Combatant> resolveTargets(Combatant caster, TargetType tt, int targetIndex) {
        List<Combatant> out = new ArrayList<Combatant>();
        boolean casterPlayer = caster.playerSide();
        switch (tt) {
            case SELF:
                out.add(caster); break;
            case SINGLE_ALLY: {
                Combatant t = combatantAt(targetIndex);
                if (t != null && t.alive() && t.playerSide() == casterPlayer) out.add(t);
                else { Combatant f = lowestLp(casterPlayer); if (f != null) out.add(f); }
                break;
            }
            case SINGLE_ENEMY: {
                Combatant t = combatantAt(targetIndex);
                if (t != null && t.alive() && t.playerSide() != casterPlayer) out.add(t);
                else { Combatant f = lowestLp(!casterPlayer); if (f != null) out.add(f); }
                break;
            }
            case ALL_ALLIES:
                for (int i = 0; i < all.size(); i++)
                    if (all.get(i).alive() && all.get(i).playerSide() == casterPlayer) out.add(all.get(i));
                break;
            case ALL_ENEMIES:
                for (int i = 0; i < all.size(); i++)
                    if (all.get(i).alive() && all.get(i).playerSide() != casterPlayer) out.add(all.get(i));
                break;
            default: break;
        }
        return out;
    }

    // ------------------------------------------------------------ enemy AI

    private void runEnemyTurn(Combatant enemy) {
        EnemyBehaviorType b = enemy.behavior();
        if (b == null) b = EnemyBehaviorType.AGGRESSIVE;
        switch (b) {
            case RANGED: {
                Combatant target = lowestLp(true);
                if (target != null) resolveAttack(enemy, target);
                else log(enemy.name() + " waits.");
                break;
            }
            case SUPPORT: {
                Combatant ally = mostWounded(false);
                if (ally != null && ally.lp() < ally.maxLp()) {
                    SpellDef heal = firstEffect(enemy, SpellEffectType.HEAL);
                    if (heal != null && enemy.sp() >= heal.spCost && enemy.spendSp(heal.spCost)) {
                        int g = ally.heal(heal.magnitude + enemy.stat(StatType.MAGIC_TALENT) / 4);
                        log(enemy.name() + " mends " + ally.name() + " (" + g + ").");
                    } else {
                        int g = ally.heal(SUPPORT_MEND);
                        log(enemy.name() + " tends " + ally.name() + " (" + g + ").");
                    }
                } else {
                    aggress(enemy);
                }
                break;
            }
            case AGGRESSIVE:
            default:
                aggress(enemy);
                break;
        }
    }

    private void aggress(Combatant enemy) {
        Combatant target = mostWounded(true);
        if (target == null) { log(enemy.name() + " waits."); return; }
        if (enemy.ranged() || enemy.pos().manhattan(target.pos()) <= MELEE_REACH) {
            resolveAttack(enemy, target);
            return;
        }
        stepToward(enemy, target);
        if (enemy.pos().manhattan(target.pos()) <= MELEE_REACH) resolveAttack(enemy, target);
        else log(enemy.name() + " advances.");
    }

    private void stepToward(Combatant mover, Combatant target) {
        int mx = mover.pos().x(), my = mover.pos().y();
        int sx = Integer.compare(target.pos().x(), mx);
        int sy = Integer.compare(target.pos().y(), my);
        if (sx != 0 && cellFree(mx + sx, my)) { mover.setPos(new GridPos(mx + sx, my)); return; }
        if (sy != 0 && cellFree(mx, my + sy)) { mover.setPos(new GridPos(mx, my + sy)); return; }
    }

    private boolean cellFree(int x, int y) {
        return x >= 0 && x < COLS && y >= 0 && y < ROWS && occupantAt(x, y) == null;
    }

    private Combatant occupantAt(int x, int y) {
        for (int i = 0; i < all.size(); i++) {
            Combatant c = all.get(i);
            if (c.alive() && c.pos() != null && c.pos().x() == x && c.pos().y() == y) return c;
        }
        return null;
    }

    // ------------------------------------------------------------ selection helpers

    /** Lowest-LP living combatant on the requested side. */
    private Combatant lowestLp(boolean playerSide) {
        Combatant best = null;
        for (int i = 0; i < all.size(); i++) {
            Combatant c = all.get(i);
            if (c.alive() && c.playerSide() == playerSide && (best == null || c.lp() < best.lp())) best = c;
        }
        return best;
    }

    /** Most-wounded (largest missing-LP) living combatant on the requested side. */
    private Combatant mostWounded(boolean playerSide) {
        Combatant best = null; int bestMissing = -1;
        for (int i = 0; i < all.size(); i++) {
            Combatant c = all.get(i);
            if (!c.alive() || c.playerSide() != playerSide) continue;
            int missing = c.maxLp() - c.lp();
            if (best == null || missing > bestMissing || (missing == bestMissing && c.lp() < best.lp())) {
                best = c; bestMissing = missing;
            }
        }
        // For attackers we want the weakest (lowest LP), not the most-wounded; when
        // targeting the player side prefer lowest LP so kills land faster.
        if (playerSide) return lowestLp(true);
        return best;
    }

    private Combatant enemyTarget(int index) {
        Combatant t = combatantAt(index);
        if (t != null && t.alive() && !t.playerSide()) return t;
        return null;
    }

    private Combatant combatantAt(int index) {
        return (index >= 0 && index < all.size()) ? all.get(index) : null;
    }

    private SpellDef spellById(Combatant c, String id) {
        if (id == null) return null;
        List<SpellDef> spells = c.knownSpells();
        for (int i = 0; i < spells.size(); i++) if (id.equals(spells.get(i).id)) return spells.get(i);
        return null;
    }

    private SpellDef firstEffect(Combatant c, SpellEffectType effect) {
        List<SpellDef> spells = c.knownSpells();
        for (int i = 0; i < spells.size(); i++) if (spells.get(i).effect == effect) return spells.get(i);
        return null;
    }

    // ------------------------------------------------------------ rewards

    private void computeRewards() {
        for (int i = 0; i < enemies.size(); i++) {
            Combatant e = enemies.get(i);
            // Task 1 convention: enemy id() is "<monsterId>#<index>"; strip the suffix
            // before resolving the reward-bearing MonsterDef.
            String baseId = e.id();
            if (baseId != null) { int h = baseId.indexOf('#'); if (h >= 0) baseId = baseId.substring(0, h); }
            MonsterDef def = null;
            try { def = model.content().monster(baseId); } catch (RuntimeException ignore) { def = null; }
            if (def != null) {
                totalXp += def.xpReward;
                totalGold += def.goldReward;
            } else {
                // api exposes no reward on Combatant; derive from combat weight.
                totalXp += e.maxLp() + e.attackPower() * 2;
                totalGold += Math.max(0, e.maxLp() / 2);
            }
        }
    }

    public int totalXp() { return totalXp; }
    public int totalGold() { return totalGold; }
    public List<String> loot() { return loot; }

    // ------------------------------------------------------------ misc

    private void log(String s) {
        log.add(s);
        while (log.size() > LOG_CAP) log.remove(0);
    }

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

    // ------------------------------------------------------------ CombatView

    @Override public int cols() { return COLS; }
    @Override public int rows() { return ROWS; }

    @Override public List<CombatantView> combatants() {
        List<CombatantView> out = new ArrayList<CombatantView>(all.size());
        for (int i = 0; i < all.size(); i++) out.add(new CombatantViewImpl(all.get(i)));
        return out;
    }

    @Override public int currentTurnIndex() {
        return current == null ? -1 : all.indexOf(current);
    }

    @Override public List<CombatActionType> availableActions() {
        List<CombatActionType> out = new ArrayList<CombatActionType>();
        if (finished || current == null || !current.playerSide() || !current.alive()) return out;
        out.add(CombatActionType.ATTACK);
        if (!current.knownSpells().isEmpty() && current.sp() > 0) out.add(CombatActionType.CAST);
        out.add(CombatActionType.ITEM);
        out.add(CombatActionType.MOVE);
        out.add(CombatActionType.DEFEND);
        out.add(CombatActionType.FLEE);
        return out;
    }

    @Override public List<String> log() { return new ArrayList<String>(log); }
    @Override public boolean finished() { return finished; }
    @Override public boolean victory() { return victory; }

    // ------------------------------------------------------------ combatant view

    private final class CombatantViewImpl implements CombatantView {
        private final Combatant c;
        CombatantViewImpl(Combatant c) { this.c = c; }
        @Override public String name() { return c.name(); }
        @Override public boolean playerSide() { return c.playerSide(); }
        @Override public int gridX() { return c.pos() == null ? 0 : c.pos().x(); }
        @Override public int gridY() { return c.pos() == null ? 0 : c.pos().y(); }
        @Override public int lp() { return c.lp(); }
        @Override public int maxLp() { return c.maxLp(); }
        @Override public int sp() { return c.sp(); }
        @Override public int maxSp() { return c.maxSp(); }
        @Override public boolean alive() { return c.alive(); }
        @Override public boolean current() { return c == current; }
        @Override public String spriteKey() { return c.spriteKey(); }
    }
}
