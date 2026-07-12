package com.whim.firetop.engine;

import com.whim.firetop.model.Board;
import com.whim.firetop.model.Card;
import com.whim.firetop.model.CardEffect;
import com.whim.firetop.model.Character;
import com.whim.firetop.model.Content;
import com.whim.firetop.model.GameState;
import com.whim.firetop.model.Item;
import com.whim.firetop.model.ItemType;
import com.whim.firetop.model.Monster;
import com.whim.firetop.model.Room;
import com.whim.firetop.model.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The turn state machine. Wraps a {@link GameState}, drives movement, dispatches
 * room resolution, resolves card effects, and detects win/lose. Combat rounds
 * themselves are driven by the UI using {@link Combat}; the engine is told the
 * result via {@link #onMonsterDefeated(Room)} / {@link #onCharacterDefeated(Character)}.
 */
public final class GameEngine {

    /** How healing a single provision restores (adopted ruling). */
    public static final int PROVISION_HEAL = 4;

    /** Listener for human-readable log lines. */
    public interface Listener {
        void onLog(String line);
        void onStateChanged();
    }

    private final GameState state;
    private final Dice dice;
    private final Deck encounterDeck;
    private final Deck treasureDeck;
    private final Deck eventDeck;
    private Listener listener = new Listener() {
        public void onLog(String line) { }
        public void onStateChanged() { }
    };

    private int movementRoll = 0;
    private boolean moved = false;

    /** Wraps existing state (used on load and new game). */
    public GameEngine(GameState state) {
        this.state = state;
        this.dice = new Dice(state.getSeed() ^ 0x5DEECE66DL);
        this.treasureDeck = new Deck(state.getTreasureDeck(), state.getSeed() + 1);
        this.eventDeck = new Deck(state.getEventDeck(), state.getSeed() + 2);
        this.encounterDeck = new Deck(state.getEncounterDeck(), state.getSeed() + 3);
    }

    /**
     * Creates a fresh game with the given adventurer names and a seed. Attributes
     * are rolled with a deterministic dice stream derived from the seed.
     */
    public static GameEngine newGame(List<String> names, long seed) {
        Dice setup = new Dice(seed);
        Board board = Content.buildDungeon();
        List<Character> players = new ArrayList<Character>();
        for (String name : names) {
            Character c = new Character(name, setup.rollSkill(), setup.rollStamina(), setup.rollLuck());
            c.setRoomId(board.getEntranceId());
            players.add(c);
        }
        board.getRoom(board.getEntranceId()).setVisited(true);
        GameState gs = new GameState(board, players,
                Content.buildEncounterDeck(), Content.buildTreasureDeck(), Content.buildEventDeck(), seed);
        GameEngine e = new GameEngine(gs);
        e.log("The party enters Firetop Mountain through the Mountain Gate.");
        return e;
    }

    public void setListener(Listener l) { this.listener = l; }
    public GameState getState() { return state; }
    public Board getBoard() { return state.getBoard(); }
    public Dice getDice() { return dice; }
    public Character currentCharacter() { return state.currentPlayer(); }
    public int getMovementRoll() { return movementRoll; }
    public boolean hasMoved() { return moved; }

    private void log(String line) {
        state.getLog().add(line);
        listener.onLog(line);
    }

    /** Rolls the movement die for the current adventurer's turn. */
    public int rollMovement() {
        movementRoll = dice.d6();
        moved = false;
        log(currentCharacter().getName() + " rolls a " + movementRoll + " for movement.");
        listener.onStateChanged();
        return movementRoll;
    }

    /** Rooms the current adventurer may legally move to this turn. */
    public Set<Integer> reachableRooms() {
        return getBoard().roomsWithin(currentCharacter().getRoomId(), movementRoll);
    }

    /**
     * Moves the current adventurer into a room (must be reachable) and returns a
     * {@link RoomResolution} describing what the UI must resolve next.
     */
    public RoomResolution moveTo(int roomId) {
        if (moved) {
            throw new IllegalStateException("Already moved this turn.");
        }
        if (!reachableRooms().contains(roomId)) {
            throw new IllegalArgumentException("Room " + roomId + " is not reachable.");
        }
        Character c = currentCharacter();
        c.setRoomId(roomId);
        moved = true;
        Room room = getBoard().getRoom(roomId);
        room.setVisited(true);
        log(c.getName() + " moves into the " + room.getName() + ".");
        listener.onStateChanged();
        return resolveRoom(room, c);
    }

    private RoomResolution resolveRoom(Room room, Character c) {
        // Collect any loose gold once.
        if (room.getGold() > 0) {
            c.addGold(room.getGold());
            log(c.getName() + " gathers " + room.getGold() + " gold pieces.");
            room.setGold(0);
        }
        switch (room.getType()) {
            case MONSTER:
            case LAIR:
                if (!room.isResolved() && room.getMonster() != null) {
                    return RoomResolution.combat(room.getMonster());
                }
                return RoomResolution.none();
            case TREASURE:
                if (!room.isResolved()) {
                    room.setResolved(true);
                    return RoomResolution.card(treasureDeck.draw(), treasureDeck);
                }
                return RoomResolution.none();
            case EVENT:
                return RoomResolution.card(eventDeck.draw(), eventDeck);
            case TRAP:
                Card trap = new Card(room.getName() + " Trap", com.whim.firetop.model.CardType.EVENT,
                        room.getDescription(), CardEffect.TEST_LUCK_TRAP, 3);
                return RoomResolution.card(trap, null);
            case SPECIAL:
                Card boon = new Card("Blessing of the " + room.getName(),
                        com.whim.firetop.model.CardType.EVENT, room.getDescription(),
                        CardEffect.RESTORE_LUCK, 2);
                return RoomResolution.card(boon, null);
            default:
                return RoomResolution.none();
        }
    }

    /**
     * Applies a drawn card's effect to a character. If the card is an encounter,
     * the returned resolution carries the monster for the UI to fight; otherwise
     * a log line describes the result. Non-encounter cards are discarded.
     */
    public RoomResolution resolveCard(Card card, Deck source, Character c) {
        if (card == null) {
            log("The deck is empty; nothing happens.");
            return RoomResolution.none();
        }
        log("Card: " + card.getName() + " — " + card.getDescription());
        switch (card.getEffect()) {
            case GAIN_GOLD:
                c.addGold(card.getMagnitude());
                log(c.getName() + " gains " + card.getMagnitude() + " gold.");
                break;
            case LOSE_STAMINA:
                c.loseStamina(card.getMagnitude());
                log(c.getName() + " loses " + card.getMagnitude() + " STAMINA.");
                break;
            case RESTORE_STAMINA:
                c.gainStamina(card.getMagnitude());
                log(c.getName() + " restores " + card.getMagnitude() + " STAMINA.");
                break;
            case RESTORE_LUCK:
                c.gainLuck(card.getMagnitude());
                log(c.getName() + " restores " + card.getMagnitude() + " LUCK.");
                break;
            case GAIN_SKILL:
                c.gainSkill(card.getMagnitude());
                log(c.getName() + " sharpens up: +" + card.getMagnitude() + " SKILL (max Initial).");
                break;
            case GAIN_POTION:
                c.getInventory().add(new Item(card.getName(), ItemType.POTION,
                        card.getDescription(), card.getMagnitude()));
                log(c.getName() + " pockets a potion.");
                break;
            case GAIN_TREASURE:
                c.getInventory().add(new Item(card.getName(), ItemType.TREASURE,
                        card.getDescription(), card.getMagnitude()));
                c.addGold(card.getMagnitude());
                log(c.getName() + " claims treasure worth " + card.getMagnitude() + " gold.");
                break;
            case GAIN_PROVISION:
                c.addProvisions(card.getMagnitude());
                log(c.getName() + " gains " + card.getMagnitude() + " provision(s).");
                break;
            case TEST_LUCK_TRAP:
                LuckTest.Result r = LuckTest.test(c, dice);
                if (r.isLucky()) {
                    log(c.getName() + " tests LUCK (rolled " + r.getRoll() + " vs " + r.getLuckBefore()
                            + ") — Lucky! The trap is avoided.");
                } else {
                    c.loseStamina(card.getMagnitude());
                    log(c.getName() + " tests LUCK (rolled " + r.getRoll() + " vs " + r.getLuckBefore()
                            + ") — Unlucky! Loses " + card.getMagnitude() + " STAMINA.");
                }
                break;
            case ENCOUNTER_MONSTER:
                Monster m = card.getMonster();
                if (source != null) {
                    source.discard(card);
                }
                return RoomResolution.combat(m);
            case NOTHING:
            default:
                log("Nothing of use here.");
                break;
        }
        if (source != null) {
            source.discard(card);
        }
        checkDefeat(c);
        listener.onStateChanged();
        return RoomResolution.none();
    }

    /** Called by the UI when the current adventurer beats a room's monster. */
    public boolean onMonsterDefeated(Room room, Monster monster) {
        log(currentCharacter().getName() + " defeats the " + monster.getName() + "!");
        if (room != null) {
            room.setResolved(true);
        }
        if (monster.getName().startsWith("Zagor") || (room != null && room.getType() == RoomType.LAIR)) {
            state.setGameOver(true);
            state.setVictory(true);
            log("Zagor is slain! The treasure of Firetop Mountain is yours. VICTORY!");
            listener.onStateChanged();
            return true;
        }
        listener.onStateChanged();
        return false;
    }

    /** Called by the UI when an adventurer is killed (in combat or by a card). */
    public void onCharacterDefeated(Character c) {
        log(c.getName() + " has fallen in Firetop Mountain.");
        checkDefeat(c);
        listener.onStateChanged();
    }

    private void checkDefeat(Character c) {
        if (!c.isAlive()) {
            if (!state.anyAlive()) {
                state.setGameOver(true);
                state.setVictory(false);
                log("The entire party has perished. DEFEAT.");
            }
        }
    }

    /** Eats a provision for the current adventurer. */
    public boolean eatProvision() {
        Character c = currentCharacter();
        if (c.eatProvision(PROVISION_HEAL)) {
            log(c.getName() + " eats a provision (+" + PROVISION_HEAL + " STAMINA).");
            listener.onStateChanged();
            return true;
        }
        return false;
    }

    /** Uses a potion from inventory to restore STAMINA. */
    public boolean usePotion(Item item) {
        Character c = currentCharacter();
        if (item != null && item.getType() == ItemType.POTION && c.getInventory().remove(item)) {
            c.gainStamina(item.getMagnitude());
            log(c.getName() + " drinks " + item.getName() + " (+" + item.getMagnitude() + " STAMINA).");
            listener.onStateChanged();
            return true;
        }
        return false;
    }

    /** Advances to the next living adventurer. Returns false if the game is over. */
    public boolean endTurn() {
        if (state.isGameOver()) {
            return false;
        }
        movementRoll = 0;
        moved = false;
        int n = state.getPlayers().size();
        for (int i = 0; i < n; i++) {
            int next = (state.getCurrentPlayerIndex() + 1 + i) % n;
            if (state.getPlayers().get(next).isAlive()) {
                state.setCurrentPlayerIndex(next);
                log("--- " + currentCharacter().getName() + "'s turn ---");
                listener.onStateChanged();
                return true;
            }
        }
        return false;
    }

    /** Syncs the live deck contents back into the state before saving. */
    public void syncForSave() {
        state.getTreasureDeck().clear();
        state.getTreasureDeck().addAll(treasureDeck.remaining());
        state.getEventDeck().clear();
        state.getEventDeck().addAll(eventDeck.remaining());
        state.getEncounterDeck().clear();
        state.getEncounterDeck().addAll(encounterDeck.remaining());
    }

    /** Describes the immediate consequence of entering a room or drawing a card. */
    public static final class RoomResolution {
        public enum Kind { NONE, COMBAT, CARD }
        private final Kind kind;
        private final Monster monster;
        private final Card card;
        private final Deck source;

        private RoomResolution(Kind kind, Monster monster, Card card, Deck source) {
            this.kind = kind;
            this.monster = monster;
            this.card = card;
            this.source = source;
        }

        public static RoomResolution none() { return new RoomResolution(Kind.NONE, null, null, null); }
        public static RoomResolution combat(Monster m) { return new RoomResolution(Kind.COMBAT, m, null, null); }
        public static RoomResolution card(Card c, Deck src) { return new RoomResolution(Kind.CARD, null, c, src); }

        public Kind getKind() { return kind; }
        public Monster getMonster() { return monster; }
        public Card getCard() { return card; }
        public Deck getSource() { return source; }
    }
}
