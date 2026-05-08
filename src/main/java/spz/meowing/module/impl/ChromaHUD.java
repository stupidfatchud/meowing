package spz.meowing.module.impl;

import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.NumberSetting;

public class ChromaHUD extends Module {

    private final NumberSetting speed = addSetting(new NumberSetting("Speed", 5, 1, 20, 1));
    private final NumberSetting spread = addSetting(new NumberSetting("Spread", 8, 1, 30, 1));
    private final NumberSetting saturation = addSetting(new NumberSetting("Saturation", 0.7, 0.1, 1.0, 0.05));

    public ChromaHUD() {
        super("ChromaHUD", "Rainbow text on all HUD elements", Category.MISC, -1);
    }

    public int getSpeed() { return (int) speed.getValue().doubleValue(); }
    public int getSpread() { return (int) spread.getValue().doubleValue(); }
    public float getSaturation() { return (float) saturation.getValue().doubleValue(); }
}
