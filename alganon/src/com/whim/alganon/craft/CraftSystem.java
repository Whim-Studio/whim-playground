package com.whim.alganon.craft;

import com.whim.alganon.api.ActionResult;
import com.whim.alganon.api.CharacterModel;
import com.whim.alganon.api.Content;
import com.whim.alganon.api.Defs.GatherNodeDef;
import com.whim.alganon.api.Defs.ItemDef;
import com.whim.alganon.api.Defs.RecipeDef;
import com.whim.alganon.api.Enums.ChatChannel;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.GameContext;
import com.whim.alganon.api.GameModel;
import com.whim.alganon.api.WorldModel;
import com.whim.alganon.api.WorldModel.NodeEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gather → process → craft tradeskills plus the NPC-populated auction/requisition house
 * (single-player economy substitute). Skill checks gate gathering and crafting; gains
 * feed the use-based skill system through {@link Callbacks}.
 */
public final class CraftSystem {

    /** Callbacks so the engine can award skill use and quest gather progress. */
    public interface Callbacks {
        void onGathered(String itemId, int qty);
        void onSkillUsed(SkillType skill, double amount);
    }

    private final Content content;
    private final GameContext ctx;
    private final Callbacks cb;
    private final List<Listing> listings = new ArrayList<Listing>();
    private int listingSeq = 1;

    public CraftSystem(Content content, GameContext ctx, Callbacks cb) {
        this.content = content;
        this.ctx = ctx;
        this.cb = cb;
    }

    public List<Listing> listings() { return listings; }

    public void reset() {
        listings.clear();
        listingSeq = 1;
    }

    /** Seed the auction house with NPC listings drawn from craftable outputs + materials. */
    public void seedListings() {
        listings.clear();
        int seeded = 0;
        for (RecipeDef r : content.recipes()) {
            ItemDef out = content.item(r.outputItemId);
            if (out == null) continue;
            long price = Math.max(1, out.value) * 2L;
            listings.add(new Listing(nextId(), out.id, 1, price, false));
            if (++seeded >= 8) break;
        }
        // A couple of raw-material listings so buying is always possible.
        for (RecipeDef r : content.recipes()) {
            for (String inId : r.inputs.keySet()) {
                ItemDef in = content.item(inId);
                if (in == null) continue;
                listings.add(new Listing(nextId(), in.id, 5, Math.max(1, in.value) * 5L, false));
                seeded++;
                break;
            }
            if (seeded >= 12) break;
        }
    }

    // ---------- gathering ----------

    public ActionResult gather(GameModel model, String nodeId) {
        WorldModel world = model.world();
        if (world == null) return ActionResult.fail("Nothing to gather here.");
        NodeEntity node = null;
        for (NodeEntity n : world.nodes()) {
            if (n.id().equals(nodeId)) { node = n; break; }
        }
        if (node == null) return ActionResult.fail("No such resource node.");
        if (node.depleted()) return ActionResult.fail(node.name() + " is depleted.");

        GatherNodeDef def = content.gatherNode(nodeId);
        if (def == null) return ActionResult.fail("Unknown resource node.");

        CharacterModel p = model.player();
        if (p.skill(SkillType.GATHERING) < def.skillReq) {
            return ActionResult.fail("Requires Gathering skill " + def.skillReq + ".");
        }
        int span = Math.max(0, def.yieldMax - def.yieldMin);
        int qty = def.yieldMin + (span > 0 ? ctx.rng().nextInt(span + 1) : 0);
        if (qty <= 0) qty = Math.max(1, def.yieldMin);

        p.addItem(def.itemId, qty);
        node.setDepleted(true);
        node.setRespawnRemaining(def.respawnSec);

        cb.onGathered(def.itemId, qty);
        cb.onSkillUsed(SkillType.GATHERING, qty);
        ctx.log(ChatChannel.LOOT, "Gathered " + qty + "x " + itemName(def.itemId) + ".");
        return ActionResult.ok("Gathered " + qty + "x " + itemName(def.itemId) + ".");
    }

    /** Advance node respawn timers (called from the engine tick). */
    public void tick(GameModel model, double dt) {
        WorldModel world = model.world();
        if (world == null) return;
        for (NodeEntity n : world.nodes()) {
            if (n.depleted()) {
                double rem = n.respawnRemaining() - dt;
                if (rem <= 0) {
                    n.setDepleted(false);
                    n.setRespawnRemaining(0);
                } else {
                    n.setRespawnRemaining(rem);
                }
            }
        }
    }

    // ---------- crafting ----------

    public ActionResult craft(GameModel model, String recipeId) {
        RecipeDef r = content.recipe(recipeId);
        if (r == null) return ActionResult.fail("Unknown recipe.");
        CharacterModel p = model.player();
        if (p.skill(r.skill) < r.skillReq) {
            return ActionResult.fail("Requires " + label(r.skill) + " skill " + r.skillReq + ".");
        }
        // verify inputs
        Map<String, Integer> inv = p.inventory();
        for (Map.Entry<String, Integer> in : r.inputs.entrySet()) {
            int have = inv.containsKey(in.getKey()) ? inv.get(in.getKey()) : 0;
            if (have < in.getValue()) {
                return ActionResult.fail("Missing " + in.getValue() + "x " + itemName(in.getKey()) + ".");
            }
        }
        // consume + produce
        for (Map.Entry<String, Integer> in : r.inputs.entrySet()) {
            p.removeItem(in.getKey(), in.getValue());
        }
        p.addItem(r.outputItemId, r.outputQty);
        cb.onSkillUsed(r.skill, r.skillReq > 0 ? r.skillReq : 1);
        ctx.log(ChatChannel.SYSTEM, "Crafted " + r.outputQty + "x " + itemName(r.outputItemId) + ".");
        return ActionResult.ok("Crafted " + itemName(r.outputItemId) + ".");
    }

    public boolean isCraftable(GameModel model, RecipeDef r) {
        CharacterModel p = model.player();
        if (p.skill(r.skill) < r.skillReq) return false;
        Map<String, Integer> inv = p.inventory();
        for (Map.Entry<String, Integer> in : r.inputs.entrySet()) {
            int have = inv.containsKey(in.getKey()) ? inv.get(in.getKey()) : 0;
            if (have < in.getValue()) return false;
        }
        return true;
    }

    // ---------- auction ----------

    public ActionResult buy(GameModel model, String listingId) {
        Listing found = null;
        for (Listing l : listings) if (l.listingId.equals(listingId)) { found = l; break; }
        if (found == null) return ActionResult.fail("Listing no longer available.");
        CharacterModel p = model.player();
        if (p.gold() < found.price) return ActionResult.fail("Not enough gold.");
        p.addGold(-found.price);
        p.addItem(found.itemId, found.quantity);
        listings.remove(found);
        if (found.sellerIsPlayer) {
            // reimbursing yourself makes no sense; NPC purchase of your goods returns gold instead
        }
        ctx.log(ChatChannel.LOOT, "Bought " + found.quantity + "x " + itemName(found.itemId)
                + " for " + found.price + " gold.");
        return ActionResult.ok("Purchased " + itemName(found.itemId) + ".");
    }

    public ActionResult post(GameModel model, String itemId, int quantity, long price) {
        if (quantity <= 0) return ActionResult.fail("Quantity must be positive.");
        if (price < 0) return ActionResult.fail("Price must be non-negative.");
        CharacterModel p = model.player();
        int have = p.inventory().containsKey(itemId) ? p.inventory().get(itemId) : 0;
        if (have < quantity) return ActionResult.fail("You don't have " + quantity + "x that item.");
        p.removeItem(itemId, quantity);
        listings.add(new Listing(nextId(), itemId, quantity, price, true));
        ctx.log(ChatChannel.SYSTEM, "Posted " + quantity + "x " + itemName(itemId)
                + " for " + price + " gold.");
        return ActionResult.ok("Listing posted.");
    }

    // ---------- persistence hooks (player listings only) ----------

    public List<Listing> exportPlayerListings() {
        List<Listing> out = new ArrayList<Listing>();
        for (Listing l : listings) if (l.sellerIsPlayer) out.add(l);
        return out;
    }

    public void importListings(List<Listing> saved) {
        for (Listing l : saved) {
            listings.add(l);
            int n = parseSeq(l.listingId);
            if (n >= listingSeq) listingSeq = n + 1;
        }
    }

    // ---------- helpers ----------

    private String nextId() { return "L" + (listingSeq++); }

    private static int parseSeq(String id) {
        try { return Integer.parseInt(id.replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { return 0; }
    }

    private String itemName(String itemId) {
        ItemDef d = content.item(itemId);
        return d != null ? d.name : itemId;
    }

    private static String label(SkillType s) {
        String n = s.name().toLowerCase();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }
}
