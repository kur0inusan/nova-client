package dev.kuroinu.nova.module;

/** Menu categories, in navigation order. */
public enum Category {
    VISUAL("Render"),
    HUD("HUD"),
    UTILITY("Misc"),
    CLIENT("Settings");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
