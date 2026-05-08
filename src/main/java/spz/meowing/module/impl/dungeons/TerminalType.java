package spz.meowing.module.impl.dungeons;

public enum TerminalType {
    NUMBERS("Click in order!"),
    COLORS("What starts with:"),
    PANES("Correct all the panes!"),
    STARTS_WITH("What starts with:"),
    RUBIX("Change all to same color!"),
    MELODY("Click the button on time!");

    private final String titleMatch;

    TerminalType(String titleMatch) {
        this.titleMatch = titleMatch;
    }

    public static TerminalType fromTitle(String title) {
        if (title == null) return null;
        String clean = title.replaceAll("§.", "");
        if (clean.startsWith("Click in order!")) return NUMBERS;
        if (clean.startsWith("Correct all the panes!")) return PANES;
        if (clean.startsWith("Change all to same color!")) return RUBIX;
        if (clean.startsWith("Click the button on time!")) return MELODY;
        if (clean.startsWith("What starts with:")) return STARTS_WITH;
        if (clean.startsWith("Select all the")) return COLORS;
        return null;
    }

    public String getTitleMatch() {
        return titleMatch;
    }
}
