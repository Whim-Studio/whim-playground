# Sword of the Samurai — Game Design Reference

A design-reference reconstruction of MicroProse's **Sword of the Samurai**, assembled from the
original game manual and corroborating secondary sources. This document is intended as a factual
basis for a clean-room recreation: it describes the systems, numbers, controls, and win/loss rules
of the original as accurately as the sources allow.

> **IMPORTANT CORRECTION ON DATE / AUTHORSHIP.** The task brief describes the game as a *1992*
> title "designed by the MicroProse team." The verified record is that **Sword of the Samurai was
> released in 1989** (with later 2014–2015 GOG/Steam/Tommo re-releases). It was designed and led by
> **Lawrence Schick** (game design / project leader / manual text), programmed by **Jim Synoski**
> (role-playing program) with **John Kennedy** (melee program) and **David McKibbin** (battle
> program), art-directed by **Michael Haire**, with music by **Jeffery L. Briggs**. **Sid Meier**
> (MicroProse co-owner) personally helped develop the **NPC AI and the dueling**, following the
> success of *Sid Meier's Pirates!*. Where this document says "the original," it means the 1989
> MS-DOS release.
> Sources: manual credits page (Steam CDN manual, see Sources §9);
> https://en.wikipedia.org/wiki/Sword_of_the_Samurai_(1989_video_game)

Primary source for the great majority of the mechanical detail below is the **official printed
manual**, *Sword of the Samurai* (MicroProse Software, Inc., 1989), recovered as the Steam re-release
PDF: `https://cdn.cloudflare.steamstatic.com/steam/apps/327950/manuals/Manual.pdf`. Direct manual
quotations are marked with quotation marks. Every non-manual claim is cited inline. Anything that
could not be verified is tagged **`[UNVERIFIED — best-effort reconstruction]`**.

---

## 0. Snapshot / Elevator Pitch

- **Genre:** genre-blending action + strategy + role-playing "life simulation." The box billed it as
  "a role-playing action-adventure simulation."
  (https://www.mobygames.com/game/1250/sword-of-the-samurai/;
  https://www.gog.com/game/sword_of_the_samurai)
- **Setting:** the **Sengoku Jidai** ("Age of the Country at War"), feudal Japan, ~1490–1600. The
  manual's own history section dates the period "from around 1490 to 1600."
- **Premise:** You are a young samurai, head of a leading family in one of Japan's clans. You rise
  **gokenin (vassal samurai) → hatamoto (lieutenant) → daimyo (warlord) → Shogun (ruler of all
  Japan)** by balancing **Honor** and **Power**, fighting duels and battles personally, and
  outmaneuvering rivals through diplomacy — or treachery.
- **The map:** **48 provinces** of feudal Japan across the lower three main islands (Honshu, Shikoku,
  Kyushu). (Manual: "a map of Japan and its forty-eight provinces";
  https://www.gog.com/game/sword_of_the_samurai)
- **Spiritual ancestor:** widely compared to *Pirates!* — a framing "life" loop punctuated by
  arcade action sequences. Computer Gaming World said it "offers even more than its spiritual
  ancestor, Pirates!" (https://en.wikipedia.org/wiki/Sword_of_the_Samurai_(1989_video_game))
- **Central tension (manual, verbatim):** "You must learn when to leave your sword in its sheath,
  when to draw it — and what to do with it once it's drawn." And: "in all cultures power corrupts —
  and those who seek absolute power don't always play by the rules."

---

## 1. Core Game Loop

### 1.1 What you are and where you start
You begin as a **young samurai of age 15** who "has just celebrated gempuku, the coming of age," now
head of "one of the leading samurai families in your clan." The glossary explicitly states: "the
player's character starts the game as a **gokenin**" — a responsible but lower-level clan samurai
(a vassal). You control a **fief**: "villages and farmlands," mostly rice land, and you "tax the
peasants for a percentage of the rice harvest." You maintain a small number of **retainers** (lesser
samurai loyal to you); "the number of warriors you can maintain is strictly limited by how much
agricultural wealth you command."

Above you sits your **hatamoto** (lieutenant lord), and above him the **daimyo** (Great Lord, ruler
of the province). You have **rival vassals** in the same clan — technically allies, actually
competitors — all angling to be promoted to hatamoto when the current one dies.

### 1.2 The clan hierarchy (the political stack you climb)
```
Emperor (in Kyoto, Omi province) — grants the title of Shogun, but wields no real power in this era
   └── Shogun (military ruler of all Japan)      ← the ultimate goal
         └── Daimyo (rules one province / clan)  ← mid-game rank
               └── Hatamoto (lieutenant)         ← first promotion goal
                     └── Gokenin / vassal samurai ← YOU start here
                           └── your own retainers (lesser samurai)
                                 └── ashigaru (peasant foot-soldiers) + peasants
```
(Structure from the manual's "Overview" and "The Clan and Your Place in It"; ranks corroborated by
https://steamcommunity.com/sharedfiles/filedetails/?id=639819146 which lists **Vassal → Hatamoto →
Daimyo** as the playable rank ladder.)

### 1.3 The turn / time model
The game plays as a **continuous stream of "role-playing scrolls"** rather than discrete numbered
turns. At each **decision point** the game "presents you with a scroll listing your available
options"; you pick an action, it resolves, time advances, and the next decision point appears. There
is no fixed board-game "turn."

- **Day granularity is implied:** "Every **morning** your spymaster prepares a scroll summarizing
  current intelligence" (the **Status Scroll**). This suggests the underlying clock advances roughly
  a day per cycle of decisions. **`[UNVERIFIED — best-effort reconstruction]`** the exact length of
  a time step and whether it is uniform.
- **Real-time action interludes:** whenever combat, travel, or a mission triggers, the game drops out
  of the scroll layer into a **real-time arcade sequence** (duel, melee, or battle) played from the
  keyboard, then returns to the scrolls. GOG/retro descriptions call the arcade layers "real-time
  battles" and "furious melee action." (https://www.gog.com/game/sword_of_the_samurai)
- **Seasons / calendar:** the manual does **not** describe explicit seasons, named months, or a
  visible calendar HUD. Rice-harvest taxation and aging clearly imply passing years, but the exact
  season/seasonal-event system is **`[UNVERIFIED — best-effort reconstruction]`**. Do not invent a
  four-seasons cycle as canon.
- **Aging is real:** "If you become aged, you will find that your physical skills begin to decrease
  markedly." Eventually "you will be too old to continue and will have no choice but to retire."
  Aging is the clock that forces succession (see §4, §5).

### 1.4 The map / province structure
Two map scales exist depending on rank:

1. **Province (local) map** — used as a **vassal/hatamoto**. When you travel, "a map of the province
   appears, indicating estates, castles, roads and rivers, towns and villages, mountains, shrines,
   and seas." You are a "small samurai figure" walking the terrain; **movement speed varies by
   terrain** ("faster on roads, slower in mountains and crossing rivers"). Each visitable location is
   labeled with its lord's name. Housing encodes rank: **samurai live in houses, hatamoto in manors,
   daimyo in castles.** Border **arrows** let you walk into adjacent provinces.
2. **Strategic (national) map** — unlocked as a **daimyo**. Press the Strategic Map key to see
   "Japan and its forty-eight provinces." Your province is highlighted; each province shows its name
   and ruler. **Unaligned provinces are gray**; **ambitious daimyo's provinces are colored, one
   color per daimyo**, and a conquered province "changes color to match" its new owner. At this scale
   you travel and conquer province-to-province rather than walking a single province.

### 1.5 The menu of actions ("Home Options")
At a home decision point the option scroll offers actions such as (manual, "Home Options" and later
sections; "the options are not listed in any order of preference"):

- **Equip Samurai** — raise more retainers, if your rice wealth supports them. Recruitment rate
  scales with your **honor** ("All samurai wish to serve a lord of high honor").
- **Practice Kenjutsu** — train swordsmanship with your fencing master. There is a **cap on
  practice**: "At some point you may find that you have learned all you can from practice fighting.
  After that, there's only one way to improve to your maximum potential — by winning actual duels."
- **Drill Your Troops** — improve **generalship** (unit discipline, formations, morale). Also capped:
  "beyond a certain point there's no substitute for battlefield experience."
- **Raise the Rice Tax** — more income, but "a mildly dishonorable action," and if pushed too far
  "they may revolt."
- **Donate Land to the Temple** — "a sign of a respectful and honorable samurai" (raises honor) but
  "may decrease the number of warriors you can maintain."
- **Travel** — to your lord's castle (to volunteer for deeds), to a rival's estate, or to another
  province. Choose to travel **alone**, **alone and disguised (as a ronin)**, or **with your troops**
  (see §1.6).
- **Marriage / family actions** — accept a matchmaker, ask for a neighbor's daughter, etc. (§5).
- **Diplomacy** — Tea Ceremony (befriend a hostile peer), Help a Neighbor (bring troops to his
  defense).
- **Intimidation / Coercion / Treachery / Assassination** — the dishonorable power actions (§3, §6).
- **Retire** or **Commit Seppuku** — end-of-life actions (§4, §5).
- As **daimyo**: **Conquer a Neighboring Province**, and eventually **Declare Yourself Shogun**.

### 1.6 Travel mode — a core strategic choice
Your travel mode changes what encounters do and what you can attempt:

| Mode | Home defense | Honor from road fights | Purpose |
|---|---|---|---|
| **Alone** | retainers stay home to defend | **maximum** honor (you fought unaided) | volunteer for bold deeds, visit rivals |
| **Alone, disguised (ronin)** | retainers stay home | **little/no** honor (nobody knows it was you) | sneaking: rescue/kidnap, coercion, incite rebellion, treachery, assassination |
| **With troops** | **estate left largely undefended** | glory via campaigns | campaigns, helping a neighbor, army attacks |

"It's far more impressive if you take on a gang of bandits alone than if you are backed up by your
army." Traveling with troops "leaves your estate largely undefended against treacherous attacks" —
a real risk, because rivals prefer to strike your family "when you are away from home."

### 1.7 The two status readouts
- **Status Scroll** — "images of you and your rivals in the order of your current status"; select a
  portrait to see that person's assets (honor, land, troops, generalship, swordsmanship).
- **Summary Scroll** — "a quick look at what your rivals are up to and what opportunities for
  advancement are available."
- In the DOS UI these map to function keys: **F1** = personal/area character overview (icons: sun =
  honor, rice plant = fief, helmet = warriors raised, war-fan = generalship, sword = swordsmanship);
  **F2** = overview of Japan (matters most as daimyo); **F3** = available quests / enemy activity
  (bold deeds and campaign opportunities; disguised-enemy warnings).
  (https://steamcommunity.com/sharedfiles/filedetails/?id=460177975)

---

## 2. The Four Sub-Games / Arcade Sequences

Sword of the Samurai has **three distinct action engines** — a **Duel**, a **Melee**, and a
**Battle** — each written by a different programmer and each selectable standalone as an "Encounter"
from the title screen "for practicing the various types of combat." (Manual, "Getting Started";
credits list separate Duel/Melee/Battle programs.) The "ninja / castle infiltration / assassination"
sequence and any "mounted/ambush" moments are **variants of the Melee engine**, not separate games.
So: one duel game, one battle game, and the melee game that covers stealth, home-defense, hostage
rescue, and assassination.

### 2.1 (a) SWORD DUEL — "Crossing Swords for Honor"
A **one-on-one, side-view katana duel** triggered when honor demands a formal single combat (you
insult a rival, a rival insults you, you meet a samurai lord face-to-face, or an alarmed
assassination target wakes up). Your samurai stands facing right toward the opponent; you "always
face toward your opponent's end of the duel area."

**Controls (manual's generic "controller"/"selector" terms, with DOS keys from the Steam controls
guide, https://steamcommunity.com/sharedfiles/filedetails/?id=460177975):**
- **Move:** push the controller (arrow keys / number pad) without a selector; pushing *back* backs
  you away.
- **Attack:** hold **selector #1** and push a direction (DOS: hold **Enter** + direction). The rhythm
  is "back – forward – back – forward, windup – hack – windup – hack." You can strike **straight**,
  **left**, or **right**, and can **slash crosswise** (move to center-left/right, then slash across).
- **Charged over-the-shoulder strike:** pull the sword all the way back over your shoulder (DOS:
  hold **Enter + down/2** to draw the sword back, then **up/8** to release). This stance is "more
  vulnerable (harder to parry from), but a swing from over the shoulder **cannot be parried**," has
  more force, and does more damage. The Steam tip notes it is unblockable and does **+damage** (the
  community guide calls it 2 damage vs 1). **`[UNVERIFIED — best-effort reconstruction]`** the exact
  per-hit damage numbers; the manual counts *wounds*, not damage points.
- **Side/shoulder swings:** **1 / 3** draw the sword over one shoulder to strike enemies to a side.
- **Parry:** hold **selector #2** + a direction (DOS: **Backspace** to defend) to hold a **left,
  right, or center** parry; you "automatically attempt to parry any attack" on the defended side.
  Critically, "**you can only parry an attack that comes on the side you are defending**" — a left
  parry does nothing against a right-side attack.

**Win / lose:**
- Each hit is a **wound**, shown on a wound indicator (yours upper-left, opponent's upper-right).
  A wound "knocks you back a couple of steps" and briefly stops you attacking.
- **"When either combatant takes four wounds, he falls and the duel is over."** (Duels are, in
  effect, **to four wounds** — closer to "to the death/incapacitation" than a single first-blood
  touch.)
- **Carry-over penalty:** "if you were wounded in a melee that directly preceded a duel, you will
  start the duel with one wound."
- **Running away:** moving off the **lower edge** ends the duel by fleeing — "such cowardly behavior
  is very dishonorable." (Hovering near the bottom edge risks being *mistaken* for a coward.)
- **Key vulnerability (manual hint):** "You are vulnerable when your sword is down, so never leave
  your sword at the bottom of a swing — pull it up immediately! You can't parry with your sword point
  stuck in the ground."

**Honor implications:** winning a duel you were dragged into (or one you provoked over a genuine
insult) raises honor; fleeing craters it. Assassination targets who are already awake must be
**dueled to the death** rather than killed in their sleep (see melee, below).

### 2.2 (b) BATTLE — "The Art of War" (army combat)
A **top-down real-time army battle**, used for campaigns (as vassal/hatamoto), succession fights
between hatamoto factions, usurping the daimyo, and province conquest/defense (as daimyo). You
command **units** of 1–8 figures, not a single man.

**Pre-battle — formation selection:** Every battle has an attacker and a defender; the enemy has
already picked, and your choices are limited to **attack** or **defense** formations accordingly:
- **Attack formations:** **Hoshi** (Arrowhead — concentrate the center to overrun/split),
  **Kakuyoku** (Crane's Wing — heavy units on both flanks to envelop), **Katana** (Long Sword —
  massed strength on one flank for a hammer blow).
- **Defense formations:** **Ganko** (Birds in Flight — strong center, light flanks + archers; counters
  Hoshi), **Koyaku** (The Yoke — strength on the flanks to swallow a Hoshi/Kakuyoku), **Engetsu**
  (Half Moon — power on one flank; counters a same-side Katana).
- Press up/down to cycle the three formations; press left/right to **mirror** them → "six choices of
  initial formation in every battle." Attacker formations draw preview lines showing each unit's
  standard march route.

**In-battle controls (Steam controls guide):** move the general's war-fan cursor with arrows/number
keys; press **1–8** to select and halt that unit; then draw a line to a destination and press **+**
or **-** to issue the move. **Space** pauses. You give **destinations, not targets** — "if they
encounter the enemy on the way to your chosen destination, they will fight," so to hit a moving enemy
you must plot an **intercept**. Order types: **Turn and March** (fast, faces the enemy), **March
Without Turning** (slow reposition keeping facing), **Turn Without Marching** (rotate in place).

**Unit types:**
- **Infantry** (ashigaru spearmen led by samurai) — slowest, cross any terrain, shaky vs cavalry
  charge.
- **Archers** — faster than infantry; stop at range and volley; refuse close combat with superior
  units and withdraw in open formation.
- **Cavalry** (all samurai) — strongest and fastest in the open; slowed in woods, **won't enter
  marsh**; **vulnerable to massed musket fire**.
- **Musketeers** (ashigaru with arquebuses; only at higher skill levels / largest armies) —
  devastating massed close-range volleys, especially vs cavalry; slow reload leaves them helpless up
  close.

**Unit scale by rank:** each figure represents **6 soldiers** as a samurai, **60** as a hatamoto,
**250** as a daimyo.

**Combat resolution:** units fight automatically on contact (or in range for missiles) until one is
"damaged so much that it has no chance to win," then it **routs**. Damage depends on: **Strength**
(numbers), **Facing** (flank/rear attacks do far more — "to be caught facing away from the enemy is
a calamity"), **Generalship** (better-trained armies hit harder), **Morale**, **Terrain** (attacking
uphill/from streams/marsh is weaker), **Range**, and **Luck**. **Morale** rises with high
generalship, outnumbering, and routing the enemy; falls with heavy damage, friendly routers passing
by, and flank/rear attacks. **Terrain:** woods hide units (great for flanking; beware hidden enemy
formations); streams/slopes/marsh slow and weaken; cavalry avoid woods (slow) and marsh (won't
enter).

**Win / lose:** "When all units on one side have routed or been destroyed the other side wins." You
can concede by ordering all units off the battlefield edge — a cowardly, honor-losing move some
generals prefer "to the loss of an entire army." Losing a province-defense battle as daimyo means
you **swear allegiance to the victor and the game is over** (see §8).

### 2.3 (c) MELEE — "One Against a Thousand" (incl. ninja / castle infiltration / assassination)
The **top-down single-hero action engine**: you are one samurai against a stream of enemies. This
one engine covers bandit fights, fief defense, home-defense against raiders, **hostage rescue**,
**kidnapping**, **inciting rebellion**, **treachery**, and **assassination**.

**Controls:** move by pushing the controller (no selector). **Swing sword:** press selector while
facing an in-range enemy (you auto-draw when an enemy is close and auto-face the nearest; with
multiple in range, push toward the one you want). **Shoot bow:** press selector while facing an
enemy *beyond* sword range (you auto-nock at long range). DOS: **Enter** fires arrows / attacks.
You **cannot parry arrows or musket balls** (you can parry swords and spears). Swings that cross at
the same instant are mutually parried.

**Win / lose:** "If you are wounded twice, you fall and the melee ends." (Melee is **2 wounds**, vs
the duel's 4.) First wound halves your movement speed. Most enemies die in one hit; some tougher ones
take two. Leaving the melee area = running away = cowardice/dishonor — **except** three sanctioned
exits: (1) leaving a castle after your mission is accomplished, (2) leaving *before* guards have
identified you on a stealth mission, (3) fleeing a travel encounter while disguised.

**Enemy roster & tactics:** **Swordsmen** (fast, deadly in tight castle rooms), **Spearmen** (outrange
you; beat them by closing inside the spear point around a corner), **Archers** (snipe from vantage;
helpless cornered), **Musketeers** (rare, higher levels only; unavoidable ball — never stand in front
during their long reload), and **Ninja** — "dressed in black … their stealth powers are such that you
won't even see them until they attack," armed with **swords and poisoned shuriken (throwing stars)**.
"If a ninja appears at a distance, he's throwing a shuriken — dodge left or right fast!" Fight ninja
in **small rooms** so they can only reach you one at a time.

**Castle infiltration (the "ninja/sneaking" sub-mode):** When you sneak into a rival's or enemy's
castle, only areas you have personally seen are visible ("fog of war"). Castle levels encode rank:
**samurai castles have 1 level, hatamoto 2, daimyo 3**; the objective is usually on the **top
level**, so "your first priority [is] finding the stairs" (up-stairs are light, down-stairs dark).
Two things raise the alarm: a guard getting a **good look at you for several seconds**, or a guard
**finding a corpse**. Once alarmed, guards "swarm out to look for you." Stealth tactics: hide in
rooms away from doors; peek-and-retreat around corners; lure an uncertain guard into a room and kill
him quietly so his body is hidden. **Daimyo third floors have "nightingale floors"** — floor sections
that "sing" (creak) when stepped on at night, revealing you; you must learn to spot them. (When you
*become* daimyo, your own third floor has nightingale floors that warn you of intruding ninja.)

**Mission variants running on the melee engine:**
- **Rescue a family member** (honorable): sneak in disguised, find the room, grab the hostage (press
  selector), carry them out; **you can't fight while carrying** — put them down, kill the guard, pick
  up, continue. If you meet the master of the house, honor forces a **duel**, so raid while he's
  away. If defeated/captured, the house lord may demand a **ransom**.
- **Take a hostage** (dishonorable): identical mechanics, opposite morality.
- **Incite a rebellion:** enter a rival's over-taxed village and **kill the tax collector** — "the
  only enemy in the village armed with a sword" — while his spear/bow thugs try to kill you; he flees,
  so you must chase him down. Success forces the rival to spend troops crushing the revolt.
- **Treachery (three flavors, each = ordered seppuku if caught):** **Treacherous Murder** (kill the
  daimyo's visiting envoy in the rival's house), **Treacherous Theft** (steal the heirloom sword the
  daimyo gifted the rival, shaming him at court), **Treacherous Frame-up** (plant enemy-clan documents
  in a sleeping guest's room to implicate the rival in treason).
- **Assassination (= ordered seppuku if caught):** reach the target's room **before the alarm** to
  kill him asleep; if the alarm has sounded he is awake and you must **duel him to the death**. Leaving
  any guard alive who can identify you = dishonor so great "you'll have no choice but to commit
  seppuku."
- **Home defense:** when a rival raids *you*, his ninja "divert, drug, or murder your guards, leaving
  you to handle the raid alone." You must stop him before he grabs your family/heirloom and reaches
  the exit stairs/door — a lucky bowshot can drop him and avoid a duel.
  (https://steamcommunity.com/sharedfiles/filedetails/?id=639819146)

### 2.4 (d) Mounted / ambush sequences
There is **no separate playable mounted-combat or ambush arcade game**. "Mounted" combat exists only
abstractly as **cavalry units inside the Battle engine** (§2.2). "Ambush" exists as (i) the manual's
historical flavor (e.g., Mori Motonari's midnight surprise attack) and (ii) **using woods to hide
units and flank** in a Battle, and (iii) disguised travel encounters where you're jumped by bandits.
Any claim of a dedicated horseback riding minigame is **`[UNVERIFIED — best-effort
reconstruction]`** and most likely false; the credits list only Duel, Melee, and Battle programs.

---

## 3. Honor vs Power — the Dual-Axis Reputation System

The whole game is a negotiation between **Honor** (bushido virtue, the daimyo's favor, your family's
long-term legacy) and **Power** (land/wealth, army size, and raw territorial control). The manual's
core statement of the promotion rule ties them together: when the hatamoto dies, "the daimyo will
choose the samurai who administers the **largest fief**, commands the **most warriors**, and, **most
importantly, who exhibits the greatest honor**. The daimyo knows that an honorable samurai is a loyal
samurai." Wikipedia summarizes this as "army size and honor have the greatest influence over how the
samurai are ranked." (https://en.wikipedia.org/wiki/Sword_of_the_Samurai_(1989_video_game))

### 3.1 The tracked assets (the "power+honor" stat block)
Every samurai (you and rivals) is rated on five things, shown as icons on the Status Scroll / F1:
1. **Honor** — rising-sun icon (the virtue axis).
2. **Land / fief size** — rice-plant icon (wealth → how many warriors you can field).
3. **Army / warriors** — helmet icon (military power).
4. **Generalship** — war-fan icon (battle skill).
5. **Swordsmanship** — sword icon (duel/melee skill).
(Icons per https://steamcommunity.com/sharedfiles/filedetails/?id=460177975 and manual "Selecting a
Clan"/"Status Scroll." Note the four *clan* strengths at character creation are honor, generalship,
swordsmanship, land; army size grows in play.)

### 3.2 How you GAIN honor
- **Winning duels** you were honorably drawn into.
- **Bold deeds:** single-handedly wiping out **bandit gangs / brigand lairs**, and other "outrages"
  the lord announces (volunteer at his castle). "Feats of arms" are the samurai path to honor.
- **Defending your fief** against invaders, and **helping a neighbor** repel his invaders.
- **Marrying up** — "if you manage to marry a woman from a family whose honor exceeds your own, your
  reputation may markedly improve."
- **Donating land to the temple** (at the cost of some troop capacity).
- **Coercing a rival to publicly proclaim his esteem for you** (adds honor — but this is itself a
  coercive act enabled by hostage-holding).
- **Refusing to be cowed:** if you're insulted and you challenge the insulter to a duel; conversely a
  rival who **backs down from your insult** loses honor.
- **Dying honorably:** "if you have an heir, your family will go on even if you die, and your son will
  benefit from the honor you achieved in death." Honor is partly a **family/dynastic** score, not just
  a personal one.

### 3.3 How you LOSE honor
- **Cowardice** — fleeing a duel, melee, or battle off the edge of the field.
- **Raising the rice tax** — "considered a mildly dishonorable action" (robbing your own peasants).
- **Marrying down** — "if you marry a woman whose family has a reputation far below yours, your own
  reputation will suffer."
- **Failing to defend** — losing your fief to invaders permanently shrinks it and shames you.
- **Being caught in dishonorable acts** — kidnapping, coercion, inciting rebellion, treachery,
  assassination. "Eventually, one who engages in such dishonorable behavior is certain to be caught."
- **Backing down** from an insult; **attacking an ally unjustly** (see below).

### 3.4 The Power path (dishonorable but effective) — and its cost
"Some samurai … prefer to avoid the risks of deadly combat and try to get ahead by dragging others
down." These **power-through-treachery** tools raise your relative standing (by lowering rivals or
seizing their assets) but risk your honor and life:
- **Kidnapping / hostage-taking** — deters a rival from attacking you and enables coercion.
- **Coercion** — force a hostage-holding victim to lend you **income (→ land wealth)** or proclaim
  **esteem (→ honor)**.
- **Inciting rebellion** — makes a rival burn troops (→ his power drops).
- **Treachery** (murder of envoy / theft of heirloom / frame-up) — shames a rival before the daimyo.
- **Assassination** — removes a rival entirely.

**The honor/power trade is explicitly asymmetric and dangerous:** treachery and assassination, "if
you are caught attempting" them, get you **ordered to commit seppuku** — and refusing that order gets
you and your **entire family hunted down and killed** (instant game over). **Attacking a fellow
clansman openly** without sufficient justification gets your lord to rule the attack unjustified and
"**you will be seriously dishonored**"; worse, if that rival holds your family hostage, "the hostage
will probably be killed." So Power moves are high-variance: they can vault you up the ladder or end
your dynasty.

### 3.5 How honor & power interact with the Shogunate and the final score
- **Reaching hatamoto/daimyo** rewards the honor-heavy climb (favor of the lord).
- **Reaching Shogun** shifts the emphasis to **Power** — "to become daimyo required courage and
  craftiness; **to become Shogun your main tool is military conquest**." As daimyo you can even
  **delegate** duels/melees to underlings "without shame," focusing on army size and tactics.
  (https://en.wikipedia.org/wiki/Sword_of_the_Samurai_(1989_video_game))
- **The final "Verdict of History" score** re-weights both axes at the end (see §4.5 and §8.3):
  Honor, Generalship, Army Size, Land, Province Control, (low) Rival Armies, and Dynasty (heir)
  together decide how long your shogunate/dynasty endures. **A pure-power, low-honor conqueror with no
  heir produces a short-lived dynasty; a high-honor conqueror with an adult heir founds one that can
  last up to three centuries.**

---

## 4. Character Progression, Attributes, Death & Succession

### 4.1 Attributes / skills
The player character and every rival are defined by the **five assets** in §3.1: **Honor,
Land/fief, Army/warriors, Generalship, Swordsmanship.** Two of these are also **arcade skill levels**
that affect the action games:
- **Swordsmanship** — governs your effectiveness in **duels and melees**.
- **Generalship** — governs your army's damage, training, and morale in **battles**.

### 4.2 How attributes improve
- **Swordsmanship:** trained via **Practice Kenjutsu**, but only up to a cap; past that, **only
  winning real duels** raises it to your maximum potential.
- **Generalship:** trained via **Drill Your Troops**, but only up to a cap; past that, **only real
  battlefield experience** improves it.
- **Land / wealth:** raise the rice tax (dishonorable), win **campaign** rewards (the lord "award[s]
  you an addition to your fief commensurate with the magnitude of your victory"), coerce a rival for
  income, or (as daimyo) conquer provinces.
- **Army size:** equip more samurai — capped by your **rice wealth** (as vassal) or by **number of
  provinces** (as daimyo, with diminishing returns: "if you double the number of provinces … you
  don't double the number of samurai").
- **Honor:** as in §3.2.

### 4.3 Rank progression
`gokenin/vassal → hatamoto (lieutenant) → daimyo (warlord) → Shogun.` Promotion mechanics:
- **→ Hatamoto:** when your hatamoto dies (battle, disease, or assassination), "the daimyo will
  simply choose the samurai of **highest status** as his new hatamoto" — status being the blend of
  land, troops, and (weighted heaviest) honor. Your whole vassal-phase goal is to be #1 when that
  vacancy opens.
- **→ Daimyo:** when the daimyo dies/retires, each hatamoto may **declare himself daimyo**. If only
  you declare, you win by default. If two declare, the other hatamoto pick sides; a lopsided split
  makes the weaker withdraw, an even split triggers a **battle**. Alternatively, **usurp**: march on
  the daimyo's castle, defeat his army, pursue him inside, and **beat him in personal combat** — "no
  one will contest your right." (For impatient, very powerful hatamoto.)
- **→ Shogun:** see §6 and §8.

### 4.4 Aging, death & succession
- **Death is final and frequently risked** (the manual leans on this hard: "The Way of the Samurai
  is found in death"). Any duel, melee, or battle can kill you.
- **Aging** degrades your physical (arcade) skills; eventually you *must* retire.
- **On your death (or retirement), your first-born son becomes your heir** and play continues as him:
  "If you meet your death before becoming Shogun, he will assume your responsibilities and your quest
  for power will continue." An heir **starts weaker** than his father ("his assets are less than his
  father's … not able to control as large a domain") but builds on the father's accomplishments —
  so the dynasty is a **multi-generational relay**, not a single life.
- **Second/third sons** are backups who "may become heirs if the first-born son dies due to a rival's
  treachery." **Daughters** help manage the household and can be **married off to peers** to improve
  relations.
- **No heir = catastrophe:** retiring or dying **without an heir ends the game** (see §8).

### 4.5 The family / dynasty honor score
The game is ultimately scored as a **dynasty**, not a person. At the victorious end (becoming
Shogun), the "**Verdict of History**" evaluates seven factors (§8.3), the last of which is "**Your
Dynasty — whether or not you have an heir.**" The least successful shoguns "are overthrown almost
immediately," while "the most successful found dynasties that last for up to **three centuries**"
(the Tokugawa-length ideal) — and "this greatest achievement is only available to those who play at
the highest skill level." Historical framing: dynasties can collapse fast (as with Nobunaga and
Hideyoshi, who died without securing lasting succession) or endure (Tokugawa).
(https://en.wikipedia.org/wiki/Sword_of_the_Samurai_(1989_video_game)) A community-reported maximum
score is "**around 12,600**." **`[UNVERIFIED — best-effort reconstruction]`** — this specific number
comes from a Steam player guide, not official docs.
(https://steamcommunity.com/sharedfiles/filedetails/?id=639819146)

---

## 5. Marriage & Family

"Every samurai should be married: he needs a wife to manage his household and an heir to carry on the
family name and tradition."

### 5.1 Getting married — two routes
1. **Matchmakers:** occasionally a matchmaker appears representing "a woman of the samurai class of
   marriageable age." Travel to the lord's castle to meet her. If several suitors compete, "the
   matchmaker will make a decision based on the contenders' **land holdings and reputation for
   honor**." (Community tip: brides are shown in a row; the **best available bride is pictured
   furthest left**. https://steamcommunity.com/sharedfiles/filedetails/?id=639819146)
2. **A peer's daughter:** if a fellow samurai has a daughter "of marriageable age (a youth or
   older)," visit and ask for her hand. His answer depends on "your reputation for honor and the
   nature of your dealings with him." Accepting you as a son-in-law **erases any prior hostility** —
   so marriage doubles as diplomacy (a strong-ally play; "marry a hatamoto's daughter for a strong
   ally").

**Marriage affects honor:** marrying **above** your family's honor raises yours; marrying **below**
lowers it (§3).

### 5.2 Children & heirs
- After marriage, "your house may be blessed with children." The **first-born son is your heir** and
  the linchpin of dynastic continuity (§4.4).
- **Second/third sons** are heir backups against a rival killing the first-born.
- **Daughters** manage the household and can be **married to peers** (a peer will "come to you and ask
  for her hand") to cement alliances.

### 5.3 Kidnapping, hostages, and rescue
- Rivals (and you) can **kidnap family members**, preferentially "when you are away from home." A held
  hostage makes the victim **less likely to attack you** and **more likely to comply with coercion**.
- **Rescue** is a disguised castle-infiltration melee (§2.3): find the hostage's room, carry them out,
  avoid dueling the house's master (raid while he's away). If you're captured mid-rescue, expect a
  **ransom** demand.
- **Coercion:** holding a rival's family member lets you force him to lend income (→ your land) or
  proclaim esteem (→ your honor) — though a high-honor rival may refuse anyway, and coercion makes him
  more hostile.
- **Attacking a hostage-holder outright** will likely get your kidnapped relative **killed**.
- **Automatic returns:** "When the head of a household dies, all hostages he held are returned," and a
  captured **heir must be returned** so he can assume his new duties.

### 5.4 Retirement, seppuku & the dynasty end-states
- **Retire:** when aged, you may retire and hand off to your heir — **but only if you have one**. "Do
  not select 'Retire' unless you have an heir — if you retire without an heir, the game is over."
  Eventually old age forces retirement.
- **Seppuku (ritual suicide):** two contexts. (1) **Voluntary atonement** — "If some great dishonor
  is laid to your name, there is one sure way to wipe it out" — seppuku "allows your heir (assuming
  you have one) to take over without a stain on his honor." (2) **Ordered** by your lord for treason
  (treachery/assassination); refusing the order = you and your **entire family are hunted down and
  killed** = game over.
- **Dying without an heir = game over** (the dynasty ends). Having sons is therefore an early
  strategic priority, not flavor.

---

## 6. Province / Political Layer

### 6.1 Serving the daimyo (vassal & hatamoto phases)
You serve within a clan under a **hatamoto** and ultimately the **daimyo**. Your rivals are the other
same-lord vassals, competing for favor and the next promotion. You earn favor by **volunteering** at
the lord's castle for:
- **Bold Deeds:** honor missions the lord announces (e.g., a bandit outrage). Travel to his castle,
  declare readiness, get directed to the problem; success "your reputation will improve, and you may
  rise in status." (These become **duel or melee** encounters.)
- **Campaigns:** short military tasks the hatamoto delegate. March with troops to the castle,
  volunteer; success earns a **fief addition** proportional to the victory. (These become **battle**
  encounters.)
Corroboration: bold deeds reward **honor**, campaign actions reward **land**; check **F3** for both.
(https://steamcommunity.com/sharedfiles/filedetails/?id=460177975)

### 6.2 Rivals in your own clan
Rivals scheme constantly for fiefs and reputation. You can befriend them (**Tea Ceremony**, **helping
their defense**, **marrying into their family**) or tear them down (**insult→duel**, **coercion**,
**inciting rebellion**, **treachery**, **assassination**). You **cannot honorably attack an ally**
outright unless he has committed multiple dishonorable offenses against you first; an unjustified
attack gets you seriously dishonored by the lord.

### 6.3 Becoming daimyo & the outside world
Once daimyo (via succession, faction battle, or usurpation — §4.3), the game opens to the **48-province
strategic map**. Now your rivals are **other daimyo/clans**, not clan-mates. Gray = unaligned/passive
provinces; colored = ambitious daimyo actively pursuing the Shogunate.

### 6.4 Conquest mechanics
- You may attack only **provinces adjacent** to one you control.
- **Unaligned provinces:** usually field a defense; beat it and "resistance … will collapse." You gain
  **just that one province**.
- **Rival provinces:** the rival "fights back with all the forces he can muster." Beat him and "your
  rival's power is broken. He becomes your vassal, and you step into his place as ruler of **all
  provinces he controlled**" — so beating a big rival can net many provinces at once.
- **Being conquered:** if a rival beats your province-defense battle, "**you swear allegiance to
  him… you lose all chance of becoming Shogun, and the game is over.**"
- **Overreach risk:** holding many provinces *without* the Shogun's authority invites **revolts** —
  "whole groups of provinces may leave your control and have to be reconquered."

### 6.5 The road to Shogun
The Emperor in **Kyoto (Omi province)** confers the title of Shogun on whoever has the raw power to
demand it. To qualify you must **control at least 24 provinces, one of which must be Omi** (the
central province containing Lake Biwa and Kyoto). Meeting the criteria unlocks a **"Declare Yourself
Shogun"** home option. On declaring, "the other daimyo … form a temporary alliance for the sole
purpose of breaking your power. **Only if you defeat all your enemies in battle can you become
Shogun.**" Declaring with *more* than the minimum 24 provinces yields a more stable eventual reign
(fewer revolts). This is the game's "you are Oda Nobunaga's would-be replacement" endgame.
(https://en.wikipedia.org/wiki/Sword_of_the_Samurai_(1989_video_game))

---

## 7. Menu Structure & Controls

The manual deliberately abstracts inputs as **"controller"** (a direction device) and **"selector"**
(a confirm/attack button), deferring to a per-platform **Technical Supplement**. The concrete DOS
keys below come from community keyboard-controls guides for the GOG/Steam release.
(https://steamcommunity.com/sharedfiles/filedetails/?id=460177975;
https://steamcommunity.com/sharedfiles/filedetails/?id=639819146; some entries corroborated by
https://classicreload.com/sword-of-the-samurai.html)

### 7.1 Screen / mode structure
```
Title screen
  ├── Encounter (practice): Duel | Melee | Battle   ← standalone action training
  ├── New Full Game → name → clan/province select → skill level → family advantage
  └── Saved Game
        │
        ▼
   Role-playing "scroll" layer (the strategic hub)
     ├── Home Option scroll (equip, practice, drill, tax, donate, travel, marriage, diplomacy,
     │                        treachery, retire/seppuku, [daimyo: conquer / declare Shogun])
     ├── Status Scroll (you + rivals ranked; per-person assets)
     ├── Summary Scroll (rivals' activity + opportunities)
     ├── Province Map (travel as vassal/hatamoto)
     └── Strategic Map of 48 provinces (travel/conquer as daimyo)
        │  (entering combat drops into →)
        ▼
   Action engines: DUEL (side-view) | MELEE (top-down solo) | BATTLE (top-down army)
```

### 7.2 Character creation choices
- **Name** your samurai.
- **Clan / starting province:** a province map; each clan shows crest and four relative strengths
  (honor / generalship / swordsmanship / land) that seed your starting stats. (Corner provinces like
  Satsuma or Dewa limit early neighbors/enemies; central provinces offer more fights but more
  threats. https://steamcommunity.com/sharedfiles/filedetails/?id=639819146)
- **Skill level (difficulty), four blades:** **Tanto** (dagger, beginner) → **Wakizashi** (short
  sword, intermediate) → **Katana** (long sword, experienced) → **No-Dachi** (great sword, master).
  Longer blade = harder game and greater rewards (the 3-century dynasty is only reachable at the
  hardest level).
- **Family advantage:** a slight edge in one of **Honor / Generalship / Swordsmanship / Land**.

### 7.3 Global / hub keys (DOS)
| Key | Action |
|---|---|
| **F1** | Personal & area character overview (asset icons; Enter to view) |
| **F2** | Overview of Japan (most useful as daimyo) |
| **F3** | Available quests / bold deeds / campaign actions / enemy activity |
| **Enter** | Select / advance scroll (also **fires arrows** in melee) |
| **Esc** | Back up a screen / cancel |
| **Space** | Pause |
| **Alt+S / Alt+R** | Save / Restore (home screen only) |
| **Alt+Q** | Quit |
| **Alt+N** | Abandon game, start new |
| **Alt+Z** | Toggle full graphics |
| **Alt+V** | Toggle sound/music |

### 7.4 Duel keys (DOS)
- Move: **arrow keys / number pad**.
- **Hold Enter** to swing; **4/left**, **6/right**, **8/up (downward)**, **7 / 9** angled swings
  (all blockable).
- **Hold Enter + 2/down** to draw the sword back; when fully back, **8/up** = unblockable extra-damage
  strike. (Merely releasing the button does *not* swing.)
- **1 / 3** = over-the-shoulder swings to a side.
- **Backspace** = defend/parry.

### 7.5 Melee keys (DOS)
- Move with directions; **Enter** = attack (sword when close) or **fire arrow** (when the enemy is
  beyond sword range); auto-face nearest enemy.

### 7.6 Battle keys (DOS)
- Move the general's-fan cursor with arrows/number keys.
- **1–8** select+halt a unit; then draw a line to the destination and press **+** or **-** to issue
  the move order; up/down cycles formations pre-battle, left/right mirrors them; **Space** pauses.

> **`[UNVERIFIED — best-effort reconstruction]`** for §7.3–7.6: these are the *DOS/GOG-release* key
> bindings reported by community guides and may differ from the exact keys printed in an original boxed
> Technical Supplement, and from non-DOS ports. The *mechanics* they trigger are all confirmed by the
> manual; only the literal key letters are secondary-sourced. The ClassicReload "WASD/jump/dodge/shoot
> upgrade" scheme appears to be a generic in-browser emulator overlay, **not** authentic to the
> original, and should not be treated as canonical.

---

## 8. Win / Loss Conditions

### 8.1 How you WIN
**Become Shogun of all Japan.** Concretely:
1. Rise to **daimyo** (§4.3).
2. Control **≥ 24 of the 48 provinces**, **including Omi** (Kyoto).
3. Choose **"Declare Yourself Shogun."**
4. **Defeat the coalition** of all other daimyo who ally against you.
Succeed and you are named Shogun by the Emperor; the game then computes the "Verdict of History"
(§8.3). Winning is the *only* victory; there is no lesser "win."

### 8.2 How you LOSE (all the ways it can end badly)
1. **Death without an heir** — dynasty ends, game over.
2. **Retiring without an heir** — same (the manual explicitly warns against selecting Retire heirless).
3. **Being conquered as a daimyo** — losing a province-defense battle to a rival means you swear
   allegiance to him; "you lose all chance of becoming Shogun, and the game is over."
4. **Refusing an ordered seppuku** — for treason (caught treachery/assassination, or a failed attempt
   on your own lord): you and your **entire family are hunted down and killed.** Wikipedia:
   "Failing to assassinate one's lord triggers the extermination of his entire family," ending the
   game. (https://en.wikipedia.org/wiki/Sword_of_the_Samurai_(1989_video_game))
5. **Being killed in a duel/melee/battle with no heir to inherit** — a special case of (1);
   assassination by a rival with no surviving son ends everything.
6. **`[UNVERIFIED — best-effort reconstruction]`** whether an accumulated-dishonor state can force a
   *terminal* seppuku even with an heir (with an heir, seppuku normally *continues* the dynasty rather
   than ending the game — so this is a continuation, not strictly a loss).

**Note on seppuku as a "soft reset":** committing seppuku when you *have* an heir is **not** a loss —
it wipes your dishonor and passes a clean slate to your son, and play continues. Death/seppuku only
ends the *game* when there is no heir.

### 8.3 The final score — "The Verdict of History"
On becoming Shogun, the game rates the durability of your rule/dynasty from seven factors (manual,
verbatim list):
1. **Your Honor** — how well you will be respected.
2. **Your Generalship** — how well you control the military.
3. **Army Size** — bigger is better.
4. **Your Land** — your wealth.
5. **Province Control** — how many provinces you held when you declared (more is better).
6. **Rival Armies** — total size of former rivals' armies (**smaller is better**).
7. **Your Dynasty** — whether you have an heir.
Outcomes range from being "overthrown almost immediately" to founding a dynasty lasting "up to three
centuries" (only reachable on the No-Dachi difficulty). Community-reported max score ≈ **12,600**
(**`[UNVERIFIED — best-effort reconstruction]`**;
https://steamcommunity.com/sharedfiles/filedetails/?id=639819146).

---

## Appendix A — Key Numbers at a Glance

| Value | Figure | Source confidence |
|---|---|---|
| Release year | **1989** (not 1992) | Verified (Wikipedia, MobyGames, manual) |
| Provinces in Japan | **48** | Verified (manual; GOG) |
| Provinces needed for Shogun | **≥ 24, incl. Omi (Kyoto)** | Verified (manual) |
| Starting age | **15** (after gempuku) | Verified (manual) |
| Duel: wounds to lose | **4** | Verified (manual) |
| Melee: wounds to lose | **2** | Verified (manual) |
| Duel start penalty after a prior melee wound | **+1 wound** | Verified (manual) |
| Soldiers per battle figure — samurai / hatamoto / daimyo | **6 / 60 / 250** | Verified (manual) |
| Castle levels — samurai / hatamoto / daimyo | **1 / 2 / 3** | Verified (manual) |
| Attack formations | **Hoshi, Kakuyoku, Katana** | Verified (manual) |
| Defense formations | **Ganko, Koyaku, Engetsu** | Verified (manual) |
| Difficulty tiers | **Tanto, Wakizashi, Katana, No-Dachi** | Verified (manual) |
| Family advantages | **Honor, Generalship, Swordsmanship, Land** | Verified (manual) |
| Max dynasty length | **up to ~3 centuries** (hardest level only) | Verified (manual) |
| Reported max score | **~12,600** | UNVERIFIED (community guide) |
| Charged over-shoulder strike damage | **2 (vs 1)** | UNVERIFIED (community; manual says "more") |

## Appendix B — Glossary (from the manual)
- **Gokenin** — a lower-level responsible clan samurai; the player's starting rank.
- **Hatamoto** — a daimyo's lieutenant/trusted advisor (2nd rank).
- **Daimyo** — hereditary warlord ruling a province (3rd rank).
- **Shogun** — military ruler of all Japan (the goal), appointed by the Emperor.
- **Ashigaru** — peasant foot-soldiers, samurai-officered.
- **Ronin** — a masterless samurai (your travel disguise).
- **Gempuku** — coming-of-age day (the game starts just after yours, age 15).
- **Seppuku / hara-kiri** — ritual suicide (honorable atonement; hara-kiri is the vulgar term).
- **Gekokujo** — "the low oppress the high": subordinates violently usurping superiors (the era's
  defining dynamic and your own strategy writ large).
- **Ikko-ikki** — fanatical peasant-warrior leagues; a common non-samurai enemy army (mostly infantry).
- **Sohei** — warrior-monks.
- **Shuriken** — throwing stars (ninja weapon, "dipped in poison" per the manual).
- **Dai-sho** — the paired katana + wakizashi that symbolize the samurai class.

---

## 9. Confidence & Sources

### 9.1 Sources consulted (with reliability notes)
1. **Official manual, *Sword of the Samurai* (MicroProse, 1989)** — recovered as the Steam re-release
   PDF and text-extracted in full (101 pages). *This is the primary and highest-confidence source and
   underpins the large majority of §1–§8.*
   `https://cdn.cloudflare.steamstatic.com/steam/apps/327950/manuals/Manual.pdf`
2. **Wikipedia — Sword of the Samurai (1989 video game)** — release/authorship, Sid Meier's role,
   ranking weighting ("army size and honor"), delegation of duels as daimyo, ending/dynasty framing,
   reception. High confidence for facts, secondary for mechanics.
   `https://en.wikipedia.org/wiki/Sword_of_the_Samurai_(1989_video_game)`
3. **MobyGames — Sword of the Samurai** — credits, genre, re-release history, reception.
   `https://www.mobygames.com/game/1250/sword-of-the-samurai/`
4. **GOG store page** — "48 warring provinces," real-time battles with infantry/cavalry/muskets,
   "furious melee action," Honor framing, the "leave your sword in its sheath" line.
   `https://www.gog.com/game/sword_of_the_samurai`
5. **Steam community "Beginner Tips" guide** — the five stat icons, rank ladder, bold-deed vs
   campaign distinction, bride-selection tip, ninja/home-defense tips, castle-stealth tips, reported
   ~12,600 max score. *Secondary/community — used for corroboration and UNVERIFIED-tagged specifics.*
   `https://steamcommunity.com/sharedfiles/filedetails/?id=639819146`
6. **Steam community "Keyboard Controls" guide** — the concrete DOS key bindings (F1–F3, Alt-keys,
   duel/melee/battle keys), bold-deed/campaign F3 detail. *Secondary/community.*
   `https://steamcommunity.com/sharedfiles/filedetails/?id=460177975`
7. **ClassicReload emulator page** — general description; note its WASD/"upgrade" control list appears
   to be a generic browser-emulator overlay and is **not** treated as authentic.
   `https://classicreload.com/sword-of-the-samurai.html`
8. **Samurai Games Wiki (Fandom)** and **TV Tropes** — indexed as leads; their pages were not fully
   retrievable during research (paywall/403), so nothing load-bearing depends on them.
   `https://samuraigames.fandom.com/wiki/Sword_Of_The_Samurai` ;
   `https://tvtropes.org/pmwiki/pmwiki.php/VideoGame/SwordOfTheSamurai`
9. **GameFAQs guide (Toenail_lord)** — a full walkthrough exists but was not directly retrievable
   (403). Listed for completeness.
   `https://gamefaqs.gamespot.com/pc/915698-sword-of-the-samurai/faqs/37891`

### 9.2 Claims explicitly marked UNVERIFIED in this document
- Exact **time-step length / whether time is uniform**, and any **season/calendar** system (§1.3).
- Exact **per-hit damage values** in duels, incl. the "charged over-shoulder = 2 damage" figure
  (§2.1, Appendix A). The manual counts **wounds**, not numeric damage.
- The **~12,600 maximum score** (§4.5, §8.3, Appendix A) — community-sourced only.
- The **literal DOS key letters** in §7.3–7.6 — mechanics verified via manual, key letters via
  community guides; original boxed Technical Supplement keys and non-DOS ports may differ.
- Any dedicated **mounted/ambush arcade minigame** (§2.4) — almost certainly does **not** exist;
  "mounted" = cavalry within the Battle engine.
- Whether **terminal dishonor can force a game-ending seppuku while an heir exists** (§8.2 item 6).

### 9.3 Overall confidence
**High.** Because the complete original 1989 manual was recovered and read end-to-end, the core loop,
the three action engines, honor/power, marriage/succession, the province/conquest layer, and the
win/loss and scoring rules are all sourced from primary documentation, with community guides used
only to pin down concrete DOS keys and a couple of numeric edge-cases (clearly tagged). The single
most important correction versus the brief is the **date (1989, not 1992)** and the fact that the
"ninja infiltration" is a **variant of the melee engine**, not a fourth standalone game.
