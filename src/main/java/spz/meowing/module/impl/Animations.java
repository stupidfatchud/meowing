package spz.meowing.module.impl;

import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.BooleanSetting;
import spz.meowing.setting.ModeSetting;
import spz.meowing.setting.NumberSetting;

public class Animations extends Module {

    private final ModeSetting swingMode = addSetting(new ModeSetting("Swing Mode", "Normal",
            "Normal", "1.7", "Tap", "Slide", "Smooth", "Spin", "Sigma", "Punch", "Down"));
    private final NumberSetting size = addSetting(new NumberSetting("Size", 1.0, 0.1, 3.0, 0.1));
    private final NumberSetting posX = addSetting(new NumberSetting("X Position", 0.0, -2.0, 2.0, 0.05));
    private final NumberSetting posY = addSetting(new NumberSetting("Y Position", 0.0, -2.0, 2.0, 0.05));
    private final NumberSetting posZ = addSetting(new NumberSetting("Z Position", 0.0, -2.0, 2.0, 0.05));
    private final NumberSetting yaw = addSetting(new NumberSetting("Yaw", 0.0, -180.0, 180.0, 5.0));
    private final NumberSetting pitch = addSetting(new NumberSetting("Pitch", 0.0, -180.0, 180.0, 5.0));
    private final NumberSetting roll = addSetting(new NumberSetting("Roll", 0.0, -180.0, 180.0, 5.0));
    private final BooleanSetting noEffects = addSetting(new BooleanSetting("No Effects", false));
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", 1.0, 0.1, 4.0, 0.1));
    private final BooleanSetting noSwing = addSetting(new BooleanSetting("No Swing", false));
    private final BooleanSetting noEquipReset = addSetting(new BooleanSetting("No Equip Reset", false));
    private final BooleanSetting disableReswing = addSetting(new BooleanSetting("Disable Reswing", false));

    public Animations() {
        super("Animations", "Customize held item rendering", Category.MISC, -1);
    }

    public String getSwingMode() { return swingMode.getValue(); }
    public double getSize() { return size.getValue(); }
    public double getPosX() { return posX.getValue(); }
    public double getPosY() { return posY.getValue(); }
    public double getPosZ() { return posZ.getValue(); }
    public double getYaw() { return yaw.getValue(); }
    public double getPitch() { return pitch.getValue(); }
    public double getRoll() { return roll.getValue(); }
    public boolean isNoEffects() { return noEffects.getValue(); }
    public double getSpeed() { return speed.getValue(); }
    public boolean isNoSwing() { return noSwing.getValue(); }
    public boolean isNoEquipReset() { return noEquipReset.getValue(); }
    public boolean isDisableReswing() { return disableReswing.getValue(); }
}
