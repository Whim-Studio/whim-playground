package com.whim.swd6.api;

import java.util.ArrayList;
import java.util.List;

/**
 * A playable branching adventure. Original content only (no published-module text).
 * A scenario is a graph of {@link Scene}s connected by ids. The adventure UI walks
 * scenes: narrating text, resolving skill checks through the engine, running combat
 * encounters, and following the player's decisions.
 *
 * Owned by the orchestrator (api). Concrete instances supplied by the rules layer
 * via {@link ContentProvider#scenario()}.
 */
public final class Scenario {

    /** How a scene is presented and resolved. */
    public enum SceneType {
        NARRATIVE,   // text + choices (or a single "continue")
        SKILL_CHECK, // roll skillName vs targetNumber -> successNext / failureNext
        COMBAT,      // fight enemies -> victoryNext / defeatNext
        DECISION,    // text + multiple branching choices
        ENDING       // terminal scene
    }

    /** A branch option shown to the player. */
    public static final class Choice {
        private final String label;
        private final String nextSceneId;

        public Choice(String label, String nextSceneId) {
            this.label = label;
            this.nextSceneId = nextSceneId;
        }

        public String getLabel() { return label; }
        public String getNextSceneId() { return nextSceneId; }
    }

    /** One node of the adventure graph. */
    public static final class Scene {
        private String id = "";
        private String title = "";
        private String text = "";
        private SceneType type = SceneType.NARRATIVE;

        private final List<Choice> choices = new ArrayList<Choice>();

        // SKILL_CHECK fields
        private String skillName = "";
        private int targetNumber = 10;
        private String successNext = "";
        private String failureNext = "";

        // COMBAT fields
        private final List<Combatant> enemies = new ArrayList<Combatant>();
        private String victoryNext = "";
        private String defeatNext = "";

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public SceneType getType() { return type; }
        public void setType(SceneType type) { this.type = type; }

        public List<Choice> getChoices() { return choices; }

        public String getSkillName() { return skillName; }
        public void setSkillName(String skillName) { this.skillName = skillName; }

        public int getTargetNumber() { return targetNumber; }
        public void setTargetNumber(int targetNumber) { this.targetNumber = targetNumber; }

        public String getSuccessNext() { return successNext; }
        public void setSuccessNext(String successNext) { this.successNext = successNext; }

        public String getFailureNext() { return failureNext; }
        public void setFailureNext(String failureNext) { this.failureNext = failureNext; }

        public List<Combatant> getEnemies() { return enemies; }

        public String getVictoryNext() { return victoryNext; }
        public void setVictoryNext(String victoryNext) { this.victoryNext = victoryNext; }

        public String getDefeatNext() { return defeatNext; }
        public void setDefeatNext(String defeatNext) { this.defeatNext = defeatNext; }
    }

    private String title = "";
    private String synopsis = "";
    private String startSceneId = "";
    private final List<Scene> scenes = new ArrayList<Scene>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }

    public String getStartSceneId() { return startSceneId; }
    public void setStartSceneId(String startSceneId) { this.startSceneId = startSceneId; }

    public List<Scene> getScenes() { return scenes; }

    /** Look up a scene by id, or null if absent. */
    public Scene sceneById(String id) {
        for (Scene s : scenes) {
            if (s.getId().equals(id)) {
                return s;
            }
        }
        return null;
    }
}
