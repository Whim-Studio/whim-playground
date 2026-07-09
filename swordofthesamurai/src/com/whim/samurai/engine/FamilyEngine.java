package com.whim.samurai.engine;

import com.whim.samurai.app.Game;
import com.whim.samurai.model.FamilyMember;
import com.whim.samurai.model.GameState;
import com.whim.samurai.model.Samurai;

/**
 * Marriage, births, aging and succession — the dynastic layer (design ref §5).
 *
 * <p>"Every samurai should be married: he needs a wife to manage his household and
 * an heir to carry on the family name and tradition." This engine implements taking
 * a wife, the household growing and aging over time, and — crucially — promoting the
 * eldest heir-eligible son into the player's seat when the head of the family dies
 * (design ref §4.4 / §5.2). Dying heir-less ends the dynasty (design ref §5.4 / §8.2).</p>
 *
 * <p>All methods are static so screens can call them directly (the turn engine that
 * would normally drive births/aging is a sibling slice we do not reference).</p>
 */
public final class FamilyEngine {
    private FamilyEngine() { }

    private static final String[] BRIDE_NAMES = {
        "Aiko", "Chiyo", "Emi", "Hana", "Kiku", "Mari", "Sato", "Yuki", "Rin", "Michi"
    };
    private static final String[] SON_NAMES = {
        "Ichiro", "Jiro", "Saburo", "Takeshi", "Hiro", "Kenji", "Yoshi", "Masa"
    };
    private static final String[] DAUGHTER_NAMES = {
        "Ayame", "Fuji", "Haru", "Kaede", "Momo", "Sakura", "Suzu", "Umé"
    };

    // --- Marriage ---------------------------------------------------------

    /** Can the player take a wife right now? (Not already married; alive.) */
    public static boolean canMarry(Game game) {
        Samurai p = player(game);
        return p != null && p.alive && !p.isMarried();
    }

    /**
     * Propose and take a wife (design ref §5.1). Creates the wife household member and
     * applies a small dowry/honor effect. Marrying into a comparable house is assumed
     * here; a full honor-of-bride comparison lives in the politics slice. Returns a
     * short flavour string describing the match, or a reason it could not proceed.
     */
    public static String marry(Game game) {
        Samurai p = player(game);
        if (p == null || !p.alive) return "There is no lord to be wed.";
        if (p.isMarried()) return p.wife.name + " is already your wife.";

        String name = pick(game, BRIDE_NAMES);
        // A bride "of marriageable age" — a youth or older (design ref §5.1).
        FamilyMember wife = new FamilyMember(name, FamilyMember.Relation.WIFE, 15 + roll(game, 6));
        p.wife = wife;

        // Marriage into an honourable house lifts your standing; a dowry adjusts koku.
        // Values are approximations of the manual's qualitative "marry up / down" rule (design ref §3.2/§5.1).
        int dowry = 40 + roll(game, 60);
        p.koku += dowry;
        p.honor += 15;

        return "You are wed to " + name + " of a respected house. "
             + "Her dowry brings " + dowry + " koku, and the match raises your honour.";
    }

    // --- Births & aging (callable helpers) --------------------------------

    /**
     * Chance the household is "blessed with children" this year (design ref §5.2).
     * A modest per-year probability; sons are heirs, daughters help manage the house
     * and can later be married to peers for alliances.
     */
    public static String maybeBirth(Game game) {
        Samurai p = player(game);
        if (p == null || !p.isMarried()) return null;
        if (p.wife.age > 45) return null;                 // approximation: fertility window
        // ~35% per call — tuned for a playable dynasty (approximation; the manual gives no rate).
        if (roll(game, 100) >= 35) return null;
        boolean son = roll(game, 2) == 0;
        FamilyMember child = son
                ? new FamilyMember(pick(game, SON_NAMES), FamilyMember.Relation.SON, 0)
                : new FamilyMember(pick(game, DAUGHTER_NAMES), FamilyMember.Relation.DAUGHTER, 0);
        p.children.add(child);
        return "Your house is blessed: a " + (son ? "son" : "daughter") + ", " + child.name + ", is born.";
    }

    /** Age the head of the family and every living household member by one year. */
    public static void ageOneYear(Game game) {
        Samurai p = player(game);
        if (p == null) return;
        p.age++;
        if (p.wife != null && p.wife.alive) p.wife.age++;
        for (FamilyMember c : p.children) if (c.alive) c.age++;
    }

    // --- Succession -------------------------------------------------------

    /**
     * Promote the eldest heir-eligible son into the player's seat (design ref §4.4).
     *
     * <p>The heir "starts weaker than his father … not able to control as large a
     * domain," but builds on the father's accomplishments — the dynasty is a relay.
     * We carry the clan name, keep the fiefs, increment the generation, fold the late
     * lord's honour into the family score, and give the heir a fresh younger household.
     * Returns true if an heir took over, false if the line has ended.</p>
     */
    public static boolean succeed(Game game) {
        GameState s = game.state;
        if (s == null) return false;
        Samurai old = s.player;
        FamilyMember heir = (old != null) ? old.heir() : null;
        if (heir == null) {
            markHeirlessGameOver(game);
            return false;
        }

        Samurai son = new Samurai();
        son.name = heir.name;
        son.clanName = (old != null) ? old.clanName : "";
        son.age = Math.max(15, heir.age);
        son.alive = true;
        // Rank and fiefs carry over; the son inherits the family's seat (design ref §4.4).
        if (old != null) {
            son.rank = old.rank;
            son.fiefs.addAll(old.fiefs);
            // Heir "starts weaker" — assets are a fraction of the father's (approximation).
            son.honor = Math.max(60, (int) (old.honor * 0.6));
            son.power = Math.max(20, (int) (old.power * 0.5));
            son.koku = Math.max(80, old.koku / 2);
            son.swordsmanship = Math.max(5, old.swordsmanship - 2);
            son.generalship   = Math.max(5, old.generalship - 2);
            son.stealth       = Math.max(4, old.stealth - 2);
            // The father's honour is remembered by the family (design ref §3.2 — dying honourably).
            s.dynastyScore += old.honor;
        }

        s.player = son;
        s.generation++;
        // A captured heir must be returned so he can assume his duties (design ref §5.3):
        // the promoted son is, by definition, free and at his seat.

        // Clear the death flags so play resumes on the map.
        s.gameOver = false;
        s.victory = false;
        s.gameOverReason = "";
        return true;
    }

    /**
     * Handle the head of the family dying without an heir — the dynasty ends
     * (design ref §5.4 / §8.2). Marks game over with a reason if not already set.
     */
    public static void markHeirlessGameOver(Game game) {
        GameState s = game.state;
        if (s == null) return;
        s.gameOver = true;
        s.victory = false;
        if (s.gameOverReason == null || s.gameOverReason.isEmpty()) {
            s.gameOverReason = "You died leaving no heir. With no son to carry the name, "
                             + "your family's line ends and the house passes into history.";
        }
    }

    /** True if a living, eligible heir exists to continue the dynasty. */
    public static boolean hasHeir(Game game) {
        Samurai p = player(game);
        return p != null && p.hasHeir();
    }

    // --- helpers ----------------------------------------------------------

    private static Samurai player(Game game) {
        return (game != null && game.state != null) ? game.state.player : null;
    }

    private static int roll(Game game, int bound) {
        return game.rng.range(0, bound - 1);
    }

    private static String pick(Game game, String[] arr) {
        return arr[game.rng.range(0, arr.length - 1)];
    }
}
