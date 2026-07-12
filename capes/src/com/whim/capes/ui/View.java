package com.whim.capes.ui;

/**
 * The top-level views switched via CardLayout in {@link MainFrame}. Each maps
 * to one CardLayout key and one nav button. Character Creation and Character
 * Sheet arrive in Phase 2; the Table View (core game loop) in Phase 3.
 */
public enum View {
    CHARACTER_CREATION("Character Creation"),
    TABLE("Table"),
    CHARACTER_SHEET("Character Sheet"),
    COMICS_CODE("Comics Code"),
    RULES_HELP("Rules Reference");

    private final String label;
    View(String label) { this.label = label; }
    public String label() { return label; }
    public String key() { return name(); }
}
