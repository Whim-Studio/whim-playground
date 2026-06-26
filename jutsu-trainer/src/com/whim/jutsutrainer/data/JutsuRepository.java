package com.whim.jutsutrainer.data;

import com.whim.jutsutrainer.domain.ChakraNature;
import com.whim.jutsutrainer.domain.HandSeal;
import com.whim.jutsutrainer.domain.Jutsu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * In-memory catalog of authentic Naruto jutsu with accurate, ordered hand-seal
 * sequences. Built once in the constructor; {@link #all()} exposes an
 * unmodifiable view.
 */
public final class JutsuRepository {

    private final List<Jutsu> jutsu;

    public JutsuRepository() {
        List<Jutsu> list = new ArrayList<Jutsu>();
        build(list);
        this.jutsu = Collections.unmodifiableList(list);
    }

    /** Unmodifiable list of every catalogued {@link Jutsu}. */
    public List<Jutsu> all() {
        return jutsu;
    }

    private static List<HandSeal> seq(HandSeal... seals) {
        return Arrays.asList(seals);
    }

    private void build(List<Jutsu> out) {
        // ---- FIRE ----------------------------------------------------------
        out.add(new Jutsu(
                "Fire Style: Fireball Jutsu", ChakraNature.FIRE, "C", "Sasuke Uchiha",
                "Kneads chakra in the chest and expels a massive orb of roaring flame. A rite of passage for the Uchiha clan.",
                seq(HandSeal.SNAKE, HandSeal.RAM, HandSeal.MONKEY, HandSeal.BOAR, HandSeal.HORSE, HandSeal.TIGER)));

        out.add(new Jutsu(
                "Fire Style: Phoenix Sage Fire Jutsu", ChakraNature.FIRE, "C", "Sasuke Uchiha",
                "Launches a volley of small, uncontrollable fireballs in every direction, often concealing shuriken within the flames.",
                seq(HandSeal.RAT, HandSeal.TIGER, HandSeal.DOG, HandSeal.OX, HandSeal.HARE, HandSeal.TIGER)));

        out.add(new Jutsu(
                "Fire Style: Dragon Flame Jutsu", ChakraNature.FIRE, "B", "Sasuke Uchiha",
                "Sends a powerful, focused jet of fire travelling along a wire or projectile toward the target.",
                seq(HandSeal.SNAKE, HandSeal.DRAGON, HandSeal.HARE, HandSeal.TIGER)));

        out.add(new Jutsu(
                "Fire Style: Great Fire Annihilation", ChakraNature.FIRE, "A", "Madara Uchiha",
                "Expels an enormous, sustained wall of flame from the mouth to scorch a wide area.",
                seq(HandSeal.SNAKE, HandSeal.HORSE, HandSeal.TIGER)));

        out.add(new Jutsu(
                "Fire Style: Flame Bullet", ChakraNature.FIRE, "B", "Jiraiya",
                "Spits chakra-laced oil and ignites it, producing an intensely hot torrent of flame.",
                seq(HandSeal.SNAKE, HandSeal.RAM, HandSeal.MONKEY, HandSeal.BOAR, HandSeal.HORSE, HandSeal.TIGER)));

        // ---- WATER ---------------------------------------------------------
        out.add(new Jutsu(
                "Water Style: Water Dragon Jutsu", ChakraNature.WATER, "B", "Tobirama Senju",
                "Shapes a colossal dragon of water that crashes into the enemy with overwhelming force. Tobirama famously formed it with a single seal, but the standard sequence is the longest known: forty-four seals.",
                seq(
                        HandSeal.OX, HandSeal.MONKEY, HandSeal.HARE, HandSeal.RAT, HandSeal.BOAR, HandSeal.BIRD,
                        HandSeal.OX, HandSeal.HORSE, HandSeal.BIRD, HandSeal.RAT, HandSeal.TIGER, HandSeal.DOG,
                        HandSeal.TIGER, HandSeal.SNAKE, HandSeal.OX, HandSeal.MONKEY, HandSeal.HARE, HandSeal.RAT,
                        HandSeal.BOAR, HandSeal.BIRD, HandSeal.OX, HandSeal.HORSE, HandSeal.BIRD, HandSeal.RAT,
                        HandSeal.TIGER, HandSeal.DOG, HandSeal.TIGER, HandSeal.SNAKE, HandSeal.OX, HandSeal.MONKEY,
                        HandSeal.HARE, HandSeal.RAT, HandSeal.BOAR, HandSeal.BIRD, HandSeal.OX, HandSeal.HORSE,
                        HandSeal.BIRD, HandSeal.RAT, HandSeal.TIGER, HandSeal.DOG, HandSeal.TIGER, HandSeal.SNAKE,
                        HandSeal.HORSE, HandSeal.TIGER)));

        out.add(new Jutsu(
                "Water Style: Water Wall", ChakraNature.WATER, "B", "Tobirama Senju",
                "Raises a circular rampart of surging water to block incoming attacks from any direction.",
                seq(HandSeal.TIGER, HandSeal.SNAKE, HandSeal.RAT, HandSeal.SNAKE, HandSeal.TIGER)));

        out.add(new Jutsu(
                "Water Style: Water Bullet Jutsu", ChakraNature.WATER, "C", "Kisame Hoshigaki",
                "Fires a high-pressure projectile of water—sometimes shaped like a shark—from the mouth.",
                seq(HandSeal.TIGER, HandSeal.OX, HandSeal.HARE)));

        out.add(new Jutsu(
                "Water Style: Exploding Water Shockwave", ChakraNature.WATER, "B", "Kisame Hoshigaki",
                "Floods the battlefield with a vast volume of water, creating an ocean Kisame can fight from freely.",
                seq(HandSeal.TIGER, HandSeal.SNAKE, HandSeal.RAM, HandSeal.MONKEY, HandSeal.SNAKE)));

        out.add(new Jutsu(
                "Water Style: Water Prison Jutsu", ChakraNature.WATER, "C", "Zabuza Momochi",
                "Traps the target inside a sphere of inescapable, unbreakable water, held in place by the caster's hand.",
                seq(HandSeal.SNAKE, HandSeal.RAM, HandSeal.HORSE, HandSeal.BIRD)));

        out.add(new Jutsu(
                "Water Style: Demon Wave", ChakraNature.WATER, "B", "Hashirama Senju",
                "Channels a tremendous surge of water that sweeps across the field and overwhelms the enemy.",
                seq(HandSeal.OX, HandSeal.HARE, HandSeal.TIGER)));

        // ---- LIGHTNING -----------------------------------------------------
        out.add(new Jutsu(
                "Chidori (Lightning Blade)", ChakraNature.LIGHTNING, "A", "Kakashi Hatake",
                "Concentrates lightning chakra into the hand with a sound like a thousand chirping birds, then drives it through the target in a high-speed thrust.",
                seq(HandSeal.OX, HandSeal.HARE, HandSeal.MONKEY)));

        out.add(new Jutsu(
                "Lightning Style: False Darkness", ChakraNature.LIGHTNING, "B", "Kakuzu",
                "Emits one or more piercing spears of lightning from the mouth, capable of skewering through stone.",
                seq(HandSeal.SNAKE, HandSeal.HORSE)));

        out.add(new Jutsu(
                "Lightning Style: Purple Electricity", ChakraNature.LIGHTNING, "A", "Hiruzen Sarutobi",
                "Gathers a crackling blade of violet lightning along the arm for a devastating slashing strike.",
                seq(HandSeal.HARE, HandSeal.MONKEY, HandSeal.HARE, HandSeal.RAT, HandSeal.OX)));

        out.add(new Jutsu(
                "Lightning Style: Lightning Beast Running Technique", ChakraNature.LIGHTNING, "A", "Darui",
                "Forms a hound of lightning that races along the ground and savages the enemy on contact.",
                seq(HandSeal.RAT, HandSeal.TIGER, HandSeal.DOG)));

        // ---- EARTH ---------------------------------------------------------
        out.add(new Jutsu(
                "Earth Style: Earth Wall", ChakraNature.EARTH, "B", "Tobirama Senju",
                "Raises a thick rampart of rock from the ground to shield against incoming attacks.",
                seq(HandSeal.TIGER, HandSeal.HARE, HandSeal.BOAR, HandSeal.DOG)));

        out.add(new Jutsu(
                "Earth Style: Swamp of the Underworld", ChakraNature.EARTH, "A", "Jiraiya",
                "Turns the ground into a vast bottomless bog that swallows even giant summons.",
                seq(HandSeal.RAT, HandSeal.BOAR, HandSeal.SNAKE, HandSeal.BIRD, HandSeal.DOG, HandSeal.TIGER)));

        out.add(new Jutsu(
                "Earth Style: Hiding Like a Mole", ChakraNature.EARTH, "D", "Onoki",
                "Softens the earth to tunnel swiftly underground, evading attacks and ambushing from below.",
                seq(HandSeal.RAM, HandSeal.SNAKE)));

        out.add(new Jutsu(
                "Earth Style: Mud Wall", ChakraNature.EARTH, "A", "Hashirama Senju",
                "Spews a torrent of mud that hardens into a massive defensive barrier across the field.",
                seq(HandSeal.TIGER, HandSeal.HARE, HandSeal.RAT, HandSeal.DOG, HandSeal.SNAKE)));

        // ---- WIND ----------------------------------------------------------
        out.add(new Jutsu(
                "Wind Style: Great Breakthrough", ChakraNature.WIND, "C", "Orochimaru",
                "Exhales a sudden, sweeping gale strong enough to flatten trees and scatter foes.",
                seq(HandSeal.RAT, HandSeal.RAM, HandSeal.MONKEY, HandSeal.BOAR, HandSeal.HORSE, HandSeal.TIGER)));

        out.add(new Jutsu(
                "Wind Style: Vacuum Sphere", ChakraNature.WIND, "B", "Hiruzen Sarutobi",
                "Fires a rapid barrage of compressed air bullets, each capable of punching through rock.",
                seq(HandSeal.SNAKE, HandSeal.RAM, HandSeal.MONKEY, HandSeal.SNAKE, HandSeal.RAM, HandSeal.TIGER)));

        out.add(new Jutsu(
                "Wind Style: Rasenshuriken", ChakraNature.WIND, "S", "Naruto Uzumaki",
                "Infuses a Rasengan with wind chakra to form a shuriken-shaped blade that shreds the target at the cellular level. Cast freeform—no hand seals.",
                Collections.<HandSeal>emptyList()));

        // ---- YIN–YANG / SPECIAL --------------------------------------------
        out.add(new Jutsu(
                "Shadow Clone Jutsu", ChakraNature.YIN_YANG, "B", "Naruto Uzumaki",
                "Creates fully solid, independent clones that evenly divide the caster's chakra. Formed with the crossed-finger Clone Seal.",
                seq(HandSeal.CLONE_SEAL)));

        out.add(new Jutsu(
                "Clone Jutsu", ChakraNature.NONE, "E", "Academy Standard",
                "Produces intangible illusory copies of the user to confuse opponents. A basic Academy graduation technique.",
                seq(HandSeal.RAM, HandSeal.SNAKE, HandSeal.TIGER)));

        out.add(new Jutsu(
                "Summoning Jutsu", ChakraNature.NONE, "C", "Jiraiya",
                "Signs a blood contract to summon an allied creature—toad, snake, slug, and more—from another location.",
                seq(HandSeal.BOAR, HandSeal.DOG, HandSeal.BIRD, HandSeal.MONKEY, HandSeal.RAM)));

        out.add(new Jutsu(
                "Summoning: Impure World Reincarnation (Edo Tensei)", ChakraNature.YIN_YANG, "S", "Orochimaru",
                "A forbidden kinjutsu that resurrects the dead in near-immortal bodies, bound to a living sacrifice. Sealed with clasped hands.",
                seq(HandSeal.TIGER, HandSeal.SNAKE, HandSeal.DOG, HandSeal.CLAP)));

        out.add(new Jutsu(
                "Transformation Jutsu", ChakraNature.NONE, "E", "Academy Standard",
                "Wreathes the user in chakra to take on the exact appearance of another person or object. A basic Academy technique.",
                seq(HandSeal.DOG, HandSeal.BOAR, HandSeal.RAM)));

        out.add(new Jutsu(
                "Reaper Death Seal", ChakraNature.YIN_YANG, "S", "Minato Namikaze",
                "Summons the Shinigami to tear the target's soul from its body and seal it away, at the cost of the caster's own life.",
                seq(HandSeal.SNAKE, HandSeal.BOAR, HandSeal.HARE, HandSeal.DOG, HandSeal.RAT, HandSeal.BIRD, HandSeal.HORSE, HandSeal.TIGER, HandSeal.CLAP)));

        out.add(new Jutsu(
                "Demonic Illusion: Hell Viewing", ChakraNature.YIN, "C", "Kurenai Yuhi",
                "A genjutsu that traps the victim in a vision of their greatest fear, paralysing them with dread.",
                seq(HandSeal.RAT, HandSeal.OX, HandSeal.SNAKE, HandSeal.MONKEY, HandSeal.RAM, HandSeal.BOAR)));

        out.add(new Jutsu(
                "Temple of Nirvana", ChakraNature.YIN, "A", "Gennō",
                "A wide-area genjutsu that lulls everyone caught within a falling shower of feathers into deep sleep.",
                seq(HandSeal.RAT, HandSeal.SNAKE, HandSeal.RAM)));

        out.add(new Jutsu(
                "Wood Style: Deep Forest Emergence", ChakraNature.YANG, "S", "Hashirama Senju",
                "Hashirama's signature Wood Release, combining earth and water natures to grow a sprawling forest in an instant.",
                seq(HandSeal.SNAKE, HandSeal.RAM, HandSeal.RAT, HandSeal.BOAR, HandSeal.DOG, HandSeal.TIGER)));

        // ---- TAIJUTSU / SEAL-LESS & ONE-HANDED -----------------------------
        out.add(new Jutsu(
                "Rasengan", ChakraNature.NONE, "A", "Naruto Uzumaki",
                "A dense, spinning sphere of pure chakra ground into the target. Requires no hand seals—only supreme chakra control.",
                Collections.<HandSeal>emptyList()));

        out.add(new Jutsu(
                "Eight Trigrams Sixty-Four Palms", ChakraNature.NONE, "—", "Neji Hyuga",
                "A Gentle Fist assault that strikes sixty-four of the body's chakra points in rapid succession, sealing the target's flow.",
                Collections.<HandSeal>emptyList()));

        out.add(new Jutsu(
                "Front Lotus", ChakraNature.NONE, "B", "Rock Lee",
                "A forbidden taijutsu opening the first inner gate to suplex the airborne target into the ground at lethal speed.",
                Collections.<HandSeal>emptyList()));

        out.add(new Jutsu(
                "Crystal Ice Mirrors", ChakraNature.WATER, "S", "Haku",
                "Haku's Ice Release kekkei genkai—formed with one-handed seals—surrounds the enemy with mirrors he travels between at blinding speed.",
                seq(HandSeal.SNAKE_HALF, HandSeal.HALF_RAM, HandSeal.HALF_TIGER)));

        out.add(new Jutsu(
                "Thousand Flying Water Needles of Death", ChakraNature.WATER, "B", "Haku",
                "Freezes ambient moisture into a storm of senbon-shaped ice needles, launched with one-handed seals.",
                seq(HandSeal.SNAKE_HALF, HandSeal.HALF_TIGER)));

        out.add(new Jutsu(
                "Sexy Jutsu", ChakraNature.NONE, "E", "Naruto Uzumaki",
                "A comedic Transformation variant turning the user into an alluring figure to startle opponents.",
                seq(HandSeal.CLONE_SEAL)));

        out.add(new Jutsu(
                "Multi Shadow Clone Jutsu", ChakraNature.YIN_YANG, "A", "Naruto Uzumaki",
                "A forbidden mass-production version of the Shadow Clone, spawning hundreds or thousands of solid clones at once.",
                seq(HandSeal.CLONE_SEAL)));
    }
}
