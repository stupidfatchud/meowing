package spz.meowing.gui.component;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import spz.meowing.setting.NumberSetting;
import spz.meowing.util.AnimationUtil;
import spz.meowing.util.ColorUtil;
import spz.meowing.util.RenderUtil;
import spz.meowing.util.ThemeHelper;

public class SliderComponent extends SettingComponent {

    private boolean dragging = false;
    private float sliderAnim = 0f;

    public SliderComponent(NumberSetting setting) {
        super(setting);
        sliderAnim = (float) setting.getPercentage();
    }

    @Override
    public void render(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int mouseX, int mouseY, float delta) {
        NumberSetting nSetting = (NumberSetting) setting;

        if (dragging) {
            int sliderX = x + 6;
            int sliderW = width - 12;
            double pct = Math.max(0, Math.min(1, (mouseX - sliderX) / (double) sliderW));
            nSetting.setFromPercentage(pct);
        }

        float target = (float) nSetting.getPercentage();
        sliderAnim = AnimationUtil.lerp(sliderAnim, target, 0.25f);

        // Label + value
        String label = setting.getName() + ": " + nSetting.getDisplayValue();
        context.drawText(textRenderer, label, x + 6, y + 2, ColorUtil.TEXT_SECONDARY, false);

        // Slider track
        int trackY = y + 12;
        int trackH = 4;
        int trackX = x + 6;
        int trackW = width - 12;

        RenderUtil.drawRoundedRect(context, trackX, trackY, trackW, trackH, 2, ColorUtil.SLIDER_BG);

        // Slider fill
        int fillW = Math.max(2, (int) (trackW * sliderAnim));
        RenderUtil.drawRoundedRect(context, trackX, trackY, fillW, trackH, 2, ThemeHelper.accent());

        // Knob
        int knobSize = 6;
        int knobX = trackX + fillW - knobSize / 2;
        int knobY = trackY - 1;
        RenderUtil.drawRoundedRect(context, knobX, knobY, knobSize, knobSize, 3, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int button) {
        if (button == 0 && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + getHeight()) {
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, int x, int y, int width) {
        return dragging;
    }

    @Override
    public int getHeight() {
        return 18;
    }
}
