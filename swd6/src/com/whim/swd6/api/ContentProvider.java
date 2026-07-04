package com.whim.swd6.api;

import java.util.List;

/**
 * Supplies all static game content: the skill catalog, character templates, the
 * equipment/weapon/armor catalogs, and the bundled test adventure. Implemented by
 * the rules layer (Task 1), consumed by the UI (Task 3) and wired in by Main.
 *
 * Owned by the orchestrator (api).
 */
public interface ContentProvider {

    /** The canonical Revised &amp; Expanded skill list (attribute-grouped order). */
    List<SkillDef> skillCatalog();

    /** Original character-creation templates (each summing to 18D of attributes). */
    List<Template> templates();

    /** Purchasable / starting weapons. */
    List<Weapon> weapons();

    /** Purchasable / starting armor. */
    List<Armor> armorCatalog();

    /** General gear catalog. */
    List<Equipment> equipmentCatalog();

    /** The bundled original test adventure. */
    Scenario scenario();

    /**
     * Build a fresh character from a template: copies attribute codes, seeds
     * suggested (0-added) skills, starting gear/credits/move, and Force flag. The
     * returned character still needs the player's 7D of skill dice allocated.
     */
    PlayerCharacter instantiate(Template template);
}
