# Star Command — Game Design Document (living)

Spirit-and-mechanics recreation of *Star Command* (SSI, 1988). This is the source of truth for
design decisions; **[ASSUMPTION]** marks a choice made where historical sources are thin or
conflicting.

## 1. Setting
Far future; Earth destroyed by aliens. Humanity survives in **The Triangle**, bordered by the
pirate **Alpha Frontier** and the insectoid **Beta Frontier**. The player is a Star Command
officer building a crew and ship and taking HQ missions — the marquee arc is capturing the
pirate warlord **Blackbeard**.

## 2. Game loop
HQ/Starport → outfit crew & ship → Galaxy map → encounter → ship combat → disable → board/capture
→ mission complete → return to HQ → repeat up the mission ladder → endgame (Blackbeard).

## 3. Screen / menu tree
- **Main Menu** — New / Continue / Help / Quit
- **Character Creation** — roll crew of up to 8, choose role, name
- **Starport (HQ)** — ship & crew status, weapon bay, shipyard trade-up, repair, briefing room
  (accept missions), save, launch
- **Galaxy Map** — 8×6 sector grid; move, scan, dock
- **Ship Combat** — turn-based action menu
- **Ground Combat** — tactical tile grid; squad move + attack, enemy AI
- **Help** — controls reference
- *(next)* multi-room unique-area crawl; external JSON content

## 4. Data model
- **Character** — strength, speed, accuracy, intellect, leadership, willpower (**[ASSUMPTION]**
  each 3..18 via 3d6, role bias, cap 20); derived HP = `10 + STR + WILL/2`.
- **Ship** — className, hull, shield, engines, weaponSlots, weapons[]; `disabled` when hull hits 0
  without exploding → boardable.
- **Weapon** — BEAM (accurate) or MISSILE (harder-hitting, less accurate); min/max dmg, accuracy, cost.
- **Sector** — frontier (CORE/ALPHA/BETA), optional Planet, hostilePresence, visited.
- **Mission** — id, title, briefing, reward, targetSector, accepted/complete.
- **GameState** — crew, ship, credits, 8×6 galaxy, ship position, missions, turn, gameWon.

## 5. Roles (skill loadouts)
Pilot, Marine, Esper, Medic, Engineer, Scout. Each nudges the relevant aptitudes at roll time,
standing in for the original's training-school specialization. **[ASSUMPTION]** exact school list
and progression are simplified.

## 6. Economy
Currency = credits (start 5000). Sinks: weapons (600–3000), ship trade-up (Corvette 4000 →
Cruiser 18000), repairs (3cr/point). Sources: bounties (destroy) and salvage (capture, larger),
mission rewards.

## 7. Combat model
One action/round; engine resolves player then enemy. Shields absorb before hull. **Disable** is a
called shot at engines: reduced damage, −15 accuracy, but leaves the enemy boardable for a larger
payout. **[ASSUMPTION]** original's exact to-hit/subsystem math undocumented; values here are
tunable and balance-swept (opening raider winnable 200/200 with starting Scout).

## 8. Win/loss
Win: capture/destroy Blackbeard's flagship (mission `m_blackbeard`). Loss: player ship destroyed.
**[ASSUMPTION]** the original's full late-game chain is fuzzy in secondary sources; v1 ends at
Blackbeard, extensible via the mission ladder.

## 9. Controls
Mouse + keyboard on every screen; shortcuts documented in README and the in-game Help screen,
kept mnemonic (numbers for combat actions, first-letter for menu commands).

## 10. Ground / boarding combat (implemented)
Tile grid (10×8). Player squad = living crew; a unit moves up to `moveRange` tiles (from speed)
then attacks within `attackRange` (Marines/Pilots reach 4, others 3). To-hit = unit accuracy −
target mobility; damage scales off strength. Enemy AI steps toward and attacks the nearest
operative. Reached by **boarding** a disabled ship or **deploying** a drop ship onto a hostile
base/hive. Wounds and deaths write back to the crew roster. Win = clear the area (loot / capture);
capturing Blackbeard on his deck is an alternate win path to destroying his flagship in space.
Balance sweep: standard boarding 300/300, Blackbeard's deck ~94% with a full squad.

## 11. Unique-area crawl (implemented)
Drop-shipping onto a base/hive opens a 4×6 room complex under fog of war: entrance at the near
corner, objective at the far corner, with enemy and loot rooms between. Moving into an uncleared
enemy room hands off to the tactical grid (`GroundCombat`) and returns on victory; loot rooms pay
credits; the objective room is the goal — Blackbeard's stronghold (a **third** win path alongside
destroying or boarding his flagship) or a hive core. Extract from the entrance. Rooms are fully
connected (no walls yet). Reveal marks adjacent rooms; wounds persist across rooms via the crew
roster.

Unique areas are now **corridor mazes**: a randomized spanning-tree carve guarantees every room
(and the objective) is reachable from the entrance, plus a few extra doorways for loops; movement
is gated on doorways and fog reveals along corridors.

## 12. Data-driven content (implemented)
Ships, weapons and crew roles load from `data/*.json` via a minimal dependency-free reader
(`engine.Json`); `engine.Content` parses and caches them. Enemy stat blocks and missions remain
in code for now.

## 13. Open items / next milestones
1. Locked-door / keycard gates inside unique areas.
2. Externalize enemy stat blocks and the mission ladder to JSON too.
3. Broader mission variety and a Beta Frontier campaign.
