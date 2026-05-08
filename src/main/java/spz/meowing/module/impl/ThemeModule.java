package spz.meowing.module.impl;

import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.ModeSetting;
import spz.meowing.setting.NumberSetting;

public class ThemeModule extends Module {

    private final ModeSetting preset = addSetting(new ModeSetting("Preset", "Dark",
            "Dark", "AMOLED", "Neon", "Minimal"));
    private final ModeSetting accentColor = addSetting(new ModeSetting("Accent", "Purple",
            "Purple", "Blue", "Cyan", "Green", "Red", "Orange", "Pink", "White"));
    private final NumberSetting bgOpacity = addSetting(new NumberSetting("BG Opacity", 0.9, 0.3, 1.0, 0.05));

    public ThemeModule() {
        super("Theme", "Global UI color theme", Category.MISC, -1);
    }

    public int getAccent() {
        return switch (accentColor.getValue()) {
            case "Blue" -> 0xFF3498DB;
            case "Cyan" -> 0xFF00D2FF;
            case "Green" -> 0xFF2ECC71;
            case "Red" -> 0xFFE74C3C;
            case "Orange" -> 0xFFE67E22;
            case "Pink" -> 0xFFFF69B4;
            case "White" -> 0xFFCCCCCC;
            default -> 0xFF6C5CE7;
        };
    }

    public int getPanelBg() {
        int alpha = (int) (bgOpacity.getValue() * 255);
        return switch (preset.getValue()) {
            case "AMOLED" -> (alpha << 24) | 0x000000;
            case "Neon" -> (alpha << 24) | 0x0A0A18;
            case "Minimal" -> (alpha << 24) | 0x1A1A1A;
            default -> (alpha << 24) | 0x111118;
        };
    }

    public int getHeaderBg() {
        return switch (preset.getValue()) {
            case "AMOLED" -> 0xFF080808;
            case "Neon" -> 0xFF101028;
            case "Minimal" -> 0xFF222222;
            default -> 0xFF18181F;
        };
    }

    public int getModuleBg() {
        return switch (preset.getValue()) {
            case "AMOLED" -> 0xFF030303;
            case "Neon" -> 0xFF080818;
            case "Minimal" -> 0xFF181818;
            default -> 0xFF0E0E16;
        };
    }

    public int getModuleHover() {
        return switch (preset.getValue()) {
            case "AMOLED" -> 0xFF0C0C0C;
            case "Neon" -> 0xFF121230;
            case "Minimal" -> 0xFF252525;
            default -> 0xFF1A1A28;
        };
    }

    public String getPreset() { return preset.getValue(); }
}
