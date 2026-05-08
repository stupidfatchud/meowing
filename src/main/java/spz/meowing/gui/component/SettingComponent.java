package spz.meowing.gui.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import spz.meowing.setting.Setting;

public abstract class SettingComponent {

    protected final Setting<?> setting;
    protected float animationProgress = 0f;

    public SettingComponent(Setting<?> setting) {
        this.setting = setting;
    }

    public abstract void render(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int mouseX, int mouseY, float delta);

    public abstract boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int button);

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, int x, int y, int width) {
        return false;
    }

    public int getHeight() {
        return 16;
    }

    public boolean isVisible() {
        return setting.isVisible();
    }

    public Setting<?> getSetting() {
        return setting;
    }
}
