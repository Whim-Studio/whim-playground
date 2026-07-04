# TEMPORARY domain placeholders — DELETE when Task 1's real domain lands

Task 2 (engine, branch `whim-wd-435`) was built in parallel with Task 1 (domain).
When this branch was written, `com.whim.shinobi.domain` did not yet exist, so the
engine ships **throwaway placeholder** domain classes so it can compile and the
headless `EngineSelfCheck` can run.

Each placeholder file carries the banner `// TODO integrate domain — PLACEHOLDER`.
The engine imports `com.whim.shinobi.domain.*` exactly as if these were the real
classes, so **integration is a straight file-for-file replacement**: when Task 1's
real `Aabb.java`, `Entity.java`, `Player.java`, `Enemy.java`, `Projectile.java`,
`Hostage.java`, `Platform.java`, `LevelMap.java`, `WorldState.java`,
`LevelBuilder.java` land, they overwrite these and this README is deleted.

## Contract the engine relies on (so Task 1 keeps the engine compiling unchanged)
Mutable physics/AI state is exposed as **public fields** on the entity classes:
- `Entity`: `Aabb box` (with public `x,y,w,h`), `double vx, vy`,
  `Enums.Plane plane`, `Enums.Facing facing`, `Enums.EntityState state`,
  `boolean alive`, `boolean grounded`.
- `Player`: `int lives, score, ninjutsu`; `Weapon weapon`; `AttackMode lastAttack`;
  `boolean crouch`; `int attackCooldown, invuln`.
- `Enemy(EnemyType)`: `EnemyType type`; `boolean blocking`; `int hp`;
  `double patrolMinX, patrolMaxX`; `int actTimer, blockTimer`; `boolean aggro`.
- `Projectile`: `boolean fromPlayer`; `Weapon weapon`; `int damage, life`.
- `Hostage`: `Aabb box`; `Plane plane`; `boolean rescued`; `RescueReward reward`.
- `Platform`: `Aabb box`; `Plane plane`.
- `WorldState`: `Player player`; `List<Enemy> enemies`; `List<Projectile> projectiles`;
  `List<Hostage> hostages`; `List<Platform> platforms`; `double cameraX`;
  `int levelWidth`; `Phase phase`; `int secondsRemaining`;
  `int hostagesTotal, hostagesRescued`; `double ninjutsuFlash`.
- `Aabb`: public `x,y,w,h`; helpers `cx() cy() right() bottom()`, `overlaps(Aabb)`.
- `LevelBuilder.firstLevel()` → fully-populated `WorldState`.

All concrete classes implement the matching `api.Views` interface.
