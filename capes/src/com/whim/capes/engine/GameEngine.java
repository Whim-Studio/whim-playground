package com.whim.capes.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.whim.capes.model.Ability;
import com.whim.capes.model.Character;
import com.whim.capes.model.Conflict;
import com.whim.capes.model.ConflictSide;
import com.whim.capes.model.ConflictType;
import com.whim.capes.model.Die;
import com.whim.capes.model.Drive;
import com.whim.capes.model.DriveType;
import com.whim.capes.model.EventLogEntry;
import com.whim.capes.model.GameState;
import com.whim.capes.model.Inspiration;
import com.whim.capes.model.Page;
import com.whim.capes.model.Player;
import com.whim.capes.model.Scene;
import com.whim.capes.model.Stake;

/**
 * The stateful rules engine (Phase 3). Drives the core loop — Scenes, Pages,
 * Claims, Conflict creation, Staking, Splitting, Ability use, Reactions and
 * Resolving — enforcing the resource and legality rules programmatically so the
 * UI only ever performs legal moves. Randomness is injected via {@link Roller}.
 *
 * <p>The resource-critical operations (Stake limits, even Splits, Overdraw
 * checks, Resolve token distribution) delegate their arithmetic to
 * {@link RulesMath} and are exercised by {@code GameEngineTest}.
 */
public final class GameEngine {
    private final GameState state;
    private final Roller roller;

    // Per-Page transient accounting (keyed by page number).
    private int accountingPage = -1;
    private final Set<String> claimsUsedFree = new HashSet<String>();       // playerIds who used their free Claim
    private final Set<String> conflictAddedFree = new HashSet<String>();    // playerIds who used their free add
    private final Set<String> freeActionUsed = new HashSet<String>();       // characterIds who used their free Action
    // Per-Action transient: which players have already Reacted to the current Action (p.40).
    private final Set<String> reactedThisAction = new HashSet<String>();

    // Turn pointer within the current Page (seat index; clockwise from the Starter).
    private int currentTurnSeat = -1;

    public GameEngine(GameState state, Roller roller) {
        this.state = state;
        this.roller = roller;
    }

    public GameState state() { return state; }

    // ----------------------------------------------------------------- Scenes
    public Scene declareScene(String playerId, String title) {
        Player p = require(state.playerById(playerId), "player");
        int number = state.scenes().size() + 1;
        Scene scene = new Scene(number, playerId, title);
        state.scenes().add(scene);
        state.recordSceneDeclared(playerId);
        log(EventLogEntry.Category.SCENE, "Scene " + number + " declared by " + p.name() + ": \"" + title + "\".");
        return scene;
    }

    /** Assigns a character to a player for the current Scene; extra characters cost a Story Token (p.20). */
    public void assignRole(String playerId, String characterId, boolean firstFree) {
        Player p = require(state.playerById(playerId), "player");
        Character c = require(state.characterById(characterId), "character");
        if (!firstFree) spendStoryTokenOrFail(p, "buy an extra character for this Scene");
        if (!p.controlledCharacterIds().contains(characterId)) p.controlledCharacterIds().add(characterId);
        log(EventLogEntry.Category.SCENE, p.name() + " plays " + c.name() + (firstFree ? "." : " (paid a Story Token)."));
    }

    // ------------------------------------------------------------------ Pages
    /**
     * Starts a new Page. The Starter advances one seat clockwise each Page
     * (p.22). Runs the Overdrawn penalty rolls (p.32) immediately.
     */
    public Page startPage(Scene scene) {
        int pageNumber = scene.pages().size() + 1;
        int starterSeat;
        if (scene.pages().isEmpty()) {
            starterSeat = state.seatOf(scene.declaringPlayerId());
        } else {
            String prevStarter = scene.pages().get(scene.pages().size() - 1).starterPlayerId();
            starterSeat = (state.seatOf(prevStarter) + 1) % state.players().size();
        }
        Player starter = state.players().get(starterSeat);
        Page page = new Page(pageNumber, starter.id());
        scene.pages().add(page);
        resetPageAccounting(pageNumber);
        currentTurnSeat = starterSeat;
        log(EventLogEntry.Category.PAGE, "Page " + pageNumber + " begins. Starter: " + starter.name() + ".");
        runOverdrawnChecks(scene, page);
        page.setPhase(Page.Phase.CLAIM);
        return page;
    }

    // ---------------------------------------------------------- turn / phase
    /** The player whose turn it currently is within the active Page (or null). */
    public Player currentActor() {
        if (currentTurnSeat < 0 || state.players().isEmpty()) return null;
        return state.players().get(currentTurnSeat % state.players().size());
    }

    /** Advances the turn one seat clockwise. */
    public Player advanceTurn() {
        if (state.players().isEmpty()) return null;
        currentTurnSeat = (Math.max(0, currentTurnSeat) + 1) % state.players().size();
        Player a = currentActor();
        log(EventLogEntry.Category.PAGE, "Turn passes to " + a.name() + ".");
        return a;
    }

    /** Advances the Page phase in order (Claim → Free Narration → Actions → Resolve → Done). */
    public Page.Phase advancePhase(Page page) {
        Page.Phase[] order = Page.Phase.values();
        int i = page.phase().ordinal();
        Page.Phase next = order[Math.min(i + 1, order.length - 1)];
        page.setPhase(next);
        log(EventLogEntry.Category.PAGE, "Phase → " + next + ".");
        return next;
    }

    /** Appends free-form story text to the shared log, tied to the current moment (p.24). */
    public void logNarration(String text) {
        if (text == null || text.trim().isEmpty()) return;
        state.eventLog().log(EventLogEntry.Category.NARRATION, "Narration", text.trim());
    }

    /**
     * Overdrawn penalty (p.32): at Page start, for each Overdrawn Drive, roll
     * the character's highest owned die and accept only a lower result. If the
     * character owns no die on the table, the penalty is skipped (Ambiguity A2).
     */
    public void runOverdrawnChecks(Scene scene, Page page) {
        for (Character c : state.roster()) {
            int overdrawnCount = countOverdrawn(c);
            for (int i = 0; i < overdrawnCount; i++) {
                Die highest = highestOwnedDie(scene, c);
                if (highest == null) {
                    log(EventLogEntry.Category.SYSTEM, c.name() + " is Overdrawn but owns no die — penalty skipped.");
                    continue;
                }
                int before = highest.value();
                int rolled = roller.rollD6();
                if (rolled < before) {
                    highest.set(rolled);
                    log(EventLogEntry.Category.SYSTEM, c.name() + " Overdrawn penalty: die " + before + " → " + rolled + ".");
                } else {
                    log(EventLogEntry.Category.SYSTEM, c.name() + " Overdrawn penalty: rolled " + rolled + " (≥ " + before + "), kept.");
                }
            }
        }
    }

    private int countOverdrawn(Character c) {
        if (c.isUndifferentiated()) return c.isUndifferentiatedOverdrawn() ? 1 : 0;
        int n = 0;
        for (Drive d : c.drives()) if (d.isOverdrawn()) n++;
        return n;
    }

    private Die highestOwnedDie(Scene scene, Character c) {
        Die best = null;
        for (Conflict conf : scene.conflicts()) {
            for (ConflictSide s : conf.sides()) {
                if (!s.alliedCharacterIds().contains(c.id())) continue;
                for (Die d : s.dice()) if (best == null || d.value() > best.value()) best = d;
            }
        }
        return best;
    }

    // -------------------------------------------------------------- Conflicts
    /** Adds a Conflict to the Scene. First add per player per Page is free; extras cost a Story Token (p.22). */
    public Conflict addConflict(Scene scene, String playerId, String title, ConflictType type, boolean free) {
        Player p = require(state.playerById(playerId), "player");
        ensureAccounting(currentPageNumber(scene));
        if (free) {
            if (conflictAddedFree.contains(playerId))
                throw new IllegalMoveException(p.name() + " already added a free Conflict this Page; extras cost a Story Token.");
            conflictAddedFree.add(playerId);
        } else {
            spendStoryTokenOrFail(p, "add an extra Conflict");
        }
        String id = "cf" + scene.number() + "-" + (scene.conflicts().size() + 1);
        Conflict c = new Conflict(id, title, type, playerId);
        scene.conflicts().add(c);
        log(EventLogEntry.Category.CONFLICT, p.name() + " adds " + type.label() + ": \"" + title + "\".");
        return c;
    }

    /** Claims a side. First Claim per player per Page is free; a side may not be Claimed twice (p.22). */
    public void claim(Scene scene, Conflict conflict, int sideIndex, String playerId, boolean free) {
        Player p = require(state.playerById(playerId), "player");
        ensureAccounting(currentPageNumber(scene));
        ConflictSide side = conflict.sides().get(sideIndex);
        if (!side.claimingPlayerIds().isEmpty() && !side.claimingPlayerIds().contains(playerId))
            throw new IllegalMoveException("That side is already Claimed by another player this Page.");
        if (free) {
            if (claimsUsedFree.contains(playerId))
                throw new IllegalMoveException(p.name() + " already used their free Claim; extra Claims cost a Story Token.");
            claimsUsedFree.add(playerId);
        } else {
            spendStoryTokenOrFail(p, "make an extra Claim");
        }
        side.claim(playerId);
        log(EventLogEntry.Category.CONFLICT, p.name() + " Claims side " + (sideIndex + 1) + " of \"" + conflict.title() + "\".");
    }

    // ------------------------------------------------------- Ch.5 participants
    /**
     * Introduces a Chapter 5 non-person participant (Thing/Location/Phenomenon/
     * Situation) into the game: builds a mundane character (p.102), registers it
     * in the roster and hands control to a player so it can act.
     */
    public Character addNonPerson(com.whim.capes.content.NonPersonTemplate t, String controllingPlayerId) {
        Player p = require(state.playerById(controllingPlayerId), "player");
        String id = "np" + (state.roster().size() + 1);
        Character c = CharacterFactory.fromNonPerson(id, t);
        state.roster().add(c);
        if (!p.controlledCharacterIds().contains(id)) p.controlledCharacterIds().add(id);
        log(EventLogEntry.Category.SYSTEM, p.name() + " brings in " + t.category().name().toLowerCase()
                + " \"" + t.name() + "\".");
        return c;
    }

    /**
     * Adds a participant's built-in Character Conflict (p.103). When this
     * Conflict later Resolves, the participant is removed from the Scene
     * (handled in {@link #resolve}).
     */
    public Conflict addCharacterConflict(Scene scene, Character participant, ConflictType type,
                                         String title, String playerId) {
        String id = "cf" + scene.number() + "-" + (scene.conflicts().size() + 1);
        Conflict c = new Conflict(id, title, type, playerId);
        c.setCharacterConflictOwnerId(participant.id());
        c.sides().get(0).ally(participant.id());
        scene.conflicts().add(c);
        log(EventLogEntry.Category.CONFLICT, participant.name() + "'s Free " + type.label() + ": \"" + title
                + "\" enters play (resolving it removes " + participant.name() + " from the Scene).");
        return c;
    }

    /**
     * Introduces an Exemplar's permanent Free Conflict (pp.75-76): the root
     * conflict between a character and their Exemplar, playable once per Scene
     * when both appear. Unlike a Ch.5 Character Conflict, resolving it does not
     * remove anyone — the relationship persists and can recur in later Scenes.
     */
    public Conflict addExemplarConflict(Scene scene, Character owner,
                                        com.whim.capes.model.Exemplar ex, String playerId) {
        String id = "cf" + scene.number() + "-" + (scene.conflicts().size() + 1);
        String title = ex.rootConflictStatement() == null || ex.rootConflictStatement().isEmpty()
                ? owner.name() + " & Exemplar (" + ex.drive().displayName() + ")"
                : ex.rootConflictStatement();
        Conflict c = new Conflict(id, title, ex.freeConflictType(), playerId);
        c.sides().get(0).ally(owner.id());
        Character exemplar = state.characterById(ex.exemplarCharacterId());
        if (exemplar != null) c.sides().get(1).ally(exemplar.id());
        scene.conflicts().add(c);
        log(EventLogEntry.Category.CONFLICT, "Exemplar Free " + ex.freeConflictType().label()
                + " enters play: \"" + title + "\".");
        return c;
    }

    /** Characters currently Overdrawn (any Drive over Strength, or an undifferentiated stack above 5). */
    public List<Character> overdrawnCharacters() {
        List<Character> out = new ArrayList<Character>();
        for (Character c : state.roster()) if (countOverdrawn(c) > 0) out.add(c);
        return out;
    }

    // ----------------------------------------------------------------- Staking
    /**
     * Stakes Debt from one Drive onto a Claimed side (pp.36-37): only one Drive
     * per character per Conflict, never more than the Drive's Strength, and only
     * as much Debt as rests on that Drive. Undifferentiated characters draw from
     * their single stack (cap 3).
     */
    public void stake(Conflict conflict, int sideIndex, Character c, DriveType driveType, int amount) {
        if (amount < 1) throw new IllegalMoveException("Stake at least 1 Debt.");
        ConflictSide side = conflict.sides().get(sideIndex);

        if (c.isUndifferentiated()) {
            if (amount > Character.UNDIFF_STAKE_CAP)
                throw new IllegalMoveException("Undifferentiated characters may Stake at most " + Character.UNDIFF_STAKE_CAP + ".");
            if (c.undifferentiatedDebt() < amount)
                throw new IllegalMoveException(c.name() + " lacks that much Debt.");
            Stake existing = side.findStake(c.id(), null);
            if (existing != null && existing.amount() + amount > Character.UNDIFF_STAKE_CAP)
                throw new IllegalMoveException("That would exceed the Undifferentiated Stake cap of " + Character.UNDIFF_STAKE_CAP + ".");
            c.removeUndifferentiatedDebt(amount);
            addToStake(side, c.id(), null, amount);
            side.ally(c.id());
            log(EventLogEntry.Category.STAKE, c.name() + " Stakes " + amount + " (undifferentiated) on \"" + conflict.title() + "\".");
            return;
        }

        Drive drive = require(c.drive(driveType), "drive " + driveType);
        if (side.hasStakeFromOtherDrive(c.id(), driveType))
            throw new IllegalMoveException(c.name() + " may Stake only one Drive per Conflict (already Staked a different Drive here).");
        Stake existing = side.findStake(c.id(), driveType);
        int already = existing == null ? 0 : existing.amount();
        if (already + amount > drive.strength())
            throw new IllegalMoveException("Cannot Stake more than the Drive's Strength (" + drive.strength() + ").");
        if (drive.debt() < amount)
            throw new IllegalMoveException(driveType + " holds only " + drive.debt() + " Debt.");
        drive.removeDebt(amount);
        addToStake(side, c.id(), driveType, amount);
        side.ally(c.id());
        log(EventLogEntry.Category.STAKE, c.name() + " Stakes " + amount + " " + driveType.displayName()
                + " on \"" + conflict.title() + "\".");
    }

    private void addToStake(ConflictSide side, String characterId, DriveType drive, int amount) {
        Stake existing = side.findStake(characterId, drive);
        if (existing != null) existing.add(amount);
        else side.stakes().add(new Stake(characterId, drive, amount));
    }

    // ---------------------------------------------------------------- Splitting
    /**
     * Splits one die on a side into {@code parts} dice, as evenly as possible
     * (p.37). A side may hold at most as many dice as it has Stakes.
     */
    public void split(Conflict conflict, int sideIndex, int dieIndex, int parts) {
        ConflictSide side = conflict.sides().get(sideIndex);
        if (side.stakeCount() < 1)
            throw new IllegalMoveException("Cannot Split a side with no Staked Debt.");
        int resultingDice = side.dice().size() - 1 + parts;
        if (resultingDice > side.stakeCount())
            throw new IllegalMoveException("A side may have at most as many dice as Stakes (" + side.stakeCount() + ").");
        Die die = side.dice().get(dieIndex);
        int[] pieces = RulesMath.evenSplit(die.value(), parts);
        side.dice().remove(dieIndex);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pieces.length; i++) {
            side.dice().add(new Die(pieces[i]));
            sb.append(pieces[i]).append(i < pieces.length - 1 ? "+" : "");
        }
        log(EventLogEntry.Category.SPLIT, "Split a die into " + sb + " on \"" + conflict.title() + "\".");
    }

    /**
     * Splits off to found a new side (p.37): by Staking a single point of Debt,
     * a character splits a die from the side they are leaving, keeps the smaller
     * resulting die on the old side, and moves the other to a brand-new side
     * they now back. The new side is Claimed by the acting player.
     */
    public ConflictSide foundNewSide(Scene scene, Conflict conflict, int fromSideIndex, int dieIndex,
                                     Character c, DriveType driveType, String newStatement, String playerId) {
        ConflictSide from = conflict.sides().get(fromSideIndex);
        Die die = from.dice().get(dieIndex);
        if (die.value() < 2) throw new IllegalMoveException("Need a die of at least 2 to split off a new side.");

        // Create the new side and Stake a single point of Debt onto it.
        ConflictSide newSide = conflict.addSide(newStatement);
        newSide.dice().clear(); // will receive the split-off die
        stake(conflict, conflict.sides().indexOf(newSide), c, driveType, 1);

        int[] pieces = RulesMath.evenSplit(die.value(), 2); // larger first
        from.dice().remove(dieIndex);
        from.dice().add(new Die(pieces[1]));                // smaller stays on old side
        newSide.dice().add(new Die(pieces[0]));             // larger founds the new side
        newSide.ally(c.id());
        newSide.claim(playerId);
        log(EventLogEntry.Category.SPLIT, c.name() + " splits off a new side of \"" + conflict.title()
                + "\" (" + pieces[1] + " stays, " + pieces[0] + " founds it).");
        return newSide;
    }

    // ---------------------------------------------------------------- Abilities
    /**
     * Uses an Ability to roll one die (p.38), starting a fresh Action (clears
     * the Reaction record). Applies the Ability's cost (super → Debt on the
     * chosen Drive; mundane → Blocks for the Scene) and marks per-Page use. The
     * die now holds the new roll with its prior value remembered; the caller
     * then calls {@link #acceptRoll} or {@link #revertRoll}.
     *
     * @return the rolled value
     */
    public int useAbilityRoll(Scene scene, Page page, Character c, Ability ability,
                              Conflict conflict, int sideIndex, int dieIndex, DriveType debtDrive) {
        ConflictSide side = conflict.sides().get(sideIndex);
        Die die = side.dice().get(dieIndex);
        checkAbilityUsable(scene, page, c, ability, die.value());
        applyAbilityCost(scene, page, c, ability, debtDrive);

        reactedThisAction.clear(); // new Action
        int rolled = roller.rollD6();
        die.placeRoll(rolled);
        side.ally(c.id());
        log(EventLogEntry.Category.ABILITY, c.name() + " uses " + ability.name() + " (" + ability.score()
                + ") to roll a die: " + die.previousValue() + " → " + rolled + " on \"" + conflict.title() + "\".");
        return rolled;
    }

    /** Accepts the last roll on a die (opens the Reaction window, p.40). */
    public void acceptRoll(Conflict conflict, int sideIndex, int dieIndex) {
        log(EventLogEntry.Category.ACTION, "Roll accepted on \"" + conflict.title() + "\" (Reactions may follow).");
    }

    /** Reverts a die to its value before the last roll (p.38); no Reaction window opens. */
    public void revertRoll(Conflict conflict, int sideIndex, int dieIndex) {
        Die die = conflict.sides().get(sideIndex).dice().get(dieIndex);
        int had = die.value();
        die.revert();
        log(EventLogEntry.Category.ACTION, "Roll not accepted: die turned back " + had + " → " + die.value() + ".");
    }

    /**
     * A Reaction (p.40): re-rolls a just-accepted die using an Ability whose
     * score ≥ the die's current value. Any player may React, but only once per
     * Action. Applies the Ability cost.
     * @return the rolled value (caller may accept/revert as with an Action)
     */
    public int react(Scene scene, Page page, String reactingPlayerId, Character c, Ability ability,
                     Conflict conflict, int sideIndex, int dieIndex, DriveType debtDrive) {
        if (reactedThisAction.contains(reactingPlayerId))
            throw new IllegalMoveException("A player may not React twice to the same Action.");
        ConflictSide side = conflict.sides().get(sideIndex);
        Die die = side.dice().get(dieIndex);
        checkAbilityUsable(scene, page, c, ability, die.value());
        applyAbilityCost(scene, page, c, ability, debtDrive);
        reactedThisAction.add(reactingPlayerId);
        int rolled = roller.rollD6();
        die.placeRoll(rolled);
        side.ally(c.id());
        log(EventLogEntry.Category.REACTION, state.playerById(reactingPlayerId).name() + " Reacts with "
                + ability.name() + " (" + ability.score() + "): " + die.previousValue() + " → " + rolled + ".");
        return rolled;
    }

    /** Uses an Ability to raise an Inspiration by one point (p.38); Ability score must be ≥ its current value. */
    public void raiseInspiration(Scene scene, Page page, Character c, Ability ability, Inspiration insp, DriveType debtDrive) {
        if (!ability.canAffect(insp.value()))
            throw new IllegalMoveException(ability.name() + " (" + ability.score() + ") cannot affect a value-" + insp.value() + " Inspiration.");
        checkAbilityUsable(scene, page, c, ability, insp.value());
        applyAbilityCost(scene, page, c, ability, debtDrive);
        insp.raiseByOne();
        log(EventLogEntry.Category.ABILITY, c.name() + " raises an Inspiration to " + insp.value() + ".");
    }

    private void checkAbilityUsable(Scene scene, Page page, Character c, Ability ability, int targetValue) {
        // Availability first: a used/blocked Ability cannot be used at all this Page/Scene.
        if (ability.isSuperPowered()) {
            if (page.isSuperAbilityUsed(c.id(), ability.name()))
                throw new IllegalMoveException(ability.name() + " was already used this Page.");
        } else {
            if (scene.isAbilityBlocked(c.id(), ability.name()))
                throw new IllegalMoveException(ability.name() + " already Blocked this Scene.");
        }
        if (!ability.canAffect(targetValue))
            throw new IllegalMoveException(ability.name() + " (" + ability.score() + ") cannot affect a value-" + targetValue + " die.");
    }

    private void applyAbilityCost(Scene scene, Page page, Character c, Ability ability, DriveType debtDrive) {
        if (ability.isSuperPowered()) {
            page.markSuperAbilityUsed(c.id(), ability.name());
            if (c.isUndifferentiated()) {
                c.addUndifferentiatedDebt(1);
            } else {
                Drive d = debtDrive != null ? c.drive(debtDrive) : (c.drives().isEmpty() ? null : c.drives().get(0));
                if (d != null) d.addDebt(1);
            }
            log(EventLogEntry.Category.TOKEN, c.name() + " earns 1 Debt from " + ability.name() + ".");
        } else {
            scene.blockAbility(c.id(), ability.name());
        }
    }

    // -------------------------------------------------------------- Inspirations
    /** Spends an Inspiration to raise one die on that character's side to the Inspiration's value (p.25). */
    public void spendInspiration(Player player, Inspiration insp, Conflict conflict, int sideIndex, int dieIndex) {
        if (!player.inspirations().contains(insp))
            throw new IllegalMoveException("That player does not hold that Inspiration.");
        Die die = conflict.sides().get(sideIndex).dice().get(dieIndex);
        die.set(insp.value());
        player.inspirations().remove(insp);
        log(EventLogEntry.Category.ABILITY, player.name() + " spends an Inspiration, setting a die to " + insp.value()
                + " on \"" + conflict.title() + "\".");
    }

    // ---------------------------------------------------------------- Resolving
    /**
     * Resolves a Conflict from the perspective of the Claimant, whose side must
     * Control (pp.30-31). Distributes doubled Debt to losers, Story Tokens from
     * winning Stakes to losing characters, and Inspirations to the Resolver (and
     * opposing side). Handles Deadlock (p.30).
     */
    public ResolveResult resolve(Scene scene, Conflict conflict, int claimingSideIndex, String resolverPlayerId) {
        ConflictSide winning = conflict.sides().get(claimingSideIndex);
        ConflictSide controlling = conflict.controllingSide();

        if (controlling == null) {
            // Tie: Deadlock only if unbreakable; otherwise it simply cannot be Resolved yet.
            boolean deadlock = RulesMath.isDeadlocked(true, anyDebtAvailable(scene), allDiceMaxed(conflict));
            if (deadlock) return resolveDeadlock(scene, conflict);
            throw new IllegalMoveException("Conflict is tied — nobody Controls, cannot Resolve yet.");
        }
        if (controlling != winning)
            throw new IllegalMoveException("The Claimed side does not Control the Conflict.");

        ResolveResult r = new ResolveResult(ResolveResult.Kind.NORMAL, conflict.title(), resolverPlayerId);

        // Gather losing sides and their dice.
        List<ConflictSide> losing = new ArrayList<ConflictSide>();
        List<Integer> losingDice = new ArrayList<Integer>();
        for (ConflictSide s : conflict.sides()) {
            if (s == winning) continue;
            losing.add(s);
            for (Die d : s.dice()) losingDice.add(d.value());
        }

        // 1) Losers take back double their Staked Debt, to the originating Drive (p.30).
        for (ConflictSide s : losing) {
            for (Stake st : s.stakes()) {
                Character c = state.characterById(st.characterId());
                int back = st.amount() * 2;
                returnDebt(c, st.driveType(), back);
                r.debtReturned += back;
                r.lines.add(c.name() + " takes back " + back + " Debt (doubled) to "
                        + (st.driveType() == null ? "stack" : st.driveType().displayName()) + ".");
            }
        }

        // 2) Winners give away Staked Debt as Story Tokens to losing characters' players (p.30).
        for (Stake st : winning.stakes()) {
            String ownerPlayerId = playerControlling(st.characterId());
            for (int i = 0; i < st.amount(); i++) {
                Player recipient = pickStoryTokenRecipient(conflict, losing, ownerPlayerId, resolverPlayerId);
                if (recipient == null) { r.storyTokensDiscarded++; }
                else { recipient.addStoryTokens(1); r.storyTokensAwarded++;
                       r.lines.add(recipient.name() + " gains a Story Token."); }
            }
        }

        // 3) Inspirations via highest-to-highest pairing (p.30).
        List<Integer> winningDice = new ArrayList<Integer>();
        for (Die d : winning.dice()) winningDice.add(d.value());
        RulesMath.InspirationSplit split = RulesMath.pairInspirations(winningDice, losingDice);
        Player resolver = state.playerById(resolverPlayerId);
        String winForWhom = winning.alliedCharacterIds().isEmpty() ? "the winning side"
                : displayName(winning.alliedCharacterIds().get(0));
        for (int v : split.resolverInspirations) {
            resolver.inspirations().add(new Inspiration(v, winForWhom));
            r.resolverInspirations++;
        }
        if (!split.opposingInspirations.isEmpty()) {
            Player opp = firstOpposingPlayer(losing);
            String oppForWhom = losing.isEmpty() || losing.get(0).alliedCharacterIds().isEmpty()
                    ? "the opposing side" : displayName(losing.get(0).alliedCharacterIds().get(0));
            for (int v : split.opposingInspirations) {
                if (opp != null) opp.inspirations().add(new Inspiration(v, oppForWhom));
                r.opposingInspirations++;
            }
        }

        conflict.markResolved();
        scene.conflicts().remove(conflict);
        removeParticipantIfCharacterConflict(scene, conflict);
        log(EventLogEntry.Category.RESOLVE, resolver.name() + " Resolves " + r.summary());
        return r;
    }

    /** When a participant's Character Conflict Resolves, that participant leaves the Scene (p.103). */
    private void removeParticipantIfCharacterConflict(Scene scene, Conflict conflict) {
        String ownerId = conflict.characterConflictOwnerId();
        if (ownerId == null) return;
        for (Conflict other : scene.conflicts()) {
            for (ConflictSide s : other.sides()) s.alliedCharacterIds().remove(ownerId);
        }
        Character owner = state.characterById(ownerId);
        log(EventLogEntry.Category.SYSTEM,
                (owner == null ? ownerId : owner.name()) + " leaves the Scene (its Character Conflict Resolved).");
    }

    /**
     * Gloat instead of Resolving when the outcome would violate the Comics Code
     * (p.41). Starting from the highest die on the side, turn dice down to 1;
     * the Resolver gains a Story Token per die turned. The Conflict stays on the
     * table.
     */
    public ResolveResult gloat(Conflict conflict, int gloatingSideIndex, String resolverPlayerId, int diceToTurn) {
        ConflictSide side = conflict.sides().get(gloatingSideIndex);
        ResolveResult r = new ResolveResult(ResolveResult.Kind.GLOAT, conflict.title(), resolverPlayerId);
        // sort dice descending, turn the top `diceToTurn` down to 1
        List<Die> dice = new ArrayList<Die>(side.dice());
        dice.sort(new java.util.Comparator<Die>() {
            @Override public int compare(Die a, Die b) { return b.value() - a.value(); }
        });
        Player resolver = state.playerById(resolverPlayerId);
        int turned = 0;
        for (int i = 0; i < Math.min(diceToTurn, dice.size()); i++) {
            if (dice.get(i).value() <= 1) break;
            dice.get(i).set(1);
            resolver.addStoryTokens(1);
            turned++;
        }
        r.storyTokensAwarded = turned;
        log(EventLogEntry.Category.GLOAT, resolver.name() + " Gloats on \"" + conflict.title() + "\": turned "
                + turned + " dice to 1, gaining " + turned + " Story Tokens.");
        return r;
    }

    private ResolveResult resolveDeadlock(Scene scene, Conflict conflict) {
        ResolveResult r = new ResolveResult(ResolveResult.Kind.DEADLOCK, conflict.title(), null);
        // All Staked Debt on all sides is treated as lost -> returned doubled; all dice -> Inspirations directly.
        for (ConflictSide s : conflict.sides()) {
            for (Stake st : s.stakes()) {
                Character c = state.characterById(st.characterId());
                int back = st.amount() * 2;
                returnDebt(c, st.driveType(), back);
                r.debtReturned += back;
            }
            Player p = firstPlayerFor(s);
            String forWhom = s.alliedCharacterIds().isEmpty() ? "a side" : displayName(s.alliedCharacterIds().get(0));
            for (Die d : s.dice()) {
                if (p != null) p.inspirations().add(new Inspiration(d.value(), forWhom));
                r.resolverInspirations++;
            }
        }
        conflict.markResolved();
        scene.conflicts().remove(conflict);
        removeParticipantIfCharacterConflict(scene, conflict);
        log(EventLogEntry.Category.RESOLVE, "Deadlock on \"" + conflict.title() + "\": " + r.summary());
        return r;
    }

    // ------------------------------------------------------------ helpers
    private void returnDebt(Character c, DriveType drive, int amount) {
        if (c == null) return;
        if (drive == null || c.isUndifferentiated()) c.addUndifferentiatedDebt(amount);
        else { Drive d = c.drive(drive); if (d != null) d.addDebt(amount); }
    }

    /** Chooses who receives one Story Token from a winning Stake (creator-first; never the Stake owner or Resolver, p.30). */
    private Player pickStoryTokenRecipient(Conflict conflict, List<ConflictSide> losing, String ownerPlayerId, String resolverPlayerId) {
        // Creator-on-losing-side, played by someone other than the Resolver, gets first token (p.30).
        String creator = conflict.creatorPlayerId();
        if (creator != null && !creator.equals(resolverPlayerId) && !creator.equals(ownerPlayerId)
                && controlsAnyLosingCharacter(creator, losing)) {
            return state.playerById(creator);
        }
        for (ConflictSide s : losing) {
            for (String charId : s.alliedCharacterIds()) {
                String pid = playerControlling(charId);
                if (pid == null) continue;
                if (pid.equals(ownerPlayerId)) continue;   // never keep your own character's Debt (p.30)
                if (pid.equals(resolverPlayerId)) continue; // the winner does not receive
                return state.playerById(pid);
            }
        }
        return null; // discarded
    }

    private boolean controlsAnyLosingCharacter(String playerId, List<ConflictSide> losing) {
        for (ConflictSide s : losing)
            for (String charId : s.alliedCharacterIds())
                if (playerId.equals(playerControlling(charId))) return true;
        return false;
    }

    private Player firstOpposingPlayer(List<ConflictSide> losing) {
        for (ConflictSide s : losing) {
            Player p = firstPlayerFor(s);
            if (p != null) return p;
        }
        return null;
    }

    private Player firstPlayerFor(ConflictSide s) {
        for (String charId : s.alliedCharacterIds()) {
            String pid = playerControlling(charId);
            if (pid != null) return state.playerById(pid);
        }
        return null;
    }

    /** The id of the player currently controlling a character (or null). */
    public String playerControlling(String characterId) {
        for (Player p : state.players()) if (p.controlledCharacterIds().contains(characterId)) return p.id();
        return null;
    }

    private String displayName(String characterId) {
        Character c = state.characterById(characterId);
        return c == null ? characterId : c.name();
    }

    private boolean anyDebtAvailable(Scene scene) {
        for (Character c : state.roster()) {
            if (c.isUndifferentiated() && c.undifferentiatedDebt() > 0) return true;
            if (c.totalRestingDebt() > 0) return true;
        }
        return false;
    }

    private boolean allDiceMaxed(Conflict conflict) {
        for (ConflictSide s : conflict.sides())
            for (Die d : s.dice())
                if (d.value() < Die.MAX) return false;
        return true;
    }

    private void spendStoryTokenOrFail(Player p, String toDoWhat) {
        if (!p.spendStoryToken())
            throw new IllegalMoveException(p.name() + " has no Story Token to " + toDoWhat + ".");
    }

    private int currentPageNumber(Scene scene) {
        return scene.pages().isEmpty() ? 0 : scene.pages().get(scene.pages().size() - 1).number();
    }

    private void ensureAccounting(int pageNumber) {
        if (accountingPage != pageNumber) resetPageAccounting(pageNumber);
    }

    private void resetPageAccounting(int pageNumber) {
        accountingPage = pageNumber;
        claimsUsedFree.clear();
        conflictAddedFree.clear();
        freeActionUsed.clear();
        reactedThisAction.clear();
    }

    private void log(EventLogEntry.Category cat, String msg) { state.eventLog().log(cat, msg); }

    private static <T> T require(T t, String what) {
        if (t == null) throw new IllegalMoveException("No such " + what + ".");
        return t;
    }
}
