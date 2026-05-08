package spz.meowing.gui.component;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import spz.meowing.setting.ModeSetting;
import spz.meowing.util.ColorUtil;
import spz.meowing.util.RenderUtil;

public class ModeComponent extends SettingComponent {

    public ModeComponent(ModeSetting setting) {
        super(setting);
    }

    @Override
    public void render(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int mouseX, int mouseY, float delta) {
        ModeSetting mSetting = (ModeSetting) setting;

        context.drawText(textRenderer, setting.getName(), x + 6, y + 4, ColorUtil.TEXT_SECONDARY, false);

        // Mode badge
        String mode = mSetting.getValue();
        int textW = textRenderer.getWidth(mode);
        int badgeX = x + width - textW - 12;
        int badgeY = y + 2;

        RenderUtil.drawRoundedRect(context, badgeX, badgeY, textW + 6, 12, 3, ColorUtil.KEYBIND_BG);
        context.drawText(textRenderer, mode, badgeX + 3, badgeY + 2, ColorUtil.TEXT_ACCENT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int button) {
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + getHeight()) {
            ModeSetting mSetting = (ModeSetting) setting;
            if (button == 0) {
                mSetting.cycle();
            } else if (button == 1) {
                mSetting.cycleBack();
            }
            return true;
        }
        return false;
    }
}
