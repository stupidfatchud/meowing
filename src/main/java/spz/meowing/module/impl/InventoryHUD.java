package spz.meowing.module.impl;

import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.BooleanSetting;
import spz.meowing.setting.ModeSetting;
import spz.meowing.setting.NumberSetting;

public class InventoryHUD extends Module {

    private final BooleanSetting showInventory = addSetting(new BooleanSetting("Show Inventory", true));
    private final BooleanSetting showArmor = addSetting(new BooleanSetting("Show Armor", true));
    private final BooleanSetting showOffhand = addSetting(new BooleanSetting("Show Offhand", true));
    private final BooleanSetting showDurability = addSetting(new BooleanSetting("Show Durability", true));
    private final BooleanSetting showItemCount = addSetting(new BooleanSetting("Show Item Count", true));
    private final BooleanSetting background = addSetting(new BooleanSetting("Background", true));
    private final NumberSetting opacity = addSetting(new NumberSetting("BG Opacity", 0.6, 0.0, 1.0, 0.05));
    private final ModeSetting layout = addSetting(new ModeSetting("Layout", "Compact", "Compact", "Full Grid", "Horizontal"));
    private final NumberSetting posX = addSetting(new NumberSetting("X Offset", 0, -500, 500, 1));
    private final NumberSetting posY = addSetting(new NumberSetting("Y Offset", 0, -500, 500, 1));

    public InventoryHUD() {
        super("InventoryHUD", "Shows inventory on screen", Category.RENDER, -1);
    }

    public boolean showInventory() { return showInventory.getValue(); }
    public boolean showArmor() { return showArmor.getValue(); }
    public boolean showOffhand() { return showOffhand.getValue(); }
    public boolean showDurability() { return showDurability.getValue(); }
    public boolean showItemCount() { return showItemCount.getValue(); }
    public boolean hasBackground() { return background.getValue(); }
    public int getBgAlpha() { return (int) (opacity.getValue() * 255); }
    public String getLayout() { return layout.getValue(); }
    public int getPosX() { return (int) posX.getValue().doubleValue(); }
    public int getPosY() { return (int) posY.getValue().doubleValue(); }
}
