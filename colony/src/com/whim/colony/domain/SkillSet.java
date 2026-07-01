package com.whim.colony.domain;

import java.util.EnumMap;
import java.util.Map;

/**
 * A colonist's proficiency across the {@link SkillType} categories. Levels are
 * plain integers (0 = untrained). Unset skills report level 0.
 */
public final class SkillSet {
    public static final int MIN_LEVEL = 0;

    private final Map<SkillType, Integer> levels = new EnumMap<SkillType, Integer>(SkillType.class);

    public SkillSet() {
    }

    /** @return the level for {@code skill}, or {@link #MIN_LEVEL} if unset. */
    public int getLevel(SkillType skill) {
        Integer level = levels.get(skill);
        return level == null ? MIN_LEVEL : level.intValue();
    }

    /** Set the level for {@code skill}, flooring negatives at {@link #MIN_LEVEL}. */
    public void setLevel(SkillType skill, int level) {
        if (skill == null) {
            return;
        }
        levels.put(skill, level < MIN_LEVEL ? MIN_LEVEL : level);
    }

    /** A live view of the underlying map; useful for the UI to iterate skills. */
    public Map<SkillType, Integer> asMap() {
        return levels;
    }
}
