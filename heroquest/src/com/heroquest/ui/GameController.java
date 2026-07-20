package com.heroquest.ui;

import com.heroquest.QuestFactory;
import com.heroquest.logic.CombatEngine;
import com.heroquest.logic.Dice;
import com.heroquest.logic.TurnManager;
import com.heroquest.logic.ZargonAI;
import com.heroquest.model.Entity;
import com.heroquest.model.GameState;
import com.heroquest.model.Hero;
import com.heroquest.model.Monster;
import com.heroquest.model.Point;
import com.heroquest.model.Spell;
import com.heroquest.model.TileType;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Mediates between the Swing view and the logic layer. The UI never touches the
 * logic engines directly: it calls the controller, which mutates state and asks
 * the panels to refresh.
 */
public final class GameController {
    private final Dice dice;
    private final TurnManager turnManager;
    private final ZargonAI zargon;

    private GameState state;
    private boolean attackMode;
    private boolean gameOver;
    private Set<Point> reachableCache = Collections.emptySet();

    private BoardPanel boardPanel;
    private SidePanel sidePanel;
    private Component dialogParent;

    public GameController() {
        this.dice = new Dice();
        CombatEngine combat = new CombatEngine(dice);
        this.turnManager = new TurnManager(dice, combat);
        this.zargon = new ZargonAI(combat);
        this.state = QuestFactory.buildTrialQuest(dice);
        turnManager.beginHeroTurn(state);
        recomputeReachable();
    }

    public void attachView(BoardPanel board, SidePanel side, Component parent) {
        this.boardPanel = board;
        this.sidePanel = side;
        this.dialogParent = parent;
        refresh();
    }

    public GameState getState() {
        return state;
    }

    public boolean isAttackMode() {
        return attackMode;
    }

    public Set<Point> getReachable() {
        return reachableCache;
    }

    public boolean canActiveHeroAttack(Monster m) {
        Hero h = state.getActiveHero();
        return h != null && !state.isActionUsed() && m.isAlive()
                && h.getPosition().isOrthogonalNeighbour(m.getPosition());
    }

    // ------------------------------------------------------------------ input

    public void onTileClicked(Point p) {
        if (gameOver || state.getActiveHero() == null) {
            return;
        }
        if (attackMode) {
            Entity e = state.entityAt(p);
            if (e instanceof Monster && canActiveHeroAttack((Monster) e)) {
                turnManager.heroAttack(state, (Monster) e);
                attackMode = false;
                afterAction();
            }
            return;
        }

        // Movement / door mode.
        if (state.getMap().tileAt(p).getType() == TileType.DOOR_CLOSED) {
            if (turnManager.openDoor(state, p)) {
                recomputeReachable();
                refresh();
            }
            return;
        }
        if (reachableCache.contains(p) && !state.isOccupied(p)) {
            if (turnManager.moveActiveHero(state, p)) {
                recomputeReachable();
                refresh();
                checkEnd();
            }
        }
    }

    // ---------------------------------------------------------------- actions

    public void toggleAttackMode() {
        if (state.isActionUsed()) {
            message("This Hero has already used their action this turn.");
            return;
        }
        attackMode = !attackMode;
        refresh();
    }

    public void doSearch() {
        if (state.isActionUsed()) {
            message("This Hero has already used their action this turn.");
            return;
        }
        turnManager.searchTreasure(state);
        afterAction();
    }

    public void doCastSpell() {
        Hero hero = state.getActiveHero();
        if (hero == null) {
            return;
        }
        if (!hero.canCastSpells() || hero.getSpells().isEmpty()) {
            message(hero.getName() + " has no spells to cast.");
            return;
        }
        if (state.isActionUsed()) {
            message("This Hero has already used their action this turn.");
            return;
        }
        List<Spell> spells = hero.getSpells();
        Spell chosen = (Spell) JOptionPane.showInputDialog(dialogParent, "Choose a spell to cast:",
                "Cast Spell", JOptionPane.QUESTION_MESSAGE, null,
                spells.toArray(), spells.get(0));
        if (chosen == null) {
            return;
        }
        Entity target = null;
        if (chosen.getEffect() == Spell.Effect.DAMAGE) {
            target = pickSpellTargetMonster();
            if (target == null) {
                message("No visible monster in range to target.");
                return;
            }
        }
        turnManager.castSpell(state, chosen, target);
        afterAction();
    }

    private Entity pickSpellTargetMonster() {
        Monster best = null;
        int bestDist = Integer.MAX_VALUE;
        Hero h = state.getActiveHero();
        for (Monster m : state.getLivingMonsters()) {
            if (!state.getMap().tileAt(m.getPosition()).isRevealed()) {
                continue;
            }
            int d = h.getPosition().manhattan(m.getPosition());
            if (d < bestDist) {
                bestDist = d;
                best = m;
            }
        }
        return best;
    }

    public void endTurn() {
        if (gameOver) {
            return;
        }
        attackMode = false;
        boolean moreHeroes = turnManager.endHeroTurn(state);
        if (moreHeroes) {
            recomputeReachable();
            refresh();
            return;
        }
        // Hero phase complete -> Zargon acts, then a new round begins.
        zargon.runZargonPhase(state);
        refresh();
        if (checkEnd()) {
            return;
        }
        turnManager.startNewRound(state);
        recomputeReachable();
        refresh();
        checkEnd();
    }

    public void newGame() {
        this.state = QuestFactory.buildTrialQuest(dice);
        this.attackMode = false;
        this.gameOver = false;
        turnManager.beginHeroTurn(state);
        recomputeReachable();
        if (boardPanel != null) {
            boardPanel.setPreferredSizeToMap(state);
        }
        refresh();
    }

    // ------------------------------------------------------------------ util

    private void afterAction() {
        recomputeReachable();
        refresh();
        checkEnd();
    }

    private void recomputeReachable() {
        if (!attackMode && state.getActiveHero() != null) {
            reachableCache = turnManager.reachableSquares(state);
        } else {
            reachableCache = Collections.emptySet();
        }
    }

    private boolean checkEnd() {
        if (state.isVictory() && !gameOver) {
            gameOver = true;
            state.log("Victory! All monsters have been vanquished.");
            refresh();
            message("Victory! The Heroes have cleared the dungeon.");
            return true;
        }
        if (state.isDefeat() && !gameOver) {
            gameOver = true;
            state.log("Defeat. The Heroes have fallen to Zargon.");
            refresh();
            message("Defeat. Zargon's minions have slain the Heroes.");
            return true;
        }
        return false;
    }

    private void message(String text) {
        if (dialogParent != null) {
            JOptionPane.showMessageDialog(dialogParent, text);
        }
    }

    private void refresh() {
        if (sidePanel != null) {
            sidePanel.refresh();
        }
        if (boardPanel != null) {
            boardPanel.repaint();
        }
    }
}
