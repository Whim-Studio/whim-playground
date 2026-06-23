# TIWAS RPG — Documentation Sections 6–10
## The Conflict, Magic & Equipment Supplement

> Built on the existing Tiwas engine (d100 roll-under, exertion = number rolled, overflow → HP, all math rounds DOWN). Every new mechanic resolves roll-under and charges PE/MP equal to the roll. Cross-references to other sections are flagged `→ See Section X`.

---

# 6. CONFLICT & COMBAT FRAMEWORK

### (a) DESIGN INTENT
Combat in Tiwas is not a separate minigame bolted onto the skill system — it *is* the skill system, run at high pressure. Every attack is a roll-under skill test, every test bleeds Physical Energy or Mental Points, and the overflow rule (→ See Section 4, Core Doc) means a desperate flurry of high rolls will literally exhaust a fighter into their own HP. Combat therefore rewards the *measured* combatant who manages their pools, while still letting the reckless animal-spirit win a fast, bloody exchange and pay for it after. Defense uses **Reflexes (bss)** so that the same Rat-quickness that lets you act first also lets you not get hit, keeping the attribute economy tight. Growth is preserved: a missed swing still earns XP, and a missed *doubles* swing still births a new combat maneuver mid-fight.

### (b) CORE RULES

**The Action Economy.** On their turn each combatant receives:
- **1 Action** — an attack, a spell (→ See Section 8), a maneuver, or a skill test.
- **1 Move** — up to **Movement Speed** squares = (bsp + bss) ÷ 15, round down.
- **Any number of Free Reactions** — but each Reaction (an active Defense) is itself a skill test and costs PE/MP equal to its roll.

You may trade your Action to gain a second Move, or to take a second Defense Reaction that round.

**Turn Order (Speed).** At the start of combat every combatant computes **Speed = bsp + bss + bse**. Act in descending Speed order. Ties are broken by higher **bss (Rat/Reflexes)**; if still tied, by a straight opposed Reflexes roll (→ See Section 7).

**The Full Round Sequence.**
1. **Initiative** — rank all combatants by Speed (once per combat, unless a maneuver re-rolls it).
2. **Declare** — on your turn, declare Move and Action.
3. **Resolve Attack** — roll d100 ≤ your weapon skill (or Might/Impact if unarmed). Pay PE = number rolled.
4. **Defender Reacts** (optional) — defender may spend their Action/Reaction to Evade or Block.
5. **Damage** — if the attack lands, compute damage (below) and subtract from defender HP.
6. **Recovery** — after the test, the *acting* character recovers PE/MP equal to **½ their Regen** (→ Core Doc). This applies to defenders' reaction-tests too.

**Resolving an Attack.**
- Roll **d100 ≤ Attack Skill**. The Attack Skill is the relevant Tier-1 skill or a weapon-linked Advanced Skill (→ See Section 10).
- **Cost:** Pay PE (Body attack) or MP (Mind attack) equal to the *number rolled*. Overflow → HP.
- **On a hit:** Damage = **Weapon Damage Modifier + Margin Bonus**, reduced by the defender's **Armor Rating** (→ See Section 10).
- **Margin Bonus** = (Attack Skill − number rolled) ÷ 10, round down. Hitting *well under* your skill is a clean, powerful strike; squeaking in just under your skill is a glancing blow.

**Damage Calculation (full formula).**
> **Damage to HP = [Weapon Damage Modifier] + [Margin Bonus] + [Might Bonus] − [Armor Rating]** (minimum 0)
- **Might Bonus** (melee only) = bpp ÷ 20, round down. The Bear's raw power adds to every melee blow.
- Ranged/finesse weapons replace Might Bonus with **Agility Bonus** = bsp ÷ 20, round down (→ See Section 10 weapon tags).

**Defense & Evasion.** Defense is a *Reaction* and a real skill test (it costs resources):
- **Evade (Reflexes, bss-based):** Roll d100 ≤ Reflexes. Pay PE = number rolled. On success, the attack misses entirely — **no damage**. On failure, normal damage applies (and you still paid the PE; high evade rolls are exhausting — fitting for frantic dodging).
- **Block (Brawn, bpe-based, requires shield/weapon):** Roll d100 ≤ Brawn. Pay PE = number rolled. On success, reduce incoming damage by your **Block Value** (= Armor Rating of the shield ×2, → See Section 10). Block does not negate position; you hold ground.
- **Passive Defense (no Reaction spent):** If you choose not to react, you have a **Static Guard** = bss ÷ 5, round down, added to your Armor Rating for that hit only. This lets a depleted character still shed a little damage for free.

A defender who is **out of PE** can still Evade — but every point of the evade roll is overflow straight to HP. Dodging on fumes can kill you.

### (c) MECHANICAL EXAMPLES

> **Reflexes & Speed.** Rhando the Hawk has bsp = 38, bss = 45, bse = 30. **Speed = 38 + 45 + 30 = 113.** **Movement Speed = (38 + 45) ÷ 15 = 83 ÷ 15 = 5** squares. His Reflexes cap (Tier 1, just bss) = 45, starting Reflexes = 22.
>
> **An attack.** Rhando attacks a bandit with a Shortsword (Damage Modifier +6, → Section 10) using his **Impact** skill of 40. He rolls **27**. 27 ≤ 40 → hit. He pays **27 PE**. Margin Bonus = (40 − 27) ÷ 10 = 1. His bpp = 50 → Might Bonus = 50 ÷ 20 = 2. Damage before armor = 6 + 1 + 2 = **9**. The bandit wears Leather (Armor Rating 2) → final damage = 9 − 2 = **7 HP**.
>
> **An evade.** The bandit (Reflexes 30, current PE 20) tries to Evade and rolls **34**. 34 > 30 → the dodge fails, he takes the full 7. Worse, he paid 34 PE against only 20 remaining → **14 overflow straight to HP**. The frantic dodge cost him more than the sword: 7 + 14 = **21 HP** lost this exchange. He also earns Failure XP = 34 − 30 = **4** in Reflexes.

### (d) TABLES

**Table 6-1: Attack Resolution at a Glance**
| Step | Formula | Pool |
|---|---|---|
| Hit? | d100 ≤ Attack Skill | — |
| Cost | PE/MP = number rolled (overflow→HP) | PE or MP |
| Margin Bonus | (Skill − Roll) ÷ 10 ↓ | — |
| Might Bonus (melee) | bpp ÷ 20 ↓ | — |
| Agility Bonus (ranged) | bsp ÷ 20 ↓ | — |
| Final Damage | DmgMod + Margin + Atk Bonus − Armor | — |

**Table 6-2: Reaction Options**
| Reaction | Skill (Attr) | Effect on success | Cost |
|---|---|---|---|
| Evade | Reflexes (bss) | Negate all damage | PE = roll |
| Block | Brawn (bpe) | −Block Value damage | PE = roll |
| Passive (Static Guard) | none | +bss÷5 Armor this hit | Free |

**Table 6-3: Common Combat Maneuvers** (declared as your Action; resolved as a skill test)
| Maneuver | Skill Used | Effect |
|---|---|---|
| Power Attack | Might (bpp) | +Weapon DmgMod again on hit; defender Static Guard ignored |
| Flurry | Impact (bps) | Two attacks this Action; pay each roll's PE separately |
| Charge | Agility (bsp) | Move + attack; +Margin step if you moved ≥3 squares |
| Aim/Feint | Perception (mss) | Next attack this round: defender's Evade target −10 (→ See Section 7 Opposed) |
| Grapple | Brawn (bpe) | Opposed vs target Toughness; success halves target Movement |

### (e) EDGE-CASE RULINGS
- **Overflow death-spiral on defense.** A character at 0 PE who is attacked repeatedly may *choose* Passive Defense (free Static Guard) every time rather than Evade, accepting partial damage instead of self-inflicted overflow. Players should be reminded: dodging is never free when the tank is empty.
- **Doubles on an attack roll that fails.** Rolling e.g. **44** and missing (skill 40) triggers a mid-combat epiphany (→ Core Doc Advanced Skills). The new Tier+1 combat maneuver is created *immediately* and may be used on the **next** turn, not retroactively. The player still pays the 44 PE and still earns Failure XP = 44 − 40 = 4.
- **Simultaneous death (Speed tie).** If two combatants would drop each other on the same Speed count, resolve the higher-bss combatant's blow first; the lower-bss combatant still acts if they survive (Rat-quickness gets the last word).

### (f) DESIGNER NOTE
The Margin Bonus is the load-bearing balance lever: a character with a high skill but a *low* roll hits hardest, which rewards investing in skill over fishing for low costs, since a low roll gives both cheap exertion *and* high damage — a deliberate "mastery" payoff. **Optional rule (Gritty):** drop Static Guard entirely so every point of damage must be actively defended; this makes PE management brutal and suits high-lethality tables only.

---

# 7. TASK DIFFICULTY & MODIFIERS

### (a) DESIGN INTENT
Difficulty in a roll-under system must touch the **target number**, never the die. Tiwas applies all difficulty as a flat modifier to your effective Skill value before you roll — harder tasks shrink your success window. Crucially, difficulty does **not** reduce the exertion cost: a hard task you barely succeed at still costs you the full number rolled, so straining against difficulty is doubly draining. This preserves the resource-exhaustion soul of the system and means a wise character avoids unnecessary hard rolls rather than grinding them.

### (b) CORE RULES

**Applying Difficulty.**
- The GM assigns a **Difficulty Modifier (DM)**, a positive or negative number.
- **Effective Skill = Skill Value + DM.** You roll d100 ≤ Effective Skill.
- Cost is still **PE/MP = number rolled**, regardless of DM. → See Section 4.
- Effective Skill cannot exceed 99 or drop below 1.

**The Difficulty Ladder** (→ Table 7-1). Bonuses widen your window (Trivial/Easy), penalties shrink it (Hard/Heroic). The GM picks the named rung, not an arbitrary number, for consistency.

**Opposed Rolls (full rules).** When two characters directly contest (arm-wrestle, stealth vs perception, a Glamour duel):
1. Both parties roll d100 ≤ their relevant (DM-adjusted) Skill. Both pay PE/MP = their own number rolled.
2. **Both succeed:** the one with the **larger Margin** (Skill − Roll) wins.
3. **One succeeds, one fails:** the success wins.
4. **Both fail:** the action stalls — *no winner*. Both earn Failure XP normally, and a re-contest may be attempted next round (paying again).
5. **Margin tie:** the higher **Speed** (bsp+bss+bse) wins (→ Core Doc). If Speed also ties, higher bss; then a coin flip.

**Assisted Rolls (one helper).** A helper makes the *same* skill test first, paying their own cost. On the helper's success, the lead character gains **DM +5** (one rung of help). The helper's *Margin ÷ 10 (↓)* may add further, to a max of +15 total assistance.

**Group Rolls (many actors, one outcome).** When a whole party attempts the same task (sneaking together, hauling a gate):
- Each member rolls their own test and pays their own cost.
- The task succeeds if **at least half (round down) succeed**.
- For *additive* tasks (lifting, a tug-of-war), instead sum each success's Margin as collective effort vs a GM-set Effort Threshold.

### (c) MECHANICAL EXAMPLES

> **A hard climb.** Mira has Agility (bsp Tier-1) = 44. The cliff is **Hard (DM −20)**. Effective Skill = 44 − 20 = **24**. She rolls **19** → success, but pays **19 PE** for a white-knuckle scramble. Had she rolled 30, she'd fail, pay 30 PE, and earn XP = 30 − 24 = 6.
>
> **An opposed stealth roll.** Thief Vael (Reflexes 50) sneaks past Guard Borin (Perception, mss-Tier1, = 40). Vael rolls **22** (success, Margin 50−22 = 28; pays 22 PE). Borin rolls **15** (success, Margin 40−15 = 25; pays 15 MP). Both succeeded → larger Margin wins: **28 > 25**, Vael slips by. Note Vael spent more PE for the win — speed costs.
>
> **Assisted lockpick.** Pico assists Dax. Pico rolls his Wits and succeeds with Margin 18 → grants Dax DM +5 (base help) + 18÷10 = +1, total **+6** to Dax's effective Perception for the pick.

### (d) TABLES

**Table 7-1: The Difficulty Ladder**
| Named Difficulty | DM to Skill | When to use |
|---|---|---|
| Trivial | +30 | Routine, calm conditions |
| Easy | +15 | Favorable conditions, good tools |
| Standard | 0 | Default; no modifier |
| Tricky | −10 | Mild pressure, poor footing |
| Hard | −20 | Serious obstacle, bad conditions |
| Severe | −30 | Hostile environment, active resistance |
| Heroic | −40 | Near the edge of possibility |
| Legendary | −50 | Once-in-a-saga feats |

**Table 7-2: Situational Modifiers (stackable)**
| Condition | DM |
|---|---|
| Superior tool / ideal gear | +10 |
| Improvised / wrong tool | −10 |
| Per helper assisting | +5 (max +15) |
| Wounded (HP ≤ ½ max) | −10 |
| Critically drained (PE or MP = 0) | −15 |
| Higher ground / setup (combat) | +10 |
| Blind / sensory-denied | −25 |

### (e) EDGE-CASE RULINGS
- **Effective Skill ≤ 0.** If DM drives Effective Skill to 0 or below, the task is *not* impossible: the character may attempt it only by rolling **exactly 01**, which always permits a success attempt (and still costs 1 PE/MP). This is the "Legendary long-shot" valve.
- **Doubles under difficulty.** Doubles trigger the epiphany on a **failed** roll based on the *number rolled*, not the modified skill — so a guard who fails a DM-crushed Perception roll of 55 still gets a Perception-derived Advanced Skill. Difficulty makes failure (and thus growth) more likely, which is thematically intended: you learn most when overmatched.
- **Both-fail opposed stalls cost real resources.** Each contest charges PE/MP again — a stubborn Glamour duel can drain both debaters into HP overflow, ending in mutual collapse.

### (f) DESIGNER NOTE
Because difficulty never reduces cost, hard tasks are a *double tax* (lower success window + same exertion). This is intentional and is the main brake on "just roll everything." **Optional rule (Forgiving):** allow a successful Trivial/Easy task to refund 5 PE/MP, modeling effortless competence — useful for lower-lethality or pulp-heroic campaigns.

---

# 8. MAGIC & THE MENTAL ARTS

### (a) DESIGN INTENT
Magic is not a separate resource system — it is the skill engine pointed inward. Every spell is an **Advanced Skill built from Mind attributes**, cast by rolling d100 ≤ the spell's skill and paying **MP equal to the number rolled**. The overflow rule makes overreaching mages literally bleed for their ambition (MP overflow → HP), the genre-perfect "casting on your own life force." Spells are *created the Tiwas way* — through doubles-failures — so a mage's grimoire is the scar-record of their failures. Concentration leans on the Rooster's **Willpower (mpe)** and the Cat's **Perception (mss)**, exactly as the core hooks promised.

### (b) CORE RULES

**Spells as Advanced Skills.**
- A spell is a Tier-N skill whose formula sums N **Mind** attributes. Its Cap = (sum of those attributes) ÷ Tier; starting value = Cap ÷ 2 (→ Core Doc).
- **Casting:** roll d100 ≤ Spell Skill (DM-adjusted, → See Section 7). Pay **MP = number rolled**. Overflow → HP.
- On success the spell takes effect at its listed magnitude; **Spell Power scales with Margin** (Skill − Roll) ÷ 10, round down — a cleanly cast spell hits harder, identical in spirit to a weapon's Margin Bonus (→ See Section 6).
- A failed cast still pays MP and still earns Failure XP. A **failed doubles** cast lets the mage invent a new Tier+1 spell on the spot (Core Doc Advanced Skills) — the canonical way new magic enters the world.

**Spell Speed & Initiative.** A spell's place in the round uses normal Speed, but a caster may add **Perception (mss)** as a one-time initiative bonus when casting (the Cat's quickness), at no MP cost — it is the spell's inherent swiftness. Spells with the **Fast** tag may be cast as a Reaction.

**Concentration & Sustained Spells.**
- A **Sustained** spell stays active across rounds. At the **start of each of your turns** while sustaining, make a **Willpower (mpe)** concentration check: roll d100 ≤ Willpower, pay MP = number rolled. Failure ends the spell (and earns XP).
- Taking HP damage while sustaining forces an **immediate** concentration check at **DM = −(damage taken)**. Drop the spell on failure.
- You may sustain at most **mep ÷ 25 (round down, minimum 1)** spells at once — the Crane's Focus governs how many threads you hold.

**Resisting Magic (Mental Defense).** A spell that targets a mind is an **Opposed Roll** (→ See Section 7): caster's Spell Skill vs the target's relevant Mind-endurance skill — **Resolve (mee)** vs raw mental assault, **Composure (mex)** vs social/illusion magic, **Focus (mep)** vs domination. The defender pays MP = their roll; if out of MP, overflow → HP.

**MP Cost Floors.** Beyond the rolled cost, potent spells carry a **Base MP Floor** — if the number rolled is *below* the Floor, you still pay the Floor (the minimum energy to shape the working). The Floor never reduces what a high roll costs.

### (c) MECHANICAL EXAMPLES

> **Building a spell.** Sora the mage has mpp (Cunning) = 60 and mss (Perception) = 40. She failed a Cunning roll on doubles (66) and forged **Mind-Spike** as a Tier-2 spell using mpp + mss. Cap = (60 + 40) ÷ 2 = **50**. Starting value = 50 ÷ 2 = **25**.
>
> **Casting it.** Sora casts Mind-Spike (skill now raised to 32) at a goblin. She rolls **18** → success, pays **18 MP**. Margin = 32 − 18 = 14 → Spell Power +1. Mind-Spike base magnitude is 5 → **6 psychic damage**. The goblin resists with **Resolve (mee) = 20**, rolls **41** → fails to resist, pays 41 MP (it only had 25 MP → **16 overflow to HP**). The goblin is spiked *and* exhausted.
>
> **Sustaining.** Sora keeps up **Veil of the Owl** (invisibility, Sustained). She has mep (Focus) = 55 → may hold 55 ÷ 25 = **2** sustained spells. Next turn she rolls Willpower (mpe 48) → 30, pays 30 MP, stays hidden. Then she's struck for 12 HP — immediate check at DM −12 → effective Willpower 36; she rolls 40 → fails, the Veil collapses.

### (d) TABLES

**Table 8-1: Starter Spell List**
| Spell | Tier / Attributes | Base Magnitude | MP Floor | Tags | Resisted by |
|---|---|---|---|---|---|
| **Mind-Spike** | 2 · mpp+mss | 5 psychic dmg | 8 | — | Resolve (mee) |
| **Ember (Stag-fire)** | 2 · mpp+mps | 6 fire dmg | 8 | Ranged | Reflexes (bss)* |
| **Veil of the Owl** | 3 · mss+mse+mee | Invisible / hidden | 12 | Sustained | — (Perception to pierce) |
| **Mend (Crane's Hand)** | 2 · mep+mpe | Heal 5 HP | 10 | Touch | — |
| **Glamoured Word** | 2 · mpx+msx | Suggest 1 act | 6 | Social | Composure (mex) |
| **Owl-Ward** | 3 · mee+mep+mpe | +10 Armor Rating, Sustained | 12 | Sustained | — |
| **Snake-Sense** | 2 · mps+mss | Detect/foresee; +10 next roll | 4 | Fast | — |
| **Dominate (Stag's Yoke)** | 4 · mpp+mpe+mep+mee | Control 1 target | 18 | Sustained | Focus (mep) |

*Ember is a hurled physical effect; the *target* dodges with Reflexes as a normal Evade (→ See Section 6).

**Table 8-2: Spell Power Scaling**
| Margin (Skill − Roll) | Power Bonus |
|---|---|
| 0–9 | +0 |
| 10–19 | +1 |
| 20–29 | +2 |
| 30–39 | +3 |
| 40+ | +4 |
(Applied to damage spells as flat magnitude; to utility spells as +1 round duration or +1 target per step, GM's call.)

### (e) EDGE-CASE RULINGS
- **Casting with empty MP on purpose.** A mage at 0 MP may still cast — every point of the roll is overflow to HP. A roll of 70 is 70 HP of self-immolating power. This is legal, sometimes heroic, frequently lethal; it is the system's "blood magic" without a new subsystem.
- **Two mages sustaining the same target.** Concurrent Sustained effects stack only if they grant different things; two **Owl-Wards** do not give +20 Armor — take the higher. Two different sustained buffs each demand their own per-turn Willpower check and MP payment, taxing the caster's Focus cap.
- **Resist roll doubles.** A target who fails to resist on doubles gets the epiphany too — a tormented mind that fails to shrug off Domination may *invent* a Mind-defense Advanced Skill from the trauma. Magic teaches its victims.

### (f) DESIGNER NOTE
The MP Floor exists so cheap "I rolled a 3" casts can't trivialize powerful spells — without it, a high-skill mage would fish for low rolls and pay almost nothing. The Floor sets a minimum tax independent of luck. **Optional rule (High Magic):** remove Floors and let raw cost rule, making mages glass-cannon gamblers — thematically rich, mechanically swingier.

---

# 9. ADVERSARY & NPC DESIGN

### (a) DESIGN INTENT
Rolling 1d100 × 24 for every goblin is unplayable. This section replaces full generation with a **tiered template** that produces NPCs already shaped to the combat math of Section 6 and the magic math of Section 8. The guiding rule: an NPC needs only the *derived stats the rules actually consult* — HP, MP, PE, Speed, and a short list of skills. Everything else is back-fillable. Tiers map cleanly to the four classic threat grades, and every NPC remains a legal Tiwas actor: they pay exertion, they overflow, they earn nothing (NPCs don't grow), and they die when HP hits 0.

### (b) CORE RULES

**The Streamlined Stat Block.** An NPC is defined by:
- **Tier** (Minion / Standard / Elite / Boss) → sets stat band.
- **HP / MP / PE** — the three pools.
- **Speed** and **Movement** — for turn order and positioning.
- **Skills** — 1 to 4 named skills with a flat value (use these for attacks, defenses, and saves). Anything unlisted defaults to the **Tier's Default Skill** value.
- **Armor Rating** and **Damage Modifier** of their main attack (→ See Section 6/10).
- **Special** — at most one or two signature tricks.

**Building by Tier (→ Table 9-1).** Pick the Tier, read the band, assign HP/MP/PE/Speed and a primary Skill within range. Done in under a minute.

**Conversion from a full PC.** To downgrade a fully-rolled character into a quick NPC: keep their three pools and Speed as-is; collapse all their skills to just the **two or three highest**, and use the **Tier Default** for everything else. To *upgrade* a Minion to Standard, add the difference in the HP/PE bands and +10 to its primary skill.

**Minion Rule (Mob Simplification).** Minions don't track PE/MP overflow individually — instead a Minion drops at **0 HP or on any single hit whose final damage ≥ ½ its HP**. Minions also roll defense as a *group*: one Evade roll for the whole pack per attacker per round.

**NPC Exertion (lightweight).** Standard+ NPCs do pay PE/MP = roll and suffer overflow, exactly like PCs — this keeps long fights against Elites self-limiting (a Boss that fishes high rolls wears itself down). Minions ignore exertion entirely (they die too fast to matter).

### (c) MECHANICAL EXAMPLES — Three Stat Blocks

> **TOWN GUARD (Standard)** — *Ox-steady, Dog-disciplined*
> HP 70 · MP 30 · PE 30 · Speed 60 · Move 4 · Armor 3 (chain)
> Skills: **Impact (spear) 45**, Brawn (block) 40, Poise 40; Default 30.
> Attack: Spear, DmgMod +6, melee (Might Bonus from bpp≈40 → +2).
> Special: *Hold the Line* — may Block as a free Reaction once/round without spending its Action.

> **GOBLIN SCOUT (Minion)** — *Rat-quick, Hawk-eyed*
> HP 24 · PE 14 · Speed 70 · Move 5 · Armor 2 (leather)
> Skills: **Agility (shortbow) 40**, Reflexes 38; Default 25.
> Attack: Shortbow, DmgMod +4, ranged (Agility Bonus from bsp≈38 → +1).
> Minion: drops at 0 HP or any hit ≥ 12 damage. Pack Evades as one roll.

> **ARCANE LICH (Boss)** — *Owl-wise, Stag-willed*
> HP 90 · MP 140 · PE 20 · Speed 80 · Move 4 · Armor 6 (bone + Owl-Ward)
> Skills: **Dominate (mpp+mpe+mep+mee) 55**, Mind-Spike 60, Willpower (mpe) 65, Resolve (mee) 60; Default 40.
> Attacks: Mind-Spike (base 5, Floor 8, resisted by Resolve), Dominate (Floor 18, resisted by Focus). → See Section 8.
> Special: *Phylactery* — at 0 HP, drops but is not destroyed unless the phylactery is broken. *Sustained Owl-Ward* already folded into its Armor 6; if concentration breaks (HP-damage check, → Sec 8), Armor falls to 3.
>
> **Example exchange:** The Lich casts Mind-Spike (skill 60) at a hero, rolls 22 → pays 22 MP (of 140, fine), Margin 38 → Power +3 → 8 psychic. Hero resists with Resolve 45, rolls 50 → fails, pays 50 MP. The Lich's exertion barely dents its huge MP pool — Bosses are dangerous precisely because their pools resist the overflow tax.

### (d) TABLES

**Table 9-1: NPC Tier Bands**
| Tier | HP | MP | PE | Speed | Primary Skill | # Named Skills | Default Skill |
|---|---|---|---|---|---|---|---|
| **Minion** | 15–25 | 0–15 | 10–15 | 50–70 | 35–40 | 1 | 25 |
| **Standard** | 50–75 | 25–40 | 25–35 | 55–70 | 40–50 | 2–3 | 30 |
| **Elite** | 80–110 | 50–90 | 35–50 | 65–80 | 50–60 | 3 | 35 |
| **Boss** | 120–180 | 100–160 | 45–70 | 75–95 | 55–70 | 4 | 40 |

**Table 9-2: Quick Threat Dials**
| Want it… | Do this |
|---|---|
| Hits harder | +2–4 Weapon DmgMod |
| Harder to hit | +1–2 Armor or +10 Reflexes |
| Tankier | +1 band of HP |
| Scarier caster | +1 band of MP + a Sustained spell |
| Faster | +15 Speed (moves it up the order) |

### (e) EDGE-CASE RULINGS
- **A Minion rolls doubles and fails.** Minions do not grow (no XP, no epiphany) — ignore the doubles entirely. Only PCs and recurring named NPCs the GM *chooses* to advance may invent Advanced Skills.
- **An NPC runs out of PE mid-fight.** Standard+ NPCs follow the overflow rule and self-damage; a clever party can "exhaust" a Brawn-heavy brute by forcing repeated defensive Evades until its high rolls bleed it out — a legitimate, intended tactic that rewards Section 7's understanding of opposed costs.

### (f) DESIGNER NOTE
Boss MP/PE pools are deliberately *huge* so the overflow tax doesn't accidentally suicide your climactic villain in three rounds — the pools are the encounter's pacing. **Optional rule (Legendary Actions):** give Bosses one extra Action per round at the cost of doubling their per-turn exertion, modeling overwhelming presence without inventing new mechanics.

---

# 10. EQUIPMENT & INVENTORY ECONOMY

### (a) DESIGN INTENT
Gear in Tiwas modifies the existing math rather than adding parallel rules. A weapon is a **Damage Modifier + an attribute threshold + a governing skill**; armor is an **Armor Rating** subtracted in the Section-6 damage step; carrying capacity is governed by the Bear's **Might (bpp)** exactly as the core hooks specify. Critically, gear can have its own **exertion surcharge** — a too-heavy weapon adds to the PE you pay — so equipment choices feed straight back into the resource-exhaustion engine. And because weapons are tools of action, mastering one is the canonical trigger for **weapon-born Advanced Skills**.

### (b) CORE RULES

**Weapon Statistics.** Each weapon lists:
- **Damage Modifier** — added to the Section-6 damage formula.
- **Attribute Threshold** — the minimum value in a named attribute to wield it cleanly. If your attribute is *below* threshold, every attack with it adds **+(threshold − your value) ÷ 5 (↓)** extra PE to the cost (straining against an unsuited weapon).
- **Governing Skill** — which skill rolls the attack (Might, Impact, Agility, etc.).
- **Tags** — Melee / Ranged / Finesse / Heavy / Two-Handed / Reach.

**Armor & Defense.**
- **Armor Rating (AR)** is subtracted from incoming damage after all bonuses (→ See Section 6).
- Heavier armor carries an **Evade Penalty** (DM to Reflexes Evade rolls, → See Section 7) and adds **Encumbrance**.
- **Shields** provide a **Block Value** (used by the Brawn Block reaction, → Section 6) equal to AR ×2.

**Encumbrance (Might-governed).**
- **Carry Limit = bpp × 2** (in Load units; a typical weapon = 2–6, armor = 4–12).
- Carrying between **bpp×2 and bpp×3**: **Encumbered** — Movement Speed halved (↓), and all Body skill rolls take DM −10.
- Over **bpp×3**: **Overloaded** — Movement = 0; you may only drop gear or make a Might check (Standard) to shuffle one square.

**Tools & Items.** A proper tool grants **DM +10** to its task; an improvised one **−10** (→ Table 7-2). Consumables (potions, scrolls) resolve as an Action with no roll unless contested; a **scroll** casts its stored spell at a fixed skill of **40**, paying MP from the *reader* as normal (overflow → HP — scrolls are not free).

**Weapon-Enabled Advanced Skills.** When you fail a *doubles* attack roll with a specific weapon, the Advanced Skill you create may be **weapon-specific** (e.g., failing Impact with a spear on a 55 → invent **Spearwall**, Tier+1, adding one attribute). Weapon-specific skills get **+5 to their starting value** to represent the focused training, but can only be used with that weapon class.

### (c) MECHANICAL EXAMPLES

> **Wielding under threshold.** Brakka (bpp Might = 30) swings a **Warhammer** (DmgMod +10, Threshold bpp 45, Heavy). She's 15 under threshold → +15÷5 = **+3 PE surcharge** per swing. She rolls 40 to hit → pays 40 + 3 = **43 PE**. The hammer hits like a truck but exhausts her fast — exactly the tradeoff.
>
> **Encumbrance.** Brakka's Carry Limit = bpp×2 = 60. She hauls Plate (Load 12) + Warhammer (6) + pack (10) = 28 — well under 60, unencumbered. Add loot worth 40 Load → total 68 > 60 but < 90 (bpp×3) → **Encumbered**: Movement halved, Body rolls at −10.
>
> **Armor in the damage step.** A Goblin's arrow deals 4 (DmgMod) +1 (Agility) +0 (Margin) = 5. Against Brakka's Plate (AR 8) → **0 damage**. Against a robed mage (AR 0) → full 5.

### (d) TABLES

**Table 10-1: Weapon Stats**
| Weapon | DmgMod | Threshold | Governing Skill | Tags | Load |
|---|---|---|---|---|---|
| Unarmed | +0 | — | Might (bpp) | Melee | 0 |
| Dagger | +3 | bsp 20 | Agility | Finesse, Melee | 1 |
| Shortsword | +6 | bpp 25 | Impact (bps) | Melee | 2 |
| Spear | +6 | bpp 30 | Impact (bps) | Melee, Reach | 3 |
| Warhammer | +10 | bpp 45 | Might (bpp) | Melee, Heavy, 2H | 6 |
| Greatsword | +9 | bpp 40 | Might (bpp) | Melee, 2H | 5 |
| Shortbow | +4 | bsp 25 | Agility (bsp) | Ranged | 2 |
| Longbow | +7 | bsp 40 | Agility (bsp) | Ranged, 2H | 4 |

**Table 10-2: Armor & Shields**
| Armor | AR | Evade Penalty (DM) | Load |
|---|---|---|---|
| Clothes/Robes | 0 | 0 | 1 |
| Leather | 2 | 0 | 4 |
| Chain | 4 | −5 | 8 |
| Plate | 8 | −15 | 12 |
| Buckler (shield) | 1 | 0 | 2 (Block 2) |
| Tower Shield | 3 | −5 | 6 (Block 6) |

**Table 10-3: Encumbrance Bands** (Carry Limit = bpp × 2)
| Load carried | State | Effect |
|---|---|---|
| ≤ bpp×2 | Unburdened | Normal |
| bpp×2 to bpp×3 | Encumbered | Move halved (↓), Body rolls DM −10 |
| > bpp×3 | Overloaded | Move 0; drop gear or Might-check to shuffle 1 sq |

### (e) EDGE-CASE RULINGS
- **Finesse vs Might bonus.** Finesse/Ranged weapons use the **Agility Bonus (bsp÷20)** instead of Might Bonus in the damage formula (→ See Section 6). A dagger-fighter with high bsp but low bpp is fully viable — the Hawk's path, not the Bear's. A weapon never adds *both* bonuses.
- **Evade in heavy armor at 0 PE.** Plate's −15 Evade penalty plus an empty PE pool is a death sentence: the wearer should usually rely on the high Armor Rating and Passive Static Guard (→ Section 6) rather than dodging. This is the intended "tank stands and takes it" play pattern.
- **Two-Handed + Shield conflict.** You cannot Block with a shield while wielding a 2H weapon; you may swap (costs your Move) but not hold both. A character "wielding" both is illegal and the GM voids the shield's Block Value.

### (f) DESIGNER NOTE
The threshold *surcharge* (rather than a flat "you can't use it") keeps the system permissive — anyone *can* swing the Warhammer, they just hemorrhage PE doing it, which self-corrects power-gaming without a hard gate. **Optional rule (Quality Tiers):** add Masterwork (+2 DmgMod or +1 AR, ×3 cost) and Crude (−2 / lower threshold relaxation) tiers to give loot a progression axis without new mechanics.

---

# ☯ TIWAS QUICK REFERENCE SHEET ☯

### CORE LOOP
**Roll d100 ≤ Skill (+DM).** Pay **PE/MP = number rolled.** Overflow → **HP damage.** Round everything **DOWN.**
**Fail:** XP = Roll − Skill. **Fail on doubles:** invent a Tier+1 Advanced Skill.
**After any test:** recover ½ Regen PE/MP.

### KEY DERIVED STATS
| Stat | Formula |
|---|---|
| HP | Σ all 12 Body attrs |
| MP | Σ all 12 Mind attrs |
| PE | bep + bes + bee |
| Speed (initiative) | bsp + bss + bse |
| Movement | (bsp + bss) ÷ 15 |
| Energy Regen / MP Regen | bep+bes / mep+mes |

### COMBAT (§6)
- **Hit:** d100 ≤ Attack Skill; pay PE = roll.
- **Damage = DmgMod + Margin + Atk Bonus − Armor** (min 0).
  - Margin = (Skill − Roll) ÷ 10 · Might Bonus = bpp÷20 (melee) · Agility Bonus = bsp÷20 (ranged/finesse).
- **Evade:** d100 ≤ Reflexes (bss) → negate. **Block:** d100 ≤ Brawn (bpe) → −Block Value. **Passive:** +bss÷5 Armor, free.
- Turn = 1 Action + 1 Move + Reactions (each Reaction costs its roll).

### DIFFICULTY LADDER (§7) — modifies Skill, never cost
Trivial +30 · Easy +15 · Standard 0 · Tricky −10 · Hard −20 · Severe −30 · Heroic −40 · Legendary −50
**Opposed:** both roll & pay; both succeed → bigger Margin wins; both fail → stall. Tie → higher Speed.
**Assist:** +5 per helper (max +15). **Group:** ≥ half succeed.

### MAGIC (§8)
- Spells = Mind-attribute Advanced Skills. Cast: d100 ≤ Spell Skill, pay MP = roll, overflow → HP.
- **Spell Power** +1 per 10 Margin. **MP Floor:** pay at least the Floor.
- **Sustain:** per-turn Willpower (mpe) check + MP; max sustained = mep÷25. Damage forces check at DM −damage.
- Resist mind magic: Opposed vs Resolve (mee) / Composure (mex) / Focus (mep).

### NPC TIERS (§9)
| Tier | HP | MP | PE | Speed | Prime Skill |
|---|---|---|---|---|---|
| Minion | 15–25 | 0–15 | 10–15 | 50–70 | 35–40 |
| Standard | 50–75 | 25–40 | 25–35 | 55–70 | 40–50 |
| Elite | 80–110 | 50–90 | 35–50 | 65–80 | 50–60 |
| Boss | 120–180 | 100–160 | 45–70 | 75–95 | 55–70 |
Minions: die at 0 HP or any hit ≥ ½ HP; ignore exertion; group-Evade.

### EQUIPMENT (§10)
- **Weapon:** DmgMod + Threshold + Governing Skill. Under threshold → +(threshold−attr)÷5 PE surcharge.
- **Armor Rating** subtracts from damage. Shield **Block Value = AR×2.**
- **Carry Limit = bpp×2.** Over ×2 = Encumbered (½ move, −10 Body); over ×3 = Overloaded.
- Proper tool +10 DM · improvised −10 · scroll casts at skill 40 (reader pays MP).

*All active mechanics cost PE/MP = the number rolled. Overflow always bites HP. The animals reward the patient and bleed the reckless.*
