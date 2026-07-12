package com.whim.capes.content;

import java.util.Arrays;
import java.util.List;

import com.whim.capes.model.ConflictType;

/**
 * A Chapter 5 non-person participant template (pp.101-111): Things, Locations,
 * Phenomena and Situations. Non-persons have the mundane ability columns
 * (Actions/Skills, Attitudes, Styles) but no Powers and no Drives (p.102). Some
 * carry a built-in Character Conflict — a Free Event/Goal that, when Resolved,
 * removes the participant from the Scene (p.103).
 */
public final class NonPersonTemplate {
    public enum Category { THING, LOCATION, PHENOMENON, SITUATION }

    private final String name;
    private final Category category;
    private final List<String> actions;   // Skill-like column
    private final List<String> attitudes;
    private final List<String> styles;
    private final ConflictType freeConflictType;   // null if none
    private final String freeConflictStatement;    // null if none

    public NonPersonTemplate(String name, Category category, List<String> actions,
                             List<String> attitudes, List<String> styles,
                             ConflictType freeConflictType, String freeConflictStatement) {
        this.name = name;
        this.category = category;
        this.actions = actions;
        this.attitudes = attitudes;
        this.styles = styles;
        this.freeConflictType = freeConflictType;
        this.freeConflictStatement = freeConflictStatement;
    }

    public String name() { return name; }
    public Category category() { return category; }
    public List<String> actions() { return actions; }
    public List<String> attitudes() { return attitudes; }
    public List<String> styles() { return styles; }
    public boolean hasFreeConflict() { return freeConflictType != null; }
    public ConflictType freeConflictType() { return freeConflictType; }
    public String freeConflictStatement() { return freeConflictStatement; }

    static List<String> list(String... items) { return Arrays.asList(items); }

    @Override public String toString() { return name + " (" + category + ")"; }
}
