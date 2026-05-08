package spz.meowing.module.impl;

import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.BooleanSetting;
import spz.meowing.setting.NumberSetting;

public class CustomTooltip extends Module {

    private final BooleanSetting rarityBorder = addSetting(new BooleanSetting("Rarity Border", true));
    private final NumberSetting borderOpacity = addSetting(new NumberSetting("Border Opacity", 1.0, 0.2, 1.0, 0.1));
    private final NumberSetting bgOpacity = addSetting(new NumberSetting("BG Opacity", 0.92, 0.3, 1.0, 0.05));
    private final BooleanSetting dottedStats = addSetting(new BooleanSetting("Dotted Stats", true));
    private final BooleanSetting rarityBadge = addSetting(new BooleanSetting("Rarity Badge", true));
    private final BooleanSetting statPanel = addSetting(new BooleanSetting("Stat Panel", true));

    public CustomTooltip() {
        super("CustomTooltip", "Custom styled item tooltips", Category.RENDER, -1);
    }

    public boolean rarityBorder() { return rarityBorder.getValue(); }
    public float borderOpacity() { return (float) borderOpacity.getValue().doubleValue(); }
    public float bgOpacity() { return (float) bgOpacity.getValue().doubleValue(); }
    public boolean dottedStats() { return dottedStats.getValue(); }
    public boolean rarityBadge() { return rarityBadge.getValue(); }
    public boolean statPanel() { return statPanel.getValue(); }

    /**
     * Detect Skyblock item rarity from the last tooltip line.
     * Returns the ARGB color for the rarity, or -1 if not a SB item.
     */
    public static int getRarityColor(String lastLine) {
        String clean = lastLine.replaceAll("§.", "").trim().toUpperCase();
        if (clean.contains("MYTHIC")) return 0xFFFF55FF;
        if (clean.contains("LEGENDARY")) return 0xFFFFAA00;
        if (clean.contains("EPIC")) return 0xFFAA00AA;
        if (clean.contains("DIVINE")) return 0xFF55FFFF;
        if (clean.contains("SPECIAL") || clean.contains("VERY SPECIAL")) return 0xFFFF5555;
        if (clean.contains("RARE")) return 0xFF5555FF;
        if (clean.contains("UNCOMMON")) return 0xFF55FF55;
        if (clean.contains("COMMON")) return 0xFFFFFFFF;
        return -1;
    }

    /**
     * Get the Minecraft formatting code color for a rarity.
     */
    public static String getRarityCode(String lastLine) {
        String clean = lastLine.replaceAll("§.", "").trim().toUpperCase();
        if (clean.contains("MYTHIC")) return "§d";
        if (clean.contains("LEGENDARY")) return "§6";
        if (clean.contains("EPIC")) return "§5";
        if (clean.contains("DIVINE")) return "§b";
        if (clean.contains("SPECIAL") || clean.contains("VERY SPECIAL")) return "§c";
        if (clean.contains("RARE")) return "§9";
        if (clean.contains("UNCOMMON")) return "§a";
        if (clean.contains("COMMON")) return "§f";
        return "";
    }

    /**
     * Extract rarity name from the last line (e.g., "MYTHIC DUNGEON SWORD" -> "MYTHIC").
     */
    public static String getRarityName(String lastLine) {
        String clean = lastLine.replaceAll("§.", "").trim().toUpperCase();
        if (clean.contains("VERY SPECIAL")) return "VERY SPECIAL";
        for (String r : new String[]{"MYTHIC", "LEGENDARY", "EPIC", "DIVINE", "SPECIAL", "RARE", "UNCOMMON", "COMMON"}) {
            if (clean.contains(r)) return r;
        }
        return "";
    }

    /**
     * Extract the item type from the last line (e.g., "MYTHIC DUNGEON SWORD" -> "DUNGEON SWORD").
     */
    public static String getItemType(String lastLine) {
        String clean = lastLine.replaceAll("§.", "").trim();
        String rarity = getRarityName(lastLine);
        if (rarity.isEmpty()) return clean;
        // Remove the rarity prefix and special chars
        String result = clean.toUpperCase().replace(rarity, "").replace("✦", "").replace("⚚", "").trim();
        return result;
    }
}
