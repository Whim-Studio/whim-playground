package com.whim.coda.data;

import com.whim.coda.model.Ability;
import com.whim.coda.model.Attribute;
import com.whim.coda.model.Edge;
import com.whim.coda.model.Flaw;
import com.whim.coda.model.Skill;
import com.whim.coda.model.Species;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Hardcoded Coda game data: species roster, skills, edges, and flaws. */
public final class DataRepository {

    private DataRepository() {
    }

    private static final List<Species> SPECIES;
    private static final List<Skill> SKILLS;
    private static final List<Edge> EDGES;
    private static final List<Flaw> FLAWS;

    static {
        SPECIES = Collections.unmodifiableList(buildSpecies());
        SKILLS = Collections.unmodifiableList(buildSkills());
        EDGES = Collections.unmodifiableList(buildEdges());
        FLAWS = Collections.unmodifiableList(buildFlaws());
    }

    /** EXACT roster, ordered. */
    public static List<Species> species() {
        return SPECIES;
    }

    public static Species speciesByName(String name) {
        if (name == null) {
            return null;
        }
        for (Species s : SPECIES) {
            if (s.getName().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    public static List<Skill> skills() {
        return SKILLS;
    }

    public static List<Edge> edges() {
        return EDGES;
    }

    public static List<Flaw> flaws() {
        return FLAWS;
    }

    // ------------------------------------------------------------------
    // Species
    // ------------------------------------------------------------------

    private static List<Species> buildSpecies() {
        List<Species> list = new ArrayList<Species>();

        // Bajoran — no attribute mods; Pagh modeled via bonusCourage = 1.
        list.add(new Species("Bajoran", mods(), abilities(
                ability("Artistic", "+1 Craft"),
                ability("Faithful", "+2 Religion"),
                ability("Pagh", "+1 Courage point")), 1));

        // Betazoid — Presence +1.
        list.add(new Species("Betazoid", mods(Attribute.PRESENCE, 1), abilities(
                ability("Psionic", "Possesses psionic potential."),
                ability("Peaceful", "+4 Negotiate"),
                ability("Telepathy 2", "Telepathy at rank 2.")), 0));

        // Cardassian — Perception +1, Vitality +1, Agility -1, Presence -1.
        list.add(new Species("Cardassian", mods(
                Attribute.PERCEPTION, 1,
                Attribute.VITALITY, 1,
                Attribute.AGILITY, -1,
                Attribute.PRESENCE, -1), abilities(
                ability("Eidetic Memory", "Near-perfect recall."),
                ability("High Pain Threshold", "Resists pain and interrogation."),
                ability("Devious", "+2 Influence"),
                ability("Prying", "+2 Inquire"),
                ability("Vesala", "Cardassian neck ridge sensitivity / cultural trait.")), 0));

        // Ferengi — Presence +1, Perception +1, Strength -2.
        list.add(new Species("Ferengi", mods(
                Attribute.PRESENCE, 1,
                Attribute.PERCEPTION, 1,
                Attribute.STRENGTH, -2), abilities(
                ability("Keen Hearing", "Exceptional auditory perception."),
                ability("Eye for Profit", "Instinct for lucrative opportunities."),
                ability("Four-Lobed Brain", "Resistant to telepathy."),
                ability("Head for Numbers", "Excels at calculation and accounting."),
                ability("Lobes for Business", "Innate commercial acumen.")), 0));

        // Human — no mods, no abilities.
        list.add(new Species("Human", mods(), abilities(), 0));

        // Klingon — Strength +1, Vitality +1, Intellect -1, Perception -1.
        list.add(new Species("Klingon", mods(
                Attribute.STRENGTH, 1,
                Attribute.VITALITY, 1,
                Attribute.INTELLECT, -1,
                Attribute.PERCEPTION, -1), abilities(), 0));

        // Romulan — EMPTY placeholder (no mods, no abilities).
        list.add(new Species("Romulan", mods(), abilities(), 0));

        // Vulcan — Intellect +1, Strength +2, Presence -3.
        list.add(new Species("Vulcan", mods(
                Attribute.INTELLECT, 1,
                Attribute.STRENGTH, 2,
                Attribute.PRESENCE, -3), abilities(
                ability("Psionic", "Possesses psionic potential (commonly).")), 0));

        return list;
    }

    private static Ability ability(String name, String description) {
        return new Ability(name, description);
    }

    private static List<Ability> abilities(Ability... a) {
        return new ArrayList<Ability>(Arrays.asList(a));
    }

    /** Build an attribute-mod map from alternating (Attribute, int) pairs. */
    private static Map<Attribute, Integer> mods(Object... pairs) {
        Map<Attribute, Integer> m = new EnumMap<Attribute, Integer>(Attribute.class);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            m.put((Attribute) pairs[i], (Integer) pairs[i + 1]);
        }
        return m;
    }

    // ------------------------------------------------------------------
    // Skills
    // ------------------------------------------------------------------

    private static List<Skill> buildSkills() {
        List<Skill> list = new ArrayList<Skill>();
        list.add(new Skill("Athletics", Attribute.VITALITY));
        list.add(new Skill("Acrobatics", Attribute.AGILITY));
        list.add(new Skill("Stealth", Attribute.AGILITY));
        list.add(new Skill("Sleight of Hand", Attribute.AGILITY));
        list.add(new Skill("Armed Combat", Attribute.AGILITY));
        list.add(new Skill("Unarmed Combat", Attribute.STRENGTH));
        list.add(new Skill("Ranged Combat", Attribute.AGILITY));
        list.add(new Skill("Influence", Attribute.PRESENCE));
        list.add(new Skill("Negotiate", Attribute.PRESENCE));
        list.add(new Skill("Inquire", Attribute.PRESENCE));
        list.add(new Skill("Command", Attribute.PRESENCE));
        list.add(new Skill("Engineering", Attribute.INTELLECT));
        list.add(new Skill("Computer Use", Attribute.INTELLECT));
        list.add(new Skill("Medicine", Attribute.INTELLECT));
        list.add(new Skill("Science", Attribute.INTELLECT));
        list.add(new Skill("Knowledge", Attribute.INTELLECT));
        list.add(new Skill("Investigate", Attribute.INTELLECT));
        list.add(new Skill("Observe", Attribute.PERCEPTION));
        list.add(new Skill("Survival", Attribute.PERCEPTION));
        list.add(new Skill("World Knowledge", Attribute.PERCEPTION));
        list.add(new Skill("Craft", Attribute.INTELLECT));
        list.add(new Skill("Repair", Attribute.INTELLECT));
        list.add(new Skill("Drive", Attribute.AGILITY));
        list.add(new Skill("Pilot", Attribute.AGILITY));
        list.add(new Skill("Religion", Attribute.PRESENCE));
        list.add(new Skill("Tactics", Attribute.INTELLECT));
        return list;
    }

    // ------------------------------------------------------------------
    // Edges
    // ------------------------------------------------------------------

    private static List<Edge> buildEdges() {
        List<Edge> list = new ArrayList<Edge>();
        list.add(new Edge("Alertness", "+1 to Observe and to avoid being surprised."));
        list.add(new Edge("Ally", "A reliable contact who can render aid."));
        list.add(new Edge("Bold", "Resistant to fear; bonus when acting decisively."));
        list.add(new Edge("Eidetic Memory", "Total recall of details once observed."));
        list.add(new Edge("Fit", "Above-average physical conditioning."));
        list.add(new Edge("Rank", "Holds a position of authority in an organization."));
        list.add(new Edge("Resolute", "Strong will; bonus to resist coercion."));
        list.add(new Edge("Sense of Direction", "Rarely becomes lost; innate orientation."));
        list.add(new Edge("Wealth", "Access to significant personal resources."));
        return list;
    }

    // ------------------------------------------------------------------
    // Flaws
    // ------------------------------------------------------------------

    private static List<Flaw> buildFlaws() {
        List<Flaw> list = new ArrayList<Flaw>();
        list.add(new Flaw("Arrogant", "Overestimates own abilities; alienates others."));
        list.add(new Flaw("Code of Honor", "Bound by a strict personal code."));
        list.add(new Flaw("Coward", "Penalty when facing fear or danger."));
        list.add(new Flaw("Curse", "A persistent misfortune or social stigma."));
        list.add(new Flaw("Impulsive", "Acts before thinking things through."));
        list.add(new Flaw("Obligation", "Owes a recurring duty or debt."));
        list.add(new Flaw("Rival", "A persistent adversary who opposes you."));
        list.add(new Flaw("Weakness", "A vulnerability or addiction that can be exploited."));
        return list;
    }
}
