package spz.meowing.module;

public enum Category {
    DUNGEONS("Dungeons"),
    RENDER("Render"),
    MISC("Misc");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
