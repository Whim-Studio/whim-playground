package com.whim.tarot.data;

import com.whim.tarot.domain.Card;
import com.whim.tarot.domain.DefaultCard;
import com.whim.tarot.domain.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The full 78-card Rider-Waite-Smith deck, hardcoded with authentic meanings and
 * public-domain Wikimedia Commons image URLs.
 *
 * <p>Card id ordering: 0..21 Major Arcana (Fool..World), then Wands 22..35,
 * Cups 36..49, Swords 50..63, Pentacles 64..77 (Ace..King within each suit).</p>
 */
public final class TarotDeckData implements CardRepository {

    private static final String CMN = "https://upload.wikimedia.org/wikipedia/commons/";

    private final List<Card> cards;
    private final Card[] byId;

    public TarotDeckData() {
        List<Card> list = new ArrayList<Card>(78);
        build(list);
        if (list.size() != 78) {
            throw new IllegalStateException("Expected 78 cards, built " + list.size());
        }
        Card[] arr = new Card[78];
        for (int i = 0; i < list.size(); i++) {
            Card c = list.get(i);
            if (c.getId() != i) {
                throw new IllegalStateException("Card at index " + i + " has id " + c.getId());
            }
            arr[i] = c;
        }
        this.cards = Collections.unmodifiableList(list);
        this.byId = arr;
    }

    public List<Card> getAllCards() { return cards; }

    public Card getById(int id) {
        if (id < 0 || id >= byId.length) {
            throw new IndexOutOfBoundsException("No card with id " + id);
        }
        return byId[id];
    }

    public int size() { return cards.size(); }

    private static void add(List<Card> list, int id, String name, Suit suit, int number,
                            String upright, String reversed, String description, String imgPath) {
        list.add(new DefaultCard(id, name, suit, number, upright, reversed, description, CMN + imgPath));
    }

    private static void build(List<Card> c) {
        // ---------------- Major Arcana (0..21) ----------------
        add(c, 0, "The Fool", Suit.MAJOR, 0,
            "New beginnings, spontaneity, innocence, and a leap of faith. A fresh start full of potential where you trust the journey and embrace the unknown.",
            "Recklessness, naivety, and foolish risk-taking. Hesitation at the cliff's edge, poor judgement, or fear that holds back a needed leap.",
            "A youth steps lightly toward a precipice, white rose and knapsack in hand, a small dog at his heels and the sun rising behind — the soul about to incarnate into experience.",
            "9/90/RWS_Tarot_00_Fool.jpg");
        add(c, 1, "The Magician", Suit.MAJOR, 1,
            "Manifestation, willpower, skill, and resourcefulness. You have all the tools you need to turn ideas into reality through focused intention.",
            "Manipulation, trickery, untapped talent, and scattered energy. Power misused or potential left idle through poor planning.",
            "A robed adept stands with one hand raised to heaven and one pointing to earth, the infinity sign above him and the four suit symbols on his table — channeling spirit into matter.",
            "d/de/RWS_Tarot_01_Magician.jpg");
        add(c, 2, "The High Priestess", Suit.MAJOR, 2,
            "Intuition, mystery, the subconscious, and inner wisdom. A call to trust your instincts and attend to the unseen and the unspoken.",
            "Secrets withheld, disconnection from intuition, and hidden agendas. Surface knowledge masking deeper truths you are ignoring.",
            "A serene priestess sits between the black and white pillars Boaz and Jachin, a crescent moon at her feet and a scroll of hidden law in her lap, veiled before the pomegranate curtain.",
            "8/88/RWS_Tarot_02_High_Priestess.jpg");
        add(c, 3, "The Empress", Suit.MAJOR, 3,
            "Abundance, fertility, nurturing, and creativity. A flourishing of nature, sensual pleasure, and motherly care that brings ideas and life to fruition.",
            "Creative block, dependence, smothering, and neglect. Stagnation or a disconnect from nurturing oneself and others.",
            "A crowned mother-figure reclines amid a wheat field and lush forest, the symbol of Venus beside her and a stream flowing nearby — the embodiment of earthly abundance.",
            "d/d2/RWS_Tarot_03_Empress.jpg");
        add(c, 4, "The Emperor", Suit.MAJOR, 4,
            "Authority, structure, stability, and leadership. The power of discipline, order, and a fatherly hand that builds lasting foundations.",
            "Tyranny, rigidity, domination, and loss of control. Authority turned overbearing, or a lack of discipline and direction.",
            "A stern ruler sits on a stone throne carved with ram's heads, holding an ankh scepter before barren mountains — worldly power founded on reason and will.",
            "c/c3/RWS_Tarot_04_Emperor.jpg");
        add(c, 5, "The Hierophant", Suit.MAJOR, 5,
            "Tradition, spiritual guidance, conformity, and shared belief. Learning through established institutions, mentors, and time-honored values.",
            "Rebellion, unconventionality, and dogma rejected. Breaking from convention or hollow ritual that no longer serves.",
            "A pope-like figure raises a hand in blessing between two pillars, two acolytes kneeling before him and crossed keys at his feet — the bridge between heaven and the faithful.",
            "8/8d/RWS_Tarot_05_Hierophant.jpg");
        add(c, 6, "The Lovers", Suit.MAJOR, 6,
            "Love, union, harmony, and meaningful choice. A heartfelt connection and alignment of values, or a crossroads requiring a decision of the heart.",
            "Disharmony, imbalance, misaligned values, and broken trust. A poor choice, temptation, or relationship friction.",
            "A man and woman stand beneath the angel Raphael, the Tree of Knowledge and the Tree of Life behind them in Eden — divine blessing upon love and conscious choice.",
            "d/db/RWS_Tarot_06_Lovers.jpg");
        add(c, 7, "The Chariot", Suit.MAJOR, 7,
            "Determination, willpower, victory, and control. Forward momentum achieved by reining opposing forces toward a single goal.",
            "Lack of direction, loss of control, and aggression. Scattered drive or being pulled apart by competing urges.",
            "An armored victor rides a chariot drawn by a black and a white sphinx, a starry canopy above and a walled city behind — mastery of opposing forces through will.",
            "9/9b/RWS_Tarot_07_Chariot.jpg");
        add(c, 8, "Strength", Suit.MAJOR, 8,
            "Courage, inner strength, patience, and compassion. Taming raw instinct with gentleness and a calm, confident heart.",
            "Self-doubt, weakness, and raw emotion unchecked. Insecurity or force used where patience was needed.",
            "A calm woman gently closes the jaws of a lion, the infinity sign above her head and a garland of flowers about her — soft power mastering the beast within.",
            "f/f5/RWS_Tarot_08_Strength.jpg");
        add(c, 9, "The Hermit", Suit.MAJOR, 9,
            "Introspection, solitude, soul-searching, and inner guidance. Withdrawing to seek wisdom and light your own way forward.",
            "Isolation, loneliness, and withdrawal. Excessive seclusion or rejecting the guidance you need.",
            "A cloaked elder stands alone on a snowy peak, lifting a lantern lit by a six-pointed star and leaning on a staff — the seeker who lights the path for others.",
            "4/4d/RWS_Tarot_09_Hermit.jpg");
        add(c, 10, "Wheel of Fortune", Suit.MAJOR, 10,
            "Cycles, fate, turning points, and good fortune. The wheel turns toward change and opportunity; destiny is in motion.",
            "Bad luck, resistance to change, and cycles broken. A downturn or clinging to control over what fate decides.",
            "A great wheel inscribed with mystic letters turns amid clouds, flanked by the sphinx, serpent, and Anubis, with the four living creatures reading at the corners — the ever-turning cycle of fortune.",
            "3/3c/RWS_Tarot_10_Wheel_of_Fortune.jpg");
        add(c, 11, "Justice", Suit.MAJOR, 11,
            "Fairness, truth, cause and effect, and accountability. Decisions weighed honestly; you reap what you have sown.",
            "Injustice, dishonesty, and lack of accountability. Unfairness, bias, or evading the consequences of one's actions.",
            "A robed judge sits between two pillars holding an upraised sword and balanced scales — impartial truth that weighs every action and renders its due.",
            "e/e0/RWS_Tarot_11_Justice.jpg");
        add(c, 12, "The Hanged Man", Suit.MAJOR, 12,
            "Surrender, new perspective, pause, and letting go. Suspending action to see the situation from a wholly different angle.",
            "Stalling, needless sacrifice, and resistance. Stagnation from refusing to release or shift your viewpoint.",
            "A man hangs serenely by one foot from a living T-cross, hands behind his back and a halo about his head — willing surrender that brings enlightenment.",
            "2/2b/RWS_Tarot_12_Hanged_Man.jpg");
        add(c, 13, "Death", Suit.MAJOR, 13,
            "Endings, transformation, transition, and release. The close of one chapter clearing the ground for profound renewal.",
            "Resistance to change, stagnation, and fear of endings. Clinging to what must pass and stalling inevitable transformation.",
            "A skeletal knight in black armor rides a white horse bearing a black banner of the white rose, as king, child, and bishop meet him and the sun rises between two towers — death as the gateway to rebirth.",
            "d/d7/RWS_Tarot_13_Death.jpg");
        add(c, 14, "Temperance", Suit.MAJOR, 14,
            "Balance, moderation, patience, and synthesis. Blending opposites with a calm, measured hand to find the middle path.",
            "Imbalance, excess, and impatience. Discord, overindulgence, or a failure to find harmony.",
            "A winged angel pours water between two cups, one foot on land and one in a stream, an iris-lined path leading to a crowned mountain — the alchemy of perfect balance.",
            "f/f8/RWS_Tarot_14_Temperance.jpg");
        add(c, 15, "The Devil", Suit.MAJOR, 15,
            "Bondage, addiction, materialism, and shadow. Attachments and unhealthy patterns that bind you, often by your own consent.",
            "Release, breaking free, and reclaiming power. Confronting fear, detaching from what enslaves you, and regaining control.",
            "A horned Baphomet looms over a chained man and woman beneath an inverted torch — the bondage of base desire, its loose chains hinting the captives could free themselves.",
            "5/55/RWS_Tarot_15_Devil.jpg");
        add(c, 16, "The Tower", Suit.MAJOR, 16,
            "Sudden upheaval, revelation, chaos, and awakening. A shocking collapse that tears down false structures to reveal the truth.",
            "Fear of change, delaying disaster, and averted catastrophe. Resisting a needed collapse or clinging to crumbling ground.",
            "Lightning strikes a tall tower and topples its crown, two figures plunging headlong into the flames below — the sudden destruction of what was built on falsehood.",
            "5/53/RWS_Tarot_16_Tower.jpg");
        add(c, 17, "The Star", Suit.MAJOR, 17,
            "Hope, renewal, inspiration, and serenity. Healing calm after the storm and faith that the future holds light.",
            "Despair, disconnection, and lost faith. Discouragement, self-doubt, or feeling cut off from hope.",
            "A nude figure kneels by a pool beneath a great star and seven smaller ones, pouring water onto land and into the water — renewal, faith, and the quiet return of hope.",
            "d/db/RWS_Tarot_17_Star.jpg");
        add(c, 18, "The Moon", Suit.MAJOR, 18,
            "Illusion, intuition, dreams, and the subconscious. Uncertainty where things are not as they seem; trust instinct through the fog.",
            "Confusion lifting, released fear, and clarity returning. Hidden truths surfacing and anxiety beginning to fade.",
            "A pale moon weeps dew over a path between two towers, a dog and a wolf baying as a crayfish crawls from the pool — the realm of dream, fear, and the deep unconscious.",
            "7/7f/RWS_Tarot_18_Moon.jpg");
        add(c, 19, "The Sun", Suit.MAJOR, 19,
            "Joy, success, vitality, and positivity. Warmth, clarity, and the radiant assurance that all will be well.",
            "Temporary gloom, blocked joy, and over-optimism. Clouded happiness or a delay in the success that is still coming.",
            "A radiant sun beams over a child riding a white horse before a wall of sunflowers, a red banner raised high — pure joy, vitality, and the triumph of light.",
            "1/17/RWS_Tarot_19_Sun.jpg");
        add(c, 20, "Judgement", Suit.MAJOR, 20,
            "Rebirth, reckoning, awakening, and absolution. A profound call to rise renewed, evaluate the past, and answer your higher purpose.",
            "Self-doubt, refusal of the call, and harsh self-judgement. Ignoring an inner summons or being trapped by past regret.",
            "An angel sounds a trumpet from the clouds as the dead rise from their coffins with arms uplifted — the great awakening and call to a higher life.",
            "d/dd/RWS_Tarot_20_Judgement.jpg");
        add(c, 21, "The World", Suit.MAJOR, 21,
            "Completion, fulfillment, wholeness, and accomplishment. The successful close of a great cycle and a sense of integration and triumph.",
            "Incompletion, loose ends, and delayed closure. A goal nearly reached but stalled by unfinished business.",
            "A dancing figure wreathed in a laurel garland holds two wands, framed by the four living creatures at the corners — the harmonious completion of the journey.",
            "f/ff/RWS_Tarot_21_World.jpg");

        // ---------------- Wands (22..35) ----------------
        add(c, 22, "Ace of Wands", Suit.WANDS, 1,
            "Inspiration, creative spark, new ventures, and raw potential. A surge of energy igniting a fresh project or passion.",
            "Delays, blocked creativity, and false starts. Lost motivation or an idea that fails to catch fire.",
            "A hand emerges from a cloud grasping a sprouting wand above a green landscape and distant castle — the seed of creative fire offered fresh.",
            "1/11/Wands01.jpg");
        add(c, 23, "Two of Wands", Suit.WANDS, 2,
            "Planning, foresight, personal power, and decisions. Surveying your options and charting a bold path beyond the familiar.",
            "Fear of the unknown, indecision, and playing it safe. Plans stalled by hesitation or a lack of vision.",
            "A robed figure holds a globe and gazes from a castle wall over land and sea, a second wand fixed beside him — the world weighed before a journey is chosen.",
            "0/0f/Wands02.jpg");
        add(c, 24, "Three of Wands", Suit.WANDS, 3,
            "Expansion, foresight, progress, and looking ahead. Early efforts paying off as you await ships coming in.",
            "Delays, obstacles, and lack of foresight. Plans frustrated or expansion stalling unexpectedly.",
            "A figure stands on a cliff watching ships sail across the sea, three wands planted around him — enterprise launched and horizons widening.",
            "f/ff/Wands03.jpg");
        add(c, 25, "Four of Wands", Suit.WANDS, 4,
            "Celebration, harmony, home, and joyful milestones. A happy gathering, stability, and well-earned festivity.",
            "Tension at home, transition, and lack of harmony. Postponed celebration or instability beneath the surface.",
            "A garlanded canopy stands on four wands as two figures raise bouquets before a festive castle — homecoming and shared rejoicing.",
            "a/a4/Wands04.jpg");
        add(c, 26, "Five of Wands", Suit.WANDS, 5,
            "Conflict, competition, rivalry, and tension. A scramble of clashing wills, disagreements, or spirited contest.",
            "Avoiding conflict, resolution, and inner tension. Quarrels ending or strife suppressed rather than settled.",
            "Five youths brandish wands in a chaotic mock-battle, each striking a different angle — the friction of competing energies.",
            "9/9d/Wands05.jpg");
        add(c, 27, "Six of Wands", Suit.WANDS, 6,
            "Victory, public recognition, success, and confidence. Triumph acknowledged and well-deserved acclaim.",
            "Egotism, fall from grace, and lack of recognition. Private doubt, delayed reward, or pride before a stumble.",
            "A laurel-crowned rider parades on a white horse amid a cheering crowd, a victory wreath on his wand — public acclaim for a hard-won win.",
            "3/3b/Wands06.jpg");
        add(c, 28, "Seven of Wands", Suit.WANDS, 7,
            "Defiance, perseverance, standing your ground, and courage. Holding the high position against challengers.",
            "Overwhelm, giving up, and yielding ground. Exhaustion from defending or a position abandoned under pressure.",
            "A man on higher ground wields his wand against six rising from below — the defense of conviction against many challengers.",
            "e/e4/Wands07.jpg");
        add(c, 29, "Eight of Wands", Suit.WANDS, 8,
            "Swift movement, momentum, news, and rapid progress. Events accelerating and messages arriving fast.",
            "Delays, frustration, and scattered energy. Stalled momentum or haste that misfires.",
            "Eight wands fly in parallel through open sky over a green valley and river — swift action speeding to its target.",
            "6/6b/Wands08.jpg");
        add(c, 30, "Nine of Wands", Suit.WANDS, 9,
            "Resilience, persistence, last stand, and boundaries. Battle-worn but unbroken, guarding what you have gained.",
            "Exhaustion, paranoia, and defensiveness. Wearing down, distrust, or refusing to lower the guard.",
            "A bandaged figure leans warily on his wand before a fence of eight others — weary vigilance before the final push.",
            "e/e7/Wands09.jpg");
        add(c, 31, "Ten of Wands", Suit.WANDS, 10,
            "Burden, responsibility, hard work, and overload. Carrying a heavy load near the goal, perhaps too much alone.",
            "Release, delegation, and collapse under strain. Setting down burdens or buckling beneath them.",
            "A man struggles forward bearing all ten wands in his arms toward a distant town — the weight of obligations carried to the end.",
            "0/0b/Wands10.jpg");
        add(c, 32, "Page of Wands", Suit.WANDS, 11,
            "Enthusiasm, exploration, free spirit, and discovery. An eager messenger of new ideas and adventurous beginnings.",
            "Hesitation, restlessness, and immature haste. Scattered enthusiasm or news delayed.",
            "A youth in a salamander-patterned tunic regards his flowering wand amid desert dunes — curiosity ablaze with possibility.",
            "6/6a/Wands11.jpg");
        add(c, 33, "Knight of Wands", Suit.WANDS, 12,
            "Action, adventure, passion, and impulsiveness. Charging boldly toward a goal with fiery energy.",
            "Recklessness, haste, and frustration. Impatience, anger, or a venture that fizzles for lack of follow-through.",
            "An armored knight on a rearing horse charges forward, wand in hand, salamanders on his tunic — passionate momentum in full gallop.",
            "1/16/Wands12.jpg");
        add(c, 34, "Queen of Wands", Suit.WANDS, 13,
            "Confidence, warmth, courage, and vivacity. A magnetic, determined spirit who inspires and leads with charisma.",
            "Jealousy, insecurity, and demanding moods. Self-doubt or fiery temperament turned inward.",
            "A crowned queen sits with a sunflower and her wand, a black cat at her feet — radiant confidence and warm-hearted authority.",
            "0/0d/Wands13.jpg");
        add(c, 35, "King of Wands", Suit.WANDS, 14,
            "Leadership, vision, boldness, and entrepreneurship. A natural-born leader who turns inspired vision into bold action.",
            "Impulsiveness, domineering, and overbearing pride. Vision without patience or leadership turned ruthless.",
            "A robed king holds a flowering wand on a throne adorned with lions and salamanders — visionary command and mature creative fire.",
            "c/ce/Wands14.jpg");

        // ---------------- Cups (36..49) ----------------
        add(c, 36, "Ace of Cups", Suit.CUPS, 1,
            "New love, emotional awakening, compassion, and overflowing feeling. The heart opening to joy, intuition, and connection.",
            "Blocked emotions, emptiness, and repressed feelings. Love withheld or a heart closed off.",
            "A hand from a cloud offers a chalice overflowing with five streams as a dove descends with the host — the wellspring of love and spirit.",
            "3/36/Cups01.jpg");
        add(c, 37, "Two of Cups", Suit.CUPS, 2,
            "Partnership, mutual attraction, union, and connection. A balanced bond of love, friendship, or harmony between two.",
            "Imbalance, breakup, and disharmony. Tension, miscommunication, or a fractured connection.",
            "A man and woman pledge their cups beneath the caduceus and winged lion's head — the sacred union of two hearts.",
            "f/f8/Cups02.jpg");
        add(c, 38, "Three of Cups", Suit.CUPS, 3,
            "Friendship, celebration, community, and joy. Coming together to rejoice with friends and shared good fortune.",
            "Overindulgence, gossip, and isolation. Excess, frayed friendships, or a third party intruding.",
            "Three women raise their cups in a joyful dance amid a bountiful harvest — fellowship and celebration.",
            "7/7a/Cups03.jpg");
        add(c, 39, "Four of Cups", Suit.CUPS, 4,
            "Apathy, contemplation, discontent, and withdrawal. Disengaged and brooding, overlooking what is offered.",
            "New awareness, acceptance, and re-engagement. Boredom lifting and renewed openness to opportunity.",
            "A youth sits beneath a tree with arms crossed, ignoring a cup offered by a cloud-borne hand — discontent blind to fresh offerings.",
            "3/35/Cups04.jpg");
        add(c, 40, "Five of Cups", Suit.CUPS, 5,
            "Loss, grief, regret, and disappointment. Mourning what spilled while two cups still stand behind you.",
            "Acceptance, recovery, and moving on. Healing from loss and turning toward what remains.",
            "A cloaked figure grieves over three spilled cups, two upright behind him, a bridge to a castle beyond the river — sorrow that overlooks remaining hope.",
            "d/d7/Cups05.jpg");
        add(c, 41, "Six of Cups", Suit.CUPS, 6,
            "Nostalgia, innocence, memories, and reunion. The sweetness of the past, childhood joy, and kindness shared.",
            "Clinging to the past, stuck in memory, and naivety. Living in nostalgia or refusing to grow up.",
            "A child offers a flower-filled cup to another in a sunlit courtyard — the warmth of memory and innocent generosity.",
            "1/17/Cups06.jpg");
        add(c, 42, "Seven of Cups", Suit.CUPS, 7,
            "Choices, fantasy, illusion, and wishful thinking. A dazzle of options where not all is as it appears.",
            "Clarity, decisiveness, and disillusion. Cutting through fantasy to choose with focus.",
            "A figure faces seven cups floating in cloud, each bearing a different vision — jewels, a wreath, a dragon, a shrouded figure — the temptations of imagination.",
            "a/ae/Cups07.jpg");
        add(c, 43, "Eight of Cups", Suit.CUPS, 8,
            "Walking away, withdrawal, seeking deeper meaning, and abandonment. Leaving behind what no longer fulfills to search for more.",
            "Fear of moving on, aimlessness, and stagnation. Clinging to an empty situation or drifting without purpose.",
            "A cloaked traveler turns away from eight stacked cups, journeying toward mountains under an eclipsed moon — the brave departure toward something deeper.",
            "6/60/Cups08.jpg");
        add(c, 44, "Nine of Cups", Suit.CUPS, 9,
            "Contentment, satisfaction, wishes fulfilled, and emotional well-being. The 'wish card' of comfort and gratitude.",
            "Smugness, overindulgence, and unfulfilled wishes. Complacency or seeking happiness in excess.",
            "A satisfied man sits with arms folded before an arc of nine cups — emotional fulfillment and the granting of a wish.",
            "2/24/Cups09.jpg");
        add(c, 45, "Ten of Cups", Suit.CUPS, 10,
            "Harmony, happy family, lasting joy, and emotional fulfillment. The bliss of love, home, and togetherness.",
            "Broken home, disconnection, and misaligned values. Domestic discord or a picture-perfect ideal unmet.",
            "A couple raises their arms toward a rainbow of ten cups as two children dance, a cottage by the river beyond — the fullness of shared happiness.",
            "8/84/Cups10.jpg");
        add(c, 46, "Page of Cups", Suit.CUPS, 11,
            "Creative inspiration, intuition, sensitivity, and gentle messages. A dreamy messenger of new feelings and imaginative ideas.",
            "Emotional immaturity, escapism, and creative block. Moodiness or feelings ignored.",
            "A young page in a flowered tunic regards a fish surfacing from his cup beside a wavy sea — imagination and tender intuition awakening.",
            "a/ad/Cups11.jpg");
        add(c, 47, "Knight of Cups", Suit.CUPS, 12,
            "Romance, charm, idealism, and following the heart. A poetic messenger pursuing love and beauty.",
            "Moodiness, unrealistic ideals, and disappointment. Charm masking inconsistency or fantasy over substance.",
            "A gentle knight rides slowly bearing a cup, winged helm and heels, a river winding to the hills — the romantic in pursuit of his ideal.",
            "f/fa/Cups12.jpg");
        add(c, 48, "Queen of Cups", Suit.CUPS, 13,
            "Compassion, emotional depth, intuition, and nurturing. A loving, empathic soul attuned to feeling and care.",
            "Emotional insecurity, overwhelm, and dependence. Feelings turned overwhelming or boundaries lost.",
            "A queen gazes into an ornate lidded cup upon a throne at the water's edge — deep empathy and the wisdom of the heart.",
            "6/62/Cups13.jpg");
        add(c, 49, "King of Cups", Suit.CUPS, 14,
            "Emotional balance, compassion, diplomacy, and calm control. Mastery of feeling — steady, wise, and caring under pressure.",
            "Emotional manipulation, moodiness, and volatility. Feelings repressed or used to control others.",
            "A king sits enthroned on a turbulent sea holding a cup and scepter, calm amid the waves — emotional mastery steady within the storm.",
            "0/04/Cups14.jpg");

        // ---------------- Swords (50..63) ----------------
        add(c, 50, "Ace of Swords", Suit.SWORDS, 1,
            "Clarity, breakthrough, truth, and mental power. A sharp new insight cutting through confusion to reveal the truth.",
            "Confusion, miscommunication, and misused force. Clouded thinking or a breakthrough turned destructive.",
            "A hand from a cloud grips an upright sword crowned with laurel above stark peaks — the triumph of clear thought and decisive truth.",
            "1/1a/Swords01.jpg");
        add(c, 51, "Two of Swords", Suit.SWORDS, 2,
            "Difficult choices, stalemate, indecision, and avoidance. A tense impasse where you refuse to see the way forward.",
            "Indecision resolved, information revealed, and release. The blindfold lifting and a choice finally made.",
            "A blindfolded woman holds two crossed swords before a moonlit sea — a guarded heart at an uneasy stalemate.",
            "9/9e/Swords02.jpg");
        add(c, 52, "Three of Swords", Suit.SWORDS, 3,
            "Heartbreak, sorrow, painful truth, and grief. Emotional pain, betrayal, or loss that pierces the heart.",
            "Recovery, forgiveness, and releasing pain. Healing after heartbreak and the easing of grief.",
            "Three swords pierce a red heart against a stormy, rain-swept sky — sorrow and the sharp clarity of painful truth.",
            "0/02/Swords03.jpg");
        add(c, 53, "Four of Swords", Suit.SWORDS, 4,
            "Rest, recovery, contemplation, and respite. A needed pause to heal and restore before re-engaging.",
            "Restlessness, burnout, and stagnation. Pushing on without rest or avoiding necessary recovery.",
            "A knight lies in repose atop a tomb, three swords on the wall and one beneath him, hands in prayer — stillness and recuperation.",
            "b/bf/Swords04.jpg");
        add(c, 54, "Five of Swords", Suit.SWORDS, 5,
            "Conflict, defeat, hollow victory, and discord. Winning at a cost, or loss amid tension and ill will.",
            "Reconciliation, making amends, and releasing resentment. Moving past conflict toward repair.",
            "A smirking man gathers swords as two defeated figures walk away beneath a ragged sky — a victory that leaves only emptiness.",
            "2/23/Swords05.jpg");
        add(c, 55, "Six of Swords", Suit.SWORDS, 6,
            "Transition, moving on, recovery, and safe passage. Leaving troubled waters for calmer shores.",
            "Stuck, resistance to change, and unfinished business. Unable to move on or carrying baggage along.",
            "A ferryman poles a boat bearing a cloaked woman and child across still water toward a far shore, six swords upright in the hull — passage from turmoil to calm.",
            "2/29/Swords06.jpg");
        add(c, 56, "Seven of Swords", Suit.SWORDS, 7,
            "Deception, strategy, stealth, and getting away. Acting alone or by cunning, sometimes dishonestly.",
            "Confession, conscience, and coming clean. Deception exposed or a change of heart.",
            "A man tiptoes from a camp carrying five swords with a sly backward glance, two left behind — cunning, secrecy, and the lone gambit.",
            "3/34/Swords07.jpg");
        add(c, 57, "Eight of Swords", Suit.SWORDS, 8,
            "Restriction, feeling trapped, self-limiting beliefs, and powerlessness. A prison largely of the mind's own making.",
            "Self-acceptance, freedom, and new perspective. Releasing the bonds and stepping into your own power.",
            "A bound, blindfolded woman stands hemmed by eight swords before a distant castle — restriction that the mind could yet escape.",
            "a/a7/Swords08.jpg");
        add(c, 58, "Nine of Swords", Suit.SWORDS, 9,
            "Anxiety, worry, fear, and sleepless nights. The torment of the mind in the darkness, often worse than reality.",
            "Hope returning, releasing worry, and recovery. Anxiety easing as fears are faced in the light.",
            "A figure sits up in bed, face in hands, nine swords mounted on the wall behind — the anguish of fear and midnight dread.",
            "2/2f/Swords09.jpg");
        add(c, 59, "Ten of Swords", Suit.SWORDS, 10,
            "Painful ending, rock bottom, betrayal, and collapse. The worst has happened, but with it comes finality and dawn.",
            "Recovery, regeneration, and the worst behind you. Slowly rising from a painful end toward renewal.",
            "A fallen figure lies pierced by ten swords along the shore as a golden dawn breaks on the horizon — a definitive ending and the promise of a new day.",
            "d/d4/Swords10.jpg");
        add(c, 60, "Page of Swords", Suit.SWORDS, 11,
            "Curiosity, vigilance, new ideas, and mental energy. A keen, restless mind hungry for truth and quick to act.",
            "Deception, haste, and scattered thoughts. Gossip, recklessness, or cutting words.",
            "A youth stands on a windswept ridge brandishing a sword aloft, hair and clouds streaming — alert, inquisitive intellect.",
            "4/4c/Swords11.jpg");
        add(c, 61, "Knight of Swords", Suit.SWORDS, 12,
            "Ambition, drive, decisive action, and bold ideas. Charging headlong toward a goal with relentless focus.",
            "Recklessness, impatience, and aggression. Rushing in without thought or trampling others in haste.",
            "An armored knight charges full tilt on a galloping horse, sword raised against a wild sky — the rush of focused, headstrong intellect.",
            "b/b0/Swords12.jpg");
        add(c, 62, "Queen of Swords", Suit.SWORDS, 13,
            "Clear thinking, independence, honesty, and perception. A sharp, fair mind that sees through illusion and speaks the truth.",
            "Coldness, bitterness, and harsh judgement. Detachment turned cruel or cutting criticism.",
            "A queen sits enthroned above the clouds, sword upright and one hand outstretched — incisive clarity tempered by experience.",
            "d/d4/Swords13.jpg");
        add(c, 63, "King of Swords", Suit.SWORDS, 14,
            "Authority, intellect, truth, and ethical judgement. Clear, principled leadership guided by reason and fairness.",
            "Tyranny, manipulation, and cold rationality. Intellect used to dominate or judgement without compassion.",
            "A king sits frontally enthroned holding an upright sword, butterflies and crescent on his throne — disciplined intellect and impartial authority.",
            "3/33/Swords14.jpg");

        // ---------------- Pentacles (64..77) ----------------
        add(c, 64, "Ace of Pentacles", Suit.PENTACLES, 1,
            "New opportunity, prosperity, abundance, and manifestation. A tangible new beginning in money, work, or material security.",
            "Lost opportunity, scarcity, and poor planning. A promising venture squandered or material insecurity.",
            "A hand from a cloud presents a golden pentacle above a flowering garden with an archway to distant mountains — a seed of prosperity offered.",
            "f/fd/Pents01.jpg");
        add(c, 65, "Two of Pentacles", Suit.PENTACLES, 2,
            "Balance, adaptability, juggling priorities, and time management. Keeping multiple demands in graceful motion.",
            "Overwhelm, disorganization, and dropped balls. Too many commitments and losing the juggle.",
            "A young man dances while juggling two pentacles bound by an infinity loop as ships ride the waves behind — adaptable balance amid life's ups and downs.",
            "9/9f/Pents02.jpg");
        add(c, 66, "Three of Pentacles", Suit.PENTACLES, 3,
            "Teamwork, collaboration, skill, and craftsmanship. Building something of quality through cooperation and expertise.",
            "Disharmony, poor teamwork, and mediocrity. Misaligned effort or skills going unrecognized.",
            "A craftsman is consulted by a monk and a noble as he works in a cathedral — the union of skill, planning, and collaboration.",
            "4/42/Pents03.jpg");
        add(c, 67, "Four of Pentacles", Suit.PENTACLES, 4,
            "Security, control, saving, and holding on. Guarding resources tightly, sometimes to the point of possessiveness.",
            "Greed loosening, generosity, and letting go. Releasing a grip on wealth or, conversely, financial instability.",
            "A crowned figure clutches one pentacle to his chest, stands on two, and balances one on his head before a city — security held in a fearful grip.",
            "3/35/Pents04.jpg");
        add(c, 68, "Five of Pentacles", Suit.PENTACLES, 5,
            "Hardship, loss, insecurity, and isolation. Material or spiritual lack and the feeling of being left out in the cold.",
            "Recovery, renewed hope, and help arriving. Coming through hard times and finding support.",
            "Two destitute figures trudge through snow past a lit stained-glass window — poverty and the warmth that waits just out of reach.",
            "9/96/Pents05.jpg");
        add(c, 69, "Six of Pentacles", Suit.PENTACLES, 6,
            "Generosity, giving and receiving, charity, and balance. The fair flow of resources between those who have and have not.",
            "Strings attached, inequality, and debt. Imbalanced giving, dependency, or charity that controls.",
            "A merchant weighs coins on a scale while dispensing alms to two kneeling beggars — the balanced exchange of giving and receiving.",
            "a/a6/Pents06.jpg");
        add(c, 70, "Seven of Pentacles", Suit.PENTACLES, 7,
            "Patience, investment, perseverance, and long-term view. Pausing to assess the slow growth of your efforts.",
            "Impatience, poor return, and wasted effort. Frustration at slow progress or a failing investment.",
            "A farmer leans on his hoe regarding seven pentacles ripening on a leafy vine — patient labor awaiting its harvest.",
            "6/6a/Pents07.jpg");
        add(c, 71, "Eight of Pentacles", Suit.PENTACLES, 8,
            "Diligence, skill development, mastery, and dedication. Focused, repeated effort that hones true craftsmanship.",
            "Perfectionism, uninspired work, and shortcuts. Tedium, sloppiness, or skill misapplied.",
            "An apprentice carves pentacle after pentacle at his bench, finished coins displayed beside him — devoted practice toward mastery.",
            "4/49/Pents08.jpg");
        add(c, 72, "Nine of Pentacles", Suit.PENTACLES, 9,
            "Abundance, self-sufficiency, luxury, and reward. Enjoying the fruits of independent effort in comfort and grace.",
            "Over-dependence, financial setback, and hollow luxury. Self-worth tied to wealth or comfort threatened.",
            "An elegant woman stands in a lush vineyard with a hooded falcon on her gloved hand — refined self-made abundance.",
            "f/f0/Pents09.jpg");
        add(c, 73, "Ten of Pentacles", Suit.PENTACLES, 10,
            "Wealth, legacy, family, and lasting security. Enduring prosperity, inheritance, and the stability of established foundations.",
            "Financial loss, family disputes, and instability. Legacy threatened or fleeting material gain.",
            "An elder with hounds watches three generations beneath an archway hung with ten pentacles — established wealth and lasting legacy.",
            "4/42/Pents10.jpg");
        add(c, 74, "Page of Pentacles", Suit.PENTACLES, 11,
            "Ambition, study, new opportunity, and diligence. A grounded learner planting seeds for future prosperity.",
            "Procrastination, lack of progress, and missed chances. Ideas left unrealized or studies neglected.",
            "A youth gazes intently at a pentacle held aloft in a green, flowering field — the student of tangible ambitions.",
            "e/ec/Pents11.jpg");
        add(c, 75, "Knight of Pentacles", Suit.PENTACLES, 12,
            "Reliability, hard work, routine, and responsibility. Steady, methodical effort that sees commitments through.",
            "Stagnation, boredom, and stubbornness. Routine turned to rut or progress stalled by inertia.",
            "An armored knight sits motionless on a sturdy horse, holding a pentacle above plowed fields — patient, dependable diligence.",
            "d/d5/Pents12.jpg");
        add(c, 76, "Queen of Pentacles", Suit.PENTACLES, 13,
            "Nurturing, practicality, abundance, and security. A warm, capable provider who tends home, work, and others with grounded care.",
            "Self-neglect, smothering, and work-life imbalance. Care withheld from oneself or material worries overwhelming.",
            "A queen cradles a pentacle on a throne carved with fruit and goats, a rabbit by her feet in a verdant bower — bountiful, down-to-earth care.",
            "8/88/Pents13.jpg");
        add(c, 77, "King of Pentacles", Suit.PENTACLES, 14,
            "Prosperity, leadership, security, and discipline. A successful, grounded provider who has mastered the material world.",
            "Greed, materialism, and stubborn control. Wealth hoarded or success turned to corruption.",
            "A robed king sits amid grapevines, scepter and pentacle in hand, a castle and bull carvings about him — abundant, stable mastery of the material realm.",
            "1/1c/Pents14.jpg");
    }
}
