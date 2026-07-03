# Engine → Domain contract (Task 2 assumptions)

The engine (Task 2) was built and verified against the following
`com.whim.populous.domain` API, derived from `POPULOUS_CONTRACT.md`. Task 1 owns
these classes; this file records the EXACT signatures the engine calls so the
orchestrator can reconcile any drift. All were compile- and runtime-verified
against a faithful throwaway stub (deleted before commit); `EngineSmokeTest`
passed end-to-end.

## GameStateManager  (the object the engine owns and drives)
```java
static GameStateManager create(long seed);   // builds 64x64 world, spawns initial GOOD+EVIL followers on flat starting land
GameState        state();                     // live GameStateView the engine snapshots
MapGrid          map();
java.util.List<Follower>   followers();       // LIVE mutable list (engine adds births, removes dead)
java.util.List<Settlement> settlements();     // LIVE mutable list
PapalMagnet      magnet(Allegiance side);     // never null; one per side

int   getMana(Allegiance side);
void  setMana(Allegiance side, int value);
int   maxMana();
int   population(Allegiance side);            // count of ALIVE followers of side
int   populationCap();

long  getTick();
void  setTick(long t);

GodPower selectedPower();
void     setSelectedPower(GodPower p);

boolean  isGameOver();
Allegiance winner();
void     setGameOver(Allegiance winner);      // sets over=true and winner

String  statusLine();
void    setStatusLine(String s);
```

## MapGrid implements Views.MapView
```java
// from MapView: int cols(); int rows(); int seaLevel(); TileView tileAt(int col,int row);
void        setSeaLevel(int v);               // FLOOD raises/restores this
boolean     inBounds(int col, int row);
int         elevationAt(int col, int row);
void        setElevation(int col, int row, int v);
void        raise(int col, int row, int radius);   // brush: raise + smooth toward neighbours
void        lower(int col, int row, int radius);   // brush: lower + smooth
int         flatAreaAt(int col, int row);          // size of contiguous same-elevation, above-sea plateau (capped ok)
void        setTerrainOverride(int col, int row, TerrainType t);  // t==null clears (SWAMP/LAVA/ROCK transient marks)
void        setOwner(int col, int row, Allegiance a);
void        setSettlement(int col, int row, SettlementType type, int level);
```

## Follower implements Views.FollowerView  (mutable)
```java
Follower(Allegiance a, double x, double y);
// from FollowerView: double x(); double y(); Allegiance allegiance(); int health(); int stamina(); boolean alive();
void setX(double x);
void setY(double y);
void setHealth(int h);      // clamp 0..100 in domain
void setStamina(int s);     // clamp 0..100 in domain
void setAlive(boolean a);
```

## Settlement
```java
Settlement(Allegiance owner, int col, int row, SettlementType type);
Allegiance owner();  int col();  int row();
SettlementType type();  void setType(SettlementType t);
int level();  void setLevel(int l);
// (population()/setPopulation(int) referenced but optional)
```

## PapalMagnet implements Views.PapalMagnetView
```java
PapalMagnet(Allegiance side);
// from view: boolean active(); int col(); int row(); Allegiance side();
void activate(int col, int row);   // sets position + active=true
void deactivate();
```

## GameState implements Views.GameStateView
Straight implementation of the api view; the engine only READS it (then copies
into an immutable `Snapshots.SnapState`). `powerAffordable(p)` is assumed to mean
`getMana(GOOD) >= p.manaCost()` for the player HUD.

### Notes / friction
- Terrain thresholds are Task 1's source of truth; the engine only relies on
  `terrain()` returning WATER/SHALLOW/SWAMP for drowning and LAVA for damage.
- The engine keeps ALL per-follower AI scratch in its own IdentityHashMap, so
  `Follower` needs no AI fields.
- If Task 1 named the brush methods differently (e.g. `raiseBrush`) or used
  `getMana`/`mana` variants, only `DivinePowers`, `MapGrid` call-sites need a
  rename — the algorithms are unaffected.
