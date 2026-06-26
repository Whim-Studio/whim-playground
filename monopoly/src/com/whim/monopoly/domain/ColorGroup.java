package com.whim.monopoly.domain;

public enum ColorGroup {
    BROWN("Brown"), LIGHT_BLUE("Light Blue"), PINK("Pink"), ORANGE("Orange"),
    RED("Red"), YELLOW("Yellow"), GREEN("Green"), DARK_BLUE("Dark Blue");
    private final String label;
    ColorGroup(String label) { this.label = label; }
    public String getLabel() { return label; }
}
