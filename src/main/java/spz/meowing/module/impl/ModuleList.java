package spz.meowing.module.impl;

import spz.meowing.module.Category;
import spz.meowing.module.Module;
import spz.meowing.setting.BooleanSetting;
import spz.meowing.setting.ModeSetting;
import spz.meowing.setting.NumberSetting;

public class ModuleList extends Module {

    private final ModeSetting sorting = addSetting(new ModeSetting("Sort", "Length", "Length", "Alphabetical"));
    private final ModeSetting colorMode = addSetting(new ModeSetting("Color", "Rainbow", "Rainbow", "Static", "Category", "Chroma"));
    private final NumberSetting rainbowSpeed = addSetting(new NumberSetting("Rainbow Speed", 5, 1, 20, 1));
    private final NumberSetting opacity = addSetting(new NumberSetting("BG Opacity", 0.6, 0.0, 1.0, 0.05));
    private final BooleanSetting background = addSetting(new BooleanSetting("Background", true));
    private final BooleanSetting shadow = addSetting(new BooleanSetting("Text Shadow", true));
    private final BooleanSetting accentBar = addSetting(new BooleanSetting("Accent Bar", true));
    private final BooleanSetting uppercase = addSetting(new BooleanSetting("Uppercase", false));

    public ModuleList() {
        super("ModuleList", "Displays enabled modules", Category.RENDER, -1);
    }

    public String getSorting() { return sorting.getValue(); }
    public String getColorMode() { return colorMode.getValue(); }
    public int getRainbowSpeed() { return (int) rainbowSpeed.getValue().doubleValue(); }
    public int getBgAlpha() { return (int) (opacity.getValue() * 255); }
    public boolean hasBackground() { return background.getValue(); }
    public boolean hasShadow() { return shadow.getValue(); }
    public boolean hasAccentBar() { return accentBar.getValue(); }
    public boolean isUppercase() { return uppercase.getValue(); }
}
