package com.whim.capes.content;

import java.util.ArrayList;
import java.util.List;

import static com.whim.capes.content.ClickLockModule.list;

/**
 * The Click-and-Lock catalogue transcribed from Chapter 4 (pp.81-99): 15
 * Power-Sets, 17 Personae and 17 Skill-Sets. Power/Skill-Sets carry five
 * Powers/Skills plus a half-set of Styles; Personae carry Attitudes plus the
 * complementary Styles.
 *
 * <p>Per p.80 these modules are inspirational starting points, not rigid data:
 * the final character shape (3-5 per column, numbered 1-up) is enforced when
 * the sheet is locked, so a module supplying slightly more or fewer than five
 * items is fine — the player crosses out and renumbers freely.
 */
public final class ClickLockData {
    private ClickLockData() {}

    public static List<ClickLockModule> powerSets() {
        List<ClickLockModule> m = new ArrayList<ClickLockModule>();
        m.add(ps("Godling",
                list("Super-strength", "Super-speed", "Invulnerability", "Laser Eye-beams", "Flight"),
                list("Inspire Awe", "Casually overpower mortals", "Divert large flying objects")));
        m.add(ps("Gadgeteer",
                list("Technical Genius", "Techno-armor", "Repulsor beams", "Sensor systems", "Flight-pack"),
                list("Improvise a gadget", "Co-opt enemy systems", "Know weak points of any system")));
        m.add(ps("Martial Artist",
                list("Acrobatics", "Combat intuition", "Precision strikes", "Focussed battle aura", "Ninja stealth"),
                list("Improvised weaponry", "Named techniques", "Brains over brawn")));
        m.add(ps("Hunter",
                list("Tireless stamina", "Ensnare", "Wrestle", "Razor-keen senses", "Find weaknesses"),
                list("Predict prey's next move", "Perch in a high place", "Exhaust your prey")));
        m.add(ps("Speedster",
                list("Super-speed", "Accelerated Reflexes", "Rapid recovery", "Do many things at once", "Faster than the laws of physics"),
                list("Trail of Disruptions", "Move someone out of the way", "Fast enough to try another plan")));
        m.add(ps("Brick",
                list("Super-leap", "Titanic Punch", "Invulnerability", "Super-strength", "Great leverage from tiny handhold"),
                list("Hit 'em with the scenery", "Massive property damage", "If it doesn't fit, force it")));
        m.add(ps("Animal Avatar",
                list("Telepathy", "Frenzy", "Big shark teeth", "Swimming", "Shark sense of smell"),
                list("Tooth-shaped gouges", "Surprise Attack", "Ominous Circling")));
        m.add(ps("Master of Natural Force",
                list("Magnetic blast", "Move metal", "Become magnetism", "Force Field", "Metal shapes"),
                list("Control everything at once", "Trap opponent", "Indirect action")));
        m.add(ps("Mind Reader",
                list("Read minds", "Project sensations", "Ascend to mental plane", "Manipulate memories", "Mind control"),
                list("Subtle influence", "Know what they're planning", "Mass crowd influence")));
        m.add(ps("Magician",
                list("Bind forces/things", "Wards and shields", "Summon forces/things", "Unleash Artifacts of Power", "Know hidden nature of the world"),
                list("Mythic resonance", "Dramatic incantation", "Runes and Sigils")));
        m.add(ps("Shapeshifter",
                list("Change Shape", "Change Size", "Change physical state", "Strengths of current form", "Resilient body"),
                list("Shift out of danger", "Reach inaccessible places", "Surround an area")));
        m.add(ps("Robot",
                list("Gigantic Size", "Armor", "Rocket Feet", "Transform into Machine", "Missile Swarm"),
                list("Devices pop out from under skin", "Massive Property Damage", "Computer Brain")));
        m.add(ps("Teleporter",
                list("Appear", "Disappear", "Teleport someone else", "Appear, act, disappear", "Teleport a piece of something"),
                list("Impossible to target", "Appear behind someone", "Look at situation from many angles")));
        m.add(ps("Mimic",
                list("Imitate Something", "Mindset of form", "Strengths of form", "Weaken what you imitate", "Absorb attacks"),
                list("Camouflage", "Mix and match forms", "Adapt to changes")));
        m.add(ps("Shootist",
                list("Shoot", "Rapid fire", "Ricochet Shot", "Pinpoint accuracy", "Extreme range shot"),
                list("Shoot equipment", "Chain reaction", "Do other things while shooting")));
        return m;
    }

    public static List<ClickLockModule> skillSets() {
        List<ClickLockModule> m = new ArrayList<ClickLockModule>();
        m.add(ss("Homeless",
                list("Scrounge", "Accost", "Rant", "Obsess", "Lament"),
                list("Know hidden face of city", "Social invisibility", "Intuition for survival")));
        m.add(ss("Petty Crook",
                list("Connections", "Information", "Run", "Hide", "Streetwise"),
                list("Wheedle", "Deceive", "Try for the Big Score")));
        m.add(ss("C.E.O.",
                list("Delegate", "Glare", "Multi-task", "Negotiate", "Command"),
                list("The buck stops here", "Thrive on stress", "Authority")));
        m.add(ss("Politician",
                list("Lead", "Anticipate", "Simplify", "Organize", "Weasel"),
                list("Inspire Action", "Pass the buck", "Connections")));
        m.add(ss("General",
                list("Command", "Strategize", "Attack", "Delay", "Decide"),
                list("Call in a favor", "Chain of Command", "Diagrams on maps")));
        m.add(ss("Police",
                list("Communicate", "Cuffs", "Shoot", "Crowd control", "Fist-fight"),
                list("\"Freeze!\"", "Repeat yourself", "Cordon off area")));
        m.add(ss("Detective",
                list("Follow", "Notice", "Tactics", "Shoot", "Investigate"),
                list("Regulations", "Put two and two together", "Dogged persistence")));
        m.add(ss("Spook",
                list("Authority", "Secrecy", "Shoot", "Intimidate", "Deceive"),
                list("Contacts", "Resources", "Surveillance")));
        m.add(ss("Mook",
                list("Shoot", "Rough-house", "Boast", "Steal", "Outnumber"),
                list("Obedience", "Improvised Weapons", "Alpha Male")));
        m.add(ss("High-Tech Mook",
                list("Armor", "Blast-gun", "Jet-pack", "Boast", "Preparation"),
                list("Team tactics", "Endanger innocents", "Cut your losses")));
        m.add(ss("Kung Fu Fighter",
                list("Punch", "Kick", "Philosophy", "Wire-Fu", "Reflexes"),
                list("Shrug it off", "Rain of Blows", "Named Styles")));
        m.add(ss("Journalist",
                list("Photograph", "Write", "Investigate", "Interview", "Notice"),
                list("Pointed question", "Fast-talk", "Press Pass")));
        m.add(ss("Lawyer",
                list("Argue", "Advise", "Recite", "Bargain", "Interrogate"),
                list("Loopholes", "Books full of precedent", "Paperwork")));
        m.add(ss("Scientist",
                list("Invent", "Research", "Science", "Analyze", "Experiment"),
                list("\"That can't be right\"", "Propose Theory", "Examine sample")));
        m.add(ss("Bartender",
                list("Insight", "Be silent", "Serve Alcohol", "Folk Wisdom", "Ask"),
                list("Clean Glasses", "Another customer", "Non-verbal cues")));
        m.add(ss("Grease Monkey",
                list("Fix-It", "Invent", "Improvise", "Analyze", "Reverse Engineer"),
                list("Bang it with a wrench", "Unnecessary improvements", "Have tools handy")));
        m.add(ss("Dock Hand",
                list("Lift", "Throw", "Seamanship", "Carouse", "Stamina"),
                list("Rumors", "Camaraderie", "Machismo")));
        return m;
    }

    public static List<ClickLockModule> personae() {
        List<ClickLockModule> m = new ArrayList<ClickLockModule>();
        // Innocents (p.88)
        m.add(pe("Angsty Nice Guy",
                list("Shy", "Selfless", "Sincere", "Secretly hurting", "Trustworthy"),
                list("Wry humor", "Desperate Effort")));
        m.add(pe("Ingenue",
                list("Shocked", "Kind", "Trusting", "Curious", "Confused"),
                list("Smile lights up the room", "Miss the subtext")));
        // Walking Wounded (p.89)
        m.add(pe("Grim",
                list("Quiet", "Melancholy", "Untrusting", "Angry"),
                list("Snap without warning", "Reminders of the past")));
        m.add(pe("Ex-Victim",
                list("Nervous", "Apologetic", "Desperate", "Understanding", "Witty"),
                list("Worst Case Scenario", "Plan ahead")));
        m.add(pe("Guilt-Ridden",
                list("Neurotic", "Doubtful", "Remorseful", "Kind", "Impatient", "Distant"),
                list("Doubt yourself", "Brood")));
        // Idealists (p.90)
        m.add(pe("Curmudgeon",
                list("Honest", "Aggressive", "Put upon", "Demanding", "Sarcastic"),
                list("Brutal realism", "Scoff")));
        m.add(pe("Crusader",
                list("Inspired", "Determined", "Judgmental", "Frustrated", "Reckless"),
                list("Infectious energy", "Face down hypocrites")));
        m.add(pe("Older but Wiser",
                list("Insightful", "Preachy", "Judgmental", "Ironic", "Disappointed"),
                list("Dire Prediction", "Understand your limitations")));
        // Action Oriented (p.91)
        m.add(pe("Spunky Kid",
                list("Optimistic", "Embarrassed", "Childish", "Reckless", "Decisive"),
                list("Point out the obvious", "Exceed Expectations")));
        m.add(pe("Thrill Junky",
                list("Joyful", "Unflappable", "Bored", "Rebellious", "Talkative"),
                list("Escape without a scratch")));
        m.add(pe("Psychotic Loner",
                list("Hotshot", "Furious", "Denial", "Abrasive", "Uncomfortable", "Confident"),
                list("Screw the rules", "Intimidate")));
        // Incomplete (p.92)
        m.add(pe("Inhuman",
                list("Curious", "Confused", "Logical", "Cold", "Superior"),
                list("See the big picture", "Misunderstand humanity")));
        m.add(pe("Simple Soul",
                list("Happy", "Sad", "Angry", "Loving", "Slow"),
                list("Cut to the chase", "Simple Solutions")));
        m.add(pe("Sycophant",
                list("Spiteful", "Remorseful", "Sly", "Enthused", "Nervous"),
                list("Apologize", "Toady")));
        // Manipulators (p.93)
        m.add(pe("Seducer",
                list("Sensual", "Understanding", "Vindictive", "Sly", "Greedy"),
                list("Pout", "\"You know you want to\"")));
        m.add(pe("Puppet Master",
                list("Demanding", "Forgiving", "Stern", "Upset", "Proud"),
                list("Don't trust your lackeys", "Trust your lackeys")));
        m.add(pe("Charmer",
                list("Selfish", "Flexible", "Reluctant", "Confident", "Friendly"),
                list("Upset people's plans", "Fast-talk")));
        return m;
    }

    private static ClickLockModule ps(String name, List<String> powers, List<String> styles) {
        return new ClickLockModule(name, ClickLockModule.Type.POWER_SET, powers, styles);
    }
    private static ClickLockModule ss(String name, List<String> skills, List<String> styles) {
        return new ClickLockModule(name, ClickLockModule.Type.SKILL_SET, skills, styles);
    }
    private static ClickLockModule pe(String name, List<String> attitudes, List<String> styles) {
        return new ClickLockModule(name, ClickLockModule.Type.PERSONA, attitudes, styles);
    }
}
