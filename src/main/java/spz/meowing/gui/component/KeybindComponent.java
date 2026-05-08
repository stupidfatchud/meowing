package spz.meowing.gui.component;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import spz.meowing.module.Module;
import spz.meowing.setting.KeybindSetting;
import spz.meowing.util.ColorUtil;
import spz.meowing.util.RenderUtil;

public class KeybindComponent extends SettingComponent {

    public static KeybindComponent listening = null;

    private final Module module;
    private float pulseAnim = 0f;

    public KeybindComponent(Module module) {
        super(module.getKeybindSetting());
        this.module = module;
    }

    public boolean isListening() {
        return listening == this;
    }

    public void onKeyPressed(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            module.setKeyCode(-1);
        } else {
            module.setKeyCode(keyCode);
        }
        listening = null;
    }

    @Override
    public void render(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int mouseX, int mouseY, float delta) {
        KeybindSetting kSetting = (KeybindSetting) setting;

        if (isListening()) {
            pulseAnim += delta * 0.1f;
        } else {
            pulseAnim = 0f;
        }

        context.drawText(textRenderer, "Bind", x + 6, y + 4, ColorUtil.TEXT_SECONDARY, false);

        // Key badge
        String keyName = isListening() ? "..." : kSetting.getKeyName();
        int textW = textRenderer.getWidth(keyName);
        int badgeX = x + width - textW - 12;
        int badgeY = y + 2;

        int badgeColor = isListening()
                ? ColorUtil.interpolate(ColorUtil.KEYBIND_LISTEN, ColorUtil.darker(ColorUtil.KEYBIND_LISTEN, 0.3f),
                    (float) (Math.sin(pulseAnim * 4) * 0.5 + 0.5))
                : ColorUtil.KEYBIND_BG;

        RenderUtil.drawRoundedRect(context, badgeX, badgeY, textW + 6, 12, 3, badgeColor);
        context.drawText(textRenderer, keyName, badgeX + 3, badgeY + 2, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int button) {
        if (button == 0 && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + getHeight()) {
            if (isListening()) {
                listening = null;
            } else {
                listening = this;
            }
            return true;
        }
        return false;
    }
}
