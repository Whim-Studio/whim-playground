package com.heroquest.logic;

import com.heroquest.model.Decks;
import com.heroquest.model.Entity;
import com.heroquest.model.GameState;
import com.heroquest.model.Hero;
import com.heroquest.model.Monster;
import com.heroquest.model.MonsterType;
import com.heroquest.model.Phase;
import com.heroquest.model.Point;
import com.heroquest.model.Spell;
import com.heroquest.model.Tile;
import com.heroquest.model.TileType;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Drives the turn-resolution state machine for the Hero phase: rolling movement,
 * moving, opening doors, and performing the single allowed action (Attack,
 * Search, Cast). The Zargon phase is delegated to {@link ZargonAI}.
 */
public final class TurnManager {
    private final Dice dice;
    private final CombatEngine combat;

    public TurnManager(Dice dice, CombatEngine combat) {
        this.dice = dice;
        this.combat = combat;
    }

    /** Prepares the active Hero's turn: roll 2d6 movement and clear flags. */
    public void beginHeroTurn(GameState state) {
        state.setPhase(Phase.HERO);
        Hero hero = state.getActiveHero();
        if (hero == null) {
            return;
        }
        int roll = dice.rollMovement();
        state.setMovementRemaining(roll);
        state.setActionUsed(false);
        state.setMoveRolled(true);
        Visibility.revealFrom(state, hero.getPosition());
        state.log(hero.getName() + " begins their turn (moved " + roll + " squares available).");
    }

    /** Squares the active Hero can currently reach with remaining movement. */
    public Set<Point> reachableSquares(GameState state) {
        Set<Point> result = new HashSet<Point>();
        Hero hero = state.getActiveHero();
        if (hero == null) {
            return result;
        }
        int budget = state.getMovementRemaining();
        Point start = hero.getPosition();
        Queue<Point> frontier = new ArrayDeque<Point>();
        Map<Point, Integer> dist = new HashMap<Point, Integer>();
        frontier.add(start);
        dist.put(start, 0);
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!frontier.isEmpty()) {
            Point cur = frontier.poll();
            int d = dist.get(cur);
            if (d >= budget) {
                continue;
            }
            for (int[] s : steps) {
                Point next = cur.translate(s[0], s[1]);
                if (dist.containsKey(next)) {
                    continue;
                }
                if (!state.getMap().isWalkable(next) || state.isOccupied(next)) {
                    continue;
                }
                dist.put(next, d + 1);
                result.add(next);
                frontier.add(next);
            }
        }
        return result;
    }

    /** Attempts to walk the active Hero to {@code target}. Returns true on success. */
    public boolean moveActiveHero(GameState state, Point target) {
        Hero hero = state.getActiveHero();
        if (hero == null || state.isOccupied(target)) {
            return false;
        }
        List<Point> path = Pathfinding.shortestPath(state, hero.getPosition(), target);
        if (path.isEmpty() || path.size() > state.getMovementRemaining()) {
            return false;
        }
        for (Point step : path) {
            hero.setPosition(step);
            Visibility.revealFrom(state, step);
            Tile t = state.getMap().tileAt(step);
            if (t.hasTrap()) {
                t.springTrap();
                int dmg = hero.wound(1);
                state.log(hero.getName() + " springs a pit trap and loses " + dmg + " Body Point.");
                break; // movement ends when a trap is sprung
            }
        }
        state.setMovementRemaining(state.getMovementRemaining() - path.size());
        return true;
    }

    /** Opens a closed door adjacent to the active Hero (a free action) and reveals beyond. */
    public boolean openDoor(GameState state, Point doorPoint) {
        Hero hero = state.getActiveHero();
        if (hero == null || !state.getMap().inBounds(doorPoint)) {
            return false;
        }
        Tile t = state.getMap().tileAt(doorPoint);
        if (t.getType() != TileType.DOOR_CLOSED) {
            return false;
        }
        if (!hero.getPosition().isOrthogonalNeighbour(doorPoint)) {
            return false;
        }
        t.setType(TileType.DOOR_OPEN);
        Visibility.revealFrom(state, doorPoint);
        Visibility.revealFrom(state, hero.getPosition());
        state.log(hero.getName() + " opens a door.");
        return true;
    }

    /** The active Hero attacks an adjacent monster; consumes the Hero's action. */
    public CombatEngine.Result heroAttack(GameState state, Monster target) {
        Hero hero = state.getActiveHero();
        if (hero == null || state.isActionUsed() || !target.isAlive()) {
            return null;
        }
        if (!hero.getPosition().isOrthogonalNeighbour(target.getPosition())) {
            return null;
        }
        CombatEngine.Result r = combat.resolveAttack(hero, target);
        state.setActionUsed(true);
        state.log(hero.getName() + " attacks " + target.getName() + ": " + r.skulls
                + " skulls vs " + r.blocks + " shields -> " + r.damage + " Body Points.");
        if (r.fatal) {
            state.log(target.getName() + " is slain!");
        }
        return r;
    }

    /** The active Hero searches the current room for treasure; consumes the action. */
    public void searchTreasure(GameState state) {
        Hero hero = state.getActiveHero();
        if (hero == null || state.isActionUsed()) {
            return;
        }
        state.setActionUsed(true);
        Decks.TreasureCard card = state.getTreasureDeck() == null ? null : state.getTreasureDeck().poll();
        if (card == null) {
            state.log(hero.getName() + " searches but the treasure deck is empty.");
            return;
        }
        state.log(hero.getName() + " searches for treasure: " + card.getText());
        switch (card.getKind()) {
            case GOLD:
                hero.addGold(card.getValue());
                break;
            case HEAL:
                hero.heal(card.getValue());
                break;
            case HAZARD:
                hero.wound(card.getValue());
                break;
            case WANDERING_MONSTER:
                spawnWanderingMonster(state, hero);
                break;
            case NOTHING:
            default:
                break;
        }
    }

    private void spawnWanderingMonster(GameState state, Hero hero) {
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] s : steps) {
            Point p = hero.getPosition().translate(s[0], s[1]);
            if (state.getMap().isWalkable(p) && !state.isOccupied(p)) {
                Monster m = new Monster(MonsterType.GOBLIN);
                m.setPosition(p);
                state.getMonsters().add(m);
                state.getMap().tileAt(p).setRevealed(true);
                state.log("A wandering Goblin appears beside " + hero.getName() + "!");
                return;
            }
        }
        state.log("A wandering monster stirs, but there is no room for it to appear.");
    }

    /** Casts a spell from the active Hero on an optional target; consumes the action. */
    public void castSpell(GameState state, Spell spell, Entity target) {
        Hero hero = state.getActiveHero();
        if (hero == null || state.isActionUsed() || !hero.getSpells().contains(spell)) {
            return;
        }
        state.setActionUsed(true);
        hero.removeSpell(spell);
        state.log(hero.getName() + " casts " + spell.getName() + ".");
        switch (spell.getEffect()) {
            case DAMAGE:
                if (target != null && target.isAlive()) {
                    int dealt = target.wound(spell.getMagnitude());
                    state.log(spell.getName() + " inflicts " + dealt + " Body Points on "
                            + target.getName() + ".");
                    if (!target.isAlive()) {
                        state.log(target.getName() + " is destroyed!");
                    }
                }
                break;
            case HEAL:
                Entity healed = target != null ? target : hero;
                healed.heal(spell.getMagnitude());
                state.log(healed.getName() + " recovers " + spell.getMagnitude() + " Body Points.");
                break;
            case DEFEND:
            case PASS:
            default:
                break;
        }
    }

    /**
     * Ends the active Hero's turn and advances to the next living Hero.
     * Returns true if another Hero still has to act this round, false when the
     * Hero phase is over and Zargon should act.
     */
    public boolean endHeroTurn(GameState state) {
        List<Hero> heroes = state.getHeroes();
        int idx = state.getActiveHeroIndex();
        for (int i = idx + 1; i < heroes.size(); i++) {
            if (heroes.get(i).isAlive()) {
                state.setActiveHeroIndex(i);
                beginHeroTurn(state);
                return true;
            }
        }
        return false; // no more heroes this round
    }

    /** Starts a fresh Hero phase from the first living Hero. */
    public void startNewRound(GameState state) {
        List<Hero> heroes = state.getHeroes();
        for (int i = 0; i < heroes.size(); i++) {
            if (heroes.get(i).isAlive()) {
                state.setActiveHeroIndex(i);
                beginHeroTurn(state);
                return;
            }
        }
    }
}
