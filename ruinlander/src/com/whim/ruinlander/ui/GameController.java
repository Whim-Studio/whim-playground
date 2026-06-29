package com.whim.ruinlander.ui;

import com.whim.ruinlander.domain.Armor;
import com.whim.ruinlander.domain.Enemy;
import com.whim.ruinlander.domain.Entity;
import com.whim.ruinlander.domain.GameMode;
import com.whim.ruinlander.domain.GameStateManager;
import com.whim.ruinlander.domain.GridMap;
import com.whim.ruinlander.domain.Item;
import com.whim.ruinlander.domain.ItemStack;
import com.whim.ruinlander.domain.Player;
import com.whim.ruinlander.domain.Position;
import com.whim.ruinlander.domain.Settlement;
import com.whim.ruinlander.domain.StatType;
import com.whim.ruinlander.domain.Tile;
import com.whim.ruinlander.domain.Weapon;
import com.whim.ruinlander.engine.AttackOutcome;
import com.whim.ruinlander.engine.CombatEngine;
import com.whim.ruinlander.engine.CombatState;
import com.whim.ruinlander.engine.CraftingSystem;
import com.whim.ruinlander.engine.EncounterGenerator;
import com.whim.ruinlander.engine.LootGenerator;
import com.whim.ruinlander.engine.Recipe;
import com.whim.ruinlander.engine.SurvivalEngine;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Central hub: owns the {@link GameStateManager} and every engine object, and
 * routes keyboard/mouse input into engine calls then repaints. The UI never
 * mutates player stats directly and never rolls its own RNG — all randomness
 * flows through the single seeded {@link Random} shared with the engines.
 */
public class GameController implements KeyListener {

    private static final long SEED = 1337L;

    private final GameStateManager gsm;
    private final SurvivalEngine survival = new SurvivalEngine();
    private final CombatEngine combat;
    private final EncounterGenerator encounters;
    private final LootGenerator loot;
    private final CraftingSystem crafting = new CraftingSystem();

    // UI panels (set by GameFrame after construction).
    private MapPanel mapPanel;
    private StatusPanel statusPanel;
    private LogPanel logPanel;
    private InventoryPanel inventoryPanel;

    private int selectedEnemy = 0;

    public GameController(GameStateManager gsm) {
        this.gsm = gsm;
        Random rng = new Random(SEED);
        this.combat = new CombatEngine(rng);
        this.encounters = new EncounterGenerator(rng);
        this.loot = new LootGenerator(rng);
    }

    public void wire(MapPanel map, StatusPanel status, LogPanel log, InventoryPanel inv) {
        this.mapPanel = map;
        this.statusPanel = status;
        this.logPanel = log;
        this.inventoryPanel = inv;
        log("You awaken in the wasteland. Survive. (WASD/arrows move · I inventory · C craft)");
    }

    // ---- accessors used by the panels --------------------------------------

    public GameStateManager getStateManager() {
        return gsm;
    }

    public CombatState getCombatState() {
        Object cs = gsm.getCombatState();
        return (cs instanceof CombatState) ? (CombatState) cs : null;
    }

    public CraftingSystem getCrafting() {
        return crafting;
    }

    public int getSelectedEnemy() {
        return selectedEnemy;
    }

    // ---- input -------------------------------------------------------------

    @Override
    public void keyPressed(KeyEvent e) {
        switch (gsm.getMode()) {
            case EXPLORATION:
                handleExploration(e);
                break;
            case COMBAT:
                handleCombat(e);
                break;
            case INVENTORY:
                handleInventory(e);
                break;
            case CRAFTING:
                handleCrafting(e);
                break;
            case GAME_OVER:
            default:
                break;
        }
        repaintAll();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private void handleExploration(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_I) {
            gsm.setMode(GameMode.INVENTORY);
            return;
        }
        if (code == KeyEvent.VK_C) {
            gsm.setMode(GameMode.CRAFTING);
            return;
        }
        int[] d = direction(code);
        if (d != null) {
            attemptMove(d[0], d[1]);
        }
    }

    private void attemptMove(int dx, int dy) {
        Player p = gsm.getPlayer();
        GridMap map = gsm.getMap();
        Position cur = p.getPosition();
        int nx = cur.x + dx;
        int ny = cur.y + dy;
        if (!map.inBounds(nx, ny)) {
            log("The edge of the known world blocks your path.");
            return;
        }
        Tile t = map.getTile(nx, ny);
        if (t.hasEntity()) {
            interact(t.getEntity(), t);
            return;
        }
        if (!t.isPassable()) {
            log("Impassable terrain (" + t.getTerrain() + ").");
            return;
        }

        // Commit the move.
        p.setPosition(new Position(nx, ny));
        t.setDiscovered(true);
        discoverAround(map, nx, ny);
        gsm.incrementTurn();

        List<String> notes = survival.applyStep(p, t.getTerrain());
        for (String n : notes) {
            log(n);
        }
        if (p.isDead()) {
            gameOver("The wasteland claims you.");
            return;
        }

        List<Enemy> ambush = encounters.maybeEncounter(t.getTerrain(), gsm.getTurnCount());
        if (!ambush.isEmpty()) {
            startCombat(ambush);
        }
    }

    private void interact(Entity ent, Tile tile) {
        if (ent instanceof Enemy) {
            List<Enemy> group = new ArrayList<Enemy>();
            group.add((Enemy) ent);
            tile.setEntity(null); // pulled into the encounter; removed from the map
            startCombat(group);
        } else if (ent instanceof com.whim.ruinlander.domain.Container) {
            com.whim.ruinlander.domain.Container c = (com.whim.ruinlander.domain.Container) ent;
            if (c.isLooted()) {
                log("The container is empty.");
                return;
            }
            // Lazily stock loot the first time it is opened, then transfer it.
            if (c.getLoot().isEmpty()) {
                loot.fill(c.getLoot(), tile.getTerrain());
            }
            int taken = 0;
            for (ItemStack s : new ArrayList<ItemStack>(c.getLoot().getStacks())) {
                int added = gsm.getPlayer().getInventory().add(s.getItem(), s.getQuantity());
                taken += added;
            }
            c.getLoot().getStacks().clear();
            c.setLooted(true);
            log(taken > 0 ? "You scavenge " + taken + " item(s) from the cache." : "Your pack is too full.");
        } else if (ent instanceof Settlement) {
            Settlement s = (Settlement) ent;
            Player p = gsm.getPlayer();
            p.addStat(StatType.FATIGUE, -40);
            p.addStat(StatType.TEMPERATURE, 20);
            p.addReputation(s.getFaction(), 2);
            log("You rest at " + s.getName() + " (" + s.getFaction() + "). Fatigue eases.");
        }
    }

    private void startCombat(List<Enemy> enemies) {
        CombatState cs = combat.start(gsm.getPlayer(), enemies);
        gsm.enterCombat(cs);
        selectedEnemy = 0;
        for (String line : cs.getLog()) {
            log(line);
        }
    }

    private void handleCombat(KeyEvent e) {
        CombatState cs = getCombatState();
        if (cs == null) {
            gsm.setMode(GameMode.EXPLORATION);
            return;
        }
        int code = e.getKeyCode();
        List<Enemy> alive = cs.aliveEnemies();

        // Number keys select a target.
        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
            int idx = code - KeyEvent.VK_1;
            if (idx < alive.size()) {
                selectedEnemy = idx;
                log("Target: " + alive.get(idx).getName() + ".");
            }
            return;
        }
        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_F) {
            if (!alive.isEmpty()) {
                if (selectedEnemy >= alive.size()) {
                    selectedEnemy = 0;
                }
                AttackOutcome o = combat.playerAttack(cs, gsm.getPlayer(), alive.get(selectedEnemy));
                log(o.getMessage());
                afterPlayerAction(cs);
            }
            return;
        }
        if (code == KeyEvent.VK_E) {
            endPlayerTurn(cs);
            return;
        }
        int[] d = direction(code);
        if (d != null) {
            AttackOutcome o = combat.playerMove(cs, gsm.getPlayer(), d[0], d[1]);
            log(o.getMessage());
            afterPlayerAction(cs);
        }
    }

    /** After any player combat action: check victory, else auto-end turn when AP runs out. */
    private void afterPlayerAction(CombatState cs) {
        if (combat.playerWon(cs)) {
            victory(cs);
            return;
        }
        if (gsm.getPlayer().getActionPoints() <= 0) {
            endPlayerTurn(cs);
        }
    }

    private void endPlayerTurn(CombatState cs) {
        List<AttackOutcome> outcomes = combat.enemyTurn(cs, gsm.getPlayer());
        for (AttackOutcome o : outcomes) {
            log(o.getMessage());
        }
        if (combat.playerLost(gsm.getPlayer())) {
            gameOver("You fall in battle.");
            return;
        }
        if (combat.playerWon(cs)) {
            victory(cs);
            return;
        }
        log("-- Round " + cs.getRound() + " -- Your move (AP " + gsm.getPlayer().getActionPoints() + ").");
    }

    private void victory(CombatState cs) {
        log("The threat is eliminated. You loot the fallen.");
        // Small reward: scrap + a chance the engine-driven loot adds supplies.
        Item scrap = com.whim.ruinlander.engine.ItemDb.get("scrap");
        if (scrap != null) {
            gsm.getPlayer().getInventory().add(scrap, 1);
        }
        gsm.endCombat();
    }

    private void handleInventory(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_I || code == KeyEvent.VK_ESCAPE) {
            gsm.setMode(GameMode.EXPLORATION);
            return;
        }
        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
            int idx = code - KeyEvent.VK_1;
            useInventoryItem(idx);
        }
    }

    private void useInventoryItem(int idx) {
        Player p = gsm.getPlayer();
        List<ItemStack> stacks = p.getInventory().getStacks();
        if (idx < 0 || idx >= stacks.size()) {
            return;
        }
        Item item = stacks.get(idx).getItem();
        if (item instanceof Weapon) {
            p.equipWeapon((Weapon) item);
            log("Equipped " + item.getName() + ".");
        } else if (item instanceof Armor) {
            p.equipArmor((Armor) item);
            log("Donned " + item.getName() + ".");
        } else if (item.isConsumable()) {
            List<String> notes = survival.consume(p, item);
            p.getInventory().remove(item.getId(), 1);
            for (String n : notes) {
                log(n);
            }
        } else {
            log(item.getName() + " has no immediate use.");
        }
    }

    private void handleCrafting(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_C || code == KeyEvent.VK_ESCAPE) {
            gsm.setMode(GameMode.EXPLORATION);
            return;
        }
        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
            int idx = code - KeyEvent.VK_1;
            List<Recipe> recipes = crafting.getRecipes();
            if (idx < recipes.size()) {
                Recipe r = recipes.get(idx);
                ItemStack out = crafting.craft(gsm.getPlayer().getInventory(), r);
                if (out != null) {
                    log("Crafted " + out.getQuantity() + "x " + out.getItem().getName() + ".");
                } else {
                    log("Missing materials for " + r.getName() + ".");
                }
            }
        }
    }

    // ---- mouse (combat target selection) -----------------------------------

    /** Called by {@link MapPanel} when a grid cell is clicked. */
    public void handleGridClick(int gx, int gy) {
        if (gsm.getMode() != GameMode.COMBAT) {
            return;
        }
        CombatState cs = getCombatState();
        if (cs == null) {
            return;
        }
        List<Enemy> alive = cs.aliveEnemies();
        for (int i = 0; i < alive.size(); i++) {
            Position ep = alive.get(i).getPosition();
            if (ep != null && ep.x == gx && ep.y == gy) {
                selectedEnemy = i;
                log("Target: " + alive.get(i).getName() + ".");
                repaintAll();
                return;
            }
        }
    }

    // ---- helpers -----------------------------------------------------------

    private void gameOver(String reason) {
        log(reason);
        log("GAME OVER. Turns survived: " + gsm.getTurnCount() + ".");
        gsm.setMode(GameMode.GAME_OVER);
    }

    private void discoverAround(GridMap map, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (map.inBounds(x + dx, y + dy)) {
                    map.getTile(x + dx, y + dy).setDiscovered(true);
                }
            }
        }
    }

    private int[] direction(int code) {
        switch (code) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                return new int[]{0, -1};
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                return new int[]{0, 1};
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                return new int[]{-1, 0};
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                return new int[]{1, 0};
            default:
                return null;
        }
    }

    private void log(String msg) {
        if (logPanel != null) {
            logPanel.append(msg);
        }
    }

    private void repaintAll() {
        if (mapPanel != null) {
            mapPanel.repaint();
        }
        if (statusPanel != null) {
            statusPanel.refresh();
        }
        if (inventoryPanel != null) {
            inventoryPanel.refresh();
        }
    }
}
