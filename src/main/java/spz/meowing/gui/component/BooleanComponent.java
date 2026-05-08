package spz.meowing.gui.component;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import spz.meowing.setting.BooleanSetting;
import spz.meowing.util.AnimationUtil;
import spz.meowing.util.ColorUtil;
import spz.meowing.util.RenderUtil;
import spz.meowing.util.ThemeHelper;

public class BooleanComponent extends SettingComponent {

    private float toggleAnim = 0f;

    public BooleanComponent(BooleanSetting setting) {
        super(setting);
        toggleAnim = setting.getValue() ? 1f : 0f;
    }

    @Override
    public void render(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int mouseX, int mouseY, float delta) {
        BooleanSetting bSetting = (BooleanSetting) setting;
        float target = bSetting.getValue() ? 1f : 0f;
        toggleAnim = AnimationUtil.lerp(toggleAnim, target, 0.2f);

        // Label
        context.drawText(textRenderer, setting.getName(), x + 6, y + 4, ColorUtil.TEXT_SECONDARY, false);

        // Toggle track
        int trackW = 20;
        int trackH = 10;
        int trackX = x + width - trackW - 6;
        int trackY = y + 3;

        int trackColor = ColorUtil.interpolate(ColorUtil.TOGGLE_OFF, ThemeHelper.accent(), toggleAnim);
        RenderUtil.drawRoundedRect(context, trackX, trackY, trackW, trackH, 5, trackColor);

        // Toggle knob
        int knobSize = 8;
        int knobRange = trackW - knobSize - 2;
        int knobX = trackX + 1 + (int) (knobRange * toggleAnim);
        int knobY = trackY + 1;
        RenderUtil.drawRoundedRect(context, knobX, knobY, knobSize, knobSize, 4, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int button) {
        if (button == 0 && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + getHeight()) {
            ((BooleanSetting) setting).toggle();
            return true;
        }
        return false;
    }
}
