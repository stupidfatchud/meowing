package spz.meowing.module.impl;

import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.BooleanSetting;
import spz.meowing.setting.ModeSetting;
import spz.meowing.setting.NumberSetting;

public class Keystrokes extends Module {

    private final NumberSetting posX = addSetting(new NumberSetting("X Offset", 4, 0, 500, 1));
    private final NumberSetting posY = addSetting(new NumberSetting("Y Offset", 100, 0, 500, 1));
    private final NumberSetting keySize = addSetting(new NumberSetting("Key Size", 22, 16, 36, 1));
    private final NumberSetting opacity = addSetting(new NumberSetting("Opacity", 0.75, 0.1, 1.0, 0.05));
    private final BooleanSetting showMouse = addSetting(new BooleanSetting("Show Mouse", true));
    private final BooleanSetting showSpace = addSetting(new BooleanSetting("Show Space", true));
    private final BooleanSetting showCps = addSetting(new BooleanSetting("Show CPS", true));
    private final BooleanSetting showSneak = addSetting(new BooleanSetting("Show Sneak", false));
    private final ModeSetting colorTheme = addSetting(new ModeSetting("Color", "Purple",
            "Purple", "Blue", "Cyan", "Green", "Red", "White", "Chroma"));
    private final ModeSetting style = addSetting(new ModeSetting("Style", "Rounded", "Rounded", "Square", "Outline"));

    // CPS tracking
    private final long[] leftClicks = new long[20];
    private final long[] rightClicks = new long[20];
    private int leftIdx = 0, rightIdx = 0;

    public Keystrokes() {
        super("Keystrokes", "Displays key inputs on screen", Category.RENDER, -1);
    }

    public void registerLeftClick() {
        leftClicks[leftIdx] = System.currentTimeMillis();
        leftIdx = (leftIdx + 1) % leftClicks.length;
    }

    public void registerRightClick() {
        rightClicks[rightIdx] = System.currentTimeMillis();
        rightIdx = (rightIdx + 1) % rightClicks.length;
    }

    public int getLeftCps() { return countRecent(leftClicks); }
    public int getRightCps() { return countRecent(rightClicks); }

    private int countRecent(long[] times) {
        long now = System.currentTimeMillis();
        int count = 0;
        for (long t : times) if (now - t < 1000) count++;
        return count;
    }

    public boolean isChroma() { return colorTheme.getValue().equals("Chroma"); }

    public int getAccentColor() {
        return switch (colorTheme.getValue()) {
            case "Blue" -> 0xFF3498DB;
            case "Cyan" -> 0xFF00D2FF;
            case "Green" -> 0xFF2ECC71;
            case "Red" -> 0xFFE74C3C;
            case "White" -> 0xFFFFFFFF;
            case "Chroma" -> 0xFFFFFFFF;
            default -> 0xFF6C5CE7;
        };
    }

    public int getPosX() { return (int) posX.getValue().doubleValue(); }
    public int getPosY() { return (int) posY.getValue().doubleValue(); }
    public int getKeySize() { return (int) keySize.getValue().doubleValue(); }
    public int getBgAlpha() { return (int) (opacity.getValue() * 255); }
    public boolean showMouse() { return showMouse.getValue(); }
    public boolean showSpace() { return showSpace.getValue(); }
    public boolean showCps() { return showCps.getValue(); }
    public boolean showSneak() { return showSneak.getValue(); }
    public String getStyle() { return style.getValue(); }
}
