package com.whim.capes.content;

import java.util.ArrayList;
import java.util.List;

import com.whim.capes.model.ConflictType;

import static com.whim.capes.content.NonPersonTemplate.list;

/**
 * The Chapter 5 catalogue of non-person participants (pp.104-111): Things,
 * Locations, Phenomena and Situations, several carrying a built-in Character
 * Conflict (Free Event/Goal). Transcribed from the rulebook; like Click-and-Lock
 * modules these are editable starting points.
 */
public final class ExtendedData {
    private ExtendedData() {}

    public static List<NonPersonTemplate> all() {
        List<NonPersonTemplate> m = new ArrayList<NonPersonTemplate>();
        NonPersonTemplate.Category T = NonPersonTemplate.Category.THING;
        NonPersonTemplate.Category L = NonPersonTemplate.Category.LOCATION;
        NonPersonTemplate.Category P = NonPersonTemplate.Category.PHENOMENON;
        NonPersonTemplate.Category S = NonPersonTemplate.Category.SITUATION;

        // Things (pp.104-105)
        m.add(new NonPersonTemplate("Space Battleship", T,
                list("Shoot", "Orbit", "Loom", "Fighters", "Shields"),
                list("Certain", "Grim", "Smug", "Ominous", "Violent"),
                list("Only one weakness", "Overkill", "Extreme range", "Sizzling energy"), null, null));
        m.add(new NonPersonTemplate("Super-Computer", T,
                list("Analyze", "Search", "Calculate", "Simulate", "Plan"),
                list("Logical", "Literal", "Smart", "Stupid", "Coy"),
                list("Patterns", "Rigid program", "Models", "Databases"), null, null));
        m.add(new NonPersonTemplate("Deathtrap", T,
                list("Needless Elaboration", "Contingency Plans", "Chain of Events", "\"Tick, Tock\""),
                list("Clever", "Spiteful", "Eccentric", "Teasing", "Arrogant"),
                list("Peculiar Style"), ConflictType.EVENT, "Trap is Sprung"));
        m.add(new NonPersonTemplate("Doomsday Device", T,
                list("Booby traps", "Hard casing", "Elaborate wiring", "Energy surge"),
                list("Relentless", "Ominous", "Mocking", "Stubborn", "Unpredictable"),
                list("Requires great finesse"), ConflictType.EVENT, "\"00:00:01\""));
        m.add(new NonPersonTemplate("Cursed Object", T,
                list("Bad Luck", "Good Luck", "Distort Perceptions", "Entice", "Bad Dreams"),
                list("Capricious", "Driven", "Spiteful", "Domineering", "Selfish"),
                list(), ConflictType.GOAL, "Claim another victim"));

        // Locations (pp.106-107)
        m.add(new NonPersonTemplate("Abandoned Amusement Park", L,
                list("Conceal", "Confuse", "Surprise", "Lull", "Disorient"),
                list("Spooked", "Relaxed", "Amused", "Lost", "Lonely"),
                list("\"Let's Split up!\"", "\"Just the wind\"", "Mirrors", "Rides", "Rickety"), null, null));
        m.add(new NonPersonTemplate("Church", L,
                list("Awe", "Shame", "Inspire", "Soothe", "Comfort"),
                list("Reverent", "Determined", "Desperate", "Calm", "Afraid"),
                list("Doves", "Crucifix", "Pews", "Candles", "Rafters"), null, null));
        m.add(new NonPersonTemplate("Volcano", L,
                list("Burn", "Smoke", "Crumble", "Shake", "Stifle"),
                list("Noxious Fumes", "Hot to the touch", "Shifting shadows"),
                list("Honey-combed with tunnels", "Bursts of lava"), ConflictType.EVENT, "Someone falls toward lava"));
        m.add(new NonPersonTemplate("Ancient Temple", L,
                list("Skulls", "Chasm", "Dead Languages", "Hordes of deadly critters"),
                list("Afraid", "Fascinated", "Greedy", "Cocky", "Cautious"),
                list("Roots and Vines"), ConflictType.EVENT, "Treasure is in hand"));
        m.add(new NonPersonTemplate("Court Room", L,
                list("Gavel", "\"Order! Order!\"", "Bailiff", "Court Recorder", "Jury"),
                list("Outraged", "Surprised", "Confident", "Crying", "Furious"),
                list(), ConflictType.EVENT, "Shocking new evidence!"));

        // Phenomena (pp.108-109)
        m.add(new NonPersonTemplate("Martial Law", P,
                list("Oppress", "Shoot", "Betray", "Arrest", "Threaten"),
                list("Paranoid", "Afraid", "Arrogant", "Loyal", "Resigned"),
                list("Authority", "Surveillance", "Police Presence", "Silence", "Sirens"), null, null));
        m.add(new NonPersonTemplate("Public Opinion", P,
                list("Orate", "Speculate", "Cheer", "Watch", "Judge"),
                list("Enthused", "Worried", "Proud", "Loyal", "Fickle"),
                list("Catchphrase", "Snap Judgment", "Headline", "One-sided"), null, null));
        m.add(new NonPersonTemplate("Murphy's Law", P,
                list("Add Insult to Injury", "Worst Possible Time", "Opportunity", "Irony"),
                list("Unsympathetic", "Angry", "Resigned", "Bitter", "Impatient"),
                list("\"Of course\""), ConflictType.GOAL, "Keep your spirits up"));
        m.add(new NonPersonTemplate("Virus", P,
                list("Contaminate", "Transform", "Lie Dormant", "Weaken victim", "Consume"),
                list("Panicked", "Confused", "Confident", "Distressed", "Frustrated"),
                list(), ConflictType.GOAL, "Contain the infection"));
        m.add(new NonPersonTemplate("Disaster", P,
                list("Chain Reaction", "Massive Scale", "Moment of Truth", "Ominous Cracking"),
                list("Desperate", "Panicked", "Fascinated", "Selfish", "Grateful"),
                list("More problems than people to solve them"), ConflictType.GOAL, "Save lives"));

        // Situations (pp.110-111)
        m.add(new NonPersonTemplate("Social Function", S,
                list("Mingle", "Dance", "Eat", "Talk", "Posture"),
                list("Antsy", "Happy", "Loud", "Annoying", "Shy"),
                list("Alcohol", "Speeches", "Three's a crowd", "A little drunk", "The kitchen"), null, null));
        m.add(new NonPersonTemplate("Sneaking", S,
                list("Sneak", "Hide", "Search", "Notice", "Overhear"),
                list("Tense", "Patient", "Curious", "Rushed", "Relieved"),
                list("Alcove", "Inching Closer", "\"Shhhh!\"", "Distraction"), null, null));
        m.add(new NonPersonTemplate("Misplaced in Time", S,
                list("Culture Shock", "Anachronism", "Foreknowledge", "Ignore taboos"),
                list("Nostalgic", "Frustrated", "Confused", "Amused", "Incredulous"),
                list(), ConflictType.GOAL, "Blend in"));
        m.add(new NonPersonTemplate("Chase", S,
                list("Accelerate", "Swerve", "Improvise", "Skid", "Lose Control"),
                list("Obstacle", "Technical Difficulties", "Narrow Confines"),
                list("Straightaway", "New terrain"), ConflictType.EVENT, "Neck and Neck"));
        m.add(new NonPersonTemplate("Mystery", S,
                list("Obvious Untruth", "Apparent Lead", "Two facts are connected", "Contradiction"),
                list("Talkative", "Desperate", "Reluctant", "Wary", "Evasive"),
                list("Theory proven wrong"), ConflictType.EVENT, "Answers are Revealed"));
        return m;
    }
}
