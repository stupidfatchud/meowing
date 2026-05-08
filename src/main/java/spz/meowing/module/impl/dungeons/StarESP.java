package spz.meowing.module.impl.dungeons;

import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.ModeSetting;
import spz.meowing.setting.NumberSetting;

/**
 * Highlights starred dungeon mobs — entities with a yellow star (✯) in their nametag.
 * These are mini-boss mobs that count toward the dungeon score.
 */
public class StarESP extends Module {

    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "3D Box", "3D Box", "2D Box", "Nametag"));
    private final NumberSetting range = addSetting(new NumberSetting("Range", 30, 5, 64, 1));
    private final NumberSetting lineWidth = addSetting(new NumberSetting("Line Width", 2, 1, 5, 1));
    private final ModeSetting color = addSetting(new ModeSetting("Color", "Yellow",
            "Yellow", "Green", "Cyan", "Purple", "Red", "White", "Rainbow"));
    private final NumberSetting rainbowSpeed = addSetting(new NumberSetting("Rainbow Speed", 1.0, 0.1, 5.0, 0.1));

    public StarESP() {
        super("StarESP", "Highlights starred dungeon mobs", Category.DUNGEONS, -1);
    }

    public String getMode() { return mode.getValue(); }
    public double getRange() { return range.getValue(); }
    public int getLineWidth() { return (int) lineWidth.getValue().doubleValue(); }
    public boolean isRainbow() { return color.getValue().equals("Rainbow"); }
    public float getRainbowSpeed() { return (float) rainbowSpeed.getValue().doubleValue(); }

    public int getColor() {
        return switch (color.getValue()) {
            case "Green" -> 0xFF2ECC71;
            case "Cyan" -> 0xFF00D2FF;
            case "Purple" -> 0xFF6C5CE7;
            case "Red" -> 0xFFE74C3C;
            case "White" -> 0xFFFFFFFF;
            case "Rainbow" -> 0xFFFFFFFF;
            default -> 0xFFFFD700; // Yellow
        };
    }
}
