package spz.meowing.module.impl;

import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.NumberSetting;

public class AspectRatio extends Module {

    private final NumberSetting customRatio = addSetting(new NumberSetting("Custom Ratio", 1.78, 0.5, 3.5, 0.01));

    public AspectRatio() {
        super("FOV", "Change display aspect ratio", Category.RENDER, -1);
    }

    public float getTargetRatio() {
        return (float) customRatio.getValue().doubleValue();
    }
}
