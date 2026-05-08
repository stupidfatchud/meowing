package spz.meowing.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import spz.meowing.gui.component.*;
import spz.meowing.module.Module;
import spz.meowing.setting.*;
import spz.meowing.util.AnimationUtil;
import spz.meowing.util.ColorUtil;
import spz.meowing.util.RenderUtil;
import spz.meowing.util.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class ModuleButton {

    private final Module module;
    private final List<SettingComponent> components = new ArrayList<>();
    private final KeybindComponent keybindComponent;

    private boolean expanded = false;
    private boolean visible = true;
    private float expandAnim = 0f;
    private float hoverAnim = 0f;
    private float enableAnim = 0f;

    public static final int MODULE_HEIGHT = 24;
    public static final int SETTING_INDENT = 6;

    public ModuleButton(Module module) {
        this.module = module;

        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting b) {
                components.add(new BooleanComponent(b));
            } else if (setting instanceof NumberSetting n) {
                components.add(new SliderComponent(n));
            } else if (setting instanceof ModeSetting m) {
                components.add(new ModeComponent(m));
            } else if (setting instanceof spz.meowing.setting.StringSetting s) {
                components.add(new spz.meowing.gui.component.StringComponent(s));
            }
        }

        keybindComponent = new KeybindComponent(module);
    }

    public void render(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int mouseX, int mouseY, float delta) {
        boolean hovered = RenderUtil.isHovered(mouseX, mouseY, x, y, width, MODULE_HEIGHT);
        hoverAnim = AnimationUtil.lerp(hoverAnim, hovered ? 1f : 0f, 0.2f);
        enableAnim = AnimationUtil.lerp(enableAnim, module.isEnabled() ? 1f : 0f, 0.15f);
        expandAnim = AnimationUtil.lerp(expandAnim, expanded ? 1f : 0f, 0.2f);

        // Module row background with hover
        int bgColor = ColorUtil.interpolate(ThemeHelper.moduleBg(), ThemeHelper.moduleHover(), hoverAnim);
        RenderUtil.drawRect(context, x, y, width, MODULE_HEIGHT, bgColor);

        // Enabled accent bar (left edge, animated height)
        if (enableAnim > 0.01f) {
            int barH = (int) ((MODULE_HEIGHT - 10) * enableAnim);
            int barY = y + (MODULE_HEIGHT - barH) / 2;
            int barColor = ColorUtil.withAlpha(ThemeHelper.accent(), (int) (255 * enableAnim));
            RenderUtil.drawRoundedRect(context, x + 2, barY, 3, barH, 1, barColor);
        }

        // Module name
        int textColor = ColorUtil.interpolate(ColorUtil.TEXT_SECONDARY, ColorUtil.TEXT_PRIMARY, enableAnim);
        context.drawText(textRenderer, module.getName(), x + 10, y + (MODULE_HEIGHT - 8) / 2, textColor, false);

        // Keybind badge (right side, compact)
        if (module.getKeyCode() > 0) {
            String keyName = module.getKeybindSetting().getKeyName();
            int keyW = textRenderer.getWidth(keyName);
            int badgeX = x + width - keyW - 10;
            int badgeY = y + (MODULE_HEIGHT - 11) / 2;
            RenderUtil.drawRoundedRect(context, badgeX - 2, badgeY, keyW + 4, 11, 2, ColorUtil.KEYBIND_BG);
            context.drawText(textRenderer, keyName, badgeX, badgeY + 2, ColorUtil.TEXT_SECONDARY, false);
        }

        // Settings panel (animated expand)
        if (expandAnim > 0.01f) {
            int settingsHeight = getSettingsHeight();
            int visibleHeight = (int) (settingsHeight * expandAnim);

            if (visibleHeight > 0) {
                int sy = y + MODULE_HEIGHT;
                context.enableScissor(x, sy, x + width, sy + visibleHeight);

                RenderUtil.drawRect(context, x, sy, width, settingsHeight, ColorUtil.darker(ThemeHelper.moduleBg(), 0.2f));

                // Accent line on left side of settings
                context.fill(x + 3, sy + 2, x + 4, sy + settingsHeight - 2,
                        ColorUtil.withAlpha(ThemeHelper.accent(), 60));

                int cy = sy + 2;
                for (SettingComponent comp : components) {
                    if (comp.isVisible()) {
                        comp.render(context, textRenderer, x + SETTING_INDENT, cy, width - SETTING_INDENT * 2, mouseX, mouseY, delta);
                        cy += comp.getHeight();
                    }
                }
                keybindComponent.render(context, textRenderer, x + SETTING_INDENT, cy, width - SETTING_INDENT * 2, mouseX, mouseY, delta);

                context.disableScissor();
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int button) {
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + MODULE_HEIGHT) {
            if (button == 0) {
                module.toggle();
                return true;
            } else if (button == 1) {
                expanded = !expanded;
                return true;
            }
        }

        if (expanded && expandAnim > 0.5f) {
            int sy = y + MODULE_HEIGHT + 2;
            int cy = sy;
            for (SettingComponent comp : components) {
                if (comp.isVisible()) {
                    if (comp.mouseClicked(mouseX, mouseY, x + SETTING_INDENT, cy, width - SETTING_INDENT * 2, button)) {
                        return true;
                    }
                    cy += comp.getHeight();
                }
            }
            if (keybindComponent.mouseClicked(mouseX, mouseY, x + SETTING_INDENT, cy, width - SETTING_INDENT * 2, button)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (SettingComponent comp : components) {
            if (comp.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, int x, int y, int width) {
        if (expanded) {
            int sy = y + MODULE_HEIGHT + 2;
            int cy = sy;
            for (SettingComponent comp : components) {
                if (comp.isVisible()) {
                    if (comp.mouseDragged(mouseX, mouseY, button, x + SETTING_INDENT, cy, width - SETTING_INDENT * 2)) {
                        return true;
                    }
                    cy += comp.getHeight();
                }
            }
        }
        return false;
    }

    public int getHeight() {
        if (!visible) return 0;
        int h = MODULE_HEIGHT;
        if (expandAnim > 0.01f) {
            h += (int) ((getSettingsHeight() + 4) * expandAnim);
        }
        return h;
    }

    private int getSettingsHeight() {
        int h = 0;
        for (SettingComponent comp : components) {
            if (comp.isVisible()) {
                h += comp.getHeight();
            }
        }
        h += keybindComponent.getHeight();
        return h;
    }

    public Module getModule() {
        return module;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
